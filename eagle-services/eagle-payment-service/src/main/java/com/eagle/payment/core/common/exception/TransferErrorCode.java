package com.eagle.payment.core.common.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * Transfer 域错误码 (70040-70059)。
 *
 * @author sunshixiong
 */
public enum TransferErrorCode implements ErrorCode {

    TRANSFER_NOT_FOUND(70040, "error.transfer.not_found", "提现单不存在"),
    DUPLICATE_TRANSFER(70041, "error.transfer.duplicate", "业务提现号已存在"),
    TRANSFER_DISABLED(70042, "error.transfer.disabled", "提现功能未开启"),
    INVALID_TRANSFER_AMOUNT(70043, "error.transfer.invalid_amount", "提现金额不合法"),
    EXCEED_SINGLE_LIMIT(70044, "error.transfer.exceed_single_limit", "单笔提现超过限额"),
    EXCEED_DAILY_AMOUNT(70045, "error.transfer.exceed_daily_amount", "当日累计提现超过限额"),
    EXCEED_DAILY_COUNT(70046, "error.transfer.exceed_daily_count", "当日提现次数超过上限"),
    TRANSFER_GATEWAY_ERROR(70047, "error.transfer.gateway_error", "提现渠道异常"),
    INVALID_TRANSFER_STATUS(70048, "error.transfer.invalid_status", "提现单状态不允许此操作"),
    CHANNEL_UNAVAILABLE(70049, "error.transfer.channel_unavailable", "提现渠道暂不可用");

    private final ErrorCode.Meta meta;

    TransferErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
