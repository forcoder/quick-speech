FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml .
COPY api/pom.xml api/
COPY common/pom.xml common/
COPY knowledge/pom.xml knowledge/
COPY agent/pom.xml agent/

RUN mvn dependency:resolve -B || true

COPY api/src api/src
COPY common/src common/src
COPY knowledge/src knowledge/src
COPY agent/src agent/src

RUN mvn clean package -pl api -am -DskipTests -B && \
    ls -la /build/api/target/*.jar && \
    java -version

FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

COPY --from=builder /build/api/target/api-*.jar app.jar

RUN java -version

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
