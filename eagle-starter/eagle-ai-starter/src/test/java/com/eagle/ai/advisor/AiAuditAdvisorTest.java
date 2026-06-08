package com.eagle.ai.advisor;

import com.eagle.ai.event.AiCallAuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAuditAdvisor")
class AiAuditAdvisorTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
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

    private AiAuditAdvisor advisor;

    @BeforeEach
    void setUp() {
        advisor = new AiAuditAdvisor(eventPublisher);
    }

    @Test
    @DisplayName("应HaveCorrect名称并排序")
    void shouldHaveCorrectNameAndOrder() {
        assertEquals("AiAuditAdvisor", advisor.getName());
        assertEquals(Ordered.LOWEST_PRECEDENCE - 200, advisor.getOrder());
    }

    @Test
    @DisplayName("前应返回请求保持不变")
    void beforeShouldReturnRequestUnchanged() {
        assertSame(request, advisor.before(request, chain));
    }

    @Nested
    @DisplayName("after")
    class After {

        @Test
        @DisplayName("使用令牌数据时应发布审计事件")
        void shouldPublishAuditEventWithTokenData() {
            when(chatResponse.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
            when(metadata.getModel()).thenReturn("gpt-4o");
            when(usage.getPromptTokens()).thenReturn(100);
            when(usage.getCompletionTokens()).thenReturn(50);
            when(usage.getTotalTokens()).thenReturn(150);

            ChatClientResponse response = buildResponse(
                    Map.of(ChatMemory.CONVERSATION_ID, "conv-1", "tenantId", "t-001"));

            advisor.before(request, chain);
            advisor.after(response, chain);

            ArgumentCaptor<AiCallAuditEvent> captor = ArgumentCaptor.forClass(AiCallAuditEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AiCallAuditEvent event = captor.getValue();
            assertEquals("conv-1", event.conversationId());
            assertEquals("t-001", event.tenantId());
            assertEquals("gpt-4o", event.model());
            assertEquals(100, event.inputTokens());
            assertEquals(50, event.outputTokens());
            assertEquals(150, event.totalTokens());
            assertTrue(event.success());
            assertTrue(event.latencyMs() >= 0);
        }

        @Test
        @DisplayName("无Metadata时应发布事件使用未知Model")
        void shouldPublishEventWithUnknownModelWhenNoMetadata() {
            ChatClientResponse response = buildResponse(Map.of());

            advisor.before(request, chain);
            advisor.after(response, chain);

            ArgumentCaptor<AiCallAuditEvent> captor = ArgumentCaptor.forClass(AiCallAuditEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            AiCallAuditEvent event = captor.getValue();
            assertEquals("unknown", event.model());
            assertEquals(0, event.inputTokens());
            assertEquals(0, event.outputTokens());
            assertTrue(event.success());
        }

        @Test
        @DisplayName("应Calculate非负数Latency")
        void shouldCalculateNonNegativeLatency() {
            ChatClientResponse response = buildResponse(Map.of());

            advisor.before(request, chain);
            advisor.after(response, chain);

            ArgumentCaptor<AiCallAuditEvent> captor = ArgumentCaptor.forClass(AiCallAuditEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            assertTrue(captor.getValue().latencyMs() >= 0);
        }

        @Test
        @DisplayName("无前时应Report负数Latency")
        void shouldReportNegativeLatencyWhenNoBefore() {
            ChatClientResponse response = buildResponse(Map.of());

            // Call after() without before()
            advisor.after(response, chain);

            ArgumentCaptor<AiCallAuditEvent> captor = ArgumentCaptor.forClass(AiCallAuditEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            assertEquals(-1L, captor.getValue().latencyMs());
        }

        @Test
        @DisplayName("使用null聊天响应时应发布事件Even")
        void shouldPublishEventEvenWithNullChatResponse() {
            ChatClientResponse response = ChatClientResponse.builder().build();

            advisor.before(request, chain);
            advisor.after(response, chain);

            verify(eventPublisher).publishEvent(any(AiCallAuditEvent.class));
        }

        @Test
        @DisplayName("应返回响应保持不变")
        void shouldReturnResponseUnchanged() {
            ChatClientResponse response = buildResponse(Map.of());
            advisor.before(request, chain);

            assertSame(response, advisor.after(response, chain));
        }

        private ChatClientResponse buildResponse(Map<String, Object> context) {
            return ChatClientResponse.builder()
                    .chatResponse(chatResponse)
                    .context(context)
                    .build();
        }
    }
}
