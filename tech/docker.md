# Docker (Engine 24 / Desktop)

## 1. What it is

**Docker** is the program that builds images and runs containers (see `dockerfile.md` for what those are). The `docker` command talks to a background service, the Docker **engine**. `docker build` makes an image; `docker run` starts a container from it; `docker compose` starts several at once from a file. Containers are isolated from each other and from the host computer; you choose what they share.

## 2. Why this repository uses it

Docker gives every reader the same environment for the demo servers and the service, and the same tool to run the vulnerability scanner without installing it. The Makefile is the only place where the long `docker` commands are written.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `Makefile` | every `docker` command line, listed below |
| `Dockerfile` | what `docker build` builds |
| `.github/workflows/ci.yml` | jobs that call the Makefile targets that use Docker |

## 4. How it works underneath

1. `docker` is only a client. It sends your request to the engine over a local socket. On macOS the engine runs inside a Linux virtual machine managed by Docker Desktop.
2. An **image** is a read-only stack of layers; a **container** is a running copy with a thin writable layer on top. When the container is removed, that layer is gone, which is why anything that must survive is put in a mounted folder (a *volume*).
3. Containers are separated by Linux features: their own process list, their own network, their own file system view, limits on memory and processes.
4. Ports: a container has its own network; to reach it from the host a port is *published* (see `docker-compose.md`).
5. Everything here needs the Docker engine running; if it is not, `docker` reports `Cannot connect to the Docker daemon`.

## 5. Walkthrough of this repository's use

### Every `docker` command in the Makefile

| Command | What it does |
| --- | --- |
| `docker build -t $(IMAGE) .` | `docker build` creates an image from a Dockerfile; `-t NAME:TAG` names ("tags") it; the final `.` is the build context. |
| `docker run --rm -v "$(CURDIR)/sample":/data:ro $(IMAGE) /data/transactions.csv` | `docker run IMAGE ARGS` creates a container from an image and runs it; `--rm` deletes the container when it ends; `-v HOST:CONTAINER:ro` mounts a host folder inside, read-only; the words after the image name are the program's arguments. |
| `docker run --rm -v "$(CURDIR)":/src -w /src ghcr.io/google/osv-scanner:v2.7.0` | The vulnerability scan: `-w /src` sets the working folder inside the container; the image is OSV-Scanner pinned to a version, from GitHub's registry `ghcr.io`. |

The options `-t`, `-v`, `-w`, `--rm`, `-d` and the rest are explained in the rows above. The files these commands use are walked through in `dockerfile.md` and `docker-compose.md`.

## 6. Try it yourself

- `docker version`. Expected: client and server version lines (the server line needs the engine to be running).
- `docker images | head`. Expected: a table of images, including the ones you built.
- `docker ps`. Expected: running containers (none, or the demo/service after `make up`).
- `docker run --rm hello-world`. Expected: a message beginning `Hello from Docker!` (needs network access to Docker Hub the first time).

## 7. Mistakes people make

- Forgetting `--rm`: stopped containers pile up.
- Writing files inside a container and expecting them on the host: use a mounted folder.
- Publishing a port on all interfaces (`-p 8080:8080`) when only the same machine should reach it; here `127.0.0.1:` is written in front.
- Assuming the image is up to date: `docker compose up --build` rebuilds, a plain `up` may reuse an old image.
- Running as root inside the container; the image uses a non-root user.

## 8. Check yourself

1. What is the difference between an image and a container?
   **Answer:** An image is the sealed package; a container is a running instance of it.
2. What does `--rm` do?
   **Answer:** Deletes the container when it ends.
3. What does `-v HOST:CONTAINER:ro` do?
   **Answer:** Shares a host folder with the container, read-only.
4. What does `docker compose up -d --wait` do?
   **Answer:** Starts the services in the background and waits until their health checks pass.
5. Where does the engine run on a Mac?
   **Answer:** In a small Linux virtual machine managed by Docker Desktop.

## 9. Further reading

- Docker overview: https://docs.docker.com/get-started/docker-overview/
- docker run reference: https://docs.docker.com/reference/cli/docker/container/run/
- Compose CLI reference: https://docs.docker.com/reference/cli/docker/compose/
