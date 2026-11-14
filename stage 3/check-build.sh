#!/bin/sh
# Verify the actual Stage 3 runner from copied source trees and another cwd.
set -eu
STAGE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUT=$(mktemp -d "${TMPDIR:-/tmp}/stage3-build-check.XXXXXX")
trap 'rm -rf "$OUT"' EXIT
check_report() {
    awk -v verdict="$2" -v paid="$3" -v revenue="$4" -v average="$5" '
        /^-- End of turn / { turns++ }
        /^===== / { reports++; final=1; if ($0 != "===== " verdict " =====") bad=1 }
        final && $0 == "Paid: " paid { p=1 }
        final && $0 == "Revenue: " revenue { r=1 }
        final && $0 == "Average satisfaction of paid customers: " average { a=1 }
        final && $0 == "Turns used: 18 / 18" { t=1 }
        END { exit (bad || turns != 18 || reports != 1 || !p || !r || !a || !t) }
    ' "$1"
}
for NAME in plain-source 'source with spaces and "quotes" \ backslash'; do
    COPY=$OUT/$NAME
    mkdir -p "$COPY"
    cp -R "$STAGE/src" "$STAGE/test-fixtures" "$COPY/"
    cp "$STAGE/run.sh" "$STAGE/run-tests.sh" "$STAGE/java-env.sh" "$STAGE/compile-sources.sh" "$COPY/"
    (cd /tmp && sh "$COPY/run-tests.sh") > "$OUT/tests.log" 2>&1
    for FIXTURE in victory-input defeat-input victory-invalid-input vip-priority-input regular-priority-input flip-happy-hour-on-input flip-happy-hour-never-input; do
        (cd /tmp && sh "$COPY/run.sh" --script "$COPY/test-fixtures/$FIXTURE.txt") > "$OUT/replay.log" 2>&1
        case "$FIXTURE" in
            defeat-input) check_report "$OUT/replay.log" DEFEAT 0 '$0.00' N/A ;;
            vip-priority-input) check_report "$OUT/replay.log" DEFEAT 1 '$6.48' 100.0 ;;
            regular-priority-input) check_report "$OUT/replay.log" DEFEAT 1 '$6.48' 95.0 ;;
            flip-happy-hour-on-input) check_report "$OUT/replay.log" VICTORY 4 '$49.50' 67.3 ;;
            flip-happy-hour-never-input) check_report "$OUT/replay.log" DEFEAT 4 '$56.10' 57.8 ;;
            *) check_report "$OUT/replay.log" VICTORY 5 '$73.38' 89.4 ;;
        esac
    done
    (cd /tmp && sh "$COPY/run.sh" < "$COPY/test-fixtures/victory-input.txt") > "$OUT/stdin.log" 2>&1
    check_report "$OUT/stdin.log" VICTORY 5 '$73.38' 89.4
    awk '/Happy Hour \(yes\/no\)>/ { offers++ }
        /Happy Hour: turn 10 \| remaining 2 \| used true/ { two=1 }
        /Happy Hour: turn 11 \| remaining 1 \| used true/ { one=1 }
        /Happy Hour: turn 12 \| remaining 0 \| used true/ { zero=1 }
        END { exit !(offers == 10 && two && one && zero) }' "$OUT/stdin.log"
    for ROLE in 'Happy Hour' Host Waiter Chef; do
        case "$ROLE" in
            'Happy Hour') : > "$OUT/eof-input.txt" ;;
            Host) printf 'no\n' > "$OUT/eof-input.txt" ;;
            Waiter) printf 'no\nwait\n' > "$OUT/eof-input.txt" ;;
            Chef) printf 'no\nwait\nwait\n' > "$OUT/eof-input.txt" ;;
        esac
        (cd /tmp && sh "$COPY/run.sh" < "$OUT/eof-input.txt") > "$OUT/eof.log" 2>&1
        awk -v role="$ROLE" 'index($0, "Interrupted: Input ended during turn 1, " role) { interrupted=1 }
            /===== / { verdict=1 } END { exit !(interrupted && !verdict) }' "$OUT/eof.log"
    done
    printf 'PASS copied-source tests, seven replays, duration/prompt counts, stdin and four-role EOF: %s\n' "$NAME"
done

# Exercise actual JDK selection using executable paths containing spaces.
. "$STAGE/java-env.sh"
mkdir -p "$OUT/jdk bin with spaces"
ln -s "$JAVAC" "$OUT/jdk bin with spaces/javac"
ln -s "$JAVA" "$OUT/jdk bin with spaces/java"
(
    unset JAVA_HOME
    PATH="$OUT/jdk bin with spaces:$PATH"
    export PATH
    . "$STAGE/java-env.sh"
    [ "$JAVAC" = "$OUT/jdk bin with spaces/javac" ]
    [ "$JAVA" = "$OUT/jdk bin with spaces/java" ]
    sh "$STAGE/run-tests.sh"
) > "$OUT/path-jdk.log" 2>&1
printf '%s\n' 'PASS installed JDK selection on PATH without JAVA_HOME'
if JAVA_HOME="$OUT/missing jdk" sh "$STAGE/run.sh" < /dev/null > "$OUT/invalid-jdk.log" 2>&1; then
    printf '%s\n' 'FAIL invalid JAVA_HOME unexpectedly succeeded' >&2
    exit 1
fi
awk '/JAVA_HOME must name an installed JDK 17 or newer/ { found=1 } END { exit !found }' "$OUT/invalid-jdk.log"
printf '%s\n' 'PASS invalid JAVA_HOME gives an actionable error'
