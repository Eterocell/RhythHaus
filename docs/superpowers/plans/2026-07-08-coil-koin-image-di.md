# Coil Artwork Loading and Koin Dependency Injection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use Coil-backed shared Compose artwork loading/caching and Koin dependency injection for the existing RhythHaus service graph.

**Architecture:** Add Coil and Koin through the version catalog, create a shared Koin module with idempotent startup, and move current `App()` service construction into DI. Add a small Coil-backed artwork composable/helper and route existing artwork surfaces through it while preserving visuals, fallbacks, models, scanner behavior, and playback behavior.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Coil 3.5.0, Koin 4.2.2, SQLDelight, existing RhythHaus shared UI.

## Global Constraints

- Coil version MUST be `3.5.0` and Koin version MUST be `4.2.2` unless implementation proves a compile blocker and records the exact reason.
- No SQLDelight schema migration.
- No scanner, TagLib/native extraction, or playback-engine behavior changes.
- Preserve existing visual layout, fallback text, content descriptions, shapes, click behavior, selected overlays, nested scroll behavior, and playback controls.
- Preserve original `artworkBytes` in library/playback models for platform media metadata and full-size display.
- Scope is Android, iOS, and desktop JVM/macOS only.
- Koin initialization MUST be idempotent and happen before `App()` renders on Android, desktop, and iOS.
- `App()` MUST resolve the existing service graph from Koin instead of constructing services inline.

---

### Task 1: Add dependencies and shared DI startup

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
- Modify: `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt`
- Modify: `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/main.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt`

**Interfaces:**
- Produces: `fun startRhythHausKoin()` idempotent startup helper.
- Produces: Koin module bindings for `PlaybackController`, `AudioMetadataReader`, `TagLibReader`, `LibraryDatabase`, `LibraryRepository`, `PlatformSourceAccess`, `PlatformAudioScanner`, `LibraryScanner`, and `ThemePreferenceStore`.
- Consumes: existing factories `createLibraryDatabase()`, `createPlatformSourceAccess()`, `createThemePreferenceStore()`, `createTagLibReader()`, `currentTimeMillis()`, and `uuid4()`.

- [ ] **Step 1: Add version catalog entries**

In `gradle/libs.versions.toml`, add under `[versions]`:

```toml
coil = "3.5.0"
koin = "4.2.2"
```

Add under `[libraries]`:

```toml
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-core = { module = "io.coil-kt.coil3:coil-core", version.ref = "coil" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
```

- [ ] **Step 2: Add shared dependencies**

In `shared/build.gradle.kts`, add to `commonMain.dependencies`:

```kotlin
implementation(libs.coil.compose)
implementation(libs.coil.core)
implementation(libs.koin.core)
implementation(libs.koin.compose)
```

If compilation later proves a platform-specific Coil artifact is required, add the narrowest necessary dependency and record it in `openspec/changes/coil-koin-image-di/tasks.md`.

- [ ] **Step 3: Create the DI module and idempotent startup helper**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt` with:

```kotlin
package com.eterocell.rhythhaus.di

import com.eterocell.rhythhaus.AudioMetadataReader
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.library.LibraryDatabase
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.PlatformAudioScanner
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.SqlDelightLibraryRepository
import com.eterocell.rhythhaus.library.createLibraryDatabase
import com.eterocell.rhythhaus.library.createPlatformSourceAccess
import com.eterocell.rhythhaus.library.currentTimeMillis
import com.eterocell.rhythhaus.library.uuid4
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.createTagLibReader
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.theme.createThemePreferenceStore
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

fun rhythHausModule(): Module = module {
    single<TagLibReader> { createTagLibReader() }
    single { AudioMetadataReader(tagLibReader = get()) }
    single<LibraryDatabase> { createLibraryDatabase() }
    single<LibraryRepository> { SqlDelightLibraryRepository(get()) }
    single<PlatformSourceAccess> { createPlatformSourceAccess() }
    single<PlatformAudioScanner> { get<PlatformSourceAccess>() as PlatformAudioScanner }
    single { PlaybackController() }
    single { createThemePreferenceStore() }
    single {
        LibraryScanner(
            repository = get(),
            platformScanner = get(),
            metadataReader = get(),
            now = { currentTimeMillis() },
            idFactory = { _ -> uuid4() },
        )
    }
}

