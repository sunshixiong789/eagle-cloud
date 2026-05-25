package com.eagle.rocketmq.lock;

import com.eagle.common.exception.codes.LockErrorCode;
import com.eagle.common.lock.LockProperties;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.beans.factory.InitializingBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * MQ 锁 token 初始化器。
 *
 * <p>启动时按 {@code eagle.lock.keys} 配置为每个 lockKey 在对应 topic 中发送一条 token 消息，
 * 是 {@link RocketMqDistributedLock} 工作的前置条件。
 *
 * <p><b>注意</b>：RocketMQ 不提供消息去重，多节点同时启动会发出多条 token，导致锁退化为
 * 信号量。生产环境应将 {@code eagle.lock.auto-init-token} 设为 {@code false}，由运维通过
 * 一次性脚本初始化（仅在主节点或单独的 init job 中执行）。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class LockTokenInitializer implements InitializingBean {

    private final RocketMqProperties mqProps;
    private final LockProperties lockProps;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!lockProps.isAutoInitToken()) {
            log.info("eagle.lock.auto-init-token = false, skip MQ lock token initialization");
            return;
        }

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration cfg = ClientConfiguration.newBuilder()
                .setEndpoints(mqProps.getEndpoints())
                .setRequestTimeout(Duration.ofMillis(mqProps.getRequestTimeoutMillis()))
                .enableSsl(mqProps.isSslEnabled())
                .build();

        try (Producer producer = provider.newProducerBuilder()
                .setClientConfiguration(cfg)
                .build()) {

            if (lockProps.getGranularity() == LockProperties.Granularity.PER_KEY) {
                for (String key : lockProps.getKeys()) {
                    sendToken(provider, producer, lockProps.getTopicPrefix() + key, key);
                }
            } else {
                sendToken(provider, producer, lockProps.getSharedTopic(), "global");
            }
        }
    }

    private void sendToken(ClientServiceProvider provider, Producer producer, String topic, String tokenKey) {
        Message msg = provider.newMessageBuilder()
                .setTopic(topic)
                .setKeys(tokenKey)
                .setBody("token".getBytes(StandardCharsets.UTF_8))
                .build();
        try {
            var receipt = producer.send(msg);
            log.info("Lock token published, topic: {}, key: {}, messageId: {}",
                    topic, tokenKey, receipt.getMessageId());
        } catch (ClientException e) {
            throw LockErrorCode.LOCK_TOKEN_INIT_FAILED.toServiceException(e);
        }
    }
}
