package com.eagle.idempotency.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * 幂等性组件错误码枚举。
 *
 * <p>覆盖 TOKEN 缺失、Token 无效/已使用、重复请求三种场景。
 *
 * @author sunshixiong
 */
public enum IdempotencyErrorCode implements ErrorCode {

    /**
     * 幂等 Token 缺失（请求 Header 中未携带 Token）
     */
    IDEMPOTENCY_TOKEN_MISSING(40001, "error.idempotency.token_missing", "幂等 Token 缺失"),

    /**
     * 幂等 Token 无效或已被使用（Token 不存在、已过期或已消费）
     */
    IDEMPOTENCY_TOKEN_INVALID(40002, "error.idempotency.token_invalid", "幂等 Token 无效或已使用"),

    /**
     * 重复请求（BUSINESS_KEY 模式下相同业务键已处理中）
     */
    IDEMPOTENCY_DUPLICATE_REQUEST(40003, "error.idempotency.duplicate", "重复请求");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    IdempotencyErrorCode(int code, String messageKey, String defaultMessage) {
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
