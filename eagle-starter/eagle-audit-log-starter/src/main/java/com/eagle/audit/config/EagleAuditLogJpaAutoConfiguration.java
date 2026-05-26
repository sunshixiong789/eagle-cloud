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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 审计日志 JPA 持久化自动配置。
 *
 * <p>类路径有 JPA 时启用：
 * <ul>
 *   <li>{@link AuditLogAutoConfigurationPackagesRegistrar} 把审计 entity 包
 *       （{@link AuditLogRecord} 所在）和 repository 包（{@link AuditLogRepository} 所在）
 *       追加到 {@code AutoConfigurationPackages}，让 Spring Boot 默认的实体扫描
 *       与仓库扫描连同业务方主类所在包一并覆盖审计相关类。</li>
 *   <li>{@link JpaAuditLogHandler} 覆盖主配置中的 {@link com.eagle.audit.handler.LoggingAuditLogHandler}。</li>
 * </ul>
 *
 * <p><b>设计要点：</b>这里既不直接用 {@code @EnableJpaRepositories}
 * （会注册 {@code JpaRepositoryConfigExtension} 抑制默认仓库扫描，导致业务方仓库失踪），
 * 也不直接用 {@code @EntityScan}
 * （会把 {@code EntityScanPackages} 写为非空，从而屏蔽业务方实体的 fallback 扫描，
 * 导致业务方 {@code @Entity} 报 "Not a managed type"）。
 * 统一改走 {@code AutoConfigurationPackages} 注册——这是 Spring Boot 默认仓库扫描和
 * 实体扫描共同的 fallback 数据源。
 *
 * @author eagle
 */
@AutoConfiguration
@AutoConfigureBefore(EagleAuditLogAutoConfiguration.class)
@ConditionalOnClass({JpaRepository.class, EntityManager.class})
@Import(AuditLogAutoConfigurationPackagesRegistrar.class)
public class EagleAuditLogJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditLogHandler.class)
    public AuditLogHandler jpaAuditLogHandler(AuditLogRepository repository,
                                              @Value("${spring.application.name:unknown}") String serviceId) {
        return new JpaAuditLogHandler(repository, serviceId);
    }
}
