package com.eagle.amqp.support;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.common.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 消息分发器：反序列化 → 业务处理 → 退避重试 → 投递 DLQ。
 *
 * <p>这一层复刻的是 RocketMQ 由 Broker 提供、而 RabbitMQ 完全没有的能力。
 * RabbitMQ 的 {@code basic.nack(requeue=true)} 会让消息立即重回队头形成死循环，
 * 既没有递增退避也没有自动 DLQ 搬运，因此必须在客户端实现：
 *
 * <ol>
 *   <li><b>反序列化失败</b> → 不重试（重试也不会变好），直接投 DLQ 并 ACK。
 *       比原实现"记日志后 ACK 丢弃"更安全 —— 消息在 DLQ 中留存待查。</li>
 *   <li><b>业务异常</b> → 就地退避重试，间隔按
 *       {@code initialBackoff × multiplier^n} 递增并封顶于 {@code maxBackoff}。
 *       达到 {@code retryAlertThreshold} 时触发告警回调。</li>
 *   <li><b>重试耗尽</b> → 带上真实尝试次数 header 投递到 DLX，然后 ACK 主队列。</li>
 * </ol>
 *
 * <p>退避期间会占用消费者线程。本项目三个服务均已开启虚拟线程
 * （{@code spring.threads.virtual.enabled=true}），阻塞成本很低；
 * 且默认只重试 4 次、封顶 30s，不会长时间占用。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class AmqpMessageDispatcher {

    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpProperties properties;

    /**
     * 处理一条消息。
     *
     * @param listener 目标消费者
     * @param message  原始 AMQP 消息
     * @param <T>      载荷类型
     */
    public <T extends BaseEvent> void dispatch(AbstractAmqpListener<T> listener, Message message) {
        String rawBody = new String(message.getBody(), StandardCharsets.UTF_8);

        T event;
        try {
            event = objectMapper.readValue(rawBody, listener.resolveEventClass());
        } catch (Exception e) {
            listener.notifyDeserializationFailed(message, rawBody, e);
            sendToDeadLetter(listener, message, 0);
            return;
        }

        if (listener instanceof AbstractDlqListener<T> dlqListener) {
            // DLQ 本身不再重试，处理失败只记日志，避免死信在 DLQ 里无限循环
            try {
                dlqListener.dispatchDeadLetter(event, message);
            } catch (Exception e) {
                log.error("[AMQP] dead letter handling failed: queue={}, eventId={}",
                        dlqListener.resolveDlqName(), event.getEventId(), e);
            }
            return;
        }

        handleWithRetry(listener, message, rawBody, event);
    }

    /**
     * 就地退避重试，耗尽后投 DLQ。
     *
     * @param listener 消费者
     * @param message  原始消息
     * @param rawBody  原始报文
     * @param event    载荷
     * @param <T>      载荷类型
     */
    private <T extends BaseEvent> void handleWithRetry(AbstractAmqpListener<T> listener,
                                                       Message message, String rawBody, T event) {
        AmqpProperties.Consumer cfg = properties.getConsumer();
        long backoffMillis = cfg.getInitialBackoff().toMillis();

        for (int attempt = 1; attempt <= cfg.getMaxAttempts(); attempt++) {
            try {
                listener.dispatch(event);
                return;
            } catch (Exception e) {
                if (attempt >= listener.resolveRetryAlertThreshold()) {
                    listener.notifyRetryAlert(message, rawBody, event, e, attempt);
                }
                if (attempt == cfg.getMaxAttempts()) {
                    log.error("[AMQP] retries exhausted, routing to DLQ: queue={}, attempts={}, eventId={}",
                            listener.resolveQueueName(), attempt, event.getEventId(), e);
                    sendToDeadLetter(listener, message, attempt);
                    return;
                }
                sleepQuietly(backoffMillis);
                backoffMillis = Math.min(
                        (long) (backoffMillis * cfg.getMultiplier()), cfg.getMaxBackoff().toMillis());
            }
        }
    }

    /**
     * 把消息投递到死信 exchange，并写入真实尝试次数。
     *
     * @param listener 消费者
     * @param message  原始消息
     * @param attempts 已尝试次数
     */
    private void sendToDeadLetter(AbstractAmqpListener<?> listener, Message message, int attempts) {
        String queueName = listener.resolveQueueName();
        String dlx = ExchangeNaming.deadLetterExchange(listener.resolveExchangeName());
        Message enriched = MessageBuilder.fromMessage(message)
                .setHeader(AbstractDlqListener.ATTEMPTS_HEADER, attempts)
                .build();
        rabbitTemplate.send(dlx, queueName, enriched);
    }

    /**
     * 退避等待。中断时恢复中断标志并立即返回，让本轮消息走到 DLQ 而非静默吞掉。
     *
     * @param millis 等待毫秒数
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
