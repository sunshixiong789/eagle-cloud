package com.eagle.audit.config;

import com.eagle.audit.application.AuditLogApplicationService;
import com.eagle.audit.interfaces.controller.AuditLogController;
import com.eagle.audit.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 审计日志查询 Controller 自动配置。
 *
 * <p>启用条件:
 * <ul>
 *   <li>Servlet Web 应用</li>
 *   <li>类路径有 JPA(查询需 Repository)</li>
 *   <li>{@code eagle.audit-log.controller-enabled=true}(默认 false,避免误暴露)</li>
 * </ul>
 *
 * @author eagle
 */
@AutoConfiguration
@AutoConfigureAfter(EagleAuditLogJpaAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({JpaRepository.class, EntityManager.class})
@ConditionalOnProperty(prefix = "eagle.audit-log", name = "controller-enabled", havingValue = "true")
public class EagleAuditLogControllerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditLogApplicationService auditLogApplicationService(AuditLogRepository repository) {
        return new AuditLogApplicationService(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogController auditLogController(AuditLogApplicationService applicationService) {
        return new AuditLogController(applicationService);
    }
}
