package com.eagle.resource.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源服务器配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.resource-server")
public class ResourceServerProperties {

    /**
     * 额外放行的请求路径（Ant 风格），会合并到默认白名单中。
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

    /**
     * 授权服务器地址，用于 Swagger UI OAuth2 授权码流程显示绝对 URL。
     *
     * <p>留空时 Swagger UI OAuth2 使用相对路径（适合通过网关聚合 Swagger 的场景）。
     * 直连调试场景下建议填写，例如：{@code http://localhost:8080}。
     */
    private String authServerUrl = "";

    /**
     * OpenAPI 文档配置。
     */
    @NestedConfigurationProperty
    private Api api = new Api();

    /**
     * OpenAPI 文档属性。
     */
    @Data
    public static class Api {

        /**
         * API 文档标题。
         */
        private String title = "Eagle API";

        /**
         * API 版本号。
         */
        private String version = "v1.0.0";

        /**
         * API 文档描述（Markdown 格式）。
         * 留空时使用内置默认描述（含认证方式说明和角色说明）。
         */
        private String description = "";
    }
}
