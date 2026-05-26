package com.eagle.monolith;

import com.eagle.system.EagleSystemApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

/**
 * 单体应用启动入口。
 * <p>
 * 扫描根明确限定到业务包，避免误扫 starter 内部 {@code @Component}（starter 内部 Bean 应通过
 * 各自的 {@code @AutoConfiguration} + 条件装配，不依赖应用层组件扫描）：
 * <ul>
 *   <li>{@code com.eagle.auth.*} — 来自 eagle-auth-service 的账号 / OAuth2 / JWT 业务代码</li>
 *   <li>{@code com.eagle.system.*} — 来自 eagle-system-service 的用户 / 角色 / 菜单 / 权限业务代码</li>
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
@ConfigurationPropertiesScan(basePackages = {"com.eagle.auth", "com.eagle.system", "com.eagle.monolith"})
@EnableCaching
@ComponentScan(
        basePackages = {"com.eagle.auth", "com.eagle.system", "com.eagle.monolith"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = EagleSystemApplication.class))
@EntityScan(basePackages = {"com.eagle.auth", "com.eagle.system", "com.eagle.monolith", "com.eagle.audit"})
@EnableJpaRepositories(basePackages = {"com.eagle.auth", "com.eagle.system", "com.eagle.monolith", "com.eagle.audit"})
public class EagleMonolithApplication {

    private final Environment env;

    public EagleMonolithApplication(Environment env) {
        this.env = env;
    }


    public static void main(String[] args) {
        SpringApplication.run(EagleMonolithApplication.class, args);
    }


    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = Optional.ofNullable(env.getProperty("server.port")).orElse("80");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path")).orElse("");
        String baseUrl = "http://localhost:" + port + contextPath;

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  🚀 Eagle Application Started Successfully!                  ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  📚 Swagger UI:      " + padRight(baseUrl + "/swagger-ui.html", 38) + "║");
        System.out.println("║  📖 API Docs:        " + padRight(baseUrl + "/v3/api-docs", 38) + "║");
        System.out.println("║  🔐 OAuth2 Token:    " + padRight(baseUrl + "/oauth2/token", 38) + "║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private String padRight(String s, int n) {
        if (s.length() > n) {
            return s.substring(0, n - 3) + "...";
        }
        return String.format("%-" + n + "s", s);
    }
}
