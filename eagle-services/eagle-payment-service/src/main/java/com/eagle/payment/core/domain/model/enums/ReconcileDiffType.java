package com.eagle.payment.core.domain.model.enums;

/**
 * 对账差异类型。
 *
 * @author sunshixiong
 */
public enum ReconcileDiffType {
    /** 渠道有此单,本地找不到 (本地丢单)。 */
    LOCAL_MISSING,
    /** 本地有此单(PAID),渠道清算单未发现 (渠道丢单)。 */
    CHANNEL_MISSING,
    /** 双方都有但金额不一致。 */
    AMOUNT_MISMATCH,
    /** 双方都有但状态不一致。 */
    STATUS_MISMATCH
}
