package com.eagle.rocketmq.listener;

import com.eagle.common.event.BaseEvent;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.message.MessageView;

/**
 * RocketMQ 死信队列（DLQ）消费者抽象基类。
 *
 * <p>当消息在 {@link AbstractRocketMqListener} 中超过 Broker 配置的最大重试次数后，
 * Broker 自动将消息投递到死信 Topic（格式：{@code %DLQ%{consumerGroup}}），
 * 由本类的子类进行兜底处理。
 *
 * <p>子类声明为 {@code @Component}，只需实现三个方法：
 * <ul>
 *   <li>{@link #getOriginalConsumerGroup()} — 原消费者组名</li>
 *   <li>{@link #getEventClass()} — 事件类型（与原消费者一致）</li>
 *   <li>{@link #handleDeadLetter(BaseEvent, int)} — 死信处理逻辑</li>
 * </ul>
 *
 * <h2>典型处理策略</h2>
 * <ol>
 *   <li><b>持久化 + 人工处理</b>：写入 {@code t_dead_letter} 表，运营平台展示并支持手动重试</li>
 *   <li><b>告警通知</b>：发送钉钉/邮件/Slack 告警，通知业务方介入</li>
 *   <li><b>自动补偿</b>：根据事件类型执行降级补偿逻辑（如取消订单、回滚库存）</li>
 * </ol>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * @Component
 * public class OrderCreatedDlqListener extends AbstractDlqListener<OrderCreatedEvent> {
 *
 *     private final DeadLetterRepository deadLetterRepository;
 *     private final AlertService alertService;
 *
 *     public OrderCreatedDlqListener(RocketMqProperties props,
 *                                    DeadLetterRepository deadLetterRepository,
 *                                    AlertService alertService) {
 *         super(props);
 *         this.deadLetterRepository = deadLetterRepository;
 *         this.alertService = alertService;
 *     }
 *
 *     @Override
 *     protected String getOriginalConsumerGroup() {
 *         return "inventory-consumer-group";  // 与原消费者组一致
 *     }
 *
 *     @Override
 *     protected Class<OrderCreatedEvent> getEventClass() {
 *         return OrderCreatedEvent.class;
 *     }
 *
 *     @Override
 *     protected void handleDeadLetter(OrderCreatedEvent event, int totalAttempts) {
 *         // 1. 持久化死信消息
 *         deadLetterRepository.save(DeadLetterRecord.of(event, totalAttempts));
 *         // 2. 告警
 *         alertService.sendAlert("订单创建事件消费失败，需人工处理: " + event.getOrderId());
 *     }
 * }
 * }</pre>
 *
 * @param <T> 事件类型，与原消费者一致
 * @author eagle
 */
@Slf4j
public abstract class AbstractDlqListener<T extends BaseEvent> extends AbstractRocketMqListener<T> {

    /**
     * 构造器透传 {@link RocketMqProperties} 给父类。子类构造器须调用 {@code super(rocketMqProperties)}。
     *
     * @param rocketMqProperties RocketMQ 全局配置
     */
    protected AbstractDlqListener(RocketMqProperties rocketMqProperties) {
        super(rocketMqProperties);
    }

    /**
     * 返回原消费者组名。
     *
     * <p>DLQ Topic 由此推导：{@code %DLQ%{originalConsumerGroup}}。
     *
     * @return 原消费者组名
     */
    protected abstract String getOriginalConsumerGroup();

    /**
     * 处理死信消息。
     *
     * <p>消息到达此处意味着已经过多次重试全部失败，业务代码中应将其持久化，
     * 防止消息彻底丢失，并触发人工介入或自动补偿流程。
     *
     * @param event         已反序列化的领域事件
     * @param totalAttempts 本次投递已累计的总尝试次数
     */
    protected abstract void handleDeadLetter(T event, int totalAttempts);

    // -------------------------------------------------------------------------
    // 固定实现：路由到 DLQ Topic
    // -------------------------------------------------------------------------

    /**
     * DLQ Topic 格式：{@code %DLQ%{originalConsumerGroup}}，由框架自动推导，禁止覆盖。
     */
    @Override
    public final String getTopic() {
        return "%DLQ%" + getOriginalConsumerGroup();
    }

    /**
     * DLQ 消费者组默认为 {@code dlq-{originalConsumerGroup}}，禁止覆盖。
     *
     * <p>使用独立消费组确保 DLQ 消息不与正常消息竞争消费。
     */
    @Override
    public final String getConsumerGroup() {
        return "dlq-" + getOriginalConsumerGroup();
    }

    /**
     * 将 {@link AbstractRocketMqListener#handle} 桥接到 {@link #handleDeadLetter}，禁止覆盖。
     */
    @Override
    protected final void handle(T event) {
        // deliveryAttempt 在 DLQ 中通常从 1 重新计数，此处仅作参考传递
        handleDeadLetter(event, 0);
    }

    /**
     * 覆盖告警日志：DLQ 消息本身就是最终失败，输出 CRITICAL 级别。
     */
    @Override
    protected void onRetryAlert(MessageView messageView, String rawBody, T event, Exception cause) {
        log.error("[CRITICAL] Dead letter message also failing in DLQ consumer! " +
                        "Manual intervention required immediately. " +
                        "dlqTopic: {}, messageId: {}, eventId: {}",
                messageView.getTopic(),
                messageView.getMessageId(),
                event != null ? event.getEventId() : "N/A",
                cause);
    }
}
