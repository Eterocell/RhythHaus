# Tasks

## 1. Queue save lifetime and explicit overlay boundary

- [x] 1.1 Update the Shared production-boundary regression so playback occurrence replacement leaves Queue, the save dialog, the captured current-plus-upcoming snapshot, and the name draft intact; cover duplicates, cancel/no-mutation, retryable failure, and dialog-first Back. Observe RED, then implement the state-lifetime fix; verify `./gradlew :shared:jvmTest :feature:playlists:impl:jvmTest --configuration-cache`.
- [x] 1.2 Resolve the overlay publisher in Shared and pass it through the Queue feature entry point; remove all Queue-feature reads of `LocalHausWindowOverlayPublisher`. Verify the Shared and Playlist JVM suites and confirm the feature has no shell service-locator access.
- [x] 1.3 Remove production-dead `executeQueueMutation` and unread `QueueRowLayoutPolicy.reservesMetadataWidth`; remove or replace helper-only test assertions with actual Shared queue-mutation behavior coverage. Verify `./gradlew :feature:playlists:impl:jvmTest :shared:jvmTest --configuration-cache`.

## 2. Accessible row identity and discoverable actions

- [x] 2.1 Add a Compose semantics regression proving a normal playable row is named for its track rather than the Select action; make the narrow row semantics fix while preserving separate selection-mode and long-press actions. Verify `./gradlew :core:ui:jvmTest --tests '*HausSongRowJvmTest*' :shared:jvmTest --tests '*TrackSelectionSemanticsJvmTest*' --configuration-cache`.
- [x] 2.2 Add named, localized Select actions to eligible Library and Search row menus; Select must not play and must toggle once. Add Search's named metadata-edit action wired to Shared's existing editor owner; preserve query/results on open and no save on cancel. Add EN/ZH resources and consumer-boundary tests; verify `./gradlew :feature:library:impl:jvmTest :feature:search:jvmTest :shared:jvmTest --configuration-cache`.

## 3. Responsive density and confirmation target minima

- [x] 3.1 Replace the hardcoded Library detail 720dp and Home-specific density decision with the root density projection; remove redundant `desktopDensity` recomputation and parameter threading while retaining the content-grid breakpoint. Test reduced content width versus app-window width; verify `./gradlew :feature:library:impl:jvmTest :shared:jvmTest --tests '*LibraryHomeContentJvmTest*' --tests '*DrillDownViewJvmTest*' --tests '*LibraryAppShellJvmTest*' --configuration-cache`.
- [x] 3.2 Replace fixed 36dp clear-library and remove-missing action heights with a 48dp minimum that can grow for enlarged text. Assert measured target bounds through the real dialogs; verify `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest*' --tests '*LibraryDialogDangerColorJvmTest*' --configuration-cache`.

## 4. Canonical API and resource ownership records

- [x] 4.1 Update `docs/architecture.md`, ADR 0001, and this change's `feature-first-modular-architecture` delta to the accepted current Library declarations/defaults and label ownership; identify the old extraction API as historical except for superseded UI declarations and injected-label ownership, retaining older consumer resolver rows that still apply. Update the Settings resource ledger from `manage_music` to `settings_section_sources` without changing Settings wording or ownership, and modify the delta's base Settings resource-ownership and resolver-table requirements so archive supersedes them without dropping unrelated requirements; verify `openspec validate studio-ui-review-remediation --strict`, `./gradlew architectureCheck --configuration-cache`, and `./gradlew :feature:settings:jvmTest --tests '*SettingsResourceOwnershipJvmTest*' --configuration-cache`.
- [x] 4.2 Update `progress.md` with exact checkpoint evidence/blockers and `roadmap.md` with the concise completed remediation entry; inspect the full diff and confirm the four deferred P3 refactors and unrelated user changes remain untouched. Evidence: `.superpowers/sdd/2026-10-26-studio-ui-review-remediation/task-4-2-report.md`.

## 5. Integration verification

- [x] 5.1 Run `./gradlew :core:ui:jvmTest :feature:library:impl:jvmTest :feature:search:jvmTest :feature:playlists:impl:jvmTest :shared:jvmTest --configuration-cache`, `./gradlew architectureCheck --configuration-cache`, `./gradlew spotlessCheck detekt --configuration-cache`, `openspec validate studio-ui-review-remediation --strict`, and `./init.sh`; record exact outcomes, review the finished diff, and leave the original redesign's manual/mixed acceptance tasks open and postponed. Evidence: `.superpowers/sdd/2026-10-26-studio-ui-review-remediation/task-5-1-report.md`; the automated blockers prevent the workflow commit/archive follow-up.

## Workflow follow-up

- 2026-10-28 automated-gate recovery: the previously installed pinned NDK is available; test-only playback fixture repairs restore the full Shared JVM and iOS suites without production playback changes. `./init.sh` and the planned integrated JVM gate now pass. See the dated recovery section in the Task 5.1 report; the earlier blocker note above is historical. Final independent spec/fixture review accepted the corrections with no P0–P2 findings.
- Independent implementation review and final spec/fixture correction review are complete; blocking findings are resolved. Both active queue-save deltas preserve the same scenario union; the canonical Library UI authority and scan/picker resource ownership are current. Optional global strict-spec validation still reports long-description warnings, while strict named validation and canonical normal validation pass.
- Commit this completed OpenSpec + Superpowers workflow with a conventional message, then archive `studio-ui-review-remediation` only after automated acceptance and repository review pass.
- Keep `studio-ui-redesign` manual/mixed acceptance open until the user performs the GUI/device/audio matrix; automated checks do not constitute that acceptance.
