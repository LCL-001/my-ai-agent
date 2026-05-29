FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
# 先只复制 pom，下载依赖（这一层被缓存，改代码不会重新下载）
COPY pom.xml .
RUN mvn dependency:resolve -q
# 再复制源码并编译
COPY src/ src/
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8123
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
