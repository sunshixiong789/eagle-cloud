package com.eagle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.modulith.Modulithic;

import java.util.Optional;

/**
 * Eagle 应用主入口
 * <p>
 * 模块划分（Spring Modulith 有界上下文）：
 * <ul>
 *   <li>{@code auth}   — 认证授权域，负责用户登录、OAuth2、微信/短信认证</li>
 *   <li>{@code system} — 系统管理域，负责用户、角色、权限、部门、菜单管理</li>
 *   <li>{@code config} — 全局配置，跨域基础设施（Security、Cache、i18n 等）</li>
 *   <li>{@code common} — 共享内核（Shared Kernel），所有模块可无限制访问</li>
 * </ul>
 * <p>
 * 模块约束声明见各模块的 {@code package-info.java}（使用 {@code @ApplicationModule}）。
 * 架构验证通过测试 {@link com.eagle.ModulithArchitectureTest} 运行：
 * <pre>gradle test --tests "com.eagle.ModulithArchitectureTest"</pre>
 *
 * @author sunshixiong
 */
// @Modulithic 仅供 Modulith 静态分析读取（compileOnly），Spring Boot 启动时不处理
@Modulithic(
        systemName = "Eagle",
        sharedModules = "common"  // common 是共享内核，所有模块无需在 allowedDependencies 中声明即可访问
)
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class EagleSystemApplication {

    private final Environment env;

    public EagleSystemApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(EagleSystemApplication.class, args);
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
