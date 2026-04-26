package com.eagle.resource.server.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;

/**
 * 缓存配置。
 *
 * <p>通过 {@code spring.cache.type} 切换实现：
 * <ul>
 *   <li>{@code caffeine}（默认）：由 Spring Boot 自动配置，通过 {@code spring.cache.caffeine.spec} 调整</li>
 *   <li>{@code redis}：注册本 Bean 覆盖默认序列化器，TTL 等参数通过 {@code spring.cache.redis.*} 配置</li>
 * </ul>
 *
 * @author 孙士雄
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 构建 JSON 序列化器，Value 中写入 {@code @class} 字段，
     * 反序列化时可还原为原始类型而非 {@code LinkedHashMap}。
     */
    private static RedisSerializer<Object> jsonSerializer() {
        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY)
                .findAndAddModules()
                .build();

        return new RedisSerializer<>() {
            @Override
            public byte @Nullable [] serialize(@Nullable Object value) throws SerializationException {
                if (value == null) {
                    return null;
                }
                try {
                    return mapper.writeValueAsBytes(value);
                } catch (JsonProcessingException e) {
                    throw new SerializationException("Redis JSON serialize failed", e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                try {
                    return mapper.readValue(bytes, Object.class);
                } catch (IOException e) {
                    throw new SerializationException("Redis JSON deserialize failed", e);
                }
            }
        };
    }

    /**
     * 自定义 Redis 缓存默认配置，仅替换序列化器为 JSON，
     * TTL / Key 前缀 / null 值缓存等均由 {@code spring.cache.redis.*} 属性驱动。
     * 会检测此 Bean 并将其作为 {@code RedisCacheManager} 的默认配置。
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        CacheProperties.Redis redis = cacheProperties.getRedis();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()));

        if (redis.getTimeToLive() != null) {
            config = config.entryTtl(redis.getTimeToLive());
        }
        if (redis.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redis.getKeyPrefix());
        }
        if (!redis.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redis.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }
}
