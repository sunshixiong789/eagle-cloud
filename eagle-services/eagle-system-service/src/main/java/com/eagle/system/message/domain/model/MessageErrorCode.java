package com.eagle.system.message.domain.model;

import com.eagle.common.exception.ErrorCode;

/**
 * 站内消息模块错误码（30501-30599）。
 *
 * @author sunshixiong
 */
public enum MessageErrorCode implements ErrorCode {

    MESSAGE_NOT_FOUND(30501, "message.not_found", "消息不存在"),
    MESSAGE_FORBIDDEN(30502, "message.forbidden", "无权访问他人的消息");

    private final ErrorCode.Meta meta;

    MessageErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
