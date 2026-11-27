# Stage 1: Build Spring Boot executable JAR (Java 25)
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# Stage 2: Hardened non-root runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /work
RUN apk update && apk upgrade --no-cache && \
    addgroup -S bibliomesh && adduser -S bibliomesh -G bibliomesh

COPY --from=build --chown=bibliomesh:bibliomesh /app/target/*.jar /work/app.jar

USER bibliomesh
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/work/app.jar"]