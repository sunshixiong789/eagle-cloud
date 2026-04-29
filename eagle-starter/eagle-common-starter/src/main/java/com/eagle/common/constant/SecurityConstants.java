package com.eagle.common.constant;

/**
 * 安全相关通用常量。
 *
 * <p>统一定义 OAuth2 端点路径和 JWT Claims 字段名，供授权服务器与资源服务器共享使用。
 *
 * @author sunshixiong
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** OAuth2 授权端点 */
    public static final String AUTH_AUTHORIZE = "/oauth2/authorize";

    /** OAuth2 Token 端点 */
    public static final String AUTH_TOKEN = "/oauth2/token";

    /** 注销端点 */
    public static final String TOKEN_LOGOUT = "/login?logout";

    /** Spring Security 角色前缀 */
    public static final String ROLE_START = "ROLE_";

    /** JWT Claim：用户 ID */
    public static final String DETAILS_USER_ID = "id";

    /** JWT Claim：角色列表 */
    public static final String DETAILS_ROLES = "roles";

    /** JWT Claim：登录名 */
    public static final String DETAILS_USERNAME = "loginName";

    /** JWT Claim：用户姓名 */
    public static final String DETAILS_USER_NAME = "userName";

    /** JWT Claim：手机号 */
    public static final String DETAILS_PHONE = "phone";

    /** JWT Claim：部门 ID */
    public static final String DETAILS_DEPT_ID = "depId";

    /** JWT Claim：部门名称 */
    public static final String DETAILS_DEPT_NAME = "depName";
}