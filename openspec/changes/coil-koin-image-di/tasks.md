# Tasks

## 1. Add dependencies and DI module

- [x] Add Coil 3.5.0 and Koin 4.2.2 aliases to `gradle/libs.versions.toml`.
  - Evidence: `coil = "3.5.0"`, `koin = "4.2.2"`, `coil-compose`, `coil-core`, `koin-core`, and `koin-compose` aliases added.
- [x] Add required Coil/Koin dependencies to `shared/build.gradle.kts` in the narrowest source sets that compile for Android, iOS, and desktop JVM.
  - Evidence: added Coil/Koin dependencies to `commonMain.dependencies`; later full Android/iOS/JVM verification passed.
- [x] Add a shared RhythHaus Koin module for the existing service graph.
  - Evidence: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt` defines `rhythHausModule()` with existing service graph bindings.
- [x] Add an idempotent shared Koin startup helper.
  - Evidence: `startRhythHausKoin()` uses multiplatform `KoinPlatform.getKoinOrNull()` guard before `startKoin`.
- [x] Initialize Koin from Android, desktop, and iOS entry points before `App()` renders.
  - Evidence: Android `MainActivity`, desktop `main.kt`, and iOS `MainViewController.kt` call `startRhythHausKoin()` before shared UI composition.
- [x] Verify focused shared compilation.
  - Evidence: Task 1 `./gradlew :shared:compileKotlinJvm --configuration-cache` passed; additional `:shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:compileDebugKotlin :shared:compileKotlinIosSimulatorArm64 --configuration-cache` passed after switching from JVM-only `GlobalContext` to `KoinPlatform.getKoinOrNull()`.

## 2. Refactor `App()` to use Koin

- [x] Resolve existing services from Koin inside `App()` instead of constructing them inline.
  - Evidence: `App()` now resolves `PlaybackController`, `TagLibReader`, `LibraryRepository`, `PlatformSourceAccess`, `LibraryScanner`, and `ThemePreferenceStore` using `koinInject`.
- [x] Preserve current service lifetimes and disposal behavior for `PlaybackController`.
  - Evidence: `DisposableEffect(controller) { onDispose { controller.release() } }` remains.
- [x] Keep scanner, repository, folder-picker, theme, and clear-library behavior unchanged.
  - Evidence: Task 2 review approved scan job/progress, repository refresh, clear-library, theme selection, and UI wiring as unchanged.
- [x] Add or update tests for dependency module construction where practical.
  - Evidence: `RhythHausDiTest` resolves `LibraryScanner` from Koin using fake/test-safe dependencies and performs a no-op scan.
- [x] Verify focused shared tests and compilation.
  - Evidence: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed; `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache` passed after a transient Maven Central TLS retry.

## 3. Route artwork surfaces through Coil

- [x] Add a shared Coil-backed artwork image composable/helper with stable artwork cache keys and size roles.
  - Evidence: `ArtworkImage.kt` adds `ArtworkImageRole`, `artworkMemoryCacheKey(bytes, role)`, and `ArtworkImage(...)` using Coil `SubcomposeAsyncImage`, stable memory/disk cache keys, enabled cache policies, and disabled crossfade.
- [x] Update track row, compact now-playing bar, album card, artist row, drill-down top bar, and expanded now-playing artwork call sites to use the helper.
  - Evidence: `LibraryRows.kt`, `LibraryChrome.kt`, `NowPlayingBar.kt`, and `NowPlayingScreen.kt` render artwork through `ArtworkImage`.
- [x] Preserve existing fallback text, selected overlays, content descriptions, shapes, and content scale behavior.
  - Evidence: Task 3 review initially found non-null corrupt artwork fallback regression; fixed by rendering fallback from `SubcomposeAsyncImage` error slot. Re-review approved with no remaining Critical/Important findings.
- [x] Remove obsolete direct artwork decode imports from updated Compose call sites.
  - Evidence: `grep -R "decodeArtworkCached\|decodeArtworkThumbnailCached\|decodeArtwork()" -n shared/src/commonMain/kotlin/com/eterocell/rhythhaus shared/src/commonTest/kotlin || true` shows only `ArtworkDecoder.kt` helper definitions.
- [x] Verify focused UI tests and compilation.
  - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' --configuration-cache` passed; `./gradlew :shared:compileKotlinJvm --configuration-cache` passed. During final Android verification, `coil3.PlatformContext.INSTANCE` failed Android common compilation; fixed to Coil common `LocalPlatformContext.current` and reran full checks successfully.

## 4. Final evidence and handoff

- [x] Run `openspec validate coil-koin-image-di --strict`.
  - Evidence: pass, `Change 'coil-koin-image-di' is valid`.
- [x] Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
  - Evidence: initial run failed in `:shared:compileAndroidMain` because `coil3.PlatformContext.INSTANCE` is unavailable on Android; fixed `ArtworkImage` to use `LocalPlatformContext.current`; rerun passed (`BUILD SUCCESSFUL in 44s`; 99 actionable tasks: 24 executed, 75 up-to-date; existing Android `setArtworkData` deprecation warning only).
- [x] Run `/usr/bin/xcrun xcodebuild -version`.
  - Evidence: pass, `Xcode 26.6`, `Build version 17F113`.
- [x] Run `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.
  - Evidence: pass before and after Coil context fix; final rerun `BUILD SUCCESSFUL in 15s`; 34 actionable tasks: 8 executed, 26 up-to-date; existing `IOSNowPlayingBridgingTest` warnings only.
- [x] Run `git diff --check`.
  - Evidence: pass, no output and exit 0.
- [x] Update this task list with evidence.
  - Evidence: this file records task completion and verification output.
- [x] Update `progress.md` and `roadmap.md` with completion evidence and remaining manual QA.
  - Evidence: final handoff entry added to `progress.md`; roadmap entry added for Coil artwork loading and Koin DI.
- [x] Commit the completed OpenSpec + Superpowers workflow with a semantic commit message unless explicitly blocked.
  - Evidence: this task list is being included in the final semantic commit after staged-diff review.
