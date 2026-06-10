## Context

RhythHaus now has a shared playback controller and platform playback engines. The visible library should be populated only by local audio imports, without sample/demo playback paths. A full local-library scanner is platform-sensitive and should be separate from this first import slice.

## Decisions

### Decision 1: Manual import before full scan

Implement a manual file import path first. The shared app asks a platform importer for audio files and receives `ImportedAudioFile` records. Common code maps those records to `Track` values and `PlayableTrack` queue items.

Rationale: Manual import proves playback with real local handles without committing to Android MediaStore permissions, macOS recursive folder indexing, or iOS media-library/MusicKit strategy.

### Decision 2: Shared importer contract with platform actuals

Add `AudioImportLauncher` in common code with platform implementations:

- Android: Activity Result `OpenMultipleDocuments` supplied from `androidApp` as an injected launcher, returning content URI strings.
- iOS: first slice returns unsupported from shared Kotlin because presenting `UIDocumentPickerViewController` cleanly needs a Swift/UIKit bridge decision.
- macOS/JVM: native AWT `FileDialog` multi-select, returning absolute file paths. On macOS this presents the system Finder-style open panel instead of Swing's cross-platform chooser.

Rationale: Android needs Activity-owned registration; desktop can use JVM APIs directly; iOS document picking requires view-controller/delegate lifecycle work that is too large to hide inside current shared entry without a bridge.

### Decision 3: Metadata-lite tracks

For this slice, derive track title from the file name/display name and leave artist/album as local/import placeholders. Duration remains unknown until the platform player loads it.

Rationale: ID3/metadata extraction differs by platform and codec. Playback with stable local handles is the immediate blocker.

## Risks

- Android content URI permissions may not survive app restart until persistence is specified.
- iOS import remains a visible limitation until a document-picker bridge is planned.
- macOS native AVFoundation playback format/runtime behavior still needs packaged DMG/manual validation with representative files.
- Imported tracks are in-memory only until persistence is planned.

## Verification

- Shared/JVM tests for imported track mapping.
- Desktop compile and Android debug build.
- iOS simulator test must still pass even if iOS importer reports unsupported.
- Manual playback validation requires selecting real local audio files on a device/desktop.
