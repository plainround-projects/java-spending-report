# Dockerfile (multi-stage, non-root)

## 1. What it is

A **Dockerfile** is a recipe for an **image**: a sealed package that holds an operating system's files, a Java runtime and your program, ready to start as a **container**. You write instructions (`FROM`, `COPY`, `RUN`, ...) one per line; `docker build` runs them in order and saves the result. Anyone who has the image can start the program in exactly the same environment, without installing Java.

## 2. Why this repository uses it

The repository promises a multi-stage image that runs as a non-root user, with pinned base images, a graceful shutdown (the signal reaches Java) and a health check where there is a server. The Dockerfile is where those promises live.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `Dockerfile` | the recipe, walked through below |
| `.dockerignore` | what is left out of the build |
| `Makefile` | `docker-build` and `docker-run` call it |
| `.github/workflows/ci.yml` | builds it in a job |

## 4. How it works underneath

1. **Build context.** `docker build .` sends the folder `.` (minus `.dockerignore` entries) to the Docker engine. `COPY` can only take files from there.
2. **Instructions run in order**, each in a temporary container started from the result of the previous one. A `RUN` produces a new layer (**layer and build cache**); `COPY` too. Docker caches layers, so the second build of an unchanged step is instant.
3. **Stages.** Everything between a `FROM` and the next `FROM` is one stage. Only the last stage becomes the image; earlier stages are used for their files.
4. **Run time.** `docker run image args` creates a container from the image and runs `ENTRYPOINT` plus the arguments (or `CMD` if none are given) as the user set by `USER`.
5. **The Docker engine on macOS** runs inside a small Linux virtual machine, because containers need a Linux kernel. Files and ports are shared with the Mac by Docker Desktop.
6. **Try it:** `docker run --rm -v "$PWD/sample":/data:ro plainround/spend:0.1.0 /data/transactions.csv` (the exit status is 2).

> New words:
>
> - **layer and build cache**: every instruction in a Dockerfile that changes files produces a layer, a saved set of file changes. Docker keeps layers; when you build again, it reuses all layers up to the first instruction (or copied file) that changed. So instructions that rarely change go first, and files that change often go last.

## 5. Walkthrough of this repository's use

### `Dockerfile`

```dockerfile
# Multi-stage: build with a JDK, run on a JRE as a non-root user. Base images are pinned by tag and digest (Temurin 25, Ubuntu 24.04).
```

