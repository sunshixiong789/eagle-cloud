# eagle-feign-starter

基于 Spring Cloud OpenFeign 的远程调用封装模块，提供开箱即用的 JWT 透传、多租户隔离、Seata 分布式事务、错误统一转换和可配置超时等能力。

## 目录

- [模块概述](#模块概述)
- [引入依赖](#引入依赖)
- [自动配置说明](#自动配置说明)
- [配置参考](#配置参考)
- [使用规范](#使用规范)
  - [FeignClient 定义规范](#feigclient-定义规范)
  - [禁止事项](#禁止事项)
- [功能使用说明](#功能使用说明)
  - [Authorization 透传](#authorization-透传)
  - [多租户 ID 透传](#多租户-id-透传)
  - [错误解码器](#错误解码器)
  - [超时配置](#超时配置)
  - [Seata XID 透传](#seata-xid-透传)
  - [日志级别配置](#日志级别配置)
- [常见问题](#常见问题)

---

## 模块概述

### 提供的能力

| 组件 | 说明 |
|------|------|
| `FeignAuthInterceptor` | 全局拦截器，透传 `Authorization`（JWT）和 `Accept-Language`（i18n） |
| `FeignErrorDecoder` | 将下游 HTTP 错误响应统一转换为 `AppException` 体系异常 |
| `FeignTenantInterceptor` | 透传 `X-Tenant-Id`（可选，依赖 `eagle-tenant-starter`） |
| `SeataXidRequestInterceptor` | 透传 Seata 分布式事务 XID（可选，依赖 `seata-spring-boot-starter`） |
| `feign.Request.Options` | 全局连接 / 读取超时，可通过配置文件覆盖 |
| `feign.Logger.Level` | 全局日志级别，可通过配置文件覆盖 |

### 依赖关系

```
eagle-feign-starter
├── eagle-common-starter              ← 异常体系（AppException / ErrorCode）
├── spring-cloud-starter-openfeign   ← OpenFeign 核心（含 B3 链路追踪集成）
├── spring-boot-starter-webmvc       ← 从 RequestContextHolder 提取当前请求
├── eagle-tenant-starter             ← 可选，compileOnly（租户 ID 透传）
└── seata-spring-boot-starter        ← 可选，compileOnly（Seata XID 透传）
```

> B3 链路追踪头（`X-B3-TraceId` 等）由 Spring Cloud OpenFeign + Micrometer Tracing 自动注入，无需手动处理。

---

## 引入依赖

在需要远程调用其他服务的模块 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':eagle-starter:eagle-feign-starter')
}
```

引入后自动生效，无需额外 `@EnableFeignClients` 以外的配置。

在服务启动类上声明 FeignClient 扫描路径：

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.eagle.yourservice")
public class YourServiceApplication { ... }
```

---

## 自动配置说明

模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册 `EagleFeignAutoConfiguration`，在 `FeignAutoConfiguration` 之后加载。

### 注册的 Bean

| Bean | 类型 | 条件 | 说明 |
|------|------|------|------|
| `feignAuthInterceptor` | `FeignAuthInterceptor` | `@ConditionalOnMissingBean` | 透传 Authorization / Accept-Language |
| `feignErrorDecoder` | `FeignErrorDecoder` | `@ConditionalOnMissingBean` | HTTP 错误 → AppException |
| `feignLoggerLevel` | `Logger.Level` | `@ConditionalOnMissingBean(Logger.Level.class)` | 全局日志级别（默认 BASIC）|
| `feignRequestOptions` | `Request.Options` | `@ConditionalOnMissingBean(Request.Options.class)` | 全局超时配置 |
| `seataXidRequestInterceptor` | `SeataXidRequestInterceptor` | Seata 在类路径时 | Seata XID 透传 |
| `feignTenantInterceptor` | `FeignTenantInterceptor` | eagle-tenant-starter 在类路径时 | 租户 ID 透传 |

**Bean 覆盖：** 所有 Bean 均支持 `@ConditionalOnMissingBean`，消费方可声明同类型 Bean 覆盖默认实现：

```java
// 覆盖错误解码器（如需自定义错误处理逻辑）
@Bean
public FeignErrorDecoder feignErrorDecoder() {
    return new MyCustomFeignErrorDecoder();
}
```

---

## 配置参考

所有配置项均有合理默认值，未配置时直接使用。

```yaml
eagle:
  feign:
    log-level: BASIC          # 日志级别：NONE / BASIC / HEADERS / FULL（默认 BASIC）
    connect-timeout: 2000     # 连接超时（毫秒，默认 2 秒）
    read-timeout: 5000        # 读取超时（毫秒，默认 5 秒）
```

### 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `eagle.feign.log-level` | `feign.Logger.Level` | `BASIC` | 全局 Feign 日志级别，影响所有未单独配置的 FeignClient |
| `eagle.feign.connect-timeout` | `int` | `2000` | TCP 连接建立超时（毫秒）。超时后抛出 `RetryableException` |
| `eagle.feign.read-timeout` | `int` | `5000` | 等待响应的读取超时（毫秒）。超时后抛出 `RetryableException` |

### 日志级别说明

| 级别 | 内容 | 推荐环境 |
|------|------|----------|
| `NONE` | 无日志 | 生产（性能最优）|
| `BASIC` | 请求方法、URL、响应状态码、耗时 | 生产（默认）|
| `HEADERS` | BASIC + 请求/响应头 | 集成测试 |
| `FULL` | 全量请求/响应体 | 本地调试 |

开启 Feign 日志还需配置日志框架的输出级别（`BASIC` 及以上均使用 DEBUG 级别输出）：

```yaml
logging:
  level:
    com.eagle.yourservice.infrastructure.remote: DEBUG
```

---

## 使用规范

### FeignClient 定义规范

**1. 定义位置**

`@FeignClient` 接口定义在调用方服务的 `infrastructure/remote/` 包中，遵循 DDD 六边形架构：

```
your-service/
└── infrastructure/
    └── remote/
        ├── InventoryFeignClient.java   ← FeignClient 接口
        └── dto/                        ← 请求 / 响应 DTO（仅此包使用）
```

**2. 接口定义**

```java
// infrastructure/remote/InventoryFeignClient.java
@FeignClient(name = "eagle-inventory-server", path = "/api/inventory")
public interface InventoryFeignClient {

    @GetMapping("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);

    @PutMapping("/{productId}/deduct")
    void deductStock(@PathVariable Long productId, @RequestBody DeductStockRequest request);
}
```

**规范要点：**
- `name` 使用服务注册名（与 Nacos / Eureka 服务名一致）
- `path` 抽取公共前缀，接口方法只写差异路径
- 使用 `@PathVariable`、`@RequestParam`、`@RequestBody` 明确参数绑定方式
- 不得在接口上标注 `@Transactional`

**3. 注入使用**

```java
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final InventoryFeignClient inventoryFeignClient;

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(CreateOrderRequest request) {
        // 直接调用，异常由全局处理器统一处理
        StockResponse stock = inventoryFeignClient.getStock(request.getProductId());
        if (stock.getAvailable() < request.getQuantity()) {
            throw OrderErrorCode.INSUFFICIENT_STOCK.toDomainException();
        }
        orderRepository.save(Order.create(request));
        inventoryFeignClient.deductStock(request.getProductId(),
                new DeductStockRequest(request.getQuantity()));
    }
}
```

### 禁止事项

```java
// ❌ 禁止在 FeignClient 中定义 fallback（失败应上抛，降级由调用方决定）
@FeignClient(name = "eagle-inventory", fallback = InventoryFallback.class)
public interface InventoryFeignClient { ... }

// ❌ 禁止手动 try-catch Feign 异常（全局处理器统一处理）
try {
    inventoryFeignClient.getStock(productId);
} catch (FeignException e) {
    return null;
}

// ❌ 禁止在 FeignClient 上加 @Transactional（远程调用不应参与本地事务）
@FeignClient(...)
@Transactional
public interface InventoryFeignClient { ... }

// ❌ 禁止使用 RestTemplate / WebClient 替代 FeignClient
restTemplate.getForObject("http://eagle-inventory/api/...", StockResponse.class);
```

---

## 功能使用说明

### Authorization 透传

`FeignAuthInterceptor` 自动将当前 HTTP 请求中的 `Authorization` Header（Bearer JWT Token）透传到所有下游 Feign 调用，**无需任何额外配置**。

```
用户请求 → Gateway → eagle-order-server → [FeignAuthInterceptor] → eagle-inventory-server
           Authorization: Bearer xxx  ←透传→  Authorization: Bearer xxx
```

同时透传 `Accept-Language`，确保下游服务返回相同语言的错误消息。

**在异步 / 定时任务上下文中：**

`RequestContextHolder` 无法获取当前 HTTP 请求（没有 Servlet 上下文），`FeignAuthInterceptor` 会跳过 Header 透传。这些场景通常使用服务账号（Client Credentials）认证，由各服务自行处理鉴权：

```java
// ✅ 定时任务中需要鉴权时，手动设置 Token
@Scheduled(cron = "0 0 * * * *")
public void syncDailyReport() {
    String serviceToken = tokenService.getClientCredentialsToken();
    // 通过 RequestTemplate 手动设置，或通过独立的 Feign configuration 处理
    reportFeignClient.generateReport(...);
}
```

---

### 多租户 ID 透传

**条件：** 引入 `eagle-tenant-starter` 后自动激活，无需额外配置。

`FeignTenantInterceptor` 将当前线程的租户 ID（`TenantContextHolder.getTenantId()`）透传为 `X-Tenant-Id` 请求头：

```yaml
# 消费方开启多租户时，feign 调用自动携带租户上下文
eagle:
  tenant:
    enabled: true
    mode: column
```

```
请求 → 服务A（tenant_id=100）→ [FeignTenantInterceptor] → 服务B
                                  X-Tenant-Id: 100 →
                                                      服务B 的 TenantIdFilter 接收并写入上下文
```

若消费方未引入 `eagle-tenant-starter`，`FeignTenantInterceptor` Bean 不会被注册，`X-Tenant-Id` 不会被注入。

---

### 错误解码器

`FeignErrorDecoder` 将下游 HTTP 错误响应自动转换为项目异常体系，**调用方无需手动 try-catch**：

| 下游状态码 | 转换结果 | HTTP 语义 |
|-----------|----------|-----------|
| `404` | `NotFoundException`（`ExternalErrorCode.EXTERNAL_SERVICE_ERROR`） | 下游资源不存在 |
| `400` | `DomainException` | 下游参数校验失败 |
| `409` | `DomainException` | 下游业务冲突 |
| `403` | `ServiceException` | 下游无权限 |
| `429` | `ServiceException` | 下游限流 |
| 其他 | `ServiceException` | 未知下游错误 |

转换后的异常会被全局异常处理器（`GlobalExceptionHandler`）捕获，向调用方返回标准错误响应：

```json
{
  "timestamp": "2026-04-29T10:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "下游服务调用失败: InventoryFeignClient#getStock(Long), status=503",
  "path": "/api/orders",
  "errorCode": 15001
}
```

**超时异常处理：**

Feign 连接/读取超时抛出 `feign.RetryableException`，未被 `FeignErrorDecoder` 捕获，会以 `ServiceException`（500）的形式被全局处理器兜底处理：

```java
// 可通过声明额外的 ErrorDecoder 专门处理超时
@Bean
public ErrorDecoder feignErrorDecoder() {
    return (methodKey, response) -> { ... };
}
```

---

### 超时配置

全局超时通过 `eagle.feign.*` 配置，适用于所有未单独配置的 FeignClient。

**全局配置：**

```yaml
eagle:
  feign:
    connect-timeout: 2000   # 默认 2 秒
    read-timeout: 5000      # 默认 5 秒
```

**按服务覆盖（Spring Cloud OpenFeign 原生支持）：**

部分下游服务响应较慢（如报表生成），可在 `application.yml` 按服务名单独配置：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          eagle-report-server:           # 服务名
            connect-timeout: 3000
            read-timeout: 30000          # 报表生成允许最长 30 秒
          eagle-inventory-server:
            connect-timeout: 1000
            read-timeout: 3000
```

> 按服务配置优先级高于 `eagle.feign.*` 全局配置，也高于 `feign.Request.Options` Bean。

**通过自定义 Configuration 覆盖（针对单个 FeignClient）：**

```java
// 单独的超时配置类（不要加 @Configuration，否则变为全局）
class SlowServiceFeignConfig {
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(3000, TimeUnit.MILLISECONDS, 60000, TimeUnit.MILLISECONDS, true);
    }
}

@FeignClient(name = "eagle-slow-server", configuration = SlowServiceFeignConfig.class)
public interface SlowServiceFeignClient { ... }
```

---

### Seata XID 透传

**条件：** 引入 `seata-spring-boot-starter` 后自动激活，无需额外配置。

`SeataXidRequestInterceptor` 将当前 Seata 分布式事务 XID（`RootContext.getXID()`）透传为 `TX_XID` 请求头，使下游服务能加入同一全局事务：

```
服务A（@GlobalTransactional）→ [SeataXidRequestInterceptor] → 服务B
                                TX_XID: 10.20.30.40:8091:...  →
                                                               服务B Seata 拦截器自动绑定
```

使用方式：在调用入口加 `@GlobalTransactional`，下游服务均可自动加入全局事务：

```java
@GlobalTransactional
@Transactional(rollbackFor = Exception.class)
public void placeOrder(PlaceOrderRequest request) {
    // Seata 全局事务开始，XID 写入 RootContext
    inventoryFeignClient.deductStock(...);   // TX_XID 透传 → 库存服务加入事务
    paymentFeignClient.chargeAccount(...);   // TX_XID 透传 → 支付服务加入事务
}
```

若未引入 `seata-spring-boot-starter`，`SeataXidRequestInterceptor` Bean 不会注册，无任何影响。

---

### 日志级别配置

全局日志级别通过 `eagle.feign.log-level` 配置（默认 `BASIC`）：

```yaml
eagle:
  feign:
    log-level: FULL   # 本地调试时开启全量日志
```

若需对特定 FeignClient 单独配置日志级别：

```java
class VerboseFeignConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

@FeignClient(name = "eagle-debug-server", configuration = VerboseFeignConfig.class)
public interface DebugFeignClient { ... }
```

同时在 `application.yml` 配置对应包的 logger 级别（Feign 日志使用 DEBUG 输出）：

```yaml
logging:
  level:
    com.eagle.yourservice.infrastructure.remote: DEBUG
```

---

## 常见问题

**Q: 下游服务报 401，但我的接口已登录，为什么 Authorization 没有透传？**

A: 检查以下两点：
1. 调用是否发生在异步线程 / 定时任务中（无 `RequestContextHolder`，Token 无法获取）
2. 下游服务是否配置了 OAuth2 资源服务器校验，确认 Token 格式是否被接受

---

**Q: Feign 调用超时了，但错误信息不够友好，能自定义吗？**

A: 覆盖 `FeignErrorDecoder`，对 `RetryableException` 做单独处理：

```java
@Bean
public ErrorDecoder feignErrorDecoder() {
    ErrorDecoder defaultDecoder = new FeignErrorDecoder();
    return (methodKey, response) -> defaultDecoder.decode(methodKey, response);
}

// 同时注册 ErrorDecoder.Default 之外的超时处理
@Bean
public Retryer feignRetryer() {
    return Retryer.NEVER_RETRY;   // 禁止默认重试，让超时直接上抛
}
```

---

**Q: 多个 FeignClient 调用同一服务，能不能统一配置超时？**

A: 可以，在 `application.yml` 按服务名配置一次即可，对该服务的所有 FeignClient 均生效：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          eagle-inventory-server:
            read-timeout: 3000
```

---

**Q: 如何在单元测试中 mock FeignClient？**

A: 直接用 `@MockBean` 或 `@Mock`，不需要真实网络调用：

```java
@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private InventoryFeignClient inventoryFeignClient;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    @Test
    void shouldThrowWhenStockInsufficient() {
        when(inventoryFeignClient.getStock(anyLong()))
                .thenReturn(new StockResponse(0L));

        assertThrows(DomainException.class,
                () -> orderApplicationService.createOrder(request));
    }
}
```

---

**Q: Seata XID 透传失败，下游服务没有加入全局事务？**

A: 检查以下几点：
1. 下游服务是否也引入了 `seata-spring-boot-starter` 并正确配置 Seata Server 地址
2. 下游服务的 Seata `@GlobalTransactional` 或 `DataSourceProxy` 是否正常工作
3. 确认 `TX_XID` Header 在下游服务的请求日志中存在（开启 `HEADERS` 日志级别验证）

---

**Q: 能否让某个 FeignClient 不透传 Authorization（如调用公开接口）？**

A: 可以通过自定义 `RequestInterceptor` 实现空拦截器，并在该 FeignClient 的 `configuration` 中覆盖：

```java
// 空拦截器：不透传任何 Header
class NoAuthFeignConfig {
    @Bean
    public RequestInterceptor feignAuthInterceptor() {
        return template -> {};   // 覆盖全局 FeignAuthInterceptor
    }
}

@FeignClient(name = "eagle-public-service", configuration = NoAuthFeignConfig.class)
public interface PublicApiFeignClient { ... }
```
