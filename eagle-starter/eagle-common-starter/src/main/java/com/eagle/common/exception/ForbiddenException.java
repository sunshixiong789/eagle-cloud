package com.eagle.common.exception;

import java.io.Serial;

/**
 * 越权访问异常 — HTTP 403 Forbidden
 * <p>
 * 用于「已认证但无权操作该资源」的业务判定（典型场景：访问他人的订单 / 消息 / 收款账号）。
 * 与 Spring Security 的 {@code AccessDeniedException} 区别在于本异常携带业务 {@link ErrorCode}，
 * 前端可据 {@code errorCode} 做精细分支。
 * <p>
 * 推荐通过 {@link ErrorCode#toForbiddenException(Object...)} 工厂方法创建：
 * <pre>{@code
 * throw OrderErrorCode.ORDER_FORBIDDEN.toForbiddenException();
 * }</pre>
 *
 * @author sunshixiong
 */
public class ForbiddenException extends AppException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException(ErrorCode code, Object... args) {
        super(code, args);
    }
}
