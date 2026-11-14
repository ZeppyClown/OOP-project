#!/bin/sh
# Compile Stage 2 with an installed JDK 17 or newer; all output is temporary.
set -eu
STAGE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$STAGE/java-env.sh"
. "$STAGE/compile-sources.sh"
OUT=$(mktemp -d "${TMPDIR:-/tmp}/stage2-run.XXXXXX")
trap 'rm -rf "$OUT"' EXIT
compile_sources "$OUT/classes" "$STAGE/src/main/java"
"$JAVA" -cp "$OUT/classes" restaurantrush.Main "$@"
