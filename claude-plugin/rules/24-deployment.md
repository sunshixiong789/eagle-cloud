# 部署规范（Deployment）

## 容器化（Docker）

### Dockerfile 分层

```dockerfile
# ✅ 多阶段构建（编译镜像 ≠ 运行镜像）
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY eagle-bom eagle-bom
COPY eagle-starter eagle-starter
COPY eagle-base-server/eagle-system-server eagle-base-server/eagle-system-server
RUN ./gradlew :eagle-base-server:eagle-system-server:bootJar --no-daemon

FROM eclipse-temurin:25-jre
LABEL maintainer="eagle@example.com"
WORKDIR /app
COPY --from=builder /workspace/eagle-base-server/eagle-system-server/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=70.0", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/dumps", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

- 用 `jre` 而非 `jdk` 作运行镜像（减小 ~150MB）
- 缓存 Gradle 依赖：先 COPY `build.gradle` 再 COPY 源码
- 使用 `MaxRAMPercentage` 而非 `-Xmx`（容器内存自适应）
- 用 `eclipse-temurin` 官方镜像，**不**用 alpine（musl libc 兼容性问题）

### 镜像标签

```
eagle/system-server:1.4.0           # 版本（不可变）
eagle/system-server:1.4.0-amd64
eagle/system-server:main-{sha}      # 主干每次构建
eagle/system-server:latest          # 仅 dev 环境
```

- **禁止**生产环境用 `latest`（无法回滚到具体版本）
- 镜像版本与 Git Tag 一致
- 多架构构建：`docker buildx build --platform linux/amd64,linux/arm64`

## 健康检查

每个服务必须暴露 Spring Boot Actuator 健康端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized       # 生产不裸暴露详细信息
      probes:
        enabled: true                     # /actuator/health/liveness, /readiness
  health:
    db:
      enabled: true
    redis:
      enabled: true
```

### Kubernetes Probes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 60
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 5
  failureThreshold: 3
```

- **liveness**：进程是否存活；失败 → 重启
- **readiness**：是否可接流量；失败 → 摘流量但不重启
- 启动慢的服务调大 `initialDelaySeconds`（避免无限重启）

## 优雅停机

```yaml
server:
  shutdown: graceful
  port: 8081

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # 停机最长等待
```

启用后接收 `SIGTERM`：
1. 停止接收新请求（健康检查失败）
2. 等待进行中的请求完成（最多 30s）
3. 关闭 Spring 容器（线程池、DB 连接、MQ 消费者）
4. JVM 退出

容器编排（K8s）应配 `terminationGracePeriodSeconds: 60`，留出余量。

## 配置外部化

**禁止**镜像内硬编码环境配置。注入方式：

| 方式 | 适用 |
|------|------|
| 环境变量 | 简单字段（DB URL、Redis 地址、Profile） |
| ConfigMap（K8s）| 大段 YAML 配置 |
| Secret（K8s）| 密码、Token、密钥 |
| Nacos | 业务动态配置（详见 `19-config.md`） |

```yaml
# k8s deployment 示例
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: NACOS_SERVER
    value: "nacos.svc.cluster.local:8848"
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: eagle-db-secret
        key: password
  - name: JASYPT_ENCRYPTOR_PASSWORD
    valueFrom:
      secretKeyRef:
        name: eagle-jasypt-secret
        key: password
```

## 资源限制（K8s）

```yaml
resources:
  requests:
    cpu: 500m
    memory: 1Gi
  limits:
    cpu: 2000m
    memory: 2Gi
```

- `requests` 是调度依据；`limits` 是硬上限
- JVM 堆 = `memory.limits × 70%`（留余量给元空间 / 直接内存 / 线程栈）
- **禁止** CPU `limits` 等于 `requests`（无突发能力）

## 日志输出

- **stdout**（不落本地文件），由编排平台采集到 ELK
- 生产 JSON 格式（详见 `13-logging.md`）
- **禁止**容器内 `RollingFileAppender`（容器重启即丢失）

## 启动顺序与依赖

```yaml
# K8s initContainer 等待依赖就绪
initContainers:
  - name: wait-for-db
    image: busybox
    command: ['sh', '-c', 'until nc -z mysql 3306; do sleep 2; done']
  - name: wait-for-nacos
    image: busybox
    command: ['sh', '-c', 'until nc -z nacos 8848; do sleep 2; done']
```

或在应用层使用 Spring Boot 启动重试 + 熔断。**禁止**写死"启动后等待 30s"。

## 滚动发布

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1                # 最多多创建 1 个新 pod
    maxUnavailable: 0          # 不允许减少可用数
```

- 灰度：先发 1 个新版本 → 观察 5 分钟 → 全量
- 回滚：`kubectl rollout undo deployment/eagle-system-server`
- **禁止** Recreate 策略（停服）

## 监控接入

每个生产服务必须接入：

| 监控 | 工具 |
|------|------|
| 指标 | Prometheus（Micrometer 暴露） |
| 链路 | Zipkin / Jaeger（`eagle-tracing-starter`） |
| 日志 | ELK / Loki |
| 告警 | AlertManager → 钉钉/企微 |
| APM | Pinpoint / Skywalking（可选） |

## 环境晋升

```
本地 → dev → test → staging → prod
```

- 每个环境独立 K8s 命名空间
- 镜像由 dev 构建，逐级晋升（不重新构建）
- staging 环境必须使用生产数据快照（脱敏后）

## 回滚

| 类型 | 回滚方式 |
|------|---------|
| 应用代码 | `kubectl rollout undo` 切回上一镜像版本 |
| 数据库 DDL | 前向修复脚本（详见 `28-migration.md`）|
| 配置变更 | Nacos 历史版本恢复 |

**事前**：上线 PR 必须包含回滚方案；**事中**：监控自动触发告警；**事后**：复盘记录。

## 禁止清单

- 禁止生产环境用 `latest` 镜像
- 禁止 root 用户运行容器（用 `USER 1000:1000`）
- 禁止暴露不必要端口（debug 端口、JMX）
- 禁止在镜像中包含 `application-prod.yml` 明文密钥
- 禁止启动时大量同步阻塞操作（启动慢→ liveness 失败重启）
- 禁止单实例部署生产服务（必须 ≥ 2 副本）
- 禁止跨环境共用 Redis / MQ / DB
