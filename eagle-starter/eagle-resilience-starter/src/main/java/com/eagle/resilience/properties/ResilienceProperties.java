package com.eagle.resilience.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 容错/熔断配置属性，为 eagle 平台提供统一默认的 Resilience4J 实例配置。
 *
 * <p>注册的默认实例名称：{@code eagle-default}，可在 {@code @CircuitBreaker}、
 * {@code @Retry}、{@code @TimeLimiter} 中直接引用：
 * <pre>
 * &#64;CircuitBreaker(name = "eagle-default", fallbackMethod = "fallback")
 * public String callRemote() { ... }
 * </pre>
 *
 * <p>示例（application.yml）：
 * <pre>
 * eagle:
 *   resilience:
 *     circuit-breaker:
 *       failure-rate-threshold: 50
 *       slow-call-rate-threshold: 80
 *       wait-duration-in-open-state: 30s
 *       sliding-window-size: 100
 *       minimum-number-of-calls: 10
 *     retry:
 *       max-attempts: 3
 *       wait-duration: 500ms
 *       exponential-backoff-multiplier: 2.0
 *     time-limiter:
 *       timeout-duration: 5s
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.resilience")
public class ResilienceProperties {

    private final CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private final RetryConfig retry = new RetryConfig();
    private final TimeLimiterConfig timeLimiter = new TimeLimiterConfig();
    /**
     * 是否启用容错配置。
     */
    private boolean enabled = true;

    @Data
    public static class CircuitBreakerConfig {

        /**
         * 错误率阈值（百分比），超过后开路。
         */
        private float failureRateThreshold = 50f;

        /**
         * 慢调用率阈值（百分比），慢调用判定时间由 {@code slowCallDurationThreshold} 决定。
         */
        private float slowCallRateThreshold = 100f;

        /**
         * 慢调用判定时间，超过此值视为慢调用。
         */
        private Duration slowCallDurationThreshold = Duration.ofSeconds(2);

        /**
         * 开路后等待时间，超时后进入半开状态。
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);

        /**
         * 滑动窗口大小（COUNT 模式下为调用次数）。
         */
        private int slidingWindowSize = 100;

        /**
         * 触发熔断所需的最少调用次数。
         */
        private int minimumNumberOfCalls = 10;

        /**
         * 半开状态下允许通过的试探调用次数。
         */
        private int permittedNumberOfCallsInHalfOpenState = 10;
    }

    @Data
    public static class RetryConfig {

        /**
         * 最大重试次数（含首次调用）。
         */
        private int maxAttempts = 3;

        /**
         * 重试等待基础时长。
         */
        private Duration waitDuration = Duration.ofMillis(500);

        /**
         * 指数退避乘数；值为 1.0 表示固定间隔。
         */
        private double exponentialBackoffMultiplier = 2.0;

        /**
         * 指数退避最大等待时长，防止无限增长。
         */
        private Duration exponentialMaxWaitDuration = Duration.ofSeconds(10);
    }

    @Data
    public static class TimeLimiterConfig {

        /**
         * 调用超时时间，超过后取消执行并抛出 {@code TimeoutException}。
         */
        private Duration timeoutDuration = Duration.ofSeconds(5);

        /**
         * 超时后是否取消正在进行的 Future。
         */
        private boolean cancelRunningFuture = true;
    }
}
