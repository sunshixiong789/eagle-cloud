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
    AI_SERVICE_UNAVAILABLE(90002, "error.ai.service_unavailable", "AI 服务暂时不可用，请稍后重试"),

    /** AI Token 月度配额已耗尽。 */
    AI_TOKEN_BUDGET_EXCEEDED(90003, "error.ai.token_budget_exceeded", "AI Token 月度配额已耗尽，请联系管理员"),

    /** 输入或输出内容违反安全策略。 */
    AI_CONTENT_SAFETY_VIOLATION(90004, "error.ai.content_safety_violation", "内容违反安全策略，请修改后重试"),

    /** 对话上下文长度超出模型限制。 */
    AI_CONTEXT_LENGTH_EXCEEDED(90005, "error.ai.context_length_exceeded", "对话上下文过长，请开启新会话或缩减历史消息");

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
