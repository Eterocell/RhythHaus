## Task 4.2: Extract Core Playback Contracts

**Scope:** Slice 4 playback. Create and register `:core:playback` without changing
package names, controller/engine/session behavior, Android service identity, Swift-facing
symbols, JNI symbols, native artifact names, or resource paths. `:shared` remains the sole
composition root and Xcode framework. The earlier Superpowers 4.4 wording remains separate;
OpenSpec item 5.3 is this extraction and item 5.4 is broader verification.

**Production inventory and ownership:** Move
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt` to
`core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`, keeping
`FakePlaybackEngine` unchanged in that production file for compatibility and whole-file/test
consumption. Do not move, duplicate, or redefine `PlayableTrack` or `AudioSource`: consume
both from `:core:model`. Move
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionController.kt`
and `PlaybackSessionSnapshot.kt` as complete files, including the behavioral
`PlaybackSessionController` port, `RevisionedPlaybackSessionSnapshot`,
`PlaybackSessionSnapshot`, `SessionQueueEntry`, `PlaybackSessionCodec`,
`PlaybackCheckpoint`, `ProgressCheckpointKey`, and normalization/value invariants; make the
port and types/methods shared composition consumes public. Move platform engine and
dispatcher files into matching core source sets:
`shared/src/androidMain/kotlin/com/eterocell/rhythhaus/{PlaybackEngine.android,PlaybackDispatchers.android}.kt`,
`shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/{PlaybackEngine.jvm,PlaybackDispatchers.jvm}.kt`,
and `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/{PlaybackEngine.ios,PlaybackDispatchers.ios}.kt`.
Move Android `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausPlaybackService.kt`
and `RhythHausTransportBridge.kt`, iOS
`shared/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridge.kt` and
`NowPlayingArtworkBridge.kt`, JVM/macOS engine/bridge files, and native
`shared/src/nativeInterop/macos/rhythhaus_audio.mm` with their playback
resource/build-task wiring from shared to core. Preserve no cinterop; the existing
`clang++` `Exec` task remains, while shared retains unrelated build wiring. Preserve manifest-relative
`.RhythHausPlaybackService`, FQCN `com.eterocell.rhythhaus.RhythHausPlaybackService`,
transport FQCN `com.eterocell.rhythhaus.RhythHausTransportBridge`,
`MacAudioPlayerBridge`, JNI exports
`Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_*`,
`librhythhaus_audio.dylib`, and resource roots `/native/macos-aarch64/` and
`/native/macos-x64/`.

