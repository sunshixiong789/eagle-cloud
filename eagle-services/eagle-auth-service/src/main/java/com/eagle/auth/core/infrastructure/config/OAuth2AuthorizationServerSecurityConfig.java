package com.eagle.auth.core.infrastructure.config;

import com.eagle.auth.core.infrastructure.security.BlacklistAwareJwtDecoder;
import com.eagle.auth.core.infrastructure.security.CustomGrantClientAuthenticationProvider;
import com.eagle.auth.core.infrastructure.security.CustomGrantPublicClientAuthenticationConverter;
import com.eagle.auth.core.infrastructure.security.PhoneOneClickAuthenticationConverter;
import com.eagle.auth.core.infrastructure.security.PhoneOneClickAuthenticationProvider;
import com.eagle.auth.core.infrastructure.security.SmsCodeAuthenticationConverter;
import com.eagle.auth.core.infrastructure.security.SmsCodeAuthenticationProvider;
import com.eagle.auth.core.infrastructure.security.TaobaoAppAuthenticationConverter;
import com.eagle.auth.core.infrastructure.security.TaobaoAppAuthenticationProvider;
import com.eagle.auth.core.infrastructure.security.TokenTrackingHandler;
import com.eagle.auth.core.infrastructure.security.WechatAppAuthenticationConverter;
import com.eagle.auth.core.infrastructure.security.WechatAppAuthenticationProvider;
import com.eagle.auth.core.infrastructure.security.WechatMiniProgramAuthenticationConverter;
import com.eagle.auth.core.infrastructure.security.WechatMiniProgramAuthenticationProvider;
import com.eagle.common.constant.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenEndpointConfigurer;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * OAuth2 Authorization Server endpoint security.
 */
@Configuration
public class OAuth2AuthorizationServerSecurityConfig {

