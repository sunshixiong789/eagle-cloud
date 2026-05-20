package com.eagle.system.auth.infrastructure.config;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.resource.server.config.EagleJwtAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.BlacklistAwareJwtDecoder;
import com.eagle.system.auth.infrastructure.security.CustomGrantClientAuthenticationProvider;
import com.eagle.system.auth.infrastructure.security.CustomGrantPublicClientAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.JwtKeyProperties;
import com.eagle.system.auth.infrastructure.security.LoginRateLimitFilter;
import com.eagle.system.auth.infrastructure.security.PhoneOneClickAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.PhoneOneClickAuthenticationProvider;
import com.eagle.system.auth.infrastructure.security.SmsCodeAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.SmsCodeAuthenticationProvider;
import com.eagle.system.auth.infrastructure.security.TokenTrackingHandler;
import com.eagle.system.auth.infrastructure.security.WechatAppAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.WechatAppAuthenticationProvider;
import com.eagle.system.auth.infrastructure.security.WechatMiniProgramAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.WechatMiniProgramAuthenticationProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenEndpointConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.eagle.common.constant.SecurityConstants.DETAILS_ROLES;

/**
 * @author 孙士雄
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SecurityConfig {

    private final LoginRateLimitFilter loginRateLimitFilter;
    private final JwtKeyProperties jwtKeyProperties;

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * OAuth2 授权持久化 — JDBC 实现。
     *
     * <p>替代 {@code InMemoryOAuth2AuthorizationService}，授权码、访问令牌、
     * 刷新令牌等数据持久化到数据库，服务重启不丢失活跃授权，多实例部署共享授权状态。
     *
     * <p>SAS 7.0.5 默认使用 Jackson 3 的 {@code JsonMapperOAuth2AuthorizationRowMapper}
     * 与 {@code JsonMapperOAuth2AuthorizationParametersMapper}，已内置 {@code java.time.*}
     * 支持，无需自定义 ObjectMapper。
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }

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
            SecurityContextRepository securityContextRepository,
            TokenTrackingHandler tokenTrackingHandler,
            RegisteredClientRepository registeredClientRepository,
            BlacklistAwareJwtDecoder jwtDecoder,
            EagleJwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        OAuth2AuthorizationServerConfigurer authServer = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(authServer.getEndpointsMatcher())
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .with(authServer, server -> server
                        .clientAuthentication(clientAuth -> clientAuth
                                .authenticationConverter(new CustomGrantPublicClientAuthenticationConverter())
                                .authenticationProvider(
                                        new CustomGrantClientAuthenticationProvider(registeredClientRepository)))
                        .tokenEndpoint(tokenEndpoint ->
                                registerCustomGrants(tokenEndpoint, wechatAppProvider, wechatProvider,
                                        smsProvider, phoneOneClickProvider, tokenTrackingHandler))
                        .oidc(Customizer.withDefaults()))
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
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    /**
     * 在 token endpoint 上注册自定义 grant_type 的 Converter + Provider。
     */
    private void registerCustomGrants(OAuth2TokenEndpointConfigurer tokenEndpoint,
                                      WechatAppAuthenticationProvider wechatAppProvider,
                                      WechatMiniProgramAuthenticationProvider wechatProvider,
                                      SmsCodeAuthenticationProvider smsProvider,
                                      PhoneOneClickAuthenticationProvider phoneOneClickProvider,
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
                .accessTokenResponseHandler(tokenTrackingHandler);
    }

    /**
     * 表单登录入口（含 Thymeleaf 登录页）。
     */
    private static final String[] FORM_LOGIN_PATHS = {
            "/login", "/login/sms", SecurityConstants.AUTH_TOKEN
    };

    /**
     * 注册 / 密码找回 / 短信发送 / 微信扫码回调 等业务入口公开路径。
     *
     * <p>system-service 用独立的两个 SecurityFilterChain（@Order 1 + @Order 2），不走
     * {@code eagle-resource-server-starter} 的自动 chain，因此 {@code eagle.resource-server.permit-paths}
     * 在此处不生效；公开路径以该常量为单一来源，避免散落在多处字符串。
     */
    private static final String[] BUSINESS_PUBLIC_PATHS = {
            "/accounts/register",
            "/accounts/password/reset",
            "/sms/code",
            "/sms/code/reset",
            "/login/reset-password",
            "/login/bind-phone",
            "/login/wechat/**",
            "/public/**"
    };

    /** 静态资源 / Swagger / Actuator health 等基础设施公开路径。 */
    private static final String[] INFRASTRUCTURE_PUBLIC_PATHS = {
            "/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/swagger-resources/**", "/webjars/**",
            "/actuator/health"
    };

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            BlacklistAwareJwtDecoder jwtDecoder,
            EagleJwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                // JWT 无状态，必须禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(FORM_LOGIN_PATHS).permitAll()
                        .requestMatchers(BUSINESS_PUBLIC_PATHS).permitAll()
                        // SockJS 探针 (/ws-stomp/info) 是普通 HTTP GET，浏览器 XHR 无法注入自定义 header，
                        // 必须放行 HTTP 层鉴权；连接鉴权由 STOMP CONNECT 帧的 ChannelInterceptor 负责。
                        .requestMatchers("/ws-stomp/**").permitAll()
                        .requestMatchers(INFRASTRUCTURE_PUBLIC_PATHS).permitAll()
                        .requestMatchers("/actuator/**").hasRole("admin")
                        .anyRequest().authenticated())
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout.logoutSuccessUrl(SecurityConstants.TOKEN_LOGOUT).permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
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
     * @param userDetailsService UserDetailsService
     * @return OAuth2TokenCustomizer
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
            UserDetailsService userDetailsService) {
        return context -> {
            if (!context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
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
                    .claim(SecurityConstants.DETAILS_PHONE, Objects.requireNonNullElse(user.getPhone(), ""));
        };
    }
}
