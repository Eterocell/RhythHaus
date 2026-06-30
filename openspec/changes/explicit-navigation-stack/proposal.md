## Why

RhythHaus currently models in-app navigation with several independent Compose state values (`selectedAlbum`, `selectedArtist`, `showNowPlayingScreen`, `showSettings`, `showSearch`, and clear-dialog visibility). This fixed the immediate Android back issue, but the state is not a real navigation model. It cannot reliably represent nested origins such as Album Detail → Search → Now Playing → back to Search → back to Album Detail, and each new overlay adds another ad-hoc flag.

This change makes navigation explicit and predictable without adding a full navigation dependency.

## What Changes

- Add a small pure Kotlin route-stack model in shared common code.
- Represent Home, Album Detail, Artist Detail, Now Playing, Search, Settings, and Clear Library dialog as `LibraryRoute` entries.
- Refactor shared Compose routing in `LibraryHomeScreen` so screen selection and back handling use one stack instead of independent booleans/nullables.
- Preserve existing UI visuals and playback behavior while improving Android system-back and shared swipe-back behavior.

## Capabilities

### New Capabilities
- `library-navigation`: explicit shared in-app navigation stack for library, details, overlays, and dialog routes.

### Modified Capabilities
- Existing library browsing and now-playing UX use stack-based back behavior while preserving current screens.

## Impact

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`: new route and stack model.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`: pure navigation behavior tests.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: route rendering/back handling refactor.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: keep existing back callback; no visual changes expected.

## Non-goals

- No deep links or process-death route restoration.
- No native Android/iOS/macOS navigation controller migration.
- No screen redesign.
- No playback, scanner, metadata, or persistence behavior changes.
- No new dependency.

## Verification

- Focused common tests for `LibraryNavigationStack`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` for shared/desktop/Android compile and regression coverage.
- Manual Android validation remains recommended for gesture/system back on device/emulator.