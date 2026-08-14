#!/usr/bin/env bash
set -euo pipefail

base_revision=${1:?"usage: $0 <base-revision> [target-revision]"}
target_revision=${2:-HEAD}

protected_paths=$(git diff --name-only f0310e5^..adb1e3d -- shared/src | sort)
candidate_paths=$(git diff --name-only "$base_revision..$target_revision" -- shared/src | sort)
duplicate_paths=$(comm -12 <(printf '%s\n' "$protected_paths") <(printf '%s\n' "$candidate_paths"))

if [[ -n "$duplicate_paths" ]]; then
    printf 'duplicate package-move paths detected between %s and %s:\n%s\n' \
        "$base_revision" "$target_revision" "$duplicate_paths" >&2
    exit 1
fi

printf 'no duplicate package-move paths detected between %s and %s\n' \
    "$base_revision" "$target_revision"
