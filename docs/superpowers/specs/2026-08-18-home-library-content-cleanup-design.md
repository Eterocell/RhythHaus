# Home Library Content Cleanup Design

Date: 2026-08-18
Status: approved in chat (bounded change; user confirmed 2026-08-18)

## Problem

The Library home page renders a folder/source management section
(`LibraryManagerCard`) unconditionally, including the configured-sources
list, per-source rescan/remove controls, the add-folder button, and the
terminal scan-outcome panel (summary, view/hide report, remove-missing).
The user wants the home page to show only library content; the
add-folder scan option must appear only when the library is first opened
or is empty.

## Current Behavior (verified)

- `feature/library/impl/.../ui/LibraryHomeContent.kt` renders:
  header → empty-state `ImportAudioCard` (only when
  `tracks.isEmpty() && sourcePickerActionVisible`) →
  `LibraryManagerCard` (always) → `ScanningCard` while a scan is active →
  playlists button → browse-mode picker → album/artist/song content.
- The manager card owns `ScanOutcomePanel` (scan summary, rescan/retry,
  report toggle with error list, remove-missing) and `SourceManagerRow`
  (access status, rescan/recover, remove).
- Report expansion state (`reportVisible`) is remembered inside
  `LibraryHomeContent`, keyed by `scanProgress?.session?.id`.
- `:feature:settings` `SettingsScreen` already renders configured-folder
  rows with rescan/remove, an add-folder action, and an
  `activeScanContent` slot (shared-supplied `ScanningCard`). It does not
  render the terminal scan-outcome panel.
- `LibraryRouteOverlays` (shared, `LibraryRoutes.kt`) supplies the
  Settings slots and receives `sources`, `scanProgress`, `scanErrors`,
  and scan callbacks; it does not currently receive
  `onRemoveMissingTracks`.

## Goal

- Home page shows no folder-management or scan-outcome UI when the
  library has content.
- Home shows the add-folder import card only when
  `tracks.isEmpty() && sourcePickerActionVisible` (first open / empty
  library).
- Home shows the scanning-progress card only while the library is empty
  and a scan is active.
- Settings becomes the single folder-management surface, gaining the
  terminal scan-outcome panel (summary, rescan/retry, view/hide report,
  remove-missing) through a Shared-supplied slot, preserving all
  existing functionality.

## Non-Goals

- No new scan/repository/database/playback changes.
- No navigation-model or route changes beyond the Settings slot wiring.
- No dependency changes between modules (settings stays slot-based;
  scan-outcome UI stays in library impl; Shared remains composition
  root).
- No Windows/Linux support.

## Decisions

### 1. Remove the manager section from home

`LibraryManagerCard`, `SourceManagerRow`, and the always-rendered
manager list item are deleted from `LibraryHomeContent`. The now-unused
parameters (`sources`, `onRescanSource`, `onRemoveSource`,
`onRemoveMissingTracks`, `scanErrors`) are removed from
`LibraryHomeContent` and from both call sites in `LibraryAppShell.kt`
(compact route content and wide list-detail). `onClearLibrary` remains
because `ImportAudioCard` still consumes it (the clear button renders
only for `hasImportedTracks`, which stays false on home).

### 2. Empty-state conditions

The import card condition stays `tracks.isEmpty() &&
sourcePickerActionVisible`. The scanning card condition narrows from
`scanProgress?.isActive == true` to `tracks.isEmpty() &&
scanProgress?.isActive == true`, so a rescan started from Settings never
surfaces progress on home.

### 3. Scan-outcome panel moves to Settings via a Shared-supplied slot

`ScanOutcomePanel` becomes a public composable in library impl (same
module as `ScanningCard`), stateless: `reportVisible` is an input and
`onToggleReport` a callback. Shared's `LibraryRouteOverlays` Settings
branch owns the report-expansion state, remembered keyed by
`scanProgress?.session?.id` (preserving the collapse-on-session-change
and survive-item-recycling behavior), resolves the source by
`session.sourceId`, and renders the panel into a new
`scanOutcomeContent: (@Composable () -> Unit)?` slot on
`SettingsScreen` (rendered after `activeScanContent`). The Settings
branch additionally requires `onRemoveMissingTracks`, which
`LibraryAppShell` already receives from `App.kt`.

### 4. Dead code and strings

The deleted home-only composables use library-impl strings
(`library_sources`, `library_empty`, `remove_source`, `recover_source`,
`source_access_available`, `source_access_lost`). After deletion these
strings have no production callers and are removed from
`feature/library/impl` resources. The outcome-panel strings
(`scan_completed`, `scan_cancelled`, `scan_failed`, `scan_summary_format`,
`view_scan_report`, `hide_scan_report`, `remove_missing`, `rescan`,
`retry_scan`, `scan_report_empty`, `scan_report_error_format`) remain in
library impl with the moved public composable. Settings strings are
untouched.

## Risks / Trade-offs

- **Risk: scan feedback is farther from the user.** The home empty-state
  flow still shows progress; terminal outcomes are one Settings
  navigation away. Accepted by the user (outcome_placement = Settings).
- **Risk: Settings gains density.** The outcome panel reuses the
  existing panel layout and slots order; no new visual language.
- **Trade-off: scan-outcome UI stays in library impl.** Keeps the
  module graph unchanged; Settings remains feature-safe with scalar
  inputs and composable slots.
