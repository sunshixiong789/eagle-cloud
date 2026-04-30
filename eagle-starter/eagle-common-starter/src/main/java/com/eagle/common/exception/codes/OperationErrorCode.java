package com.eagle.common.exception.codes;

import com.eagle.common.exception.ErrorCode;

/**
 * 业务操作错误码（13001–13006）
 */
public enum OperationErrorCode implements ErrorCode {
    OPERATION_FAILED(13001, "error.operation.failed", "操作失败"),
    DUPLICATE_OPERATION(13002, "error.operation.duplicate", "请勿重复操作"),
    OPERATION_NOT_ALLOWED(13003, "error.operation.not_allowed", "当前状态不允许此操作"),
    DEPENDENT_DATA_EXISTS(13004, "error.operation.dependent_data_exists", "存在关联数据，无法删除"),
    MESSAGE_REQUIRED(13005, "error.chat.message_required", "消息内容不能为空"),
    RECIPIENT_REQUIRED(13006, "error.chat.recipient_required", "接收者不能为空");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    OperationErrorCode(int code, String messageKey, String defaultMessage) {
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
