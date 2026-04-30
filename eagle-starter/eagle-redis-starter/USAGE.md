# eagle-redis-starter — Redis 多级缓存 + 分布式锁 + 限流 + 布隆过滤器

## 何时使用

- 跨服务共享缓存（推荐 Redisson + Caffeine 多级）
- 分布式锁（Redisson 实现 `DistributedLock`，自动注册为默认实现）
- 限流（令牌桶 / 滑动窗口）
- 布隆过滤器（缓存穿透防护）
- 延迟队列、Pub/Sub Topic、原子计数

## 何时不要使用

- 强一致性事务场景（用数据库 + 唯一约束）
- 单机本地缓存（用 Caffeine 直接，不必引入 Redis）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-redis-starter')
```

```yaml
spring.data.redis:
  host: ${REDIS_HOST:127.0.0.1}
  port: 6379
  password: ${REDIS_PASSWORD:}

eagle.redis:
  enabled: true
  cache-default-ttl: 30m         # @Cacheable 默认过期时间
  cache-null-value-ttl: 2m       # 空值缓存（防穿透）
```

## 核心 API

| 类 | 用途 |
|---|---|
| `RedisDistributedLock` | `DistributedLock` 的 Redisson 实现（自动作为默认 Bean）|
| `CacheProtectionUtil` | **缓存击穿防护**：`getWithLock(key, ttl, loader)` |
| `RedissonBloomFilterUtil` | **缓存穿透防护**：`add` / `contains` |
| `RedisRateLimiter` | 简易令牌桶限流（基于 Redis 脚本） |
| `RedissonRateLimiterUtil` | Redisson `RRateLimiter`（精确限流） |
| `RedissonAtomicUtil` | 原子计数器（incr / decr / cas） |
| `RedissonDelayedQueueUtil` | 延迟队列 |
| `RedissonTopicUtil` | Pub/Sub Topic |

Spring 缓存抽象正常使用：`@Cacheable` / `@CacheEvict` / `@Caching`。

## 最小示例

```java
@RequiredArgsConstructor
@Service
public class UserService {

    private final CacheProtectionUtil cacheProtection;
    private final DistributedLock lock;
    private final RedissonBloomFilterUtil bloomFilter;
    private final UserRepository repository;

    /** 缓存击穿防护：单飞回源 */
    public User findById(Long id) {
        return cacheProtection.getWithLock(
            "eagle:user:" + id,
            Duration.ofMinutes(30),
            () -> repository.findById(id).orElse(null)
        );
    }

    /** 缓存穿透防护：布隆过滤器 */
    public Optional<User> tryFind(Long id) {
        if (!bloomFilter.contains("user", id.toString())) {
            return Optional.empty();
        }
        return Optional.ofNullable(findById(id));
    }

    /** 分布式锁 */
    public void atomicUpdate(Long userId) {
        lock.executeWithLock("user:update:" + userId, Duration.ofSeconds(10), () -> {
            // 临界区
        });
    }

    /** Spring 缓存抽象 */
    @Cacheable(value = "USER_CACHE", key = "#id", unless = "#result == null")
    public User getCached(Long id) {
        return repository.findById(id).orElse(null);
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.redis.enabled` | boolean | `true` | 总开关 |
| `eagle.redis.cache-default-ttl` | Duration | `30m` | 默认缓存 TTL |
| `eagle.redis.cache-null-value-ttl` | Duration | `2m` | 空值缓存 TTL |
| `eagle.redis.cache-key-prefix` | String | `eagle:` | Key 前缀 |

Redis 连接走 Spring Boot 标准配置（`spring.data.redis.*`）。

## 常见错误

- ❌ Key 不带前缀 → ✅ 用 `eagle:{module}:{entity}:{id}` 命名
- ❌ TTL 设为 -1（永不过期）→ ✅ 显式声明 TTL（30m–12h）
- ❌ 缓存大对象（> 100KB）→ ✅ 拆字段缓存
- ❌ `@Cacheable` 缓存敏感字段 → ✅ DTO 排除或单独缓存
- ❌ 自己 `new RedissonClient` → ✅ 注入 starter 提供的 Bean
- ❌ 高 QPS 不防护击穿 → ✅ 用 `CacheProtectionUtil.getWithLock`

## 关联规则

- `.claude/rules/14-cache.md` — 缓存命名 / TTL / 击穿防护
- `.claude/rules/08-concurrency.md` — 缓存与事务（AFTER_COMMIT 失效）
