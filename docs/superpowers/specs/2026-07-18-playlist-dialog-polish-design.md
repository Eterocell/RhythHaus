# Playlist Dialog Polish Design

## Summary

RhythHaus will correct playlist-hub spacing and tab-button legibility, then establish one shared Compose dialog shell for every current app modal. The shell preserves each route's existing state, copy, callbacks, and accessibility behavior while standardizing its panel, scrim, dimensions, and action treatment across Android, iOS, and desktop JVM/macOS.

## Goals

- Align the playlist hub's content inset and vertical rhythm with Library home.
- Make `Saved` and `Queue` tab labels readable in both themes without clipped descenders.
- Replace every current custom modal shell with one reusable app-wide `HausDialog` component.
- Reuse the Clear Library dialog's centered title, explanatory text, and action-row hierarchy as the visual basis.
- Render dialog panels with a solid theme-aware `HausColors.current.panel` background rather than a liquid-glass or transparent surface.
- Use a light-colored dialog dim treatment in dark mode while keeping scrim-tap dismissal, dialog semantics, and explicit cancel actions.

## Non-goals

- Changing playlist, queue, source-removal, or clear-library mutation behavior.
- Changing localized strings, navigation routes, persistence, production dependencies, or theme palette values.
- Adding new dialogs or altering which existing operations require confirmation.
- Altering the Now Playing bar visibility policy.

## Current State

`LibraryHomeContent` uses a 20dp horizontal inset, `rememberSystemBarTopPadding()` as its only top content padding, 18dp item spacing, and `NowPlayingBarContentPadding` at the bottom. `PlaylistScreenFrame` currently applies `safeContentPadding()` in addition to its own page padding, producing a visibly larger top inset.

The playlist tab and compact action controls rely on Miuix defaults or undersized inside margins. In light theme this lets the `Saved` and `Queue` label color approach their button background; their vertical text area can crop descenders.

The app currently has separate modal shells in `PlaylistScreens.kt`, `LibraryDialogs.kt`, and `SettingsScreen.kt`. They differ in panel transparency, glass backdrop use, semantics, padding, button sizing, and scrim rendering even though they serve the same modal interaction model.

## Design

### Shared dialog primitive

Create an internal common-main `HausDialog` composable under `com.eterocell.rhythhaus.ui`. It provides:

- A full-window semantic dialog region with a title supplied by the caller and an accessible dismiss action.
- A tap-dismissable scrim that blocks interaction with the underlying screen; the panel itself consumes taps so they do not dismiss the dialog.
- A centered, full-width solid `HausColors.current.panel` card with 24dp rounded corners, 24dp outer inset, and a bounded maximum content height.
- Scrollable body content when its natural height exceeds the bounded panel height.
- A dark-theme scrim that is visibly light rather than ink-black; light theme remains a restrained ink dim. The exact color is derived from the active `HausColorPalette`, not a new palette token.
- Content-slot ownership: title/body/action composables remain with each existing dialog, so their validation notices, selected-list controls, destructive emphasis, and callbacks do not move into the generic shell.

The primitive does not use `rhythHausLiquidGlass` or accept a `LayerBackdrop`; all existing dialog callers remove those visual-only inputs. This deliberately makes panel rendering deterministic and opaque across platforms.

### Dialog migration

Migrate all current app dialogs to `HausDialog`:

- Clear Library confirmation.
- Remove Folder confirmation.
- Create and rename playlist dialogs.
- Add to Playlist picker, including inline creation.
- Playlist destructive confirmations.
- Clear Upcoming queue confirmation.

Each dialog retains its current resource strings, confirmation/dismiss callbacks, mutation error display, and selection state. Existing delete/clear action colors continue to use `HausColors.current.pulse`; neutral and primary actions explicitly provide foreground colors and sufficient internal vertical space.

### Playlist hub layout and controls

`PlaylistScreenFrame` adopts the same system-top-only and 20dp horizontal content inset as Library home. Its list spacing is adjusted to the Library home rhythm while preserving the playlist-specific back affordance and its current Now Playing bottom spacer.

`PlaylistTabs` defines both selected and unselected button foreground/background colors explicitly from `HausColors.current`, rather than relying on Miuix defaults. Tab labels use a stable line height and button inside margins/minimum height that leave room for descenders in Latin and CJK text. The compact actions used in playlist dialogs and notices receive the same text-fit correction.

## Data Flow and Error Handling

This is a presentational refactor. Dialog-state reducers, callback wiring, repository mutations, and queue-controller commands remain unchanged. A dialog failure continues to retain entered text or selection and display its existing localized error. Scrim taps, cancel buttons, and the system accessibility dismiss action invoke the existing dismiss callback exactly once.

## Accessibility and Responsive Behavior

Every migrated dialog keeps its title as the accessibility pane title and exposes an explicit dismiss action. The shared panel has a maximum height and scrollable content, preventing long localized source names, playlist names, picker lists, and error messages from extending beyond compact windows. Text colors must meet the established palette contrast in light and dark modes; dialog action text must not crop or overlap at the smallest supported width.

## Testing and Verification

Add focused common tests for pure layout/presentation policy that prove:

- Playlist hub insets match Library home policy.
- Selected and unselected tab colors resolve to contrasting palette values.
- The shared dialog selects the correct theme-specific scrim and solid panel colors.
- Dialog bounds and action metrics preserve readable text space on compact dimensions.

Add a JVM-only Compose UI-test regression that locates the shared dialog's semantics node, invokes `SemanticsActions.Dismiss`, and proves the existing dismiss callback removes the dialog. The test-only source set may add `org.jetbrains.compose.ui:ui-test` and `compose.desktop.currentOs`; Android/iOS test configuration and all production dependencies remain unchanged.

Retain existing playlist and library-source behavioral tests. Completion evidence must include:

```bash
openspec validate playlist-dialog-polish --strict
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
GIT_MASTER=1 git diff --check
```

Manual visual QA covers compact and wide desktop windows plus Android/iOS targets where available, light/dark themes, all dialog types, scrim contrast, long localized labels, keyboard focus/submit, and Now Playing bar clearance.

## Implementation Slices

1. Add the shared dialog/presentation policy and focused tests.
2. Migrate settings dialogs and Clear Library to the shared shell.
3. Migrate all playlist dialogs, correct playlist frame/tab/action metrics, and add focused tests.
4. Run strict OpenSpec and platform verification, execute visual QA where a runtime is available, then update the roadmap and progress evidence.
