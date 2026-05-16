---
name: eagle-mybatis
description: Use when working with MyBatis-Plus in eagle-cloud projects — IEagleService/EagleServiceImpl base classes, BaseMapperPlus, EaglePageQuery/EaglePageResult unified pagination, slow SQL interceptor
---

# eagle-mybatis-starter — MyBatis-Plus 增强（通用 CRUD + 慢 SQL + 自动审计）

## 何时使用

- 业务模块使用 MyBatis-Plus（与 JPA 二选一）
- 需要通用 Service / Mapper 基类（`IEagleService` / `BaseMapperPlus`）
- 统一分页响应 `EaglePageResult`
- 慢 SQL 拦截 + 自动审计字段填充

## 何时不要使用

- 已用 JPA 的模块（不要混用）

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
  optimistic-locker-enabled: true
  performance-enabled: false        # 仅开发期开启
  slow-sql-millis: 1000
```

## 核心 API

| 类 / 接口                      | 主要方法                                                                                                                                            |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `BaseMapperPlus<T>`         | 继承 MP `BaseMapper<T>`，扩展 `selectBatchByIds(ids)`                                                                                                |
| `IEagleService<T>`          | 继承 MP `IService<T>`，扩展：`pageQuery(EaglePageQuery, Wrapper<T>)` / `getByIdOrThrow(id, errorMsg)` / `saveOrUpdateBatchOptimized(list, batchSize)` |
| `EagleServiceImpl<M, T>`    | `IEagleService` 默认实现                                                                                                                            |
| `EaglePageQuery`            | `pageNum`(1) / `pageSize`(20，最大 200) / `orderBy` / `orderDirection`("desc") / `toPage()`                                                        |
| `EaglePageResult<T>`        | `records / total / pageNum / pageSize / totalPages / hasNext / hasPrevious`，`static of(IPage)` + `convert(Function)`                            |
| `EagleMetaObjectHandler`    | 自动填充 `createBy / createTime / updateBy / updateTime`                                                                                            |
| `MybatisSlowSqlInterceptor` | 慢 SQL 拦截器，超 `slow-sql-millis` 输出 WARN                                                                                                           |
| `QueryHelper`               | 动态查询条件构造工具                                                                                                                                      |

## 最小示例

```java
// Mapper
public interface OrderMapper extends BaseMapperPlus<Order> {
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    Order findByOrderNo(String orderNo);
}

// Service 接口
public interface OrderService extends IEagleService<Order> {
    Order createOrder(CreateOrderRequest request);
}

// Service 实现
@Service
public class OrderServiceImpl extends EagleServiceImpl<OrderMapper, Order>
        implements OrderService {

    @Override
    public Order createOrder(CreateOrderRequest request) {
        Order order = Order.from(request);
        save(order);                          // 自动填充 createBy / createTime
        return order;
    }
}

// 分页查询（统一格式）
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public EaglePageResult<OrderResponse> list(@Valid QueryOrderRequest query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(query.getStatus() != null, Order::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), Order::getOrderNo, query.getKeyword());

        EaglePageResult<Order> page = orderService.pageQuery(query, wrapper);
        return page.convert(OrderResponse::from);
    }
}

// 请求 DTO 继承 EaglePageQuery
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryOrderRequest extends EaglePageQuery {
    private String keyword;
    private OrderStatus status;
}

// ID 不存在自动抛 NotFoundException
Order order = orderService.getByIdOrThrow(orderId, "订单不存在");

// 批量保存（自动分批）
orderService.

saveOrUpdateBatchOptimized(orders, 500);
```

## 配置项

| key                                       | 类型      | 默认      | 说明            |
|-------------------------------------------|---------|---------|---------------|
| `eagle.mybatis.optimistic-locker-enabled` | boolean | `true`  | 启用乐观锁插件       |
| `eagle.mybatis.performance-enabled`       | boolean | `false` | SQL 性能分析（仅开发） |
| `eagle.mybatis.slow-sql-millis`           | long    | `1000`  | 慢 SQL 阈值      |

## 常见错误

- ❌ 同模块混用 JPA + MyBatis → ✅ 一种 ORM
- ❌ Service 实现继承 `ServiceImpl` → ✅ 继承 `EagleServiceImpl<M, T>`，自动获得 starter 扩展
- ❌ 用 `getById` 不判 null → ✅ 用 **`getByIdOrThrow(id, msg)`**，自动抛 404
- ❌ 自定义分页响应格式 → ✅ 统一用 `EaglePageResult.convert(...)`
- ❌ 业务字段写 `created_by = ?` → ✅ `EagleMetaObjectHandler` 自动填充
- ❌ 拼接 SQL 字符串 → ✅ `LambdaQueryWrapper` / `QueryHelper`

## 关联规则

- `.claude/rules/06-database.md`
- `.claude/rules/23-performance.md` — 慢 SQL
- `.claude/rules/05-api.md` — 分页响应
