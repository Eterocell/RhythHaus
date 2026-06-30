# Theme Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add persisted System/Light/Dark theme selection to shared RhythHaus settings and make the shared Compose UI readable in light and dark themes.

**Architecture:** Common code owns the theme mode model, DataStore-backed preference contract, active palette, and Settings UI. Platform source sets provide DataStore file creation/path support for Android, iOS, and macOS/JVM. The root `App()` observes the persisted mode and passes it into `RhythHausTheme` plus `SettingsScreen`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix, AndroidX DataStore Preferences, kotlinx.coroutines Flow, kotlin.test.

## Global Constraints

- Use OpenSpec change `theme-selection` as the binding product spec.
- Use AndroidX DataStore Preferences for theme persistence; do not use SQLDelight for this setting.
- Supported platforms for this change are Android, iOS, and macOS/desktop JVM only.
- Preserve existing layout, typography, navigation, playback, scanner, metadata, and library behavior.
- Keep shared-first implementation under `shared/src/commonMain` with platform actuals only for persistence/system seams.
- Do not add alpha dependencies unless stable DataStore `1.2.1` cannot compile for the required KMP targets.
- Run real verification commands and record exact outcomes in `progress.md`.

---

### Task 1: Theme model and DataStore preference foundation

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Theme.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.kt`
- Create: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.android.kt`
- Create: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.ios.kt`
- Create: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.jvm.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ThemeTest.kt`
- Create or modify JVM test as needed: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/ThemePreferenceStoreJvmTest.kt`

**Interfaces:**
- Produces: `enum class RhythHausThemeMode { System, Light, Dark }`
- Produces: `fun RhythHausThemeMode.Companion.fromSerialized(value: String?): RhythHausThemeMode`
- Produces: `val RhythHausThemeMode.serialized: String`
- Produces: `data class HausColorPalette(...)`
- Produces: `val LightHausPalette: HausColorPalette`
- Produces: `val DarkHausPalette: HausColorPalette`
- Produces: `fun resolveHausPalette(mode: RhythHausThemeMode, systemIsDark: Boolean): HausColorPalette`
- Produces: `interface ThemePreferenceStore { val selectedThemeMode: Flow<RhythHausThemeMode>; suspend fun setSelectedThemeMode(mode: RhythHausThemeMode) }`
- Produces: `expect fun createThemePreferenceStore(): ThemePreferenceStore`
- Produces: `@Composable expect fun systemPrefersDarkTheme(): Boolean`

- [ ] **Step 1: Add failing common theme tests**

Create `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ThemeTest.kt` with tests proving:
- `System`, `Light`, and `Dark` serialize to `system`, `light`, and `dark`.
- missing/invalid serialized values parse to `System`.
- `resolveHausPalette(System, false)` returns the light palette.
- `resolveHausPalette(System, true)` returns the dark palette.
- Settings option order is System, Light, Dark.

Run: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ThemeTest' --configuration-cache`
Expected: FAIL because the theme model does not exist yet.

- [ ] **Step 2: Add DataStore dependencies**

Add stable DataStore aliases to `gradle/libs.versions.toml`:
- version key such as `androidx-datastore = "1.2.1"`
- `androidx-datastore-core = { module = "androidx.datastore:datastore-core", version.ref = "androidx-datastore" }`
- `androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "androidx-datastore" }`

Wire required dependencies in `shared/build.gradle.kts`. Prefer commonMain dependencies if the artifacts resolve for all targets. If Gradle proves the stable artifacts need platform-specific placement, keep the smallest target-specific dependency layout that compiles for Android, iOS, and JVM.

- [ ] **Step 3: Implement common model and palette**

Create `Theme.kt` with the model, parser, option metadata, `HausColorPalette`, `LightHausPalette`, `DarkHausPalette`, and `resolveHausPalette`.

Use these baseline palette values unless neighboring code requires a direct slot rename:
- Light: keep current values: ink `0xFF111018`, paper `0xFFFFFAF1`, muted `0xFF776F66`, line `0x1A111018`, panel `0xFFF5EBDD`, panelStrong `0xFFE9D8C2`, pulse `0xFFFF5E3A`.
- Dark: paper/background `0xFF0F1117`, ink/text `0xFFF7EFE4`, muted `0xFFB7AFA6`, line `0x33F7EFE4`, panel `0xFF1A1D26`, panelStrong `0xFF252A36`, pulse `0xFFFF7A52`.

- [ ] **Step 4: Implement DataStore preference store**

Create `ThemePreferenceStore.kt` using DataStore Preferences. Store key `theme_mode`. Expose a Flow defaulting to System and a setter that writes the serialized value.

Platform actuals should provide safe app-specific paths:
- Android: under application context files directory using the existing app context holder/seam already used by library database/source access.
- iOS: under app documents or application support directory; create parent directories as needed.
- JVM/macOS: under `~/Library/Application Support/RhythHaus/theme.preferences_pb`; create parent directories as needed.

- [ ] **Step 5: Implement system dark-mode seam**

Add `@Composable expect fun systemPrefersDarkTheme(): Boolean` in common code.
Actuals may use Compose foundation/material APIs if available; otherwise return false only if no supported cross-platform API exists. Prefer `isSystemInDarkTheme()` where it compiles for the source set.

