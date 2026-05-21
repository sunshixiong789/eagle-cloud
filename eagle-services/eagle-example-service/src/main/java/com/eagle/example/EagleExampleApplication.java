package com.eagle.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Optional;

/**
 * Eagle Example Service — 全量 Starter 验证模块
 * <p>
 * 本模块集成除 AI 外所有 eagle-starter，用于验证自动配置与核心功能。
 *
 * @author sunshixiong
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class EagleExampleApplication {

    private final Environment env;

    public EagleExampleApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(EagleExampleApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = Optional.ofNullable(env.getProperty("server.port")).orElse("8080");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path")).orElse("");
        String baseUrl = "http://localhost:" + port + contextPath;

        log.info(
                """

                        ╔══════════════════════════════════════════════════════════════╗
                        ║  🚀 Eagle Example Service Started Successfully!              ║
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
