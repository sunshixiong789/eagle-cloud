package com.eagle.ai.config;

import com.eagle.ai.advisor.AiAuditAdvisor;
import com.eagle.ai.advisor.AiRateLimitAdvisor;
import com.eagle.ai.advisor.ContentSafetyAdvisor;
import com.eagle.ai.advisor.TokenUsageAdvisor;
import com.eagle.ai.properties.AiProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Eagle AI 自动配置。
 *
 * <p>在任意 Spring AI 提供商 starter（如 {@code spring-ai-starter-model-openai}）存在时自动装配：
 * <ul>
 *   <li>{@link ChatMemoryRepository} — 对话历史存储，优先使用 Redis（由 {@link EagleAiRedisAutoConfiguration}
 *       注册），回退到内存实现</li>
 *   <li>{@link ChatMemory} — 带滑动窗口的对话记忆</li>
 *   <li>{@link TokenUsageAdvisor} — Token 用量 Micrometer 指标（需要 MeterRegistry）</li>
 *   <li>{@link ContentSafetyAdvisor} — 内容安全过滤（{@code eagle.ai.safety.enabled=true} 时激活）</li>
 *   <li>{@link AiAuditAdvisor} — AI 调用审计日志与事件发布</li>
 *   <li>{@link ChatClient} — 预装配记忆 + 指标 + 限流 + 安全 + 审计 Advisor 的 ChatClient</li>
 * </ul>
 *
 * <p>所有 Bean 均标注 {@link ConditionalOnMissingBean}，消费方可自由覆盖。
 *
 * <h2>基础用法</h2>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class AiAssistantService {
 *
 *     private final ChatClient chatClient;
 *
 *     // 普通调用（阻塞）
 *     public String chat(String userId, String message) {
 *         String key = AiConversationKey.of(tenantId, userId);
 *         return chatClient.prompt()
 *                 .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key))
 *                 .user(message)
 *                 .call()
 *                 .content();
 *     }
 *
 *     // 流式调用（SSE / WebSocket）
 *     public Flux<String> stream(String userId, String message) {
 *         String key = AiConversationKey.of(tenantId, userId);
 *         return chatClient.prompt()
 *                 .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, key))
 *                 .user(message)
 *                 .stream()
 *                 .content();
 *     }
 *
 *     // 结构化输出（直接映射为 DTO）
 *     public MyDto extract(String text) {
 *         return chatClient.prompt()
 *                 .user(text)
 *                 .call()
 *                 .entity(MyDto.class);
 *     }
 * }
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnClass({ChatModel.class, ChatClient.class})
@EnableConfigurationProperties(AiProperties.class)
@ConditionalOnProperty(name = "eagle.ai.enabled", havingValue = "true", matchIfMissing = true)
public class EagleAiAutoConfiguration {