- [ ] **Step 6: Verify Task 1**

Run:
`./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ThemeTest' --configuration-cache`
Expected: PASS.

Run:
`./gradlew :shared:compileKotlinJvm --configuration-cache`
Expected: BUILD SUCCESSFUL.

Commit with message:
`feat: add theme preference model`

### Task 2: Root theme wiring and shared color migration

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausColors.kt` if not replaced by Task 1
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- Modify other shared UI files only where direct `Haus*` color usages remain.

**Interfaces:**
- Consumes Task 1 theme model and preference store.
- Produces: `RhythHausTheme(selectedThemeMode: RhythHausThemeMode, content: @Composable () -> Unit)` or equivalent.
- Produces: active palette usage across visible shared Compose UI.

- [ ] **Step 1: Wire App to preference Flow**

In `App()`, create the store with `remember { createThemePreferenceStore() }`, collect `selectedThemeMode` with initial `RhythHausThemeMode.System`, and pass the selected mode to `RhythHausTheme`.

Use `rememberCoroutineScope()` to persist updates from Settings in a coroutine.

- [ ] **Step 2: Update RhythHausTheme**

Change `RhythHausTheme` to resolve the selected mode against `systemPrefersDarkTheme()`, provide the active palette, and choose Miuix `lightColorScheme()` or `darkColorScheme()`.

- [ ] **Step 3: Migrate color reads mechanically**

Replace visible hardcoded `HausInk`, `HausPaper`, `HausMuted`, `HausLine`, `HausPanel`, `HausPanelStrong`, and `HausPulse` reads in shared composables with the active palette. Keep the names concise, e.g. `val colors = LocalHausColors.current` near the top of each composable and then `colors.ink`, `colors.paper`, etc.

Do not change layout, spacing, text, navigation, playback, scanner, or metadata logic.

- [ ] **Step 4: Verify Task 2**

Run:
`./gradlew :shared:compileKotlinJvm --configuration-cache`
Expected: BUILD SUCCESSFUL.

Run a targeted search:
`rg 'Haus(Ink|Paper|Muted|Line|Panel|PanelStrong|Pulse)' shared/src/commonMain/kotlin/com/eterocell/rhythhaus`
Expected: only palette definitions/compatibility wrappers remain, or remaining usages are intentionally non-composable constants not used for active themed UI.

Commit with message:
`feat: wire light and dark app theme`

### Task 3: Settings appearance selector

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify or create tests as practical for option metadata.

**Interfaces:**
- Consumes: `RhythHausThemeMode.options` or equivalent metadata from Task 1.
- Produces: Settings Appearance section that calls `onThemeModeSelected(mode)`.

- [ ] **Step 1: Extend SettingsScreen signature**

Add parameters:
`currentThemeMode: RhythHausThemeMode`
`onThemeModeSelected: (RhythHausThemeMode) -> Unit`

Update the `SettingsScreen` call site in `App.kt`.

- [ ] **Step 2: Add Appearance section**

Add an Appearance section before Manage Music. Render one compact dropdown selector whose expanded list contains System, Light, and Dark. Use active palette colors. The selected option should be visually distinct through fill/border/text weight, not only text copy.

- [ ] **Step 3: Persist selection from App**

Wire `onThemeModeSelected` from `App.kt` so selecting a mode launches a coroutine and calls `themePreferenceStore.setSelectedThemeMode(mode)`. The Flow should update the active theme.

- [ ] **Step 4: Verify Task 3**

Run:
`./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
Expected: BUILD SUCCESSFUL.

Commit with message:
`feat: add settings theme selector`

### Task 4: OpenSpec, iOS, and full verification handoff

**Files:**
- Modify: `openspec/changes/theme-selection/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes all previous tasks.
- Produces completed task checklist and verification evidence.

- [ ] **Step 1: Run iOS and OpenSpec validation**

Run:
`/usr/bin/xcrun xcodebuild -version`
Expected: prints installed Xcode version.

Run:
`./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
Expected: BUILD SUCCESSFUL, unless blocked by environment; record exact blocker if unavailable.

Run:
`openspec validate theme-selection --strict`
Expected: valid.

- [ ] **Step 2: Run final focused/full verification**

Run:
`./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
Expected: BUILD SUCCESSFUL.

Optionally run `./init.sh` if time allows and no known environment blocker exists.

- [ ] **Step 3: Update OpenSpec tasks**

Mark completed tasks in `openspec/changes/theme-selection/tasks.md` with `[x]` only for tasks actually implemented and verified.

- [ ] **Step 4: Update progress.md**

Add a top handoff entry with:
- Route: openspec+superpowers
- Owner: implementation
- Scope: light/dark/system theme selection with DataStore persistence
- Verification commands and exact pass/fail/blocker output summaries
- Changed files
- Manual visual validation recommendation
- Next owner and blockers

- [ ] **Step 5: Commit final evidence**

Commit remaining docs/evidence with a conventional message, or amend only if instructed by the coordinator. Suggested message if separate:
`docs: record theme selection verification`
