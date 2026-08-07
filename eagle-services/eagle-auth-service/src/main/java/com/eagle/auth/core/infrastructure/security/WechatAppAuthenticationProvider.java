package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.WechatWebUserApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.SocialProvider;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import com.eagle.auth.core.domain.service.WechatWebService;
import com.eagle.auth.core.domain.service.WechatWebService.WechatWebUserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * 微信 App 登录认证提供者（grant_type = wechat_app）。
 *
 * <p>openid / unionid 命中 → 直登（unionid 命中补绑本渠道 openid）；
 * 未命中 → 发放 BindTicket 抛 {@code binding_required}，
 * 客户端走 {@code social_bind} 挂靠到手机号主账号。
 *
 * @author sunshixiong
 */
@Component
public class WechatAppAuthenticationProvider
        extends AbstractCustomGrantAuthenticationProvider {

    private final WechatWebService wechatWebService;
    private final WechatWebUserApplicationService wechatWebUserApplicationService;
    private final BindTicketStore bindTicketStore;
    private final BlacklistChecker blacklistChecker;

    public WechatAppAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            WechatWebService wechatWebService,
            WechatWebUserApplicationService wechatWebUserApplicationService,
            BindTicketStore bindTicketStore,
            BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.wechatWebService = wechatWebService;
        this.wechatWebUserApplicationService = wechatWebUserApplicationService;
        this.bindTicketStore = bindTicketStore;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return WechatAppAuthenticationToken.WECHAT_APP;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return WechatAppAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        WechatAppAuthenticationToken authToken = (WechatAppAuthenticationToken) authentication;
        WechatWebUserInfo info = wechatWebService.exchangeAppCode(authToken.getCode());
        blacklistChecker.checkWechat(info.openid(), ClientIpHolder.get());
        return wechatWebUserApplicationService.findWechatAccount(
                        WechatChannel.APP, info.openid(), info.unionid())
                .orElseThrow(() -> new SocialBindingRequiredException(
                        bindTicketStore.save(BindTicket.ofWechat(
                                WechatChannel.APP, info.openid(), info.unionid(),
                                info.nickname(), info.avatar())),
                        SocialProvider.WECHAT));
    }
}
