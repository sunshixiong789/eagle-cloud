# Eagle Cloud

基于 **DDD（领域驱动设计）+ 六边形架构 + Spring Modulith** 构建的 Spring Boot 模块化单体平台，为微服务拆分就绪。

内置完整的 **OAuth2 授权服务器**、**RBAC 权限管理**、**多种第三方登录**（微信小程序 / PC 扫码 / H5 / 短信验证码），以及面向*
*互联网高并发**的全套基础设施，开箱即用。

## 特性

- **DDD 分层架构** — 严格的 `interfaces / application / domain / infrastructure` 四层分离，领域逻辑纯净无框架依赖
- **六边形架构（Ports & Adapters）** — 跨域协作通过 Port 接口隔离，拆分微服务时仅替换基础设施层实现，业务代码零改动
- **Spring Modulith 模块治理** — 编译期静态验证模块边界，杜绝循环依赖和非法跨模块访问
- **OAuth2 授权服务器** — 基于 Spring Authorization Server，支持授权码 + PKCE、刷新令牌、微信登录、短信登录
- **RBAC 权限体系** — 用户、角色、权限、部门、菜单、岗位、字典完整管理
- **行级数据权限** — 基于 AspectJ 的细粒度数据访问控制
- **多级缓存** — Redis (Redisson) + Caffeine 两级缓存，内置缓存穿透 / 击穿 / 雪崩三重防护
- **API 网关** — Spring Cloud Gateway + Sentinel 限流（Nacos 持久化）+ Request ID 全链路注入 + 链路追踪
- **统一异常体系** — 类型化异常 + ErrorCode 枚举 + i18n 国际化消息
- **接口幂等** — TOKEN / BUSINESS_KEY / RESULT_CACHE 三种模式，注解驱动零侵入
- **分布式 ID** — Snowflake / UUID / 号段三策略，内置订单号 / 支付单号语义生成器
- **流量治理** — Sentinel 声明式限流（`@RateLimit`）+ 程序化规则管理，支持 WARM_UP / 匀速排队
- **分布式事务** — Seata AT 模式自动集成 + TCC 模板 + 编程式全局事务
- **全文搜索** — Elasticsearch 流式 DSL 构建器、通用 Repository、高亮回写、聚合提取
- **MyBatis-Plus 增强** — 统一分页 / 慢 SQL / 审计填充 / 链式查询条件辅助
- **支付集成** — 支付宝 / 微信支付双网关，统一 pay / refund / transfer / notify 接口
- **实时推送** — STOMP WebSocket + SSE 双模式，离线消息存储，Micrometer 连接指标
- **分库分表** — Apache ShardingSphere YAML 驱动，零侵入分库分表与读写分离
- **Excel 导入导出** — Apache POI + `@ExcelColumn` 注解，万行以上自动切换流式写入
- **容错弹性** — Resilience4J 熔断器 / 重试 / 超时，`eagle-default` 命名实例开箱即用
- **字段级加密** — AES-256 JPA `AttributeConverter`，`@Convert` 注解驱动，零侵入渐进迁移
- **操作审计日志** — `@AuditLog` 注解，AOP 切面 + 异步事件 + 可插拔 Handler
- **AI 集成** — Spring AI 2.x，ChatClient / EmbeddingClient，支持多模型切换
- **全链路压测** — `X-Eagle-Gray` 压测流量标记，跨服务自动透传
- **GraalVM Native Image** — 支持原生镜像编译

## 技术栈

