# Miuix Theme Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Material3 Theme with Miuix Theme in RhythHaus App.kt

**Architecture:** Add `miuix-ui` dependency, swap `MaterialTheme` → `MiuixTheme`, replace Material3 component imports with Miuix equivalents (`Card`, `Button`, `Slider`, `Surface`). Map existing custom colors through `lightColorScheme()` builder.

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, Miuix v0.9.2

## Global Constraints

- Compose Multiplatform 1.11.1 (already in libs.versions.toml)
- Kotlin 2.4.0 (already in libs.versions.toml)
- Miuix v0.9.2 — latest release as of 2026-06-23
- Module: `miuix-ui` only (core theme + basic components)
- No dark mode, no ThemeController, no Monet — simple `lightColorScheme()` with custom colors
- Keep `material3` dependency initially, remove after verification passes
- Only touch `App.kt`, `libs.versions.toml`, `shared/build.gradle.kts`

## API Differences

| Feature | Material3 | Miuix |
|---------|-----------|-------|
| Theme | `MaterialTheme(colorScheme = ...)` | `MiuixTheme(colors = ...)` |
| Color scheme | `materialColorScheme(primary=..., background=...)` | `lightColorScheme(primary=..., background=...)` |
| Button colors | `buttonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)` | `buttonColors(color, contentColor, disabledColor, disabledContentColor)` |
| Button shape | `shape = RoundedCornerShape(N.dp)` | `cornerRadius = N.dp` |
| Card colors | `cardColors(containerColor = ...)` | `defaultColors(color = ...)` |
| Card shape | `shape = RoundedCornerShape(N.dp)` | `cornerRadius = N.dp` |
| Card elevation | `elevation = CardDefaults.cardElevation(...)` | Not supported — remove |
| Surface | `Surface(modifier, color, shape, ...)` | Same signature, different import |
| Slider | `Slider(value, onValueChange, modifier)` | Same signature, different import |

- `colors: ButtonColors` which has `color`, `contentColor`, `disabledColor`, `disabledContentColor` instead of `containerColor`, `contentColor`, `disabledContainerColor`

---

### Task 1: Add miuix-ui dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts:93-98`

**Produces:** `miuix-ui` available in `commonMain`

- [ ] **Step 1: Add version and library to libs.versions.toml**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
miuix = "0.9.2"
```

In `[libraries]`, add (alphabetically among existing entries):

```toml
miuix-ui = { module = "top.yukonga.miuix.kmp:miuix-ui", version.ref = "miuix" }
```

- [ ] **Step 2: Add dependency to shared/build.gradle.kts**

In `shared/build.gradle.kts`, in `commonMain.dependencies` block (after `implementation(projects.taglib)`), add:

```kotlin
implementation(libs.miuix.ui)
```

- [ ] **Step 3: Test dependency resolution**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: MiuiX JARs downloaded and compiled (current code still uses Material3 — no errors from new dep alone).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "build: add miuix-ui v0.9.2 dependency"
```

---

### Task 2: Replace MaterialTheme with MiuixTheme

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:27-33,131-143`

**Produces:** `MiuixTheme` wrapping the app with custom colors

- [ ] **Step 1: Add Miuix imports, keep Material3 for now**

In `App.kt` imports section, add after the existing M3 imports:

```kotlin
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
```

Replace the existing `import androidx.compose.material3.MaterialTheme` with the Miuix import (we'll keep other M3 imports for now).

- [ ] **Step 2: Rewrite RhythHausTheme**

Replace:

```kotlin
private fun RhythHausTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = HausPaper,
            surface = HausPanel,
            primary = HausInk,
            secondary = HausPulse,
            onBackground = HausInk,
            onSurface = HausInk,
        ),
        content = content,
    )
}
```

With:

```kotlin
private fun RhythHausTheme(content: @Composable () -> Unit) {
    MiuixTheme(
        colors = lightColorScheme(
            primary = HausInk,
            onPrimary = HausPaper,
            secondary = HausPulse,
            onSecondary = HausPaper,
            background = HausPaper,
            onBackground = HausInk,
            surface = HausPanel,
            onSurface = HausInk,
            surfaceContainer = HausPanel,
            onSurfaceContainer = HausInk,
            secondaryVariant = HausPulse,
            onSecondaryVariant = HausPaper,
            disabledSecondaryVariant = HausPulse.copy(alpha = 0.28f),
            disabledOnSecondaryVariant = HausPaper.copy(alpha = 0.28f),
        ),
        content = content,
    )
}
```

Note: `lightColorScheme()` provides defaults for all ~60 Colors fields — we only set the ones we need.

- [ ] **Step 3: Verify theme compiles**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: SUCCESS. Material3 components still compile under MiuixTheme (both are Compose theme providers).

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: replace MaterialTheme with MiuixTheme"
```

---

