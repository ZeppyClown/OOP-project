#!/bin/sh
# Compile Stage 1 into a temp dir and run every registered check (java -ea).
# Usage (from anywhere): sh "stage 1/run-tests.sh"
# Needs JDK 17+. Uses $JAVA_HOME/bin if JAVA_HOME is set, otherwise javac/java on PATH.
set -eu
STAGE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BIN=${JAVA_HOME:+$JAVA_HOME/bin/}
OUT=$(mktemp -d "${TMPDIR:-/tmp}/stage1-tests.XXXXXX")
trap 'rm -rf "$OUT"' EXIT

find "$STAGE/src/main/java" -name '*.java' -type f | sed 's/.*/"&"/' > "$OUT/main.txt"
"${BIN}javac" --release 17 -d "$OUT/main" "@$OUT/main.txt"

find "$STAGE/src/test/java" -name '*.java' -type f | sed 's/.*/"&"/' > "$OUT/test.txt"
"${BIN}javac" --release 17 -cp "$OUT/main" -d "$OUT/test" "@$OUT/test.txt"

"${BIN}java" -ea -Dstage1.fixtures="$STAGE/test-fixtures" -cp "$OUT/main:$OUT/test" restaurantrush.AllTests
