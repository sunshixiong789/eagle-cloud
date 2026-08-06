# 高频陷阱 + 存量违例台账 + PR 自检

# 一、高频陷阱（写代码前先扫一眼）

凭直觉写必然出错的 Eagle 特有 API 与命名，全部已对照源码核实。

| # | 陷阱 | 正确写法 |
|---|---|---|
| 1 | 审计字段名 | `createBy` / `updateBy` / `createTime` / `updateTime`（**不是** `createdBy` / `createdAt`）；类型 `Long` + `LocalDateTime` |
| 2 | `BaseEntity` 无软删除字段 | 只有 `id/createBy/updateBy/createTime/updateTime/version`；要 `deleted` 得自己声明 |
| 3 | 表名前缀 | `sys_` / `auth_` / `user_` / `eagle_`，**不是** `t_` |
| 4 | 异步执行器 Bean 名 | `taskExecutor`（**不是** `eagleTaskExecutor`） |
| 5 | 租户上下文 API | `TenantContextHolder.getTenantId()` / `setTenantId()` / `clear()`（**没有** `getCurrentTenantId()`） |
| 6 | 分布式锁签名 | `tryLock(String key, long waitTime, long leaseTime, Supplier<T>)` —— **`long` 秒，不是 `Duration`**；简写 `tryLock(key, supplier)` 默认 3s/30s |
| 7 | 缓存击穿防护 | `getWithMutex(String key, Duration ttl, Supplier<T> loader, Class<T> type)` —— **4 个参数** |
| 8 | 数据权限 | `eagle-row-security-starter` **已移除**，无 `@DataPermission` / `DataPermissionProvider`。只剩 `Role` 上的业务枚举 `DataScope`：`ALL` / `SELF` / `DEPT` / `DEPT_AND_CHILD` / `CUSTOM`（**没有** `DEPT_ONLY` / `SELF_ONLY`），需手写过滤 |
| 9 | AMQP 消费者 | 继承 `AbstractAmqpListener<T>` 实现 `getTopic()` / `getEventClass()` / `handle(T)`，**不用** `@RabbitListener`；**必须手写构造器显式 `super(amqpProperties)`，禁用 `@RequiredArgsConstructor`**（`AbstractDlqListener` 同理） |
| 10 | 多租户装配条件 | 由 `eagle.tenant.mode` 决定（`column`/`database`，默认 `COLUMN`）。**不存在 `eagle.tenant.enabled`** |
| 11 | 读写分离装配条件 | 由 `eagle.datasource.master.url` 是否配置决定。**不存在 `eagle.datasource.enabled`** |
| 12 | 脱敏 | **无 `@Sensitive` 注解**；日志用 `LogMask.phone/email/idCard/token`，响应体无统一机制 |
| 13 | 审计表名 | `eagle_audit_log`（**不是** `t_audit_log`） |
| 14 | Token 撤销 key | `token:blacklist:{jti}` + `account:online:{accountId}` |
| 15 | Jackson 包名 | 核心类 `tools.jackson.*`，注解仍 `com.fasterxml.jackson.annotation.*`（详见 `06-boot4.md`） |
| 16 | 幂等 key | 用 `BaseEvent.eventId`，**不用** MQ 的 `MsgId`（重投递会变） |
| 17 | 生产必改的默认值 | `eagle.storage.type` 默认 `local`；`eagle.tracing.sampling-probability` 默认 `1.0`（全采样） |
| 18 | 开虚拟线程前 | 必须先设 `eagle.async.concurrency-limit` 正数，否则无背压打爆下游 |

---

# 二、存量违例台账

规则写的是**应然**。下列条目存量代码已违反，**新代码不得新增**，也**不要**为达标发起批量改写 PR。

