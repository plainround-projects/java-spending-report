# Changelog

All notable changes to this project are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [0.1.0] - 2026-10-26

### Added

- `spend`, a command-line report over a CSV export of card transactions: totals per category per month, the five largest purchases, and the rows it could not read with line numbers and reasons.
- Exact money (`BigDecimal` from strings, rounded half-even once at print time), `ParsedRow` as a sealed type so a bad row is data, streaming aggregation with a capped list of rejects, and exit status 0, 1, 2 or 64.
- Layered packages (`domain`, `application`, `infrastructure`, `interfaces`) with the dependency rule enforced by ArchUnit.
- Unit, architecture and end-to-end tests, JaCoCo coverage gates, Spotless (google-java-format), Checkstyle, `javac -Xlint:all -Werror`, a dependency audit and a CI workflow.
- A multi-stage, non-root `Dockerfile`, a Maven wrapper and a `Makefile`.
