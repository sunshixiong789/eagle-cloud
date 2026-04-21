package com.eagle.system.auth.infrastructure.security;

import com.eagle.auth.application.service.AccountApplicationService;
import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.service.WechatService;
import com.eagle.auth.domain.service.WechatService.WechatUserInfo;
import com.eagle.common.dto.EagleUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * 微信小程序登录认证提供者
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatMiniProgramAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final WechatService wechatService;
    private final AccountApplicationService accountApplicationService;

    @Override
    public Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {
        WechatMiniProgramAuthenticationToken authToken = (WechatMiniProgramAuthenticationToken) authentication;

        // 验证客户端
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClient(authToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null ||
                !registeredClient.getAuthorizationGrantTypes().contains(WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_client"));
        }

        // 调用微信 API 获取 openid
        WechatUserInfo wechatUserInfo = wechatService.getUserInfo(authToken.getCode());

        // 查找或自动创建账号（委托给应用服务）
        Account account = accountApplicationService.findOrCreateByWechatOpenid(
                wechatUserInfo.openid(), wechatUserInfo.unionid());

        // 构建 EagleUser 用于 principal
        EagleUser eagleUser = new EagleUser(
                account.getId(), account.getUsername(), "", account.getUsername(),
                null, "", account.getPhone(),
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        // 生成 Token
        return generateTokens(eagleUser, registeredClient, clientPrincipal,
                authToken.getAdditionalParameters());
    }

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return WechatMiniProgramAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private OAuth2AccessTokenAuthenticationToken generateTokens(EagleUser eagleUser,
                                                                 RegisteredClient registeredClient,
                                                                 OAuth2ClientAuthenticationToken clientPrincipal,
                                                                Map<String, Object> additionalParameters) {
        EagleUserAuthenticationToken userAuthentication = new EagleUserAuthenticationToken(eagleUser);

        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizationGrantType(WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM)
                .authorizedScopes(registeredClient.getScopes());

        // 生成 access token
        OAuth2TokenContext tokenContext = tokenContextBuilder
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("server_error", "Failed to generate access token", null));
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(),
                generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(),
                registeredClient.getScopes()
        );

        // 生成 refresh token
        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2TokenContext refreshTokenContext = tokenContextBuilder
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                    .build();
            OAuth2Token generatedRefreshToken = tokenGenerator.generate(refreshTokenContext);
            if (generatedRefreshToken != null) {
                refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
            }
        }

        // 保存授权信息
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(eagleUser.getUsername())
                .authorizationGrantType(WechatMiniProgramAuthenticationToken.WECHAT_MINI_PROGRAM)
                .authorizedScopes(registeredClient.getScopes())
                .attribute(Principal.class.getName(), userAuthentication);

        if (generatedAccessToken instanceof ClaimAccessor claimAccessor) {
            authorizationBuilder.token(accessToken, metadata ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claimAccessor.getClaims()));
        } else {
            authorizationBuilder.accessToken(accessToken);
        }
        if (refreshToken != null) {
            authorizationBuilder.refreshToken(refreshToken);
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, refreshToken,
                Collections.emptyMap()
        );
    }

    private static OAuth2ClientAuthenticationToken getAuthenticatedClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (authentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken token) {
            clientPrincipal = token;
        }
        if (clientPrincipal == null || !clientPrincipal.isAuthenticated()) {
            throw new OAuth2AuthenticationException("invalid_client");
        }
        return clientPrincipal;
    }
}
