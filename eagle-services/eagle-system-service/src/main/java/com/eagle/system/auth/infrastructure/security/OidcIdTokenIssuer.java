package com.eagle.system.auth.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Map;

/**
 * 自定义 grant_type（SMS / 微信 / 一键登录等）签发 OIDC ID Token 的共享逻辑。
 * <p>
 * 当 RegisteredClient 的 scopes 包含 {@link OidcScopes#OPENID} 时，必须与 access_token 一并签发
 * OidcIdToken 并写入 OAuth2Authorization。否则 refresh_token grant 在 SAS 内部
 * {@code OAuth2RefreshTokenAuthenticationProvider} 中生成新 ID Token 时会触发 NPE
 * （{@code JwtGenerator} 期望 authorization 中已存在 ID Token 上下文）。
 * <p>
 * ID Token 的 claims 同样走 {@link ClaimsMetadataSanitizer} 净化，避免持久化 metadata 中的
 * Number 类型被 SAS Jackson PolymorphicTypeValidator 拒绝反序列化。
 */
final class OidcIdTokenIssuer {

    private OidcIdTokenIssuer() {
    }

    /**
     * 若 client 授予 openid scope，则生成 ID Token 并追加到 {@code authorizationBuilder}。
     *
     * @param authorizationBuilder 必须已经通过 {@code .token(accessToken, ...)} 写入了 access_token，
     *                             ID Token 生成会以此 build() 后的快照作为 token context 的 authorization
     */
    static void issueIfOpenid(OAuth2Authorization.Builder authorizationBuilder,
                              RegisteredClient registeredClient,
                              Authentication principal,
                              AuthorizationGrantType grantType,
                              OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
        if (!registeredClient.getScopes().contains(OidcScopes.OPENID)) {
            return;
        }

        OAuth2TokenContext idTokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(principal)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorization(authorizationBuilder.build())
                .authorizationGrantType(grantType)
                .authorizedScopes(registeredClient.getScopes())
                .tokenType(new OAuth2TokenType(OidcParameterNames.ID_TOKEN))
                .build();

        OAuth2Token generatedIdToken = tokenGenerator.generate(idTokenContext);
        if (!(generatedIdToken instanceof Jwt idTokenJwt)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "server_error", "Failed to generate id_token", null));
        }

        OidcIdToken idToken = new OidcIdToken(
                idTokenJwt.getTokenValue(),
                idTokenJwt.getIssuedAt(),
                idTokenJwt.getExpiresAt(),
                idTokenJwt.getClaims()
        );
        Map<String, Object> safeIdTokenClaims = ClaimsMetadataSanitizer.sanitize(idTokenJwt.getClaims());
        authorizationBuilder.token(idToken, metadata ->
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, safeIdTokenClaims));
    }
}
