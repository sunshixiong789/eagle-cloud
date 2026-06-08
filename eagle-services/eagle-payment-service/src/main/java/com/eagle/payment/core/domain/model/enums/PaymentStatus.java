package com.eagle.payment.core.domain.model.enums;

/**
 * 支付订单状态机。
 *
 * <pre>
 *   CREATED ──下单成功(走渠道)──&gt; PAYING ──回调成功──&gt; PAID
 *                                       ──回调失败──&gt; FAILED
 *                                       ──过期────&gt; EXPIRED
 *                                       ──主动取消──&gt; CANCELLED
 *           ──主动取消(尚未走渠道)─&gt; CANCELLED
 * </pre>
 *
 * <p>{@link #PAID} / {@link #FAILED} / {@link #EXPIRED} / {@link #CANCELLED} 为终态,
 * 不允许向其他状态迁移。{@link #PAID} 之后可走退款链路(由 {@code Refund} 聚合根承载)。
 *
 * @author sunshixiong
 */
public enum PaymentStatus {
    /** 已下单,尚未提交到渠道。 */
    CREATED,
    /** 已提交到渠道,等待用户支付与异步回调。 */
    PAYING,
    /** 支付成功(终态)。 */
    PAID,
    /** 支付失败(终态)。 */
    FAILED,
    /** 过期未支付(终态)。 */
    EXPIRED,
    /** 主动取消(终态)。 */
    CANCELLED;

    /** 该状态是否为终态(不允许再迁移)。 */
    public boolean isTerminal() {
        return this == PAID || this == FAILED || this == EXPIRED || this == CANCELLED;
    }
}
