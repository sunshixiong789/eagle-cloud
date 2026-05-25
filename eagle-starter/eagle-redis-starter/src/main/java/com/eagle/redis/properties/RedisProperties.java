package com.eagle.redis.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 缓存配置属性。
 *
 * <p>示例配置（application.yml）：
 * <pre>
 * eagle:
 *   redis:
 *     default-ttl: 30m
 *     cache-null-values: true       # 缓存 null 防缓存穿透
 *     key-prefix: "eagle:"          # 多服务共享 Redis 时防 key 冲突
 *     transaction-aware: true       # 写操作与事务同步，提交后才生效
 *     cache-ttls:                   # 各缓存区域独立 TTL
 *       USER_CACHE: 10m
 *       PERMISSION_CACHE: 60m
 * </pre>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.redis")
public class RedisProperties {

    /**
     * 全局默认缓存 TTL，各缓存区域未单独配置时使用。
     */
    private Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * 是否允许缓存 null 值。
     * 开启后可防止缓存穿透（查询不存在的数据时，null 结果也被缓存）。
     */
    private boolean cacheNullValues = true;

    /**
     * 缓存 key 前缀。
     * 多个服务共用同一 Redis 实例时，设置不同前缀防止 key 冲突。
     * 如 "eagle:system:"，最终 key 格式为 "{prefix}{cacheName}::{key}"。
     */
    private String keyPrefix = "";

    /**
     * 是否开启事务感知模式。
     * 开启后，{@code @CachePut}/{@code @CacheEvict} 等写操作会等事务提交后才真正执行，
     * 防止事务回滚后缓存与数据库不一致。
     */
    private boolean transactionAware = true;

    /**
     * 各缓存区域的独立 TTL 配置，key 为 cacheName（如 "USER_CACHE"）。
     * 未配置的缓存使用 {@link #defaultTtl}。
     */
    private Map<String, Duration> cacheTtls = new LinkedHashMap<>();
}
