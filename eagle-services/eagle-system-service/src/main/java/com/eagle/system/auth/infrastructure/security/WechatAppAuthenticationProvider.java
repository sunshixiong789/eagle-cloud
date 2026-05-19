package com.eagle.system.auth.infrastructure.security;

import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.application.service.WechatWebUserService;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.service.WechatWebService;
import com.eagle.system.auth.domain.service.WechatWebService.WechatWebUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
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
 * 微信 App 登录认证提供者。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatAppAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final WechatWebService wechatWebService;
    private final WechatWebUserService wechatWebUserService;
    private final UserDetailsService userDetailsService;

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

    @Override
    public Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {
        WechatAppAuthenticationToken authToken = (WechatAppAuthenticationToken) authentication;

        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClient(authToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null ||
                !registeredClient.getAuthorizationGrantTypes().contains(WechatAppAuthenticationToken.WECHAT_APP)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_client"));
        }

        WechatWebUserInfo wechatUserInfo = wechatWebService.exchangeAppCode(authToken.getCode());

        Account account = wechatWebUserService.findOrCreateWechatWebAccount(wechatUserInfo);

        EagleUser eagleUser = (EagleUser) userDetailsService.loadUserByUsername(account.getUsername());

        return generateTokens(eagleUser, registeredClient, clientPrincipal,
                authToken.getAdditionalParameters());
    }

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return WechatAppAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private OAuth2AccessTokenAuthenticationToken generateTokens(EagleUser eagleUser,
                                                                RegisteredClient registeredClient,
                                                                OAuth2ClientAuthenticationToken clientPrincipal,
                                                                Map<String, Object> additionalParameters) {
        // OAuth2Authorization 中的 Principal 必须是 SAS Jackson 白名单内的类型，
        // 否则 /userinfo 等回读授权信息的端点反序列化会被 PolymorphicTypeValidator 拒绝。
        // 这里只持久化 username，业务扩展字段由 jwtTokenCustomizer 经 UserDetailsService 二次加载写入 JWT claims。
        UsernamePasswordAuthenticationToken userAuthentication =
                new UsernamePasswordAuthenticationToken(eagleUser.getUsername(), null, eagleUser.getAuthorities());

        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizationGrantType(WechatAppAuthenticationToken.WECHAT_APP)
                .authorizedScopes(registeredClient.getScopes());

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

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(eagleUser.getUsername())
                .authorizationGrantType(WechatAppAuthenticationToken.WECHAT_APP)
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
}
