package com.eagle.resource.server.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 Eagle 资源服务器安全配置。
 *
 * <p>将此注解添加到应用主类（或任意 {@code @Configuration} 类）上，即可激活：
 * <ul>
 *   <li>OAuth2 JWT 资源服务器过滤链（无状态，支持可配置放行路径）</li>
 *   <li>{@link EagleJwtAuthenticationConverter}（{@link com.eagle.common.dto.EagleUser} 作为 Principal）</li>
 *   <li>缓存配置 {@link CacheConfig}</li>
 * </ul>
 *
 * <p>与 Spring Boot 自动配置等效，两者不会重复注册（所有 Bean 均有 {@code @ConditionalOnMissingBean} 保护）：
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableEagleResourceServer
 * public class MyServiceApplication { ... }
 * }</pre>
 *
 * @author eagle
 * @see ResourceServerAutoConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ResourceServerAutoConfiguration.class)
public @interface EnableEagleResourceServer {
}
