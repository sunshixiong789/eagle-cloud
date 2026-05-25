package com.eagle.datajpa.config;

import com.eagle.common.dto.EagleUser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA 审计填充器：仅在 Spring Security 存在时启用。
 *
 * <p>从 {@link SecurityContextHolder} 提取当前用户 ID 写入 {@code @CreatedBy}/{@code @LastModifiedBy}。
 * 未登录场景（定时任务、系统内部调用）回退到 {@code 0L}，避免审计字段为 null。
 *
 * <p>消费方未引入 {@code spring-security-core} 时整个类不会被加载，避免 {@link NoClassDefFoundError}。
 *
 * @author eagle
 */
@AutoConfiguration(after = JpaConfig.class)
@ConditionalOnClass(Authentication.class)
public class SecurityAuditorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> auth.getPrincipal() instanceof EagleUser)
                .map(auth -> ((EagleUser) auth.getPrincipal()).getId())
                .or(() -> Optional.of(0L));
    }
}
