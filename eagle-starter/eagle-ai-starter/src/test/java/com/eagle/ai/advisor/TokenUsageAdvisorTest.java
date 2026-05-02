package com.eagle.ai.advisor;

import com.eagle.ai.properties.AiProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenUsageAdvisor")
class TokenUsageAdvisorTest {

    private SimpleMeterRegistry meterRegistry;
    private TokenUsageAdvisor advisor;

    @Mock
    private AdvisedRequest request;
    @Mock
    private CallAroundAdvisorChain chain;
    @Mock
    private ChatResponse chatResponse;
    @Mock
    private ChatResponseMetadata metadata;
    @Mock
    private Usage usage;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        AiProperties props = new AiProperties();
        advisor = new TokenUsageAdvisor(meterRegistry, props);
    }

    @Test
    @DisplayName("should have correct name and order")
    void shouldHaveCorrectNameAndOrder() {
        assertEquals("TokenUsageAdvisor", advisor.getName());
        assertEquals(Ordered.LOWEST_PRECEDENCE - 100, advisor.getOrder());
    }

    @Nested
    @DisplayName("aroundCall")
    class AroundCall {

        @Test
        @DisplayName("should record token counters when usage data is available")
        void shouldRecordTokenCounters() {
            AdvisedResponse response = mock(AdvisedResponse.class);
            when(chain.nextAroundCall(request)).thenReturn(response);
            when(response.response()).thenReturn(chatResponse);
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getPromptTokens()).thenReturn(100L);
            when(usage.getGenerationTokens()).thenReturn(50L);
            when(usage.getTotalTokens()).thenReturn(150L);

            advisor.aroundCall(request, chain);

            assertEquals(100.0, meterRegistry.counter("eagle.ai.token.input", "type", "call").count());
            assertEquals(50.0, meterRegistry.counter("eagle.ai.token.output", "type", "call").count());
            assertEquals(150.0, meterRegistry.counter("eagle.ai.token.total", "type", "call").count());
        }

        @Test
        @DisplayName("should skip recording when chatResponse is null")
        void shouldSkipWhenChatResponseNull() {
            AdvisedResponse response = mock(AdvisedResponse.class);
            when(chain.nextAroundCall(request)).thenReturn(response);
            when(response.response()).thenReturn(null);

            advisor.aroundCall(request, chain);

            assertEquals(0.0, findCounter("eagle.ai.token.total", "call"));
        }

        @Test
        @DisplayName("should skip recording when totalTokens is zero")
        void shouldSkipWhenTotalTokensZero() {
            AdvisedResponse response = mock(AdvisedResponse.class);
            when(chain.nextAroundCall(request)).thenReturn(response);
            when(response.response()).thenReturn(chatResponse);
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getTotalTokens()).thenReturn(0L);

            advisor.aroundCall(request, chain);

            assertEquals(0.0, findCounter("eagle.ai.token.total", "call"));
        }
    }

    private double findCounter(String name, String callType) {
        Counter counter = meterRegistry.find(name).tag("type", callType).counter();
        return counter != null ? counter.count() : 0.0;
    }
}
