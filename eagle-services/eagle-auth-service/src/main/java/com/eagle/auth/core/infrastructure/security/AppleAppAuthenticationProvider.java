package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.service.AppleIdentityService;
import com.eagle.auth.core.domain.service.AppleIdentityService.AppleAuthorization;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/** Apple App 登录认证提供者（grant_type = apple_app）。 */
@Component
public class AppleAppAuthenticationProvider extends AbstractCustomGrantAuthenticationProvider {

    private final AppleIdentityService appleIdentityService;
    private final AccountApplicationService accountApplicationService;
    private final BlacklistChecker blacklistChecker;

    public AppleAppAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            AppleIdentityService appleIdentityService,
            AccountApplicationService accountApplicationService,
            BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.appleIdentityService = appleIdentityService;
        this.accountApplicationService = accountApplicationService;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return AppleAppAuthenticationToken.APPLE_APP;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return AppleAppAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        AppleAppAuthenticationToken token = (AppleAppAuthenticationToken) authentication;
        AppleAuthorization authorization = appleIdentityService.authorize(
                token.getIdentityToken(), token.getAuthorizationCode(), token.getNonce());
        blacklistChecker.checkApple(authorization.subject(), ClientIpHolder.get());
        return accountApplicationService.findOrCreateByApple(
                authorization.subject(), authorization.email(), token.getFullName(),
                authorization.encryptedRefreshToken());
    }
}