    /**
     * OAuth2 Authorization Server 端点的 SecurityFilterChain。
     *
     * <p>自定义 grant_type（短信 / 微信 / 一键登录）的 4 套 Converter + Provider
     * 通过 {@link #registerCustomGrants} 抽出，避免链式 DSL 单行不可读。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            WechatAppAuthenticationProvider wechatAppProvider,
            WechatMiniProgramAuthenticationProvider wechatProvider,
            SmsCodeAuthenticationProvider smsProvider,
            PhoneOneClickAuthenticationProvider phoneOneClickProvider,
            TaobaoAppAuthenticationProvider taobaoAppProvider,
            SecurityContextRepository securityContextRepository,
            TokenTrackingHandler tokenTrackingHandler,
            RegisteredClientRepository registeredClientRepository,
            AuthorizationServerSettings authorizationServerSettings,
            BlacklistAwareJwtDecoder jwtDecoder) throws Exception {

        OAuth2AuthorizationServerConfigurer authServer = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(authServer.getEndpointsMatcher())
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .with(authServer, server -> server
                        .clientAuthentication(clientAuth -> clientAuth
                                .authenticationConverter(new CustomGrantPublicClientAuthenticationConverter(
                                        authorizationServerSettings.getTokenRevocationEndpoint()))
                                .authenticationProvider(
                                        new CustomGrantClientAuthenticationProvider(registeredClientRepository)))
                        .tokenEndpoint(tokenEndpoint ->
                                registerCustomGrants(tokenEndpoint, wechatAppProvider, wechatProvider,
                                        smsProvider, phoneOneClickProvider, taobaoAppProvider, tokenTrackingHandler))
                        .oidc(oidc -> oidc.userInfoEndpoint(userInfo -> userInfo
                                .userInfoMapper(OAuth2AuthorizationServerSecurityConfig::mapUserInfoFromIdToken))))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SecurityConstants.AUTH_TOKEN).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                // OIDC /userinfo / introspection / revoke 端点需要 Bearer access_token；
                // oauth2AuthorizationServer().oidc(Customizer.withDefaults()) 不会注册
                // BearerTokenAuthenticationFilter——必须在 @Order(1) chain 显式启用 Resource Server，
                // 否则 OidcUserInfoAuthenticationProvider 拿不到 accessTokenAuthentication。
                //
                // 此处不能用 EagleJwtAuthenticationConverter:
                // 它返回的 EagleAuthentication 继承自 AbstractAuthenticationToken,principal 是 EagleUser。
                // 而 SAS 的 OidcUserInfoAuthenticationProvider 要求 principal 必须是
                // AbstractOAuth2TokenAuthenticationToken 子类(默认 JwtAuthenticationToken),否则直接
                // 抛 INVALID_TOKEN。/api/* 业务端点(@Order(2) chain)才需要 EagleAuthentication
                // 让 @PreAuthorize SpEL 直接访问 authentication.principal.id。
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.decoder(jwtDecoder)));

        return http.build();
    }

    /**
     * /userinfo 响应的 claim 来源 mapper:从 id_token 取全部 claim,剥掉 JWT 协议字段后返回。
     *
     * <p>替换 SAS 默认 {@code DEFAULT_USER_INFO_MAPPER}——后者通过 {@code getClaimsRequestedByScope}
     * 仅放行 OIDC Core 标准 claim(sub / name / picture / phone_number / email / address ...),
     * 把 {@link com.eagle.common.constant.SecurityConstants#DETAILS_ROLES roles} 和
     * {@link com.eagle.common.constant.SecurityConstants#DETAILS_USER_ID id} 这类非标准业务 claim
     * 全部丢弃,导致前端拿不到角色信息。
     *
     * <p>这里直接拷贝 id_token 全量 claim 作为 /userinfo 响应,业务 claim 由
     * JWT customizer 统一写入,两侧字段对齐。仅过滤掉 JWT 协议字段
     * (iss/aud/exp/iat/nbf/jti/azp/at_hash/nonce/scope),它们对前端无意义且可能泄漏内部信息。
     */
    private static OidcUserInfo mapUserInfoFromIdToken(OidcUserInfoAuthenticationContext context) {
        OAuth2Authorization authorization = context.getAuthorization();
        OidcIdToken idToken = Objects.requireNonNull(authorization.getToken(OidcIdToken.class)).getToken();
        Map<String, Object> claims = new HashMap<>(idToken.getClaims());
        claims.keySet().removeAll(JWT_PROTOCOL_CLAIMS);
        return new OidcUserInfo(claims);
    }

    /**
     * /userinfo 响应里需要剥离的 JWT 协议字段——它们对前端无意义,且可能泄漏 issuer / token 内部标识。
     */
    private static final Set<String> JWT_PROTOCOL_CLAIMS = Set.of(
            "iss", "aud", "exp", "iat", "nbf", "jti", "azp", "at_hash", "nonce", "scope");

    /**
     * 在 token endpoint 上注册自定义 grant_type 的 Converter + Provider。
     */
    private void registerCustomGrants(OAuth2TokenEndpointConfigurer tokenEndpoint,
                                      WechatAppAuthenticationProvider wechatAppProvider,
                                      WechatMiniProgramAuthenticationProvider wechatProvider,
                                      SmsCodeAuthenticationProvider smsProvider,
                                      PhoneOneClickAuthenticationProvider phoneOneClickProvider,
                                      TaobaoAppAuthenticationProvider taobaoAppProvider,
                                      TokenTrackingHandler tokenTrackingHandler) {
        tokenEndpoint
                .accessTokenRequestConverter(new WechatAppAuthenticationConverter())
                .authenticationProvider(wechatAppProvider)
                .accessTokenRequestConverter(new WechatMiniProgramAuthenticationConverter())
                .authenticationProvider(wechatProvider)
                .accessTokenRequestConverter(new SmsCodeAuthenticationConverter())
                .authenticationProvider(smsProvider)
                .accessTokenRequestConverter(new PhoneOneClickAuthenticationConverter())
                .authenticationProvider(phoneOneClickProvider)
                .accessTokenRequestConverter(new TaobaoAppAuthenticationConverter())
                .authenticationProvider(taobaoAppProvider)
                .accessTokenResponseHandler(tokenTrackingHandler);
    }
}
