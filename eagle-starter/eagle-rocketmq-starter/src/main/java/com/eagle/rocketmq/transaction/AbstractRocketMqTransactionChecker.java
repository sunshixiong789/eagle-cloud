package com.eagle.rocketmq.transaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.TransactionResolution;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;

/**
 * RocketMQ 事务消息回查基类。
 *
 * <p>当 Broker 未在超时时间内收到本地事务的提交/回滚确认时，触发此回查。
 * 子类需实现 {@link #isTransactionCommitted(MessageView)} 来判断本地事务是否已提交。
 *
 * <p>典型实现：查询事务日志表（outbox table）或业务状态字段。
 *
 * <pre>{@code
 * @Component
 * public class OrderTransactionChecker extends AbstractRocketMqTransactionChecker {
 *
 *     private final OrderRepository orderRepository;
 *
 *     public OrderTransactionChecker(OrderRepository orderRepository) {
 *         this.orderRepository = orderRepository;
 *     }
 *
 *     @Override
 *     protected boolean isTransactionCommitted(MessageView messageView) {
 *         // 从消息属性中取出业务 ID（发布时通过 setProperties 设置）
 *         String orderId = messageView.getProperties().get("orderId");
 *         return orderRepository.existsById(Long.parseLong(orderId));
 *     }
 * }
 * }</pre>
 *
 * @author 孙士雄
 */
@Slf4j
public abstract class AbstractRocketMqTransactionChecker implements TransactionChecker {

    /**
     * 判断本地事务是否已提交。
     *
     * <p>返回 {@code true} → 提交消息；{@code false} → 保持 UNKNOWN 让 Broker 继续回查。
     * 如果确认本地事务已回滚，请返回一个能让上层转为 {@link TransactionResolution#ROLLBACK} 的标识，
     * 或覆盖 {@link #check(MessageView)} 直接返回 ROLLBACK。
     *
     * @param messageView 回查消息视图（含消息属性、eventId 等）
     * @return 本地事务是否已提交
     */
    protected abstract boolean isTransactionCommitted(MessageView messageView);

    @Override
    public TransactionResolution check(MessageView messageView) {
        try {
            boolean committed = isTransactionCommitted(messageView);
            TransactionResolution resolution = committed
                    ? TransactionResolution.COMMIT
                    : TransactionResolution.UNKNOWN;
            log.debug("Transaction check result: {}, topic: {}, messageId: {}",
                    resolution, messageView.getTopic(), messageView.getMessageId());
            return resolution;
        } catch (Exception e) {
            // 回查本身出现异常时，返回 UNKNOWN 让 Broker 下次继续重试
            log.error("Transaction check failed, topic: {}, messageId: {}",
                    messageView.getTopic(), messageView.getMessageId(), e);
            return TransactionResolution.UNKNOWN;
        }
    }
}
