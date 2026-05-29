FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src/ src/
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8123
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
