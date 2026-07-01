package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.TaobaoService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaobaoAppAuthenticationProviderTest {

    private TaobaoService taobaoService;
    private AccountRepository accountRepository;
    private AccountApplicationService accountApplicationService;
    private BlacklistChecker blacklistChecker;
    private TaobaoAppAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        taobaoService = mock(TaobaoService.class);
        accountRepository = mock(AccountRepository.class);
        accountApplicationService = mock(AccountApplicationService.class);
        blacklistChecker = mock(BlacklistChecker.class);
        provider = new TaobaoAppAuthenticationProvider(
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class),
                mock(UserDetailsService.class),
                taobaoService, accountRepository, accountApplicationService,
                blacklistChecker);
    }

    private TaobaoAppAuthenticationToken token() {
        // authenticateGrant() 只读取 tbAccessToken/tbAuthCode，不访问 principal；
        // 但父类 OAuth2AuthorizationGrantAuthenticationToken 要求 clientPrincipal 非空，
        // 故用 TestingAuthenticationToken 占位。
        return new TaobaoAppAuthenticationToken("acc", "authcode", null, null,
                new TestingAuthenticationToken("eagleApp", null), Map.of());
    }

    @Test
    @DisplayName("openUid 已绑账号 → 直接返回该账号（老用户直登）")
    void returningUserLogsInDirectly() {
        Account existing = Account.createFromPhone("13800138000");
        when(taobaoService.resolveOpenUid("acc", "authcode")).thenReturn("uid-1");
        when(accountRepository.findByTaobaoBindingOpenUid("uid-1")).thenReturn(Optional.of(existing));

        Account result = provider.authenticateGrant(token());

        assertSame(existing, result);
    }

    @Test
    @DisplayName("新 openUid → 直接以淘宝身份创建账号，无需手机号（新用户直登）")
    void newUserLogsInDirectlyWithoutPhone() {
        Account created = Account.createFromTaobao("uid-2");
        when(taobaoService.resolveOpenUid("acc", "authcode")).thenReturn("uid-2");
        when(accountRepository.findByTaobaoBindingOpenUid("uid-2")).thenReturn(Optional.empty());
        when(accountApplicationService.findOrCreateByTaobao("uid-2")).thenReturn(created);

        Account result = provider.authenticateGrant(token());

        assertSame(created, result);
        assertEquals("uid-2", result.getTaobaoBinding().getOpenUid());
    }
}
