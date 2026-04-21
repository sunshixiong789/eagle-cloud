package com.eagle.resource.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * 启用 Eagle 资源服务器安全配置。
 * <p>
 * 将此注解添加到应用主类（或任意 {@code @Configuration} 类）上，
 * 即可启用 OAuth2 JWT 资源服务器过滤链及 {@link EagleJwtAuthenticationConverter}。
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableEagleResourceServer
 * public class MyServiceApplication { ... }
 * }</pre>
 *
 * @author 孙士雄
 * @see ResourceServerSecurityConfig
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ResourceServerSecurityConfig.class, EagleJwtAuthenticationConverter.class})
public @interface EnableEagleResourceServer {
}