### Task 3: Replace Material3 components with Miuix equivalents

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` (all component import + usage sites)

**Consumes:** MiuixTheme from Task 2, miuix-ui dep from Task 1

**Produces:** Zero Material3 imports in App.kt

- [ ] **Step 1: Replace Surface import + usage**

Remove: `import androidx.compose.material3.Surface`
(Miuix Surface was added in Task 2 imports)

The usage `Surface(modifier = modifier.fillMaxSize(), color = HausPaper)` is API-compatible with Miuix Surface — no usage change needed.

- [ ] **Step 2: Replace ImportAudioCard — Card**

Remove: `import androidx.compose.material3.Card` and `import androidx.compose.material3.CardDefaults`

In `ImportAudioCard` (line ~284), replace:

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = HausPanel),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
) {
```

With:

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 24.dp,
    colors = CardDefaults.defaultColors(color = HausPanel),
) {
```

- [ ] **Step 3: Replace NowPlayingCard — Card**

In `NowPlayingCard` (line ~352), replace:

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(32.dp),
    colors = CardDefaults.cardColors(containerColor = HausInk),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
) {
```

With:

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 32.dp,
    colors = CardDefaults.defaultColors(color = HausInk),
) {
```

- [ ] **Step 4: Replace DeveloperPanel Card**

In `DeveloperPanel`/`DeveloperMetadataRow` (line ~502), replace:

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = HausPanelStrong),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
) {
```

With:

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    cornerRadius = 16.dp,
    colors = CardDefaults.defaultColors(color = HausPanelStrong),
) {
```

- [ ] **Step 5: Replace ImportAudioCard Button**

Remove: `import androidx.compose.material3.Button` and `import androidx.compose.material3.ButtonDefaults`

In `ImportAudioCard` (line ~311), replace:

```kotlin
Button(
    onClick = importLauncher::launch,
    enabled = importLauncher.isAvailable,
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .semantics { contentDescription = "Import local audio files" },
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = HausInk,
        contentColor = HausPaper,
        disabledContainerColor = HausMuted.copy(alpha = 0.28f),
        disabledContentColor = HausPaper.copy(alpha = 0.28f),
    ),
) {
```

With:

```kotlin
Button(
    onClick = importLauncher::launch,
    enabled = importLauncher.isAvailable,
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .semantics { contentDescription = "Import local audio files" },
    cornerRadius = 16.dp,
    colors = ButtonDefaults.buttonColors(
        color = HausInk,
        contentColor = HausPaper,
        disabledColor = HausMuted.copy(alpha = 0.28f),
        disabledContentColor = HausPaper.copy(alpha = 0.28f),
    ),
) {
```

- [ ] **Step 6: Replace NowPlayingCard Buttons (play/pause + import)**

In `NowPlayingCard` (line ~437), two Button instances. Replace each one:

First button (play/pause):

```kotlin
Button(
    onClick = onPlayPause,
    modifier = Modifier
        .weight(1f)
        .semantics { contentDescription = ... },
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = ...,
        contentColor = ...,
        disabledContainerColor = ...,
        disabledContentColor = ...,
    ),
) {
```

Replace with:

```kotlin
Button(
    onClick = onPlayPause,
    modifier = Modifier
        .weight(1f)
        .semantics { contentDescription = ... },
    cornerRadius = 16.dp,
    colors = ButtonDefaults.buttonColors(
        color = ...,
        contentColor = ...,
        disabledColor = ...,
        disabledContentColor = ...,
    ),
) {
```

Second button (import/seek) — same pattern: `shape` → `cornerRadius`, `containerColor` → `color`, `disabledContainerColor` → `disabledColor`.

- [ ] **Step 7: Replace Slider**

Remove: `import androidx.compose.material3.Slider`

In `NowPlayingCard` (line ~422), the Slider is:

```kotlin
Slider(
    value = playbackState.progressFraction,
    onValueChange = onSeekFraction,
    modifier = Modifier.semantics { contentDescription = "Playback seek position" },
)
```

Miuix Slider has the same signature for `value`, `onValueChange`, `modifier` — no usage change needed.

- [ ] **Step 8: Remove unused Material3 imports**

Remove any remaining `import androidx.compose.material3.*` lines. If `Text` is still imported from Material3, remove that import — `Text` is from `androidx.compose.foundation.text`, not Material3.

The only remaining Material3 references should be none.

- [ ] **Step 9: Build verification**

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: SUCCESS, no compile errors. All components migrated.

- [ ] **Step 10: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
git commit -m "feat: replace Material3 components with Miuix equivalents"
```

---

### Task 4: Remove Material3 dependency and full verification

**Files:**
- Modify: `shared/build.gradle.kts:97` (remove `libs.compose.material3`)

**Produces:** Clean build with zero Material3 dependencies

- [ ] **Step 1: Remove material3 from deps**

In `shared/build.gradle.kts`, in `commonMain.dependencies`, remove line 97:

```kotlin
implementation(libs.compose.material3)
```

- [ ] **Step 2: Full harness verification**

```bash
./init.sh
```

Expected: BUILD SUCCESSFUL — shared JVM tests, iOS simulator tests, desktop compile, Android debug build all pass.

- [ ] **Step 3: Commit**

```bash
git add shared/build.gradle.kts
git commit -m "refactor: remove material3 dependency after miuix migration"
```
