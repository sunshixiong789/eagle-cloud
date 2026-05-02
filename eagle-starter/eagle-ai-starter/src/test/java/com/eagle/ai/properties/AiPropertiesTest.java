package com.eagle.ai.properties;

import org.junit.jupiter.api.DisplayName;
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

        // Chat defaults
        AiProperties.Chat chat = props.getChat();
        assertEquals(10, chat.getMemoryWindowSize());
        assertNull(chat.getSystemPrompt());

        // Memory defaults
        AiProperties.Memory memory = props.getMemory();
        assertEquals("eagle:ai:chat:memory", memory.getKeyPrefix());
        assertEquals(Duration.ofDays(7), memory.getTtl());

        // RateLimit defaults
        AiProperties.RateLimit rateLimit = props.getRateLimit();
        assertFalse(rateLimit.isEnabled());
        assertEquals(60, rateLimit.getRequestsPerMinute());
        assertTrue(rateLimit.isPerConversation());

        // Metrics defaults
        AiProperties.Metrics metrics = props.getMetrics();
        assertTrue(metrics.isEnabled());
        assertEquals("eagle.ai", metrics.getPrefix());
    }
}
