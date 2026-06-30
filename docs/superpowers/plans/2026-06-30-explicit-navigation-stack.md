# Explicit Navigation Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace RhythHaus's ad-hoc local navigation booleans/nullables with a small explicit shared route stack so Android/system back and shared swipe-back return to the previous in-app screen.

**Architecture:** Add a pure common Kotlin `LibraryRoute` / `LibraryNavigationStack` model with Home as an immutable root. Refactor `LibraryHomeScreen` to render from `navigation.current` and push/pop routes for Album Detail, Artist Detail, Now Playing, Search, Settings, and Clear Library dialog while preserving existing screen visuals and playback behavior.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform common UI, kotlin.test common tests, existing Compose `BackHandler` dependency.

## Global Constraints

- WORK IN: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus`.
- Shared-first implementation: common route model and UI changes go under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus`.
- Do not add a new navigation dependency or platform-native navigation controller.
- Preserve current UI visuals and playback behavior except route/back behavior.
- Home is the immutable navigation root; Android back is consumed only when a route can pop.
- Search, Settings, Now Playing, and Clear Library dialog must preserve the opener route as their back destination.
- Follow TDD for production code: write the focused failing test first, verify RED, implement, verify GREEN.
- Do not commit unless explicitly instructed by the coordinator; this repository already has unrelated modified files.

---

### Task 1: Pure navigation route stack

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface LibraryRoute`
  - `data object LibraryRoute.Home : LibraryRoute`
  - `data class LibraryRoute.AlbumDetail(val album: String) : LibraryRoute`
  - `data class LibraryRoute.ArtistDetail(val artist: String) : LibraryRoute`
  - `data object LibraryRoute.NowPlaying : LibraryRoute`
  - `data object LibraryRoute.Search : LibraryRoute`
  - `data object LibraryRoute.Settings : LibraryRoute`
  - `data object LibraryRoute.ClearLibraryDialog : LibraryRoute`
  - `data class LibraryNavigationStack(val routes: List<LibraryRoute> = listOf(LibraryRoute.Home))`
  - `val LibraryNavigationStack.current: LibraryRoute`
  - `val LibraryNavigationStack.canPop: Boolean`
  - `fun LibraryNavigationStack.push(route: LibraryRoute): LibraryNavigationStack`
  - `fun LibraryNavigationStack.replaceTop(route: LibraryRoute): LibraryNavigationStack`
  - `fun LibraryNavigationStack.pop(): LibraryNavigationStack`
  - `fun LibraryNavigationStack.popToRoot(): LibraryNavigationStack`
- Consumes: none.

- [ ] **Step 1: Write the failing navigation tests**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`:

```kotlin
package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryNavigationTest {
    @Test
    fun rootStackStartsAtHomeAndCannotPopPastHome() {
        val stack = LibraryNavigationStack()

        assertEquals(LibraryRoute.Home, stack.current)
        assertFalse(stack.canPop)
        assertEquals(stack, stack.pop())
    }

    @Test
    fun duplicateTopPushIsNoOp() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Search)
            .push(LibraryRoute.Search)

        assertEquals(listOf(LibraryRoute.Home, LibraryRoute.Search), stack.routes)
    }

    @Test
    fun searchOpenedFromAlbumReturnsToAlbum() {
        val album = LibraryRoute.AlbumDetail("Blue Train")
        val stack = LibraryNavigationStack()
            .push(album)
            .push(LibraryRoute.Search)

        assertEquals(LibraryRoute.Search, stack.current)
        assertTrue(stack.canPop)
        assertEquals(album, stack.pop().current)
        assertEquals(LibraryRoute.Home, stack.pop().pop().current)
    }

    @Test
    fun nowPlayingOpenedFromArtistReturnsToArtist() {
        val artist = LibraryRoute.ArtistDetail("John Coltrane")
        val stack = LibraryNavigationStack()
            .push(artist)
            .push(LibraryRoute.NowPlaying)

        assertEquals(LibraryRoute.NowPlaying, stack.current)
        assertEquals(artist, stack.pop().current)
    }

    @Test
    fun clearDialogPopsBackToSettingsOrigin() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.Settings)
            .push(LibraryRoute.ClearLibraryDialog)

        assertEquals(LibraryRoute.ClearLibraryDialog, stack.current)
        assertEquals(LibraryRoute.Settings, stack.pop().current)
    }

    @Test
    fun pushingHomeReturnsToRoot() {
        val stack = LibraryNavigationStack()
            .push(LibraryRoute.AlbumDetail("A"))
            .push(LibraryRoute.Search)
            .push(LibraryRoute.Home)

        assertEquals(listOf(LibraryRoute.Home), stack.routes)
        assertEquals(LibraryRoute.Home, stack.current)
    }
}
```

- [ ] **Step 2: Run focused test to verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: FAIL because `LibraryNavigationStack` / `LibraryRoute` do not exist yet.

- [ ] **Step 3: Implement the route stack**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`:

```kotlin
package com.eterocell.rhythhaus

