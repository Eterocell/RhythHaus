## Why

The playlist hub currently has a larger content inset than Library home, and its Saved/Queue labels can lose contrast or clip text in compact light-theme layouts. Dialogs also use several incompatible shells, including transparent or liquid-glass panels and dark-mode scrims that do not match the intended design system.

## What Changes

- Align playlist hub insets and vertical rhythm with Library home while preserving its back affordance and Now Playing clearance.
- Make Saved/Queue tabs and compact playlist actions use explicit contrasting colors and text-safe metrics.
- Add an app-wide solid-panel dialog primitive with accessible dismissal, bounded scrollable content, and a light dim treatment in dark mode.
- Migrate all current Clear Library, source-removal, playlist, picker, and queue confirmation dialogs to the shared primitive without changing their behavior or copy.

## Capabilities

### New Capabilities
- `haus-dialog-system`: Shared Compose dialog presentation, theme behavior, accessibility, and migration requirements.
- `playlist-visual-polish`: Playlist hub insets, tab contrast, and text-fit requirements.

### Modified Capabilities

- None.

## Impact

- Shared Compose UI in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/`, `library/ui/`, and `settings/`.
- Existing playlist and source-management UI tests receive focused presentation-policy coverage; persistence, route, localization, playback, and dependency contracts remain unchanged.
