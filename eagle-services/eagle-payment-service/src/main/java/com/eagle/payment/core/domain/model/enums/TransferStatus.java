package com.eagle.payment.core.domain.model.enums;

/**
 * 提现 / B2C 转账状态机。
 *
 * <pre>
 *   PENDING ──风控通过 + 渠道下单成功──&gt; REVIEWING ──渠道异步成功──&gt; SUCCESS
 *                                                ──渠道异步失败──&gt; FAILED
 *                                                ──收款方退票────&gt; RETURNED
 *           ──风控拒绝 / 渠道下单失败──&gt; FAILED (终态)
 * </pre>
 *
 * <p>{@link #SUCCESS} / {@link #FAILED} / {@link #RETURNED} 为终态。{@link #RETURNED}
 * 表示已扣款但收款方退回 (如对方账户异常 / 实名不符),金额回到商户账户,需人工介入。
 *
 * @author sunshixiong
 */
public enum TransferStatus {
    PENDING,
    REVIEWING,
    SUCCESS,
    FAILED,
    RETURNED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == RETURNED;
    }
}
