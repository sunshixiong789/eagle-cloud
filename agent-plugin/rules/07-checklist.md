# 高频陷阱 + 存量违例台账 + PR 自检

# 一、高频陷阱（写代码前先扫一眼）

凭直觉写必然出错的 Eagle 特有 API 与命名，全部已对照源码核实。

| # | 陷阱 | 正确写法 |
|---|---|---|
| 1 | 审计字段名 | `createBy` / `updateBy` / `createTime` / `updateTime`（**不是** `createdBy` / `createdAt`）；类型 `Long` + `LocalDateTime` |
| 2 | `BaseEntity` 无软删除字段 | 只有 `id/createBy/updateBy/createTime/updateTime/version`；要 `deleted` 得自己声明 |
| 3 | 表名前缀 | `sys_` / `auth_` / `user_` / `eagle_`，**不是** `t_` |
| 4 | 异步执行器 Bean 名 | `taskExecutor`（**不是** `eagleTaskExecutor`） |
| 5 | **多租户已整体移除** | `eagle-tenant-starter` 源码已清空并移出 `settings.gradle`。**`TenantContextHolder` / `@TenantFilter` / `eagle.tenant.*` 全部不存在**，`ContextPropagationConfig` 也不再传播租户。不要写任何租户相关代码 |
| 6 | 分布式锁签名 | `tryLock(String key, long waitTime, long leaseTime, Supplier<T>)` —— **`long` 秒，不是 `Duration`**；简写 `tryLock(key, supplier)` 默认 3s/30s |
| 7 | 缓存击穿防护 | `getWithMutex(String key, Duration ttl, Supplier<T> loader, Class<T> type)` —— **4 个参数** |
| 8 | 数据权限 | 无 `@DataPermission` / `DataPermissionProvider`。只剩 `Role` 上的业务枚举 `DataScope`：`ALL` / `SELF` / `DEPT` / `DEPT_AND_CHILD` / `CUSTOM`（**没有** `DEPT_ONLY` / `SELF_ONLY`），需手写过滤 |
| 9 | AMQP 消费者 | 继承 `AbstractAmqpListener<T>` 实现 `getTopic()` / `getEventClass()` / `handle(T)`，**不用** `@RabbitListener`（exchange 名运行时才定，注解常量表达不了）；**必须手写构造器显式 `super(amqpProperties)`，禁用 `@RequiredArgsConstructor`**（`AbstractDlqListener` 同理）。容器由 `AmqpListenerRegistrar` 启动期注册 |
| 10 | AMQP 通配 routing key | 用 `#`（零或多个单词），**不是** RocketMQ 的 `*` —— AMQP 里 `*` 只匹配**恰好一个**单词，照搬会静默收不到消息。常量 `ExchangeNaming.MATCH_ALL_ROUTING_KEY` |
| 11 | 读写分离 / 分库分表 | `eagle-dynamic-datasource-starter` **已移除**，无 `@ReadOnly` / `DataSourceContextHolder`。分库分表用 `eagle-sharding-starter`（`eagle.sharding.*`） |
| 12 | 脱敏 | **无 `@Sensitive` 注解**；日志用 `LogMask.phone/email/idCard/token`，响应体无统一机制 |
| 13 | 审计表名 | `eagle_audit_log`（**不是** `t_audit_log`） |
| 14 | Token 撤销 key | `token:blacklist:{jti}` + `account:online:{accountId}` |
| 15 | Jackson 包名 | 核心类 `tools.jackson.*`，注解仍 `com.fasterxml.jackson.annotation.*`（详见 `06-boot4.md`） |
| 16 | 幂等 key | 用 `BaseEvent.eventId`，**不用** MQ 的 `MsgId`（重投递会变） |
| 17 | 生产必改的默认值 | `eagle.storage.type` 默认 `local`；`eagle.tracing.sampling-probability` 默认 `1.0`（全采样） |
| 18 | 开虚拟线程前 | 必须先设 `eagle.async.concurrency-limit` 正数，否则无背压打爆下游 |
| 19 | 自定义 `SecurityFilterChain` | 一旦自定义 chain 取代 starter 默认 chain，`oauth2ResourceServer.jwt` **必须显式接 `EagleJwtAuthenticationConverter`**，否则 principal 不是 `EagleUser`，所有 `hasRole(...)` 静默失效（不报错、全部 403） |
| 20 | starter 里的 util 类 | 只标 `@Component` 业务服务扫不到（跨根包），必须在 `@AutoConfiguration` 里显式 `@Bean` 注册（见 `06-boot4.md`） |
| 21 | starter 装配开关 | **不存在** `eagle.xxx.enabled` 总开关，引入依赖即生效；条件注解只用于「选实现」而非「要不要装」 |
| 22 | dom4j 传递依赖 | `dom4j 2.1.3` 会传递 `pull-parser:2`（含 `org.gjt.xpp.*`），曾导致启动期 SAX 解析器冲突崩溃，引入 dom4j 的服务必须 `exclude group: 'pull-parser'` |
| 23 | **9 个 starter 已移除** | `tenant` / `rocketmq`（→ `amqp`）/ `dynamic-datasource` / `elasticsearch` / `excel` / `notification` / `seata` / `sentinel` / `ai` —— 目录还在但 `src` 已清空且不在 `settings.gradle`。**不要 import 这些包，也不要照抄它们的 skill 文档** |
| 24 | 注册中心是 Consul | `spring-cloud-starter-consul-discovery`，**不是 Nacos**。配置走 `spring.cloud.consul.*` |

---

# 二、存量违例台账

规则写的是**应然**。下列条目存量代码已违反，**新代码不得新增**，也**不要**为达标发起批量改写 PR。

