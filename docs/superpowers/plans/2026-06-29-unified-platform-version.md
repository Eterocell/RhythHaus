# Unified Platform Version Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make root `gradle.properties` the single place to edit RhythHaus version name and version code for Android, desktop/macOS, and iOS.

**Architecture:** Add `rhythhaus.versionName` and `rhythhaus.versionCode` to root Gradle properties. Gradle modules read those properties directly for Android and desktop packaging. A root Gradle sync task writes `iosApp/Configuration/Version.xcconfig`, which iOS `Config.xcconfig` includes for Xcode build settings.

**Tech Stack:** Kotlin Gradle DSL, Android Gradle Plugin, Compose Desktop native distributions, Xcode xcconfig files.

## Global Constraints

- Work in `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus`.
- Preserve existing local changes in `iosApp/iosApp.xcodeproj/project.pbxproj`; only patch the version-related lines needed by this plan.
- Do not change app IDs, bundle IDs, signing teams, deployment targets, product names, plugin versions, dependency versions, SDK versions, Kotlin/Compose versions, or packaging scope.
- Do not add external dependencies or toolchain requirements.
- Root `gradle.properties` is the single editable source of truth with `rhythhaus.versionName=1.0.0` and `rhythhaus.versionCode=1`.
- Focused verification commands:
  - `./gradlew syncIosVersionXcconfig :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache`
  - `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showBuildSettings | grep -E 'MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION'`

---

### Task 1: Wire shared app version metadata

**Files:**
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/gradle.properties`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/build.gradle.kts`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/androidApp/build.gradle.kts`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/desktopApp/build.gradle.kts`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/iosApp/Configuration/Config.xcconfig`
- Create: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/iosApp/Configuration/Version.xcconfig`
- Modify: `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/iosApp/iosApp.xcodeproj/project.pbxproj` only to remove or inherit target-level hardcoded `MARKETING_VERSION = 1.0;` entries if needed.

**Interfaces:**
- Consumes: root Gradle properties `rhythhaus.versionName` and `rhythhaus.versionCode`.
- Produces: Gradle task `syncIosVersionXcconfig`.
- Produces: iOS xcconfig variables `RHYTHHAUS_VERSION_NAME` and `RHYTHHAUS_VERSION_CODE`.

- [ ] **Step 1: Re-check workspace and version declarations**

Run:

```bash
git status --short
rg -n "versionCode|versionName|packageVersion|MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION|rhythhaus.version" androidApp desktopApp iosApp gradle.properties build.gradle.kts
```

Expected:
- `iosApp/iosApp.xcodeproj/project.pbxproj` may already be modified before this task starts.
- Current hardcoded version declarations are still visible before edits.

- [ ] **Step 2: Add root version properties**

Append these lines to root `gradle.properties` if they are not already present:

```properties
rhythhaus.versionName=1.0.0
rhythhaus.versionCode=1
```

Do not remove or change existing Gradle daemon, Android, or Kotlin settings.

- [ ] **Step 3: Add root Gradle helpers and iOS sync task**

In root `build.gradle.kts`, add the following imports at the top if missing:

```kotlin
import java.util.Properties
```

Add this root-level code:

```kotlin
fun ProviderFactory.requiredGradleProperty(name: String): Provider<String> =
    gradleProperty(name).orElse(
        provider { throw GradleException("Missing required Gradle property '$name'") },
    )

val rhythHausVersionName: Provider<String> = providers.requiredGradleProperty("rhythhaus.versionName")
val rhythHausVersionCode: Provider<String> = providers.requiredGradleProperty("rhythhaus.versionCode")

val syncIosVersionXcconfig by tasks.registering {
    val outputFile = layout.projectDirectory.file("iosApp/Configuration/Version.xcconfig")
    inputs.property("rhythHausVersionName", rhythHausVersionName)
    inputs.property("rhythHausVersionCode", rhythHausVersionCode)
    outputs.file(outputFile)

    doLast {
        val versionName = rhythHausVersionName.get()
        val versionCode = rhythHausVersionCode.get()
        versionCode.toIntOrNull()
            ?: throw GradleException("Gradle property 'rhythhaus.versionCode' must be an integer, was '$versionCode'")

        outputFile.asFile.writeText(
            """
            // Generated from root gradle.properties by ./gradlew syncIosVersionXcconfig.
            // Edit rhythhaus.versionName and rhythhaus.versionCode in ../../gradle.properties.
            RHYTHHAUS_VERSION_NAME = $versionName
            RHYTHHAUS_VERSION_CODE = $versionCode
            """.trimIndent() + "\n",
        )
    }
}
```

If root `build.gradle.kts` does not exist, create it with exactly that content. If it exists, preserve existing content and add only these helpers/task.

- [ ] **Step 4: Update Android version wiring**

In `androidApp/build.gradle.kts`, add these provider values after dependencies or before `android {}`:

```kotlin
val rhythHausVersionName = providers.gradleProperty("rhythhaus.versionName")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionName'") })
val rhythHausVersionCode = providers.gradleProperty("rhythhaus.versionCode")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionCode'") })
```

