# Android Baseline Profiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship generated app-owned Android Baseline Profile rules for returning-user cold startup and the first Library Songs scroll, with reproducible packaging and device evidence.

**Architecture:** A dedicated Android test module runs the same real-app journeys for collection and measurement. The optimized release consumes versioned rules without a device; the benchmark uses a separately signed, R8-minified release-derived target.

**Tech Stack:** AGP 9.5.1, Kotlin 2.4.21, AndroidX Baseline Profile and Macrobenchmark 1.5.0, UI Automator 2.4.1, Activity Compose, ProfileInstaller 1.4.1 or newer compatible resolved version.

**Spec:** [Superpowers design](../specs/2026-11-20-android-baseline-profiles-design.md) and [OpenSpec requirements](../../../openspec/changes/add-android-baseline-profiles/specs/android-baseline-profiles/spec.md).

## Global Constraints

- Do not change SDK, AGP, Kotlin, NDK, app ID, production signing, app ABI, or direct-download/Play packaging policy. Use the repository's `gradle-run` workflow for the Gradle commands below.
- Use the real app, real imported 256-WAV fixture and a disposable test user/device on API 34+. Never clear or replace a personal app install or write its library.
- Only generation and explicitly invoked device tests may use ADB. Check in actual generated rules, never handwritten placeholder rules. No app-generated Startup Profile DEX tuning or CI performance threshold.
- Timings use the same non-debuggable R8 benchmark APK against itself: `Partial(BaselineProfileMode.Require)` versus `None`, at least 10 cold launches and 5 first-scroll iterations per mode. Use a low-end physical device or `aosp_cf_x86_64_phone-userdebug`; label Cuttlefish results as reference results, not physical-device evidence.

## Review Focus

- Release task graph accidentally includes generation, or benchmark signing or shell profiling leaks into production → Task 1 variant and task-graph checks.
- Library shell draws before publication is applied, or onboarding reports from its loading placeholder → Task 2 ready-state and on-device frame checks.
- A clean install, wrong fixture, system dialog, or non-moving fling passes as a benchmark journey → Task 3 negative preflight and scroll checks.
- Dependency-only profile assets look like shipped RhythHaus coverage → Task 4 optimized profile descriptor check.
- Profile-required mode fails to apply the profile, TTID exists without TTFD, or a benchmark tests a different APK → Task 5 run and evidence requirements.

---

### Task 1: Isolate collection, benchmark and shipping targets (OpenSpec 1.1)

**Files:** `settings.gradle.kts`, `gradle/libs.versions.toml`, `androidApp/build.gradle.kts`; create `baselineprofile/build.gradle.kts`.

**Contract:** `:androidApp` consumes the `:baselineprofile` producer. Its optimized benchmark variant and non-minified collection variant use the normal app ID and matching local signing; their manifest enables shell profiling. Shipping release retains its existing signing and manifest. Use a variant-only `BuildConfig.PROFILE_JOURNEYS` flag (default false) to expose test selectors in the local targets.

- [ ] Add the consumer/producer plugin wiring, use `targetProjectPath = ":androidApp"` and self-instrumenting test setup, and reuse the catalog SDK levels. Configure connected generation, `saveInSrc = true`, `automaticGenerationDuringBuild = false`, and release-source output. Add a direct ProfileInstaller dependency only if needed to guarantee the compatible resolved version; confirm its receiver in the benchmark target manifest. Keep production release R8 and split behavior untouched.
- [ ] Run `:androidApp:tasks :baselineprofile:tasks --all` to verify the generation and minified benchmark test tasks. Expected names are `:androidApp:generateReleaseBaselineProfile` and `:baselineprofile:connectedBenchmarkReleaseAndroidTest`; record the observed names in the runbook if AGP exposes different names. Assemble the two local targets and check merged manifests, build configuration, and signing. Run `:androidApp:assembleRelease --dry-run` with no device and verify it schedules no profile generation or device test tasks.

### Task 2: Report meaningful startup and expose selectors (OpenSpec 2.1)

