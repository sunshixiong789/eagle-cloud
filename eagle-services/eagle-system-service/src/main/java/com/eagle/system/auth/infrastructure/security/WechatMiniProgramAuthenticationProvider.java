package com.eagle.system.auth.infrastructure.security;

import com.eagle.system.auth.application.service.AccountApplicationService;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.service.WechatService;
import com.eagle.system.auth.domain.service.WechatService.WechatUserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * 微信小程序登录认证提供者（grant_type = wechat_mini_program）。
 *
 * @author sunshixiong
 */
@Component
public class WechatMiniProgramAuthenticationProvider
        extends AbstractCustomGrantAuthenticationProvider {

    private final WechatService wechatService;
    private final AccountApplicationService accountApplicationService;
    private final BlacklistChecker blacklistChecker;

    public WechatMiniProgramAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            WechatService wechatService,
            AccountApplicationService accountApplicationService,
            BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.wechatService = wechatService;
        this.accountApplicationService = accountApplicationService;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return WechatMiniProgramAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        WechatMiniProgramAuthenticationToken authToken =
                (WechatMiniProgramAuthenticationToken) authentication;
        WechatUserInfo info = wechatService.getUserInfo(authToken.getCode());
        blacklistChecker.checkWechat(info.openid(), ClientIpHolder.get());
        return accountApplicationService.findOrCreateByWechatOpenid(info.openid(), info.unionid());
    }
}
