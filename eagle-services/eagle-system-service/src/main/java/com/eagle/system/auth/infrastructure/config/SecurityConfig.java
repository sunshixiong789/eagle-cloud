package com.eagle.system.auth.infrastructure.config;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.infrastructure.security.BlacklistAwareJwtDecoder;
import com.eagle.system.auth.infrastructure.security.CustomGrantClientAuthenticationProvider;
import com.eagle.system.auth.infrastructure.security.CustomGrantPublicClientAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.EagleUserAuthenticationToken;
import com.eagle.system.auth.infrastructure.security.JwtKeyProperties;
import com.eagle.system.auth.infrastructure.security.LoginRateLimitFilter;
import com.eagle.system.auth.infrastructure.security.PhoneOneClickAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.PhoneOneClickAuthenticationProvider;
import com.eagle.system.auth.infrastructure.security.SmsCodeAuthenticationConverter;
import com.eagle.system.auth.infrastructure.security.SmsCodeAuthenticationProvider;
import com.eagle.system.auth.infrastructure.security.TokenTrackingHandler;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.eagle.common.constant.SecurityConstants.DETAILS_ROLES;

/**
 * @author 孙士雄 15:24
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
     * OAuth2 授权持久化 — JDBC 实现
     * <p>
     * 替代 {@code InMemoryOAuth2AuthorizationService}，
     * 授权码、访问令牌、刷新令牌等数据持久化到数据库，
     * 服务重启不丢失活跃授权，多实例部署共享授权状态。
     * <p>
     * SAS 7.0.5 默认使用 Jackson 3 的 {@code JsonMapperOAuth2AuthorizationRowMapper}
     * 与 {@code JsonMapperOAuth2AuthorizationParametersMapper}，已内置 {@code java.time.*}
     * 支持，无需自定义 ObjectMapper。
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcOperations jdbcOperations,
                                                           RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }

    /**
     * OAuth2 授权同意持久化 — JDBC 实现
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcOperations jdbcOperations,
                                                                         RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, registeredClientRepository);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(JwtEncoder jwtEncoder,
                                                                      OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtCustomizer);
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }


    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           OAuth2AuthorizationService authorizationService,
                                           OAuth2TokenGenerator<?> tokenGenerator,
                                           WechatMiniProgramAuthenticationProvider wechatProvider,
                                           SmsCodeAuthenticationProvider smsProvider,
                                           PhoneOneClickAuthenticationProvider phoneOneClickProvider,
                                           SecurityContextRepository securityContextRepository,
                                           TokenTrackingHandler tokenTrackingHandler,
                                           RegisteredClientRepository registeredClientRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .oauth2AuthorizationServer((authorizationServer) -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer
                            .clientAuthentication(clientAuth -> clientAuth
                                    .authenticationConverter(new CustomGrantPublicClientAuthenticationConverter())
                                    .authenticationProvider(new CustomGrantClientAuthenticationProvider(registeredClientRepository))
                            )
                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                    .accessTokenRequestConverter(new WechatMiniProgramAuthenticationConverter())
                                    .authenticationProvider(wechatProvider)
                                    .accessTokenRequestConverter(new SmsCodeAuthenticationConverter())
                                    .authenticationProvider(smsProvider)
                                    .accessTokenRequestConverter(new PhoneOneClickAuthenticationConverter())
                                    .authenticationProvider(phoneOneClickProvider)
                                    .accessTokenResponseHandler(tokenTrackingHandler)
                            )
                            .oidc(Customizer.withDefaults());
                })
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .requestMatchers(SecurityConstants.AUTH_TOKEN).permitAll()
                                .anyRequest().authenticated()
                )
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          SecurityContextRepository securityContextRepository,
                                                          BlacklistAwareJwtDecoder jwtDecoder) throws Exception {
        http
                // JWT 无状态，必须禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login", "/login/sms", SecurityConstants.AUTH_TOKEN).permitAll()
                        .requestMatchers("/accounts/register").permitAll()
                        .requestMatchers("/accounts/password/reset").permitAll()
                        .requestMatchers("/sms/code/reset").permitAll()
                        .requestMatchers("/login/reset-password").permitAll()
                        .requestMatchers("/login/bind-phone").permitAll()
                        .requestMatchers("/login/wechat/**").permitAll()
                        .requestMatchers("/sms/code").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("admin")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        // 指定上面的自定义 HTML 页面路径
                        .loginPage("/login")
                        .permitAll()
                )
                .logout(logout -> logout
                        // 注销后带参数跳转
                        .logoutSuccessUrl(SecurityConstants.TOKEN_LOGOUT)
                        .permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
        return http.build();
    }

    /**
     * 跨域配置
     * <p>
     * 当前使用通配符允许所有来源，生产环境请将 setAllowedOriginPatterns 替换为具体域名列表，
     * 例如：{@code List.of("https://app.example.com", "https://admin.example.com")}
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 生产环境替换为具体域名，禁止通配符
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 应用于所有路径，包括 API 接口和认证端点
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 从 PKCS12 密钥库加载 RSA 密钥对
     * <p>
     * 密钥持久化后，服务重启不会导致已签发的 JWT 失效；
     * 多实例部署时所有实例共享同一密钥对，Token 互相认可。
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
        RSAKey rsaKey = RSAKey.load(keyStore, jwtKeyProperties.getKeyAlias(), password);
        log.info("JWT 签名密钥加载完成，alias: {}", jwtKeyProperties.getKeyAlias());
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    /**
     * 将用户信息写入 JWT
     *
     * @param userDetailsService UserDetailsService
     * @return OAuth2TokenCustomizer
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(UserDetailsService userDetailsService) {
        return context -> {
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                Authentication principal = context.getPrincipal();
                EagleUser user;

                // 自定义 grant type（微信/短信）已经在 Provider 中构建了 EagleUser
                if (principal instanceof EagleUserAuthenticationToken eagleAuth) {
                    user = (EagleUser) eagleAuth.getPrincipal();
                } else {
                    // 标准授权码流程，通过 UserDetailsService 加载
                    String username = principal.getName();
                    user = (EagleUser) userDetailsService.loadUserByUsername(username);
                }
                if (user != null) {
                    context.getClaims()
                            // JWT claim 中存业务角色 code（不含 ROLE_ 前缀），
                            // 由资源服务器的 EagleJwtAuthenticationConverter 统一补 Spring Security 框架前缀，
                            // 防止双重前缀导致 hasRole('admin') 校验失败。
                            .claim(DETAILS_ROLES, user.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                                    .map(a -> a.startsWith(SecurityConstants.ROLE_START)
                                            ? a.substring(SecurityConstants.ROLE_START.length())
                                            : a)
                                    .collect(Collectors.toList()))
                            .claim(SecurityConstants.DETAILS_USER_ID, user.getId())
                            .claim(SecurityConstants.DETAILS_USERNAME, user.getUsername())
                            .claim(SecurityConstants.DETAILS_USER_NAME, Objects.requireNonNullElse(user.getName(), ""))
                            .claim(SecurityConstants.DETAILS_DEPT_ID, Objects.requireNonNullElse(user.getDeptId(), 0L))
                            .claim(SecurityConstants.DETAILS_DEPT_NAME, Objects.requireNonNullElse(user.getDeptName(), ""))
                            .claim(SecurityConstants.DETAILS_PHONE, Objects.requireNonNullElse(user.getPhone(), ""));
                }
            }
        };
    }
}