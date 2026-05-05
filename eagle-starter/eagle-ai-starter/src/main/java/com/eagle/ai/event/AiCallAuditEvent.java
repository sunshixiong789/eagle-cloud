package com.eagle.ai.event;

/**
 * AI 调用审计事件。
 *
 * <p>由 {@link com.eagle.ai.advisor.AiAuditAdvisor} 在每次 AI 调用完成后发布，
 * 消费方可通过 {@code @EventListener} 或 {@code @TransactionalEventListener} 处理：
 * <pre>{@code
 * @Async
 * @EventListener
 * public void onAiCall(AiCallAuditEvent event) {
 *     auditLogService.save(event);
 * }
 * }</pre>
 *
 * @param conversationId  会话 ID（来自 advisor context）
 * @param tenantId        租户 ID（来自 advisor context，可能为 null）
 * @param model           模型名称（来自响应元数据，可能为 unknown）
 * @param inputTokens     输入 Token 数
 * @param outputTokens    输出 Token 数
 * @param totalTokens     总 Token 数
 * @param latencyMs       本次调用延迟（毫秒）
 * @param success         是否成功（false 表示发生异常）
 */
public record AiCallAuditEvent(
        String conversationId,
        String tenantId,
        String model,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        long latencyMs,
        boolean success
) {}
