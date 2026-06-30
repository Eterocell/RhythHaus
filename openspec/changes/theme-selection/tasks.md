## 1. Theme domain and persistence

- [x] 1.1 Add DataStore dependency aliases and wire them into the shared module without changing unrelated dependencies.
- [x] 1.2 Add `RhythHausThemeMode` with stable serialized values, parsing fallback to `System`, display labels/descriptions, and common tests.
- [x] 1.3 Add light/dark Haus palettes and theme resolution helpers with common tests.
- [x] 1.4 Add DataStore-backed theme preference store that defaults to `System`, persists selected modes, and ignores invalid persisted strings safely.
- [x] 1.5 Add Android, iOS, and JVM/macOS DataStore file-path/factory actuals for supported platforms.

## 2. Compose theme wiring

- [x] 2.1 Update `RhythHausTheme` to accept a selected mode, resolve system mode, provide the active Haus palette, and choose Miuix `lightColorScheme` / `darkColorScheme`.
- [x] 2.2 Wire `App()` to observe the persisted theme mode and save changes from Settings.
- [x] 2.3 Migrate visible shared Compose surfaces and controls from fixed light Haus colors to the active palette, preserving existing layout and copy.

## 3. Settings appearance selector

- [x] 3.1 Add `currentThemeMode` and `onThemeModeSelected` parameters to `SettingsScreen`.
- [x] 3.2 Add an Appearance section with System, Light, and Dark selectable controls and clear selected-state styling.
- [x] 3.3 Ensure Settings, Search, Now Playing, bottom bar, dialogs, cards, rows, and developer panels remain readable in light and dark palettes.

## 4. Verification and handoff

- [x] 4.1 Run focused theme/common persistence tests.
- [x] 4.2 Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- [x] 4.3 Run `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` if iOS actual code is touched.
- [x] 4.4 Run `openspec validate theme-selection --strict`.
- [x] 4.5 Record changed files, verification output, remaining visual/manual validation recommendations, and next owner in `progress.md`.
