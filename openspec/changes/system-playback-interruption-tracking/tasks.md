## 1. Establish RED regressions

- [ ] 1.1 Extend `core/playback/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt`'s fake provider with interruption-handler storage and simulation methods; add failing tests for interruption begin, interruption end with and without `shouldResume`, route disconnect, and callback forwarding. Run `./gradlew :core:playback:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache` and retain the RED result before production changes.
- [ ] 1.2 Add macOS route-monitoring RED tests in `core/playback/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt` for listener lifetime, one-shot route-event consumption, route disconnect during playback, and route disconnect while paused. Run `./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache` and retain the unsuppressed RED result.

## 2. Implement iOS interruption and route observation

- [ ] 2.1 Add declaration-specific KDoc'd `IOSAudioInterruptionHandler` and the `IOSAudioPlayerProvider.interruptionHandler` property in `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridge.kt`; preserve the existing completion-handler ABI and Swift framework visibility.
- [ ] 2.2 Update `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift` to track play state, observe `AVAudioSession.interruptionNotification` and `routeChangeNotification`, filter `.began`, `.ended`, and `oldDeviceUnavailable`, forward the new handler callbacks, and remove observer tokens during teardown.
- [ ] 2.3 Update `core/playback/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` to install generation/source-guarded interruption handlers, cancel/restart progress, reactivate the audio session before an allowed resume, publish Now Playing rate and `Paused`/`Playing`, and clear interruption state on source replacement, clear, and release.
- [ ] 2.4 Run `./gradlew :core:playback:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`; verify the new iOS tests are GREEN and no existing iOS bridge tests regress.

## 3. Implement macOS route-loss observation

- [ ] 3.1 Update `core/playback/build.gradle.kts` so `buildMacosAudioHelper` links `-framework CoreAudio`, without changing dependencies, target versions, or toolchains.
- [ ] 3.2 Add Core Audio HAL default-output monitoring to `core/playback/src/nativeInterop/macos/rhythhaus_audio.mm`; track the previously active output device, set an atomic pending flag only when that device becomes unavailable, remove the listener across reset/release lifetime boundaries, and add native test hooks for listener count and event injection.
- [ ] 3.3 Expose `consumeRouteDisconnected()` and the native test hooks through `core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`; add the internal bridge-injection constructor/factory seam without changing the public factory signature.
- [ ] 3.4 Update `MacOSNativePlaybackEngine` in `core/playback/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt` to consume route loss before normal progress publication, stop progress, pause the native player, update Now Playing, and emit exactly one generation-valid `Paused` status without auto-resume.
- [ ] 3.5 Run `./gradlew :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest' --configuration-cache`; verify native listener lifetime, one-shot consumption, active-playback pause, paused-state no-op, and existing native playback tests are GREEN.

## 4. Cross-platform verification and acceptance

- [ ] 4.1 Inspect the complete diff against `proposal.md`, `design.md`, and `specs/playback-interruption-tracking/spec.md`; confirm only the declared platform, test, and lifecycle files are changed and common playback contracts remain untouched.
- [ ] 4.2 Run `./gradlew :core:playback:jvmTest --configuration-cache`, `./gradlew :core:playback:iosSimulatorArm64Test --configuration-cache`, `./gradlew :shared:jvmTest --configuration-cache`, and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`; record exact pass/fail/skip evidence.
- [ ] 4.3 Run `./gradlew spotlessApply --configuration-cache`, then separately `./gradlew spotlessCheck --configuration-cache`, `./gradlew detekt --configuration-cache`, and `./gradlew architectureCheck --configuration-cache`.
- [ ] 4.4 Run `./init.sh`; record any unavailable Xcode/device/runtime checks as blockers and do not claim them as passed.
- [ ] 4.5 Update `progress.md` with the route, changed files, exact verification evidence, and next safe action; update `roadmap.md` with the completed playback-state synchronization entry and any deferred hardware-runtime validation.
