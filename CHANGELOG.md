# Changelog

All notable changes to this project are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [0.1.0] - 2026-10-26

### Added

- `tech/`: 27 notes that teach every technology of this repository from zero, through its own files, with an index in `tech/README.md`.
- `spend`, a command-line report over a CSV export of card transactions: totals per category per month, the five largest purchases, and the rows it could not read with line numbers and reasons.
- Exact money (`BigDecimal` from strings, rounded half-even once at print time), `ParsedRow` as a sealed type so a bad row is data, streaming aggregation with a capped list of rejects, and exit status 0, 1, 2 or 64.
- Layered packages (`domain`, `application`, `infrastructure`, `interfaces`) with the dependency rule enforced by ArchUnit.
- Unit, architecture and end-to-end tests, JaCoCo coverage gates, Spotless (google-java-format), Checkstyle, `javac -Xlint:all -Werror`, a dependency audit and a CI workflow.
- A multi-stage, non-root `Dockerfile`, a Maven wrapper and a `Makefile`.

### Notes

- The container build was run on 2026-11-02 on a quiet machine: `make docker-build` built the image (334 MB, runs as `10001:10001`, user `app`), `make docker-run` printed the sample report with the ten rejected rows and exited with status 2 as expected, and `docker run` with no arguments printed the usage line. `make audit` (OSV-Scanner in Docker) was run earlier.
