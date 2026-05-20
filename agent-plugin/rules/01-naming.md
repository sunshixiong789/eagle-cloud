# 命名规范（Naming Conventions）

遵循 Google Java Style Guide — Naming 规范。

| 类型          | 规范               | 示例                                           |
|-------------|------------------|----------------------------------------------|
| 类 / 接口 / 枚举 | `UpperCamelCase` | `OrderApplicationService`、`PaymentErrorCode` |
| 方法 / 变量     | `lowerCamelCase` | `findByUsername`、`toNotFoundException`       |
| 常量 / 枚举值    | `CONSTANT_CASE`  | `ORDER_NOT_FOUND`、`MAX_RETRY_COUNT`          |
| 包名          | 全小写，无下划线         | `com.eagle.system.base.domain.model`         |
| 测试类         | 被测类名 + `Test`    | `OrderApplicationServiceTest`                |

## DDD 分层命名约定

| 组件            | 命名规则                                   | 示例                                            |
|---------------|----------------------------------------|-----------------------------------------------|
| 聚合根           | 名词                                     | `Order`、`Product`、`User`                      |
| 子实体           | `{Name}Entity`                         | `OrderItemEntity`、`DictItemEntity`            |
| 值对象           | 名词（描述属性组合）                             | `Money`、`Address`、`UserProfile`               |
| 领域事件          | `{聚合根}{动作}Event`                       | `OrderCreatedEvent`、`UserUpdatedEvent`        |
| 聚合根基类         | —                                      | `BaseAggregateRoot<T>`                        |
| 子实体基类         | —                                      | `BaseEntity`                                  |
| Repository 接口 | `{聚合根}Repository`                      | `OrderRepository`、`UserRepository`            |
| Repository 投影 | `{Name}Summary` / `{Name}View`         | `OrderSummary`、`UserView`                     |
| 领域服务接口        | `{Name}Service`（domain/service 包）      | `PricingService`、`InventoryValidationService` |
| 领域服务实现        | `{Name}ServiceImpl`（infrastructure 包）  | `PricingServiceImpl`                          |
| 应用服务          | `{Name}ApplicationService`             | `OrderApplicationService`                     |
| Controller    | `{Name}Controller`                     | `OrderController`                             |
| 请求 DTO        | `{Action}{Name}Request`                | `CreateOrderRequest`、`UpdateUserRequest`      |
| 响应 DTO        | `{Name}Response`                       | `OrderResponse`、`UserResponse`                |
| CQRS 投影接口     | `{Name}Summary`                        | `OrderSummary`                                |
| Mapper        | `{Name}Mapper`（纯 Java `@Component`）    | `OrderMapper`、`UserMapper`                    |
| 事件处理器         | `{Name}EventHandler`                   | `OrderEventHandler`                           |
| Properties    | `{Name}Properties`                     | `PaymentProperties`、`StorageProperties`       |
| Port 接口       | `{Name}Port` / `{Name}QueryPort`       | `PaymentPort`、`InventoryQueryPort`            |
| Adapter 实现    | `{Name}Adapter` / `{Name}QueryService` | `PaymentAdapter`                              |

## ErrorCode 命名约定

| 枚举   | 命名规则                   | 示例                                     |
|------|------------------------|----------------------------------------|
| 枚举类  | `{Domain}ErrorCode`    | `OrderErrorCode`、`PaymentErrorCode`    |
| 枚举常量 | `CONSTANT_CASE`，描述错误场景 | `ORDER_NOT_FOUND`、`INSUFFICIENT_STOCK` |

## 模块治理命名约定（Spring Modulith / 微服务）

| 文件     | 命名规则                      | 示例                                        |
|--------|---------------------------|-------------------------------------------|
| 模块声明   | 包根目录的 `package-info.java` | `auth/package-info.java`                  |
| 命名接口声明 | 子包的 `package-info.java`   | `auth/domain/port/package-info.java`      |
| 命名接口名称 | 小写短名（kebab-case）          | `"port"`、`"security"`、`"domain-services"` |
