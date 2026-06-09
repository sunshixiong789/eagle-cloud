# 代码分层规范（DDD 架构）

适用于 Spring Modulith 模块化单体和未来微服务拆分。规则只记录 Eagle 的架构边界和项目决策；DDD 通用概念不展开。

## 依赖方向

模块内固定为：

```text
interfaces -> application -> domain <- infrastructure
```

- `interfaces`：Controller、request/response DTO、Bean Validation。
- `application`：用例编排、事务边界、DTO/领域映射。
- `domain`：聚合根、子实体、值对象、Repository 接口、领域服务接口、事件、Port。
- `infrastructure`：JPA、远程调用、MQ、缓存、配置、安全、定时任务等适配器。

## 跨域依赖

- 出站端口（Driven Port）定义在调用方 `domain/port/`，由本模块或其他模块 `infrastructure/adapter/` 实现。
- 禁止直接依赖其他域的聚合根、Repository、领域服务实现或内部包。
- 跨域协作优先二选一：Port + Adapter，或发布方领域事件 + 订阅方处理器。
- 跨域事件定义在发布方 `domain/event/`，通过 `@NamedInterface("event")` 暴露；订阅方在 `allowedDependencies` 声明依赖。

## 模块目录

```text
{module}/
├── interfaces/
│   ├── controller/
│   └── dto/{request,response}/
├── application/
│   ├── service/
│   └── mapper/
├── domain/
│   ├── model/{aggregate,entity,valueobject,enums}/
│   ├── repository/
│   ├── service/
│   ├── event/
│   └── port/
└── infrastructure/
    ├── persistence/
    ├── adapter/
    ├── event/
    ├── service/
    ├── config/
    ├── security/
    ├── schedule/
    ├── remote/
    └── messaging/
```

## DTO 映射

项目统一使用纯 Java `@Component` Mapper，位于 `application/mapper/`。

- 方法命名 `toResponse / toDto / toDomain`；入参为 `null` 时返回 `null`。
- 字段逐行显式映射；枚举输出 String 时用 `.name()`。
- 批量转换由调用方 `stream().map(mapper::toResponse).toList()` 完成。
- Mapper 不访问 Repository / Service，不做跨聚合查询和业务判断。
- 禁止 MapStruct、ModelMapper、`BeanUtils.copyProperties` 等反射/生成式 DTO 映射。

## 聚合根

- 聚合根继承 `BaseAggregateRoot<T>`；子实体继承 `BaseEntity`。
- 聚合根通过静态工厂创建，通过业务方法修改状态，不暴露 setter。
- 子实体增删改必须通过聚合根业务方法完成。
- 跨聚合只保存 ID，不建对象引用或物理外键。

## 领域事件

- 领域事件使用 `BaseEvent` 能力，`eventId` 为时间有序 UUID。
- 聚合根内部调用 `registerEvent()` 记录事件，由事务提交后发布。
- 创建型事件需要数据库 ID 时，在聚合根 `@PostPersist` 后注册。
- 禁止应用服务手动调用 `publishEvent()` 绕过聚合根。

## CQRS

- 写模型走聚合根和 Repository。
- 列表、详情、后台查询等读模型可使用 Repository 投影接口、只读 QueryPort 或独立查询服务。
- 复杂读查询不得污染聚合根业务方法。

## 微服务拆分就绪

- 领域层不依赖基础设施实现。
- 外部系统访问通过 Port；拆分时新增 HTTP/gRPC/MQ adapter 替换本地实现。
- 跨服务契约走 DTO、OpenAPI 或集成事件 JSON 字段，不共享内部实体。
