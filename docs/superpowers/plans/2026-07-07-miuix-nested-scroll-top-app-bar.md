# Miuix Nested-Scroll TopAppBar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render Library nested-scroll collapsed top chrome with Miuix `SmallTopAppBar` while preserving RhythHaus glass/backdrop and scroll-state behavior.

**Architecture:** Keep `NestedScrollBlurChrome` as the RhythHaus glass overlay shell and replace only its hand-built collapsed title row with the existing `RhythHausTopAppBar` wrapper. Extend the wrapper with optional color/inset/padding parameters while preserving defaults for Search, Settings, and Library drill-down back/title bars.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix 0.9.3 `miuix-ui`, RhythHaus shared Compose UI, OpenSpec.

## Global Constraints

- Use existing Miuix 0.9.3 `miuix-ui`; do not add dependencies.
- Do not add `miuix-navigation3-adaptive`.
- Do not touch `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`.
- Keep `NestedScrollBlurChrome`'s glass/backdrop shell, status-bar coverage, early return, divider, and progress thresholding.
- Do not adopt `MiuixScrollBehavior.nestedScrollConnection` for Library lists in this change.
- Preserve `nestedScrollChromeStateFor(...)`, Library scroll reporting, bottom-bar visibility, route transitions, track rows, Now Playing behavior, and existing Search/Settings/drill-down `RhythHausTopAppBar` behavior.
- Use patch/write_file tools; do not commit from subagents.
- Each task must update `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md` with exact verification evidence and write its report under `.superpowers/sdd/miuix-nested-scroll-top-app-bar/`.

---

## File Structure

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt`
  - Owns the shared RhythHaus wrapper around Miuix `SmallTopAppBar`.
  - Task 1 extends this wrapper with optional customization parameters.

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
  - Owns `NestedScrollBlurChrome`, `rememberSystemBarTopPadding`, scrollbar, and list-state conversion.
  - Task 2 replaces only the collapsed toolbar content with the Miuix wrapper.

- `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md`
  - Tracks task completion and command evidence.

- `.superpowers/sdd/miuix-nested-scroll-top-app-bar/task-N-report.md`
  - Task reports with status, changed files, commands/output, self-review, concerns.

---

### Task 1: Extend `RhythHausTopAppBar` Customization Points

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt`
- Modify: `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md`
- Create: `.superpowers/sdd/miuix-nested-scroll-top-app-bar/task-1-report.md`

**Interfaces:**
- Consumes: existing Miuix `SmallTopAppBar` signature in `miuix-ui` 0.9.3.
- Produces: backwards-compatible `RhythHausTopAppBar` with this minimum callable shape still valid:

```kotlin
RhythHausTopAppBar(
    title = stringResource(Res.string.search),
    onBack = onDismiss,
)
```

- Produces additional optional parameters after `subtitle`:

```kotlin
color: Color = HausColors.current.paper,
titleColor: Color = HausColors.current.ink,
subtitleColor: Color = HausColors.current.muted,
defaultWindowInsetsPadding: Boolean = false,
titlePadding: Dp = TopAppBarDefaults.TitlePadding,
navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
```

- [ ] **Step 1: Inspect the current wrapper and call sites**

Read:

```bash
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt
```

Confirm existing call sites use only `title`, `onBack`, and optional defaulted parameters.

- [ ] **Step 2: Extend wrapper parameters without changing defaults**

Patch `RhythHausTopAppBar.kt` to add imports and parameters. The resulting composable should have this shape:

```kotlin
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults

@Composable
fun RhythHausTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    color: Color = HausColors.current.paper,
    titleColor: Color = HausColors.current.ink,
    subtitleColor: Color = HausColors.current.muted,
    defaultWindowInsetsPadding: Boolean = false,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
) {
    SmallTopAppBar(
        title = title,
        modifier = modifier,
        color = color,
        titleColor = titleColor,
        subtitle = subtitle,
        subtitleColor = subtitleColor,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        titlePadding = titlePadding,
        navigationIconPadding = navigationIconPadding,
        actionIconPadding = actionIconPadding,
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
                        tint = titleColor,
                    )
                }
            }
        },
    )
}
```

Add `import androidx.compose.ui.unit.Dp` if missing.

- [ ] **Step 3: Run focused compile**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Update OpenSpec task evidence**

Mark Task 1 complete in `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md` and record the exact compile outcome.

- [ ] **Step 5: Write report**

