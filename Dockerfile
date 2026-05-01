FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# 安装Maven
RUN apk add --no-cache curl && \
    curl -sL https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn

# 复制pom文件
COPY pom.xml .
COPY api/pom.xml api/
COPY common/pom.xml common/
COPY knowledge/pom.xml knowledge/
COPY agent/pom.xml agent/

# 下载依赖
RUN mvn dependency:go-offline -B || true

# 复制源码
COPY api/src api/src
COPY common/src common/src
COPY knowledge/src knowledge/src
COPY agent/src agent/src

# 构建
RUN mvn clean package -pl api -am -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/api/target/api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
