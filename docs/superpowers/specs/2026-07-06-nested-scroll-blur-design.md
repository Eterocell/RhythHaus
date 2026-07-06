# Nested Scroll Blur Design

Route: openspec+superpowers

## Goal

Add a Material 3 Expressive-inspired nested-scroll visual treatment to the shared Compose Library/Home and track-list pages without changing navigation, playback, scanning, or data behavior.

## Design

RhythHaus will keep its existing paper/panel visual language and add a lightweight shared scroll chrome layer:

- Scrollable Library/Home and album/artist track-list pages remain `LazyColumn` based.
- Each page gets a transparent-to-frosted top overlay that reacts to scroll position.
- As the user scrolls content underneath the top area, the overlay becomes more opaque, applies a small blur/backdrop-style scrim, and slightly compresses/settles the header area.
- The content list keeps enough top padding/spacer so the first row can visually pass under the chrome without being hidden.
- Existing `NowPlayingBar` scroll-hide behavior remains unchanged.

## Approach

Use Compose Multiplatform common APIs plus the multiplatform Haze library (`dev.chrisbanes.haze:haze`) from `commonMain`. Lists register as `hazeSource` and the top chrome uses `hazeEffect`, so the roadmap requirement for a Haze/Backdrop blur treatment is implemented with a real shared Haze dependency.

## Acceptance Criteria

- Library/Home scroll has a top nested-scroll chrome effect.
- Album and artist track lists have the same effect.
- The effect strengthens only after content scrolls under the top area.
- Existing NowPlayingBar hide/show behavior is preserved.
- No new platform-specific files are required.
- JVM shared compile and shared JVM tests pass.
