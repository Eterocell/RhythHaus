# Multiple Music Library Folders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Android and desktop JVM/macOS users add, rescan, and remove multiple independent music-folder sources while iOS remains limited to one app-local source.

**Architecture:** Reuse the existing stable source IDs and source-aware scanner. Add transactional source removal to the repository, keep a refreshed source list in the composition root, expose platform add-source capability through the picker launcher, and add source-scoped management controls to Settings while preserving the single-active-scan model.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Koin, Miuix, Android SAF, JVM AWT folder picker.

## Global Constraints

- Scope is Android and desktop JVM/macOS for multiple user-selected folders; iOS remains a single app-local source.
- Use repeated single-folder picker launches; do not introduce picker-level multi-select.
- Keep source IDs stable from Android SAF URI and desktop canonical path.
- No new dependencies and no SQLDelight table/column/index migration.
- Source removal must be transactional and source-scoped.
- Preserve current playback, artwork lazy loading, scan progress/cancellation, navigation, clear-library, and theme behavior.
- Only one scan may be active; add, rescan, and remove actions are disabled while it is active.
- Do not add Windows/Linux product or packaging support.

---

### Task 1: Add Source-Scoped Repository Removal

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
- Modify: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibrarySource.sq`
- Modify: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`
- Modify: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanSession.sq`
- Modify: `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/ScanError.sq`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`
- Modify repository fakes that implement `LibraryRepository` as required by compilation.

**Interfaces:**
- Produces: `fun LibraryRepository.removeSource(sourceId: String)`.
- Guarantees: removes source-scoped errors, sessions, tracks, and source atomically; leaves all other source data unchanged.

- [ ] **Step 1: Write a failing SQLDelight repository test**

Add a test that persists `source-1` and `source-2`, one track and scan session/error for each, calls `removeSource("source-1")`, and asserts only `source-2` records remain. Update test track helpers to accept `sourceId`.

- [ ] **Step 2: Run the focused test to verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.removeSourceDeletesOnlySelectedSourceData' --configuration-cache`

Expected: compilation fails because `removeSource` does not exist.

- [ ] **Step 3: Add source-scoped delete queries**

Add parameterized delete queries for tracks by source, scan errors whose `scanId` belongs to sessions for the source, sessions by source, and the source row itself. These are query-only changes and MUST NOT change persisted schema shape or add a migration.

- [ ] **Step 4: Implement repository removal**

Add `removeSource(sourceId)` to the interface and in-memory implementation. In-memory removal resolves session IDs for the source before deleting related errors. In SQLDelight, execute the four generated delete queries inside `database.transaction { ... }`, deleting dependent rows before the source.

- [ ] **Step 5: Update repository test doubles**

Add narrow `removeSource` implementations to every test fake. Do not change unrelated fake behavior.

- [ ] **Step 6: Verify GREEN**

Run the focused test and then:

`./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Add Platform Capability and Source Orchestration

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.jvm.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.ios.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`

**Interfaces:**
- Produces: `PlatformFolderPickerLauncher.supportsAdditionalSources: Boolean`.
- Produces: pure `sourcePickerActionVisible(supportsAdditionalSources, sourceCount)` decision helper.
- Produces: `LibraryHomeScreen` callbacks/data for `sources`, `onRescanSource`, and `onRemoveSource`.

- [ ] **Step 1: Write failing capability decision tests**

