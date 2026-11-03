# Proposal

## Why

The current UI has route-dependent safe-area handling that can differ between a settled screen and its transition frames, and the Now Playing surface no longer dismisses by downward drag. The approved adaptive direction needs one coherent, platform-aware presentation contract across the full app surface before implementation begins.

## What Changes

- Establish a shared adaptive app shell with one safe-area/inset owner, a measured Shared route header, stable navigation and playback chrome, and transition geometry that does not jump between static and animated states.
- Adapt the existing Home-rooted navigation shell to compact bottom navigation and wider rail/sidebar layouts using the existing shared window-size policy. Navigation remains independent of playback; Settings remains a global tool action.
- Redesign the presentation of all existing screens and named states in the approved inventory, preserving their feature ownership, current data/state flows, playback/scanning/storage behavior, and existing route/back semantics.
- Restore downward dismissal for the expanded Now Playing Controls surface. Preserve controls, queue reordering, lyrics/timer scrolling, higher-layer dialogs, and platform Back ownership; do not add new iOS interactive-back behavior or a second navigation authority.
- Use the separate `theme-selection` change for the new palette contract; this change consumes that contract and does not own palette persistence or Android Monet resolution.

## Capabilities

### New Capabilities
- `adaptive-app-shell`: stable safe-area ownership, responsive shared navigation/chrome, and transition geometry.
- `adaptive-screen-presentation`: complete adaptive presentation of existing feature routes and their required states.
- `now-playing-dismissal`: safe downward dismissal of the expanded player while preserving nested interaction and Back precedence.

### Modified Capabilities
None. Existing library, playlist, selection, playback-failure, onboarding, desktop-import, dialog, and scroll-chrome requirements remain authoritative and are preserved rather than redefined.

## Impact

- `:shared`: `library/ui/AppShellLayout.kt`, `LibraryAppShell.kt`, `LibraryRoutes.kt`, navigation/back composition, and their JVM UI tests.
- `:core:ui`: shared semantic colors, dimensions, typography, and reusable presentation components; the theme-selection change owns the palette model and preference contract.
- Feature-owned presentation: `feature/library/impl`, `feature/search`, `feature/playlists/impl`, `feature/nowplaying`, `feature/settings`, and `feature/onboarding`, including their common English/Simplified Chinese resources and existing UI tests.
- No database schema, scanner, repository, playback engine, queue transaction, platform import, packaging, or dependency change is intended. No Windows or Linux support is added.
- Planning references: the [approved redesign brief](../../../docs/superpowers/specs/2026-10-29-platform-adaptive-ui-redesign-design.md), [interactive prototype](../../../docs/superpowers/designs/2026-10-29-platform-adaptive-ui-redesign.html), [token evidence](../../../docs/superpowers/designs/2026-10-29-platform-adaptive-ui-redesign.tokens.json), [adaptive implementation plan](../../../docs/superpowers/plans/2026-10-30-platform-adaptive-ui-redesign.md), and separate [theme-selection plan](../../../docs/superpowers/plans/2026-10-30-theme-selection-palettes.md).
