package com.eagle.ai.config;

import com.eagle.ai.advisor.AiRateLimitAdvisor;
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
 *   <li>{@link ChatClient} — 预装配记忆 + 指标 + 限流 Advisor 的 ChatClient</li>
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
     * 预配置的 {@link ChatClient} Bean。
     *
     * <p>默认装配：
     * <ol>
     *   <li>{@link MessageChatMemoryAdvisor} — 多轮对话记忆</li>
     *   <li>{@link TokenUsageAdvisor} — Token 用量指标（存在时自动装配）</li>
     *   <li>{@link AiRateLimitAdvisor} — 限流（存在时自动装配）</li>
     *   <li>全局 System Prompt（配置了 {@code eagle.ai.chat.system-prompt} 时生效）</li>
     * </ol>
     *
     * <p>调用示例（携带会话 ID 以隔离不同用户的对话历史）：
     * <pre>{@code
     * chatClient.prompt()
     *     .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
     *                            AiConversationKey.of(tenantId, userId)))
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
                                 ObjectProvider<AiRateLimitAdvisor> rateLimitAdvisorProvider) {
        var b = builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        );

        // 可选：Token 用量指标
        tokenUsageAdvisorProvider.ifAvailable(b::defaultAdvisors);

        // 可选：限流
        rateLimitAdvisorProvider.ifAvailable(b::defaultAdvisors);

        // 可选：全局 System Prompt
        if (StringUtils.hasText(properties.getChat().getSystemPrompt())) {
            b.defaultSystem(properties.getChat().getSystemPrompt());
        }

        return b.build();
    }
}
