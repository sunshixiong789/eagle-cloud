package com.eagle.common.constant;

/**
 * 安全相关通用常量。
 *
 * <p>统一定义 OAuth2 端点路径和 JWT Claims 字段名，供授权服务器与资源服务器共享使用。
 *
 * @author sunshixiong
 */
public final class SecurityConstants {

    /**
     * OAuth2 授权端点
     */
    public static final String AUTH_AUTHORIZE = "/oauth2/authorize";
    /**
     * OAuth2 Token 端点
     */
    public static final String AUTH_TOKEN = "/oauth2/token";
    /**
     * 注销端点
     */
    public static final String TOKEN_LOGOUT = "/login?logout";
    /**
     * Spring Security 角色前缀
     */
    public static final String ROLE_START = "ROLE_";
    /**
     * JWT Claim：用户 ID（业务 ID，非 OIDC 标准 — OIDC {@code sub} 已被 Spring 默认填为 username，
     * 这里另存数据库主键供下游服务使用）
     */
    public static final String DETAILS_USER_ID = "id";
    /**
     * JWT Claim：角色列表（业界惯例，OIDC Core 无标准 claim；RFC 9068 也仅给"groups"建议名而非强制）
     */
    public static final String DETAILS_ROLES = "roles";
    /**
     * JWT Claim：登录名 — 对应 OIDC Core {@code preferred_username}
     */
    public static final String DETAILS_USERNAME = "preferred_username";
    /**
     * JWT Claim：用户姓名 — 对应 OIDC Core {@code name}
     */
    public static final String DETAILS_USER_NAME = "name";
    /**
     * JWT Claim：手机号 — 对应 OIDC Core {@code phone_number}
     */
    public static final String DETAILS_PHONE = "phone_number";
    /**
     * JWT Claim：头像 URL — 对应 OIDC Core {@code picture}
     */
    public static final String DETAILS_AVATAR = "picture";

    private SecurityConstants() {
    }
}