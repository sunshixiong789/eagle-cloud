package com.eagle.rocketmq.publisher;

import com.alibaba.fastjson2.JSON;
import com.eagle.common.event.BaseEvent;
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

/**
 * RocketMQ 领域事件发布器实现。
 *
 * <p>基于 RocketMQ 5.x 轻量客户端（grpc-based）。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class RocketMqDomainEventPublisher implements DomainEventPublisher, InitializingBean, DisposableBean {

    private final RocketMqProperties properties;
    private Producer producer;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!properties.isEnabled()) {
            return;
        }
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
                .setEndpoints(properties.getEndpoints())
                .build();
        producer = provider.newProducerBuilder()
                .setTopics(properties.getTopicPrefix() + "*")
                .setClientConfiguration(configuration)
                .build();
        log.info("RocketMQ producer initialized, endpoints: {}", properties.getEndpoints());
    }

    @Override
    public void destroy() throws Exception {
        if (producer != null) {
            producer.close();
            log.info("RocketMQ producer closed");
        }
    }

    @Override
    public <T extends BaseEvent> void publish(T event) {
        String topic = properties.getTopicPrefix() + event.getClass().getSimpleName();
        publish(topic, event);
    }

    @Override
    public <T extends BaseEvent> void publish(String topic, T event) {
        if (!properties.isEnabled() || producer == null) {
            log.warn("RocketMQ is disabled, event dropped: {}", event.getClass().getSimpleName());
            return;
        }
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setBody(JSON.toJSONString(event).getBytes(StandardCharsets.UTF_8))
                    .setKeys(event.getEventId())
                    .build();
            producer.send(message);
            log.info("Domain event published to RocketMQ, topic: {}, eventId: {}",
                    topic, event.getEventId());
        } catch (ClientException e) {
            log.error("Failed to publish domain event to RocketMQ, topic: {}", topic, e);
            throw new RuntimeException("RocketMQ publish failed", e);
        }
    }
}
