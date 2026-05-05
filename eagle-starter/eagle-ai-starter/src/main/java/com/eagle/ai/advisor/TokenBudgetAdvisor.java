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
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Token 月度配额 Advisor（基于 Redis INCR + TTL）。
 *
 * <p>在每次 AI 调用前检查当前月度 Token 用量是否已超出配额，超额则拒绝请求（90003）。
 * 调用成功后，将本次 Token 消耗累加到 Redis 月度计数器。
 *
 * <p>配额标识符优先使用 context 中的 {@code tenantId}，其次 {@code conversationId}，
 * 均无则使用 {@code "default"}。
 *
 * <p>Redis key 格式：{@code {keyPrefix}:{yyyy-MM}:{identifier}}，TTL 40 天（覆盖整月 + 缓冲）。
 *
 * <p>注意：检查与计数非原子操作，存在极小的超额竞态（~1 个并发请求）。
 * 企业场景可接受此轻微超额，如需严格不超额请使用 Lua 脚本原子检查。
 *
 * <p>仅在 {@code eagle.ai.budget.enabled=true} 且 Redis 可用时注册此 Bean。
 */
public class TokenBudgetAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(TokenBudgetAdvisor.class);

    /** 在限流检查（HIGHEST_PRECEDENCE+100）之后运行，配额耗尽则早于实际调用拒绝。 */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 200;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    static final String CONTEXT_KEY_TENANT = "tenantId";

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final long defaultMonthlyTokens;

    public TokenBudgetAdvisor(StringRedisTemplate redisTemplate, AiProperties properties) {
        this.redisTemplate = redisTemplate;
        AiProperties.Budget cfg = properties.getBudget();
        this.keyPrefix = cfg.getKeyPrefix();
        this.defaultMonthlyTokens = cfg.getDefaultMonthlyTokens();
    }

    @Override
    public String getName() {
        return "TokenBudgetAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        String identifier = resolveIdentifier(request.context());
        String monthKey = buildMonthKey(identifier);

        String currentStr = redisTemplate.opsForValue().get(monthKey);
        long current = parseCount(currentStr);

        if (current >= defaultMonthlyTokens) {
            log.warn("AI token budget exceeded: identifier={}, used={}, limit={}", identifier, current, defaultMonthlyTokens);
            throw AiErrorCode.AI_TOKEN_BUDGET_EXCEEDED.toServiceException();
        }

        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        int tokens = extractTotalTokens(response);
        if (tokens <= 0) {
            return response;
        }

        String identifier = resolveIdentifier(response.context());
        String monthKey = buildMonthKey(identifier);

        Long newTotal = redisTemplate.opsForValue().increment(monthKey, tokens);
        if (newTotal != null && newTotal <= tokens) {
            // 首次写入，设置 40 天 TTL 确保覆盖整月
            redisTemplate.expire(monthKey, 40, TimeUnit.DAYS);
        }

        log.debug("AI token budget updated: identifier={}, added={}, total={}", identifier, tokens, newTotal);
        return response;
    }

    // ==================== 内部工具 ====================

    private String resolveIdentifier(java.util.Map<String, Object> context) {
        Object tenantId = context.get(CONTEXT_KEY_TENANT);
        if (tenantId != null && !tenantId.toString().isBlank()) {
            return "tenant:" + tenantId;
        }
        Object conversationId = context.get(ChatMemory.CONVERSATION_ID);
        if (conversationId != null && !conversationId.toString().isBlank()) {
            return "conv:" + conversationId;
        }
        return "default";
    }

    private String buildMonthKey(String identifier) {
        String yearMonth = LocalDate.now().format(MONTH_FORMATTER);
        return keyPrefix + ":" + yearMonth + ":" + identifier;
    }

    private long parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private int extractTotalTokens(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return 0;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null || usage.getTotalTokens() == null) {
            return 0;
        }
        return Math.max(0, usage.getTotalTokens());
    }
}