Write `.superpowers/sdd/miuix-nested-scroll-top-app-bar/task-1-report.md` with:

```text
# Task 1 Report: Extend RhythHausTopAppBar

Status: DONE

Files changed:
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt
- openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md

Verification:
- ./gradlew :shared:compileKotlinJvm --configuration-cache
  Result: paste the real command outcome from the command output; include status line, duration, actionable-task count when Gradle prints one, and configuration-cache line when present.

Implementation notes:
- Existing wrapper API remains source-compatible.
- Defaults remain paper/ink/muted and defaultWindowInsetsPadding=false.
- Back icon tint now follows titleColor so transparent/glass-backed usage can control contrast.

Self-review:
- No dependency changes.
- No platform files touched.
- Existing Search/Settings/DrillDownHeader call sites still compile.

Concerns:
- None, or exact concern.
```

---

### Task 2: Migrate `NestedScrollBlurChrome` Title Content to Miuix TopAppBar

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- Modify: `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md`
- Create: `.superpowers/sdd/miuix-nested-scroll-top-app-bar/task-2-report.md`

**Interfaces:**
- Consumes Task 1 extended `RhythHausTopAppBar` optional parameters.
- Preserves `NestedScrollBlurChrome(state, title, backdrop, modifier, statusBarHeight)` signature.
- Preserves `nestedScrollChromeStateFor(...)` behavior and tests.

- [ ] **Step 1: Inspect current chrome implementation**

Read:

```bash
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt
```

Confirm call sites still pass `NestedScrollBlurChrome` the same state/title/backdrop/statusBarHeight values.

- [ ] **Step 2: Replace only the custom toolbar title row**

In `NestedScrollBlurChrome`, keep the outer `Box`, `chromeHeight`, `rhythHausLiquidGlass(...)`, bottom toolbar `Box`, and bottom divider.

Replace the inner custom `Row` containing the pulse dot and `Text` with:

```kotlin
RhythHausTopAppBar(
    title = title,
    onBack = null,
    modifier = Modifier
        .align(Alignment.CenterStart)
        .fillMaxWidth()
        .alpha(titleProgress),
    color = Color.Transparent,
    titleColor = HausColors.current.ink.copy(alpha = 0.86f),
    defaultWindowInsetsPadding = false,
    titlePadding = 20.dp,
    navigationIconPadding = 0.dp,
    actionIconPadding = 0.dp,
)
```

Required import changes:

- Add `import androidx.compose.ui.graphics.Color`.
- Add `import com.eterocell.rhythhaus.ui.RhythHausTopAppBar`.
- Remove imports that become unused only because of the old row removal, likely:
  - `androidx.compose.foundation.layout.Arrangement`
  - `androidx.compose.foundation.layout.Row`
  - `androidx.compose.foundation.layout.size`
  - `androidx.compose.foundation.shape.CircleShape`
  - `androidx.compose.ui.draw.clip`
  - `androidx.compose.ui.text.font.FontWeight`
  - `androidx.compose.ui.text.style.TextOverflow`
  - `androidx.compose.ui.unit.sp`
  - `top.yukonga.miuix.kmp.basic.Text`

Do not remove imports used elsewhere in the file.

- [ ] **Step 3: Confirm unchanged behavior by code inspection**

Verify these remain in `NestedScrollBlurChrome`:

```kotlin
val progress = state.progress.coerceIn(0f, 1f)
if (progress <= 0f) return
val titleProgress = ((progress - 0.68f) / 0.32f).coerceIn(0f, 1f)
val chromeHeight = statusBarHeight + NestedScrollChromeToolbarHeight
.rhythHausLiquidGlass(...)
.height(1.dp)
.background(HausColors.current.line.copy(alpha = 0.42f * progress))
```

Verify no changes were made to:

```text
LibraryHomeContent.kt
LibraryDetailContent.kt
LibraryNavigation.kt nestedScrollChromeStateFor(...)
NowPlayingBar usage
```

- [ ] **Step 4: Run focused verification**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
openspec validate miuix-nested-scroll-top-app-bar --strict
git diff --check
```

Expected: all pass.

- [ ] **Step 5: Update OpenSpec task evidence**

Mark Task 2 complete in `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md` and record exact outcomes for all focused commands.

- [ ] **Step 6: Write report**

Write `.superpowers/sdd/miuix-nested-scroll-top-app-bar/task-2-report.md` with:

```text
# Task 2 Report: Migrate NestedScrollBlurChrome to Miuix TopAppBar

