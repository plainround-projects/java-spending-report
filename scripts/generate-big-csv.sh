#!/usr/bin/env bash
# Usage: generate-big-csv.sh ROWS OUTPUT
# Writes ROWS transactions in the sample's format; every 1000th row is bad on purpose.
set -euo pipefail
rows="${1:?rows}"
output="${2:?output file}"
mkdir -p "$(dirname "$output")"
awk -v rows="$rows" 'BEGIN {
  print "date,description,amount,currency,category";
  split("Groceries,Travel,Books,Health,Fun,Eating Out,Home", categories, ",");
  for (row = 1; row <= rows; row++) {
    month = (row % 12) + 1; day = (row % 28) + 1; category = categories[(row % 7) + 1];
    if (row % 1000 == 0) { printf "2026-%02d-%02d,Bad %d,oops,USD,%s\n", month, day, row, category; continue }
    printf "2026-%02d-%02d,Shop %d,%d.%02d,USD,%s\n", month, day, row, (row % 500) + 1, row % 100, category
  }
}' > "$output"
echo "wrote $rows rows to $output ($(wc -c < "$output" | tr -d ' ') bytes)" >&2
