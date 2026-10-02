# Design: Android MediaStore source

## Boundary

`LibraryPlatformKind.AndroidMediaStoreAudio` identifies one read-only logical source with handle `android-mediastore-audio`. Shared owns source registration, scan admission, state publication, and permission/recovery callbacks. The Android library platform adapter owns permission inspection and `ContentResolver` queries. Settings receives a neutral source projection and a neutral request-permission action; it does not know Android permissions or intents.

## Permission model

The adapter reports `Available` only when the audio permission required by the API level is granted. `LostAccess` means the source remains persisted but cannot scan. The source is never auto-created if permission is absent, and startup never requests permission. Settings offers an Android-host callback to request permission or open app settings. A denied request leaves the source unchanged and does not start a scan.

## Scan

The adapter queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` for audio rows marked `IS_MUSIC != 0`, ordered by `_ID`. It accepts supported audio MIME types or name extensions and emits stable source-local keys from the MediaStore row ID. Playback uses the row content URI. Metadata is read through the existing URI metadata-reader seam, with the scanner's filename fallback. Size and `DATE_MODIFIED` are carried into the existing scan event, guarding missing values and timestamp overflow. Query rows are captured while the cursor is owned by `use`; no cursor survives a sequence suspension. A read-only descriptor probe closes before each candidate is yielded; metadata retriever ownership is confined to metadata extraction. Individual I/O failures become recoverable skipped rows; permission or query failure fails the scan rather than providing successful remove-missing evidence.

## Lifecycle and deduplication

After permission is granted, a separate explicit Add action registers the source through the existing App-owned mutation coordinator. Grant callbacks and startup only refresh access; they never register, scan, or resurrect a deleted source. Scans are explicit and use the existing coordinator. Revocation never removes the source. A rescan updates rows by the source ID plus `mediastore:<rowId>`, while SAF sources remain independent even if they point at the same physical media. Remove-missing uses the existing authoritative completed scan behavior.

Recoverably skipped existing MediaStore rows receive an identity-only seen update. The SQL query changes only `lastSeenScanId`; it does not reconstruct a track from lazy artwork or effective-tag projections. This preserves artwork, overrides, favorites, history and timestamps while genuinely absent rows remain removable. This is a query-only database change, not a schema change.

Shared projects the synthetic source name from the Settings EN/ZH resource before passing sources to presentation. The stable ID and handle remain unchanged; restored sources follow locale changes without a naming migration or changes to SAF labels.

## Verification

Add Android host tests for API-level permission selection, denied/granted access, query projection and closure, stable IDs, unsupported filtering, cancellation, and non-MediaStore fail-closed behavior. Add Shared tests for no startup permission request, source registration, and recovery actions. Validate Android compilation and source-management behavior; manual physical permission and playback acceptance remains user-owned.
