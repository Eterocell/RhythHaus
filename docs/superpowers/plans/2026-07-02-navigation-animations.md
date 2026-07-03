# Navigation Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add direction-aware shared Compose route transition animations for RhythHaus Home, detail, Now Playing, Search, Settings, and Clear Library dialog routes.

**Architecture:** Keep `LibraryNavigationStack` as the source of truth and add pure transition metadata beside it. `LibraryHomeScreen` updates navigation through small helper functions that record the last transition kind, then a single root `AnimatedContent` wrapper uses that kind to animate route changes without duplicating per-screen animation logic.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform commonMain, Compose animation APIs already available from current dependencies, kotlin.test common tests, OpenSpec.

## Global Constraints

- Animate shared Compose route transitions for Home, album detail, artist detail, Now Playing, Search, Settings, and Clear Library dialog.
- Make push and pop directions visually distinct.
- Keep the existing `LibraryNavigationStack` as the source of truth.
- Preserve existing visible back, left-edge swipe, Android system/predictive back, playback, scanner, library, and theme behavior.
- Avoid adding dependencies.
- No native platform navigation migration.
- No navigation-library adoption.
- No deep links or saved-state restoration.
- No custom Android predictive-back progress animation.
- No screen content redesign.

---

## File Structure

- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
  - Add `LibraryNavigationTransition` enum and pure helper `classifyTransition(from, to)`.
  - Keep existing route stack APIs intact.
