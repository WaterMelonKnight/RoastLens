# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/roastlens-*.jar /tmp/roastlens.jar

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S roastlens && adduser -S -G roastlens -h /app roastlens \
    && mkdir -p /app/data && chown -R roastlens:roastlens /app
WORKDIR /app
COPY --from=build --chown=roastlens:roastlens /tmp/roastlens.jar ./roastlens.jar
USER roastlens
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "roastlens.jar"]
