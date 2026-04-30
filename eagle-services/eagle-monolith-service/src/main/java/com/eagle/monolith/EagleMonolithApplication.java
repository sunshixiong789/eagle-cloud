package com.eagle.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 单体应用启动入口。
 * <p>
 * 扫描根明确限定到业务包，避免误扫 starter 内部 {@code @Component}（starter 内部 Bean 应通过
 * 各自的 {@code @AutoConfiguration} + 条件装配，不依赖应用层组件扫描）：
 * <ul>
 *   <li>{@code com.eagle.system.*} — 来自 eagle-system-service 的认证 / 用户 / 角色 / 菜单 / OAuth2 业务代码</li>
 *   <li>{@code com.eagle.monolith.*} — 单体专属的扩展配置 / Bean 覆盖</li>
 * </ul>
 * eagle-system-service 中 {@code EagleSystemApplication} 上的
 * {@code @ConfigurationPropertiesScan} / {@code @EnableCaching} / 启动 Banner 等元注解
 * 在被扫描为 {@code @Configuration} 后照常生效。
 * <p>
 * 不引入任何 Nacos / 注册中心 / Gateway / Sentinel 等微服务基础设施。
 */
@SpringBootApplication(scanBasePackages = {"com.eagle.system", "com.eagle.monolith"})
public class EagleMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(EagleMonolithApplication.class, args);
    }
}
