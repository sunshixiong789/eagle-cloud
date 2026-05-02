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
 * <p>指标 Tag：{@code type}（call / stream）。
 * 消费方可在此基础上叠加 {@code model}、{@code tenant} 等 Tag，
 * 只需注册自定义 {@link TokenUsageAdvisor} Bean 覆盖即可。
 *
 * <p>流式场景下，token 数据通常仅出现在最后一条 {@link ChatResponse}，
 * 本 Advisor 通过 {@code after} 回调在 finish reason 出现时记录。
 */
public class TokenUsageAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenUsageAdvisor.class);

    /** 在所有 Advisor 链的最外层运行，确保最终响应已完整。 */
    private static final int ORDER = Ordered.LOWEST_PRECEDENCE - 100;

    private final MeterRegistry meterRegistry;
    private final String metricPrefix;

    public TokenUsageAdvisor(MeterRegistry meterRegistry, AiProperties properties) {
        this.meterRegistry = meterRegistry;
        this.metricPrefix = properties.getMetrics().getPrefix();
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

        Long inputTokens = usage.getPromptTokens();
        Long outputTokens = usage.getGenerationTokens();
        Long totalTokens = usage.getTotalTokens();

        if (totalTokens == null || totalTokens == 0) {
            return;
        }

        counter("token.input").increment(inputTokens != null ? inputTokens : 0);
        counter("token.output").increment(outputTokens != null ? outputTokens : 0);
        counter("token.total").increment(totalTokens);

        log.debug("AI token usage: input={}, output={}, total={}", inputTokens, outputTokens, totalTokens);
    }

    private Counter counter(String name) {
        return Counter.builder(metricPrefix + "." + name)
                .register(meterRegistry);
    }
}
