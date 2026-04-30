package com.eagle.common.exception;

import java.io.Serial;

/**
 * 资源不存在异常 — HTTP 404 Not Found
 * <p>
 * 推荐通过 {@link ErrorCode#toNotFoundException(Object...)} 工厂方法创建：
 * <pre>{@code
 * throw SystemErrorCode.USER_NOT_FOUND.toNotFoundException();
 * }</pre>
 *
 * @author sunshixiong
 */
public class NotFoundException extends AppException {
    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(ErrorCode code, Object... args) {
        super(code, args);
    }
}
