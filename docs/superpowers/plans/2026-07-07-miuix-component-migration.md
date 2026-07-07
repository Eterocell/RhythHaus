# Miuix Component Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Selectively migrate standard RhythHaus shared Compose UI from hand-rolled bare/custom controls to Miuix components while preserving app-specific music UI and existing behavior.

**Architecture:** Add only actually-used Miuix modules at the existing `miuix = "0.9.3"` version line, then migrate UI in narrow behavior-preserving slices. Settings gets the first concrete migration via `OverlayDropdownPreference`; Search, Library rows, and Clear Library dialog are evaluated and migrated only where Miuix components preserve semantics without awkward workarounds.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.11.1, Miuix 0.9.3 (`miuix-ui`, `miuix-blur`, candidate `miuix-preference`), OpenSpec, Gradle configuration cache.

## Global Constraints

- Use the selective Miuix-first approach approved by the user.
- Add Miuix modules as needed; every added Miuix module must use `version.ref = "miuix"` from `gradle/libs.versions.toml`.
- Do not add `top.yukonga.miuix.kmp:miuix-navigation3-adaptive`.
- Preserve existing Settings, Search, Library, Clear Library, playback, route animation, bottom bar, and glass/status-bar behavior.
- Keep product-specific music UI custom where Miuix is not a suitable semantic fit: artwork rendering, equalizer visuals, scrubber gestures, bottom-sheet gestures, edge-swipe gestures, adaptive shell behavior, and glass/backdrop wrappers.
- The pre-existing modified file `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` is outside this change and must not be touched.
- Use `rhythhaus-miuix-usage` guidance: overlay dropdown/dialog components must be inside Miuix `Scaffold` popup host; blur paths must retain `isRenderEffectSupported()` / `isRuntimeShaderSupported()` gating.
- Run Android debug assembly after adding Miuix dependencies to catch duplicate classes or manifest issues.

---

## File Structure

- Modify `gradle/libs.versions.toml`: add `miuix-preference = { module = "top.yukonga.miuix.kmp:miuix-preference", version.ref = "miuix" }` when Task 1 needs it.
- Modify `shared/build.gradle.kts`: add `implementation(libs.miuix.preference)` in `commonMain.dependencies` when Task 1 needs it.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`: replace custom appearance dropdown with Miuix `Scaffold` + `OverlayDropdownPreference`.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`: selectively migrate search input/container or result row standard pieces if Miuix `TextField`/`IconButton`/`Surface` preserve behavior.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`: evaluate `TrackRow` and `ArtistRow`; migrate only if cleanly preserves artwork/selected/accessibility/layout behavior, otherwise leave and document in report.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt`: evaluate Miuix `OverlayDialog`; preserve in-window route animation, so prefer keeping route shell and migrate inner components only if needed.
- Modify `openspec/changes/miuix-component-migration/tasks.md`: mark completed tasks with evidence.
- Modify `progress.md`: final handoff evidence.

### Real Miuix APIs confirmed from 0.9.3 source jars

- `top.yukonga.miuix.kmp.preference.OverlayDropdownPreference(items: List<String>, selectedIndex: Int, title: String, summary: String? = null, showValue: Boolean = true, renderInRootScaffold: Boolean = true, onSelectedIndexChange: ((Int) -> Unit)? = null, ...)`
- `top.yukonga.miuix.kmp.basic.Scaffold(content: @Composable (PaddingValues) -> Unit, popupHost default = MiuixPopupHost)`
- `top.yukonga.miuix.kmp.basic.TextField(value: String, onValueChange: (String) -> Unit, label: String = "", useLabelAsPlaceholder: Boolean = false, singleLine: Boolean = false, trailingIcon: @Composable (() -> Unit)? = null, ...)` exists via overload after the `TextFieldValue` overload in `TextField.kt`.
- `top.yukonga.miuix.kmp.basic.IconButton(onClick: () -> Unit, backgroundColor: Color = Color.Unspecified, cornerRadius: Dp = IconButtonDefaults.CornerRadius, ...)`
- `top.yukonga.miuix.kmp.overlay.OverlayDialog(show: Boolean, title: String? = null, summary: String? = null, onDismissRequest: (() -> Unit)? = null, renderInRootScaffold: Boolean = true, content: @Composable () -> Unit, ...)`

## Task 1: Add and verify `miuix-preference`

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Modify: `openspec/changes/miuix-component-migration/tasks.md`

**Interfaces:**
- Produces: `libs.miuix.preference` Gradle accessor for later tasks.
- Consumes: existing `miuix = "0.9.3"` version reference.

- [ ] **Step 1: Confirm dependency is absent and current Miuix line is 0.9.3**

Run:
```bash
grep -n "miuix" gradle/libs.versions.toml shared/build.gradle.kts
```
Expected: `miuix-ui` and `miuix-blur` exist; `miuix-preference` is absent.

- [ ] **Step 2: Add version catalog alias**