Status: DONE

Files changed:
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
- openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md

Verification:
- ./gradlew :shared:compileKotlinJvm --configuration-cache
  Result: paste the real command outcome from the command output; include status line, duration, actionable-task count when Gradle prints one, and configuration-cache line when present.
- ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
  Result: paste the real command outcome from the command output; include status line, duration, actionable-task count when Gradle prints one, and configuration-cache line when present.
- openspec validate miuix-nested-scroll-top-app-bar --strict
  Result: paste the real command outcome from the command output; include status line, duration, actionable-task count when Gradle prints one, and configuration-cache line when present.
- git diff --check
  Result: paste the real command outcome from the command output; include status line, duration, actionable-task count when Gradle prints one, and configuration-cache line when present.

Implementation notes:
- Replaced only custom collapsed title row with RhythHausTopAppBar/Miuix SmallTopAppBar path.
- Preserved glass shell, status-bar overlay height, progress threshold, and divider.
- Did not introduce MiuixScrollBehavior nested scroll connection.

Self-review:
- No dependency changes.
- No platform files touched.
- Search/Settings/drill-down back/title bars are untouched except for wrapper defaults from Task 1.

Concerns:
- Manual visual QA remains needed for glass/title fade.
```

---

### Task 3: Final Verification and Handoff

**Files:**
- Modify: `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`
- Create: `.superpowers/sdd/miuix-nested-scroll-top-app-bar/final-report.md`

**Interfaces:**
- Consumes completed and reviewed Tasks 1 and 2.
- Produces final evidence and commit-ready scoped diff.

- [ ] **Step 1: Run OpenSpec validation**

Run:

```bash
openspec validate miuix-nested-scroll-top-app-bar --strict
```

Expected: `Change 'miuix-nested-scroll-top-app-bar' is valid`.

- [ ] **Step 2: Run broad JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run iOS toolchain/version check**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
```

Expected: prints installed Xcode version and exits 0.

- [ ] **Step 4: Run iOS simulator tests**

Run:

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run diff hygiene and status checks**

Run:

```bash
git diff --check
git status --short
git diff --stat -- . ':!iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme'
```

Expected:
- `git diff --check` exits 0 with no whitespace errors.
- `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` remains modified but unstaged/out-of-scope.

- [ ] **Step 6: Update final evidence**

Update `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md` Task 3 with exact command outcomes.

Prepend a `progress.md` handoff entry:

```text
## Handoff - 2026-07-07 Miuix nested-scroll TopAppBar migration

Route: openspec+superpowers
Owner: implementation
Input: User asked to also migrate nested scroll to use the Miuix TopAppBar.
Output:
- Extended `RhythHausTopAppBar` with optional customization points while preserving existing defaults.
- Migrated `NestedScrollBlurChrome` collapsed title content to the Miuix TopAppBar path.
- Preserved RhythHaus glass/backdrop overlay, status-bar coverage, scroll progress threshold, divider, Library scroll reporting, and Now Playing behavior.
Verification:
- Record each final verification command and the exact observed outcome from Steps 1-5.
Changed files:
- List each in-scope source, documentation, OpenSpec, progress, and roadmap path changed by this migration.
Next owner: user for manual visual/runtime QA of Library home and drill-down nested-scroll chrome on target devices.
Blockers: none for automated verification. Pre-existing modified `iosApp/.../iosApp.xcscheme` remains out of scope and was not touched.
Commit: pending staged diff review.
```

Update `roadmap.md` with one concise completed entry:

```text
- [x] Miuix nested-scroll TopAppBar 迁移：Library home/drill-down collapsed glass chrome 的标题内容改用 Miuix `SmallTopAppBar` 路径，保留 RhythHaus glass/backdrop、状态栏覆盖与滚动触发逻辑
```

- [ ] **Step 7: Write final report**

Write `.superpowers/sdd/miuix-nested-scroll-top-app-bar/final-report.md` summarizing final verification, changed files, manual QA gap, and out-of-scope iOS scheme status.

- [ ] **Step 8: Stage and commit scoped changes after staged diff review**

Stage only in-scope files. Do not stage `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`.

Before committing, show:

```bash
git diff --cached --name-status
git diff --cached --check
git status --short
```

Then commit:

```bash
git commit -m "feat: migrate nested scroll top bar to Miuix"
```

Expected post-commit status:

```text
 M iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme
```