Test that the picker action is visible with zero sources on every platform-capability value, remains visible with existing sources when additional sources are supported, and is hidden with an existing source when additional sources are unsupported.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`

Expected: unresolved helper/property failure.

- [ ] **Step 3: Add platform capability**

Add `supportsAdditionalSources` to `PlatformFolderPickerLauncher`. Android and JVM actual launchers return `true`; iOS returns `false`. `isAvailable` remains unchanged because iOS still needs to provision or rescan its app-local source.

- [ ] **Step 4: Generalize source scan launch**

In `App()`, keep `librarySources` initialized from `repository.sources()` and decorated with `platformAccess.accessStatus(source)`. Extract the existing scan job body into one local source-accepting function used by picker success and rescan. After every terminal scan, refresh both sources and tracks on Main. Preserve cancellation and messages.

- [ ] **Step 5: Add source removal and clear refresh**

Add an `onRemoveSource` callback that performs `repository.removeSource(source.id)` on `Dispatchers.Default`, then refreshes sources/tracks on Main. Extend clear-library refresh to clear both lists. Do not permit callback invocation while scan progress is active.

- [ ] **Step 6: Thread data and callbacks through route shell**

Pass `sources`, picker visibility/capability, `onRescanSource`, and `onRemoveSource` through `LibraryHomeScreen` and route overlays to Settings without changing browse/detail behavior.

- [ ] **Step 7: Verify focused tests and compilation**

Run the focused test, then:

`./gradlew :shared:compileKotlinJvm :shared:compileKotlinIosSimulatorArm64 :androidApp:compileDebugKotlin --configuration-cache`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Add Settings Source Management UI

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt` or create one focused source-row file if local conventions support it.
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-zh/strings.xml`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`

**Interfaces:**
- Consumes: source list and callbacks from Task 2.
- Produces: configured-source rows, source status copy, rescan/remove controls, and removal confirmation.

- [ ] **Step 1: Add failing UI decision tests**

Add pure tests for source status text inputs/state mapping and mutation-enabled logic (`!scanProgress.isActive`). Avoid screenshot assertions in common tests.

- [ ] **Step 2: Add localized copy**

Add matching English and Simplified Chinese resources for configured folders, available/lost access, never scanned/last scanned, rescan, remove folder, and removal confirmation. Reuse existing add/cancel strings where their meaning is exact.

- [ ] **Step 3: Render configured sources in Settings**

Under Manage Music, show one compact source row per persisted source. Use stable dimensions, Miuix controls/icons already available in the project, source display name as the primary label, and access/last-scan state as secondary text. Do not display raw handles unless needed as a fallback for an empty display name.

- [ ] **Step 4: Add rescan and removal actions**

Rescan invokes the source-scoped callback. Remove opens a confirmation overlay consistent with the existing clear-library dialog style; confirmation invokes source removal. Disable both while any scan is active.

- [ ] **Step 5: Apply picker visibility rule**

Show the picker action when `supportsAdditionalSources` is true or no source exists. Thus Android/JVM can always add another source, while iOS can create its first app-local source but cannot add another after it exists. Preserve the Library empty-state add card with the same rule.

- [ ] **Step 6: Verify focused tests and builds**

Run the source-management test, then:

`./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`

Expected: `BUILD SUCCESSFUL`.

### Task 4: Final Verification and Handoff

**Files:**
- Modify: `openspec/changes/multi-library-folders/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`
- Modify: `.superpowers/sdd/progress.md`

- [ ] **Step 1: Validate OpenSpec**

Run: `openspec validate multi-library-folders --strict`

Expected: `Change 'multi-library-folders' is valid`.

- [ ] **Step 2: Run repository and source-management tests**

Run the focused repository and source-management test classes. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run platform verification**

Run: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`

Run: `/usr/bin/xcrun xcodebuild -version`

Run: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`

Expected: success, or record exact pre-existing blockers without claiming pass.

- [ ] **Step 4: Review diff hygiene**

Run `git diff --check`, `git status --short`, and review the full scoped diff. Do not include the user's pre-existing `roadmap.md` modification except the requested completion update.

- [ ] **Step 5: Record durable evidence**

Mark OpenSpec tasks complete with exact commands/results, prepend a `progress.md` handoff, mark the roadmap item complete with concise limitations/manual QA, and append clean task reviews to `.superpowers/sdd/progress.md`.

- [ ] **Step 6: Commit**

After staged-diff review, commit only this workflow's files with a semantic message such as `feat: support multiple library folders`, unless the user explicitly says not to commit.
