package com.eagle.ai.config;

import com.eagle.ai.advisor.AiRateLimitAdvisor;
import com.eagle.ai.advisor.TokenBudgetAdvisor;
import com.eagle.ai.memory.RedisChatMemoryRepository;
import com.eagle.ai.properties.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Eagle AI Redis 扩展配置。
 *
 * <p>当 Redis（{@link StringRedisTemplate}）在类路径且 Bean 可用时自动激活，
 * 在主配置 {@link EagleAiAutoConfiguration} 之前运行，注册：
 * <ul>
 *   <li>{@link RedisChatMemoryRepository} — 替换默认的 InMemory 存储，对话历史持久化到 Redis</li>
 *   <li>{@link AiRateLimitAdvisor} — 可选限流 Advisor，需要 {@code eagle.ai.rate-limit.enabled=true}</li>
 * </ul>
 */
@AutoConfiguration(before = EagleAiAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@EnableConfigurationProperties(AiProperties.class)
public class EagleAiRedisAutoConfiguration {

    /**
     * Redis 持久化对话历史存储。
     *
     * <p>消费方可注册自定义 {@link ChatMemoryRepository} Bean 覆盖此实现：
     * <pre>{@code
     * @Bean
     * public ChatMemoryRepository chatMemoryRepository(...) { return new MyRepository(...); }
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public ChatMemoryRepository redisChatMemoryRepository(StringRedisTemplate redisTemplate,
                                                          ObjectMapper objectMapper,
                                                          AiProperties properties) {
        return new RedisChatMemoryRepository(redisTemplate, objectMapper, properties);
    }

    /**
     * AI 调用限流 Advisor（Redis 滑动分钟窗口）。
     *
     * <p>需要 {@code eagle.ai.rate-limit.enabled=true} 才激活。
     */
    @Bean
    @ConditionalOnMissingBean(AiRateLimitAdvisor.class)
    @ConditionalOnProperty(name = "eagle.ai.rate-limit.enabled", havingValue = "true")
    public AiRateLimitAdvisor aiRateLimitAdvisor(StringRedisTemplate redisTemplate,
                                                 AiProperties properties) {
        return new AiRateLimitAdvisor(redisTemplate, properties);
    }

    /**
     * Token 月度配额 Advisor（Redis INCR + TTL）。
     *
     * <p>需要 {@code eagle.ai.budget.enabled=true} 才激活。
     * 超出月度配额后拒绝请求（90003）。
     */
    @Bean
    @ConditionalOnMissingBean(TokenBudgetAdvisor.class)
    @ConditionalOnProperty(name = "eagle.ai.budget.enabled", havingValue = "true")
    public TokenBudgetAdvisor tokenBudgetAdvisor(StringRedisTemplate redisTemplate,
                                                 AiProperties properties) {
        return new TokenBudgetAdvisor(redisTemplate, properties);
    }
}
