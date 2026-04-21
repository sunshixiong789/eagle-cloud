package com.eagle.system.upms.domain.model.enums;

/**
 * 日志类型
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/9-09:50
 */
public enum LogType {

    /**
     * 登录
     */
    LOGIN,

    /**
     * 登出
     */
    LOGOUT,

    /**
     * 操作
     */
    OPERATION,

    /**
     * 异常
     */
    EXCEPTION,

    /**
     * API调用
     */
    API_CALL
}
