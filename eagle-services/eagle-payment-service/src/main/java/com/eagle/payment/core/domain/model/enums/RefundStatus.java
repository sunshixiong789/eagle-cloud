package com.eagle.payment.core.domain.model.enums;

/**
 * 退款单状态机。
 *
 * <pre>
 *   PENDING ──提交到渠道──&gt; REFUNDING ──渠道回调成功──&gt; REFUNDED
 *                                    ──渠道回调失败──&gt; FAILED
 *           ──发起前校验失败──&gt; FAILED (终态)
 * </pre>
 *
 * <p>{@link #REFUNDED} / {@link #FAILED} 为终态。{@link #REFUNDED} 后由领域事件触发
 * Payment.accumulateRefund 与跨服务集成事件。
 *
 * @author sunshixiong
 */
public enum RefundStatus {
    /** 退款单已创建,尚未提交到渠道。 */
    PENDING,
    /** 已提交到渠道,等待异步回调。 */
    REFUNDING,
    /** 退款成功 (终态)。 */
    REFUNDED,
    /** 退款失败 (终态)。 */
    FAILED;

    public boolean isTerminal() {
        return this == REFUNDED || this == FAILED;
    }
}
