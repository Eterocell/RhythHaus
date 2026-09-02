# Build Quality and CI Repair Design

Date: 2026-09-02
Status: approved in chat (user confirmed 2026-09-02)

## Problem

`qualityCheck` fails because `ArchitectureCheckPlugin` observes only the legacy `com.github.ben-manes.versions` plugin ID, while the root build applies `io.github.ben-manes.versions`. Its identity set for `dependencyUpdatesAggregation` is therefore empty, so the checker interprets generated aggregate edges as authored dependencies and reports a root self-cycle plus forbidden edges.

The dependency catalog resolves Compose Multiplatform 1.12.0 and Skiko 0.150.1 while Miuix 0.9.3 requests older Compose artifacts. JVM Compose rendering through Miuix Squircle therefore throws `NoSuchMethodError` for `Image.makeShader`.

GitHub CI currently validates the root quality gate and JVM tests but does not independently build the Android app, desktop application, or linked Xcode iOS app.

## Goals

- Make `qualityCheck` pass for a valid repository while retaining normal architecture-edge enforcement.
- Update Miuix to user-selected `0.9.4-rc01` without changing Compose Multiplatform 1.12.0 or forcing Skiko versions.
- Run architecture, ktfmt-backed Spotless, and Detekt as separately visible CI validations.
- Keep the JVM test job and add build-only Android, desktop, and iOS app jobs.
- Build iOS through Xcode's unsigned Simulator target, covering the Swift wrapper and Kotlin framework linkage.

## Non-Goals

- No application behavior, public API, database schema, dependency-graph policy, or platform-support changes.
- No Android Host Test or iOS Simulator Test task in CI.
- No resolution-strategy workaround or unrelated dependency/toolchain upgrade.

## Design

### Architecture aggregate filtering

The checker retains identity-based filtering for the Versions plugin aggregate configuration. It registers the existing capture logic for both `io.github.ben-manes.versions` and `com.github.ben-manes.versions`, then filters only exact captured objects. This recognizes the production plugin ID without allowing configuration-name-based suppression.

A TestKit regression will use the production plugin ID and verify a clean architecture task; the existing legacy-ID fixture remains compatibility coverage. A paired forbidden ordinary dependency stays negative, proving that the fix does not discard authored violations. Existing KSP and Android synthetic-self identity paths stay unchanged.

### Miuix compatibility

The catalog's Miuix coordinate changes from `0.9.3` to `0.9.4-rc01`. Compose Multiplatform stays at `1.12.0`; no direct Skiko dependency or version force is introduced. Existing production-composable `:core:ui:jvmTest` and `:feature:search:jvmTest` cases become the regression proof for the `makeShader` linkage failure.

### CI build matrix

The workflow keeps the current JVM test job. The quality job invokes, independently, `architectureCheck`, `spotlessCheck`, and `detekt`.

Three build-only jobs run on macOS:

- Android: `:androidApp:assembleDebug`.
- Desktop: `:desktopApp:compileKotlin`.
- iOS: `xcodebuild` of the `iosApp` Simulator app with code signing disabled.

The jobs use the current Java 21 and Gradle setup conventions. None selects Android Host Test or iOS Simulator Test tasks. Separating jobs makes each platform's failure directly attributable.

## Acceptance Criteria

1. A current-ID Versions aggregate dependency no longer causes `ARCH-CYCLE` or `ARCH-EDGE`; an ordinary forbidden project dependency still does.
2. `qualityCheck`, `spotlessCheck`, and `detekt` pass after the architecture fix.
3. The former Miuix rendering failure does not occur in `:core:ui:jvmTest` or `:feature:search:jvmTest`.
4. CI's workflow contains explicit static-quality, Android-build, desktop-build, and Xcode iOS-app-build coverage; it contains no Android Host Test or iOS Simulator Test invocation.
5. The full existing JVM test battery, Android debug assembly, desktop compilation, and Xcode unsigned Simulator build pass.

## Risks

Miuix `0.9.4-rc01` is pre-release software. The scope is intentionally limited to the compatibility repair and validated against the affected real rendering tests and supported-platform builds. The Gradle public model cannot assign provenance to equal project dependencies; the existing exact-identity filter plus authored-aggregate negative regression remain the boundary, and recognizing the current plugin ID does not affect ordinary module dependencies.

## OpenSpec

This design is implemented by `openspec/changes/repair-build-quality-ci/`. Its durable behavior contracts are the architecture-governance-gates delta and the continuous-integration-build-gates capability.