| 规则 | 存量违例 | 位置 |
|---|---|---|
| 禁 `@PreAuthorize("isAuthenticated()")` | **13 处** | 各 Controller |
| 禁 `@PreAuthorize("permitAll()")` | **4 处** | 各 Controller |
| 禁 `@Value` 注入配置 | **2 处** | `BlacklistCacheWarmer`、`EagleAuditLogJpaAutoConfiguration` |
| JPA 实体禁 `@Data` / `@Builder` | **1 处** | `AuditLogRecord`（starter 内部持久化记录，非领域聚合根） |
| 错误码号段不得冲突 | **2 组 / 共 7 个码** | `40001-40003`(File/Idempotency)、`90001-90004`(Lock/Ai) |
| 资源不存在用 `toNotFoundException()` | `toDomainException` 98 次 vs `toNotFoundException` 4 次，存在语义漂移 | 全局 |
| `sealed` + 模式匹配建模 | 使用数 0（本轮新引入） | 仅约束新代码 |

## ArchUnit 冻结基线（`LayeredArchitectureTest`）

分层违例已冻结在 `eagle-services/eagle-system-service/archunit_store/`，**新增违例会让测试失败，存量不阻塞**。
修好一处基线自动少一处，不会倒退。当前冻结 22 条：

| 违例 | 条数 | 典型案例 / 成因 |
|---|---|---|
| `interfaces` 依赖 `infrastructure` | 10 | `AnnouncementView.of(AnnouncementSnapshot)` —— DTO 直接吃 infrastructure 的缓存快照 |
| `domain` 依赖 `interfaces` | 6 | `UserMessage` / `Announcement` 调 `MessageErrorCode`，而该 ErrorCode 放在了 `interfaces/exception/` —— **ErrorCode 应下沉到 domain** |
| 应用服务命名不符 | 4 | `AuthorizationQueryService`、`SystemLogRecorder`、`AnnouncementAdminService`、`AnnouncementQueryService`（CQRS 查询服务是否该另立命名，待定） |
| 聚合根暴露 public setter | 1 | `Role.setDataScope` |
| Controller 含 try-catch | 1 | `FileController.parseMediaType`（私有解析辅助方法） |

**注意**：子实体（继承 `BaseEntity`）按 `00-core.md` **允许** `@Setter`，该规则只查聚合根。

复核台账：

```bash
grep -rc '@PreAuthorize("isAuthenticated()")' --include='*.java' eagle-services | grep -v ':0'
grep -rc '@PreAuthorize("permitAll()")'      --include='*.java' eagle-services | grep -v ':0'
grep -rE '@Value\("\$\{' --include='*.java' eagle-services eagle-starter | grep -v /build/
```

---

# 三、必跑命令

```bash
gradle :eagle-services:eagle-system-service:test                                  # 小改动
gradle :eagle-services:eagle-system-service:test --tests "*ModulithArchitectureTest"  # Modulith 边界变更
gradle build                                                                      # 公共 starter / BOM / 跨模块契约
```

一键：`/check-arch`。模块路径以 `settings.gradle` 为准（`eagle-services:eagle-system-service` / `eagle-auth-service` / `eagle-gateway-service`）。

## 静态自检（无输出即合规）

```bash
# 禁 @Value 注入配置
grep -rEn '@Value\s*\(\s*"\$\{' --include="*.java" eagle-services eagle-starter | grep -v /build/

# 漏网的物理外键（@ForeignKey 常换行，必须跨行检查）
find eagle-services eagle-starter -name "*.java" -not -path "*/build/*" -exec perl -0777 -ne \
  'while (/\@JoinColumn\b/g) { print "$ARGV\n" unless substr($_, pos(), 200) =~ /NO_CONSTRAINT/ }' {} \; | sort -u

# 重名索引（跨 schema 会导致 H2/PG 启动失败）
find . -name "*.java" -not -path "*/build/*" | xargs grep -hE "@Index\(name\s*=\s*\"" \
  | grep -oE 'name\s*=\s*"[^"]+"' | sort | uniq -c | awk '$1 > 1'

# 错误码撞号
find . -name "*ErrorCode.java" -not -path "*/build/*" -exec grep -hoE '\(([0-9]{4,6}),' {} \; \
  | tr -d '(,' | sort | uniq -d

# 规则提到的类是否真实存在（防止规则描述虚构 API）
for id in LogMask SecurityUtils EagleUser DistributedLock CacheProtectionUtil BaseEntity; do
  printf "%-22s %s\n" "$id" "$(find . -name "$id.java" -not -path '*/build/*' | head -1)"
done
```

