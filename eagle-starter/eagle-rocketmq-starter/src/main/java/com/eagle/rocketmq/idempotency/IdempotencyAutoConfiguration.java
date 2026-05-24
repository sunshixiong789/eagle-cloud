package com.eagle.rocketmq.idempotency;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 消费侧幂等检查器自动装配。
 *
 * <p>装配条件:
 * <ul>
 *   <li>{@link StringRedisTemplate} 在 classpath 上(即消费方引入了 {@code eagle-redis-starter} 或
 *       {@code spring-boot-starter-data-redis})</li>
 *   <li>容器内尚无其他 {@link IdempotencyChecker} Bean(允许业务方自行覆盖)</li>
 * </ul>
 *
 * <p>无 Redis 依赖的项目可自行声明 DB 唯一约束方案的 {@link IdempotencyChecker} Bean。
 *
 * @author sunshixiong
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotencyChecker.class)
    public IdempotencyChecker redisIdempotencyChecker(StringRedisTemplate redisTemplate,
                                                      IdempotencyProperties properties) {
        return new RedisIdempotencyChecker(redisTemplate, properties);
    }
}
