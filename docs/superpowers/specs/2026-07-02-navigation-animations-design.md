# Navigation Animations Design

## Context

RhythHaus already uses a shared Compose `LibraryNavigationStack` to render Home, album detail, artist detail, Now Playing, Search, Settings, and Clear Library dialog routes. The root route switch in `App.kt` changes screens instantly today, so navigation lacks spatial continuity even though back handling and route ownership are centralized.

This change adds route transition animation inside shared Compose. The goal is to improve perceived polish across Android, iOS-hosted Compose, and desktop/macOS without replacing the existing route stack or migrating to native platform navigation.

## Goals

- Animate route transitions for all shared Compose screens: Home, album detail, artist detail, Now Playing, Search, Settings, and Clear Library dialog.
- Keep route ownership centralized in `LibraryNavigationStack` and root rendering in `LibraryHomeScreen`.
- Use direction-aware transitions: forward pushes should feel like moving deeper; back pops should feel like returning.
- Preserve existing visible back buttons, left-edge swipe-back, Android system/predictive back handling, route behavior, playback behavior, scanner behavior, library persistence, and theme behavior.
- Avoid adding dependencies.

## Non-goals

- No native SwiftUI, Android Navigation, desktop-native, or platform-specific navigation migration.
- No deep links, saved-state restoration, route serialization, or navigation-library adoption.
- No redesign of Home/detail/Search/Settings/Now Playing content.
- No custom Android predictive-back progress animation in this change.
- No claim of live visual QA without device or screenshot evidence.

## Design

### Navigation motion model

Add a small shared navigation transition model next to the existing route stack. The model should identify the transition kind whenever navigation changes:

- `Push`: a new route is added on top of the stack.
- `Pop`: the stack returns to the previous route.
- `Replace`: the current top route is replaced.
- `Root`: navigation returns to Home.
- `None`: no route change.

The route stack remains the source of truth. The transition kind only describes how the last state change should animate; it must not introduce a second navigation state machine.

### Root-level animation

Wrap the root route rendering in `AnimatedContent(targetState = navigation.current)` at the `LibraryHomeScreen` route owner. This keeps animation behavior in one place and avoids per-screen duplicated enter/exit code.

Recommended transition rules:

- `Push`: new screen fades in and slides from the right; old screen fades out and slides slightly left.
- `Pop`: previous screen fades in and slides from the left; dismissed screen fades out and slides right.
- `Replace`: use a subtle fade with minimal horizontal movement.
- `Root`: use a pop-like return animation toward Home.
- `None`: no meaningful route change; avoid unnecessary animation churn.

Use conservative timing around 220-260ms so transitions feel responsive on mobile and desktop. The animation should use existing Compose animation APIs from current dependencies.

### Overlay routes

Search, Settings, Now Playing, and Clear Library dialog are currently shared routes, not native modal destinations. They should participate in the same route animation layer so opening them from Home or detail screens preserves stack expectations. Clear Library dialog may continue to render visually as a dialog, but route entry/exit should still respect the stack transition.

### Existing behavior preservation

Visible back controls, left-edge swipe-back, and Android system/predictive back already call the route-pop path. They should continue to call the same pop function; only the visual transition between route render states changes.

Playback and selection state must not reset because of animation. `AnimatedContent` content keys and remembers should be structured so route changes do not accidentally recreate unrelated controller or library state.

## Acceptance criteria

- Home, album detail, artist detail, Now Playing, Search, Settings, and Clear Library dialog transitions animate when entered/exited through the shared route stack.
- Push and pop transitions use opposite horizontal direction, making forward/back movement visually clear.
- Existing route-stack behavior remains unchanged: back pops exactly one route, Search/Settings opened from detail return to that detail screen, and Home cannot pop.
- No new dependency is added.
- Common navigation tests cover transition classification or equivalent pure route-transition behavior.
- Focused tests and broad JVM/desktop/Android verification pass, or blockers are recorded exactly.

## Risks

- `App.kt` is large, so edits should be limited to route ownership/rendering and small helper functions.
- `AnimatedContent` can accidentally duplicate active composable content during transitions. Keep side effects outside animated route content where possible and avoid adding new side effects inside transition lambdas.
- Visual feel still needs manual validation on real devices/desktops; automated checks prove behavior and compilation, not subjective polish.
