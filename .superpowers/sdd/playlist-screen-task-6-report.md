# Task 6 Report: Queue UI and Accessibility

Status: DONE_WITH_CONCERNS

## Scope

- Implemented the Playlist hub Queue tab on top of reviewed Task 5 primitives.
- Derived the physical view from the active occurrence: current first, then only the upcoming suffix; history is excluded.
- Kept the current occurrence pinned with no drag, move, or remove controls.
- Kept upcoming rows keyed and targeted by occurrence ID so duplicate tracks remain independent.
- Wired reorder, remove, and confirmed clear through the Task 3 typed controller commands.
- Re-read the controller `StateFlow` after every result and show the localized queue-changed notice on rejection.
- Reused the exact existing `NowPlayingBarContentPadding` in the rendered hub spacer.

## Strict TDD evidence

1. Initial RED:
   - `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache`
   - Failed at test compilation only because the Queue presentation, confirmation, and mutation-wiring APIs did not exist.
2. Wiring RED:
   - The same focused command failed only because `QueueMutationDispatcher` did not exist.
3. Review-fix RED:
   - The same focused command failed only because the production-consumed `QueueRowState`, `QueueRowAction`, `semanticState`, and `availableActions` contract did not exist.
4. GREEN:
   - `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`
   - Passed after an isolated rerun (`BUILD SUCCESSFUL in 3s`).

## Verification

- Focused Queue/controller gate: pass (`BUILD SUCCESSFUL in 3s`).
- Supported JVM/desktop/Android matrix: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 10s`); only the existing Android artwork metadata deprecation warning was emitted.
- Strict OpenSpec validation: `openspec validate playlist-screen --strict` passed (`Change 'playlist-screen' is valid`).
- `GIT_MASTER=1 git diff --check`: pass.
- Kotlin LSP: unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests are the executable Kotlin diagnostics.
- One parallel Gradle attempt failed while two builds wrote the same Kotlin cache (`dirty-sources.txt` disappeared). After `./gradlew --stop`, the focused command passed in isolation; no code failure remained.

## Runtime and accessibility QA

- Launched `./gradlew :desktopApp:run --configuration-cache` and inspected the real 863x798 desktop Playlist Queue tab through Orca.
- The live Chinese accessibility tree showed the current occurrence first with title/state and no drag/move/remove descendants.
- Upcoming rows exposed localized track-named drag, move-up, move-down, and remove controls; first/last move boundaries were disabled correctly.
- Opened the localized clear-upcoming confirmation and verified its title/message without executing the destructive command.
- The Queue scroll surface and Now Playing surface remained separate, with the existing bottom inset retained.
- Screenshot capture succeeded, but this model cannot decode image pixels; no pixel-level spacing, contrast, or glyph-fidelity pass is claimed. Task 7 retains full manual/device acceptance ownership.

## Review

- Initial independent review found three Important issues: helper-only semantics coverage, missing queue drag-boundary coverage, and disconnected tested padding.
- Fixes made the tested semantic state/action contract production-consumed, added above/between/below/fallback drag target tests, and bound the actual hub spacer to Queue presentation padding. Queue entry containers expose localized content/state descriptions without a false role; artwork retains its separate image semantics.
- Re-review verdict: PASS with no remaining Critical or Important findings.

## Concerns / deferrals

- Compose exposes no suitable list-row role for these non-click queue containers, so they intentionally carry no role; localized content/state descriptions identify current and queued rows, while artwork retains its separate image semantics.
- Pixel-level compact/wide/light/dark/CJK visual acceptance, target-device audible behavior, and iOS FK proof remain Task 7 work and were not claimed or modified here.
- Pre-existing unrelated edits to `.superpowers/sdd/progress.md` and Task 1-5 reports were preserved and must not be staged with Task 6.

## Follow-up review fixes

Status: DONE_WITH_CONCERNS

### Adjudication: per-row removal confirmation rejected

- A review suggestion proposed confirmation for each upcoming-row removal.
- This was not implemented because it conflicts with the approved Task 6 interaction contract. The binding brief requires explicit confirmation for `Clear upcoming`; the editable-playback-queue OpenSpec likewise names confirmation only for `Clear upcoming` and defines upcoming removal as an immediate serialized controller command.
- Upcoming-row remove therefore remains immediate and occurrence-ID targeted. `Clear upcoming` remains the only Queue mutation with an explicit confirmation dialog.
- No controller, session, Saved-playlist, dependency, or route behavior was changed by this adjudication.

### Applied fixes

- Removed the inaccurate `Role.Image` from queue-entry containers. Entry containers now retain localized content and state descriptions without a false role; artwork keeps its separate thumbnail/image semantics.
- Added a production-consumed adaptive row policy through `BoxWithConstraints`:
  - compact editable rows below 520 dp place drag metadata on the primary row and move/remove controls on a secondary trailing row;
  - wide editable rows may keep controls inline;
  - current rows remain a single uncluttered metadata row with no mutation controls.
- Raised drag, move-up, move-down, and remove targets to a minimum `44.dp x 44.dp`.
- Added tests for role-free entry semantics, compact secondary-row placement, wide inline placement, current no-action placement, metadata-width reservation, and the 44 dp minimum target.

### Follow-up TDD and verification

- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache` failed at test compilation only because `queueRowLayoutPolicy` and `QueueActionPlacement` did not exist after correcting a missing test `dp` import.
- GREEN: the focused `PlaylistScreensTest` passed (`BUILD SUCCESSFUL in 7s`).
- Focused Queue/controller gate passed (`BUILD SUCCESSFUL in 1s`).
- Supported JVM/desktop/Android matrix passed (`BUILD SUCCESSFUL in 8s`); only the existing Android artwork metadata deprecation warning was emitted.
- `openspec validate playlist-screen --strict`: pass.
- `GIT_MASTER=1 git diff --check`: pass.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined.

