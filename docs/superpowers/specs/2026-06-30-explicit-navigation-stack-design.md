# Explicit Navigation Stack Design

## Problem

RhythHaus currently represents in-app navigation with several independent mutable flags in `LibraryHomeScreen`: selected album, selected artist, expanded Now Playing, Settings, Search, and dialog visibility. This fixed the immediate Android back bug, but the model is fragile because it does not encode a coherent back stack. It cannot cleanly preserve an origin path such as Home → Album → Search → Now Playing → back to Search → back to Album.

## Goals

- Replace local screen booleans/nullables for main navigation with a single explicit navigation stack.
- Preserve current UI behavior and visual design while making back behavior predictable.
- Keep the implementation shared-first in `shared/src/commonMain`.
- Do not add a navigation dependency; use a small pure Kotlin route model and stack helper.
- Keep Android system back and shared left-edge swipe-back popping one route at a time before the app can close.
- Keep Settings, Search, Now Playing, Album detail, Artist detail, and Clear Library dialog represented in one navigation model.

## Non-goals

- No visual redesign of screens.
- No deep-link handling in this slice.
- No platform-specific native navigation controller.
- No persistence/restoration across process death in this slice.
- No change to playback queue semantics, scanning, or library persistence.

## Recommended approach

Use a lightweight route-stack model in common Kotlin.

`LibraryRoute` is a sealed route type:

- `Home`
- `AlbumDetail(album: String)`
- `ArtistDetail(artist: String)`
- `NowPlaying`
- `Search`
- `Settings`
- `ClearLibraryDialog`

`LibraryNavigationStack` stores an ordered list of routes with `Home` as the immutable root. It exposes pure operations:

- `push(route)` adds a new route unless it is already the current route.
- `replaceTop(route)` swaps the current route while preserving earlier history.
- `pop()` removes the current route when possible and returns whether a route was popped.
- `current` returns the active route.
- `contains(route)` supports state restoration decisions where needed.

The stack is held in Compose state in `LibraryHomeScreen`, but navigation decisions become route operations instead of independent flags.

## Data mapping

Album and artist routes use stable display keys already available in the current grouped UI:

- `LibraryRoute.AlbumDetail(album.album)` resolves the selected album by matching `AlbumGroup.album` from `groupTracksByAlbum(snapshot.tracks)`.
- `LibraryRoute.ArtistDetail(artist.artist)` resolves the selected artist by matching `ArtistGroup.artist` from `groupTracksByArtist(snapshot.tracks)`.

If the library changes and a route no longer resolves, the UI pops back toward Home rather than rendering stale detail content.

## Back behavior

Back is centralized:

1. If current route is `ClearLibraryDialog`, dismiss only the dialog.
2. Else if current route is `Settings`, `Search`, `NowPlaying`, `AlbumDetail`, or `ArtistDetail`, pop exactly one route.
3. Else if current route is `Home`, do not consume back so Android may close the app.

Left-edge swipe-back on detail and Now Playing screens calls the same `pop()` operation.

## Search and Settings origins

Search and Settings are pushed on top of the current route. This preserves origin:

- Home → Search → back returns Home.
- Album detail → Search → back returns Album detail.
- Album detail → Now Playing → back returns Album detail.

Search result selection remains playback-only for this slice; it may stay on Search and update playback state unless a later UX spec decides to dismiss or expand Now Playing.

## Dialog behavior

Clear Library dialog becomes `LibraryRoute.ClearLibraryDialog` pushed above the current route. Cancel, dismiss, Android back, and successful clear all pop that route. Successful clear also clears the repository and tracks as today.

## Tests

Add common JVM tests for the pure navigation model:

- root stack starts at Home and cannot pop past Home;
- push/pop preserves album/search origin;
- duplicate push does not create duplicate top entries;
- clear dialog is just another route and pops back to its origin;
- invalid album/artist route resolution can be handled by popping to Home/detail fallback in UI helper tests if a pure resolver is introduced.

## Risks

- `App.kt` is already large. Keep the first implementation surgical: create the pure navigation model in a new file, then refactor only navigation state and route rendering.
- Route keys are currently names, not database IDs. Duplicate album names across artists already collapse in current grouping, so this does not introduce a new behavior regression. A future library model can add stable group IDs.
- Compose BackHandler is currently used and deprecated in favor of NavigationEventHandler. Keep it for this slice because the current project already depends on it and the route refactor is the main goal.

## Acceptance criteria

- Album/artist detail, Now Playing, Search, Settings, and Clear Library dialog are all driven by `LibraryRoute`/`LibraryNavigationStack` rather than separate navigation booleans/nullables.
- Android back from Search/Settings/Now Playing/detail returns to the previous in-app route instead of exiting when a route is available to pop.
- Opening Search/Settings/Now Playing from a detail route preserves that detail route as the back destination.
- Current UI visuals and playback behavior remain unchanged except for more predictable navigation.
- Focused navigation tests pass, and the existing shared/desktop/Android verification command passes or any blocker is recorded exactly.
