# Desktop drag import design

## Decision

Add a JVM-only `PlatformDesktopDrop` seam owned by the Library feature. It converts a `List<Path>` into a validated `DesktopDropResult` containing canonical folders, readable audio files, and localized-safe failure categories. Shared receives the result through a callback and performs source registration plus scanning through the existing `LibraryOperationCoordinator`; the adapter never touches the repository.

Folders reuse `jvmFolderSourceId(canonicalPath)` and the existing recursive `JvmFolderSourceAccess`. Files use a deterministic source ID derived from the sorted canonical file set and a source handle that encodes the file set in a bounded, stable format. A narrow `JvmDroppedFilesSourceAccess` scans only those files, preserving source-local relative keys and original `AudioSource.FilePath` values. Re-dropping an unchanged set reuses the source; a changed set creates a new source while the old source remains subject to existing lifecycle rules.

Compose desktop receives drag events at the application shell and exposes a semantic import target. The target remains available only on JVM; Android/iOS render the existing picker unchanged. A focused import button invokes the existing folder picker as the keyboard fallback. Drop validation occurs before admission and returns localized result categories; valid siblings continue even when some entries fail.

## Error and lifecycle rules

- Empty, invalid-only, unreadable, and unsupported drops publish a recoverable message and do not register a source.
- Duplicate paths are canonicalized and de-duplicated before source identity calculation.
- A drop does not auto-remove missing files and never writes or deletes user files.
- Registration and scan are admitted as one serialized Shared operation; cancellation preserves the source and existing scan report semantics.
- Accessibility exposes target name, accepted content, and active drag state; visual highlighting is not the authority.

## Testing

- JVM Library tests cover canonicalization, stable identity, supported extension filtering, invalid sibling retention, duplicate paths, and file-source scan event paths.
- Shared JVM tests cover one coordinator admission, no duplicate source registration, cancellation/failure publication, and unchanged playback.
- Compose JVM tests cover visible/accessibility drop target state, keyboard fallback, EN/ZH labels, and compact layout reachability.
- Desktop smoke uses a temporary folder and WAV files, then removes all temporary artifacts.
