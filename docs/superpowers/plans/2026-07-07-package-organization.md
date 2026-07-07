# Package Organization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move RhythHaus shared Compose/UI components into focused Kotlin packages while preserving behavior and keeping `App()` stable at the root package.

**Architecture:** Use a staged hybrid feature-first package refactor. The root package keeps the app entry point, existing `library` infrastructure remains unchanged, library feature UI moves to `library.ui`, feature screens move to `nowplaying`, `search`, and `settings`, reusable helpers move to `ui`/`theme`, and playback/model moves happen only if safe after lower-risk stages compile.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix, NavigationEvent, SQLDelight, Kotlin expect/actual source sets.

## Global Constraints

- Preserve `com.eterocell.rhythhaus.App` as the shared root app entry point.
- No visual redesign or screen behavior changes.
- No route, adaptive, route animation, predictive/system back, bottom-bar, Now Playing overlay, clear-library, scan/import, playback, theme, strings, content descriptions, Miuix/blur, or platform seam behavior changes.
- No scanner, repository, database, playback engine, TagLib, source-access, artwork-cache, native navigation, dependency, toolchain, resource, or Windows/Linux scope changes.
- Keep existing non-UI library infrastructure under `com.eterocell.rhythhaus.library`.
- Move expect/actual files only when every corresponding common/Android/JVM/iOS declaration can be moved to the same package in the same task.
- High-risk playback/model package moves may be deferred if they would cause disproportionate platform or Swift-facing churn; record deferrals in task report and final evidence.
- Use file moves/package declarations/import updates only; do not rewrite component bodies except for import qualification required by package moves.
- Run the verification commands listed in each task and report exact outputs.

---

## File Structure

Target package layout:

```text
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/
  App.kt
  library/                 # existing scanner/repository/source-access/persistence stays here
  library/ui/              # library shell/routes/state/navigation/content/chrome/dialog/rows
  nowplaying/              # NowPlayingScreen, NowPlayingBar
  search/                  # SearchScreen
  settings/                # SettingsScreen
  ui/                      # shared Compose UI helpers/gestures/artwork decode if moved
  theme/                   # theme palette/theme preferences expect/actual
  playback/                # playback abstractions/engines only if moved safely
  model/                   # common models/metadata/import labels only if moved safely
```

---

### Task 1: Move library UI/navigation into `library.ui`

**Files:**
- Move:
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppState.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryHomeContent.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDetailContent.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryChrome.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDialogs.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRows.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt`
  - `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt` -> `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
  - `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt` -> `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowserTest.kt`
- Modify:
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
  - any root/common test imports required by compilation
- Report: `.superpowers/sdd/package-organization/task-1-report.md`

**Interfaces:**
- Produces public/importable `com.eterocell.rhythhaus.library.ui.LibraryHomeScreen` consumed by root `App.kt`.
- Produces package `com.eterocell.rhythhaus.library.ui` for all library route/navigation/content components.
- Keeps root package model/playback/theme/shared UI helpers unchanged for later tasks.

- [ ] **Step 1: Inspect current library UI references**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
symbols = ['LibraryHomeScreen','LibraryRoute','LibraryNavigationStack','LibrarySnapshot','BrowseMode','LibraryScrollPosition','LibraryAdaptiveLayoutMode','LibraryHomeContent','DrillDownView']
root = Path('shared/src')
for sym in symbols:
    print(f'## {sym}')
    for p in root.rglob('*.kt'):
        text = p.read_text()
        if sym in text:
            print(p)
PY
```

Expected: references are mostly in root-package common files and common tests.

- [ ] **Step 2: Move files with `mkdir`/`git mv`**

Run:

```bash
mkdir -p shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui
mkdir -p shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppState.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryHomeContent.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDetailContent.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryChrome.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDialogs.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRows.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt
git mv shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt
git mv shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowserTest.kt
```

- [ ] **Step 3: Rewrite moved package declarations**

Set the first line in all moved common/test files to:

```kotlin
package com.eterocell.rhythhaus.library.ui
```

Use a scripted replace, then inspect the first line of each moved file.

- [ ] **Step 4: Add imports for root-package symbols used by moved files**

Because Task 1 moves library UI only, moved files must import root package symbols they used implicitly before. Add only imports required by compilation. Commonly needed imports include:

```kotlin
import com.eterocell.rhythhaus.BackChip
import com.eterocell.rhythhaus.HausColors
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.RhythHausThemeMode
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.decodeArtworkCached
import com.eterocell.rhythhaus.decodeArtworkThumbnailCached
import com.eterocell.rhythhaus.formatDuration
import com.eterocell.rhythhaus.hausClickable
import com.eterocell.rhythhaus.leftEdgeSwipeBack
import com.eterocell.rhythhaus.recordRhythHausBackdrop
import com.eterocell.rhythhaus.rememberRhythHausBackdrop
import com.eterocell.rhythhaus.rhythHausLiquidGlass
import com.eterocell.rhythhaus.toPlayableTrack
```

Do not add wildcard imports from the project package; prefer exact imports.

- [ ] **Step 5: Update root consumers**

In `App.kt`, import the moved shell:

```kotlin
import com.eterocell.rhythhaus.library.ui.LibraryHomeScreen
```

Update any remaining root-package tests or common files that reference moved library UI/navigation symbols.

- [ ] **Step 6: Run focused tests and compile**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
git diff --check -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus shared/src/commonTest/kotlin/com/eterocell/rhythhaus
```

