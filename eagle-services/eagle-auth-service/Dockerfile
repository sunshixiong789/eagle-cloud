# syntax=docker/dockerfile:1
#
# eagle-auth-service 云原生镜像 —— 三阶段构建
#
#   stage 1 jre-builder    : jlink 裁出只含所需 JDK 模块的运行时（整包 JRE ~180 MB → ~85 MB）
#   stage 2 layer-extractor: 把 bootJar 拆成 4 个变更频率不同的层
#   stage 3 runtime        : alpine + 裁剪运行时 + 分层应用，非 root 运行
#
# 前置：宿主机先执行 gradle :eagle-services:eagle-auth-service:bootJar
#
# ⚠️ 三个服务的 stage 1 / stage 2 完全一致，只有 stage 3 的端口、挂载点与 JVM 参数不同
#    （JVM 参数按各自的流量画像分别标定，见下方注释）。改动请三份同步。

# ─────────────────────────────────────────────────────────────────────────────
# stage 1：jlink 定制运行时
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS jre-builder

# 模块清单 = jdeps 静态扫描结果 ∪ 一组只在运行期反射加载、jdeps 看不见的模块：
#   java.desktop          Spring 的 Introspector / java.beans —— 缺了直接起不来，别删
#   java.instrument       ByteBuddy / Hibernate 字节码增强
#   java.management(.rmi) Actuator metrics、JMX
#   java.naming + jdk.naming.dns  JNDI 与 Netty DNS resolver
#   java.sql(.rowset)     JDBC（gateway 用不到，但三份保持一致便于维护，代价可忽略）
#   jdk.charsets          ⚠️ GBK/GB18030 —— 第三方老接口的签名编码，缺了会乱码
#   jdk.localedata        ⚠️ jlink 默认只留 en locale；配合 --include-locales=en,zh 保住中文
#   jdk.crypto.ec/cryptoki TLS ECDSA
#   jdk.unsupported       sun.misc.Unsafe —— Netty / Hibernate / fastjson2 都要
#   jdk.jcmd + jdk.attach jcmd / jstack / jmap —— 线上查内存必备
#   jdk.jdwp.agent        远程 debug
#
# 若日后报 NoClassDefFoundError 且缺失类属于 java.*/jdk.*，回来补对应模块。
ARG JAVA_MODULES="java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.attach,jdk.charsets,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.httpserver,jdk.jcmd,jdk.jdwp.agent,jdk.jfr,jdk.localedata,jdk.management,jdk.management.agent,jdk.naming.dns,jdk.net,jdk.security.auth,jdk.unsupported,jdk.zipfs"

# --generate-cds-archive：为 JDK 自身的类生成 CDS 归档，class metadata 只读映射、
#   可在多容器间共享，既省 metaspace 又缩短启动。归档约 15 MB，值这个价。
RUN "$JAVA_HOME/bin/jlink" \
        --add-modules "$JAVA_MODULES" \
        --include-locales=en,zh \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-6 \
        --generate-cds-archive \
        --output /javaruntime \
    && /javaruntime/bin/java -version

# ─────────────────────────────────────────────────────────────────────────────
# stage 2：拆分层
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS layer-extractor
WORKDIR /build

ARG JAR_FILE=build/libs/eagle-auth-service-*-SNAPSHOT.jar
COPY ${JAR_FILE} application.jar

# 分层规则见根 build.gradle 的 bootJar.layered：
#   dependencies/          第三方依赖，几乎不变          → 最先 COPY，缓存命中率最高
#   spring-boot-loader/    非 launcher 模式下为空，占位保留
#   snapshot-dependencies/ eagle-* 内网快照，随发版变
#   application/           本服务字节码，每次提交都变     → 最后 COPY
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ─────────────────────────────────────────────────────────────────────────────
# stage 3：运行时
# ─────────────────────────────────────────────────────────────────────────────
FROM alpine:3.21
LABEL maintainer="eagle"

ENV JAVA_HOME=/opt/java \
    PATH="/opt/java/bin:${PATH}" \
    TZ=Asia/Shanghai \
    LANG=C.UTF-8

# libstdc++ / zlib：musl 上 HotSpot 的运行时依赖。
# tzdata 只为取出 Asia/Shanghai 这一个 zone，取完即卸载（省约 3 MB）。
#
# /app/logs 必须在镜像里预建并归属 spring：compose 把命名卷挂到这里，且 JAVA_OPTS 的
# HeapDumpPath 指向 /app/logs/heapdump。镜像里没有该目录时，Docker 会以 root 属主新建卷，
# 非 root 的 spring 用户写不进去 —— OOM 时 heap dump 会静默失败，恰好在最需要它的时候。
RUN apk add --no-cache libstdc++ zlib \
    && apk add --no-cache --virtual .tz tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo 'Asia/Shanghai' > /etc/timezone \
    && apk del .tz \
    && addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /usr/app/config /usr/app/cache /app/logs/heapdump  \
    && chown -R spring:spring /usr/app /app/logs 

COPY --from=jre-builder /javaruntime ${JAVA_HOME}

WORKDIR /usr/app

# 按「变更频率从低到高」COPY —— 改一行业务代码只重推最后这层（约 1 MB）
COPY --from=layer-extractor --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=layer-extractor --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=layer-extractor --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=layer-extractor --chown=spring:spring /build/extracted/application/ ./

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

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS $JAVA_OPTS_APPEND -jar application.jar"]
