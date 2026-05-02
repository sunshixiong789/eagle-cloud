package com.eagle.ai.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * AI 模块错误码。
 *
 * <p>错误码范围 90001–90099，对应 i18n key 前缀 {@code error.ai.*}。
 */
public enum AiErrorCode implements ErrorCode {

    /** AI 请求频率超出限制（限流触发）。 */
    AI_RATE_LIMIT_EXCEEDED(90001, "error.ai.rate_limit_exceeded", "AI 请求频率超出限制，请稍后再试"),

    /** AI 服务不可用（Provider 异常）。 */
    AI_SERVICE_UNAVAILABLE(90002, "error.ai.service_unavailable", "AI 服务暂时不可用，请稍后重试");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    AiErrorCode(int code, String messageKey, String defaultMessage) {
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
