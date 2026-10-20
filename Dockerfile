# syntax=docker/dockerfile:1
#
# music-metadata image:
#   docker build -t music-metadata .

# ---- Build stage: JDK + Maven wrapper, compiles and packages the app ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Wrapper and pom first: this layer only rebuilds when they change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# The API contract: the build generates the API interfaces from it.
COPY api/openapi.yaml api/openapi.yaml
COPY src src

# Tests are skipped here: they need Docker (Testcontainers) and run in CI instead.
# The cache mount keeps downloaded Maven dependencies between builds.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress -DskipTests package \
    && cp target/music-metadata-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- Runtime stage: JRE only, no build tools or source code ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as an unprivileged user, never as root.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
USER app

# One layer per kind of content, least to most frequently changed: a code change only replaces the last layer.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8080
# Size the heap from the container's memory limit, not the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
