# 多阶段构建：编译 + 运行
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# 配置阿里云Maven镜像加速
COPY pom.xml .
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 复制pom文件
COPY api/pom.xml api/
COPY common/pom.xml common/
COPY knowledge/pom.xml knowledge/
COPY agent/pom.xml agent/

# 下载依赖（利用缓存层）
RUN mvn dependency:go-offline -B || true

# 复制源码并构建
COPY common/src common/src
COPY knowledge/src knowledge/src
COPY agent/src agent/src
COPY api/src api/src

RUN mvn clean package -pl api -am -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/api/target/api-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
