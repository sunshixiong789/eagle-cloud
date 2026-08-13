# 核心约定

最高优先级。只记录**模型默认会做错**的项目决策；通用代码质量（缩进、大小写、Conventional Commits）按默认能力执行。

规则写的是**应然**。凡与存量代码冲突处已标注违例数——**新代码按规则写，不要为达标批量改存量**。

## 不确定时先求证，不要凭记忆写

本仓库是 Spring Boot 4 / Jackson 3 / Hibernate 7 / Java 25，主流语料以 Boot 3 / Jackson 2 为主；
Eagle 自有 API（`DistributedLock` / `CacheProtectionUtil` / `AbstractAmqpListener` / `LogMask` …）
的签名更是没有任何语料。**凭直觉写出来的 API 大概率不存在。**

- 用到任何 Eagle 专有类 → 先 `grep` 一处已有调用，照着写
- 用到版本敏感 API（Security DSL、Jackson、Hibernate）→ 先读仓库里的同类实现
- 查不到又必须用 → 在回答里明说"未在仓库中找到该 API，以下为推测写法"，**不要**默默编一个

宁可多读一个文件，也不要产出一个编译不过、或编译通过但语义错误的调用。

## 回答语言

对话、PR / commit 正文、文档正文 → **简体中文**。代码、命令、路径、技术名词（`Spring Modulith`、`@ApplicationModule`）→ 用户用英文提问时跟随中文。

## 配置注入：禁止 `@Value`

```java
// ❌ 禁（存量违例 2 处）
@Value("${eagle.payment.gateway-url}") private String url;

// ✅ @ConfigurationProperties，Properties 类放 infrastructure/config/
@Data @Validated @ConfigurationProperties(prefix = "eagle.payment")
public class PaymentProperties {
    @NotBlank private String gatewayUrl;
    private Duration timeout = Duration.ofSeconds(5);   // 用 Duration/DataSize，不用 long
}
```

- 前缀统一 `eagle.{feature}`，字段必须有合理默认值
- 在 `@AutoConfiguration` 上 `@EnableConfigurationProperties(XxxProperties.class)`，不用 `@ConfigurationPropertiesScan`
- 加 `@Validated` + Bean Validation 实现启动期 fail-fast
- 唯一例外：`@Value("classpath:xxx")` 注入 `Resource`（优先 `ResourceLoader`）

## Lombok（按角色区分）

| 角色 | 注解 | 约束 |
|---|---|---|
| 聚合根 | `@Getter @NoArgsConstructor` | 静态工厂创建，业务方法改状态，**无 setter**，**禁 `@Builder`** |
| 子实体 | `@Getter @Setter @NoArgsConstructor` | 由聚合根级联管理 |
| Service / Controller | `@RequiredArgsConstructor` | 构造器注入 |
| DTO / 值对象 | **不用 Lombok** | 一律 `record`，见 `01-java25.md` |

**JPA 实体禁止 `@Data` 和 `@Builder`。**

例外：`AbstractAmqpListener` / `AbstractDlqListener` 子类**禁用 `@RequiredArgsConstructor`** —— 基类是构造器注入，Lombok 生成的构造器不会调用带参 `super`，必须手写构造器显式 `super(amqpProperties)`。

## 空安全

新增模块的 `package-info.java` 加 JSpecify `@NullMarked`（现有 23 个 package 已加）。

## DDD 命名（只列非显然项）

| 组件 | 规则 | 示例 |
|---|---|---|
| 子实体 | `{Name}Entity` | `OrderItemEntity` |
| 应用服务 | `{Name}ApplicationService` | `OrderApplicationService` |
| 领域服务 | 接口 `{Name}Service` 在 `domain/service/`，实现 `{Name}ServiceImpl` 在 `infrastructure/` | `PricingService` |
| Port / Adapter | `{Name}Port` / `{Name}QueryPort` → `{Name}Adapter` | `InventoryQueryPort` |
| 投影接口 | `{Name}Summary` / `{Name}View` | `OrderSummary` |
| Mapper | `{Name}Mapper`（纯 Java `@Component`） | `OrderMapper` |
| 错误码 | `{Domain}ErrorCode` | `OrderErrorCode` |
| 领域事件 | `{聚合根}{动作}Event`，动作过去时 | `OrderPaidEvent` |
| Named Interface | 小写短名 | `"port"` / `"event"` |

## 测试

现状：`@Nested` 85 文件、`@DisplayName` 139、`MockitoExtension` 63、AssertJ 32、`@SpringBootTest` 仅 3 —— **以纯单元测试为主**，保持这个比例。

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("订单应用服务")
class OrderApplicationServiceTest {

    @Nested
    @DisplayName("创建订单")
    class Create {
        @Test
        @DisplayName("库存不足时抛 DomainException")
        void shouldThrowWhenStockInsufficient() { /* AAA */ }
    }
}
```

- 方法名 `should{行为}When{前提}`，配 `@DisplayName` 写中文
- 断言用 AssertJ `assertThat`，不用 JUnit 原生 `assertEquals`
- **不连真实 DB / Redis / RabbitMQ / Consul / 网络**；starter 自动配置用 `ApplicationContextRunner`
- 需要真实基础设施的 smoke test 标 `@Disabled("manual infrastructure test")`
- 禁止：测试间依赖执行顺序、用 `Thread.sleep()` 等异步结果（用 Awaitility）

| 变更类型 | 必跑 |
|---|---|
| 普通变更 | `gradle :eagle-services:{svc}:test` |
| Modulith 边界 / Named Interface | `gradle :eagle-services:{svc}:test --tests "*ModulithArchitectureTest"` |
| 公共 starter / BOM / Gradle / 跨模块契约 | `gradle build` |

真实模块路径见 `settings.gradle`：`eagle-services:eagle-system-service`、`eagle-auth-service`、`eagle-gateway-service`。

## 依赖

- 版本号集中在 `eagle-bom`，业务模块与 starter **不写版本号**
- 新增第三方依赖先进 BOM 再引用
- **禁止业务 PR 夹带依赖升级**（独立 PR + 独立评审）
- starter 依赖范围：核心用 `api`，可选用 `compileOnly`（配 `@ConditionalOnClass`）
- **禁止**业务模块自行集成 Redisson / RabbitMQ Client / MinIO Client —— 收敛到对应 starter
- 当前 **19 个 starter** 在 `settings.gradle` 中；`tenant` / `rocketmq` / `dynamic-datasource` /
  `elasticsearch` / `excel` / `notification` / `seata` / `sentinel` / `ai` 这 9 个**已清空并移出构建**
  （目录空壳仍在），不要 import 也不要引用它们的 skill 文档

## Git

- 主干 `main`，功能分支短生命周期（`feature/` `fix/` `chore/`）
- 带 scope 的 Conventional Commits，scope 用模块 / starter / 领域名
- 业务变更、格式化、重命名、依赖升级**不混在同一 commit**
- 共享分支禁止 rebase 和 `push --force`（个人分支用 `--force-with-lease`）

## 禁止清单

- 静默吞掉 `Exception` / `Throwable`
- 提交注释掉的代码、无说明的 TODO、调试输出
- `BeanUtils.copyProperties` 等反射式拷贝
- 提交密钥、令牌、本地端点、构建产物、IDE 私有配置
