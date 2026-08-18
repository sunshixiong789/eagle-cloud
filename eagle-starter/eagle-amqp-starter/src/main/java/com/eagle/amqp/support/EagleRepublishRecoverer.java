package com.eagle.amqp.support;

import com.eagle.amqp.listener.AbstractDlqListener;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecovererWithConfirms;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.Duration;
import java.util.Map;

/**
 * 重试耗尽后把消息投到该消息所属队列对应的 DLX。
 *
 * <p><b>一个 bean 服务全部 listener</b>：DLX 与 routing key 由父类按 SpEL 表达式
 * <b>逐条消息</b>求值得出，而不是每个 listener 各建一个 recoverer。这依赖本 starter 的命名约定
 * （见 {@link ExchangeNaming}）：
 *
 * <pre>
 * DLX          = {消息发来的 exchange} + ".dlx"   ← messageProperties.receivedExchange
 * routing key  = {消息所在的 queue 名}            ← messageProperties.consumerQueue
 * </pre>
 *
 * <p>而 DLQ 正是以主 queue 名为 key 绑定到 DLX 的，两端严丝合缝。
 * 表达式求值的 root object 是 {@link Message} 本身，这是父类提供的构造器。
 *
 * <p>父类已负责搬运本身，并自动附上 {@code x-exception-message} /
 * {@code x-exception-stacktrace} / {@code x-original-exchange} / {@code x-original-routingKey}。
 * 本子类只补 {@link AbstractDlqListener#ATTEMPTS_HEADER}：业务失败为
 * {@code max-retries + 1}，坏报文跳过重试时记 1。
 *
 * <p>投递走框架的 {@link RepublishMessageRecovererWithConfirms}，等 broker confirm，
 * 避免「recoverer 以为发出去了、原消息已 ack、DLX 其实没收下」。
 *
 * @author eagle
 */
public class EagleRepublishRecoverer extends RepublishMessageRecovererWithConfirms {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    /**
     * {@code {receivedExchange}.dlx}，与 {@link ExchangeNaming#deadLetterExchange(String)} 同构。
     */
    private static final String DLX_EXPRESSION =
            "messageProperties.receivedExchange + '" + ExchangeNaming.DLX_SUFFIX + "'";

    /**
     * DLQ 按主 queue 名绑定到 DLX，所以 routing key 就是消息所在的 queue 名。
     */
    private static final String ROUTING_KEY_EXPRESSION = "messageProperties.consumerQueue";

    private final int totalAttempts;
    private final ConfigurableApplicationContext applicationContext;

    /**
     * @param errorTemplate      投递用的 template
     * @param maxRetries         配置的最大重试次数（不含首次投递）
     * @param applicationContext 用于识别停机，避免把正在退避的消息推进 DLQ
     */
    public EagleRepublishRecoverer(RabbitTemplate errorTemplate, long maxRetries,
                                   ConfigurableApplicationContext applicationContext) {
        super(errorTemplate,
                PARSER.parseExpression(DLX_EXPRESSION),
                PARSER.parseExpression(ROUTING_KEY_EXPRESSION),
                ConfirmType.CORRELATED);
        this.totalAttempts = (int) maxRetries + 1;
        this.applicationContext = applicationContext;
    }

    /**
     * 由自动配置在构造完成后设置，避免构造器里调 overlay 方法触发 this-escape。
     */
    public void applyConfirmTimeout(Duration confirmTimeout) {
        setConfirmTimeout(confirmTimeout.toMillis());
    }

    @Override
    public void recover(Message message, Throwable cause) {
        if (shouldRequeueInsteadOfDlq(cause)) {
            throw new ImmediateRequeueAmqpException(
                    "requeue instead of DLQ: shutting down or forced requeue", cause);
        }
        super.recover(message, cause);
    }

    private boolean shouldRequeueInsteadOfDlq(Throwable cause) {
        if (applicationContext != null && !applicationContext.isActive()) {
            return true;
        }
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof ImmediateRequeueAmqpException) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Map<String, Object> additionalHeaders(Message message, Throwable cause) {
        int attempts = EagleAmqpRetry.shouldRetry(cause) ? this.totalAttempts : 1;
        return Map.of(AbstractDlqListener.ATTEMPTS_HEADER, attempts);
    }
}