sealed interface LibraryRoute {
    data object Home : LibraryRoute
    data class AlbumDetail(val album: String) : LibraryRoute
    data class ArtistDetail(val artist: String) : LibraryRoute
    data object NowPlaying : LibraryRoute
    data object Search : LibraryRoute
    data object Settings : LibraryRoute
    data object ClearLibraryDialog : LibraryRoute
}

data class LibraryNavigationStack(
    val routes: List<LibraryRoute> = listOf(LibraryRoute.Home),
) {
    val current: LibraryRoute = routes.lastOrNull() ?: LibraryRoute.Home
    val canPop: Boolean = routes.size > 1

    fun push(route: LibraryRoute): LibraryNavigationStack = when {
        route == LibraryRoute.Home -> popToRoot()
        route == current -> this
        else -> copy(routes = normalizedRoutes(routes + route))
    }

    fun replaceTop(route: LibraryRoute): LibraryNavigationStack = when {
        route == LibraryRoute.Home -> popToRoot()
        routes.size <= 1 -> push(route)
        else -> copy(routes = normalizedRoutes(routes.dropLast(1) + route))
    }

    fun pop(): LibraryNavigationStack = if (canPop) {
        copy(routes = routes.dropLast(1))
    } else {
        this
    }

    fun popToRoot(): LibraryNavigationStack = copy(routes = listOf(LibraryRoute.Home))

    private fun normalizedRoutes(candidate: List<LibraryRoute>): List<LibraryRoute> = when {
        candidate.isEmpty() -> listOf(LibraryRoute.Home)
        candidate.first() != LibraryRoute.Home -> listOf(LibraryRoute.Home) + candidate.filterNot { it == LibraryRoute.Home }
        else -> candidate
    }
}
```

- [ ] **Step 4: Run focused test to verify GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: BUILD SUCCESSFUL.

---

### Task 2: Route-driven screen rendering in `LibraryHomeScreen`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`

**Interfaces:**
- Consumes: `LibraryRoute`, `LibraryNavigationStack` from Task 1.
- Produces: `LibraryHomeScreen` rendering based on `navigation.current` instead of `selectedAlbum`, `selectedArtist`, and `showNowPlayingScreen`.

- [ ] **Step 1: Inspect current route state block**

Read `App.kt` around `LibraryHomeScreen`. The current local navigation state to replace is:

```kotlin
var selectedAlbum by remember { mutableStateOf<AlbumGroup?>(null) }
var selectedArtist by remember { mutableStateOf<ArtistGroup?>(null) }
var showNowPlayingScreen by remember { mutableStateOf(false) }
```

Keep `browseMode` and selected-track synchronization logic.

- [ ] **Step 2: Add stack state and helpers**

In `LibraryHomeScreen`, after `browseMode`, add:

```kotlin
var navigation by remember { mutableStateOf(LibraryNavigationStack()) }
fun pushRoute(route: LibraryRoute) {
    navigation = navigation.push(route)
}
fun popRoute() {
    navigation = navigation.pop()
}
```

Do not remove overlay booleans yet in this task; Task 3 handles Settings/Search/dialog.

- [ ] **Step 3: Replace album/artist route mutations**

Change album card click from:

```kotlin
onClick = { selectedAlbum = albumGroup },
```

to:

```kotlin
onClick = { pushRoute(LibraryRoute.AlbumDetail(albumGroup.album)) },
```

Change artist row click from:

```kotlin
onClick = { selectedArtist = artistGroup },
```

to:

```kotlin
onClick = { pushRoute(LibraryRoute.ArtistDetail(artistGroup.artist)) },
```

Change bottom bar expand from:

```kotlin
onExpand = { if (selectedTrack != null) showNowPlayingScreen = true },
```

to:

```kotlin
onExpand = { if (selectedTrack != null) pushRoute(LibraryRoute.NowPlaying) },
```

Do the same in `DrillDownView`'s NowPlayingBar `onExpand`.

- [ ] **Step 4: Render by `navigation.current`**

Replace the outer `if (selectedAlbum != null) ... else if (selectedArtist != null) ... else ...` chain with a `when (val route = navigation.current)`.

Use these resolution patterns:

```kotlin
val albums = remember(snapshot.tracks) { groupTracksByAlbum(snapshot.tracks) }
val artists = remember(snapshot.tracks) { groupTracksByArtist(snapshot.tracks) }
```

For album route:

```kotlin
is LibraryRoute.AlbumDetail -> {
    val album = albums.firstOrNull { it.album == route.album }
    if (album == null) {
        LaunchedEffect(route) { navigation = navigation.pop() }
        return@LibraryHomeScreen
    }
    // existing album DrillDownView body, with onBack = ::popRoute
}
```

For artist route, mirror album route with `artists.firstOrNull { it.artist == route.artist }`.

For Now Playing route:

```kotlin
LibraryRoute.NowPlaying -> {
    if (selectedTrack == null) {
        LaunchedEffect(route) { navigation = navigation.pop() }
        return@LibraryHomeScreen
    }
    val currentLibTrack = libraryTracks.firstOrNull { it.id == selectedTrack.id }
    NowPlayingScreen(
        track = selectedTrack,
        playbackState = playbackState,
        playbackController = playbackController,
        tagLibReader = tagLibReader,
        currentLibraryTrack = currentLibTrack,
        onBack = ::popRoute,
    )
}
```

