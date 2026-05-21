package com.eagle.resource.server.config;

import com.eagle.resource.server.properties.ResourceServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 资源服务器安全配置。
 *
 * <p>不带 {@code @Configuration}，不参与 component scan。
 * 通过 {@link EnableEagleResourceServer} 或 {@link ResourceServerAutoConfiguration} 显式 {@code @Import} 激活。
 *
 * <p>默认放行路径：
 * <ul>
 *   <li>{@code /public/**} — 公开接口约定路径</li>
 *   <li>{@code /actuator/health}、{@code /actuator/info} — 健康检查</li>
 *   <li>Swagger UI 相关路径</li>
 * </ul>
 *
 * <p>可通过 {@code eagle.resource-server.permit-paths} 追加额外放行路径。
 *
 * @author 孙士雄
 */
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, SecurityFilterChain.class})
@RequiredArgsConstructor
public class ResourceServerSecurityConfig {

    private static final String[] DEFAULT_PERMIT_PATHS = {
            "/public/**",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private final ResourceServerProperties properties;

    /**
     * 配置资源服务器的无状态安全过滤链。
     *
     * @param http                            HttpSecurity
     * @param eagleJwtAuthenticationConverter JWT 认证转换器
     * @return SecurityFilterChain
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            EagleJwtAuthenticationConverter eagleJwtAuthenticationConverter) throws Exception {

        String[] permitPaths = buildPermitPaths();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(permitPaths).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(eagleJwtAuthenticationConverter)));

        return http.build();
    }

    /**
     * 合并默认白名单与用户自定义白名单。
     */
    private String[] buildPermitPaths() {
        List<String> all = new ArrayList<>(Arrays.asList(DEFAULT_PERMIT_PATHS));
        List<String> extra = properties.getPermitPaths();
        if (extra != null && !extra.isEmpty()) {
            all.addAll(extra);
        }
        return all.toArray(String[]::new);
    }
}