---

# 四、自检清单

## 语言（Java 25）
- [ ] 新 DTO / 值对象 / 事件载荷用 `record`，未加 Lombok
- [ ] 封闭类型层级用 `sealed`；对 `sealed` 的 switch **不写 `default`**
- [ ] 类型判断用模式匹配，未出现 `instanceof` + 强转链
- [ ] 未使用 preview 特性（如 Structured Concurrency）

## 架构
- [ ] 分层依赖符合 `interfaces → application → domain ← infrastructure`
- [ ] 跨服务协作走 HTTP client + 集成事件，未 import 对方内部包
- [ ] 新模块声明 `@ApplicationModule`，对外包声明 `@NamedInterface`
- [ ] DTO 映射用 record 静态工厂或 `@Component` Mapper（无 MapStruct / `BeanUtils`）
- [ ] 事件由聚合根 `registerEvent()` 发出

## API / 异常
- [ ] 无新增 `@PreAuthorize("isAuthenticated()")` / `permitAll()`；公开接口在 yml `permit-paths`
- [ ] 响应不用 `ApiResult` 包装；创建返 `201`
- [ ] 请求 DTO 有 Bean Validation + `@Valid`
- [ ] `Pageable` 三注解齐全（`@ParameterObject` + `@Parameter` + `@PageableDefault`）
- [ ] 资源不存在抛 `toNotFoundException()` 而非 `toDomainException()`
- [ ] 新错误码号段不与台账冲突；i18n 三语齐全

## 数据
- [ ] 聚合间只存 ID；**无物理外键**
- [ ] 索引名含表名前缀、schema 内唯一
- [ ] 枚举字段 `EnumType.STRING`
- [ ] 表名前缀符合 `sys_`/`auth_`/`user_`
- [ ] 写操作有事务；`@Transactional` 内无远程调用
- [ ] 生产 `ddl-auto: validate`

## 并发 / 事件 / 消息
- [ ] 领域事件处理器 `@Async + AFTER_COMMIT`；跨域额外 `REQUIRES_NEW`
- [ ] MQ 消费幂等（用 `eventId`）；DLQ 有处理和告警
- [ ] 消费方自己声明 `XxxMessage`，未 import 生产方 `XxxIntegrationEvent`
- [ ] `ThreadLocal` / MDC 有 `try/finally` 清理

## 安全 / 租户
- [ ] 当前用户走 `SecurityUtils.getCurrentUser()` 或 `@AuthenticationPrincipal EagleUser`，**未用 `Jwt`**
- [ ] 无明文密码 / 密钥 / Token；敏感配置 `ENC()`
- [ ] 日志中手机号、邮箱等经 `LogMask` 处理
- [ ] `@TenantFilter` 标在 Service/Repository 而非 Entity；跨租户操作有 reason + 审计

## Starter / 配置 / 依赖
- [ ] `@AutoConfiguration` + `AutoConfiguration.imports` 注册；条件注解齐全
- [ ] 配置走 `@ConfigurationProperties`，无 `@Value`
- [ ] 新依赖先进 BOM；本 PR 未夹带依赖升级

---

# 五、PR 描述模板

```markdown
## 背景

## 变更内容

## 影响范围

## 自测
- [ ] `gradle :eagle-services:{svc}:test`

## 风险与回滚

## 评审重点
```
