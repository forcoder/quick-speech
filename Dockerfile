FROM eclipse-temurin:17-jdk-alpine

WORKDIR /build

# 安装curl和unzip
RUN apk add --no-cache curl unzip

# 安装Maven
RUN curl -sL https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz | tar xz -C /opt && \
    ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn

# 复制项目文件
COPY . .

# 构建
RUN mvn clean package -pl api -am -DskipTests -B

# 运行
WORKDIR /app
RUN cp /build/api/target/api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
