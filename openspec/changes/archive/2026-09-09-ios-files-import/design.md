## Context

The existing common picker contract returns a `LibrarySource`. Android and JVM implementations already select persistent source handles, while the iOS implementation only creates the app-local directory. The iOS host already uses a Swift `UIDocumentPickerViewController` provider pattern for playlist backup and registers native providers before `MainViewController` is created.

The managed destination is the existing app-local music directory returned by `appLocalMusicFolderPath()`. The scanner and database already know how to scan that directory as `LibraryPlatformKind.IosAppLocal`; no schema change or new persisted external handle is required.

## Goals / Non-Goals

**Goals:**

- Let iOS select both audio files and directories through Files.app.
- Copy selected content into the existing managed music directory.
- Keep external security-scoped access balanced around enumeration and copy only.
- Make repeated imports deterministic and duplicate-safe.
- Preserve the current common scan orchestration and source identity.
- Report cancellation, unavailability, failure, and copy counts through the existing callback-first picker seam.
- Keep the implementation testable without requiring a simulator to present a picker.

**Non-Goals:**

- MusicKit or Apple Music integration.
- Persistent security-scoped bookmarks or playback from external URLs.
- Android or macOS picker changes.
- Audio transcoding, tag rewriting, cloud upload, or database schema changes.
- Full background import while the app is suspended.

## Decisions

### Keep one source-picker seam and change only the iOS adapter

The common `PlatformFolderPickerLauncher` remains the application-facing seam so `App` continues to normalize the returned `LibrarySource` and start the existing scan coordinator. The iOS actual launcher delegates to an injected native provider; Android and JVM actuals remain unchanged. The result remains `Success(source)` after the copy operation, so existing persistence and playback continue to use `ios-app-local` and local file paths.

The iOS provider ABI is declared in an iOS-facing shared source set and retained by a bridge singleton, following the existing playlist document provider pattern. Swift owns UIKit picker presentation and Foundation file access; Kotlin owns Compose state, source normalization, scan admission, and repository publication.

### Use one multi-selection document picker with file and folder content types

The provider presents `UIDocumentPickerViewController(forOpeningContentTypes: [UTType.audio, UTType.folder], asCopy: false)` with multiple selection enabled. Each selected URL is processed as a file or recursively enumerated directory. External URLs are never returned to Kotlin and never stored as source handles.

### Copy into a deterministic managed namespace

The destination is the existing app-local music directory. For each source file, preserve its last path component after removing path separators and invalid filename characters. If the destination path is free, copy atomically through a temporary file and rename. If it exists, compare file size and bytes; identical content is reported as duplicate and retained, while different content receives a deterministic numeric suffix. This preserves useful filenames for metadata fallback while preventing accidental overwrite.

The implementation does not promise global content deduplication across different names; duplicate safety is defined by the managed destination identity and byte comparison at that destination. The scanner's existing source-local identity then prevents duplicate database tracks on the subsequent scan.

### Default-register the managed source and bypass self-import

`appLocalMusicFolderPath()` already resolves to the Files-visible `Documents/RhythHaus` directory; it is the existing `ios-app-local` source handle and playback root, not a new child directory. At first iOS startup, Shared obtains that platform-default source and persists it only when no `ios-app-local` source exists. The normal App-owned coordinator then performs one initial scan. Existing source records are retained unchanged, so the default never produces a duplicate source or rewrites a legacy handle.

The native importer compares every selected URL against the managed directory after path standardization and symlink resolution. A selection equal to the directory or beneath it is already managed: it is excluded from security-scoped enumeration and copy. If the selection contains only managed content, Swift returns a dedicated already-managed terminal status; Kotlin maps it to the existing source success path so the normal scan runs. A mixed selection still copies only external items. This prevents recursive self-copy, suffixes, and misleading imported counts.

### Use bounded, serial operation state

The provider has one active operation at a time. A new launch while a picker or copy operation is active returns a failure result rather than presenting a second picker. Picker delegate callbacks verify the current controller before finishing. Cleanup is idempotent and happens on cancellation, unavailable presenter, failure, and success.

The provider performs the copy synchronously on the document callback's background queue and returns to the main queue for completion/UI teardown. The first implementation reports aggregate counts through a stable serializable result object; detailed per-file diagnostics remain in the native error message and later can be promoted without changing the source contract.

### Keep import and scan as two observable phases

Import completion returns the existing source only after copying settles. Common `App` then invokes the existing serialized scan coordinator. This avoids publishing tracks before files are durable and keeps scan cancellation semantics unchanged. The current transient `importMessage` is extended to summarize counts and failures; active import disables competing source mutations through the existing coordinator admission path.

## Interfaces

The exact public ABI is intentionally narrow:

- `IOSLibraryImportProvider.importAudio(destinationPath:completion:)` — native provider entrypoint.
- `IOSLibraryImportCompletion.complete(status:imported:duplicates:unsupported:failed:message:)` — terminal callback.
- `IOSLibraryImportBridge.provider` — retained provider injection before Compose startup.
- `PlatformFolderPickResult.Success(LibrarySource)` — existing common success path after a successful or duplicate-only copy operation.
- `defaultPlatformLibrarySource()` — platform default source; iOS returns `Documents/RhythHaus`, Android/JVM return no default.

The iOS actual maps native statuses to `Unavailable`, `Failure`, or the existing source success. Cancellation maps to a distinct no-error outcome at the native boundary and clears the transient message without starting a scan.

## Testing Strategy

- Common/JVM tests cover native-result mapping, source identity preservation, duplicate-only and already-managed success, cancellation mapping, default-source seeding, and picker capability visibility.
- iOS host tests cover pure filename sanitization, deterministic destination collision selection, byte-identical duplicate detection, managed-directory containment, and failure aggregation without UIKit presentation.
- iOS compilation validates Swift/Kotlin ABI alignment.
- Simulator/device smoke validation exercises Files.app selection, directory recursion, cancellation, repeated import, scan, restart, and playback from the copied local path.

## Risks / Trade-offs

- Files.app provider behavior differs between simulator and physical devices; automated tests cannot prove the picker UI itself.
- Synchronous copying in the picker callback avoids a second async ABI but can block the document callback for large files; the first implementation should copy on a dedicated background queue and keep only completion/UI work on main.
- Filename-based destination identity does not deduplicate identical content under different names. A future content-hash manifest can improve this without changing the user-visible source contract.
- The visible Documents marker currently invites manual file placement. The import UI must state that selected files are copied, so users understand why storage usage increases.
