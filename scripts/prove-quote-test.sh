#!/usr/bin/env bash
# The lesson's step 6 check: with the quote handling deleted, exactly one of the five parser tests
# must fail. Works on a temporary copy, so the working tree is never touched.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
rsync -a --exclude target --exclude .git "$root"/ "$work"/
splitter="$work/src/main/java/com/plainround/spend/infrastructure/csv/CsvFieldSplitter.java"
# Replace the quote toggle with a no-op, i.e. every comma now splits.
sed -i.bak 's/insideQuotes = !insideQuotes;/insideQuotes = false;/' "$splitter"
cmp -s "$splitter" "$splitter.bak" && { echo "mutation did not apply" >&2; exit 1; }
cd "$work"
set +e
./mvnw -q -B test -Dtest=CsvRowParserTest -Djacoco.skip=true -Dspotless.check.skip=true >out.log 2>&1
set -e
failed="$(grep -E 'Tests run:.*Failures' out.log | tail -1)"
echo "$failed"
grep -E '^\[ERROR\]   CsvRowParserTest\.' out.log || true
echo "$failed" | grep -q 'Tests run: 5, Failures: 1' && echo "OK: exactly one of the five parser tests fails" \
  || { echo "UNEXPECTED: see $work/out.log" >&2; trap - EXIT; exit 1; }
