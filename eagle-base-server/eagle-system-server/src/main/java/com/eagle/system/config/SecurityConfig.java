package com.eagle.system.config;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
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
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
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


/**
 * @author 孙士雄 15:24
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SecurityConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .oauth2AuthorizationServer((authorizationServer) -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer
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
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
        http
                // JWT 无状态，必须禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/login", SecurityConstants.AUTH_TOKEN).permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // 指定上面的自定义 HTML 页面路径
                        .loginPage("/login")
                        .permitAll()
                )
                .logout(logout -> logout
                        // 注销后带参数跳转
                        .logoutSuccessUrl(SecurityConstants.TOKEN_LOGOUT)
                        .permitAll()
                );
        return http.build();
    }

    /**
     * 跨域配置，允许所有。生产改为部分跨域
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("POST"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(SecurityConstants.AUTH_TOKEN, config);
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
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .authorizationEndpoint(SecurityConstants.AUTH_AUTHORIZE)
                .tokenEndpoint(SecurityConstants.AUTH_TOKEN)
                .build();
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
                String username = principal.getName();
                EagleUser user = (EagleUser) userDetailsService.loadUserByUsername(username);
                context.getClaims()
                        .claim(SecurityConstants.DETAILS_ROLES, user.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                                .filter(a -> !a.startsWith(SecurityConstants.ROLE_START))
                                .collect(Collectors.toList()))
                        .claim(SecurityConstants.DETAILS_USER_ID, user.getId())
                        .claim(SecurityConstants.DETAILS_USERNAME, user.getUsername())
                        .claim(SecurityConstants.DETAILS_USER_NAME, Objects.requireNonNullElse(user.getName(), ""))
                        .claim(SecurityConstants.DETAILS_DEP_ID, Objects.requireNonNullElse(user.getDeptId(), 0L))
                        .claim(SecurityConstants.DETAILS_DEP_NAME, Objects.requireNonNullElse(user.getDeptName(), ""))
                        .claim(SecurityConstants.DETAILS_PHONE, Objects.requireNonNullElse(user.getPhone(), ""));
            }
        };
    }
}