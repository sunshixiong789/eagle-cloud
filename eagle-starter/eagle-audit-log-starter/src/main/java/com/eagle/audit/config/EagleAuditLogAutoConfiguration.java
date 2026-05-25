package com.eagle.audit.config;

import com.eagle.audit.aspect.AuditLogAspect;
import com.eagle.audit.context.AuditLogUserProvider;
import com.eagle.audit.handler.AuditLogHandler;
import com.eagle.audit.handler.LoggingAuditLogHandler;
import com.eagle.audit.listener.AuditLogEventListener;
import com.eagle.audit.properties.AuditLogProperties;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * 操作审计日志自动配置。
 *
 * <p>注册审计切面、事件监听器和默认日志处理器。
 * 业务方可通过实现 {@link AuditLogHandler} 和 {@link AuditLogUserProvider} 替换默认行为。
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
     * 无用户上下文时降级为匿名。
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
