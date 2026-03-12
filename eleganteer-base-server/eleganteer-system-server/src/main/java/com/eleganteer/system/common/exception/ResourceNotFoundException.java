package com.eleganteer.system.common.exception;

/**
 * 资源不存在异常
 * <p>
 * 用于表示请求的资源不存在，对应 HTTP 404 Not Found
 *
 * @author 孙士雄
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
