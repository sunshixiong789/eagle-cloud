package com.eagle.resource.server.config;

import com.eagle.resource.server.properties.ResourceServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 资源服务器自动配置入口。
 *
 * <p>通过 Spring Boot SPI（{@code AutoConfiguration.imports}）自动激活，
 * 或通过 {@link EnableEagleResourceServer} 注解手动激活，两者效果相同。
 *
 * <p>注册以下配置：
 * <ul>
 *   <li>{@link EagleJwtAuthenticationConverter} — JWT → {@link com.eagle.common.dto.EagleUser} 认证转换</li>
 *   <li>{@link ResourceServerSecurityConfig} — Servlet 无状态 OAuth2 JWT 安全过滤链</li>
 *   <li>{@link ReactiveResourceServerSecurityConfig} — WebFlux 无状态 OAuth2 JWT 安全过滤链</li>
 *   <li>{@link CacheConfig} — 缓存启用及 Redis 缓存 JSON 序列化配置</li>
 * </ul>
 *
 * <p>OpenAPI / Swagger UI 集成统一由 {@code eagle-openapi-starter} 提供，
 * 不再在本 starter 内部装配。
 *
 * @author 孙士雄
 */
@AutoConfiguration
@ConditionalOnClass(Jwt.class)
@EnableConfigurationProperties(ResourceServerProperties.class)
@Import({
        EagleJwtAuthenticationConverter.class,
        ResourceServerSecurityConfig.class,
        ReactiveResourceServerSecurityConfig.class,
        CacheConfig.class
})
public class ResourceServerAutoConfiguration {
}
