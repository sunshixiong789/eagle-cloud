package com.eagle.rocketmq.lock;

import com.eagle.common.exception.codes.LockErrorCode;
import com.eagle.common.lock.DistributedLock;
import com.eagle.common.lock.LockProperties;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 基于 RocketMQ 5.x SimpleConsumer 的 {@link DistributedLock} 实现。
 *
 * <p><b>原理</b>：每个 lockKey 对应 topic 中预置一条 token 消息，
 * {@code tryLock} 通过 {@link SimpleConsumer#receive} 独占拉取该 token（broker 保证仅一个消费者拿到），
 * 拉取成功即获得锁；持锁期间 token 处于 invisible 状态对其他消费者不可见；
 * 释放锁时调用 {@link SimpleConsumer#changeInvisibleDuration} 让 token 立即重新可见。
 *
 * <p><b>崩溃恢复</b>：进程意外终止未释放锁时，token 的 {@code invisibleDuration}（即
 * 业务传入的 {@code leaseTime}）到期后自动重新可见，等价于 Redis 锁的 TTL 行为。
 *
 * <p><b>限制</b>：
 * <ul>
 *   <li>{@link LockProperties.Granularity#PER_KEY} 模式下，未在 {@code eagle.lock.keys} 声明的
 *       lockKey 无法获取锁（无对应 SimpleConsumer）</li>
 *   <li>{@link LockProperties.Granularity#SHARED_TOPIC} 模式下所有 lockKey 退化为全局单锁</li>
 * </ul>
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class RocketMqDistributedLock implements DistributedLock, InitializingBean, DisposableBean {

    private final RocketMqProperties mqProps;
    private final LockProperties lockProps;
    private final Map<String, SimpleConsumer> consumerCache = new ConcurrentHashMap<>();
    private ClientServiceProvider provider;
    private SimpleConsumer sharedConsumer;

    @Override
    public void afterPropertiesSet() throws Exception {
        provider = ClientServiceProvider.loadService();
        ClientConfiguration cfg = ClientConfiguration.newBuilder()
                .setEndpoints(mqProps.getEndpoints())
                .setRequestTimeout(Duration.ofMillis(mqProps.getRequestTimeoutMillis()))
                .enableSsl(mqProps.isSslEnabled())
                .build();

        if (lockProps.getGranularity() == LockProperties.Granularity.PER_KEY) {
            for (String key : lockProps.getKeys()) {
                consumerCache.put(key, buildConsumer(cfg, lockProps.getTopicPrefix() + key));
            }
            log.info("RocketMQ distributed lock initialized in PER_KEY mode, keys: {}", lockProps.getKeys());
        } else {
            sharedConsumer = buildConsumer(cfg, lockProps.getSharedTopic());
            log.info("RocketMQ distributed lock initialized in SHARED_TOPIC mode, topic: {}",
                    lockProps.getSharedTopic());
        }
    }

    @Override
    public <T> T tryLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        SimpleConsumer consumer = resolveConsumer(lockKey);
        MessageView token = pollToken(consumer, lockKey, waitTime, leaseTime);
        if (token == null) {
            log.warn("Failed to acquire MQ distributed lock: {}", lockKey);
            throw LockErrorCode.LOCK_ACQUIRE_FAILED.toServiceException();
        }
        try {
            return supplier.get();
        } finally {
            releaseToken(consumer, lockKey, token);
        }
    }

    @Override
    public void destroy() {
        consumerCache.values().forEach(this::silentClose);
        consumerCache.clear();
        if (sharedConsumer != null) {
            silentClose(sharedConsumer);
            sharedConsumer = null;
        }
    }

    // -------------------------------------------------------------------------
    // 内部工具
    // -------------------------------------------------------------------------

    private SimpleConsumer buildConsumer(ClientConfiguration cfg, String topic) throws ClientException {
        FilterExpression filter = new FilterExpression("*", FilterExpressionType.TAG);
        return provider.newSimpleConsumerBuilder()
                .setClientConfiguration(cfg)
                .setConsumerGroup(lockProps.getConsumerGroup())
                .setAwaitDuration(Duration.ofSeconds(lockProps.getPollIntervalSeconds()))
                .setSubscriptionExpressions(Map.of(topic, filter))
                .build();
    }

    private SimpleConsumer resolveConsumer(String lockKey) {
        if (lockProps.getGranularity() == LockProperties.Granularity.SHARED_TOPIC) {
            return sharedConsumer;
        }
        SimpleConsumer c = consumerCache.get(lockKey);
        if (c == null) {
            log.error("MQ lock key not declared in eagle.lock.keys: {}", lockKey);
            throw LockErrorCode.LOCK_ACQUIRE_FAILED.toServiceException();
        }
        return c;
    }

    private MessageView pollToken(SimpleConsumer consumer, String lockKey, long waitTime, long leaseTime) {
        long deadline = System.currentTimeMillis() + waitTime * 1000L;
        do {
            try {
                List<MessageView> msgs = consumer.receive(1, Duration.ofSeconds(leaseTime));
                if (msgs != null && !msgs.isEmpty()) {
                    return msgs.get(0);
                }
            } catch (ClientException e) {
                log.error("MQ lock receive failed, key: {}", lockKey, e);
                throw LockErrorCode.LOCK_ACQUIRE_FAILED.toServiceException(e);
            } catch (Throwable t) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw LockErrorCode.LOCK_INTERRUPTED.toServiceException(t);
                }
                throw LockErrorCode.LOCK_ACQUIRE_FAILED.toServiceException(t);
            }
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    private void releaseToken(SimpleConsumer consumer, String lockKey, MessageView token) {
        try {
            consumer.changeInvisibleDuration(token, Duration.ofSeconds(1));
        } catch (ClientException e) {
            // 释放失败不影响业务返回，invisibleDuration 到期后 token 自动恢复
            log.error("Failed to release MQ lock: {}, will recover after invisibleDuration", lockKey, e);
        }
    }

    private void silentClose(SimpleConsumer consumer) {
        try {
            consumer.close();
        } catch (IOException e) {
            log.warn("Failed to close SimpleConsumer", e);
        }
    }
}