fun startRhythHausKoin() {
    if (GlobalContext.getOrNull() != null) return
    startKoin {
        modules(rhythHausModule())
    }
}
```

If `ThemePreferenceStore` is not public or the import differs, inspect `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.kt` and use the actual type.

- [ ] **Step 4: Initialize Koin on Android**

In `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt`, import `com.eterocell.rhythhaus.di.startRhythHausKoin` and call it after `setRhythHausAndroidContext(this)` and before `setContent`:

```kotlin
setRhythHausAndroidContext(this)
startRhythHausKoin()
ensureNotificationPermission()
```

- [ ] **Step 5: Initialize Koin on desktop**

In `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/main.kt`, import `com.eterocell.rhythhaus.di.startRhythHausKoin` and call it before `application` creates the window:

```kotlin
fun main() {
    startRhythHausKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "RhythHaus",
        ) {
            App()
        }
    }
}
```

- [ ] **Step 6: Initialize Koin on iOS**

In `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt`, import `com.eterocell.rhythhaus.di.startRhythHausKoin` and initialize before returning the controller:

```kotlin
@Suppress("FunctionName")
fun MainViewController() = ComposeUIViewController {
    startRhythHausKoin()
    App()
}
```

If Compose creates the lambda too late for the stated requirement, use a block-bodied function:

```kotlin
@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    startRhythHausKoin()
    return ComposeUIViewController { App() }
}
```

- [ ] **Step 7: Verify focused compilation**

Run: `./gradlew :shared:compileKotlinJvm --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

### Task 2: Refactor App to resolve services from Koin

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Optional test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`

**Interfaces:**
- Consumes: Koin bindings from Task 1.
- Produces: `App()` that resolves `PlaybackController`, `AudioMetadataReader`, `TagLibReader`, `LibraryRepository`, `PlatformSourceAccess`, `LibraryScanner`, and `ThemePreferenceStore` from Koin.

- [ ] **Step 1: Replace direct construction imports**

In `App.kt`, remove now-unused imports for direct construction:

```kotlin
import com.eterocell.rhythhaus.library.SqlDelightLibraryRepository
import com.eterocell.rhythhaus.library.createLibraryDatabase
import com.eterocell.rhythhaus.library.createPlatformSourceAccess
import com.eterocell.rhythhaus.library.currentTimeMillis
import com.eterocell.rhythhaus.library.uuid4
import com.eterocell.rhythhaus.taglib.createTagLibReader
import com.eterocell.rhythhaus.theme.createThemePreferenceStore
```

Add:

```kotlin
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import org.koin.compose.koinInject
```

Keep `PlatformAudioScanner` only if still needed by type checks; after DI it should not be needed in `App()`.

- [ ] **Step 2: Resolve services through Koin**

Replace the top of `App()` service construction with:

```kotlin
val controller = koinInject<PlaybackController>()
val tagLibReader = koinInject<TagLibReader>()
val repository = koinInject<LibraryRepository>()
val platformAccess = koinInject<PlatformSourceAccess>()
val scanner = koinInject<LibraryScanner>()
val themePreferenceStore = koinInject<ThemePreferenceStore>()
```

Remove the inline `remember { ... }` blocks that constructed those services. Keep local mutable UI state (`libraryTracks`, `importMessage`, `scanProgress`, `scanJob`) unchanged.

- [ ] **Step 3: Preserve disposal semantics**

Keep the existing `DisposableEffect(controller) { onDispose { controller.release() } }` unchanged so the Koin-provided controller is released when the composition is disposed.

- [ ] **Step 4: Preserve folder picker and scan behavior**

Keep `rememberPlatformFolderPickerLauncher { result -> ... }` behavior unchanged, including:

```kotlin
val source = result.source
scanJob = scope.launch(Dispatchers.Default) { ... }
val session = scanner.scan(source) { scanJob?.isActive != true }
libraryTracks = repository.tracks()
```

No scanner logic should move into this task.

- [ ] **Step 5: Add a DI smoke test if possible without platform context**

If Koin can start with an override test module in common/JVM tests without touching platform context, create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt` that verifies Koin can resolve `LibraryScanner` from fake/test-safe dependencies. If this requires platform-only context or conflicts with global Koin state, skip the test and record the reason in tasks evidence; do not add brittle platform-context tests.

