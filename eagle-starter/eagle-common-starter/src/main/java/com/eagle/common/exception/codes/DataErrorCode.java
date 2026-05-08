package com.eagle.common.exception.codes;

import com.eagle.common.exception.ErrorCode;

/**
 * 数据验证错误码（12001–12006）
 */
public enum DataErrorCode implements ErrorCode {

    DATA_ALREADY_EXISTS(12001, "error.data.already_exists", "数据已存在"),
    DATA_NOT_FOUND(12002, "error.data.not_found", "数据不存在"),
    INVALID_DATA_FORMAT(12003, "error.data.invalid_format", "数据格式错误"),
    DATA_VALIDATION_FAILED(12004, "error.data.validation_failed", "数据校验失败"),
    INVALID_PHONE_FORMAT(12005, "error.validation.phone_format", "手机号格式不正确"),
    EXPORT_LIMIT_EXCEEDED(12006, "error.data.export_limit", "导出数据超出上限，请缩小筛选范围");

    private final ErrorCode.Meta meta;

    DataErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
