package com.eagle.redis.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisOperations;

/**
 * Redis 自动配置类。
 *
 * @author 孙士雄
 */
@AutoConfiguration
@ConditionalOnClass(RedisOperations.class)
public class RedisAutoConfiguration {

    /**
     * 配置 RedissonClient（由 redisson-spring-boot-starter 自动提供，
     * 此处仅作显式声明以便其他 Bean 依赖）。
     *
     * @param redissonClient RedissonClient
     * @return RedissonClient
     */
    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(RedissonClient redissonClient) {
        return redissonClient;
    }
}