| 类别            | 技术                                            | 版本                    |
|---------------|-----------------------------------------------|-----------------------|
| 语言            | Java                                          | 25                    |
| 框架            | Spring Boot                                   | 4.0.6                 |
| 微服务           | Spring Cloud / Spring Cloud Alibaba           | 2025.1.1 / 2025.1.0.0 |
| 模块治理          | Spring Modulith                               | 2.0.5                 |
| ORM (JPA)     | Hibernate                                     | 7.2.6                 |
| ORM (MyBatis) | MyBatis-Plus                                  | 3.5.11                |
| 数据库           | MySQL / PostgreSQL / H2                       | 9.x / 42.7 / -        |
| 搜索引擎          | Elasticsearch                                 | 9.x                   |
| 缓存            | Redis (Redisson) + Caffeine                   | 4.3.0 / 3.2.0         |
| 安全            | Spring Security + OAuth2 Authorization Server | -                     |
| 网关            | Spring Cloud Gateway + Sentinel               | -                     |
| 注册中心          | Nacos                                         | v3                    |
| 消息队列          | Apache RocketMQ                               | 2.3.5                 |
| 分布式事务         | Seata                                         | 2.2.0                 |
| 定时任务          | XXL-JOB                                       | 2.4.2                 |
| 对象存储          | MinIO                                         | 8.5.17                |
| 支付            | 支付宝 SDK / 微信支付 APIv3                          | 4.39 / 0.2.14         |
| 实时推送          | STOMP WebSocket + SSE                         | -                     |
| 分库分表          | Apache ShardingSphere                         | 5.5.0                 |
| Excel 处理      | Apache POI                                    | 5.x                   |
| 容错弹性          | Resilience4J                                  | 2.2.0                 |
| AI 集成         | Spring AI                                     | 2.x                   |
| API 文档        | SpringDoc OpenAPI                             | 3.0.2                 |
| 构建工具          | Gradle (Groovy DSL)                           | 9.x                   |

## 项目结构

```
eagle-cloud/
├── eagle-base-server/                  # 可执行服务
│   ├── eagle-system-server/            # 系统服务（OAuth2 + 用户权限管理）
│   ├── eagle-gateway-server/           # API 网关
│   └── docker-compose.yml              # 开发环境容器编排
│
└── eagle-starter/                          # 可复用 Starter 库（共 28 个）
    │
    │   # ── 基础能力 ──────────────────────────────────────────────────
    ├── eagle-common-starter/               # 核心基础设施（基类、异常体系、领域事件、i18n、压测流量标记）
    ├── eagle-data-jpa-starter/             # JPA / Hibernate 配置（审计、多数据库方言）
    ├── eagle-redis-starter/                # Redis + Caffeine 多级缓存（穿透 / 击穿 / 雪崩防护）
    ├── eagle-resource-server-starter/      # OAuth2 资源服务器 JWT 验证
    ├── http-client-starter/                # RestClient / HTTP Service（JWT / 租户 / Seata XID 透传）
    ├── eagle-tracing-starter/              # 分布式链路追踪（Brave / Zipkin）
    ├── eagle-openapi-starter/              # Swagger / OpenAPI 文档集成
    │
    │   # ── 数据访问 ──────────────────────────────────────────────────
    ├── eagle-dynamic-datasource-starter/   # 多数据源动态路由（主从切换 / 多从轮询 / @ReadOnly）
    ├── eagle-sharding-starter/             # 分库分表（Apache ShardingSphere YAML 驱动）
    ├── eagle-mybatis-starter/              # MyBatis-Plus 增强（分页 / 慢 SQL / 审计填充 / 查询辅助）
    ├── eagle-elasticsearch-starter/        # Elasticsearch（流式 DSL / 通用 Repository / 高亮 / 聚合）
    │
    │   # ── 消息与任务 ────────────────────────────────────────────────
    ├── eagle-rocketmq-starter/             # RocketMQ v5 消息队列（事务消息 / DLQ / 消费骨架）
    ├── eagle-scheduler-starter/            # 分布式定时任务（XXL-JOB 2.4.2）
    ├── eagle-notification-starter/         # 多渠道消息推送（阿里云 SMS / Spring Mail）
    │
    │   # ── 高并发 ────────────────────────────────────────────────────
    ├── eagle-idempotency-starter/          # 接口幂等（TOKEN / BUSINESS_KEY / RESULT_CACHE 三模式）
    ├── eagle-id-generator-starter/         # 分布式 ID（Snowflake / UUID / 号段 + 订单号语义生成）
    ├── eagle-sentinel-starter/             # 流量治理（@RateLimit 注解 + 规则动态管理）
    ├── eagle-seata-starter/                # 分布式事务（AT 自动集成 + TCC 模板 + 编程式事务）
    ├── eagle-resilience-starter/           # 容错弹性（Resilience4J 熔断器 / 重试 / 超时）
    ├── eagle-payment-starter/              # 支付集成（支付宝 / 微信双网关 + 转账 + 签名验证）
    ├── eagle-websocket-starter/            # 实时推送（STOMP WebSocket + SSE + 离线消息存储）
    │
    │   # ── 平台能力 ──────────────────────────────────────────────────
    ├── eagle-row-security-starter/         # 行级数据权限（@DataPermission，JPA Specification）
    ├── eagle-tenant-starter/               # 多租户支持（COLUMN / DATABASE 隔离模式）
    ├── eagle-oss-minio-starter/            # 对象存储（MinIO，签名 URL / 分片上传）
    ├── eagle-excel-starter/                # Excel 导入导出（@ExcelColumn，流式写入）
    ├── eagle-encrypt-starter/              # 字段级加密（AES-256，JPA @Convert 注解驱动）
    ├── eagle-audit-log-starter/            # 操作审计日志（@AuditLog，AOP + 异步事件）
    └── eagle-ai-starter/                   # AI 集成（Spring AI 2.x，ChatClient / EmbeddingClient）
```

