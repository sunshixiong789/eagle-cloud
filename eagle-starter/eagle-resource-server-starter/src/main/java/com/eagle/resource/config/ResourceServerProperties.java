package com.eagle.resource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 资源服务器配置属性
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.security.oauth2.resource-server")
public class ResourceServerProperties {

    /**
     * JWT 签发者 URI（授权服务器地址）
     */
    private String issuerUri = "http://localhost:8080";

    /**
     * JWK Set URI（公钥端点）
     */
    private String jwkSetUri;

    /**
     * 是否启用资源服务器
     */
    private boolean enabled = true;

    /**
     * 公开的 URL 路径模式（不需要认证）
     */
    private String[] publicPaths = {"/public/**", "/actuator/health", "/actuator/info"};

    /**
     * 是否启用 Swagger 文档访问（无需认证）
     */
    private boolean enableSwagger = true;
}
