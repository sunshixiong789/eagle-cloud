# PR 评审与提交前检查清单

本清单只列 Eagle 项目高风险项；通用代码质量由模型默认能力、IDE、CI 和专项规则覆盖。

## 必跑命令

```bash
gradle :path:to:module:test
gradle :path:to:module:test --tests "*.ModulithArchitectureTest"
gradle build
```

- 小改动跑受影响模块测试。
- Modulith 边界变更跑架构测试。
- 公共 starter、BOM、Gradle、跨模块契约变更跑 `gradle build`。

## 自检清单

### 架构

- [ ] 分层依赖符合 `interfaces -> application -> domain <- infrastructure`。
- [ ] 跨域不直接 import 对方 domain、repository 或内部包。
- [ ] 跨域协作通过 Port + Adapter、领域事件或 `@NamedInterface`。
- [ ] 新模块声明 `@ApplicationModule`，对外包声明 `@NamedInterface`。

### API / OpenAPI

- [ ] Controller 方法权限符合 `05-api.md` / `12-security.md`。
- [ ] 请求 DTO 有 Bean Validation；响应不用领域实体直接暴露。
- [ ] DTO / Controller 注解满足 `18-openapi.md`，错误码有文档。

### 数据库 / 迁移

- [ ] 聚合间只存 ID；禁止物理 FK。
- [ ] 枚举字段使用 `EnumType.STRING`。
- [ ] 多租户表有 `tenant_id` 且索引以前导列设计。
- [ ] Flyway 文件命名、不可变和回滚说明符合 `28-migration.md`。

### 异常 / i18n / 日志

- [ ] 业务异常通过 `ErrorCode` 工厂创建。
- [ ] i18n key 三语齐全；前端可用错误码或 key 渲染。
- [ ] 关键业务节点有日志；敏感字段脱敏；异常日志保留堆栈。

### 并发 / 事件 / 消息

- [ ] 写操作事务边界清晰；只读查询使用 readOnly。
- [ ] 领域事件处理符合 `@Async + AFTER_COMMIT`，跨域处理独立事务。
- [ ] MQ Topic、契约、幂等、DLQ、告警符合 `15-messaging.md`。

### 安全 / 租户 / 权限

- [ ] 无明文密码、密钥、Token、真实凭据。
- [ ] 租户上下文、数据权限、跨租户审计符合 `17-tenant-permission.md`。
- [ ] 文件上传、CORS、CSRF、输入安全符合对应规则。

### Starter / 配置 / 依赖

- [ ] Starter 自动配置、条件装配、Properties、imports 符合 `10-starter.md`。
- [ ] 新依赖先过 BOM 和审查；依赖升级独立 PR。
- [ ] 生产配置无危险默认值，敏感配置加密或外部化。

### 前端 / UI

- [ ] API 契约、分页、鉴权、租户、上传和错误处理与后端规则一致。
- [ ] 不信任前端传入的 MIME、tenantId、权限标识或价格金额等敏感字段。
- [ ] 涉及 UI 的变更已启动页面手工验证。

## PR 描述

```markdown
## 背景

## 变更内容

## 影响范围

## 自测
- [ ] `gradle :path:to:module:test`

## 风险与回滚

## 评审重点
```
