package com.eagle.system.base.domain.model.enums;

import com.eagle.common.exception.ErrorCode;

/**
 * 用户领域错误码（10001–10013）
 */
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(10001, "error.user.not_found", "用户不存在"),
    USERNAME_REQUIRED(10007, "error.user.username_required", "用户名不能为空");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    UserErrorCode(int code, String messageKey, String defaultMessage) {
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
