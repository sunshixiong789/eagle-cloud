# syntax=docker/dockerfile:1
#
# eagle-auth-service 运行镜像 —— 单阶段，直接跑 bootJar。
#
# 前置：宿主机先执行 `gradle :eagle-services:eagle-auth-service:bootJar`。
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
ARG JAR_FILE=build/libs/eagle-auth-service-*-SNAPSHOT.jar
COPY --chown=spring:spring ${JAR_FILE} application.jar

VOLUME /usr/app/config
VOLUME /app/logs

USER spring:spring

EXPOSE 8081

# ── JVM 参数（按 mem_limit=2G 标定）──────────────────────────────────────────
# 流量画像：auth 只处理登录 / token 刷新 —— 各资源服务器用 JWKS **本地**验签，
# 不会每个请求都打到这里。10 万 DAU 对应峰值大约 50~200 QPS，属于低负载服务。
# 预算：堆 1024M(50%) + metaspace ≤256M + code cache ≤96M + direct ≤128M + JVM ~100M
#      = 1604M ≤ 2048M，留约 440M 余量。
#
# ⚠️ MaxDirectMemorySize 必须显式设：不设时默认等于 -Xmx，堆外可以再悄悄吃掉一整个堆的量，
#    这是容器被 OOMKilled 最常见的原因。
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=50.0 \
    -XX:InitialRAMPercentage=25.0 \
    -XX:MaxMetaspaceSize=256m \
    -XX:ReservedCodeCacheSize=96m \
    -XX:MaxDirectMemorySize=128m \
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
