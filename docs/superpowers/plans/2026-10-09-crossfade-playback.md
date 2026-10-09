# Crossfade Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver persisted opt-in true dual-player crossfade on Android, iOS and macOS while retaining existing queue, ReplayGain and sleep-stop ownership.

**Architecture:** Platform engines add a staged incoming voice beneath their one system session. The controller admits and promotes a fenced transition; Shared owns durable policy and startup readiness. Native voice operations cannot publish selection changes without controller admission.

**Tech Stack:** Kotlin Multiplatform, Media3, Swift/AVAudioPlayer, existing macOS JNI/Objective-C++, Compose/Miuix, DataStore, Koin. No new dependencies.

**Spec:** `docs/superpowers/specs/2026-10-09-crossfade-playback-design.md`, `openspec/changes/crossfade-playback/design.md`, change specs.

## Global Constraints

- Default Off; persisted choices Off/3/5/8 seconds; durations below 250 ms are ineligible.
- Effective duration is min(configured, half outgoing duration, half incoming duration, outgoing remaining).
- Linear envelopes multiplied by per-track attenuation-only ReplayGain; no system volume/media writes.
- No overlap with active sleep timer, Repeat One, Stop After Current, unknown duration, single occurrence or manual transitions.
- Production Android/iOS/macOS support is required; no silent platform no-op fallback.
- User owns physical listening/system-control acceptance; automation and runtime smoke remain implementation-owned.

## Review Focus

- Preparation finishing after a replacement must not affect the winner: Task 2 fenced completion regression.
- Incoming failure must not corrupt outgoing before promotion or resurrect it afterward: Tasks 2–5.
- Pausing at half-fade must not resume either voice at unity: Tasks 2–5 captured pre-play gains.
- Sleep count/history/checkpoints cannot double count retiring completion: Task 2 production listener tests.
- UI recreation/admitted preference write and delayed startup read cannot diverge gain/persistence: Task 6 actual-Koin tests.

## Task 1: Common overlap capability and policy

**Files:** create `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Crossfade.kt`; create `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/CrossfadePolicyTest.kt`.
**Interfaces:** exact `CrossfadePlaybackEngine` methods and `CrossfadeDuration` described in technical design; produce `effectiveCrossfadeWindowMillis(requestedMillis, outgoingMillis, incomingMillis, remainingMillis): Long?`.
- [ ] Write tests: 5000/20000/4000/5000 yields 2000; remaining 1000 yields 1000; remaining 249 yields null; nonpositive durations yield null; no NaN/out-of-range gain accepted.
- [ ] Run `./gradlew :core:playback:jvmTest --tests '*CrossfadePolicyTest*' --configuration-cache`; capture missing policy RED.
- [ ] Implement pure policy and documented capability; do not mutate existing engine API or factories in this task.
- [ ] Run focused GREEN, review capability consumers, commit `feat: define crossfade voice contract`.

## Task 2: Controller transition authority

**Files:** modify `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`; modify `SleepTimer.kt` only where arming admission is owned; create `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/CrossfadeControllerTest.kt`.
**Interfaces:** consume Task 1; produce `PlaybackController.setCrossfadeDuration(duration: CrossfadeDuration): Unit`. Controller constructor retains existing ABI; native engine capability is inspected without allocating a second engine. Engine ordinary methods target promoted incoming.
- [ ] Write a two-voice behavioral fake with deferred prepare and captured per-voice generation/run/gain/position. Tests assert actual second play while outgoing remains running, effective shuffled successor/duplicate occurrence, exactly one started/checkpoint, and no double advancement on outgoing completion.
- [ ] Run focused RED before production transition writes.
- [ ] Implement gate-owned transition and lazy prepare/tick jobs following technical design. Stage once per candidate/run; gain work stops while paused/ineligible. Promote only incoming Playing; checkpoint incoming position; retire old completion independently of current generation.
- [ ] Add race regressions for blocked preparation/replaced queue, clear/release, seek/manual-next, repeat-mode change, reconciliation removing each member, preparation and incoming failure, late prepare below 250ms, normalization mode switch and sleep timer arming mid-overlap. Ensure old completion does not count against newly armed timer.
- [ ] Run controller GREEN plus existing ReplayGain/sleep/recovery focused regressions. Preserve separately recorded pre-change Core baseline failures rather than weakening assertions.
- [ ] Independent authority review, commit `feat: coordinate crossfade transitions`.

## Task 3: macOS real dual-player adapter

