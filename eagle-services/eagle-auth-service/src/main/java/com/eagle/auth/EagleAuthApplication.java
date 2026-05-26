package com.eagle.auth;

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
 * Eagle 认证服务入口（Servlet 栈）。
 *
 * <p>承载 OAuth2 Authorization Server、Thymeleaf 表单登录、自定义 grant（SMS / WeChat /
 * Phone One-Click）、JWT 签发与黑名单、账户聚合根与 WebSocket STOMP CONNECT 鉴权。
 *
 * <p>与 {@code eagle-system-service}（WebFlux）通过 RocketMQ JSON 事件 + 自建 RestClient
 * 解耦：本服务对外发布 {@code eagle_auth_events} 事件，并暴露 {@code /internal/**}
 * 同步 API；下游服务自行维护事件 POJO 与客户端 DTO。
 *
 * <p>主类必须放在 {@code com.eagle.auth} 包内，避免与其他 starter 的
 * {@code AutoConfigurationPackages} 注册产生祖先/子包重叠（例如
 * {@code com.eagle.audit.repository} 是 {@code com.eagle} 的子包，
 * 主类若放在 {@code com.eagle} 顶级包会导致 Spring Data JPA 仓库扫描
 * 在祖先与子包各扫到一次，触发 {@code BeanDefinitionOverrideException}）。
 *
 * @author sunshixiong
 */
@Slf4j
@Modulithic(systemName = "EagleAuth")
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class EagleAuthApplication {

    private final Environment env;

    public EagleAuthApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(EagleAuthApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = Optional.ofNullable(env.getProperty("server.port")).orElse("80");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path")).orElse("");
        String baseUrl = "http://localhost:" + port + contextPath;

        log.info(
                """

                        ╔══════════════════════════════════════════════════════════════╗
                        ║  🔐 Eagle Auth Service Started Successfully!                 ║
                        ╠══════════════════════════════════════════════════════════════╣
                        ║  📚 Swagger UI:      {}║
                        ║  📖 API Docs:        {}║
                        ║  🔐 OAuth2 Token:    {}║
                        ║  👤 Login Form:      {}║
                        ╚══════════════════════════════════════════════════════════════╝
                        """,
                padRight(baseUrl + "/swagger-ui.html", 38),
                padRight(baseUrl + "/v3/api-docs", 38),
                padRight(baseUrl + "/oauth2/token", 38),
                padRight(baseUrl + "/login", 38));
    }

    private String padRight(String s, int n) {
        if (s.length() > n) {
            return s.substring(0, n - 3) + "...";
        }
        return String.format("%-" + n + "s", s);
    }
}
