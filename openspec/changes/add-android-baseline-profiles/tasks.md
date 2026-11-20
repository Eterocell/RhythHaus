# Tasks

Execution plan: [Android Baseline Profiles Implementation Plan](../../../docs/superpowers/plans/2026-11-20-android-baseline-profiles.md).

## 1. Build targets

- [x] 1.1 Wire Android test, collection and minified benchmark variants with isolated local signing and release builds that never schedule device work.

## 2. Startup readiness

- [x] 2.1 Report the meaningful Android onboarding or Library frame and expose stable profiling selectors; verify loading, empty and recoverable Library states.

## 3. Reusable journeys

- [x] 3.1 Prepare the disposable fixture and implement cold-start and first-scroll journeys that fail when readiness, fixture or scrolling is absent.

## 4. Profile generation and packaging

- [x] 4.1 Generate and check in app-owned release rules from both verified journeys, with per-journey provenance.
- [x] 4.2 Decode the compiled release profile to verify app-owned coverage and verify ordinary release APK, split APK and AAB contracts without a device.

## 5. Measurement and evidence

- [x] 5.1 Paired on-device timing waived by the user for this change. The release-equivalent Macrobenchmark harness remains available; the [Pixel 7 Pro run and isolated retries](../../../baselineprofile/README.md#pixel-7-pro-benchmark-attempt--2026-11-20) produced no timing samples.
- [x] 5.2 Record raw evidence and reported metrics or exact blockers; update the handoff and roadmap, and commit the accepted change. See the [runbook evidence](../../../baselineprofile/README.md#initial-collection-evidence--2026-11-20).
