package com.eagle.redis.config;

import com.eagle.common.lock.DistributedLock;
import com.eagle.redis.lock.RedisDistributedLock;
import com.eagle.redis.util.CacheProtectionUtil;
import com.eagle.redis.util.RedisRateLimiter;
import com.eagle.redis.util.RedissonAtomicUtil;
import com.eagle.redis.util.RedissonBloomFilterUtil;
import com.eagle.redis.util.RedissonDelayedQueueUtil;
import com.eagle.redis.util.RedissonRateLimiterUtil;
import com.eagle.redis.util.RedissonTopicUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

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
 * <p><b>条件说明：</b>util Bean 全部使用 {@code @ConditionalOnClass(RedissonClient.class)}
 * 而非 {@code @ConditionalOnBean(RedissonClient.class)}，原因是 Redisson 自身的
 * {@code RedissonAutoConfigurationV4} 与本类的加载顺序不确定，
 * {@code @ConditionalOnBean} 在条件评估阶段可能因 RedissonClient bean definition
 * 尚未声明而失败，导致 bean 被静默跳过。改用 {@code @ConditionalOnClass} 后，
 * 若运行时 RedissonClient 真不存在，Spring 装配 @Bean 入参时会报清晰的根因错误。
 *
 * @author eagle
 */
@AutoConfiguration(after = RedisCacheConfig.class)
@ConditionalOnClass(RedisOperations.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnMissingBean(DistributedLock.class)
    @ConditionalOnProperty(name = "eagle.lock.type", havingValue = "redis", matchIfMissing = true)
    public DistributedLock redisDistributedLock(RedissonClient redissonClient) {
        return new RedisDistributedLock(redissonClient);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnMissingBean
    public RedissonTopicUtil redissonTopicUtil(RedissonClient redissonClient) {
        return new RedissonTopicUtil(redissonClient);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnMissingBean
    public RedissonBloomFilterUtil redissonBloomFilterUtil(RedissonClient redissonClient) {
        return new RedissonBloomFilterUtil(redissonClient);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnMissingBean
    public RedissonAtomicUtil redissonAtomicUtil(RedissonClient redissonClient) {
        return new RedissonAtomicUtil(redissonClient);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnMissingBean
    public RedissonRateLimiterUtil redissonRateLimiterUtil(RedissonClient redissonClient) {
        return new RedissonRateLimiterUtil(redissonClient);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnMissingBean
    public RedissonDelayedQueueUtil redissonDelayedQueueUtil(RedissonClient redissonClient) {
        return new RedissonDelayedQueueUtil(redissonClient);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnMissingBean
    public CacheProtectionUtil cacheProtectionUtil(
            RedissonClient redissonClient,
            @Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate) {
        return new CacheProtectionUtil(redissonClient, redisTemplate);
    }

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public RedisRateLimiter redisRateLimiter(StringRedisTemplate stringRedisTemplate) {
        return new RedisRateLimiter(stringRedisTemplate);
    }
}
