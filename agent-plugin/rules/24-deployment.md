# 部署规范

部署规则保留生产红线和 Eagle 服务约定；Docker/Kubernetes 通用模板不在规则中长篇展开。

## 镜像

- 使用多阶段构建，编译镜像与运行镜像分离。
- 运行镜像使用非 root 用户，固定时区和 JVM 基础参数。
- 镜像标签包含版本和 git sha；禁止生产使用 `latest`。

## 健康检查

- 服务暴露 Spring Boot Actuator 健康端点。
- K8s 使用 startup、readiness、liveness probes；readiness 必须能反映依赖未就绪状态。
- 优雅停机开启，确保 HTTP、MQ、定时任务有关闭窗口。

## 配置与密钥

- 配置外部化：profile、Nacos、环境变量或 Secret。
- 生产密钥来自 Secret / 环境变量 / 加密配置，不写入镜像、仓库或 ConfigMap 明文。
- `SPRING_PROFILES_ACTIVE` 由部署环境显式指定。

## 资源与发布

- 生产服务至少 2 副本；设置 requests/limits。
- 滚动发布要配置 maxUnavailable / maxSurge；有状态或强依赖服务先做兼容性验证。
- 上线 PR 必须包含回滚方式；DB 变更遵守 `28-migration.md`。

## 日志与监控

- 应用日志输出 stdout/stderr，由平台采集。
- 生产接入指标、日志、链路追踪和告警；关键告警包含实例、版本、traceId 或 requestId。

## 环境晋升

- dev -> test -> staging -> prod 逐级晋升。
- staging 尽量接近生产配置；使用生产数据快照时必须脱敏。

## 禁止清单

- 生产单实例部署。
- 镜像内置密钥、配置文件或本地路径。
- 手工改生产容器内文件。
- 未说明回滚方案的上线。
