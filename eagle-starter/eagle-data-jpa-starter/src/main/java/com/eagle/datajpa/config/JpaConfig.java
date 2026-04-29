package com.eagle.datajpa.config;

import com.eagle.common.dto.EagleUser;
import com.eagle.datajpa.properties.JpaProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA 自动配置。
 *
 * <ul>
 *   <li>开启 JPA 审计，从 Spring Security 上下文自动填充 {@code @CreatedBy}/{@code @LastModifiedBy}</li>
 *   <li>根据 {@link JpaProperties} 配置 Hibernate 批量写入、慢 SQL 阈值等参数</li>
 * </ul>
 *
 * @author 孙士雄
 */
@AutoConfiguration
@EnableJpaAuditing
@ConditionalOnClass(name = "jakarta.persistence.EntityManager")
@EnableConfigurationProperties(JpaProperties.class)
public class JpaConfig {

    /**
     * 审计填充器：从 Spring Security 上下文提取当前用户 ID。
     *
     * <p>未登录（定时任务、系统内部调用）时回退到 {@code 0L}，
     * 避免 {@code @CreatedBy} 字段产生 null 值。
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> auth.getPrincipal() instanceof EagleUser)
                .map(auth -> ((EagleUser) auth.getPrincipal()).getId())
                .or(() -> Optional.of(0L));
    }

    /**
     * 将 {@link JpaProperties} 中的配置应用到 Hibernate SessionFactory。
     *
     * <p>等效于在 {@code application.yml} 中设置：
     * <pre>
     * spring.jpa.properties:
     *   hibernate.jdbc.batch_size: 100
     *   hibernate.order_inserts: true
     *   hibernate.order_updates: true
     *   hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS: 2000
     * </pre>
     * 通过此 bean 统一管理，消费方只需配置 {@code eagle.jpa.*} 即可，
     * 无需在每个服务的 yml 里重复写原生 Hibernate 属性。
     */
    @Bean
    public HibernatePropertiesCustomizer eagleHibernatePropertiesCustomizer(
            JpaProperties jpaProperties) {
        return hibernateProperties -> {
            // 批量写入：相同类型的 INSERT/UPDATE 合并为一次 JDBC batch，减少数据库往返次数
            if (jpaProperties.getBatchSize() > 0) {
                hibernateProperties.put("hibernate.jdbc.batch_size",
                        String.valueOf(jpaProperties.getBatchSize()));
                hibernateProperties.put("hibernate.order_inserts",
                        String.valueOf(jpaProperties.isOrderInserts()));
                hibernateProperties.put("hibernate.order_updates",
                        String.valueOf(jpaProperties.isOrderUpdates()));
            }
            // 慢 SQL 日志：超过阈值的查询以 WARN 级别记录
            if (jpaProperties.getSlowQueryThresholdMillis() > 0) {
                hibernateProperties.put(
                        "hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS",
                        String.valueOf(jpaProperties.getSlowQueryThresholdMillis()));
            }
        };
    }
}
