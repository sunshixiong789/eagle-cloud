package com.eagle.common.exception;

import java.io.Serial;

/**
 * 服务异常 — HTTP 500 Internal Server Error
 * <p>
 * 用于基础设施故障和外部服务调用失败，例如：
 * <ul>
 *   <li>短信发送失败</li>
 *   <li>微信登录接口异常</li>
 *   <li>数据库/缓存连接失败</li>
 * </ul>
 * 推荐通过 {@link ErrorCode#toServiceException(Object...)} 或
 * {@link ErrorCode#toServiceException(Throwable)} 工厂方法创建：
 * <pre>{@code
 * throw AuthErrorCode.SMS_SEND_FAILED.toServiceException(cause);
 * }</pre>
 *
 * @author sunshixiong
 */
public class ServiceException extends AppException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ServiceException(ErrorCode code, Object... args) {
        super(code, args);
    }

    public ServiceException(ErrorCode code, Throwable cause) {
        super(code, cause);
    }
}
