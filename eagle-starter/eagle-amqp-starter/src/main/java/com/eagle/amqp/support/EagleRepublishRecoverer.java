package com.eagle.amqp.support;

import com.eagle.amqp.listener.AbstractDlqListener;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

import java.util.Map;

/**
 * 重试耗尽后把消息投到 DLX，附带本 starter 约定的尝试次数 header。
 *
 * <p>父类 {@link RepublishMessageRecoverer} 已经负责了搬运本身，并自动附上
 * {@code x-exception-message} / {@code x-exception-stacktrace} /
 * {@code x-original-exchange} / {@code x-original-routingKey} —— 这些诊断信息
 * 比迁移期手写的 {@code sendToDeadLetter()} 完整得多，那份手写实现只带了一个尝试次数。
 *
 * <p>本子类只补一件父类不知道的事：{@link AbstractDlqListener#ATTEMPTS_HEADER}。
 * recoverer 只在<b>重试耗尽</b>时被调用，所以此刻的总尝试次数恒等于配置的
 * {@code max-attempts}，直接取配置值即为准确值。
 *
 * @author eagle
 */
public class EagleRepublishRecoverer extends RepublishMessageRecoverer {

    private final int maxAttempts;

    /**
     * @param errorTemplate   投递用的 template
     * @param deadLetterExchange DLX 名
     * @param routingKey      投到 DLX 时用的 routing key，约定为主 queue 名
     *                        （DLQ 正是按这个 key 绑定到 DLX 的）
     * @param maxAttempts     配置的最大尝试次数
     */
    public EagleRepublishRecoverer(AmqpTemplate errorTemplate, String deadLetterExchange,
                                   String routingKey, int maxAttempts) {
        super(errorTemplate, deadLetterExchange, routingKey);
        this.maxAttempts = maxAttempts;
    }

    @Override
    protected Map<String, Object> additionalHeaders(Message message, Throwable cause) {
        return Map.of(AbstractDlqListener.ATTEMPTS_HEADER, this.maxAttempts);
    }
}
