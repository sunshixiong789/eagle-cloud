package com.eagle.amqp.support;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.common.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 消息分发器：反序列化 → 交给业务 {@code handle()}。
 *
 * <p><b>重试与 DLQ 投递不在这里</b> —— 由容器上的 retry advice 完成，
 * 见 {@code AmqpListenerRegistrar#buildRetryInterceptor}。本类只做两件事：
 *
 * <ol>
 *   <li><b>反序列化</b>：失败时抛 {@link AmqpRejectAndDontRequeueException}，
 *       重试策略据此跳过重试，直接进 recoverer 投 DLQ 留证
 *       （报文本身有问题，重试多少次都一样）。</li>
 *   <li><b>DLQ 分支</b>：死信交给 {@code AbstractDlqListener}，失败只记日志 ——
 *       死信再投递会在 DLQ 里打转。</li>
 * </ol>
 *
 * <p>业务异常一律直接上抛，交给重试策略。
 *
 * <p>迁移期这里曾手写 {@code handleWithRetry()} 退避循环与 {@code sendToDeadLetter()}
 * 死信搬运，理由是"RabbitMQ 没有 Broker 侧重试"。实际上 Spring AMQP 的
 * {@code RetryInterceptorBuilder} + {@code RepublishMessageRecoverer} 早已提供，
 * 且在 spring-rabbit 4.1 已改用 Spring Framework 7 内置的 {@code core.retry}，
 * 连额外依赖都不需要。两段手写实现已删除。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class AmqpMessageDispatcher {

    private final ObjectMapper objectMapper;

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
            // 重试改变不了报文本身，抛这个异常让重试策略跳过它，直接进 recoverer → DLQ 留证
            throw new AmqpRejectAndDontRequeueException("message deserialization failed", e);
        }

        if (listener instanceof AbstractDlqListener<T> dlqListener) {
            // DLQ 本身不重试也不再投递，处理失败只记日志，避免死信在 DLQ 里打转
            try {
                dlqListener.dispatchDeadLetter(event, message);
            } catch (Exception e) {
                log.error("[AMQP] dead letter handling failed: queue={}, eventId={}",
                        dlqListener.resolveDlqName(), event.getEventId(), e);
            }
            return;
        }

        // 业务异常直接上抛：重试与 DLQ 投递由容器的 retry advice 接管
        // （AmqpListenerRegistrar#buildRetryInterceptor）
        listener.dispatch(event);
    }

}
