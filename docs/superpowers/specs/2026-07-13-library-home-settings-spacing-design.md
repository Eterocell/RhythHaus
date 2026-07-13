# Library Home Chrome and Settings Spacing Design

## Summary

RhythHaus will simplify the shared Library home by removing its scroll-triggered nested top bar entirely. The existing Library header remains the first list item and begins below the system status-bar safe area. The Settings page will retain system safe-area protection while adopting a more compact app-owned spacing rhythm.

## Current State

`LibraryHomeContent` derives `NestedScrollChromeState` from the home `LazyListState` and overlays `NestedScrollBlurChrome` after the list begins scrolling. The list already reserves the status-bar height through its top content padding, so removing the overlay does not require replacing it with another bar.

`SettingsScreen` applies `safeContentPadding()` and then adds 20 dp horizontal and 16 dp vertical page padding, 18 dp spacing between every lazy-list item, and another 16 dp bottom content padding. The combined system and app-owned spacing makes the page feel unnecessarily loose.

## Design

### Library home

Remove the home-only nested-scroll chrome state derivation and the `NestedScrollBlurChrome` overlay from `LibraryHomeContent`. Keep the home list's status-bar top content padding, 20 dp horizontal padding, header, browse controls, content, Now Playing spacing, and scroll-driven Now Playing bar visibility behavior unchanged.

The album and artist drill-down surfaces remain separate. Their `DrillDownMiuixScrollChrome`, artwork transition, Miuix scroll behavior, safe-start back-button inset, and title presentation are unchanged.

If removal leaves `NestedScrollBlurChrome`, `NestedScrollChromeState`, and `nestedScrollChromeStateFor` without production callers, remove those home-specific declarations and their obsolete tests rather than retaining dead UI infrastructure. Shared helpers still used by drill-down screens remain.

### Settings

Keep `safeContentPadding()` so the page continues to avoid status bars, display cutouts, navigation bars, and desktop-safe content regions. Reduce only app-owned spacing to:

- 16 dp horizontal page padding;
- 8 dp vertical page padding;
- 12 dp spacing between lazy-list items;
- 8 dp final bottom content padding.

Keep `RhythHausTopAppBar`, its 44 dp back target, appearance preference, source-management rows and controls, scan status, dialogs, typography, colors, and behavior unchanged.

## Layout Policy and Testability

Represent the selected Settings values in a small internal immutable layout-policy value exposed by a pure function or constant. `SettingsScreen` consumes that policy when constructing `Modifier.padding`, `PaddingValues`, and `Arrangement.spacedBy`. A common test asserts the exact values, making future accidental spacing expansion visible without introducing screenshot-test infrastructure.

Library home coverage will assert the retained status-bar content inset policy and remove tests that exist solely for the deleted nested chrome progression. Existing navigation and bottom-bar scroll tests continue to protect unrelated behavior.

## Constraints

- Shared Compose Multiplatform UI only.
- Preserve system-derived safe-area padding on Library home and Settings.
- Do not add a replacement Library home top bar, title row, blur layer, or collapse animation.
- Do not change album/artist drill-down chrome or Miuix scroll behavior.
- Do not change navigation, playback, scanning, source management, dialogs, dependencies, persistence, or platform integrations.
- Do not add Windows or Linux product support.

## Error and Accessibility Impact

The change introduces no new asynchronous work or error path. Existing disabled states, scan errors, picker messages, and dialog behavior remain unchanged. Safe-area handling and existing 44 dp interactive targets remain intact, and no semantics or accessible labels are removed.

## Verification

- A RED/GREEN common test proves the exact compact Settings layout policy.
- Existing Library navigation and bottom-bar scroll tests pass after obsolete nested-chrome tests are removed.
- `openspec validate library-home-settings-spacing --strict`
- `./gradlew :shared:jvmTest --configuration-cache`
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
- `/usr/bin/xcrun xcodebuild -version`
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
- `git diff --check`
- Manual visual QA remains required for the final perceived density and safe-area appearance on target devices and desktop window sizes.
