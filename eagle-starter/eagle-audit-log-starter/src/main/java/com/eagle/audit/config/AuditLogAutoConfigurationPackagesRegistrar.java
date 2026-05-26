package com.eagle.audit.config;

import com.eagle.audit.repository.AuditLogRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 把 {@code com.eagle.audit.repository} 追加到 {@link AutoConfigurationPackages} 的基础包列表。
 *
 * <p>替代直接在 starter 上声明 {@code @EnableJpaRepositories(basePackageClasses = AuditLogRepository.class)} —
 * 后者会注册 {@code JpaRepositoryConfigExtension}，触发 Spring Boot {@code JpaRepositoriesAutoConfiguration}
 * 的 {@code @ConditionalOnMissingBean} 判定失败，**关闭默认 JPA 仓储自动扫描**，导致业务方 {@code com.eagle.system.**}
 * 下的所有 {@code JpaRepository} 失踪。
 *
 * <p>本注册器只往 {@code AutoConfigurationPackages} 加包，不注册 {@code JpaRepositoryConfigExtension}，
 * 因此默认 {@code JpaRepositoriesAutoConfiguration} 依然生效，会按"业务方主类所在包 + 本 starter 追加的包"
 * 共同扫描所有 {@code JpaRepository}。
 */
class AuditLogAutoConfigurationPackagesRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                        @NonNull BeanDefinitionRegistry registry) {
        AutoConfigurationPackages.register(registry,
                AuditLogRepository.class.getPackage().getName());
    }
}
