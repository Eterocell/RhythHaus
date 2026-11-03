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

## 5. Curated palettes and Android dynamic color

- [x] 5.1 Add stable palette IDs and all 29 semantic color roles plus scrim alpha to `:core:ui`; encode Moss/Bay/Clay/Iris light/dark values from the approved token file, migrate all existing Haus palette consumers to canonical roles without aliases, and add common contrast/resolution tests before implementation.
- [x] 5.2 Extend `ThemePreferenceStore` with independently parsed/persisted palette ID and Monet intent; verify default/invalid values and restart behavior in `ThemePreferenceStoreJvmTest`.
- [x] 5.3 Add an Android API 31+ dynamic-color provider and fallback projection; map only the specified Material roles, retain curated warning/success roles, and test provider absence/API 29–30 fallback without leaking Android types into common code.
- [x] 5.4 Extend Shared theme resolution and app-state wiring to consume mode, palette, dynamic intent, effective source, and recoverable persistence errors; verify settings writes do not change unrelated playback/navigation state.
- [x] 5.5 Update the explicit `:feature:settings` appearance entry point and Shared callers; render accessible mode segments, four palette previews, API-gated Monet and fallback status, with EN/ZH resources and selected-state tests.
- [x] 5.6 Update `docs/architecture.md` and `docs/adr/0001-feature-first-module-boundaries.md` for the changed public Settings appearance contract; preserve feature-to-shell dependency direction and add no service locator.
- [ ] 5.7 Run focused Core UI, Settings, and Shared tests; Android/desktop compilation; iOS simulator tests for touched common actuals; then run `./init.sh`, strict OpenSpec validation, independent review, and user-only appearance/device acceptance without claiming unperformed checks.
