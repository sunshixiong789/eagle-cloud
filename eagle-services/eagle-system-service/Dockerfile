# syntax=docker/dockerfile:1
#
# eagle-system-service 运行镜像 —— 单阶段，直接跑 bootJar。
#
# 前置：宿主机先执行 `gradle :eagle-services:eagle-system-service:bootJar`。
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
    && mkdir -p /usr/app/config /usr/app/cache /app/logs/heapdump /data/eagle/storage \
    && chown -R spring:spring /usr/app /app/logs /data/eagle/storage

WORKDIR /usr/app

# 通配 *-SNAPSHOT.jar 以兼容版本变更；-plain.jar 不会被匹配
ARG JAR_FILE=build/libs/eagle-system-service-*-SNAPSHOT.jar
COPY --chown=spring:spring ${JAR_FILE} application.jar

VOLUME /usr/app/config
VOLUME /app/logs

# 本地文件存储：容器以非 root 用户 spring 运行，而 WORKDIR /usr/app 归 root，
# 直接写默认相对路径 ./data 会 AccessDenied。此处预建独立目录并授权给 spring，
# 由 EAGLE_STORAGE_BASE_PATH 指向；生产把持久卷挂到此路径（卷 owner 需为 spring / 设 fsGroup）。
ENV EAGLE_STORAGE_BASE_PATH=/data/eagle/storage
VOLUME /data/eagle/storage

USER spring:spring

EXPOSE 8082

# ── JVM 参数（按 mem_limit=4G 标定）──────────────────────────────────────────
# 流量画像：后台管理端 HTTP 本身 QPS 很低（几十），真正吃资源的是 **C 端 App 的
# WebSocket 长连接** —— /ws-stomp 在本服务，经 gateway 的 /system/ws-stomp 转发。
# 每条 WS 连接约 30~50 KB 堆（Tomcat WsSession 文本/二进制缓冲各 8K + STOMP 订阅注册表
# + SockJS 会话 + WebSocketSessionManager 的 map）。
#   1 万并发 WS ≈ 400 MB；2 万 ≈ 800 MB。堆按 2G 给，够到约 2~3 万并发。
# ⚠️ 连接数还有一堵**硬墙**在 server.tomcat.max-connections（见 application.yml），
#    默认 8192，与内存无关；先撞的是它。
#
# 预算：堆 2048M(50%) + metaspace ≤384M + code cache ≤128M + direct ≤512M + JVM ~100M
#      = 3172M ≤ 4096M，余量覆盖 200 个 Tomcat 平台线程的栈（未开虚拟线程）。
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=50.0 \
    -XX:InitialRAMPercentage=25.0 \
    -XX:MaxMetaspaceSize=384m \
    -XX:ReservedCodeCacheSize=128m \
    -XX:MaxDirectMemorySize=512m \
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
