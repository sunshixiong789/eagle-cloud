package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.SocialProvider;
import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import com.eagle.auth.core.domain.service.AppleIdentityService;
import com.eagle.auth.core.domain.service.AppleIdentityService.AppleAuthorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppleAppAuthenticationProviderTest {

    private AppleIdentityService identityService;
    private AccountApplicationService accountService;
    private BindTicketStore bindTicketStore;
    private BlacklistChecker blacklistChecker;
    private AppleAppAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        identityService = mock(AppleIdentityService.class);
        accountService = mock(AccountApplicationService.class);
        bindTicketStore = mock(BindTicketStore.class);
        blacklistChecker = mock(BlacklistChecker.class);
        provider = new AppleAppAuthenticationProvider(
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class),
                mock(UserDetailsService.class),
                identityService, accountService, bindTicketStore, blacklistChecker);
    }

    private AppleAppAuthenticationToken token() {
        return new AppleAppAuthenticationToken(
                "signed-jwt", "apple-auth-code", "nonce-1", "小明",
                new TestingAuthenticationToken("eagleApp", null), Map.of());
    }

    private AppleAuthorization authorization() {
        return new AppleAuthorization(
                "apple-subject-1", "relay@privaterelay.appleid.com", "encrypted-token");
    }

    @Test
    @DisplayName("subject 已挂靠 → 验签+黑名单后直登")
    void boundSubjectLogsInDirectly() {
        Account account = Account.createFromPhone("13800138000");
        account.bindApple("apple-subject-1", "encrypted-token");
        when(identityService.authorize(
                "signed-jwt", "apple-auth-code", "nonce-1")).thenReturn(authorization());
        when(accountService.findByAppleSubject("apple-subject-1", "encrypted-token"))
                .thenReturn(Optional.of(account));

        Account result = provider.authenticateGrant(token());

        assertSame(account, result);
        verify(blacklistChecker).checkApple("apple-subject-1", null);
    }

    @Test
    @DisplayName("subject 未挂靠 → 发放含 Apple 附带信息的 BindTicket 并抛 binding_required")
    void unboundSubjectRequiresBinding() {
        when(identityService.authorize(
                "signed-jwt", "apple-auth-code", "nonce-1")).thenReturn(authorization());
        when(accountService.findByAppleSubject("apple-subject-1", "encrypted-token"))
                .thenReturn(Optional.empty());
        when(bindTicketStore.save(BindTicket.ofApple(
                "apple-subject-1", "relay@privaterelay.appleid.com", "小明", "encrypted-token")))
                .thenReturn("ticket-1");

        SocialBindingRequiredException ex = assertThrows(SocialBindingRequiredException.class,
                () -> provider.authenticateGrant(token()));

        assertEquals("ticket-1", ex.getBindTicket());
        assertEquals(SocialProvider.APPLE, ex.getProvider());
    }
}
