package com.eagle.amqp.listener;

import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.support.ExchangeNaming;
import com.eagle.common.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.core.Message;

/**
 * AMQP 消息消费者基类，取代原 {@code AbstractRocketMqListener}。
 *
 * <p><b>三个抽象方法签名与原基类完全一致</b>，因此 43 个既有子类的
 * {@code handle()} 实现一行都不用改，只需换 import、父类名与构造器参数类型。
 *
 * <p>子类必须手写构造器显式调用 {@code super(properties)} ——
 * 本类只有带参构造器，Lombok {@code @RequiredArgsConstructor} 生成的构造器
 * 会隐式调 {@code super()}（不存在）导致编译失败。这条约束与原基类一致。
 *
 * <pre>{@code
 * @Component
 * public class OrderPlacedConsumer extends AbstractAmqpListener<OrderPlacedMessage> {
 *
 *     private static final String TOPIC_NAME = "trade_order_placed";
 *     private final OrderService orderService;
 *
 *     // 必须手写构造器，不能用 @RequiredArgsConstructor
 *     public OrderPlacedConsumer(AmqpProperties properties, OrderService orderService) {
 *         super(properties);
 *         this.orderService = orderService;
 *     }
 *
 *     @Override protected String getTopic() { return TOPIC_NAME; }
 *     @Override protected String getConsumerGroup() { return "user_membership_order_placed"; }
 *     @Override protected Class<OrderPlacedMessage> getEventClass() { return OrderPlacedMessage.class; }
 *     @Override protected void handle(OrderPlacedMessage event) { orderService.complete(event); }
 * }
 * }</pre>
 *
 * <p><b>子类可以被 AOP 代理</b>（{@code @Transactional} / {@code @RateLimit} / {@code @Async} …）。
 * 但这有个不显眼的前提：CGLIB 代理实例经 Objenesis 绕过构造器创建，
 * 它自己的 {@code amqpProperties} 恒为 null，而本类的 {@code resolveXxx()} 是 final、
 * CGLIB 覆盖不了，只能在这个"空壳"代理实例上执行。因此
 * <b>本类的 final 方法一律不得直接访问 {@code amqpProperties}</b>，
 * 必须经 {@link #getExchangePrefix()} 这类可覆盖方法取值（会被代理转发到真正的 target）。
 * {@code AmqpProxySafetyTest} 守着这条约束。
 *
 * <p><b>容器注册</b>不在本类内完成 —— exchange / queue 名是运行时由
 * {@link #getTopic()} 决定的，无法用 {@code @RabbitListener} 注解常量。
 * 由 {@code AmqpListenerRegistrar} 在启动期遍历所有本类的 bean，
 * 声明拓扑并为每个 listener 建一个监听容器。
 *
 * @param <T> 消息载荷类型
 * @author eagle
 */
@Slf4j
public abstract class AbstractAmqpListener<T extends BaseEvent> {

    protected final AmqpProperties amqpProperties;

    protected AbstractAmqpListener(AmqpProperties amqpProperties) {
        this.amqpProperties = amqpProperties;
    }

    // ---------------------------------------------------------------------
    // 子类必须实现（签名与原 AbstractRocketMqListener 一致）
    // ---------------------------------------------------------------------

    /**
     * 订阅的 topic 逻辑名，会被拼成 exchange 名（前缀由配置补齐）。
     *
     * @return topic 名
     */
    protected abstract String getTopic();

    /**
     * 消息载荷类型，用于 JSON 反序列化。
     *
     * @return 载荷 Class
     */
    protected abstract Class<T> getEventClass();

    /**
     * 业务处理。抛异常将触发退避重试，重试耗尽后进入 DLQ。
     *
     * @param event 反序列化后的消息载荷
     */
    protected abstract void handle(T event);

    // ---------------------------------------------------------------------
    // 可选覆盖
    // ---------------------------------------------------------------------

    /**
     * 消费者分组，决定 queue 名后缀。
     *
     * <p><b>同一 topic 上的每个消费者都必须覆盖此方法给出唯一值。</b>
     * 仍返回 {@code eagle_default}、或与另一个主消费者解析出同一 queue，
     * {@code AmqpListenerRegistrar} 会在启动期失败。
     *
     * @return 消费者分组名
     */
    protected String getConsumerGroup() {
        return amqpProperties.getConsumerGroup();
    }

