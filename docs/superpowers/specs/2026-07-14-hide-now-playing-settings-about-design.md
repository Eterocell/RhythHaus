# Hide Now Playing Bar on Settings Information Screens Design

## Summary

RhythHaus will not show the Now Playing bottom bar while the current route is Settings, About, or Open Source Libraries. Playback continues without interruption, and returning to another route restores the bar according to its existing scroll-driven visibility state.

## Visibility Policy

The navigation layer will expose one route policy that answers whether a route permits the Now Playing bottom bar. `LibraryRoute.Settings`, `LibraryRoute.SettingsAbout`, and `LibraryRoute.OpenSourceLibraries` do not permit it. Home, album detail, artist detail, Search, Now Playing, and any existing overlay route retain their current behavior.

The app shell will combine this route policy with the existing bottom-bar visibility state. Route suppression does not mutate playback state, selected track state, or the scroll-derived visibility flag. This keeps presentation policy separate from playback and ensures that leaving any suppressed settings-information route restores the correct existing state.

## Alternatives Considered

Screen-local hiding was rejected because all three settings-information screens would need to coordinate with shell-owned chrome. A generic route-metadata framework was rejected as unnecessary for a small route group. A pure route predicate is centralized, explicit, and directly testable.

## Testing and Verification

Common navigation tests will first establish RED coverage for the route policy: Settings, About, and Open Source Libraries suppress the bar, while representative non-settings routes permit it. Tests will also cover composition with the existing visibility flag so a route cannot force a scroll-hidden bar visible. Focused common tests and the supported JVM, desktop, and Android build matrix will verify the change; iOS verification will be attempted and any known unrelated common-test blocker recorded exactly.

## Scope

This change affects shared navigation presentation only. It does not change playback, queue state, persistence, scanning, Settings/About/Open Source Libraries content, platform entry points, dependencies, or database schemas.
