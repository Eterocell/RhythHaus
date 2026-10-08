# Proposal

## Why

Desktop users can add folders through the native picker, but cannot drag music folders or files from Finder into RhythHaus. This makes the desktop import path slower than the existing local-first model and leaves the remaining cross-platform import policy implicit.

## What Changes

- Accept desktop drag-and-drop of folders and supported audio files in the shared application surface.
- Register dropped folders as reference-backed JVM folder sources without copying media.
- Register dropped audio files through a stable managed desktop source that scans the dropped files without copying them.
- Reject unsupported, unreadable, duplicate, and mixed-invalid drops with localized recoverable feedback; readable siblings remain importable.
- Reuse the existing App-owned scan coordinator, source identity, deduplication, playback, and remove-missing lifecycle.
- Expose accessible drag-target state and keyboard-equivalent import fallback through the existing picker.
- Do not add filesystem watching, Android/iOS drag behavior, or automatic startup scanning.

## Capabilities

### New Capabilities

- `desktop-drag-import`

### Modified Capabilities

- `local-library-scanning`

### Removed Capabilities

None.

## Impact

- Shared application composition and JVM desktop platform seams gain a narrow drop-result contract.
- The Library feature owns desktop path validation and source construction; Shared remains the source-registration and scan coordinator.
- No database schema change is required; existing `LibrarySource` and scanner persistence are reused.
- Existing folder-picker and iOS/Android behavior remains unchanged.
