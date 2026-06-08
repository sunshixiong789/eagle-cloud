package com.eagle.payment.core.domain.model.enums;

/**
 * Transfer 受理模式。
 *
 * <ul>
 *   <li>{@link #IMMEDIATE} 立即到账:create 后直接调渠道下单。</li>
 *   <li>{@link #APPROVAL} 需审核:create 后进入 {@code PENDING_APPROVAL},
 *       由管理员通过 {@code /admin/transfers/{id}/approve} 审核后再调渠道。</li>
 * </ul>
 *
 * @author sunshixiong
 */
public enum TransferMode {
    IMMEDIATE,
    APPROVAL
}
