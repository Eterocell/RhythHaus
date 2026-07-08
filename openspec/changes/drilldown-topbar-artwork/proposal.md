# Change: Drill-Down Top-Bar Artwork

## Why

Album and artist detail screens already have embedded artwork available through their tracks, and the app now prefers real artwork over letter placeholders. The drill-down Miuix top bar still shows only text, so album/artist track lists lose visual context at the top of the screen.

## What Changes

- Derive representative artwork for album and artist detail routes from the first track in the detail list with embedded artwork bytes.
- Pass that optional artwork into the existing drill-down Miuix top-bar chrome.
- Render the artwork as the full drill-down Miuix top-bar background using the existing cached artwork decode path; when artwork is present, remove the glass/blur effect from that top bar, draw the back button on a circular surface, and draw the album/artist title on Material-chip-like pill backgrounds.
- Preserve current title collapse, back navigation, glass chrome, list rows, playback, and fallback behavior.

## Impact

- Shared Compose UI only.
- No dependency, schema, scanner, TagLib, playback-engine, or platform-source changes.
- Manual QA remains useful for visual placement on device/simulator; automated verification covers compile, library tests, OpenSpec validation, and platform build/test commands.
