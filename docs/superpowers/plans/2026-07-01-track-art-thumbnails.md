# Track Art Thumbnails Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce track-list scroll jank by using cached thumbnail artwork for compact list/bar artwork surfaces while preserving full-size artwork for expanded playback UI.

**Architecture:** Keep original embedded artwork bytes in the existing models and repository schema. Add a common thumbnail cache API plus platform actual thumbnail decoders, then route only compact Compose artwork surfaces through that API. Use common tests for cache behavior and platform builds for actual decoder compilation.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform `ImageBitmap`, Android `BitmapFactory`, Skia (`org.jetbrains.skia`) on JVM/iOS, Kotlin common tests, Gradle configuration cache.

## Global Constraints

- Work in `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/.worktrees/track-art-thumbnails`; use absolute paths for file tools.
- OpenSpec change: `openspec/changes/track-art-thumbnails`.
- Superpowers design: `docs/superpowers/specs/2026-07-01-track-art-thumbnails-design.md`.
- No new dependencies.
- No SQLDelight schema changes or migrations.
- No scanner, TagLib/native metadata extraction, playback-engine, MediaSession, audio-session, or platform media metadata changes.
- Preserve `LibraryTrack.artworkBytes`, `Track.artworkBytes`, and `PlayableTrack.artworkBytes` as original embedded artwork bytes.
- Use thumbnails only for compact UI artwork: `AlbumMark`/`TrackRow` and `NowPlayingBar`.
- Keep `NowPlayingScreen` on full-size `decodeArtwork()`.
- Do not claim measured FPS/performance improvement without runtime profiling evidence.
- After code changes, run at least `./gradlew :shared:compileKotlinJvm --configuration-cache`; final task runs broad JVM/desktop/Android and iOS simulator verification.

---

### Task 1: Thumbnail cache and platform decoders

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.kt`
- Modify: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.android.kt`
- Modify: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.jvm.kt`
- Modify: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.ios.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ArtworkCacheTest.kt`
- Modify: `openspec/changes/track-art-thumbnails/tasks.md`

**Interfaces:**
- Consumes: existing `expect fun ByteArray.decodeArtwork(): ImageBitmap?`
- Produces: `expect fun ByteArray.decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap?`
- Produces: `fun ByteArray.decodeArtworkThumbnailCached(maxPixelSize: Int = 128): ImageBitmap?`
- Produces: `ArtworkCache.get(bytes: ByteArray, maxPixelSize: Int? = null)` and `put(bytes: ByteArray, image: ImageBitmap, maxPixelSize: Int? = null)`

- [ ] **Step 1: Write failing common tests for cache key separation and bucket reuse**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ArtworkCacheTest.kt`:

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ArtworkCacheTest {
    private val bytes = byteArrayOf(1, 2, 3, 4)

    @AfterTest
    fun tearDown() {
        ArtworkCache.clear()
    }

    @Test
    fun fullSizeAndThumbnailEntriesUseDistinctCacheKeys() {
        val full = ImageBitmap(4, 4)
        val thumbnail = ImageBitmap(2, 2)

        ArtworkCache.put(bytes, full)
        ArtworkCache.put(bytes, thumbnail, maxPixelSize = 128)

        assertSame(full, ArtworkCache.get(bytes))
        assertSame(thumbnail, ArtworkCache.get(bytes, maxPixelSize = 128))
        assertEquals(2, ArtworkCache.size())
    }

    @Test
    fun thumbnailSizeIsPartOfCacheKey() {
        val small = ImageBitmap(2, 2)
        val large = ImageBitmap(3, 3)

        ArtworkCache.put(bytes, small, maxPixelSize = 64)
        ArtworkCache.put(bytes, large, maxPixelSize = 128)

        assertSame(small, ArtworkCache.get(bytes, maxPixelSize = 64))
        assertSame(large, ArtworkCache.get(bytes, maxPixelSize = 128))
        assertNull(ArtworkCache.get(bytes, maxPixelSize = 96))
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache
```

Expected: FAIL because `ArtworkCache.get/put` do not yet accept `maxPixelSize`.

- [ ] **Step 3: Extend common artwork API and cache**

Replace `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.kt` with:

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.decodeArtwork(): ImageBitmap?

expect fun ByteArray.decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap?

/**
 * In-memory cache for decoded artwork images. Keyed by ByteArray content hash plus size bucket.
 * A null size bucket is the original/full-size decode; non-null buckets are thumbnails.
 * All access is from Compose @Composable functions on the main thread — no synchronization needed.
 */
object ArtworkCache {
    private data class Key(
        val contentHash: Int,
        val maxPixelSize: Int?,
    )

    private val cache = HashMap<Key, ImageBitmap>(64)

