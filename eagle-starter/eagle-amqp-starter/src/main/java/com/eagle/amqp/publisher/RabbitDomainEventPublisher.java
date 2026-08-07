package com.eagle.amqp.publisher;

import com.eagle.amqp.exception.AmqpErrorCode;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.support.ExchangeNaming;
import com.eagle.common.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link DomainEventPublisher} 的 RabbitMQ 实现。
 *
 * <p>消息以 JSON 投递到 topic exchange，{@code eventId} 写入 message id 与
 * {@code correlationId}，供消费侧做幂等（沿用原实现"用 BaseEvent.eventId 而非 MQ 自身 msgId"
 * 的约定 —— msgId 在重投递时会变）。
 *
 * <p><b>序列化不走 {@code RabbitTemplate} 的 {@code MessageConverter}</b>，而是用注入的
 * {@link ObjectMapper} 直接序列化成 {@link Message}。原因：Boot 自动配置的 {@code RabbitTemplate}
 * 未挂 converter 时默认走 {@code SimpleMessageConverter}（Java 序列化，不是 JSON）；
 * 而 Spring AMQP 4.x 面向 Jackson 3 的 {@code JacksonJsonMessageConverter} 构造器要求
 * {@code tools.jackson.databind.json.JsonMapper}（{@code ObjectMapper} 的子类），
 * 与消费侧 {@code AmqpMessageDispatcher} 已经在用的通用 {@code ObjectMapper} bean 类型不对等。
 * 两端都用同一个注入的 {@code ObjectMapper} 直接读写字节，行为对称且不依赖隐式类型转换。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final AmqpProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 进程内已声明过的 exchange，避免每次发送都跑一次 declare。
     * 对应原实现的 {@code RocketMqTopicAdmin.ensureTopic} 去重逻辑。
     */
    private final Set<String> declaredExchanges = ConcurrentHashMap.newKeySet();

    @Override
    public <T extends BaseEvent> void publish(String topic, T event) {
        publish(topic, ExchangeNaming.MATCH_ALL_ROUTING_KEY, event);
    }

    @Override
    public <T extends BaseEvent> void publish(String topic, String routingKey, T event) {
        String exchange = ExchangeNaming.exchange(properties.getExchangePrefix(), topic);
        ensureExchange(exchange);

        String key = (routingKey == null || routingKey.isBlank())
                ? ExchangeNaming.MATCH_ALL_ROUTING_KEY
                : routingKey;

        try {
            byte[] body = objectMapper.writeValueAsBytes(event);
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            messageProperties.setContentEncoding("UTF-8");
            messageProperties.setMessageId(event.getEventId());
            messageProperties.setCorrelationId(event.getEventId());
            Message message = MessageBuilder.withBody(body).andProperties(messageProperties).build();

            // 带上 CorrelationData：broker 的 confirm/nack 是异步回调，
            // 没有它就只知道"有消息没进 broker"，不知道是哪一条（见 PublishConfirmLogger）。
            rabbitTemplate.send(exchange, key, message, new CorrelationData(event.getEventId()));
            log.info("[AMQP] published: exchange={}, routingKey={}, eventId={}",
                    exchange, key, event.getEventId());
        } catch (JacksonException e) {
            log.error("[AMQP] serialize failed: exchange={}, routingKey={}, eventId={}",
                    exchange, key, event.getEventId(), e);
            throw AmqpErrorCode.PUBLISH_FAILED.toServiceException(e);
        } catch (AmqpException e) {
            log.error("[AMQP] publish failed: exchange={}, routingKey={}, eventId={}",
                    exchange, key, event.getEventId(), e);
            throw AmqpErrorCode.PUBLISH_FAILED.toServiceException(e);
        }
    }

    /**
     * 幂等声明 exchange。
     *
     * <p>只声明 exchange 不声明 queue —— queue 由消费方声明并绑定。
     * 这样生产方不需要知道有哪些消费者，符合 pub/sub 的解耦方向。
     *
     * @param exchange exchange 名
     */
    private void ensureExchange(String exchange) {
        if (declaredExchanges.contains(exchange)) {
            return;
        }
        rabbitAdmin.declareExchange(
                ExchangeBuilder.topicExchange(exchange).durable(true).build());
        declaredExchanges.add(exchange);
        log.debug("[AMQP] exchange declared: {}", exchange);
    }
}
