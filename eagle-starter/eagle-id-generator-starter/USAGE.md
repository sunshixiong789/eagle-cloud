# eagle-id-generator-starter — 分布式 ID 生成（雪花 / 号段 / TSID / NanoID / 业务单号）

## 何时使用

- 聚合根主键（替代数据库自增 ID，方便分库分表）
- 业务单号（订单号、流水号、合同号）
- 全局唯一标识（消息 ID、请求 ID）

## 何时不要使用

- 单体应用 + 单库（`@GeneratedValue(IDENTITY)` 即够）
- 仅需可读简单 ID（用 UUID）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-id-generator-starter')
```

```yaml
eagle.id-generator:
  enabled: true
  default-type: snowflake             # snowflake / segment / tsid / uuid / nanoid
  snowflake:
    worker-id: ${WORKER_ID:1}         # 0–31，集群内唯一
    datacenter-id: ${DC_ID:1}         # 0–31
  segment:
    business-key: order               # 号段业务标识
    step: 1000                        # 单次申请号段大小
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `IdGenerator` | 生成器接口（`nextId()` / `nextIdStr()`） |
| `SnowflakeIdGenerator` | 雪花算法（64-bit Long） |
| `SegmentIdGenerator` | 号段模式（依赖 DB 单调递增） |
| `TsidIdGenerator` | TSID（time-sorted）|
| `UuidIdGenerator` | UUID |
| `NanoIdGenerator` | NanoID（短随机） |
| `OrderNoGenerator` | 业务单号生成器（前缀 + 时间 + 序号） |
| `IdGeneratorFacade` | 统一门面（按 type 分发） |
| `IdGeneratorUtil` | 静态工具（无依赖注入即可用） |

## 最小示例

```java
// 注入使用
@RequiredArgsConstructor
@Service
public class OrderApplicationService {
    private final IdGeneratorFacade idGenerator;
    private final OrderNoGenerator orderNoGen;

    public Order create() {
        long id = idGenerator.nextId();                       // 雪花 ID
        String orderNo = orderNoGen.generate("ORD");          // ORD202604301035001
        return Order.create(id, orderNo);
    }
}

// 静态工具（特殊场景，工具类内）
long id = IdGeneratorUtil.snowflake();
String orderNo = IdGeneratorUtil.orderNo("ORD");

// 实体主键（替代 IDENTITY）
@Entity
public class Order {
    @Id
    @GenericGenerator(name = "snowflake", strategy = "com.eagle.idgenerator.SnowflakeStrategy")
    @GeneratedValue(generator = "snowflake")
    private Long id;
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.id-generator.enabled` | boolean | `true` | 总开关 |
| `eagle.id-generator.default-type` | String | `snowflake` | 默认类型 |
| `eagle.id-generator.snowflake.worker-id` | long | `1` | 集群内唯一 |
| `eagle.id-generator.snowflake.datacenter-id` | long | `1` | 数据中心 |
| `eagle.id-generator.segment.business-key` | String | — | 号段业务标识 |
| `eagle.id-generator.segment.step` | int | `1000` | 号段步长 |

## 常见错误

- ❌ 多实例 `worker-id` 重复 → ✅ K8s 用 StatefulSet 序号 / Nacos 注册分配
- ❌ 时钟回拨未处理 → ✅ Snowflake 实现自带回拨保护
- ❌ 业务单号用 `UUID` → ✅ 用 `OrderNoGenerator`（含前缀 + 时间，可读）
- ❌ 高并发 `IdGeneratorUtil.snowflake()` → ✅ 注入 Facade（同步开销小，但更规范）
- ❌ 号段表无单调约束 → ✅ DB schema 建表加唯一索引

## 关联规则

- `.claude/rules/06-database.md` — 主键策略
- `.claude/rules/24-deployment.md` — `WORKER_ID` 容器化注入