    /**
     * 绑定用的 routing key，取代原基类的 {@code getTagExpression()}。
     *
     * <p>刻意改名而非沿用旧名：RocketMQ 的 {@code "*"} 表示"全部 tag"，
     * 而 AMQP 的 {@code *} 只匹配<b>恰好一个</b>单词 —— 照搬会静默收不到消息。
     * 改名强制覆盖了该方法的 6 个子类在编译期注意到语义变化。
     *
     * @return routing key 模式，默认 {@code #}（匹配全部）
     */
    protected String getRoutingKey() {
        return ExchangeNaming.MATCH_ALL_ROUTING_KEY;
    }

    /**
     * 反序列化失败时的回调，默认记 ERROR 日志。
     *
     * <p>反序列化失败不重试（重试也不会变好），消息直接进 DLQ ——
     * 比原实现"记日志后 ACK 丢弃"更安全，消息可在 DLQ 中留存待查。
     *
     * @param message 原始 AMQP 消息
     * @param rawBody 原始报文
     * @param cause   反序列化异常
     */
    protected void onDeserializationFailed(Message message, String rawBody, Exception cause) {
        log.error("[AMQP DESERIALIZE FAILED] queue={}, body={}", resolveQueueName(), rawBody, cause);
    }

    // ---------------------------------------------------------------------
    // 供 Registrar 使用的拓扑解析
    // ---------------------------------------------------------------------

    /**
     * exchange 名的环境前缀（{@code dev_} / {@code test_} / {@code prod_}）。
     *
     * <p><b>刻意做成可覆盖的实例方法，而不是在下面的 final 方法里直读 {@code amqpProperties}。</b>
     * 子类一旦因为任何 AOP 注解（{@code @Transactional} / {@code @RateLimit} /
     * {@code @Cacheable} / {@code @Async} …）被 Spring 包成 CGLIB 代理，
     * 代理实例是经 Objenesis <b>绕过构造器</b>创建的，它自己的 {@code amqpProperties} 恒为 null；
     * 而 {@code resolveXxx()} 是 final、CGLIB 覆盖不了，只能在这个字段为 null 的代理实例上执行。
     * 直读字段 → 启动期 {@code AmqpListenerRegistrar} 注册 listener 时 NPE，整个应用起不来。
     * 走这个可覆盖方法则会被代理转发到真正的 target 实例，取到正确的值。
     *
     * <p>⚠️ 因此：<b>本类中任何 final 方法都不得直接访问 {@code amqpProperties}</b>，
     * 一律经由可覆盖的实例方法（本方法、{@link #getConsumerGroup()}）取值。
     * {@code AmqpProxySafetyTest} 守着这条约束。
     *
     * @return exchange 名前缀
     */
    protected String getExchangePrefix() {
        return amqpProperties.getExchangePrefix();
    }

    /**
     * 解析最终 exchange 名（含环境前缀）。
     *
     * @return exchange 名
     */
    public final String resolveExchangeName() {
        return ExchangeNaming.exchange(getExchangePrefix(), getTopic());
    }

    /**
     * 解析最终 queue 名。
     *
     * @return queue 名
     */
    public final String resolveQueueName() {
        return ExchangeNaming.queue(resolveExchangeName(), getConsumerGroup());
    }

    /**
     * 暴露 routing key 给 Registrar 建绑定。
     *
     * @return routing key
     */
    public final String resolveRoutingKey() {
        return getRoutingKey();
    }

    /**
     * 暴露载荷类型给消息转换器。
     *
     * @return 载荷 Class
     */
    public final Class<T> resolveEventClass() {
        return getEventClass();
    }

    /**
     * 由监听容器回调的入口，把已反序列化的载荷交给业务 {@link #handle}。
     *
     * @param event 消息载荷
     */
    public final void dispatch(T event) {
        handle(event);
    }

    /**
     * 供分发器触发反序列化失败回调。
     *
     * @param message 原始消息
     * @param rawBody 原始报文
     * @param cause   反序列化异常
     */
    public final void notifyDeserializationFailed(Message message, String rawBody, Exception cause) {
        onDeserializationFailed(message, rawBody, cause);
    }
}
