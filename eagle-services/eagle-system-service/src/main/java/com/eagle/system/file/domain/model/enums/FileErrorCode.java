package com.eagle.system.file.domain.model.enums;

import com.eagle.common.exception.ErrorCode;

/**
 * 文件领域错误码（40001–40010）
 *
 * @author sunshixiong
 */
public enum FileErrorCode implements ErrorCode {

    FILE_NOT_FOUND(40001, "error.file.not_found", "文件不存在"),
    FILE_EMPTY(40002, "error.file.empty", "文件为空"),
    FILE_TOO_LARGE(40003, "error.file.size_exceeded", "文件大小超过限制"),
    UNSUPPORTED_FILE_TYPE(40004, "error.file.unsupported_format", "不支持的文件类型"),
    INVALID_FILE_NAME(40005, "error.file.invalid_name", "非法的文件名"),
    FILE_UPLOAD_FAILED(40006, "error.file.upload_failed", "文件上传失败"),
    FILE_DOWNLOAD_FAILED(40007, "error.file.download_failed", "文件下载失败"),
    FILE_ACCESS_DENIED(40008, "error.file.access_denied", "无权访问该文件");

    private final ErrorCode.Meta meta;

    FileErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
