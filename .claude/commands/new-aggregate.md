---
description: 在指定模块中创建聚合根 + Repository + ErrorCode + ApplicationService + Controller + DTO 全栈骨架
argument-hint: "<service>:<module>:<AggregateName>，例 eagle-system-server:order:Order"
---

# /new-aggregate — 创建聚合根全栈骨架

为指定模块创建一个完整的聚合根，含：
- Domain：聚合根 + Repository + ErrorCode + 领域事件
- Application：ApplicationService + Mapper（MapStruct）
- Infrastructure：JPA Repository 实现
- Web：Controller + Request DTO + Response DTO

严格遵循 `03-architecture.md` / `01-naming.md` / `02-code-style.md`。

## 输入解析

`$ARGUMENTS` 格式：`<service>:<module>:<AggregateName>`

- service：`eagle-system-server`
- module：`order`
- AggregateName（UpperCamelCase）：`Order`

若缺省，交互询问。

## 执行步骤

### 1. 聚合根类

```java
package com.eagle.{service}.{module}.domain.model.aggregate;

@Entity
@Table(name = "t_{module}_{aggregate_snake}", indexes = {
    @Index(name = "idx_tenant_status", columnList = "tenant_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class {Aggregate} extends BaseAggregateRoot<{Aggregate}> {

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 32, unique = true)
    private String {aggregate}No;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private {Aggregate}Status status = {Aggregate}Status.CREATED;

    /** 静态工厂方法 */
    public static {Aggregate} create(String {aggregate}No /*, ProfileHints hints*/) {
        {Aggregate} agg = new {Aggregate}();
        agg.{aggregate}No = {aggregate}No;
        agg.tenantId = TenantContextHolder.getCurrentTenantId();
        // agg.profileHints = hints;  // @Transient 用于 @PostPersist
        return agg;
    }

    @PostPersist
    void onPostPersist() {
        // registerEvent(new {Aggregate}CreatedEvent(getId(), {aggregate}No, ...));
    }

    /** 业务方法（替代 setter）*/
    public void cancel() {
        if (status != {Aggregate}Status.CREATED) {
            throw {Aggregate}ErrorCode.INVALID_STATUS_TRANSITION.toDomainException();
        }
        this.status = {Aggregate}Status.CANCELLED;
        registerEvent(new {Aggregate}CancelledEvent(getId()));
    }
}
```

### 2. 状态枚举

```java
package com.eagle.{service}.{module}.domain.model.enums;

public enum {Aggregate}Status {
    CREATED, CONFIRMED, CANCELLED, COMPLETED
}
```

### 3. ErrorCode

```java
package com.eagle.{service}.{module}.web.exception;

@Getter
@RequiredArgsConstructor
public enum {Aggregate}ErrorCode implements ErrorCode {
    {AGG}_NOT_FOUND(/* 起始码 */, "error.{module}.{aggregate}.not_found", "{Aggregate} 不存在"),
    INVALID_STATUS_TRANSITION(..., "error.{module}.{aggregate}.invalid_status", "状态变更不合法");

    private final int code;
    private final String i18nKey;
    private final String defaultMessage;
}
```

并提示用户在 `i18n/messages_*.properties` 中加翻译（`/add-error-code` 可批量添加）。

### 4. Repository 接口

```java
package com.eagle.{service}.{module}.domain.repository;

public interface {Aggregate}Repository extends JpaRepository<{Aggregate}, Long> {

    Optional<{Aggregate}> findBy{Aggregate}No(String {aggregate}No);

    @EntityGraph(attributePaths = {/* 关联子实体 */})
    Optional<{Aggregate}> findFullById(Long id);
}
```

### 5. 领域事件

```java
package com.eagle.{service}.{module}.domain.event;

public record {Aggregate}CreatedEvent(Long {aggregate}Id, String {aggregate}No)
    extends BaseEvent { }

public record {Aggregate}CancelledEvent(Long {aggregate}Id) extends BaseEvent { }
```

### 6. ApplicationService

```java
package com.eagle.{service}.{module}.application.service;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class {Aggregate}ApplicationService {

    private final {Aggregate}Repository repository;
    private final {Aggregate}Mapper mapper;

    public {Aggregate}Response create(Create{Aggregate}Request request) {
        {Aggregate} agg = {Aggregate}.create(generateNo());
        repository.save(agg);
        return mapper.toResponse(agg);
    }

    @Transactional(readOnly = true)
    public {Aggregate}Response findById(Long id) {
        return repository.findById(id)
            .map(mapper::toResponse)
            .orElseThrow({Aggregate}ErrorCode.{AGG}_NOT_FOUND::toNotFoundException);
    }

    public void cancel(Long id) {
        {Aggregate} agg = repository.findById(id)
            .orElseThrow({Aggregate}ErrorCode.{AGG}_NOT_FOUND::toNotFoundException);
        agg.cancel();
    }
}
```

### 7. Mapper（MapStruct）

```java
@Mapper
public interface {Aggregate}Mapper {
    {Aggregate}Response toResponse({Aggregate} agg);
    List<{Aggregate}Response> toResponseList(List<{Aggregate}> list);
}
```

### 8. Controller + DTO

```java
@Tag(name = "{Aggregate}", description = "{aggregate} 管理")
@RestController
@RequestMapping("/api/v1/{aggregates}")
@RequiredArgsConstructor
public class {Aggregate}Controller {

    private final {Aggregate}ApplicationService service;

    @Operation(summary = "创建 {aggregate}")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public {Aggregate}Response create(@Valid @RequestBody Create{Aggregate}Request request) {
        return service.create(request);
    }

    @Operation(summary = "查询 {aggregate}")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public {Aggregate}Response get(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "取消 {aggregate}")
    @PreAuthorize("hasRole('admin') or @{aggregate}AccessChecker.canModify(#id, authentication)")
    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id) {
        service.cancel(id);
    }
}
```

### 9. 单元测试骨架

```java
@ExtendWith(MockitoExtension.class)
class {Aggregate}ApplicationServiceTest {
    @Mock private {Aggregate}Repository repository;
    @Mock private {Aggregate}Mapper mapper;
    @InjectMocks private {Aggregate}ApplicationService service;

    @Nested @DisplayName("create")
    class Create { /* ... */ }

    @Nested @DisplayName("cancel")
    class Cancel { /* ... */ }
}
```

## 后续提示

完成后输出：

1. 已生成文件列表
2. 提示用户：
   - 用 `/add-error-code` 添加 i18n 翻译
   - 写 Flyway 建表脚本（`28-migration.md` 模板）
   - 跑 `/check-arch` 确保架构验证通过
3. 关键 TODO 占位：JPA 字段 / 业务方法 / 事件载荷 / 单元测试覆盖

## 参考规则

- `01-naming.md` `02-code-style.md` `03-architecture.md`
- `06-database.md` — 实体规范
- `07-exception.md` — ErrorCode
- `09-testing.md` — 单元测试结构
- `18-openapi.md` — Swagger 注解
