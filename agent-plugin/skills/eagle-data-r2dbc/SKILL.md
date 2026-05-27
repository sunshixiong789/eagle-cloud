---
name: eagle-data-r2dbc
description: Use when building reactive WebFlux services with R2DBC in eagle-cloud — BaseR2dbcAggregateRoot / BaseR2dbcEntity base classes with auditing and optimistic locking, ReactiveAuditorAware auto-fill, R2dbcRepository usage, non-blocking database access
---

# eagle-data-r2dbc-starter — 响应式 R2DBC 数据访问

## 何时使用

- 服务使用 **Spring WebFlux**（反应式编程），需要非阻塞数据库访问
- 网关（`eagle-gateway-server`）等 WebFlux 服务的数据层
- 需要与 JPA 隔离的独立响应式聚合根

## 何时不要使用

- 普通 Servlet 业务服务 → 用 `eagle-data-jpa-starter`（JPA + Hibernate）
- 在 `@Transactional` 方法内已有 JPA Session → 禁止混用 R2DBC

## 依赖与启用

```gradle
implementation project(':eagle-starter:eagle-data-r2dbc-starter')
// 还需要 R2DBC 驱动，例如 MySQL：
runtimeOnly 'io.asyncer:r2dbc-mysql'
```

```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://${DB_HOST:127.0.0.1}:3306/${DB_NAME:eagle}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
  data:
    r2dbc:
      repositories:
        enabled: true
```

引入后自动开启 `@EnableR2dbcAuditing`，`ReactiveAuditorAware` 从 `ReactiveSecurityContextHolder` 自动填充审计字段。

## 核心基类

### `BaseR2dbcAggregateRoot<T>`

| 字段          | 类型             | 说明                       |
|-------------|----------------|--------------------------|
| `id`        | `Long`         | `@Id`，数据库自增              |
| `createBy`  | `Long`         | `@CreatedBy`，自动填充        |
| `updateBy`  | `Long`         | `@LastModifiedBy`，自动填充   |
| `createTime`| `LocalDateTime`| `@CreatedDate`，自动填充      |
| `updateTime`| `LocalDateTime`| `@LastModifiedDate`，自动填充 |
| `version`   | `Long`         | `@Version`，乐观锁，`save()` 自动比对 |

支持 `registerEvent(Object event)`，`R2dbcRepository.save()` 后自动发布至 `ApplicationEventPublisher`。

### `BaseR2dbcEntity`

同 `BaseR2dbcAggregateRoot` 但无事件能力——用于聚合内子实体（一对多嵌套场景极少用，R2DBC 不支持 Lazy Loading）。

## 最小示例

```java
// 1) 聚合根定义
@Table("t_notification")
@Getter
@NoArgsConstructor
public class Notification extends BaseR2dbcAggregateRoot<Notification> {

    private String title;
    private String content;
    private NotificationStatus status;

    public static Notification create(String title, String content) {
        Notification n = new Notification();
        n.title = title;
        n.content = content;
        n.status = NotificationStatus.PENDING;
        n.registerEvent(new NotificationCreatedEvent(title));
        return n;
    }

    public void send() {
        this.status = NotificationStatus.SENT;
        registerEvent(new NotificationSentEvent(getId()));
    }
}

// 2) Repository（继承 R2dbcRepository）
public interface NotificationRepository extends R2dbcRepository<Notification, Long> {

    Flux<Notification> findByStatus(NotificationStatus status);

    @Query("SELECT * FROM t_notification WHERE status = :status LIMIT :limit")
    Flux<Notification> findPendingBatch(@Param("status") String status, @Param("limit") int limit);
}

// 3) 应用服务（全链路非阻塞）
@Service
@RequiredArgsConstructor
public class NotificationApplicationService {

    private final NotificationRepository repository;

    public Mono<Notification> create(String title, String content) {
        return repository.save(Notification.create(title, content));
    }

    public Flux<Notification> findPending() {
        return repository.findByStatus(NotificationStatus.PENDING);
    }
}

// 4) Controller（WebFlux 端点）
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest req) {
        return service.create(req.title(), req.content())
                .map(notificationMapper::toResponse);
    }
}
```

## 事务

```java
// R2DBC 事务用 @Transactional（需要 R2dbcTransactionManager）
@Transactional
public Mono<Void> atomicUpdate(Long id, String newTitle) {
    return repository.findById(id)
            .flatMap(n -> {
                // 修改...
                return repository.save(n);
            })
            .then();
}
```

## 常见错误

- ❌ 在 WebFlux 路径中调用 JPA / JDBC（阻塞 IO 导致 Netty EventLoop 死锁）→ ✅ 全链路用 R2DBC
- ❌ R2DBC 实体写 `@OneToMany` / `@ManyToOne` → ✅ R2DBC 不支持 JPA 关联，跨聚合只存 ID
- ❌ `@Transactional` 用 JPA `JpaTransactionManager` → ✅ 需要 `R2dbcTransactionManager`
- ❌ `save()` 后事件没有发布 → ✅ 继承 `BaseR2dbcAggregateRoot` 并调用 `registerEvent()`

## 关联规则

- `.claude/rules/03-architecture.md` — 聚合根规范（R2DBC 版同样适用）
- `.claude/rules/06-database.md` — 跨聚合只存 ID，无物理 FK
- `.claude/rules/08-concurrency.md` — 乐观锁与事务边界
