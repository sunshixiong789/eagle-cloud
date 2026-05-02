package com.eagle.ai.util;

import org.jspecify.annotations.Nullable;

/**
 * AI 会话 key 工具类。
 *
 * <p>多租户场景下，必须将 {@code tenantId} 前缀嵌入 conversationId，
 * 以防止不同租户之间的对话历史相互污染。
 *
 * <p>推荐用法：
 * <pre>{@code
 * // 单租户 / 内部系统
 * String key = AiConversationKey.of(userId);
 *
 * // 多租户 SaaS 场景（传入当前租户 ID）
 * String key = AiConversationKey.of(TenantContextHolder.getTenantId(), userId);
 *
 * // 在 ChatClient 调用时传入
 * chatClient.prompt()
 *     .advisors(a -> a.param(MessageChatMemoryAdvisor.CONVERSATION_ID_KEY, key))
 *     .user(message)
 *     .call()
 *     .content();
 * }</pre>
 */
public final class AiConversationKey {

    private static final String SEPARATOR = ":";

    private AiConversationKey() {
    }

    /**
     * 生成单租户会话 key，直接使用 userId 作为 key。
     *
     * @param userId 用户唯一标识（非 null）
     * @return conversationId
     */
    public static String of(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return userId;
    }

    /**
     * 生成多租户会话 key，格式：{tenantId}:{userId}。
     *
     * <p>若 {@code tenantId} 为 null 或空白，则退化为 {@link #of(String)}，
     * 避免在单租户环境错误调用时引发异常。
     *
     * @param tenantId 租户唯一标识（可为 null）
     * @param userId   用户唯一标识（非 null）
     * @return conversationId
     */
    public static String of(@Nullable String tenantId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            return userId;
        }
        return tenantId + SEPARATOR + userId;
    }
}