    fun get(bytes: ByteArray, maxPixelSize: Int? = null): ImageBitmap? = cache[Key(bytes.contentHashCode(), maxPixelSize)]

    fun put(bytes: ByteArray, image: ImageBitmap, maxPixelSize: Int? = null) {
        cache[Key(bytes.contentHashCode(), maxPixelSize)] = image
    }

    fun clear() = cache.clear()

    fun size(): Int = cache.size
}

/**
 * Decodes full-size artwork with caching — first checks ArtworkCache, falls back to platform decode.
 */
fun ByteArray.decodeArtworkCached(): ImageBitmap? = ArtworkCache.get(this) ?: decodeArtwork()?.also { ArtworkCache.put(this, it) }

/**
 * Decodes a thumbnail-sized artwork image with caching for compact list/bar surfaces.
 */
fun ByteArray.decodeArtworkThumbnailCached(maxPixelSize: Int = 128): ImageBitmap? =
    ArtworkCache.get(this, maxPixelSize) ?: decodeArtworkThumbnail(maxPixelSize)?.also { ArtworkCache.put(this, it, maxPixelSize) }
```

- [ ] **Step 4: Add Android thumbnail actual**

In `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.android.kt`, keep `decodeArtwork()` and add imports/actual:

```kotlin
package com.eterocell.rhythhaus

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.decodeArtwork(): ImageBitmap? = try {
    BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap()
} catch (_: Exception) {
    null
}

actual fun ByteArray.decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap? = try {
    val target = maxPixelSize.coerceAtLeast(1)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, target)
    }
    BitmapFactory.decodeByteArray(this, 0, size, options)?.asImageBitmap()
} catch (_: Exception) {
    null
}

private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
    var sampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / sampleSize >= target && halfHeight / sampleSize >= target) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}
```

- [ ] **Step 5: Add Skia thumbnail actual for JVM**

Replace `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.jvm.kt` with:

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.math.roundToInt
import kotlin.math.min
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Image as SkiaImage

actual fun ByteArray.decodeArtwork(): ImageBitmap? = try {
    SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
} catch (_: Exception) {
    null
}

actual fun ByteArray.decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap? = decodeSkiaArtworkThumbnail(maxPixelSize)

private fun ByteArray.decodeSkiaArtworkThumbnail(maxPixelSize: Int): ImageBitmap? = try {
    val source = SkiaImage.makeFromEncoded(this)
    val target = maxPixelSize.coerceAtLeast(1)
    val scale = min(target.toFloat() / source.width, target.toFloat() / source.height).coerceAtMost(1f)
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)
    val surface = Surface.makeRasterN32Premul(width, height)
    surface.canvas.drawImageRect(
        image = source,
        dst = Rect.makeWH(width.toFloat(), height.toFloat()),
        samplingMode = SamplingMode.LINEAR,
        paint = null,
    )
    surface.makeImageSnapshot().toComposeImageBitmap()
} catch (_: Exception) {
    null
}
```

- [ ] **Step 6: Add Skia thumbnail actual for iOS**

Replace `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.ios.kt` with the same code as the JVM file in Step 5, preserving the package name.

- [ ] **Step 7: Verify task GREEN**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both BUILD SUCCESSFUL.

- [ ] **Step 8: Update OpenSpec task evidence and commit**

Update `openspec/changes/track-art-thumbnails/tasks.md` Task 1 checkboxes with evidence lines. Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.kt \
  shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.android.kt \
  shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.jvm.kt \
  shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.ios.kt \
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ArtworkCacheTest.kt \
  openspec/changes/track-art-thumbnails/tasks.md
git commit -m "feat: add artwork thumbnail cache"
```

### Task 2: Use thumbnails in compact artwork surfaces

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- Modify: `openspec/changes/track-art-thumbnails/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: `ByteArray.decodeArtworkThumbnailCached(maxPixelSize: Int = 128)` from Task 1.
- Preserves: `NowPlayingScreen` full-size `decodeArtwork()` call.

- [ ] **Step 1: Route `AlbumMark` through thumbnail cache**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, change `AlbumMark` artwork decode from:

```kotlin
val artworkBitmap = remember(track.artworkBytes) {
    track.artworkBytes?.decodeArtwork()
}
```

to:

```kotlin
val artworkBitmap = remember(track.artworkBytes) {
    track.artworkBytes?.decodeArtworkThumbnailCached()
}
```

Do not change selected-row behavior, fallback text, row shape, or click behavior.

- [ ] **Step 2: Route compact `NowPlayingBar` through thumbnail cache**

In `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`, change:

```kotlin
val artworkBitmap = remember(track?.artworkBytes) {
    track?.artworkBytes?.decodeArtwork()
}
```

