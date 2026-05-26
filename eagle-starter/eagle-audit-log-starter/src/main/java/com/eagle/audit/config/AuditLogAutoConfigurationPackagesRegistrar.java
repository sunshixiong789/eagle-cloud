package com.eagle.audit.config;

import com.eagle.audit.model.AuditLogRecord;
import com.eagle.audit.repository.AuditLogRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 把审计日志的 entity 包与 repository 包追加到 {@link AutoConfigurationPackages}。
 *
 * <p>同时承担两件事：
 * <ul>
 *   <li>repository 包让 Spring Boot 默认 {@code JpaRepositoriesAutoConfiguration} 扫描到
 *       {@link AuditLogRepository}（替代 {@code @EnableJpaRepositories}——后者会注册
 *       {@code JpaRepositoryConfigExtension}，关闭默认仓库自动扫描，导致业务方
 *       {@code JpaRepository} 集体失踪）。</li>
 *   <li>entity 包让 Spring Boot 默认 {@code JpaBaseConfiguration#getPackagesToScan()}
 *       发现 {@link AuditLogRecord}（替代 starter 上的 {@code @EntityScan}——后者会把
 *       {@code EntityScanPackages} 写为非空，从而完全屏蔽业务方实体的 fallback 扫描，
 *       导致业务方 {@code @Entity}（如 {@code User}）报 "Not a managed type"）。</li>
 * </ul>
 *
 * <p><b>防御性检查：</b>若业务方 {@code @SpringBootApplication} 主类已经放在审计包的
 * 祖先包（比如主类位于 {@code com.eagle} 而审计包是 {@code com.eagle.audit.repository}），
 * 则祖先包递归扫描已经能覆盖审计类，再追加子包会触发同一仓库被同一次扫描两次注册，
 * 抛出 {@code BeanDefinitionOverrideException}。这种情况下本注册器自动跳过对应子包。
 */
class AuditLogAutoConfigurationPackagesRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String AUTO_CONFIGURATION_PACKAGES_BEAN =
            AutoConfigurationPackages.class.getName();

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                        @NonNull BeanDefinitionRegistry registry) {
        registerIfNotCovered(registry, AuditLogRepository.class.getPackage().getName());
        registerIfNotCovered(registry, AuditLogRecord.class.getPackage().getName());
    }

    private static void registerIfNotCovered(BeanDefinitionRegistry registry, String candidate) {
        if (alreadyCovered(registry, candidate)) {
            return;
        }
        AutoConfigurationPackages.register(registry, candidate);
    }

    /**
     * 检测 {@link AutoConfigurationPackages} 中是否已有等于或祖先于 {@code candidate} 的包，
     * 用于防止同一审计类被祖先包与子包各扫到一次的双重注册问题。
     */
    private static boolean alreadyCovered(BeanDefinitionRegistry registry, String candidate) {
        if (!registry.containsBeanDefinition(AUTO_CONFIGURATION_PACKAGES_BEAN)) {
            return false;
        }
        BeanDefinition definition = registry.getBeanDefinition(AUTO_CONFIGURATION_PACKAGES_BEAN);
        ConstructorArgumentValues args = definition.getConstructorArgumentValues();
        if (!args.hasIndexedArgumentValue(0)) {
            return false;
        }
        ValueHolder holder = args.getIndexedArgumentValue(0, String[].class);
        if (holder == null || !(holder.getValue() instanceof String[] existing)) {
            return false;
        }
        for (String pkg : existing) {
            if (candidate.equals(pkg) || candidate.startsWith(pkg + ".")) {
                return true;
            }
        }
        return false;
    }
}