    /**
     * 内存对话历史存储（回退实现）。
     *
     * <p>若 Redis 可用，{@link EagleAiRedisAutoConfiguration} 会先注册
     * {@link com.eagle.ai.memory.RedisChatMemoryRepository}，此 Bean 不会生效。
     */
    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 带滑动窗口的对话记忆 Bean。
     *
     * <p>窗口大小由 {@code eagle.ai.chat.memory-window-size} 配置（默认 10 条）。
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatMemory chatMemory(ChatMemoryRepository repository, AiProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.getChat().getMemoryWindowSize())
                .build();
    }

    /**
     * Token 用量 Micrometer 指标 Advisor。
     *
     * <p>需要 {@link MeterRegistry} Bean 存在，且 {@code eagle.ai.metrics.enabled=true}。
     */
    @Bean
    @ConditionalOnMissingBean(TokenUsageAdvisor.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(name = "eagle.ai.metrics.enabled", havingValue = "true", matchIfMissing = true)
    public TokenUsageAdvisor tokenUsageAdvisor(MeterRegistry meterRegistry, AiProperties properties) {
        return new TokenUsageAdvisor(meterRegistry, properties);
    }

    /**
     * 内容安全过滤 Advisor。
     *
     * <p>仅在 {@code eagle.ai.safety.enabled=true} 时激活。按配置的正则黑名单拦截用户输入，
     * 命中则拒绝请求（90004）。
     */
    @Bean
    @ConditionalOnMissingBean(ContentSafetyAdvisor.class)
    @ConditionalOnProperty(name = "eagle.ai.safety.enabled", havingValue = "true")
    public ContentSafetyAdvisor contentSafetyAdvisor(AiProperties properties) {
        return new ContentSafetyAdvisor(properties);
    }

    /**
     * AI 调用审计 Advisor。
     *
     * <p>记录每次调用的延迟、Token 用量、模型名，并通过 {@link ApplicationEventPublisher}
     * 发布 {@link com.eagle.ai.event.AiCallAuditEvent}，供消费方异步持久化审计日志。
     */
    @Bean
    @ConditionalOnMissingBean(AiAuditAdvisor.class)
    public AiAuditAdvisor aiAuditAdvisor(ApplicationEventPublisher eventPublisher) {
        return new AiAuditAdvisor(eventPublisher);
    }

    /**
     * 预配置的 {@link ChatClient} Bean。
     *
     * <p>默认装配（按 Advisor 执行顺序）：
     * <ol>
     *   <li>{@link ContentSafetyAdvisor}（HIGHEST_PRECEDENCE+300）— 内容过滤，存在时优先拦截</li>
     *   <li>{@link AiRateLimitAdvisor}（HIGHEST_PRECEDENCE+100）— 限流，存在时激活</li>
     *   <li>{@link com.eagle.ai.advisor.TokenBudgetAdvisor}（HIGHEST_PRECEDENCE+200）— 配额，存在时激活</li>
     *   <li>{@link MessageChatMemoryAdvisor} — 多轮对话记忆</li>
     *   <li>{@link TokenUsageAdvisor}（LOWEST_PRECEDENCE-100）— Token 指标，存在时激活</li>
     *   <li>{@link AiAuditAdvisor}（LOWEST_PRECEDENCE-200）— 审计，始终激活</li>
     * </ol>
     *
     * <p>调用示例（携带会话 ID 以隔离不同用户的对话历史）：
     * <pre>{@code
     * chatClient.prompt()
     *     .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
     *                            AiConversationKey.of(tenantId, userId))
     *                      .param("tenantId", tenantId))
     *     .user("你好")
     *     .call()
     *     .content();
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean(ChatClient.class)
    @ConditionalOnBean(ChatModel.class)
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 AiProperties properties,
                                 ObjectProvider<TokenUsageAdvisor> tokenUsageAdvisorProvider,
                                 ObjectProvider<AiRateLimitAdvisor> rateLimitAdvisorProvider,
                                 ObjectProvider<ContentSafetyAdvisor> safetyAdvisorProvider,
                                 ObjectProvider<AiAuditAdvisor> auditAdvisorProvider) {
        var b = builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        );

        // 可选：内容安全（safety.enabled=true 时）
        safetyAdvisorProvider.ifAvailable(b::defaultAdvisors);

        // 可选：限流（rate-limit.enabled=true 且 Redis 可用时）
        rateLimitAdvisorProvider.ifAvailable(b::defaultAdvisors);

        // 可选：Token 指标（metrics.enabled=true 且 MeterRegistry 可用时）
        tokenUsageAdvisorProvider.ifAvailable(b::defaultAdvisors);

        // 审计：始终装配（ApplicationEventPublisher 始终可用）
        auditAdvisorProvider.ifAvailable(b::defaultAdvisors);

        // 可选：全局 System Prompt
        if (StringUtils.hasText(properties.getChat().getSystemPrompt())) {
            b.defaultSystem(properties.getChat().getSystemPrompt());
        }

        return b.build();
    }
}
