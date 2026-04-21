package com.eagle.common.exception.codes;

import com.eagle.common.exception.ErrorCode;

/** 文件操作错误码（14001–14004） */
public enum FileErrorCode implements ErrorCode {
    FILE_NOT_FOUND(14001, "error.file.not_found", "文件不存在"),
    UNSUPPORTED_FILE_FORMAT(14002, "error.file.unsupported_format", "文件格式不支持"),
    FILE_SIZE_EXCEEDED(14003, "error.file.size_exceeded", "文件大小超过限制"),
    FILE_UPLOAD_FAILED(14004, "error.file.upload_failed", "文件上传失败");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    FileErrorCode(int code, String messageKey, String defaultMessage) {
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
