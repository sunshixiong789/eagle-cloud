package com.eagle.ai.advisor;

import com.eagle.ai.exception.AiErrorCode;
import com.eagle.ai.properties.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * AI 调用限流 Advisor（基于 Redis INCR + EXPIRE 滑动分钟窗口）。
 *
 * <p>默认按 {@code conversationId} 独立限流（可通过 {@code eagle.ai.rate-limit.per-conversation=false}
 * 改为全局限流）。超限时抛出 {@link com.eagle.common.exception.ServiceException}（90001）。
 *
 * <p>Redis key 格式：{@code eagle:ai:rate:limit:{limitKey}}，TTL 60 秒。
 *
 * <p>仅在 {@code eagle.ai.rate-limit.enabled=true} 且 Redis 可用时注册此 Bean。
 */
public class AiRateLimitAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimitAdvisor.class);

    /** 在 TokenUsageAdvisor 内侧运行，限流通过后才执行真正 AI 调用。 */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;
    private static final String KEY_PREFIX = "eagle:ai:rate:limit:";
    private static final String GLOBAL_KEY = "global";

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;
    private final boolean perConversation;

    public AiRateLimitAdvisor(StringRedisTemplate redisTemplate, AiProperties properties) {
        this.redisTemplate = redisTemplate;
        this.requestsPerMinute = properties.getRateLimit().getRequestsPerMinute();
        this.perConversation = properties.getRateLimit().isPerConversation();
    }

    @Override
    public String getName() {
        return "AiRateLimitAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        checkRateLimit(request);
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    // ==================== 内部工具 ====================

    private void checkRateLimit(ChatClientRequest request) {
        String limitKey = buildLimitKey(request);
        if (!tryAcquire(limitKey)) {
            log.warn("AI rate limit exceeded for key={}, limit={}/min", limitKey, requestsPerMinute);
            throw AiErrorCode.AI_RATE_LIMIT_EXCEEDED.toServiceException();
        }
    }

    private String buildLimitKey(ChatClientRequest request) {
        if (!perConversation) {
            return GLOBAL_KEY;
        }
        Object conversationId = request.context().get(ChatMemory.CONVERSATION_ID);
        return conversationId != null ? conversationId.toString() : GLOBAL_KEY;
    }

    /**
     * 基于 Redis INCR + EXPIRE 的简单分钟级限流。
     * 第一次 INCR 后设置 60s TTL，窗口内累计 INCR 直到 TTL 到期后自动重置。
     */
    private boolean tryAcquire(String limitKey) {
        String redisKey = KEY_PREFIX + limitKey;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            // 第一次写入，设置窗口过期
            redisTemplate.expire(redisKey, 60, TimeUnit.SECONDS);
        }
        return count <= requestsPerMinute;
    }
}
