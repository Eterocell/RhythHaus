# Miuix TopAppBar Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace ordinary custom RhythHaus back/title bars with a Miuix `SmallTopAppBar` wrapper while preserving existing screen behavior and product-specific chrome.

**Architecture:** Introduce one shared `RhythHausTopAppBar` wrapper in shared UI, then migrate Search, Settings, and Library drill-down header usages in narrow slices. Nested-scroll glass chrome, Now Playing surfaces, artwork/equalizer UI, route stack, and platform code stay unchanged.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.11.1, Miuix 0.9.3 `miuix-ui`, existing Compose Material icons, OpenSpec, Gradle configuration cache.

## Global Constraints

- Use Miuix `SmallTopAppBar` or `TopAppBar` for ordinary page-level back/title chrome when behavior is preserved.
- Prefer `SmallTopAppBar` for this change because the current targets are compact bars.
- Preserve Settings content, appearance dropdown, scan controls, import message, and clear-library behavior.
- Preserve Search focus requester, query state, placeholder, clear action, filtering, result selection, now-playing highlight, equalizer, and dismiss behavior.
- Preserve Library drill-down large page title, track rows, selected state, section label, left-edge swipe back, scroll reporting, nested-scroll glass chrome, and Now Playing bar behavior.
- Do not migrate nested-scroll glass chrome, Now Playing screen/bar, artwork/equalizer visuals, scrubber gestures, adaptive shell, or blur/backdrop wrappers.
- Do not add new dependencies. `miuix-ui` is already available; `miuix-navigation3-adaptive` must remain absent.
- The pre-existing modified file `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` is outside this change and must not be touched.
- Update `openspec/changes/miuix-top-app-bar/tasks.md` with exact verification evidence after each task.

---

## File Structure

- Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt`: shared wrapper around Miuix `SmallTopAppBar` and Miuix `IconButton` with localized back icon.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`: replace custom `BackChip` + Search title row with `RhythHausTopAppBar`.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`: replace custom `BackChip` + Settings title row with `RhythHausTopAppBar`.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`: replace only `DrillDownHeader`'s `BackChip` + subtitle row with `RhythHausTopAppBar`; keep large title.
- Modify `openspec/changes/miuix-top-app-bar/tasks.md`: record task completion evidence.
- Modify `.superpowers/sdd/miuix-top-app-bar/*.md`: task briefs/reports/review packages.
- Modify `progress.md` and `roadmap.md` only during final verification/handoff.

### Real Miuix API confirmed from 0.9.3 source jar

```kotlin
@Composable
fun SmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
    bottomContent: @Composable () -> Unit = {},
)
```

## Task 1: Create Shared `RhythHausTopAppBar`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt`
- Modify: `openspec/changes/miuix-top-app-bar/tasks.md`

**Interfaces:**
- Produces:
```kotlin
@Composable
fun RhythHausTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
)
```
- Consumes: `HausColors.current`, `Res.string.back`, Miuix `SmallTopAppBar`, Miuix `IconButton`, existing Compose Material icons dependency.

- [ ] **Step 1: Inspect available arrow icon imports**

Run:
```bash
rg "Icons\.AutoMirrored|Icons\.Filled\.ArrowBack|material.icons" shared/src/commonMain/kotlin shared/build.gradle.kts
```
Expected: existing Material icons are already available through current dependencies. If `Icons.AutoMirrored.Filled.ArrowBack` does not compile later, use `Icons.Filled.ArrowBack` instead.

- [ ] **Step 2: Add the wrapper file**

Create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt`:

```kotlin
package com.eterocell.rhythhaus.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.back
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

@Composable
fun RhythHausTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
) {
    SmallTopAppBar(
        title = title,
        modifier = modifier,
        color = HausColors.current.paper,
        titleColor = HausColors.current.ink,
        subtitle = subtitle,
        subtitleColor = HausColors.current.muted,
        defaultWindowInsetsPadding = false,
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    backgroundColor = Color.Transparent,
                    minWidth = 44.dp,
                    minHeight = 44.dp,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        tint = HausColors.current.ink,
                    )
                }
            }
        },
    )
}
```

If `automirrored.filled.ArrowBack` is unavailable, change imports and `imageVector` to:

```kotlin
import androidx.compose.material.icons.filled.ArrowBack
...
imageVector = Icons.Filled.ArrowBack
```

Do not add a new dependency.

- [ ] **Step 3: Compile the wrapper**

Run:
```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```
Expected: pass.

- [ ] **Step 4: Update OpenSpec evidence**

Edit `openspec/changes/miuix-top-app-bar/tasks.md` Task 1 checkbox to complete and record the exact command outcome.

## Task 2: Replace Search and Settings Top Bars

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `openspec/changes/miuix-top-app-bar/tasks.md`

**Interfaces:**
- Consumes: `RhythHausTopAppBar(title, onBack, modifier, subtitle)` from Task 1.
- Produces: Search and Settings screens using the shared Miuix top app bar wrapper.

- [ ] **Step 1: Read the current files**

Use file read tools for:
```text
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt
```
Confirm both still contain a `BackChip` + title `Row` before editing.

- [ ] **Step 2: Replace Search top row**

In `SearchScreen.kt`:

Remove unused imports after the edit if they are no longer needed:
```kotlin
import com.eterocell.rhythhaus.ui.BackChip
```

Add:
```kotlin
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
```

Replace this shape:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
) {
    BackChip(onClick = onDismiss)
    Spacer(Modifier.weight(1f))
    Text(stringResource(Res.string.search), color = HausColors.current.ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
}
```

