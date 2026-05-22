package com.eagle.auth.infrastructure.security;

import com.eagle.auth.application.service.AccountApplicationService;
import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.service.SmsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * 短信验证码登录认证提供者（grant_type = sms_code）。
 *
 * <p>具体差异点（其余流程由 {@link AbstractCustomGrantAuthenticationProvider} 接管）：
 * <ol>
 *   <li>IP / PHONE 黑名单前置拦截</li>
 *   <li>短信验证码校验</li>
 *   <li>findOrCreateByPhone 自动注册</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Component
public class SmsCodeAuthenticationProvider extends AbstractCustomGrantAuthenticationProvider {

    private final SmsService smsService;
    private final AccountApplicationService accountApplicationService;
    private final BlacklistChecker blacklistChecker;

    public SmsCodeAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                         OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
                                         UserDetailsService userDetailsService,
                                         SmsService smsService,
                                         AccountApplicationService accountApplicationService,
                                         BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.smsService = smsService;
        this.accountApplicationService = accountApplicationService;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return SmsCodeAuthenticationToken.SMS_CODE;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return SmsCodeAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        SmsCodeAuthenticationToken authToken = (SmsCodeAuthenticationToken) authentication;
        blacklistChecker.checkLogin(null, authToken.getPhone(), ClientIpHolder.get(), null);

        if (!smsService.verifyCode(authToken.getPhone(), authToken.getCode())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_grant", "验证码错误或已过期", null));
        }
        return accountApplicationService.findOrCreateByPhone(authToken.getPhone());
    }
}