Expected: all commands pass.

- [ ] **Step 7: Write report**

Write `.superpowers/sdd/package-organization/task-1-report.md` with:

- files moved/modified;
- exact verification command outputs;
- any imports made public/internal due to package move;
- confirmation that root `App()` remained in `com.eterocell.rhythhaus`;
- concerns or none.

---

### Task 2: Move Now Playing, Search, and Settings feature UI packages

**Files:**
- Move:
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify:
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
  - `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt` if `EqualizerStrip` imports change
  - tests only if required by imports
- Report: `.superpowers/sdd/package-organization/task-2-report.md`

**Interfaces:**
- Produces `com.eterocell.rhythhaus.nowplaying.NowPlayingScreen` and `NowPlayingBar` consumed by `library.ui.LibraryAppShell` and/or `LibraryDetailContent`.
- Produces `com.eterocell.rhythhaus.search.SearchScreen` and `com.eterocell.rhythhaus.settings.SettingsScreen` consumed by `library.ui.LibraryRoutes`.
- Keeps playback/model/shared UI root package unchanged until later tasks.

- [ ] **Step 1: Inspect current feature UI references**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
for sym in ['NowPlayingScreen','NowPlayingBar','SearchScreen','SettingsScreen','EqualizerStrip']:
    print(f'## {sym}')
    for p in Path('shared/src').rglob('*.kt'):
        if sym in p.read_text():
            print(p)
PY
```

- [ ] **Step 2: Move feature files**

Run:

```bash
mkdir -p shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying
mkdir -p shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search
mkdir -p shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt
```

- [ ] **Step 3: Rewrite package declarations**

Use package declarations:

```kotlin
package com.eterocell.rhythhaus.nowplaying
package com.eterocell.rhythhaus.search
package com.eterocell.rhythhaus.settings
```

- [ ] **Step 4: Add exact imports and update consumers**

Feature files will need exact imports from root or `library.ui`. Examples:

```kotlin
import com.eterocell.rhythhaus.BackChip
import com.eterocell.rhythhaus.HausColors
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.decodeArtwork
import com.eterocell.rhythhaus.formatDuration
import com.eterocell.rhythhaus.leftEdgeSwipeBack
import com.eterocell.rhythhaus.toPlayableTrack
import com.eterocell.rhythhaus.library.ui.EqualizerStrip
import com.eterocell.rhythhaus.library.ui.LibraryScrollPosition
```

In `library.ui` consumers, add imports for moved feature screens/bars:

```kotlin
import com.eterocell.rhythhaus.nowplaying.NowPlayingBar
import com.eterocell.rhythhaus.nowplaying.NowPlayingScreen
import com.eterocell.rhythhaus.search.SearchScreen
import com.eterocell.rhythhaus.settings.SettingsScreen
```

- [ ] **Step 5: Run verification**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
git diff --check -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus shared/src/commonTest/kotlin/com/eterocell/rhythhaus
```

Expected: all commands pass.

- [ ] **Step 6: Write report**

Write `.superpowers/sdd/package-organization/task-2-report.md` with moved files, verification output, behavior-preservation notes, and concerns.

---

### Task 3: Move reusable UI and theme helpers

