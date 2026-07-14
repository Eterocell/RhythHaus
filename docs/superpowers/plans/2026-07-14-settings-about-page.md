# Settings About Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add shared Settings-derived About and Open Source Libraries screens that show RhythHaus identity, the Gradle-owned version, source repository, and AboutLibraries attributions across Android, iOS, and desktop JVM/macOS.

**Architecture:** Extend the existing custom `LibraryRoute` stack. Generate common build metadata from `rhythhaus.versionName`; keep the RhythHaus mark and AboutLibraries JSON in common Compose resources. Use the regular AboutLibraries plugin and Compose Material 3 renderer, never Android-only attribution or metadata APIs.

**Tech Stack:** Kotlin 2.4.0, Compose Multiplatform 1.11.1, Miuix 0.9.3, AboutLibraries 15.0.3, Kotlin Multiplatform common/JVM tests, Gradle.

## Global Constraints

- Use AboutLibraries `15.0.3`, `com.mikepenz.aboutlibraries.plugin`, and `aboutlibraries-compose-m3`.
- Generate the common version from `rhythhaus.versionName`; do not duplicate it, use Android `BuildConfig`, or use platform-only resources in common UI.
- The version generator must have provider-backed Gradle input, declared output, common-source registration, and an override-property verification task.
- AboutLibraries export is explicit maintenance/CI work; ordinary compilation and tests consume the checked-in JSON without modifying it.
- Verify compact and wide route rendering ownership, not only route-stack mutation.
- Recreate the launcher mark as a standalone common logo resource; do not copy Android adaptive-icon XML.
- Use `https://github.com/Eterocell/RhythHaus` exactly for the source action.
- Preserve Settings safe insets, `CompactSettingsLayoutPolicy`, source management, dialogs, and existing navigation semantics.
- Include English and Chinese strings for all new user-visible copy.
- Do not change database, scanning, playback, persistence, app entry points, or Windows/Linux scope.
- Use strict RED-GREEN TDD for all new behavioral code.

---

### Task 1: Common build metadata and attribution catalog

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `shared/build.gradle.kts`
- Create: `shared/src/commonMain/composeResources/files/aboutlibraries.json`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt`
- Create: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutLibrariesCatalogTest.kt`

**Interfaces:**
- Produces: generated `RhythHausBuildInfo.versionName: String` in common source.
- Produces: a common `files/aboutlibraries.json` resource.

- [ ] **Step 1: Write failing metadata and catalog tests**

Add a common test that asserts `RhythHausBuildInfo.versionName` is nonblank, a dedicated Gradle verification task that compiles with an overridden `-Prhythhaus.versionName`, and a JVM test that reads `Res.readBytes("files/aboutlibraries.json")`, parses the catalog with the pinned 15.0.3 API, and asserts it has at least one library.

- [ ] **Step 2: Verify RED**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' --tests 'com.eterocell.rhythhaus.settings.AboutLibrariesCatalogTest' --configuration-cache
```

Expected: compilation fails because the generated version API and catalog dependency/resource do not exist.

- [ ] **Step 3: Add the minimal Gradle configuration**

Add version-catalog aliases for the AboutLibraries plugin and `aboutlibraries-compose-m3`; apply the plugin as `false` at root and in `shared`. Register a `shared` Gradle task that reads `providers.gradleProperty("rhythhaus.versionName")`, declares that provider as an input, writes `RhythHausBuildInfo.kt` under a declared generated common source directory, registers that directory with `commonMain`, and makes relevant compilation tasks depend on it. Configure AboutLibraries to export the common JSON resource without timestamps, but do not make export a compilation dependency.

- [ ] **Step 4: Generate catalog and verify GREEN**

```bash
./gradlew :shared:exportLibraryDefinitions --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' --tests 'com.eterocell.rhythhaus.settings.AboutLibrariesCatalogTest' --configuration-cache
./gradlew :shared:verifyRhythHausVersionOverride -Prhythhaus.versionName=9.9.9 --configuration-cache
```

Expected: both tests pass and the JSON catalog is tracked.

### Task 2: Route stack and Settings entry

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`

