package com.eagle.message.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * 消息模块错误码（17001–17004）。
 *
 * @author eagle
 */
public enum MessageErrorCode implements ErrorCode {

    CHANNEL_NOT_SUPPORTED(17001, "error.message.channel_not_supported", "不支持的消息渠道类型"),
    SEND_FAILED(17002, "error.message.send_failed", "消息发送失败"),
    TEMPLATE_NOT_FOUND(17003, "error.message.template_not_found", "消息模板不存在"),
    EMPTY_RECIPIENTS(17004, "error.message.empty_recipients", "消息接收人不能为空");

    private final ErrorCode.Meta meta;

    MessageErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
