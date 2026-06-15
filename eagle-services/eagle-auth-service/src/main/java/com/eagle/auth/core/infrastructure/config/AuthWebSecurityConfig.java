package com.eagle.auth.core.infrastructure.config;

import com.eagle.auth.core.infrastructure.security.BlacklistAwareJwtDecoder;
import com.eagle.auth.core.infrastructure.security.LoginRateLimitFilter;
import com.eagle.common.constant.SecurityConstants;
import com.eagle.resource.server.config.EagleJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Business and web endpoint security for auth-service.
 */
@Configuration
@RequiredArgsConstructor
public class AuthWebSecurityConfig {

    /**
     * 表单登录入口（含 Thymeleaf 登录页）。
     */
    private static final String[] FORM_LOGIN_PATHS = {
            "/login", "/login/sms", SecurityConstants.AUTH_TOKEN
    };
    /**
     * 注册 / 密码找回 / 短信发送 / 微信扫码回调 等业务入口公开路径。
     *
     * <p>auth-service 用独立的两个 SecurityFilterChain（@Order 1 + @Order 2），不走
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
    /**
     * 静态资源 / Swagger / Actuator health 等基础设施公开路径。
     */
    private static final String[] INFRASTRUCTURE_PUBLIC_PATHS = {
            "/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/swagger-resources/**", "/webjars/**",
            "/actuator/health"
    };
    /**
     * 跨服务内部 API 路径,供同集群其他服务直连。本服务侧仅 permitAll;
     * <strong>必须</strong>叠加网关 {@code InternalPathBlockingGlobalFilter}(字面 + URL decode 双重过滤)
     * 与部署期网络隔离(K8s NetworkPolicy / 安全组限制 9090 端口)构成完整防御。
     * 详见 {@code agent-plugin/rules/12-security.md}。
     */
    private static final String[] INTERNAL_PATHS = {
            "/internal/**"
    };

    private final LoginRateLimitFilter loginRateLimitFilter;

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            BlacklistAwareJwtDecoder jwtDecoder,
            EagleJwtAuthenticationConverter jwtAuthenticationConverter,
            UserDetailsService userDetailsService,
            RememberMeProperties rememberMeProperties) throws Exception {
        http
                // JWT 无状态，必须禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(FORM_LOGIN_PATHS).permitAll()
                        .requestMatchers(BUSINESS_PUBLIC_PATHS).permitAll()
                        .requestMatchers(INTERNAL_PATHS).permitAll()
                        // SockJS 探针 (/ws-stomp/info) 是普通 HTTP GET，浏览器 XHR 无法注入自定义 header，
                        // 必须放行 HTTP 层鉴权；连接鉴权由 STOMP CONNECT 帧的 ChannelInterceptor 负责。
                        .requestMatchers("/ws-stomp/**").permitAll()
                        .requestMatchers(INFRASTRUCTURE_PUBLIC_PATHS).permitAll()
                        .requestMatchers("/actuator/**").hasRole("admin")
                        .anyRequest().authenticated())
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.loginPage("/login").permitAll())
                // 登录页"记住我"勾选（input name="remember-me"，Spring 默认参数名）签发 hash-token Cookie：
                // 会话过期 / 关闭浏览器后由 RememberMeAuthenticationFilter 凭该 Cookie 自动重建认证态。
                // key 必须稳定且集群一致（详见 RememberMeProperties），否则 Cookie 互相无法校验 → 静默失效。
                .rememberMe(remember -> remember
                        .key(rememberMeProperties.getKey())
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds((int) rememberMeProperties.getValidity().toSeconds()))
                .logout(logout -> logout.logoutSuccessUrl(SecurityConstants.TOKEN_LOGOUT).permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }
}
