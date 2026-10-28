# Studio UI Review Remediation Implementation Plan

> **For agentic workers:** Required execution skill: `subagent-driven-development` or `executing-plans`. Execute one task at a time after user approval. This is a plan, not evidence that any implementation or verification has run.

**Goal:** Close the verified P2 review findings in the already implemented Studio UI without widening the feature scope or claiming manual UI acceptance.

**Architecture:** Shared remains the only shell, routing, Back, playback, and overlay-host composition authority. Library, Search, and Queue receive explicit feature contracts from Shared. `core/ui` remains the owner of reusable row semantics; feature modules own their labels and actions. Root window-density projection remains the only wide/compact density source.

**Tech stack:** Existing Kotlin Multiplatform, Compose Multiplatform, Gradle, and current UI modules. No dependencies, toolchains, database schemas, or platform APIs added.

**Specs:** [Approved design](../specs/2026-10-26-studio-ui-review-remediation-design.md); [OpenSpec proposal](../../../openspec/changes/studio-ui-review-remediation/proposal.md); [OpenSpec design](../../../openspec/changes/studio-ui-review-remediation/design.md); [OpenSpec specs](../../../openspec/changes/studio-ui-review-remediation/specs/); [OpenSpec tasks](../../../openspec/changes/studio-ui-review-remediation/tasks.md).

**Approval:** The user approved both this plan and its design; implementation and verification are recorded in `progress.md` and the OpenSpec task evidence. The Library API decision is to update the binding architecture contract to the current redesigned signatures; no old-signature aliases or dead fields.

**Manual acceptance status:** The `studio-ui-redesign` GUI/device/audio matrix remains user-performed, postponed, unverified, and open. Automated tests and a desktop smoke run do not pass or remove those criteria.

## Global constraints

- Route: `openspec+superpowers`. Follow the OpenSpec tasks and this plan in order; one active checkpoint at a time.
- TDD for each behavioral change: update a consumer-boundary test first, observe the expected failure, implement the narrow fix, then run the focused target. No test-only production helpers or source-text assertions.
- No changes to database, scan admission, playback transaction/queue authority, platform permission/picker behavior, route/Back ownership, or iOS export ABI.
- Queue save captures the current-plus-upcoming occurrence snapshot once at dialog open. Playback may progress while naming; the modal, immutable snapshot, and name draft remain associated with that save operation.
- Feature code must not resolve shell services from a service-locator CompositionLocal. Shared supplies the window-overlay publisher through the feature composition boundary.
- Interactive disabled/hidden content must remove pointer, gesture, keyboard, and accessibility actions; opacity alone is not deactivation.
- Row-menu `Select` must not call playback. Normal activation retains exact visible ordering. Selection mode toggles once. Metadata editing reuses the existing editor and draft/restore semantics.
- The root `LocalHausDesktopDensity` projection is the source for row/detail density. Keep `WIDE_LIBRARY_BREAKPOINT_DP` only for its documented Library content/grid use; remove duplicated density calculations and forwarding.
- Affected maintenance actions use `heightIn(min = 48.dp)` (or an equivalent measured minimum), not a fixed 36dp height. Large text remains un-clipped; iOS uses the same 48dp/pt minimum.
- Preserve EN/ZH catalog parity. Settings remains owner of source-management copy; its current key is `settings_section_sources`.
- The four P3 refactors are out of scope: queue/list policy deduplication, repeated navigation destination/forwarder cleanup, repeated `BrowseMode` switch cleanup, and `HausSongRow` parameter-object redesign.
- Each completed checkpoint records exact command output and reviewed diff in `progress.md`; after all implementation work, update `roadmap.md`. No item is marked complete without command evidence and diff review.

## Review focus

1. Queue save while playback advances: original occurrence IDs/order stay captured; text and failure draft survive; cancel saves nothing; Back closes dialog before Queue.
2. Queue overlay composition: Shared passes a publisher; the feature has no `LocalHausWindowOverlayPublisher` read.
3. Library/Search menus: named Select is reachable without long press, does not play, and is visible to desktop pointer and assistive technology; Search metadata edit preserves query/results and cancellation.
4. Row semantics: ordinary row identity does not announce “Select”; existing favorite/current-track/selection actions retain their independent meanings.
5. Destructive action size: both clear-library and remove-missing targets meet minimum size at default and enlarged text.
6. Density transition: equivalent available content uses the same root density decision; no hardcoded 720dp detail policy or redundant `desktopDensity` chain remains.
7. Current API/resource ledgers agree with implementation; old fields are not reintroduced as shims.

## File map

