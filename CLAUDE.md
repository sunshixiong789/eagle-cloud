# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 项目概述

Eagle Cloud 是一个基于领域驱动设计（DDD）原则和 Spring Modulith 模块化单体架构构建的 Spring Boot 微服务平台。项目使用 Java 25、Spring Boot 4.0.3、Spring Cloud 2025.1.1 和 Spring Cloud Alibaba 2025.1.0.0。

## 构建命令

```bash
# 构建整个项目
./gradlew build

# 构建特定模块
./gradlew :eagle-base-server:eagle-system-server:build

# 清理构建
./gradlew clean build

# 运行整个项目的测试
./gradlew test

# 运行特定模块的测试
./gradlew :eagle-base-server:eagle-system-server:test

# 运行单个测试类
./gradlew :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass"

# 运行单个测试方法
./gradlew :eagle-base-server:eagle-system-server:test --tests "com.eagle.system.YourTestClass.testMethod"

# 构建原生镜像（需要 GraalVM）
./gradlew nativeCompile

# 构建 Docker 镜像
./gradlew bootBuildImage
```

## 运行应用

```bash
# 运行系统服务
./gradlew :eagle-base-server:eagle-system-server:bootRun

# 运行网关服务
./gradlew :eagle-base-server:eagle-gateway-server:bootRun
```

## 项目结构

### 模块组织

项目分为两大类：

1. **eagle-base-server**：可执行的微服务
   - `eagle-gateway-server`：使用 Spring Cloud Gateway 的 API 网关
   - `eagle-system-server`：系统服务，包含 OAuth2 授权服务器、用户管理和基础系统功能

2. **eagle-starter**：可复用的库模块（不可执行）
   - `eagle-common-starter`：通用工具、基础类和领域事件基础设施
   - `eagle-data-jpa-starter`：JPA/Hibernate 配置和工具
   - `eagle-feign-starter`：OpenFeign 客户端配置
   - `eagle-resource-server-starter`：OAuth2 资源服务器配置

### DDD 架构

每个微服务遵循严格的 DDD 分层架构：

```
com.eagle.{service-name}.{module-name}/
├── domain/              # 领域层（业务逻辑）
│   ├── model/
│   │   ├── entity/      # 聚合根和实体
│   │   ├── valueobject/ # 值对象
│   │   └── enums/       # 领域枚举
│   ├── repository/      # 仓储接口
│   ├── service/         # 领域服务
│   └── event/           # 领域事件
├── application/         # 应用层（用例）
│   ├── service/         # 应用服务
│   └── mapper/          # DTO 映射器（MapStruct）
└── infrastructure/      # 基础设施层（技术细节）
    ├── persistence/     # 仓储实现
    ├── security/        # 安全配置
    ├── messaging/       # 事件发布/处理
    └── external/        # 外部服务集成
```

### Spring Modulith 结构

system-server 使用 Spring Modulith 进行模块化组织。`com.eagle.system` 下的每个顶级包代表一个模块：
- `base`：核心系统功能（用户、角色、权限）
- `authorization`：OAuth2 授权服务器功能
- `config`：横切配置

## 领域驱动设计模式

### 聚合根

所有聚合根必须继承 `BaseAggregateRoot<T>`：

```java
@Entity
public class User extends BaseAggregateRoot<User> {
    // 领域逻辑
}
```

`BaseAggregateRoot` 提供：
- 自动生成的 ID（Long 类型，IDENTITY 策略）
- 审计字段：`createBy`、`updateBy`、`createTime`、`updateTime`
- 使用 `@Version` 的乐观锁
- 领域事件管理能力

### 领域事件

所有领域事件必须继承 `BaseEvent`：

```java
public class UserCreatedEvent extends BaseEvent {
    private final Long userId;
    private final String username;

    public UserCreatedEvent(Long userId, String username) {
        super(); // 生成 eventId 和 occurredOn
        this.userId = userId;
        this.username = username;
    }
}
```

领域事件的特点：
- 不可变（所有字段为 final）
- 使用过去时命名（例如 `UserCreatedEvent`，而不是 `CreateUserEvent`）
- 通过 `BaseEvent` 自动分配 UUID 和时间戳

要从聚合根发布事件，使用从 `AbstractAggregateRoot` 继承的 Spring Data 的 `registerEvent()` 方法。

## 关键技术

- **构建工具**：Gradle 8.x with Kotlin DSL
- **Java 版本**：Java 25
- **Spring Boot**：4.0.3
- **Spring Cloud**：2025.1.1
- **Spring Cloud Alibaba**：2025.1.0.0
- **Spring Modulith**：2.0.3
- **数据库**：MySQL 9.6.0 / PostgreSQL 42.7.10 with Druid 连接池
- **ORM**：Hibernate 7.2.6 with JPA
- **安全**：Spring Security with OAuth2 Authorization Server
- **API 文档**：SpringDoc OpenAPI 3.0.2
- **映射**：MapStruct 1.6.3
- **工具库**：Hutool 5.8.43、Guava 33.5.0、Apache Commons

## 重要说明

### Gradle 配置

- Starter 模块设置 `bootJar.enabled = false` 和 `jar.enabled = true`（它们是库，不是应用程序）
- Base server 模块是可执行的 Spring Boot 应用程序
- 所有模块使用 Spring Boot 依赖管理
- 启用 Hibernate 字节码增强以优化延迟加载

### 依赖范围

Starter 模块对其依赖使用 `api` 范围（而不是 `implementation`），以向消费模块暴露传递依赖。

### 测试

- 所有测试使用 JUnit 5（Jupiter）
- 测试运行器：`./gradlew test` 使用 JUnit Platform

### 原生镜像支持

项目配置了 GraalVM 原生镜像编译。Starter 模块禁用 AOT 处理，但可执行服务启用。

## 常用模式

### 仓储模式

领域仓储是领域层中的接口：

```java
// domain/repository/UserRepository.java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

### 应用服务

应用服务编排用例并协调领域对象之间的交互：

```java
@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public UserDTO createUser(CreateUserCommand command) {
        // 用例逻辑
    }
}
```

### DTO 映射

使用 MapStruct 进行 DTO 转换：

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
    User toEntity(CreateUserCommand command);
}
```
