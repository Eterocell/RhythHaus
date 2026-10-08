# Desktop drag import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add accessible desktop drag-and-drop import for folders and supported audio files without copying media or bypassing the existing scan lifecycle.

**Architecture:** The JVM Library feature validates dropped paths and creates stable source descriptors. Shared owns admission, registration, and scanning through its existing coordinator. Compose renders an accessible drop target with the existing picker as keyboard fallback.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, JVM AWT/Compose drag events, existing LibrarySource/PlatformSourceAccess, Shared coordinator, JVM Compose tests.

**Spec:** `openspec/changes/desktop-drag-drop/specs/desktop-drag-import/spec.md`

## Global Constraints

- Desktop drops MUST NOT copy or delete user media.
- Desktop drops MUST use the existing App-owned source registration and scan coordinator.
- Android and iOS picker behavior MUST remain unchanged.
- Missing media MUST remain an explicit Remove missing action, never automatic deletion.
- New resources MUST have exact EN/ZH parity and Library ownership.

## Review Focus

- Canonical paths, duplicate input and stable source IDs: test same set in different order and repeated drops.
- Mixed valid/invalid drops: test valid siblings survive unsupported and unreadable entries.
- File-set changes: test replacement set does not silently mutate the old source.
- Scan admission races: test drop registration and scan cannot bypass or overlap destructive operations.
- Accessible compact UI: test target semantics, keyboard fallback and EN/ZH labels at 600×400dp.

---

### Task 1: Desktop path admission and source descriptors

**Files:**
- Create: `feature/library/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/DesktopDropImport.jvm.kt`
- Create: `feature/library/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/impl/JvmDroppedFilesSourceAccess.kt`
- Test: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/DesktopDropImportJvmTest.kt`

**Interfaces:**
- Produces `DesktopDropResult`, `DesktopDropFailure`, `desktopDropSourceDescriptors(paths: List<String>)`, and `scanDroppedFiles(source: LibrarySource)`.

- [ ] Step 1: Write tests for canonicalization, extension filtering, stable order-independent identity, mixed failures, and source-local scan events.
- [ ] Step 2: Run `./gradlew :feature:library:impl:jvmTest --tests '*DesktopDropImportJvmTest*'`; expect RED.
- [ ] Step 3: Implement bounded canonical validation and dropped-file source scanning; preserve original absolute paths and deterministic keys.
- [ ] Step 4: Rerun the focused test; expect GREEN.
- [ ] Step 5: Commit `feat: add desktop drop source admission`.

### Task 2: Shared registration and scan orchestration

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/DesktopDropImportTest.kt`

**Interfaces:**
- Consumes Task 1 source descriptors.
- Produces one coordinator-admitted `onDesktopDrop` callback and localized terminal outcomes.

- [ ] Step 1: Add a failing test for one registration/scan admission, duplicate reuse, cancellation, and unchanged playback.
- [ ] Step 2: Run `./gradlew :shared:jvmTest --tests '*DesktopDropImportTest*'`; expect RED.
- [ ] Step 3: Add the Shared callback and route every accepted source through the existing coordinator and publication owner.
- [ ] Step 4: Rerun focused Shared tests; expect GREEN.
- [ ] Step 5: Commit `feat: route desktop drops through library coordinator`.

### Task 3: Accessible Compose desktop target

**Files:**
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/DesktopDropTarget.jvm.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `feature/library/impl/src/commonMain/composeResources/values/strings.xml`
- Modify: `feature/library/impl/src/commonMain/composeResources/values-zh/strings.xml`
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/DesktopDropTargetSemanticsJvmTest.kt`

**Interfaces:**
- Consumes Shared `onDesktopDrop` callback and Task 1 drop result.
- Produces visible/accessibility target state and picker fallback.

- [ ] Step 1: Add RED tests for EN/ZH semantics, drag-active state, keyboard fallback, invalid-only feedback, and 600×400 reachability.
- [ ] Step 2: Run focused Compose tests; expect RED.
- [ ] Step 3: Implement JVM-only drop event bridge and common semantic target without changing Android/iOS surfaces.
- [ ] Step 4: Rerun focused UI tests; expect GREEN.
- [ ] Step 5: Commit `feat: add accessible desktop drop target`.

### Task 4: Integration smoke, review and closeout

**Files:**
- Modify: `openspec/changes/desktop-drag-drop/tasks.md`
- Modify: `roadmap.md`
- Modify: `progress.md`

- [ ] Step 1: Run desktop temporary-folder smoke covering folder/file drop, repeat identity, scan and playback path without user-library writes.
- [ ] Step 2: Run Library/Shared/desktop tests, desktop compile, iOS/Android compile, Spotless, Detekt and Architecture Check.
- [ ] Step 3: Obtain independent review focused on paths, race admission, missing-file behavior and accessibility.
- [ ] Step 4: Synchronize canonical specs, archive the change, record evidence and commit.
