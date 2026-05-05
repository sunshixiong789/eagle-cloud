package com.eagle.ai.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
 *       model: gpt-4o
 *       temperature: 0.7
 *       max-tokens: 2048
 *       timeout: 30s
 *     memory:
 *       ttl: 7d
 *     rate-limit:
 *       enabled: true
 *       requests-per-minute: 30
 *       per-tenant: true
 *       tenant-requests-per-minute: 300
 *     budget:
 *       enabled: true
 *       default-monthly-tokens: 1000000
 *     safety:
 *       enabled: true
 *       blocked-patterns:
 *         - "(?i)\\b(password|secret|token)\\b"
 *       check-output: false
 *     metrics:
 *       enabled: true
 *       include-model-tag: true
 *       include-tenant-tag: true
 *     resilience:
 *       enabled: true
 *       instance-name: eagle-ai-default
 *     embedding:
 *       enabled: true
 *       default-top-k: 4
 * }</pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "eagle.ai")
public class AiProperties {

    /** 是否启用 Eagle AI 自动配置（默认开启）。 */
    private boolean enabled = true;

    /** 对话（Chat）配置。 */
    @Valid
    private Chat chat = new Chat();

    /** 对话记忆存储配置。 */
    @Valid
    private Memory memory = new Memory();

    /** 限流配置（需要 Redis）。 */
    @Valid
    private RateLimit rateLimit = new RateLimit();

    /** 指标上报配置（需要 Micrometer）。 */
    @Valid
    private Metrics metrics = new Metrics();

    /** Token 月度配额管理配置（需要 Redis）。 */
    @Valid
    private Budget budget = new Budget();

    /** 内容安全过滤配置。 */
    @Valid
    private Safety safety = new Safety();

    /** Resilience4J 容错配置。 */
    @Valid
    private Resilience resilience = new Resilience();

    /** Embedding 向量检索配置（需要 EmbeddingModel）。 */
    @Valid
    private Embedding embedding = new Embedding();

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
        @Positive
        private int memoryWindowSize = 10;

        /**
         * 模型名称，null 则使用提供商 starter 默认配置。
         * 示例：{@code gpt-4o}、{@code claude-sonnet-4-6}、{@code qwen-max}。
         */
        private String model;

        /**
         * 生成温度（0.0–2.0），null 则使用提供商默认值。
         * 值越低输出越稳定，越高越有创意。
         */
        private Double temperature;

        /**
         * 单次生成最大 Token 数，null 则使用提供商默认值。
         */
        private Integer maxTokens;

        /**
         * 单次 AI 调用超时，默认 30 秒。用于 TimeLimiter / @TimeLimiter 注解。
         */
        @NotNull
        private Duration timeout = Duration.ofSeconds(30);
    }

    @Data
    public static class Memory {

        /**
         * 对话历史在 Redis 中的过期时间，默认 7 天。
         * 仅在使用 Redis 存储时生效；InMemory 存储随进程生命周期。
         */
        @NotNull
        private Duration ttl = Duration.ofDays(7);

        /**
         * Redis key 前缀，默认 {@code eagle:ai:chat:memory}。
         */
        @NotBlank
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
        @Positive
        private int requestsPerMinute = 60;

        /**
         * true = 按 conversationId 独立限流；false = 全局共享限流桶。
         */
        private boolean perConversation = true;

        /**
         * 是否同时按租户（tenantId）维度限流。
         * 需要调用方在 context 中传入 {@code tenantId} 键。
         */
        private boolean perTenant = false;

        /**
         * 每个租户每分钟最大请求次数，默认 300。仅 {@code perTenant=true} 时生效。
         */
        @Positive
        private int tenantRequestsPerMinute = 300;
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
        @NotBlank
        private String prefix = "eagle.ai";

        /**
         * 是否在指标 Tag 中包含模型名（{@code model} Tag），默认开启。
         * 关闭可减少指标基数（cardinality）。
         */
        private boolean includeModelTag = true;

        /**
         * 是否在指标 Tag 中包含租户 ID（{@code tenant} Tag），默认开启。
         * 需要调用方在 context 中传入 {@code tenantId} 键。
         */
        private boolean includeTenantTag = true;
    }

    @Data
    public static class Budget {

        /**
         * 是否启用月度 Token 配额管理（需要 Redis）。默认关闭。
         */
        private boolean enabled = false;

        /**
         * 每个租户/用户默认月度 Token 上限，默认 100 万 Token。
         */
        @Positive
        private long defaultMonthlyTokens = 1_000_000L;

        /**
         * Redis key 前缀，默认 {@code eagle:ai:budget}。
         * 完整 key 格式：{prefix}:{yyyy-MM}:{identifier}
         */
        @NotBlank
        private String keyPrefix = "eagle:ai:budget";
    }

    @Data
    public static class Safety {

        /**
         * 是否启用内容安全过滤。默认关闭。
         */
        private boolean enabled = false;

        /**
         * 输入内容黑名单正则列表，匹配则拒绝请求（抛 90004）。
         * 示例：{@code ["(?i)\\bpassword\\b", "(?i)\\bsecret\\b"]}。
         */
        private List<String> blockedPatterns = new ArrayList<>();

        /**
         * 是否同时对输出内容做安全检查，默认仅检查输入。
         */
        private boolean checkOutput = false;
    }

    @Data
    public static class Resilience {

        /**
         * 是否自动注册 AI 专属 Resilience4J 实例，默认开启。
         * 需要 {@code eagle-resilience-starter} 在类路径。
         */
        private boolean enabled = true;

        /**
         * 注册的 CircuitBreaker / TimeLimiter 实例名称，默认 {@code eagle-ai-default}。
         * 可在 {@code @CircuitBreaker(name = "eagle-ai-default")} 中引用。
         */
        @NotBlank
        private String instanceName = "eagle-ai-default";

        /**
         * 慢调用判定时间（AI 场景宽松），默认 10 秒。
         */
        @NotNull
        private Duration slowCallDurationThreshold = Duration.ofSeconds(10);
    }

    @Data
    public static class Embedding {

        /**
         * 是否启用 Embedding / RAG 支持，默认开启（需要 EmbeddingModel）。
         */
        private boolean enabled = true;

        /**
         * RAG 相似度检索默认返回条数（topK），默认 4。
         */
        @Positive
        private int defaultTopK = 4;

        /**
         * RAG 相似度阈值（0.0–1.0），低于此分数的文档不返回，默认 0.7。
         */
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double defaultSimilarityThreshold = 0.7;
    }
}
