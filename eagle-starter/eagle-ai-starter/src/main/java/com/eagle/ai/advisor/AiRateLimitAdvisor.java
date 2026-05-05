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
 * <p>支持两个维度独立限流（均可独立开关）：
 * <ol>
 *   <li><b>会话维度</b>（{@code per-conversation=true}）：按 {@code conversationId} 独立限流，
 *       默认每分钟 60 次</li>
 *   <li><b>租户维度</b>（{@code per-tenant=true}）：按 {@code tenantId} 独立限流，
 *       默认每分钟 300 次；需调用方在 context 中传入 {@code tenantId} 键</li>
 * </ol>
 *
 * <p>超限时抛出 {@link com.eagle.common.exception.ServiceException}（90001）。
 *
 * <p>Redis key 格式：
 * <ul>
 *   <li>会话：{@code eagle:ai:rate:limit:conv:{conversationId}}</li>
 *   <li>租户：{@code eagle:ai:rate:limit:tenant:{tenantId}}</li>
 * </ul>
 * TTL 60 秒，窗口到期自动重置。
 *
 * <p>仅在 {@code eagle.ai.rate-limit.enabled=true} 且 Redis 可用时注册此 Bean。
 */
public class AiRateLimitAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimitAdvisor.class);

    /** 在所有 Advisor 链的最外侧运行（最早 before），限流通过后才进入后续链路。 */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;
    private static final String KEY_PREFIX = "eagle:ai:rate:limit:";
    private static final String GLOBAL_KEY = "global";

    static final String CONTEXT_KEY_TENANT = "tenantId";

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;
    private final boolean perConversation;
    private final boolean perTenant;
    private final int tenantRequestsPerMinute;

    public AiRateLimitAdvisor(StringRedisTemplate redisTemplate, AiProperties properties) {
        this.redisTemplate = redisTemplate;
        AiProperties.RateLimit cfg = properties.getRateLimit();
        this.requestsPerMinute = cfg.getRequestsPerMinute();
        this.perConversation = cfg.isPerConversation();
        this.perTenant = cfg.isPerTenant();
        this.tenantRequestsPerMinute = cfg.getTenantRequestsPerMinute();
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
        checkConversationLimit(request);
        checkTenantLimit(request);
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    // ==================== 内部工具 ====================

    private void checkConversationLimit(ChatClientRequest request) {
        if (!perConversation) {
            return;
        }
        Object conversationId = request.context().get(ChatMemory.CONVERSATION_ID);
        String limitKey = "conv:" + (conversationId != null ? conversationId.toString() : GLOBAL_KEY);
        if (!tryAcquire(limitKey, requestsPerMinute)) {
            log.warn("AI conversation rate limit exceeded: key={}, limit={}/min", limitKey, requestsPerMinute);
            throw AiErrorCode.AI_RATE_LIMIT_EXCEEDED.toServiceException();
        }
    }

    private void checkTenantLimit(ChatClientRequest request) {
        if (!perTenant) {
            return;
        }
        Object tenantId = request.context().get(CONTEXT_KEY_TENANT);
        if (tenantId == null) {
            return;
        }
        String limitKey = "tenant:" + tenantId;
        if (!tryAcquire(limitKey, tenantRequestsPerMinute)) {
            log.warn("AI tenant rate limit exceeded: tenantId={}, limit={}/min", tenantId, tenantRequestsPerMinute);
            throw AiErrorCode.AI_RATE_LIMIT_EXCEEDED.toServiceException();
        }
    }

    /**
     * 基于 Redis INCR + EXPIRE 的简单分钟级限流。
     * 第一次 INCR 后设置 60s TTL，窗口内累计直到 TTL 到期后自动重置。
     */
    private boolean tryAcquire(String limitKey, int limit) {
        String redisKey = KEY_PREFIX + limitKey;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            redisTemplate.expire(redisKey, 60, TimeUnit.SECONDS);
        }
        return count <= limit;
    }
}
