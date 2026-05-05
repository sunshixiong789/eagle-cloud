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
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBudgetAdvisor")
class TokenBudgetAdvisorTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AdvisorChain chain;
    @Mock
    private ChatClientRequest request;
    @Mock
    private ChatResponse chatResponse;
    @Mock
    private ChatResponseMetadata metadata;
    @Mock
    private Usage usage;

    private TokenBudgetAdvisor advisor;
    private AiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.getBudget().setDefaultMonthlyTokens(1000L);
        advisor = new TokenBudgetAdvisor(redisTemplate, properties);
    }

    @Test
    @DisplayName("should have correct name and order")
    void shouldHaveCorrectNameAndOrder() {
        assertEquals("TokenBudgetAdvisor", advisor.getName());
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 200, advisor.getOrder());
    }

    @Nested
    @DisplayName("before")
    class Before {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(request.context()).thenReturn(Map.of("tenantId", "t-001"));
        }

        @Test
        @DisplayName("should pass when current usage is below budget")
        void shouldPassWhenBelowBudget() {
            when(valueOps.get(anyString())).thenReturn("500");

            assertDoesNotThrow(() -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("should pass when Redis key does not exist yet")
        void shouldPassWhenKeyNotExists() {
            when(valueOps.get(anyString())).thenReturn(null);

            assertDoesNotThrow(() -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("should throw when current usage equals budget")
        void shouldThrowWhenUsageEqualsBudget() {
            when(valueOps.get(anyString())).thenReturn("1000");

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("should throw when current usage exceeds budget")
        void shouldThrowWhenUsageExceedsBudget() {
            when(valueOps.get(anyString())).thenReturn("1500");

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("should use tenantId as identifier when present")
        void shouldUseTenantIdAsIdentifier() {
            when(valueOps.get(anyString())).thenReturn("0");

            advisor.before(request, chain);

            verify(valueOps).get(contains("tenant:t-001"));
        }

        @Test
        @DisplayName("should use conversationId when no tenantId in context")
        void shouldUseConversationIdWhenNoTenant() {
            when(request.context()).thenReturn(Map.of(ChatMemory.CONVERSATION_ID, "conv-1"));
            when(valueOps.get(anyString())).thenReturn("0");

            advisor.before(request, chain);

            verify(valueOps).get(contains("conv:conv-1"));
        }

        @Test
        @DisplayName("should use default identifier when context is empty")
        void shouldUseDefaultIdentifierWhenEmpty() {
            when(request.context()).thenReturn(Map.of());
            when(valueOps.get(anyString())).thenReturn("0");

            advisor.before(request, chain);

            verify(valueOps).get(contains("default"));
        }
    }

    @Nested
    @DisplayName("after")
    class After {

        @Test
        @DisplayName("should increment Redis counter with total tokens")
        void shouldIncrementCounter() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getTotalTokens()).thenReturn(150);
            ChatClientResponse response = buildResponse(Map.of("tenantId", "t-001"));
            when(valueOps.increment(anyString(), eq(150L))).thenReturn(150L);

            advisor.after(response, chain);

            verify(valueOps).increment(contains("tenant:t-001"), eq(150L));
        }

        @Test
        @DisplayName("should set 40-day TTL on first write")
        void shouldSetTtlOnFirstWrite() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getTotalTokens()).thenReturn(100);
            ChatClientResponse response = buildResponse(Map.of("tenantId", "t-001"));
            when(valueOps.increment(anyString(), anyLong())).thenReturn(100L);

            advisor.after(response, chain);

            verify(redisTemplate).expire(anyString(), eq(40L), eq(TimeUnit.DAYS));
        }

        @Test
        @DisplayName("should not set TTL on subsequent writes")
        void shouldNotSetTtlOnSubsequentWrites() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getTotalTokens()).thenReturn(100);
            ChatClientResponse response = buildResponse(Map.of("tenantId", "t-001"));
            when(valueOps.increment(anyString(), anyLong())).thenReturn(500L);

            advisor.after(response, chain);

            verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("should skip increment when totalTokens is zero")
        void shouldSkipWhenZeroTokens() {
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getTotalTokens()).thenReturn(0);
            ChatClientResponse response = buildResponse(Map.of("tenantId", "t-001"));

            advisor.after(response, chain);

            verify(valueOps, never()).increment(anyString(), anyLong());
        }

        @Test
        @DisplayName("should skip increment when chatResponse is null")
        void shouldSkipWhenNoChatResponse() {
            ChatClientResponse response = ChatClientResponse.builder().build();

            advisor.after(response, chain);

            verify(valueOps, never()).increment(anyString(), anyLong());
        }

        private ChatClientResponse buildResponse(Map<String, Object> context) {
            return ChatClientResponse.builder()
                    .chatResponse(chatResponse)
                    .context(context)
                    .build();
        }
    }
}