Edit `gradle/libs.versions.toml` in `[libraries]` near existing Miuix aliases:
```toml
miuix-ui = { module = "top.yukonga.miuix.kmp:miuix-ui", version.ref = "miuix" }
miuix-blur = { module = "top.yukonga.miuix.kmp:miuix-blur", version.ref = "miuix" }
miuix-preference = { module = "top.yukonga.miuix.kmp:miuix-preference", version.ref = "miuix" }
```

- [ ] **Step 3: Add shared dependency**

Edit `shared/build.gradle.kts` in `commonMain.dependencies` near existing Miuix dependencies:
```kotlin
implementation(libs.miuix.ui)
implementation(libs.miuix.blur)
implementation(libs.miuix.preference)
```

- [ ] **Step 4: Verify dependency resolution and Android compatibility**

Run:
```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :androidApp:assembleDebug --configuration-cache
```
Expected: both commands pass. If Android fails with duplicate Miuix classes, stop and report BLOCKED; do not work around by adding `miuix-navigation3-adaptive` or downgrading Miuix.

- [ ] **Step 5: Update OpenSpec task evidence**

Edit `openspec/changes/miuix-component-migration/tasks.md`: mark Task 1 complete only after the commands pass and include exact command outcomes.

## Task 2: Replace Settings appearance dropdown with Miuix `OverlayDropdownPreference`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `openspec/changes/miuix-component-migration/tasks.md`

**Interfaces:**
- Consumes: `libs.miuix.preference` from Task 1.
- Consumes: `RhythHausThemeMode.settingsOptions`, `displayLabelResource()`, `displayDescriptionResource()`.
- Produces: Settings appearance selection implemented by Miuix `OverlayDropdownPreference` inside Miuix `Scaffold` popup host.

- [ ] **Step 1: Read current Settings file**

Run through tool/read, not shell cat:
```text
read_file shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt
```
Confirm the custom `AppearanceDropdown` and `AppearanceDropdownOption` are still present before editing.

- [ ] **Step 2: Replace custom dropdown with Miuix component**

Implementation target:
- Remove imports that exist only for custom dropdown implementation: `background`, `border`, `RoundedCornerShape`, `mutableStateOf`, `remember`, `setValue`, `getValue`, `clip`, `hausClickable`, and `selected` resource if no longer used.
- Add imports:
```kotlin
import androidx.compose.foundation.layout.WindowInsets
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
```
- Wrap the current Settings content in Miuix `Scaffold` so overlay popup host exists:
```kotlin
Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HausColors.current.paper,
        contentWindowInsets = WindowInsets(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // existing content
        }
    }
}
```
If `WindowInsets(0.dp)` does not compile, inspect Compose API and use `WindowInsets(0, 0, 0, 0)` instead.

- Replace `AppearanceDropdown` implementation with:
```kotlin
@Composable
private fun AppearanceDropdown(
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
) {
    val options = RhythHausThemeMode.settingsOptions
    val selectedIndex = options.indexOf(currentThemeMode).coerceAtLeast(0)
    OverlayDropdownPreference(
        items = options.map { it.displayLabelResource() },
        selectedIndex = selectedIndex,
        title = stringResource(Res.string.appearance),
        summary = currentThemeMode.displayDescriptionResource(),
        modifier = Modifier.fillMaxWidth(),
        renderInRootScaffold = false,
        onSelectedIndexChange = { index ->
            options.getOrNull(index)?.let(onThemeModeSelected)
        },
    )
}
```
- Delete `AppearanceDropdownOption` completely.

- [ ] **Step 3: Compile Settings migration**

Run:
```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```
Expected: pass. If imports/API mismatch, inspect the exact Miuix source jar or compiler error and fix once. Do not replace with a new custom dropdown.

- [ ] **Step 4: Run Android duplicate-class check**

Run:
```bash
./gradlew :androidApp:assembleDebug --configuration-cache
```
Expected: pass.

- [ ] **Step 5: Update OpenSpec task evidence**

Mark Task 2 complete in `openspec/changes/miuix-component-migration/tasks.md` with the compile/build evidence.

## Task 3: Selectively migrate Search standard pieces

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- Modify: `openspec/changes/miuix-component-migration/tasks.md`

**Interfaces:**
- Consumes: existing `SearchScreen` behavior: focus requester, query state, filtering, clear action, result selection, equalizer for playing row.
- Produces: either Miuix `TextField`/`IconButton` search input or a documented decision to keep `BasicTextField` if Miuix replacement risks behavior.

- [ ] **Step 1: Inspect current Search code and Miuix `TextField` signature**

Read `SearchScreen.kt`. Confirm search behavior uses `FocusRequester`, placeholder, clear action, single line, and `query` filtering.

- [ ] **Step 2: Try Miuix `TextField` only if focus can be preserved**

Preferred implementation if it compiles cleanly:
```kotlin
TextField(
    value = query,
    onValueChange = { query = it },
    modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester),
    label = stringResource(Res.string.search_placeholder),
    useLabelAsPlaceholder = true,
    singleLine = true,
    textStyle = TextStyle(color = HausColors.current.ink, fontSize = 15.sp),
    cursorBrush = SolidColor(HausColors.current.pulse),
    trailingIcon = if (query.isNotEmpty()) {
        {
            IconButton(
                onClick = { query = "" },
                backgroundColor = Color.Transparent,
                minWidth = 40.dp,
                minHeight = 40.dp,
            ) {
                Text(stringResource(Res.string.clear), color = HausColors.current.pulse, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    } else {
        null
    },
)
```
Remove the outer custom bordered search `Box` only if Miuix `TextField` gives equivalent readable styling.

