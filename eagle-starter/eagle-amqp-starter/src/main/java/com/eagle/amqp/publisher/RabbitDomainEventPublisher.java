package com.eagle.amqp.publisher;

import com.eagle.amqp.exception.AmqpErrorCode;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.support.ExchangeNaming;
import com.eagle.common.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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
 * {@link ObjectMapper} 直接序列化成 {@link Message}。
 *
 * <p>理由<b>不是</b>「装不上 converter」—— {@code JacksonJsonMessageConverter} 有无参构造器，
 * 挂上去很容易。真正的原因是<b>类型头</b>：该 converter 默认把载荷的全限定类名写进
 * {@code __TypeId__} header，消费侧据此决定反序列化目标类。而本项目的集成事件契约规定
 * <b>生产方与每个消费方各自声明自己的类</b>（生产方 {@code XxxIntegrationEvent}、
 * 消费方 {@code XxxMessage}，靠 JSON 字段名兼容，见 rules/02-architecture.md）——
 * {@code __TypeId__} 会指向一个消费方 classpath 上<b>根本不存在</b>的类，直接反序列化失败。
 * 要用 converter 就得再配 {@code TypePrecedence.INFERRED} 或自定义 {@code ClassMapper}，
 * 比两端各一行 {@code writeValueAsBytes} / {@code readValue} 更绕且更易配错。
 *
 * <p>两端都用同一个注入的 {@code ObjectMapper} 直接读写字节，行为对称，
 * 且与服务自身的 Jackson 定制（日期格式等）保持一致。
 *
 * @author eagle
 */
@Slf4j
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final AmqpProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 进程内已声明过的 exchange，避免每次发送都跑一次 declare。
     */
    private final Set<String> declaredExchanges = ConcurrentHashMap.newKeySet();

    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin,
                                      ConnectionFactory connectionFactory, AmqpProperties properties,
                                      ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
        this.properties = properties;
        this.objectMapper = objectMapper;
        // 连接重建 = broker 可能已重启并丢了元数据，缓存必须作废重新声明。
        // RabbitAdmin 对自己管理的 Declarable bean 本来就会在 onCreate 时重声明，
        // 这里是让本类这份「按需声明」的缓存跟上同一套语义 ——
        // 少了这一步，broker 重建后本进程会一直认为「已声明过」，
        // 之后每次 publish 都撞 404 NOT_FOUND，直到重启服务才恢复。
        connectionFactory.addConnectionListener(connection -> declaredExchanges.clear());
    }

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
