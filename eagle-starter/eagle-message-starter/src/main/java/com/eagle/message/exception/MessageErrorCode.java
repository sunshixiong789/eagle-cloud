package com.eagle.message.exception;

import com.eagle.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息模块错误码（17000 段）。
 *
 * @author 孙士雄
 */
@Getter
@RequiredArgsConstructor
public enum MessageErrorCode implements ErrorCode {

    CHANNEL_NOT_SUPPORTED(17001, "error.message.channel_not_supported", "不支持的消息渠道类型"),
    SEND_FAILED(17002, "error.message.send_failed", "消息发送失败"),
    TEMPLATE_NOT_FOUND(17003, "error.message.template_not_found", "消息模板不存在"),
    EMPTY_RECIPIENTS(17004, "error.message.empty_recipients", "消息接收人不能为空");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;
}