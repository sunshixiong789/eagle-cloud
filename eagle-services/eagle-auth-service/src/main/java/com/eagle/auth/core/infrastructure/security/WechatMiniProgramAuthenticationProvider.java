package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.WechatWebUserService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.SocialProvider;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.port.BindTicket;
import com.eagle.auth.core.domain.port.BindTicketStore;
import com.eagle.auth.core.domain.service.WechatService;
import com.eagle.auth.core.domain.service.WechatService.WechatUserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

/**
 * 微信小程序登录认证提供者（grant_type = wechat_mini_program）。
 *
 * <p>openid / unionid 命中 → 直登（unionid 命中补绑小程序 openid，
 * 同一微信主体不重复验手机号）；未命中 → 发放 BindTicket 抛
 * {@code binding_required}，客户端走 {@code social_bind} 挂靠到手机号主账号。
 *
 * @author sunshixiong
 */
@Component
public class WechatMiniProgramAuthenticationProvider
        extends AbstractCustomGrantAuthenticationProvider {

    private final WechatService wechatService;
    private final WechatWebUserService wechatWebUserService;
    private final BindTicketStore bindTicketStore;
    private final BlacklistChecker blacklistChecker;

    public WechatMiniProgramAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            WechatService wechatService,
            WechatWebUserService wechatWebUserService,
            BindTicketStore bindTicketStore,
            BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.wechatService = wechatService;
        this.wechatWebUserService = wechatWebUserService;
        this.bindTicketStore = bindTicketStore;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return WechatMiniProgramAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        WechatMiniProgramAuthenticationToken authToken =
                (WechatMiniProgramAuthenticationToken) authentication;
        WechatUserInfo info = wechatService.getUserInfo(authToken.getCode());
        blacklistChecker.checkWechat(info.openid(), ClientIpHolder.get());
        return wechatWebUserService.findWechatAccount(
                        WechatChannel.MINI_PROGRAM, info.openid(), info.unionid())
                .orElseThrow(() -> new SocialBindingRequiredException(
                        bindTicketStore.save(BindTicket.ofWechat(
                                WechatChannel.MINI_PROGRAM, info.openid(), info.unionid(),
                                null, null)),
                        SocialProvider.WECHAT));
    }
}
