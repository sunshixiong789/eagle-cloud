package com.eagle.system.common.exception;

import com.eagle.common.exception.ErrorCode;

/** 系统管理资源错误码（20001–20008） */
public enum SystemErrorCode implements ErrorCode {

    DEPT_NOT_FOUND(20001, "error.dept.not_found", "部门不存在"),
    ROLE_NOT_FOUND(20002, "error.role.not_found", "角色不存在"),
    DICT_NOT_FOUND(20004, "error.dict.not_found", "字典不存在"),
    POST_NOT_FOUND(20005, "error.post.not_found", "岗位不存在"),
    LOG_NOT_FOUND(20006, "error.log.not_found", "日志不存在"),
    POST_CODE_EXISTS(20008, "error.post.code_already_exists", "岗位编码已存在");

    private final int code;
    private final String messageKey;
    private final String defaultMessage;

    SystemErrorCode(int code, String messageKey, String defaultMessage) {
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
