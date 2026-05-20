# eagle-monolith-service

Eagle 平台**单体启动模块**。复用 `eagle-system-service` 全部业务代码（认证 / 用户 / 角色 / 菜单 / 权限 / OAuth2
等），剥离 Nacos / 注册中心 / Gateway / Sentinel 等微服务专属基础设施，下游用户拿到即可作为单体应用一键启动。

## 定位

- **零基础设施**：无需 Nacos / Gateway / 网关代理，独立 JAR 启动即可
- **代码零拷贝**：通过 Gradle `implementation project(...)` 直接复用 `eagle-system-service` 业务代码
- **单实例可用**：内置 H2（local） + Caffeine（本地缓存），开箱即用
- **生产就绪**：切到 `prod` profile 即可使用 MySQL + Redis 真实依赖
- **二次扩展**：在 `com.eagle.monolith` 包下新增 Bean / Configuration 覆盖系统服务默认行为

## 启动类（EagleMonolithApplication）

```
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = {"com.eagle.system", "com.eagle.monolith"})
@EnableCaching
@ComponentScan(
    basePackages = {"com.eagle.system", "com.eagle.monolith"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = EagleSystemApplication.class)
)
@EntityScan(basePackages = {"com.eagle.system", "com.eagle.monolith"})
@EnableJpaRepositories(basePackages = {"com.eagle.system", "com.eagle.monolith"})
public class EagleMonolithApplication { ... }
```

**关键设计**：

- **必须排除 `EagleSystemApplication`**：该类带 `@SpringBootApplication`（=
  `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`），若被组件扫描会触发二次 auto-configuration
  加载，导致 `AsyncConfig` 等单例 bean 重复注册（典型表现："Only one AsyncConfigurer may exist"）
- **显式 `@EntityScan` / `@EnableJpaRepositories`**：因 `eagle-system-service` 以 `implementation` 范围引入 JPA
  starter，传递依赖在本模块编译期不可见，不能依赖默认包扫描
- **`@ComponentScan` 限定到业务包**：避免误扫 starter 内部 `@Component`（starter 应通过自身 `@AutoConfiguration` +
  条件装配生效）

## 依赖（build.gradle）

```groovy
implementation(project(':eagle-services:eagle-system-service')) {
    exclude group: 'com.alibaba.cloud',
            module: 'spring-cloud-starter-alibaba-nacos-discovery'   // 单体不需要注册中心
}
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

排除 Nacos 后，单体直接使用本地配置 + 本地数据源，**无需 Nacos 即可启动**。

## 启动

```bash
# 本机零依赖启动（H2 内存库 + Caffeine 本地缓存）
./gradlew :eagle-services:eagle-monolith-service:bootRun

# 生产模式（MySQL + Redis）
SPRING_PROFILES_ACTIVE=prod \
EAGLE_ADMIN_PASSWORD=your-strong-password \
SPRING_DATASOURCE_URL=jdbc:mysql://... \
./gradlew :eagle-services:eagle-monolith-service:bootRun
```

| 端点               | 默认地址                              | 说明     |
|------------------|-----------------------------------|--------|
| Swagger UI       | http://localhost/swagger-ui.html  | API 文档 |
| OAuth2 Token     | http://localhost/oauth2/token     | 令牌端点   |
| OAuth2 Authorize | http://localhost/oauth2/authorize | 授权码端点  |
| WebSocket（STOMP） | ws://localhost/ws-stomp           | 实时推送   |
| Actuator Health  | http://localhost/actuator/health  | 健康检查   |

## Profile

| Profile | 数据源       | 缓存       | 用途          |
|---------|-----------|----------|-------------|
| `local` | H2（内存）    | Caffeine | 本机快速验证 / 默认 |
| `prod`  | MySQL（外部） | Redis    | 生产 / 容器化部署  |

切换：`SPRING_PROFILES_ACTIVE=prod` 或 `--spring.profiles.active=prod`。

## 关键配置（前缀 `eagle.*`）

| 配置项                            | 默认                           | 说明                            |
|--------------------------------|------------------------------|-------------------------------|
| `eagle.admin.password`         | `localDev@2026`              | 初始管理员密码（生产必须通过环境变量改）          |
| `eagle.jwt.keystore-location`  | `classpath:jwt-keystore.p12` | JWT 签名 keystore（来自 system 服务） |
| `eagle.jwt.keystore-password`  | `eagle-jwt-dev-2026`         | keystore 密码                   |
| `eagle.oauth.default-client.*` | —                            | 默认 OAuth2 公开客户端（PKCE）         |
| `eagle.websocket.endpoint`     | `/ws-stomp`                  | STOMP 握手路径                    |
| `eagle.log.cleanup.cron`       | `0 0 2 * * ?`                | 审计日志每日清理                      |
| `spring.jpa.open-in-view`      | `false`                      | 关闭 OSIV，避免视图层延迟加载             |

完整字段见 `src/main/resources/application.yml` 与 `application-{local,prod}.yml`。

## 单体 vs 微服务取舍

| 维度      | 单体（本服务）              | 微服务（system + gateway）     |
|---------|----------------------|---------------------------|
| 部署      | 单 JAR / 单容器          | 至少 2 服务（system + gateway） |
| 注册中心    | 不需要                  | 需要 Nacos                  |
| 网关      | 不需要                  | 需要 eagle-gateway-service  |
| 限流 / 链路 | 业务内 Spring AOP 即可    | Sentinel + Zipkin         |
| 服务发现    | N/A                  | Nacos `lb://`             |
| 启动复杂度   | 极低                   | 中等                        |
| 适用规模    | 小型 SaaS / PoC / 私有部署 | 中大型团队 / 多业务域 / 高可用要求      |

业务代码完全相同 — 当规模增长时，可平滑切换到 `eagle-system-service` + `eagle-gateway-service` 部署模式，无需改业务代码。

## 二次扩展

在 `com.eagle.monolith` 下新增 `Configuration` / `Bean` 即可覆盖默认行为，例如：

- 自定义 `DataSource`（多数据源）
- 自定义 `CacheManager`（替换 Caffeine 为 Redis）
- 增加业务模块（在新包根 `package-info.java` 加 `@ApplicationModule`，并加入 `@EnableJpaRepositories` 扫描范围）

## 容器化

```dockerfile
FROM eclipse-temurin:25-jre
COPY build/libs/eagle-monolith-service-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
```

需注入的环境变量：`EAGLE_ADMIN_PASSWORD` / `EAGLE_JWT_KEYSTORE_PASSWORD` / `SPRING_DATASOURCE_*` / `SPRING_REDIS_*`。

## 注意事项

- **不要**重新引入 Nacos / Gateway 依赖 — 那会让单体退化为微服务节点
- **不要**在 `com.eagle.monolith` 下定义带 `@SpringBootApplication` 的类（会被组件扫描触发 auto-configuration 重复加载）
- **AsyncConfigurer 唯一性**：所有 `@Async` 公共线程池配置应在 `eagle-common-starter` 中统一声明，单体侧不要重复定义
- 生产部署务必切到 `prod` profile，并通过环境变量注入敏感字段