**Interfaces:**
- Produces: `LibraryRoute.SettingsAbout` and `LibraryRoute.OpenSourceLibraries`.
- Extends: `SettingsScreen` with `onAboutClick: () -> Unit`.

- [ ] **Step 1: Write failing route test**

Add a `LibraryNavigationTest` case that pushes `Settings`, `SettingsAbout`, and `OpenSourceLibraries`, then proves successive pops return to About, Settings, and Home. Add shell-level compact/wide route classification coverage proving both pages are treated as active non-detail overlays.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Expected: compilation fails because the two routes are missing.

- [ ] **Step 3: Add routes and wiring**

Add the two routes, exhaustive rendering branches, and callbacks that use existing `pushRoute`/`popRoute` operations. Add the Settings About action after current management content without changing `CompactSettingsLayoutPolicy`, source-management callbacks, dialogs, or button targets.

- [ ] **Step 4: Verify GREEN**

Run the Step 2 command. Expected: all navigation tests pass.

### Task 3: Shared About surface

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt`
- Create: `shared/src/commonMain/composeResources/drawable/rhythhaus_logo.xml`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-zh/strings.xml`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt`

**Interfaces:**
- Produces: `SettingsAboutScreen(onOpenLibraries, onDismiss)`.
- Consumes: `RhythHausBuildInfo.versionName`, common logo resource, `LocalUriHandler`, and `RhythHausTopAppBar`.

- [ ] **Step 1: Write failing About contract tests**

Add pure tests for the source URL constant and nonblank generated version presented by the About screen contract.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' --configuration-cache
```

Expected: compilation fails because the About contract/screen is absent.

- [ ] **Step 3: Implement the minimal shared page**

Create a standalone common drawable based on the existing launcher mark, preserving the Android adaptive resources separately. Add matching English and Chinese strings. Create an insets-safe About screen using the shared top bar, logo, app name, generated version, source action via `LocalUriHandler`, and an Open Source Libraries action.

- [ ] **Step 4: Verify GREEN and compilation**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both commands pass.

### Task 4: Open Source Libraries surface

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutLibrariesCatalogTest.kt`
- Modify: `shared/src/commonMain/composeResources/files/aboutlibraries.json`

**Interfaces:**
- Produces: `OpenSourceLibrariesScreen(onDismiss)`.
- Consumes: `Res.readBytes("files/aboutlibraries.json")`, `produceLibraries`, and `LibrariesContainer`.

- [ ] **Step 1: Extend the catalog test for UI input**

Assert the parsed catalog exposes nonempty library data using the same parser that the screen will consume.

- [ ] **Step 2: Verify RED**

Run the focused catalog test and confirm it fails before the screen/resource loading path exists.

- [ ] **Step 3: Implement loading and renderer states**

Add an insets-safe shared libraries screen with the common top bar, localized loading state while parsing, and `LibrariesContainer` once metadata is available. Do not add a separate UI system or Android-only implementation.

- [ ] **Step 4: Regenerate and verify GREEN**

```bash
./gradlew :shared:exportLibraryDefinitions --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutLibrariesCatalogTest' --configuration-cache
```

Expected: catalog parses and the focused test passes.

Review the regenerated JSON for missing POM/native license metadata and verify its project-wide attribution policy against Android, JVM, and iOS dependency coverage. Add overrides only for concrete omissions.

### Task 5: Full verification and durable evidence

**Files:**
- Modify: `openspec/changes/add-settings-about-page/tasks.md`
- Modify: `roadmap.md`
- Modify: `progress.md`

- [ ] **Step 1: Run strict validation and supported-platform checks**

```bash
openspec validate add-settings-about-page --strict
./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
git diff --check
```

- [ ] **Step 2: Record actual evidence**

Mark OpenSpec tasks complete only after evidence exists. Mark roadmap item 19 complete only after the required checks pass; update `progress.md` with commands, results, source-link/logo manual-QA limits, and the exact existing iOS blocker if it remains.

- [ ] **Step 3: Review scope**

Confirm the diff is limited to the planned shared UI, resource, Gradle, test, and durable-evidence files; do not alter the pre-existing `roadmap.md` content outside item 19.