If focus requester or TextField overload does not compile after a focused fix, keep the current `BasicTextField` and only migrate the clear action container to Miuix `IconButton` if clean.

- [ ] **Step 3: Evaluate `SearchResultRow`**

Do not force `BasicComponent` if unavailable or awkward. It is acceptable to leave `Surface` because it is already Miuix. If changing, preserve:
- now-playing row background;
- title pulse color while playing;
- equalizer display;
- click behavior that sets queue, plays, and dismisses.

- [ ] **Step 4: Compile and run relevant tests**

Run:
```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```
Expected: pass.

- [ ] **Step 5: Update OpenSpec task evidence**

Record whether Search was migrated or intentionally left partially custom, with exact reason and command evidence.

## Task 4: Selectively migrate Library rows and Clear Library dialog pieces

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt`
- Modify: `openspec/changes/miuix-component-migration/tasks.md`

**Interfaces:**
- Consumes: existing row composables `TrackRow`, `ArtistRow`, `AlbumCard`, and route overlay `AnimatedClearLibraryDialogRoute`.
- Produces: only behavior-preserving Miuix replacements; documented no-op decisions where custom UI is the better semantic fit.

- [ ] **Step 1: Evaluate `TrackRow` and `ArtistRow` against Miuix components**

Rows must preserve artwork/artist marks, selected state, content descriptions, click behavior, duration/metadata display, and spacing. If Miuix `BasicComponent` is not present in accessible source jars or would make duration/artwork/selected layout awkward, do not migrate these rows. Document this in the report/OpenSpec evidence.

- [ ] **Step 2: Keep album/artwork/equalizer custom**

Do not replace:
- `AlbumMark`
- album artwork `Box`/`Image` blocks
- `EqualizerStrip`
- gradient placeholders
These are product-specific.

- [ ] **Step 3: Evaluate Clear Library dialog**

Because `AnimatedClearLibraryDialogRoute` is route content and should animate with route transitions, do not replace the whole shell with `OverlayDialog` unless verified that route-level animation still applies. Preferred safe option:
- keep outer full-screen `Box` scrim and tap-to-dismiss behavior;
- keep Miuix `Card` and `Button` inner content as already implemented;
- optionally adjust button/container imports only if there is an obvious Miuix standard component improvement.

- [ ] **Step 4: Compile and run relevant tests**

Run:
```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```
Expected: pass.

- [ ] **Step 5: Update OpenSpec task evidence**

Record exactly which components were migrated and which remained custom with reasons.

## Task 5: Final verification, progress, and review-ready handoff

**Files:**
- Modify: `openspec/changes/miuix-component-migration/tasks.md`
- Modify: `progress.md`
- Review: all changed source/build/spec/plan files

**Interfaces:**
- Consumes: Tasks 1-4 completed changes and evidence.
- Produces: validated OpenSpec change, verification evidence, final handoff.

- [ ] **Step 1: Validate OpenSpec**

Run:
```bash
openspec validate miuix-component-migration --strict
```
Expected: `Change 'miuix-component-migration' is valid`.

- [ ] **Step 2: Run broad JVM/desktop/Android verification**

Run:
```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```
Expected: pass. If a known flaky test fails, rerun the exact failing test once and record both outputs.

- [ ] **Step 3: Run iOS verification**

Run:
```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```
Expected: Xcode available and iOS simulator tests pass. If Xcode is unavailable, record the exact blocker.

- [ ] **Step 4: Run whitespace/diff checks**

Run:
```bash
git diff --check
git status --short
git diff --stat
```
Expected: no whitespace errors. Status may still show the pre-existing modified Xcode scheme; do not include it in this change.

- [ ] **Step 5: Update `progress.md`**

Append a handoff entry:
```text
## Handoff - 2026-07-07 Miuix component migration

Route: openspec+superpowers
Owner: implementation
Input: miuix-component-migration spec/plan
Output: <summary of migrated components and documented custom keep decisions>
Verification:
- openspec validate miuix-component-migration --strict: pass
- ./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache: pass | fail
- /usr/bin/xcrun xcodebuild -version: pass | fail
- ./gradlew :shared:iosSimulatorArm64Test --configuration-cache: pass | fail
- git diff --check: pass
Changed files:
- <paths>
Next owner: user for visual validation of Settings dropdown/Search/Library UI
Blockers: <none or exact blocker>
Commit: pending unless user approves staged diff
```

- [ ] **Step 6: Final review before completion**

Review the final diff against the spec:
- no `miuix-navigation3-adaptive` added;
- all new Miuix aliases use `version.ref = "miuix"`;
- Settings dropdown uses Miuix component or blocker is recorded;
- product-specific custom UI was not over-migrated;
- verification evidence exists.
