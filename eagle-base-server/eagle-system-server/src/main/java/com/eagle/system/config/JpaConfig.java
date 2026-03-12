package com.eagle.system.config;

import com.eagle.eagle.common.dto.EagleUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * @author 孙士雄 14:24
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(auth -> auth.getPrincipal() instanceof EagleUser)
                .map(Authentication::getPrincipal)
                .map(value -> ((EagleUser) value).getId())
                .or(() -> Optional.of(0L));
    }
}