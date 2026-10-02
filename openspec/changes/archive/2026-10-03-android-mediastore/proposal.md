# Proposal: Android MediaStore source

## Why

Add an optional Android MediaStore audio source beside existing SAF folder sources. The source is read-only, permission-aware, stable across rescans, and does not copy or mutate media. It gives Android users a library-wide audio view when MediaStore permission is granted while preserving existing SAF behavior and source lifecycle rules.

## What Changes

- Android `READ_MEDIA_AUDIO` on API 33+ and `READ_EXTERNAL_STORAGE` on older supported APIs.
- A persisted `android-mediastore-audio` source with deterministic identity.
- Read-only MediaStore scanning through the existing App-owned coordinator.
- Permission state and recovery guidance in Settings.
- Stable content-URI track sources, source-local IDs, metadata, size, and modification time.

## Non-goals

- Replacing SAF folder sources.
- Writing tags, deleting media, or requesting broad video/image permissions.
- iOS/macOS MediaStore equivalents.
- Automatic startup permission prompts.
