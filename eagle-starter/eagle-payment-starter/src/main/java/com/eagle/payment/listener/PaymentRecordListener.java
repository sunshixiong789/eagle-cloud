package com.eagle.payment.listener;

import com.eagle.payment.event.PaymentNotifyEvent;
import com.eagle.payment.model.NotifyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

/**
 * 支付通知记录模板监听器。
 *
 * <p>继承此类并实现 {@link #onPaymentSuccess} 和 {@link #onPaymentFailed}，
 * 即可自动监听支付成功/失败事件并记录到业务数据库。
 *
 * <p>此类使用同步 {@link EventListener}，若需事务支持或异步处理，
 * 子类可在覆盖方法上添加 {@code @Transactional} 或 {@code @Async} 注解。
 *
 * <p>示例：
 * <pre>{@code
 * @Component
 * public class OrderPaymentListener extends PaymentRecordListener {
 *
 *     private final OrderApplicationService orderService;
 *
 *     public OrderPaymentListener(OrderApplicationService orderService) {
 *         this.orderService = orderService;
 *     }
 *
 *     @Override
 *     protected void onPaymentSuccess(NotifyResult result) {
 *         orderService.confirmPayment(result.getOutTradeNo(), result.getTradeNo());
 *     }
 *
 *     @Override
 *     protected void onPaymentFailed(NotifyResult result) {
 *         orderService.markPaymentFailed(result.getOutTradeNo());
 *     }
 * }
 * }</pre>
 *
 * @author eagle
 */
@Slf4j
public abstract class PaymentRecordListener {

    /**
     * 监听支付通知事件，根据结果分发到对应处理方法。
     *
     * <p>此方法为 {@code final}，确保子类不绕过异常兜底逻辑。
     * 子类异常将被捕获并记录日志，不影响事件发布方的执行流程。
     *
     * @param event 支付通知事件
     */
    @EventListener
    public final void onPaymentNotify(PaymentNotifyEvent event) {
        NotifyResult result = event.getResult();
        try {
            if (result.isSuccess()) {
                onPaymentSuccess(result);
            } else {
                onPaymentFailed(result);
            }
        } catch (Exception e) {
            log.error("[Payment] Error processing payment notify, outTradeNo: {}: {}",
                    result.getOutTradeNo(), e.getMessage(), e);
        }
    }

    /**
     * 支付成功时的处理逻辑，由子类实现。
     *
     * <p>典型场景：更新订单状态为已支付、触发发货流程等。
     *
     * @param result 支付通知解析结果（success = true）
     */
    protected abstract void onPaymentSuccess(NotifyResult result);

    /**
     * 支付失败时的处理逻辑，由子类实现。
     *
     * <p>典型场景：将订单标记为支付失败、释放库存预占等。
     *
     * @param result 支付通知解析结果（success = false）
     */
    protected abstract void onPaymentFailed(NotifyResult result);
}