**Shared retention and construction seam:** Keep
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`
(including reconciler/result/phase), `PlaybackSessionStore.kt`,
`shared/src/{androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.{android,jvm,ios}.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackProcessLifecycle.kt`,
`Logger.kt`, `di/RhythHausDi.kt`, DataStore adapters, Koin composition, App/root
orchestration, artwork-loader composition, and the `LibraryTrack` adapter in shared. Split
the package-stable `createPlatformPlaybackEngine()` `expect`/`actual` family out of the
otherwise moved `Playback.kt` into explicit new
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlatformPlaybackEngineFactory.kt` and
matching `PlatformPlaybackEngineFactory.{android,jvm,ios}.kt` facade files. It is solely a
composition facade: its Android actual delegates to public core
`createAndroidPlaybackEngine()`, JVM actual delegates to
`createJvmPlaybackEngine()`, and iOS actual delegates to
`createIOSPlaybackEngine(IOSRelativeFilePathResolver)`. Shared supplies
`IOSRelativeFilePathResolver` with `appLocalMusicFolderPath()` only for relative
`AudioSource.FilePath`; core handles absolute paths, URIs, and unsupported descriptors.
Engine implementation classes and helpers remain private/internal where current consumers
permit. `PlaybackController` requires an explicit
`PlatformPlaybackEngine` and has no default shared factory. Shared Koin calls its facade,
injects one engine into one controller, and proves `PlaybackSessionController` resolves to
that exact controller singleton; core never calls shared.

**Public API, explicit API, and KDoc inventory:** Preserve and document the common
cross-module surface in `Playback.kt`: `PlaybackStatus.{Idle,Loading,Buffering,Playing,Paused,Stopped,Error}`;
`RepeatMode.{RepeatOne,RepeatPlaylist,StopAfterCurrent,StopAfterQueue}`;
`ShuffleMode.{Off,On}`; `PlaybackError.{message,cause}`; `QueueOccurrence.{id,track}`;
`QueueMutationResult.Applied`; `QueueMutationResult.Rejected.reason`;
`QueueMutationRejection.{CurrentOccurrence,StaleOccurrence,InvalidTargetIndex,CommandsDisabled}`;
and `PlaybackState.{currentOccurrenceId,queue,status,positionMillis,durationMillis,repeatMode,shuffleMode,error,currentOccurrence,currentTrack,canPlay,isPlaying,progressFraction}`.
Do not expose internal `checkpointRevision`. Preserve `LoadedPlayback.{generation,durationMillis}`
and every `PlaybackEngineListener` callback: `onPlaybackStatus`, `onPlaybackProgress`,
`onPlaybackCompleted`, `onPlaybackError`, `onSkipToNext`, and `onSkipToPrevious`. Preserve
every `PlatformPlaybackEngine` member: `listener`, `loadPaused`, `clear`,
`setUserTransportEnabled`, `play`, `pause`, `stop`, `seekTo`, and `release`.

`PlaybackController` retains its explicit `PlatformPlaybackEngine` constructor boundary and
every public member: `state`, `checkpoints`, `setQueue`, `setOccurrenceQueue`, `selectTrack`,
`selectOccurrence`, `setRepeatMode`, `cycleRepeatMode`, `setShuffleMode`,
`toggleShuffleMode`, `play`, `pause`, `stop`, `seekTo`, `togglePlayPause`,
`restartCurrentTrack`, `skipToNext`, `skipToPrevious`, `reorderUpcoming`, `removeUpcoming`,
`clearUpcoming`, `release`, `setCommandsEnabled`, `sessionSnapshot`,
`awaitCheckpointFence`, `restoreSession`, and `reconcileSession`, plus its six public
`PlaybackEngineListener` overrides named above. Public production `FakePlaybackEngine`
remains unchanged with `listener`, `released`, all nine `PlatformPlaybackEngine`
members/overrides, `fail`, `complete`, and `activeGenerationForTest`. It stays
production/public. `playbackEngineDispatcher` remains internal.

Document public `createAndroidPlaybackEngine`, `createJvmPlaybackEngine`, and
`createIOSPlaybackEngine(IOSRelativeFilePathResolver)`. The retained shared
`createPlatformPlaybackEngine` facade remains outside the core public-KDoc gate, while its
package and signature remain stable. `PlaybackSessionController` is currently internal but
becomes public and documented as the cross-module behavioral port, with `checkpoints`,
`sessionSnapshot`, `restoreSession`, `reconcileSession`, `awaitCheckpointFence`, and
`setCommandsEnabled`. Preserve/document `RevisionedPlaybackSessionSnapshot.{snapshot,revision}`;
`PlaybackSessionSnapshot` primary properties
`{queue,currentOccurrenceId,positionMillis,repeatMode,shuffleMode}`, legacy constructor
parameters `{queueIds,currentTrackId,positionMillis,repeatMode,shuffleMode,legacyTrackIds}`,
and derived `{queueIds,currentTrackId}`; `SessionQueueEntry.{occurrenceId,trackId}`;
`PlaybackSessionCodec` constants `{maxIds,maxIdCharacters,maxIdUtf8Bytes,maxEncodedUtf8Bytes}`
and functions `{encodeSnapshot,decodeSnapshot,encodeQueue,decodeQueue,encodeIds,decodeIds}`;
`PlaybackCheckpoint.{snapshot,revision}`; `Immediate.{snapshot,revision}`;
`PlayingProgress.{key,snapshot,revision}`; and
`ProgressCheckpointKey.{generation,currentOccurrenceId,secondBucket}`.

On Android, document `setRhythHausAndroidContext`, `RhythHausPlaybackService`, its public
overrides `onCreate`, `onGetSession`, `onTaskRemoved`, and `onDestroy`, and
`createAndroidPlaybackEngine`; retain the current service FQCN and manifest. Transport,
controller, token, and request helpers remain internal/private and are not widened. Preserve
and document the iOS Swift surface:
`IOSAudioPlayerCompletionHandler.onPlaybackCompleted`;
`IOSAudioPlayerProvider.{completionHandler,load,play,pause,stop,seekTo,currentPositionMillis,currentDurationMillis,isPlaying,fadeOutAndStop}`;
`IOSAudioPlayerBridge.provider`; `NowPlayingArtworkProvider.setArtwork`;
`NowPlayingArtworkBridge.provider`; `IOSRelativeFilePathResolver.resolve`; and
`createIOSPlaybackEngine`. Engine, remote-command, and teardown helpers remain
internal/private. On JVM, document `createJvmPlaybackEngine`; the native engine,
`MacAudioPlayerBridge`, its methods/native declarations, progress helpers, and loader remain
internal/private with unchanged JNI identities.

Every public production declaration/member in the new strict core module, including overrides
and public constructor properties, requires explicit visibility/types and succinct
behavior-preserving KDoc regardless of whether shared currently consumes it. Internal/private
declarations do not need widening or public KDoc. Do not expand behavior or signatures.

**Build, dependency, and policy inventory:** Add `:core:playback` to
`settings.gradle.kts` and create `core/playback/build.gradle.kts` with the controlled core
and Android-KMP conventions used by existing core modules. Configure JVM, Android with
host tests, `iosArm64`, and `iosSimulatorArm64`, including the existing JVM 11 and Android
compile/min-SDK policy. Its common API dependencies are `api(projects.core.model)` and
`api(libs.kotlinx.coroutinesCore)`; implementation dependencies are
`implementation(projects.core.platform)` and `implementation(libs.kermit)`; Android owns
its Media3 dependencies. Move `nativeAudioResourceRoot`, `macosAudioResourceArch`,
`macosAudioHelperOutputFile`, `macosAudioHelperSourceFile`, `javaHomePath`,
`buildMacosAudioHelper`, the `jvmMain` generated-resource source directory, and the
`jvmProcessResources`/`processJvmMainResources` dependencies from shared to core. Preserve
the generated output
`build/generated/nativeAudioResources/jvmMain/native/$macosAudioResourceArch/librhythhaus_audio.dylib`.
Enable strict explicit API and add the private/internal core playback logger using
`Logger.withTag("RhythHaus")` so moved controller/iOS engine logging preserves tag and
behavior; shared `Logger.kt` remains compatibility-owned and core must not import shared
`log`. Core must never depend on shared, features, DataStore, Koin, or apps. Update
`shared/build.gradle.kts` to use `api(projects.core.playback)` and both iOS framework
declarations to export only this new core module in addition to existing allow-listed
exports. Update `ArchitectureAllowList.kt` for only
`:core:playback -> :core:model`/`:core:platform` and `:shared -> :core:playback`.

**Test inventory:** Move these tests into matching `core/playback` source sets while
preserving packages and adapting platform construction to the corresponding core factory,
never shared `createPlatformPlaybackEngine()`: common
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt` and
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshotTest.kt`;
Android host `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt`
and `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridgeTest.kt`;
JVM `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`; and iOS
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt`,
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingBridgingTest.kt`,
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingDiagnosticTest.kt`,
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`, and
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSCommandEnabledAfterTargetTest.kt`. Add
`core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackContractTest.kt` for
public controller/session/factory contract characterization. Retain shared integration and
composition tests: `PlaybackSessionCoordinatorTest`, `PlaybackSessionStoreJvmTest`,
`RhythHausDiTest`, `RhythHausDiFactoryJvmTest`, `PlaylistLifecycleIntegrationJvmTest`,
`AppDispatcherJvmTest`, `AppScanCancellationTest`, and `LibraryPlaybackSelectionTest`.

- [ ] Inventory the exact production, native, Gradle, test, and consumer paths above before
  edits. Include `settings.gradle.kts`, `shared/build.gradle.kts`,
  `core/playback/build.gradle.kts`, `ArchitectureAllowList.kt`, and
  `ArchitectureCheckPluginFunctionalTest.kt`; classify each as core move, shared retention,
  build/policy update, moved test, or unchanged consumer invariant. Confirm the Android app
  manifest/application and Swift consumers remain unchanged, and establish a clean baseline
  with `git status --short` and the relevant existing focused tests.
- [ ] Before registering the module, create the core-owned test directories, relocate the
  listed module-owned tests, and add `PlaybackContractTest.kt`. Run
  `./gradlew :core:playback:allTests :core:playback:compileKotlinJvm --configuration-cache`.
  Expected RED: Gradle reports that `:core:playback` is absent. Do not relocate
  `FakePlaybackEngine` to test source.
- [ ] Add a focused `ArchitectureCheckPluginFunctionalTest` functional fixture module that
  models `:core:playback`, strict explicit API, and a preserved-package
  `com.eterocell.rhythhaus`/`.session` source. Its valid candidate graph has
  `:core:playback -> :core:model`, `:core:playback -> :core:platform`,
  `:shared -> :core:playback`, and a positive `:shared` iOS export of
  `:core:playback`. This is architecture-policy RED, distinct from the absent-module
  compilation RED: before allow-list completion, run the positive-policy selector and require
  `ARCH-EDGE :core:playback [architecture] -> :core:model`,
  `ARCH-EDGE :core:playback [architecture] -> :core:platform`, `ARCH-PACKAGE` for the
  preserved `com.eterocell.rhythhaus`/session source under current `.playback`-only
  ownership, and `ARCH-IOS-EXPORT :shared -> :core:playback`.
- [ ] GREEN that policy by adding only core-playback outgoing edges to model/platform,
  retaining existing `:shared -> :core:playback`, changing the core-playback package-root
  policy to package-stable `com.eterocell.rhythhaus` covering `.session` and existing
  subpackages, and making `allowsIosExport` permit only
  `modulePath == ":shared" && exportedProjectPath == ":core:playback"`. The checker is
  fail-closed: add and run `corePlaybackCannotDependOnShared` as immediate characterization
  GREEN with `./gradlew :build-logic:convention:test --tests
  '*ArchitectureCheckPluginFunctionalTest.corePlaybackCannotDependOnShared'
  --configuration-cache`; expected GREEN because the malformed fixture is rejected with
  exactly `ARCH-EDGE :core:playback [architecture] -> :shared`. Retain
  `UnapprovedIosExport` of `:core:model` as a failing negative control.
- [ ] Before production construction changes, add compilable shared characterization for the
  explicit-engine `PlaybackController` shape and one-engine/one-controller/exact-session-
  controller identity in `RhythHausDiTest`/`RhythHausDiFactoryJvmTest`. Run `./gradlew
  :shared:jvmTest --tests '*RhythHausDiTest' --tests '*RhythHausDiFactoryJvmTest'
  --configuration-cache`; expected baseline GREEN because this captures the existing
  singleton behavior before relocation. Do not attempt an impossible Kotlin compile-negative
  test; the absent-module and architecture-policy cases provide the task's RED evidence, and
  explicit API plus these composition tests characterize the constructor boundary.
- [ ] GREEN the module wiring and moves: register and configure `:core:playback`; apply the
  KMP/core conventions and exact API/implementation dependencies; move the complete common,
  session, Android/JVM/iOS, native, and test inventories; move native resource/build tasks;
  add the Kermit logger; and preserve every Kotlin package. Keep `FakePlaybackEngine` in
  core production `Playback.kt`. Create public core factories, private/internal engine
  implementations, and the core iOS resolver port. Add the explicit shared facade files and
  delegate each platform actual to its core factory. Adapt shared Koin to inject the facade
  singleton into explicit-engine `PlaybackController`; retain shared store/coordinator/
  lifecycle/DataStore/App/logger/adapter ownership.
- [ ] Run focused GREEN checks: `./gradlew :core:playback:allTests
  :core:playback:compileKotlinJvm --configuration-cache`; `./gradlew
  :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest'
  --configuration-cache`; `./gradlew :core:playback:testAndroidHostTest
  --configuration-cache`; and `./gradlew :core:playback:iosSimulatorArm64Test
  --configuration-cache`. Run retained shared JVM selectors where applicable:
  `./gradlew :shared:jvmTest --tests '*PlaybackSessionCoordinatorTest' --tests
  '*PlaybackSessionStoreJvmTest' --tests '*RhythHausDiTest' --tests
  '*RhythHausDiFactoryJvmTest' --tests '*PlaylistLifecycleIntegrationJvmTest' --tests
  '*AppDispatcherJvmTest' --tests '*AppScanCancellationTest' --tests
  '*LibraryPlaybackSelectionTest' --configuration-cache`.
- [ ] Run full architecture verification: `./gradlew :architecture-processor:clean
  :architecture-processor:jar --configuration-cache`, then `./gradlew
  :build-logic:convention:test
  -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
  --configuration-cache`. Run `./gradlew architectureCheck --configuration-cache
  --configuration-cache-problems=fail --no-parallel` twice and require configuration-cache
  reuse on the second invocation.
- [ ] Run the cross-target consumer matrix: `./gradlew :core:playback:allTests
  :shared:jvmTest :androidApp:assembleDebug :desktopApp:compileKotlin
  :shared:compileKotlinIosSimulatorArm64 :core:playback:compileKotlinIosArm64
  :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64
  --configuration-cache`. The two link tasks are Kotlin/Native Shared framework/export
  linkage evidence, not Swift compilation. Then run `/usr/bin/xcrun xcodebuild -version`,
  followed by `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp
  -configuration Debug -destination 'generic/platform=iOS Simulator'
  CODE_SIGNING_ALLOWED=NO build` as Swift consumer compilation evidence. If it is unavailable
  or fails, record the exact blocker and do not claim it passed. Then run `./gradlew
  :core:playback:iosSimulatorArm64Test :shared:iosSimulatorArm64Test
  --configuration-cache`. Confirm Android packaging, desktop compilation, Kotlin/Native
  framework linkage/export, Swift consumer compilation when successful, and iOS simulator
  coverage; do not claim runtime launch, runtime UI, or `./init.sh` unless actually run.
- [ ] Run `./gradlew spotlessApply --configuration-cache`, followed by standalone
  `./gradlew spotlessCheck --configuration-cache` and `./gradlew detekt
  --configuration-cache`. Before acceptance, independently review the plan, design, and
  OpenSpec alignment: `openspec/changes/feature-first-modularization/design.md` distinguishes
  Library/Playlist shared implementations from atomic core playback implementation ownership.
  Run `openspec validate feature-first-modularization --strict`; OpenSpec items 5.3 and 5.4
  and Superpowers acceptance 4.4 remain unchecked until their actual evidence. Run actual
  changed-file `git diff --check`;
  this planning amendment only uses
  `git diff --check -- docs/superpowers/plans/2026-07-27-feature-first-modularization.md`.
- [ ] Obtain independent review and record exact RED/GREEN, architecture, cross-target,
  quality, export, and invariant evidence only after acceptance. Then commit the completed
  implementation with `git add core/playback shared settings.gradle.kts build-logic
  && git commit -m "refactor: extract playback contracts"`; exclude unchanged `androidApp`
  and planning documents from implementation staging, and do not stage or commit this
  planning-only amendment.
