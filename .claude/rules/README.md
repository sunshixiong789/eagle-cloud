# Project Rules Index

通用 DDD 开发规范，适用于模块化单体和微服务架构。按需加载对应规范文件：

| 文件 | 规范 | 适用场景 |
|------|------|----------|
| [01-naming.md](01-naming.md) | 命名规范 | 新建类、方法、变量、包、ErrorCode 枚举时 |
| [02-architecture.md](02-architecture.md) | DDD 分层架构规范 | 新增模块、跨域协作、聚合根设计时 |
| [03-api.md](03-api.md) | RESTful 接口规范 | 新增/修改 Controller 接口时 |
| [04-logging.md](04-logging.md) | 日志规范 | 添加日志时 |
| [05-security.md](05-security.md) | 安全规范 | 权限控制、CORS、认证配置时 |
| [06-concurrency.md](06-concurrency.md) | 并发规范 | 事务、缓存失效、@Async、领域事件时 |
| [07-testing.md](07-testing.md) | 单元测试规范 | 编写测试、PR 前架构验证时 |
| [08-code-style.md](08-code-style.md) | 代码规范（Google Java Style）| Code Review 时 |
| [09-exception.md](09-exception.md) | 异常处理规范 | 抛出/捕获异常、新增 ErrorCode 时 |
| [10-database.md](10-database.md) | 数据库规范 | 实体类、枚举存储、聚合引用、索引时 |
| [11-configuration.md](11-configuration.md) | 配置注入规范 | 新增配置属性时 |
| [12-modulith.md](12-modulith.md) | 模块边界治理规范 | 新增模块、调试架构测试失败时 |
