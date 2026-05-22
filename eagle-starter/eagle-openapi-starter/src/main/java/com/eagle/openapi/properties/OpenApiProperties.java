package com.eagle.openapi.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAPI 配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.openapi")
public class OpenApiProperties {

    /**
     * 文档标题
     */
    private String title = "Eagle API";

    /**
     * 版本号
     */
    private String version = "v1.0.0";

    /**
     * 描述
     */
    private String description;

    /**
     * OAuth2 授权服务器地址，用于 Swagger UI OAuth2 授权码流程显示绝对 URL。
     *
     * <p>留空时使用相对路径（适合通过网关聚合 Swagger 的场景）；
     * 直连调试场景下建议填写具体地址，如 {@code http://localhost:8080}。
     */
    private String authServerUrl = "";
}
