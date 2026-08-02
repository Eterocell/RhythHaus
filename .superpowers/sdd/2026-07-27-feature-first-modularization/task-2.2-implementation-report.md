# Task 2.2 Implementation Report

## Scope

Created `:core:ui` and moved the approved package-stable generic UI boundary without changing Kotlin package names. `:shared` depends on it through `api(projects.core.ui)` and iOS exports remain unchanged.

## Failing-First Evidence

1. Baseline: `git status --short` recorded the pre-existing modified plan file.
2. `./gradlew :core:ui:allTests --configuration-cache` failed as expected before module creation: `Project 'core:ui' not found in root project 'RhythHaus'.`
3. After module/test skeleton creation and before production moves, the narrow core test compile failed as expected on unresolved moved symbols/resources, including `ArtworkImageRole`, `ArtworkImage`, theme/dialog primitives, and controlled core resource accessors.

## Implementation

Moved to `:core:ui`:

- Generic artwork: `ArtworkImageRole`, `artworkMemoryCacheKey`, `ArtworkImage`, and the expect/actual decoder/cache family for Android, JVM, and iOS.
- Theme/UI: `Theme.kt`, `HausColors.kt`, `HausClickable.kt`, `BackChip.kt`, `RhythHausTopAppBar.kt`, and the full `HausDialog`/`HausLazyDialog` family.
- Resources: English and Chinese `back` and `back_button` only, generated under `rhythhaus.core.ui.generated.resources` with public resource accessors for consumers.
- Tests: existing artwork cache, generic artwork key, theme, and dialog contracts; existing dialog JVM semantics; focused JVM `BackChip` and top-app-bar contracts.

Retained in `:shared`:

- `LocalTrackArtworkLoader`, `TrackArtworkLoadState`, initial/load/remember lazy state helpers, and `LazyTrackArtworkImage`.
- All existing track artwork loading, eager/lazy, failure, cancellation, and absence contracts.
- Added focused shared cancellation characterization coverage proving the exact loader `CancellationException` instance is rethrown.
- The retained source was renamed from `ArtworkImage.kt` to `TrackArtworkImage.kt` only to avoid two modules publishing the same `ArtworkImageKt` JVM file facade; package and Kotlin symbols are unchanged.

`LibraryChrome.kt` now references the controlled core `back` resource, the only required surviving consumer update.

## Green Verification

### Task 2.2 Follow-up Corrections

- Pre-change source evidence: `core/ui/build.gradle.kts` declared Compose Foundation and Compose Resources as `implementation`; the review correctly identified that their public types/accessors therefore lacked an API dependency declaration. `jvmApiElements` reported `No dependencies` both before and after the correction, so it is not used as evidence for this KMP common dependency boundary.
- Changed only `libs.compose.foundation` and `libs.compose.components.resources` from `implementation` to `api`; Miuix, Coil, Material, and icons remain implementation dependencies.
- Navigation bounds/title tests are characterization-first GREEN: production already provided the 44 dp BackChip/top-app-bar targets and existing English label, content-description, click, title, and null behavior were preserved.
- Cancellation test is characterization-first GREEN: production already rethrows the exact `CancellationException` instance; the added test records that contract without production changes.
- Post-change API evidence: `./gradlew :core:ui:dependencies --configuration jvmApiElements --configuration-cache` - PASS, but this KMP JVM publication projection still reports `No dependencies`; it is not sufficient to classify common API ownership. The precise supported alternative, `./gradlew :core:ui:dependencies --configuration commonMainApi --configuration-cache`, - PASS and lists Compose Foundation and Compose Resources alongside Runtime and UI. The exact pre/post commands and their outputs are retained here rather than treating `jvmApiElements` as a dependency report.
- Downstream compile: `./gradlew :core:ui:compileKotlinJvm :shared:compileKotlinJvm --configuration-cache` - PASS.

- `./gradlew :core:ui:jvmTest :core:ui:compileKotlinJvm --configuration-cache` - PASS (19 JVM tests).
- `./gradlew :shared:clean :shared:jvmTest --no-build-cache --configuration-cache` - PASS (561 JVM tests). The clean/no-build-cache invocation was required after relocating a same-named JVM file facade.
- `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail` - PASS. No allow-list/build-logic change was necessary; the approved `:shared -> :core:ui` edge is already accepted.
- `./gradlew :core:ui:allTests :core:ui:compileKotlinJvm --configuration-cache` - PASS (Android host, JVM, and iOS simulator); 1m27s.
- Repeated `./gradlew :core:ui:allTests :core:ui:compileKotlinJvm --configuration-cache` - PASS with configuration cache reused; 873ms.
- `git diff --check` - PASS.
- Focused navigation JVM test - PASS, including 44 dp bounds and title assertions.
- Focused shared `ArtworkImageTest` selector - PASS, including exact cancellation identity assertion.

## Changed Paths

- `settings.gradle.kts`
- `core/ui/build.gradle.kts` and all `core/ui/src/**` moved implementation, resource, and test files
- `shared/build.gradle.kts`
- `shared/src/{androidMain,commonMain,iosMain,jvmMain}/kotlin/com/eterocell/rhythhaus/ui/**` moved/split source files
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/**` moved source files
- `shared/src/{commonTest,jvmTest}/kotlin/com/eterocell/rhythhaus/{ui,theme}/**` moved/split tests
- `shared/src/commonMain/composeResources/values/strings.xml`
- `shared/src/commonMain/composeResources/values-zh/strings.xml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- this report

## Blockers And Risks

No blockers. No build outputs are tracked. The approved Task 2.2 plan amendment in `docs/superpowers/plans/2026-07-27-feature-first-modularization.md` predates implementation and was not changed by the implementation writer.
