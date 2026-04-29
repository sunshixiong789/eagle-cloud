package com.eagle.redis.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * Redis 基础设施错误码。
 *
 * @author 孙士雄
 */
public enum RedisErrorCode implements ErrorCode {

    LOCK_ACQUIRE_FAILED(90001, "error.redis.lock_acquire_failed", "获取分布式锁失败"),
    LOCK_INTERRUPTED(90002, "error.redis.lock_interrupted", "获取分布式锁被中断");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    RedisErrorCode(int code, String messageKey, String defaultMessage) {
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
