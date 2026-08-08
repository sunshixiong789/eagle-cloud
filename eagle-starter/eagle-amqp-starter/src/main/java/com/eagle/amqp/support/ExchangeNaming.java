package com.eagle.amqp.support;

/**
 * AMQP 拓扑命名规则，生产侧与消费侧共用，保证两边拼出同一个名字。
 *
 * <p>命名约定：
 * <pre>
 * exchange   {prefix}{topic}                  TopicExchange, durable
 * queue      {prefix}{topic}.{consumerGroup}  durable
 * DLX        {prefix}{topic}.dlx
 * DLQ        {prefix}{topic}.{consumerGroup}.dlq
 * </pre>
 *
 * <p>queue 名带 consumerGroup 后缀是关键设计：同一 exchange 上的每个消费者各自一个 queue，
 * 各收全量消息。这修复了迁移前"多个消费者共用默认 consumer-group 导致竞争消费、
 * 一条消息只被其中一个处理"的线上缺陷。
 *
 * @author eagle
 */
public final class ExchangeNaming {

    /**
     * 默认 routing key。
     *
     * <p><b>注意语义差异</b>：RocketMQ 用 {@code "*"} 表示"全部 tag"，
     * 而 AMQP 的 {@code *} 只匹配<b>恰好一个</b>单词、{@code #} 才是"零或多个单词"。
     * 照搬 {@code "*"} 会导致订阅静默收不到消息。
     */
    public static final String MATCH_ALL_ROUTING_KEY = "#";

    /**
     * DLX 名后缀。公开是因为 {@code EagleRepublishRecoverer} 要把它拼进 SpEL 表达式
     * （DLX 名在那里是逐条消息求值的，拿不到 {@link #deadLetterExchange(String)} 的返回值）。
     */
    public static final String DLX_SUFFIX = ".dlx";

    private static final String DLQ_SUFFIX = ".dlq";

    private ExchangeNaming() {
    }

    /**
     * 拼出 exchange 名。
     *
     * @param prefix 环境前缀，可为 null / 空
     * @param topic  逻辑 topic 名
     * @return exchange 名
     */
    public static String exchange(String prefix, String topic) {
        return (prefix == null ? "" : prefix) + topic;
    }

    /**
     * 拼出 queue 名。
     *
     * @param exchange      exchange 名（已含前缀）
     * @param consumerGroup 消费者分组
     * @return queue 名
     */
    public static String queue(String exchange, String consumerGroup) {
        return exchange + "." + consumerGroup;
    }

    /**
     * 拼出死信 exchange 名。
     *
     * @param exchange 主 exchange 名
     * @return DLX 名
     */
    public static String deadLetterExchange(String exchange) {
        return exchange + DLX_SUFFIX;
    }

    /**
     * 拼出死信 queue 名。
     *
     * @param queue 主 queue 名
     * @return DLQ 名
     */
    public static String deadLetterQueue(String queue) {
        return queue + DLQ_SUFFIX;
    }
}
