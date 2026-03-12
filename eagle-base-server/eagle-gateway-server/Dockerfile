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

# 复制 JAR 文件
COPY --chown=spring:spring /build/libs/eagle-gateway-server-0.0.1-SNAPSHOT.jar /usr/app/app.jar

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
