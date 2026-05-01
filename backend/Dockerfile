FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 配置阿里云Maven镜像
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 复制pom文件
COPY pom.xml .
COPY api/pom.xml api/
COPY common/pom.xml common/
COPY knowledge/pom.xml knowledge/
COPY agent/pom.xml agent/

# 下载依赖
RUN mvn dependency:resolve -B || true

# 复制源码并构建
COPY api/src api/src
COPY common/src common/src
COPY knowledge/src knowledge/src
COPY agent/src agent/src

RUN mvn clean package -pl api -am -DskipTests -B

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/api/target/api-*.jar app.jar

# 创建启动脚本
RUN echo '#!/bin/bash' > /app/start.sh && \
    echo 'echo "=== Java Version ==="' >> /app/start.sh && \
    echo 'java -version' >> /app/start.sh && \
    echo 'echo "=== JAR Info ==="' >> /app/start.sh && \
    echo 'ls -la /app/app.jar' >> /app/start.sh && \
    echo 'echo "=== Starting Application ==="' >> /app/start.sh && \
    echo 'java -Xmx512m -jar /app/app.jar' >> /app/start.sh && \
    chmod +x /app/start.sh

EXPOSE 8080

CMD ["/app/start.sh"]
