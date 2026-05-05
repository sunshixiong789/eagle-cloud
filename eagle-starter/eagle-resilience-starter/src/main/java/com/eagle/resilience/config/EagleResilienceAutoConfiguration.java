package com.eagle.resilience.config;

import com.eagle.resilience.properties.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Resilience4J 容错自动配置。
 *
 * <p>注册 {@code eagle-default} 命名实例，可在 {@code @CircuitBreaker}、
 * {@code @Retry}、{@code @TimeLimiter} 中直接引用：
 * <pre>
 * &#64;CircuitBreaker(name = "eagle-default", fallbackMethod = "fallback")
 * public String callRemote() { ... }
 * </pre>
 *
 * @author eagle
 */
@AutoConfiguration
@ConditionalOnClass(CircuitBreakerRegistry.class)
@ConditionalOnProperty(name = "eagle.resilience.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ResilienceProperties.class)
public class EagleResilienceAutoConfiguration {

    static final String DEFAULT_INSTANCE = "eagle-default";

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties properties) {
        ResilienceProperties.CircuitBreakerConfig cfg = properties.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cfg.getFailureRateThreshold())
                .slowCallRateThreshold(cfg.getSlowCallRateThreshold())
                .slowCallDurationThreshold(cfg.getSlowCallDurationThreshold())
                .waitDurationInOpenState(cfg.getWaitDurationInOpenState())
                .slidingWindowSize(cfg.getSlidingWindowSize())
                .minimumNumberOfCalls(cfg.getMinimumNumberOfCalls())
                .permittedNumberOfCallsInHalfOpenState(cfg.getPermittedNumberOfCallsInHalfOpenState())
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry(ResilienceProperties properties) {
        ResilienceProperties.RetryConfig cfg = properties.getRetry();

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(cfg.getMaxAttempts())
                .waitDuration(cfg.getWaitDuration())
                .intervalFunction(attempt -> {
                    double multiplier = cfg.getExponentialBackoffMultiplier();
                    if (multiplier <= 1.0) {
                        return cfg.getWaitDuration().toMillis();
                    }
                    long wait = (long) (cfg.getWaitDuration().toMillis() * Math.pow(multiplier, attempt - 1));
                    long maxWait = cfg.getExponentialMaxWaitDuration().toMillis();
                    return Math.min(wait, maxWait);
                })
                .build();

        return RetryRegistry.of(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public TimeLimiterRegistry timeLimiterRegistry(ResilienceProperties properties) {
        ResilienceProperties.TimeLimiterConfig cfg = properties.getTimeLimiter();

        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(cfg.getTimeoutDuration())
                .cancelRunningFuture(cfg.isCancelRunningFuture())
                .build();

        return TimeLimiterRegistry.of(config);
    }
}
