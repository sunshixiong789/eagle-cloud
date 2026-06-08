package com.eagle.payment.core.common.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * Payment 域错误码 (70001–70019)。
 *
 * @author sunshixiong
 */
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(70001, "error.payment.not_found", "支付订单不存在"),
    DUPLICATE_PAYMENT(70002, "error.payment.duplicate", "业务订单号已存在,请勿重复下单"),
    INVALID_AMOUNT(70003, "error.payment.invalid_amount", "支付金额不合法"),
    INVALID_STATUS(70004, "error.payment.invalid_status", "支付订单状态不允许此操作"),
    PAYMENT_EXPIRED(70005, "error.payment.expired", "支付订单已过期"),
    PAYMENT_CANCELLED(70006, "error.payment.cancelled", "支付订单已取消"),
    CHANNEL_UNAVAILABLE(70007, "error.payment.channel_unavailable", "支付渠道暂不可用"),
    GATEWAY_ERROR(70008, "error.payment.gateway_error", "支付渠道异常"),
    SIGNATURE_INVALID(70009, "error.payment.signature_invalid", "回调签名校验失败"),
    NOTIFY_UNKNOWN_PAYMENT(70010, "error.payment.notify_unknown_payment", "回调对应的支付订单不存在");

    private final ErrorCode.Meta meta;

    PaymentErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
