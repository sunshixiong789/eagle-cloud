package com.eagle.resource.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源服务器配置属性。
 *
 * <p>OpenAPI / Swagger UI 相关配置已迁移到 {@code eagle.openapi.*}
 * （由 {@code eagle-openapi-starter} 提供）。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.resource-server")
public class ResourceServerProperties {

    /**
     * 额外放行的请求路径（Servlet 用 Ant 语法，WebFlux 用 PathPattern 语法），会合并到默认白名单中。
     *
     * <p>默认已放行：{@code /public/**}、{@code /actuator/health}、{@code /actuator/info}、
     * Swagger UI 相关路径（{@code /swagger-ui/**}、{@code /v3/api-docs/**} 等）。
     *
     * <p>示例：
     * <pre>{@code
     * eagle:
     *   resource-server:
     *     permit-paths:
     *       - /sms/code
     *       - /auth/refresh
     * }</pre>
     */
    private List<String> permitPaths = new ArrayList<>();
}
