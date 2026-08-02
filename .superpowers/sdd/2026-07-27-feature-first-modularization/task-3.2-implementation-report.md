# Task 3.2 Implementation Report

Route: openspec+superpowers
Owner: implementation
Status: complete
Base commit: `ba500c3`
Commit: `07da78e` (`refactor: extract core platform capability`)

## Acceptance Inventory

The qualifying complete expect/actual family is the package-stable
`com.eterocell.rhythhaus.library` `currentTimeMillis()` / `uuid4()` family:

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryUtils.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryUtils.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryUtils.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/LibraryUtils.ios.kt`

`currentTimeMillis()` is consumed by Library scanning and Playlist backup.
`uuid4()` is consumed by Library scanning and Playback. This satisfies the required
two-independent-domain threshold. Source access/scanning, metadata, playback engine
and dispatch, persistence, theme, backup-document, and application-context seams
remain feature/shared-owned.

The mandated inventory command was run exactly:

```text
rg -n '^expect |^actual ' shared/src/{commonMain,androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/{Platform.kt,AudioMetadata.kt,library/PlatformSourceAccess.kt}
```

It reported the common `getPlatform`, `rememberPlatformFolderPickerLauncher`, and
`createPlatformSourceAccess` expects, then reported absent extensionless platform
paths because their actual files use platform suffixes. This command does not include
the qualified `LibraryUtils*.kt` family; that family was separately inspected above.

## RED/GREEN Evidence

RED command, run after creating only
`core/platform/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlatformCapabilityBoundaryTest.kt`:

```text
./gradlew :core:platform:allTests --configuration-cache
```

Result: failed as expected in 6 seconds with `Cannot locate tasks that match
':core:platform:allTests' as project 'platform' not found in project ':core'`.

The first post-registration run of the same command failed because the core KSP
policy additionally requires public KDoc on Android/JVM actual declarations. KDoc
was added without changing runtime behavior.

GREEN command:

```text
./gradlew :core:platform:allTests --configuration-cache
```

Result: passed in 6 seconds with 32 actionable tasks and configuration cache reuse.
The boundary test characterizes current epoch-millisecond behavior and RFC 4122
version-4 UUID shape plus 16-value uniqueness, accepting either UUID letter case.

## Verification

```text
./gradlew :core:platform:jvmTest :core:platform:testAndroidHostTest :core:platform:iosSimulatorArm64Test :core:platform:compileKotlinJvm :core:platform:compileAndroidMain :core:platform:compileKotlinIosSimulatorArm64 architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Passed in 1 second with 41 actionable tasks and configuration cache stored. XML
results: JVM 2/0/0/0, Android host 2/0/0/0, iOS simulator arm64 2/0/0/0
(tests/failures/errors/skips).

```text
./gradlew :core:platform:dependencies --configuration commonMainImplementation --configuration-cache --no-parallel
```

Passed; `commonMainImplementation` has no dependencies. A source/build scan found
no production dependency on shared, apps, features, core database/ui/model, Compose,
Media3, DataStore, or TagLib. A Gradle build-script scan found no `export(...)` calls,
so shared has no added iOS framework export.

`git diff --check` passed. `./init.sh` was intentionally skipped per user direction.
The final `:core:platform:spotlessKotlinCheck` rerun passed with `:core:platform:allTests`
and strict `architectureCheck` in 5 seconds (44 actionable tasks).

## Changed Paths

- `core/platform/build.gradle.kts`: new KMP core module with JVM, Android host test,
  and iOS arm64/simulator targets; Android namespace remains
  `com.eterocell.rhythhaus.platform`.
- `core/platform/src/{commonMain,androidMain,jvmMain,iosMain}/.../library/LibraryUtils*.kt`:
  moved the complete capability family with the original package and behavior; common
  expects and all actuals have explicit public visibility and KDoc.
- `core/platform/src/commonTest/.../PlatformCapabilityBoundaryTest.kt`: added capability
  boundary characterization.
- `settings.gradle.kts`: registered `:core:platform`.
- `shared/build.gradle.kts`: added `api(projects.core.platform)` to retain the public
  package surface transitively.
- `build-logic/.../ArchitectureAllowList.kt`: permits the preserved library package
  for `:core:platform` only.
- `shared/src/*/kotlin/.../library/LibraryUtils*.kt`: removed after the atomic move.

## Self-Review

The complete qualified expect/actual family moved atomically with no Kotlin package
rename or behavioral change. `:core:platform` has no production dependencies;
shared owns the single transitive API edge and continues to be the only iOS framework
facade. The architecture allow-list adjustment is limited to the platform module's
preserved package root. No controller-owned ledger, roadmap, OpenSpec artifact, plan,
ADR, or root evidence ledger was changed.

Concerns: none. Existing Gradle deprecation, signing, and taglib host-test warnings
were emitted but are pre-existing and non-blocking.
