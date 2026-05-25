package com.eagle.rocketmq.transaction;

import com.eagle.common.event.BaseEvent;

/**
 * 事务消息发布器接口（Outbox Pattern）。
 *
 * <p>RocketMQ 事务消息保证本地事务与消息发布的原子性：
 * <ol>
 *   <li>向 Broker 发送半消息（消费者不可见）</li>
 *   <li>执行本地事务（如写入数据库）</li>
 *   <li>本地事务成功则提交消息，失败则回滚</li>
 *   <li>若 Broker 未收到确认，触发 {@link AbstractRocketMqTransactionChecker} 回查</li>
 * </ol>
 *
 * <p>典型用法（应用服务中）：
 * <pre>{@code
 * @Transactional
 * public void createOrder(CreateOrderRequest req) {
 *     Order order = Order.create(req.getOrderNo());
 *     // 发布事务消息：本地 DB 写入 + 消息发布原子
 *     transactionalEventPublisher.publishInTransaction(
 *         new OrderCreatedEvent(order.getId(), order.getOrderNo()),
 *         () -> {
 *             orderRepository.save(order);
 *             return true;
 *         }
 *     );
 * }
 * }</pre>
 *
 * @author eagle
 */
public interface TransactionalEventPublisher {

    /**
     * 以事务方式发布领域事件，Topic 由事件类名自动推导。
     *
     * @param event    领域事件
     * @param callback 本地事务回调
     * @param <T>      事件类型
     */
    <T extends BaseEvent> void publishInTransaction(T event, TransactionCallback callback);

    /**
     * 以事务方式发布领域事件到指定 Topic。
     *
     * @param topic    目标 Topic
     * @param event    领域事件
     * @param callback 本地事务回调
     * @param <T>      事件类型
     */
    <T extends BaseEvent> void publishInTransaction(String topic, T event, TransactionCallback callback);
}