**Files:**
- Move common UI helpers:
  - `BackChip.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/BackChip.kt`
  - `HausClickable.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausClickable.kt`
  - `SwipeBackGesture.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt`
  - `VerticalSheetGesture.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt`
  - `MusicProgressScrubber.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`
  - `LiquidGlassChrome.kt` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt`
  - `ArtworkDecoder.kt` plus Android/JVM/iOS actuals -> `com.eterocell.rhythhaus.ui`
- Move theme files:
  - `HausColors.kt`, `Theme.kt`, `ThemePreferenceStore.kt` plus Android/JVM/iOS actuals -> `com.eterocell.rhythhaus.theme`
  - `ThemeTest.kt` -> `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/theme/ThemeTest.kt`
  - `ArtworkCacheTest.kt`, `MusicProgressScrubberTest.kt`, `BottomBarModeTest.kt` if package-private access requires move to `ui`
- Modify moved feature/library files imports.
- Report: `.superpowers/sdd/package-organization/task-3-report.md`

**Interfaces:**
- Produces reusable UI helpers under `com.eterocell.rhythhaus.ui`.
- Produces theme API under `com.eterocell.rhythhaus.theme`.
- Preserves expect/actual matching for `decodeArtwork*`, theme preference store, and platform theme behavior.

- [ ] **Step 1: Inspect expect/actual declarations**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
for sym in ['expect fun decodeArtwork', 'actual fun decodeArtwork', 'expect fun createThemePreferenceStore', 'actual fun createThemePreferenceStore', 'expect fun systemPrefersDarkTheme', 'actual fun systemPrefersDarkTheme']:
    print(f'## {sym}')
    for p in Path('shared/src').rglob('*.kt'):
        text=p.read_text()
        if sym in text:
            print(p)
PY
```

Expected: all corresponding actuals are identified before moving.

- [ ] **Step 2: Move UI helper files and matching tests**

Run `git mv` for the listed UI files. Move tests only if they access package-private symbols that must remain package-private. Prefer preserving `internal` API and exact imports over broad visibility changes.

- [ ] **Step 3: Move theme files and actuals together**

Move common and platform files in one batch:

```bash
mkdir -p shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme
mkdir -p shared/src/androidMain/kotlin/com/eterocell/rhythhaus/theme
mkdir -p shared/src/iosMain/kotlin/com/eterocell/rhythhaus/theme
mkdir -p shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/theme
mkdir -p shared/src/commonTest/kotlin/com/eterocell/rhythhaus/theme
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausColors.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/HausColors.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Theme.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/Theme.kt
git mv shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.kt
git mv shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.android.kt shared/src/androidMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.android.kt
git mv shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.ios.kt shared/src/iosMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.ios.kt
git mv shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.jvm.kt shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.jvm.kt
git mv shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ThemeTest.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/theme/ThemeTest.kt
```

- [ ] **Step 4: Rewrite package declarations and imports**

Use:

```kotlin
package com.eterocell.rhythhaus.ui
package com.eterocell.rhythhaus.theme
```

Update all consumers with exact imports such as:

```kotlin
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.theme.createThemePreferenceStore
import com.eterocell.rhythhaus.theme.resolveHausPalette
import com.eterocell.rhythhaus.theme.systemPrefersDarkTheme
import com.eterocell.rhythhaus.ui.BackChip
import com.eterocell.rhythhaus.ui.decodeArtwork
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.ui.leftEdgeSwipeBack
import com.eterocell.rhythhaus.ui.recordRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rememberRhythHausBackdrop
import com.eterocell.rhythhaus.ui.rhythHausLiquidGlass
```

- [ ] **Step 5: Run cross-source-set verification**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.theme.ThemeTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:compileAndroidMain --configuration-cache
./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache
git diff --check -- shared/src
```

Expected: all commands pass.

- [ ] **Step 6: Write report**

Write `.superpowers/sdd/package-organization/task-3-report.md` with files moved, expect/actual alignment evidence, verification outputs, and concerns.

---

### Task 4: Assess and move playback/model files only where safe

**Files:**
- Candidate move to `playback`:
  - `Playback.kt`
  - `PlaybackEngine.android.kt`, `PlaybackEngine.ios.kt`, `PlaybackEngine.jvm.kt`
  - `PlaybackDispatchers.android.kt`, `PlaybackDispatchers.ios.kt`, `PlaybackDispatchers.jvm.kt`
  - `RhythHausPlaybackService.kt`, `RhythHausTransportBridge.kt`
  - `NowPlayingArtworkBridge.kt` only if it is part of playback/media bridge move and iOS tests still pass
- Candidate move to `model`:
  - `MusicModels.kt`
  - `AudioMetadata.kt`, `AudioMetadata.android.kt`, `AudioMetadata.ios.kt`, `AudioMetadata.jvm.kt`
  - `ImportLabels.kt`, `ImportLabels.android.kt`, `ImportLabels.ios.kt`, `ImportLabels.jvm.kt`
- Tests possibly affected:
  - `PlaybackControllerTest.kt`
  - `BottomBarModeTest.kt` if not already moved
  - iOS Now Playing tests
  - scanner tests if metadata package moves
- Report: `.superpowers/sdd/package-organization/task-4-report.md`

**Interfaces:**
- May produce `com.eterocell.rhythhaus.playback` and `com.eterocell.rhythhaus.model`.
- May also intentionally defer some or all moves if blast radius is too high.
- Must preserve root `App()` and all platform compilation.

- [ ] **Step 1: Generate blast-radius map**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
candidates = ['PlaybackController','PlaybackState','PlaybackStatus','PlaybackEngine','PlayableTrack','Track','AudioMetadata','ImportLabels','NowPlayingArtwork']
for sym in candidates:
    print(f'## {sym}')
    for p in Path('shared/src').rglob('*.kt'):
        if sym in p.read_text():
            print(p)
PY
```

