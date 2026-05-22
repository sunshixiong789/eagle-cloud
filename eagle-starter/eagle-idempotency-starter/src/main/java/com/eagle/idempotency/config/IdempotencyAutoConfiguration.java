package com.eagle.idempotency.config;

import com.eagle.idempotency.aspect.IdempotencyAspect;
import com.eagle.idempotency.controller.IdempotencyTokenController;
import com.eagle.idempotency.filter.ReactiveIdempotencyTokenWebFilter;
import com.eagle.idempotency.properties.IdempotencyProperties;
import com.eagle.idempotency.support.IdempotencyTokenResolver;
import com.eagle.idempotency.support.ReactiveIdempotencyTokenResolver;
import com.eagle.idempotency.support.ServletIdempotencyTokenResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.WebFilter;

/**
 * 幂等性组件自动配置。
 *
 * <p>依赖 {@link RedissonClient} 在类路径存在时激活。注册以下 Bean：
 * <ul>
 *   <li>{@link IdempotencyAspect} — AOP 切面，拦截 {@code @Idempotent} 方法</li>
 *   <li>{@link IdempotencyTokenController} — REST 接口，提供 Token 生成能力</li>
 *   <li>{@link IdempotencyTokenResolver} — Servlet / WebFlux 各注册一个实现</li>
 * </ul>
 *
 * @author sunshixiong
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    /**
     * 注册幂等性 AOP 切面。
     *
     * @param redissonClient     Redisson 客户端
     * @param properties         幂等性配置属性
     * @param tokenResolver      当前 HTTP 请求 Token 解析器（Servlet / Reactive 双实现）
     * @param objectMapper       Jackson ObjectMapper（用于 RESULT_CACHE 模式序列化/反序列化响应）
     * @param applicationContext Spring 容器（用于按名称查找 IdempotencyKeyExtractor Bean）
     * @return 幂等性切面 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyAspect idempotencyAspect(
            RedissonClient redissonClient,
            IdempotencyProperties properties,
            IdempotencyTokenResolver tokenResolver,
            ObjectMapper objectMapper,
            ApplicationContext applicationContext) {
        return new IdempotencyAspect(redissonClient, properties, tokenResolver, objectMapper, applicationContext);
    }

    /**
     * 注册幂等 Token 生成控制器。
     *
     * @param redissonClient Redisson 客户端
     * @param properties     幂等性配置属性
     * @return Token 控制器 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyTokenController idempotencyTokenController(
            RedissonClient redissonClient,
            IdempotencyProperties properties) {
        return new IdempotencyTokenController(redissonClient, properties);
    }

    /**
     * Servlet（WebMVC）环境下的 Token 解析器：从 {@link HttpServletRequest} 取 header。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(HttpServletRequest.class)
    static class ServletIdempotencySupport {

        @Bean
        @ConditionalOnMissingBean(IdempotencyTokenResolver.class)
        public IdempotencyTokenResolver servletIdempotencyTokenResolver(HttpServletRequest request) {
            return new ServletIdempotencyTokenResolver(request);
        }
    }

    /**
     * WebFlux 环境下的 Token 解析器：通过 {@link ReactiveIdempotencyTokenWebFilter} 在请求线程
     * 把 header 写入上下文，AOP 切面在同步代码路径上直接读取。结合 {@code Hooks.enableAutomaticContextPropagation()}
     * 在跨线程（如 {@code subscribeOn}）后仍可读到。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(DispatcherHandler.class)
    static class ReactiveIdempotencySupport {

        @Bean
        @ConditionalOnMissingBean(IdempotencyTokenResolver.class)
        public IdempotencyTokenResolver reactiveIdempotencyTokenResolver() {
            return new ReactiveIdempotencyTokenResolver();
        }

        @Bean
        @ConditionalOnMissingBean(ReactiveIdempotencyTokenWebFilter.class)
        public WebFilter reactiveIdempotencyTokenWebFilter() {
            return new ReactiveIdempotencyTokenWebFilter();
        }

        @Bean
        @ConditionalOnMissingBean(IdempotencyContextPropagationRegistrar.class)
        @ConditionalOnClass(name = "io.micrometer.context.ContextRegistry")
        public IdempotencyContextPropagationRegistrar idempotencyContextPropagationRegistrar() {
            return new IdempotencyContextPropagationRegistrar();
        }
    }
}
