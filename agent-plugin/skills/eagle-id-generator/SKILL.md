---
name: eagle-id-generator
description: Use when generating distributed IDs in eagle-cloud projects — Snowflake, UUID v7, TSID, NanoId, business order numbers (orderNo/payNo/refundNo) via IdGeneratorFacade or IdGeneratorUtil static helpers
---

# eagle-id-generator-starter — Snowflake / UUID v7 / TSID / NanoId / 业务订单号

## 何时使用

- 聚合根主键（替代数据库自增 ID，便于分库分表）
- 业务单号（订单 / 支付 / 退款流水）
- 全局唯一短码（邀请码、分享码）

## 何时不要使用

- 单库自增 ID 已够用 → 保持 `@GeneratedValue(IDENTITY)`

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-id-generator-starter')
```

```yaml
eagle.id-generator:
  enabled: true
  type: SNOWFLAKE                # 默认 IdGenerator 实现：SNOWFLAKE / UUID / TSID
  worker-id: ${WORKER_ID:1}      # Snowflake workerId，0–31，集群内唯一
  datacenter-id: ${DC_ID:1}      # Snowflake dataCenterId，0–31
  enable-facade: true            # 同时注册 IdGeneratorFacade 和 OrderNoGenerator
  tsid:
    node-id: 1                   # TSID 节点 ID（依 nodeBits 决定）
    node-bits: 10                # 8/10/12 → 256/1024/4096 节点
  nano-id:
    default-size: 21             # 默认 21 字符（≈ UUID v4 碰撞概率）
```

## 核心 API

### 默认 `IdGenerator` Bean

由 `eagle.id-generator.type` 决定具体实现（SNOWFLAKE / UUID / TSID）：

```java
public interface IdGenerator {
    long nextId();

    String nextIdStr();
}
```

### `IdGeneratorFacade`（注入使用，推荐）

```java
// 默认实现
long pk = facade.nextId();
long
String s = facade.nextIdStr();

// Snowflake
long sf = facade.snowflakeId();

// UUID v7（time-ordered Unix Epoch）
long uuidLong = facade.uuidLong();      // 高 64 位
String uuidStr = facade.uuid();         // 32 位无连字符
UUID uuid = facade.uuidV7();            // 36 位标准格式

// TSID
long tsidLong = facade.tsidLong();
String tsidStr = facade.tsidStr();      // 13 位 Crockford Base32

// NanoId
String n = facade.nanoId();             // 21 字符
String n8 = facade.nanoId(8);           // 8 字符短码

// 业务单号（前缀 + 时间戳 + 随机）
String orderNo = facade.orderNo("ORD"); // ORD20260430123456789
String payNo = facade.payNo();          // 前缀 PAY
String refundNo = facade.refundNo();    // 前缀 RFD
```

### `IdGeneratorUtil`（静态，无注入场景）

```java
long id = IdGeneratorUtil.nextId();
String uuid = IdGeneratorUtil.uuid();
String tsid = IdGeneratorUtil.tsidStr();
String code = IdGeneratorUtil.nanoId(8);
```

⚠️ 静态工具依赖 Spring 启动后初始化（`InitializingBean`），不要在 Spring 加载完成前调用。

## 最小示例

```java

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final IdGeneratorFacade idFacade;
    private final OrderRepository orderRepository;

    public Order create(CreateOrderRequest req) {
        String orderNo = idFacade.orderNo("ORD");
        Order order = Order.create(orderNo);
        return orderRepository.save(order);   // ID 由 JPA IDENTITY 生成（DB 端）
    }

    public PaymentRecord pay(Order order) {
        String payNo = idFacade.payNo();
        return paymentRepository.save(PaymentRecord.of(order, payNo));
    }
}

// 邀请码（短 NanoId）
public String generateInviteCode() {
    return idFacade.nanoId(8);   // 例 V1StGXR8
}
```

## 配置项

| key                                       | 类型      | 默认          | 说明                                       |
|-------------------------------------------|---------|-------------|------------------------------------------|
| `eagle.id-generator.enabled`              | boolean | `true`      | 总开关                                      |
| `eagle.id-generator.type`                 | enum    | `SNOWFLAKE` | 默认 IdGenerator：`SNOWFLAKE / UUID / TSID` |
| `eagle.id-generator.worker-id`            | long    | `1`         | Snowflake workerId（集群唯一）                 |
| `eagle.id-generator.datacenter-id`        | long    | `1`         | Snowflake dataCenterId                   |
| `eagle.id-generator.sequence`             | long    | `0`         | 序列起始（兼容字段）                               |
| `eagle.id-generator.enable-facade`        | boolean | `true`      | 同时注册 Facade + OrderNoGenerator           |
| `eagle.id-generator.tsid.node-id`         | int     | `1`         | TSID 节点 ID                               |
| `eagle.id-generator.tsid.node-bits`       | int     | `10`        | TSID 节点位数                                |
| `eagle.id-generator.nano-id.default-size` | int     | `21`        | NanoId 默认长度                              |

## 常见错误

- ❌ 多实例 `worker-id` 重复 → ✅ K8s StatefulSet 序号 / 启动脚本注入
- ❌ 业务单号用 UUID → ✅ 用 `orderNo(prefix)`（含时间，可读）
- ❌ Spring 启动前用 `IdGeneratorUtil.xxx()` → ✅ 静态工具需等 Spring 初始化完成
- ❌ 期望默认是 UUID → ✅ 默认 **`SNOWFLAKE`**
- ❌ 把 `IdGeneratorFacade.nextId()` 用作前端可见 ID → ✅ Snowflake long 在前端可能精度丢失，用 `nextIdStr()` 或 JSON 序列化为
  String

## 关联规则

- `.claude/rules/04-data.md` — 主键策略
