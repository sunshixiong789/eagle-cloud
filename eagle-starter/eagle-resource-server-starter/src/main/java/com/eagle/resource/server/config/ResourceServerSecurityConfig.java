package com.eagle.resource.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 资源服务器安全配置。
 * <p>
 * 不带 {@code @Configuration}，不参与 component scan。
 * 通过 {@link EnableEagleResourceServer} 或 {@link ResourceServerAutoConfiguration} 显式 {@code @Import} 激活。
 *
 * @author 孙士雄
 */
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class ResourceServerSecurityConfig {

    /**
     * 配置资源服务器的安全过滤链。
     *
     * @param http                        HttpSecurity
     * @param eagleJwtAuthenticationConverter JWT 认证转换器
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            EagleJwtAuthenticationConverter eagleJwtAuthenticationConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/swagger-resources/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(eagleJwtAuthenticationConverter)));

        return http.build();
    }
}