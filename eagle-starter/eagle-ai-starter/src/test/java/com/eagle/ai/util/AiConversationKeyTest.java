package com.eagle.ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AiConversationKey")
class AiConversationKeyTest {

    @Nested
    @DisplayName("of(userId)")
    class OfUserId {

        @Test
        @DisplayName("should return userId when no tenantId")
        void shouldReturnUserId() {
            assertEquals("user123", AiConversationKey.of("user123"));
        }
    }

    @Nested
    @DisplayName("of(tenantId, userId)")
    class OfTenantIdAndUserId {

        @Test
        @DisplayName("should combine tenantId and userId with colon")
        void shouldCombineWithColon() {
            assertEquals("tenant1:user123", AiConversationKey.of("tenant1", "user123"));
        }

        @Test
        @DisplayName("should return userId only when tenantId is null")
        void shouldReturnUserIdWhenTenantIdNull() {
            assertEquals("user123", AiConversationKey.of(null, "user123"));
        }

        @Test
        @DisplayName("should return userId only when tenantId is blank")
        void shouldReturnUserIdWhenTenantIdBlank() {
            assertEquals("user123", AiConversationKey.of("  ", "user123"));
        }

        @Test
        @DisplayName("should return userId only when tenantId is empty")
        void shouldReturnUserIdWhenTenantIdEmpty() {
            assertEquals("user123", AiConversationKey.of("", "user123"));
        }
    }
}
