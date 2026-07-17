# Gradle 发布与使用指南

本文档描述 Eagle Cloud 项目的产物如何发布、以及外部项目如何接入。

## 一、发布体系总览

### 可发布产物

| 模块                 | 类型                    | groupId     | artifactId             | 用途                           |
|--------------------|-----------------------|-------------|------------------------|------------------------------|
| `eagle-bom`        | BOM (`java-platform`) | `com.eagle` | `eagle-bom`            | 统一管理项目所有依赖版本                 |
| `eagle-starter:*`  | 库 (`jar`)             | `com.eagle` | `eagle-{name}-starter` | Spring Boot Starter，供业务方按需引入 |
| `eagle-services:*` | 可执行应用 (`bootJar`)     | `com.eagle` | `eagle-{name}-service` | 通常不发布到仓库，由 Docker 镜像分发       |

### 版本管理

- 全部模块 groupId 统一：`com.eagle`
- 当前版本：`1.0.0-SNAPSHOT`（在根 `build.gradle` 的 `allprojects` 块统一定义）
- SNAPSHOT 版本走 snapshot 仓库，正式版走 release 仓库（自动判别）

### 仓库配置位置

根 `build.gradle` 的 `configure(subprojects)` 块统一注册了 `nexus` Maven 仓库，所有子模块（含 BOM）共享。仓库 URL
与凭证通过参数 / 环境变量注入，**不入仓库**。

---

## 二、发布到本地仓库（mavenLocal）

适用场景：本地开发调试，让本机其他项目能直接引用未发布的 SNAPSHOT 版本。

### 命令

```bash
# 仅发布 BOM
gradle :eagle-bom:publishToMavenLocal

# 发布单个 starter
gradle :eagle-starter:eagle-redis-starter:publishToMavenLocal

# 全量发布所有模块
gradle publishToMavenLocal
```

### 产物位置

默认在 `~/.m2/repository/com/eagle/`，若 Maven 配置了自定义本地仓库（如 `~/repository/`），产物会落到该目录。每个模块包含：

```
com/eagle/eagle-bom/1.0.0-SNAPSHOT/
├── eagle-bom-1.0.0-SNAPSHOT.pom        # Maven POM
└── eagle-bom-1.0.0-SNAPSHOT.module     # Gradle Module Metadata
```

### 消费方启用

引用方在 `build.gradle` 的 `repositories` 中加入 `mavenLocal()`：

```groovy
repositories {
    mavenLocal()                                      // 优先查本地
    maven { url = 'https://maven.aliyun.com/repository/public' }
    mavenCentral()
}
```

> ⚠️ **生产构建禁用 mavenLocal**：本地缓存可能含未审计内容，CI / 生产构建不应启用。

---

## 三、发布到 Nexus 私服

适用场景：团队内部共享，最常见的方式。

### 凭证配置（三选一）

#### 方式 A — 用户级 `gradle.properties`（推荐）

写到 `~/.gradle/gradle.properties`，不入仓库，跨项目共享：

```properties
nexusReleaseUrl  = https://nexus.your-domain.com/repository/maven-releases/
nexusSnapshotUrl = https://nexus.your-domain.com/repository/maven-snapshots/
nexusUsername    = your-username
nexusPassword    = your-token-or-password
```

#### 方式 B — 命令行参数（一次性）

```bash
gradle :eagle-bom:publishBomPublicationToNexusRepository \
  -PnexusReleaseUrl=https://nexus.example.com/repository/maven-releases/ \
  -PnexusSnapshotUrl=https://nexus.example.com/repository/maven-snapshots/ \
  -PnexusUsername=alice \
  -PnexusPassword=*****
```

#### 方式 C — 环境变量（CI 推荐）

```bash
export NEXUS_RELEASE_URL=https://nexus.example.com/repository/maven-releases/
export NEXUS_SNAPSHOT_URL=https://nexus.example.com/repository/maven-snapshots/
export NEXUS_USERNAME=ci-user
export NEXUS_PASSWORD=$NEXUS_TOKEN
gradle publish
```

