package com.eagle.payment.core.common.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * Refund 域错误码 (70020-70039)。
 *
 * @author sunshixiong
 */
public enum RefundErrorCode implements ErrorCode {

    REFUND_NOT_FOUND(70020, "error.refund.not_found", "退款单不存在"),
    DUPLICATE_REFUND(70021, "error.refund.duplicate", "业务退款号已存在,请勿重复发起"),
    INVALID_REFUND_AMOUNT(70022, "error.refund.invalid_amount", "退款金额不合法"),
    EXCEED_REFUNDABLE(70023, "error.refund.exceed_refundable", "退款金额超过可退余额"),
    PAYMENT_NOT_PAID(70024, "error.refund.payment_not_paid", "支付未完成,无法退款"),
    PARTIAL_DISABLED(70025, "error.refund.partial_disabled", "当前未开启部分退款"),
    REFUND_GATEWAY_ERROR(70026, "error.refund.gateway_error", "退款渠道异常"),
    INVALID_REFUND_STATUS(70027, "error.refund.invalid_status", "退款单状态不允许此操作");

    private final ErrorCode.Meta meta;

    RefundErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
