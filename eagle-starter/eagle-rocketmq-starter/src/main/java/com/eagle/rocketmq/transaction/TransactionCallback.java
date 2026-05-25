package com.eagle.rocketmq.transaction;

/**
 * 本地事务回调。
 *
 * <p>在 RocketMQ 事务消息中，本地事务与半消息发送之间的业务执行体。
 * 返回 {@code true} 则提交消息（消费者可见），{@code false} 则回滚（消息不可见）。
 *
 * <pre>{@code
 * transactionalEventPublisher.publishInTransaction(event, () -> {
 *     // 本地数据库写操作
 *     orderRepository.save(order);
 *     return true;  // 成功则提交消息
 * });
 * }</pre>
 *
 * @author eagle
 */
@FunctionalInterface
public interface TransactionCallback {

    /**
     * 执行本地事务。
     *
     * @return {@code true} 提交消息；{@code false} 回滚消息
     */
    boolean execute();
}
