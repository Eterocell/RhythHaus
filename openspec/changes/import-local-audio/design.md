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

For the original import slice, derive track title from the file name/display name and leave artist/album as local/import placeholders when rich metadata is unavailable. Rich metadata now flows through a separate native TagLib wrapper seam: common code depends on normalized `AudioMetadata`, `:taglib` exposes a path-oriented `TagLibReader`, and platform-specific actuals call or scaffold native TagLib bindings. RhythHaus must not add hand-written Kotlin ID3/FLAC/MP4 parsers for this path.

Current platform state:

- macOS/JVM: a JNI-shaped native helper is built and loaded from JVM resources. It can return an explicit unsupported result from the skeleton when TagLib is not available/linked at build time; once Homebrew/system or packaged TagLib is linked, this same helper is the desktop metadata path.
- Android: Kotlin/JNI call shape exists, but no Android TagLib source, CMake external native build, or per-ABI packaged native library is included yet. Android imports continue to work with filename fallback and do not claim real metadata reads.
- iOS: the Kotlin actual honestly returns unsupported. The expected future layout is documented in `:taglib` Gradle comments (`taglib/native/include/rh_taglib.h`, `taglib/native/src/rh_taglib.cpp`, and a TagLib iOS static library/XCFramework); no Kotlin/Native cinterop is committed until those native inputs exist.

Rationale: ID3/metadata extraction differs by platform and codec, and TagLib is the intended parser. Keeping Kotlin at the wrapper/API layer avoids duplicating brittle tag parsers while preserving stable local import/playback fallback behavior.

## Risks

- Android content URI permissions may not survive app restart until persistence is specified.
- Android metadata reads require a packaged native TagLib library and likely an app-cache file path handoff for content URIs before calling the path-oriented wrapper.
- iOS import remains a visible limitation until a document-picker bridge is planned.
- iOS metadata reads require a TagLib static library/XCFramework plus Kotlin/Native cinterop before support can be claimed.
- macOS metadata reads require linking the JNI helper against real TagLib for non-skeleton results and later DMG packaging/codesigning review for native libraries.
- macOS native AVFoundation playback format/runtime behavior still needs packaged DMG/manual validation with representative files.
- Imported tracks are in-memory only until persistence is planned.

## Verification

- Shared/JVM tests for imported track mapping.
- Desktop compile and Android debug build.
- iOS simulator test must still pass even if iOS importer reports unsupported.
- OpenSpec validation for the import-local-audio change.
- Manual playback validation requires selecting real local audio files on a device/desktop.
