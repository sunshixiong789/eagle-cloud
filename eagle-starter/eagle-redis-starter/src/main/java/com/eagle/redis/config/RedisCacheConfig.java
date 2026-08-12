package com.eagle.redis.config;

import com.eagle.redis.properties.RedisProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
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

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Redis 缓存自动配置。
 *
 * <p>统一配置 JSON 序列化，key 前缀、TTL、null 值缓存等均通过
 * {@link RedisProperties}（{@code eagle.redis.*}）外部化配置，无需修改代码。
 *
 * @author eagle
 */
@AutoConfiguration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
@EnableConfigurationProperties(RedisProperties.class)
public class RedisCacheConfig {

    /**
     * 把 final 实现的根级集合替换成 Jackson 可识别的可变实现。
     *
     * <p>仅当根值是 final {@link Collection} / {@link Map} 时替换：
     * {@code List.of() / Map.of() / Set.of() / Stream.toList() /
     * Collections.empty*() / Collections.singleton*()} 等返回的都是 final 类，
     * 无 public 构造器，Jackson 既写不出可还原的类型包装、也无法直接构造。
     * 非 final 实现（{@code ArrayList / HashMap / TreeMap / LinkedHashSet} 等）原样放行，
     * 由 {@code DefaultTyping.NON_FINAL} 正常处理。
     *
     * <p>嵌套集合作为 POJO 字段时由 Jackson 声明类型静态推断，无需深拷贝。
     */
    private static Object normalizeRoot(Object value) {
        if (!Modifier.isFinal(value.getClass().getModifiers())) {
            return value;
        }
        if (value instanceof Map<?, ?> m) {
            return new LinkedHashMap<>(m);
        }
        if (value instanceof Set<?> s) {
            return new LinkedHashSet<>(s);
        }
        if (value instanceof Collection<?> c) {
            return new ArrayList<>(c);
        }
        return value;
    }

    /**
     * 老数据兼容：识别 fix 前写入 Redis 的裸空 {@code []} / {@code {}}。
     */
    private static @Nullable Object tryFallback(byte[] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8).trim();
        if ("[]".equals(json)) {
            return new ArrayList<>();
        }
        if ("{}".equals(json)) {
            return new LinkedHashMap<>();
        }
        return null;
    }

    /**
     * 专用于 Redis 序列化的 ObjectMapper。
     *
     * <p>开启 {@code DefaultTyping.NON_FINAL_AND_RECORDS}，在 JSON 中写入 {@code @class} 字段，
     * 反序列化时可还原为原始类型而非 {@code LinkedHashMap}。相比 {@code NON_FINAL}，
     * 该模式也覆盖作为缓存根值的 record DTO；record 是 final 类，否则第一次回源正常、
     * 第二次从 Redis 按 {@code Object} 反序列化时会因缺少类型信息失败。
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
                        DefaultTyping.NON_FINAL_AND_RECORDS,
                        JsonTypeInfo.As.PROPERTY)
                .findAndAddModules()
                .build();
    }

    /**
     * Redis JSON 序列化器（共享，避免重复创建 ObjectMapper）。
     *
     * <p>写入前对根值做一次浅 normalize：把 {@code java.util.ImmutableCollections}
     * 系列（{@code List.of() / Map.of() / Set.of() / Stream.toList()} 的返回值）
     * 替换成 {@link ArrayList} / {@link LinkedHashMap} / {@link LinkedHashSet}。
     * 原因：{@code DefaultTyping.NON_FINAL} 不给 final 类写类型包装，
     * 直接写出来的 {@code []} 在以 {@code Object} 反序列化时会撞
     * {@code AsArrayTypeDeserializer} 期望的 {@code [type_id, value]} 格式而报错。
     * POJO 字段里的不可变集合不受影响，Jackson 用声明类型静态推断。
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
                    return redisObjectMapper.writeValueAsBytes(normalizeRoot(value));
                } catch (JacksonException e) {
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
                } catch (JacksonException e) {
                    // 兼容 fix 前老数据：被 NON_FINAL 跳过类型包装的裸空集合
                    Object fallback = tryFallback(bytes);
                    if (fallback != null) {
                        return fallback;
                    }
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
