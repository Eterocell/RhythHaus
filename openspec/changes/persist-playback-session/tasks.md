# Playback Session Persistence Tasks

## 1. Bounded snapshot codec and store

- [ ] 1.1 Add ID-only snapshot, complete checkpoints, and codec with exact limits `10_000`, `4_096`, `16_384`, and `1_048_576`; exclude effective shuffle order.
- [ ] 1.2 Add codec tests for delimiters/emoji, empty/duplicate/unpaired-surrogate IDs, malformed/truncated/trailing input, exact plus one-over bounds for `maxIds`, `maxIdCharacters`, and `maxEncodedUtf8Bytes`, plus the largest reachable 12,288-byte UTF-8 payload under the defense-in-depth `maxIdUtf8Bytes=16_384` ceiling.
- [ ] 1.3 Add `PlaybackSessionStore` and Android/iOS/JVM factories for `playback_session.preferences_pb`, corruption replacement, all-field atomic edit, malformed-read empty/default, and pre-edit invalid-save preservation.
- [ ] 1.4 Run focused JVM codec/store RED then GREEN tests.

## 2. Generation-safe paused engine contract and transport gate

- [ ] 2.1 Change engines to `loadPaused(track, generation): LoadedPlayback`, `clear(generation)`, generation-tagged callbacks, and `setUserTransportEnabled(enabled)`.
- [ ] 2.2 Adapt controller generation allocation/filtering while preserving existing enabled playback behavior.
- [ ] 2.3 Implement Android request tokens in MediaItems with observable-current-token filtering and paused readiness acknowledgement; modify `RhythHausPlaybackService.kt`, `RhythHausTransportBridge.kt`, and `SkipRoutingPlayer` to use one process-shared transport-enabled gate that removes/rejects relevant play, pause, stop, seek, next, and previous commands through the production service wrapper.
- [ ] 2.4 Implement iOS generation capture/old-source invalidation plus internal pure `IOSRemoteTransportGate` in `PlaybackEngine.ios.kt`; verify disabled play/seek returns command failed without provider action, then succeeds when enabled in `IOSNowPlayingInfoTest.kt` and/or `IOSAudioPlayerBridgeTest.kt`.
- [ ] 2.5 Implement JVM generation capture/old-source invalidation and macOS native transport state exposed by `MacAudioPlayerBridge`. The bridge owns a transport-enabled Boolean independently of its handle; `setTransportEnabled` updates retained/current state, and reset/native-handle creation reapplies retained state before remote registration or acceptance. Modify every remote handler in `nativeInterop/macos/rhythhaus_audio.mm` and prove disabled reset/load, rejected remote play/seek with no native effect, then explicit reenabling in `JvmPlaybackEngineTest.kt` through a testable bridge/helper.
- [ ] 2.6 Add common/JVM/iOS/Android-host tests for stale callbacks, same-track Android token isolation, no playing callback before paused load acknowledgement, existing enabled transport, Android service/bridge production play/seek rejection while disabled, iOS pure remote-gate decisions, and macOS bridge/native remote gate behavior surviving disabled `loadPaused` handle reset.

## 3. Controller restore, checkpoints, and reconciliation

- [ ] 3.1 Add `PlaybackSessionController` and command propagation to engine transport gating.
- [ ] 3.2 Add paused restore sequence, empty paused restore-load failure state, and reconciliation that regenerates runtime shuffle order without persisting it.
- [ ] 3.3 Add immediate and keyed progress checkpoints, including key resets.
- [ ] 3.4 Run controller tests RED then GREEN for gate, paused restore, reconcile branches, and checkpoint behavior.

## 4. FIFO coordinator and failed-safe behavior

- [ ] 4.1 Add one FIFO actor for restore, checkpoint, reconcile, and flush, collapsing only adjacent checkpoints and preserving barriers.
- [ ] 4.2 Save normalized restore state before checkpoint collection; queue reconcile during restore and return only `Applied` or `FailedSafeApplied`.
- [ ] 4.3 Implement corrupt-read, unexpected throwing-read, restore-load, and save-failure behavior. Every restore exit must reenable controller/platform commands and complete its reply; non-corruption read failure applies empty paused state and enters process-lifetime FailedSafe without a hang.
- [ ] 4.4 Run coordinator tests RED then GREEN for ordering, barriers, controlled throwing-read completion, failure results, flush completion, and failed-safe reconcile.

## 5. Process ownership and publication integration

- [ ] 5.1 Start Android context/Koin in `RhythHausApplication`, keep iOS/JVM singleton startup, and remove Compose release/flush ownership.
- [ ] 5.2 Bind singleton controller, store, coordinator, and cancellation-safe `PlaybackProcessLifecycle` in Koin using one mutex-created process-scoped deferred/job attempt that individual waiters cannot cancel/reset.
- [ ] 5.3 Make App scan/remove/clear publish library content after reconciliation completion, including `FailedSafeApplied`.
- [ ] 5.4 Run lifecycle/DI/publication RED then GREEN tests for concurrent/repeated restore-once, a cancelled first waiter followed by a second waiter with one coordinator restore, and reconcile-before-publication.

## 6. Verification and evidence

- [ ] 6.1 Run `openspec validate persist-playback-session --strict`.
- [ ] 6.2 Run `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- [ ] 6.3 Run `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`, recording any exact blocker.
- [ ] 6.4 Run `GIT_MASTER=1 git diff --check`, review all requirements, and update execution evidence.
