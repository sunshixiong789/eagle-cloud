package com.eagle.resource.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 资源服务器安全配置
 * 配置 OAuth2 资源服务器，使用 JWT 进行身份验证
 *
 * @author 孙士雄
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ResourceServerSecurityConfig {

    /**
     * 配置资源服务器的安全过滤链
     */
    @Bean
    public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（JWT 无状态认证不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置会话管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置授权规则
                .authorizeHttpRequests(authorize -> authorize
                        // 公开端点
                        .requestMatchers("/public/**").permitAll().requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Swagger/OpenAPI 文档
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        // 其他所有请求需要认证
                        .anyRequest().authenticated())
                // 配置 OAuth2 资源服务器
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(eagleJwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * 配置 JWT 认证转换器
     */
    @Bean
    public EagleJwtAuthenticationConverter eagleJwtAuthenticationConverter() {
        return new EagleJwtAuthenticationConverter();
    }
}
