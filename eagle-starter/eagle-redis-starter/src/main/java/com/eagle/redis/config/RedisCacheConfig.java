package com.eagle.redis.config;

import com.eagle.redis.properties.RedisProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存自动配置。
 *
 * <p>统一配置 JSON 序列化，key 前缀、TTL、null 值缓存等均通过
 * {@link RedisProperties}（{@code eagle.redis.*}）外部化配置，无需修改代码。
 *
 * @author 孙士雄
 */
@AutoConfiguration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
@EnableConfigurationProperties(RedisProperties.class)
public class RedisCacheConfig {

    /**
     * 专用于 Redis 序列化的 ObjectMapper。
     *
     * <p>开启 {@code DefaultTyping.NON_FINAL}，在 JSON 中写入 {@code @class} 字段，
     * 反序列化时可还原为原始类型而非 {@code LinkedHashMap}。
     * 此 bean 与 Spring MVC 的全局 ObjectMapper 隔离，互不影响。
     */
    @Bean("redisObjectMapper")
    @ConditionalOnMissingBean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        return JsonMapper.builder()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType(Object.class)
                                .build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY)
                .findAndAddModules()
                .build();
    }

    /**
     * Redis JSON 序列化器（共享，避免重复创建 ObjectMapper）。
     */
    @Bean("redisJsonSerializer")
    @ConditionalOnMissingBean(name = "redisJsonSerializer")
    public RedisSerializer<Object> redisJsonSerializer(
            @org.springframework.beans.factory.annotation.Qualifier("redisObjectMapper")
            ObjectMapper redisObjectMapper) {
        return new RedisSerializer<>() {
            @Override
            public byte @Nullable [] serialize(@Nullable Object value) throws SerializationException {
                if (value == null) {
                    return null;
                }
                try {
                    return redisObjectMapper.writeValueAsBytes(value);
                } catch (JsonProcessingException e) {
                    throw new SerializationException("Redis JSON serialize failed", e);
                }
            }

            @Override
            public @Nullable Object deserialize(byte @Nullable [] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                try {
                    return redisObjectMapper.readValue(bytes, Object.class);
                } catch (IOException e) {
                    throw new SerializationException("Redis JSON deserialize failed", e);
                }
            }
        };
    }

    /**
     * 配置 RedisTemplate，key 用 String 序列化，value 用 JSON 序列化。
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            @org.springframework.beans.factory.annotation.Qualifier("redisJsonSerializer")
            RedisSerializer<Object> jsonSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 CacheManager。
     *
     * <ul>
     *   <li>TTL、null 值缓存、key 前缀通过 {@link RedisProperties} 配置</li>
     *   <li>支持通过 {@code eagle.redis.cache-ttls} 为不同缓存区域设置独立 TTL</li>
     *   <li>{@code transactionAware} 保证写操作在事务提交后才真正落入 Redis</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @org.springframework.beans.factory.annotation.Qualifier("redisJsonSerializer")
            RedisSerializer<Object> jsonSerializer,
            RedisProperties redisProperties) {

        RedisCacheConfiguration defaultConfig = buildCacheConfig(
                jsonSerializer, redisProperties, redisProperties.getDefaultTtl());

        // 各缓存区域独立 TTL
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        redisProperties.getCacheTtls().forEach((cacheName, ttl) ->
                cacheConfigs.put(cacheName, buildCacheConfig(jsonSerializer, redisProperties, ttl)));

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs);

        if (redisProperties.isTransactionAware()) {
            builder.transactionAware();
        }

        return builder.build();
    }

    /**
     * 构建单个缓存区域的配置。
     */
    private RedisCacheConfiguration buildCacheConfig(
            RedisSerializer<Object> jsonSerializer,
            RedisProperties properties,
            Duration ttl) {

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        if (!properties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }

        if (StringUtils.hasText(properties.getKeyPrefix())) {
            config = config.prefixCacheNameWith(properties.getKeyPrefix());
        }

        return config;
    }
}
