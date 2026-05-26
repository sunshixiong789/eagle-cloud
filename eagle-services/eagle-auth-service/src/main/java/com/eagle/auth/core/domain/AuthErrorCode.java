package com.eagle.auth.core.domain;

import com.eagle.common.exception.ErrorCode;

/**
 * 认证领域错误码（11001–11044）
 */
public enum AuthErrorCode implements ErrorCode {

    // ==================== 认证流程（11001–11015）====================

    INVALID_TOKEN(11001, "error.auth.invalid_token", "Token无效"),
    TOKEN_EXPIRED(11002, "error.auth.token_expired", "Token已过期"),
    INVALID_CAPTCHA(11003, "error.auth.invalid_captcha", "验证码错误"),
    CAPTCHA_EXPIRED(11004, "error.auth.captcha_expired", "验证码已过期"),
    LOGIN_BLOCKED(11005, "error.auth.login_blocked", "登录尝试过于频繁，请30分钟后重试"),
    OPENID_REQUIRED(11006, "error.auth.openid_required", "openid 不能为空"),
    WEB_OPENID_REQUIRED(11007, "error.auth.web_openid_required", "webOpenid 不能为空"),
    MP_OPENID_REQUIRED(11008, "error.auth.mp_openid_required", "mpOpenid 不能为空"),
    WECHAT_CODE_REQUIRED(11009, "error.auth.wechat_code_required", "微信登录 code 不能为空"),
    SMS_PHONE_REQUIRED(11010, "error.auth.sms_phone_required", "手机号不能为空"),
    SMS_CODE_REQUIRED(11011, "error.auth.sms_code_required", "验证码不能为空"),
    SMS_RATE_LIMIT(11012, "error.auth.sms_rate_limit", "发送过于频繁，请60秒后重试"),
    SMS_SEND_FAILED(11013, "error.auth.sms_send_failed", "短信发送失败"),
    WECHAT_LOGIN_FAILED(11014, "error.auth.wechat_login_failed", "微信登录失败"),
    WECHAT_USER_INFO_FAILED(11015, "error.auth.wechat_user_info_failed", "获取微信用户信息失败"),

    // ==================== OAuth2 客户端（11016–11021）====================

    CLIENT_NOT_FOUND(11016, "error.client.not_found", "客户端不存在"),
    CLIENT_ID_EXISTS(11017, "error.client.already_exists", "客户端 ID 已存在"),
    CLIENT_ID_REQUIRED(11018, "error.client.id_required", "客户端 ID 不能为空"),
    CLIENT_NAME_REQUIRED(11019, "error.client.name_required", "客户端名称不能为空"),
    CLIENT_GRANT_TYPE_REQ(11020, "error.client.grant_type_required", "授权类型不能为空"),
    CLIENT_DISABLED(11021, "error.client.disabled", "客户端已被禁用"),

    // ==================== 账号管理（11022–11029）====================

    ACCOUNT_NOT_FOUND(11022, "error.account.not_found", "账号不存在"),
    ACCOUNT_ALREADY_EXISTS(11023, "error.account.already_exists", "账号已存在"),
    ACCOUNT_LOCKED(11024, "error.account.locked", "账号已被锁定"),
    ACCOUNT_NOT_LOCKED(11025, "error.account.not_locked", "账号未被锁定"),
    PASSWORD_REQUIRED(11026, "error.account.password_required", "密码不能为空"),
    NEW_PASSWORD_REQUIRED(11027, "error.account.new_password_required", "新密码不能为空"),
    PHONE_REQUIRED(11028, "error.account.phone_required", "手机号不能为空"),
    ACCOUNT_USERNAME_REQUIRED(11029, "error.account.username_required", "用户名不能为空"),

    // ==================== 手机号绑定与密码找回（11030–11033）====================

    PHONE_NOT_BOUND(11030, "error.account.phone_not_bound", "该手机号未绑定任何账号"),
    PHONE_ALREADY_BOUND(11031, "error.account.phone_already_bound", "该手机号已绑定其他账号"),
    SMS_CODE_INVALID(11032, "error.auth.sms_code_invalid", "短信验证码错误或已过期"),
    ACCOUNT_PHONE_ALREADY_SET(11033, "error.account.phone_already_set", "该账号已绑定手机号"),

    // ==================== 手机号一键登录（11034–11037）====================

    ONE_CLICK_TOKEN_REQUIRED(11034, "error.auth.one_click_token_required", "一键登录 access_token 不能为空"),
    ONE_CLICK_VERIFY_FAILED(11035, "error.auth.one_click_verify_failed", "一键登录校验失败"),
    ONE_CLICK_PROVIDER_DISABLED(11036, "error.auth.one_click_provider_disabled", "一键登录服务未启用"),
    ONE_CLICK_PHONE_PARSE_FAILED(11037, "error.auth.one_click_phone_parse_failed", "一键登录获取手机号失败"),

    // ==================== 账号冻结（11038–11040）====================

    ACCOUNT_FROZEN(11038, "error.account.frozen", "账号已被冻结：{0}"),
    ACCOUNT_NOT_FROZEN(11039, "error.account.not_frozen", "账号未被冻结"),
    ACCOUNT_FREEZE_UNTIL_INVALID(11040, "error.account.freeze_until_invalid", "冻结到期时间必须晚于当前时间"),

    // ==================== 黑名单（11041–11044）====================

    IDENTITY_BLACKLISTED(11041, "error.auth.identity_blacklisted", "该身份已被禁止访问"),
    IP_BLACKLISTED(11042, "error.auth.ip_blacklisted", "当前 IP 已被禁止访问"),
    BLACKLIST_DUPLICATE(11043, "error.blacklist.duplicate", "该黑名单条目已存在"),
    BLACKLIST_NOT_FOUND(11044, "error.blacklist.not_found", "黑名单条目不存在");

    private final ErrorCode.Meta meta;

    AuthErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
