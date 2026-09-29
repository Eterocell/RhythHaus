# Save Queue as Playlist Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Save the visible current-and-upcoming queue as an ordered durable playlist without disturbing playback.

**Architecture:** The feature Queue tab freezes the current-and-upcoming occurrence track IDs when Save opens a name dialog. Shared forwards confirmation through the existing serialized playlist mutation owner and atomic repository operation. No playback or schema change.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight existing playlist persistence, JVM Compose tests.

**Spec:** `docs/superpowers/specs/2026-09-29-save-queue-as-playlist-design.md`; `openspec/changes/save-queue-as-playlist/specs/saved-playlists/spec.md`.

## Global Constraints
- Keep Shared as the sole cross-feature route and mutation composition root.
- Capture the displayed current and upcoming occurrences in order at modal open; never include the historical prefix or collapse duplicate tracks.
- Use `PlaylistStateOwner.mutate` and `PlaylistRepository.createWithEntries`; no schema, source, or playback mutation.
- The user owns manual UI/device/listening acceptance; automated verification remains implementation-owned.

## Review Focus
- Current occurrence absent but queue nonempty: no save action or empty playlist.
- Same track occurs twice: preserve two independent ordered entries.
- Queue changes while name modal open: save only frozen original snapshot.
- Track disappears before confirmation: no partial playlist, modal retains draft/error.
- Back dismissal: only the active modal closes, no create call.

---

### Task 1: Queue snapshot and feature modal

**Files:** Modify `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`, EN/ZH `feature/playlists/impl/src/commonMain/composeResources/values*/strings.xml`; test `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistQueueSaveJvmTest.kt` and update two existing hub callsite tests.

**Interfaces:** `PlaylistHubScreen` adds required `onSaveQueueAsPlaylist: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit`; the saved order comes from `queueTabPresentation(playbackState).rows.map { it.occurrence.track.id }` on action invocation.

- [ ] Write a failing Compose regression for a historical prefix, duplicated tracks, changed queue during naming, and a successful callback receiving the frozen ordered IDs; assert queue controls/transport state remain unchanged.
- [ ] Run `./gradlew :feature:playlists:impl:jvmTest --tests '*PlaylistQueueSaveJvmTest' --configuration-cache`; expect failure before production wiring.
- [ ] Implement the queue action, separate frozen draft, name dialog and feature dismissal identity; migrate `PlaylistArtworkJvmTest` and `PlaylistFeatureDismissalTest` callsites.
- [ ] Add failing-then-passing tests for empty/unselected queue, cancel/Back, failure retention/retry, and accessibility at 600×400 dp; verify EN/ZH string resolution.
- [ ] Re-run focused feature tests; expect pass.

### Task 2: Shared authoritative creation

**Files:** Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`; test `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRouteAdapterJvmTest.kt` or `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt`.

**Interfaces:** Consumes Task 1's `onSaveQueueAsPlaylist(name, trackIds, onOutcome)`; invokes `onPlaylistMutation({ createWithEntries(name, trackIds) }, onOutcome)`.

- [ ] Add a failing Shared route regression asserting ordered playlist entries persisted from Queue UI with repeated track IDs, authoritative snapshot publication, and unchanged playback queue.
- [ ] Run focused Shared test selector and confirm failure.
- [ ] Add callback forwarding in PlaylistHub route, keeping Shared mutation ownership.
- [ ] Run focused Shared test selector; expect pass; run existing playlist repository transaction tests for missing-track rollback.

### Task 3: Verification and lifecycle

**Files:** Update `openspec/changes/save-queue-as-playlist/tasks.md`, `openspec/specs/saved-playlists/spec.md`, `roadmap.md`, `progress.md`.

- [ ] Run focused feature and Shared JVM tests, `:desktopApp:compileKotlin`, `:shared:compileTestKotlinIosSimulatorArm64`, separate `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, strict OpenSpec validation, and `git diff --check`; record actual output and any Android environment limitation.
- [ ] Review diff against specification, sync saved-playlists delta to canonical spec, mark tasks complete only after evidence, archive the change and commit conventionally; record user-owned manual acceptance as pending.
