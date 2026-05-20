package com.eagle.idempotency.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * 幂等性组件错误码（40001–40003）。
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

    private final ErrorCode.Meta meta;

    IdempotencyErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
