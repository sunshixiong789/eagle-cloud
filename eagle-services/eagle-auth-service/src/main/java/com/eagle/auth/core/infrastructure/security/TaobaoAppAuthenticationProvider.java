package com.eagle.auth.core.infrastructure.security;

import com.eagle.auth.core.application.service.AccountApplicationService;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.TaobaoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 淘宝 App 登录认证提供者（grant_type = taobao_app）。
 *
 * <p>编排：解析 openUid → 黑名单 → 查淘宝绑定。命中即直登；未命中直接以 openUid 创建新账号
 * （与微信登录同规则，无需手机号）。
 *
 * @author sunshixiong
 */
@Component
public class TaobaoAppAuthenticationProvider extends AbstractCustomGrantAuthenticationProvider {

    private final TaobaoService taobaoService;
    private final AccountRepository accountRepository;
    private final AccountApplicationService accountApplicationService;
    private final BlacklistChecker blacklistChecker;

    public TaobaoAppAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService,
            TaobaoService taobaoService,
            AccountRepository accountRepository,
            AccountApplicationService accountApplicationService,
            BlacklistChecker blacklistChecker) {
        super(authorizationService, tokenGenerator, userDetailsService);
        this.taobaoService = taobaoService;
        this.accountRepository = accountRepository;
        this.accountApplicationService = accountApplicationService;
        this.blacklistChecker = blacklistChecker;
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return TaobaoAppAuthenticationToken.TAOBAO_APP;
    }

    @Override
    protected Class<? extends Authentication> authenticationTokenClass() {
        return TaobaoAppAuthenticationToken.class;
    }

    @Override
    protected Account authenticateGrant(Authentication authentication) {
        TaobaoAppAuthenticationToken token = (TaobaoAppAuthenticationToken) authentication;
        String openUid = taobaoService.resolveOpenUid(token.getTbAccessToken(), token.getTbAuthCode());
        blacklistChecker.checkTaobao(openUid, ClientIpHolder.get());

        Optional<Account> bound = accountRepository.findByTaobaoBindingOpenUid(openUid);
        if (bound.isPresent()) {
            return bound.get();
        }

        // 新 openUid：直接以淘宝身份创建账号，无需手机号
        return accountApplicationService.findOrCreateByTaobao(openUid);
    }
}
