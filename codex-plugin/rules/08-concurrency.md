# 并发规范（Concurrency）

## 事务

- 所有写操作（create / update / delete）必须加 `@Transactional(rollbackFor = Exception.class)`
- 只读查询加 `@Transactional(readOnly = true)` 优化性能
- 禁止在 `@Transactional` 方法内调用远程服务（远程失败不应触发数据库回滚）
- 事务方法不得被同类内部调用（Spring AOP 代理限制）

## 领域事件异步处理

领域事件处理器必须使用 `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)`：

```java
// ✅ 正确：事务提交后异步执行，与主事务完全解耦
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderPaid(OrderPaidEvent event) {
    notificationService.sendConfirmation(event.getOrderId());
}

// ❌ 错误：同步处理，事件处理失败会导致主业务事务回滚
@EventListener
public void handleOrderPaid(OrderPaidEvent event) {
    notificationService.sendConfirmation(event.getOrderId());
}
```

## 跨域事件的事务传播

处理来自其他域的事件时，必须使用 `Propagation.REQUIRES_NEW` 开启独立事务，确保不与发送方事务耦合：

```java
// ✅ 正确：跨域事件使用独立事务，避免级联回滚
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handleAccountRegistered(AccountRegisteredEvent event) {
    userApplicationService.createUserFromAccount(event);
}

// ❌ 错误：使用默认事务传播，跨域事件处理失败可能影响发送方
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAccountRegistered(AccountRegisteredEvent event) {
    userApplicationService.createUserFromAccount(event);
}
```

## 事件驱动缓存失效

缓存失效优先由领域事件驱动，避免在应用服务上手动加 `@CacheEvict`：

```java
// ✅ 正确：聚合根方法中注册事件，Handler 异步失效缓存
public void updateProfile(UserProfile newProfile) {
    this.profile = newProfile;
    this.registerEvent(new UserUpdatedEvent(this.getId(), this.username));
}
// Handler 中：evictCache(event.getUsername());

// ❌ 错误：应用服务手动维护缓存（与业务耦合）
@CacheEvict(value = "USER_CACHE", allEntries = true)
public void updateProfile(...) {
}
```

**例外**：`delete` 等不通过聚合根方法的操作，仍可使用 `@CacheEvict`。

## 多缓存失效

多个缓存同时失效使用 `@Caching`（`@CacheEvict` 不可重复）：

```java
// ✅ 正确
@Caching(evict = {
        @CacheEvict(value = "CACHE_A", allEntries = true),
        @CacheEvict(value = "CACHE_B", allEntries = true)
})

// ❌ 错误：编译失败，@CacheEvict 不是可重复注解
@CacheEvict(value = "CACHE_A", allEntries = true)
@CacheEvict(value = "CACHE_B", allEntries = true)
```

## 并发控制

- 聚合根使用乐观锁（`@Version`，继承自 `BaseAggregateRoot` / `BaseEntity`）处理并发更新
- 需要悲观锁时使用 `@Lock(LockModeType.PESSIMISTIC_WRITE)`，严格控制锁粒度

## 线程安全

- Service / Repository / Controller 均为 Spring 单例 Bean，禁止定义可变实例变量
- 工具类必须设计为无状态（stateless）或线程安全
- 使用 `ThreadLocal` 时，使用完毕必须调用 `remove()` 防止内存泄漏

## 异步线程池

- **禁止**在业务代码中直接 `new Thread()` 或使用无界线程池
- 所有 `@Async` 方法统一使用集中配置的 `TaskExecutor` Bean
- 线程池应配置：合理的核心/最大线程数、有界队列、`CallerRunsPolicy` 拒绝策略、优雅关闭
