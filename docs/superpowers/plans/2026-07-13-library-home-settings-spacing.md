# Library Home Chrome and Settings Spacing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the Library home nested top bar and apply the approved compact Settings spacing while preserving system safe areas and unrelated behavior.

**Architecture:** Keep platform-derived safe-area values separate from app-owned spacing. Settings consumes a small immutable layout policy with exact `Dp` values. Library home retains its system-bar top content inset through a pure pass-through seam, while home-only nested-chrome state and rendering are removed without touching drill-down Miuix chrome.

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, Miuix 0.9.3, Kotlin Multiplatform common tests, Gradle.

## Global Constraints

- Shared Compose Multiplatform UI only.
- Preserve system-derived safe-area padding on Library home and Settings.
- Settings app-owned spacing is exactly 16 dp horizontal, 8 dp vertical, 12 dp inter-item, and 8 dp final bottom padding.
- Do not add a replacement Library home top bar, title row, blur layer, or collapse animation.
- Do not change album/artist `DrillDownMiuixScrollChrome`, artwork transitions, title presentation, back navigation, safe-start inset, or Miuix scroll behavior.
- Preserve navigation, playback, scanning, source management, dialogs, dependencies, persistence, and platform integrations.
- Preserve 44 dp icon targets and existing accessibility labels.
- Do not add Windows or Linux product support.

---

### Task 1: Compact Settings layout policy

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt:100-145`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt`

**Interfaces:**
- Produces: `internal data class SettingsLayoutPolicy` with four `Dp` properties.
- Produces: `internal val CompactSettingsLayoutPolicy` containing the approved exact values.
- Consumes: existing `safeContentPadding()`, `PaddingValues`, and `Arrangement.spacedBy` APIs.

- [ ] **Step 1: Write the failing compact-policy test**

Create `SettingsScreenTest.kt`:

```kotlin
package com.eterocell.rhythhaus.settings

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsScreenTest {
    @Test
    fun compactSettingsLayoutPolicyUsesApprovedSpacing() {
        assertEquals(16.dp, CompactSettingsLayoutPolicy.horizontalPagePadding)
        assertEquals(8.dp, CompactSettingsLayoutPolicy.verticalPagePadding)
        assertEquals(12.dp, CompactSettingsLayoutPolicy.itemSpacing)
        assertEquals(8.dp, CompactSettingsLayoutPolicy.bottomContentPadding)
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest.compactSettingsLayoutPolicyUsesApprovedSpacing' --configuration-cache
```

Expected: compilation fails because `CompactSettingsLayoutPolicy` does not exist.

- [ ] **Step 3: Add the immutable layout policy**

In `SettingsScreen.kt`, import `androidx.compose.ui.unit.Dp` and add before `SettingsScreen`:

```kotlin
internal data class SettingsLayoutPolicy(
    val horizontalPagePadding: Dp,
    val verticalPagePadding: Dp,
    val itemSpacing: Dp,
    val bottomContentPadding: Dp,
)

internal val CompactSettingsLayoutPolicy = SettingsLayoutPolicy(
    horizontalPagePadding = 16.dp,
    verticalPagePadding = 8.dp,
    itemSpacing = 12.dp,
    bottomContentPadding = 8.dp,
)
```

- [ ] **Step 4: Apply the policy without changing safe-area handling**

Inside `SettingsScreen`, assign:

```kotlin
val layoutPolicy = CompactSettingsLayoutPolicy
```

Keep `.safeContentPadding()` in its current modifier chain and replace only app-owned values:

```kotlin
.padding(
    horizontal = layoutPolicy.horizontalPagePadding,
    vertical = layoutPolicy.verticalPagePadding,
)
```

```kotlin
contentPadding = PaddingValues(bottom = layoutPolicy.bottomContentPadding),
verticalArrangement = Arrangement.spacedBy(layoutPolicy.itemSpacing),
```

- [ ] **Step 5: Run focused GREEN and source-management regression tests**

Run:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' \
  --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' \
  --configuration-cache
```

Expected: both test classes pass.

- [ ] **Step 6: Review the task diff**

Confirm `.safeContentPadding()`, `RhythHausTopAppBar`, all source-management callbacks, 44 dp icon targets, 48 dp primary buttons, dialogs, and semantics remain unchanged.

- [ ] **Step 7: Commit the Settings behavior and test**

```bash
GIT_MASTER=1 git add \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt
GIT_MASTER=1 git commit -m "feat: compact settings layout spacing" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 2: Remove Library home nested chrome

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt:43-191`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt:243-300`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt:62-86`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt:425-465`

**Interfaces:**
- Produces: `internal fun libraryHomeTopContentPadding(systemBarTopPadding: Dp): Dp`.
- Preserves: `rememberSystemBarTopPadding`, `DrillDownMiuixScrollChrome`, `LibraryScrollPosition`, and Now Playing visibility functions.
- Removes: home-only `NestedScrollChromeState`, `nestedScrollChromeStateFor`, and `NestedScrollBlurChrome` when no callers remain.

- [ ] **Step 1: Write the failing safe-top policy test**

Add `androidx.compose.ui.unit.dp` to `LibraryNavigationTest.kt`, then add:

```kotlin
@Test
fun libraryHomeTopContentPaddingPreservesSystemBarInset() {
    assertEquals(
        37.dp,
        libraryHomeTopContentPadding(systemBarTopPadding = 37.dp),
    )
}
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest.libraryHomeTopContentPaddingPreservesSystemBarInset' --configuration-cache
```