Replace:

```kotlin
versionCode = 1
versionName = "1.0"
```

with:

```kotlin
versionCode = rhythHausVersionCode.get().toIntOrNull()
    ?: throw GradleException("Gradle property 'rhythhaus.versionCode' must be an integer, was '${rhythHausVersionCode.get()}'")
versionName = rhythHausVersionName.get()
```

- [ ] **Step 5: Update desktop package version wiring**

In `desktopApp/build.gradle.kts`, add:

```kotlin
val rhythHausVersionName = providers.gradleProperty("rhythhaus.versionName")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionName'") })
```

Replace:

```kotlin
packageVersion = "1.0.0"
```

with:

```kotlin
packageVersion = rhythHausVersionName.get()
```

- [ ] **Step 6: Create committed iOS version xcconfig**

Create `iosApp/Configuration/Version.xcconfig` with:

```xcconfig
// Generated from root gradle.properties by ./gradlew syncIosVersionXcconfig.
// Edit rhythhaus.versionName and rhythhaus.versionCode in ../../gradle.properties.
RHYTHHAUS_VERSION_NAME = 1.0.0
RHYTHHAUS_VERSION_CODE = 1
```

This file is committed so Xcode can resolve versions before Gradle has run in a fresh checkout.

- [ ] **Step 7: Include iOS version xcconfig**

Modify `iosApp/Configuration/Config.xcconfig` so the top of the file includes `Version.xcconfig` and maps Xcode version settings from the shared keys:

```xcconfig
#include "Version.xcconfig"

TEAM_ID=

PRODUCT_NAME=RhythHaus
PRODUCT_BUNDLE_IDENTIFIER=com.eterocell.rhythhaus.RhythHaus$(TEAM_ID)

CURRENT_PROJECT_VERSION=$(RHYTHHAUS_VERSION_CODE)
MARKETING_VERSION=$(RHYTHHAUS_VERSION_NAME)

OTHER_LDFLAGS = $(inherited) -lsqlite3
```

Preserve the existing bundle identifier and linker flags.

- [ ] **Step 8: Remove target-level iOS hardcoded marketing versions**

In `iosApp/iosApp.xcodeproj/project.pbxproj`, remove only target build-setting lines like:

```pbxproj
MARKETING_VERSION = 1.0;
```

Do not change any unrelated Xcode project settings. The target already uses `baseConfigurationReferenceRelativePath = Config.xcconfig`, so removing target-level `MARKETING_VERSION` lets the xcconfig value apply. If the project file contains `CURRENT_PROJECT_VERSION` target-level hardcoding, remove only those hardcoded version lines too.

- [ ] **Step 9: Sync iOS xcconfig and verify version declarations**

Run:

```bash
./gradlew syncIosVersionXcconfig --configuration-cache
rg -n "versionCode|versionName|packageVersion|MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION|rhythhaus.version" androidApp desktopApp iosApp gradle.properties build.gradle.kts
```

Expected:
- `gradle.properties` has `rhythhaus.versionName=1.0.0` and `rhythhaus.versionCode=1`.
- `androidApp/build.gradle.kts` reads Gradle properties.
- `desktopApp/build.gradle.kts` reads Gradle properties.
- `iosApp/Configuration/Config.xcconfig` maps `CURRENT_PROJECT_VERSION` and `MARKETING_VERSION` from `RHYTHHAUS_*` variables.
- `iosApp/Configuration/Version.xcconfig` contains the synced values.
- No hardcoded `MARKETING_VERSION = 1.0;` remains in target build settings.

- [ ] **Step 10: Run focused verification**

Run:

```bash
./gradlew syncIosVersionXcconfig :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache
/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showBuildSettings | grep -E 'MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION'
```

Expected:
- Gradle exits 0.
- Xcode build settings show `MARKETING_VERSION = 1.0.0` and `CURRENT_PROJECT_VERSION = 1`, or equivalent resolved values.
- If Xcode cannot query settings due to an environment/project issue, record the exact output and verify `Config.xcconfig`/`Version.xcconfig` by inspection.

- [ ] **Step 11: Review diff and commit**

Run:

```bash
git diff -- gradle.properties build.gradle.kts androidApp/build.gradle.kts desktopApp/build.gradle.kts iosApp/Configuration/Config.xcconfig iosApp/Configuration/Version.xcconfig iosApp/iosApp.xcodeproj/project.pbxproj
git status --short
```

Expected:
- Diffs are limited to version-source/wiring changes.
- Existing pbxproj modifications are preserved except version hardcoding removal.

Stage only the version-unification files and commit:

```bash
git add gradle.properties build.gradle.kts androidApp/build.gradle.kts desktopApp/build.gradle.kts iosApp/Configuration/Config.xcconfig iosApp/Configuration/Version.xcconfig iosApp/iosApp.xcodeproj/project.pbxproj
git diff --cached --stat
git commit -m "build: unify platform version metadata"
```
