package com.eleganteer.system.common.constant;

/**
 * 权限相关通用常量
 *
 * @author sunshixoing
 */
public class SecurityConstants {
    /**
     * oauth 认证端点
     */
    public static final String AUTH_AUTHORIZE = "/authorize";
    /**
     * 授权token url
     */
    public static final String AUTH_TOKEN = "/oauth/token";
    /**
     * 注销token url
     */
    public static final String TOKEN_LOGOUT = "/login?logout";

    /**
     * 权限开始
     */
    public static final String ROLE_START = "ROLE_";

    /**
     * 用户ID字段
     */
    public static final String DETAILS_USER_ID = "id";

    /**
     * 权限
     */
    public static final String DETAILS_ROLES = "roles";

    /**
     * 登录名
     */
    public static final String DETAILS_USERNAME = "loginName";
    /**
     * 用户名称
     */
    public static final String DETAILS_USER_NAME = "userName";

    /**
     * 电话
     */
    public static final String DETAILS_PHONE = "phone";
    /**
     * 部门ID
     */
    public static final String DETAILS_DEP_ID = "depId";
    /**
     * 部门名称
     */
    public static final String DETAILS_DEP_NAME = "depName";
}
