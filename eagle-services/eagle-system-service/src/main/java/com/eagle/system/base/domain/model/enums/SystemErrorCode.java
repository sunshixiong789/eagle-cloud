package com.eagle.system.base.domain.model.enums;

import com.eagle.common.exception.ErrorCode;

/**
 * 系统管理资源错误码（20001–20008）
 */
public enum SystemErrorCode implements ErrorCode {

    DEPT_NOT_FOUND(20001, "error.dept.not_found", "部门不存在"),
    ROLE_NOT_FOUND(20002, "error.role.not_found", "角色不存在"),
    DICT_NOT_FOUND(20004, "error.dict.not_found", "字典不存在"),
    POST_NOT_FOUND(20005, "error.post.not_found", "岗位不存在"),
    LOG_NOT_FOUND(20006, "error.log.not_found", "日志不存在"),
    POST_CODE_EXISTS(20008, "error.post.code_already_exists", "岗位编码已存在");

    private final ErrorCode.Meta meta;

    SystemErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
