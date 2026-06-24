package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.SmsService;
import com.eagle.auth.core.domain.service.TaobaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaobaoAppAuthenticationProviderTest {

    private TaobaoService taobaoService;
    private AccountRepository accountRepository;
    private AccountApplicationService accountApplicationService;
    private SmsService smsService;
    private BlacklistChecker blacklistChecker;
    private TaobaoAppAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        taobaoService = mock(TaobaoService.class);
        accountRepository = mock(AccountRepository.class);
        accountApplicationService = mock(AccountApplicationService.class);
        smsService = mock(SmsService.class);
        blacklistChecker = mock(BlacklistChecker.class);
        provider = new TaobaoAppAuthenticationProvider(
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class),
                mock(UserDetailsService.class),
                taobaoService, accountRepository, accountApplicationService,
                smsService, blacklistChecker);
    }

    private TaobaoAppAuthenticationToken token(String phone, String smsCode) {
        // authenticateGrant() 只读取 tbAuthCode/phone/smsCode，不访问 principal；
        // 但父类 OAuth2AuthorizationGrantAuthenticationToken 要求 clientPrincipal 非空，
        // 故用 TestingAuthenticationToken 占位（计划草稿中的 null 在父类校验下会抛 IllegalArgumentException）。
        return new TaobaoAppAuthenticationToken("acc", "authcode", phone, smsCode,
                new TestingAuthenticationToken("eagleApp", null), java.util.Map.of());
    }

    @Test
    @DisplayName("openUid 已绑账号 → 直接返回该账号（老用户直登）")
    void returningUserLogsInWithoutPhone() {
        Account existing = Account.createFromPhone("13800138000");
        when(taobaoService.resolveOpenUid("acc", "authcode")).thenReturn("uid-1");
        when(accountRepository.findByTaobaoBindingOpenUid("uid-1")).thenReturn(Optional.of(existing));

        Account result = provider.authenticateGrant(token(null, null));

        assertSame(existing, result);
    }

    @Test
    @DisplayName("新 openUid 且无手机号 → 抛 PHONE_BINDING_REQUIRED")
    void newUserWithoutPhoneRequiresBinding() {
        when(taobaoService.resolveOpenUid("acc", "authcode")).thenReturn("uid-2");
        when(accountRepository.findByTaobaoBindingOpenUid("uid-2")).thenReturn(Optional.empty());

        OAuth2AuthenticationException ex = assertThrows(OAuth2AuthenticationException.class,
                () -> provider.authenticateGrant(token(null, null)));
        assertEquals(AuthErrorCode.PHONE_BINDING_REQUIRED.getMessageKey(), ex.getError().getDescription());
    }

    @Test
    @DisplayName("新 openUid + 验证码错 → 抛 invalid_grant")
    void newUserWrongSmsCodeRejected() {
        when(taobaoService.resolveOpenUid("acc", "authcode")).thenReturn("uid-3");
        when(accountRepository.findByTaobaoBindingOpenUid("uid-3")).thenReturn(Optional.empty());
        when(smsService.verifyCode("13800138000", "000000")).thenReturn(false);

        assertThrows(OAuth2AuthenticationException.class,
                () -> provider.authenticateGrant(token("13800138000", "000000")));
    }

    @Test
    @DisplayName("新 openUid + 验证码对 → 按手机号合并并绑定淘宝")
    void newUserBindsPhoneAndTaobao() {
        Account merged = Account.createFromPhone("13800138000");
        when(taobaoService.resolveOpenUid("acc", "authcode")).thenReturn("uid-4");
        when(accountRepository.findByTaobaoBindingOpenUid("uid-4")).thenReturn(Optional.empty());
        when(smsService.verifyCode("13800138000", "123456")).thenReturn(true);
        when(accountApplicationService.findOrCreateByPhone("13800138000")).thenReturn(merged);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = provider.authenticateGrant(token("13800138000", "123456"));

        assertEquals("uid-4", result.getTaobaoBinding().getOpenUid());
        verify(accountRepository).save(merged);
    }
}
