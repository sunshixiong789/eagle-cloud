package com.eagle.system.common.exception;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.ErrorCode;

/**
 * 资源冲突异常 — HTTP 409 Conflict
 * <p>
 * 推荐通过 {@link ErrorCode#toConflictException(Object...)} 工厂方法创建：
 * <pre>{@code
 * throw UserErrorCode.USER_ALREADY_EXISTS.toConflictException();
 * }</pre>
 *
 * @author sunshixiong
 */
public class ConflictException extends AppException {
    private static final long serialVersionUID = 1L;

    public ConflictException(ErrorCode code, Object... args) {
        super(code, args);
    }
}
