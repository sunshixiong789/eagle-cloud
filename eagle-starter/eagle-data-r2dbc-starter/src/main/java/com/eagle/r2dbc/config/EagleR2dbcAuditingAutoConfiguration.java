package com.eagle.r2dbc.config;

import com.eagle.common.dto.EagleUser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import java.util.Objects;

/**
 * R2DBC 审计字段自动填充。
 *
 * <p>启用 {@code @EnableR2dbcAuditing}，注册默认 {@link ReactiveAuditorAware}：
 * 从响应式 {@code SecurityContext} 提取 {@link EagleUser#getId()} 写入
 * {@code @CreatedBy}/{@code @LastModifiedBy}。
 *
 * <p>未登录场景（定时任务、系统内部调用）回退到 {@code 0L}，避免审计字段为 null。
 *
 * <p>消费方未引入 {@code spring-security-core} 时整个类不会被加载。
 *
 * @author eagle
 */
@AutoConfiguration(after = EagleR2dbcAutoConfiguration.class)
@ConditionalOnClass({Authentication.class, ReactiveAuditorAware.class})
@EnableR2dbcAuditing(auditorAwareRef = "reactiveAuditorAware")
public class EagleR2dbcAuditingAutoConfiguration {

    /**
     * 默认 ReactiveAuditorAware：从响应式 SecurityContext 提取当前用户 ID。
     *
     * <p>未登录 / Principal 非 {@link EagleUser} / SecurityContext 缺失时返回 {@code 0L}，
     * 避免审计字段为 null。
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveAuditorAware<Long> reactiveAuditorAware() {
        return () -> ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .filter(auth -> auth.getPrincipal() instanceof EagleUser)
                .map(auth -> ((EagleUser) Objects.requireNonNull(auth.getPrincipal())).getId())
                .defaultIfEmpty(0L)
                .onErrorReturn(0L);
    }
}
