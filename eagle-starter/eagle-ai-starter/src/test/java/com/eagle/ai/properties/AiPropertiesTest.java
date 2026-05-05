package com.eagle.ai.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AiProperties")
class AiPropertiesTest {

    @Test
    @DisplayName("should have sensible defaults")
    void shouldHaveSensibleDefaults() {
        AiProperties props = new AiProperties();

        assertTrue(props.isEnabled());

        AiProperties.Chat chat = props.getChat();
        assertEquals(10, chat.getMemoryWindowSize());
        assertNull(chat.getSystemPrompt());
        assertNull(chat.getModel());
        assertNull(chat.getTemperature());
        assertNull(chat.getMaxTokens());
        assertEquals(Duration.ofSeconds(30), chat.getTimeout());

        AiProperties.Memory memory = props.getMemory();
        assertEquals("eagle:ai:chat:memory", memory.getKeyPrefix());
        assertEquals(Duration.ofDays(7), memory.getTtl());

        AiProperties.RateLimit rateLimit = props.getRateLimit();
        assertFalse(rateLimit.isEnabled());
        assertEquals(60, rateLimit.getRequestsPerMinute());
        assertTrue(rateLimit.isPerConversation());
        assertFalse(rateLimit.isPerTenant());
        assertEquals(300, rateLimit.getTenantRequestsPerMinute());

        AiProperties.Metrics metrics = props.getMetrics();
        assertTrue(metrics.isEnabled());
        assertEquals("eagle.ai", metrics.getPrefix());
        assertTrue(metrics.isIncludeModelTag());
        assertTrue(metrics.isIncludeTenantTag());
    }

    @Nested
    @DisplayName("Budget defaults")
    class BudgetDefaults {

        @Test
        @DisplayName("should default to disabled with 1M token limit")
        void shouldHaveSensibleDefaults() {
            AiProperties.Budget budget = new AiProperties().getBudget();

            assertFalse(budget.isEnabled());
            assertEquals(1_000_000L, budget.getDefaultMonthlyTokens());
            assertEquals("eagle:ai:budget", budget.getKeyPrefix());
        }
    }

    @Nested
    @DisplayName("Safety defaults")
    class SafetyDefaults {

        @Test
        @DisplayName("should default to disabled with empty pattern list")
        void shouldHaveSensibleDefaults() {
            AiProperties.Safety safety = new AiProperties().getSafety();

            assertFalse(safety.isEnabled());
            assertNotNull(safety.getBlockedPatterns());
            assertTrue(safety.getBlockedPatterns().isEmpty());
            assertFalse(safety.isCheckOutput());
        }
    }

    @Nested
    @DisplayName("Resilience defaults")
    class ResilienceDefaults {

        @Test
        @DisplayName("should default to enabled with eagle-ai-default instance name")
        void shouldHaveSensibleDefaults() {
            AiProperties.Resilience resilience = new AiProperties().getResilience();

            assertTrue(resilience.isEnabled());
            assertEquals("eagle-ai-default", resilience.getInstanceName());
            assertEquals(Duration.ofSeconds(10), resilience.getSlowCallDurationThreshold());
        }
    }

    @Nested
    @DisplayName("Embedding defaults")
    class EmbeddingDefaults {

        @Test
        @DisplayName("should default to enabled with topK=4 and threshold=0.7")
        void shouldHaveSensibleDefaults() {
            AiProperties.Embedding embedding = new AiProperties().getEmbedding();

            assertTrue(embedding.isEnabled());
            assertEquals(4, embedding.getDefaultTopK());
            assertEquals(0.7, embedding.getDefaultSimilarityThreshold(), 0.001);
        }
    }
}
