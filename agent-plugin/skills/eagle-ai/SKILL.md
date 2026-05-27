---
name: eagle-ai
description: Use when integrating AI capabilities in eagle-cloud projects — Spring AI ChatClient with advisors (rate limit, token budget, content safety, audit, Resilience4J), RedisChatMemoryRepository for conversation history, EmbeddingClient RAG retrieval, AiProperties configuration
---

# eagle-ai-starter — Spring AI 对话 / Embedding / RAG

## 何时使用

- 集成 LLM 对话（ChatClient），需要限流、Token 配额、内容安全、审计
- 对话历史持久化到 Redis（`RedisChatMemoryRepository`）
- RAG 检索（`EmbeddingClient` + VectorStore）
- AI 调用 Micrometer 指标上报（Token 用量、模型、租户维度）

## 何时不要使用

- 纯规则引擎决策 → 不需要 LLM
- 仅需要 Embedding 不需要对话 → 可不引 chat 相关配置

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-ai-starter')
// 还需要 LLM provider starter，例如：
implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
// 或
implementation 'org.springframework.ai:spring-ai-zhipuai-spring-boot-starter'
```

引入后 `ChatClient.Builder`、`RedisChatMemoryRepository`（需 Redis）、内置 Advisors 自动注册。

```yaml
eagle:
  ai:
    chat:
      system-prompt: "你是一个专业的企业助手，请用中文回答问题。"
      memory-window-size: 10        # 携带历史消息条数
      model: gpt-4o                 # null 则使用 provider 默认
      temperature: 0.7
      max-tokens: 2048
      timeout: 30s
    memory:
      ttl: 7d
      key-prefix: "eagle:ai:chat:memory"
    rate-limit:
      enabled: false
      requests-per-minute: 60
      per-conversation: true
      per-tenant: false
      tenant-requests-per-minute: 300
    budget:
      enabled: false
      default-monthly-tokens: 1000000
    safety:
      enabled: false
      blocked-patterns:
        - "(?i)\\b(password|secret|token)\\b"
      check-output: false
    metrics:
      enabled: true
      prefix: "eagle.ai"
      include-model-tag: true
      include-tenant-tag: true
    resilience:
      enabled: true
      instance-name: eagle-ai-default
      slow-call-duration-threshold: 10s
    embedding:
      enabled: true
      default-top-k: 4
      default-similarity-threshold: 0.7
```

## 核心组件

| 类 / 接口                    | 用途                                                                  |
|-----------------------------|---------------------------------------------------------------------|
| `AiRateLimitAdvisor`        | 限流 Advisor（需 Redis），防止 AI 调用被刷接口                                    |
| `TokenBudgetAdvisor`        | 月度 Token 配额管理（需 Redis），超限抛 `AI_BUDGET_EXCEEDED`                     |
| `ContentSafetyAdvisor`      | 黑名单正则过滤，命中抛 `AI_CONTENT_BLOCKED`                                    |
| `AiAuditAdvisor`            | 操作审计，发布 `AiCallAuditEvent`（可被 `eagle-audit-log-starter` 捕获）         |
| `TokenUsageAdvisor`         | Token 用量统计，上报 Micrometer 指标 `eagle.ai.token.*`                      |
| `RedisChatMemoryRepository` | 对话历史存 Redis（`List` 数据结构），`keyPrefix + conversationId`               |
| `AiErrorCode`               | AI 域错误码：`AI_RATE_LIMIT_EXCEEDED(90001)` / `AI_CONTENT_BLOCKED(90004)` |

## 最小示例

```java
// 1) 注入 ChatClient.Builder（starter 已装配 Advisors）
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final ChatClient chatClient;

    /** 带对话记忆的单轮对话 */
    public String chat(String conversationId, String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor.param(
                        AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .call()
                .content();
    }
}

// 2) 注入 ChatClient.Builder 自定义构建（更细粒度控制）
@Configuration
class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          RedisChatMemoryRepository memoryRepository) {
        return builder
                .defaultSystem("你是 Eagle 平台助手")
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(memoryRepository),
                        new AiRateLimitAdvisor(rateLimiter),
                        new ContentSafetyAdvisor(safety),
                        new TokenUsageAdvisor(meterRegistry, "eagle.ai")
                )
                .build();
    }
}

// 3) 流式输出
public Flux<String> streamChat(String conversationId, String userMessage) {
    return chatClient.prompt()
            .user(userMessage)
            .advisors(a -> a.param(
                    AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
            .stream()
            .content();
}

// 4) RAG 检索
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public List<Document> search(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.query(query).withTopK(4).withSimilarityThreshold(0.7));
    }
}
```

## 配置项速查

| key                                          | 默认          | 说明                      |
|----------------------------------------------|-------------|-------------------------|
| `eagle.ai.chat.system-prompt`                | 空           | 系统提示词                   |
| `eagle.ai.chat.memory-window-size`           | `10`        | 携带历史消息条数                |
| `eagle.ai.chat.timeout`                      | `30s`       | 单次调用超时                  |
| `eagle.ai.memory.ttl`                        | `7d`        | Redis 对话历史 TTL          |
| `eagle.ai.rate-limit.enabled`                | `false`     | 开启限流（需 Redis）           |
| `eagle.ai.rate-limit.requests-per-minute`    | `60`        | 每 conversationId 每分钟限制  |
| `eagle.ai.budget.enabled`                    | `false`     | 开启月度 Token 配额（需 Redis）  |
| `eagle.ai.budget.default-monthly-tokens`     | `1_000_000` | 默认月度 Token 上限           |
| `eagle.ai.safety.enabled`                    | `false`     | 开启内容安全过滤                |
| `eagle.ai.resilience.instance-name`          | `eagle-ai-default` | R4J 实例名             |
| `eagle.ai.embedding.default-top-k`           | `4`         | RAG topK               |
| `eagle.ai.embedding.default-similarity-threshold` | `0.7`  | RAG 相似度阈值              |

## 常见错误

- ❌ 直接 `new ChatClient(...)` → ✅ 注入 `ChatClient.Builder` 构建，Advisors 自动装配
- ❌ `eagle.ai.rate-limit.enabled=true` 未引 `eagle-redis-starter` → ✅ 限流 Advisor 需要 Redis
- ❌ 对话历史无限增长 → ✅ 配置 `memory-window-size` 控制窗口长度
- ❌ 未设置 `conversationId` → ✅ 缺少 `CHAT_MEMORY_CONVERSATION_ID_KEY` 参数时记忆不生效

## 关联规则

- `.claude/rules/12-security.md` — 敏感字段不进 Prompt
- `.claude/rules/14-cache.md` — Redis TTL 设计
- `.claude/rules/21-resilience.md` — Resilience4J 熔断
