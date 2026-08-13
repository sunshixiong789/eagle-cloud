# Changelog

本文件记录 `eagle-cloud` Claude Code Plugin 的版本变更。
遵循 [Keep a Changelog](https://keepachangelog.com/) 格式与 [SemVer](https://semver.org/) 版本规范。

## [Unreleased]

### Added — `ForbiddenException`（HTTP 403）

`eagle-common-starter` 的异常体系新增第五种：`ForbiddenException` + `ErrorCode.toForbiddenException(Object...)`。
用于「已认证但无权操作该资源」的**业务判定**（访问他人订单 / 消息 / 收款账号），
与 Spring Security 声明式拒绝的 `AccessDeniedException` 并列映射到 403，区别是本异常携带业务 `errorCode`。
在此之前这类场景只能抛 `DomainException`，对外呈现为 400，前端无法按状态码区分越权。
规范见 `../.agents/rules/03-api-error.md`。

### Fixed — 状态码语义：多类异常被兜底吞成 500

| 位置 | 症状 | 修复 |
|---|---|---|
| `GlobalExceptionHandler`（Servlet） | 缺必填请求头返回 500 而非 400；路径无映射返回 500 而非 404 | 兜底分支先识别 `ErrorResponse`（Spring 6 起所有内建 Web 异常都实现），按其 `getStatusCode()` 返回 |
| `ReactiveGlobalExceptionHandler` | 网关未匹配路由返回 500 而非 404 | 新增 `ErrorResponse` 分支，透传 `ResponseStatusException` 的状态码 |
| `ReactiveGlobalExceptionHandler` | `@PreAuthorize` 拒绝返回 500 而非 403 | `AccessDeniedException` 判定由「类名精确相等」改为沿父类上溯——实际抛的是子类 `AuthorizationDeniedException` |
| `ReactiveGlobalExceptionHandler` | order 由 `HIGHEST_PRECEDENCE` 降为 `HIGHEST_PRECEDENCE + 10` | 让出最高优先级给应用自己的基础设施级处理器（网关的 502/503/504 映射此前被完全遮蔽，是死代码） |

根因是 `@ExceptionHandler(Exception.class)` 的优先级高于 Spring 的 `DefaultHandlerExceptionResolver`，
写了兜底 advice 就必须自己处理内建 Web 异常，否则所有本该 4xx 的框架异常统一变 500。

### Removed — ⚠️ 破坏性变更：`eagle-audit-log-starter` 移除多租户字段

随 `eagle-tenant-starter` 下线，审计日志的租户维度已无写入方，一并移除：

| 位置 | 移除内容 |
|---|---|
| `AuditLogRecord` | `tenantId` 字段、`tenant_id` 列、`idx_audit_log_tenant` 索引 |
| `AuditLogEntry` | `tenantId` 字段 |
| `AuditLogUserProvider` | `getCurrentTenantId()` 默认方法 |
| `AuditLogQueryRequest` | `tenantId` 查询条件 |
| `AuditLogResponse` | `tenantId` 组件（**record 组件 15 → 14，按位置构造的调用方需同步改**） |
| `AuditLogSpecification` | `tenantIdEquals(String)` |

**存量库需手工执行**（JPA `ddl-auto=update` 只加不删，不会自动清理）：

```sql
ALTER TABLE eagle_audit_log DROP INDEX idx_audit_log_tenant;
ALTER TABLE eagle_audit_log DROP COLUMN tenant_id;
```

生产是 `ddl-auto=validate`：**必须先执行上面的 DDL 再发布新版本**，否则实体与表结构不匹配会启动失败。

### Removed — `sys_file` 移除 tenant_id（system-service）

文件模块的租户字段同样已无来源（恒为配置里的 `"default"`），一并移除：

| 位置 | 移除内容 |
|---|---|
| `FileMetadata` | `tenantId` 字段、`tenant_id` 列、`idx_file_tenant_uploader` 索引 |
| `FileMetadata.create(...)` | 首个 `tenantId` 参数（8 → 7 个） |
| `FileStorageProperties` | `defaultTenantId` |
| `application.yml` | `eagle.file.default-tenant-id` |

**objectKey 路径规则同步变更**：`{tenant}/{uploadedBy}/{yyyy/MM/dd}/{uuid}.{ext}`
→ `{uploadedBy}/{yyyy/MM/dd}/{uuid}.{ext}`。该功能尚未上线、无存量对象，不需要迁移；
若已有存量数据则**不可直接套用本次变更**。

```sql
ALTER TABLE sys_file DROP INDEX idx_file_tenant_uploader;
ALTER TABLE sys_file DROP COLUMN tenant_id;
```

### Changed — 规则与 skill 同步 starter 移除

- 9 个 starter 移出构建（`tenant` / `rocketmq`→`amqp` / `dynamic-datasource` / `elasticsearch` /
  `excel` / `notification` / `seata` / `sentinel` / `ai`），对应 8 个 skill 目录已删除
- `../.agents/rules/` 全量同步：多租户、分布式事务、限流、注册中心（Nacos→Consul）、错误码号段
- 新增 `../.agents/rules/08-quality.md`（内聚 / 耦合 / 可维护性判据）
- `interfaces/dto/` 全量 record 化（45 个 `@Data` class → 0）
- `check-java-conventions.sh` 新增拦截：DTO 用 `@Data`、`eagle.xxx.enabled` 总开关、引用已移除能力
- `verify-rules.sh` 修复配置键校验假阳性（仅比 prefix 会让 `eagle.{prefix}.*` 全部误通过）

## [1.0.0] — 2026-04-30

### Added — 初始版本

#### Rules（28 份开发规范）

| 文件                              | 说明                                                  |
|---------------------------------|-----------------------------------------------------|
| `01-naming.md`                  | 命名约定（DDD / Modulith / ErrorCode）                    |
| `02-code-style.md`              | Google Java Style + Lombok + JSpecify `@NullMarked` |
| `03-architecture.md`            | DDD 分层、Port/Adapter、聚合根创建型事件                        |
| `04-modulith.md`                | `@ApplicationModule` / `@NamedInterface` 边界治理       |
| `05-api.md`                     | RESTful URL、`@PreAuthorize`、CORS、响应格式               |
| `06-database.md`                | JPA 实体、跨聚合 ID 引用、索引、CQRS 投影                         |
| `07-exception.md`               | `AppException` 体系、`ErrorCode` 工厂方法                  |
| `08-concurrency.md`             | 事务、领域事件 `@Async + AFTER_COMMIT`、缓存失效                |
| `09-testing.md`                 | JUnit 5 + Mockito、AAA、命名、覆盖要求                       |
| `10-starter.md`                 | `@AutoConfiguration` + Properties + imports         |
| `11-feign.md`                   | HTTP Service 客户端位置、错误处理、分页参数                        |
| `12-security.md`                | OAuth2 / JWT、密码、敏感字段脱敏、审计                           |
| `13-logging.md`                 | SLF4J 占位符、MDC、核心操作埋点 INFO/DEBUG 选择                  |
| `14-cache.md`                   | Redis+Caffeine、Key 命名、击穿/穿透/雪崩防护                    |
| `15-messaging.md`               | RocketMQ Topic、幂等、死信、事务消息                           |
| `16-transaction-distributed.md` | Seata AT/TCC 选型、本地消息表                               |
| `17-tenant-permission.md`       | 多租户隔离（COLUMN/DATABASE）、行级数据权限                       |
| `18-openapi.md`                 | SpringDoc 注解、版本、错误码文档化                              |
| `19-config.md`                  | Properties、Nacos、profile、Jasypt 加密                  |
| `20-i18n.md`                    | messages 组织、key 规则                                  |
| `22-git.md`                     | 分支模型、Conventional Commits、Tag                       |
| `23-performance.md`             | N+1、慢 SQL、连接池、Async 池                               |
| `24-deployment.md`              | Dockerfile、K8s、健康检查、优雅停机                            |
| `25-review-checklist.md`        | PR 前完整自检清单                                          |
| `26-file-storage.md`            | MinIO Bucket、Key 设计、上传校验                            |
| `27-scheduling.md`              | XXL-JOB 路由、分片、幂等                                    |
| `28-migration.md`               | Flyway 命名、不可变、回滚                                    |
| `30-dependency.md`              | Gradle 范围、BOM、CVE                                   |

#### Commands（5 个）

- `/check-arch` — Modulith 架构验证 + 模块测试 + 全量构建一键检查
- `/new-module` — 按 DDD 模板创建新业务模块
- `/new-aggregate` — 创建聚合根全栈骨架
- `/new-starter` — 按 Spring Boot 4 模板创建新 starter
- `/add-error-code` — 在 ErrorCode 枚举追加常量并同步 i18n 三语翻译

#### Skills（22 个 starter）

`eagle-common` / `eagle-data-jpa` / `eagle-mybatis` / `eagle-dynamic-datasource`
/ `eagle-elasticsearch` / `eagle-id-generator` / `eagle-idempotency` / `eagle-redis`
/ `eagle-rocketmq` / `eagle-resource-server` / `eagle-feign-client` / `eagle-tracing`
/ `eagle-tenant` / `eagle-row-security` / `eagle-openapi` / `eagle-oss-minio`
/ `eagle-notification` / `eagle-payment` / `eagle-scheduler` / `eagle-seata`
/ `eagle-sentinel` / `eagle-websocket`

每个 skill 含精确的英文 frontmatter description，便于 AI 在编码时按场景自动加载。

### Notes — 关键实现细节

所有 USAGE/SKILL 文档**完全基于源码精确撰写**，避免 AI 编造 API。已验证：

- 22 份 USAGE.md 全部对照实际 `*Properties.java` 与公共 API 类
- 28 份规则文件中受 USAGE 偏差污染的章节已修正：
    - `getCurrentTenantId()` → `getTenantId()`
    - `getWithLock(...)` → `getWithMutex(key, ttl, loader, type)`
    - `@RocketMQMessageListener` → `extends AbstractRocketMqListener<T>`
    - `MessageExt` → `MessageView`
    - `publishOrderly` → `publishOrdered`
    - 审计字段 `createdAt/updatedAt` → `createTime/updateTime`
    - `eagleTaskExecutor` → `taskExecutor`
    - `DataScope.DEPT_ONLY/SELF_ONLY` → `DEPT/SELF`
    - `@DataPermission(type, deptColumn, creatorColumn)` → `(deptField, userField)`

### CI

- GitHub Actions：`.github/workflows/plugin-sync-check.yml`
- GitLab CI：`.gitlab-ci.yml`
- Gitee Workflow：`.gitee/workflows/plugin-sync-check.yml`

### 支持的 Git 服务（接入方式 A — 私有 Marketplace）

- **Gitee**（国内推荐，访问速度优）
- **GitHub** / GitHub Enterprise
- **GitLab**（自建 / SaaS）
- **Gitea**（自托管）

业务项目接入只需更换 `marketplaces.url`，plugin 内容完全相同。

校验内容：

1. `sync.sh` 执行后 `claude-plugin/` 与源一致（防漏同步）
2. `plugin.json` / `marketplace.json` 必备字段齐全
3. 两份元数据 version 一致
4. 所有 SKILL.md 含 frontmatter
5. USAGE.md 与 SKILL.md 数量匹配（防新增 starter 漏注册）

---

## 版本规则（SemVer）

| 变更类型                                    | 版本位              | 示例    |
|-----------------------------------------|------------------|-------|
| 修订 USAGE 错别字、补充示例、修复说明                  | **Patch**（1.0.x） | 1.0.1 |
| 新增 starter / skill / command / rule     | **Minor**（1.x.0） | 1.1.0 |
| 删除/重命名 skill；规则原则颠覆性变更；CLAUDE.md 接入方式变更 | **Major**（x.0.0） | 2.0.0 |

业务项目应锁定版本（Git tag 或 marketplace.json 中显式 `version`），避免主干变更影响。

## 升级指引模板（每次发版填充）

```
### [X.Y.Z] — YYYY-MM-DD

#### Added
- 新增的 starter / skill / command / rule

#### Changed
- 不破坏接入方式的修改（如 USAGE 描述更精确）

#### Fixed
- 修复的错误描述、错别字、API 偏差

#### Deprecated
- 标记为废弃但仍保留的内容

#### Removed
- 移除的内容（major 版本必须列）

#### Migration Guide（major 版本必填）
- 如何从旧版本升级到本版本
```
