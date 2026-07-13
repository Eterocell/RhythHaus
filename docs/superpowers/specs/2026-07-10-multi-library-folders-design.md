# Multiple Music Library Folders Design

## Summary

RhythHaus will let Android and desktop JVM/macOS users build one local library from multiple independently managed folders. Selecting another folder adds or refreshes that source without replacing existing sources or tracks. iOS remains on its single app-local source because arbitrary multi-folder access is outside the current platform model.

## Current State

`LibrarySource`, `LibraryTrack.sourceId`, and the shared repository already support multiple persisted source rows. Android generates stable source IDs from SAF tree URIs, desktop generates stable IDs from canonical folder paths, and the scanner upserts the selected source before importing source-scoped tracks. The missing behavior is durable source management: `App()` does not retain a source list in Compose state, shared UI does not display configured sources, and the repository cannot remove one source with its dependent data.

## Design

Repeated folder selection is additive. A successful Android or desktop picker result starts a scan for that source; the scanner's existing source upsert refreshes a previously selected source with the same stable ID and inserts a genuinely new source otherwise. Track uniqueness remains `(sourceId, sourceLocalKey)`, so equal relative paths in different folders remain independent.

The composition root owns a current `List<LibrarySource>` alongside the current track list. It refreshes both lists after scans, source removal, and clear-library operations. Shared management UI receives the source list and exposes one row per configured source with its display name, access state, and last-scan state. Available sources can be rescanned. Removing a source requires confirmation and deletes only that source and all dependent tracks, scan sessions, and scan errors in one repository transaction before refreshing UI state.

Folder-add capability is explicit rather than inferred from a label. `PlatformFolderPickerLauncher` exposes whether the current platform supports adding additional sources. Android and desktop return true; iOS returns false. Existing iOS app-local setup/rescan behavior remains available, but no add-another-folder action is shown.

Only one scan runs at a time, preserving the existing scan job, progress, and cancellation model. Add and rescan actions are disabled while a scan is active. Removing the source currently being scanned is also disabled until that scan reaches a terminal state.

## Constraints

- Scope is Android and desktop JVM/macOS for multiple user-selected folders; iOS remains a single app-local source.
- Use repeated single-folder picker launches; do not introduce picker-level multi-select.
- Keep source IDs stable from Android SAF URI and desktop canonical path.
- No new dependencies and no SQLDelight table/column/index migration.
- Source removal must be transactional and source-scoped.
- Preserve current playback, artwork lazy loading, scan progress/cancellation, navigation, clear-library, and theme behavior.
- Do not add Windows/Linux product or packaging support.

## Error Handling

Selecting a folder whose stable source ID already exists rescans and refreshes that source instead of duplicating it. Lost-access sources remain visible with their status and can be removed; rescan reports the existing scan failure path. Picker cancellation and picker failures retain the existing user-facing status behavior. A source is removed from UI only after the repository transaction succeeds.

## Verification

- Repository tests prove two sources persist independently and removing one deletes only its source-scoped tracks and scan history.
- Shared orchestration tests prove source lists refresh after add/scan, remove, and clear operations.
- UI decision tests prove add-folder capability is enabled for Android/desktop semantics and excluded for iOS.
- `openspec validate multi-library-folders --strict`
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
- `/usr/bin/xcrun xcodebuild -version`
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
- `git diff --check`
