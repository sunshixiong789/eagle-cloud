# 分布式事务规范

技术栈：`eagle-seata-starter` 提供 Seata AT/TCC 集成；`eagle-feign-starter` 自动透传 XID；`eagle-rocketmq-starter` 提供事务消息。

## 选型决策树

```
跨服务写操作
├── 强一致性必需 + 都是关系数据库 + 数据量小   → Seata AT
├── 强一致性必需 + 涉及非 SQL 资源 / 性能敏感 → Seata TCC
├── 最终一致性可接受（推荐 90% 场景）        → 本地消息表 + MQ
└── 单服务多数据源                           → JTA / @Transactional 复合
```

**默认选最终一致性**——AT/TCC 性能代价显著，仅在金融/扣库存等强一致场景使用。

## 方案一：Seata AT（默认模式）

适用：所有参与方都是关系数据库，且能容忍 RM 二阶段回滚的延迟（毫秒级）。

```java
// ✅ 仅在事务发起方加 @GlobalTransactional
@GlobalTransactional(rollbackFor = Exception.class, timeoutMills = 60000)
public void placeOrder(CreateOrderRequest req) {
    orderApplicationService.create(req);          // 本地事务
    inventoryFeignClient.lockStock(req.items());  // 远程，XID 自动透传
    walletFeignClient.deduct(req.userId(), req.amount());
}

// ✅ 远程参与方使用普通 @Transactional 即可
@Transactional(rollbackFor = Exception.class)
public void lockStock(...) { ... }
```

- **只在发起方**加 `@GlobalTransactional`，参与方不加
- `timeoutMills` 必须显式设置（默认 60s 偏长）
- 涉及 AT 的表必须有 `undo_log` 表（Seata Schema）
- **禁止**热点行高并发更新（AT 全局锁会成为瓶颈）

## 方案二：Seata TCC

适用：扣库存、扣余额等高并发场景，或参与方非关系型资源。

```java
@LocalTCC
public interface StockTccAction {
    @TwoPhaseBusinessAction(name = "stockReserve", commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepare(BusinessActionContext ctx, @BusinessActionContextParameter("productId") Long productId, @BusinessActionContextParameter("qty") int qty);

    boolean commit(BusinessActionContext ctx);
    boolean rollback(BusinessActionContext ctx);
}
```

- TCC **必须**保证三阶段方法**幂等**（commit/rollback 可能被重试）
- **空回滚**：未执行 try 直接 rollback → 写入"已回滚"标记跳过
- **悬挂**：try 在 rollback 之后到达 → 检查"已回滚"标记拒绝
- 所有 TCC 接口实现必须维护 `t_tcc_action_log` 记录三阶段状态

## 方案三：本地消息表 + MQ（推荐）

适用：90% 场景，写本地事务 + 发外部消息原子性保证。

```java
// ✅ 利用 RocketMQ 事务消息（eagle-rocketmq-starter 已封装）
@Transactional(rollbackFor = Exception.class)
public void payOrder(Long orderId) {
    Order order = orderRepository.findByIdForUpdate(orderId);
    order.markPaid();
    orderRepository.save(order);

    publisher.publishInTransaction(
        "prod_order_paid",
        new OrderPaidIntegrationEvent(orderId, ...),
        () -> {} // 事务消息回调（在本地事务内）
    );
}
```

下游服务消费消息执行后续动作，**必须幂等**（详见 `15-messaging.md`）。

```
┌──────────┐                ┌─────────┐                ┌──────────┐
│ 订单服务  │ — 本地事务 + → │ MQ      │ — 至少一次 — →  │ 库存服务  │
│ DB 写入  │   消息发送      │ Broker  │   消费幂等      │ DB 写入  │
└──────────┘                └─────────┘                └──────────┘
```

## XID 透传

`eagle-feign-starter` 已注册 `SeataXidRequestInterceptor`：

- HTTP 请求自动加上 `TX_XID` 头
- 远程方收到后自动加入到当前 XID 上下文

**禁止**手动操作 `RootContext.bind/unbind`，由 starter 自动管理。

## 失败处理

| 失败类型 | 行为 |
|---------|------|
| 全局事务超时（TC 触发） | 自动 rollback 所有参与方 |
| 参与方 commit 失败 | TC 重试直到达到最大次数 → 人工介入 |
| 参与方下线 | 数据保持中间态，等服务恢复后 TC 重试 |
| 网络抖动 | 自动重试 3 次 |

**告警阈值**：单服务回滚率 > 1% / 全局事务超时数 > 0 → 立即告警。

## 跨服务调用的隔离

```java
// ❌ 禁止：在 @Transactional 内调用远程
@Transactional
public void x() {
    repo.save(...);
    feignClient.callRemote();   // 远程慢 → DB 连接长时间持有
}

// ✅ 拆分：本地事务 + 异步事件触发远程
@Transactional
public void x() {
    repo.save(aggregate);
    aggregate.registerEvent(new XEvent(...));   // AFTER_COMMIT 异步触发远程
}
```

例外：`@GlobalTransactional` 模式下"必须"在事务内调用远程（这正是分布式事务的目的）。

## 测试

- 单元测试：使用 `@SpringBootTest` + Testcontainers 启动 Seata + RocketMQ + DB
- 故障注入：用 `chaosblade` 模拟网络分区 / 服务宕机，验证补偿正确性
- **禁止**仅靠开发环境观察验证分布式事务正确性

## 禁止清单

- 禁止在 `@Transactional` 默认传播下嵌套调用（自调用导致事务失效）
- 禁止用 `try-catch` 吞掉 `@GlobalTransactional` 下的异常（导致全局事务无法感知）
- 禁止 TCC 三阶段方法非幂等
- 禁止在没有补偿机制的情况下手动跨服务调用
- 禁止用分布式锁替代分布式事务（锁解决并发，不解决一致性）
