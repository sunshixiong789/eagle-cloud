package com.eagle.ai.memory;

import com.eagle.ai.properties.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisChatMemoryRepository")
class RedisChatMemoryRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private RedisChatMemoryRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String KEY_PREFIX = "eagle:ai:chat:memory";

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        repository = new RedisChatMemoryRepository(redisTemplate, objectMapper, properties);
    }

    @Nested
    @DisplayName("findConversationIds")
    class FindConversationIds {

        @Test
        @DisplayName("should return conversationIds extracted from Redis keys via SCAN")
        void shouldReturnConversationIdsViaScan() {
            List<String> redisKeys = List.of(
                    KEY_PREFIX + ":conv-1",
                    KEY_PREFIX + ":conv-2",
                    KEY_PREFIX + ":conv-3"
            );
            @SuppressWarnings("unchecked")
            Cursor<String> cursor = mockCursor(redisKeys);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            List<String> result = repository.findConversationIds();

            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.contains("conv-1"));
            assertTrue(result.contains("conv-2"));
            assertTrue(result.contains("conv-3"));
        }

        @Test
        @DisplayName("should return empty list when no keys found")
        void shouldReturnEmptyWhenNoKeys() {
            @SuppressWarnings("unchecked")
            Cursor<String> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenReturn(false);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            List<String> result = repository.findConversationIds();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return partial results when scan throws exception")
        void shouldReturnPartialResultsOnException() {
            @SuppressWarnings("unchecked")
            Cursor<String> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenReturn(true);
            when(cursor.next())
                    .thenReturn(KEY_PREFIX + ":conv-1")
                    .thenThrow(new RuntimeException("Redis connection lost"));
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            // Should not throw, returns whatever was collected before the error
            assertDoesNotThrow(() -> repository.findConversationIds());
        }

        @Test
        @DisplayName("should use SCAN (not KEYS) to list conversations")
        void shouldUseScanNotKeys() {
            @SuppressWarnings("unchecked")
            Cursor<String> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenReturn(false);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            repository.findConversationIds();

            verify(redisTemplate).scan(any(ScanOptions.class));
            verify(redisTemplate, never()).keys(anyString());
        }
    }

    @Nested
    @DisplayName("findByConversationId")
    class FindByConversationId {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("should deserialize messages from Redis JSON")
        void shouldDeserializeMessages() throws Exception {
            String json = "[{\"type\":\"USER\",\"content\":\"Hello\"},{\"type\":\"ASSISTANT\",\"content\":\"Hi!\"}]";
            when(valueOps.get(KEY_PREFIX + ":conv-1")).thenReturn(json);

            List<Message> messages = repository.findByConversationId("conv-1");

            assertEquals(2, messages.size());
            assertInstanceOf(UserMessage.class, messages.get(0));
            assertInstanceOf(AssistantMessage.class, messages.get(1));
            assertEquals("Hello", messages.get(0).getText());
            assertEquals("Hi!", messages.get(1).getText());
        }

        @Test
        @DisplayName("should return empty list when key not found")
        void shouldReturnEmptyWhenKeyNotFound() {
            when(valueOps.get(anyString())).thenReturn(null);

            List<Message> result = repository.findByConversationId("unknown");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when JSON is invalid")
        void shouldReturnEmptyWhenJsonInvalid() {
            when(valueOps.get(anyString())).thenReturn("not-valid-json");

            List<Message> result = repository.findByConversationId("conv-1");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should map SYSTEM type to SystemMessage")
        void shouldMapSystemType() throws Exception {
            String json = "[{\"type\":\"SYSTEM\",\"content\":\"You are a helper.\"}]";
            when(valueOps.get(anyString())).thenReturn(json);

            List<Message> messages = repository.findByConversationId("conv-1");

            assertInstanceOf(SystemMessage.class, messages.get(0));
        }

        @Test
        @DisplayName("should treat unknown message type as USER")
        void shouldTreatUnknownTypeAsUser() throws Exception {
            String json = "[{\"type\":\"TOOL\",\"content\":\"tool result\"}]";
            when(valueOps.get(anyString())).thenReturn(json);

            List<Message> messages = repository.findByConversationId("conv-1");

            assertInstanceOf(UserMessage.class, messages.get(0));
        }
    }

    @Nested
    @DisplayName("saveAll")
    class SaveAll {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("should serialize messages and save with TTL")
        void shouldSaveWithTtl() {
            List<Message> messages = List.of(
                    new UserMessage("Hello"),
                    new AssistantMessage("Hi!")
            );

            repository.saveAll("conv-1", messages);

            verify(valueOps).set(
                    eq(KEY_PREFIX + ":conv-1"),
                    contains("USER"),
                    eq(7 * 24 * 3600L),
                    eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("should use configured TTL from properties")
        void shouldUseConfiguredTtl() {
            AiProperties properties = new AiProperties();
            properties.getMemory().setTtl(java.time.Duration.ofDays(14));
            repository = new RedisChatMemoryRepository(redisTemplate, objectMapper, properties);

            repository.saveAll("conv-1", List.of(new UserMessage("test")));

            verify(valueOps).set(anyString(), anyString(), eq(14 * 24 * 3600L), eq(TimeUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("deleteByConversationId")
    class DeleteByConversationId {

        @Test
        @DisplayName("should delete the conversation key")
        void shouldDeleteKey() {
            repository.deleteByConversationId("conv-1");

            verify(redisTemplate).delete(KEY_PREFIX + ":conv-1");
        }
    }

    @SuppressWarnings("unchecked")
    private Cursor<String> mockCursor(List<String> keys) {
        Cursor<String> cursor = mock(Cursor.class);
        Iterator<String> it = keys.iterator();
        when(cursor.hasNext()).thenAnswer(inv -> it.hasNext());
        when(cursor.next()).thenAnswer(inv -> it.next());
        return cursor;
    }
}
