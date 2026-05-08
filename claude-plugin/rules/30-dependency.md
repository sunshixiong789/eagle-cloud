# 依赖管理规范（Gradle）

## 整体策略

- **集中版本管理**：所有版本号在 `eagle-bom`（Java Platform 模块）声明
- **传递依赖**：通过 `api` / `implementation` 严格区分暴露范围
- **第三方版本**：升级前 Review 影响、读 release notes、跑测试

## Gradle 依赖范围

| 范围                    | 含义                     | 何时使用                                            |
|-----------------------|------------------------|-------------------------------------------------|
| `api`                 | 编译期 + 运行期 + **暴露给消费方** | starter 公开 API；BOM 入口                           |
| `implementation`      | 编译期 + 运行期，**不暴露**      | 业务服务依赖；私有实现细节                                   |
| `compileOnly`         | 仅编译期                   | 注解处理器、可选依赖（`@ConditionalOnClass`）               |
| `runtimeOnly`         | 仅运行期                   | JDBC 驱动、Logback 实现                              |
| `annotationProcessor` | 注解处理                   | Lombok、MapStruct、Spring Configuration Processor |
| `testImplementation`  | 测试编译 + 运行              | JUnit、Mockito                                   |
| `testCompileOnly`     | 仅测试编译                  | 测试用 Lombok                                      |

## 典型 starter 依赖（详见 `10-starter.md`）

```gradle
dependencies {
    // ✅ 核心依赖：暴露给消费方
    api project(':eagle-starter:eagle-common-starter')
    api 'org.springframework.boot:spring-boot-starter-data-jpa'

    // ✅ 可选依赖：消费方按需引入
    compileOnly 'io.micrometer:micrometer-tracing-bridge-brave'

    // ✅ 注解处理
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // ✅ 仅运行期
    runtimeOnly 'mysql:mysql-connector-j'

    // ✅ 测试
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

## 业务服务依赖

```gradle
dependencies {
    // ✅ 引入 BOM（管理所有版本，不引入实际依赖）
    implementation platform(project(':eagle-bom'))

    // ✅ 引入 starter（不写版本号，由 BOM 管控）
    implementation project(':eagle-starter:eagle-common-starter')
    implementation project(':eagle-starter:eagle-data-jpa-starter')
    implementation project(':eagle-starter:eagle-redis-starter')

    // ✅ 第三方
    implementation 'org.springframework.boot:spring-boot-starter-web'

    runtimeOnly 'mysql:mysql-connector-j'

    annotationProcessor 'org.projectlombok:lombok'
    compileOnly 'org.projectlombok:lombok'
}
```

## eagle-bom 维护

集中声明所有版本，业务服务/starter 引入时**不写版本号**：

```gradle
// eagle-bom/build.gradle
javaPlatform {
    allowDependencies()
}

dependencies {
    api platform('org.springframework.boot:spring-boot-dependencies:4.0.6')
    api platform('org.springframework.cloud:spring-cloud-dependencies:2025.1.1')

    constraints {
        api 'com.alibaba:druid-spring-boot-3-starter:1.2.28'
        api 'com.baomidou:mybatis-plus-spring-boot3-starter:3.5.x'
        // ...
    }
}
```

新增第三方依赖**必须**先在 BOM 声明，再在使用方引入。

## 版本升级策略

| 类型               | 触发            | 流程                                   |
|------------------|---------------|--------------------------------------|
| **Patch（x.y.Z）** | 月度例行          | BOM 升级 → 跑 CI → 合并                   |
| **Minor（x.Y.0）** | 季度评估          | 阅读 release notes → 评估破坏性 → 灰度环境跑 1 周 |
| **Major（X.0.0）** | 半年/年度评估       | 立项 + 兼容性测试 + 分阶段上线                   |
| **安全补丁**         | CVE 公告后 24h 内 | 紧急 PR → CI → 上线                      |

**禁止**：业务 PR 中夹带依赖升级（独立 PR + 独立评审）。

## 依赖检查

每月执行：

```bash
# 1) 查看可升级版本
./gradlew dependencyUpdates

# 2) 检查依赖冲突 / 树
./gradlew :path:to:module:dependencies

# 3) 检查 CVE 漏洞
./gradlew dependencyCheckAnalyze

# 4) 检查未使用 / 缺失依赖
./gradlew dependencyAnalysis  # gradle-dependency-analysis-plugin
```

CVE 高危漏洞（CVSS ≥ 7）**必须**在 1 周内修复。

## 依赖冲突处理

```gradle
// ✅ 强制版本（少用，会破坏 BOM 一致性）
configurations.all {
    resolutionStrategy {
        force 'org.slf4j:slf4j-api:2.0.13'
    }
}

// ✅ 排除传递依赖
implementation('com.example:lib') {
    exclude group: 'commons-logging', module: 'commons-logging'
}
```

冲突先 `gradle dependencyInsight --dependency xxx` 定位来源，再决定 `force` 或 `exclude`。

## 第三方依赖审查

新增第三方依赖前问：

1. **必要性**：是否能用 JDK / 现有依赖实现？
2. **健康度**：维护活跃？最近一次提交多久？issue 处理速度？
3. **License**：与项目兼容（Apache 2 / MIT 推荐；GPL / AGPL 慎入）
4. **传递依赖**：会引入多少间接依赖？是否冲突？
5. **包大小**：是否显著增大 jar？
6. **替代方案**：Spring Boot starter 是否已包含同类能力？

引入前在 PR 描述说明上述评估。

## 禁止依赖

| 类型                   | 替代                           |
|----------------------|------------------------------|
| Apache HttpClient 旧版 | Spring `RestClient` / OkHttp |
| Joda Time            | `java.time`                  |
| Apache Commons Lang2 | Lang3                        |
| `log4j 1.x`          | Logback / Log4j2             |
| `xstream` 旧版         | Jackson                      |
| 任何带未修复 CVE 的版本       | 升级或替换                        |

## 内部 starter 复用

业务服务**优先使用** `eagle-starter` 提供的能力，避免自行实现：

| 需求      | 使用                                         |
|---------|--------------------------------------------|
| 缓存      | `eagle-redis-starter`                      |
| MQ      | `eagle-rocketmq-starter`                   |
| 分布式锁    | `eagle-common-starter` 的 `DistributedLock` |
| OSS     | `eagle-oss-minio-starter`                  |
| 多租户     | `eagle-tenant-starter`                     |
| 数据权限    | `eagle-row-security-starter`               |
| 链路追踪    | `eagle-tracing-starter`                    |
| OpenAPI | `eagle-openapi-starter`                    |

**禁止**业务模块自行集成 Redisson、RocketMQ Client、MinIO Client（应统一收敛到 starter）。

## 发布

详见 `eagle-doc/Gradle发布与使用指南.md`。

## 禁止清单

- 禁止业务代码硬编码版本号（统一走 BOM）
- 禁止依赖范围用 `compile`（已废弃）
- 禁止生产依赖 SNAPSHOT 版本
- 禁止业务 PR 夹带依赖升级
- 禁止使用未审查的 GitHub 个人项目作为依赖
- 禁止引入与现有 starter 同功能的第三方库（重复 + 冲突源）
- 禁止 GPL / AGPL 协议依赖（除非确认合规）
- 禁止跳过 CI 的依赖审查
