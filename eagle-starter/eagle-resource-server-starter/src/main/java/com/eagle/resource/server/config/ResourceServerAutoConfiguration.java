package com.eagle.resource.server.config;

import com.eagle.resource.server.properties.ResourceServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 资源服务器自动配置入口。
 *
 * <p>通过 Spring Boot SPI（{@code AutoConfiguration.imports}）自动激活，
 * 或通过 {@link EnableEagleResourceServer} 注解手动激活，两者效果相同。
 *
 * <p>注册以下配置：
 * <ul>
 *   <li>{@link EagleJwtAuthenticationConverter} — JWT → {@link com.eagle.common.dto.EagleUser} 认证转换</li>
 *   <li>{@link ResourceServerSecurityConfig} — 无状态 OAuth2 JWT 安全过滤链，支持可配置放行路径</li>
 *   <li>{@link CacheConfig} — 缓存启用及 Redis 缓存 JSON 序列化配置</li>
 *   <li>{@link OpenApiConfig} — Swagger UI + OAuth2 / Bearer Token 认证集成</li>
 * </ul>
 *
 * @author 孙士雄
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(ResourceServerProperties.class)
@Import({
        EagleJwtAuthenticationConverter.class,
        ResourceServerSecurityConfig.class,
        CacheConfig.class,
        OpenApiConfig.class
})
public class ResourceServerAutoConfiguration {
}
