# Dockerfile syntax

## 1. What it is

A **Dockerfile** is a text file of instructions that tell Docker how to build an **image**: a read-only bundle of a minimal operating system, a program and its files. Each instruction (`FROM`, `COPY`, `RUN`, ...) adds a **layer**, a saved set of file changes. A container (see `docker.md`) is a running instance of an image.

## 2. Why this repository uses it

The repository promises a `Dockerfile` with a multi-stage build, a pinned base image and a non-root user. The file builds the program from source with the JDK and ships only the jar on a small JRE.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `Dockerfile` | both stages |
| `.dockerignore` | files that are not sent to the build (`.git`, `target`, logs) |
| `docker-compose.yml` | (not used here; the Makefile calls `docker build` and `docker run`) |
| `Makefile` | `docker-build`, `docker-run` |

## 4. How it works underneath

- `docker build` sends the **build context** (the folder, minus `.dockerignore` entries) to Docker, then runs the instructions in order. For each one it checks the cache; on a hit it reuses the layer, on a miss it runs the instruction in a temporary container and saves the result as a new layer.
- In a multi-stage build every `FROM` starts a new image. Only the last stage becomes the result; earlier stages can be copied from with `COPY --from=<name>`.
- `RUN` executes at build time; `ENTRYPOINT` and `CMD` execute when a container starts.
- An image is identified by its **digest** (hash). A tag like `25-jre-noble` is only a label that can move to a newer image; the digest never moves.

## 5. Walkthrough of this repository's use

```dockerfile
# Multi-stage: build with a JDK, run on a JRE as a non-root user. Base images are pinned by tag and digest (Temurin 25, Ubuntu 24.04).
ARG JDK_IMAGE=eclipse-temurin:25-jdk-noble@sha256:ba0f0e1a33864105e1d454e88105c617ab40f71062db1f306bf58be32a3f8e5e
ARG JRE_IMAGE=eclipse-temurin:25-jre-noble@sha256:3f2223fe04b750aa5a8179e29b5c9a1ddf7665fc9bb08bfe11ad01d143bc13a5
```

The comment states the idea: **multi-stage**. One Dockerfile holds two builds. `ARG` defines a build-time variable with a default. Both base images are named by tag **and digest**: `eclipse-temurin:25-jdk-noble@sha256:...`. The tag says which release (Temurin, an OpenJDK build, Java 25, Ubuntu 24.04 "noble"). The digest is a hash of the exact image bytes, so the image can never change under the tag. `jdk` is the full development kit (compiler, `jcmd`); `jre` is the smaller runtime.

```dockerfile
FROM ${JDK_IMAGE} AS build
# The Maven wrapper downloads Maven with curl; the JDK image has neither curl nor wget.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /src
```

`FROM ... AS build` starts the first stage, named `build`, from the JDK image. The `RUN apt-get ...` line installs `curl` because the Maven wrapper needs `curl` or `wget` to download Maven and this image has neither. `&&` runs the next command only if the previous one succeeded; `\` continues the line; `--no-install-recommends` skips optional packages; `rm -rf /var/lib/apt/lists/*` deletes the package index so it does not stay in the layer. `WORKDIR /src` sets (and creates) the folder for the next instructions.

```dockerfile
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:resolve
COPY config config
COPY src src
RUN ./mvnw -B -q package -DskipTests -Djacoco.skip=true
```

The order matters for the **layer cache**. Docker caches each instruction's result and reuses it if the instruction and the files it copies are unchanged. So the files that change rarely come first: `.mvn` and `mvnw`, then `pom.xml`, then `RUN ./mvnw ... dependency:resolve`, which downloads the libraries. Only then come `config` and `src`, which change often. If you edit one Java file, Docker reuses the dependency layer and only rebuilds the last step: `./mvnw -B -q package -DskipTests -Djacoco.skip=true`, which compiles and writes the shaded jar.

```dockerfile
FROM ${JRE_IMAGE}
RUN groupadd --gid 10001 app \
 && useradd --uid 10001 --gid app --no-create-home --no-log-init --shell /usr/sbin/nologin app
WORKDIR /app
```

The second `FROM` starts the final image from the small JRE. Everything from the first stage is thrown away unless copied. The `RUN groupadd ... && useradd ...` makes a user and group with the fixed numeric id **10001**, with no home folder (`--no-create-home`), no entry in the login log (`--no-log-init`) and a shell that refuses logins (`/usr/sbin/nologin`).

```dockerfile
COPY --from=build /src/target/spend.jar /app/spend.jar
USER 10001:10001
```

`COPY --from=build` copies just the jar out of the first stage; nothing else (no Maven, no sources, no JDK) is in the final image. `USER 10001:10001` makes every later instruction and the running container use that non-root user, so a bug in the program cannot act as `root` inside the container.

```dockerfile
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/spend.jar"]
```

`ENTRYPOINT` is the program the container runs: `java -XX:MaxRAMPercentage=75 -jar /app/spend.jar`. The JSON-array form ("exec form") starts `java` directly as process 1, so signals such as `SIGTERM` from `docker stop` reach the JVM and its shutdown hook. `-XX:MaxRAMPercentage=75` lets the JVM use up to 75% of the container's memory limit for the heap. `CMD ["--help"]` gives the default **arguments** for the entry point: with no arguments, `docker run image` prints the usage line. Any arguments after the image name replace `CMD`, so `docker run image /data/transactions.csv` runs the report.

## 6. Try it yourself

- `docker build -t spend-test .` (needs Docker and a network). Expected: steps `[build 1/8]` ... and the last line `naming to docker.io/library/spend-test`.
- `docker history spend-test`. Expected: one line per layer with its size.
- `docker run --rm spend-test id` is not possible because the entry point is `java`; use `docker run --rm --entrypoint id spend-test`. Expected: `uid=10001(app) gid=10001(app)`.
- `cat .dockerignore`. Expected: `.git`, `.github`, `target`, `.idea`, `.omc`, `*.log`.

## 7. Mistakes people make

- Copying all sources before downloading dependencies: every edit would repeat the download. The order here avoids that.
- Running as root in the container. `USER 10001:10001` prevents it.
- Using `latest` or a floating tag: the build would change without a commit. Tag plus digest pins it.
- Shell form `ENTRYPOINT java -jar x.jar`: the shell becomes process 1 and swallows `SIGTERM`. The exec form here avoids it.
- Leaving the package index or build tools in the final image; the second stage starts clean.

## 8. Check yourself

1. What is the difference between `RUN` and `ENTRYPOINT`?
   **Answer:** `RUN` runs while the image is built; `ENTRYPOINT` is what runs when a container starts.
2. Why are `pom.xml` and the dependency download before `COPY src src`?
   **Answer:** So Docker reuses the cached dependency layer when only source files change.
3. What does a digest add to a tag?
   **Answer:** It identifies the exact image bytes; a tag can later point to a different image.
4. Why a second `FROM`?
   **Answer:** To start the final image from the small JRE and leave the compiler, Maven and sources behind.
5. What does `USER 10001:10001` do?
   **Answer:** Runs the rest of the build steps and the container as a non-root user and group.

## 9. Further reading

- Dockerfile reference: https://docs.docker.com/reference/dockerfile/
- Multi-stage builds: https://docs.docker.com/build/building/multi-stage/
- Build cache: https://docs.docker.com/build/cache/
- Eclipse Temurin images (25-jdk-noble, 25-jre-noble): https://hub.docker.com/_/eclipse-temurin
