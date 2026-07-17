# Playlist Screen Task 5 Report

## Status

DONE_WITH_CONCERNS

OpenSpec `playlist-screen` tasks 5.1-5.5 are complete and committed. Tasks 6.1-6.4 and 7.1-7.4 remain unchanged and unchecked. Saved hub/detail CRUD, exact-occurrence playback, duplicate-safe removal/reorder, per-track picker, inline creation, and searchable multi-select are implemented in shared Compose UI. Automated functional and supported platform gates pass; Task 5-specific runtime visual interaction could not be fully verified because Orca could capture the Compose window but could not focus it and marked synthetic scroll/hotkey input unverified.

## Scope delivered

- Replaced Task 4 playlist route placeholders with shared `PlaylistHubScreen` and keyed `PlaylistDetailScreen` content.
- Added trimmed non-empty create/rename validation without uniqueness constraints. Name dialogs close only after `PlaylistStateOwner` returns a revisioned `SnapshotConfirmed`; failed mutations retain confirmed repository state and entered text.
- Added confirmed playlist deletion that returns to the hub only after successful publication. Track removal is also confirmation-backed, and removing the final entry retains the empty playlist.
- Rendered ordered entries with real `PlaylistEntry.id` lazy keys and existing `LazyTrackArtworkImage` thumbnail patterns.
- Built saved-playlist playback from exact visible entry order and selected entry occurrence; duplicate track IDs remain independently playable.
- Added long-press drag reorder that submits one adjacent move at gesture completion plus labeled move-up/move-down controls and track-name semantics.
- Added `Add to playlist` overflow actions to Home Songs, Search, album detail, and artist detail.
- Added one shared picker for existing playlists plus inline create-and-append. Every confirmation calls repository `append`, so repeated confirmations create independent entries.
- Added a playlist-detail searchable browser over authoritative `LibraryTrack` order. Selection is keyed by track ID and confirmation appends selected tracks in filtered visible order.
- Reused existing Miuix controls, `HausColors`, 40-48 dp interaction targets, safe-area layout, responsive route ownership, artwork, and English/Chinese resources. No dependency, repository schema, playback/session schema, Queue-tab control, or unrelated Library/Now Playing redesign was added.

## Strict RED/GREEN evidence

### Initial saved workflow RED

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Result: expected test compilation failure only because Task 5 APIs such as `PlaylistNameDraft`, `movedPlaylistEntryIds`, `savedPlaylistPlaybackRequest`, `AddToPlaylistPickerState`, `PlaylistTrackBrowserState`, and `playlistDetailModel` did not exist.

### First behavior GREEN

The same focused command passed after adding the pure models and shared screens (`BUILD SUCCESSFUL in 5s`). Ten initial tests covered trim/blank validation, failure draft retention, entry-ID move/remove, accessible move availability, selected duplicate occurrence playback, picker append, inline creation, visible-order browser append, keyed selection, and empty-playlist retention.

### Cross-surface RED/GREEN

Three more tests were added for exact-track picker actions, create-then-append planning, and authoritative title/artist/album filtering including Chinese text. The RED attempt first stopped at the intentionally missing Search picker parameters; after completing all four surface seams and shell ownership, the focused Task 5 plus route command passed (`BUILD SUCCESSFUL in 6s`).

Final focused command after real entry keys, retry-state restoration, artwork reuse, and drag finalization:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
```

Post-commit result: PASS (`BUILD SUCCESSFUL in 454ms`; 26 actionable tasks, 4 executed and 22 up-to-date).

## Verification evidence

- Complete supported matrix after `./gradlew --stop`: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 20s`; 101 actionable tasks, 13 executed and 88 up-to-date). The only source warning was the unchanged Android `MediaMetadata.Builder.setArtworkData` deprecation.
- Post-commit supported matrix passed again (`BUILD SUCCESSFUL in 3s`; 101 actionable tasks, 4 executed, 1 from cache, 96 up-to-date).
- `openspec validate playlist-screen --strict`: PASS before and after commits (`Change 'playlist-screen' is valid`).
- OpenSpec status: tasks 5.1-5.5 checked; Task 6 and Task 7 remain unchecked.
- English/Chinese Compose resource parity: PASS, 136 keys in each locale.
- `GIT_MASTER=1 git diff --check`: PASS.
- Kotlin LSP diagnostics are unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation/tests are the executable Kotlin checks; no LSP-clean claim is made.
- Repository-wide `spotlessCheck` is blocked by a pre-existing import-order violation in untouched `androidApp/build.gradle.kts`. `:shared:spotlessKotlinCheck` is also blocked by pre-existing violations in 50+ untouched shared files. No broad `spotlessApply` was run.
- One first fresh full-matrix run failed the untouched timing-sensitive `PlaybackControllerTest.stopAfterCurrentStopsAtCurrentTrackEndWithoutAdvancing` immediate asynchronous assertion at line 384. Its forced isolated rerun passed (`BUILD SUCCESSFUL in 10s`), and the subsequent fresh complete matrix passed. Task 5 does not modify repeat/completion logic; no unrelated playback test or production fix was made.

## Visual QA

