# Session Progress

## Handoff - 2026-07-06 liquid glass backdrop chrome

Route: openspec+superpowers
Owner: implementation
Scope: Replace nested-scroll top chrome and bottom NowPlayingBar panel surfaces with Kyant0 Backdrop liquid-glass effect.
Implementation:
- Added Backdrop and Shapes dependencies through the version catalog.
- Added local `LiquidGlassChrome.kt` wrapper for Backdrop recording/effects/fallback draw surface.
- Recorded Library/Home and drill-down content layers and routed nested-scroll top chrome through Backdrop glass.
- Routed root and drill-down `NowPlayingBar` rounded card containers through Backdrop glass while preserving existing controls and gestures.
Verification:
- `openspec validate liquid-glass-backdrop-chrome --strict`: pass (`Change 'liquid-glass-backdrop-chrome' is valid`).
- `git diff --check`: pass (no output, exit 0).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 36s`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 26s`).
Acceptance:
- Requirement matched: yes — visible Library/Home chrome, drill-down chrome, and bottom bar rounded card now use Kyant0 Backdrop glass with fallback surface draw.
- Scope controlled: yes — no playback, scanner, navigation model, route-transition, empty-library, or control redesign changes beyond backdrop recording and glass surfaces.
- Edge cases/risk reviewed: Backdrop shader support may vary by platform; fallback surface remains readable; manual visual validation recommended on Android/iOS/macOS for final glass tuning.
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `openspec/changes/liquid-glass-backdrop-chrome/proposal.md`
- `openspec/changes/liquid-glass-backdrop-chrome/design.md`
- `openspec/changes/liquid-glass-backdrop-chrome/tasks.md`
- `openspec/changes/liquid-glass-backdrop-chrome/specs/library-ui/spec.md`
- `docs/superpowers/specs/2026-07-06-liquid-glass-backdrop-chrome-design.md`
- `docs/superpowers/plans/2026-07-06-liquid-glass-backdrop-chrome.md`
- `progress.md`
Next owner: user for manual visual validation of glass appearance on Android/iOS/macOS.
Blockers: none for automated verification.
Commit: implementation commits `a29a6cd`, `bd3c6d5`, `12802cc`, `acc0df6`; final docs/evidence commit pending.


## Handoff - 2026-07-06 fix nested scroll chrome layout

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported nested scroll chrome issues: on the main Library screen the scrolled top bar covered the whole screen with the title at the bottom; on track-list screens the chrome worked but its color did not cover the iOS status bar.
Root cause:
- The main screen placed `LazyColumn` and `NestedScrollBlurChrome` directly inside `Surface` (Miuix `Box` with `propagateMinConstraints = true`). `Surface` propagated full-screen min constraints to the chrome, and `Modifier.height(chromeHeight)` only constrained max height, allowing the chrome to stretch to the full screen on the main screen.
- The track list wrapped its content in an inner `Box` (default `propagateMinConstraints = false`), which is why it did not stretch, but `NestedScrollBlurChrome` read `WindowInsets.statusBars` internally. If the `LazyColumn`'s `statusBarsPadding()` consumed the insets before the chrome read them, the chrome could compute a height that did not include the status bar.
Fix:
- Changed `NestedScrollBlurChrome` to accept `statusBarHeight: Dp` as a parameter and use `Modifier.requiredHeight(chromeHeight)` so its height is exact regardless of propagated min constraints.
- Read the status bar height once in each route (Library/Home and `DrillDownView`) before the list consumes insets, replaced `LazyColumn.statusBarsPadding()` with `Modifier.padding(top = statusBarHeight)`, and passed the same height to `NestedScrollBlurChrome`.
- Wrapped the main screen `LazyColumn` + chrome in an inner `Box` to match the working track-list pattern and isolate layout constraints.
Verification:
- `./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`; one known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` failed on first broad run, passed on targeted rerun).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- iOS simulator visual check (forced chrome visible): main screen chrome now sits at the top only, title is at the top, and the scrim extends behind the status bar; at rest the chrome is hidden as intended.
- `git diff --check`: pass (no output).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for on-device visual confirmation, especially track-list scroll behavior on iOS.
Blockers: none.

## Handoff - 2026-07-06 fix iOS TagLib deployment target mismatch

Route: systematic-debugging (bugfix)
Owner: implementation
Input: Xcode Release/device link warning: `Shared.framework/Shared[149](rh_taglib.cpp.o)` was built for newer iOS 26.5 than being linked at iOS 26.0.
Root cause:
- `taglib/build.gradle.kts` built iOS TagLib static archives with custom CMake tasks and no explicit `CMAKE_OSX_DEPLOYMENT_TARGET`, so AppleClang used the current iPhoneOS 26.5 SDK default/min version for object metadata while `iosApp` links at `IPHONEOS_DEPLOYMENT_TARGET = 26.0`.
- A second issue made the build nondeterministic across iOS targets: both `iosArm64` and `iosSimulatorArm64` copied `librhythhaus_taglib.a` / `libtag.a` to the same `src/nativeInterop/cinterop/` paths, so device and simulator builds could overwrite each other's archives.
Fix:
- Added `rhythhaus.ios.deploymentTarget=26.0` in `gradle.properties`; `taglib/build.gradle.kts` resolves `iosTagLibDeploymentTarget` from Xcode's `IPHONEOS_DEPLOYMENT_TARGET` environment variable when present, otherwise falls back to the Gradle property, and passes it to CMake as `-DCMAKE_OSX_DEPLOYMENT_TARGET` for iOS TagLib builds.
- Moved generated iOS TagLib archives to target-specific build outputs under `taglib/build/generated/iosTagLib/<target>/`, and the generated cinterop `.def` now points each KMP target at its own library directory.
Verification:
- Rebuilt clean iOS TagLib/device framework path: `./gradlew :shared:linkReleaseFrameworkIosArm64 --configuration-cache` -> `BUILD SUCCESSFUL`.
- Verified deployment target source selection: default CLI build wrote `CMAKE_OSX_DEPLOYMENT_TARGET=26.0`; test override `IPHONEOS_DEPLOYMENT_TARGET=26.1 ./gradlew :taglib:buildIosTagLibHelperIosArm64 --configuration-cache` wrote `CMAKE_OSX_DEPLOYMENT_TARGET=26.1`; rebuilt default artifacts back to 26.0 afterward.
- Inspected rebuilt object metadata: `rh_taglib.cpp.o` reports `minos 26.0`, `sdk 26.5`.
- Xcode Release/device build: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build` -> `** BUILD SUCCEEDED **`; grep count for `was built for newer 'iOS' version` was `0`.
- Simulator coverage after target-specific archive separation: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> `BUILD SUCCESSFUL`.
Changed files:
- `gradle.properties`: `rhythhaus.ios.deploymentTarget=26.0` fallback/source of truth for CLI builds.
- `taglib/build.gradle.kts`: Xcode env/Gradle property provider chain for iOS native deployment target and target-specific generated static archive paths.
Next owner: user for normal Xcode Archive/signing validation if desired.
Blockers: none.

## Handoff - 2026-07-03 navigation animation polish

