## Why

RhythHaus's R8 release APK already carries a compiled Baseline Profile, but the repository has no way to generate app-owned rules for cold startup and Library scrolling or measure their impact. The Compose performance review needs a repeatable comparison before attributing a runtime improvement to profiling.

## What Changes

- Add an opt-in `:baselineprofile` instrumentation module targeting `:androidApp`, with a fixed, disposable audio catalog and shared startup and first-scroll journeys.
- Generate and check in app-owned Baseline Profile rules for both journeys. Package them in the normal release without making normal builds run device tests.
- Mark meaningful Android startup readiness for time-to-full-display and expose stable selectors for the Library scroll journey.
- Verify app-owned profile rules reach the optimized release APK. Provide startup and scroll benchmarks for both profile-required and uncompiled modes on the same qualifying test device. The user waived timing execution for this change.

## Capabilities

### New Capabilities

- `android-baseline-profiles`: Reproducible app-owned Android profile generation, release packaging and on-device measurement.

### Modified Capabilities

None. Android release identity, signing, R8 and the ABI artifact contract remain unchanged.

## Impact

- Android Gradle wiring, a new test-only module, generated release profile rules, and documentation for the test catalog and run procedure.
- Android startup reporting and nonlocalized Compose test selectors; no UI redesign, persistence schema change, production data seeding, or change to iOS or desktop behavior.
- No performance uplift is assumed, and no new timing gate is enabled in CI.
