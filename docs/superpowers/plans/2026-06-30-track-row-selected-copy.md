# Track Row Selected Copy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace debug/prototype selected-track row copy with a polished user-facing status.

**Architecture:** This is a small shared Compose UI polish change isolated to `TrackRow` in `App.kt`, plus OpenSpec/task/progress evidence. No model, persistence, playback, route, or platform-specific changes are required.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform shared UI, Miuix Text/Surface components, OpenSpec.

## Global Constraints

- Selected track rows must display `Now playing`.
- Shared UI must not display `queued on shared UI` or a selected-state animation percentage.
- Preserve existing row selection styling, click behavior, title/artist/album metadata, duration, and playback behavior.
- Do not change playback-state models, queue semantics, scanner, persistence, navigation, theme selection, or platform-specific code.

---

### Task 1: TrackRow selected status copy

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `openspec/changes/polish-track-row-selected-copy/tasks.md`
- Modify: `progress.md`
- Create: `.superpowers/sdd/polish-track-row-selected-copy-report.md`

**Interfaces:**
- Consumes: existing private composable `TrackRow(track: Track, selected: Boolean, onClick: () -> Unit)`.
- Produces: same `TrackRow` signature and behavior, with selected status text changed to `Now playing`.

- [ ] **Step 1: Inspect current source**

Read `TrackRow` in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` and confirm the selected-row `AnimatedVisibility` currently renders `queued on shared UI ${(selectionAlpha * 100).toInt()}%`.

- [ ] **Step 2: Replace the user-visible selected status**

In `TrackRow`, remove `selectionAlpha` / `animateFloatAsState` if no longer needed, and change the selected `Text` to:

```kotlin
AnimatedVisibility(visible = selected) {
    Text(
        text = "Now playing",
        color = HausColors.current.pulse,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
    )
}
```

Do not change row colors, border, click handling, semantics, title, metadata, or duration.

- [ ] **Step 3: Verify no debug copy remains**

Run:

```bash
rg 'queued on shared UI|selectionAlpha' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
```

Expected: no matches.

- [ ] **Step 4: Run focused compile**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run OpenSpec validation**

Run:

```bash
openspec validate polish-track-row-selected-copy --strict
```

Expected: `Change 'polish-track-row-selected-copy' is valid`.

- [ ] **Step 6: Run broad verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: `BUILD SUCCESSFUL`. If the known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` fails, rerun that targeted test and then rerun the broad command; record exact outcomes.

- [ ] **Step 7: Update task/progress evidence**

Mark `openspec/changes/polish-track-row-selected-copy/tasks.md` complete with command evidence. Add a new top handoff entry in `progress.md` including scope, implementation summary, verification commands, acceptance, changed files, blockers, and commit message.

- [ ] **Step 8: Commit**

Run:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt openspec/changes/polish-track-row-selected-copy docs/superpowers/specs/2026-06-30-track-row-selected-copy.md docs/superpowers/plans/2026-06-30-track-row-selected-copy.md progress.md .superpowers/sdd/polish-track-row-selected-copy-report.md
git commit -m "fix: polish selected track row copy"
```
