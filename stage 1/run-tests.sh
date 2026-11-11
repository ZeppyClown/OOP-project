#!/bin/sh
# Compile Stage 1 into a temp dir and run every registered check.
# Usage (from anywhere): sh "stage 1/run-tests.sh"
set -eu
STAGE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(dirname -- "$STAGE")
OUT=$(mktemp -d "${TMPDIR:-/tmp}/stage1-tests.XXXXXX")
trap 'rm -rf "$OUT"' EXIT

# Some domain classes currently live in the sibling "stage-1" folder; include it until they move.
MAIN_DIRS="$STAGE/src/main/java"
[ -d "$ROOT/stage-1/src/main/java" ] && MAIN_DIRS="$MAIN_DIRS
$ROOT/stage-1/src/main/java"

printf '%s\n' "$MAIN_DIRS" | while IFS= read -r dir; do
    find "$dir" -name '*.java' -type f
done | sed 's/.*/"&"/' > "$OUT/main.txt"
javac --release 17 -d "$OUT/main" "@$OUT/main.txt"

find "$STAGE/src/test/java" -name '*.java' -type f | sed 's/.*/"&"/' > "$OUT/test.txt"
javac --release 17 -cp "$OUT/main" -d "$OUT/test" "@$OUT/test.txt"

java -ea -Dstage1.fixtures="$STAGE/test-fixtures" -cp "$OUT/main:$OUT/test" restaurantrush.AllTests
