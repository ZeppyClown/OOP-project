#!/bin/sh
# Shared by build/run/test. Select an installed JDK; never install or download one.

jdk_works() {
    [ -x "$1" ] && [ -x "$2" ] || return 1
    compiler_version=$("$1" -version 2>&1) || return 1
    runtime_version=$("$2" -version 2>&1) || return 1
    compiler_major=$(printf '%s\n' "$compiler_version" | sed -n '1s/^[^0-9]*\([0-9][0-9]*\).*/\1/p')
    runtime_major=$(printf '%s\n' "$runtime_version" | sed -n '1s/^[^0-9]*\([0-9][0-9]*\).*/\1/p')
    [ "${compiler_major:-0}" -ge 17 ] && [ "${runtime_major:-0}" -ge 17 ]
}

select_jdk() {
    if [ -n "${JAVA_HOME:-}" ]; then
        JAVAC=$JAVA_HOME/bin/javac
        JAVA=$JAVA_HOME/bin/java
        if ! jdk_works "$JAVAC" "$JAVA"; then
            printf '%s\n' 'JAVA_HOME must name an installed JDK 17 or newer with bin/java and bin/javac.' >&2
            return 1
        fi
        return
    fi

    # Prefer a usable PATH toolchain; this also works without macOS utilities.
    JAVAC=$(command -v javac) || JAVAC=
    JAVA=$(command -v java) || JAVA=
    if jdk_works "$JAVAC" "$JAVA"; then
        return
    fi
    for jdk_candidate in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17; do
        if jdk_works "$jdk_candidate/bin/javac" "$jdk_candidate/bin/java"; then
            JAVAC=$jdk_candidate/bin/javac
            JAVA=$jdk_candidate/bin/java
            return
        fi
    done
    if [ -x /usr/libexec/java_home ]; then
        jdk_candidate=$(/usr/libexec/java_home -v 17 2>/dev/null) || jdk_candidate=
        if [ -n "$jdk_candidate" ] && jdk_works "$jdk_candidate/bin/javac" "$jdk_candidate/bin/java"; then
            JAVAC=$jdk_candidate/bin/javac
            JAVA=$jdk_candidate/bin/java
            return
        fi
    fi

    printf '%s\n' 'No installed JDK 17 or newer found. Set JAVA_HOME to an existing JDK; no download was attempted.' >&2
    return 1
}

select_jdk
