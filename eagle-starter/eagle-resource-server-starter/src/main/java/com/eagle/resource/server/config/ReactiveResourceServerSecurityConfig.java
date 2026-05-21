package com.eagle.resource.server.config;

import com.eagle.resource.server.properties.ResourceServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reactive resource server security configuration.
 */
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({ServerHttpSecurity.class, SecurityWebFilterChain.class})
@RequiredArgsConstructor
public class ReactiveResourceServerSecurityConfig {

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
     * Configures stateless JWT resource server security for WebFlux applications.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityWebFilterChain.class)
    public SecurityWebFilterChain resourceServerSecurityWebFilterChain(
            ServerHttpSecurity http,
            EagleJwtAuthenticationConverter eagleJwtAuthenticationConverter) {

        String[] permitPaths = buildPermitPaths();

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(permitPaths).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(
                                        eagleJwtAuthenticationConverter))))
                .build();
    }

    private String[] buildPermitPaths() {
        List<String> all = new ArrayList<>(Arrays.asList(DEFAULT_PERMIT_PATHS));
        List<String> extra = properties.getPermitPaths();
        if (extra != null && !extra.isEmpty()) {
            all.addAll(extra);
        }
        return all.toArray(String[]::new);
    }
}
