package com.eagle.amqp.listener;

import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.support.ExchangeNaming;
import com.eagle.common.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.core.Message;

/**
 * 死信队列消费者基类。
 *
 * <p>与原 RocketMQ 版本的关键差异：
 *
 * <ol>
 *   <li><b>不再靠 {@code getOriginalConsumerGroup()} 拼 {@code %DLQ%group}</b>。
 *       RocketMQ 的 DLQ topic 名是 Broker 侧固定格式，而 AMQP 用显式 DLX ——
 *       子类改为声明它守护的<b>主 topic + 主消费组</b>，DLQ 名由命名规则推出。
 *       这同时修掉了迁移前"13+ 个 DLQ listener 写死的 group 名与真实 group 对不上、
 *       DLQ 队列永远收不到消息"的缺陷。</li>
 *   <li><b>{@code totalAttempts} 不再恒为 0</b>。原实现在桥接时硬编码传 0；
 *       现在从重试耗尽时写入的 header 读真实次数。</li>
 * </ol>
 *
 * @param <T> 消息载荷类型
 * @author eagle
 */
@Slf4j
public abstract class AbstractDlqListener<T extends BaseEvent> extends AbstractAmqpListener<T> {

    /**
     * 重试耗尽时由 {@code DeadLetterPublishingRecoverer} 写入的尝试次数 header。
     */
    public static final String ATTEMPTS_HEADER = "x-eagle-attempts";

    protected AbstractDlqListener(AmqpProperties amqpProperties) {
        super(amqpProperties);
    }

    // ---------------------------------------------------------------------
    // 子类实现
    // ---------------------------------------------------------------------

    /**
     * 被守护的主消费者的 topic。
     *
     * @return 主 topic 名
     */
    protected abstract String getOriginalTopic();

    /**
     * 被守护的主消费者的分组名。
     *
     * <p><b>必须与主消费者 {@code getConsumerGroup()} 的返回值完全一致</b>，
     * 否则会守着一个空队列 —— 这正是迁移前的缺陷成因。
     *
     * @return 主消费组名
     */
    protected abstract String getOriginalConsumerGroup();

    /**
     * 死信处理逻辑。
     *
     * @param event         消息载荷
     * @param totalAttempts 进入 DLQ 前的总尝试次数
     */
    protected abstract void handleDeadLetter(T event, int totalAttempts);

    // ---------------------------------------------------------------------
    // 拓扑推导：DLQ 名由主 queue 名 + .dlq 得出，子类不可覆盖
    // ---------------------------------------------------------------------

    @Override
    protected final String getTopic() {
        return getOriginalTopic();
    }

    @Override
    protected final String getConsumerGroup() {
        return getOriginalConsumerGroup();
    }

    /**
     * DLQ 队列名 = 主 queue 名 + {@code .dlq}。
     *
     * @return DLQ 队列名
     */
    public final String resolveDlqName() {
        return ExchangeNaming.deadLetterQueue(
                ExchangeNaming.queue(resolveExchangeName(), getOriginalConsumerGroup()));
    }

    @Override
    protected final void handle(T event) {
        handleDeadLetter(event, 0);
    }

    /**
     * 由监听容器调用，带上从 header 解析出的真实尝试次数。
     *
     * @param event   消息载荷
     * @param message 原始 AMQP 消息，用于读取尝试次数 header
     */
    public final void dispatchDeadLetter(T event, Message message) {
        handleDeadLetter(event, resolveAttempts(message));
    }

    /**
     * 从消息 header 解析总尝试次数。
     *
     * <p>优先读本 starter 写入的 {@link #ATTEMPTS_HEADER}；
     * 若消息是经 broker 原生 DLX 转来的，回落到 AMQP 标准的 {@code x-death} 计数。
     *
     * @param message 原始消息
     * @return 尝试次数，解析不出时返回 0
     */
    private int resolveAttempts(Message message) {
        Object attempts = message.getMessageProperties().getHeader(ATTEMPTS_HEADER);
        if (attempts instanceof Number number) {
            return number.intValue();
        }
        Object death = message.getMessageProperties().getHeader("x-death");
        if (death instanceof java.util.List<?> list && !list.isEmpty()
                && list.getFirst() instanceof java.util.Map<?, ?> first
                && first.get("count") instanceof Number count) {
            return count.intValue();
        }
        return 0;
    }
}
