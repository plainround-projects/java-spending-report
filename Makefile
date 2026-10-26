# make up | run | test | lint | down      (make help lists everything)
MVN := ./mvnw
JAR := target/spend.jar
SAMPLE := sample/transactions.csv
BIG := target/big.csv
BIG_ROWS ?= 3000000
IMAGE := plainround/spend:0.1.0

-include .env
export

.PHONY: help up run cents bom test lint format audit check stream-check quote-check docker-build docker-run down clean

help: ## list the targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed -E 's/:.*## /\t/' | sort

# There is no server to start: "up" builds the runnable jar, which is all "run" needs.
up: ## build target/spend.jar (tests skipped; make test runs them)
	$(MVN) -q -B clean package -DskipTests -Djacoco.skip=true
	@ls -l $(JAR)

run: $(JAR) ## print the report for the lesson's sample file (exit status 2: ten rows are bad on purpose)
	@java -jar $(JAR) $(SAMPLE); status=$$?; echo "exit status: $$status"; test $$status -eq 2

cents: $(JAR) ## 0.10 + 0.20 prints 0.30, not 0.30000000000000004
	@java -jar $(JAR) sample/cents.csv 2>/dev/null | grep -E 'total|Treats'

bom: $(JAR) ## a file with a byte-order mark and an accented description reads correctly
	@java -jar $(JAR) sample/bom-and-accent.csv 2>/dev/null | grep -E 'rows read|Caf'

$(JAR):
	$(MAKE) --no-print-directory up

test: ## unit, architecture and end-to-end tests with coverage (target/site/jacoco/index.html)
	$(MVN) -B clean verify

lint: ## Spotless (google-java-format) check, Checkstyle, compiler -Xlint:all -Werror
	$(MVN) -B spotless:check checkstyle:check clean compile test-compile

format: ## rewrite the sources with google-java-format
	$(MVN) -B spotless:apply

audit: ## known-vulnerability scan of the dependencies with OSV-Scanner (needs Docker)
	docker run --rm -v "$(CURDIR)":/src -w /src ghcr.io/google/osv-scanner:v2.7.0 scan source --recursive /src

check: lint test ## everything CI runs except the audit

stream-check: up ## a 3-million-row file read with a 32 MB heap, which only works if the file is streamed
	scripts/generate-big-csv.sh $(BIG_ROWS) $(BIG)
	java -Xmx32m -jar $(JAR) $(BIG) | sed -n '1p;/^Rejected rows/p'

quote-check: ## delete the quote handling in a copy and show that exactly one parser test fails
	scripts/prove-quote-test.sh

docker-build: ## build the multi-stage image
	docker build -t $(IMAGE) .

docker-run: docker-build ## run the image on the sample file
	docker run --rm -v "$(CURDIR)/sample":/data:ro $(IMAGE) /data/transactions.csv; test $$? -eq 2

down: ## remove the build output
	$(MVN) -q -B clean
	rm -f $(BIG)

clean: down
