package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.service.AppleIdentityService;
import com.eagle.auth.core.domain.service.AppleIdentityService.AppleIdentity;
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
        AppleIdentity identity = appleIdentityService.verify(
                token.getIdentityToken(), token.getNonce());
        blacklistChecker.checkApple(identity.subject(), ClientIpHolder.get());
        return accountApplicationService.findOrCreateByApple(
                identity.subject(), identity.email(), token.getFullName());
    }
}
