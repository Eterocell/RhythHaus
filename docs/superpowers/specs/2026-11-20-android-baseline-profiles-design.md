# Android Baseline Profiles Design

## Intent

Ship app-owned profile rules for RhythHaus cold startup and the first Library Songs scroll, then measure what ahead-of-time compilation changes. The current R8 release APK contains `baseline.prof` and `baseline.profm`, but there is no app-owned rules file, generator or Macrobenchmark module. Existing profile assets are not evidence that these journeys are covered.

## Approaches

- **Dedicated AndroidX generator and Macrobenchmark module — selected.** Reuse journeys across generation and measurement, keep profiling separate from production signing and device-free building, and check in the generated input for each release build.
- **One-off capture.** Faster initial setup, but not repeatable when startup or the Library changes, and does not establish a controlled timing comparison.
- **Generation during every release build.** Keeps rules fresh but makes distribution builds depend on device state. Retain an explicit generation command instead.

## Build and release boundaries

Add `:baselineprofile` as an Android-only `com.android.test` module targeting `:androidApp`. Use the catalog for the AndroidX Baseline Profile plugin and test dependencies. Start with stable AndroidX Benchmark/plugin `1.5.0`, UI Automator `2.4.1`, and ProfileInstaller `1.4.1` or the already resolved newer compatible version. Verify producer/consumer variant wiring against the existing AGP `9.5.1` and its built-in Kotlin convention; do not copy a legacy Kotlin Android plugin or upgrade AGP to make the sample work.

Use the plugin's non-minified release-derived target for collection and its non-debuggable, R8-minified benchmark release target for measurement. Keep shell profiling and local signing on profiling targets only. No change to shipping application ID, release signing, shrinker configuration, Android SDK levels, TagLib/ABI selection, split APKs, or Play AAB behavior. Both local targets retain the app ID and use the same local signing key; run them only on a disposable device or test user with no personal RhythHaus install. Read the target ID from the tested artifact. Normal builds consume checked-in rules from `androidApp/src/release/generated/baselineProfiles/` without connecting to ADB. Do not introduce app-generated Startup Profile DEX-layout rules in this change: the measured experiment is ART compilation of a fixed APK, not DEX-layout tuning.

## Fixture and journeys

The harness owns a fixture of 256 short generated WAVs with stable, unique names `RhythHausBenchmark-001.wav` through `RhythHausBenchmark-256.wav`. Prepare the files in a dedicated `Music/RhythHausBenchmark` folder on the dedicated test user/device. Install a profiling target, resolve any system prompt and complete or skip onboarding, import the fixture folder through RhythHaus's normal folder picker, and wait for the scan to finish. Preserve the application data on replacement with the same locally signed variant; use API 34+ for the documented workflow. If the fixture or permission is lost, fail preflight and re-prepare it before collecting. Do not seed the production database, bypass onboarding with a production intent, clear an existing install, or depend on a user's library.

- **Cold startup:** launch from a stopped process through the launcher; wait for the actual startup screen to be usable. For this fixture the expected screen is Library; onboarding and permission handling are outside measurement. Record the normal cold-start TTID and TTFD for this returning-user journey, not first-install or first-scan latency.
- **First Library scroll:** launch to a ready Library, choose Songs by resource ID (the default is Albums), and confirm the fixture catalog and first item. Setup and navigation are outside the measured scroll block. Apply a gesture margin and fling the Songs list; a changed visible fixture row is required. The generator runs both journeys; test selectors are locale-independent and measurements do not substitute an empty list or idle screen.

Android reports a meaningful frame with Activity Compose `ReportDrawnWhen`: a visible first-run onboarding page or usable Library content after terminal initial publication has been applied to the UI, including legitimate empty or error states. An onboarding loading surface or the shell's initial empty projection is not fully drawn. Use the production publication state and actual route, not a delay or a nonempty-track predicate. This is Android-only reporting; it must not delay UI rendering or change any destination. Expose the ready marker, Songs control and list through narrowly scoped Compose test tags surfaced as resource IDs in profiling variants.

## Acceptance evidence

1. Generate rules from both verified journeys with `:androidApp:generateReleaseBaselineProfile` and check in the nonempty file. Retain app-owned descriptors from startup and scroll as provenance.
2. Build the normal release with no device task. Inspect `assets/dexopt/baseline.prof` and `assets/dexopt/baseline.profm`, and decode the binary profile against the APK DEX and R8 mapping to identify app-owned generated rules. A dependency-only profile must not pass this gate. Confirm APK/AAB release identity and ABI contracts are unchanged.
3. On the same low-end physical device or `aosp_cf_x86_64_phone-userdebug` reference, run both compilation modes (`Partial(BaselineProfileMode.Require)` and `None`) against the same R8 benchmark APK and catalog. Record device/API/build, APK and profile input identity, and the raw Macrobenchmark JSON/traces. Use at least 10 cold launches per mode and five first-scroll measurements per mode. Report TTID and TTFD medians and scroll P50/P90/P95/P99 `frameDurationCpuMs` and `frameOverrunMs`. This comparison measures the merged profile, including dependency rules, versus no AOT; it does not isolate the performance contribution of app-owned rules. Record slower or inconclusive results as observed; do not require or assume a speedup.

## Risks and non-goals

- A permission sheet, stale catalog, non-moving fling or absent TTFD is invalid evidence, not a zero-valued result. No suppressed device validity errors count toward the performance comparison.
- No profiling on debug builds, broad Compose fixes, CI timing thresholds, new UI, or changes to desktop/iOS behavior.
- No connected Android device was available during planning. Host setup/packaging proof cannot replace collection and timing evidence.

References: [Android Baseline Profile generation](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile), [Baseline Profile measurement](https://developer.android.com/topic/performance/baselineprofiles/measure-baselineprofile), [AndroidX Benchmark releases](https://developer.android.com/jetpack/androidx/releases/benchmark).
