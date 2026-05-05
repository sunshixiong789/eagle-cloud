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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    @DisplayName("should have correct name and order")
    void shouldHaveCorrectNameAndOrder() {
        assertEquals("AiRateLimitAdvisor", advisor.getName());
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 100, advisor.getOrder());
    }

    @Test
    @DisplayName("after should return response unchanged")
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
        @DisplayName("should pass when count is within limit")
        void shouldPassWhenWithinLimit() {
            when(valueOps.increment(anyString())).thenReturn(3L);

            assertDoesNotThrow(() -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("should set TTL on first increment")
        void shouldSetTtlOnFirstIncrement() {
            when(valueOps.increment(anyString())).thenReturn(1L);

            advisor.before(request, chain);

            verify(redisTemplate).expire(anyString(), eq(60L), any());
        }

        @Test
        @DisplayName("should not set TTL on subsequent increments")
        void shouldNotSetTtlOnSubsequentIncrements() {
            when(valueOps.increment(anyString())).thenReturn(2L);

            advisor.before(request, chain);

            verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("should throw when count exceeds limit")
        void shouldThrowWhenExceedsLimit() {
            when(valueOps.increment(anyString())).thenReturn(6L);

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("should use global key when no conversationId in context")
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
        @DisplayName("should check tenant limit separately")
        void shouldCheckTenantLimit() {
            when(valueOps.increment(anyString())).thenReturn(1L);

            advisor.before(request, chain);

            // Both conv and tenant keys should be incremented
            verify(valueOps, times(2)).increment(anyString());
        }

        @Test
        @DisplayName("should throw when tenant limit exceeded")
        void shouldThrowWhenTenantLimitExceeded() {
            when(valueOps.increment(contains("conv:"))).thenReturn(1L);
            when(valueOps.increment(contains("tenant:"))).thenReturn(11L);

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("should skip tenant check when tenantId not in context")
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
        @DisplayName("should skip rate check when perConversation=false and perTenant=false")
        void shouldSkipWhenBothDisabled() {
            advisor.before(request, chain);

            verify(valueOps, never()).increment(anyString());
        }
    }
}
