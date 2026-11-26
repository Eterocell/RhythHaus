# Design

## Context

See [proposal.md](proposal.md) for motivation. The current `:androidApp` release is minified and already packages profile assets from the build/dependency graph. Measuring app-owned coverage needs more than checking those assets exist. Library state is published to the UI asynchronously; Albums is the default browse mode.

## Goals / Non-Goals

**Goals:** Deterministic consumer/producer variants, reusable journeys, explicit meaningful-frame reporting, and evidence that the generated app rules are packaged and applied.

**Non-Goals:** First-run/scan benchmarking, automatic device execution in release builds, a DEX-layout experiment, and CI timing gates.

## Decisions

1. A standalone `:baselineprofile` Android test module targets `:androidApp`. The plugin supplies separate collection and measurement targets; explicit generation saves release-source rules. Regular release tasks use those rules without scheduling instrumentation or using profiling signing or manifest settings.
2. A disposable test device/user and a fixed imported WAV folder exercise the actual persistence and rendering path. Reuse the readiness and Songs fling helpers for both generator and benchmarks. Use a real stopped process for cold startup and keep navigation/setup outside scroll timing.
3. Report fully drawn from Android when the visible onboarding or resolved Library screen is meaningful. The Library must use the initial publication already applied to the UI; do not infer readiness from the empty projection or a nonempty row count.
4. Decode the optimized release profile against DEX and the R8 mapping to prove app-owned coverage. Use identical minified benchmark APK, device, and fixture for both ART compilation modes. Record actual measurements or an explicit blocker.

See the [Superpowers design](../../../../docs/superpowers/specs/2026-11-20-android-baseline-profiles-design.md) for fixture, metrics, and distribution details.

## Risks / Trade-offs

- The profile plugin and dedicated test module must resolve the current AGP built-in Kotlin variants; check real tasks rather than treating a template as compatibility evidence.
- The same package is installed with local profiling signing. Only use a disposable test user/device with no personal app data, and preserve the prepared fixture across compatible replacements.
- Existing dependency profile bytes can mask a broken app-owned rules path. Presence and profile digest alone do not prove coverage.
- Profile generation is required. The user waived paired timing for this change; any future timing comparison still requires a qualifying target, not a stock emulator.
