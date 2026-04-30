package com.eagle.ai.config;

import com.eagle.ai.properties.AiProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
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
 * <p>在任意 Spring AI 提供商 starter（如 spring-ai-starter-model-openai）存在时自动装配：
 * <ul>
 *   <li>{@link ChatMemoryRepository} — 对话历史存储，默认内存实现，可替换为 JDBC/Redis 等</li>
 *   <li>{@link ChatMemory} — 带滑动窗口的对话记忆，窗口大小由 {@code eagle.ai.chat.memory-window-size} 控制</li>
 *   <li>{@link ChatClient} — 预配置记忆 Advisor 和系统提示词的 Chat 客户端，可直接注入使用</li>
 * </ul>
 *
 * <p>三个 Bean 均标注 {@code @ConditionalOnMissingBean}，应用可自由覆盖任意一个。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class AiAssistantService {
 *
 *     private final ChatClient chatClient;
 *
 *     public String chat(String conversationId, String userMessage) {
 *         return chatClient.prompt()
 *                 .advisors(a -> a.param(MessageChatMemoryAdvisor.CONVERSATION_ID_KEY, conversationId))
 *                 .user(userMessage)
 *                 .call()
 *                 .content();
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
     * 对话历史存储 Bean。
     *
     * <p>默认使用内存存储，重启后历史清空。
     * 生产环境可替换为持久化存储：
     * <pre>{@code
     * @Bean
     * public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
     *     return new JdbcChatMemoryRepository(jdbcTemplate);
     * }
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 对话记忆 Bean，使用滑动窗口策略限制历史消息数量。
     *
     * <p>窗口大小由 {@code eagle.ai.chat.memory-window-size} 配置（默认 10）。
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
     * 预配置的 {@link ChatClient} Bean。
     *
     * <p>默认挂载 {@link MessageChatMemoryAdvisor}，支持多轮对话记忆。
     * 若配置了 {@code eagle.ai.chat.system-prompt}，则全局注入系统提示词。
     *
     * <p>调用时通过 Advisor 参数传入会话 ID 以隔离不同用户的对话历史：
     * <pre>{@code
     * chatClient.prompt()
     *     .advisors(a -> a.param(MessageChatMemoryAdvisor.CONVERSATION_ID_KEY, userId.toString()))
     *     .user("你好")
     *     .call()
     *     .content();
     * }</pre>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 AiProperties properties) {
        var b = builder.defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
        );
        if (StringUtils.hasText(properties.getChat().getSystemPrompt())) {
            b.defaultSystem(properties.getChat().getSystemPrompt());
        }
        return b.build();
    }
}
