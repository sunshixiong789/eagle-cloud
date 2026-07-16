package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.WechatWebUserService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.SocialProvider;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import com.eagle.auth.core.domain.service.WechatWebService;
import com.eagle.auth.core.domain.service.WechatWebService.WechatWebUserInfo;
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
import static org.mockito.Mockito.when;

class WechatAppAuthenticationProviderTest {

    private WechatWebService wechatWebService;
    private WechatWebUserService wechatWebUserService;
    private BindTicketStore bindTicketStore;
    private BlacklistChecker blacklistChecker;
    private WechatAppAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        wechatWebService = mock(WechatWebService.class);
        wechatWebUserService = mock(WechatWebUserService.class);
        bindTicketStore = mock(BindTicketStore.class);
        blacklistChecker = mock(BlacklistChecker.class);
        provider = new WechatAppAuthenticationProvider(
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class),
                mock(UserDetailsService.class),
                wechatWebService, wechatWebUserService, bindTicketStore, blacklistChecker);
    }

    private WechatAppAuthenticationToken token() {
        return new WechatAppAuthenticationToken("app-code",
                new TestingAuthenticationToken("eagleApp", null), Map.of());
    }

    private WechatWebUserInfo info() {
        return new WechatWebUserInfo("oid-1", "uid-1", "Nick", "https://a.png", "app");
    }

    @Test
    @DisplayName("openid/unionid 命中 → 直登")
    void boundIdentityLogsInDirectly() {
        Account existing = Account.createFromPhone("13800138000");
        when(wechatWebService.exchangeAppCode("app-code")).thenReturn(info());
        when(wechatWebUserService.findWechatAccount(WechatChannel.APP, "oid-1", "uid-1"))
                .thenReturn(Optional.of(existing));

        Account result = provider.authenticateGrant(token());

        assertSame(existing, result);
    }

    @Test
    @DisplayName("未命中 → 发放含昵称头像的 BindTicket 并抛 binding_required")
    void unboundIdentityRequiresBinding() {
        when(wechatWebService.exchangeAppCode("app-code")).thenReturn(info());
        when(wechatWebUserService.findWechatAccount(WechatChannel.APP, "oid-1", "uid-1"))
                .thenReturn(Optional.empty());
        when(bindTicketStore.save(BindTicket.ofWechat(
                WechatChannel.APP, "oid-1", "uid-1", "Nick", "https://a.png")))
                .thenReturn("ticket-1");

        SocialBindingRequiredException ex = assertThrows(SocialBindingRequiredException.class,
                () -> provider.authenticateGrant(token()));

        assertEquals("ticket-1", ex.getBindTicket());
        assertEquals(SocialProvider.WECHAT, ex.getProvider());
    }
}
