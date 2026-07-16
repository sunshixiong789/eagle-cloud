package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.service.AppleIdentityService;
import com.eagle.auth.core.domain.service.AppleIdentityService.AppleAuthorization;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppleAppAuthenticationProviderTest {

    @Test
    void verifiesIdentityBeforeFindingOrCreatingAccount() {
        AppleIdentityService identityService = mock(AppleIdentityService.class);
        AccountApplicationService accountService = mock(AccountApplicationService.class);
        BlacklistChecker blacklistChecker = mock(BlacklistChecker.class);
        AppleAppAuthenticationProvider provider = new AppleAppAuthenticationProvider(
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class),
                mock(UserDetailsService.class),
                identityService, accountService, blacklistChecker);
        AppleAppAuthenticationToken token = new AppleAppAuthenticationToken(
                "signed-jwt", "apple-auth-code", "nonce-1", "小明",
                new TestingAuthenticationToken("eagleApp", null), Map.of());
        AppleAuthorization authorization = new AppleAuthorization(
                "apple-subject-1", "relay@privaterelay.appleid.com", "encrypted-token");
        Account account = Account.createFromApple(
                authorization.subject(), authorization.email(), "小明", "encrypted-token");
        when(identityService.authorize(
                "signed-jwt", "apple-auth-code", "nonce-1")).thenReturn(authorization);
        when(accountService.findOrCreateByApple(
                authorization.subject(), authorization.email(), "小明", "encrypted-token"))
                .thenReturn(account);

        Account result = provider.authenticateGrant(token);

        assertSame(account, result);
        verify(blacklistChecker).checkApple("apple-subject-1", null);
    }
}
