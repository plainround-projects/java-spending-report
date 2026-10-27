# Docker (Engine 24)

## 1. What it is

Docker runs programs in **containers**: isolated processes that see their own file system, their own network and a limited share of memory and CPU, while using the host's operating-system kernel. A container starts from an **image**, a read-only bundle of files. Docker Desktop on a Mac runs a small Linux virtual machine and starts the containers inside it.

## 2. Why this repository uses it

Containers give the same Java 25 runtime, the same user and the same files everywhere, and they allow limits (read-only file system, no capabilities, memory cap). Here Docker also runs a tool the host does not need to install (the OSV scanner).

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `Dockerfile` | how the image is built (see `dockerfile.md`) |
| `.dockerignore` | what is left out of the build context |
| `Makefile` | `audit`, `docker-build`, `docker-run` |
| `.github/workflows/ci.yml` | the CI jobs that call these targets |

## 4. How it works underneath

- The `docker` command is a client. It talks to the Docker daemon (a background service) over a socket. The daemon pulls images from registries (Docker Hub, `ghcr.io`), builds images, and starts containers.
- A container is **not** a virtual machine: it is a normal Linux process with namespaces (its own view of processes, network, files) and cgroups (resource limits).
- Files written inside a container disappear when it is removed (`--rm`) unless a folder is **mounted** from the host (`-v`) or is a volume.
- Ports: a container has its own network. `-p host:container` (or compose `ports:`) forwards a host port to a container port. Publishing on `127.0.0.1` keeps it reachable only from the same machine.
- On a Mac the first `docker build` is slower: the VM has to download images and packages, and it shares the machine's CPU with everything else.

## 5. Walkthrough of this repository's use

```make
audit: ## known-vulnerability scan of the dependencies with OSV-Scanner (needs Docker)
	docker run --rm -v "$(CURDIR)":/src -w /src ghcr.io/google/osv-scanner:v2.7.0 scan source --recursive /src
```

`docker run --rm -v "$(CURDIR)":/src -w /src ghcr.io/google/osv-scanner:v2.7.0 scan source --recursive /src`: `run` starts a container from an image; `--rm` deletes it when it ends; `-v host:container` **mounts** a host folder inside the container (here the project, at `/src`); `-w /src` sets the working folder; the image is named with a registry (`ghcr.io`, GitHub's), a path and a tag; everything after the image name is the command for the program inside (`scan source --recursive /src`). See `osv-scanner.md`.

```make
docker-build: ## build the multi-stage image
	docker build -t $(IMAGE) .

docker-run: docker-build ## run the image on the sample file
	docker run --rm -v "$(CURDIR)/sample":/data:ro $(IMAGE) /data/transactions.csv; test $$? -eq 2
```

`docker build -t plainround/spend:0.1.0 .` builds the image from the `Dockerfile` in the current folder (`.` is the build context) and names it. `docker run --rm -v "$(CURDIR)/sample":/data:ro IMAGE /data/transactions.csv` runs the report in a container: `:ro` mounts the sample folder read-only, and the path after the image is passed to the program as its argument. The program exits 2 (rows rejected), so the recipe ends with `test $$? -eq 2`.

## 6. Try it yourself

- `docker version`. Expected: Client and Server sections, version 24 or newer.
- `docker run --rm hello-world`. Expected: a short message `Hello from Docker!`.
- `docker images | head`. Expected: the images on your machine (after a build, one named `plainround/spend`).
- `make audit`. Expected: `No issues found`.

## 7. Mistakes people make

- Assuming the daemon is running. `Cannot connect to the Docker daemon` means Docker Desktop is not started.
- Putting secrets in an image or a Dockerfile; they stay in the layers. This repository has none (see `.env.example`).
- Mounting the project folder read-write when read-only is enough (`:ro`).
- Running as root. The image defines a non-root user.
- Forgetting `--rm` and collecting stopped containers.

## 8. Check yourself

1. What is the difference between an image and a container?
   **Answer:** An image is the read-only template; a container is a running (or stopped) instance of it.
2. What does `-v "$(CURDIR)":/src` do?
   **Answer:** It mounts the current folder into the container at `/src`.
3. Is a container a virtual machine?
   **Answer:** No. It is an isolated process sharing the host kernel.
4. What does `--rm` do?
   **Answer:** Deletes the container when it exits.
5. Where does `docker run ghcr.io/google/osv-scanner:v2.7.0` get the image the first time?
   **Answer:** It pulls it from the GitHub Container Registry.

## 9. Further reading

- Docker Engine 24 documentation: https://docs.docker.com/engine/
- CLI reference: https://docs.docker.com/reference/cli/docker/
- Bind mounts: https://docs.docker.com/engine/storage/bind-mounts/
