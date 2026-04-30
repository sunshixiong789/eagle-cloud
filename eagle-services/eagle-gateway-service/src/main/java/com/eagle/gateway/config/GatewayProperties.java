package com.eagle.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Gateway 全局配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.gateway")
public class GatewayProperties {

    private Security security = new Security();
    private Openapi openapi = new Openapi();

    /**
     * 安全相关配置。
     */
    @Data
    public static class Security {

        /**
         * OAuth2 授权服务器根地址，用于拼接 JWK Set URI。
         * 对应环境变量 AUTH_SERVER_URL。
         */
        private String authServerUrl = "http://localhost:80";

        /**
         * 无需 JWT 认证的公开路径（Ant 风格）。
         * 可在 application.yml 中追加或覆盖。
         */
        private List<String> publicPaths = List.of(
                "/public/**",
                "/actuator/health",
                "/actuator/info",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/webjars/**",
                "/oauth2/**",
                "/login",
                "/error"
        );
    }

    /**
     * OpenAPI 聚合相关配置。
     */
    @Data
    public static class Openapi {

        /**
         * 是否通过 Nacos 动态发现服务并聚合 API 文档，默认开启。
         */
        private boolean discoveryEnabled = true;
    }
}