凭证查找优先级：`-P 参数` > `gradle.properties` > 环境变量。

### 发布命令

```bash
# 仅发布 BOM 到 Nexus
gradle :eagle-bom:publishBomPublicationToNexusRepository

# 发布单个 starter 到 Nexus
gradle :eagle-starter:eagle-redis-starter:publishMavenJavaPublicationToNexusRepository

# 一键发布所有模块到 Nexus
gradle publishAllPublicationsToNexusRepository
```

简写形式（同时发到所有已注册仓库，含 mavenLocal 和 nexus）：

```bash
gradle publish
```

### 仓库自动选择规则

构建脚本根据 `version` 自动选择：

| 项目 version       | 实际推送仓库             |
|------------------|--------------------|
| `1.0.0-SNAPSHOT` | `nexusSnapshotUrl` |
| `1.0.0`          | `nexusReleaseUrl`  |

要切换正式版发布，只需修改根 `build.gradle` 的 `version`，无需改其他地方。

### 内网 HTTP Nexus

脚本会根据 URL scheme 自动 `allowInsecureProtocol = true`，内网 http 私服无需额外配置。**对外网必须用 HTTPS**。

---

## 四、发布到 Maven Central

适用场景：开源对外发布。流程比私服严格得多——必须 GPG 签名、必须有完整 POM 元信息（开发者、许可证、SCM 链接）、必须用 HTTPS。

### 前置准备

1. **注册 Sonatype 账号**：[https://central.sonatype.com/](https://central.sonatype.com/)
    - 验证 groupId 所有权（域名验证或 GitHub 仓库验证）
    - 当前 groupId `com.eagle` 不是合法域名，发布到 Central 前需要：
        - 改成已有域名（如 `com.example.eagle`、`io.github.username`）
        - 或迁移到 `io.github.{你的 GitHub 用户名}`
2. **生成 GPG 密钥**：
   ```bash
   gpg --gen-key
   gpg --keyserver hkps://keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

### 配置改动

#### 1. 添加签名插件与 Central 发布插件

根 `build.gradle` 的 plugins 块：

```groovy
plugins {
    // ... 现有插件 ...
    id 'signing'
    id 'com.vanniktech.maven.publish' version '0.30.0' apply false  // 推荐，简化 Central 发布流程
}
```

> 不用 `com.vanniktech.maven.publish` 也可以纯手写 `signing` + `publishing` + `nexus-publish-plugin`，但工作量大且容易出错。

#### 2. 完善 POM 元信息

每个 publication 必须包含：

```groovy
publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
            groupId = 'io.github.your-name'

            pom {
                name = 'Eagle Common Starter'
                description = 'Eagle Cloud 公共基础设施'
                url = 'https://github.com/your-name/eagle-cloud'
                licenses {
                    license {
                        name = 'Apache License 2.0'
                        url = 'https://www.apache.org/licenses/LICENSE-2.0'
                    }
                }
                developers {
                    developer {
                        id = 'sunshixiong'
                        name = 'Sun Shixiong'
                        email = 'shixiong.sun@example.com'
                    }
                }
                scm {
                    connection = 'scm:git:git://github.com/your-name/eagle-cloud.git'
                    developerConnection = 'scm:git:ssh://git@github.com/your-name/eagle-cloud.git'
                    url = 'https://github.com/your-name/eagle-cloud'
                }
            }
        }
    }
}
```

#### 3. 配置签名

```groovy
signing {
    def signingKey = findProperty('signingKey') ?: System.getenv('GPG_SIGNING_KEY')
    def signingPassword = findProperty('signingPassword') ?: System.getenv('GPG_SIGNING_PASSWORD')
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign publishing.publications
}
```

#### 4. 添加 Central 仓库

```groovy
publishing {
    repositories {
        maven {
            name = 'centralPortal'
            url = 'https://central.sonatype.com/api/v1/publisher/'
            credentials {
                username = findProperty('centralUsername') ?: System.getenv('SONATYPE_USERNAME')
                password = findProperty('centralPassword') ?: System.getenv('SONATYPE_PASSWORD')
            }
        }
    }
}
```

### 发布流程

```bash
# 1. 切换为 release 版本
# 修改根 build.gradle: version = '1.0.0'（去掉 -SNAPSHOT）

