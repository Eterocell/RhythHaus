# Plan: Standardized Back Navigation

## Context

The repo already has an explicit `LibraryNavigationStack` and Compose Multiplatform `ui-backhandler` dependency. Existing visible labels are inconsistent (`← BACK`, `← LIBRARY`, `< Back`). Android manifest does not explicitly opt into predictive back.

## Implementation steps

1. Create a shared back-chip composable
   - Add a small common composable such as `BackAffordance` or `BackChip` in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus`.
   - Match existing rounded chip color/spacing style.
   - Set visible text to `‹ Back` and semantic content description to `Back`.

2. Replace visible labels
   - In `DrillDownHeader`, replace the inline `← BACK` chip with the shared component.
   - In `NowPlayingScreen`, replace `← LIBRARY` with the shared component.
   - In `SearchScreen`, replace `< Back` with the shared component.
   - In `SettingsScreen`, replace `< Back` with the shared component.

3. Centralize system/predictive back
   - In `LibraryHomeScreen`, replace the root `BackHandler(enabled = navigation.canPop)` with `PredictiveBackHandler(enabled = navigation.canPop)` and pop after the predictive event completes.
   - Remove duplicate screen-level `BackHandler` registrations where the root route-stack handler already covers the current route.
   - Preserve `leftEdgeSwipeBack(onBack)` in drill-down and now playing because it supports hosted iOS/shared Compose gestures.

4. Android manifest opt-in
   - Add `android:enableOnBackInvokedCallback="true"` to the main Android activity.

5. Verification
   - `openspec validate standardize-back-navigation --strict`
   - `./gradlew :shared:compileKotlinJvm --configuration-cache`
   - `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
   - Record manual Android predictive-back validation as recommended follow-up if no emulator/device interaction is run.

## Risks

- `PredictiveBackHandler` is deprecated in Compose 1.11.1 in favor of `NavigationEventHandler`, but it is the available compatibility API already exposed by the current dependency. Treat deprecation warnings as known until a larger navigation API migration is planned.
- Predictive-back visual preview needs manual Android 13+ validation; automated Gradle builds only verify wiring compiles.