A **comment (#)**-style comment (Dockerfiles use `#` too) that states the idea: a ****multi-stage build**** build. The Dockerfile builds with a JDK in the first stage and runs on a JRE as a **non-root user** user in the second. The base images are pinned by tag *and* digest (**registry and tag**).

> New words:
>
> - **comment (#)**: a line (or the end of a line) starting with `#` is ignored by the shell; it is a note for the reader. The first line `#!...` is the only `#` line that has a meaning.
> - **multi-stage build**: a Dockerfile with several `FROM` lines. Each starts a new stage from a new base image. Files can be copied from an earlier stage into a later one, so the large build tools (JDK, Maven, sources) stay behind and the final image holds only the result (a small JRE and the jar).
> - **non-root user**: on Linux the user `root` can do anything. A program that does not need that power should run as an ordinary user, so a bug or an attacker inside the container cannot change system files. The final image creates a user with the fixed number 10001 and switches to it.
> - **registry and tag**: a registry is a warehouse of images on the internet (Docker Hub, GitHub's `ghcr.io`). An image is named `repository:tag`; the tag (`25-jdk-noble`) picks a version. Tags can be moved to point at a new image, so a **digest** (`@sha256:...`, a fingerprint of the exact content) is added to pin it for ever.

```dockerfile
ARG JDK_IMAGE=eclipse-temurin:25-jdk-noble@sha256:ba0f0e1a33864105e1d454e88105c617ab40f71062db1f306bf58be32a3f8e5e
ARG JRE_IMAGE=eclipse-temurin:25-jre-noble@sha256:3f2223fe04b750aa5a8179e29b5c9a1ddf7665fc9bb08bfe11ad01d143bc13a5
```

`ARG` defines a build-time variable with a default value. Two are defined before the first `FROM`, so the `FROM` lines can use them. `eclipse-temurin:25-jdk-noble@sha256:...` reads: the repository `eclipse-temurin` (the Eclipse Temurin build of OpenJDK), the tag `25-jdk-noble` (Java 25, JDK, Ubuntu 24.04 "noble"), and the digest, a hash of the exact image contents, so the image cannot change under the tag. `JRE_IMAGE` is the same for the runtime-only image (**JDK and JRE**).

> New words:
>
> - **JDK and JRE**: the JDK (Java Development Kit) has the compiler and the tools (`javac`, `jcmd`, `jfr`); the JRE (Java Runtime Environment) can only run Java programs and is smaller. The build stage uses the JDK, the final image the JRE.

```dockerfile
FROM ${JDK_IMAGE} AS build
```

`FROM` starts a stage from a base **image**; `${JDK_IMAGE}` inserts the variable; `AS build` names the stage so that a later stage can copy files out of it.

> New words:
>
> - **image**: the sealed, read-only package from which containers are started: a stack of file-system layers (the operating-system files, Java, your jar) plus a note of what command to run. One image can start many containers. An image has a name and a tag, e.g. `plainround/spend:0.1.0`.

```dockerfile
# The Maven wrapper downloads Maven with curl; the JDK image has neither curl nor wget.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*
```

The comment says why: the Maven wrapper downloads Maven with `curl` and the JDK image has neither `curl` nor `wget`. `RUN` executes a command while the image is built and saves the changes as a **layer and build cache**. The command is a chain: `\` continues the line, `&&` runs the next command only if the previous one succeeded. It uses **apt-get**: update the package list, install `curl` and `ca-certificates` (the trusted certificates that let `curl` talk HTTPS), then delete the lists.

> New words:
>
> - **layer and build cache**: every instruction in a Dockerfile that changes files produces a layer, a saved set of file changes. Docker keeps layers; when you build again, it reuses all layers up to the first instruction (or copied file) that changed. So instructions that rarely change go first, and files that change often go last.
> - **apt-get**: the Debian/Ubuntu program that installs software packages. `apt-get update` refreshes the list of available packages; `apt-get install -y --no-install-recommends curl ca-certificates` installs them without asking (`-y`) and without optional extras; removing `/var/lib/apt/lists/*` afterwards deletes the downloaded lists so the layer stays small.

```dockerfile
WORKDIR /src
```

`WORKDIR` sets (and creates) the folder in which the next instructions run.

```dockerfile
COPY .mvn .mvn
COPY mvnw pom.xml ./
```

`COPY source destination` copies files from the **build context** (the project folder on your computer, minus what `.dockerignore` excludes) into the image. First the Maven wrapper settings, then `mvnw` and `pom.xml` (the `./` destination means the current folder, `/src`).

```dockerfile
RUN ./mvnw -B -q dependency:resolve
```

Downloads every dependency and plugin, nothing else. Because only the files that define the dependencies have been copied so far, this layer and build cache stays cached until `pom.xml` changes, so rebuilding after a source change does not download everything again.

```dockerfile
COPY config config
COPY src src
```

Now the files that change often: the Checkstyle configuration and the sources (`src`).

```dockerfile
RUN ./mvnw -B -q package -DskipTests -Djacoco.skip=true
```

Builds the shaded jar without running tests or coverage (`-DskipTests -Djacoco.skip=true`, see `maven.md`). The tests run in CI, not in the image build. This is the end of the first stage.

```dockerfile
FROM ${JRE_IMAGE}
```

The second stage starts from the smaller JRE image. Nothing from the first stage is here unless it is copied explicitly: no JDK, no Maven, no sources.

```dockerfile
RUN groupadd --gid 10001 app \
 && useradd --uid 10001 --gid app --no-create-home --no-log-init --shell /usr/sbin/nologin app
```

Creates a user group `app` with the fixed number 10001 (`groupadd --gid 10001 app`) and a user `app` with the same number (`useradd --uid 10001 --gid app`) that has no home folder (`--no-create-home`), is not recorded in the login log (`--no-log-init`) and cannot log in (`--shell /usr/sbin/nologin`). This is for the non-root user rule.

```dockerfile
WORKDIR /app
```

The folder for the program.

```dockerfile
COPY --from=build /src/target/spend.jar /app/spend.jar
```

Copies one file out of the *first stage* (`--from=build`): the shaded jar, to `/app/spend.jar`. This is what multi-stage build gives: only the result travels.

```dockerfile
USER 10001:10001
```

From here on, and when the container runs, the user is number 10001, not root.

```dockerfile
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/spend.jar"]
```

`ENTRYPOINT` is the command that starts when a container starts, written in JSON-array form so that no shell is involved and the Java program is **process 1** (it receives the stop signal directly). `-XX:MaxRAMPercentage=75` lets the JVM use up to 75% of the container's memory limit for its heap; `-jar /app/spend.jar` runs the shaded jar.

> New words:
>
> - **process 1**: the first process in a container. Signals such as the stop request from `docker stop` go to it. If the Java program is started directly as the container's command it is process 1 and receives the signal, so its shutdown hook runs; if a shell were put in front, the shell would get the signal instead.

```dockerfile
CMD ["--help"]
```

`CMD` gives the **default arguments** for the entry point: with no arguments, `docker run image` prints the usage line. Arguments after the image name replace `CMD`, so `docker run image /data/transactions.csv` runs the report.

### `.dockerignore`

```gitignore
.git
.github
target
.idea
.omc
```

`.dockerignore` lists what is **not** sent to Docker as the build context. Each line is a pattern. `.git` (the history), `.github` (CI files), `target` (build output), `.idea` (an editor's folder) and `.omc` (a tool's folder) are left out, so the build is faster and the image never contains them.

```gitignore
*.log
```

`*.log` leaves out any log file.

## 6. Try it yourself

- `docker build -t plainround/spend:0.1.0 .`. Expected: the build runs both stages and ends with the image name (the first build is slow, later ones use the cache).
- `docker image inspect --format '{{.Config.User}}' plainround/spend:0.1.0`. Expected: `10001:10001`.
- `docker run --rm --entrypoint id plainround/spend:0.1.0` (the JRE image has `id`). Expected: `uid=10001(app) gid=10001(app)`.
- `docker history plainround/spend:0.1.0`. Expected: a list of layers with sizes; the largest are the base image and the jar.

## 7. Mistakes people make

- Copying the whole project before downloading dependencies: every source change would re-download everything. Here `pom.xml` is copied and resolved first.
- Running as root because it is the default. The `USER` line prevents it.
- Using a shell-form `ENTRYPOINT` (`ENTRYPOINT java -jar x.jar`): a shell becomes process 1 and the Java program never sees the stop signal.
- Floating tags (`latest`): the image changes without a commit. Here tag and digest are fixed.
- A JDK in the final image: bigger and with more tools than needed.

## 8. Check yourself

1. Why two `FROM` lines?
   **Answer:** A multi-stage build: the first stage builds with the JDK and Maven, the second contains only the JRE and the jar.
2. What does `USER 10001:10001` do?
   **Answer:** Runs everything after it, and the container, as a non-root user.
3. Why copy `pom.xml` before `src`?
   **Answer:** So the dependency download layer stays cached until the pom changes.
4. What does the digest after `@sha256:` give?
   **Answer:** A fingerprint of the exact image, so the tag cannot be moved under the build.
5. Why is `ENTRYPOINT` in JSON-array form?
   **Answer:** No shell is started, so the Java program is process 1 and receives the stop signal.

## 9. Further reading

- Dockerfile reference: https://docs.docker.com/reference/dockerfile/
- Multi-stage builds: https://docs.docker.com/build/building/multi-stage/
- Eclipse Temurin images: https://hub.docker.com/_/eclipse-temurin