### Spring Modulith 模块划分

`eagle-system-server` 内按有界上下文划分为 4 个模块：

| 模块         | 职责                                                        |
|------------|-----------------------------------------------------------|
| **auth**   | 认证授权 — OAuth2 授权服务器、微信 / 短信第三方登录、账号管理                     |
| **base**   | 系统管理 — 用户、角色、权限、部门、菜单、岗位、字典、审计日志                          |
| **config** | 全局配置 — Security、Cache、Async、i18n、OpenAPI、WebSocket、全局异常处理 |
| **common** | 共享内核 — 跨域事件契约、ErrorCode 枚举、通用 DTO                         |

模块间通过 **领域事件** 异步解耦，跨域依赖通过 **六边形 Port 接口** 隔离。

### DDD 分层架构

每个业务模块内部遵循严格的四层架构：

```
{module}/
├── interfaces/             # 接口层（REST Controller + DTO）
├── application/            # 应用层（用例编排、事务边界、纯 Java Mapper）
├── domain/                 # 领域层（聚合根、实体、值对象、仓储接口、领域事件、Port 接口）
└── infrastructure/         # 基础设施层（仓储实现、Port 适配器、安全、配置）
```

依赖方向（单向）：`interfaces → application → domain ← infrastructure`

## 高并发 Starter 使用说明

### eagle-idempotency-starter — 接口幂等

三种模式通过 `@Idempotent` 注解声明，零侵入：

```java
// TOKEN 模式：客户端提前申请 token，一次性消费
@PostMapping("/orders")
@Idempotent(mode = IdempotencyMode.TOKEN)
public OrderResponse createOrder(@RequestBody CreateOrderRequest req) { ... }

// BUSINESS_KEY 模式：SpEL 提取业务键 SETNX 防重
@Idempotent(mode = IdempotencyMode.BUSINESS_KEY, key = "#req.orderNo")
public void payOrder(@RequestBody PayRequest req) { ... }

// RESULT_CACHE 模式：缓存首次响应，重试直接返回缓存结果
@Idempotent(mode = IdempotencyMode.RESULT_CACHE, tokenHeader = "X-Idempotency-Key")
public PayResult submitPay(@RequestBody PayRequest req) { ... }
```

申请 Token（TOKEN / RESULT_CACHE 模式）：`GET /idempotency/token`

---

### eagle-id-generator-starter — 分布式 ID

```java
@Autowired
private IdGeneratorFacade idGen;

long orderId  = idGen.snowflakeId();            // Snowflake ID
String orderNo = idGen.orderNo();               // ORD20240115000000001
String payNo   = idGen.payNo();                 // PAY20240115000000002
String refundNo = idGen.refundNo();             // RFD20240115000000003
```

---

### eagle-sentinel-starter — 流量治理