For Home, Settings, Search, and ClearLibraryDialog in this task, render the existing Home content path. Task 3 will overlay Settings/Search/dialog from routes.

- [ ] **Step 5: Remove replaced local state**

Remove `selectedAlbum`, `selectedArtist`, and `showNowPlayingScreen` declarations and all assignments to them.

- [ ] **Step 6: Compile focused common code**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: BUILD SUCCESSFUL.

---

### Task 3: Overlay/dialog routes and back integration

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `openspec/changes/explicit-navigation-stack/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: Task 1 route stack and Task 2 route rendering.
- Produces: Settings, Search, Clear Library dialog, `BackHandler`, bottom bar, and screen callbacks driven by route stack.

- [ ] **Step 1: Replace top-level overlay state parameters**

In `App()`, remove:

```kotlin
var showClearDialog by remember { mutableStateOf(false) }
var showSettings by remember { mutableStateOf(false) }
var showSearch by remember { mutableStateOf(false) }
```

Remove the corresponding `showClearDialog`, `onShowClearDialog`, `showSettings`, `onShowSettings`, `showSearch`, and `onShowSearch` parameters from `LibraryHomeScreen` and its call site.

Keep repository clear behavior, but let `LibraryHomeScreen` own the dialog route push/pop.

- [ ] **Step 2: Convert settings/search entry points to route pushes**

Where the UI currently calls `onShowSettings(true)` or `onShowSearch(true)`, change to:

```kotlin
pushRoute(LibraryRoute.Settings)
pushRoute(LibraryRoute.Search)
```

This includes:
- Home `NowPlayingBar`
- Album/artist `DrillDownView` call sites
- `DrillDownView` internal `NowPlayingBar`

- [ ] **Step 3: Convert clear dialog to route**

Replace dialog visibility condition with:

```kotlin
if (navigation.current == LibraryRoute.ClearLibraryDialog) {
    BackHandler { popRoute() }
    Dialog(onDismissRequest = ::popRoute) {
        // existing dialog content
    }
}
```

Change cancel button and confirm behavior:

```kotlin
onClick = ::popRoute
```

```kotlin
onClick = {
    onClearLibrary()
    popRoute()
}
```

If `onClearLibrary` currently closes the dialog itself, remove that responsibility from the callback and leave route pop in the UI layer.

- [ ] **Step 4: Convert Settings route overlay**

Replace `if (showSettings)` with:

```kotlin
if (navigation.current == LibraryRoute.Settings) {
    BackHandler { popRoute() }
    SettingsScreen(
        folderPickerLauncher = folderPickerLauncher,
        importMessage = importMessage,
        scanProgress = scanProgress,
        scanJob = scanJob,
        hasImportedTracks = snapshot.tracks.isNotEmpty(),
        onClearLibrary = { pushRoute(LibraryRoute.ClearLibraryDialog) },
        onDismiss = ::popRoute,
    )
}
```

- [ ] **Step 5: Convert Search route overlay**

Replace `if (showSearch)` with:

```kotlin
if (navigation.current == LibraryRoute.Search) {
    BackHandler { popRoute() }
    SearchScreen(
        libraryTracks = libraryTracks,
        tagLibReader = tagLibReader,
        playbackController = playbackController,
        playbackState = playbackState,
        onDismiss = ::popRoute,
    )
}
```

- [ ] **Step 6: Add central BackHandler for stack routes**

Add one central handler after route helper declarations:

```kotlin
BackHandler(enabled = navigation.canPop) {
    popRoute()
}
```

If route-specific handlers for overlays remain, ensure they only call `popRoute()` and do not conflict. It is acceptable to remove route-specific BackHandlers where the central handler already covers them, but dialogs can keep an explicit handler for readability.

- [ ] **Step 7: Update OpenSpec task checkboxes and progress evidence**

After code and verification, update `openspec/changes/explicit-navigation-stack/tasks.md` to mark completed items `[x]`.

Append a `progress.md` handoff with:

```text
## Handoff - 2026-06-30 explicit navigation stack

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Replace ad-hoc shared Compose navigation booleans/nullables with explicit route stack.
Implementation:
- Added `LibraryRoute` and `LibraryNavigationStack` with common tests.
- Refactored `LibraryHomeScreen` route rendering for Home, Album Detail, Artist Detail, Now Playing, Search, Settings, and Clear Library dialog.
Verification:
- <exact commands and outcomes>
Acceptance:
- Requirement matched: <yes/no>
- Scope controlled: <yes/no>
Changed files:
- <paths>
Next owner: user for manual Android system/gesture-back validation on device/emulator.
Blockers: <none or exact blocker>
```

- [ ] **Step 8: Run verification**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
openspec validate explicit-navigation-stack --strict
```

Expected: all commands succeed. If any fails, fix root cause or record exact blocker before reporting completion.
