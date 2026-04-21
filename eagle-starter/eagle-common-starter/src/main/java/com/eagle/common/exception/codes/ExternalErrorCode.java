package com.eagle.common.exception.codes;

import com.eagle.common.exception.ErrorCode;

/** 外部服务错误码（15001–15002） */
public enum ExternalErrorCode implements ErrorCode {
    EXTERNAL_SERVICE_ERROR(15001, "error.external.service_error", "外部服务调用失败"),
    EXTERNAL_SERVICE_TIMEOUT(15002, "error.external.service_timeout", "外部服务调用超时");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    ExternalErrorCode(int code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
