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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("应HaveCorrect名称并排序")
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
        @DisplayName("Below预算时应通过")
        void shouldPassWhenBelowBudget() {
            when(valueOps.get(anyString())).thenReturn("500");

            assertDoesNotThrow(() -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("key不Exists时应通过")
        void shouldPassWhenKeyNotExists() {
            when(valueOps.get(anyString())).thenReturn(null);

            assertDoesNotThrow(() -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("UsageEquals预算时应抛出")
        void shouldThrowWhenUsageEqualsBudget() {
            when(valueOps.get(anyString())).thenReturn("1000");

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("UsageExceeds预算时应抛出")
        void shouldThrowWhenUsageExceedsBudget() {
            when(valueOps.get(anyString())).thenReturn("1500");

            assertThrows(RuntimeException.class, () -> advisor.before(request, chain));
        }

        @Test
        @DisplayName("应Use租户ID作为标识")
        void shouldUseTenantIdAsIdentifier() {
            when(valueOps.get(anyString())).thenReturn("0");

            advisor.before(request, chain);

            verify(valueOps).get(contains("tenant:t-001"));
        }

        @Test
        @DisplayName("无租户时应Use会话ID")
        void shouldUseConversationIdWhenNoTenant() {
            when(request.context()).thenReturn(Map.of(ChatMemory.CONVERSATION_ID, "conv-1"));
            when(valueOps.get(anyString())).thenReturn("0");

            advisor.before(request, chain);

            verify(valueOps).get(contains("conv:conv-1"));
        }

        @Test
        @DisplayName("空时应Use默认标识")
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
        @DisplayName("应递增计数器")
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
        @DisplayName("首次写入时应设置TTL")
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
        @DisplayName("SubsequentWrites时不应设置TTL")
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
        @DisplayName("零Tokens时应跳过")
        void shouldSkipWhenZeroTokens() {
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getTotalTokens()).thenReturn(0);
            ChatClientResponse response = buildResponse(Map.of("tenantId", "t-001"));

            advisor.after(response, chain);

            verify(valueOps, never()).increment(anyString(), anyLong());
        }

        @Test
        @DisplayName("无聊天响应时应跳过")
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
