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
        @DisplayName("应返回用户ID")
        void shouldReturnUserId() {
            assertEquals("user123", AiConversationKey.of("user123"));
        }
    }

    @Nested
    @DisplayName("of(tenantId, userId)")
    class OfTenantIdAndUserId {

        @Test
        @DisplayName("使用Colon时应Combine")
        void shouldCombineWithColon() {
            assertEquals("tenant1:user123", AiConversationKey.of("tenant1", "user123"));
        }

        @Test
        @DisplayName("租户IDnull时应返回用户ID")
        void shouldReturnUserIdWhenTenantIdNull() {
            assertEquals("user123", AiConversationKey.of(null, "user123"));
        }

        @Test
        @DisplayName("租户ID空白时应返回用户ID")
        void shouldReturnUserIdWhenTenantIdBlank() {
            assertEquals("user123", AiConversationKey.of("  ", "user123"));
        }

        @Test
        @DisplayName("租户ID空时应返回用户ID")
        void shouldReturnUserIdWhenTenantIdEmpty() {
            assertEquals("user123", AiConversationKey.of("", "user123"));
        }
    }
}
