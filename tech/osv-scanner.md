# OSV-Scanner (2.7.0)

## 1. What it is

Every library you use is code written by someone else, and sometimes a security problem is found in it and published. OSV-Scanner is a command-line tool from Google that reads a project's dependency list and checks every library and version against OSV (Open Source Vulnerabilities), a public database of known security problems. It prints the libraries with known issues, or `No issues found`.

## 2. Why this repository uses it

The repository promises that dependencies are pinned and "checked by the CI's audit step". Exact versions in `pom.xml` make the list known; the scanner tells you whether any of those versions has a published vulnerability.

## 3. Where it appears

| File | What it does with the technology |
| --- | --- |
| `Makefile` target `audit` | runs the scanner in a container |
| `.github/workflows/ci.yml` | job `audit` calls `make audit` |
| `pom.xml` | the file the scanner reads |

## 4. How it works underneath

1. The scanner walks the folder and finds `pom.xml`. For Maven it extracts the dependencies (direct ones, and, with its Maven resolver, also those they pull in).
2. For each `group:artifact:version` it asks the OSV database whether a known vulnerability affects that version.
3. Exit status 0 means no issues; non-zero means findings or an error, which fails `make audit` and the CI job.
4. The tool runs in a container (`ghcr.io/google/osv-scanner:v2.7.0`), so nothing must be installed on the host.

## 5. Walkthrough of this repository's use

### The command

```make
audit: ## known-vulnerability scan of the dependencies with OSV-Scanner (needs Docker)
	docker run --rm -v "$(CURDIR)":/src -w /src ghcr.io/google/osv-scanner:v2.7.0 scan source --recursive /src
```

| Part | Meaning |
| --- | --- |
| `docker run --rm` | start a container and delete it afterwards |
| `-v "$(CURDIR)":/src` | share the project folder (Make `$(CURDIR)`) with the container as `/src` |
| `-w /src` | start in that folder |
| `ghcr.io/google/osv-scanner:v2.7.0` | the image: the scanner, version pinned, from the GitHub registry |
| `scan source` | scan a source tree (as opposed to an image or a lock file alone) |
| `--recursive` | look in subfolders too |
| `/src` | the folder to scan, inside the container |

A normal run prints `Scanned /src/pom.xml file and found 5 packages` and `No issues found`. A warning about the `pomxml` enricher running on untrusted artifacts is expected: it says the Maven resolver may contact Maven Central.

## 6. Try it yourself

- `make audit`. Expected: `Scanned /src/pom.xml file and found 5 packages` ... `No issues found` (needs Docker running and network access).
- Pin an old, vulnerable version in a scratch copy (for example an old `logback-classic`) and run again. Expected: a table of vulnerabilities with OSV ids and fixed versions.
- `docker run --rm ghcr.io/google/osv-scanner:v2.7.0 --version`. Expected: `osv-scanner version: 2.7.0`.

## 7. Mistakes people make

- Treating "no issues" as "safe": the database only lists known problems.
- Scanning with floating versions: the answer would change without a commit. The pom pins exact versions.
- Ignoring the audit job because it needs the network: it needs Docker and access to `ghcr.io` and the OSV API.
- Auditing only direct dependencies; the scan resolves transitive ones too.

## 8. Check yourself

1. What does OSV stand for?
   **Answer:** Open Source Vulnerabilities, the database the scanner queries.
2. What file does it read in this repository?
   **Answer:** `pom.xml`.
3. Why does the scan run in a container?
   **Answer:** No installation is needed on the host, and the version is pinned by the image tag.
4. What exit status means success?
   **Answer:** 0 (no findings).
5. Does `No issues found` prove the code is secure?
   **Answer:** No. It only means no known vulnerability is listed for those versions.

## 9. Further reading

- OSV-Scanner documentation: https://google.github.io/osv-scanner/
- OSV database: https://osv.dev/
- Usage (`scan source`): https://google.github.io/osv-scanner/usage/
