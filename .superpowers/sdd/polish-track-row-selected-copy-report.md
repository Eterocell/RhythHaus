# Polish Track Row Selected Copy Report

Route: openspec+superpowers
Owner: implementation
Scope: `TrackRow` selected-row copy in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` plus OpenSpec/progress evidence.

## Implementation

- Replaced selected `TrackRow` status copy from `queued on shared UI ${(selectionAlpha * 100).toInt()}%` to `Now playing`.
- Removed now-unused `selectionAlpha` animation state and its `animateFloatAsState`/`tween` imports.
- Preserved selected-row highlight, click behavior, metadata display, duration display, playback, queue, navigation, theme, scanner, persistence, and platform code.

## Verification

- `openspec validate polish-track-row-selected-copy --strict`: pass (`Change 'polish-track-row-selected-copy' is valid`).
- `rg 'queued on shared UI|selectionAlpha' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: pass/no matches (exit 1, empty output).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`). Existing warnings only: `PredictiveBackHandler` deprecation and expect/actual beta warnings.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`). Existing warnings only: Android artwork deprecation, `PredictiveBackHandler` deprecation, and expect/actual beta warnings.

## Acceptance

- Requirement matched: yes — selected track rows display `Now playing` and no longer expose debug copy or percentage state.
- Scope controlled: yes — code change isolated to `TrackRow`/imports in `App.kt`; evidence files updated.
- Blockers: none.