- [ ] **Step 6: Verify focused compilation/tests**

Run: `./gradlew :shared:compileKotlinJvm --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

Run any added focused test, for example:
`./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

### Task 3: Add Coil-backed artwork helper and update artwork call sites

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkImage.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt`
- Optional test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/ArtworkImageTest.kt`

**Interfaces:**
- Produces: `enum class ArtworkImageRole { Thumbnail, Card, Hero }`.
- Produces: `fun artworkMemoryCacheKey(bytes: ByteArray, role: ArtworkImageRole): String` as an internal pure helper.
- Produces: `@Composable fun ArtworkImage(...)` wrapper using Coil.

- [ ] **Step 1: Create the artwork helper**

Create `ArtworkImage.kt`:

```kotlin
package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class ArtworkImageRole(val keySuffix: String) {
    Thumbnail("thumbnail"),
    Card("card"),
    Hero("hero"),
}

internal fun artworkMemoryCacheKey(bytes: ByteArray, role: ArtworkImageRole): String =
    "artwork:${role.keySuffix}:${bytes.contentHashCode()}:${bytes.size}"

@Composable
internal fun ArtworkImage(
    artworkBytes: ByteArray?,
    contentDescription: String,
    role: ArtworkImageRole,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit,
) {
    if (artworkBytes == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { fallback() }
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(coil3.PlatformContext.INSTANCE)
            .data(artworkBytes)
            .memoryCacheKey(artworkMemoryCacheKey(artworkBytes, role))
            .diskCacheKey(artworkMemoryCacheKey(artworkBytes, role))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = null,
    )
}
```

If `coil3.PlatformContext.INSTANCE`, `memoryCacheKey`, or `diskCacheKey` API differs in Coil 3.5.0, inspect dependency API via compiler errors and use the correct Coil 3 KMP API. Preserve the public interface above.