```java
// 声明式限流
@RateLimit(resource = "createOrder", qps = 100, behavior = FlowControlBehavior.WARM_UP)
public OrderResponse createOrder(...) { ... }

// 程序化规则（启动时或动态下发）
@Autowired SentinelRuleManager ruleManager;
ruleManager.addFlowRule("createOrder", 100, RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
ruleManager.addSlowCallDegradeRule("payOrder", 1000, 0.5, 10);
```

---

### eagle-seata-starter — 分布式事务

```java
// AT 模式：在 @GlobalTransactional 方法上自动开启全局事务（Seata 原生注解）

// 编程式全局事务
@Autowired GlobalTransactionTemplate txTemplate;
OrderResult result = txTemplate.execute("createOrder", () -> {
    inventoryService.deduct(req.getProductId(), req.getQuantity());
    return orderService.create(req);
});

// TCC 模式：实现 TccAction 接口，配合 @TwoPhaseBusinessAction
```

---

### eagle-elasticsearch-starter — 全文搜索

```java
// 流式 DSL 构建查询
NativeQuery query = EsQueryBuilder.<ProductDoc>builder()
    .multiMatch("手机", List.of("name", "description"))
    .term("category", "digital")
    .range("price", BigDecimal.valueOf(1000), BigDecimal.valueOf(5000))
    .highlight(List.of("name", "description"))
    .page(1, 20)
    .build();

// 通用 Repository
public class ProductRepository extends BaseElasticSearchRepository<ProductDoc> {
    public SearchHits<ProductDoc> searchProducts(String keyword) {
        return search(EsQueryBuilder.<ProductDoc>builder()
            .multiMatch(keyword, List.of("name")).build());
    }
}
```

---

### eagle-mybatis-starter — MyBatis-Plus 增强

```java
// 统一分页查询
public EaglePageResult<UserVO> listUsers(UserQuery req) {
    LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
    QueryHelper.likeAny(req.getKeyword(), UserDO::getName, UserDO::getPhone);
    QueryHelper.dateBetween(wrapper, UserDO::getCreateTime, req.getStart(), req.getEnd());
    QueryHelper.conditionEq(wrapper, req.getStatus() != null, UserDO::getStatus, req.getStatus());

    EaglePageQuery page = new EaglePageQuery();
    page.setPageNum(req.getPageNum()).setPageSize(req.getPageSize()).addOrderDesc("create_time");
    return EaglePageResult.of(userMapper.selectPage(page.toPage(), wrapper));
}

// 继承基础 Service
@Service
public class UserServiceImpl extends EagleServiceImpl<UserMapper, UserDO> implements UserService {
    public UserDO getOrThrow(Long id) { return getByIdOrThrow(id); }
}
```

---

### eagle-payment-starter — 支付集成

```java
// 统一支付接口，渠道透明
@Autowired
@Qualifier("alipayPaymentGateway")  // 或 wechatPaymentGateway
private PaymentGateway paymentGateway;

PayResult result = paymentGateway.pay(PayRequest.builder()
    .outTradeNo(orderNo).amount(new BigDecimal("99.00"))
    .subject("商品名称").notifyUrl("https://example.com/notify").build());

// 统一回调：配置 eagle.payment.alipay.notify-path / wechat.notify-path 即可
// 回调成功后发布 PaymentNotifyEvent，业务方监听处理
@EventListener
public void onPayment(PaymentNotifyEvent event) { ... }
```

---

### eagle-websocket-starter — 实时推送

```java
// WebSocket 点对点推送
@Autowired WebSocketSessionManager wsManager;
wsManager.sendToUser(userId, "/queue/order-status", orderStatusDto);

// SSE 服务端推送
@Autowired SseEmitterManager sseManager;

@GetMapping(value = "/sse/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@PathVariable String userId) {
    return sseManager.connect(userId);
}
// 主动推送
sseManager.sendToUser(userId, "ORDER_UPDATE", orderDto);
sseManager.broadcast("ANNOUNCEMENT", announcementDto);
```

客户端 JS 示例：

```javascript
// WebSocket（STOMP）
const client = new Client({ brokerURL: 'ws://localhost/ws' });
client.subscribe('/user/queue/order-status', msg => { ... });

// SSE
const es = new EventSource('/sse/' + userId);
es.addEventListener('ORDER_UPDATE', e => console.log(JSON.parse(e.data)));
```

