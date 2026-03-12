package com.eleganteer.system.common.exception;

/**
 * 资源冲突异常
 * <p>
 * 用于表示资源已存在或状态冲突的情况，对应 HTTP 409 Conflict
 *
 * @author 孙士雄
 * @since 1.0.0
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
