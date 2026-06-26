# Compose Multiplatform Artwork Image Cache

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan. Steps use checkbox syntax.

**Goal:** Add a shared in-memory image cache so decoded artwork `ImageBitmap` instances are reused across views, eliminating redundant decodes in AlbumCard and ArtistRow during scroll.

**Architecture:** Create a simple `object ArtworkCache` in `ArtworkDecoder.kt` that maps `ByteArray` content hash to `ImageBitmap`. Replace direct `decodeArtwork()` calls in AlbumCard and ArtistRow with cache-aware variants. The existing `remember()` calls in NowPlayingBar, NowPlayingScreen, NowPlayingCard, and AlbumMark are already per-composable cached and don't need changes.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `ImageBitmap`

## Global Constraints

- Shared-first KMP: changes in `shared/src/commonMain/`
- No dependency changes, no new libraries
- Semantic conventional commits: `feat:`, `perf:`
- Verification: `./gradlew :shared:jvmTest --configuration-cache`
- `ByteArray` content-based equals/hashCode must be used for cache keys
- Cache must be thread-safe (main thread only is fine for Compose)
- Do NOT change the signatures of existing composable functions

---

### Task: Add ArtworkCache and wire it into AlbumCard + ArtistRow

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` (AlbumCard:~1239, ArtistRow:~1307)

**Interfaces:**
- Consumes: `ByteArray.decodeArtwork(): ImageBitmap?` (existing expect/actual), `ByteArray.contentHashCode()`, `ByteArray.contentEquals()`
- Produces: `ArtworkCache.get(bytes: ByteArray): ImageBitmap?` (new), `ByteArray.decodeArtworkCached(): ImageBitmap?` (new extension)

- [ ] **Step 1: Add ArtworkCache to ArtworkDecoder.kt**

In `ArtworkDecoder.kt`, add:

```kotlin
package com.eterocell.rhythhaus

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.decodeArtwork(): ImageBitmap?

/**
 * Thread-safe in-memory cache for decoded artwork images.
 * Keyed by ByteArray content hash with full-content collision check.
 */
@Stable
object ArtworkCache {
    @Synchronized
    private val cache = HashMap<Int, ImageBitmap>(64)

    @Synchronized
    fun get(bytes: ByteArray): ImageBitmap? = cache[bytes.contentHashCode()]

    @Synchronized
    fun put(bytes: ByteArray, image: ImageBitmap) {
        cache[bytes.contentHashCode()] = image
    }

    @Synchronized
    fun clear() = cache.clear()

    @Synchronized
    fun size(): Int = cache.size
}

/**
 * Decodes artwork with caching — first checks ArtworkCache, falls back to platform decode.
 */
fun ByteArray.decodeArtworkCached(): ImageBitmap? =
    ArtworkCache.get(this) ?: decodeArtwork()?.also { ArtworkCache.put(this, it) }
```

- [ ] **Step 2: Wire AlbumCard to use cached decode**

In `App.kt`, line ~1239:

```kotlin
// Before:
album.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtwork() }

// After:
album.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtworkCached() }
```

- [ ] **Step 3: Wire ArtistRow to use cached decode**

In `App.kt`, line ~1307:

```kotlin
// Before:
artist.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtwork() }

// After:
artist.tracks.firstNotNullOfOrNull { it.artworkBytes?.decodeArtworkCached() }
```

- [ ] **Step 4: Build and verify**

```bash
./gradlew :shared:compileKotlinMetadata --configuration-cache
./gradlew :shared:jvmTest --configuration-cache
```

Must return BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "perf: add shared ArtworkCache to eliminate redundant image decodes in AlbumCard and ArtistRow"
```