- `./gradlew :desktopApp:run --configuration-cache` reached `:desktopApp:run` for the Task 5 worktree and launched PID `61866` with an 800x600 `RhythHaus` window.
- Orca status/runtime, Accessibility, screenshots, and element frames were available. The accessibility tree exposed existing Chinese/Japanese album labels without dropped glyphs and confirmed the wide list-detail shell.
- Task 5-specific interaction was not accepted: the app restored a deep Albums scroll position where the Playlists entry was not visible; Orca could not focus the Compose window; scroll and `CmdOrCtrl+Home` were marked `synthetic_input` and unverified; the sole scroll attempt unexpectedly opened an album detail.
- The current model cannot decode the captured PNG, no reference baseline exists, and no trustworthy resize/theme automation was available. Therefore compact, wide Task 5 content, light/dark, dialogs, duplicate rows, drag, and CJK pixel-level rendering remain manual acceptance gaps. No visual PASS is claimed for those states.
- Source-level visual review confirms a real shared Compose component tree rather than a raster mock, semantic `HausColors`, Miuix controls, existing artwork primitives, adaptive route ownership, localized labels, and 40-48 dp controls.

## Commits

- `60cbcd0 feat: add saved playlist screens`
- `7d0f2a0 feat: add playlist actions to track rows`
- `223f0da feat: wire saved playlist workflows`
- `c83dc7f docs: complete saved playlist UI tasks`

Every commit uses the repository's semantic English style plus the required Sisyphus footer and co-author trailer. No push was performed.

## Concerns and deferrals

- Manual Task 5 visual/interaction acceptance remains required for compact/wide, light/dark, CJK, keyboard dialogs, duplicate rows, drag, and labeled move controls because desktop automation input was unverified.
- Task 6 owns Queue-tab presentation and controls and was not implemented or marked complete.
- Task 7 owns full iOS FK proof, full target-device/manual acceptance, final integration evidence, roadmap/progress completion, and final review. No iOS simulator or iOS FK proof is claimed by Task 5.
- Repository/shared Spotless and Kotlin LSP remain blocked by pre-existing project/tooling state described above.
- The five pre-existing modified files `.superpowers/sdd/progress.md` and `task-1-report.md` through `task-4-report.md` were preserved and never staged. This `task-5-report.md` is intentionally uncommitted per the user request.

Route: openspec+superpowers / strict RED-GREEN Task 5
Owner: implementation
Input: `.superpowers/sdd/task-5-brief.md`, approved OpenSpec/design/plan artifacts
Output: four atomic commits plus this uncommitted report
Next owner: Task 6 implementation after Task 5 review acceptance
Blockers: manual Task 5 runtime visual/interaction acceptance; no focused JVM, complete supported matrix, desktop compile, Android assemble, OpenSpec, resource-parity, or diff-hygiene blocker

## Review findings follow-up — 2026-07-17

### Status

All five Task 5 review findings are resolved and committed. OpenSpec 5.1-5.5 were explicitly reopened in `e0eb0ce`, revalidated after implementation, and re-completed in `af710c9`. Tasks 6.1-6.4 and 7.1-7.4 remain unchanged and unchecked.

### Root causes and coherent fixes

1. **Active modal mutation failures:** `PlaylistStateOwner` produced a revisioned terminal action, but App's launcher exposed only success to callers. App now reduces the outcome once and returns that same `SnapshotConfirmed` or `MutationFailed` action to the initiating workflow. Create/rename retain their local draft and show localized `playlist_mutation_failed` inside the still-open dialog on failure. Picker/browser retain selected playlist, entered name, query, and selected track IDs in `PlaylistState`, remain open after failure, and render the same localized error inside the active modal. Opening or editing a modal clears stale mutation error state without changing the revisioned owner contract.
2. **Retained-snapshot read failures:** hub/detail presentation now derives a non-destructive read-failure notice independently from confirmed content. A confirmed snapshot remains visible while localized `playlist_load_failed` and an explicit retry action render alongside it. Initial unconfirmed failure behavior remains unchanged.
3. **Search accessibility:** Search result overflow now consumes `SearchAddToPlaylistPresentation(trackId, trackTitle)`, applies a localized track-specific semantic label, and dispatches the exact track ID. New EN/ZH `playlist_add_track_accessibility_format` resources preserve parity.
4. **Target-index drag reorder:** saved rows measure their centers with `onGloballyPositioned`. A drag session owns the dragged `PlaylistEntry.id`, continuously resolves the nearest measured target index, and consumes exactly one complete entry-ID order on drag end. It no longer converts only the sign of movement into an adjacent step or emits during pointer movement. Existing labeled move-up/move-down controls remain unchanged.
5. **Presentation wiring coverage:** six new regressions exercise presentation state consumed by the actual Compose paths: create/rename modal failure visibility and draft retention, picker/browser failure visibility and selection retention, retained hub content plus read retry, Search exact-track semantic presentation/action wiring, destructive confirmation gating, and one-shot final drag-order dispatch. These are not detached helper-only assertions; every presentation model is instantiated by its corresponding production modal, route, Search row, confirmation, or drag handler.

