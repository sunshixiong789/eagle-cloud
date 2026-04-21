package com.eagle.system.config;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.infrastructure.security.BlacklistAwareJwtDecoder;
import com.eagle.system.auth.infrastructure.security.EagleUserAuthenticationToken;
import com.eagle.system.auth.infrastructure.security.LoginRateLimitFilter;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
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
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.eagle.common.constant.SecurityConstants.DETAILS_ROLES;

/**
 * @author 孙士雄 15:24
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SecurityConfig {

    private final LoginRateLimitFilter loginRateLimitFilter;

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
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
                                           SecurityContextRepository securityContextRepository,
                                           TokenTrackingHandler tokenTrackingHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .oauth2AuthorizationServer((authorizationServer) -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer
                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                    .accessTokenRequestConverter(new WechatMiniProgramAuthenticationConverter())
                                    .authenticationProvider(wechatProvider)
                                    .accessTokenRequestConverter(new SmsCodeAuthenticationConverter())
                                    .authenticationProvider(smsProvider)
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

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
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
                            .claim(DETAILS_ROLES, user.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                                    .collect(Collectors.toList()))
                            .claim(SecurityConstants.DETAILS_USER_ID, user.getId())
                            .claim(SecurityConstants.DETAILS_USERNAME, user.getUsername())
                            .claim(SecurityConstants.DETAILS_USER_NAME, Objects.requireNonNullElse(user.getName(), ""))
                            .claim(SecurityConstants.DETAILS_DEP_ID, Objects.requireNonNullElse(user.getDeptId(), 0L))
                            .claim(SecurityConstants.DETAILS_DEP_NAME, Objects.requireNonNullElse(user.getDeptName(), ""))
                            .claim(SecurityConstants.DETAILS_PHONE, Objects.requireNonNullElse(user.getPhone(), ""));
                }
            }
        };
    }
}