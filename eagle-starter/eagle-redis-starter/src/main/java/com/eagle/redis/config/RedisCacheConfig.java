package com.eagle.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.time.Duration;

/**
 * Redis 缓存配置。
 *
 * <p>统一配置序列化方式为 JSON，支持对象类型信息保留。
 *
 * @author 孙士雄
 */
@AutoConfiguration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisCacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

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
     * 配置 RedisTemplate，使用 JSON 序列化。
     *
     * @param connectionFactory RedisConnectionFactory
     * @return RedisTemplate
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        RedisSerializer<Object> jsonSerializer = jsonSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 CacheManager，默认 TTL 30 分钟。
     *
     * @param connectionFactory RedisConnectionFactory
     * @return CacheManager
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> jsonSerializer = jsonSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }
}
