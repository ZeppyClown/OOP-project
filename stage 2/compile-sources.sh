#!/bin/sh
# Sourced helper: compile all roots in ONE javac invocation. javac argument files
# have their own quoting rules; quoting @file alone does not quote its entries.
compile_sources() {
    classes=$1
    shift
    source_args=$(dirname -- "$classes")/sources.txt
    find "$@" -name '*.java' -type f -exec sh -c '
        for source do
            printf "\""
            printf "%s" "$source" | sed '\''s/\\/\\\\/g; s/"/\\"/g'\''
            printf "\"\n"
        done
    ' sh {} + > "$source_args"
    "$JAVAC" --release 17 -Xlint:all -d "$classes" "@$source_args"
}
