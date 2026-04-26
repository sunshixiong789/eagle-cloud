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
     * OAuth2 授权服务器地址（用于文档中的 authorizeUrl/tokenUrl）
     */
    private String authServerUrl = "http://localhost:80";
}
