package com.eagle.ai.config;

import com.eagle.ai.properties.AiProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Eagle AI Resilience4J 专属配置。
 *
 * <p>当 {@link CircuitBreakerRegistry} 和 {@link TimeLimiterRegistry} Bean 可用（即
 * {@code eagle-resilience-starter} 在类路径）时自动激活，为 AI 调用注册专属命名配置：
 * <ul>
 *   <li>CircuitBreaker {@code eagle-ai-default}：失败率阈值 50%；慢调用阈值 10 秒（AI 比普通 HTTP 慢）；
 *       熔断等待 60 秒；滑动窗口 20 次；最少触发 5 次</li>
 *   <li>TimeLimiter {@code eagle-ai-default}：超时时间与 {@code eagle.ai.chat.timeout}（默认 30 秒）对齐</li>
 * </ul>
 *
 * <p>消费方可直接在方法上引用命名实例：
 * <pre>{@code
 * @TimeLimiter(name = "eagle-ai-default")
 * @CircuitBreaker(name = "eagle-ai-default", fallbackMethod = "fallback")
 * public CompletableFuture<String> callAi(String prompt) {
 *     return CompletableFuture.supplyAsync(
 *         () -> chatClient.prompt().user(prompt).call().content());
 * }
 *
 * private CompletableFuture<String> fallback(String prompt, Throwable t) {
 *     log.warn("AI call failed, using fallback. reason={}", t.getMessage());
 *     return CompletableFuture.completedFuture("服务暂时不可用，请稍后重试。");
 * }
 * }</pre>
 *
 * <p>若已通过 YAML（{@code resilience4j.circuitbreaker.instances.eagle-ai-default}）
 * 或代码注册同名配置，本配置不覆盖（幂等）。
 */
@AutoConfiguration(after = EagleAiAutoConfiguration.class)
@ConditionalOnClass({CircuitBreakerRegistry.class, TimeLimiterRegistry.class})
@ConditionalOnBean({CircuitBreakerRegistry.class, TimeLimiterRegistry.class})
@ConditionalOnProperty(name = "eagle.ai.resilience.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AiProperties.class)
public class EagleAiResilienceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EagleAiResilienceAutoConfiguration.class);

    /**
     * AI 专属 Resilience4J 实例注册器。
     *
     * <p>在所有 Singleton Bean 就绪后执行，避免与 Resilience4J Spring Boot Autoconfigure
     * 的 YAML 属性绑定冲突——若同名配置已存在则跳过。
     */
    @Bean
    @ConditionalOnMissingBean(name = "eagleAiResilienceCustomizer")
    public SmartInitializingSingleton eagleAiResilienceCustomizer(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            AiProperties properties) {
        return () -> {
            AiProperties.Resilience cfg = properties.getResilience();
            String instanceName = cfg.getInstanceName();

            if (circuitBreakerRegistry.getConfiguration(instanceName).isEmpty()) {
                CircuitBreakerConfig aiConfig = CircuitBreakerConfig.custom()
                        .failureRateThreshold(50.0f)
                        .slowCallRateThreshold(80.0f)
                        .slowCallDurationThreshold(cfg.getSlowCallDurationThreshold())
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(5)
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build();
                circuitBreakerRegistry.addConfiguration(instanceName, aiConfig);
                log.debug("Registered AI circuit breaker config: name={}, slowCallThreshold={}",
                        instanceName, cfg.getSlowCallDurationThreshold());
            }

            if (timeLimiterRegistry.getConfiguration(instanceName).isEmpty()) {
                TimeLimiterConfig aiConfig = TimeLimiterConfig.custom()
                        .timeoutDuration(properties.getChat().getTimeout())
                        .cancelRunningFuture(true)
                        .build();
                timeLimiterRegistry.addConfiguration(instanceName, aiConfig);
                log.debug("Registered AI time limiter config: name={}, timeout={}",
                        instanceName, properties.getChat().getTimeout());
            }
        };
    }
}