- [ ] **Step 2: Add a pure cache-key test**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/ArtworkImageTest.kt`:

```kotlin
package com.eterocell.rhythhaus.ui

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ArtworkImageTest {
    @Test
    fun artworkMemoryCacheKeyIncludesRoleAndSize() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val thumbnailKey = artworkMemoryCacheKey(bytes, ArtworkImageRole.Thumbnail)
        val heroKey = artworkMemoryCacheKey(bytes, ArtworkImageRole.Hero)

        assertNotEquals(thumbnailKey, heroKey)
        assertTrue(thumbnailKey.contains("thumbnail"))
        assertTrue(heroKey.contains("hero"))
        assertTrue(thumbnailKey.endsWith(":4"))
    }
}
```

- [ ] **Step 3: Update track row `AlbumMark`**

In `LibraryRows.kt`, remove direct `decodeArtworkThumbnailCached` usage in `AlbumMark`. Use `ArtworkImage` with `ArtworkImageRole.Thumbnail` and keep existing fallback content. The selected overlay must still appear above artwork/fallback as it does now.

- [ ] **Step 4: Update album and artist surfaces**

In `LibraryRows.kt`, replace `remember(album.tracks) { firstNotNullOfOrNull { it.artworkBytes?.decodeArtworkCached() } }` and artist equivalent with candidate bytes selection only:

```kotlin
val albumArtworkBytes = remember(album.tracks) { album.tracks.firstNotNullOfOrNull { it.artworkBytes } }
```

Render with `ArtworkImage(role = ArtworkImageRole.Card, artworkBytes = albumArtworkBytes, ...)` and preserve existing fallback initials/equalizer content.

- [ ] **Step 5: Update drill-down top bar**

In `LibraryChrome.kt`, replace `topBarArtwork` bitmap decoding with candidate bytes selection:

```kotlin
val topBarArtworkBytes = remember(topBarArtworkCandidates) { topBarArtworkCandidates.firstOrNull() }
val hasArtwork = topBarArtworkBytes != null
```

Render the background with `ArtworkImage(role = ArtworkImageRole.Hero, artworkBytes = topBarArtworkBytes, ...)`, preserving the current scrim, chips, back button, compact animation, and no-placeholder fallback behavior. If Coil cannot load a byte array, its own error still leaves the existing container/scrim; do not add new text placeholders.

- [ ] **Step 6: Update compact NowPlayingBar**

In `NowPlayingBar.kt`, replace `decodeArtworkThumbnailCached()` bitmap logic with `ArtworkImage(role = ArtworkImageRole.Thumbnail, artworkBytes = track?.artworkBytes, ...)`, preserving fallback text and all controls/gesture behavior.

- [ ] **Step 7: Update expanded NowPlayingScreen**

In `NowPlayingScreen.kt`, remove `ImageBitmap` plumbing and pass `track.artworkBytes` to `NowPlayingArtworkPane`. Render with `ArtworkImage(role = ArtworkImageRole.Hero, artworkBytes = track.artworkBytes, ...)`, preserving fallback title text, gradient background, card shape, and adaptive layout behavior.

- [ ] **Step 8: Verify direct decode call sites**

Run:

```bash
grep -R "decodeArtworkCached\|decodeArtworkThumbnailCached\|decodeArtwork()" -n shared/src/commonMain/kotlin/com/eterocell/rhythhaus shared/src/commonTest/kotlin || true
```

Expected: updated Compose artwork surfaces no longer call direct decode helpers. Platform actual decoder files and old helper definitions may remain.

- [ ] **Step 9: Run focused tests and compilation**

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew :shared:compileKotlinJvm --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

### Task 4: Final verification, evidence, and commit

**Files:**
- Modify: `openspec/changes/coil-koin-image-di/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`

**Interfaces:**
- Consumes: implementation and verification output from Tasks 1-3.
- Produces: durable evidence and a semantic commit.

- [ ] **Step 1: Validate OpenSpec**

Run: `openspec validate coil-koin-image-di --strict`
Expected: `Change 'coil-koin-image-di' is valid`.

- [ ] **Step 2: Run focused and platform verification**

Run: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
Expected: `BUILD SUCCESSFUL`.

Run: `/usr/bin/xcrun xcodebuild -version`
Expected: prints installed Xcode version and exits 0.

Run: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
Expected: `BUILD SUCCESSFUL`, unless an exact toolchain blocker is recorded.

Run: `git diff --check`
Expected: no output and exit 0.

- [ ] **Step 3: Update OpenSpec task evidence**

Mark Tasks 1-4 complete in `openspec/changes/coil-koin-image-di/tasks.md` and add commands that actually ran with pass/fail evidence.

- [ ] **Step 4: Update session state**

Prepend a handoff entry to `progress.md` with route, owner, scope, verification, changed files, next owner, blockers, and commit status.

- [ ] **Step 5: Update roadmap**

Append a concise checked entry to `roadmap.md` noting that Coil-backed artwork loading and Koin DI were added, with manual artwork-cache/runtime QA remaining.

- [ ] **Step 6: Review staged diff before commit**

Run:

```bash
git status --short
git diff --stat
git diff --check
git diff --cached --stat
```

Stage only files in this change and describe the staged diff before committing.

- [ ] **Step 7: Commit**

Run:

```bash
git commit -m "feat: add Coil artwork loading and Koin DI"
```

Expected: commit succeeds.
