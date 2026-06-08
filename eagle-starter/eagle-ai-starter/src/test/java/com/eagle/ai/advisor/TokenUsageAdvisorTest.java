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
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenUsageAdvisor")
class TokenUsageAdvisorTest {

    private SimpleMeterRegistry meterRegistry;
    private TokenUsageAdvisor advisor;

    @Mock
    private AdvisorChain chain;
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
    @DisplayName("应HaveCorrect名称并排序")
    void shouldHaveCorrectNameAndOrder() {
        assertEquals("TokenUsageAdvisor", advisor.getName());
        assertEquals(Ordered.LOWEST_PRECEDENCE - 100, advisor.getOrder());
    }

    @Test
    @DisplayName("前应返回请求保持不变")
    void beforeShouldReturnRequestUnchanged() {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(mock(org.springframework.ai.chat.prompt.Prompt.class))
                .build();
        assertSame(request, advisor.before(request, chain));
    }

    private double findCounter(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? counter.count() : 0.0;
    }

    @Nested
    @DisplayName("after")
    class After {

        @Test
        @DisplayName("应Record令牌Counters")
        void shouldRecordTokenCounters() {
            ChatClientResponse response = ChatClientResponse.builder()
                    .chatResponse(chatResponse)
                    .build();
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getPromptTokens()).thenReturn(100);
            when(usage.getCompletionTokens()).thenReturn(50);
            when(usage.getTotalTokens()).thenReturn(150);

            advisor.after(response, chain);

            assertEquals(100.0, findCounter("eagle.ai.token.input"));
            assertEquals(50.0, findCounter("eagle.ai.token.output"));
            assertEquals(150.0, findCounter("eagle.ai.token.total"));
        }

        @Test
        @DisplayName("聊天响应null时应跳过")
        void shouldSkipWhenChatResponseNull() {
            ChatClientResponse response = ChatClientResponse.builder().build();

            advisor.after(response, chain);

            assertEquals(0.0, findCounter("eagle.ai.token.total"));
        }

        @Test
        @DisplayName("TotalTokens零时应跳过")
        void shouldSkipWhenTotalTokensZero() {
            ChatClientResponse response = ChatClientResponse.builder()
                    .chatResponse(chatResponse)
                    .build();
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(usage.getTotalTokens()).thenReturn(0);

            advisor.after(response, chain);

            assertEquals(0.0, findCounter("eagle.ai.token.total"));
        }
    }
}
