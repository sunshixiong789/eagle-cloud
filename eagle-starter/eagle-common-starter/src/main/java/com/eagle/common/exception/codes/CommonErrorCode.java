package com.eagle.common.exception.codes;

import com.eagle.common.exception.ErrorCode;

/**
 * HTTP 标准错误及服务端错误码
 */
public enum CommonErrorCode implements ErrorCode {

    INVALID_PARAMETER(400, "error.common.invalid_parameter", "请求参数错误"),
    UNAUTHORIZED(401, "error.common.unauthorized", "未授权，请先登录"),
    FORBIDDEN(403, "error.common.forbidden", "无权限访问"),
    NOT_FOUND(404, "error.common.not_found", "资源不存在"),
    METHOD_NOT_ALLOWED(405, "error.common.method_not_allowed", "请求方法不支持"),
    CONFLICT(409, "error.common.conflict", "请求冲突"),
    TOO_MANY_REQUESTS(429, "error.common.too_many_requests", "请求过于频繁，请稍后再试"),
    INTERNAL_SERVER_ERROR(500, "error.server.internal_error", "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "error.server.service_unavailable", "服务暂时不可用");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    CommonErrorCode(int code, String messageKey, String defaultMessage) {
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
