package com.eagle.rocketmq.transaction;

import com.alibaba.fastjson2.JSON;
import com.eagle.common.event.BaseEvent;
import com.eagle.rocketmq.exception.RocketMqErrorCode;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.Transaction;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.apache.rocketmq.client.apis.producer.TransactionResolution;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * RocketMQ 事务消息发布器实现。
 *
 * <p>通过 RocketMQ 半消息机制（Two-Phase Commit）保证本地事务与消息发布的原子性：
 * <ol>
 *   <li>发送半消息（Half Message），Broker 接收但消费者不可见</li>
 *   <li>执行本地事务（{@link TransactionCallback}）</li>
 *   <li>成功则 commit，失败则 rollback</li>
 *   <li>若未收到确认，Broker 触发 {@link TransactionChecker} 回查</li>
 * </ol>
 *
 * @author eagle
 */
@Slf4j
public class RocketMqTransactionalEventPublisher implements TransactionalEventPublisher,
        InitializingBean, DisposableBean {

    private final RocketMqProperties properties;
    /**
     * 可选的回查检查器，注入时才启用事务消息，否则退化为普通消息
     */
    private final List<AbstractRocketMqTransactionChecker> checkers;

    private ClientServiceProvider provider;
    private Producer transactionProducer;

    public RocketMqTransactionalEventPublisher(RocketMqProperties properties,
                                               List<AbstractRocketMqTransactionChecker> checkers) {
        this.properties = properties;
        this.checkers = checkers;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            provider = ClientServiceProvider.loadService();
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(properties.getEndpoints())
                    .setRequestTimeout(Duration.ofMillis(properties.getRequestTimeoutMillis()))
                    .enableSsl(properties.isSslEnabled())
                    .build();

            // 组合多个 Checker：按消息 Topic 路由到对应检查器，无匹配则 UNKNOWN
            TransactionChecker compositeChecker = buildCompositeChecker();

            transactionProducer = provider.newProducerBuilder()
                    .setClientConfiguration(configuration)
                    .setTransactionChecker(compositeChecker)
                    .build();
            log.info("RocketMQ transaction producer initialized, endpoints: {}, checkers: {}",
                    properties.getEndpoints(), checkers.size());
        } catch (ClientException e) {
            throw RocketMqErrorCode.PRODUCER_INIT_FAILED.toServiceException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (transactionProducer != null) {
            transactionProducer.close();
            log.info("RocketMQ transaction producer closed");
        }
    }

    @Override
    public <T extends BaseEvent> void publishInTransaction(T event, TransactionCallback callback) {
        publishInTransaction(deriveTopic(event), event, callback);
    }

    @Override
    public <T extends BaseEvent> void publishInTransaction(String topic, T event, TransactionCallback callback) {
        if (transactionProducer == null) {
            log.warn("RocketMQ transaction producer is not ready, event dropped: topic={}", topic);
            return;
        }
        Transaction transaction = null;
        try {
            transaction = transactionProducer.beginTransaction();
            Message message = buildMessage(topic, event);
            transactionProducer.send(message, transaction);

            boolean committed = executeLocalTransaction(event.getEventId(), callback);
            if (committed) {
                transaction.commit();
                log.info("Transaction message committed, topic: {}, eventId: {}", topic, event.getEventId());
            } else {
                transaction.rollback();
                log.warn("Transaction message rolled back, topic: {}, eventId: {}", topic, event.getEventId());
            }
        } catch (ClientException e) {
            rollbackSilently(transaction, event.getEventId());
            throw RocketMqErrorCode.PUBLISH_FAILED.toServiceException(e);
        }
    }

    // -------------------------------------------------------------------------
    // 内部工具方法
    // -------------------------------------------------------------------------

    private <T extends BaseEvent> String deriveTopic(T event) {
        return properties.getTopicPrefix() + event.getClass().getSimpleName();
    }

    private <T extends BaseEvent> Message buildMessage(String topic, T event) {
        byte[] body = JSON.toJSONString(event).getBytes(StandardCharsets.UTF_8);
        return provider.newMessageBuilder()
                .setTopic(topic)
                .setBody(body)
                .setKeys(event.getEventId())
                .build();
    }

    private boolean executeLocalTransaction(String eventId, TransactionCallback callback) {
        try {
            return callback.execute();
        } catch (Exception e) {
            log.error("Local transaction failed, eventId: {}", eventId, e);
            return false;
        }
    }

    private void rollbackSilently(Transaction transaction, String eventId) {
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch (Exception ex) {
                log.warn("Failed to rollback transaction, eventId: {}", eventId, ex);
            }
        }
    }

    /**
     * 合并所有注册的 Checker：任意一个返回 COMMIT 或 ROLLBACK 即终止，否则返回 UNKNOWN。
     */
    private TransactionChecker buildCompositeChecker() {
        return messageView -> {
            for (AbstractRocketMqTransactionChecker checker : checkers) {
                TransactionResolution result = checker.check(messageView);
                if (result != TransactionResolution.UNKNOWN) {
                    return result;
                }
            }
            return TransactionResolution.UNKNOWN;
        };
    }
}
