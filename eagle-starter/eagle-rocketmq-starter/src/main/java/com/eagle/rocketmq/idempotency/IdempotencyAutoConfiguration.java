package com.eagle.rocketmq.idempotency;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 消费侧幂等检查器自动装配。
 *
 * <p>装配条件(同时满足):
 * <ul>
 *   <li>{@link StringRedisTemplate} class 在 classpath 上(引入了 {@code eagle-redis-starter} 或
 *       {@code spring-boot-starter-data-redis})</li>
 *   <li>容器内实际**存在** {@link StringRedisTemplate} Bean(避免"引入依赖但未配 Redis"启动失败)</li>
 *   <li>容器内尚无其他 {@link IdempotencyChecker} Bean(允许业务方自行覆盖)</li>
 * </ul>
 *
 * <p>无 Redis 依赖的项目可自行声明 DB 唯一约束方案的 {@link IdempotencyChecker} Bean。
 *
 * <p><strong>使用警告</strong>:Redis SETNX 与下游 DB 事务跨事务域,
 * 在"业务事务回滚 → MQ 重投递"链路下会造成静默丢失(SETNX 占位先于 DB rollback,后续重投递被假成功跳过)。
 * 仅在"业务侧没有 DB 唯一约束兜底"且"消费幂等错过 1 次可接受"的场景使用。
 * 优先方案:聚合根关键字段加 unique 索引,捕获 {@code DataIntegrityViolationException} 视为重复。
 *
 * @author sunshixiong
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(IdempotencyChecker.class)
    public IdempotencyChecker redisIdempotencyChecker(StringRedisTemplate redisTemplate,
                                                      IdempotencyProperties properties) {
        return new RedisIdempotencyChecker(redisTemplate, properties);
    }
}
