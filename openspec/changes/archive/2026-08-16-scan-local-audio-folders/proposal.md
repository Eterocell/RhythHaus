# Scan local audio folders

## Why

RhythHaus currently imports one or more selected audio files. Users need a durable local-library workflow that lets them add a folder/source, recursively scan audio, persist the library, and manage rescans/errors instead of selecting individual files every session.

## What Changes

- Add a shared local library scanning capability backed by a KMP database.
- Replace the primary import UX with a folder/source library manager.
- Support recursive scanning on Android, macOS/JVM, and iOS using platform-appropriate source semantics.
- Preserve existing shared playback by producing persisted `AudioSource.FilePath` or `AudioSource.Uri` handles.

## Scope

- Android: Storage Access Framework tree picker, persisted URI permission, recursive document traversal.
- macOS/JVM: native folder picker and recursive filesystem traversal.
- iOS: app-local music folder provisioning/scanning for now.
- Shared: SQLDelight or equivalent KMP persistence, repository, scan orchestration, scan progress/cancel/rescan/remove-missing/error report UI.

## Out of scope

- Apple Music/media-library access on iOS.
- Arbitrary iOS Files app folder access as the primary model.
- Windows/Linux support.
- Rewriting playback engines unless scan-produced sources reveal a specific gap.
- Custom Kotlin metadata parsers; metadata remains behind the native TagLib wrapper seam.

## Input design

- `docs/superpowers/specs/2026-06-11-select-folder-scan-audio-design.md`
