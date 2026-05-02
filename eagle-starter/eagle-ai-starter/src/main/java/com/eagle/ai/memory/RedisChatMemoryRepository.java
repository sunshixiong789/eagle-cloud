package com.eagle.ai.memory;

import com.eagle.ai.properties.AiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 {@link ChatMemoryRepository} 实现。
 *
 * <p>对话历史以 JSON 格式序列化后存储在 Redis String 中，key 格式：
 * {@code {keyPrefix}:{conversationId}}，支持 TTL 自动过期。
 *
 * <p>消息类型映射：
 * <ul>
 *   <li>{@code USER} → {@link UserMessage}</li>
 *   <li>{@code ASSISTANT} → {@link AssistantMessage}</li>
 *   <li>{@code SYSTEM} → {@link SystemMessage}</li>
 * </ul>
 *
 * <p>生产环境若需更换存储只需注册自定义 {@link ChatMemoryRepository} Bean 即可覆盖此实现。
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryRepository.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final long ttlSeconds;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     AiProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = properties.getMemory().getKeyPrefix();
        this.ttlSeconds = properties.getMemory().getTtl().toSeconds();
    }

    @Override
    public List<String> findConversationIds() {
        String pattern = keyPrefix + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        int prefixLen = keyPrefix.length() + 1;
        return keys.stream()
                .filter(k -> k.length() > prefixLen)
                .map(k -> k.substring(prefixLen))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = buildKey(conversationId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<MessageDto> dtos = objectMapper.readValue(json, new TypeReference<>() {});
            return dtos.stream()
                    .map(this::toMessage)
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize chat memory for conversationId={}, returning empty", conversationId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = buildKey(conversationId);
        List<MessageDto> dtos = messages.stream()
                .map(this::toDto)
                .toList();
        try {
            String json = objectMapper.writeValueAsString(dtos);
            redisTemplate.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat memory for conversationId={}", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(buildKey(conversationId));
    }

    // ==================== 内部工具 ====================

    private String buildKey(String conversationId) {
        return keyPrefix + ":" + conversationId;
    }

    private MessageDto toDto(Message message) {
        return new MessageDto(message.getMessageType().name(), message.getText());
    }

    private Message toMessage(MessageDto dto) {
        MessageType type;
        try {
            type = MessageType.valueOf(dto.type());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown message type '{}', treating as USER", dto.type());
            type = MessageType.USER;
        }
        return switch (type) {
            case ASSISTANT -> new AssistantMessage(dto.content());
            case SYSTEM -> new SystemMessage(dto.content());
            default -> new UserMessage(dto.content());
        };
    }

    /** 消息序列化 DTO（仅保留类型 + 文本，工具调用等复杂内容暂不支持）。 */
    record MessageDto(String type, String content) {}
}
