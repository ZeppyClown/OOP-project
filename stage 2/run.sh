#!/bin/sh
# Compile and play Stage 1. Interactive by default; extra arguments go to Main.
# Usage: sh "stage 1/run.sh"                                   (play at the keyboard)
#        sh "stage 1/run.sh" --script "stage 1/test-fixtures/victory-input.txt"
# Needs JDK 17+. Uses $JAVA_HOME/bin if JAVA_HOME is set, otherwise javac/java on PATH.
set -eu
STAGE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BIN=${JAVA_HOME:+$JAVA_HOME/bin/}
OUT=$(mktemp -d "${TMPDIR:-/tmp}/stage1-run.XXXXXX")
trap 'rm -rf "$OUT"' EXIT

find "$STAGE/src/main/java" -name '*.java' -type f | sed 's/.*/"&"/' > "$OUT/main.txt"
"${BIN}javac" --release 17 -d "$OUT/classes" "@$OUT/main.txt"
"${BIN}java" -cp "$OUT/classes" restaurantrush.Main "$@"
