package com.eagle.system;

import lombok.extern.slf4j.Slf4j;
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
 * Eagle System Service 主入口。
 * <p>
 * 模块划分(Spring Modulith 有界上下文):
 * <ul>
 *   <li>{@code base}    — 系统管理域,用户、角色、权限、部门、菜单、岗位、字典、系统日志</li>
 *   <li>{@code message} — 站内消息中心(平台级横切,不依赖业务模块)</li>
 *   <li>{@code file}    — 文件管理(元数据 + OSS 集成)</li>
 * </ul>
 * <p>
 * 与 auth-service 通过两种方式集成:
 * <ul>
 *   <li>RocketMQ topic {@code eagle.auth.events} 异步消费集成事件</li>
 *   <li>RestClient 同步调用 {@code /internal/**} 内部 API</li>
 * </ul>
 * <p>
 * 模块约束声明见各模块的 {@code package-info.java}({@code @ApplicationModule})。
 * <pre>gradle :eagle-services:eagle-system-service:test --tests "*.ModulithArchitectureTest"</pre>
 *
 * @author sunshixiong
 */
// @Modulithic 仅供 Modulith 静态分析读取（compileOnly），Spring Boot 启动时不处理
@Slf4j
@Modulithic(systemName = "Eagle")
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

        // OAuth2 Authorization Server 已在 24f3f21 拆出到 eagle-auth-service,
        // 此处不再打印 /oauth2/token URL,避免误导调用方仍向 system 发授权请求。
        log.info(
                """

                        ╔══════════════════════════════════════════════════════════════╗
                        ║  🚀 Eagle Application Started Successfully!                  ║
                        ╠══════════════════════════════════════════════════════════════╣
                        ║  📚 Swagger UI:      {}║
                        ║  📖 API Docs:        {}║
                        ╚══════════════════════════════════════════════════════════════╝
                        """,
                padRight(baseUrl + "/swagger-ui.html", 38),
                padRight(baseUrl + "/v3/api-docs", 38));
    }

    private String padRight(String s, int n) {
        if (s.length() > n) {
            return s.substring(0, n - 3) + "...";
        }
        return String.format("%-" + n + "s", s);
    }

}