With:
```kotlin
RhythHausTopAppBar(
    title = stringResource(Res.string.search),
    onBack = onDismiss,
)
```

Do not change the Search field, focus requester, filtering, result list, result row, equalizer, or dismiss callback.

- [ ] **Step 3: Replace Settings top row**

In `SettingsScreen.kt`:

Remove unused imports after the edit if they are no longer needed:
```kotlin
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import com.eterocell.rhythhaus.ui.BackChip
```

Add:
```kotlin
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
```

Replace this shape:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
) {
    BackChip(onClick = onDismiss)
    Spacer(Modifier.weight(1f))
    Text(
        text = stringResource(Res.string.settings),
        color = HausColors.current.ink,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
    )
}
```

With:
```kotlin
RhythHausTopAppBar(
    title = stringResource(Res.string.settings),
    onBack = onDismiss,
)
```

Do not change the Settings `Scaffold`, appearance dropdown, scan controls, folder picker button, import message, or clear-library button.

- [ ] **Step 4: Verify Search/Settings compile and navigation test**

Run:
```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```
Expected: both pass.

- [ ] **Step 5: Update OpenSpec evidence**

Edit `openspec/changes/miuix-top-app-bar/tasks.md` Task 2 checkbox to complete and record exact command outcomes plus a note that Search/Settings behavior outside top chrome was unchanged.

## Task 3: Replace Library Drill-Down Header Back/Subtitle Row

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `openspec/changes/miuix-top-app-bar/tasks.md`

**Interfaces:**
- Consumes: `RhythHausTopAppBar(title, onBack, modifier, subtitle)` from Task 1.
- Produces: `DrillDownHeader` using Miuix top app bar for back/subtitle chrome while preserving the large title below.

- [ ] **Step 1: Read the current drill-down header**

Use file read tools for:
```text
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt
```
Confirm `DrillDownHeader` still has a `Row` with `BackChip(onClick = onBack)` and subtitle `Text` above the large title.

- [ ] **Step 2: Replace only the back/subtitle row**

In `LibraryRows.kt`:

Add:
```kotlin
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
```

Remove `BackChip` import only if no longer used in this file.

Replace this shape inside `DrillDownHeader`:
```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
) {
    BackChip(onClick = onBack)
    Text(
        text = subtitle,
        color = HausColors.current.muted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
```

With:
```kotlin
RhythHausTopAppBar(
    title = subtitle,
    onBack = onBack,
)
```

Keep the existing large title immediately below:
```kotlin
Text(
    text = title,
    color = HausColors.current.ink,
    fontSize = 44.sp,
    lineHeight = 42.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = (-1.6).sp,
    fontFamily = FontFamily.SansSerif,
)
```

Do not change `DrillDownView`, `NestedScrollBlurChrome`, `TrackRow`, `SectionLabel`, list content padding, scroll reporting, left-edge swipe back, or Now Playing bar behavior.

- [ ] **Step 3: Verify drill-down compile and navigation test**

Run:
```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```
Expected: both pass.

- [ ] **Step 4: Update OpenSpec evidence**

Edit `openspec/changes/miuix-top-app-bar/tasks.md` Task 3 checkbox to complete and record exact command outcomes plus a note that the large drill-down title and nested-scroll chrome were preserved.

## Task 4: Final Verification and Handoff Evidence

**Files:**
- Modify: `openspec/changes/miuix-top-app-bar/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`

**Interfaces:**
- Consumes: completed Tasks 1-3.
- Produces: final verification record and clean scoped handoff.

- [ ] **Step 1: Validate OpenSpec**

Run:
```bash
openspec validate miuix-top-app-bar --strict
```
Expected: `Change 'miuix-top-app-bar' is valid`.

- [ ] **Step 2: Run broad JVM/desktop/Android verification**

Run:
```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```
Expected: pass. If a known transient playback test fails, rerun the specific failing test once and then rerun the broad command once; record all outcomes exactly.

- [ ] **Step 3: Run Xcode availability and iOS simulator verification**

Run:
```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```
Expected: both pass. If `xcodebuild` is unavailable, record the blocker and do not claim iOS verification passed.

- [ ] **Step 4: Run diff hygiene**

Run:
```bash
git diff --check
```
Expected: no output and exit 0.

- [ ] **Step 5: Update final evidence**

Update `openspec/changes/miuix-top-app-bar/tasks.md` Task 4 with exact command outcomes.

Append a handoff entry to `progress.md` containing:
```text
Route: openspec+superpowers
Owner: implementation
Input: User asked to use miuix TopAppBar instead of current custom top bar.
Output: <concise summary>
Verification:
- <exact commands and pass/fail>
Changed files:
- <files changed>
Next owner: user for manual visual/runtime QA
Blockers: <none or exact blocker>
Commit: <pending until committed>
```

Update `roadmap.md` with one concise completed entry for the Miuix TopAppBar migration.

- [ ] **Step 6: Final staged diff review and commit**

Before committing, describe the staged diff to the user-facing controller. Stage only in-scope files and keep `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` unstaged.

Run:
```bash
git status --short
git diff --cached --name-status
git diff --cached --check
git commit -m "feat: migrate top bars to Miuix"
```

Expected: commit succeeds and post-commit status shows only the pre-existing out-of-scope iOS scheme modification.
