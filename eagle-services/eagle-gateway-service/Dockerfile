# syntax=docker/dockerfile:1
#
# eagle-gateway-service 运行镜像 —— 单阶段，直接跑 bootJar。
#
# 前置：宿主机先执行 `gradle :eagle-services:eagle-gateway-service:bootJar`。
#      ⚠️ 别在 gradle 还在写 jar 的时候起 docker build —— 半成品 jar 会以
#         "Invalid or corrupt jarfile" 形式在容器里炸。等 gradle 退出再构建。
#
# ⚠️ 三个服务的这份文件结构一致，只有端口、挂载点与 JVM 参数不同
#    （JVM 参数按各自的流量画像分别标定，见下方注释）。改动请三份同步。

FROM eclipse-temurin:25-jdk-alpine
LABEL maintainer="eagle"

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8

# tzdata：镜像自带的 zoneinfo 不含 Asia/Shanghai，日志时间会按 UTC 走。
#
# /app/logs 必须在镜像里预建并归属 spring：compose 把命名卷挂到这里，且 JAVA_OPTS 的
# HeapDumpPath 指向 /app/logs/heapdump。镜像里没有该目录时，Docker 会以 root 属主新建卷，
# 非 root 的 spring 用户写不进去 —— OOM 时 heap dump 会静默失败，恰好在最需要它的时候。
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo 'Asia/Shanghai' > /etc/timezone \
    && addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /usr/app/config /usr/app/cache /app/logs/heapdump \
    && chown -R spring:spring /usr/app /app/logs

WORKDIR /usr/app

# 通配 *-SNAPSHOT.jar 以兼容版本变更；-plain.jar 不会被匹配
ARG JAR_FILE=build/libs/eagle-gateway-service-*-SNAPSHOT.jar
COPY --chown=spring:spring ${JAR_FILE} application.jar

VOLUME /usr/app/config
VOLUME /app/logs

USER spring:spring

EXPOSE 8080

# ── JVM 参数（按 mem_limit=2G 标定）──────────────────────────────────────────
# 流量画像：**网关看到 100% 的流量**，是全链路 QPS 最高的组件（后端各服务只看到自己那一片）。
# 但它是 Spring Cloud Gateway / WebFlux / Netty —— 反应式，少量 event loop 线程处理
# 全部连接，**主要工作内存在堆外（PooledByteBufAllocator 的 direct buffer）而不是堆**。
# 所以这里刻意把堆压小、把 direct 放大，跟 auth / system 的配比正好相反。
#
# 预算：堆 819M(40%) + direct ≤640M + metaspace ≤256M + code cache ≤128M + JVM ~100M
#      = 1943M ≤ 2048M。
#
# ⚠️ 这一项以前是最危险的：MaxDirectMemorySize 不设时默认等于 -Xmx，
#    对一个 Netty 组件意味着堆外无上限，容器必然先被 OOMKilled。
# ⚠️ WebSocket 代理：每条客户端 WS 连接会在网关侧再开一条到 system 的上游连接，
#    即 2 条 Netty channel，各自持有 direct buffer。按每条连接合计约 15~30 KB 估，
#    1 万并发 WS ≈ 150~300 MB direct —— 640M 的上限要按你的实际在线数复核。
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=40.0 \
    -XX:InitialRAMPercentage=20.0 \
    -XX:MaxMetaspaceSize=256m \
    -XX:ReservedCodeCacheSize=128m \
    -XX:MaxDirectMemorySize=640m \
    -Xss768k \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:ParallelGCThreads=4 \
    -XX:ConcGCThreads=1 \
    -XX:+AutoCreateSharedArchive \
    -XX:SharedArchiveFile=/usr/app/cache/app.jsa \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump \
    -Dfile.encoding=UTF-8" \
    JAVA_OPTS_APPEND=""

# CONSUL_INSTANCE_IP 空串归一化为「未定义」：compose 的 environment 无法条件性省略键，
# 不填时注入的是空串而非缺省。空串会顶掉 application.yml 里
# ${CONSUL_INSTANCE_IP:${spring.cloud.client.ip-address}} 的回落（Spring 的 :default
# 只在变量未定义时生效），使注册 Address 和健康检查 URL 都缺 host，实例被 Consul 注销。
ENTRYPOINT ["sh", "-c", "[ -n \"$CONSUL_INSTANCE_IP\" ] || unset CONSUL_INSTANCE_IP; exec java $JAVA_OPTS $JAVA_OPTS_APPEND -jar application.jar"]
