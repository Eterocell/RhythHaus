# Sleep Timer Implementation Plan

> REQUIRED SUB-SKILL: use subagent-driven-development or executing-plans. Implement in dependency order with failing-before/passing-after evidence and independent review at each stable checkpoint.

**Goal:** Timed or natural-completion sleep timer with optional reversible ten-second fade, accessible Now Playing control, and Android/iOS/macOS engine gain.

**Architecture:** Controller-owned monotonic deadline/completion count and token/generation-safe stop; all platform gain paths implement a required `PlatformPlaybackEngine` contract; Shared adapts timer state/actions to feature-owned Now Playing UI. No database or persistent timer state.

**Stack:** Kotlin Multiplatform, coroutines/StateFlow, Media3, Swift AVAudioPlayer, macOS JNI/Objective-C++, Compose resources and semantics.

**Spec:** `openspec/changes/sleep-timer/specs/sleep-timer/spec.md`; design at `openspec/changes/sleep-timer/design.md`.

## Task 1 — Controller timer and required gain contract

**Files:** `core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`; new controller-owned timer model/logic if appropriate; `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`; fake engine implementations in existing core tests.

1. Write RED controller tests for timed expiry (including pause/load), arm/replace/cancel, queue clear/release, 1/N natural completions and RepeatOne precedence, manual skip/retry/stale and duplicate generation, unchanged queue/modes/history inputs, gain before timed/final-track stop and restoration. Use controllable clock/scheduler or short bounded transitions with deterministic synchronization rather than unbounded sleeps.
2. Add required platform playback gain method and the controller timer state/commands. Guard deadline, fade, and completion claims with timer token plus authoritative generation; stop once, reject stale engine work, and reset gain before subsequent media. Avoid locks around engine calls. Keep existing `stop()` transport semantics unchanged except timer-driven call path.
3. Migrate every existing test fake/adapter to the required gain contract; no silent no-op compatibility default. Run focused core tests and commit `feat(playback): add authoritative sleep timer` only after GREEN. Independently review stop/load and completion races.

## Task 2 — Platform gain adapters

**Files:** `core/playback/src/androidMain/.../PlaybackEngine.android.kt`, `RhythHausPlaybackService.kt`, Android host tests; `core/playback/src/iosMain/.../PlaybackEngine.ios.kt`, `IOSAudioPlayerBridge.kt`, iOS tests and `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift`; `core/playback/src/jvmMain/.../PlaybackEngine.jvm.kt`, `core/playback/src/nativeInterop/macos/rhythhaus_audio.mm`, JVM/macOS native bridge tests.

1. Android: add RED service-level test for ExoPlayer gain; implement service-controlled gain via Media3 player/controller command. Reapply gain across async service connection/load and restore unity on cancellation/release. Never alter global device volume.
2. iOS: add RED Kotlin bridge and Swift host tests for Swift AVAudioPlayer gain, track replacement, and restored unity. Adapt Kotlin-to-Swift provider ABI; all mutation stays on the serialized main-thread path. Keep existing short teardown fade untouched.
3. macOS: add RED native/JVM bridge tests for AVAudioPlayer gain and disposal; expose JNI method under handle lock and propagate controller gain through load replacement. Check header/signature consistency across Kotlin and Objective-C++.
4. Run focused platform checks once edits settle, commit adapter changes, and independently review native ABI, real volume target, background behavior and reset semantics.

## Task 3 — Shared and Now Playing integration

**Files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt`; `feature/nowplaying/src/commonMain/.../NowPlayingContent.kt` and EN/ZH `composeResources`; corresponding JVM semantic/resource tests.

1. Write RED feature tests for timer mode choices, arm/replace/cancel callbacks, fade toggle, idle/active semantic announcement, and scroll reachability at 600×400 dp, both compact and split. Add Shared route test proving state/action forwarding from the actual controller rather than a mock-echo.
2. Add feature-owned timer panel and resource labels; Shared collects timer state and forwards narrow callbacks. Preserve Back and collapsed-overlay behavior, existing transport accessibility, and independent playback state.
3. Run focused Now Playing/Shared JVM tests; commit and independently review both production routes and accessibility/localization ownership.

## Task 4 — Cross-platform verification and closeout

1. Run core playback, Now Playing, Shared focused tests, Android host/assemble when toolchain permits, iOS Simulator test-code compilation and Swift host tests, macOS JVM/native smoke with a disposable fixture; exercise timer expiration and post-cancel gain on a real supported runtime. Report exact blocked commands separately.
2. Run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, `openspec validate sleep-timer --strict`, and `git diff --check` as independent gates; request an independent stable-revision full-branch review and repair findings.
3. Sync `openspec/specs/sleep-timer/spec.md`, mark proven tasks, archive only after the automated acceptance and review gates. Update `roadmap.md` and `progress.md` with evidence, manual device scenarios owned by user as pending, and no false full-suite or Android build claims. Commit with conventional message and integrate only after conscious review of known baseline blockers.
