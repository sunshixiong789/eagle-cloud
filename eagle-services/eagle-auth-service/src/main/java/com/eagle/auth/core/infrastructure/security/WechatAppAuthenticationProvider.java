package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.WechatWebUserService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.service.WechatWebService;
import com.eagle.auth.core.domain.service.WechatWebService.WechatWebUserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * 微信 App 登录认证提供者（grant_type = wechat_app）。
 *
 * @author sunshixiong
 */
@Component
public class WechatAppAuthenticationProvider
        extends AbstractCustomGrantAuthenticationProvider {

    private final WechatWebService wechatWebService;
    private final WechatWebUserService wechatWebUserService;
    private final BlacklistChecker blacklistChecker;

    public WechatAppAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            WechatWebService wechatWebService,
            WechatWebUserService wechatWebUserService,
            BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.wechatWebService = wechatWebService;
        this.wechatWebUserService = wechatWebUserService;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return WechatAppAuthenticationToken.WECHAT_APP;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return WechatAppAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        WechatAppAuthenticationToken authToken = (WechatAppAuthenticationToken) authentication;
        WechatWebUserInfo info = wechatWebService.exchangeAppCode(authToken.getCode());
        blacklistChecker.checkWechat(info.openid(), ClientIpHolder.get());
        return wechatWebUserService.findOrCreateWechatWebAccount(info);
    }
}
