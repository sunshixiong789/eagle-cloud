package com.eagle.monolith;

import com.eagle.system.EagleSystemApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 单体应用启动入口。
 * <p>
 * 扫描根明确限定到业务包，避免误扫 starter 内部 {@code @Component}（starter 内部 Bean 应通过
 * 各自的 {@code @AutoConfiguration} + 条件装配，不依赖应用层组件扫描）：
 * <ul>
 *   <li>{@code com.eagle.system.*} — 来自 eagle-system-service 的认证 / 用户 / 角色 / 菜单 / OAuth2 业务代码</li>
 *   <li>{@code com.eagle.monolith.*} — 单体专属的扩展配置 / Bean 覆盖</li>
 * </ul>
 * <p>
 * 必须显式排除 {@link EagleSystemApplication}：该类带 {@code @SpringBootApplication}
 * （= {@code @SpringBootConfiguration} + {@code @EnableAutoConfiguration} + {@code @ComponentScan}），
 * 若被扫描会触发二次 auto-configuration 加载，导致 {@code AsyncConfig} 等单例 bean 重复注册
 * （表现为 "Only one AsyncConfigurer may exist"）。这里直接用 {@code @ConfigurationPropertiesScan}
 * 与 {@code @EnableCaching} 等价复刻其元注解。
 * <p>
 * 不引入任何 Nacos / 注册中心 / Gateway / Sentinel 等微服务基础设施。
 */
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = {"com.eagle.system", "com.eagle.monolith"})
@EnableCaching
@ComponentScan(
        basePackages = {"com.eagle.system", "com.eagle.monolith"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = EagleSystemApplication.class
        )
)
@EntityScan(basePackages = {"com.eagle.system", "com.eagle.monolith"})
@EnableJpaRepositories(basePackages = {"com.eagle.system", "com.eagle.monolith"})
public class EagleMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(EagleMonolithApplication.class, args);
    }
}
