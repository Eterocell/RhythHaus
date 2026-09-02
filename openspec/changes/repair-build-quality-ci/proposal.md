## Why

The repository's mandatory quality gate currently treats project dependencies injected by the Ben Manes Versions aggregate as authored architecture edges, so `qualityCheck` always fails. The architecture plugin listens only for the legacy Versions plugin ID while the root build applies the current ID, leaving its identity exclusion set empty. In parallel, Miuix 0.9.3 is binary-incompatible with the resolved Compose Multiplatform 1.12.0/Skiko runtime, preventing JVM UI tests from rendering. CI also lacks build-only validation of the Android, desktop, and linked iOS app deliverables.

## What Changes

- Observe both the current and legacy Ben Manes Versions plugin IDs, then exclude only the actual `dependencyUpdatesAggregation` project-dependency instances, preserving architecture checks for authored edges, KSP registrations, and Android synthetic self-dependencies.
- Upgrade Miuix from 0.9.3 to 0.9.4-rc01 while retaining Compose Multiplatform 1.12.0.
- Make CI run architecture, ktfmt-backed Spotless, and Detekt gates explicitly; retain the existing JVM test job.
- Add Android debug assembly, desktop Kotlin compilation, and a no-signing iOS Simulator app build as separate CI jobs.
- Do not add Android Host Test or iOS Simulator Test execution to CI.

## Capabilities

### New Capabilities
- `continuous-integration-build-gates`: CI validates all supported platform deliverables without executing platform test suites.

### Modified Capabilities
- `architecture-governance-gates`: The graph collector ignores only plugin-owned dependency-update aggregate edges captured at the correct Gradle lifecycle point.

## Impact

- Build logic: `ArchitectureCheckPlugin` and its TestKit functional tests.
- Dependency management: `gradle/libs.versions.toml`.
- CI: `.github/workflows/quality.yml`.
- Documentation and tracking: OpenSpec change artifacts, Superpowers design and implementation-plan records, `progress.md`, and `roadmap.md` when implementation is accepted.
- No application feature, database schema, public API, or platform-test scope changes.
