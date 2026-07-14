# Hide Now Playing Bar on Settings Information Screens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hide the shell-owned Now Playing bottom bar only on Settings, About, and Open Source Libraries while preserving playback and existing scroll-driven visibility.

**Architecture:** Add pure, exhaustive route eligibility and combined visibility functions beside `LibraryRoute`, then use the combined function as the app shell animation target. Keep the bar composed so existing return-animation behavior remains intact; measure its wrapper and translate the complete interactive height while fading it out.

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, `kotlin.test`, Gradle, OpenSpec.

## Global Constraints

- `LibraryRoute.Settings`, `LibraryRoute.SettingsAbout`, and `LibraryRoute.OpenSourceLibraries` MUST suppress the Now Playing bottom bar.
- Every route outside that settings-information group MUST retain current behavior.
- Route eligibility MUST combine with the existing scroll-derived visibility state and MUST NOT overwrite it.
- Playback, queues, persistence, navigation structure, dependencies, platform entry points, and database schemas MUST remain unchanged.
- Use strict RED-GREEN TDD and make no unrelated refactors.

---

### Task 1: Route-Aware Bottom-Bar Visibility

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt:342-370`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`

**Interfaces:**
- Consumes: `LibraryRoute` and `LibraryAppState.isNowPlayingBarVisible: Boolean`.
- Produces: `fun routePermitsNowPlayingBar(route: LibraryRoute): Boolean` and `fun shouldShowNowPlayingBar(route: LibraryRoute, existingVisibility: Boolean): Boolean`.

- [x] **Step 1: Write failing route-policy tests**

Add imports only if required, then add these tests inside `LibraryNavigationTest`:

```kotlin
@Test
fun settingsInformationRoutesSuppressNowPlayingBar() {
    assertFalse(routePermitsNowPlayingBar(LibraryRoute.Settings))
    assertFalse(routePermitsNowPlayingBar(LibraryRoute.SettingsAbout))
    assertFalse(routePermitsNowPlayingBar(LibraryRoute.OpenSourceLibraries))
}

@Test
fun otherRoutesPermitNowPlayingBar() {
    val permittedRoutes = listOf(
        LibraryRoute.Home,
        LibraryRoute.AlbumDetail("Album"),
        LibraryRoute.ArtistDetail("Artist"),
        LibraryRoute.NowPlaying,
        LibraryRoute.Search,
        LibraryRoute.ClearLibraryDialog,
    )

    permittedRoutes.forEach { route ->
        assertTrue(routePermitsNowPlayingBar(route), "Expected $route to permit the bar")
    }
}

@Test
fun routeEligibilityCombinesWithExistingVisibility() {
    assertTrue(shouldShowNowPlayingBar(LibraryRoute.Home, existingVisibility = true))
    assertFalse(shouldShowNowPlayingBar(LibraryRoute.Home, existingVisibility = false))
    assertFalse(shouldShowNowPlayingBar(LibraryRoute.Settings, existingVisibility = true))
    assertFalse(shouldShowNowPlayingBar(LibraryRoute.SettingsAbout, existingVisibility = true))
}
```

- [x] **Step 2: Run the focused test to verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Expected: compilation fails because `routePermitsNowPlayingBar` and `shouldShowNowPlayingBar` do not exist.

- [x] **Step 3: Add the minimal exhaustive route policy**

Add beside the other pure route policies in `LibraryNavigation.kt`:

```kotlin
fun routePermitsNowPlayingBar(route: LibraryRoute): Boolean = when (route) {
    LibraryRoute.Settings,
    LibraryRoute.SettingsAbout,
    LibraryRoute.OpenSourceLibraries,
    -> false

    LibraryRoute.Home,
    is LibraryRoute.AlbumDetail,
    is LibraryRoute.ArtistDetail,
    LibraryRoute.NowPlaying,
    LibraryRoute.Search,
    LibraryRoute.ClearLibraryDialog,
    -> true
}

fun shouldShowNowPlayingBar(
    route: LibraryRoute,
    existingVisibility: Boolean,
): Boolean = existingVisibility && routePermitsNowPlayingBar(route)
```

- [x] **Step 4: Integrate the combined policy without removing the bar from composition**

In `LibraryAppShell.kt`, calculate the target immediately before `animateFloatAsState` and use it in the animation target:

```kotlin
val shouldShowBottomBar = shouldShowNowPlayingBar(
    route = appState.navigation.current,
    existingVisibility = appState.isNowPlayingBarVisible,
)
val bottomBarOffset by animateFloatAsState(
    targetValue = if (shouldShowBottomBar) 0f else 1f,
    animationSpec = tween(250),
    label = "BottomBarOffset",
)
```

Measure the wrapper with `onSizeChanged`, calculate its pixel offset with the tested `nowPlayingBarOffsetPx(hiddenFraction, measuredHeightPx)` helper, and apply the offset with `IntOffset`. Do not change `NowPlayingBar` content, playback callbacks, `LibraryAppState`, or the overlay logic.

- [x] **Step 5: Run focused tests and compilation to verify GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL` and all `LibraryNavigationTest` cases pass.

- [x] **Step 6: Review Task 1 before integration**

Confirm the follow-up diff changes only the route policy, focused tests, and required artifacts. Confirm Open Source Libraries is explicitly suppressed and no playback method is called by the new policy.

### Task 2: Verification and Durable Evidence

**Files:**
- Modify: `openspec/changes/hide-now-playing-settings-about/tasks.md`
- Modify: `roadmap.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: the completed Task 1 implementation and OpenSpec requirements.
- Produces: strict validation, platform verification results, checked task boxes, and session handoff evidence.

- [x] **Step 1: Run strict OpenSpec validation**

Run:

```bash
openspec validate hide-now-playing-settings-about --strict
```

Expected: `Change 'hide-now-playing-settings-about' is valid`.

- [x] **Step 2: Run the supported JVM, desktop, and Android matrix**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 3: Check iOS tooling and attempt simulator tests**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: record the actual result. If the known unrelated `AppScanCancellationTest.kt` JVM-only `Thread` references still block common-test compilation, record the exact locations and do not claim an iOS test pass.

- [x] **Step 4: Check source hygiene**

Run:

```bash
GIT_MASTER=1 git diff --check
```

Expected: no output and exit code 0.

- [x] **Step 5: Update lifecycle artifacts**

Mark completed OpenSpec tasks with `- [x]`. Update the roadmap and `progress.md` handoff to state the exact three suppressed routes, verification results, and manual visual-QA limitation.

- [x] **Step 6: Perform final review and leave an explicit integration state**

Review all changes against the spec. After verification, commit the implementation and its tests atomically, then commit the planning/evidence artifacts separately. Do not push.

## Plan Self-Review

- Every requirement maps to Task 1 tests and implementation or Task 2 verification/evidence.
- Function names and signatures are consistent across tests, production code, and shell integration.
- The plan contains no unresolved placeholders and explicitly suppresses the full settings-information route group while preserving playback, scroll state, and composition behavior.
