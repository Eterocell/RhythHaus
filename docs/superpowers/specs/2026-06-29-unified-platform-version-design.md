# Unified Platform Version Design

Date: 2026-06-29
Route: openspec+superpowers
Status: approved design input

## User Request

Unify version name and version code of all supported platforms into a single place to edit.

## Current Context

RhythHaus currently declares app versions separately:

- Android: `androidApp/build.gradle.kts` hardcodes `versionCode = 1` and `versionName = "1.0"`.
- Desktop/macOS DMG: `desktopApp/build.gradle.kts` hardcodes `packageVersion = "1.0.0"`.
- iOS: `iosApp/Configuration/Config.xcconfig` already declares `CURRENT_PROJECT_VERSION=1` and `MARKETING_VERSION=1.0`, while `iosApp/iosApp.xcodeproj/project.pbxproj` also contains hardcoded target-level `MARKETING_VERSION = 1.0;` entries.

The repository has an existing local modification in `iosApp/iosApp.xcodeproj/project.pbxproj`. Implementation must preserve existing user/project changes and avoid unrelated Xcode project rewrites.

## Goals

- Make root `gradle.properties` the single editable source for app version metadata.
- Provide one version name and one version code used by Android, desktop/macOS packaging, and iOS.
- Keep Xcode builds usable without requiring the user to manually edit Xcode project settings.
- Keep the change lightweight and project-local.

## Non-goals

- Do not change app IDs, bundle IDs, signing teams, deployment targets, or product names.
- Do not change plugin versions, dependency versions, Android SDK versions, or Kotlin/Compose versions.
- Do not add external dependencies or toolchain requirements.
- Do not introduce Windows/Linux packaging.
- Do not migrate or regenerate the Xcode project structure.

## Design

### Single source of truth

Add the app version keys to root `gradle.properties`:

```properties
rhythhaus.versionName=1.0.0
rhythhaus.versionCode=1
```

The root `gradle.properties` file is acceptable because app version metadata is committed project configuration, not machine-specific local configuration.

### Android

`androidApp/build.gradle.kts` reads the two root Gradle properties via Gradle providers:

- `rhythhaus.versionName` -> `defaultConfig.versionName`
- `rhythhaus.versionCode` -> `defaultConfig.versionCode`

The Android `versionCode` must parse as an integer. If either property is missing or invalid, the Gradle build should fail clearly during configuration rather than silently falling back to stale hardcoded values.

### Desktop/macOS

`desktopApp/build.gradle.kts` reads `rhythhaus.versionName` from the same root Gradle property and uses it as Compose Desktop `nativeDistributions.packageVersion`.

The current product scope remains macOS DMG only.

### iOS

iOS cannot consume Gradle properties directly from Xcode build settings, so the repo should include a small Gradle-owned sync path:

- Create or update `iosApp/Configuration/Version.xcconfig` from root `gradle.properties`.
- `iosApp/Configuration/Config.xcconfig` includes `Version.xcconfig` and maps:
  - `CURRENT_PROJECT_VERSION=$(RHYTHHAUS_VERSION_CODE)`
  - `MARKETING_VERSION=$(RHYTHHAUS_VERSION_NAME)`
- `iosApp/iosApp.xcodeproj/project.pbxproj` should stop hardcoding target-level `MARKETING_VERSION = 1.0;` and instead inherit/use the xcconfig value.

The generated/synced xcconfig should contain:

```xcconfig
RHYTHHAUS_VERSION_NAME = 1.0.0
RHYTHHAUS_VERSION_CODE = 1
```

A committed `Version.xcconfig` is acceptable so Xcode can read version values even when opened directly before any Gradle task has run. Gradle should also provide a sync task so future edits to `gradle.properties` can refresh the iOS xcconfig deterministically.

## Acceptance Criteria

- Editing only `rhythhaus.versionName` and `rhythhaus.versionCode` in root `gradle.properties` is sufficient to control Android, desktop/macOS, and iOS app versions.
- Android no longer hardcodes `versionCode = 1` or `versionName = "1.0"`.
- Desktop no longer hardcodes `packageVersion = "1.0.0"`.
- iOS `Config.xcconfig` derives `CURRENT_PROJECT_VERSION` and `MARKETING_VERSION` from version keys synced from root `gradle.properties`.
- Existing Xcode project changes are preserved; no unrelated Xcode settings are rewritten.
- No dependency, plugin, SDK, signing, app ID, or packaging-scope changes are introduced.

## Verification Plan

Run focused Gradle verification:

```bash
./gradlew :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache
```

Verify iOS build settings resolve the unified values:

```bash
/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showBuildSettings | grep -E 'MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION'
```

If iOS build settings cannot be queried because of a local Xcode/project environment issue, record the exact blocker and verify the xcconfig files by inspection.
