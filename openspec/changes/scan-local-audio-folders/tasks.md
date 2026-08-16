## 1. Existing planning baseline

- [x] 1.1 Validate this OpenSpec change before implementation starts.
- [x] 1.2 Add SQLDelight or equivalent KMP database dependencies and generated database configuration.
- [x] 1.3 Verify dependency setup with focused Gradle tasks before feature code.
- [x] 1.4 Add the previously planned scanner/platform contracts and initial UI wiring where already represented by the existing change history.

The checkpoints below are the amended ora-10/ora-11 implementation contract. They remain unchecked until independently implemented and reviewed. No task below is claimed complete by this amendment.

## 2. Repository contract and SQLDelight behavior

- [ ] 2.1 Define `LibraryRepository` in `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt` and define `RemoveMissingTracksResult` plus `RemoveMissingTracksRejectionReason` in a new API file under `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/`. The exact API is `removeMissingTracks(sourceId: String, requestedScanId: String): RemoveMissingTracksResult`. Add `latestTerminalScanSession(): ScanSession?` as a global query; do not add a source parameter or a snapshot wrapper. Keep source-removal and clear methods as existing repository methods, with serialization owned by App.
- [ ] 2.2 Add/extend the API contract in `feature/library/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryApiContractTest.kt` for the exact method signature, `Removed(count)`, `Rejected(reason)`, and `RemoveMissingTracksRejectionReason`. Run `./gradlew :feature:library:api:jvmTest --tests '*LibraryApiContractTest' --configuration-cache` and capture the expected RED before implementation.
- [ ] 2.3 Add/extend in-memory behavior in `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt` and `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt` for the authoritative-completed-session rule, global terminal query, deterministic ordering, and unchanged tracks on rejection.
- [ ] 2.4 Add RED production SQLDelight coverage in `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt` for the complete rejection matrix: wrong source, stale completed, active, cancelling, cancelled, failed, absent, and missing source; assert explicit enum reasons and unchanged tracks. Also cover equal completion/start times, id tie-breaks, global terminal selection, and `Completed`/`Cancelled`/`Failed` error loading. Run `./gradlew :feature:library:impl:jvmTest --tests '*SqlDelightLibraryRepositoryJvmTest' --configuration-cache`.
- [ ] 2.5 Implement only query/transaction changes in `core/database/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanSession.sq` and `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`. The remove-missing query validates that the requested session is the same source's latest valid `Completed` session ordered by completed/start/id and deletes missing tracks in one transaction. The restoration query is global across terminal statuses and orders by `COALESCE(completedAtEpochMillis, startedAtEpochMillis)`, startedAt, id. Do not add a migration; source removal and clear remain coordinator-serialized repository calls.
- [ ] 2.6 GREEN the API contract with `./gradlew :feature:library:api:jvmTest --tests '*LibraryApiContractTest' --configuration-cache`, then GREEN the in-memory and SQLDelight implementation tests with `./gradlew :feature:library:impl:jvmTest --tests '*LibraryRepositoryContractTest' --tests '*SqlDelightLibraryRepositoryJvmTest' --configuration-cache`. Verify generation, migration consistency, and database build with `./gradlew :core:database:generateCommonMainRhythHausDatabaseInterface :core:database:verifyCommonMainRhythHausDatabaseMigration :core:database:build --configuration-cache`.

## 3. App-owned operation coordinator

- [ ] 3.1 Define the single App-owned coordinator and token/publication interfaces in the existing shared App orchestration path, with admission result, current-token validation, `scan`, `removeMissing`, `removeSource`, and `clear`, plus injectable repository, playlist refresh, playback reconciliation, and cancellation-await dependencies.
- [ ] 3.2 Extend existing `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt` for cancellation-await-before-mutation, stale progress and combined library+playlist publications, token races, and repeated-click admission rejection. Extend `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt` for serialized remove-source/clear/remove-missing operations and coordinator-owned playlist/playback refresh. Run `./gradlew :shared:jvmTest --tests '*AppScanCancellationTest' --tests '*LibrarySourceManagementTest' --configuration-cache` and retain RED before implementation.
- [ ] 3.3 Implement the coordinator in the App-owned production path. A mutation must not write until scan cancellation has reached terminal persistence; every publication must validate the operation token; combined library and playlist snapshots must publish from the same admitted operation.
- [ ] 3.4 GREEN the existing shared tests with the same command, including playback reconciliation and playlist refresh ordering.

## 4. Startup restoration

- [ ] 4.1 Extend the existing startup/restoration coverage in `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt` and, where source mutation behavior is exercised, `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`: restore global latest terminal session and errors when its source exists; suppress it when the source is absent; ignore active/cancelling sessions. Run `./gradlew :shared:jvmTest --tests '*AppScanCancellationTest' --tests '*LibrarySourceManagementTest' --configuration-cache` for RED.
- [ ] 4.2 Implement App startup loading through global `latestTerminalScanSession(): ScanSession?`, restore only `Completed`, `Cancelled`, and `Failed` state plus errors, and suppress the single outcome panel when its source no longer exists. Do not restore active/cancelling state.
- [ ] 4.3 GREEN the existing restart coverage with the same command and verify no new schema migration is created.

## 5. Production Library manager boundary

- [ ] 5.1 Preserve the current intended manager states/actions in `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt` and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`: empty/add-source, scanning/cancel, completed/rescan/add-source/remove-missing/report, cancelled/retry, failed/retry/report, and lost-access/recovery. Use coordinator callbacks only; do not duplicate operation admission in UI.
- [ ] 5.2 Add RED production wiring tests in `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContentJvmTest.kt` for compact and wide layouts, all manager actions, repeated clicks, picker cancellation/recovery, and terminal-state restoration. Run `./gradlew :feature:library:impl:jvmTest --tests '*LibraryHomeContentJvmTest' --configuration-cache`.
- [ ] 5.3 GREEN the production UI tests and verify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` owns exactly one coordinator and wires the same state/actions into compact and wide surfaces. Run `./gradlew :feature:library:impl:jvmTest :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`.

## 6. Integration acceptance and handoff

- [ ] 6.1 Add/retain production-boundary coverage for playback reconciliation and playlist refresh after remove-source, clear, and remove-missing; these operations are serialized by the coordinator, and stale-token publications must be rejected.
- [ ] 6.2 Run the focused repository, coordinator, restart, and production UI commands above, then `./init.sh` only when implementation is authorized. Record exact results and blockers in the implementation handoff; this documentation amendment does not update implementation status.
- [ ] 6.3 Run `openspec validate scan-local-audio-folders --strict` and `git diff --check`.
- [ ] 6.4 Review the final diff against the original request, confirm no migration was added, and leave all newly amended requirements unchecked until implementation and review are complete.
