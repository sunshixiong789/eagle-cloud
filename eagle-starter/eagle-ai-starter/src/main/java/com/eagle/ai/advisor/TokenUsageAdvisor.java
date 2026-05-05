package com.eagle.ai.advisor;

import com.eagle.ai.properties.AiProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;

/**
 * Token 用量指标 Advisor。
 *
 * <p>拦截每次 AI 调用，从 {@link ChatResponse} 元数据中提取 Token 用量，
 * 并上报到 Micrometer：
 * <ul>
 *   <li>{@code {prefix}.token.input}  — prompt/input tokens</li>
 *   <li>{@code {prefix}.token.output} — completion/output tokens</li>
 *   <li>{@code {prefix}.token.total}  — total tokens</li>
 * </ul>
 *
 * <p>指标 Tag（按配置可选）：
 * <ul>
 *   <li>{@code model} — 从响应元数据提取，未知时为 {@code unknown}</li>
 *   <li>{@code tenant} — 从 advisor context 中读取 {@code tenantId} 键，
 *       调用方可在 {@code ChatClient.prompt().advisors(a -> a.param("tenantId", tid))} 中传入</li>
 * </ul>
 *
 * <p>流式场景下，token 数据通常仅出现在最后一条 {@link ChatResponse}，
 * 本 Advisor 通过 {@code after} 回调在 finish reason 出现时记录。
 */
public class TokenUsageAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    /** 在所有 Advisor 链的最内层运行（最晚 before，最早 after），确保获取到完整响应。 */
    private static final int ORDER = Ordered.LOWEST_PRECEDENCE - 100;

    private static final String TAG_MODEL = "model";
    private static final String TAG_TENANT = "tenant";
    private static final String UNKNOWN = "unknown";
    static final String CONTEXT_KEY_TENANT = "tenantId";

    private final MeterRegistry meterRegistry;
    private final String metricPrefix;
    private final boolean includeModelTag;
    private final boolean includeTenantTag;

    public TokenUsageAdvisor(MeterRegistry meterRegistry, AiProperties properties) {
        this.meterRegistry = meterRegistry;
        this.metricPrefix = properties.getMetrics().getPrefix();
        this.includeModelTag = properties.getMetrics().isIncludeModelTag();
        this.includeTenantTag = properties.getMetrics().isIncludeTenantTag();
    }

    @Override
    public String getName() {
        return "TokenUsageAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        recordUsage(response);
        return response;
    }

    // ==================== 内部工具 ====================

    private void recordUsage(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }

        Integer inputTokens = usage.getPromptTokens();
        Integer outputTokens = usage.getCompletionTokens();
        Integer totalTokens = usage.getTotalTokens();

        if (totalTokens == null || totalTokens == 0) {
            return;
        }

        String model = resolveModel(chatResponse);
        String tenant = resolveTenant(response);

        counter("token.input", model, tenant).increment(inputTokens != null ? inputTokens : 0);
        counter("token.output", model, tenant).increment(outputTokens != null ? outputTokens : 0);
        counter("token.total", model, tenant).increment(totalTokens);

        log.debug("AI token usage: input={}, output={}, total={}, model={}, tenant={}",
                inputTokens, outputTokens, totalTokens, model, tenant);
    }

    private String resolveModel(ChatResponse chatResponse) {
        if (!includeModelTag) {
            return UNKNOWN;
        }
        String model = chatResponse.getMetadata().getModel();
        return (model != null && !model.isBlank()) ? model : UNKNOWN;
    }

    private String resolveTenant(ChatClientResponse response) {
        if (!includeTenantTag) {
            return UNKNOWN;
        }
        Object tenantId = response.context().get(CONTEXT_KEY_TENANT);
        return tenantId != null ? tenantId.toString() : UNKNOWN;
    }

    private Counter counter(String name, String model, String tenant) {
        Counter.Builder builder = Counter.builder(metricPrefix + "." + name);
        if (includeModelTag) {
            builder.tag(TAG_MODEL, model);
        }
        if (includeTenantTag) {
            builder.tag(TAG_TENANT, tenant);
        }
        return builder.register(meterRegistry);
    }
}
