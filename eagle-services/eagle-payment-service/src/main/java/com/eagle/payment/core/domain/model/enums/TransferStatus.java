package com.eagle.payment.core.domain.model.enums;

/**
 * 提现 / B2C 转账状态机。
 *
 * <pre>
 *   IMMEDIATE 模式:
 *     PENDING ──submittedToChannel──&gt; SUBMITTED ──成功──&gt; SUCCESS ──退票──&gt; RETURNED
 *                                              ──失败──&gt; FAILED
 *     PENDING ──渠道下单失败────────&gt; FAILED
 *
 *   APPROVAL 模式:
 *     PENDING_APPROVAL ──approve──&gt; (内部 submittedToChannel) → SUBMITTED → SUCCESS/FAILED/RETURNED
 *     PENDING_APPROVAL ──reject ──&gt; REJECTED
 * </pre>
 *
 * <p>终态:{@link #SUCCESS} / {@link #FAILED} / {@link #REJECTED} / {@link #RETURNED}。
 *
 * @author sunshixiong
 */
public enum TransferStatus {
    PENDING,
    PENDING_APPROVAL,
    SUBMITTED,
    SUCCESS,
    FAILED,
    REJECTED,
    RETURNED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == REJECTED || this == RETURNED;
    }
}
