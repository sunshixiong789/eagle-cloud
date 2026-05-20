package com.eagle.ai.advisor;

import com.eagle.ai.event.AiCallAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;

/**
 * AI 调用审计 Advisor。
 *
 * <p>记录每次 AI 调用的关键信息并发布 {@link AiCallAuditEvent}，供消费方持久化审计日志。
 * 同时向 {@code audit.eagle.ai} logger 输出结构化 INFO 日志（可通过 ELK 采集）。
 *
 * <p>记录字段：
 * <ul>
 *   <li>conversationId — 从 context 读取</li>
 *   <li>tenantId — 从 context 读取</li>
 *   <li>model — 从响应元数据读取</li>
 *   <li>inputTokens / outputTokens / totalTokens</li>
 *   <li>latencyMs — before() 到 after() 的时间差（ThreadLocal 存储）</li>
 *   <li>success — after() 正常完成为 true</li>
 * </ul>
 *
 * <p>消费方订阅事件示例：
 * <pre>{@code
 * @Async
 * @EventListener
 * public void onAiCall(AiCallAuditEvent event) {
 *     aiAuditRepository.save(AiAuditRecord.from(event));
 * }
 * }</pre>
 */
public class AiAuditAdvisor implements BaseAdvisor {

    static final String CONTEXT_KEY_TENANT = "tenantId";
    private static final Logger log = LoggerFactory.getLogger(AiAuditAdvisor.class);
    private static final Logger auditLog = LoggerFactory.getLogger("audit.eagle.ai");
    /**
     * 在 TokenUsageAdvisor（LOWEST_PRECEDENCE-100）之前运行，
     * 确保审计日志在 token 指标记录之后获取数据。
     */
    private static final int ORDER = Ordered.LOWEST_PRECEDENCE - 200;
    private static final String UNKNOWN = "unknown";
    private final ApplicationEventPublisher eventPublisher;
    private final ThreadLocal<Long> startTimeHolder = new ThreadLocal<>();

    public AiAuditAdvisor(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getName() {
        return "AiAuditAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        startTimeHolder.set(System.currentTimeMillis());
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        try {
            publishAuditEvent(response, true);
        } finally {
            startTimeHolder.remove();
        }
        return response;
    }

    // ==================== 内部工具 ====================

    private void publishAuditEvent(ChatClientResponse response, boolean success) {
        Long startTime = startTimeHolder.get();
        long latencyMs = startTime != null ? System.currentTimeMillis() - startTime : -1;

        String conversationId = resolveString(response.context().get(ChatMemory.CONVERSATION_ID));
        String tenantId = resolveString(response.context().get(CONTEXT_KEY_TENANT));
        String model = UNKNOWN;
        int inputTokens = 0;
        int outputTokens = 0;
        int totalTokens = 0;

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            String m = chatResponse.getMetadata().getModel();
            if (m != null && !m.isBlank()) {
                model = m;
            }
            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage != null) {
                inputTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                outputTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens() : 0;
            }
        }

        auditLog.info("conversationId={}, tenantId={}, model={}, inputTokens={}, outputTokens={}, totalTokens={}, latencyMs={}, success={}",
                conversationId, tenantId, model, inputTokens, outputTokens, totalTokens, latencyMs, success);

        AiCallAuditEvent event = new AiCallAuditEvent(
                conversationId, tenantId, model,
                inputTokens, outputTokens, totalTokens,
                latencyMs, success);
        eventPublisher.publishEvent(event);
    }

    private String resolveString(Object value) {
        return value != null ? value.toString() : null;
    }
}