# 2. 发布
gradle publishAllPublicationsToCentralPortalRepository

# 3. 登录 https://central.sonatype.com/，进入 "Deployments"，确认 staging 通过校验后点击 Publish
```

> Central 发布有 Staging 流程，发完后必须人工 Promote。SNAPSHOT 仍走单独的 snapshot 仓库（无需 Promote）。

---

## 五、消费方接入指南

### 场景 1：新建独立 Spring Boot 项目，使用 Eagle 体系

#### 项目级 `settings.gradle`

```groovy
rootProject.name = 'my-business-app'
```

#### 项目级 `build.gradle`

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.6'
}

group = 'com.example'
version = '0.1.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    // 1. 公司 Nexus（Eagle 产物所在地）
    maven {
        url = 'https://nexus.your-domain.com/repository/maven-public/'  // group 仓库聚合 release+snapshot+proxy
        credentials {
            username = findProperty('nexusUsername') ?: System.getenv('NEXUS_USERNAME')
            password = findProperty('nexusPassword') ?: System.getenv('NEXUS_PASSWORD')
        }
    }
    // 2. 阿里云镜像加速（中央仓库代理）
    maven { url = 'https://maven.aliyun.com/repository/public' }
    mavenCentral()
}

dependencies {
    // 关键：先引入 BOM，后续依赖无需写版本
    implementation platform('com.eagle:eagle-bom:1.0.0-SNAPSHOT')

    // Eagle Starter 按需引入（无版本号）
    implementation 'com.eagle:eagle-redis-starter'
    implementation 'com.eagle:eagle-data-jpa-starter'
    implementation 'com.eagle:eagle-resource-server-starter'

    // 任何 BOM 管理过的第三方依赖也无需版本
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'cn.hutool:hutool-all'
    runtimeOnly 'com.mysql:mysql-connector-j'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

#### 验证

```bash
gradle dependencies --configuration compileClasspath
# 应能看到 com.eagle:eagle-bom:1.0.0-SNAPSHOT 被解析，所有传递依赖版本由 BOM 决定
```

### 场景 2：仅使用 BOM 管理版本，不引入 Eagle Starter

```groovy
dependencies {
    implementation platform('com.eagle:eagle-bom:1.0.0-SNAPSHOT')

    // 享受 Eagle BOM 锁定的版本，但不依赖 Eagle 自身代码
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.redisson:redisson-spring-boot-starter'
    implementation 'cn.hutool:hutool-all'
    runtimeOnly 'com.mysql:mysql-connector-j'
}
```

### 场景 3：在 Eagle 主仓库内新建子模块

直接享受根 `build.gradle` 的统一配置，子模块 `build.gradle` 极简：

```groovy
// eagle-starter/eagle-newfeature-starter/build.gradle
dependencies {
    api project(':eagle-starter:eagle-common-starter')
    api 'org.springframework.boot:spring-boot-starter-web'   // 无版本，BOM 已管

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

然后在根 `settings.gradle` 加一行 `include`：

```groovy
include 'eagle-starter:eagle-newfeature-starter'
```

### 场景 4：跨项目锁定版本（多服务统一）

如果团队有多个独立服务都基于 Eagle，强烈建议**所有服务都引用同一个 `eagle-bom` 版本**，避免各服务依赖版本漂移。

升级 BOM 时只需改一处：

```groovy
implementation platform('com.eagle:eagle-bom:1.1.0')   // 新版
```

所有 Eagle Starter 和被 BOM 管理的第三方库会自动跟随到新版本。

---

## 六、常见问题（FAQ）

### Q1：发布失败，提示 `Could not find org.xxx:xxx`

**原因**：自定义 configuration 没继承 BOM platform 约束。

**解决**：在根 `build.gradle` 的 `bomTargets` 列表加上 configuration 名字。例如新增 `testFixturesImplementation`：

```groovy
def bomTargets = ['implementation', 'compileOnly', /* ... */, 'testFixturesImplementation']
```

### Q2：消费方拉不到 SNAPSHOT 最新版本

**原因**：Gradle 默认缓存 SNAPSHOT 24 小时。

**解决**：

```bash
gradle clean build --refresh-dependencies
```

或在消费方 `build.gradle` 调整缓存策略：

```groovy
configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}
```

### Q3：发到 Nexus 报 `401 Unauthorized`

依次检查：

1. `gradle.properties` 中是否有 `nexusUsername` / `nexusPassword`
2. Nexus 账号是否对目标 repository 有 deploy 权限
3. release 仓库是否禁止重复发布同版本号（多数 Nexus 默认禁止覆盖）—— 改为 SNAPSHOT 或升版本号

### Q4：发到 Nexus 报 `400 Bad Request - repository does not allow updating assets`

发布的是同一个 release 版本两次。Release 仓库不允许覆盖。**升一个版本号或改用 SNAPSHOT**。

### Q5：消费方报 `Could not resolve com.eagle:eagle-bom:1.0.0-SNAPSHOT`

依次检查：

1. 消费方 `repositories` 是否包含 Eagle 产物所在的仓库
2. 该仓库是否开放匿名读取，否则需配置 credentials
3. BOM 是否已经发布到该仓库（在 Nexus UI 直接搜 `eagle-bom`）

### Q6：本地能用，CI 拉不到

CI 环境一般无 `~/.gradle/gradle.properties`，需通过环境变量传凭证：

```yaml
# .github/workflows/ci.yml 示例
env:
  NEXUS_USERNAME: ${{ secrets.NEXUS_USERNAME }}
  NEXUS_PASSWORD: ${{ secrets.NEXUS_PASSWORD }}
```

### Q7：升级 BOM 版本后，部分依赖未生效

确认升级路径：

```bash
gradle :your-module:dependencyInsight --dependency redisson-spring-boot-starter
```

如果有其他 BOM（如 Spring Boot 自带）覆盖了 Eagle BOM 的版本，需调整 BOM import 顺序——**Gradle platform 是后导入覆盖**，把
Eagle BOM 放在最后即可。

---

## 七、相关任务速查

```bash
# 查看所有发布相关任务
gradle :eagle-bom:tasks --group=publishing

# 验证 POM 内容
gradle :eagle-bom:generatePomFileForBomPublication
cat eagle-bom/build/publications/bom/pom-default.xml

# 查看依赖解析路径
gradle :eagle-services:eagle-system-service:dependencyInsight --dependency mysql-connector-j

# 查看 BOM 当前管理了哪些版本
cat eagle-bom/build.gradle | grep -E "Version|version"
```

---

## 八、变更检查清单

发布前自查：

- [ ] 版本号是否已在根 `build.gradle` 升级（SNAPSHOT 不一定升，正式版必须升）
- [ ] 新增 starter 是否已加入 `settings.gradle`
- [ ] 新增模块 `build.gradle` 是否使用 `api` 暴露需要传递给消费方的依赖
- [ ] `gradle build -x test` 通过
- [ ] `gradle :eagle-bom:generatePomFileForBomPublication` 输出的 POM 包含新版本约束
- [ ] CI 上 `gradle publish` 凭证已配置
- [ ] 通知下游服务团队 BOM 版本变更（可选，建团队通讯录）
