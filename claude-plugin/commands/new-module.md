---
description: 按 Modulith + DDD 模板创建新业务模块（含 package-info、四层目录骨架）
argument-hint: "<服务名>:<模块名>，例 eagle-system-server:order"
---

# /new-module — 创建新业务模块

按 `03-architecture.md` + `04-modulith.md` 规范创建一个完整的 DDD 业务模块骨架。

## 输入解析

- `$ARGUMENTS` 形如 `eagle-system-server:order`
- 若缺省，提示用户输入：服务名（service）、模块名（module）、模块显示名、allowedDependencies

## 执行步骤

### 1. 创建目录结构

```
{service}/src/main/java/com/eagle/{service-short}/{module}/
├── package-info.java                    # @ApplicationModule
├── web/
│   ├── controller/
│   └── dto/
│       ├── request/
│       └── response/
├── application/
│   ├── service/
│   └── mapper/
├── domain/
│   ├── model/
│   │   ├── aggregate/
│   │   ├── entity/
│   │   ├── valueobject/
│   │   └── enums/
│   ├── repository/
│   ├── service/
│   ├── event/
│   │   └── package-info.java            # @NamedInterface("event")
│   └── port/
│       └── package-info.java            # @NamedInterface("port")
└── infrastructure/
    ├── persistence/
    ├── adapter/
    ├── event/
    ├── service/
    ├── config/
    └── messaging/
```

### 2. 生成 `package-info.java`

```java
@ApplicationModule(
        displayName = "{显示名}模块",
        allowedDependencies = {"common", "auth::port", "auth::event"}
)
@NullMarked
package com.eagle.{service-short}.{module};

        import org.jspecify.annotations.NullMarked;
        import org.springframework.modulith.ApplicationModule;
```

### 3. 生成事件包 `package-info.java`

```java
@NamedInterface("event")
@NullMarked
package com.eagle.{service-short}.{module}.domain.event;

        import org.jspecify.annotations.NullMarked;
        import org.springframework.modulith.NamedInterface;
```

### 4. 生成 ErrorCode 枚举骨架

```java
package com.eagle.{service-short}.{module}.web.exception;

        import com.eagle.common.exception.ErrorCode;
        import com.eagle.common.exception.AppException;
// ... 模板代码
```

### 5. 生成模块说明 README（可选）

`{module}/README.md`：模块职责、聚合列表、对外暴露的 NamedInterface、与其他模块的关系。

### 6. 验证

```bash
./gradlew :{service}:test --tests "*.ModulithArchitectureTest"
```

确保新模块未破坏架构约束。

## 用户交互

询问用户：

1. **模块名**（snake_case）：`order`
2. **显示名**（中文）：`订单`
3. **allowedDependencies**（默认 `common`）：是否需要 `auth::port` / `auth::event` / 其他模块?
4. **首个聚合根名称**：`Order`（用于生成示例代码）
5. **是否启用领域事件**（默认 yes）：会自动生成 `domain/event/` + `@NamedInterface("event")`

## 输出

- 完整目录结构
- 所有 `package-info.java`
- 占位的 ErrorCode 枚举（提示后续按 `01-naming.md` 命名）
- 提示下一步：使用 `/new-aggregate` 创建聚合根

## 参考规则

- `03-architecture.md` — DDD 分层
- `04-modulith.md` — 模块声明
- `01-naming.md` — 命名规范
- `02-code-style.md` — `@NullMarked` 要求
