# Session Progress

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