| 规则 | 存量违例 | 位置 |
|---|---|---|
| Controller 不注入 `Repository` | **1 处** | `AccountInternalController.accountRepository`（`LoginController` / `WechatWebLoginController` 的 `SecurityContextRepository` 是 Spring Security 的上下文存储，非数据仓储，不计入） |
| 禁 `@PreAuthorize("isAuthenticated()")` | **13 处** | 各 Controller |
| 禁 `@PreAuthorize("permitAll()")` | **4 处** | 各 Controller |
| 禁 `@Value` 注入配置 | **2 处** | `BlacklistCacheWarmer`、`EagleAuditLogJpaAutoConfiguration` |
| JPA 实体禁 `@Data` / `@Builder` | **1 处** | `AuditLogRecord`（starter 内部持久化记录，非领域聚合根） |
| 错误码号段不得冲突 | **2 组 / 共 7 个码** | `40001-40003`(File/Idempotency)、`90001-90004`(Lock/Ai) |
| 资源不存在用 `toNotFoundException()` | `toDomainException` 98 次 vs `toNotFoundException` 4 次，存在语义漂移 | 全局 |
| `sealed` + 模式匹配建模 | 使用数 0（本轮新引入） | 仅约束新代码 |

## ArchUnit 分层门禁（`LayeredArchitectureTest`）

两个服务各有一份**同规则**的 `LayeredArchitectureTest`，各 10 条。
**无冻结基线——任何违例直接让测试失败。**

**改规则时两个测试文件要一起改**，否则规则只在一边生效，另一边会悄悄腐化。

### 存量已清零（2026-08-07）

原先冻结在 `archunit_store/` 的 51 条存量违例（system 22 + auth 29）已全部修复，
`FreezingArchRule`、`archunit_store/`、`archunit.properties` 一并移除。

清理过程中确立了三条**包放置约定**——原违例大多不是真的架构错误，而是类放错了层：

| 约定 | 落地位置 | 理由 |
|---|---|---|
| `@ConfigurationProperties` 配置契约与四层平级 | `auth: core/config` | 配置是应用输入而非基础设施实现，同一个 `AdminProperties` application 和 interfaces 都要读；Controller 读配置不该算分层违例 |
| 集成事件契约（MQ 消息体）放 application 层 | `system: base/application/event` | 它是 application 用例方法的入参（`onAccountRegistered` 等），infrastructure 的 consumer 与 HTTP 降级端点都只是它的驱动适配器 |
| ErrorCode 枚举放 domain 层 | `system: */domain/model`、`auth: core/domain` | 领域规则的错误语义属于领域；放 `interfaces/exception` 会让聚合根反向依赖外层。`UserErrorCode` / `SystemErrorCode` / `FileErrorCode` 本就在 domain，那两个是异类 |

Controller 确实需要限流器 / 黑名单这类**真**基础设施能力时，收进应用服务
（见 `SmsApplicationService`、`AccountApplicationService.register`），不要直接注入。

**注意**：子实体（继承 `BaseEntity`）按 `00-core.md` **允许** `@Setter`，该规则只查聚合根。

复核台账：

```bash
grep -rc '@PreAuthorize("isAuthenticated()")' --include='*.java' eagle-services | grep -v ':0'
grep -rc '@PreAuthorize("permitAll()")'      --include='*.java' eagle-services | grep -v ':0'
grep -rE '@Value\("\$\{' --include='*.java' eagle-services eagle-starter | grep -v /build/

# 非 record 的 DTO 数量（已全量迁移完毕，基线 0 —— 出现任何非 0 都是新增违例）
find eagle-services -path '*interfaces/dto/*.java' -not -path '*/build/*' -not -name '*Test.java' \
  -exec grep -L '^public record ' {} + | wc -l
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

## 安全
- [ ] 当前用户走 `SecurityUtils.getCurrentUser()` 或 `@AuthenticationPrincipal EagleUser`，**未用 `Jwt`**
- [ ] 无明文密码 / 密钥 / Token；敏感配置 `ENC()`
- [ ] 日志中手机号、邮箱等经 `LogMask` 处理
- [ ] 未引用已移除的能力（`TenantContextHolder` / `@DataPermission` / `@GlobalTransactional` / Sentinel / RocketMQ）
- [ ] 自定义 `SecurityFilterChain` 已显式接 `EagleJwtAuthenticationConverter`

## Starter / 配置 / 依赖
- [ ] `@AutoConfiguration` + `AutoConfiguration.imports` 注册；条件注解齐全
- [ ] 无 `eagle.xxx.enabled` 总开关；starter 内 util 类已显式 `@Bean` 注册
- [ ] 配置走 `@ConfigurationProperties`，无 `@Value`
- [ ] 新依赖先进 BOM；本 PR 未夹带依赖升级

## 质量 / 内聚耦合（详见 `08-quality.md`）
- [ ] 新 DTO 是 `record`；无「new 空对象 + 逐字段 set」的分步装配
- [ ] 应用服务未做聚合内部状态判断（业务规则在领域层）
- [ ] Controller 未注入 `Repository`，方法体 ≤ 5 行
- [ ] 类 / 方法 / 参数未超规模硬上限（12 方法 / 50 行 / 5 参数）
- [ ] 未为单实现抽接口、未为"以后可能需要"预留参数或配置项
- [ ] 通用机制（重试 / 死信 / 线程池 / 序列化 / 调度）先确认过框架与已引入依赖不提供，才自己写；
      未绕开框架的装配路径（手动 new 掉 Boot 的 `*Factory` 会让对应 `spring.*` 配置整片失效）
- [ ] 进 starter 的代码不含业务领域名词
- [ ] 本次改动删净了不再被调用的方法 / 字段 / import

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
