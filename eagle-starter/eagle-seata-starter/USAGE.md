# eagle-seata-starter — Seata 分布式事务（AT / TCC）

## 何时使用

- 跨服务**强一致性**写操作（金融、库存、扣款）
- 多个数据库 / 多个服务的事务必须原子（成功一起成功，失败一起回滚）
- 需要 XID 跨服务自动透传

## 何时不要使用

- 最终一致性即可（用本地消息表 + RocketMQ 事务消息，详见 `16-transaction-distributed.md`）
- 单服务多数据源（用 JTA）
- 高并发热点行更新（AT 全局锁会成为瓶颈）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-seata-starter')
```

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: eagle_tx_group
  service:
    vgroup-mapping.eagle_tx_group: default
    grouplist.default: ${SEATA_SERVER:127.0.0.1:8091}
  registry:
    type: nacos
    nacos:
      server-addr: ${NACOS_SERVER:127.0.0.1:8848}
      namespace: ${NACOS_NAMESPACE:dev}

eagle.seata:
  enabled: true
  default-timeout-ms: 60000
```

参与方需要建 `undo_log` 表（AT 模式）。

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `@GlobalTransactional` | Seata 注解：发起方加，开启全局事务 |
| `GlobalTransactionTemplate` | 编程式全局事务模板 |
| `TransactionCallback<T>` | 模板回调接口 |
| `TccAction` | TCC 抽象（业务实现 try / commit / rollback） |
| `TccIdempotencyHelper` | TCC 幂等 / 防悬挂 / 防空回滚帮助类 |
| `SeataUtil` | 工具：`getCurrentXid` / `bind` / `unbind` |
| `SeataEnvironmentPostProcessor` | 启动时自动配置 |

## 最小示例

### AT 模式（最常用）

```java
// ✅ 仅发起方加 @GlobalTransactional，参与方用普通 @Transactional
@Service
@RequiredArgsConstructor
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final InventoryFeignClient inventoryClient;
    private final WalletFeignClient walletClient;

    @GlobalTransactional(rollbackFor = Exception.class, timeoutMills = 60000)
    public void placeOrder(CreateOrderRequest req) {
        Order order = orderRepository.save(Order.create(req));
        inventoryClient.lockStock(req.getItems());        // XID 自动透传
        walletClient.deduct(req.getUserId(), req.getAmount());
    }
}

// 参与方
@Service
@Transactional(rollbackFor = Exception.class)
public class InventoryService {
    public void lockStock(List<Item> items) {
        // 普通本地事务，Seata 自动加入全局事务
    }
}
```

### TCC 模式（高并发场景）

```java
@LocalTCC
public interface StockTccAction extends TccAction {

    @TwoPhaseBusinessAction(name = "stockReserve",
        commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepare(BusinessActionContext ctx,
        @BusinessActionContextParameter("productId") Long productId,
        @BusinessActionContextParameter("qty") int qty);

    boolean commit(BusinessActionContext ctx);
    boolean rollback(BusinessActionContext ctx);
}

@Component
@RequiredArgsConstructor
public class StockTccActionImpl implements StockTccAction {
    private final TccIdempotencyHelper idempotency;

    @Override
    public boolean prepare(BusinessActionContext ctx, Long productId, int qty) {
        return idempotency.runOnce(ctx, () -> {
            stockRepository.freeze(productId, qty);
            return true;
        });
    }

    @Override
    public boolean commit(BusinessActionContext ctx) {
        return idempotency.runOnce(ctx, () -> {
            stockRepository.deductFrozen(ctx.getActionContext("productId"));
            return true;
        });
    }

    @Override
    public boolean rollback(BusinessActionContext ctx) {
        return idempotency.runOnce(ctx, () -> {
            stockRepository.unfreeze(ctx.getActionContext("productId"));
            return true;
        });
    }
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.seata.enabled` | boolean | `true` | 总开关 |
| `eagle.seata.default-timeout-ms` | int | `60000` | 默认全局事务超时 |
| `seata.tx-service-group` | String | — | 事务分组 |
| `seata.registry.type` | enum | `nacos` | TC 注册中心 |

## 常见错误

- ❌ 参与方也加 `@GlobalTransactional` → ✅ 仅发起方
- ❌ 不写 `timeoutMills` → ✅ 显式设置（默认 60s 偏长）
- ❌ 高并发热点行直接 AT → ✅ 改用 TCC 或本地消息表
- ❌ TCC 三阶段方法非幂等 → ✅ 用 `TccIdempotencyHelper.runOnce`
- ❌ try-catch 吞掉异常 → ✅ 异常必须上抛触发回滚
- ❌ 参与方不建 `undo_log` 表 → ✅ AT 模式必备

## 关联规则

- `.claude/rules/16-transaction-distributed.md` — 选型决策树 / 三大模式对比
- `.claude/rules/11-feign.md` — XID 自动透传（已内置）
- `.claude/rules/08-concurrency.md` — `@Transactional` 边界