**Files:** modify `core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`, `core/playback/src/nativeInterop/macos/rhythhaus_audio.mm`, existing native header/JNI declarations located by symbol references; modify `core/playback/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`.
**Interfaces:** implement Task 1 on production engine; native handle retains one focus/routes/Now Playing authority with outgoing and incoming AVAudioPlayers. JNI signatures mirror stage/start/set-two-gains/retire/cancel methods; isolate callbacks by captured source/run token.
- [ ] Add disposable WAV native tests proving both positions increase concurrently, unity/zero initial envelopes and half-fade gains before pause/resume; capture failing lack of capability/native overlap.
- [ ] Implement staging without resetting outgoing handle, source-owned callback isolation and retirement; ordinary clear/release drains both players under existing callback lifetime protocol.
- [ ] Test failure, stale completion, manual/system commands, route loss and resource cleanup. Media SHA-256 remains identical.
- [ ] Run `./gradlew :core:playback:jvmTest --tests '*JvmPlaybackEngineTest*' --configuration-cache`; independent native review and commit.

## Task 4: Android service dual-player adapter

**Files:** modify `core/playback/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`, `RhythHausPlaybackService.kt`, existing transport bridge; modify `core/playback/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt` and `RhythHausTransportBridgeTest.kt`.
**Interfaces:** implement Task 1; one session-facing player facade with internal secondary ExoPlayer; existing same-UID engine connection admission remains authoritative. Commands map stage/start/gain/retire to service without exposing another session.
- [ ] Write host regressions for captured generations, internal/external transport admission, logical-current metadata handoff, both-player pause/stop/release and failed preparation retention.
- [ ] Capture focused RED; implement secondary player lifecycle, audio-focus/noisy propagation and pre-play gain; promotion changes system-facing logical media without independent auto-advance.
- [ ] Run `./gradlew :core:playback:testAndroidHostTest :core:playback:compileAndroidMain --configuration-cache`; smoke actual service if a safe device is present, otherwise record physical boundary.
- [ ] Independent Media3 consumer review and commit.

## Task 5: iOS native dual-player adapter

**Files:** modify `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`, `IOSAudioPlayerBridge.kt`, `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift`; modify bridge iosTests and `iosApp/iosAppTests/RhythHausAudioPlayerProviderTests.swift`.
**Interfaces:** implement Task 1. Provider gains stage/start/two-gain/retire/cancel operations with captured voice identity under main-thread serialized ownership. Repository-owned provider consumers migrate together; no no-op unsupported bridge.
- [ ] Write bridge/XCTest temporary WAV tests proving concurrent positions, captured completion identity, unchanged audio bytes, shared AVAudioSession and interrupted/route-loss pause of both voices.
- [ ] Capture RED; implement provider retained voice pair, staged preparation failure cleanup, pre-play gains, remote command routing and retiring callback isolation.
- [ ] Run `./gradlew :core:playback:compileTestKotlinIosSimulatorArm64 :shared:linkDebugFrameworkIosSimulatorArm64 --configuration-cache` then available simulator XCTest scheme for provider tests.
- [ ] Independent Swift consumer/lifetime review and commit.

## Task 6: Persisted policy and Settings cutover

**Files:** create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playback/CrossfadePreferenceStore.kt`, `CrossfadePreferenceOwner.kt`, three platform actual stores under shared playback; modify `RhythHausDi.kt`, `PlaybackProcessLifecycle.kt`, `App.kt`, `LibraryRoutes.kt`, `feature/settings/.../SettingsScreen.kt`, EN/ZH strings and existing resource ledger. Tests in Shared preference/lifecycle and Settings semantics follow existing ReplayGain counterparts.
**Interfaces:** store `selectedDuration: StateFlow<CrossfadeDuration>` and `suspend setDuration(duration)`; owner `state: StateFlow<CrossfadePreferenceState>` (selectedDuration/isBusy/saveFailed), `suspend restore()`, `suspend select(duration)`; neutral Settings enum Off/Seconds3/Seconds5/Seconds8, mandatory selected/busy/error/callback inputs. DI singleton owner restores after normalization and before session readiness.
- [ ] Write real-file corruption/reopen, accepted cancelled-write, failed-write retry, concurrent recreation and delayed-startup actual-Koin regressions.
- [ ] Write EN/ZH radio selected/disabled semantics and exclusive modal tests; capture RED.
- [ ] Implement owner/store/DI/startup ordering and migrate every route/test caller. No Settings dependency on core playback.
- [ ] Run focused Shared/Settings tests and all three Kotlin compilations; independent composition review, commit.

## Task 7: Integrated runtime acceptance and lifecycle

- [ ] Run disposable real native WAV queue via actual controller: two voices overlap, incoming history once, retiring completion cannot advance twice; ReplayGain and timer arming settle safely; SHA-256 unchanged. Remove throwaway smoke scaffolds afterward.
- [ ] Run separate `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck` configuration-cache commands and changed platform/test matrix. Do not claim full baseline suites or APK without success.
- [ ] Review whole production branch against specs and prior defects; repair and reverify all concrete findings.
- [ ] Update canonical specs, OpenSpec tasks, roadmap/progress evidence and user-owned physical boundaries. Strict OpenSpec validation and archive after reviewed execution evidence.
- [ ] Commit final scoped changes; do not merge/push until explicitly requested.
