# Replace Emoji Controls With Icons Report

Date: 2026-07-01
Route: openspec+superpowers
Owner: implementation
Change: `replace-emoji-controls-with-icons`

## Implementation

- Added shared commonMain dependency alias for Compose Material Icons Extended.
  - The originally planned `compose-multiplatform` version `1.11.1` did not resolve for `org.jetbrains.compose.material:material-icons-extended`; pinned the icon artifact to available JetBrains Compose icon version `1.7.3` while keeping the rest of the Compose setup unchanged.
- Replaced mini-player control glyph text in `NowPlayingBar.kt`:
  - play/pause/empty play button now renders `Icons.Filled.PlayArrow` or `Icons.Filled.Pause` with existing paper tint.
  - search/settings buttons now render `Icons.Filled.Search` and `Icons.Filled.Settings` with existing ink tint.
- Replaced full now-playing transport glyph text in `NowPlayingScreen.kt`:
  - previous/play-pause/next controls now render `Icons.Filled.SkipPrevious`, `Icons.Filled.PlayArrow`/`Pause`, and `Icons.Filled.SkipNext`.
- Preserved existing button containers, sizes, colors/tints, click lambdas, playback callbacks, navigation callbacks, queue logic, scanner, persistence, platform code, and artwork fallback behavior outside the targeted controls.

## Verification

- `openspec validate replace-emoji-controls-with-icons --strict`: pass (`Change 'replace-emoji-controls-with-icons' is valid`).
- `rg '▶|⏸|⏮|⏭|🔍|⚙️' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: pass/no matches (exit 1, empty output).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`:
  - first run failed because `org.jetbrains.compose.material:material-icons-extended:1.11.1` was not available.
  - after pinning `compose-material-icons = "1.7.3"`, rerun passed (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).

Warnings observed were pre-existing/dependency/tooling warnings: Gradle convention deprecations, Compose `PredictiveBackHandler` deprecation, expect/actual beta warnings, Android artwork API deprecation, and TagLib CMake utf8cpp fallback notes.

## Acceptance

- Requirement matched: yes — transport/search/settings controls use vector icons rather than targeted emoji/text glyphs.
- Accessibility/theme: yes — each icon sets explicit content description and uses the same theme-driven tint/color as prior control styling.
- Scope controlled: yes — no playback, queue, scanner, persistence, navigation, theme selection, platform-specific code, or non-control artwork fallback behavior was changed.
- Blockers: none.
