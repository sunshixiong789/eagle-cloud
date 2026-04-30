package com.eagle.redis.config;

import com.eagle.common.lock.DistributedLock;
import com.eagle.redis.lock.RedisDistributedLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisOperations;

/**
 * Redis 自动配置类。
 *
 * <p>RedissonClient 由 redisson-spring-boot-starter 自动提供，
 * 本配置类仅作为 eagle-redis-starter 的自动配置入口，
 * 实际 RedisTemplate 和 CacheManager 配置见 {@link RedisCacheConfig}。
 *
 * <p>同时按 {@code eagle.lock.type=redis}（默认）注册 {@link RedisDistributedLock}
 * 作为 {@link DistributedLock} 默认实现。
 *
 * @author 孙士雄
 */
@AutoConfiguration(after = RedisCacheConfig.class)
@ConditionalOnClass(RedisOperations.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(DistributedLock.class)
    @ConditionalOnProperty(name = "eagle.lock.type", havingValue = "redis", matchIfMissing = true)
    public DistributedLock redisDistributedLock(RedissonClient redissonClient) {
        return new RedisDistributedLock(redissonClient);
    }
}
