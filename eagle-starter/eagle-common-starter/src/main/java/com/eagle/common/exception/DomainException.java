package com.eagle.common.exception;

/**
 * 领域异常 — HTTP 400 Bad Request
 * <p>
 * 用于领域验证失败和业务状态不变性违反，例如：
 * <ul>
 *   <li>必填字段为空</li>
 *   <li>用户已被锁定，不能重复锁定</li>
 *   <li>角色数量超出上限</li>
 * </ul>
 * 推荐通过 {@link ErrorCode#toDomainException(Object...)} 工厂方法创建：
 * <pre>{@code
 * throw UserErrorCode.USERNAME_REQUIRED.toDomainException();
 * }</pre>
 *
 * @author sunshixiong
 */
public class DomainException extends AppException {
    private static final long serialVersionUID = 1L;

    public DomainException(ErrorCode code, Object... args) {
        super(code, args);
    }
}
