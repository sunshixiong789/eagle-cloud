package com.eagle.system.base.domain.model.enums;

import com.eagle.common.exception.ErrorCode;

/**
 * 用户领域错误码（10001–10014）
 */
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(10001, "error.user.not_found", "用户不存在"),
    USERNAME_REQUIRED(10007, "error.user.username_required", "用户名不能为空"),
    MAX_ROLES_EXCEEDED(10013, "error.user.max_roles_exceeded", "用户最多分配 10 个角色"),
    ADMIN_USER_PROTECTED(10014, "error.user.admin_protected", "初始化管理员用户不允许执行该操作");

    private final ErrorCode.Meta meta;

    UserErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
