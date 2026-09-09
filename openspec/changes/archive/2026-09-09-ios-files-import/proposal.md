## Why

The current iOS source picker only creates one RhythHaus app-local directory, so users cannot bring existing audio files from Files.app into the library. A first-class import path is required before the local-first player can be usable on a clean iOS installation and must preserve the existing shared scan, persistence, and playback contracts.

## What Changes

- Add a shared callback-first import contract for selecting audio files or directories on iOS.
- Implement the iOS bridge with `UIDocumentPickerViewController` and security-scoped access only for the duration of copying.
- Copy selected supported audio files into a managed RhythHaus sandbox directory with deterministic identities and duplicate-safe behavior.
- Keep the resulting managed directory behind the existing `LibrarySource` and scanner/playback seams.
- Expose cancellation, picker failure, unsupported-file, duplicate, and copy-failure outcomes without partially publishing an invalid source.
- Preserve the existing app-local source as the managed destination and keep Android/macOS source behavior unchanged.
- Add user-facing import progress/result state and the minimum onboarding copy explaining that imported files are copied into app storage.
- Register the existing Files-visible `Documents/RhythHaus` app-local source by default and scan it once when first created; selecting that managed directory or its contents must scan it without copying files back into itself.

## Capabilities

### New Capabilities

- `ios-files-import`: Select local audio files or folders through Files.app, copy them into RhythHaus-managed app storage, and publish them as a playable local library source.

### Modified Capabilities

- `local-library-scanning`: Extend the iOS local-source workflow from creating an empty app-local folder to importing selected Files.app content into that managed source before scanning.

## Impact

- Affected shared library import contracts, iOS library implementation, iOS Swift document-picker bridge, localized resources, and focused common/JVM/iOS tests.
- No database schema change: imported tracks continue to use the existing source and scanner persistence model.
- No Android or macOS picker behavior change.
- MusicKit, persistent security-scoped bookmarks, cloud sync, and direct tag writing remain out of scope.
