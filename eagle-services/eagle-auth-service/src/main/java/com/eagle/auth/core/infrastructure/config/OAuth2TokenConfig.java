package com.eagle.auth.core.infrastructure.config;

import com.eagle.auth.core.infrastructure.security.JwtKeyProperties;
import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.eagle.common.constant.SecurityConstants.DETAILS_ROLES;

/**
 * JWT signing, JWK publishing, and token customization.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OAuth2TokenConfig {

    private final JwtKeyProperties jwtKeyProperties;

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(
            JwtEncoder jwtEncoder, OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtCustomizer);
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator,
                new OAuth2AccessTokenGenerator(),
                new OAuth2RefreshTokenGenerator());
    }

    /**
     * 从 PKCS12 密钥库加载 RSA 密钥对。
     *
     * <p>密钥持久化后，服务重启不会导致已签发的 JWT 失效；多实例部署时所有实例共享同一密钥对，
     * Token 互相认可。支持多 key 滚动：当前 keystore 中有 N 个 alias 时全部加载到 JWKSet，
     * 签名使用配置的 active alias；旧 key 仍可校验未过期的存量 token。
     *
     * @return JWKSource
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = jwtKeyProperties.getKeystorePassword().toCharArray();
        try (InputStream is = jwtKeyProperties.getKeystoreLocation().getInputStream()) {
            keyStore.load(is, password);
        }
        JWKSet jwkSet = JwtKeySetLoader.loadAll(keyStore, password, jwtKeyProperties);
        log.info("JWT 签名密钥加载完成，active alias={}, total keys={}",
                jwtKeyProperties.getKeyAlias(), jwkSet.size());
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * 锁定 issuer，避免随反向代理 / 前端入口的 X-Forwarded-Host 漂移。
     *
     * <p>显式调用 {@code .issuer(...)} 后，Spring Authorization Server 不再从请求头派生 issuer，
     * 所有客户端拿到的 token 的 {@code iss} claim 都是同一个稳定 URL。
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings(OAuthServerProperties properties) {
        log.info("OAuth2 Authorization Server issuer locked to: {}", properties.getIssuer());
        return AuthorizationServerSettings.builder().issuer(properties.getIssuer()).build();
    }

    /**
     * 将用户信息写入 JWT。
     *
     * <p>同时写 access_token 与 id_token：
     * <ul>
     *   <li>access_token：下游资源服务器 {@code EagleJwtAuthenticationConverter} 从 claim 重建 EagleUser</li>
     *   <li>id_token：OIDC {@code /userinfo} 端点默认从 id_token claim 派生响应——业务字段（avatar/phone/userName/roles）
     *       不写 id_token 时，userinfo 仅能返回 {@code sub}。这里统一写两边，让前端用 access_token 调 {@code /userinfo}
     *       即可拿到完整用户信息</li>
     * </ul>
     *
     * @param userDetailsService UserDetailsService
     * @return OAuth2TokenCustomizer
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
            UserDetailsService userDetailsService) {
        return context -> {
            OAuth2TokenType tokenType = context.getTokenType();
            boolean isAccessToken = OAuth2TokenType.ACCESS_TOKEN.equals(tokenType);
            boolean isIdToken = OidcParameterNames.ID_TOKEN.equals(tokenType.getValue());
            if (!isAccessToken && !isIdToken) {
                return;
            }
            Authentication principal = context.getPrincipal();
            // 所有 grant type 的 Principal 在 OAuth2Authorization 中均只持久化 username（SAS Jackson 白名单兼容），
            // 此处统一经 UserDetailsService 二次加载，把业务扩展字段写入 JWT claims；
            // 下游资源服务器由 EagleJwtAuthenticationConverter 从 claims 重建 EagleUser。
            EagleUser user = (EagleUser) userDetailsService.loadUserByUsername(principal.getName());
            context.getClaims()
                    // JWT claim 中存业务角色 code（不含 ROLE_ 前缀），
                    // 由资源服务器统一补 Spring Security 框架前缀，防止双重前缀导致 hasRole 校验失败。
                    .claim(DETAILS_ROLES, user.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(Objects::nonNull)
                            .map(a -> a.startsWith(SecurityConstants.ROLE_START)
                                    ? a.substring(SecurityConstants.ROLE_START.length()) : a)
                            .collect(Collectors.toList()))
                    .claim(SecurityConstants.DETAILS_USER_ID, user.getId())
                    .claim(SecurityConstants.DETAILS_USERNAME, user.getUsername())
                    .claim(SecurityConstants.DETAILS_USER_NAME, Objects.requireNonNullElse(user.getName(), ""))
                    .claim(SecurityConstants.DETAILS_PHONE, Objects.requireNonNullElse(user.getPhone(), ""))
                    .claim(SecurityConstants.DETAILS_AVATAR, Objects.requireNonNullElse(user.getAvatar(), ""));
        };
    }
}
