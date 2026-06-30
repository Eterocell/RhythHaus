# Tasks

## 1. Dependency and icon wrapper

- [x] Add a Compose Material Icons dependency that resolves for shared/commonMain.
- [x] Use Compose vector icons for play, pause, previous, next, search, and settings controls.

## 2. Replace mini-player controls

- [x] Replace `NowPlayingBar.kt` play/pause text glyphs with Material vector icons.
- [x] Replace `NowPlayingBar.kt` search/settings emoji glyphs with Material vector icons.
- [x] Preserve existing mini-player click behavior, colors, sizing, progress, and navigation.

## 3. Replace full now-playing transport controls

- [x] Replace `NowPlayingScreen.kt` previous/play-pause/next text glyphs with Material vector icons.
- [x] Preserve existing transport click behavior, colors, sizing, and playback behavior.

## 4. Verification and handoff

- [x] Run `openspec validate replace-emoji-controls-with-icons --strict`.
- [x] Run focused shared JVM compile.
- [x] Run broad JVM/desktop/Android verification.
- [x] Search for targeted emoji/text control glyphs and confirm no control usage remains.
- [x] Update `progress.md` with evidence.
- [x] Commit with a semantic message.

## Evidence

- `openspec validate replace-emoji-controls-with-icons --strict`: pass (`Change 'replace-emoji-controls-with-icons' is valid`).
- `rg '▶|⏸|⏮|⏭|🔍|⚙️' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: pass/no matches (exit 1, empty output).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass after pinning Material Icons Extended to resolvable JetBrains Compose icon version `1.7.3`; initial `1.11.1` artifact lookup failed.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- Report: `.superpowers/sdd/replace-emoji-controls-with-icons-report.md`.
