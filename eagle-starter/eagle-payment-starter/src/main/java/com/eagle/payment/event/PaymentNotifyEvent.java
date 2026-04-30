package com.eagle.payment.event;

import com.eagle.payment.model.NotifyResult;
import org.springframework.context.ApplicationEvent;

/**
 * 支付异步通知事件。
 *
 * <p>支付回调验签成功后由 {@link com.eagle.payment.controller.PaymentNotifyController}
 * 发布此事件，业务方通过 {@link org.springframework.context.event.EventListener} 或
 * {@link org.springframework.transaction.event.TransactionalEventListener} 监听处理。
 *
 * <p>示例：
 * <pre>{@code
 * @Async
 * @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * @Transactional(propagation = Propagation.REQUIRES_NEW)
 * public void handlePaymentNotify(PaymentNotifyEvent event) {
 *     NotifyResult result = event.getResult();
 *     if (result.isSuccess()) {
 *         orderService.confirmPayment(result.getOutTradeNo(), result.getTradeNo());
 *     }
 * }
 * }</pre>
 *
 * @author eagle
 */
public class PaymentNotifyEvent extends ApplicationEvent {

    /**
     * 支付通知解析结果
     */
    private final NotifyResult result;

    /**
     * 创建支付通知事件。
     *
     * @param source 事件来源（通常为发布方的 Controller 实例）
     * @param result 通知解析结果
     */
    public PaymentNotifyEvent(Object source, NotifyResult result) {
        super(source);
        this.result = result;
    }

    /**
     * 获取支付通知解析结果。
     *
     * @return 通知结果
     */
    public NotifyResult getResult() {
        return result;
    }
}
