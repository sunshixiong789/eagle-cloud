package com.eagle.redis.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisOperations;

/**
 * Redis 自动配置类。
 *
 * <p>RedissonClient 由 redisson-spring-boot-starter 自动提供，
 * 本配置类仅作为 eagle-redis-starter 的自动配置入口，
 * 实际 RedisTemplate 和 CacheManager 配置见 {@link RedisCacheConfig}。
 *
 * @author 孙士雄
 */
@AutoConfiguration(after = RedisCacheConfig.class)
@ConditionalOnClass(RedisOperations.class)
public class RedisAutoConfiguration {
}
