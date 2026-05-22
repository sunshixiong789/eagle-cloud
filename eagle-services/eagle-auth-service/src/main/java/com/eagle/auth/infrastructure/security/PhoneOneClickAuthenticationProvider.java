package com.eagle.auth.infrastructure.security;

import com.eagle.auth.application.service.AccountApplicationService;
import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.service.PhoneOneClickService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * 手机号一键登录认证提供者（grant_type = phone_one_click）。
 *
 * <p>调用运营商 SDK 校验 access_token 换取真实手机号后，复用短信登录的
 * findOrCreateByPhone 自动注册逻辑。补黑名单前置校验，与短信路径一致。
 *
 * @author sunshixiong
 */
@Component
public class PhoneOneClickAuthenticationProvider extends AbstractCustomGrantAuthenticationProvider {

    private final PhoneOneClickService phoneOneClickService;
    private final AccountApplicationService accountApplicationService;
    private final BlacklistChecker blacklistChecker;

    public PhoneOneClickAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            PhoneOneClickService phoneOneClickService,
            AccountApplicationService accountApplicationService,
            BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.phoneOneClickService = phoneOneClickService;
        this.accountApplicationService = accountApplicationService;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return PhoneOneClickAuthenticationToken.PHONE_ONE_CLICK;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return PhoneOneClickAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        PhoneOneClickAuthenticationToken authToken = (PhoneOneClickAuthenticationToken) authentication;
        String phone = phoneOneClickService.verifyAndGetPhone(authToken.getAccessToken());
        blacklistChecker.checkLogin(null, phone, ClientIpHolder.get(), null);
        return accountApplicationService.findOrCreateByPhone(phone);
    }
}
