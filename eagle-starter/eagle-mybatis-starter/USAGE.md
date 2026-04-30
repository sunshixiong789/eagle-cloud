# eagle-mybatis-starter — MyBatis-Plus 增强（自动审计 + 慢 SQL + 通用 CRUD）

## 何时使用

- 业务模块使用 MyBatis-Plus 数据访问（与 JPA 二选一，按模块定）
- 需要通用 CRUD（`IEagleService` / `BaseMapperPlus`）
- 慢 SQL 拦截监控
- 自动填充审计字段

## 何时不要使用

- 已用 JPA 的模块（不要混用，统一一种 ORM）
- 简单只读查询（直接 JdbcTemplate 即可）

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-mybatis-starter')
runtimeOnly 'mysql:mysql-connector-j'
```

```yaml
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-not-delete-value: 0
      logic-delete-value: 1

eagle.mybatis:
  enabled: true
  slow-sql-millis: 1000               # 慢 SQL 阈值（毫秒）
  auto-fill-enabled: true             # 自动填充 createdBy/createdAt/...
```

## 核心 API

| 类 / 接口 | 用途 |
|---|---|
| `BaseMapperPlus<T>` | Mapper 基类（继承 MyBatis-Plus `BaseMapper` + 扩展批量方法） |
| `IEagleService<T>` | Service 基类接口（封装通用 CRUD） |
| `EagleServiceImpl<M, T>` | Service 默认实现 |
| `EaglePageQuery` | 分页入参（page / size / orderBy / orderDir） |
| `EaglePageResult<T>` | 分页响应（统一格式） |
| `EagleMetaObjectHandler` | 自动填充处理器 |
| `MybatisSlowSqlInterceptor` | 慢 SQL 拦截器（自动记录） |
| `QueryHelper` | 动态查询条件构造工具 |

## 最小示例

```java
// Mapper
public interface OrderMapper extends BaseMapperPlus<Order> {
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    Order findByOrderNo(String orderNo);
}

// Service
public interface OrderService extends IEagleService<Order> {
    Order createOrder(CreateOrderRequest request);
}

@Service
public class OrderServiceImpl extends EagleServiceImpl<OrderMapper, Order>
    implements OrderService {

    @Override
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.from(request);
        save(order);   // 自动填充 createdBy/createdAt
        return order;
    }
}

// 分页查询
@GetMapping
public EaglePageResult<OrderResponse> list(EaglePageQuery query) {
    Page<Order> page = orderService.page(query.toPage(),
        QueryHelper.eq("status", query.getStatus())
                   .orderByDesc("created_at"));
    return EaglePageResult.from(page, OrderResponse::from);
}
```

## 配置项

| key | 类型 | 默认 | 说明 |
|---|---|---|---|
| `eagle.mybatis.enabled` | boolean | `true` | 总开关 |
| `eagle.mybatis.slow-sql-millis` | long | `1000` | 慢 SQL 阈值 |
| `eagle.mybatis.auto-fill-enabled` | boolean | `true` | 启用审计字段填充 |

## 常见错误

- ❌ 同模块混用 JPA + MyBatis → ✅ 一个模块只用一种 ORM
- ❌ 自己写分页 SQL（`LIMIT ?, ?`）→ ✅ 用 MP 的 `Page<T>` 或 `EaglePageQuery`
- ❌ 不开启逻辑删除 → ✅ 配置 `logic-delete-field: deleted`
- ❌ 业务字段写 `created_by = ?` → ✅ 让 `EagleMetaObjectHandler` 自动填充
- ❌ 拼接 SQL 字符串 → ✅ 用 `QueryHelper` / `LambdaQueryWrapper`

## 关联规则

- `.claude/rules/06-database.md` — 实体规范、索引
- `.claude/rules/23-performance.md` — 慢 SQL 阈值
- `.claude/rules/05-api.md` — 分页响应格式
