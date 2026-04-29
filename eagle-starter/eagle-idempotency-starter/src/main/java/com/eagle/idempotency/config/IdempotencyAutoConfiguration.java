package com.eagle.idempotency.config;

import com.eagle.idempotency.aspect.IdempotencyAspect;
import com.eagle.idempotency.controller.IdempotencyTokenController;
import com.eagle.idempotency.properties.IdempotencyProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 幂等性组件自动配置。
 *
 * <p>依赖 {@link RedissonClient} 在类路径存在时激活，可通过 {@code eagle.idempotency.enabled=false} 关闭。
 * 注册以下 Bean：
 * <ul>
 *   <li>{@link IdempotencyAspect} — AOP 切面，拦截 {@code @Idempotent} 方法</li>
 *   <li>{@link IdempotencyTokenController} — REST 接口，提供 Token 生成能力</li>
 * </ul>
 *
 * @author sunshixiong
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnProperty(name = "eagle.idempotency.enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyAutoConfiguration {

    /**
     * 注册幂等性 AOP 切面。
     *
     * @param redissonClient     Redisson 客户端
     * @param properties         幂等性配置属性
     * @param request            当前 HTTP 请求（用于 TOKEN / RESULT_CACHE 模式读取 Header）
     * @param objectMapper       Jackson ObjectMapper（用于 RESULT_CACHE 模式序列化/反序列化响应）
     * @param applicationContext Spring 容器（用于按名称查找 IdempotencyKeyExtractor Bean）
     * @return 幂等性切面 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyAspect idempotencyAspect(
            RedissonClient redissonClient,
            IdempotencyProperties properties,
            HttpServletRequest request,
            ObjectMapper objectMapper,
            ApplicationContext applicationContext) {
        return new IdempotencyAspect(redissonClient, properties, request, objectMapper, applicationContext);
    }

    /**
     * 注册幂等 Token 生成控制器。
     *
     * @param redissonClient Redisson 客户端
     * @param properties     幂等性配置属性
     * @return Token 控制器 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyTokenController idempotencyTokenController(
            RedissonClient redissonClient,
            IdempotencyProperties properties) {
        return new IdempotencyTokenController(redissonClient, properties);
    }
}
