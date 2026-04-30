# 多阶段构建：编译 + 运行
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# 先复制pom文件，利用Docker缓存层
COPY pom.xml .
COPY api/pom.xml api/
COPY common/pom.xml common/
COPY knowledge/pom.xml knowledge/
COPY agent/pom.xml agent/

# 下载依赖（缓存层）
RUN apk add --no-cache curl && \
    curl -sL https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar xz -C /opt/ && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn && \
    mvn dependency:resolve -B -q

# 复制源码并构建
COPY common/src common/src
COPY knowledge/src knowledge/src
COPY agent/src agent/src
COPY api/src api/src

RUN mvn clean package -DskipTests -B -q

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/api/target/api-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
