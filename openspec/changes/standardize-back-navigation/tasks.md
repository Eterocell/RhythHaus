# Tasks

## 1. Shared back affordance

- [x] Add a shared back-chip composable in common UI code.
  - Evidence: added `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt` with visible label `‹ Back`, `Back` content description, and shared rounded ink/paper chip styling.
- [x] Replace `← BACK`, `← LIBRARY`, and `< Back` with the shared `‹ Back` affordance in drill-down, now playing, search, and settings.
  - Evidence: `DrillDownHeader`, `NowPlayingScreen`, `SearchScreen`, and `SettingsScreen` now call `BackChip`; source search found no remaining `← BACK`, `← LIBRARY`, or `< Back` labels under shared RhythHaus common UI.
- [x] Keep all existing navigation destinations and callbacks unchanged.
  - Evidence: visible controls still invoke existing `onBack` / `onDismiss` / route-pop callbacks; no playback, scanner, theme, library persistence, or route model semantics changed.

## 2. Predictive/system back wiring

- [x] Register a root predictive/system back handler for `LibraryNavigationStack` pops when `navigation.canPop` is true.
  - Evidence: `LibraryHomeScreen` uses `PredictiveBackHandler(enabled = navigation.canPop)` and pops exactly one route after the completed back progress flow.
- [x] Remove or avoid duplicate route-level handlers that bypass the root predictive handler.
  - Evidence: removed nested `BackHandler` registrations from drill-down, now playing, search, settings, and clear-library dialog paths; root navigation owner is the shared system/predictive back consumer.
- [x] Preserve existing visible back buttons and shared left-edge swipe-back gestures by routing them to the same pop callback.
  - Evidence: `BackChip` calls existing route-pop callbacks; `leftEdgeSwipeBack(onBack)` remains on drill-down and now-playing surfaces.
- [x] Explicitly opt the Android main activity into predictive back in `AndroidManifest.xml`.
  - Evidence: `androidApp/src/main/AndroidManifest.xml` main activity has `android:enableOnBackInvokedCallback="true"`.

## 3. Verification and handoff

- [x] Run OpenSpec validation.
  - Evidence: `openspec validate standardize-back-navigation --strict` -> `Change 'standardize-back-navigation' is valid`.
- [x] Run focused shared JVM compile.
  - Evidence: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> `BUILD SUCCESSFUL`.
- [x] Run broad JVM/desktop/Android verification.
  - Evidence: initial broad run failed in known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun passed; broad rerun with `--rerun-tasks` passed: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --rerun-tasks --configuration-cache` -> `BUILD SUCCESSFUL`.
- [x] Update `progress.md` with verification evidence.
  - Evidence: progress handoff added for this change.
- [x] Commit with a semantic message.
  - Evidence: commit created with message `feat: standardize back navigation`.