Expected: compilation fails because `libraryHomeTopContentPadding` does not exist.

- [ ] **Step 3: Add and consume the safe-top pass-through**

In `LibraryHomeContent.kt`, import `Dp` and add:

```kotlin
internal fun libraryHomeTopContentPadding(systemBarTopPadding: Dp): Dp =
    systemBarTopPadding
```

Inside `LibraryHomeContent`, retain `rememberSystemBarTopPadding()` and consume the helper:

```kotlin
val homeTopContentPadding = libraryHomeTopContentPadding(rememberSystemBarTopPadding())
```

Use it for:

```kotlin
contentPadding = PaddingValues(top = homeTopContentPadding),
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same command from Step 2.

Expected: the focused test passes.

- [ ] **Step 5: Remove only the Library home overlay and state**

From `LibraryHomeContent.kt`, remove:

- the `derivedStateOf` and `getValue` imports used only by home chrome;
- `homeScrollChromeState` derivation;
- the final `NestedScrollBlurChrome(...)` overlay.

Simplify the outer composition only as needed; preserve the list, backdrop recording, header, 20 dp horizontal padding, 18 dp item spacing, browse content, and `NowPlayingBarContentPadding`.

- [ ] **Step 6: Delete newly dead home-only declarations**

From `LibraryChrome.kt`, remove `NestedScrollBlurChrome` and imports that become unused. Preserve `NestedScrollChromeToolbarHeight` because drill-down chrome uses it, along with every drill-down helper.

From `LibraryNavigation.kt`, remove `NestedScrollChromeState` and `nestedScrollChromeStateFor`. Preserve `LibraryScrollPosition` and all Now Playing visibility logic.

- [ ] **Step 7: Remove obsolete nested-chrome progression tests**

Delete only:

- `nestedScrollChromeIsInactiveAtTopOfList`
- `nestedScrollChromeProgressesWithinFirstItem`
- `nestedScrollChromeIsFullyActiveAfterFirstItem`
- `nestedScrollChromeCanStillUseExplicitHeaderOffsetWhenRequested`

Keep the new safe-top test and all navigation/Now Playing tests.

- [ ] **Step 8: Run focused Library tests and compilation**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both commands pass.

- [ ] **Step 9: Review the task diff**

Confirm `DrillDownMiuixScrollChrome`, artwork handling, `MiuixScrollBehavior`, safe-start navigation padding, title chips, back action, and Now Playing visibility logic are unchanged.

- [ ] **Step 10: Commit the Library behavior and test**

```bash
GIT_MASTER=1 git add \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt
GIT_MASTER=1 git commit -m "feat: remove library home nested chrome" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

### Task 3: Full verification and durable evidence

**Files:**
- Modify: `openspec/changes/library-home-settings-spacing/tasks.md`
- Modify: `roadmap.md`
- Modify: `progress.md`
- Preserve: `docs/superpowers/specs/2026-07-13-library-home-settings-spacing-design.md`
- Preserve: `openspec/changes/library-home-settings-spacing/`

**Interfaces:**
- Consumes: completed Tasks 1 and 2 plus their test evidence.
- Produces: accurate OpenSpec completion state, roadmap completion entry, and session handoff evidence.

- [ ] **Step 1: Run strict OpenSpec validation**

```bash
openspec validate library-home-settings-spacing --strict
```

Expected: `Change 'library-home-settings-spacing' is valid`.

- [ ] **Step 2: Run shared JVM tests**

```bash
./gradlew :shared:jvmTest --configuration-cache
```

Expected: pass with zero test failures.

- [ ] **Step 3: Run desktop and Android verification**

```bash
./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: both tasks pass; record existing unrelated warnings separately.

- [ ] **Step 4: Run iOS toolchain and simulator verification**

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: record the actual result. If existing common-test `Thread` references still block iOS compilation, record the exact file/line output and do not fix it in this change.

- [ ] **Step 5: Run diff hygiene and scope review**

```bash
GIT_MASTER=1 git diff --check
GIT_MASTER=1 git status --short
```

Review every changed source and test file. Confirm no dependency, database, scanner, playback, source-access, navigation-model, platform-integration, Windows, or Linux change is present.

- [ ] **Step 6: Update durable evidence**

Mark OpenSpec tasks complete only when their evidence exists. Update roadmap item 16 to describe removal of the Library home nested top bar and the exact Settings compact spacing, including the manual visual-QA limitation. Add a top `progress.md` handoff with route, scope, commands, outcomes, changed files, blocker status, and next owner.

- [ ] **Step 7: Revalidate evidence artifacts**

```bash
openspec validate library-home-settings-spacing --strict
GIT_MASTER=1 git diff --check
```

Expected: both pass.

- [ ] **Step 8: Commit specifications, plan, and completion evidence**

```bash
GIT_MASTER=1 git add \
  docs/superpowers/specs/2026-07-13-library-home-settings-spacing-design.md \
  docs/superpowers/plans/2026-07-13-library-home-settings-spacing.md \
  openspec/changes/library-home-settings-spacing \
  roadmap.md \
  progress.md
GIT_MASTER=1 git commit -m "docs: complete library home settings spacing" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

- [ ] **Step 9: Review final branch state**

```bash
GIT_MASTER=1 git status --short
GIT_MASTER=1 git log --oneline -3
```

Expected: the worktree is clean and the three atomic commits are present.
