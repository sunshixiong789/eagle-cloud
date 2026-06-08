package com.eagle.ai.advisor;

import com.eagle.ai.properties.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiRateLimitAdvisor")
class AiRateLimitAdvisorTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AdvisorChain chain;
    @Mock
    private ChatClientRequest request;

    private AiRateLimitAdvisor advisor;
    private AiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setRequestsPerMinute(5);
        advisor = new AiRateLimitAdvisor(redisTemplate, properties);
    }

    @Test
    @DisplayName("应HaveCorrect名称并排序")
    void shouldHaveCorrectNameAndOrder() {
        assertEquals("AiRateLimitAdvisor", advisor.getName());
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 100, advisor.getOrder());
    }

    @Test
    @DisplayName("后应返回响应保持不变")
    void afterShouldReturnResponseUnchanged() {
        ChatClientResponse response = ChatClientResponse.builder().build();
        assertSame(response, advisor.after(response, chain));
    }

    @Nested
    @DisplayName("conversation rate limit")
    class ConversationRateLimit {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(request.context()).thenReturn(Map.of(ChatMemory.CONVERSATION_ID, "conv-1"));
        }

        @Test
        @DisplayName("内限制时应通过")
        void shouldPassWhenWithinLimit() {
            when(valueOps.increment(anyString())).thenReturn(3L);

            assertDoesNotThrow(() -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("首次递增时应设置TTL")
        void shouldSetTtlOnFirstIncrement() {
            when(valueOps.increment(anyString())).thenReturn(1L);

            advisor.before(request, chain);

            verify(redisTemplate).expire(anyString(), eq(60L), any());
        }

        @Test
        @DisplayName("SubsequentIncrements时不应设置TTL")
        void shouldNotSetTtlOnSubsequentIncrements() {
            when(valueOps.increment(anyString())).thenReturn(2L);

            advisor.before(request, chain);

            verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("Exceeds限制时应抛出")
        void shouldThrowWhenExceedsLimit() {
            when(valueOps.increment(anyString())).thenReturn(6L);

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("无会话ID时应UseGlobalkey")
        void shouldUseGlobalKeyWhenNoConversationId() {
            when(request.context()).thenReturn(Map.of());
            when(valueOps.increment(anyString())).thenReturn(1L);

            advisor.before(request, chain);

            verify(valueOps).increment(contains("conv:global"));
        }
    }

    @Nested
    @DisplayName("tenant rate limit")
    class TenantRateLimit {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            properties.getRateLimit().setPerTenant(true);
            properties.getRateLimit().setTenantRequestsPerMinute(10);
            // Recreate advisor after mutating properties — constructor copies config values
            advisor = new AiRateLimitAdvisor(redisTemplate, properties);
            when(request.context()).thenReturn(
                    Map.of(ChatMemory.CONVERSATION_ID, "conv-1", "tenantId", "t-001"));
        }

        @Test
        @DisplayName("应Check租户限制")
        void shouldCheckTenantLimit() {
            when(valueOps.increment(anyString())).thenReturn(1L);

            advisor.before(request, chain);

            // Both conv and tenant keys should be incremented
            verify(valueOps, times(2)).increment(anyString());
        }

        @Test
        @DisplayName("租户限制Exceeded时应抛出")
        void shouldThrowWhenTenantLimitExceeded() {
            when(valueOps.increment(contains("conv:"))).thenReturn(1L);
            when(valueOps.increment(contains("tenant:"))).thenReturn(11L);

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("无租户ID时应跳过租户Check")
        void shouldSkipTenantCheckWhenNoTenantId() {
            when(request.context()).thenReturn(Map.of(ChatMemory.CONVERSATION_ID, "conv-1"));
            when(valueOps.increment(anyString())).thenReturn(1L);

            advisor.before(request, chain);

            // Only conv key should be incremented
            verify(valueOps, times(1)).increment(anyString());
        }
    }

    @Nested
    @DisplayName("per-conversation disabled")
    class PerConversationDisabled {

        @BeforeEach
        void setUp() {
            properties.getRateLimit().setPerConversation(false);
            // Recreate advisor after mutating properties — constructor copies config values
            advisor = new AiRateLimitAdvisor(redisTemplate, properties);
        }

        @Test
        @DisplayName("同时已禁用时应跳过")
        void shouldSkipWhenBothDisabled() {
            advisor.before(request, chain);

            verify(valueOps, never()).increment(anyString());
        }
    }
}
