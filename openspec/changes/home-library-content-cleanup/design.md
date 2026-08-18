## Context

`LibraryHomeContent` renders the manager card unconditionally, mixing
folder management with library browsing. Settings already hosts the
folder-management surface (configured-folder rows, add-folder action,
active-scan slot) but lacks the terminal scan-outcome panel. The change
moves all folder/scan management out of home; Settings becomes the
single management surface while the home page shows only library
content plus the empty-state import card.

## Goals / Non-Goals

**Goals:**

- Home shows no source rows, rescan/remove controls, add-folder button,
  or scan-outcome panel when the library has content.
- Home shows the add-folder import card only when
  `tracks.isEmpty() && sourcePickerActionVisible`.
- Home shows the scanning-progress card only while the library is empty
  and a scan is active.
- Settings renders the terminal scan-outcome panel (summary,
  rescan/retry, view/hide report, remove-missing) through a
  Shared-supplied slot, preserving all existing functionality.
- Scan-report expansion state survives Settings list-item recycling and
  collapses when the displayed session changes.

**Non-Goals:**

- No scan, repository, database, or playback behavior changes.
- No route-model or navigation changes beyond the Settings slot wiring.
- No dependency changes between modules.
- No Windows/Linux support.

## Decisions

### Remove the manager section from home

`LibraryManagerCard` and `SourceManagerRow` are deleted from
`LibraryHomeContent`, along with their always-rendered lazy item. The
now-unused parameters (`sources`, `onRescanSource`, `onRemoveSource`,
`onRemoveMissingTracks`, `scanErrors`) are removed from
`LibraryHomeContent` and from both `LibraryAppShell` call sites (compact
route content and wide list-detail). `onClearLibrary` stays: it feeds
`ImportAudioCard`, whose clear button renders only for
`hasImportedTracks` (always false on home).

### Keep the empty-state import card

The import card condition remains `tracks.isEmpty() &&
sourcePickerActionVisible`, covering first open and empty library. The
scanning card condition narrows from `scanProgress?.isActive == true` to
`tracks.isEmpty() && scanProgress?.isActive == true`, so a rescan
started from Settings never surfaces progress on home.

### Move the scan-outcome panel to Settings via a Shared-supplied slot

`ScanOutcomePanel` becomes a public stateless composable in
`:feature:library:impl` (same module as `ScanningCard`): `reportVisible`
is an input and `onToggleReport` a callback. Shared's
`LibraryRouteOverlays` Settings branch owns report-expansion state,
remembered keyed by `scanProgress?.session?.id` (preserving
collapse-on-session-change and survive-item-recycling), resolves the
source by `session.sourceId`, and renders the panel into a new
`scanOutcomeContent: (@Composable () -> Unit)?` slot on
`SettingsScreen`, rendered after `activeScanContent`. The Settings
branch additionally receives `onRemoveMissingTracks`, which
`LibraryAppShell` already obtains from `App.kt`.

### Remove dead source-management strings

The deleted home-only composables consume library-impl strings
`library_sources`, `library_empty`, `remove_source`, `recover_source`,
`source_access_available`, `source_access_lost`. After deletion these
have no production callers and are removed from `:feature:library:impl`
resources. Outcome-panel strings remain in library impl with the moved
public composable. Settings strings are untouched.

## Risks / Trade-offs

- **Risk: terminal scan feedback is one Settings navigation away.** The
  home empty-state flow still shows active progress; the outcome panel
  moves to the folder-management surface. Accepted by the user.
- **Risk: Settings gains density.** The panel reuses the existing
  outcome layout and slots order; no new visual language.
- **Trade-off: scan-outcome UI stays in library impl.** Keeps the module
  graph unchanged; Settings remains feature-safe with scalar inputs and
  composable slots.