to:

```kotlin
val artworkBitmap = remember(track?.artworkBytes) {
    track?.artworkBytes?.decodeArtworkThumbnailCached()
}
```

Do not change bar layout, controls, callbacks, icons, or progress rendering.

- [ ] **Step 3: Confirm intended full decode call sites**

Run:

```bash
rg 'decodeArtwork\(' shared/src/commonMain/kotlin/com/eterocell/rhythhaus shared/src/androidMain/kotlin/com/eterocell/rhythhaus shared/src/jvmMain/kotlin/com/eterocell/rhythhaus shared/src/iosMain/kotlin/com/eterocell/rhythhaus
rg 'decodeArtworkThumbnailCached' shared/src/commonMain/kotlin/com/eterocell/rhythhaus
```

Expected:

- Direct `decodeArtwork()` remains in platform actual decoder implementations and expanded/full artwork surfaces such as `NowPlayingScreen`.
- `AlbumMark` and `NowPlayingBar` use `decodeArtworkThumbnailCached()`.

- [ ] **Step 4: Verify compile/tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both BUILD SUCCESSFUL.

- [ ] **Step 5: Update OpenSpec task evidence and commit**

Update `openspec/changes/track-art-thumbnails/tasks.md` Task 2 checkboxes for the compact-surface routing and focused verification steps completed so far. Commit:

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt \
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt \
  openspec/changes/track-art-thumbnails/tasks.md
git commit -m "fix: use thumbnails for compact artwork"
```

### Task 3: Broad verification, handoff, and final commit

**Files:**
- Modify: `openspec/changes/track-art-thumbnails/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: completed Tasks 1 and 2.
- Produces: validation evidence and handoff record.

- [ ] **Step 1: Validate OpenSpec change**

Run:

```bash
openspec validate track-art-thumbnails --strict
```

Expected: `Change 'track-art-thumbnails' is valid`.

- [ ] **Step 2: Run broad JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: BUILD SUCCESSFUL. If the known `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` flake appears, rerun the targeted test, then rerun the broad command and record exact evidence.

- [ ] **Step 3: Run iOS toolchain and simulator verification**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: Xcode prints a version and iOS simulator tests are BUILD SUCCESSFUL. If Xcode is unavailable, record the exact blocker and do not claim iOS validation passed.

- [ ] **Step 4: Confirm no schema/dependency changes**

Run:

```bash
git diff --stat main...HEAD
git diff --name-only main...HEAD | sort
```

Expected changed implementation files are limited to artwork decoder/common UI/test files plus OpenSpec/Superpowers/progress docs. No `LibraryTrack.sq`, `gradle/libs.versions.toml`, or `shared/build.gradle.kts` changes.

- [ ] **Step 5: Update task evidence and progress handoff**

Update `openspec/changes/track-art-thumbnails/tasks.md` so all completed checkboxes include evidence lines. Add a new top entry in `progress.md` using this shape:

```text
## Handoff - 2026-07-01 track art thumbnails

Route: openspec+superpowers
Owner: implementation
Scope: Add memory-only cached thumbnail artwork decode path for compact track-list and now-playing-bar artwork.
Root cause: TrackRow/AlbumMark and NowPlayingBar decoded full embedded artwork bytes in compact Compose surfaces; lazy-list recomposition could re-trigger full-size image decode while scrolling.
Implementation:
- Added cache-key separation for full-size and thumbnail artwork entries.
- Added platform thumbnail decoders for Android BitmapFactory and Skia-backed JVM/iOS.
- Routed AlbumMark/TrackRow and NowPlayingBar to `decodeArtworkThumbnailCached()`.
- Preserved original artwork bytes and full-size NowPlayingScreen decode.
Verification:
- <commands and results>
Acceptance:
- Requirement matched: yes.
- Scope controlled: yes; no schema/dependency/scanner/playback changes.
- Edge cases/risk reviewed: runtime FPS improvement still needs manual/device profiling; this change removes the source-level full-decode-on-row-composition hotspot.
Changed files:
- <paths>
Next owner: user for manual scroll-performance validation with a large artwork-heavy library.
Blockers: none for automated verification, or exact blocker.
Commit: pending final semantic evidence commit.
```

- [ ] **Step 6: Commit evidence**

Run:

```bash
git add openspec/changes/track-art-thumbnails/tasks.md progress.md
git commit -m "docs: record track artwork thumbnail evidence"
```

- [ ] **Step 7: Final review package**

Before reporting completion, run:

```bash
git status --short
git log --oneline -5
git diff --stat main...HEAD
```

Expected: worktree clean; recent commits include the docs/spec plan commit(s), thumbnail cache implementation, compact artwork routing, and evidence commit. Report exact verification commands and outcomes.
