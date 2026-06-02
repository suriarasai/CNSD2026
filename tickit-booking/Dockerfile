# syntax=docker/dockerfile:1

###########  Stage 1: build  ###########
# Factor II: base image tags are pinned, never :latest. This image pins both
# Maven and the JDK.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first for faster rebuilds.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Build the bootable jar (tests run separately, not during image build).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

###########  Stage 2: run  ###########
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# curl is used by the compose health check (Factor IX).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Factor VI: run as a non-root, stateless process.
RUN useradd --system --uid 10001 appuser
COPY --from=build /workspace/target/*.jar /app/app.jar
USER appuser

EXPOSE 8080
# Factor XI: logs go to stdout/stderr (default). Factor IX: SIGTERM -> graceful shutdown.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