- [ ] **Step 2: Decide move vs defer**

Move only files whose expect/actual/platform references are manageable within this task. Defer any move that would require product behavior changes, Swift-facing API churn, or broad platform bridge rewrites.

Record decisions in the report before code changes.

- [ ] **Step 3: If moving playback, move all matching platform files together**

Create `playback` directories in common, Android, iOS, JVM source sets. Use `git mv` and update package declarations to:

```kotlin
package com.eterocell.rhythhaus.playback
```

Update all consumers with exact imports. Keep Android manifest/service references valid if service class package changes; if that is too risky, defer Android service/bridge package moves.

- [ ] **Step 4: If moving model files, move all matching expect/actual files together**

Create `model` directories in common, Android, iOS, JVM source sets. Use package:

```kotlin
package com.eterocell.rhythhaus.model
```

Update library scanner, UI, playback, and tests with exact imports.

- [ ] **Step 5: Run verification**

Run at minimum:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:compileAndroidMain --configuration-cache
./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache
git diff --check -- shared/src androidApp/src
```

If test package names changed, run the package-correct names. If moves are deferred, still run `:shared:compileKotlinJvm` and focused tests affected by imports.

- [ ] **Step 6: Write report**

Write `.superpowers/sdd/package-organization/task-4-report.md` with:

- moved files;
- deferred files and exact reason;
- platform manifest/Swift/API considerations;
- verification outputs;
- concerns.

---

### Task 5: Final verification and evidence

**Files:**
- Modify:
  - `openspec/changes/package-organization/tasks.md`
  - `progress.md`
- Report: `.superpowers/sdd/package-organization/task-5-report.md`

**Interfaces:**
- Produces completed OpenSpec tasks and progress handoff evidence.
- Produces no behavior code changes unless final verification finds a package/import defect.

- [ ] **Step 1: Verify package layout**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
for base in ['shared/src/commonMain/kotlin/com/eterocell/rhythhaus','shared/src/commonTest/kotlin/com/eterocell/rhythhaus','shared/src/androidMain/kotlin/com/eterocell/rhythhaus','shared/src/iosMain/kotlin/com/eterocell/rhythhaus','shared/src/jvmMain/kotlin/com/eterocell/rhythhaus']:
    print('\n#', base)
    for p in sorted(Path(base).rglob('*.kt')):
        package = next((line for line in p.read_text().splitlines() if line.startswith('package ')), '<no package>')
        print(f'{p.relative_to(base)} -> {package}')
PY
```

Check that:

- root common package has `App.kt` and intentionally deferred files only;
- `library/ui` contains library UI/navigation files;
- `nowplaying`, `search`, `settings`, `ui`, and `theme` contain the moved files required by completed tasks;
- any playback/model deferrals are documented.

- [ ] **Step 2: Run OpenSpec validation**

Run:

```bash
openspec validate package-organization --strict
```

Expected: `Change 'package-organization' is valid`.

- [ ] **Step 3: Run package-correct focused tests**

Run package-correct focused tests, adjusting names if Task 4 moved playback/model tests:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Also run any moved helper/theme/playback tests identified by earlier task reports.

- [ ] **Step 4: Run broad verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
git diff --check
```

Expected: all commands pass. Existing unrelated warnings may be recorded but not treated as blockers if builds succeed.

- [ ] **Step 5: Complete OpenSpec tasks and progress handoff**

Mark every completed checkbox in `openspec/changes/package-organization/tasks.md`.

Append a `progress.md` handoff using the AGENTS.md format:

```text
## Handoff - 2026-07-07 package organization

Route: openspec+superpowers
Owner: implementation
Input: package-organization spec/plan
Output: <package refactor summary>
Verification:
- `<command>`: pass (...)
Changed files:
- `<path>`
Next owner: user for manual smoke validation; OpenSpec/user for archive when satisfied.
Blockers: <none or exact blocker/deferral>
Commit: pending.
```

- [ ] **Step 6: Write report**

Write `.superpowers/sdd/package-organization/task-5-report.md` summarizing final package layout, verification, blockers/deferrals, and changed files.
