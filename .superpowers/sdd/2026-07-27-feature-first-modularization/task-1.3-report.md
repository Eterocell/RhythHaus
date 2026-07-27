# Task 1.3 Report: Stable Architecture Checker

## Scope

Implemented the approved two-layer contract only. `architectureCheck` reads public Gradle/KGP/AGP model facts for project edges, resources, SQLDelight ownership, KMP strict explicit API, and iOS exports. Normal JVM `:architecture-processor` reports semantic package-root and public-KDoc failures during production compilation. Core/API conventions apply it only to production KSP configurations when the root includes the processor; transitional `:shared` has no source-policy gate.

Excluded: lexical/token/import-provider behavior, compiler internals/Analysis API, KSP task/output facts in `architectureCheck`, root `check`/`qualityCheck`/CI wiring, application source, toolchain/publication changes, and Task 1.4.

## RED And GREEN

- RED command: `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache`.
- RED result: expected failure before implementation. The new package-root and public-KDoc production-compilation fixtures both returned `UnexpectedBuildSuccess`, proving processor/wiring were absent.
- GREEN: the same focused TestKit/KSP command passed after implementation (12 tests), including the invalid package-root and missing-public-KDoc production diagnostics.

## Verification

- `./gradlew :build-logic:convention:test --configuration-cache`: pass (62 tests).
- `./gradlew :architecture-processor:compileKotlin --configuration-cache`: pass.
- `./gradlew :architecture-processor:jar --configuration-cache`: pass.
- `./gradlew architectureCheck --configuration-cache`: pass; immediate second run reused configuration cache.
- `./gradlew spotlessApply --configuration-cache`, separate `spotlessCheck`, and separate `detekt`: pass.
- `openspec validate feature-first-modularization --strict`: pass (`Change 'feature-first-modularization' is valid`).
- `git diff --check`: pass.

## Changed Files

- `settings.gradle.kts`: includes `:architecture-processor`.
- `architecture-processor/`: JVM KSP processor build, provider, and Java SPI descriptor.
- `gradle/libs.versions.toml` and `build-logic/convention/build.gradle.kts`: KSP aliases/API.
- Core/API convention scripts: narrow production consumer wiring.
- `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/`: public-model-only checker and corrected allow-list.
- `ArchitectureCheckPluginFunctionalTest.kt`: RED-first KSP integration and public-model fixture coverage.
- OpenSpec/progress/roadmap: submitted-for-review status; 2.1/2.3 remain unchecked and 2.4 is untouched.

## Self-Review, Risks, And Commit

The checker does not parse build scripts or Kotlin source and does not claim import-provider enforcement. The sole processor artifact is `:architecture-processor`, never the convention JAR. Existing standalone convention fixtures remain supported because KSP activates only in roots containing that project. No current application module applies the core/API conventions, so production processor behavior is proven through compilation fixtures. Independent review remains required before acceptance.

Commit: pending final diff review; required message is `build: enforce architecture dependency gates`.
