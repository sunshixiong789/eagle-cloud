package com.eagle.common.exception.codes;

import com.eagle.common.exception.ErrorCode;

/**
 * 分布式锁错误码（90001–90010）。
 *
 * <p>从 redis-starter 提取至 common-starter，供所有 {@code DistributedLock}
 * 实现（Redis / RocketMQ / 其他）共用。
 *
 * @author 孙士雄
 */
public enum LockErrorCode implements ErrorCode {

    LOCK_ACQUIRE_FAILED(90001, "error.lock.acquire_failed", "获取分布式锁失败"),
    LOCK_INTERRUPTED(90002, "error.lock.interrupted", "获取分布式锁被中断"),
    LOCK_TOKEN_INIT_FAILED(90003, "error.lock.token_init_failed", "分布式锁令牌初始化失败"),
    LOCK_RELEASE_FAILED(90004, "error.lock.release_failed", "分布式锁释放失败");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    LockErrorCode(int code, String messageKey, String defaultMessage) {
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
