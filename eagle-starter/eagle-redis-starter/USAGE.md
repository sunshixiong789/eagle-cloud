# eagle-redis-starter — Redis 缓存 + Redisson 分布式锁/限流/布隆/原子/延迟队列/Pub-Sub

## 何时使用

- Spring 缓存抽象（`@Cacheable`，多 cacheName 独立 TTL）
- 分布式锁（Redisson 实现 `DistributedLock`，自动注册）
- 缓存击穿/穿透/雪崩防护（`CacheProtectionUtil`）
- 限流（令牌桶 + 滑动窗口 Lua 实现，或 Redisson 原生 `RRateLimiter`）
- 布隆过滤器、原子计数（CAS 防超扣）、延迟队列、Pub/Sub Topic

## 何时不要使用

- 单机本地缓存（直接 Caffeine）
- 强一致性事务场景（DB + 唯一约束）
- 持久化消息队列（用 RocketMQ）

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
  default-ttl: 30m              # @Cacheable 默认 TTL
  cache-null-values: true       # 缓存 null 防穿透
  key-prefix: "eagle:"          # 多服务共享 Redis 时防 key 冲突
  transaction-aware: true       # 事务提交后再写缓存
  cache-ttls: # 各 cacheName 独立 TTL
    USER_CACHE: 10m
    PERMISSION_CACHE: 60m
```

## 核心 API

| 类                          | 主要方法                                                                                                                                 |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `CacheProtectionUtil`      | `getWithMutex(key, ttl, loader, type)` 防击穿；`jitter(base, ratio)` 防雪崩；`evict(key)`                                                    |
| `RedissonBloomFilterUtil`  | `init(name, expected, falseProb)` / `add` / `contains` / `addAll` / `count` / `delete`                                               |
| `RedisRateLimiter`（Lua）    | `tryAcquire(key, capacity, rate)` 令牌桶 / `tryAcquireWindow(key, max, window)` 滑动窗口                                                    |
| `RedissonRateLimiterUtil`  | `tryAcquire(key, rate, interval)` / 全参版含 `RateType.OVERALL/PER_CLIENT` 与 permits                                                     |
| `RedissonAtomicUtil`       | `initIfAbsent` / `increment` / `decrement` / `addAndGet` / **`decrementIfSufficient`（CAS 防超扣）** / `compareAndSet` / `get` / `delete` |
| `RedissonDelayedQueueUtil` | `offer(name, item, delay, unit)` / `take(name)` 阻塞 / `poll(name [, timeout, unit])` / `cancel` / `size`                              |
| `RedissonTopicUtil`        | `publish(topic, msg)` / `subscribe(topic, type, listenerKey, listener)` / `unsubscribe` / `countSubscribers`                         |
| `RedisDistributedLock`     | `DistributedLock` 默认实现（Bean 自动注册，业务注入 `DistributedLock` 即可）                                                                          |

Spring 缓存：`@Cacheable / @CacheEvict / @Caching` 直接用。

## 最小示例

```java

@Service
@RequiredArgsConstructor
public class UserService {

    private final CacheProtectionUtil cacheProtection;
    private final DistributedLock lock;
    private final RedissonAtomicUtil atomic;
    private final RedissonBloomFilterUtil bloom;
    private final UserRepository repository;

    /** 防击穿：单飞回源 + 空值占位防穿透 */
    public User findById(Long id) {
        return cacheProtection.getWithMutex(
                "eagle:user:" + id,
                cacheProtection.jitter(Duration.ofMinutes(30), 0.2),  // ±20% 抖动
                () -> repository.findById(id).orElse(null),
                User.class
        );
    }

    /** 布隆过滤器（启动期预热）*/
    @PostConstruct
    public void warmUpBloomFilter() {
        bloom.init("user:exist", 1_000_000, 0.001);
        bloom.addAll("user:exist", repository.findAllIds());
    }

    public Optional<User> tryFind(Long id) {
        if (!bloom.contains("user:exist", id)) return Optional.empty();
        return Optional.ofNullable(findById(id));
    }

    /** 库存原子扣减（CAS 防超扣）*/
    public void deductStock(Long skuId, int qty) {
        boolean ok = atomic.decrementIfSufficient("stock:sku:" + skuId, qty);
        if (!ok) throw OrderErrorCode.INSUFFICIENT_STOCK.toDomainException();
    }

    /** 分布式锁（注入 DistributedLock，参数为 long 秒）*/
    public void atomicUpdate(Long userId) {
        lock.tryLock("user:update:" + userId, 5L, 30L, () -> {
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

| key                             | 类型       | 默认     | 说明                 |
|---------------------------------|----------|--------|--------------------|
| `eagle.redis.default-ttl`       | Duration | `30m`  | 默认 TTL             |
| `eagle.redis.cache-null-values` | boolean  | `true` | 缓存 null（防穿透）       |
| `eagle.redis.key-prefix`        | String   | `""`   | Key 前缀             |
| `eagle.redis.transaction-aware` | boolean  | `true` | 事务提交后才写缓存          |
| `eagle.redis.cache-ttls`        | Map      | `{}`   | 按 cacheName 独立 TTL |

Redis 连接走 Spring Boot 标准 `spring.data.redis.*`。

## 常见错误

- ❌ `getWithLock` → ✅ 真实方法是 **`getWithMutex(key, ttl, loader, type)`**（4 参数含 Class）
- ❌ 把 Redisson 的 `lock.lock()` 直接用 → ✅ 注入 `DistributedLock`，统一抽象
- ❌ TTL 设为 -1 → ✅ 永不过期是反模式
- ❌ 布隆过滤器不 `init` 直接 `contains` → ✅ 启动期 init + 预热
- ❌ 高并发热点扣减用 SQL → ✅ `decrementIfSufficient`（CAS）
- ❌ 同 key 高 QPS 不防击穿 → ✅ 用 `getWithMutex`
- ❌ 列表全期一致 TTL → ✅ `jitter()` 加抖动防雪崩

## 关联规则

- `.claude/rules/04-data.md` — 事件驱动失效
