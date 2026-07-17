## 1. SQLDelight Playlists, Migration, and Repository

- [x] 1.1 Add playlist and playlist-entry SQLDelight tables, ordered queries, indexes, and foreign-key cascades; configure `schemaOutputDirectory`, check in the version-1 baseline database, and add `shared/src/commonMain/sqldelight/migrations/1.sqm` without explicit transaction delimiters.
- [x] 1.2 Add the shared `PlaylistRepository` contract plus in-memory and SQLDelight implementations with trimmed non-empty names, duplicate entries, contiguous transactional ordering, confirmed-state publication, and DI registration.
- [x] 1.3 Change JVM, Android, and iOS database factories to open through schema-aware drivers and prove production foreign-key enforcement on every path; use the schema-aware `JdbcSqliteDriver` constructor with `RhythHausDatabase.Schema`.
- [x] 1.4 Add repository, migration, foreign-key, and cascade coverage for playlist deletion, `removeMissingTracks`, `removeSource`, `clearAll`, rollback, duplicate entries, and empty-playlist retention; run `:shared:verifyCommonMainRhythHausDatabaseMigration`.

## 2. Occurrence-Aware Playback and Session Compatibility

- [ ] 2.1 Lock `PlaylistEntry.id` as the `QueueOccurrence.id` used when a saved playlist becomes a playback queue; only after this decision is committed may Tasks 1 and 2 run in parallel.
- [ ] 2.2 Replace queue-position identity with `QueueOccurrence(id, track)` across playback state, visible-list selection, current occurrence, shuffle/skip order, checkpoint keys, and row highlighting while retaining the underlying engine media identity as the library-track ID.
- [ ] 2.3 Persist ordered occurrence-ID and library-track-ID pairs in one ordered serialized DataStore preference value with an explicit current occurrence; never serialize queue membership as a set.
- [ ] 2.4 Preserve legacy track-ID-only DataStore reads by deterministically normalizing valid legacy snapshots into unique occurrences; retain paused restore, duplicate-preserving reconciliation, and existing playback/session fail-safe semantics.
- [ ] 2.5 Add duplicate-occurrence selection, skip, shuffle, checkpoint, session round-trip, legacy normalization, paused restore, and surviving-current reconciliation tests.

## 3. Serialized Upcoming Queue Commands

- [ ] 3.1 Add controller-owned serialized reorder-upcoming, remove-upcoming, and clear-upcoming commands that validate the latest occurrence queue within the controller boundary and return recoverable outcomes.
- [ ] 3.2 Publish one complete state update and immediate session checkpoint for every accepted command while preserving current occurrence loading, position, play/pause status, repeat mode, shuffle mode, engine generation, and stale callback protections.
- [ ] 3.3 Add controller coverage for duplicate targeting, pinned-current rejection, stale IDs, invalid positions, concurrent command serialization, clear-upcoming, and transport-state preservation.

## 4. Playlist Navigation, State, and Localization

- [ ] 4.1 Add typed playlist-hub and saved-playlist-detail routes, route-aware state, Library home entry, route content wiring, and stale-detail recovery to the hub with a recoverable message.
- [ ] 4.2 Inject `PlaylistRepository` in `App.kt`, own playlist snapshot refresh after confirmed writes and library-source cascades/reconciliation, thread playlist dependencies through Library routes, and add loading/error/retry state, selected tabs, and exact visible-order playback wiring.
- [ ] 4.3 Add complete English and Chinese Compose resource strings for playlist tabs, CRUD, errors, confirmations, queue outcomes, and all accessibility labels/state descriptions.
- [ ] 4.4 Add navigation and state tests for routes, stale recovery, Now Playing bar policy, localized action state, exact visible-order playback, and duplicate occurrence row keys.

## 5. Saved Playlist UI and Add Workflows

- [ ] 5.1 Implement Saved-tab and playlist-detail presentation with create, rename, confirmed delete, ordered entries, empty state, mutation failures retaining confirmed state, and row playback/removal.
- [ ] 5.2 Add drag reorder plus labeled accessible move-up/move-down actions, semantic labels containing playlist or track names, and confirmation-backed destructive actions.
- [ ] 5.3 Add `Add to playlist` actions on Library home, Search, album detail, and artist detail with existing-playlist picking and inline creation; append an independent entry on every confirmation.
- [ ] 5.4 Add the playlist-detail searchable multi-select browser over authoritative library tracks, keyed selection, and visible-order append semantics.
- [ ] 5.5 Add UI-state tests for CRUD, both add workflows, duplicate rows, blank names, failures, confirmation, accessible reorder, and empty-playlist retention. Task 6 follows Task 5 because both modify `PlaylistScreens.kt` and `PlaylistScreensTest.kt`.

## 6. Queue UI and Accessibility

- [ ] 6.1 Implement the Queue tab with a distinct empty state, current occurrence rendered first and pinned, and upcoming occurrences rendered with occurrence-ID Compose keys.
- [ ] 6.2 Wire upcoming reorder, removal, and confirmed clear commands to controller results; refresh state and show a recoverable queue-changed message after rejection.
- [ ] 6.3 Provide drag and labeled move-up/move-down controls only for upcoming rows, semantic labels containing track names, role/state descriptions for current and queued rows, and no current-occurrence mutation affordance.
- [ ] 6.4 Add UI-state and controller-wiring tests for pinning, empty state, duplicate occurrences, rejected stale commands, accessibility semantics, and no overlap with the Now Playing bar after Task 5 is complete and reviewed.

## 7. Integration, Verification, and Evidence

- [ ] 7.1 Verify Task 4's playlist snapshot lifecycle publishes after source-removal and clear-library cascades plus authoritative playback reconciliation; confirm saved-playlist edits do not retroactively mutate an active resolved queue.
- [ ] 7.2 Run the focused and full strict verification matrix, including migration verification and `:shared:iosSimulatorArm64Test`; require actual `LibraryDatabaseIosTest` execution to prove production-driver FK enforcement. If existing common-test `Thread` compilation blocks that test, record `[blocked] iOS FK proof` with exact command/output and stop before final completion unless the user explicitly accepts the unverified iOS risk; iOS main compilation is not proof.
- [ ] 7.3 Perform compact/wide, light/dark, duplicate-row, drag and accessible reorder, dialog keyboard, current pinning, queue-during-playback, Now Playing overlap, and target-device audible queue manual QA; record actual results.
- [ ] 7.4 Create change-specific SDD reports, update `progress.md` and `roadmap.md` with command evidence, review the final scoped diff, complete strict OpenSpec validation, and obtain the final two-stage review only after iOS FK proof executes or the user explicitly accepts the recorded `[blocked] iOS FK proof` risk. Task 7 runs last.
