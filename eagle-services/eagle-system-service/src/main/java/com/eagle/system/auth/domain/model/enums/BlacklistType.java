package com.eagle.system.auth.domain.model.enums;

/**
 * 黑名单类型枚举
 *
 * @author sunshixiong
 */
public enum BlacklistType {
    /**
     * 账号 ID（值为 Long 字符串）
     */
    ACCOUNT_ID,
    /**
     * 手机号
     */
    PHONE,
    /**
     * 邮箱
     */
    EMAIL,
    /**
     * IP 地址
     */
    IP,
    /**
     * 微信 openid
     */
    OPENID
}
