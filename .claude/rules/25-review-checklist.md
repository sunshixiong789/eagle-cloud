# PR 评审与提交前检查清单

提交 PR 前**必须**全部通过，CI 失败的 PR 不得合并。

## PR 前必跑命令

```bash
# 1) 完整构建（含编译 / 测试 / Modulith 验证）
./gradlew clean build

# 2) Modulith 架构验证（变更涉及 system-server 时必跑）
./gradlew :eagle-base-server:eagle-system-server:test \
    --tests "*.ModulithArchitectureTest"

# 3) 仅本模块测试（开发期快速反馈）
./gradlew :path:to:module:test

# 4) 静态检查（如已配置 checkstyle / spotbugs）
./gradlew check
```

CI 流水线必跑：`clean build` + `test` + Modulith 验证。

## 自检清单（提交前逐条对照）

### 命名 / 代码风格（01-naming.md / 02-code-style.md）

- [ ] 类、方法、变量符合命名约定
- [ ] 4 空格缩进；行宽 ≤ 100；K&R 大括号
- [ ] 无通配符 import
- [ ] `public` API 有 Javadoc
- [ ] 无魔法数字（命名常量或枚举替代）
- [ ] 无注释掉的代码 / 无意义 TODO
- [ ] JPA 实体未用 `@Data` / `@Builder`
- [ ] Service / Controller 用 `@RequiredArgsConstructor`，不用 `@Autowired`
- [ ] 配置注入用 `@ConfigurationProperties`，不用 `@Value`

### 架构（03-architecture.md / 04-modulith.md）

- [ ] 跨域不直接 import 对方 domain / repository
- [ ] 跨域调用通过 Port + Adapter 或领域事件
- [ ] 新增模块有 `package-info.java` 声明 `@ApplicationModule`
- [ ] 跨域事件定义在发布方 `domain/event/` 并 `@NamedInterface("event")`
- [ ] 聚合根创建型事件在 `@PostPersist` 中注册（非应用服务手动调用）
- [ ] 跨聚合只存 ID，无 `@ManyToMany` / `@ManyToOne` 跨聚合关联

### API（05-api.md / 18-openapi.md）

- [ ] URL 用名词复数 + kebab-case；无动词
- [ ] 所有 Controller 方法有 `@PreAuthorize`
- [ ] 请求 DTO `@Valid` 校验
- [ ] CORS 生产环境无 `*` + credentials 同时开
- [ ] 创建接口返回 `201 Created`
- [ ] 列表用 `Page<T>` + 投影
- [ ] DTO 有 `@Schema`，必填字段 `requiredMode = REQUIRED`

### 数据库（06-database.md / 28-migration.md）

- [ ] 新建表有索引（特别是 `tenant_id` 前导）
- [ ] 枚举字段 `@Enumerated(EnumType.STRING)`
- [ ] 字段有 `@Column(comment = ...)`
- [ ] 非空字段 `nullable = false`
- [ ] Flyway 迁移文件命名 `V{yyyyMMddHHmm}__{snake}.sql`
- [ ] 迁移文件不修改已发布版本

### 异常 / 日志（07-exception.md / 13-logging.md）

- [ ] 抛异常用 `ErrorCode.toXxxException()`
- [ ] Controller 不 try-catch
- [ ] 日志用占位符（不字符串拼接）
- [ ] `log.error` 异常作为最后一个独立参数（含堆栈）
- [ ] 不打印密码 / Token / 完整请求体（敏感字段脱敏）
- [ ] 无 `printStackTrace()` / `System.out`

### 并发（08-concurrency.md）

- [ ] 写操作 `@Transactional(rollbackFor = Exception.class)`
- [ ] 只读查询 `readOnly = true`
- [ ] 跨域事件处理器 `@Async + @TransactionalEventListener(AFTER_COMMIT) + REQUIRES_NEW`
- [ ] `@Async` 显式指定 TaskExecutor
- [ ] `ThreadLocal` 在 finally 中 `remove()`
- [ ] 事务方法不被同类内部调用

