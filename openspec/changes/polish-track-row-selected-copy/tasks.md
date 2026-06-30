# Tasks

## 1. Replace selected-row debug copy

- [x] Replace the selected `TrackRow` debug text with `Now playing`.
- [x] Remove now-unused selection-alpha animation state if it is only used for the old percentage text.
- [x] Preserve selected-row highlight, row click behavior, metadata display, and duration display.

## 2. Verification and handoff

- [x] Run `openspec validate polish-track-row-selected-copy --strict`.
  - Evidence: pass, `Change 'polish-track-row-selected-copy' is valid`.
- [x] Run `./gradlew :shared:compileKotlinJvm --configuration-cache`.
  - Evidence: pass, `BUILD SUCCESSFUL`.
- [x] Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` or record exact blocker/flake and successful rerun evidence.
  - Evidence: pass, `BUILD SUCCESSFUL`.
- [x] Search source for `queued on shared UI` and confirm no shared UI match remains.
  - Evidence: `rg 'queued on shared UI|selectionAlpha' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` returned no matches (exit 1).
- [x] Update `progress.md` with evidence.
- [x] Commit with a semantic message.
