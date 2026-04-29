package com.eagle.payment.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一企业付款/转账结果。
 *
 * <p>转账接口通常为同步结果，但实际到账时间因渠道和金额而异。
 * {@code success = true} 仅表示第三方受理成功，不代表资金已到账。
 *
 * @author eagle
 */
@Data
@Builder
public class TransferResult {

    /** 是否转账成功（第三方受理成功） */
    private boolean success;

    /** 第三方转账单号（受理成功时有值） */
    private String orderId;

    /** 商户转账流水号（与请求对应） */
    private String outBizNo;

    /** 错误信息（success = false 时有值） */
    private String errorMessage;
}