No persistence, repository, playback, session, dependency, Queue-tab Task 6, or unrelated Library/Now Playing contract changed.

### Strict RED evidence

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Result: expected test compilation failure (`BUILD FAILED in 2s`) only because the new production presentation APIs did not exist: `playlistNameModalPresentation`, `PlaylistModalNotice`, `playlistPickerPresentation`, `playlistBrowserPresentation`, `playlistRoutePresentation`, `PlaylistRoutePresentationNotice`, `searchAddToPlaylistPresentation`, `playlistDestructivePresentation`, and `PlaylistDragPresentation`.

### GREEN and verification evidence

- Focused Task 5/navigation GREEN after implementation: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 7s`; 26 actionable tasks, 8 executed and 18 up-to-date).
- Fresh complete supported matrix after `./gradlew --stop`: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 21s`; 101 actionable tasks, 19 executed and 82 up-to-date; unchanged Android artwork metadata deprecation only).
- Post-commit focused suite passed (`BUILD SUCCESSFUL in 970ms`; 26 actionable tasks, 5 executed and 21 up-to-date).
- Post-commit complete supported matrix passed (`BUILD SUCCESSFUL in 3s`; 101 actionable tasks, 4 executed, 1 from cache, 96 up-to-date).
- English/Chinese Compose resource parity passed with 137 keys in each locale.
- `openspec validate playlist-screen --strict`: PASS before and after commits (`Change 'playlist-screen' is valid`).
- `GIT_MASTER=1 git diff --check`: PASS.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests are the executable Kotlin checks.
- Task 6 and Task 7 remain unchecked. No push was performed.

### Review-fix commits

- `e0eb0ce docs: reopen saved playlist UI tasks`
- `a095a21 fix: retain playlist modal failures`
- `832bd8b fix: propagate playlist mutation outcomes`
- `519695d fix: label search playlist actions`
- `af710c9 docs: revalidate saved playlist UI tasks`

Every commit uses the repository's semantic English style plus the required Sisyphus footer and co-author trailer. This report remains intentionally uncommitted. The pre-existing dirty `.superpowers/sdd/progress.md` and Task 1-4 reports were preserved and never staged.
## Remaining Task 5 review findings — atomic inline workflow and outcome wiring

Route: systematic-debugging + strict TDD within the approved `playlist-screen` OpenSpec change.

### Root causes and corrections

- Failed delete exposed only a success callback to `PlaylistDetailScreen`; the revisioned `MutationFailed` was reduced globally but could not reach the active confirmation. The delete boundary now returns the actual revisioned outcome. Failure retains the confirmation, confirmed playlist snapshot, and renders localized `playlist_mutation_failed` inside `ConfirmationDialog`; success closes and routes back.
- Inline picker previously called `create` then `append` as two repository mutations. `PlaylistRepository.createWithEntries(name, trackIds)` now publishes the in-memory playlist only after initial entry construction succeeds and performs SQL playlist/entry inserts inside one `database.transaction`. A missing-track FK abort rolls back the playlist. Names remain nonunique and duplicate entries remain independent and contiguous. No schema or migration file changed.
- `PlaylistMutationWorkflow`, `PlaylistMutationDecision`, and `playlistMutationDecision` now define and drive actual create, rename, delete, picker append, picker inline-create, browser append, remove, and reorder outcome callback behavior. The inline picker calls `createWithEntries` directly and retains track/name/selection plus localized failure for retry.

### Strict RED/GREEN evidence

- RED command:
  `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache`
  Failed at test compilation on missing `createWithEntries`, `PlaylistMutationWorkflow`, `PlaylistMutationDecision`, and `playlistMutationDecision` APIs.
- Focused GREEN command:
  `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`
  Passed: `BUILD SUCCESSFUL in 9s`.
- Repository regressions cover in-memory atomic publication, SQL FK rollback, duplicate initial entries, nonunique trimmed names, and contiguous positions.
- Task 5 regressions invoke actual workflow decisions for all mutation surfaces, failed delete snapshot retention, and inline-create retry-state retention.

### Verification evidence

- Supported matrix: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` — passed, `BUILD SUCCESSFUL in 10s`.
- Migration verification: `./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration --configuration-cache` — passed.
- Xcode available: Xcode 26.6 (17F113).
- iOS simulator suite attempted: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` — blocked before Task 5 test compilation by pre-existing JVM-only `Thread` references in `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt:64,340`; no Task 5 file caused the failure.
- `openspec validate playlist-screen --strict` — passed.
- EN/ZH resource parity — passed, 137 keys.
- `GIT_MASTER=1 git diff --check` — passed.
- Kotlin LSP unavailable because `kotlin-ls` is not installed and installation was previously declined; JVM, Android, desktop, and native compilation commands provide compiler diagnostics.
- Global `:shared:spotlessKotlinCheck` remains blocked by 50 pre-existing out-of-scope formatting violations, beginning in Android playback/session files; no repository-wide formatting was applied.

OpenSpec 1.2 and 5.1–5.5 were explicitly reopened and re-completed. Tasks 6.1–6.4 and 7.1–7.4 remain unchecked and untouched. No dependencies, schema, playback/session behavior, or unrelated UI were changed.
