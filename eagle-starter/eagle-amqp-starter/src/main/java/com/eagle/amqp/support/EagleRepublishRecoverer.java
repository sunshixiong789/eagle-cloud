package com.eagle.amqp.support;

import com.eagle.amqp.listener.AbstractDlqListener;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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
 * 表达式求值的 root object 是 {@link Message} 本身，这是
 * {@link RepublishMessageRecoverer} 提供的构造器，不是自己写的分发逻辑。
 *
 * <p>父类已负责搬运本身，并自动附上 {@code x-exception-message} /
 * {@code x-exception-stacktrace} / {@code x-original-exchange} / {@code x-original-routingKey}。
 * 本子类只补一件父类不知道的事：{@link AbstractDlqListener#ATTEMPTS_HEADER}。
 * recoverer 只在<b>重试耗尽</b>时被调用，所以此刻的总尝试次数恒等于
 * {@code spring.rabbitmq.listener.simple.retry.max-retries + 1}（+1 是首次投递）。
 *
 * @author eagle
 */
public class EagleRepublishRecoverer extends RepublishMessageRecoverer {

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

    /**
     * @param errorTemplate 投递用的 template
     * @param maxRetries    配置的最大重试次数（不含首次投递）
     */
    public EagleRepublishRecoverer(AmqpTemplate errorTemplate, long maxRetries) {
        super(errorTemplate,
                PARSER.parseExpression(DLX_EXPRESSION),
                PARSER.parseExpression(ROUTING_KEY_EXPRESSION));
        this.totalAttempts = (int) maxRetries + 1;
    }

    @Override
    protected Map<String, Object> additionalHeaders(Message message, Throwable cause) {
        return Map.of(AbstractDlqListener.ATTEMPTS_HEADER, this.totalAttempts);
    }
}