---

## 快速开始

### 环境要求

- **Java 25**（推荐使用 [SDKMAN](https://sdkman.io/) 或 [Eclipse Temurin](https://adoptium.net/) 安装）
- **Gradle 9.x**（项目未包含 Gradle Wrapper，需自行安装）
- **MySQL 8.0+**（生产/开发环境）或使用内置 H2（本地开发）

### 方式一：本地零依赖启动（H2 + Caffeine）

无需安装 MySQL、Redis、Nacos，使用内置 H2 数据库 + Caffeine 本地缓存立即运行：

```bash
# 1. 克隆项目
git clone https://gitee.com/sunshixiong/eleganteer-cloud.git
cd eleganteer-cloud

# 2. 构建
gradle build

# 3. 启动系统服务（默认 local profile，使用 H2 + Caffeine）
gradle :eagle-base-server:eagle-system-server:bootRun
```

服务启动后访问：

- **Swagger UI** — http://localhost/swagger-ui.html
- **OAuth2 Token 端点** — http://localhost/oauth2/token
- **API 文档** — http://localhost/v3/api-docs

> 默认管理员账号：`admin` / `123456`

### 方式二：Docker Compose 一键启动

```bash
# 1. 构建应用 JAR
gradle build

# 2. 启动全部服务（MySQL + Redis + Nacos + Gateway + System）
cd eagle-base-server
docker compose up -d
```

| 服务            | 端口   | 说明          |
|---------------|------|-------------|
| MySQL         | 3306 | 数据库         |
| Redis         | 6379 | 缓存          |
| Nacos         | 8848 | 注册中心 / 配置中心 |
| Gateway       | 8080 | API 网关入口    |
| System Server | 8082 | 系统服务        |

### 方式三：连接外部 MySQL + Redis

1. 复制环境变量模板：

```bash
cp .env.example .env
```

2. 编辑 `.env` 填入实际配置（`.env` 已在 `.gitignore` 中，不会提交）：

```properties
# MySQL（dev profile）
DB_HOST=localhost
DB_PORT=3306
DB_NAME=eagle
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=

# Nacos
NACOS_SERVER_ADDR=localhost:8848
NACOS_NAMESPACE=
NACOS_GROUP=DEFAULT_GROUP

# 微信小程序（可选）
WECHAT_MINI_APP_ID=
WECHAT_MINI_APP_SECRET=

# 短信配置（由 eagle-notification-starter 统一处理）
SMS_PROVIDER=hnsls
SMS_ACCESS_KEY_ID=
SMS_ACCESS_KEY_SECRET=
SMS_SIGN_NAME=
SMS_TEMPLATE_ID=

# 手拉手短信特有配置
HNSLS_SMS_USERNAME=
HNSLS_SMS_PASSWORD=
HNSLS_SMS_CONTENT_TEMPLATE=您的验证码是{code}，5分钟内有效。
HNSLS_SMS_SEND_URL=https://xapi.hnsls.com.cn/eums/sms/utf8/send.do
HNSLS_SMS_CHARSET=UTF-8
```

3. 以 dev profile 启动：

```bash
gradle :eagle-base-server:eagle-system-server:bootRun --args='--spring.profiles.active=dev'
```

### 启动网关（可选）

本地开发可直接访问系统服务，如需网关路由：

```bash
# local profile（无需 Nacos）
gradle :eagle-base-server:eagle-gateway-server:bootRun
```

网关地址：http://localhost:8080

## 配置说明

### 环境变量（.env）

项目使用 `.env` 文件管理本地敏感配置，避免将密码、密钥等信息提交到版本库。

**文件说明：**

| 文件             | 用途                | 是否提交 Git              |
|----------------|-------------------|-----------------------|
| `.env.example` | 变量模板，列出所有可用变量及默认值 | ✅ 提交（安全占位值）           |
| `.env`         | 本地实际配置，填入真实密码/密钥  | ❌ 已在 `.gitignore` 中忽略 |

**初始化步骤：**

```bash
cp .env.example .env   # 首次克隆后执行一次
```

**完整变量列表：**

```properties
# ── MySQL（dev profile 使用）────────────────────────
DB_HOST=localhost
DB_PORT=3306
DB_NAME=eagle
DB_USERNAME=root
DB_PASSWORD=

# ── Redis ────────────────────────────────────────────
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=

# ── H2（local profile 使用）─────────────────────────
H2_DB_PATH=./db/eagle
H2_DB_USERNAME=root
H2_DB_PASSWORD=123456

# ── 微信小程序（可选）────────────────────────────────
WECHAT_MINI_APP_ID=
WECHAT_MINI_APP_SECRET=

# ── 微信 PC 扫码登录（可选）──────────────────────────
WECHAT_WEB_APP_ID=
WECHAT_WEB_APP_SECRET=
WECHAT_WEB_REDIRECT_URI=http://localhost/login/wechat/pc/callback

# ── 微信 H5 公众号登录（可选）────────────────────────
WECHAT_MP_APP_ID=
WECHAT_MP_APP_SECRET=
WECHAT_MP_REDIRECT_URI=http://localhost/login/wechat/h5/callback

# ── 短信配置（由 eagle-notification-starter 统一处理）────────
SMS_PROVIDER=hnsls
SMS_ACCESS_KEY_ID=
SMS_ACCESS_KEY_SECRET=
SMS_SIGN_NAME=
SMS_TEMPLATE_ID=

# ── 手拉手短信特有配置 ───────────────────────────────
HNSLS_SMS_USERNAME=
HNSLS_SMS_PASSWORD=
HNSLS_SMS_CONTENT_TEMPLATE=您的验证码是{code}，5分钟内有效。
HNSLS_SMS_SEND_URL=https://xapi.hnsls.com.cn/eums/sms/utf8/send.do
HNSLS_SMS_CHARSET=UTF-8

# ── 管理员账户（可选，默认 admin/123456）──────────────
EAGLE_ADMIN_PASSWORD=123456

# ── 基础设施服务（可选）──────────────────────────────
NACOS_SERVER_ADDR=localhost:8848
NACOS_NAMESPACE=
NACOS_GROUP=DEFAULT_GROUP
SENTINEL_DASHBOARD=localhost:8858
```

> **注意：** `.env` 仅在本地开发时由 IDE / IntelliJ 的 [EnvFile 插件](https://plugins.jetbrains.com/plugin/7861-envfile)
> 或 `--env-file` 参数加载。Docker Compose 会自动读取同目录下的 `.env` 文件。生产环境通过 Kubernetes Secret /
> 容器编排平台的环境变量注入，
**不使用** `.env` 文件。

### Profile 配置

| Profile     | 数据库              | 缓存               | 注册中心       | 适用场景           |
|-------------|------------------|------------------|------------|----------------|
| `local`（默认） | H2 文件数据库         | Caffeine 本地缓存    | 禁用         | 本地开发、调试        |
| `dev`       | MySQL + HikariCP | Redis (Redisson) | Nacos      | 开发环境           |
| `docker`    | MySQL (容器)       | Redis (容器)       | Nacos (容器) | Docker Compose |
| `prod`      | MySQL            | Redis            | Nacos      | 生产环境           |

### OAuth2 认证

项目内置 OAuth2 授权服务器，默认注册客户端：

| 配置项               | 值                                                                               |
|-------------------|---------------------------------------------------------------------------------|
| Client ID         | `eagleWeb`                                                                      |
| 授权类型              | `authorization_code` (PKCE), `refresh_token`, `wechat_mini_program`, `sms_code` |
| PKCE              | 必须启用（公开客户端）                                                                     |
| Access Token 有效期  | 1 小时                                                                            |
| Refresh Token 有效期 | 30 天                                                                            |
| Scopes            | `openid`, `profile`, `email`, `address`, `phone`                                |

### 第三方登录（可选配置）

通过环境变量或 `application.yml` 配置：

- **微信小程序** — `WECHAT_MINI_APP_ID` / `WECHAT_MINI_APP_SECRET`
- **微信 PC 扫码** — `WECHAT_WEB_APP_ID` / `WECHAT_WEB_APP_SECRET`
- **微信 H5 公众号** — `WECHAT_MP_APP_ID` / `WECHAT_MP_APP_SECRET`
- **短信服务** — `SMS_PROVIDER`（aliyun/tencent/hnsls）/ `SMS_ACCESS_KEY_ID` / `SMS_ACCESS_KEY_SECRET` /
  `SMS_SIGN_NAME` / `SMS_TEMPLATE_ID`；手拉手额外需要 `HNSLS_SMS_USERNAME` / `HNSLS_SMS_PASSWORD`

## 常用命令

```bash
# 构建
gradle build                    # 构建全部
gradle clean build              # 清理后构建

# 测试
gradle test                     # 运行全部测试
gradle :eagle-base-server:eagle-system-server:test    # 运行指定模块测试
gradle :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass"           # 单个测试类
gradle :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass.testMethod" # 单个测试方法

# 架构验证（PR 前必须通过）
gradle :eagle-base-server:eagle-system-server:test --tests "*.ModulithArchitectureTest"

# 运行服务
gradle :eagle-base-server:eagle-system-server:bootRun   # 系统服务
gradle :eagle-base-server:eagle-gateway-server:bootRun   # 网关服务

# GraalVM Native Image
gradle nativeCompile
```

## API 网关

网关提供统一入口，**仅做路由 + 流量治理**；所有 JWT 验签与用户身份提取由下游 `eagle-resource-server-starter` 负责。

核心功能：

- **请求增强** — `RequestEnrichmentGlobalFilter` 注入 `X-Request-Id`（UUID v4，全链路追溯）+ `X-Real-IP`（从 XFF /
  RemoteAddress 提取）
- **请求日志** — 记录请求方法、路径、状态码、耗时、客户端 IP、链路 Trace ID
- **Sentinel 限流** — 网关流控规则 + 降级规则，通过 sentinel-datasource-nacos 持久化到 Nacos，重启不丢规则
- **Seata 事务透传** — 自动传递分布式事务 XID
- **路由级超时与重试** — `routes[].metadata` 定义路由级超时；GET 幂等接口配 Retry filter 指数退避重试
- **WebSocket 路由** — 通过 `lb:ws://` 转发 STOMP（如 `/ws-stomp/**`）
- **统一错误响应** — `GatewayWebExceptionHandler` 输出 `ErrorResult` JSON，含 requestId，前端可与响应头 `X-Request-Id` 对照
- **API 文档聚合** — 通过 Nacos 动态发现各服务的 OpenAPI 文档

详细配置与运维场景见 `eagle-services/eagle-gateway-service/README.md`。

## 资源服务器依赖选择

`eagle-resource-server-starter` 只提供 OAuth2 JWT 资源服务器能力，不再替业务应用选择
Servlet/WebMVC 或 WebFlux。每个资源服务器必须在自己的 `build.gradle` 中显式选择 Web
栈、接口文档栈和数据访问栈。

### Servlet / Spring MVC 资源服务器

适用于传统阻塞式 Controller、JPA/Hibernate、Thymeleaf、Servlet Filter 等场景。

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-resource-server-starter')

    // Web 模块：Servlet / Spring MVC
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'

    // API 文档：通用 OpenAPI 配置 + WebMVC 版 SpringDoc UI
    implementation project(':eagle-starter:eagle-openapi-starter')
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui'

    // 数据库：阻塞式 JPA / JDBC
    implementation project(':eagle-starter:eagle-data-jpa-starter')

    // 可选：缓存、数据权限、租户、链路追踪等平台能力
    implementation project(':eagle-starter:eagle-redis-starter')
    implementation project(':eagle-starter:eagle-row-security-starter')
    implementation project(':eagle-starter:eagle-tenant-starter')
    implementation project(':eagle-starter:eagle-tracing-starter')
}
```

### Reactive / WebFlux 资源服务器

适用于响应式 Controller、`Mono` / `Flux`、R2DBC、非阻塞 HTTP 调用等场景。不要同时引入
`spring-boot-starter-webmvc` 和 `spring-boot-starter-webflux`。

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-resource-server-starter')

    // Web 模块：Reactive / WebFlux
    implementation project(':eagle-starter:eagle-webflux-starter')

    // API 文档：通用 OpenAPI 配置 + WebFlux 版 SpringDoc UI
    implementation project(':eagle-starter:eagle-openapi-starter')
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui'

    // 数据库：响应式 PostgreSQL / R2DBC
    implementation project(':eagle-starter:eagle-data-r2dbc-starter')

    // 可选：缓存、租户、链路追踪等平台能力
    implementation project(':eagle-starter:eagle-redis-starter')
    implementation project(':eagle-starter:eagle-tenant-starter')
    implementation project(':eagle-starter:eagle-tracing-starter')
}
```

WebFlux + R2DBC PostgreSQL 的基础配置：

```yaml
spring:
  r2dbc:
    url: r2dbc:pool:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:eagle}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:}
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${AUTH_SERVER_JWK_SET_URI:http://localhost/oauth2/jwks}

eagle:
  resource-server:
    permit-paths:
      - /public/**
      - /actuator/health
      - /actuator/info
```

如果应用只需要鉴权但不暴露 HTTP 接口，不要引入 WebMVC/WebFlux starter；仅引入
`eagle-resource-server-starter` 和实际需要的基础设施 starter。

## 开发指南

### 新增业务模块

1. 在 `eagle-system-server` 对应包下创建模块目录，遵循 `interfaces / application / domain / infrastructure` 分层
2. 在模块根目录创建 `package-info.java`，声明 `@ApplicationModule` 和 `allowedDependencies`
3. 为需要暴露的子包添加 `@NamedInterface`
4. 运行 `gradle test --tests "*.ModulithArchitectureTest"` 验证模块边界

### 新增错误码

在对应域的 ErrorCode 枚举中添加常量，并在 i18n 消息文件中添加翻译：

```java
ORDER_ITEM_LIMIT_EXCEEDED(30005, "error.order.item_limit", "订单项超出上限");
```

使用时通过工厂方法抛出类型化异常：

```java
throw OrderErrorCode.ORDER_ITEM_LIMIT_EXCEEDED.toDomainException();    // → 400
throw OrderErrorCode.ORDER_NOT_FOUND.toNotFoundException();             // → 404
throw OrderErrorCode.ORDER_DUPLICATE.toConflictException();             // → 409
throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(cause);        // → 500
```

### 编码规范

项目编码规范定义在 `.claude/rules/` 目录下，涵盖命名、架构分层、RESTful
API、日志、安全、并发、测试、代码风格、异常处理、数据库、配置注入、模块治理、消息、多租户、数据权限、OpenAPI、i18n、容错弹性、事件驱动、Git
工作流、性能、部署、审查清单、文件存储、定时任务、迁移、依赖管理等 30 项规范。

## Agent Plugin（AI 辅助编码，支持 Claude Code + Codex CLI）

本仓库内置 **Eagle Cloud Agent Plugin**——同时面向 **Claude Code** 与 **Codex CLI** 的开发插件，包含 30 份开发规范（rules）、6
个项目命令（commands）、29 个 starter / 编排 skill，帮助 AI 在编写 Eagle 平台代码时自动遵循架构约定。

### Claude Code 安装

在 Claude Code 会话中：

```
/plugin marketplace add https://gitee.com/sunjones/eagle-cloud.git
/plugin install eagle-cloud@eagle-cloud
```

### Codex CLI 安装

在 shell 中：

```bash
codex plugin marketplace add https://gitee.com/sunjones/eagle-cloud.git
codex plugin install eagle-cloud@eagle-cloud
```

> 两者首条命令每台机器只需运行一次（marketplace 全局生效）；第二条命令每个项目运行一次。安装后重启会话即可加载。

### 验证

会话内输入 `/check-arch` 应被识别为本插件提供的命令；写 `SecurityUtils.getCurrentUserId()` 一类 Eagle starter API 时，AI
会自动加载对应 skill（如 `eagle-resource-server`）。

详细使用说明见 [`agent-plugin/README.md`](agent-plugin/README.md) 与 [`agent-plugin/USAGE.md`](agent-plugin/USAGE.md)。

## License

[Apache License 2.0](LICENSE)
