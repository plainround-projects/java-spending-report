# Multi-stage: build with a JDK, run on a JRE as a non-root user. Base images are pinned by tag and digest (Temurin 25, Ubuntu 24.04).
ARG JDK_IMAGE=eclipse-temurin:25-jdk-noble@sha256:ba0f0e1a33864105e1d454e88105c617ab40f71062db1f306bf58be32a3f8e5e
ARG JRE_IMAGE=eclipse-temurin:25-jre-noble@sha256:3f2223fe04b750aa5a8179e29b5c9a1ddf7665fc9bb08bfe11ad01d143bc13a5

FROM ${JDK_IMAGE} AS build
# The Maven wrapper downloads Maven with curl; the JDK image has neither curl nor wget.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:resolve
COPY config config
COPY src src
RUN ./mvnw -B -q package -DskipTests -Djacoco.skip=true

FROM ${JRE_IMAGE}
RUN groupadd --gid 10001 app \
 && useradd --uid 10001 --gid app --no-create-home --no-log-init --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /src/target/spend.jar /app/spend.jar
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/spend.jar"]
CMD ["--help"]