- Modify `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
  - Add tests for transition classification while preserving existing stack tests.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
  - Add Compose animation imports.
  - Track `lastNavigationTransition` next to `navigation`.
  - Route all push/pop calls through helpers that update transition metadata.
  - Wrap the route switch / overlay route rendering in one root `AnimatedContent`.
- Modify `openspec/changes/navigation-animations/tasks.md`
  - Mark tasks complete after implementation and verification.
- Modify `progress.md`
  - Record verification evidence and handoff.

---

### Task 1: Pure Navigation Transition Metadata

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Consumes: existing `LibraryRoute` and `LibraryNavigationStack.routes` / `current`.
- Produces:
  - `enum class LibraryNavigationTransition { None, Push, Pop, Replace, Root }`
  - `fun classifyNavigationTransition(from: LibraryNavigationStack, to: LibraryNavigationStack): LibraryNavigationTransition`

- [ ] **Step 1: Add failing tests for transition classification**

Add these tests to `LibraryNavigationTest.kt` after `pushingHomeReturnsToRoot()`:

```kotlin
    @Test
    fun pushingNestedRouteClassifiesAsPush() {
        val from = LibraryNavigationStack()
        val to = from.push(LibraryRoute.AlbumDetail("Blue Train"))

        assertEquals(LibraryNavigationTransition.Push, classifyNavigationTransition(from, to))
    }

    @Test
    fun poppingNestedRouteClassifiesAsPop() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("Blue Train"))
            .push(LibraryRoute.Search)
        val to = from.pop()

        assertEquals(LibraryNavigationTransition.Pop, classifyNavigationTransition(from, to))
    }

    @Test
    fun pushingHomeClassifiesAsRoot() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("Blue Train"))
            .push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Home)

        assertEquals(LibraryNavigationTransition.Root, classifyNavigationTransition(from, to))
    }

    @Test
    fun replacingTopRouteClassifiesAsReplace() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.Search)
        val to = from.replaceTop(LibraryRoute.Settings)

        assertEquals(LibraryNavigationTransition.Replace, classifyNavigationTransition(from, to))
    }

    @Test
    fun duplicateTopPushClassifiesAsNone() {
        val from = LibraryNavigationStack()
            .push(LibraryRoute.Search)
        val to = from.push(LibraryRoute.Search)

        assertEquals(LibraryNavigationTransition.None, classifyNavigationTransition(from, to))
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: FAIL with unresolved references for `LibraryNavigationTransition` and `classifyNavigationTransition`.

- [ ] **Step 3: Add the transition enum and classifier**

Modify `LibraryNavigation.kt` so it contains this code after `LibraryRoute` and before `LibraryNavigationStack`:

```kotlin
enum class LibraryNavigationTransition {
    None,
    Push,
    Pop,
    Replace,
    Root,
}

fun classifyNavigationTransition(
    from: LibraryNavigationStack,
    to: LibraryNavigationStack,
): LibraryNavigationTransition = when {
    from.routes == to.routes -> LibraryNavigationTransition.None
    to.current == LibraryRoute.Home && from.current != LibraryRoute.Home -> LibraryNavigationTransition.Root
    to.routes.size > from.routes.size -> LibraryNavigationTransition.Push
    to.routes.size < from.routes.size -> LibraryNavigationTransition.Pop
    from.current != to.current -> LibraryNavigationTransition.Replace
    else -> LibraryNavigationTransition.None
}
```

Do not change existing `LibraryNavigationStack` behavior.

- [ ] **Step 4: Run focused tests to verify pass**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 5: Review diff for Task 1**

Run:

```bash
git diff -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt | cat
```

Expected: only transition metadata and tests changed; existing stack behavior remains intact.

---

### Task 2: Root Shared Compose Route Animation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes:
  - `LibraryNavigationTransition`
  - `classifyNavigationTransition(from: LibraryNavigationStack, to: LibraryNavigationStack)`
  - existing route rendering branches in `LibraryHomeScreen`
- Produces:
  - direction-aware `AnimatedContent` route wrapper in `LibraryHomeScreen`
  - helper functions that update `navigation` and `lastNavigationTransition` together

- [ ] **Step 1: Add Compose animation imports**

In `App.kt`, add these imports near the existing animation import:

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
```

Keep the existing `AnimatedVisibility` import.

- [ ] **Step 2: Opt in to animation API on `LibraryHomeScreen` if required by the compiler**

Update the annotation above `LibraryHomeScreen` from:

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
```

to:

```kotlin
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
```

If the compiler does not require `ExperimentalAnimationApi`, remove that import and opt-in during cleanup.

- [ ] **Step 3: Track transition metadata with navigation state**

Inside `LibraryHomeScreen`, replace the current navigation helper block:

```kotlin
    var navigation by remember { mutableStateOf(LibraryNavigationStack()) }
    fun pushRoute(route: LibraryRoute) {
        navigation = navigation.push(route)
    }
    fun popRoute() {
        navigation = navigation.pop()
    }
```

with:

```kotlin
    var navigation by remember { mutableStateOf(LibraryNavigationStack()) }
    var lastNavigationTransition by remember { mutableStateOf(LibraryNavigationTransition.None) }
    fun updateNavigation(next: LibraryNavigationStack) {
        lastNavigationTransition = classifyNavigationTransition(navigation, next)
        navigation = next
    }
    fun pushRoute(route: LibraryRoute) {
        updateNavigation(navigation.push(route))
    }
    fun popRoute() {
        updateNavigation(navigation.pop())
    }
```

- [ ] **Step 4: Add the route transition helper functions**

Add these private functions near other private UI helpers, after `LibraryHomeScreen` and before `HeaderSection`:

```kotlin
private const val NavigationAnimationMillis = 240
private const val NavigationSlideDistancePx = 90

private fun routeContentTransform(transition: LibraryNavigationTransition): ContentTransform = when (transition) {
    LibraryNavigationTransition.Push -> routeSlideContentTransform(forward = true)
    LibraryNavigationTransition.Pop,
    LibraryNavigationTransition.Root,
    -> routeSlideContentTransform(forward = false)
    LibraryNavigationTransition.Replace -> routeFadeContentTransform()
    LibraryNavigationTransition.None -> routeFadeContentTransform()
}

private fun routeSlideContentTransform(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (
        fadeIn(animationSpec = tween(NavigationAnimationMillis)) +
            slideInHorizontally(
                animationSpec = tween(NavigationAnimationMillis),
                initialOffsetX = { NavigationSlideDistancePx * direction },
            )
        ).togetherWith(
        fadeOut(animationSpec = tween(NavigationAnimationMillis)) +
            slideOutHorizontally(
                animationSpec = tween(NavigationAnimationMillis),
                targetOffsetX = { -NavigationSlideDistancePx * direction },
            ),
    ).using(SizeTransform(clip = false))
}

private fun routeFadeContentTransform(): ContentTransform = fadeIn(
    animationSpec = tween(NavigationAnimationMillis),
).togetherWith(
    fadeOut(animationSpec = tween(NavigationAnimationMillis)),
).using(SizeTransform(clip = false))
```

If `EnterTransition`, `ExitTransition`, or `ExperimentalAnimationApi` imports are unused after this helper compiles, remove them.

- [ ] **Step 5: Wrap the route rendering in `AnimatedContent`**

In `LibraryHomeScreen`, wrap the existing route-dependent rendering with:

```kotlin
    AnimatedContent(
        targetState = navigation.current,
        transitionSpec = { routeContentTransform(lastNavigationTransition) },
        label = "LibraryRouteTransition",
    ) { currentRoute ->
        when (val route = currentRoute) {
            // move the existing when branches here
        }
    }
```

Move the existing `when (val route = navigation.current) { ... }` into the `AnimatedContent` lambda and switch it to `when (val route = currentRoute) { ... }`.

Important preservation details:

- Keep `NavigationBackHandler` outside `AnimatedContent`.
- Keep `albums` and `artists` `remember(...)` values outside `AnimatedContent`.
- Preserve all current callbacks exactly: `pushRoute`, `popRoute`, playback queue setup, `selectedTrackId` updates, `onShowSettings`, and `onShowSearch`.
- Keep `SettingsScreen`, `SearchScreen`, and `Dialog` rendering inside the animated content by moving their current `if (navigation.current == ...)` blocks into the matching route branches if needed:
  - `LibraryRoute.Settings` should render the Home surface plus `SettingsScreen` overlay.
  - `LibraryRoute.Search` should render the Home surface plus `SearchScreen` overlay.
  - `LibraryRoute.ClearLibraryDialog` should render the Home surface plus clear-library `Dialog`.

Use a small private composable only if it reduces duplication of the Home surface across Home/Settings/Search/ClearLibraryDialog. Do not redesign the UI.

- [ ] **Step 6: Run compile and fix import/API issues**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: PASS. If it fails due to an animation API import or `using(...)` availability, adapt to the exact Compose 1.11 API while preserving the same behavior. Do not add dependencies.

- [ ] **Step 7: Run focused navigation tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: PASS.

- [ ] **Step 8: Review route animation diff**

Run:

```bash
git diff -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt | cat
```

Expected: route animation wrapper and helper changes only; no playback/scanner/library/theme behavior changes.

---

### Task 3: Verification, OpenSpec Task Update, and Handoff Evidence

**Files:**
- Modify: `openspec/changes/navigation-animations/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: implementation from Tasks 1-2.
- Produces: completed task checklist and session handoff evidence.

- [ ] **Step 1: Validate OpenSpec change**

Run:

```bash
openspec validate navigation-animations --strict
```

Expected: `Change 'navigation-animations' is valid`.

- [ ] **Step 2: Run broad JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: PASS. If a known flaky test fails, rerun the exact failing test once and record both outputs.

- [ ] **Step 3: Run iOS availability check and simulator tests if practical**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: PASS if Xcode and simulator toolchain are available. If unavailable, record the exact blocker and do not claim iOS validation passed.

- [ ] **Step 4: Update OpenSpec task checklist**

Edit `openspec/changes/navigation-animations/tasks.md` so all completed items are checked:

```markdown
# Tasks

- [x] 1. Add pure navigation transition classification beside `LibraryNavigationStack`.
  - [x] Add transition kinds for push, pop, replace, root, and no-op route changes.
  - [x] Keep `LibraryNavigationStack` as the source of truth; transition metadata must not become a second navigation stack.
  - [x] Add common tests proving push/pop/root/duplicate behavior and preserving existing route-stack expectations.

- [x] 2. Add root-level shared Compose route animation.
  - [x] Wrap the `LibraryHomeScreen` route switch in `AnimatedContent` keyed by `navigation.current`.
  - [x] Apply direction-aware push/pop/root/replace transitions with conservative 220-260ms timing.
  - [x] Preserve existing visible back, left-edge swipe, Android system/predictive back, playback, scanner, library, and theme behavior.

- [x] 3. Verify and record evidence.
  - [x] Run `openspec validate navigation-animations --strict`.
  - [x] Run focused common navigation tests.
  - [x] Run broad JVM/desktop/Android verification.
  - [x] Update `progress.md` with exact commands, outcomes, changed files, risks, and next owner.
```

If any verification command is blocked, leave only that subtask unchecked and record the blocker in `progress.md`.

- [ ] **Step 5: Update `progress.md`**

Prepend a handoff entry using this exact structure, filling in final command outcomes and changed files:

```text
## Handoff - 2026-07-02 navigation animations

Route: openspec+superpowers
Owner: implementation
Scope: Add shared Compose direction-aware route transition animations for Home, detail, Now Playing, Search, Settings, and Clear Library dialog routes.
Implementation:
- Added pure navigation transition classification for push/pop/replace/root/no-op route changes.
- Added common tests covering transition classification and preserving route-stack behavior.
- Wrapped shared route rendering in root-level AnimatedContent with direction-aware push/pop/root/replace transitions.
- Preserved existing visible back, left-edge swipe, Android system/predictive back, playback, scanner, library, and theme behavior.
Verification:
- `openspec validate navigation-animations --strict`: pass
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass
- `/usr/bin/xcrun xcodebuild -version`: <version or blocker>
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: <pass or blocker>
Acceptance:
- Requirement matched: yes — shared route changes animate and push/pop directions are distinct.
- Scope controlled: yes — no new dependencies, native navigation migration, playback, scanner, persistence, or theme behavior changes.
- Edge cases/risk reviewed: automated checks prove route metadata and compilation; subjective animation polish still needs manual visual validation on Android/iOS/macOS.
Changed files:
- `docs/superpowers/specs/2026-07-02-navigation-animations-design.md`
- `docs/superpowers/plans/2026-07-02-navigation-animations.md`
- `openspec/changes/navigation-animations/proposal.md`
- `openspec/changes/navigation-animations/design.md`
- `openspec/changes/navigation-animations/specs/library-navigation/spec.md`
- `openspec/changes/navigation-animations/tasks.md`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `progress.md`
Next owner: user for manual visual validation; OpenSpec/user for archive when satisfied.
Blockers: none for automated verification.
Commit: pending semantic commit after user reviews staged diff, unless user asks not to commit.
```

- [ ] **Step 6: Final diff review**

Run:

```bash
git diff --check
git diff --stat
git diff -- docs/superpowers/specs/2026-07-02-navigation-animations-design.md docs/superpowers/plans/2026-07-02-navigation-animations.md openspec/changes/navigation-animations shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt progress.md | cat
```

Expected: no whitespace errors; diff matches this plan only.

- [ ] **Step 7: Commit only after staged-diff summary**

Because the user prefers staged diffs described before committing, do not commit silently. If committing is requested at the end of the OpenSpec workflow:

```bash
git add docs/superpowers/specs/2026-07-02-navigation-animations-design.md docs/superpowers/plans/2026-07-02-navigation-animations.md openspec/changes/navigation-animations shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt progress.md
git diff --cached --stat
git diff --cached -- docs/superpowers/specs/2026-07-02-navigation-animations-design.md docs/superpowers/plans/2026-07-02-navigation-animations.md openspec/changes/navigation-animations shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt progress.md | cat
```

Summarize the staged diff, then commit only if the user approves:

```bash
git commit -m "feat: add navigation animations"
```

## Self-Review

- Spec coverage: Task 1 covers pure transition classification and tests. Task 2 covers all shared Compose route animation requirements without new dependencies. Task 3 covers OpenSpec validation, broad verification, iOS availability, task checklist, and handoff evidence.
- Red-flag scan: no incomplete-marker or vague filler steps remain.
- Type consistency: plan uses `LibraryNavigationTransition` and `classifyNavigationTransition(from, to)` consistently across tests and implementation steps.
