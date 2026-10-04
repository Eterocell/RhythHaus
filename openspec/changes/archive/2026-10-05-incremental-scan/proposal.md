# Proposal: Incremental scan experience

## Why
Explicit rescans currently expose progress and errors but do not explain how the library changed. A persisted, bounded change summary lets users understand scan results while preserving the existing explicit deletion and recovery controls.

## What Changes
- Persist a terminal scan change summary with added, modified, unchanged, and missing counts plus bounded details.
- Classify observations using stable source-local identity and size/modification metadata.
- Expose the summary through Shared to the Settings scan report with English and Simplified Chinese accessible labels.
- Keep failed/cancelled scans non-authoritative and keep missing-file deletion explicit.

## Non-goals
No background watcher, automatic startup scan, new platform permission, source identity migration, or automatic deletion.
