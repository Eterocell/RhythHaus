# Vector Icon Controls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace emoji/text control glyphs with cohesive Material vector icons in the mini-player and full now-playing transport controls.

**Architecture:** Add the Compose Material Icons dependency to shared commonMain, then render icons through Compose `Icon` using Material `Icons.Filled.*` image vectors. Keep existing button containers, colors, sizes, and callbacks.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Material Icons image vectors, Miuix surfaces/text, OpenSpec.

## Global Constraints

- Use vector icons for play, pause, previous, next, search, and settings controls.
- Do not render `▶`, `⏸`, `⏮`, `⏭`, `🔍`, or `⚙️` as user-visible control text in `NowPlayingBar.kt` or `NowPlayingScreen.kt`.
- Preserve existing click behavior, playback behavior, navigation behavior, layout, theme colors, and platform scope.
- Album artwork fallback initials/music-note text are out of scope unless they are inside a transport/search/settings control.

---

### Task 1: Replace emoji controls with Material vector icons

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Modify: `openspec/changes/replace-emoji-controls-with-icons/tasks.md`
- Modify: `progress.md`
- Create: `.superpowers/sdd/replace-emoji-controls-with-icons-report.md`

**Interfaces:**
- Consumes: existing `NowPlayingBar(...)` and `NowPlayingScreen(...)` signatures and callbacks.
- Produces: same public composable signatures and behavior, with vector icons replacing control glyph text.

- [ ] **Step 1: Add Material Icons dependency**

In `gradle/libs.versions.toml`, add a library alias:

```toml
compose-material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "compose-multiplatform" }
```

In `shared/build.gradle.kts` commonMain dependencies, add:

```kotlin
implementation(libs.compose.material.icons.extended)
```

- [ ] **Step 2: Replace NowPlayingBar control glyphs**

In `NowPlayingBar.kt`, import:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
```

Replace the play/pause `Text` inside the 36.dp button with `Icon(...)` using `Icons.Filled.Pause` when playing and `Icons.Filled.PlayArrow` otherwise. For `track == null`, use `Icons.Filled.PlayArrow` with the existing paper tint; do not keep the `♪` text inside the control.

Replace search/settings `Text` with `Icon(...)` using `Icons.Filled.Search` and `Icons.Filled.Settings`, tinting with `HausColors.current.ink` and content descriptions `Search` / `Settings`.

- [ ] **Step 3: Replace NowPlayingScreen transport glyphs**

In `NowPlayingScreen.kt`, import:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
```

Replace previous/play-pause/next `Text` controls with `Icon(...)` using `Icons.Filled.SkipPrevious`, `Icons.Filled.Pause` or `Icons.Filled.PlayArrow`, and `Icons.Filled.SkipNext`. Preserve existing button sizes/backgrounds/click lambdas and use existing colors as icon tint.

- [ ] **Step 4: Search for removed glyphs**

Run:

```bash
rg '▶|⏸|⏮|⏭|🔍|⚙️' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
```

Expected: no matches.

- [ ] **Step 5: Run validation and builds**

Run:

```bash
openspec validate replace-emoji-controls-with-icons --strict
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: OpenSpec valid and Gradle `BUILD SUCCESSFUL`. If a known flaky playback test fails, rerun the targeted test and then rerun broad verification; record exact outcomes.

- [ ] **Step 6: Update evidence and commit**

Update `openspec/changes/replace-emoji-controls-with-icons/tasks.md` and `progress.md` with evidence. Write `.superpowers/sdd/replace-emoji-controls-with-icons-report.md`.

Commit:

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt openspec/changes/replace-emoji-controls-with-icons docs/superpowers/specs/2026-07-01-vector-icon-controls.md docs/superpowers/plans/2026-07-01-vector-icon-controls.md progress.md .superpowers/sdd/replace-emoji-controls-with-icons-report.md
git commit -m "fix: replace emoji controls with vector icons"
```
