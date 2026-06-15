# 多租户与数据权限规范

适用 `eagle-tenant-starter` 与 `eagle-row-security-starter`。规则保留 API、注解位置和隔离红线。

## 多租户策略

- 默认使用 COLUMN 模式：业务表包含 `tenant_id`。
- DATABASE 模式只用于强隔离场景，并通过 starter 路由数据源。
- 应用层禁止手动拼接 `WHERE tenant_id = ?`；租户过滤由 starter 注入。

## 租户上下文

- API：`TenantContextHolder.getTenantId()`、`setTenantId()`、`clear()`。
- 网关 / 资源服务器 / Feign / WebClient / MQ / Async 都必须透传租户上下文。
- 异步任务使用装饰过的 `TaskExecutor`，任务结束后清理 ThreadLocal。

## 实体

- 多租户业务实体必须有 `tenantId` 字段，对应列 `tenant_id`，创建后不可更新。
- 多租户表索引以 `tenant_id` 为前导列。
- `@TenantFilter` 标在 Service / Repository 上，不标在 `@Entity` 上。
- 跨租户共享配置、字典等需明确声明为非租户数据。

## 跨租户操作

- 必须显式提供 reason，并写审计日志。
- 使用受控 API 临时切换租户上下文，`try/finally` 恢复。
- 禁止普通业务路径隐式跨租户查询或批量操作。

## DATABASE 模式

- 使用 starter 的数据源路由能力，不在业务代码手动选择 DataSource。
- 路由 key 来自可信租户上下文，不信任前端传值。
- 迁移、备份、监控、连接池容量必须按租户规模评估。

## 行级数据权限

- 使用 `@DataPermission(deptField, userField)` 声明数据范围。
- `DataScope` 固定为：`ALL / SELF / DEPT / DEPT_AND_CHILD / CUSTOM`。
- 业务方实现 `DataPermissionProvider`，提供当前用户、部门、角色和自定义范围。
- 数据权限与租户隔离同时生效：先租户隔离，再行级过滤。

## 测试

- 单元测试覆盖租户上下文设置/清理、跨租户拒绝、数据权限范围、异步透传。
- 不连接真实数据库；使用 repository mock 或 SQL 片段断言。

## 禁止清单

- 手写 SQL 拼接租户或数据权限条件。
- 信任前端传入的 tenantId、deptId、userId 作为权限依据。
- `@TenantFilter` 标在 Entity。
- ThreadLocal 不清理。
- 跨租户操作无审计。