| Surface | Target files |
| --- | --- |
| Queue snapshot and state lifetime | `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`, `NowPlayingPanelPresentation.kt`, `LibraryAppShell.kt`; `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaybackQueueContent.kt` |
| Queue overlay host | `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausWindowOverlayHost.kt`; Shared queue composition; explicit publisher parameter at the feature boundary |
| Queue tests/cleanup | `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt`; `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistQueueSaveJvmTest.kt`; `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt` |
| Shared row semantics | `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausSongRow.kt`; `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/HausSongRowJvmTest.kt` |
| Library/Search actions | `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`; `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`; Shared `LibraryAppShell.kt` / `LibraryRoutes.kt`; owning EN/ZH resource catalogs |
| Row-action tests | Library `LibraryHomeContentJvmTest.kt`; Search `SearchSelectionPoliciesJvmTest.kt`; Shared `SearchRouteAdapterJvmTest.kt` and `TrackSelectionSemanticsJvmTest.kt` |
| Density | `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppShellLayout.kt`, `LibraryAppShell.kt`, `LibraryRoutes.kt`; Library `LibraryHomeContent.kt`, `LibraryDetailContent.kt`, `LibraryRows.kt` |
| Confirmation target sizes | `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt`; Shared `LibraryAppShellJvmTest.kt` and existing dialog/UI behavior tests |
| Canonical contracts | `docs/architecture.md`, `docs/adr/0001-feature-first-module-boundaries.md`, `roadmap.md`, `progress.md`; OpenSpec deltas and the proposed design file above |

## Task 1: Queue save lifetime and explicit overlay boundary

**Tests:** Existing `PlaylistQueueSaveJvmTest`, `PlaylistScreensTest`, and `LibraryAppShellJvmTest`. Do not retain test coverage whose only contract is the internal `executeQueueMutation` helper.

- [x] **1.1 RED — update the production boundary regressions.** In the existing Shared shell test, replace the assertion that playback replacement dismisses the save dialog with observable assertions that the Queue surface, dialog, captured original occurrence snapshot, and typed name remain. Add current-plus-upcoming duplicate-occurrence coverage at dialog-open time, plus cancel/no-mutation, retryable failure, and dialog-first Back coverage. Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest*' --configuration-cache`; confirm the changed behavior fails for the expected reason before implementation.
- [x] **1.2 Implement queue lifetime and publisher injection.** Keep Queue/save presentation state alive across current-occurrence changes without creating a second authoritative queue. Remove the feature read of `LocalHausWindowOverlayPublisher`; resolve the host publisher in Shared and pass it through the Queue entry point. Preserve existing no-current/terminal route behavior. Verify with `./gradlew :shared:jvmTest :feature:playlists:impl:jvmTest --configuration-cache`.
- [x] **1.3 Remove test-only queue dead weight.** Delete production-dead `executeQueueMutation` and unread `QueueRowLayoutPolicy.reservesMetadataWidth`. Remove or replace helper-only `PlaylistScreensTest` assertions with behavior coverage at the actual Shared queue-mutation boundary; do not duplicate `QueueMutationDispatcher`. Verify `./gradlew :feature:playlists:impl:jvmTest :shared:jvmTest --configuration-cache`.

## Task 2: Accessible row identity and discoverable actions

**Tests:** `HausSongRowJvmTest`, `LibraryHomeContentJvmTest`, `SearchSelectionPoliciesJvmTest`, `SearchRouteAdapterJvmTest`, `TrackSelectionSemanticsJvmTest`, and `LibraryAppShellJvmTest`.

- [x] **2.1 RED — correct primary row semantics.** Add a real Compose semantics assertion that a normal playable row is named for its track, not by the Select action; selection remains a separate long-press/menu action and selection mode remains a single toggle. Run `./gradlew :core:ui:jvmTest --tests '*HausSongRowJvmTest*' :shared:jvmTest --tests '*TrackSelectionSemanticsJvmTest*' --configuration-cache`; observe the targeted failure.
- [x] **2.2 Implement row semantics without changing the public row's unrelated responsibilities.** Stop using `selectLabel` as the normal row's `contentDescription`; retain a separately named Select action/long-click label and current-track/favorite semantics. Use the existing row interface unless a necessary callback is proven missing. Verify with `./gradlew :core:ui:jvmTest :shared:jvmTest --tests '*TrackSelectionSemanticsJvmTest*' --configuration-cache`.
- [x] **2.3 RED — assert named menu actions at Library and Search consumers.** Add consumer-boundary coverage that Select is present without a long-press, does not play, and toggles once; Search's metadata menu opens the existing editor for the same track through the Shared shell while preserving query/result state, and cancel commits nothing. Run `./gradlew :feature:library:impl:jvmTest :feature:search:jvmTest :shared:jvmTest --tests '*LibraryHomeContentJvmTest*' --tests '*SearchSelectionPoliciesJvmTest*' --tests '*SearchRouteAdapterJvmTest*' --tests '*TrackSelectionSemanticsJvmTest*' --tests '*LibraryAppShellJvmTest*' --configuration-cache`; observe expected failures.
- [x] **2.4 Implement localized row menus and Search editing.** Add a named row-action menu to eligible Library and Search rows. Include Select for normal-mode rows and Edit metadata wherever the existing callback is available. Wire Search to Shared's existing metadata-editor owner; add EN/ZH labels in their owning feature catalogs. Ensure menu clicks cannot bubble into play. Verify with `./gradlew :feature:library:impl:jvmTest :feature:search:jvmTest :shared:jvmTest --tests '*LibraryHomeContentJvmTest*' --tests '*SearchSelectionPoliciesJvmTest*' --tests '*SearchRouteAdapterJvmTest*' --tests '*LibraryAppShellJvmTest*' --configuration-cache`.

