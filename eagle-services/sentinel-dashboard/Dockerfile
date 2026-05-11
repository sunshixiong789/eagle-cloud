# Sentinel Dashboard 官方发行版 jar（Alibaba 未提供官方 Docker 镜像，仅在 GitHub Releases 发布 fat jar）。
# 基础镜像沿用 eagle-services 其他服务的 eclipse-temurin:25-jre-alpine，保持一致。
FROM eclipse-temurin:25-jre-alpine
LABEL maintainer="eagle"

# Sentinel 版本（需与 spring-cloud-alibaba-sentinel 客户端协议兼容；客户端走 Spring Cloud Alibaba 2025.1.0.0）
ARG SENTINEL_VERSION=1.8.9
ENV SENTINEL_VERSION=${SENTINEL_VERSION}

# 非 root 用户
RUN addgroup -S sentinel && adduser -S sentinel -G sentinel

# 时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

WORKDIR /usr/app

# Sentinel Dashboard fat jar 由 Alibaba 在 GitHub Releases 发布（Alibaba 未提供官方 docker 镜像）。
# 构建前需把 jar 下载到 Dockerfile 同目录：
#   https://github.com/alibaba/Sentinel/releases/download/${SENTINEL_VERSION}/sentinel-dashboard-${SENTINEL_VERSION}.jar
COPY sentinel-dashboard-${SENTINEL_VERSION}.jar /usr/app/sentinel-dashboard.jar
RUN chmod 444 /usr/app/sentinel-dashboard.jar

USER sentinel:sentinel

EXPOSE 8858

# JAVA_OPTS 由 docker-compose 注入（server.port / csp.sentinel.dashboard.server / project.name / 鉴权账号等）
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /usr/app/sentinel-dashboard.jar"]
