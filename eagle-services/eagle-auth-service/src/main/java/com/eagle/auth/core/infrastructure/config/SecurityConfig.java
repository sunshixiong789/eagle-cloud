package com.eagle.auth.core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Auth service security entry configuration.
 *
 * <p>Detailed filter chains, token/JWK beans, and OAuth2 persistence are split into focused
 * configuration classes in this package so each class owns one security concern.
 *
 * @author eagle
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // cost=12：约 250ms / 次（M1 实测），满足 NIST 800-63B 抵御 GPU 暴力破解的要求。
        return new BCryptPasswordEncoder(12);
    }
}
