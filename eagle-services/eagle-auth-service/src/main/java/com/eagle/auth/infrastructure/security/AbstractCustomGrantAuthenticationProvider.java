package com.eagle.auth.infrastructure.security;

import com.eagle.common.dto.EagleUser;
import com.eagle.auth.domain.AuthErrorCode;
import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.model.enums.AccountStatus;
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

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

/**
 * 自定义 grant_type Provider 的共享骨架。
 *
 * <p>提取自原先 4 个 Provider（短信 / 微信 App / 微信小程序 / 手机号一键登录）的大段重复代码：
 * <ul>
 *   <li>客户端认证校验 + grant_type 白名单</li>
 *   <li>access_token / refresh_token / OIDC id_token 生成</li>
 *   <li>OAuth2Authorization 持久化（含 ClaimsMetadataSanitizer）</li>
 *   <li>账号冻结状态校验 — 强制各 grant 行为一致，避免 #6 类问题</li>
 * </ul>
 *
 * <p>子类只需实现：
 * <ol>
 *   <li>{@link #grantType()} — 当前 grant_type</li>
 *   <li>{@link #authenticationTokenClass()} — 当前 grant 对应的 AuthenticationToken 类</li>
 *   <li>{@link #authenticateGrant(Authentication)} — 校验凭据 + 黑名单 + 返回认证后的 Account</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
public abstract class AbstractCustomGrantAuthenticationProvider implements AuthenticationProvider {

    protected final OAuth2AuthorizationService authorizationService;
    protected final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    protected final UserDetailsService userDetailsService;

    protected AbstractCustomGrantAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            UserDetailsService userDetailsService) {
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 子类暴露当前 grant_type，用于 client 注册校验 + token context。
     */
    protected abstract AuthorizationGrantType grantType();

    /**
     * 子类暴露 token class，用于 supports() 判断。
     */
    protected abstract Class<? extends Authentication> authenticationTokenClass();

    /**
     * 校验该 grant 自身的凭据（短信码 / OAuth code / 一键 access_token），
     * 执行黑名单前置检查，返回查到 / 新建的账号。
     */
    protected abstract Account authenticateGrant(Authentication authentication);

    @Override
    public Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken clientPrincipal = requireAuthenticatedClient(authentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        AuthorizationGrantType grant = grantType();
        if (registeredClient == null
                || !registeredClient.getAuthorizationGrantTypes().contains(grant)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_client"));
        }

        Account account = authenticateGrant(authentication);
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "account_frozen",
                    AuthErrorCode.ACCOUNT_FROZEN.getDefaultMessage(),
                    null));
        }

        EagleUser eagleUser = (EagleUser) userDetailsService.loadUserByUsername(account.getUsername());
        return generateTokens(eagleUser, registeredClient, clientPrincipal, grant);
    }

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return authenticationTokenClass().isAssignableFrom(authentication);
    }

    private OAuth2ClientAuthenticationToken requireAuthenticatedClient(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2ClientAuthenticationToken token
                && token.isAuthenticated()) {
            return token;
        }
        throw new OAuth2AuthenticationException("invalid_client");
    }

    /**
     * 生成 access_token + 可选 refresh_token，附带 OIDC id_token，
     * 持久化到 OAuth2AuthorizationService 后返回标准 OAuth2AccessTokenAuthenticationToken。
     */
    protected OAuth2AccessTokenAuthenticationToken generateTokens(
            EagleUser eagleUser,
            RegisteredClient registeredClient,
            OAuth2ClientAuthenticationToken clientPrincipal,
            AuthorizationGrantType grantType) {
        // OAuth2Authorization 中的 Principal 必须是 SAS Jackson 白名单内类型，
        // /userinfo 等端点反序列化才能通过 PolymorphicTypeValidator。
        UsernamePasswordAuthenticationToken userAuthentication =
                new UsernamePasswordAuthenticationToken(
                        eagleUser.getUsername(), null, eagleUser.getAuthorities());

        DefaultOAuth2TokenContext.Builder ctxBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizationGrantType(grantType)
                .authorizedScopes(registeredClient.getScopes());

        OAuth2Token generatedAccess = tokenGenerator.generate(
                ctxBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build());
        if (generatedAccess == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "server_error", "Failed to generate access token", null));
        }
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccess.getTokenValue(),
                generatedAccess.getIssuedAt(),
                generatedAccess.getExpiresAt(),
                registeredClient.getScopes());

        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes()
                .contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2TokenContext refreshCtx = ctxBuilder
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
            OAuth2Token generatedRefresh = tokenGenerator.generate(refreshCtx);
            if (generatedRefresh != null) {
                refreshToken = (OAuth2RefreshToken) generatedRefresh;
            }
        }

        OAuth2Authorization.Builder authzBuilder = OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .principalName(eagleUser.getUsername())
                .authorizationGrantType(grantType)
                .authorizedScopes(registeredClient.getScopes())
                .attribute(Principal.class.getName(), userAuthentication);

        if (generatedAccess instanceof ClaimAccessor claimAccessor) {
            Map<String, Object> safeClaims = ClaimsMetadataSanitizer.sanitize(claimAccessor.getClaims());
            authzBuilder.token(accessToken, md ->
                    md.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, safeClaims));
        } else {
            authzBuilder.accessToken(accessToken);
        }
        if (refreshToken != null) {
            authzBuilder.refreshToken(refreshToken);
        }

        // openid scope 客户端必须同时签发 OIDC id_token，否则 refresh_token grant
        // 内部生成新 id_token 时会因缺少 OidcIdToken 上下文 NPE。
        OidcIdTokenIssuer.issueIfOpenid(authzBuilder, registeredClient,
                userAuthentication, grantType, tokenGenerator);

        OAuth2Authorization authorization = authzBuilder.build();
        authorizationService.save(authorization);

        log.info("custom grant issued: grant={}, accountId={}, username={}",
                grantType.getValue(), eagleUser.getId(), eagleUser.getUsername());

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, refreshToken,
                Collections.emptyMap());
    }
}
