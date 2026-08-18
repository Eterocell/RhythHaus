## Why

The Library home page renders an unconditional folder/source management
section (`LibraryManagerCard`): configured-sources rows with rescan and
remove controls, the add-folder button, and the terminal scan-outcome
panel (summary, view/hide report, remove-missing). This clutters the
home page even when the library already has content. The home page
should show library content only; the add-folder scan option should
appear only when the library is first opened or is empty.

## What Changes

- Remove the folder-management section (source rows, rescan/remove
  controls, add-folder button, scan-outcome panel) from the Library home
  page.
- Keep the home add-folder import card only when the library is empty
  (`tracks.isEmpty() && sourcePickerActionVisible`).
- Show the home scanning-progress card only while the library is empty
  and a scan is active.
- Move the terminal scan-outcome panel into Settings through a
  Shared-supplied composable slot, so rescan/retry, view/hide report,
  and remove-missing remain reachable and Settings becomes the single
  folder-management surface.
- Remove now-dead home-only source-management UI and its unused
  library-impl strings.

## Capabilities

### New Capabilities

- `home-library-content-cleanup`: Defines when folder-management and
  scan UI may appear on the Library home, and that Settings is the
  folder-management surface including the terminal scan-outcome panel.

### Modified Capabilities

None.

## Impact

- Library home UI (`:feature:library:impl` `LibraryHomeContent`).
- Settings UI (`:feature:settings` `SettingsScreen`) gains one
  Shared-supplied composable slot.
- Shared composition (`LibraryAppShell` / `LibraryRoutes`) wiring and
  scan-outcome state ownership.
- Common/JVM UI tests for home and Settings; route-adapter tests.
- No API, dependency, database, scanner, playback-engine, or
  platform-source changes.
