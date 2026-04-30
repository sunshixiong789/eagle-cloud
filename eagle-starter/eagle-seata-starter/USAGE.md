# eagle-seata-starter — Seata 分布式事务（AT / TCC）

## 何时使用

- 跨服务**强一致性**写操作
- 多个数据库 / 多个服务的事务必须原子
- 需要 XID 跨服务自动透传

## 何时不要使用

- 最终一致性可接受（用本地消息表 + RocketMQ 事务消息）
- 单服务多数据源（用 JTA）
- 高并发热点行更新（AT 全局锁瓶颈，改用 TCC）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-seata-starter')
```

```yaml
eagle.seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: eagle_tx_group
```

`SeataEnvironmentPostProcessor` 自动桥接到 Seata 原生 `seata.*` 配置。Seata Server 接入参数（注册中心、配置中心）按 Seata
文档单独配置。

参与方需要建 `undo_log` 表（AT 模式）。

## 核心 API

| 类 / 注解                      | 用途                                                                              |
|-----------------------------|---------------------------------------------------------------------------------|
| `@GlobalTransactional`      | Seata 标准注解：发起方加，开启全局事务（不是 starter 自定义）                                          |
| `GlobalTransactionTemplate` | 编程式全局事务：`execute(txName, TransactionCallback<T>)` / `execute(txName, Runnable)` |
| `TransactionCallback<T>`    | 函数式：`T doInTransaction()`                                                       |
| `TccAction<T>`              | TCC 抽象：`tryAction(ctx, T param)` / `confirm(ctx)` / `cancel(ctx)`               |
| `TccIdempotencyHelper`      | TCC 幂等 / 防悬挂 / 防空回滚帮助类                                                          |
| `SeataUtil`                 | `getCurrentXid()` / `bind` / `unbind`                                           |

## AT 模式示例（最常用）

```java
// ✅ 仅发起方加 @GlobalTransactional
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final InventoryFeignClient inventoryClient;
    private final WalletFeignClient walletClient;

    @GlobalTransactional(rollbackFor = Exception.class, timeoutMills = 60000)
    public void placeOrder(CreateOrderRequest req) {
        Order order = orderRepository.save(Order.create(req));
        inventoryClient.lockStock(req.getItems());           // XID 自动透传（feign starter）
        walletClient.deduct(req.getUserId(), req.getAmount());
    }
}

// ✅ 参与方用普通 @Transactional
@Service
@Transactional(rollbackFor = Exception.class)
public class InventoryService {
    public void lockStock(List<Item> items) {
        // Seata 自动加入全局事务
    }
}
```

## 编程式（GlobalTransactionTemplate）

```java

@Service
@RequiredArgsConstructor
public class BatchService {

    private final GlobalTransactionTemplate txTemplate;

    public List<Long> batchProcess(List<Item> items) {
        // 有返回值
        return txTemplate.execute("batchProcess", () -> {
            return items.stream().map(this::processOne).toList();
        });

        // 无返回值
        // txTemplate.execute("batch", () -> items.forEach(this::processOne));
    }
}
```

## TCC 模式示例（高并发场景）

```java

@LocalTCC
public interface InventoryTccAction extends TccAction<InventoryTccParam> {

    @TwoPhaseBusinessAction(name = "inventoryTcc",
            commitMethod = "confirm", rollbackMethod = "cancel")
    @Override
    boolean tryAction(BusinessActionContext ctx,
                      @BusinessActionContextParameter("param") InventoryTccParam param);

    @Override
    boolean confirm(BusinessActionContext ctx);

    @Override
    boolean cancel(BusinessActionContext ctx);
}

@Service
@RequiredArgsConstructor
public class InventoryTccActionImpl implements InventoryTccAction {

    private final TccIdempotencyHelper idempotency;
    private final InventoryRepository inventoryRepository;

    @Override
    public boolean tryAction(BusinessActionContext ctx, InventoryTccParam param) {
        return idempotency.runOnce(ctx, () ->
                inventoryRepository.freezeStock(param.getProductId(), param.getQuantity())
        );
    }

    @Override
    public boolean confirm(BusinessActionContext ctx) {
        return idempotency.runOnce(ctx, () -> {
            // 执行真正扣减（幂等保证）
            return inventoryRepository.deductFrozen(ctx);
        });
    }

    @Override
    public boolean cancel(BusinessActionContext ctx) {
        return idempotency.runOnce(ctx, () -> {
            // 释放冻结（处理空回滚 / 悬挂）
            inventoryRepository.releaseFrozen(ctx);
            return true;
        });
    }
}
```

## 配置项

| key                            | 类型      | 默认               | 说明                                         |
|--------------------------------|---------|------------------|--------------------------------------------|
| `eagle.seata.enabled`          | boolean | `true`           | 总开关                                        |
| `eagle.seata.application-id`   | String  | —                | Seata 应用 ID（建议同 `spring.application.name`） |
| `eagle.seata.tx-service-group` | String  | `eagle_tx_group` | 事务分组                                       |

⚠️ Seata 完整配置（注册中心 / 配置中心 / 服务地址等）走 `seata.*` 原生前缀，starter 不重复包装。

## 常见错误

- ❌ 参与方也加 `@GlobalTransactional` → ✅ 仅发起方
- ❌ 不写 `timeoutMills` → ✅ 显式设置（默认偏长）
- ❌ try-catch 吞掉异常 → ✅ 异常必须上抛触发回滚
- ❌ TCC 三阶段方法非幂等 → ✅ 用 **`TccIdempotencyHelper.runOnce`**
- ❌ 参与方不建 `undo_log` → ✅ AT 模式必备（每个参与方数据库都要）
- ❌ 配置混 `eagle.seata.tx-service-group` 与 `seata.tx-service-group` → ✅ 用 `eagle.seata.*`，starter 自动桥接

## 关联规则

- `.claude/rules/16-transaction-distributed.md` — 选型决策
- `.claude/rules/11-feign.md` — XID 自动透传
- `.claude/rules/08-concurrency.md`
