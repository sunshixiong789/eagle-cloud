package com.eagle.audit.config;

import com.eagle.audit.handler.AuditLogHandler;
import com.eagle.audit.handler.JpaAuditLogHandler;
import com.eagle.audit.model.AuditLogRecord;
import com.eagle.audit.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 审计日志 JPA 持久化自动配置。
 *
 * <p>类路径有 JPA 时启用:
 * <ul>
 *   <li>{@code @EntityScan} 把 {@link AuditLogRecord} 注册到默认持久化单元</li>
 *   <li>{@code @EnableJpaRepositories} 启用 {@link AuditLogRepository}
 *       (basePackages 限定 starter 自己的 repository 包,不干扰业务方默认扫描)</li>
 *   <li>{@link JpaAuditLogHandler} 覆盖主配置中的 {@link com.eagle.audit.handler.LoggingAuditLogHandler}</li>
 * </ul>
 *
 * @author eagle
 */
@AutoConfiguration
@AutoConfigureBefore(EagleAuditLogAutoConfiguration.class)
@ConditionalOnClass({JpaRepository.class, EntityManager.class})
@EntityScan(basePackageClasses = AuditLogRecord.class)
@EnableJpaRepositories(basePackageClasses = AuditLogRepository.class)
public class EagleAuditLogJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditLogHandler.class)
    public AuditLogHandler jpaAuditLogHandler(AuditLogRepository repository,
                                              @Value("${spring.application.name:unknown}") String serviceId) {
        return new JpaAuditLogHandler(repository, serviceId);
    }
}
