FROM eclipse-temurin:25-jre-alpine
LABEL maintainer="eagle"

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

WORKDIR /usr/app

# 复制 JAR 文件（构建前需先在宿主机执行 gradle :eagle-services:eagle-gateway-service:bootJar）
# 通配 *-SNAPSHOT.jar 以兼容版本变更；plain.jar 不会被匹配
ARG JAR_FILE=build/libs/eagle-gateway-service-*-SNAPSHOT.jar
COPY --chown=spring:spring ${JAR_FILE} /usr/app/app.jar

# 创建配置卷
VOLUME /usr/app/config

# 切换到非 root 用户
USER spring:spring

EXPOSE 8080

# 优化 JVM 参数（容器环境）
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
