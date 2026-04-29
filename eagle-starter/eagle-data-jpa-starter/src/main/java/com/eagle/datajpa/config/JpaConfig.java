package com.eagle.datajpa.config;

import com.eagle.common.dto.EagleUser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA 审计自动配置
 * <p>
 * 自动注入当前登录用户 ID 到 {@code @CreatedBy} / {@code @LastModifiedBy} 审计字段。
 * 仅在 classpath 存在 JPA EntityManager 时激活。
 *
 * @author sunshixiong
 */
@AutoConfiguration
@EnableJpaAuditing
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
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