### Runtime and review evidence

- Relaunched the updated desktop app and inspected the real compact 800x600 Chinese Queue surface through Orca.
- Current remained first with no action descendants. Upcoming rows retained independent localized drag/move/remove controls; entry nodes appeared as generic containers rather than image-role entries, while artwork remained separately exposed.
- The production policy selects secondary-row actions at this compact width, preserving weighted metadata in the primary row. Pixel decoding remains unavailable, so exact pixel spacing is not claimed; Task 7 still owns full compact/wide/light/dark and device acceptance.
- Focused independent re-review verdict: PASS with no Critical or Important findings. It explicitly confirmed immediate per-row remove is correct and only `Clear upcoming` requires confirmation.

## Stale drag-center follow-up

Status: DONE_WITH_CONCERNS

### Root cause and fix

- Queue row centers were remembered across queue presentations and keyed by positional index. After an upcoming queue shrink, clear, or reorder, an old center could retain an invalid index or become associated with a different occurrence.
- Queue measurements are now keyed by occurrence ID.
- The center map is cleared whenever the ordered `presentation.upcomingOccurrenceIds` list changes, covering shrink, clear, replacement, and reorder.
- Drag target selection iterates only the current upcoming occurrence IDs, ignores stale/non-current center entries, maps the selected occurrence through the current order, and clamps the result to current `upcomingIds.indices` before dispatch.

### Strict RED/GREEN evidence

- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache` failed at test compilation only because the occurrence-aware `queueDragTargetIndex` API did not exist.
- The deterministic regression measures A/B/C at indices 0..2, removes final C from current upcoming IDs, then drags below the remaining list and requires target index 1 rather than stale index 2.
- GREEN: focused `PlaylistScreensTest` passed (`BUILD SUCCESSFUL in 12s`).

### Verification and review

- Focused Queue/controller gate passed (`BUILD SUCCESSFUL in 1s`).
- Supported JVM/desktop/Android matrix passed (`BUILD SUCCESSFUL in 11s`); only the existing Android artwork metadata deprecation warning was emitted.
- `openspec validate playlist-screen --strict`: pass.
- `GIT_MASTER=1 git diff --check`: pass.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined.
- Focused independent review verdict: PASS. It confirmed stale removed occurrences cannot become candidates, reorder cannot misassociate occurrence-ID centers, and the dispatched target is current-order and bounds safe.

### Preserved behavior

- Immediate per-row remove adjudication, adaptive compact layout, role-free queue-container semantics, controller/session behavior, Saved workflows, dependencies, and all Task 7 status remain unchanged.

## Final evidence correction

- Final whole-branch context review found durable wording that still required or described a row role even though the reviewed implementation correctly removed inaccurate `Role.Image` semantics.
- OpenSpec Task 6.3 and the approved plan now require accurate localized content/state descriptions without assigning a false role when Compose lacks a suitable list-row role.
- This report's earlier role wording is corrected above. Production and tests remain unchanged; the role-free entry-container behavior and separate artwork image semantics are the reviewed implementation.
- Compact/wide/light/dark/CJK pixel acceptance and target-device audible behavior remain unverified follow-up evidence and are not claimed as passes.
