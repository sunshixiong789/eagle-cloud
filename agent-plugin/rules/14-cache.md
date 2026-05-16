# 缓存规范（Cache）

技术栈：`eagle-redis-starter` 提供 Redisson + Caffeine 多级缓存 + `CacheProtectionUtil`（穿透/击穿防护）+
`RedissonBloomFilterUtil`。

## 缓存层级选型

| 数据特征               | 选型                            |
|--------------------|-------------------------------|
| 单服务内频繁读、变更少（字典、配置） | Caffeine（本地）                  |
| 跨服务共享、变更频繁         | Redis                         |
| 高并发读、可容忍短暂不一致      | Caffeine + Redis 多级           |
| 计数器 / 限流 / 排行榜     | Redis 原生数据结构（不走 `@Cacheable`） |
| 大对象（> 100KB）       | **不缓存**，或拆字段缓存                |

## Key 命名规范

格式：`eagle:{module}:{entity}:{id}` 或 `eagle:{module}:{entity}:{biz-suffix}`

```
eagle:base:user:1001              # 用户聚合
eagle:base:user:username:alice    # 反查 Key
eagle:auth:token:revoked:abc123   # 黑名单
eagle:order:summary:list:page:0:size:20:tenant:t1   # 列表缓存（含查询条件）
```

- 全小写、`:` 分隔层级
- `entity` 用单数（`user` 不用 `users`）
- 多租户场景必须包含 `tenant:{tenantId}`
- **禁止**在 Key 中拼接用户输入（防注入），可哈希后拼接

## TTL（必须显式声明）

```yaml
# ✅ 通过 RedisCacheConfig 集中配置
spring.cache.redis:
  time-to-live: 30m              # 默认值，禁止 -1（永不过期）
```

```java
// ✅ 不同业务不同 TTL
@Cacheable(value = "USER_CACHE", key = "#id")        // 30 分钟
@Cacheable(value = "DICT_CACHE", key = "#code")      // 12 小时（变更少）
@Cacheable(value = "TOKEN_CACHE", key = "#jti")      // 与 Token 有效期一致
```

| 数据类型        | 推荐 TTL        | 备注          |
|-------------|---------------|-------------|
| 用户/角色/权限    | 30 min        | 事件驱动失效      |
| 字典/配置/枚举    | 12 h          | 变更后手动失效     |
| Token / 黑名单 | 与 Token 有效期对齐 | 不可早于        |
| 列表查询        | 5–15 min      | 短 TTL 容忍不一致 |
| 一次性验证码      | 5 min         | 校验后立即删除     |

**禁止**永不过期的缓存（`ttl=-1`）——内存泄漏。

## 三大穿透/击穿/雪崩防护

### 穿透（查不存在的 Key）

```java
// ✅ 方案一：缓存空值（短 TTL）
User user = redisTemplate.opsForValue().get(key);
if(user ==NULL_PLACEHOLDER)return Optional.

empty();
if(user ==null){
user =userRepository.

findById(id).

orElse(null);
    redisTemplate.

opsForValue().

set(key, user !=null?user:NULL_PLACEHOLDER,
    user !=null?Duration.ofMinutes(30) :Duration.

ofMinutes(2));
        }

// ✅ 方案二：布隆过滤器（大量 Key 时优先用此）
        if(!bloomFilter.

contains(id))return Optional.

empty();
```

### 击穿（热点 Key 过期瞬间）

使用 `CacheProtectionUtil.getWithMutex()` 获取互斥锁后回源（注意 4 个参数，最后一个是返回类型 Class）：

```java
// ✅ 单飞 — 同一 Key 同时只有一个线程回源
return cacheProtectionUtil.getWithMutex(
    "eagle:base:user:"+id,
    Duration.ofMinutes(30),
    ()->userRepository.

findById(id).

orElse(null),

User .class
);

// ✅ 防雪崩抖动：基础 TTL ± 20%
Duration ttl = cacheProtectionUtil.jitter(Duration.ofMinutes(30), 0.2);
```

### 雪崩（大量 Key 同时过期）

- TTL 添加随机抖动（基础 TTL ± 10%），由 `RedisCacheConfig` 统一处理
- 关键缓存预热（启动时主动加载）
- Redis 故障降级：`CacheErrorHandler` 捕获异常，直接走数据库

## `@Cacheable` 使用规则

```java
// ✅ 正确
@Cacheable(value = "USER_CACHE", key = "#id", unless = "#result == null")
public User findById(Long id) { ...}

@CacheEvict(value = "USER_CACHE", key = "#user.id")
public void delete(User user) { ...}

// ❌ 不要在私有方法上加（Spring AOP 代理失效）
@Cacheable("X")
private User load(Long id) { ...}

// ❌ 不要在同类内部调用（绕过代理）
public User x() {
    return load(1L);
}  // 缓存失效

// ❌ 不要在写方法上加 @Cacheable（语义混乱）
```

`unless = "#result == null"` 防止缓存 null 覆盖，但若已用空值穿透防护方案，应改为缓存 null 占位。

## 多缓存失效

```java
// ✅ @Caching 组合（@CacheEvict 不可重复）
@Caching(evict = {
        @CacheEvict(value = "USER_CACHE", key = "#user.id"),
        @CacheEvict(value = "USER_USERNAME_CACHE", key = "#user.username"),
        @CacheEvict(value = "USER_LIST_CACHE", allEntries = true)
})
public void update(User user) { ...}
```

## 事件驱动失效（推荐方式）

聚合根更新优先通过领域事件失效缓存（详见 `08-concurrency.md`）：

```java
// ✅ 聚合根方法注册事件
public void updateProfile(UserProfile p) {
    this.profile = p;
    registerEvent(new UserUpdatedEvent(getId(), getUsername()));
}

// 事件处理器异步失效
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onUserUpdated(UserUpdatedEvent e) {
    cacheManager.getCache("USER_CACHE").evict(e.getUserId());
}
```

## 一致性策略

- **写策略**：先写 DB → 提交事务 → AFTER_COMMIT 异步失效缓存（不是双写）
- **读策略**：先读缓存 → miss 回源 → 回写缓存（带 TTL）
- **强一致性场景**（如余额）：直接查 DB，不缓存

## 禁止清单

- 禁止 `@Cacheable` 缓存超大对象（> 100KB）
- 禁止缓存包含敏感字段的对象（密码、Token）
- 禁止用 `@Cacheable` 缓存写操作返回值
- 禁止在事务内写缓存（事务回滚 → 缓存与 DB 不一致）
- 禁止用 Redis 做强一致性事务（用 Redisson 锁 + DB 唯一约束替代）
- 禁止在循环中逐 Key 查询 Redis，使用 `multiGet` / `mget`
