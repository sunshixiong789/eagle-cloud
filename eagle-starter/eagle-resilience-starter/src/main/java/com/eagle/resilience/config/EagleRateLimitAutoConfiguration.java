package com.eagle.resilience.config;

import com.eagle.resilience.aspect.RateLimitAspect;
import com.eagle.resilience.handler.RateLimitExceptionHandler;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

/**
 * 声明式限流自动配置（{@code @RateLimit} 注解支持）。
 *
 * <p>装配内容：
 * <ul>
 *   <li>{@link RateLimiterRegistry} / {@link BulkheadRegistry} —— 按资源名缓存限流器实例</li>
 *   <li>{@link RateLimitAspect} —— 拦截 {@code @RateLimit}</li>
 *   <li>{@link RateLimitExceptionHandler} —— 限流异常转 HTTP 429</li>
 * </ul>
 *
 * <p>registry 均以空配置创建：具体阈值由每个 {@code @RateLimit} 注解在首次调用时
 * 按资源名注册，因此这里不需要（也不应该）预设全局默认阈值。
 *
 * <p>可通过 {@code eagle.resilience.rate-limit.enabled=false} 整体关闭。
 *
 * @author eagle
 * @see com.eagle.resilience.annotation.RateLimit
 */
@AutoConfiguration(after = EagleResilienceAutoConfiguration.class)
@ConditionalOnClass({RateLimiterRegistry.class, Aspect.class})
@ConditionalOnProperty(name = "eagle.resilience.rate-limit.enabled",
        havingValue = "true", matchIfMissing = true)
public class EagleRateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimiterRegistry rateLimiterRegistry,
                                           BulkheadRegistry bulkheadRegistry) {
        return new RateLimitAspect(rateLimiterRegistry, bulkheadRegistry);
    }

    /**
     * 限流异常 → HTTP 429 的处理器，仅 Servlet Web 环境装配。
     *
     * @param messageSource i18n 消息源，用于本地化 429 文案
     * @return 限流异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HttpServletRequest.class)
    public RateLimitExceptionHandler rateLimitExceptionHandler(MessageSource messageSource) {
        return new RateLimitExceptionHandler(messageSource);
    }
}
