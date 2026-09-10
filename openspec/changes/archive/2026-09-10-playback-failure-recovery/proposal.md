# Proposal: Playback failure recovery

## Problem

RhythHaus currently exposes a generic playback-error message but gives listeners no explicit recovery path. A failed local track can leave a queue stranded; user-facing causes such as missing files, lost access, unsupported formats, and decoding failures are not reliably represented.

## Goal

Provide shared, non-destructive playback recovery: structured failure classification where supported, retry, skip, and removal of only the failed queue occurrence.

## Scope

- Add failure kinds to the shared playback contract.
- Preserve structured load failures through controller async handling.
- Classify only platform-native evidence; default to unknown.
- Add error-only Now Playing recovery actions: retry, skip, remove from queue.
- Preserve queue occurrence identity, session checkpoints, command gates, and platform isolation.

## Non-goals

No media-file/library/source deletion, source rebind, permission recovery, automatic retries/skips, backoff, SQLDelight migration, or playback engine replacement.

## Affected capabilities

- New: `playback-failure-recovery`.
- Existing `audio-playback` remains behaviorally compatible; its error state becomes actionable.
