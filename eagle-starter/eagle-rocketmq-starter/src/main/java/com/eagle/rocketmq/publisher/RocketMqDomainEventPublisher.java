package com.eagle.rocketmq.publisher;

import com.alibaba.fastjson2.JSON;
import com.eagle.common.event.BaseEvent;
import com.eagle.rocketmq.exception.RocketMqErrorCode;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * RocketMQ 领域事件发布器实现。
 *
 * <p>基于 RocketMQ 5.x 轻量客户端（gRPC），支持同步、异步、延迟和顺序消息。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class RocketMqDomainEventPublisher implements DomainEventPublisher, InitializingBean, DisposableBean {

    private final RocketMqProperties properties;

    private ClientServiceProvider provider;
    private Producer producer;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            provider = ClientServiceProvider.loadService();
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(properties.getEndpoints())
                    .setRequestTimeout(Duration.ofMillis(properties.getRequestTimeoutMillis()))
                    .enableSsl(properties.isSslEnabled())
                    .build();
            producer = provider.newProducerBuilder()
                    .setClientConfiguration(configuration)
                    .build();
            log.info("RocketMQ producer initialized, endpoints: {}", properties.getEndpoints());
        } catch (ClientException e) {
            throw RocketMqErrorCode.PRODUCER_INIT_FAILED.toServiceException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (producer != null) {
            producer.close();
            log.info("RocketMQ producer closed");
        }
    }

    // -------------------------------------------------------------------------
    // 同步发送
    // -------------------------------------------------------------------------

    @Override
    public <T extends BaseEvent> void publish(T event) {
        publish(deriveTopic(event), null, event);
    }

    @Override
    public <T extends BaseEvent> void publish(String topic, T event) {
        publish(topic, null, event);
    }

    @Override
    public <T extends BaseEvent> void publish(String topic, String tag, T event) {
        if (!isReady()) {
            log.warn("RocketMQ is disabled, event dropped: {}", event.getClass().getSimpleName());
            return;
        }
        doSend(topic, event.getEventId(), buildMessage(topic, tag, null, null, event));
    }

    // -------------------------------------------------------------------------
    // 异步发送
    // -------------------------------------------------------------------------

    @Override
    public <T extends BaseEvent> CompletableFuture<Void> publishAsync(T event) {
        return publishAsync(deriveTopic(event), event);
    }

    @Override
    public <T extends BaseEvent> CompletableFuture<Void> publishAsync(String topic, T event) {
        if (!isReady()) {
            log.warn("RocketMQ is disabled, async event dropped: topic={}", topic);
            return CompletableFuture.completedFuture(null);
        }
        Message message = buildMessage(topic, null, null, null, event);
        return producer.sendAsync(message)
                .thenAccept(receipt -> log.info(
                        "Domain event async published, topic: {}, eventId: {}, messageId: {}",
                        topic, event.getEventId(), receipt.getMessageId()))
                .exceptionally(ex -> {
                    log.error("Failed to async publish domain event, topic: {}, eventId: {}",
                            topic, event.getEventId(), ex);
                    throw RocketMqErrorCode.PUBLISH_FAILED.toServiceException(ex);
                });
    }

    // -------------------------------------------------------------------------
    // 延迟消息
    // -------------------------------------------------------------------------

    @Override
    public <T extends BaseEvent> void publishDelayed(T event, Duration delay) {
        publishDelayed(deriveTopic(event), event, delay);
    }

    @Override
    public <T extends BaseEvent> void publishDelayed(String topic, T event, Duration delay) {
        if (!isReady()) {
            log.warn("RocketMQ is disabled, delayed event dropped: {}", event.getClass().getSimpleName());
            return;
        }
        long deliveryTimestamp = System.currentTimeMillis() + delay.toMillis();
        doSend(topic, event.getEventId(), buildMessage(topic, null, deliveryTimestamp, null, event));
    }

    // -------------------------------------------------------------------------
    // 顺序消息（FIFO）
    // -------------------------------------------------------------------------

    @Override
    public <T extends BaseEvent> void publishOrdered(T event, String messageGroup) {
        publishOrdered(deriveTopic(event), event, messageGroup);
    }

    @Override
    public <T extends BaseEvent> void publishOrdered(String topic, T event, String messageGroup) {
        if (!isReady()) {
            log.warn("RocketMQ is disabled, ordered event dropped: {}", event.getClass().getSimpleName());
            return;
        }
        doSend(topic, event.getEventId(), buildMessage(topic, null, null, messageGroup, event));
    }

    // -------------------------------------------------------------------------
    // 内部工具方法
    // -------------------------------------------------------------------------

    private <T extends BaseEvent> String deriveTopic(T event) {
        return properties.getTopicPrefix() + event.getClass().getSimpleName();
    }

    private boolean isReady() {
        return producer != null;
    }

    /**
     * 构建 RocketMQ 消息，支持 Tag、延迟时间戳和消息分组（顺序消息）。
     */
    private <T extends BaseEvent> Message buildMessage(String topic, String tag,
                                                       Long deliveryTimestamp, String messageGroup, T event) {
        byte[] body = JSON.toJSONString(event).getBytes(StandardCharsets.UTF_8);
        var builder = provider.newMessageBuilder()
                .setTopic(topic)
                .setBody(body)
                .setKeys(event.getEventId());

        if (tag != null && !tag.isBlank()) {
            builder.setTag(tag);
        }
        if (deliveryTimestamp != null) {
            builder.setDeliveryTimestamp(deliveryTimestamp);
        }
        if (messageGroup != null && !messageGroup.isBlank()) {
            builder.setMessageGroup(messageGroup);
        }
        return builder.build();
    }

    /**
     * 带重试的同步发送。
     */
    private void doSend(String topic, String eventId, Message message) {
        if (!isReady()) {
            return;
        }
        int attempts = 0;
        ClientException lastException = null;
        while (attempts <= properties.getMaxAttempts()) {
            try {
                var receipt = producer.send(message);
                log.info("Domain event published, topic: {}, eventId: {}, messageId: {}",
                        topic, eventId, receipt.getMessageId());
                return;
            } catch (ClientException e) {
                lastException = e;
                attempts++;
                log.warn("RocketMQ send attempt {}/{} failed, topic: {}, eventId: {}",
                        attempts, properties.getMaxAttempts() + 1, topic, eventId, e);
            }
        }
        log.error("All {} send attempts failed, topic: {}, eventId: {}",
                properties.getMaxAttempts() + 1, topic, eventId, lastException);
        throw RocketMqErrorCode.PUBLISH_FAILED.toServiceException(lastException);
    }
}
