package com.eagle.system.auth.infrastructure.security;

import com.eagle.system.auth.application.service.BlacklistApplicationService;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.enums.BlacklistType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 登录 / 注册链路的黑名单前置校验
 *
 * <p>所有判断都依赖 {@code TenantContextHolder} 已被 {@code TenantIdFilter} 填充。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class BlacklistChecker {

    private final BlacklistApplicationService blacklist;

    /**
     * 用户名/手机号登录前置校验
     */
    public void checkLogin(String username, String phone, String ip, Long accountId) {
        if (ip != null && blacklist.isBlacklisted(BlacklistType.IP, ip)) {
            throw AuthErrorCode.IP_BLACKLISTED.toServiceException();
        }
        if (phone != null && !phone.isBlank()
                && blacklist.isBlacklisted(BlacklistType.PHONE, phone)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
        if (accountId != null
                && blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, String.valueOf(accountId))) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
    }

    /**
     * 注册前置校验
     */
    public void checkRegister(String phone, String email, String ip) {
        if (ip != null && blacklist.isBlacklisted(BlacklistType.IP, ip)) {
            throw AuthErrorCode.IP_BLACKLISTED.toServiceException();
        }
        if (phone != null && !phone.isBlank()
                && blacklist.isBlacklisted(BlacklistType.PHONE, phone)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
        if (email != null && !email.isBlank()
                && blacklist.isBlacklisted(BlacklistType.EMAIL, email)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
    }

    /**
     * 账号 ID 黑名单校验。
     *
     * <p>密码登录与所有自定义 grant 在签发 token 前都会经 {@code UserDetailsService} 加载账号，
     * 在该路径统一校验 {@link BlacklistType#ACCOUNT_ID}，
     * 即可覆盖"账号被加黑后不允许再次登录"的场景。
     */
    public void checkAccount(Long accountId) {
        if (accountId != null
                && blacklist.isBlacklisted(BlacklistType.ACCOUNT_ID, String.valueOf(accountId))) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
    }

    /**
     * 微信登录前置校验
     */
    public void checkWechat(String openid, String ip) {
        if (ip != null && blacklist.isBlacklisted(BlacklistType.IP, ip)) {
            throw AuthErrorCode.IP_BLACKLISTED.toServiceException();
        }
        if (openid != null && !openid.isBlank()
                && blacklist.isBlacklisted(BlacklistType.OPENID, openid)) {
            throw AuthErrorCode.IDENTITY_BLACKLISTED.toServiceException();
        }
    }
}