## Task 3: Density source and destructive action targets

**Tests:** `LibraryHomeContentJvmTest`, `DrillDownViewJvmTest`, `LibraryAppShellJvmTest`, and existing clear/remove-missing dialog UI behavior.

- [x] **3.1 Unify responsive density.** Replace Library detail's hardcoded 720dp and Home's separate `!compact` density decision with the root density projection. Remove redundant recomputation and `desktopDensity` parameters threaded through Shared/Library route and row adapters; retain the feature breakpoint only for its documented content/grid policy. Add a boundary regression that distinguishes app-window width from reduced content width. Verify with `./gradlew :feature:library:impl:jvmTest :shared:jvmTest --tests '*LibraryHomeContentJvmTest*' --tests '*DrillDownViewJvmTest*' --tests '*LibraryAppShellJvmTest*' --configuration-cache`.
- [x] **3.2 Fix confirmation target minima.** Replace explicit 36dp clear-library and remove-missing action heights with a 48dp minimum that can grow for enlarged text. Add measured bounds/semantics assertions through the actual dialogs, not a source-string test. Verify with `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest*' --tests '*LibraryDialogDangerColorJvmTest*' --configuration-cache`.

## Task 4: Canonical API and resource ownership ledgers

- [x] **4.1 Update binding architecture records and the active OpenSpec delta.** Update `docs/architecture.md`, ADR 0001, and the active `feature-first-modular-architecture` spec delta to the accepted current `BrowseMode`, `LibrarySelectionPage`, `LibraryDetailSummary`, `LibrarySharedLabels`, `LibraryHomeContent`, and `DrillDownView` contract from the proposed design. Record the twelve current labels and exact defaults/ownership; keep the 2026-08-10 extraction design historical except for the superseded UI declarations and injected-label membership/ownership, retaining the older consumer resolver rows that still apply. Replace stale `manage_music` resource-ledger entries with the current `settings_section_sources` key without changing Settings wording or feature ownership. The delta MUST modify the base Settings resource-ownership requirement and the base resolver-table requirement so archive supersedes them without dropping unrelated requirements. Verify `openspec validate studio-ui-review-remediation --strict`, `./gradlew architectureCheck --configuration-cache`, and `./gradlew :feature:settings:jvmTest --tests '*SettingsResourceOwnershipJvmTest*' --configuration-cache`.
- [x] **4.2 Record implementation checkpoint evidence.** After Tasks 1–4, update `progress.md` with completed evidence and remaining manual blockers; update `roadmap.md` with the concise remediation entry. Review the staged/diff file inventory against this plan and verify no P3 refactor or unrelated user change was absorbed. Evidence: `.superpowers/sdd/2026-10-26-studio-ui-review-remediation/task-4-2-report.md`.

## Task 5: Integration verification

- [x] **5.1 Run final automated gates and inspect the completed diff.** Run `./gradlew :core:ui:jvmTest :feature:library:impl:jvmTest :feature:search:jvmTest :feature:playlists:impl:jvmTest :shared:jvmTest --configuration-cache`, `./gradlew architectureCheck --configuration-cache`, `./gradlew spotlessCheck detekt --configuration-cache`, `openspec validate studio-ui-review-remediation --strict`, then `./init.sh`. Record exact success or blocker output. Review all changed files against the acceptance criteria; keep the original redesign's manual/mixed acceptance items open and postponed. Automated checks do not count as GUI/device/audio acceptance. Evidence: `.superpowers/sdd/2026-10-26-studio-ui-review-remediation/task-5-1-report.md`; the workflow commit/archive condition remains gated by the recorded automated blockers.

## Workflow follow-up

- Perform an independent review of the implementation diff before finishing the change; fix all blocking findings.
- Commit the completed OpenSpec + Superpowers workflow with a conventional message, then archive `studio-ui-review-remediation` only after automated requirements and repository review pass. Do not archive or mark the original `studio-ui-redesign` manual/mixed acceptance items complete.
- Obtain the user's manual GUI/device/audio acceptance separately when they choose to perform it; record it as user-performed evidence only.
