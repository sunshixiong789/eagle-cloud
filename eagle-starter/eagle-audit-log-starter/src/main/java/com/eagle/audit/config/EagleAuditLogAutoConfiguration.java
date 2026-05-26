package com.eagle.audit.config;

import com.eagle.audit.aspect.AuditLogAspect;
import com.eagle.audit.context.AuditLogUserProvider;
import com.eagle.audit.handler.AuditLogHandler;
import com.eagle.audit.handler.LoggingAuditLogHandler;
import com.eagle.audit.listener.AuditLogEventListener;
import com.eagle.audit.properties.AuditLogProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * 操作审计日志主自动配置(切面 + 事件 + 兜底 Handler/Provider)。
 *
 * <p>更高阶能力由独立的自动配置启用:
 * <ul>
 *   <li>{@link EagleAuditLogJpaAutoConfiguration} — JPA 持久化(类路径有 JpaRepository)</li>
 *   <li>{@link EagleAuditLogSecurityAutoConfiguration} — Security 用户上下文(类路径有 SecurityContextHolder + JWT)</li>
 *   <li>{@link EagleAuditLogControllerAutoConfiguration} — 查询 Controller({@code eagle.audit-log.controller-enabled=true})</li>
 * </ul>
 *
 * <p>嵌套优先级:Jpa/Security 自动配置标注 {@code @AutoConfigureBefore},
 * 确保它们的 Bean 在主配置兜底 Bean 之前注册,@ConditionalOnMissingBean 才会按预期跳过兜底。
 *
 * @author eagle
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@EnableConfigurationProperties(AuditLogProperties.class)
public class EagleAuditLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditLogHandler auditLogHandler() {
        return new LoggingAuditLogHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogUserProvider auditLogUserProvider() {
        return new AnonymousAuditLogUserProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogAspect auditLogAspect(AuditLogProperties properties,
                                         ApplicationEventPublisher eventPublisher,
                                         ObjectMapper objectMapper,
                                         AuditLogUserProvider userProvider) {
        return new AuditLogAspect(properties, eventPublisher, objectMapper, userProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogEventListener auditLogEventListener(AuditLogHandler handler) {
        return new AuditLogEventListener(handler);
    }

    /**
     * 无用户上下文时降级为匿名(Spring Security 不可用时兜底)。
     */
    static class AnonymousAuditLogUserProvider implements AuditLogUserProvider {
        @Override
        public String getCurrentUserId() {
            return "anonymous";
        }

        @Override
        public String getCurrentUserName() {
            return "anonymous";
        }
    }
}