Route: openspec+superpowers
Owner: implementation
Scope: Three polish items on top of the base navigation-animations change: (1) Android predictive back visual progress, (2) NowPlayingBar fixed during route transitions, (3) NowPlayingBar expands/collapses into Now Playing with a growth animation instead of a route-push fade/slide.
Implementation:
- Predictive back: read `navState.transitionState` (kept `NavigationEventInfo.None`; confirmed `NavigationEventInfo.Slide` does not exist in `navigationevent` 1.1.x by extracting the sources jar). When `transitionState` is `InProgress` with direction `TRANSITIONING_BACK`, extract `latestEvent.progress` (0f-1f) and apply it as a horizontal offset on the `AnimatedContent` route container so the current route visually tracks the drag. Gesture completion still pops via the existing `onBackCompleted`; cancellation naturally resets to 0 because progress is derived fresh from `transitionState` every recomposition, not stored.
- Fixed bottom bar: restructured `LibraryHomeScreen` root into a `Box` containing `AnimatedContent` (route content, now `fillMaxSize()` + predictive-back offset) and `NowPlayingBar` as a fixed sibling aligned `BottomCenter`. Removed the previous `NowPlayingBar` call from inside the Home/Settings/Search/ClearLibraryDialog route branch.
- Bottom bar expand/collapse: replaced the `LibraryRoute.NowPlaying` route-push rendering (which used to show `NowPlayingScreen` via the standard `AnimatedContent` fade/slide) with an empty placeholder inside `AnimatedContent`, and added a new `NowPlayingExpandOverlay` composable rendered outside `AnimatedContent`, driven by an `Animatable<Float>` (0f = bar, 1f = full screen; expand `tween(300)`, collapse `tween(250)`). The overlay grows a `Surface` from the bottom via `fillMaxHeight(fraction)`, shrinks its top corner radius from 24dp to 0dp as it grows, and fades the `NowPlayingScreen` content in via `alpha(fraction)`. `leftEdgeSwipeBack` and `BackChip` inside `NowPlayingScreen` are unchanged for closing.
Self-review (independent reviewer subagent timed out after 600s with 0 findings returned; coordinator performed the review directly):
- Confirmed no double-render: the `NowPlaying` branch inside `AnimatedContent` is an empty `Box`; actual content only renders in `NowPlayingExpandOverlay`.
- Confirmed `predictiveBackProgress` is safe: computed fresh from `navState.transitionState` each recomposition (not `remember`ed), so it naturally returns to 0 when `transitionState` returns to `Idle` after gesture completion or cancellation.
- Confirmed `LaunchedEffect(isVisible)` cannot race: `Animatable.animateTo` cancels any in-flight animation on the same instance before starting a new one, so rapid open/close taps are safe.
- Open caveat not fully resolved by automated checks: the `LibraryRoute.NowPlaying` target still participates in `AnimatedContent`'s route transition (transitioning to/from an empty `Box`), so the underlying screen still runs its own fade/slide via `routeContentTransform` while the expand overlay grows on top of it simultaneously. This is expected to look fine since the overlay covers the screen as it grows, but has not been visually confirmed on a device/simulator.
Verification:
- `openspec validate navigation-animation-polish --strict`: pass (`Change 'navigation-animation-polish' is valid`).
- `openspec validate navigation-animations --strict`: pass (`Change 'navigation-animations' is valid`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 98 actionable tasks: 13 executed, 5 from cache, 80 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 22s`; 33 actionable tasks: 8 executed, 25 up-to-date).
- `git diff --check`: pass (no output, exit 0).
Acceptance:
- Requirement matched: yes for all three polish items at the source/automated-verification level; predictive back progress, fixed bottom bar, and expand/collapse animation are implemented per spec.
- Scope controlled: yes — no new dependencies, no `SharedTransitionScope` adoption, no native navigation migration, no changes to playback, scanner, library persistence, theme, or Now Playing screen content layout beyond the expand wrapper.
- Edge cases/risk reviewed: the external reviewer subagent timed out without returning a verdict; coordinator performed the review directly and found no blocking issues, but flagged the simultaneous-transition-layering caveat above as needing manual visual confirmation on device/simulator (Android 13+ for predictive back gesture in particular).
Changed files:
- `docs/superpowers/specs/2026-07-02-navigation-animation-polish-design.md`
- `docs/superpowers/plans/2026-07-02-navigation-animation-polish.md`
- `openspec/changes/navigation-animation-polish/proposal.md`
- `openspec/changes/navigation-animation-polish/design.md`
- `openspec/changes/navigation-animation-polish/specs/library-navigation/spec.md`
- `openspec/changes/navigation-animation-polish/tasks.md`
- `openspec/changes/navigation-animation-polish/.openspec.yaml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `progress.md`
Next owner: user for manual visual validation, especially Android 13+ predictive back gesture and the bar-to-Now-Playing expand/collapse feel on device; OpenSpec/user for archive of both `navigation-animations` and `navigation-animation-polish` when satisfied.
Blockers: none for automated verification. Manual visual confirmation not performed in this session.
Commit: pending — user asked for the base navigation-animations work and this polish work to land in one commit; awaiting staged-diff review and approval.

## Handoff - 2026-07-02 navigation animations

Route: openspec+superpowers
Owner: implementation
Scope: Add shared Compose direction-aware route transition animations for Home, detail, Now Playing, Search, Settings, and Clear Library dialog routes.
Implementation:
- Added pure navigation transition classification for push/pop/replace/root/no-op route changes.
- Added common tests covering transition classification and preserving route-stack behavior.
- Wrapped shared route rendering in root-level AnimatedContent with direction-aware push/pop/root/replace transitions.
- Replaced the Clear Library platform `Dialog` with an in-window Compose overlay so that route also participates in the AnimatedContent transition.
- Preserved existing visible back, left-edge swipe, Android system/predictive back, playback, scanner, library, and theme behavior.
Verification:
- `openspec validate navigation-animations --strict`: pass (`Change 'navigation-animations' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass before final fix (`BUILD SUCCESSFUL in 357ms`) and pass after final fix (`BUILD SUCCESSFUL in 329ms`; 24 actionable tasks: 4 executed, 20 up-to-date; configuration cache reused).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass after final fix (`BUILD SUCCESSFUL in 323ms`; 15 actionable tasks: 3 executed, 12 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: initial fail in known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` (`60 tests completed, 1 failed`; Android debug package still completed before `:shared:jvmTest` failure); targeted rerun passed; exact broad rerun passed; final post-fix broad run passed (`BUILD SUCCESSFUL in 5s`; 98 actionable tasks: 12 executed, 86 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --configuration-cache`: pass (`BUILD SUCCESSFUL in 919ms`; 33 actionable tasks: 5 executed, 28 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass before final fix (`BUILD SUCCESSFUL in 21s`) and pass after final fix (`BUILD SUCCESSFUL in 13s`; 33 actionable tasks: 8 executed, 25 up-to-date; configuration cache reused).
- `git diff --check`: pass (no output, exit 0).
- `git diff --stat`: reviewed; tracked diff summary after final fix/evidence updates showed `progress.md`, `App.kt`, `LibraryNavigation.kt`, and `LibraryNavigationTest.kt` with 341 insertions and 149 deletions; untracked new OpenSpec/docs/report files are listed below.
Reviews:
- Task 1 review: clean.
- Task 2 review: clean after timeout recovery.
- Final whole-change review: found one Important issue that Clear Library platform `Dialog` would likely not animate inside parent `AnimatedContent`.
- Final fix and re-review: clean; no Critical, Important, or Minor findings.
Acceptance:
- Requirement matched: yes — shared route changes animate, push/pop directions are distinct, and Clear Library uses in-window content instead of platform `Dialog` so it participates in the route transition.
- Scope controlled: yes — no new dependencies, native navigation migration, playback, scanner, persistence, or theme behavior changes.
- Edge cases/risk reviewed: automated checks prove route metadata and compilation; subjective animation polish still needs manual visual validation on Android/iOS/macOS.
Changed files:
- `docs/superpowers/specs/2026-07-02-navigation-animations-design.md`
- `docs/superpowers/plans/2026-07-02-navigation-animations.md`
- `openspec/changes/navigation-animations/proposal.md`
- `openspec/changes/navigation-animations/design.md`
- `openspec/changes/navigation-animations/specs/library-navigation/spec.md`
- `openspec/changes/navigation-animations/tasks.md`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `.superpowers/sdd/navigation-animations/task-3-report.md`
- `progress.md`
Next owner: user for manual visual validation; OpenSpec/user for archive when satisfied.
Blockers: none for automated verification. One broad verification run hit known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun and exact broad rerun passed.
Commit: pending semantic commit after user reviews staged diff, unless user asks not to commit.

## Handoff - 2026-07-02 fix iOS lockscreen controls and artwork regression

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported iOS lockscreen media controls are again greyed out and album art does not show.
Investigation evidence:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` still ignored the handler tokens returned by `MPRemoteCommand.addTargetWithHandler`, matching the known Kotlin/Native MediaRemote failure mode where enabled commands can be dropped by iOS and rendered as unsupported/greyed.
- Several remote handlers called `updateNowPlayingInfo()` without explicit paused/stopped `playbackRate = 0.0`, leaving stale `MPNowPlayingInfoPropertyPlaybackRate = 1.0` after pause/stop/toggle-pause paths.
- Kotlin replaced the whole `MPNowPlayingInfoCenter.nowPlayingInfo` dictionary on every refresh without carrying forward Swift-inserted `MPMediaItemPropertyArtwork`, so artwork set by `RhythHausArtworkProvider` was deleted by later play/pause/seek/progress updates.
Fix:
- Retained all iOS remote command handler tokens for the engine lifetime in `remoteCommandHandlerTokens`.
- Made pause/toggle-pause/stop remote command handlers publish explicit `playbackRate = 0.0`; seek now mirrors the actual player playing state.
- Preserved any existing `MPMediaItemPropertyArtwork` entry when rebuilding the Now Playing dictionary, and reset `artworkTrackId` during track-switch teardown so the next track republishes artwork.
- Added `nowPlayingDictionaryPreservesArtworkAndExplicitPausedRate` iosTest coverage.
Verification:
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 13s`); `TEST-iosSimulatorArm64Test.com.eterocell.rhythhaus.IOSNowPlayingInfoTest.xml` shows 5 tests, 0 failures, including `nowPlayingDictionaryPreservesArtworkAndExplicitPausedRate`.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `git diff --check`: pass.
Acceptance:
- Requirement matched: source-level regression causes for greyed command routing and artwork deletion are addressed.
- Scope controlled: yes — iOS playback engine/test only; no Android, desktop, scanner, database, dependency, or UI behavior changed.
- Not verified: live lockscreen/Control Center visual confirmation on simulator/device was not captured in this CLI session.
Changed files:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`
Next owner: user for live iOS lockscreen/Control Center visual confirmation with an artwork-bearing track.
Blockers: none for automated validation; no live iOS visual capture was performed.

## Handoff - 2026-07-01 Android release metadata R8 keep rules

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported metadata scans correctly in Android debug builds but not in release builds.
Root cause: Release builds enable R8 minification/shrinking while debug builds do not. Source/build inspection showed the Android TagLib JNI bridge returns `NativeTagLibReadResult` from native C++ via `FindClass`/constructor signatures. A release APK dex inspection before the fix showed R8 kept `NativeTagLibBridge` native method names but optimized `NativeTagLibReadResult` into an abstract shell with no constructor, fields, or accessors. That makes the native result mapping fail only in minified release builds, so metadata falls back to display-name/default values. Debug APKs kept the normal Kotlin data-class shape.
Fix:
- Added `androidApp/proguard-rules.pro` with keep rules for JNI native method descriptor classes and the TagLib JNI bridge/result/write metadata classes.
- Wired the release build type in `androidApp/build.gradle.kts` to use the default optimized Android rules plus `proguard-rules.pro`.
Tight feedback loop:
- Before fix, `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease --configuration-cache` built both variants; release dex check failed because `NativeTagLibReadResult` had `Access flags 0x0401 (PUBLIC ABSTRACT)` and no constructor/accessors.
- After fix, release dex check passes: `NativeTagLibReadResult` is `PUBLIC FINAL`; `NativeTagLibBridge.readFdNative`/`readPathNative`, `NativeWriteBridge.writePathNative`, and `WriteMeta` getters are present.
Verification:
- `./gradlew :androidApp:assembleRelease --rerun-tasks --configuration-cache`: pass (`BUILD SUCCESSFUL`); followed by dex inspection keep-check pass.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :androidApp:assembleRelease --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `git diff --check`: pass.
Acceptance:
- Requirement matched: yes for the release-vs-debug metadata cause that is reproducible locally; the release APK now preserves the JNI result classes needed by metadata scanning.
- Scope controlled: yes — Android release shrinker configuration only; no scanner logic, database schema, playback, native TagLib C++, or metadata merge behavior changed.
- Edge cases/risk reviewed: live Android SAF/provider scanning in an installed release build still needs device validation with real tagged files; automated verification proves the release-only R8/JNI break is fixed.
Changed files:
- `androidApp/build.gradle.kts`: release build now applies default optimized ProGuard rules plus app rules.
- `androidApp/proguard-rules.pro`: keep rules for TagLib JNI bridges/result/value classes.
Next owner: user for installing release build and rescanning/clearing existing fallback rows if needed.
Blockers: none for automated validation; no Android device was attached for live release scan validation.

## Handoff - 2026-07-01 Android SAF metadata fallback

Route: systematic-debugging (bugfix follow-up)
Owner: implementation
Input: User reported that after the FD TagLib change, scanned Android tracks still have missing metadata and duration `0:00`.
Investigation evidence:
- Current Android APK contains `librhythhaus_taglib.so` for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Native symbol check confirmed all packaged/generated Android `.so` slices export `Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readFdNative`, `Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPathNative`, and `rh_taglib_read_fd`.
- A local native probe using the macOS helper verified `rh_taglib_read_fd` can read the generated WAV fixture title/artist/album/duration from a normal seekable FD, so the FD bridge itself works for at least file-backed descriptors.
- No adb device was attached (`adb devices` listed none), so exact Android provider/runtime failure could not be captured from device logs.
Root-cause hypothesis acted on:
- Real Android SAF providers can still produce descriptors/streams that TagLib cannot fully parse or cannot derive duration from. Because `AudioMetadataReader` returned null on TagLib Unsupported/Failed or accepted partial TagLib results with null duration, the scanner persisted fallback title/artist/album and null duration, rendering as `0:00`.
Fix:
- Added a platform metadata fallback seam: `internal expect fun readPlatformAudioMetadata(source: AudioSource): AudioMetadata?`.
- Android actual uses `MediaMetadataRetriever` for `AudioSource.FileDescriptor` via `/proc/self/fd/<fd>` and for `AudioSource.Uri` via `setDataSource(context, uri)`.
- `AudioMetadataReader` now merges missing fields from the platform fallback when TagLib returns no metadata or partial metadata lacking title/artist/album/duration/artwork. TagLib remains the primary reader; Android framework metadata fills gaps, especially duration.
- JVM/iOS actual fallbacks return null, preserving current TagLib-only behavior there.
- Added regression coverage where TagLib returns title/artist/album but no duration; scanner fills duration from the platform fallback while preserving TagLib fields.
Verification:
- Native probe: `rh_taglib_read_path` and `rh_taglib_read_fd` both returned status 0, title/artist/album, duration=1, sample=8000, channels=1 for `/tmp/rhythhaus-fd-fixture.wav`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerFillsMissingDurationFromPlatformMetadataFallback' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `git diff --check`: pass.
Changed files added in this follow-up:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.ios.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
Next owner: user for Android device clear/re-scan/manual validation with real SAF music files. If metadata still shows fallback values, capture device logcat around scan; no device was connected in this session.
Blockers: none for automated validation; live Android SAF provider behavior not device-verified.
Commit: not created; user did not ask to commit.

## Handoff - 2026-07-01 Android SAF FD metadata

Route: systematic-debugging (bugfix)
Owner: implementation
Scope: Replace Android SAF metadata temp-file handoff with a file-descriptor handoff so TagLib can read metadata without copying user audio into app storage.
Root cause: Android SAF playback sources are `content://` URIs, while RhythHaus TagLib metadata reads previously only accepted filesystem paths. The prior temp-file fix avoided persistent copies, but still copied every document before TagLib could run; if the metadata source fell back to URI, `AudioMetadataReader` returned null and tracks kept fallback metadata.
Fix:
- Added a metadata-only `AudioSource.FileDescriptor(fd, displayName)` and routed `AudioMetadataReader` through a new `TagLibReader.readFd` path.
- Added native `rh_taglib_read_fd(int fd, const char* display_name)` using `dup(fd)` plus TagLib `FileStream`/`FileRef(IOStream*)`, with shared extraction logic for path and descriptor reads.
- Added Android JNI `readFdNative(fd, displayName)` and wired `AndroidNativeTagLibReader.readFd` to it.
- Changed Android SAF scanning to open each supported document with `ContentResolver.openFileDescriptor(document.uri, "r")`, pass that descriptor as the metadata source, and close it in `cleanupMetadataAudioSource` after scanner enrichment. Playback persistence still uses the original `content://` URI.
- Kept legacy persistent metadata cache removal on scan start so old copied-cache files are cleaned on rescan.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`); rebuilt Android TagLib helper slices during the build.
- `./gradlew :taglib:buildAllAndroidTagLibHelpers --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :taglib:buildMacosTagLibHelper :taglib:buildAllAndroidTagLibHelpers --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — Android SAF metadata now uses option 2 (file descriptor) instead of temp-copy files; original `content://` playback URI is still persisted.
- Scope controlled: yes — no database schema, playback behavior, scanner UI, or iOS/JVM metadata source changes beyond exhaustive `AudioSource` handling.
- Edge cases/risk reviewed: `FileDescriptor` is metadata-only and throws if accidentally routed to platform playback. Automated tests/builds prove compilation and scanner routing; real Android provider behavior still needs manual device rescan with local music to confirm provider seekability and actual embedded metadata/artwork extraction.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
- `taglib/src/commonMain/kotlin/com/eterocell/rhythhaus/taglib/RhythHausTagLib.kt`
- `taglib/src/androidMain/kotlin/com/eterocell/rhythhaus/taglib/TagLibReader.android.kt`
- `taglib/native/include/rh_taglib.h`
- `taglib/native/src/rh_taglib.cpp`
- `taglib/native/jni/rh_taglib_jni.cpp`
Next owner: user for Android device rescan/manual validation with real SAF music files; implementation if a provider returns a non-seekable descriptor and needs a fallback.
Blockers: none for automated validation.
Commit: not created; user did not ask to commit.

## Handoff - 2026-07-01 fix Android SAF import metadata copies

Route: systematic-debugging (bugfix)
Owner: implementation
Scope: Fix Android folder import behavior where scanned SAF audio files appeared copied into app storage and metadata fallback values returned again.
Root cause: Android SAF scanning intentionally preserved playback as `content://` URIs, but copied every supported document into a persistent app cache under `cacheDir/rhythhaus-taglib/<sourceId>` so native TagLib could read a filesystem path. That made imports look like the app had copied the music into internal storage. Metadata fallback happened whenever that filesystem handoff failed or fell back to the original URI, because `AudioMetadataReader` returns null for `AudioSource.Uri`.
Fix:
- Changed Android SAF metadata handoff to create a per-candidate temporary file under `cacheDir/rhythhaus-taglib-temp`, pass that filesystem path only to metadata extraction, and persist the original `content://` playback URI unchanged.
- Added `cleanupMetadataAudioSource` to `AudioScanCandidate` and `audioCandidateForSourceFile`; `LibraryScanner` now always invokes it in a `finally` after metadata read, so the temp copy is deleted whether TagLib succeeds or falls back.
- Removed the legacy persistent metadata-cache directory for the scanned source at scan start so old internal-storage copies are cleaned up on the next Android rescan.
- Extended the existing scanner regression to assert that the metadata filesystem source is used while the playback URI is preserved and cleanup runs.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — Android persisted tracks still reference the selected external SAF document URI; copied files are now metadata-only temporary files cleaned after each read, with legacy persistent cache cleaned on rescan.
- Scope controlled: yes — no scanner UI, database schema, playback engine, native TagLib ABI, iOS, or JVM folder behavior changes.
- Manual note: existing rows with fallback metadata may need a rescan to refresh stored title/artist/album/duration/artwork.
Changed files:
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
Next owner: user for Android device rescan/manual validation with real local music files.
Blockers: none for automated validation.

## Handoff - 2026-07-01 fix iOS lockscreen skip buttons greyed out

Route: systematic-debugging (bugfix)
Owner: implementation
Scope: Fix user report — iOS lockscreen media controls, previous/next (forward/backward) buttons greyed out.
Root cause (Phase 1/2): `IOSPlaybackEngine.registerRemoteCommands()` enabled and handled
`previousTrackCommand`/`nextTrackCommand`, but never explicitly disabled the skip-interval commands
(`skipForwardCommand`, `skipBackwardCommand`, `seekForwardCommand`, `seekBackwardCommand`). iOS
prefers the interval commands over the track commands on the lock screen when the interval commands
are left enabled; with no handler attached, they render greyed out and suppress the working
previous/next track buttons.
Fix (Phase 4, single change):
- Extracted the command enable/disable wiring from `registerRemoteCommands()` into a new
  internal top-level `configureIOSRemoteCommandAvailability(commandCenter: MPRemoteCommandCenter)`
  in `PlaybackEngine.ios.kt`, called from `registerRemoteCommands()` unchanged (still guarded by
  `remoteCommandsRegistered` to avoid handler accumulation, per known cinterop pitfall).
  The function now additionally disables `skipForwardCommand`, `skipBackwardCommand`,
  `seekForwardCommand`, `seekBackwardCommand`.
- Added `remoteCommandConfigurationEnablesTrackControlsAndDisablesIntervalControls` regression test
  in `IOSNowPlayingInfoTest.kt` (iosTest) asserting the real `MPRemoteCommandCenter.sharedCommandCenter()`
  singleton's `.enabled` flags after calling the new function — not a mock.
Verification:
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 16s`);
  new test confirmed executed via `TEST-iosSimulatorArm64Test.com.eterocell.rhythhaus.IOSNowPlayingInfoTest.xml`
  (4 tests, 0 failures, including `remoteCommandConfigurationEnablesTrackControlsAndDisablesIntervalControls`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
Acceptance:
- Requirement matched: yes — root cause fixed at the source (command registration), not a UI-level workaround.
- Scope controlled: yes — iOS-only change, no touch to Android/desktop media session code, no new
  dependencies, no behavior change to play/pause/stop/scrub/track-skip handlers themselves.
- Not verified: actual on-device/simulator lock-screen visual confirmation that the buttons are no
  longer greyed. The fix is verified at the API level (command `.enabled` state) which directly
  controls the lock-screen rendering per Apple's `MPRemoteCommandCenter` behavior, but no screenshot
  evidence was captured.
Changed files:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`
Next owner: user for on-device/simulator visual confirmation; no further automated action pending.
Blockers: none for automated validation; on-device lock-screen visual confirmation not performed.

## Handoff - 2026-07-01 track art thumbnails

Route: openspec+superpowers
Owner: implementation
Scope: Add memory-only cached thumbnail artwork decode path for compact track-list and now-playing-bar artwork.
Root cause: Source inspection showed compact track-row artwork (`AlbumMark` inside `TrackRow`) and `NowPlayingBar` decoded full embedded artwork bytes in Compose surfaces used during list scrolling. Lazy-list row composition could therefore trigger full-size image decode work for a 54dp row mark.
Implementation:
- Added cache-key separation for full-size and thumbnail artwork entries in `ArtworkCache`.
- Added `decodeArtworkThumbnail(maxPixelSize: Int)` expect/actual implementations: Android uses sampled `BitmapFactory` decode plus final scaling; JVM/iOS use Skia raster thumbnail rendering.
- Added `decodeArtworkThumbnailCached(maxPixelSize: Int = 128)` for compact UI surfaces.
- Added `ArtworkCacheTest` coverage for cache bucket separation, empty-cache behavior, and rectangular thumbnail dimension bounds without requiring Skiko native image construction in tests.
- Routed `AlbumMark`/`TrackRow` and compact `NowPlayingBar` through `decodeArtworkThumbnailCached()`.
- Preserved original `artworkBytes` on track/playback models and kept expanded `NowPlayingScreen` on full-size `decodeArtwork()`.
Verification:
- `openspec validate track-art-thumbnails --strict`: pass (`Change 'track-art-thumbnails' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileKotlinJvm :shared:compileAndroidMain --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `rg 'decodeArtwork\\(' shared/src/commonMain/kotlin/com/eterocell/rhythhaus shared/src/androidMain/kotlin/com/eterocell/rhythhaus shared/src/jvmMain/kotlin/com/eterocell/rhythhaus shared/src/iosMain/kotlin/com/eterocell/rhythhaus`: pass; direct full decode remains in platform actuals, `NowPlayingScreen`, legacy `NowPlayingCard`, and common cached full-size helper.
- `rg 'decodeArtworkThumbnailCached' shared/src/commonMain/kotlin/com/eterocell/rhythhaus`: pass; shows `AlbumMark`, `NowPlayingBar`, and helper.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — compact row/bar artwork uses cached thumbnails, expanded Now Playing remains full-size, and original artwork bytes remain available for platform metadata/full artwork.
- Scope controlled: yes — no dependency, SQLDelight schema, scanner, TagLib/native metadata, playback-engine, MediaSession, audio-session, or platform media metadata changes.
- Edge cases/risk reviewed: runtime scroll/FPS improvement still needs manual validation with a large artwork-heavy library; this change removes the source-level full-decode-on-row-composition hotspot.
Changed files:
- `docs/superpowers/plans/2026-07-01-track-art-thumbnails.md`
- `docs/superpowers/specs/2026-07-01-track-art-thumbnails-design.md`
- `openspec/changes/track-art-thumbnails/design.md`
- `openspec/changes/track-art-thumbnails/proposal.md`
- `openspec/changes/track-art-thumbnails/specs/library-ui/spec.md`
- `openspec/changes/track-art-thumbnails/tasks.md`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.ios.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ArtworkCacheTest.kt`
- `progress.md`
Next owner: user for manual scroll-performance validation with a large artwork-heavy library; OpenSpec/user for archive when satisfied.
Blockers: none for automated verification.
Commit: `b139cfa docs: spec track artwork thumbnails`, `dab756f feat: add artwork thumbnail cache`, `e7491ef fix: use thumbnails for compact artwork`, `1ffb579 fix: tighten artwork thumbnail cache tests`, `f9ec7ab docs: record track artwork thumbnail evidence`.

## Handoff - 2026-07-01 ui ux fixes batch

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `ui-ux-fixes-batch`: empty-library onboarding/adaptive album grid, Songs browse mode, Search polish and 44dp compact controls, removal of user-facing TagLib developer panels, and final evidence handoff.
Implementation:
- Added OpenSpec change artifacts under `openspec/changes/ui-ux-fixes-batch/` and Superpowers design/plan documents under `docs/superpowers/`.
- Added common `LibraryBrowserTest` coverage for album-grid breakpoints and `BrowseMode` ordering.
- Added `albumGridColumnsForWidth(widthDp: Float)` and adaptive album-grid rendering; empty Home now shows `ImportAudioCard`.
- Extended browse mode to `Albums, Artists, Songs`; Songs mode renders all tracks and starts playback using the full-library playable queue.
- Added Search query `Clear` action and dismisses Search after a result starts playback.
- Increased BackChip and bottom-bar Search/Settings effective hit targets to at least 44dp while preserving icon sizes/callbacks.
- Removed normal UI TagLib developer panels and dead developer-only panel/helper code from App/NowPlayingScreen.
Verification:
- `openspec validate ui-ux-fixes-batch --strict`: pass (`Change 'ui-ux-fixes-batch' is valid`).
- Task-level focused checks passed during execution, including `:shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest'` and `:shared:compileKotlinJvm` where required.
- `rg 'DEV · TagLib|ALL PROPERTIES|URI source — TagLib requires a filesystem path' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: pass/no matches (`rg_exit=1`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 542ms`).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 2m 43s`).
Acceptance:
- Requirement matched: yes — all five OpenSpec task groups have recorded evidence and task-scoped reviews for Tasks 1-4 found no critical or important issues.
- Scope controlled: yes — no new dependencies; no native platform UI rewrite; no scanner, metadata extraction, playback engine, MediaSession, audio-session, database schema, playlists, genres, folders/sources, recently-added, queue redesign, or stable album identity changes.
- Edge cases/risk reviewed: no live visual QA/device screenshot evidence was performed or claimed; manual cross-device visual validation remains optional.
Changed files:
- `docs/superpowers/plans/2026-07-01-ui-ux-fixes-batch.md`
- `docs/superpowers/specs/2026-07-01-ui-ux-fixes-batch-design.md`
- `openspec/changes/ui-ux-fixes-batch/design.md`
- `openspec/changes/ui-ux-fixes-batch/proposal.md`
- `openspec/changes/ui-ux-fixes-batch/specs/library-ui/spec.md`
- `openspec/changes/ui-ux-fixes-batch/tasks.md`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt`
Next owner: user/OpenSpec for optional manual visual validation and eventual archive of `ui-ux-fixes-batch` when satisfied.
Blockers: none for automated validation.
Commit:
- `defab2b docs: spec ui ux fixes batch`
- `793d3b8 docs: plan ui ux fixes batch`
- `82c0bc8 feat: improve empty library album browsing`
- `db87aed feat: add songs browse mode`
- `dc27d69 fix: polish search and compact controls`
- `21d5810 fix: remove user facing developer panels`
- final evidence update: `docs: record ui ux fixes batch evidence`

## Handoff - 2026-07-01 replace emoji controls with icons

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `replace-emoji-controls-with-icons`: replace transport/search/settings control glyphs with Material vector icons and record evidence.
Implementation:
- Added a shared commonMain Compose Material Icons Extended dependency alias and dependency. The planned `org.jetbrains.compose.material:material-icons-extended:1.11.1` artifact did not resolve, so the icon artifact is pinned to the available JetBrains Compose icon version `1.7.3` while existing Compose Multiplatform dependencies remain unchanged.
- Replaced `NowPlayingBar.kt` play/pause/empty play, search, and settings control `Text` glyphs with `Icon` using `PlayArrow`, `Pause`, `Search`, and `Settings` image vectors.
- Replaced `NowPlayingScreen.kt` previous/play-pause/next transport `Text` glyphs with `Icon` using `SkipPrevious`, `PlayArrow`/`Pause`, and `SkipNext` image vectors.
- Preserved existing control containers, sizes, theme-driven colors/tints, click behavior, playback behavior, navigation behavior, queue behavior, scanner, persistence, platform code, and non-control artwork fallback text.
Verification:
- `openspec validate replace-emoji-controls-with-icons --strict`: pass (`Change 'replace-emoji-controls-with-icons' is valid`).
- `rg '▶|⏸|⏮|⏭|🔍|⚙️' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: pass/no matches (exit 1, empty output).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: initial fail because `org.jetbrains.compose.material:material-icons-extended:1.11.1` was unavailable; pass after pinning icon artifact to `1.7.3` (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — targeted transport/search/settings controls now render vector icons with content descriptions and theme-aware tints instead of targeted emoji/text glyphs.
- Scope controlled: yes — no playback, queue, scanner, persistence, navigation, theme selection, platform-specific code, or out-of-scope artwork fallback changes.
- Edge cases/risk reviewed: automated compile/test verification passed; manual visual confirmation remains optional for icon appearance across devices.
Changed files:
- `gradle/libs.versions.toml`: Material Icons Extended alias/version.
- `shared/build.gradle.kts`: commonMain Material Icons dependency.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`: mini-player vector icons.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: expanded transport vector icons.
- `openspec/changes/replace-emoji-controls-with-icons/tasks.md`: completed tasks and evidence.
- `.superpowers/sdd/replace-emoji-controls-with-icons-report.md`: implementation/verification report.
- `progress.md`: handoff evidence.
Next owner: OpenSpec/user for archive or manual visual validation if desired.
Blockers: none.
Commit: pending semantic commit `fix: replace emoji controls with vector icons`.

## Handoff - 2026-07-01 polish track row selected copy

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `polish-track-row-selected-copy`: selected `TrackRow` user-facing copy and evidence updates.
Implementation:
- Replaced selected `TrackRow` debug/prototype text `queued on shared UI ...%` with `Now playing`.
- Removed now-unused `selectionAlpha` animation state and `animateFloatAsState`/`tween` imports.
- Preserved selected-row highlight, row click behavior, metadata display, duration display, playback, queue semantics, scanner, persistence, navigation, theme selection, and platform-specific code.
Verification:
- `openspec validate polish-track-row-selected-copy --strict`: pass (`Change 'polish-track-row-selected-copy' is valid`).
- `rg 'queued on shared UI|selectionAlpha' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: pass/no matches (exit 1, empty output).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`). Warnings were existing `PredictiveBackHandler` deprecation and expect/actual beta warnings.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`). Warnings were existing Android artwork deprecation, `PredictiveBackHandler` deprecation, and expect/actual beta warnings.
Acceptance:
- Requirement matched: yes — selected rows display `Now playing` and no longer expose debug text or selected-state percentages.
- Scope controlled: yes — implementation code change is isolated to `TrackRow`/imports in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`; evidence files updated.
- Edge cases/risk reviewed: no behavior/state changes; manual visual smoke remains optional for copy appearance.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: selected-row copy and unused animation imports/state removal.
- `openspec/changes/polish-track-row-selected-copy/tasks.md`: completed tasks and evidence.
- `.superpowers/sdd/polish-track-row-selected-copy-report.md`: implementation/verification report.
- `progress.md`: handoff evidence.
Next owner: OpenSpec/user for archive or manual UI visual validation if desired.
Blockers: none.
Commit: `d3255b6 fix: polish selected track row copy`.

## Handoff - 2026-06-30 standardize back navigation

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `standardize-back-navigation`: shared visible back chip, root predictive/system back route-pop handling, Android manifest opt-in, and handoff evidence.
Implementation:
- Added shared commonMain `BackChip` with visible label `‹ Back`, `Back` content description, rounded dark chip styling, and existing `hausClickable` feedback.
- Replaced drill-down `← BACK`, now-playing `← LIBRARY`, and search/settings `< Back` labels with `BackChip` while preserving existing route-pop callbacks.
- Replaced the `LibraryHomeScreen` root `BackHandler(enabled = navigation.canPop)` with `PredictiveBackHandler(enabled = navigation.canPop)` that pops one route after completed predictive/system back progress.
- Removed duplicate child `BackHandler` registrations for drill-down, now playing, settings, search, and clear-library dialog so root navigation owns system/predictive back consumption. Preserved shared left-edge swipe-back gestures for drill-down and now playing.
- Added `android:enableOnBackInvokedCallback="true"` to the Android main activity.
Verification:
- `openspec validate standardize-back-navigation --strict`: pass (`Change 'standardize-back-navigation' is valid`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`). Warning: `PredictiveBackHandler` is deprecated in Compose 1.11.1 in favor of `NavigationEventHandler`, expected/recorded in the plan.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: initial run failed in known/flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --configuration-cache`: pass.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --rerun-tasks --configuration-cache`: pass.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --rerun-tasks --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — all visible targeted back controls use shared `‹ Back`, route-pop callbacks are unchanged, and Android predictive/system back is wired at the route-stack owner with manifest opt-in.
- Scope controlled: yes — no playback, scanner, theme selection, library persistence, or route semantics changed.
- Edge cases/risk reviewed: Android predictive-back visual preview still needs manual Android 13+ emulator/device validation; automated verification proves wiring compiles and broad JVM/desktop/Android checks pass.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `androidApp/src/main/AndroidManifest.xml`
- `openspec/changes/standardize-back-navigation/tasks.md`
- `.superpowers/sdd/standardize-back-navigation-report.md`
- `progress.md`
Next owner: user for manual Android 13+ predictive-back gesture preview/runtime validation.
Blockers: none for automated verification.
Commit: semantic commit with message `feat: standardize back navigation`.

## Handoff - 2026-06-30 theme selection

Route: openspec+superpowers (subagent-driven with coordinator recovery after subagent timeout/stale reports)
Owner: implementation
Scope: Add persisted System/Light/Dark theme selection and light/dark shared Compose palettes using AndroidX DataStore Preferences.
Implementation:
- Added DataStore 1.2.1 dependencies and a shared `ThemePreferenceStore` with Android, iOS, and JVM/macOS actuals.
- Added `RhythHausThemeMode`, stable serialization/parsing, display labels/descriptions, light/dark Haus palettes, and palette resolution tests.
- Wired `App()` to collect the persisted theme mode, resolve System against platform dark-mode state, provide `LocalHausColors`, and choose Miuix light/dark color schemes.
- Migrated shared UI color usage to active palette accessors across App, Settings, Search, Now Playing, bottom bar, and scrubber surfaces.
- Added Settings Appearance section with System/Light/Dark options and persisted selection callback.
Verification:
- `openspec validate theme-selection --strict`: valid.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6 Build 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: BUILD SUCCESSFUL.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL on final rerun. An earlier broad run failed once in known transient `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun of that test passed before the final broad rerun passed.
Acceptance:
- Requirement matched: yes — Settings exposes System/Light/Dark; selection is DataStore-backed and persisted across supported platforms; shared UI resolves light/dark palettes.
- Scope controlled: yes — no SQLDelight preference table, no native settings screens, no playback/scanner/library schema changes.
- Edge cases/risk reviewed: invalid persisted values fall back to System; manual visual validation is still recommended on Android/iOS/macOS for dark-theme aesthetics.
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Theme.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.android.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.ios.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.jvm.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausColors.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicProgressScrubber.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ThemeTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/ThemePreferenceStoreJvmTest.kt`
- `openspec/changes/theme-selection/*`
- `docs/superpowers/specs/2026-06-30-theme-selection-design.md`
- `docs/superpowers/plans/2026-06-30-theme-selection.md`
- `progress.md`
Next owner: user for manual visual validation of dark/light/system themes on devices.
Blockers: none for automated verification.

Update - dropdown theme selector:
- User requested replacing the three Appearance cards with a dropdown list.
- Design approved: one compact shared Compose dropdown row that expands to System, Light, and Dark choices and reuses the existing persisted selection callback.

## Handoff - 2026-06-30 explicit navigation stack

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Replace ad-hoc shared Compose navigation booleans/nullables with explicit route stack.
Implementation:
- Added `LibraryRoute` and `LibraryNavigationStack` with common tests.
- Refactored `LibraryHomeScreen` route rendering for Home, Album Detail, Artist Detail, Now Playing, Search, Settings, and Clear Library dialog.
- Removed top-level `showClearDialog`, `showSettings`, and `showSearch` state from `App()` and `LibraryHomeScreen` parameters.
- Converted bottom-bar and drill-down settings/search entry points to `LibraryRoute.Settings` / `LibraryRoute.Search` pushes.
- Converted Clear Library dialog visibility, dismiss/cancel/confirm, Settings clear-library action, Search dismiss, central Android `BackHandler`, and left-edge/back callbacks to route stack push/pop operations.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: BUILD SUCCESSFUL.
- `openspec validate explicit-navigation-stack --strict`: valid (`Change 'explicit-navigation-stack' is valid`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: initial run failed once in pre-existing/flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun of that test passed; reran full command and it was BUILD SUCCESSFUL. Warnings: Compose `BackHandler` deprecation in favor of NavigationEventHandler; existing expect/actual beta and Android artwork deprecation warnings remain.
Acceptance:
- Requirement matched: yes.
- Scope controlled: yes; no playback behavior changes intended, and no unrelated modified files were touched beyond required OpenSpec/progress evidence.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `openspec/changes/explicit-navigation-stack/tasks.md`
- `progress.md`
Next owner: user for manual Android system/gesture-back validation on device/emulator.
Blockers: none.

## Current state

Last updated: 2026-06-25
Current change: UI polish (button font, next-track sync) + iOS lockscreen player panel
Three-commit bugfix series on main: Clear Library font, NowPlayingScreen next-track staleness, iOS MPRemoteCommandCenter
Workflow route: openspec+superpowers
State source of truth: OpenSpec for durable product changes; Superpowers for clarification/brainstorming/task execution discipline; this file for session continuity and verification evidence.

## Handoff - 2026-06-30 bottom bar insets + Android back navigation

Route: systematic-debugging
Owner: implementation
Input: User reported bottom bar covering album/artist content, missing bottom inset padding when Android navigation bar is hidden, and Android back/swipe-back closing the app instead of returning to the previous in-app screen.
Root cause:
- Main and album/artist drill-down LazyColumns only left a fixed 8dp/80dp trailing spacer while NowPlayingBar is overlaid at the bottom, so final album/artist rows could scroll under the bar.
- NowPlayingBar used `safeContentPadding()`, which does not intentionally reserve a bottom gutter when Android is in gesture/hidden-navigation mode.
- Shared Compose navigation state was local (`selectedAlbum`, `selectedArtist`, `showNowPlayingScreen`, overlays), but no Compose back handler consumed Android system back gestures/buttons before the Activity default finished the app.
Output:
- `NowPlayingBar.kt`: bottom bar now uses `navigationBarsPadding()` plus an explicit 12dp bottom gutter and 16dp side inset; exported `NowPlayingBarContentPadding = 144.dp` for list content clearance.
- `App.kt`: main and drill-down lists now use the shared bottom content spacer; album/artist drill-down, settings/search overlays, and clear dialog register Compose `BackHandler` callbacks to pop/dismiss instead of exiting the app.
- `NowPlayingScreen.kt`: expanded now-playing screen registers `BackHandler(onBack)` so Android back returns to the prior screen.
- `gradle/libs.versions.toml` and `shared/build.gradle.kts`: added the Compose `ui-backhandler` dependency needed for shared back handling.
Verification:
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL. Warnings: Compose BackHandler is deprecated in favor of NavigationEventHandler; existing expect/actual and Android artwork deprecation warnings remain.
Acceptance:
- Requirement matched: yes for bottom list safe area, extra bottom bar inset in hidden-nav/gesture mode, and Android system back handling for in-app screens/overlays.
- Scope controlled: yes; only shared Compose layout/navigation and the dependency alias were changed.
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
Next owner: user for manual Android gesture-nav validation on device/emulator.
Blockers: none for compile/test verification.

## Handoff - 2026-06-30 Android hardware media-button controls (IMPLEMENTED, awaiting device validation)

Route: openspec+superpowers (durable playback-architecture change per AGENTS.md), executed via subagent-driven-development (reviewer subagent gate).
Owner: implementation -> awaiting user on-device validation before OpenSpec archive.
Input: User report "cable control is not working on Android just like airpods control wouldn't work on iOS previously." User approved the full MediaSessionService refactor.
Root cause (systematic-debugging Phase 1, refined by reading media3 1.10.1 source): the Android engine built a standalone Activity-scoped Media3 `MediaSession` (`PlaybackEngine.android.kt`) with no `MediaSessionService`, no audio focus, a single `setMediaItem` (so next/prev had nothing to act on), and no audio-becoming-noisy handling. media3 does self-register a runtime button receiver for an active session, so the dominant real gaps were missing audio focus + foreground service surface + queue + becoming-noisy.
Output:
- New `shared/src/androidMain/.../RhythHausPlaybackService.kt`: `MediaSessionService` hosting ExoPlayer with `AudioAttributes(usage=MEDIA, content=MUSIC, handleAudioFocus=true)` + `setHandleAudioBecomingNoisy(true)`; wraps player in `SkipRoutingPlayer` (ForwardingPlayer) advertising next/prev and routing them to the bridge. `onTaskRemoved` keeps service alive while playing (no super, canonical pattern).
- New `shared/src/androidMain/.../RhythHausTransportBridge.kt`: process-level @Volatile skip handlers so the service player drives the shared controller's queue (single source of truth; no forked playlist).
- `PlaybackEngine.android.kt`: restructured to connect via async `MediaController` (`SessionToken` + `buildAsync`); FIFO `pendingActions` queue (fixes load+play-before-connect race); `disposed` guard against connection-callback resurrection; `release()` cancels scope + tears down controller/future/bridge; listener setter wires bridge -> `onSkipToNext/onSkipToPrevious`.
- `androidApp/.../MainActivity.kt`: requests `POST_NOTIFICATIONS` at runtime on API 33+ (backs lock-screen/notification transport surface).
- `androidApp/src/main/AndroidManifest.xml`: declares the service (`foregroundServiceType=mediaPlayback`), `MediaButtonReceiver`, and `FOREGROUND_SERVICE`/`FOREGROUND_SERVICE_MEDIA_PLAYBACK`/`POST_NOTIFICATIONS` permissions.
- New `shared/src/androidHostTest/.../RhythHausTransportBridgeTest.kt`: 4 tests (handler invocation, no-op safety, engine listener->bridge wiring).
- OpenSpec change `openspec/changes/android-media-button-controls/` (proposal/design/tasks/specs); `openspec validate --strict` -> valid.
Reviewer subagent (spec+quality gate) found 2 Critical + 3 Important; all addressed: C1 single-slot pendingAction overwrite -> FIFO queue; C2 missing POST_NOTIFICATIONS runtime request -> added in MainActivity; I1 uncancelled scope -> scope.cancel() in release(); I2 release-during-connect leak -> disposed guard releases late-arriving controller; I3 double-release -> branch on controller!=null. M1 skip wrap-around is pre-existing PlaybackController behavior (unchanged).
Verification: `./gradlew :androidApp:assembleDebug` -> BUILD SUCCESSFUL; `:shared:jvmTest` + `:shared:testAndroidHostTest` -> BUILD SUCCESSFUL; merged debug manifest confirmed to contain the service (mediaPlayback), MediaButtonReceiver, and FOREGROUND_SERVICE_MEDIA_PLAYBACK. Not committed (pre-existing unrelated working-tree changes present; AGENTS.md commits only when fully complete).
Next owner: user for on-device validation matrix — wired cable inline-remote play/pause, Bluetooth play/pause, lock-screen/notification next/previous, unplug-mid-playback auto-pause, and POST_NOTIFICATIONS prompt on API 33+. Residual untested seam: async MediaController connection ordering (not device-independently testable; covered by the FIFO fix + disposed guard but needs runtime confirmation).
Blockers: cable/BT/lock-screen behavior cannot be validated in this environment (no physical device); pre-existing ktlint error in SwipeBackGesture.kt (untouched file) fails repo-wide spotlessApply.

## Handoff - 2026-06-30 Android hardware media-button controls (AWAITING APPROVAL)

Route: openspec+superpowers (durable playback-architecture change per AGENTS.md)
Owner: OpenSpec (proposal staged) -> awaiting user approval before implementation
Input: User report "cable control is not working on Android just like airpods control wouldn't work on iOS previously."
Root cause (systematic-debugging Phase 1): `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt:45` builds a standalone Media3 `MediaSession` in Activity-scoped code (`MainActivity` -> `setRhythHausAndroidContext`). The OS delivers hardware media buttons (wired cable inline remote, Bluetooth) as `android.intent.action.MEDIA_BUTTON` broadcasts, which require a registered `androidx.media3.session.MediaButtonReceiver` backed by a `MediaSessionService` to receive/route them. Verified absent: no `MediaSessionService`, no `MediaButtonReceiver`, no `<service>`/`<receiver>` in `androidApp/src/main/AndroidManifest.xml` (only the launcher activity). A bare Activity-scoped session has no delivery target, so the broadcasts are dropped. Android twin of the prior iOS AirPods bug (missing `MPRemoteCommandCenter` handlers). Secondary gaps: single `setMediaItem` (no queue -> next/prev have nothing to skip to) and no audio-becoming-noisy handling (no auto-pause on unplug).
Output: Staged OpenSpec change `openspec/changes/android-media-button-controls/` (proposal.md, design.md, tasks.md, specs/android-media-controls/spec.md ADDED, specs/audio-playback/spec.md MODIFIED). `openspec validate android-media-button-controls --strict` -> valid. NO code changed (durable playback-architecture work requires user-approved spec+plan per AGENTS.md; cable/BT controls need on-device manual validation unavailable here).
Verification: spec validation only; no build run for this change (no code touched).
Next owner: user to approve the proposal/plan, then implementation owner (subagent-driven-development) executes tasks.md; manual device matrix (cable remote, BT, lock-screen next/prev, unplug auto-pause) required before archive.
Blockers: awaiting user approval; manual on-device validation cannot run in this environment.

Note: also regenerated Android launcher icons from `icons/dark_mode.svg` this session (gradient adaptive foreground + 10 mipmap PNGs); `:androidApp:assembleDebug` BUILD SUCCESSFUL. Separate from the media-button change.

## Handoff - 2026-06-30 iOS archive version sync

Route: systematic-debugging
Owner: implementation
Input: User observed that running Archive in Xcode does not trigger `./gradlew syncIosVersionXcconfig`, leaving `iosApp/Configuration/Version.xcconfig` stale after editing root `gradle.properties`.
Root cause: `Version.xcconfig` is read while Xcode resolves build settings for archive, but the existing target build phase only runs `:shared:embedAndSignAppleFrameworkForXcode`; no archive/run pre-action invokes `syncIosVersionXcconfig` before build settings are evaluated. After commit `400bcbe` changed root version to 0.0.3, `Version.xcconfig` still contained 0.0.2 and `xcodebuild -showBuildSettings` resolved iOS version values to 0.0.2.
Output: Added an Xcode scheme pre-action that runs `./gradlew syncIosVersionXcconfig --configuration-cache` from the repo root with the same `JAVA_HOME`/Homebrew PATH setup used by the Kotlin framework build phase. Synced committed `Version.xcconfig` to 0.0.3. The local ignored user scheme was also updated so this developer's current Xcode scheme triggers the pre-action immediately.
Verification: A Python assertion comparing `gradle.properties` to `xcodebuild -showBuildSettings` failed before sync (`MARKETING_VERSION` stale: expected 0.0.3, actual 0.0.2). After adding the pre-action and intentionally resetting `Version.xcconfig` stale, `xcodebuild ... archive CODE_SIGNING_ALLOWED=NO` ran `:syncIosVersionXcconfig`, completed with `** ARCHIVE SUCCEEDED **`, rewrote `Version.xcconfig` to 0.0.3/000003, and the archive Info.plist reported CFBundleShortVersionString 0.0.3 and CFBundleVersion 000003.
Next owner: user for normal signed Xcode Archive/Organizer validation.
Blockers: none for unsigned archive verification.

## Handoff - 2026-06-30 Android TagLib SAF metadata handoff

Route: systematic-debugging
Owner: implementation
Input: Android imports many audio files but album/metadata fields remain fallback values, unlike the previous iOS metadata behavior.
Root cause: Android SAF scanner stored and passed `AudioSource.Uri(content://...)` into metadata enrichment. `AudioMetadataReader` intentionally returns null for `AudioSource.Uri`, and native TagLib reads filesystem paths only. The Android TagLib `.so` packaging was present, but the scan path never handed TagLib a readable file path for SAF documents.
Output: Added a separate `metadataAudioSource` to `AudioScanCandidate` so playback can preserve the original content URI while metadata enrichment can use a temporary filesystem path. Android SAF scanning now copies supported audio documents into an app cache file under `context.cacheDir/rhythhaus-taglib/<sourceId>/...` and supplies that cache path to TagLib. Common scanner regression coverage verifies metadata is read from the separate filesystem source while the stored/playback source remains the original URI.
Verification: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri' --configuration-cache` -> BUILD SUCCESSFUL; `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache` -> BUILD SUCCESSFUL; `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; APK contains `librhythhaus_taglib.so` for arm64-v8a, armeabi-v7a, and x86_64. Full `:shared:jvmTest` currently has unrelated macOS native playback failures (`No native macOS player has been loaded`) in `JvmPlaybackEngineTest`, outside this Android metadata path.
Next owner: user for manual Android re-scan/runtime validation with real tagged SAF audio; clear/re-scan may be needed for already-imported fallback metadata rows.
Blockers: none for Android compile/APK packaging; full JVM suite has unrelated macOS playback test failures.

## Handoff - 2026-06-30 music progress scrubber

Route: openspec+superpowers
Owner: implementation
Input: User requested a music-player progress slider that supports single-click destination seeking and avoids multiple intermediate seeks while dragging.
Output: Added shared Compose `MusicProgressScrubber` with pure seek math and interaction-state tests; replaced expanded now-playing Miuix `Slider` with one-shot tap and drag-release seeking. Follow-up fix made scrub preview state Compose-observable and cancellation cleanup robust. `NowPlayingBar` remains passive progress.
Verification: Task 1 and Task 2 implementers ran focused `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.MusicProgressScrubberTest' --configuration-cache` and `./gradlew :shared:compileKotlinJvm --configuration-cache` with BUILD SUCCESSFUL. Task reviews passed clean after the observable-preview fix. Final `./init.sh` -> BUILD SUCCESSFUL, including shared JVM tests, desktop compile, Android debug build, Xcode 26.6 Build 17F113, and iOS simulator tests.
Next owner: user for manual Android/iOS/macOS interaction validation with real playback: tap-to-seek and drag-release seeking should produce no audible intermediate jumps.
Blockers: none for automated verification.

## Handoff - 2026-06-30 iOS audio-session UI unresponsiveness warning

Route: systematic-debugging
Owner: implementation
Input: Xcode/runtime warnings in `PlaybackEngine.ios.kt` at prepare/configure audio-session sites: `AVAudioPlayer.prepareToPlay`, `AVAudioSession.setCategory`, and `AVAudioSession.setActive` can lead to UI unresponsiveness when called on the main thread.
Root cause: shared `PlaybackController` engine work was already asynchronous, but the iOS `playbackEngineDispatcher` actual still used `Dispatchers.Main`, so iOS backend load/configuration work still ran the blocking Apple audio calls on the UI thread.
Output: Added iOS regression coverage in `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt` asserting iOS playback engine work does not use `Dispatchers.Main`; changed `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackDispatchers.ios.kt` to `Dispatchers.Default` while Android stays Main and JVM stays IO.
Verification: Targeted iOS dispatcher regression first failed, then passed. `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> BUILD SUCCESSFUL. `./init.sh` -> BUILD SUCCESSFUL, including shared JVM tests, desktop compile, Android debug build, Xcode 26.6 Build 17F113, and iOS simulator tests.
Next owner: user for manual iOS runtime validation that warnings are gone during real playback load/start.
Blockers: none for automated verification.



## Handoff - 2026-06-30 iOS track-switch blast beep mitigation

Route: systematic-debugging
Owner: implementation
Input: Possible blast/beep artifact when switching between tracks.
Root cause hypothesis: iOS track switching called `release()` from `load()`, which immediately stopped the current `AVAudioPlayer` while the next player was being prepared. Abrupt stop/disposal at a non-zero waveform crossing can produce an audible transient, especially with fast auto-advance/skip.
Output: Added a dedicated iOS track-switch teardown path that fades the current `AVAudioPlayer` volume to silence over 50 ms before stopping it, without clearing Now Playing state like a full user-facing release. Added iOS regression coverage for the soft teardown constants/strategy.
Verification: Targeted iOS regression first failed to compile before the production symbols existed, then passed after implementation. `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> BUILD SUCCESSFUL. `./init.sh` -> BUILD SUCCESSFUL, including shared JVM tests, desktop compile, Android debug build, Xcode 26.6 Build 17F113, and iOS simulator tests.
Next owner: user for manual iOS runtime listening test while rapidly skipping and auto-advancing between real tracks.
Blockers: none for automated verification.

## Subagent-driven execution outcome

Used Subagent-Driven Development on docs/superpowers/plans/2026-06-11-taglib-metadata-module.md (native TagLib wrapper plan). Assessment subagent confirmed: Tasks 1-3 (module/API, C ABI shim, JVM/macOS JNI) are complete with pinned upstream TagLib v2.3 FetchContent builds and real fixture tests passing; Tasks 4/5 Android/iOS remaining as honest unsupported scaffolds; Task 6 shared integration complete; Task 7 OpenSpec/docs complete.

Android native packaging subagent was dispatched but hit an HTTP 429 rate limit mid-task. Incomplete changes were cleaned up. The native TagLib Android NDK/CMake per-ABI builds remain the next feasible implementation gap.

## Completed

- Initialized a first shared Compose Multiplatform product surface for RhythHaus.
- Added shared demo music models and formatting tests.
- Scoped desktop native packaging to macOS DMG only for current target scope.
- Confirmed OpenSpec is initialized via `openspec/` and `openspec/config.yaml`.
- Created project agent harness files:
  - `AGENTS.md`
  - `docs/harness-engineering.md`
  - `init.sh`
  - `progress.md`

## In progress

- OpenSpec change `play-music-all-platforms` has first implementation slice completed and validated.
  - Proposal: `openspec/changes/play-music-all-platforms/proposal.md`
  - Design: `openspec/changes/play-music-all-platforms/design.md`
  - Spec: `openspec/changes/play-music-all-platforms/specs/audio-playback/spec.md`
  - Tasks: `openspec/changes/play-music-all-platforms/tasks.md`
  - Implementation: shared playback model/controller/UI plus Android Media3, iOS AVFAudio, and macOS AVFoundation Objective-C++/JNI engine.
  - Validation: `./init.sh` -> BUILD SUCCESSFUL on 2026-06-10.
  - Follow-up backend implementation: Android playback migrated from platform `MediaPlayer` to Media3/ExoPlayer; macOS/JVM playback migrated from Java Sound `Clip` to native AVFoundation through a temporary JNA bridge, then replaced with a small Objective-C++ helper called through JNI; iOS remains on native AVFAudio `AVAudioPlayer`. MacOS/JVM playback now starts a daemon scheduled progress publisher while playing so the shared Compose progress slider advances continuously instead of only updating on play/pause/seek events.
  - Follow-up validation: added JVM regression test `nativeMacPlaybackEnginePublishesProgressWhilePlaying`; first run failed at `JvmPlaybackEngineTest.kt:105` because no periodic progress events were emitted; after the fix, targeted test passed. `openspec validate play-music-all-platforms` -> valid; `openspec validate import-local-audio` -> valid; `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; `/usr/bin/xcrun xcodebuild -version` -> Xcode 26.5 Build 17F42; `./gradlew :shared:iosSimulatorArm64Test :desktopApp:packageDmg --configuration-cache` -> BUILD SUCCESSFUL and produced `desktopApp/build/compose/binaries/main/dmg/RhythHaus-1.0.0.dmg`; `jar tf shared/build/libs/shared-jvm.jar | grep -E 'native/.*/librhythhaus_audio.dylib'` -> `native/macos-aarch64/librhythhaus_audio.dylib`.
- OpenSpec change `import-local-audio` has first manual import slice completed and validated.
  - Proposal: `openspec/changes/import-local-audio/proposal.md`
  - Design: `openspec/changes/import-local-audio/design.md`
  - Spec: `openspec/changes/import-local-audio/specs/local-audio-import/spec.md`
  - Tasks: `openspec/changes/import-local-audio/tasks.md`
  - Implementation: shared import model/UI, Android document picker, macOS/JVM native Finder-style file dialog, iOS unsupported-state placeholder.
  - Validation: `./init.sh` -> BUILD SUCCESSFUL on 2026-06-10; `openspec validate import-local-audio` -> valid.
  - Follow-up update: removed sample/demo library playback path and `AudioSource.DemoTone`; empty library now prompts for local import only. Replaced macOS/JVM Swing `JFileChooser` with native AWT `FileDialog` so macOS opens the system Finder-style panel.
  - Follow-up validation: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; `/usr/bin/xcrun xcodebuild -version` -> Xcode 26.5 Build 17F42; `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> BUILD SUCCESSFUL; `openspec validate import-local-audio` -> valid.

## Next steps

1. Manually validate foreground play/pause/seek on Android device/emulator and macOS using real local audio files imported through the UI.
2. Verify packaged macOS DMG runtime behavior for the native AVFoundation Objective-C++/JNI helper.
3. Keep iOS playback on native Apple audio APIs; decide whether the existing Kotlin/Native `AVAudioPlayer`, AVFoundation `AVPlayer`, or a Swift bridge best fits the iOS import/media-library path.
4. Plan the iOS document-picker bridge so iOS can import local files instead of showing the current unsupported-state message.

## Handoff - 2026-06-24 Now Playing Panel + Track Ordering + Artwork Display

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Transform NowPlayingCard into clickable floating NowPlayingBar with expand-to-full-screen NowPlayingScreen; order album/artist tracks by track number instead of alphabetically; display album/track/artist artwork images everywhere instead of text placeholders.

Implementation:
- Task 1 (data models): Added trackNumber, discNumber, artworkBytes, artworkMimeType to AudioMetadata, LibraryTrack, and Track models. Updated SQLDelight schema (4 new columns in library_track table), all repository queries, library scanner, and UI-mapping functions (librarySnapshot, toUiTrack) to flow the new fields end-to-end.
- Task 2 (ordering): Changed all 4 track-grouping functions in LibraryBrowser.kt from alphabetical sortedBy to discNumber → trackNumber → title ordering.
- Task 3 (artwork display): Updated AlbumMark, AlbumCard, and ArtistRow composables to decode and show artwork Image with ContentScale.Crop when available, falling back to existing text placeholders.
- Task 4 (NowPlayingBar): Created new floating bar composable with mini progress bar, artwork thumbnail, track info, and play/pause button. Extracted HausColors.kt to share color constants.
- Task 5 (NowPlayingScreen): Created full-screen expanded view with large artwork, track metadata (including track number), seek bar, and transport controls (stop/play-pause/next).
- Task 6 (wiring): Replaced inline NowPlayingCard in LibraryHomeScreen and DrillDownView with Box-overlayed NowPlayingBar at bottom and expandable NowPlayingScreen.

Verification:
- `./init.sh`: BUILD SUCCESSFUL — shared JVM tests, desktop compile, Android debug APK, iOS simulator tests all pass.
- 7 commits: 5f93931, 8d6501a (fix), a7468bc, 17b0ebb, dbf2932, f8bf98e, 509a5a9

Acceptance:
- Requirement matched: yes for all 3 features (floating bar + expand, track-number ordering, artwork display).
- Scope controlled: yes; only data model, ordering, artwork, and UI changes. No platform-specific or unrelated changes.
- Remaining risk: (a) iOS artwork decode returns null — artwork falls back to text placeholders on iOS; (b) SQL schema migration requires fresh install for existing dev databases; (c) manual visual confirmation of artwork rendering with real embedded-artwork audio files on Android/macOS.

Changed files:
- AudioMetadata.kt, MusicModels.kt, LibraryModels.kt: extended data models
- LibraryTrack.sq: SQL schema +4 columns
- SqlDelightLibraryRepository.kt: new column read/write
- LibraryScanner.kt: pass metadata through
- LibraryBrowser.kt: track-number ordering
- App.kt: artwork display in 3 composables + wiring NowPlayingBar/Screen
- HausColors.kt, NowPlayingBar.kt, NowPlayingScreen.kt: new composables

Next owner: user for manual visual/runtime validation.
Blockers: none for compile/test verification.

## Decisions

- First platform scope: Android, iOS, macOS/desktop JVM.
- Windows/Linux support is future scope only.
- Use shared-first Compose Multiplatform UI.
- OpenSpec owns durable requirements/specs/tasks because `openspec/` exists.
- Playback backend direction: Android uses Media3/ExoPlayer, iOS uses native Apple audio APIs, and macOS uses a native AVFoundation Objective-C++/JNI helper rather than Java Sound or JNA for product-grade playback.
- Superpowers owns clarification, brainstorming, task execution discipline, and TDD-style implementation loops for durable work.
- Do not create `feature_list.json` for OpenSpec-owned tasks.
- Completed OpenSpec + Superpowers workflow changes should be committed by default unless the user explicitly says not to commit.
- Commit messages should use semantic/conventional style such as `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, or `chore: ...`.
- Harness owns verification, acceptance, scope, lifecycle, and handoff evidence.

## Verification evidence

Latest successful harness verification:

```bash
./init.sh
```

Result: BUILD SUCCESSFUL for both Gradle phases. Details from 2026-06-10 playback implementation:

- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.5, Build version 17F42.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: BUILD SUCCESSFUL.

Harness verification command to use going forward:

```bash
./init.sh
```

## Changed files in current playback work

- `gradle/libs.versions.toml` - added shared coroutine dependency alias.
- `shared/build.gradle.kts` - added `kotlinx-coroutines-core` to common code, Android Activity Compose and Media3 to Android shared source set, and a macOS native audio helper build/resource task for JVM.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt` - shared playback domain, controller, engine contract, fake engine, and formatting helper.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioImport.kt` - shared imported-audio model, import launcher contract, and imported-library mapping.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt` - added `AudioSource` to `Track` so imported rows are playable.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` - shared now-playing playback controls, import card, seek display, status/error display, and accessibility content descriptions.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt` - Android Media3/ExoPlayer engine with context-backed URI playback.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioImport.android.kt` - Android `OpenMultipleDocuments` audio picker.
- `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt` - provides Android application context for content URI playback.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` - iOS `AVAudioPlayer` engine and foreground audio session setup.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt` - iOS unsupported import placeholder with user-facing copy.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt` - macOS native AVFoundation-backed playback engine through an Objective-C++/JNI helper.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioImport.jvm.kt` - macOS/JVM native AWT file dialog importer.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/SharedCommonTest.kt` - playback and import mapping tests.
- `openspec/changes/play-music-all-platforms/design.md` - recorded first-slice engine and format decisions.
- `openspec/changes/play-music-all-platforms/tasks.md` - marked implemented/verified tasks and remaining manual validation.
- `openspec/changes/import-local-audio/*` - planned, specified, and task-tracked manual local audio import.
- `progress.md` - updated handoff/evidence.

## Completion evidence checklist

- [x] Workflow route recorded: `openspec+superpowers`.
- [x] Current owner recorded: harness-creator for harness files; OpenSpec for future durable product tasks.
- [x] Fact source conflict avoided: no `feature_list.json` created because OpenSpec is initialized.
- [x] Verification commands documented in `AGENTS.md`, `docs/harness-engineering.md`, and `init.sh`.
- [x] Known platform scope recorded.
- [x] Next safe action recorded.

## Handoff

Route: openspec+superpowers
Owner: implementation
Input: corrected Task 7 request to record native TagLib wrapper architecture and current platform state in OpenSpec/progress/docs
Output: `openspec/changes/import-local-audio/design.md` documents that rich import metadata flows through the native `:taglib` wrapper seam, not hand-written Kotlin parsers; `openspec/changes/import-local-audio/tasks.md` tracks the documentation follow-up and remaining native TagLib linking/packaging work; `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md` is preserved as the corrected native-wrapper plan and points the next action at real TagLib library linking per platform.
Next owner: implementation for platform native TagLib linking/packaging
Blockers: real rich metadata support remains blocked until native TagLib is linked/packaged per platform: macOS/JVM helper currently supports skeleton unsupported behavior unless TagLib is available at build/link time; Android has JNI-shaped scaffold but no packaged native library; iOS has unsupported scaffold and documented expected native layout but no cinterop yet.

## Handoff - 2026-06-11 native TagLib metadata docs

Route: openspec+superpowers
Owner: implementation
Scope: OpenSpec/progress/docs only; no source/build files changed.
Verification:
- `openspec validate import-local-audio --strict`: pass (`Change 'import-local-audio' is valid`).
Acceptance:
- Requirement matched: yes; docs record native TagLib wrapper architecture and reject hand-written Kotlin metadata parsing.
- Scope controlled: yes; changes limited to import-local-audio OpenSpec docs, progress, and the corrected Superpowers plan.
- Edge cases/risk reviewed: Android/iOS/macOS current support is documented honestly; no completed Android/iOS rich metadata support is claimed.
Changed files:
- `openspec/changes/import-local-audio/design.md`: native-wrapper metadata architecture, current platform state, and linking/packaging risks.
- `openspec/changes/import-local-audio/tasks.md`: Task 5 documentation follow-up and remaining real TagLib linking task.
- `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md`: corrected plan included under docs and updated Task 7/current next action.
- `progress.md`: handoff evidence for this docs task.
Next owner: implementation
Blockers: none for docs; real metadata support still requires linking/packaging native TagLib libraries per platform before claiming full support.
Commit: docs task commit created after this handoff update with message `docs: record native taglib import metadata plan`.

## Handoff - 2026-06-11 native TagLib wrapper full verification

Route: openspec+superpowers
Owner: harness-creator
Scope: verification-only final review for native `:taglib` wrapper work at HEAD `e54d788`; no source changes.
Verification:
- Initial `git status --short && git rev-parse --short HEAD`: pass; worktree was clean and HEAD was `e54d788`.
- `./init.sh`: pass; completed with `=== Harness verification complete ===` after Gradle reported `BUILD SUCCESSFUL` for the documented harness phases.
- `./gradlew :taglib:jvmTest :taglib:assembleAndroidMain :taglib:iosSimulatorArm64Test --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL in 1s`, 30 actionable tasks, configuration cache entry stored.
- `cmake -S taglib/native -B taglib/build/cmake-verify && cmake --build taglib/build/cmake-verify`: pass; CMake configured and built `librhythhaus_taglib.dylib`. Output also reported `TagLib was not found by CMake find_package(TagLib) or pkg-config; building unsupported shim skeleton only.` Shell startup emitted non-fatal local profile noise: `bash: ${candidate_name^^}: bad substitution` and missing broot launcher path.
- `openspec validate import-local-audio --strict`: pass; output `Change 'import-local-audio' is valid`.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `taglib/src`: pass; no matches.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `shared/src`: pass; only `Uri.parse(value)` in Android playback URI conversion matched, not metadata parsing.
Acceptance:
- Requirement matched: yes; full harness, focused taglib Gradle tasks, CMake shim configure/build, OpenSpec validation, and no-Kotlin-parser search were executed successfully.
- Scope controlled: yes; verification evidence only.
- Edge cases/risk reviewed: real TagLib linkage/packaging remains incomplete per platform; current CMake build confirms the unsupported skeleton path when TagLib is not discoverable locally.
Changed files:
- `progress.md`: added this final verification evidence.
Next owner: implementation or user for real TagLib linkage/packaging per platform.
Blockers: none for verification; remaining product limitation is that rich metadata support still needs real TagLib library linkage/packaging on macOS/JVM, Android, and iOS before claiming full platform metadata support.
Commit: docs verification commit with message `docs: record native taglib verification evidence`.

## Handoff - 2026-06-11 upstream TagLib JVM/macOS verification

Route: openspec+superpowers
Owner: harness-creator
Scope: verification and evidence recording for upstream TagLib JVM/macOS build/link correction at HEAD `3849c941769890862bba3da89fef3303ec679b8c`; no source/build files changed.
Upstream TagLib correction commits:
- `aa16f826` `feat: build upstream taglib for jvm reader`: Gradle fetches/builds pinned upstream `https://github.com/taglib/taglib` v2.3 commit `1b94b93762636ebe5733180c3e825be4621e4c7f`, statically links `libtag.a` into the macOS/JVM JNI helper, and builds with `RH_TAGLIB_HAS_TAGLIB=1`.
- `ae30fd1` `test: verify jvm taglib reads real fixture`: JVM test generates a WAV RIFF/INFO fixture and asserts real `createTagLibReader`/`readPath` returns `Found` through JNI/C ABI/upstream TagLib.
- `3849c94` `docs: plan upstream taglib mobile builds`: OpenSpec/docs clarify Android/iOS still need upstream TagLib builds packaged and wired from the same pinned source.
Verification:
- Initial `git status --short && git rev-parse HEAD`: pass; worktree was clean and HEAD was `3849c941769890862bba3da89fef3303ec679b8c`.
- `./init.sh`: pass; completed with `=== Harness verification complete ===` after Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :taglib:allTests --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; `:taglib:checkoutUpstreamTagLib` output `HEAD is now at 1b94b93 Version 2.3`; `:taglib:jvmTest` and `:taglib:iosSimulatorArm64Test` passed/up-to-date.
- `./gradlew :taglib:buildMacosTagLibHelper --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; shell startup emitted non-fatal local profile noise `bash: ${candidate_name^^}: bad substitution` and missing broot launcher path.
- `openspec validate import-local-audio --strict`: pass; output `Change 'import-local-audio' is valid`.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `taglib/src`: pass; no matches.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `shared/src`: pass; only Android playback URI conversion matched (`AudioSource.Uri -> Uri.parse(value)`), unrelated to metadata parsing.
- Linkage check on `taglib/build/generated/nativeTagLibResources/jvmMain/native/macos-aarch64/librhythhaus_taglib.dylib`: pass; `file` reported `Mach-O 64-bit dynamically linked shared library arm64`; `otool -L` showed only system dylibs (`libc++.1.dylib`, `libSystem.B.dylib`) besides itself, consistent with static TagLib linkage; `nm -gU ... | grep -E 'TagLib|rh_taglib|Java_com_eterocell_rhythhaus_taglib'` showed `_Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPathNative` and many `TagLib` symbols such as `__ZN6TagLib10ByteVector10fromBase64ERKS0_`.
Acceptance:
- Requirement matched: yes; JVM/macOS now actually builds, statically links, and tests upstream TagLib v2.3 from the pinned upstream commit.
- Scope controlled: yes; only `progress.md` evidence changed in this task.
- No custom Kotlin parser claim: confirmed; targeted source search did not find metadata parser implementation under `taglib/src` or `shared/src`.
Remaining limitations:
- Android still needs upstream TagLib v2.3 built for supported ABIs, packaged, and wired through JNI before Android rich metadata support can be claimed.
- iOS still needs upstream TagLib v2.3 built/packaged, cinterop/native wiring completed, and tests before iOS rich metadata support can be claimed.
- The project must not claim a custom Kotlin metadata parser; metadata support is through native TagLib wrapper/linkage.
Changed files:
- `progress.md`: added this upstream TagLib JVM/macOS verification handoff/evidence.
Next owner: implementation for Android/iOS upstream TagLib packaging and wiring.
Blockers: none for JVM/macOS verification; remaining product limitation is mobile native TagLib packaging/wiring.
Commit: docs verification commit with message `docs: record upstream taglib verification evidence`.

## Handoff - 2026-06-11 CMake FetchContent TagLib final verification

Route: openspec+superpowers
Owner: harness-creator
Scope: final verification and evidence recording for CMake FetchContent TagLib refactor at HEAD `f263e987db85e4dc70e9e69a00203e3d1f858426`; no source/build files changed.
CMake FetchContent correction:
- Upstream TagLib import/build now lives self-contained in `taglib/native/CMakeLists.txt` via CMake `FetchContent` pinned to `1b94b93762636ebe5733180c3e825be4621e4c7f`.
- Gradle no longer performs upstream git clone/checkout; it invokes CMake and copies the generated helper dylib into JVM resources.
Verification:
- Initial `git status --short && git rev-parse HEAD`: pass; worktree was clean and HEAD was `f263e987db85e4dc70e9e69a00203e3d1f858426`.
- `./gradlew :taglib:buildMacosTagLibHelper --rerun-tasks --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; CMake configured/generated and built `librhythhaus_taglib.dylib`; output noted bundled utfcpp from TagLib source and non-fatal local shell startup noise from the user's bash profile.
- `./gradlew :taglib:jvmTest --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; `:taglib:jvmTest` was up-to-date with helper built/up-to-date.
- `./gradlew :taglib:allTests --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; JVM and iOS simulator taglib tests were up-to-date.
- `openspec validate import-local-audio --strict`: pass; output `Change 'import-local-audio' is valid`.
- Linkage check on `taglib/build/native/macosTagLibHelper-arm64/librhythhaus_taglib.dylib`: pass; `otool -L` showed only itself plus system `libc++.1.dylib` and `libSystem.B.dylib`; `nm -gU` showed exported JNI symbol `_Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPathNative`.
- Targeted Gradle search in `taglib/build.gradle.kts` for `git clone|checkoutUpstreamTagLib|Exec\(|git\s+checkout`: pass; no matches.
- Targeted Kotlin parser search in `taglib/src` for parser signatures/ID3/MPEG/RandomAccessFile/ByteBuffer/synchsafe/readBytes: pass; no matches.
Acceptance:
- Requirement matched: yes; CMake-owned upstream TagLib import was freshly verified with build/test/OpenSpec/linkage/search evidence.
- Scope controlled: yes; only `progress.md` evidence changed in this task.
- No custom Kotlin parser claim: confirmed by targeted search; metadata remains through the native TagLib wrapper path.
Changed files:
- `progress.md`: added this CMake FetchContent final verification handoff/evidence.
Next owner: implementation/user for any remaining Android/iOS native TagLib packaging/wiring beyond this macOS/JVM verification.
Blockers: none for this verification.
Commit: docs verification commit with message `docs: record cmake taglib import evidence`.

## Handoff - 2026-06-11 local folder scanning SQLDelight setup

Route: openspec+superpowers
Owner: implementation
Scope: Task 2 only for `scan-local-audio-folders`: SQLDelight version catalog aliases, shared module SQLDelight plugin/database configuration, platform driver dependencies, and initial library schema/queries.
Verification:
- Initial `git status --short --branch`: pass; worktree was clean on `main...egl/main` before edits.
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: initial fail in `:shared:generateCommonMainRhythHausDatabaseInterface` because default SQLDelight SQLite 3.18 dialect did not parse `INSERT ... ON CONFLICT ... DO UPDATE` from the approved plan schema.
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: pass after configuring SQLDelight SQLite 3.38 dialect; Gradle reported `BUILD SUCCESSFUL in 5s`, with `:shared:generateCommonMainRhythHausDatabaseInterface` up-to-date and `:shared:compileKotlinMetadata SKIPPED`.
- `openspec validate scan-local-audio-folders --strict`: pass; output `Change 'scan-local-audio-folders' is valid`.
Acceptance:
- Requirement matched: yes; SQLDelight 2.3.2 aliases/plugin/dependencies and `RhythHausDatabase` schema were added, with `resources.srcDir(nativeAudioResourceRoot)` preserved.
- Scope controlled: yes; no feature code beyond database build setup/schema.
- Edge cases/risk reviewed: explicit SQLite 3.38 dialect is required for planned upsert syntax.
Changed files:
- `gradle/libs.versions.toml`: SQLDelight version, runtime/coroutines/platform driver libraries, plugin alias.
- `shared/build.gradle.kts`: SQLDelight plugin/database configuration, platform dependencies, preserved JVM native resource source dir.
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq`: initial library source/track/scan schema and queries.
- `openspec/changes/scan-local-audio-folders/tasks.md`: marked dependency setup and focused verification complete.
- `progress.md`: recorded this handoff evidence.
Next owner: implementation for Task 3 shared library domain models.
Blockers: none.
Commit: semantic commit with message `build: add library database setup`.


## Handoff - 2026-06-23 subagent-driven scanner/source access slice

Route: openspec+superpowers
Owner: implementation
Scope: subagent-driven implementation slice for `scan-local-audio-folders` tasks 1.1, 3.1-3.5, and 4.1-4.3; no UI changes and no repository schema changes.
Subagent inputs:
- Scanner/source-access review found missing iOS actual, missing Android DocumentFile dependency, automatic remove-missing data-loss risk, and metadata-reader failure risk.
- Gradle/database review confirmed SQLDelight setup tasks 1.2/1.3 were already complete and recommended `./gradlew :shared:compileKotlinMetadata --configuration-cache`.
- Slice planning recommended scanner orchestration and platform source access as conflict-safe next tasks, reserving OpenSpec/progress updates for the coordinator.
Implementation:
- Added Android `androidx.documentfile:documentfile` dependency for SAF tree traversal.
- Added iOS app-local folder picker/source scanner actual for `rememberPlatformFolderPickerLauncher` and `IOSAppLocalSourceAccess`.
- Kept Android SAF and JVM folder source access seams compile-safe across targets.
- Changed `LibraryScanner` to preserve already imported tracks after a completed scan instead of automatically deleting missing tracks; explicit remove-missing remains a later UI/action task.
- Changed metadata enrichment to fall back to filename metadata if `AudioMetadataReader.read` throws.
- Added common scanner tests for non-destructive completed scans and metadata-reader failure fallback.
Verification:
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: pass after source-access seam fixes; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :shared:compileKotlinMetadata :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `/usr/bin/xcrun xcodebuild -version`: pass; Xcode 26.5 Build 17F42.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: initial failures in the new iOS source-access actual, then pass after correcting enum/API usage; final Gradle run reported `BUILD SUCCESSFUL`.
- `openspec validate scan-local-audio-folders --strict`: pass; output `Change 'scan-local-audio-folders' is valid`.
Acceptance:
- Requirement matched: yes for scanner contracts/orchestration/cancellation/metadata fallback and Android/JVM/iOS first source access implementations.
- Scope controlled: yes; shared library manager UI, platform-focused source tests, full `./init.sh`, and archive remain open.
- Edge cases/risk reviewed: unsupported-file accounting may still need product/UI tuning; iOS app-local source uses a deterministic `createdAtEpochMillis = 0L` until a shared clock/source factory is introduced; explicit remove-missing action is still pending.
Changed files:
- `gradle/libs.versions.toml`: Android DocumentFile alias.
- `shared/build.gradle.kts`: Android DocumentFile dependency.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`: metadata fallback and non-destructive scan completion.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`: source picker/access contract and shared source-local key helpers.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`: Android SAF picker/source access.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.jvm.kt`: macOS/JVM native folder picker/source access.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.ios.kt`: iOS app-local folder source access.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`: scanner regression coverage.
- `openspec/changes/scan-local-audio-folders/tasks.md`: marked completed tasks for this slice only.
- `progress.md`: recorded this handoff evidence.
Next owner: implementation for platform-focused tests where practical and shared library manager UI tasks 5.1-5.4.
Blockers: none for this slice; full completion still requires UI integration, platform-focused tests, full `./init.sh`, and final OpenSpec archival.

## Handoff - 2026-06-23 Android Media3 system controls slice

Route: openspec+superpowers
Owner: implementation
Scope: user-requested Android platform audio API/control-panel slice for `play-music-all-platforms`; no iOS/macOS media-control changes and no long-running background playback claim.
Implementation:
- Added Media3 Session dependency for Android shared playback.
- Wrapped the active Android ExoPlayer in a Media3 `MediaSession` and released it with the player.
- Built Android Media3 `MediaItem`/`MediaMetadata` from shared `PlayableTrack` title, artist, album, id, and source so Android system media controls can show current track information and transport controls.
- Added Android host regression coverage for the metadata exposed to platform controls.
- Updated OpenSpec design/spec/tasks for the Android system media-controls scope while keeping background playback out of scope.
Verification:
- `./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' --configuration-cache`: initial RED failed on missing helper, then pass after implementation; final Gradle run reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
- `./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
Acceptance:
- Requirement matched: yes for Android platform media session/metadata wiring to support system control-panel display and transport controls during playback.
- Scope controlled: yes; no foreground service, notification-service manifest, iOS Now Playing, macOS remote controls, or background playback support was added.
- Edge cases/risk reviewed: emulator/device manual validation is still required to visually confirm Android control-panel rendering with a real playable local file.
Changed files:
- `gradle/libs.versions.toml`: Media3 Session alias.
- `shared/build.gradle.kts`: Android Media3 Session dependency.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`: MediaSession lifecycle and track metadata MediaItem construction.
- `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt`: Android metadata regression test.
- `openspec/changes/play-music-all-platforms/design.md`: Android system media-controls scope and non-goal adjustment.
- `openspec/changes/play-music-all-platforms/specs/audio-playback/spec.md`: Android system controls scenario.
- `openspec/changes/play-music-all-platforms/tasks.md`: completed Android media-session task and remaining manual device check.
- `progress.md`: recorded this handoff evidence.
Next owner: implementation/user for Android emulator/device manual validation with real playback and system control-panel observation.
Blockers: none for compile/test verification; visual Android control-panel confirmation requires an emulator/device playback session.

## Handoff - 2026-06-23 all-platform system media controls correction

Route: openspec+superpowers
Owner: implementation
Scope: correction to extend the platform media-control slice beyond Android to all first platforms: Android, iOS, and macOS. No long-running background playback service/notification support was added.
Implementation:
- iOS `AVAudioPlayer` engine now updates and clears `MPNowPlayingInfoCenter` with title, artist, album, elapsed time, and duration on load/play/pause/stop/seek/release.
- macOS/JVM AVFoundation helper now links `MediaPlayer.framework`, exposes JNI methods for Now Playing metadata/position/clear operations, and updates `MPNowPlayingInfoCenter` through the Objective-C++ helper.
- Android Media3 session/metadata wiring from the prior slice remains in place.
- Added focused iOS and macOS/JVM tests for the new metadata seams.
- Updated OpenSpec design/spec/tasks to describe platform system media controls across Android, iOS, and macOS rather than Android only.
Verification:
- `./gradlew :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSNowPlayingInfoTest' --configuration-cache`: initial RED failed on missing `buildIOSNowPlayingInfo`, then pass after implementation; final Gradle run reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.macOSNowPlayingInfoUpdateAcceptsTrackMetadata' --configuration-cache`: initial RED failed on missing bridge methods, then failed once on missing `MediaPlayer.framework` link, then pass after linking; final Gradle run reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
- `./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
Acceptance:
- Requirement matched: yes for platform-native media information/control seams on Android, iOS, and macOS foreground playback sessions.
- Scope controlled: yes; no streaming, background playback guarantee, Android foreground-service notification, iOS background mode, or macOS menu-bar/remote-control UI was added.
- Edge cases/risk reviewed: real system media-control rendering still requires manual validation on Android device/emulator, iOS simulator/device Control Center, and macOS Now Playing/Control Center with a real local audio file.
Changed files:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: iOS Now Playing metadata lifecycle.
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`: iOS metadata regression test.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: macOS bridge calls for Now Playing metadata/position.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: macOS `MPNowPlayingInfoCenter` native helper implementation.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`: macOS bridge metadata regression test.
- `shared/build.gradle.kts`: links macOS helper with `MediaPlayer.framework` and keeps Android Media3 Session dependency.
- `openspec/changes/play-music-all-platforms/design.md`: all-platform media-controls design update.
- `openspec/changes/play-music-all-platforms/specs/audio-playback/spec.md`: iOS/macOS system media-controls scenarios.
- `openspec/changes/play-music-all-platforms/tasks.md`: iOS/macOS media-controls tasks and manual validation follow-ups.
- `progress.md`: recorded this correction handoff evidence.
Next owner: implementation/user for manual platform validation with real playback on Android, iOS, and macOS.
Blockers: none for compile/test verification; visual system media-control confirmation requires platform runtime sessions.

## Handoff - 2026-06-23 macOS Now Playing visibility fix

Route: openspec+superpowers
Owner: implementation
Scope: macOS-specific fix after manual runtime feedback that metadata did not appear in macOS Control Center while music was playing.
Root cause:
- The native helper populated `MPNowPlayingInfoCenter.nowPlayingInfo`, but did not set `MPNowPlayingInfoCenter.playbackState` or `MPNowPlayingInfoPropertyPlaybackRate` during play/pause/stop transitions. macOS Control Center can treat metadata-only updates as inactive, so the session may not surface while playing.
Implementation:
- Added macOS bridge playback-state update API and regression coverage.
- `MacOSNativePlaybackEngine` now updates native Now Playing playback state on play, pause, and stop.
- Objective-C++ helper now sets `MPNowPlayingInfoCenter.playbackState` and `MPNowPlayingInfoPropertyPlaybackRate`, and clears state on release.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.macOSNowPlayingPlaybackStateUpdatesForControlCenterVisibility' --configuration-cache`: initial RED failed on missing bridge method, then pass after implementation; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
Acceptance:
- Requirement matched: yes for the identified missing macOS active playback-state signal needed by system media controls.
- Scope controlled: yes; no unrelated platform changes or background playback service support added in this fix.
- Remaining risk: visual confirmation still requires running desktop playback with a real audio file and checking macOS Control Center/Now Playing UI.
Changed files:
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: playback-state bridge calls and status mapping.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: native playback state/rate updates.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`: macOS playback-state regression test.
- `progress.md`: recorded this bugfix evidence.
Next owner: user/implementation for manual macOS Control Center confirmation.
Blockers: none for compile/test verification.

## Handoff - 2026-06-23 macOS remote command registration fix

Route: openspec+superpowers
Owner: implementation
Scope: second macOS-specific runtime fix after Control Center still did not show media information with metadata plus playback state/rate.
Root cause hypothesis:
- Metadata fields were not the likely weak point: title, artist, album, duration, elapsed time, playback rate, and playback state were already populated. The missing native seam was `MPRemoteCommandCenter` registration, so macOS may not classify the JVM process as a controllable Now Playing media session.
Implementation:
- Added `MacAudioPlayerBridge.registerNowPlayingRemoteCommands()` and calls it when a macOS track is loaded.
- Native helper now enables play, pause, toggle play/pause, stop, and change playback position commands via `MPRemoteCommandCenter` and maps them to the active `AVAudioPlayer`.
- Added focused JVM regression coverage for remote command registration.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.macOSNowPlayingRegistersRemoteCommandsForControlCenter' --configuration-cache`: initial RED failed on missing bridge method, then pass after implementation; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
Acceptance:
- Requirement matched: yes for the missing native remote-command/control registration needed for macOS Control Center discoverability.
- Scope controlled: yes; fix is limited to the macOS native helper/JVM bridge and tests.
- Remaining risk: visual confirmation still requires running desktop playback and checking macOS Control Center/Now Playing UI.
Changed files:
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: registers remote commands when loading a track.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: registers `MPRemoteCommandCenter` handlers.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`: remote-command registration regression test.
- `progress.md`: recorded this runtime fix evidence.
Next owner: user/implementation for manual macOS Control Center confirmation.
Blockers: none for compile/test verification.

## Handoff - 2026-06-23 Android TagLib native packaging completion

Route: openspec+superpowers
Owner: implementation
Scope: Complete `feat/taglib-metadata-module` Android native TagLib packaging from the preserved Superpowers plan `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md` and OpenSpec follow-up `openspec/changes/import-local-audio/tasks.md`.
Implementation:
- `taglib/build.gradle.kts` now builds pinned upstream `github.com/taglib/taglib` v2.3 commit `1b94b93762636ebe5733180c3e825be4621e4c7f` with Android NDK/CMake for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Generated `librhythhaus_taglib.so` slices are copied into `taglib/src/androidMain/jniLibs/<abi>/` by `packageAndroidTagLibJniLibs`; `.gitignore` keeps generated binaries out of source control.
- Android packaging hooks make TagLib Android JNI/native merge tasks depend on the native build/copy task.
Verification:
- `./gradlew :taglib:buildAllAndroidTagLibHelpers --configuration-cache`: pass; all three ABI helpers built.
- `./gradlew :taglib:allTests --configuration-cache`: pass.
- `./gradlew :taglib:assembleAndroidMain :androidApp:assembleDebug --configuration-cache`: pass.
- `unzip -l taglib/build/outputs/aar/taglib.aar | grep 'librhythhaus_taglib.so'`: pass; AAR contains `jni/arm64-v8a`, `jni/armeabi-v7a`, and `jni/x86_64` slices.
- `unzip -l androidApp/build/outputs/apk/debug/androidApp-debug.apk | grep 'librhythhaus_taglib.so'`: pass; APK contains `lib/arm64-v8a`, `lib/armeabi-v7a`, and `lib/x86_64` slices.
Acceptance:
- Requirement matched: Android native TagLib packaging from pinned upstream v2.3 is complete enough for AAR/APK packaging verification.
- Scope controlled: no Kotlin metadata parser added; iOS remains honestly pending.
- Remaining risk: Android content URI metadata still needs app-cache file path handoff and device/emulator runtime metadata validation before claiming end-to-end SAF rich metadata.
Next owner: implementation for Android content-URI-to-file handoff/runtime validation, or iOS TagLib XCFramework/cinterop packaging.
Blockers: none for Android native library packaging; iOS rich metadata support remains blocked on native static library/XCFramework/cinterop work.

## Handoff - 2026-06-23 iOS TagLib cinterop completion

Route: openspec+superpowers
Owner: implementation
Scope: Complete iOS TagLib cinterop from plan `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md` Task 5 and OpenSpec `openspec/changes/import-local-audio/tasks.md` 5.5.
Implementation:
- `taglib/native/CMakeLists.txt` supports `RHYTHHAUS_TAGLIB_BUILD_STATIC=ON` for iOS static library builds.
- `taglib/build.gradle.kts` builds pinned upstream TagLib v2.3 for `iosArm64` (device) and `iosSimulatorArm64` (simulator) as static `librhythhaus_taglib.a`.
- `taglib/src/nativeInterop/cinterop/rh_taglib.def` declares cinterop binding; a generated `.def` in the build directory resolves the per-target absolute path to `librhythhaus_taglib.a`.
- `taglib/src/iosMain/kotlin/com/eterocell/rhythhaus/taglib/TagLibReader.ios.kt` now calls `rh_taglib_read_path`/`rh_taglib_free_result` through Kotlin/Native cinterop instead of returning unsupported.
- `taglib/gradle.properties` enables cinterop commonization.
- `taglib/src/nativeInterop/cinterop/.gitignore` keeps generated `.a` out of git.
Verification:
- `./gradlew :taglib:iosSimulatorArm64Test --configuration-cache`: pass; includes native CMake build, cinterop generation, Kotlin compilation, linking, and test execution.
Acceptance:
- Requirement matched: iOS TagLib cinterop links real upstream TagLib v2.3 through the C ABI shim.
- Scope controlled: no Kotlin metadata parser added.
- Remaining risk: runtime metadata validation with real audio files on device/simulator remains a manual follow-up.
Next owner: user/implementation for manual iOS playback/metadata runtime validation.
Blockers: none.

## Handoff - 2026-06-23 developer panel for TagLib metadata

Route: openspec+superpowers
Owner: implementation
Scope: shared Compose UI developer panel that displays native TagLib-parsed metadata on the main library page.
Implementation:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` adds a collapsible `DeveloperPanel` on the main `LibraryHomeScreen`, rendered between the import card and now-playing card.
- The panel lists each imported file with its source handle and the parsed `AudioMetadata` (title, artist, album, duration) returned by the native `:taglib` wrapper, or a clear "metadata unavailable" line when the native reader returns no tags.
- `LibraryHomeScreen` now receives `importedFiles` so the panel can show raw parsed results without altering the existing library/track models.
Verification:
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL.
- `./init.sh`: BUILD SUCCESSFUL (JVM tests, desktop compile, Android debug build, iOS simulator shared tests).
Acceptance:
- Requirement matched: developer panel exists on the main page and displays TagLib-parsed metadata for imported files.
- Scope controlled: shared UI only; no model/engine/native changes.
- Remaining risk: real device/runtime metadata values still require manual import validation with real audio files.
Next owner: user/implementation for manual import + metadata runtime validation.
Blockers: none.

## Handoff - 2026-06-24 artwork in platform system media controls

Route: openspec+superpowers
Owner: implementation
Scope: Pass embedded artwork from TagLib-parsed tracks through `PlayableTrack` into Android, macOS, and iOS system media controls (notification center, Control Center, Now Playing widget).

Implementation:
- Added `artworkBytes: ByteArray?` to `PlayableTrack` with correct ByteArray equals/hashCode.
- Pass `track.artworkBytes` through `Track.toPlayableTrack()` in `App.kt`.
- Android: `buildAndroidPlaybackMediaMetadata` sets artwork data via `MediaMetadata.Builder.setArtworkData(byte[])`.
- macOS: Added `artwork` property and `setArtworkFromBytes:` method to `RhythHausAudioPlayer` native helper (ObjC++). Artwork is preserved across all now-playing info update paths. Added `nativeSetArtwork` JNI method. Gradle build links `AppKit.framework` for `NSImage`.
- JVM bridge: `MacAudioPlayerBridge.setArtwork()` calls `nativeSetArtwork` when loading a track.
- iOS: Artwork skipped (`MPMediaItemPropertyArtwork` deferred) — Kotlin/Native cinterop for `ByteArray → NSData → UIImage → MPMediaItemArtwork` requires stable Foundation bridging APIs not available in current KMP version. App's own Compose `NowPlayingCard` still displays artwork.

Verification:
- `./init.sh`: pass. BUILD SUCCESSFUL for shared JVM tests + desktop compile + Android debug build + iOS simulator tests.
- `openspec validate play-music-all-platforms --strict`: pass.
- `openspec validate import-local-audio --strict`: pass.

Acceptance:
- Requirement matched: yes for Android and macOS system media controls; iOS deferred (cinterop limitation documented).
- Scope controlled: yes; no background playback, notification, or unrelated platform changes.
- Remaining risk: visual confirmation requires running desktop/Android playback with real embedded-artwork audio files and checking system Control Center/notification artwork rendering.

Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`: `PlayableTrack.artworkBytes` field + equals/hashCode.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: `toPlayableTrack()` passes artwork.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`: `buildAndroidPlaybackMediaMetadata` sets artwork data.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: added deferred-iOS-artwork comment.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: bridge `setArtwork` + `nativeSetArtwork` JNI declaration.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: artwork property, setter, JNI method, preserved across all now-playing updates.
- `shared/build.gradle.kts`: linked `AppKit.framework` for macOS `NSImage`.

Next owner: user/implementation for manual macOS/Android artwork runtime confirmation and iOS cinterop artwork follow-up.
Blockers: none for compile/test; iOS system Control Center artwork deferred.

## Handoff - 2026-06-25 UI polish + iOS lockscreen fixes

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Three independent bugfixes: button font consistency (Clear Library → Black), NowPlayingScreen next-track UI staleness (LaunchedEffect sync), iOS lockscreen player panel (MPRemoteCommandCenter + playbackState).

Implementation:
- Task 1 (26b5c47): Changed Clear Library button `fontWeight` from `FontWeight.Medium` to `FontWeight.Black` in `ImportAudioCard` to match the "Add music folder" button.
- Task 2 (6129b35): Added `LaunchedEffect(playbackState.currentTrack?.id)` in both `LibraryHomeScreen` and `DrillDownView` to sync local `selectedTrackId` with the controller's current track when advancing via next-track button or playback completion.
- Task 3 (daf1811): Registered `MPRemoteCommandCenter` handlers (play, pause, togglePlayPause, stop, changePlaybackPosition) in iOS `PlaybackEngine.ios.kt` via block-based callbacks. Set `playbackState` on `MPNowPlayingInfoCenter` on play/pause/stop/release. Added `MPNowPlayingInfoPropertyPlaybackRate` to nowPlayingInfo dictionary. Used `ULong` values for `playbackState` (0uL/1uL/2uL) and top-level `MPRemoteCommandHandlerStatusSuccess` constant.

Verification:
- `./init.sh`: BUILD SUCCESSFUL — all platforms pass (JVM tests, desktop compile, Android debug, iOS simulator tests).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: BUILD SUCCESSFUL.
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: BUILD SUCCESSFUL.

Acceptance:
- Requirement matched: yes for all 3 bugfixes.
- Scope controlled: yes; only App.kt (font + LaunchedEffect) and PlaybackEngine.ios.kt (MPRemoteCommandCenter).
- Remaining risk: iOS lockscreen widget appearance needs manual runtime validation on a real iOS device/simulator with an active playback session. The compile/test build passes but visual confirmation requires a device.

Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: Clear Library FontWeight.Black + 2 LaunchedEffect blocks
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: MPRemoteCommandCenter registration, playbackState, playbackRate

Next owner: user for manual iOS lockscreen runtime validation.
Blockers: none for compile/test verification.

## Handoff - 2026-06-26 Dev panel scroll + DrillDownView wiring

Route: openspec+superpowers
Owner: implementation
Scope: Debug why dev panel was missing — the panel was visible but unreachable due to missing scroll on NowPlayingScreen and null currentLibraryTrack in DrillDownView.
Root cause:
- `NowPlayingScreen` Column had `fillMaxSize()` but no `verticalScroll`, so content below the fold (including dev panel) was clipped on smaller screens.
- `DrillDownView` hardcoded `currentLibraryTrack = null` when calling `NowPlayingScreen`, so dev panel never showed from album/artist drill-down.
Implementation:
- `NowPlayingScreen.kt`: added `import androidx.compose.foundation.rememberScrollState` and `import androidx.compose.foundation.verticalScroll`, added `.verticalScroll(rememberScrollState())` to the main Column modifier.
- `App.kt`: added `libraryTracks: List<LibraryTrack>` parameter to `DrillDownView`, resolved `currentLibTrack` from `currentTrack.id`, passed it to `NowPlayingScreen`. Updated both callers (album/artist drill-down in `LibraryHomeScreen`) to pass `libraryTracks`.
Verification:
- `./init.sh`: BUILD SUCCESSFUL — all platforms pass (JVM tests, iOS simulator tests, desktop compile, Android debug APK).
Acceptance:
- Requirement matched: yes; dev panel is now reachable via scroll on NowPlayingScreen and visible from drill-down views.
- Scope controlled: yes; only NowPlayingScreen scrolling and DrillDownView wiring.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for manual visual confirmation.
Blockers: none.

## Handoff - 2026-06-29 Main screen summary removal

Route: implementation
Owner: implementation
Scope: Remove the `RHYTHHAUS` pill and main-screen track count/duration summaries, while keeping album/artist drill-down track-count subtitles.
Implementation:
- `App.kt`: removed the summary text from `HeaderSection`; the main `Library queue` `SectionLabel` now passes `subtitle = null`; `SectionLabel` renders its subtitle only when present, preserving drill-down subtitles.
Verification:
- `./gradlew :shared:jvmTest --configuration-cache`: BUILD SUCCESSFUL. Existing Compose dependency version mismatch warnings were emitted.
Acceptance:
- Requirement matched: yes; the main screen no longer shows `xx tracks · xxx:xx` or `xx tracks • xxx:xx total`, and album/artist drill-down subtitles remain.
- Scope controlled: yes; only shared main-screen header/section-label UI changed.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for optional visual confirmation.
Blockers: none.

## Handoff - 2026-06-29 Drill-down scrollbar travel fix

Route: systematic-debugging
Owner: implementation
Scope: Fix custom album/artist drill-down scrollbar thumb being constrained to the upper right side.
Root cause:
- The scroll indicator used a hard-coded `scrollFraction * 100.dp` offset capped at `200.dp`, so the thumb could not travel across the actual available track height.
Implementation:
- `App.kt`: compute scroll fraction from total vs visible lazy-list items and use `BoxWithConstraints` to offset the thumb across `maxHeight - thumbHeight`.
Verification:
- `./gradlew :shared:jvmTest --configuration-cache`: BUILD SUCCESSFUL. Existing Compose dependency version mismatch warnings were emitted.
Acceptance:
- Requirement matched: yes; scrollbar travel now scales to the full right-side track height instead of a fixed upper band.
- Scope controlled: yes; only the custom drill-down scroll indicator changed.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for visual confirmation in album/artist track screens.
Blockers: none.

## Handoff - 2026-06-29 Square artwork and swipe back

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Make shared Compose album artwork square and add shared Compose-only left-edge swipe-back to detail/full-screen views.
Input: `docs/superpowers/specs/2026-06-29-square-artwork-swipe-back-design.md` and `docs/superpowers/plans/2026-06-29-square-artwork-swipe-back.md`.
Implementation:
- `App.kt`: made AlbumCard and older inline NowPlayingCard artwork square with `aspectRatio(1f)`, kept decoded artwork cropped, and applied `leftEdgeSwipeBack(onBack)` to `DrillDownView`.
- `NowPlayingScreen.kt`: made full Now Playing artwork square and applied `leftEdgeSwipeBack(onBack)` to the full-screen surface.
- `SwipeBackGesture.kt`: added shared Compose left-edge horizontal drag helper with edge-start and distance thresholds.
Verification:
- Implementer: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL.
- Reviewer first pass: spec rejected for missing `ContentScale.Crop` on older inline NowPlayingCard artwork; quality approved.
- Fix: commit `7c9cdba` added `ContentScale.Crop` to the inline artwork Image only and re-ran `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL.
- Reviewer second pass: spec approved; quality approved; no findings.
- Harness final verification: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 497ms. Existing Compose dependency version mismatch warning remains.
Acceptance:
- Requirement matched: yes; rectangular album art paths are square, compact square/circle artwork remains unchanged, and shared Compose swipe-back applies to album/artist drill-down and full Now Playing.
- Scope controlled: yes; no `iosApp` files, dependencies, Material migration, or native SwiftUI navigation changes.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SwipeBackGesture.kt`
Commits:
- `19fa018` `docs: design square artwork swipe back`
- `1c525b6` `docs: plan square artwork swipe back`
- `5a6898a` `feat: add square artwork and swipe back`
- `7c9cdba` `fix: crop inline now playing artwork`
Next owner: user for visual/manual gesture confirmation on target devices.
Blockers: none.

## Handoff - 2026-06-29 Selectable scrollbar and ripple feedback

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Make the shared Compose drill-down scrollbar selectable and add press feedback to main visible custom clickables/lists.
Input: `docs/superpowers/specs/2026-06-29-selectable-scrollbar-ripple-design.md` and `docs/superpowers/plans/2026-06-29-selectable-scrollbar-ripple.md`.
Implementation:
- `HausClickable.kt`: added `Modifier.hausClickable(onClick)` using foundation `clickable`, remembered `MutableInteractionSource`, and `LocalIndication.current` to avoid Material/Material3 imports.
- `App.kt`: replaced approved visible custom clickables with `hausClickable` and replaced the visual-only drill-down scrollbar with a right-edge 24 dp tap/drag scrubber and 6 dp thumb.
- `NowPlayingBar.kt`, `NowPlayingScreen.kt`, `SearchScreen.kt`, and `SettingsScreen.kt`: applied `hausClickable` to visible custom controls/lists while preserving invisible overlay blockers.
Verification:
- Initial implementer timed out after partial edits; controller inspected state and ran `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 453ms.
- Recovery implementer: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 578ms; committed `5a04ac0`.
- Task reviewer: spec approved; quality approved; no Critical/Important findings. Minor note: manual visual/device verification was not performed, and `LocalIndication.current` was accepted as satisfying visible press feedback without Material/Material3 imports.
- Harness final verification: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 414ms. Existing Compose dependency version mismatch warning remains.
Acceptance:
- Requirement matched: yes; scrollbar hit target is wider and selectable via tap/drag, and main visible custom clickables use shared press feedback.
- Scope controlled: yes; changes are limited to shared Compose files, with no iOS/platform/dependency/version catalog changes.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausClickable.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
Commits:
- `959b975` `docs: design selectable scrollbar ripple feedback`
- `7aaa584` `docs: plan selectable scrollbar ripple feedback`
- `5a04ac0` `feat: add selectable scrollbar and ripple feedback`
Next owner: user for manual visual confirmation on target devices.
Blockers: none.

## Handoff - 2026-06-29 Unified platform version metadata

Route: openspec+superpowers
Owner: implementation
Scope: Make root `gradle.properties` the single editable source for app version name/code across Android, desktop/macOS, and iOS.
Input: `docs/superpowers/specs/2026-06-29-unified-platform-version-design.md` and `docs/superpowers/plans/2026-06-29-unified-platform-version.md`.
Implementation:
- `gradle.properties`: added `rhythhaus.versionName=1.0.0` and `rhythhaus.versionCode=1`.
- `androidApp/build.gradle.kts`: reads Gradle properties for `versionName` and integer `versionCode`.
- `desktopApp/build.gradle.kts`: reads the same version name for Compose Desktop `packageVersion`.
- `build.gradle.kts`: added cacheable `syncIosVersionXcconfig` task to write iOS version settings from Gradle properties.
- `iosApp/Configuration/Config.xcconfig` and `Version.xcconfig`: map Xcode `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` from the synced version keys.
Verification:
- `./gradlew syncIosVersionXcconfig --configuration-cache`: BUILD SUCCESSFUL; wrote valid xcconfig syntax.
- `./gradlew syncIosVersionXcconfig :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache`: BUILD SUCCESSFUL in 12s. Existing Compose dependency mismatch/deprecation warnings remain.
- `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showBuildSettings | grep -E 'MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION'`: resolved `MARKETING_VERSION = 1.0.0`, `CURRENT_PROJECT_VERSION = 1`, `RHYTHHAUS_VERSION_NAME = 1.0.0`, and `RHYTHHAUS_VERSION_CODE = 1`.
Acceptance:
- Requirement matched: yes; changing only root `gradle.properties` version keys controls Android, desktop/macOS, and iOS version metadata after the sync task updates the committed xcconfig.
- Scope controlled: yes; no app IDs, signing settings, deployment targets, SDK/plugin/dependency versions, or packaging scope changed.
Changed files:
- `gradle.properties`
- `build.gradle.kts`
- `androidApp/build.gradle.kts`
- `desktopApp/build.gradle.kts`
- `iosApp/Configuration/Config.xcconfig`
- `iosApp/Configuration/Version.xcconfig`
Next owner: user for future version bumps by editing `gradle.properties` and running `./gradlew syncIosVersionXcconfig`.
Blockers: none.

## Handoff - 2026-06-29 iOS target version/build wiring

Route: openspec+superpowers follow-up
Owner: implementation
Input: User noted the unified version metadata must also update the iOS target's Version and Build fields.
Output:
- `iosApp/iosApp.xcodeproj/project.pbxproj`: Debug and Release target build settings now set `CURRENT_PROJECT_VERSION = "$(RHYTHHAUS_VERSION_CODE)"`, `INFOPLIST_KEY_CFBundleShortVersionString = "$(MARKETING_VERSION)"`, and `INFOPLIST_KEY_CFBundleVersion = "$(CURRENT_PROJECT_VERSION)"`.
Verification:
- `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showBuildSettings | grep -E 'MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION|INFOPLIST_KEY_CFBundleShortVersionString|INFOPLIST_KEY_CFBundleVersion'`: resolved `MARKETING_VERSION = 1.0.0`, `CURRENT_PROJECT_VERSION = 1`, `INFOPLIST_KEY_CFBundleShortVersionString = 1.0.0`, `INFOPLIST_KEY_CFBundleVersion = 1`, and the shared `RHYTHHAUS_VERSION_*` values.
- `./gradlew syncIosVersionXcconfig --configuration-cache`: BUILD SUCCESSFUL.
Next owner: user for future version bumps in `gradle.properties`; run `./gradlew syncIosVersionXcconfig` before opening/releasing from Xcode if the xcconfig is stale.
Blockers: none.

## Handoff - 2026-07-06 library scroll bar visibility

Route: openspec+superpowers
Owner: implementation
Scope: Scroll direction controls `NowPlayingBar` visibility on every scrollable screen that renders a bar: Home Library list, Search results, and album/artist track-list drill-down screens.
Implementation:
- Added pure common `LibraryScrollPosition` and `decideNowPlayingBarVisibilityForLibraryScroll` helper with tests for same-item down/up, item-boundary down/up, and jitter no-op behavior.
- Wired Home, Search results, and album/artist `DrillDownView` list scroll states to a hoisted `isNowPlayingBarVisible` state using the tested helper.
- Rendered both root fixed and drill-down `NowPlayingBar` paths through bottom `AnimatedVisibility` while preserving existing bar callbacks and Now Playing overlay.
Reviews:
- Task 1 independent review: clean; no Critical, Important, or Minor findings.
- Task 2 initial independent review: clean for the original Home-only scope.
- Widened-scope independent review: clean; no Critical or Important findings. One Minor EOF whitespace finding in `App.kt` was already fixed before staging and rechecked with `git diff --check` / `git diff --cached --check`.
Verification:
- `openspec validate library-scroll-bar-visibility --strict`: pass (`Change 'library-scroll-bar-visibility' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 988ms`; 24 actionable tasks: 7 executed, 17 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 98 actionable tasks: 12 executed, 86 up-to-date). Existing Android deprecation warning for `MediaMetadata.Builder.setArtworkData` remains unrelated.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 15s`; 33 actionable tasks: 8 executed, 25 up-to-date). Existing iOS test warnings remain unrelated.
- `git diff --check`: pass (no output, exit 0) after trimming a trailing blank line in `App.kt`.
Acceptance:
- Requirement matched: yes at source/automated-verification level — Home, Search results, and album/artist track lists now hide on downward scroll, show on upward scroll, preserve jitter no-op, bottom animation, and no pointer interception after hidden `AnimatedVisibility` exit.
- Scope controlled: yes — no new dependencies, native navigation changes, route-stack changes, playback/scanner/library/theme/platform changes, or changes to Now Playing content layout.
- Edge cases/risk reviewed: automated checks verify helper behavior and compilation; subjective animation feel and live gesture/hit-test behavior still need optional manual visual validation on device/simulator.
Changed files:
- `docs/superpowers/specs/2026-07-06-library-scroll-bar-visibility-design.md`
- `docs/superpowers/plans/2026-07-06-library-scroll-bar-visibility.md`
- `openspec/changes/library-scroll-bar-visibility/proposal.md`
- `openspec/changes/library-scroll-bar-visibility/design.md`
- `openspec/changes/library-scroll-bar-visibility/specs/library-navigation/spec.md`
- `openspec/changes/library-scroll-bar-visibility/tasks.md`
- `openspec/changes/library-scroll-bar-visibility/.openspec.yaml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `progress.md`
Next owner: user/OpenSpec for manual visual validation and archive when satisfied.
Blockers: none for automated validation.
Commit: pending semantic commit after staged-diff review/approval.



## Handoff - 2026-07-06 playback repeat shuffle

Route: openspec+superpowers
Owner: implementation
Scope: Shared playback repeat/shuffle modes and NowPlayingScreen controls.
Implementation:
- Added shared RepeatMode/ShuffleMode state and controller APIs.
- Centralized completion, previous, and next through mode-aware effective order logic.
- Added shuffle effective order generation that preserves current track and keeps visible library order unchanged.
- Added NowPlayingScreen repeat/shuffle controls using Material vector icons.
- Stabilized controller mode tests by isolating state-machine assertions from asynchronous fake-engine callbacks after broad verification exposed an order-dependent race.
Verification:
- openspec validate playback-repeat-shuffle --strict: pass (`Change 'playback-repeat-shuffle' is valid`).
- ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache: pass after deterministic test hardening (`BUILD SUCCESSFUL in 1s`).
- ./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache: first two broad runs failed in `PlaybackControllerTest.disablingShuffleReturnsToOriginalQueueOrderFromCurrentTrack` because async fake-engine callbacks raced controller state assertions; exact focused reruns passed, tests were made deterministic in `cbfcfdc`, and final broad rerun passed (`BUILD SUCCESSFUL in 2s`).
- /usr/bin/xcrun xcodebuild -version: Xcode 26.6, Build version 17F113.
- ./gradlew :shared:iosSimulatorArm64Test --configuration-cache: pass (`BUILD SUCCESSFUL in 18s`).
- git diff --check: pass before evidence edits; will be rerun after evidence commit.
Acceptance:
- Requirement matched: yes — repeat modes, shuffle modes, controller navigation semantics, and NowPlayingScreen controls match the approved plan at source/test level.
- Scope controlled: yes — no dependency changes; no mini-player/system-notification controls; visible library/browse order remains unchanged.
- Edge cases/risk reviewed: stop-after-current keeps current track and stops at duration; stop-after-queue stops at final effective track without wrapping; repeat-playlist wraps; repeat-one loops automatically while manual transport remains adjacent; shuffle enable/disable and shuffled queue replacement covered by tests. Manual playback UX validation on device/simulator was not performed in this CLI session.
Changed files:
- docs/superpowers/specs/2026-07-06-playback-repeat-shuffle-design.md
- docs/superpowers/plans/2026-07-06-playback-repeat-shuffle.md
- openspec/changes/playback-repeat-shuffle/.openspec.yaml
- openspec/changes/playback-repeat-shuffle/proposal.md
- openspec/changes/playback-repeat-shuffle/design.md
- openspec/changes/playback-repeat-shuffle/specs/audio-playback/spec.md
- openspec/changes/playback-repeat-shuffle/tasks.md
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
- shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt
- progress.md
Next owner: user/OpenSpec for manual playback UX validation and archive when satisfied.
Blockers: none for automated validation. Unrelated untracked `roadmap.md` remains untouched.
Commit:
- 4c2974e docs: spec playback repeat shuffle
- 5f1225a feat: add playback mode state
- ac59554 feat: add repeat and shuffle queue navigation
- 0c6f394 fix: preserve loading state during auto play transitions
- 906a00e feat: add now playing repeat shuffle controls
- cbfcfdc test: stabilize playback controller mode tests
- Evidence/docs finalization commit pending.


## Handoff - 2026-07-06 roadmap items 2-4

Route: mixed — systematic-debugging for roadmap bugfixes, openspec+superpowers for Nested Scroll/Haze UI feature.
Owner: implementation
Input: roadmap.md unfinished items in order.
Output:
- Fixed NowPlayingBar re-enter animation after returning from NowPlayingScreen by keeping the fixed bottom bar in composition and animating scroll hide/show with offset/alpha instead of gating it on `showNowPlaying`.
- Fixed compact Miuix button descender clipping risk by reducing inside vertical margin on fixed-height 36dp/40dp text buttons so glyphs such as `g` have enough vertical content space.
- Added Nested Scroll/Haze feature for Library/Home and album/artist track-list pages: pure tested `NestedScrollChromeState`, Haze dependency (`dev.chrisbanes.haze:haze`), `hazeSource` on lists, and top `hazeEffect` chrome driven by list scroll progress.
Changed files:
- `roadmap.md`
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `docs/superpowers/specs/2026-07-06-nested-scroll-blur-design.md`
- `docs/superpowers/plans/2026-07-06-nested-scroll-blur.md`
- `openspec/changes/nested-scroll-blur/*`
Verification:
- `openspec validate nested-scroll-blur --strict` -> `Change 'nested-scroll-blur' is valid`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` first failed red because `nestedScrollChromeStateFor` was missing, then passed after implementation.
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` -> `BUILD SUCCESSFUL` after Haze dependency/API integration.
Next owner: implementation can continue with roadmap item 5 (`i18n`) via OpenSpec/Superpowers.
Blockers: none for JVM/common verification. Manual visual validation on Android/iOS/desktop not performed in this session.


## Handoff - 2026-07-06 i18n

Route: openspec+superpowers
Owner: implementation
Input: roadmap.md item 5 (`i18n`).
Output:
- Added OpenSpec change `openspec/changes/i18n/` and Superpowers design/plan docs for shared Compose i18n.
- Added Compose Multiplatform string resources under `shared/src/commonMain/composeResources/values/strings.xml` and `values-zh/strings.xml`.
- Migrated shared Compose UI text/content descriptions in BackChip, NowPlayingBar, NowPlayingScreen, SearchScreen, SettingsScreen, and primary App.kt UI paths to `stringResource(Res.string.*)` while leaving user media metadata unchanged.
- Marked roadmap i18n item complete.
Changed files include:
- `roadmap.md`
- `progress.md`
- `shared/src/commonMain/composeResources/values/strings.xml`
- `shared/src/commonMain/composeResources/values-zh/strings.xml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `docs/superpowers/specs/2026-07-06-i18n-design.md`
- `docs/superpowers/plans/2026-07-06-i18n.md`
- `openspec/changes/i18n/*`
Verification:
- `openspec validate i18n --strict` -> `Change 'i18n' is valid`.
- `./gradlew :shared:compileKotlinJvm --configuration-cache` after resource creation -> `BUILD SUCCESSFUL`.
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` after code migration -> `BUILD SUCCESSFUL`.
Next owner: final verification/review; roadmap has no remaining unchecked item.
Blockers: none for shared JVM verification. Manual locale visual QA not performed.


## Handoff - 2026-07-06 nested scroll bugfix

Route: systematic-debugging (bugfix)
Owner: implementation
Input: user reported Nested Scroll effect covers nearly all Library screen, transition is not smooth, duplicate large heading + toolbar title visible simultaneously, and toolbar does not cover iOS status bar.
Root cause:
- `NestedScrollBlurChrome` used a fixed 92dp layer after `statusBarsPadding()` plus a negative `headerOffsetPx`, making the Haze/scrim area visually too large and unstable.
- Toolbar title alpha followed raw scroll progress from 0, so it appeared while the large page heading was still visible.
- Status bar coverage depended on a padded box height rather than a stable status-bar + toolbar structure.
Fix:
- Made default nested-scroll header offset zero and shortened activation distance to 96px for a faster, less jumpy transition.
- Reworked `NestedScrollBlurChrome` into a status-bar-covering container plus a fixed 56dp toolbar area.
- Reduced Haze blur/tint/scrim intensity and delayed toolbar title fade-in until scroll progress is ~68%, preventing simultaneous prominent headings.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` -> `BUILD SUCCESSFUL`.
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` -> `BUILD SUCCESSFUL`.
- `git diff --check && openspec validate nested-scroll-blur --strict` -> no diff-check output, `Change 'nested-scroll-blur' is valid`.
Next owner: user for manual visual check on iOS/Library screen; implementation can tune thresholds/height further if needed.
Blockers: none.


## Handoff - 2026-07-06 nested scroll blur root-cause fix

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported the Haze/blur nested-scroll chrome still covers nearly all of the Library screen after scrolling, contradicting the prior (unverified) parameter-tuning fix.

Phase 1 root cause investigation:
- Built and ran the real iOS app in the iPhone 17 (iOS 26.5) simulator via `xcodebuild` + `xcrun simctl`, not just static code reading.
- Reproduced the exact user-reported symptom: after scrolling, screenshots showed the content grid (album thumbnails + captions) uniformly blurred well below the intended 56dp toolbar, while status bar and toolbar title stayed sharp.
- Quantified this objectively (not just visual impression) with a Laplacian-variance sharpness metric on the same album cover crop before/after scroll: 1129 (sharp, unscrolled) -> 41 (heavily blurred, scrolled), a ~96% sharpness loss confirming a real blur filter was drawn over content far outside the chrome box.
- Isolated the Haze effect specifically via a red-color-box substitution experiment to separate "my Box layout is oversized" from "Haze itself draws past its host bounds" hypotheses.
- Traced Haze 1.7.2 library source (`HazeEffectNode.kt`, `BlurEffect.kt`) and found the actual root causes in `NestedScrollBlurChrome`:
  1. `Modifier.statusBarsPadding().height(0.dp)` inside the haze-effect Box squeezed the status-bar inset to zero instead of reserving space for it (this explains "does not cover iOS status bar").
  2. The Haze `hazeEffect` call had no explicit `clipToAreasBounds`, relying on Haze's implicit heuristic (`backgroundColor.alpha <= 0.9f` triggers clip) with alpha 0.92 sitting right at that fragile boundary, and the chrome Box had no fixed total height, so its measured height (and thus the Haze layer bounds) was not reliably constrained to the toolbar strip.
- Fix implemented: compute `chromeHeight = statusBarInset + 56dp` explicitly via `WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`, apply it as a single `Modifier.height(chromeHeight)` on the outer chrome Box, and pass `clipToAreasBounds = true` / `blurEnabled = true` explicitly to `hazeEffect` instead of relying on the alpha heuristic.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL.
- `./gradlew :shared:jvmTest --configuration-cache` -> BUILD SUCCESSFUL (all LibraryNavigationTest cases pass, including nested-scroll-chrome tests).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL.
- iOS: rebuilt via `xcodebuild ... build` (BUILD SUCCEEDED) and reinstalled/relaunched on iPhone 17 simulator (iOS 26.5) to confirm the app runs with the fix. The GUI automation driver (computer_use) that was used to reproduce the bug lost its session mid-task and could not be revived with the tools available in this session, so a fresh visual/AX-tree re-confirmation of "chrome no longer covers most of the screen after scrolling" on-device was not completed after the fix landed — this is an honest gap, not claimed as done.
Changed files (this session, in addition to prior nested-scroll-blur / i18n work):
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` (NestedScrollBlurChrome rewritten; new WindowInsets imports)
Next owner: user (or a future session with a working computer_use/simulator driver) to visually re-confirm on iOS/Android/desktop that the chrome is now bounded to the toolbar strip and covers the status bar.
Blockers: none for automated/JVM verification. On-device visual re-confirmation after the fix is the one gap — GUI driver session died and could not be restarted with available tools.


## Handoff - 2026-07-06 drop Haze from nested scroll chrome

Route: systematic-debugging (bugfix follow-up, user-directed)
Owner: implementation
Input: user asked to drop Haze from the nested-scroll chrome and use a plain (unblurred) scrim instead, pending their own visual check.
Output:
- Removed `hazeSource`/`hazeEffect`/`HazeState` usage from `NestedScrollBlurChrome` and its two call sites (Library home list, DrillDownView track list) in App.kt.
- `NestedScrollBlurChrome` now renders a fixed-height (status bar inset + 56dp toolbar) plain color scrim instead of a blurred Haze layer; scrim opacity still scales with scroll progress.
- Removed the `dev.chrisbanes.haze:haze` dependency entirely: `gradle/libs.versions.toml` (version + library entries) and `shared/build.gradle.kts` (`implementation(libs.haze)`).
- Marked roadmap item 4 (nested scroll + blur) back to `[ ]` / WIP since the blur requirement is currently dropped pending the user's own visual confirmation of the plain-scrim look.
Verification:
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` -> BUILD SUCCESSFUL.
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL.
- iOS: `xcodebuild ... build` -> BUILD SUCCEEDED, reinstalled/relaunched on iPhone 17 simulator (iOS 26.5), app launches and Library screen renders.
- NOT verified: live on-device scroll behavior of the new plain-scrim chrome. The computer_use GUI automation driver used earlier in this session to reproduce/diagnose the Haze bug lost its session and could not be revived with the tools available here, so a fresh screenshot-based confirmation of the post-Haze-removal scroll chrome was not completed. This is an open item for the user or a future session with a working driver.
Next owner: user, to visually confirm the plain-scrim nested-scroll chrome looks acceptable; if so, mark roadmap item 4 done, if not, decide follow-up (different blur approach, or keep plain scrim as final design).
Blockers: none for build verification. On-device visual confirmation is pending.
