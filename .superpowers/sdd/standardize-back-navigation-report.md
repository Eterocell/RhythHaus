# Standardize Back Navigation Report

Status: implemented and automated verification passed.

## Implementation summary

- Added `BackChip` in shared common UI with visible label `‹ Back`, `Back` content description, rounded ink/paper chip styling, and existing `hausClickable` press feedback.
- Replaced targeted visible back labels in:
  - `DrillDownHeader` (`App.kt`)
  - `NowPlayingScreen.kt`
  - `SearchScreen.kt`
  - `SettingsScreen.kt`
- Centralized system/predictive back in `LibraryHomeScreen` with `PredictiveBackHandler(enabled = navigation.canPop)`, popping one route after completed back progress.
- Removed duplicate nested `BackHandler` registrations from route-level screens/dialog overlays so root route-stack handling owns system/predictive back.
- Preserved shared left-edge swipe-back gestures for drill-down and now-playing views.
- Added `android:enableOnBackInvokedCallback="true"` to the Android main activity.

## Verification evidence

- `openspec validate standardize-back-navigation --strict`
  Result: pass — `Change 'standardize-back-navigation' is valid`.
- `./gradlew :shared:compileKotlinJvm --configuration-cache`
  Result: pass — `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
  Result: initial fail in known/flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --configuration-cache`
  Result: pass — `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --rerun-tasks --configuration-cache`
  Result: pass — `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --rerun-tasks --configuration-cache`
  Result: pass — `BUILD SUCCESSFUL`.
- Source search for `← BACK`, `← LIBRARY`, and `< Back` under shared common UI found no remaining matches.
- `git diff --check` passed with no whitespace errors.

## Notes / concerns

- Compose Multiplatform 1.11.1 marks `PredictiveBackHandler` deprecated in favor of `NavigationEventHandler`; this was expected in the approved plan and left unchanged to match the OpenSpec implementation shape.
- Android predictive-back visual preview still needs manual Android 13+ emulator/device validation; automated verification confirms compile/test/build wiring only.