**Files:** `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt`; in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/`, `AppLibraryPublication.kt`, `AppProcessState.kt`, `AppLibraryContentHost.kt`, `library/ui/LibraryRoutes.kt`, and new `AppStartupDrawn.kt`; platform actuals in shared Android, iOS and JVM source sets; in `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/`, `LibraryHomeContent.kt` and `LibraryRows.kt`.

**Contract:** `InitialLibraryPublicationState` carries the authoritative initial publication revision. Library readiness requires the owning UI to have applied that revision or a newer one. Register the Android `ReportDrawnWhen` reporter before the first frame and satisfy it when the actual first-run onboarding page or resolved Library is visible. iOS/JVM receive no startup-reporting side effects. The profiling targets expose `rhythhaus-library-ready`, `rhythhaus-library-songs`, and `rhythhaus-library-songs-list` resource IDs; the list ID appears only for the Songs `LazyColumn`.

- [ ] Add `AppStartupDrawnTest` under `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/`. Assert that eligibility loading, a placeholder home and a terminal publication not yet applied do not permit reporting; assert that a visible first-run page, a visible home with the initial revision applied, and a visible home with a newer revision do. Use terminal empty and recoverable-error publications to pin the readiness behavior without requiring tracks.
- [ ] Propagate `publish(content).revision` into the terminal initial state before exposing it. Use `libraryRevision` as the applied value. Report from the actual onboarding render branch or Library home content rather than the route request or a scan effect. In the Android activity, scope `testTagsAsResourceId` to `BuildConfig.PROFILE_JOURNEYS`; the Android reporter itself runs on ordinary production launches too.
- [ ] Run `:shared:jvmTest --tests '*AppStartupDrawnTest'`. On the profiling target, trace a fresh first-run launch and an ordinary returning launch. Confirm the fully drawn event follows the usable screen, including on empty Library, rather than the loading surface. If the device is unavailable, leave that acceptance open.

### Task 3: Implement the fixture and the two real-app journeys (OpenSpec 3.1)

**Files:** Create `baselineprofile/src/main/kotlin/com/eterocell/rhythhaus/baselineprofile/LibraryJourneys.kt` and `BaselineProfileGenerator.kt`; add `baselineprofile/prepare-fixture.py` and `baselineprofile/README.md`.

**Contract:** `MacrobenchmarkScope.launchReadyLibrary()` launches and awaits the ready marker; `MacrobenchmarkScope.prepareSongs()` enters Songs and confirms the prepared first row; `MacrobenchmarkScope.flingSongs()` uses the tagged list and verifies visible fixture titles changed. Use finite waits with an actionable failure message. Use the tested APK's application ID, not the self-instrumenting test package.

- [ ] Generate 256 one-second mono PCM WAVs (`8 kHz`, `16-bit`) named `RhythHausBenchmark-001.wav` through `RhythHausBenchmark-256.wav` in a caller-selected host directory. The runbook documents manual transfer to `Music/RhythHausBenchmark`, SAF import, completion or skipping of onboarding, and waiting for scan completion. Confirm all 256 are in the disposable library before capture; never silently recreate or clear app data.
- [ ] Add `BaselineProfileGenerator.coldStartup()` and `.firstSongsScroll()` using `BaselineProfileRule.collect` with `includeInStartupProfile = false`. Invoke the same setup or journey helpers used by the benchmarks; avoid unrelated UI paths. Use the visible Songs row set before and after the fling to reject a gesture that reveals no new fixture row. Reset to the first row by starting each journey from a stopped process; do not rely on state left by another test.
- [ ] On the disposable target, run the journey with the fixture and record which row becomes visible after the fling. Before import on a clean disposable target, verify that a startup blocked by onboarding or a missing fixture produces a useful failure rather than an accepted scroll. Confirm a blocked or non-moving scroll cannot pass the post-gesture assertion.

### Task 4: Generate and prove the app-owned release artifact (OpenSpec 4.1–4.2)

**Files:** `androidApp/src/release/generated/baselineProfiles/baseline-prof.txt`, `baselineprofile/README.md`.

- [ ] Run `:androidApp:generateReleaseBaselineProfile` against the prepared device; retain per-journey collection output. Confirm the checked-in merged rules have at least one method descriptor reached in startup and one specifically reached by scrolling. Do not commit an app-generated `startup-prof.txt`.
- [ ] Disconnect the device or make it unavailable to Gradle; run `:androidApp:verifyReleaseApks`, `:androidApp:verifyReleaseApks -Prhythhaus.android.splitApk=true`, and `:androidApp:verifyReleaseAab` in separate invocations. Keep APKs separately if a later split build would overwrite the ordinary artifact. Record each task outcome and inspect `assets/dexopt/baseline.prof` and `.profm` in the release APK and the profile assets in the AAB.
- [ ] Decode the binary APK profile with Profgen against its DEX and full matching R8 mapping (`androidApp/build/outputs/mapping/release/mapping.txt`). Record the decoder version/command, APK checksum, and decoded descriptor matches for generated startup and scroll rules. An archive listing, nonzero size, or dependency-only matches cannot pass. Document the evidence and regeneration command in the runbook.

### Task 5: Run and report paired release-equivalent measurements (OpenSpec 5.1–5.2)

**Files:** Create `baselineprofile/src/main/kotlin/com/eterocell/rhythhaus/baselineprofile/LibraryMacrobenchmark.kt`; update `baselineprofile/README.md`, `progress.md` and `roadmap.md`.

- [ ] Add four `@Test`s: `coldStartupNone`, `coldStartupWithProfile`, `firstScrollNone`, `firstScrollWithProfile`. Use `StartupTimingMetric()` and `StartupMode.COLD` with 10 iterations for startup. For first scroll, force-stop/start, select Songs and confirm the first fixture row in `setupBlock`; use `FrameTimingMetric()` and five iterations with only the fling and visible-row check in the measurement block. Use `CompilationMode.None()` and `CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require, warmupIterations = 0)`; never fall back to `UseIfAvailable` or suppress device-validity errors.
- [ ] Run the verified minified benchmark connected test task with `-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark` against the same qualifying device and the same locally signed APK for all four tests. Inspect the actual APK's packaged profile and require successful profiled compilation. Confirm every startup iteration has both TTID and TTFD; reject missing TTFD or any failed scroll assertion. Retain JSON and Perfetto traces outside version control.
- [ ] Record commit, device model, API and build, APK and rule-file hashes, sample counts, TTID and TTFD medians, frame CPU duration and frame overrun P50/P90/P95/P99, and artifact locations. Label the result as merged Baseline Profile versus no AOT, not app-owned rules versus dependency profiles alone. Report slower or inconclusive results as measured. If any evidence is blocked, record the exact command/error and leave the corresponding OpenSpec box unchecked.
- [ ] Update `progress.md`, `roadmap.md`, and OpenSpec with accepted evidence only. Review and commit the completed change using the repository's selective commit workflow.
