package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.WechatWebUserService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.SocialProvider;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import com.eagle.auth.core.domain.service.WechatService;
import com.eagle.auth.core.domain.service.WechatService.WechatUserInfo;
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

class WechatMiniProgramAuthenticationProviderTest {

    private WechatService wechatService;
    private WechatWebUserService wechatWebUserService;
    private BindTicketStore bindTicketStore;
    private BlacklistChecker blacklistChecker;
    private WechatMiniProgramAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        wechatService = mock(WechatService.class);
        wechatWebUserService = mock(WechatWebUserService.class);
        bindTicketStore = mock(BindTicketStore.class);
        blacklistChecker = mock(BlacklistChecker.class);
        provider = new WechatMiniProgramAuthenticationProvider(
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class),
                mock(UserDetailsService.class),
                wechatService, wechatWebUserService, bindTicketStore, blacklistChecker);
    }

    private WechatMiniProgramAuthenticationToken token() {
        return new WechatMiniProgramAuthenticationToken("js-code",
                new TestingAuthenticationToken("eagleApp", null), Map.of());
    }

    @Test
    @DisplayName("openid/unionid 命中 → 直登（含 unionid 跨渠道归并）")
    void boundIdentityLogsInDirectly() {
        Account existing = Account.createFromPhone("13800138000");
        when(wechatService.getUserInfo("js-code"))
                .thenReturn(new WechatUserInfo("oid-1", "uid-1", "sk"));
        when(wechatWebUserService.findWechatAccount(
                WechatChannel.MINI_PROGRAM, "oid-1", "uid-1"))
                .thenReturn(Optional.of(existing));

        Account result = provider.authenticateGrant(token());

        assertSame(existing, result);
        verify(blacklistChecker).checkWechat("oid-1", null);
    }

    @Test
    @DisplayName("未命中 → 发放 BindTicket 并抛 binding_required")
    void unboundIdentityRequiresBinding() {
        when(wechatService.getUserInfo("js-code"))
                .thenReturn(new WechatUserInfo("oid-1", "uid-1", "sk"));
        when(wechatWebUserService.findWechatAccount(
                WechatChannel.MINI_PROGRAM, "oid-1", "uid-1"))
                .thenReturn(Optional.empty());
        when(bindTicketStore.save(BindTicket.ofWechat(
                WechatChannel.MINI_PROGRAM, "oid-1", "uid-1", null, null)))
                .thenReturn("ticket-1");

        SocialBindingRequiredException ex = assertThrows(SocialBindingRequiredException.class,
                () -> provider.authenticateGrant(token()));

        assertEquals("ticket-1", ex.getBindTicket());
        assertEquals(SocialProvider.WECHAT, ex.getProvider());
    }
}