### 测试（09-testing.md）

- [ ] 新功能有单元测试（覆盖正常 / 边界 / 异常路径）
- [ ] 使用 `@Nested` + `@DisplayName` 分组
- [ ] 不连接真实 DB / 网络 / 文件
- [ ] 不使用 `Thread.sleep()` 做时序控制

### Starter（10-starter.md）

- [ ] `@AutoConfiguration` + `AutoConfiguration.imports`，不用旧式 `@Configuration` + `spring.factories`
- [ ] 必加 `@ConditionalOnClass` 防止类路径缺失报错
- [ ] 提供 `@ConditionalOnMissingBean` 允许覆盖
- [ ] `bootJar.enabled = false`、`jar.enabled = true`

### Feign（11-feign.md）

- [ ] FeignClient 在 `infrastructure/remote/` 包
- [ ] 无 fallback 默认实现
- [ ] `Pageable` 参数加 `@SpringQueryMap`
- [ ] 不在 FeignClient 上加 `@Transactional`

### 安全（12-security.md）

- [ ] 无明文密码 / 密钥 / Token（Git / 日志 / 异常 / Swagger example）
- [ ] 敏感字段输出脱敏（手机/身份证/银行卡）
- [ ] 密码用 BCrypt（cost ≥ 12）
- [ ] 无 SQL 字符串拼接
- [ ] 关键操作有审计日志

### 缓存（14-cache.md）

- [ ] Key 符合 `eagle:{module}:{entity}:{id}` 命名
- [ ] 显式 TTL，无 `ttl=-1`
- [ ] 高 QPS 缓存有击穿防护（`CacheProtectionUtil`）
- [ ] 不缓存敏感字段

### 消息（15-messaging.md）

- [ ] Topic 命名 `{env}_{domain}_{event}`
- [ ] 消费端实现幂等（用 `eventId` 不用 `MsgId`）
- [ ] 死信队列有 `AbstractDlqListener` + 告警
- [ ] 消息体不含敏感字段

### 多租户 / 数据权限（17-tenant-permission.md）

- [ ] 业务实体有 `tenantId` 字段且不可更新
- [ ] 索引以 `tenant_id` 为前导
- [ ] `@DataPermission` 注解声明数据范围
- [ ] 异步任务 TaskExecutor 装饰器透传 ThreadLocal

### 配置（19-config.md）

- [ ] 敏感字段 `ENC()` 加密
- [ ] 不在 `application.yml` 硬编码 profile
- [ ] `@RefreshScope` 仅用于业务开关 / 阈值
- [ ] Properties 类有 `@Validated` 校验

### 性能（23-performance.md）

- [ ] 无 N+1 查询（`@EntityGraph` 或 fetch join）
- [ ] 高频查询有索引
- [ ] 大数据量分批 / 流式处理
- [ ] Feign 调用有超时

## PR 描述模板

```markdown
## 背景

<为什么做这件事>

## 变更内容

- 模块 A：xxx
- 模块 B：xxx

## 影响范围

- [ ] 数据库结构变更（含 Flyway 脚本）
- [ ] 配置项新增 / 变更
- [ ] 公共 API 破坏性变更
- [ ] 跨服务事件契约变更

## 自测

- [ ] 本地构建通过
- [ ] Modulith 验证通过
- [ ] 单元测试通过且覆盖关键路径
- [ ] 涉及 UI 的已手工验证

## 风险与回滚

<上线风险与快速回滚方案>
```

## 评审重点（Reviewer 视角）

1. **架构边界**：跨域依赖是否合规？是否制造未来包袱？
2. **数据一致性**：事务边界、事件时序、幂等
3. **性能**：是否引入 N+1 / 大事务 / 同步阻塞
4. **安全**：权限注解、敏感字段、外部输入校验
5. **可观测性**：关键路径有日志 / 指标 / 告警
6. **可测试性**：依赖是否能 mock，逻辑是否分离
