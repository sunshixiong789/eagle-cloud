# eagle-restclient-starter — 同步 RestClient + @HttpExchange

## 何时使用

- 服务间同步 RPC：使用 Spring HTTP Service Interface 声明式客户端
- 调用外部 HTTP API：使用自动配置后的 `RestClient.Builder`
- 需要自动透传当前用户 Token、语言、压测标记、租户 ID、Seata XID
- 需要将下游 `ErrorResult` 自动转换为 `AppException` 体系
- 需要统一连接超时、读取超时、服务发现负载均衡

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-restclient-starter')
```

无需 `@EnableFeignClients`。引入 starter 后，`RestClient.Builder`、`EagleRestServiceClientFactory`
和全局 `RestClientCustomizer` 自动生效。

```yaml
eagle:
  http-client:
    connect-timeout: 2s
    read-timeout: 5s
    error-handler-enabled: true
    buffer-content: true
    pressure-test-header-enabled: true
    propagated-headers:
      - Authorization
      - Accept-Language
      - X-Request-Id
      - X-Correlation-Id
```

## 核心能力

| 能力        | 默认行为                                                              |
|-----------|-------------------------------------------------------------------|
| Header 透传 | 从当前 Servlet 请求透传 `Authorization`、`Accept-Language` 等配置项           |
| 压测标记      | `PressureTestContext` 标记存在时透传 `X-Eagle-Gray: true`                |
| 租户透传      | 类路径存在 `eagle-tenant-starter` 时透传 `X-Tenant-Id`                    |
| Seata 透传  | 类路径存在 Seata 时透传 `TX_XID`                                          |
| 错误转换      | 4xx / 5xx 转为项目 `AppException`，提取下游 `ErrorResult.message`          |
| 服务发现      | 自动提供 `loadBalancedRestClientBuilder` 与 `createLoadBalancedClient` |
| 观测链路      | 复用 Spring Boot RestClient / Micrometer Observation 自动配置           |

## 声明式服务间调用

接口放在调用方服务的 `infrastructure/remote/` 包中：

```java
@HttpExchange("/api/v1/inventory")
public interface InventoryClient {

    @GetExchange("/{productId}/stock")
    StockResponse getStock(@PathVariable Long productId);

    @PostExchange("/lock")
    void lockStock(@RequestBody LockStockRequest request);

    @GetExchange("/items")
    Page<ItemResponse> findItems(@RequestParam int page,
                                 @RequestParam int size,
                                 @RequestParam String sort);
}
```

创建客户端 Bean：

```java
@Configuration(proxyBeanMethods = false)
class RemoteClientConfiguration {

    @Bean
    InventoryClient inventoryClient(EagleRestServiceClientFactory factory) {
        return factory.createLoadBalancedClient(InventoryClient.class, "eagle-inventory-service");
    }
}
```

业务代码直接注入接口：

```java
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final InventoryClient inventoryClient;

    public void create(CreateOrderRequest request) {
        StockResponse stock = inventoryClient.getStock(request.productId());
        inventoryClient.lockStock(new LockStockRequest(request.productId(), request.quantity()));
    }
}
```

## 外部 API 调用

```java
@Bean
RestClient wechatRestClient(RestClient.Builder builder) {
    return builder.clone()
            .baseUrl("https://api.weixin.qq.com")
            .defaultHeader("Accept", "application/json")
            .build();
}
```

公开接口或服务账号场景中，如不希望透传用户 Header，可单独创建 `RestClient.builder()`，
或声明自己的 `RestClientCustomizer` / `RestClient` Bean 覆盖。

## 错误转换

下游返回项目标准 `ErrorResult` 时自动提取 `message`：

| 下游 HTTP              | 抛出异常                |
|----------------------|---------------------|
| 400                  | `DomainException`   |
| 404                  | `NotFoundException` |
| 409                  | `ConflictException` |
| 403 / 429 / 5xx / 其他 | `ServiceException`  |

调用方通常无需捕获 HTTP 客户端异常，由全局异常处理器统一处理。

## 常见场景建议

- 分页查询：HTTP Service Interface 暂不使用 `Pageable` 自动展开，显式声明 `page`、`size`、`sort` 参数。
- 文件上传：使用 `RestClient` + `MultipartBodyBuilder`，不要塞进通用 RPC 接口。
- 流式下载：使用 `RestClient.exchange(...)` 自行处理响应流。
- 异步任务 / 定时任务：没有入站 Servlet 请求时不会透传用户 Token，按业务使用服务账号 Token。
- 事务边界：不要在远程客户端接口上加 `@Transactional`；跨服务强一致用 Seata，最终一致用领域事件 / MQ。
- 降级策略：失败默认上抛，降级由调用方应用服务按业务语义处理。

## 关联规则

- `.claude/rules/11-feign.md`（历史文件名，内容已迁移为 HTTP Client 规范）
- `.claude/rules/16-transaction-distributed.md` — XID 透传
- `.claude/rules/12-security.md` — JWT 透传
- `.claude/rules/17-tenant-permission.md` — 租户透传
