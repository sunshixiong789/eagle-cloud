# eagle-redis-starter

基于 Redisson + Spring Cache 的 Redis 功能封装模块，提供开箱即用的缓存、分布式锁、限流、原子计数、布隆过滤器、延迟队列、发布订阅等能力。

## 目录

- [模块概述](#模块概述)
- [引入依赖](#引入依赖)
- [自动配置说明](#自动配置说明)
- [配置参考](#配置参考)
- [使用规范](#使用规范)
    - [Key 命名规范](#key-命名规范)
    - [序列化规范](#序列化规范)
    - [缓存使用规范](#缓存使用规范)
    - [异常处理规范](#异常处理规范)
- [功能使用说明](#功能使用说明)
    - [Spring Cache 注解缓存](#spring-cache-注解缓存)
    - [分布式锁 RedisLockUtil](#分布式锁-redislockutil)
    - [限流 RedisRateLimiter](#限流-redisratelimiter)
    - [Redisson 限流 RedissonRateLimiterUtil](#redisson-限流-redissonratelimiterutil)
    - [原子计数 RedissonAtomicUtil](#原子计数-redissonatomicutil)
    - [布隆过滤器 RedissonBloomFilterUtil](#布隆过滤器-redissonbloomfilterutil)
    - [延迟队列 RedissonDelayedQueueUtil](#延迟队列-redissondelayedqueueutil)
    - [发布订阅 RedissonTopicUtil](#发布订阅-redissontopiutil)
- [工具选型指南](#工具选型指南)
- [常见问题](#常见问题)

---

## 模块概述

### 提供的能力

| 分类     | 组件                              | 说明                                     |
|--------|---------------------------------|----------------------------------------|
| 缓存     | `RedisCacheManager`（自动配置）       | Spring Cache 注解驱动，支持 JSON 序列化、按域配置 TTL |
| 缓存     | `RedisTemplate<String, Object>` | 低层级操作，key 为 String，value 为 JSON        |
| 分布式锁   | `RedisLockUtil`                 | 基于 Redisson `RLock`，支持自动释放             |
| 限流（轻量） | `RedisRateLimiter`              | Lua 脚本实现令牌桶 + 滑动窗口                     |
| 限流（持久） | `RedissonRateLimiterUtil`       | Redisson `RRateLimiter`，支持全局/单节点模式     |
| 原子计数   | `RedissonAtomicUtil`            | 基于 `RAtomicLong`，CAS 无锁操作              |
| 布隆过滤器  | `RedissonBloomFilterUtil`       | 防缓存穿透，基于 `RBloomFilter`                |
| 延迟队列   | `RedissonDelayedQueueUtil`      | 延迟任务，重启不丢，基于 `RDelayedQueue`           |
| 发布订阅   | `RedissonTopicUtil`             | 分布式广播，基于 `RTopic`                      |

### 依赖关系

```
eagle-redis-starter
├── eagle-common-starter          ← 异常体系 (AppException / ErrorCode)
├── spring-boot-starter-data-redis ← RedisTemplate / StringRedisTemplate
├── redisson-spring-boot-starter   ← RedissonClient（自动注入）
└── spring-boot-starter-cache      ← @EnableCaching / @Cacheable 等注解
```

---

## 引入依赖

在需要使用 Redis 功能的服务模块 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-redis-starter')
}
```

引入后自动生效，无需额外 `@EnableCaching` 或其他注解。

---

## 自动配置说明

模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册以下两个自动配置类：

| 配置类                      | 条件                                              | 注册的 Bean                                                                 |
|--------------------------|-------------------------------------------------|--------------------------------------------------------------------------|
| `RedisCacheConfig`       | `RedisConnectionFactory` 存在                     | `redisObjectMapper`、`redisJsonSerializer`、`RedisTemplate`、`CacheManager` |
| `RedisAutoConfiguration` | `RedisOperations` 存在（在 `RedisCacheConfig` 之后加载） | 无额外 Bean（入口类）                                                            |

所有工具类（`RedisLockUtil`、`RedisRateLimiter` 等）均通过 `@Component` 注册为 Bean，引入模块后可直接
`@RequiredArgsConstructor` 注入使用。

**Bean 覆盖：** 若项目有特殊需求，可声明同名 Bean 覆盖默认实现：

```java
// 覆盖默认 CacheManager（如需多级缓存等自定义逻辑）
@Bean
@Primary
public CacheManager cacheManager(...) { ... }

// 覆盖默认 RedisTemplate
@Bean("redisTemplate")
public RedisTemplate<String, Object> redisTemplate(...) { ... }
```

---

## 配置参考

在服务的 `application.yml` 中按需配置，所有项均有默认值，未配置时直接使用。

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      lettuce:                          # 连接池（Lettuce）
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 2
          max-wait: 1000ms

eagle:
  redis:
    default-ttl: 30m                    # 默认缓存 TTL（默认 30 分钟）
    cache-null-values: true             # 是否缓存 null，防缓存穿透（默认 true）
    key-prefix: ""                      # 全局 key 前缀，多服务共用 Redis 时设置（如 "sys:"）
    transaction-aware: true             # 缓存写操作与事务同步（默认 true）
    cache-ttls:                         # 各缓存域独立 TTL（未配置的域使用 default-ttl）
      USER_CACHE: 1h
      ROLE_CACHE: 24h
      PERMISSION_CACHE: 24h
      ORDER_CACHE: 30m
```

### 配置项说明

| 配置项                             | 类型                      | 默认值    | 说明                                                       |
|---------------------------------|-------------------------|--------|----------------------------------------------------------|
| `eagle.redis.default-ttl`       | `Duration`              | `30m`  | 所有未单独配置的缓存的过期时间                                          |
| `eagle.redis.cache-null-values` | `boolean`               | `true` | 为 `true` 时，查询结果为 null 也会缓存（防缓存穿透）；为 `false` 时，null 结果不缓存 |
| `eagle.redis.key-prefix`        | `String`                | `""`   | 全局 key 前缀，设置后 key 格式变为 `{prefix}{cacheName}::{key}`      |
| `eagle.redis.transaction-aware` | `boolean`               | `true` | 开启后，`@CacheEvict` 等写操作在当前事务提交后才执行，防止事务回滚导致的缓存/DB 不一致     |
| `eagle.redis.cache-ttls`        | `Map<String, Duration>` | `{}`   | 按缓存域名称配置独立 TTL，key 为 `@Cacheable(value = "...")` 中的值     |

### Redisson 连接配置

Redisson 连接由 `redisson-spring-boot-starter` 自动读取 `spring.data.redis.*` 配置，无需额外配置 `redisson.yml`
。如需集群模式或哨兵模式，按 Redisson 文档配置即可。

---

## 使用规范

### Key 命名规范

所有 Redis key 必须遵循统一命名格式，便于监控、排查和批量清理：

```
{业务域}:{资源类型}:{标识}
```

| 示例                           | 说明              |
|------------------------------|-----------------|
| `stock:sku:1001`             | 商品 SKU 1001 的库存 |
| `lock:order:create:userId`   | 用户下单的分布式锁       |
| `rate_limit:api:createOrder` | 下单接口限流 key      |
| `user:exist`                 | 用户布隆过滤器         |
| `order:timeout`              | 订单超时延迟队列        |
| `cache:evict:user`           | 用户缓存失效 Topic    |

**禁止：**

- 禁止使用无意义的短 key（如 `u1`、`tmp`）
- 禁止不同业务复用相同 key 名
- 多服务共用 Redis 时，必须在 `eagle.redis.key-prefix` 中配置服务前缀

### 序列化规范

模块自动配置以下序列化策略，**禁止手动修改**：

| 对象              | Key 序列化                 | Value 序列化             |
|-----------------|-------------------------|-----------------------|
| `RedisTemplate` | `StringRedisSerializer` | JSON（含 `@class` 类型信息） |
| `CacheManager`  | `StringRedisSerializer` | JSON（含 `@class` 类型信息） |

Value 使用带类型信息的 JSON（`DefaultTyping.NON_FINAL`），反序列化时可还原为原始类型，不会变成 `LinkedHashMap`。

**注意：** Redis 专用 `ObjectMapper`（Bean 名 `redisObjectMapper`）与 Spring MVC 全局 `ObjectMapper` 完全隔离，修改 MVC 的
Jackson 配置不会影响 Redis 序列化行为。

**缓存对象要求：** 存入 Redis 的对象必须是可序列化的 POJO，避免存入以下类型：

- JPA 懒加载代理对象（`Hibernate$ByteBuddy$xxx`）— 序列化会触发额外 SQL 或失败
- `Page<T>` 等包含不可序列化字段的 Spring 内置类型
- 含有循环引用的对象图

### 缓存使用规范

**1. 缓存域命名**

`@Cacheable(value = "...")` 中的 `value` 统一使用大写下划线风格，并在 `application.yml` 显式配置 TTL：

```yaml
# ✅ 必须在配置文件中声明各缓存域的 TTL
eagle.redis.cache-ttls:
  USER_CACHE: 1h
  PERMISSION_CACHE: 24h
```

```java
// ✅ value 使用大写下划线
@Cacheable(value = "USER_CACHE", key = "#userId")
public UserResponse findById(Long userId) { ... }

// ❌ 禁止随意命名缓存域
@Cacheable(value = "user", key = "#id")
```

**2. 缓存粒度**

- 缓存**单个对象**而非列表，便于精确失效
- 列表 / 分页查询不建议缓存（分页条件变化多，缓存命中率低）
- 缓存 Response DTO，不要缓存领域对象（避免序列化 JPA 代理）

```java
// ✅ 缓存单个 DTO
@Cacheable(value = "USER_CACHE", key = "#userId")
public UserResponse findById(Long userId) { ... }

// ❌ 不建议缓存分页列表
@Cacheable(value = "USER_LIST")
public Page<UserResponse> findAll(Pageable pageable) { ... }
```

**3. 缓存失效策略**

优先通过**领域事件**驱动缓存失效，避免在应用服务中手动 `@CacheEvict`：

```java
// ✅ 推荐：聚合根更新时注册事件，事件处理器负责失效缓存
public void updateProfile(UserProfile profile) {
    this.profile = profile;
    registerEvent(new UserUpdatedEvent(getId(), getUsername()));
}

// 事件处理器（infrastructure/event/）
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onUserUpdated(UserUpdatedEvent event) {
    cacheManager.getCache("USER_CACHE").evict(event.getUserId());
}
```

`delete` 等不通过聚合根方法的操作，直接用 `@CacheEvict`：

```java
@CacheEvict(value = "USER_CACHE", key = "#userId")
@Transactional(rollbackFor = Exception.class)
public void deleteUser(Long userId) { ... }
```

**4. 事务一致性**

`transaction-aware: true`（默认）确保缓存写操作（`@CacheEvict`、`@CachePut`）在事务提交后执行，防止事务回滚后缓存被错误清空：

```java
// 场景：事务回滚时，缓存不会被错误失效
@CacheEvict(value = "USER_CACHE", key = "#userId")
@Transactional(rollbackFor = Exception.class)
public void updateUser(Long userId, UpdateUserRequest request) {
    userRepository.save(...);
    // 若此处抛出异常，事务回滚，@CacheEvict 不会执行 ✅
    someOtherService.doSomethingThatMightFail();
}
```

### 异常处理规范

分布式锁失败时抛出 `ServiceException`（HTTP 500），由全局异常处理器统一返回。业务代码**无需 try-catch**：

```java
// ✅ 直接调用，锁失败时全局处理器返回 500
redisLockUtil.tryLock("order:create:" + userId, () -> {
    orderRepository.save(order);
});

// ❌ 不需要手动处理锁失败
try {
    redisLockUtil.tryLock(...);
} catch (ServiceException e) {
    return "系统繁忙";
}
```

---

## 功能使用说明

### Spring Cache 注解缓存

直接使用 Spring Cache 注解，无需额外配置：

```java
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    // 查询缓存（缓存 key = 方法参数 userId）
    @Cacheable(value = "USER_CACHE", key = "#userId")
    @Transactional(readOnly = true)
    public UserResponse findById(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::toNotFoundException);
    }

    // 更新后清除指定 key 的缓存
    @CacheEvict(value = "USER_CACHE", key = "#userId")
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UpdateUserRequest request) { ... }

    // 删除后清除整个缓存域
    @CacheEvict(value = "USER_CACHE", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) { ... }

    // 同时失效多个缓存域
    @Caching(evict = {
        @CacheEvict(value = "USER_CACHE", allEntries = true),
        @CacheEvict(value = "ROLE_CACHE", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public void rebuildPermissions() { ... }
}
```

---

### 分布式锁 RedisLockUtil

#### 适用场景

- 防止同一用户重复提交（重复下单、重复支付）
- 分布式环境下需要串行执行的操作
- 秒杀、抢购等高竞争场景下的悲观锁控制

#### 不适用场景

- 计数器原子自增/自减 → 使用 `RedissonAtomicUtil`
- 高并发低竞争的库存扣减 → 使用 `RedissonAtomicUtil.decrementIfSufficient`（CAS 更高效）

#### 默认参数

| 参数          | 默认值  | 说明                                 |
|-------------|------|------------------------------------|
| `waitTime`  | 3 秒  | 等待获取锁的最长时间，超时抛出 `ServiceException` |
| `leaseTime` | 30 秒 | 持锁最长时间，超时后自动释放（防死锁）                |

#### 使用示例

```java
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final RedisLockUtil redisLockUtil;

    // 有返回值（默认 waitTime=3s, leaseTime=30s）
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        return redisLockUtil.tryLock("order:create:" + userId, () -> {
            // 幂等校验
            if (orderRepository.existsByRequestId(request.getRequestId())) {
                throw OrderErrorCode.DUPLICATE_ORDER.toDomainException();
            }
            Order order = Order.create(userId, request);
            orderRepository.save(order);
            return orderMapper.toResponse(order);
        });
    }

    // 无返回值（自定义等待 5 秒，持锁 60 秒）
    public void syncInventory() {
        redisLockUtil.tryLock("sync:inventory", 5, 60, () -> {
            inventorySyncService.syncFromWarehouse();
        });
    }
}
```

---

### 限流 RedisRateLimiter

基于 Lua 脚本实现原子操作，适合**无状态的接口限流**。

#### 两种算法对比

| 算法   | 方法                 | 特点            | 适合场景         |
|------|--------------------|---------------|--------------|
| 令牌桶  | `tryAcquire`       | 允许一定突发，稳定补充令牌 | 接口 QPS 限制    |
| 滑动窗口 | `tryAcquireWindow` | 精确控制时间窗口内总次数  | 频率限制（如短信验证码） |

#### 令牌桶示例

```java
@Service
@RequiredArgsConstructor
public class SmsApplicationService {

    private final RedisRateLimiter rateLimiter;

    public void sendVerifyCode(String phone) {
        // 每个手机号：每秒补充 1 个令牌，桶容量 3（允许短暂突发）
        if (!rateLimiter.tryAcquire("sms:" + phone, 3, 1.0)) {
            throw SmsErrorCode.SEND_TOO_FREQUENT.toDomainException();
        }
        smsProvider.send(phone, generateCode());
    }
}
```

#### 滑动窗口示例

```java
// 每个用户每分钟最多 10 次查询
if (!rateLimiter.tryAcquireWindow("query:" + userId, 10, Duration.ofMinutes(1))) {
    throw CommonErrorCode.RATE_LIMIT_EXCEEDED.toDomainException();
}
```

---

### Redisson 限流 RedissonRateLimiterUtil

基于 Redisson `RRateLimiter`，适合**长期运行的限流器**或**对下游服务的调用速率控制**。

#### 与 RedisRateLimiter 的区别

|      | `RedisRateLimiter` | `RedissonRateLimiterUtil`           |
|------|--------------------|-------------------------------------|
| 实现   | Lua 脚本             | Redisson `RRateLimiter`             |
| 状态   | 无持久状态（轻量）          | Redis 持久状态                          |
| 模式   | 无                  | 支持 `OVERALL`（全局）/ `PER_CLIENT`（单节点） |
| 适合场景 | 接口限流（短期）           | 下游调用速率控制（长期）                        |

#### 使用示例

```java
@Service
@RequiredArgsConstructor
public class WechatNotifyService {

    private final RedissonRateLimiterUtil rateLimiterUtil;

    public void sendTemplateMsg(String openId, String content) {
        // 微信 API 全局每秒不超过 100 次（多实例合计）
        boolean acquired = rateLimiterUtil.tryAcquire(
            "wechat:template-msg",
            100, 1, RateIntervalUnit.SECONDS,
            RateType.OVERALL, 1
        );
        if (!acquired) {
            throw ExternalErrorCode.WECHAT_RATE_LIMIT.toServiceException();
        }
        wechatClient.sendTemplate(openId, content);
    }
}
```

> **注意：** 修改限流速率前需先删除旧限流器，否则 `trySetRate` 对已存在的限流器无效：
> ```java
> rateLimiterUtil.delete("wechat:template-msg");   // 先删除
> rateLimiterUtil.tryAcquire("wechat:template-msg", newRate, ...);  // 再创建
> ```

---

### 原子计数 RedissonAtomicUtil

基于 `RAtomicLong` 实现，所有操作均为原子性，**无需加锁**，适合高并发读写计数场景。

#### 适用场景

| 场景          | 方法                        |
|-------------|---------------------------|
| 库存扣减（防超卖）   | `decrementIfSufficient`   |
| 积分 / 余额消费   | `decrementIfSufficient`   |
| 计数器（浏览量、点赞） | `increment` / `addAndGet` |
| CAS 版本控制    | `compareAndSet`           |

#### 完整示例：库存管理

```java
@Service
@RequiredArgsConstructor
public class StockService {

    private final RedissonAtomicUtil atomicUtil;

    /** 商品上架时初始化库存（幂等，已存在不覆盖） */
    public void initStock(Long skuId, long quantity) {
        atomicUtil.initIfAbsent("stock:sku:" + skuId, quantity, Duration.ofDays(7));
    }

    /** 下单扣减库存，不足返回 false */
    public boolean deductStock(Long skuId, long quantity) {
        return atomicUtil.decrementIfSufficient("stock:sku:" + skuId, quantity);
    }

    /** 取消订单回滚库存 */
    public void restoreStock(Long skuId, long quantity) {
        atomicUtil.addAndGet("stock:sku:" + skuId, quantity);
    }

    /** 查询实时库存 */
    public long queryStock(Long skuId) {
        return atomicUtil.get("stock:sku:" + skuId);
    }
}
```

**在应用服务中使用：**

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(CreateOrderRequest request) {
    boolean deducted = stockService.deductStock(
        request.getSkuId(), request.getQuantity());
    if (!deducted) {
        throw OrderErrorCode.INSUFFICIENT_STOCK.toDomainException();
    }
    orderRepository.save(Order.create(request));
}
```

#### 并发控制策略选择

`decrementIfSufficient` 内部使用 **CAS 自旋**，无需加锁：

- **大库存 + 高并发**（如电商普通商品）→ 直接使用 `decrementIfSufficient`，无锁效率高
- **极小库存 + 极高并发**（如秒杀 1 件）→ 配合 `RedisLockUtil` 悲观锁，减少无效自旋：

```java
// 秒杀场景：先加锁再扣减，避免大量 CAS 自旋
redisLockUtil.tryLock("seckill:sku:" + skuId, 1, 5, () -> {
    if (!atomicUtil.decrementIfSufficient("stock:sku:" + skuId, 1)) {
        throw OrderErrorCode.INSUFFICIENT_STOCK.toDomainException();
    }
    orderRepository.save(Order.create(skuId, userId));
});
```

---

### 布隆过滤器 RedissonBloomFilterUtil

用于**防止缓存穿透**：查询前先判断数据是否可能存在，若一定不存在则直接拦截，无需查询缓存和数据库。

> **特性：** 判断"不存在"时 100% 准确；判断"存在"时有一定误判率（false positive），误判率可在初始化时配置。

#### 使用规范

1. 应用**启动时**必须初始化并预热（从 DB 加载所有有效 ID）
2. 新增数据时**同步写入**布隆过滤器
3. 布隆过滤器**不支持删除**，若有大量删除操作需定期重建（重新 `delete` + `init` + 全量预热）

#### 完整示例

**步骤 1：启动预热**

```java
@Component
@RequiredArgsConstructor
public class BloomFilterInitializer implements ApplicationRunner {

    private final RedissonBloomFilterUtil bloomFilter;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 预期容量 100 万，误判率 0.1%
        bloomFilter.init("user:exist", 1_000_000, 0.001);
        bloomFilter.addAll("user:exist", userRepository.findAllIds());
    }
}
```

**步骤 2：查询时拦截**

```java
@Cacheable(value = "USER_CACHE", key = "#userId")
@Transactional(readOnly = true)
public UserResponse findById(Long userId) {
    // 布隆过滤器判断一定不存在 → 直接返回 null，不查 DB
    if (!bloomFilter.contains("user:exist", userId)) {
        return null;
    }
    return userRepository.findById(userId)
            .map(userMapper::toResponse)
            .orElse(null);
}
```

**步骤 3：新增时同步写入**

```java
@Transactional(rollbackFor = Exception.class)
public void createUser(CreateUserRequest request) {
    User user = User.create(request);
    userRepository.save(user);
    bloomFilter.add("user:exist", user.getId());
}
```

---

### 延迟队列 RedissonDelayedQueueUtil

基于 `RDelayedQueue` 实现，任务到期后自动转入消费队列，**服务重启后任务不丢失**。

#### 适用场景

- 订单超时未支付自动取消
- 优惠券到期提醒
- 延迟通知（如注册 N 天后推送引导消息）

#### 不适用场景

需要可靠消息投递（持久化、消费确认、死信队列、广播消费）→ 使用 `eagle-rocketmq-starter`。

#### 生产者：投递任务

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(CreateOrderRequest request) {
    Order order = Order.create(request);
    orderRepository.save(order);

    // 30 分钟后触发超时检查
    delayedQueue.offer("order:timeout", order.getId(), 30, TimeUnit.MINUTES);
}
```

#### 消费者：监听到期任务

推荐使用 `@Async` 方法循环阻塞消费，并在 `ApplicationRunner` 中启动：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer implements ApplicationRunner {

    private final RedissonDelayedQueueUtil delayedQueue;
    private final OrderApplicationService orderApplicationService;

    @Override
    public void run(ApplicationArguments args) {
        startConsuming();
    }

    @Async
    public void startConsuming() {
        while (!Thread.currentThread().isInterrupted()) {
            Long orderId = delayedQueue.take("order:timeout");  // 阻塞等待
            if (orderId == null) break;  // 线程被中断
            try {
                orderApplicationService.cancelIfUnpaid(orderId);
            } catch (Exception e) {
                log.error("订单超时处理失败，orderId={}", orderId, e);
                // 生产环境建议将失败任务写入死信队列或告警
            }
        }
    }
}
```

#### 取消未到期任务

```java
// 用户主动支付后，取消超时检查任务
delayedQueue.cancel("order:timeout", orderId);
```

#### 定时轮询模式（可选）

若不想使用阻塞线程，可用 `@Scheduled` 定时非阻塞轮询：

```java
@Scheduled(fixedDelay = 500)   // 每 500ms 轮询一次
public void pollTimeoutOrders() {
    Long orderId;
    while ((orderId = delayedQueue.poll("order:timeout")) != null) {
        orderApplicationService.cancelIfUnpaid(orderId);
    }
}
```

---

### 发布订阅 RedissonTopicUtil

基于 `RTopic` 实现，向指定 topic 发布消息后，所有订阅该 topic 的节点（包含当前节点）均会收到。

#### 适用场景

- **多节点本地缓存失效同步**（最典型用途）
- 配置变更实时通知
- 节点间轻量级广播

#### 与 RocketMQ 的区别

|       | `RedissonTopicUtil` | `eagle-rocketmq-starter` |
|-------|---------------------|--------------------------|
| 消息持久化 | 否（断连丢失）             | 是                        |
| 消费确认  | 否                   | 是                        |
| 广播方式  | 全节点广播（含发送方）         | 可选广播 / 集群消费              |
| 适合场景  | 缓存刷新、配置推送           | 业务事件、订单、通知               |

#### 完整示例：多节点 Caffeine 本地缓存同步

```java
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final RedissonTopicUtil topicUtil;
    private final Cache<Long, UserResponse> localCache;  // Caffeine L1 缓存

    private static final String TOPIC = "cache:evict:user";

    /** 应用启动时订阅缓存失效通知 */
    @PostConstruct
    public void subscribe() {
        topicUtil.subscribe(TOPIC, Long.class, "userCacheEvict",
            (channel, userId) -> localCache.invalidate(userId));
    }

    /** 更新用户后广播通知所有节点失效本地缓存 */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UpdateUserRequest request) {
        userRepository.save(userMapper.toDomain(request));
        topicUtil.publish(TOPIC, userId);  // 广播给所有节点（含当前节点）
    }
}
```

#### 订阅去重

相同 `listenerKey` 重复订阅会被忽略，在 `@PostConstruct` 中安全调用：

```java
// 第二次调用不会重复注册监听器
topicUtil.subscribe("cache:evict:user", Long.class, "userCacheEvict", listener);
topicUtil.subscribe("cache:evict:user", Long.class, "userCacheEvict", listener); // 跳过
```

---

## 工具选型指南

| 需求             | 推荐工具                                       | 备注            |
|----------------|--------------------------------------------|---------------|
| 防重复提交、串行执行     | `RedisLockUtil`                            | 悲观锁，适合低并发串行场景 |
| 库存扣减（大库存）      | `RedissonAtomicUtil.decrementIfSufficient` | CAS 无锁，高并发高效  |
| 库存扣减（秒杀场景）     | `RedisLockUtil` + `RedissonAtomicUtil`     | 悲观锁减少无效自旋     |
| 计数器（浏览量、点赞）    | `RedissonAtomicUtil.increment`             | 原子自增，无需加锁     |
| 接口 QPS 限流（无状态） | `RedisRateLimiter`                         | 轻量，Lua 脚本     |
| 对下游服务调用限速      | `RedissonRateLimiterUtil`                  | 支持全局 / 单节点模式  |
| 防缓存穿透          | `RedissonBloomFilterUtil`                  | 需启动预热，不支持删除   |
| 延迟任务（超时取消等）    | `RedissonDelayedQueueUtil`                 | 持久化，重启不丢      |
| 多节点缓存同步        | `RedissonTopicUtil`                        | 广播，允许少量丢失     |
| 可靠消息（持久化）      | `eagle-rocketmq-starter`                   | 需要确认 / 死信时使用  |

---

## 常见问题

**Q: `@CacheEvict` 没有生效，缓存没有被清除？**

A: 检查以下几点：

1. 是否通过 Spring 代理调用（不能是同类内部调用）
2. `transaction-aware: true` 时，缓存失效在事务提交后执行，确认事务是否正常提交
3. key 表达式是否正确（`#userId` 对应方法参数名）

---

**Q: 缓存读取后返回 `LinkedHashMap` 而不是原始对象？**

A: 自动配置的序列化器已开启类型信息（`@class`），正常情况不会出现此问题。若发生，检查是否有自定义 `RedisTemplate` 覆盖了默认配置，确认
value 序列化器是 `redisJsonSerializer`。

---

**Q: 多服务共用一个 Redis，key 冲突怎么办？**

A: 配置服务专属前缀：

```yaml
eagle.redis.key-prefix: "sys:"  # eagle-system-server
eagle.redis.key-prefix: "gw:"   # eagle-gateway-server
```

最终 key 格式为 `{prefix}{cacheName}::{key}`，如 `sys:USER_CACHE::1001`。

---

**Q: 延迟队列消费者服务重启后，已有的延迟任务还在吗？**

A: 任务保存在 Redis 中，服务重启后只需重新启动消费者循环即可继续消费，任务不会丢失。推荐在 `ApplicationRunner.run()`
中启动消费者，确保服务启动后立即开始处理。

---

**Q: 布隆过滤器中的数据怎么删除？**

A: 布隆过滤器**不支持单条删除**。若需要删除已有数据（如用户注销），可以：

1. 接受误判：允许该 ID 通过过滤器，后续查询 DB 返回空即可
2. 定期重建：在低峰期执行 `bloomFilter.delete(filterName)` + 重新 `init` + 全量预热
