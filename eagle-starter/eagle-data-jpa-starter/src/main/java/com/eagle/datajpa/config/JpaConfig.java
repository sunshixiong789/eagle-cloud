package com.eagle.datajpa.config;

import com.eagle.common.dto.EagleUser;
import com.eagle.datajpa.properties.JpaProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.Map;
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
     * <p>以 {@link BeanPostProcessor} 方式拦截 {@link AbstractEntityManagerFactoryBean}，
     * 在 bean 初始化前注入批量写入和慢 SQL 阈值等 Hibernate 属性。
     * 已通过 {@code spring.jpa.properties.*} 显式设置的属性优先级更高，不会被覆盖。
     */
    @Bean
    public BeanPostProcessor eagleHibernatePropertiesConfigurer(JpaProperties jpaProperties) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof AbstractEntityManagerFactoryBean emfBean)) {
                    return bean;
                }
                Map<String, Object> defaults = new LinkedHashMap<>();
                if (jpaProperties.getBatchSize() > 0) {
                    defaults.put("hibernate.jdbc.batch_size", String.valueOf(jpaProperties.getBatchSize()));
                    defaults.put("hibernate.order_inserts", String.valueOf(jpaProperties.isOrderInserts()));
                    defaults.put("hibernate.order_updates", String.valueOf(jpaProperties.isOrderUpdates()));
                }
                if (jpaProperties.getSlowQueryThresholdMillis() > 0) {
                    defaults.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS",
                            String.valueOf(jpaProperties.getSlowQueryThresholdMillis()));
                }
                // 已有属性优先，仅补充未被用户覆盖的键
                defaults.keySet().removeAll(emfBean.getJpaPropertyMap().keySet());
                if (!defaults.isEmpty()) {
                    emfBean.setJpaPropertyMap(defaults);
                }
                return emfBean;
            }
        };
    }
}
