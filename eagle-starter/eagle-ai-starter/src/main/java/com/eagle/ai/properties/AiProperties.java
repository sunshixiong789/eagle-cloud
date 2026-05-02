package com.eagle.ai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Eagle AI 配置属性。
 *
 * <p>所有属性均有默认值，无需额外配置即可启动。典型配置示例：
 * <pre>{@code
 * eagle:
 *   ai:
 *     chat:
 *       system-prompt: "你是一个专业的企业级助手，请用中文回答问题。"
 *       memory-window-size: 20
 *     memory:
 *       ttl: 7d
 *     rate-limit:
 *       enabled: true
 *       requests-per-minute: 30
 *     metrics:
 *       enabled: true
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "eagle.ai")
public class AiProperties {

    /** 是否启用 Eagle AI 自动配置（默认开启）。 */
    private boolean enabled = true;

    /** 对话（Chat）配置。 */
    private Chat chat = new Chat();

    /** 对话记忆存储配置。 */
    private Memory memory = new Memory();

    /** 限流配置（需要 Redis）。 */
    private RateLimit rateLimit = new RateLimit();

    /** 指标上报配置（需要 Micrometer）。 */
    private Metrics metrics = new Metrics();

    // ==================== 嵌套配置类 ====================

    @Data
    public static class Chat {

        /**
         * 默认系统提示词（System Prompt）。
         * 不填则使用提供商默认行为；也可在调用时通过 {@code ChatClient.prompt().system(...)} 覆盖。
         */
        private String systemPrompt;

        /**
         * 对话记忆窗口大小 —— 每次请求携带的历史消息条数上限，默认 10。
         * 调大此值可保留更长上下文，但会增加 Token 消耗。
         */
        private int memoryWindowSize = 10;
    }

    @Data
    public static class Memory {

        /**
         * 对话历史在 Redis 中的过期时间，默认 7 天。
         * 仅在使用 Redis 存储时生效；InMemory 存储随进程生命周期。
         */
        private Duration ttl = Duration.ofDays(7);

        /**
         * Redis key 前缀，默认 {@code eagle:ai:chat:memory}。
         */
        private String keyPrefix = "eagle:ai:chat:memory";
    }

    @Data
    public static class RateLimit {

        /**
         * 是否开启 AI 调用限流（需要 Redis）。默认关闭。
         */
        private boolean enabled = false;

        /**
         * 每个会话每分钟最大请求次数，默认 60。
         */
        private int requestsPerMinute = 60;

        /**
         * true = 按 conversationId 独立限流；false = 全局共享限流桶。
         */
        private boolean perConversation = true;
    }

    @Data
    public static class Metrics {

        /**
         * 是否启用 Token 用量 Micrometer 指标，默认开启。
         */
        private boolean enabled = true;

        /**
         * 指标名前缀，默认 {@code eagle.ai}。
         * 最终指标名：{prefix}.token.input / {prefix}.token.output / {prefix}.token.total
         */
        private String prefix = "eagle.ai";
    }
}
