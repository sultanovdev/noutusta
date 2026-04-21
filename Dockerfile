FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests clean package \
    && cp target/*.jar app.jar


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd --create-home --shell /bin/bash appuser

COPY --from=builder /build/app.jar app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
