# UI/UX Restructure: Bottom Bar Navigation, Settings, Search

**Status:** draft  
**Date:** 2026-06-29  
**Route:** openspec+superpowers → brainstorming

## Goal

Restructure the main screen to add bottom bar navigation with Settings and Search screens, move the "Manage Music" panel into Settings, and add proper safe area handling.

## Current State

```
┌──────────────────────┐
│ HeaderSection        │
│ ImportAudioCard      │  ← has "Add music folder" + "Clear Library"
│ "Library queue" label│
│ BrowseModePicker     │
│ Album grid / Artists │
│ ... (scrollable)     │
├──────────────────────┤
│ NowPlayingBar (float)│  ← track info + play/pause only
└──────────────────────┘
```

## Target State

### Start Page (no track selected / library empty)

Shows a clean Now Playing area. When no music is loaded, shows empty state with "Import music from Settings" hint.

### Start Page (with tracks)

```
┌──────────────────────┐
│ HeaderSection        │
│ "Library queue" label│
│ BrowseModePicker     │
│ Album grid / Artists │
│ ... (scrollable)     │
├──────────────────────┤
│ NowPlayingBar        │  ← track info + play/pause + 🔍 + ⚙️
│  [safe area inset]   │
└──────────────────────┘
```

### Bottom Bar (`NowPlayingBar`)

- Track info (artwork thumbnail, title, artist) on the left
- Play/pause button in center
- Search button (magnifying glass icon) and Settings button (gear icon) on the right
- Proper bottom safe area padding so it doesn't overlap the home indicator

### Settings Screen

New full-screen overlay or separate composable:

- "Manage Music" section: "Add music folder" button + scan status
- "Clear Library" button with confirmation dialog
- Future: playback settings, appearance, about

### Search Screen

New full-screen overlay or separate composable:

- Search text field at top with auto-focus
- Filters `libraryTracks` list by track name, artist name, or album name (case-insensitive contains match)
- Results displayed as a scrollable track list (similar to `DrillDownView`)
- Empty state when no matches

### Start Page Simplification

- `ImportAudioCard` moved entirely into Settings screen
- The "No tracks imported" guidance text on the start page replaced with brief hint pointing to Settings
- `HeaderSection` and library browser unchanged

## Files Changed

| File | Change |
|------|--------|
| `App.kt` | Remove ImportAudioCard from LazyColumn; add `showSettings`/`showSearch` state; add Settings and Search screens; update `NowPlayingBar` to accept `onSettings`/`onSearch` callbacks |
| `NowPlayingBar.kt` | Add Search + Settings icon buttons; add safe area padding |
| New: `SettingsScreen.kt` | Settings screen composable with Manage Music panel, Clear Library |
| New: `SearchScreen.kt` | Search screen with text field and filtered track list |

## Verification

```bash
./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug --configuration-cache
```

All existing tests pass; new screens compile on all platforms.
