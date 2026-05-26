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
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 审计日志 JPA 持久化自动配置。
 *
 * <p>类路径有 JPA 时启用:
 * <ul>
 *   <li>{@code @EntityScan} 把 {@link AuditLogRecord} 注册到默认持久化单元</li>
 *   <li>{@link AuditLogAutoConfigurationPackagesRegistrar} 把 {@code com.eagle.audit.repository} 追加到
 *       {@code AutoConfigurationPackages}，让 Spring Boot 默认 {@code JpaRepositoriesAutoConfiguration}
 *       连同业务方主类所在包一并扫描到 {@link AuditLogRepository}。这里**不**直接用
 *       {@code @EnableJpaRepositories}，否则会注册 {@code JpaRepositoryConfigExtension} 进而抑制默认自动扫描，
 *       导致业务方 {@code JpaRepository} 集体失踪</li>
 *   <li>{@link JpaAuditLogHandler} 覆盖主配置中的 {@link com.eagle.audit.handler.LoggingAuditLogHandler}</li>
 * </ul>
 *
 * @author eagle
 */
@AutoConfiguration
@AutoConfigureBefore(EagleAuditLogAutoConfiguration.class)
@ConditionalOnClass({JpaRepository.class, EntityManager.class})
@EntityScan(basePackageClasses = AuditLogRecord.class)
@Import(AuditLogAutoConfigurationPackagesRegistrar.class)
public class EagleAuditLogJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditLogHandler.class)
    public AuditLogHandler jpaAuditLogHandler(AuditLogRepository repository,
                                              @Value("${spring.application.name:unknown}") String serviceId) {
        return new JpaAuditLogHandler(repository, serviceId);
    }
}
