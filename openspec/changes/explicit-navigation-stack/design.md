## Context

Current navigation is distributed across local Compose state in `App.kt`: selected album, selected artist, expanded Now Playing, Settings, Search, and Clear Library dialog. Back handlers were added to each surface, but each surface still owns its own navigation flag. This makes origin-preserving flows brittle and makes future navigation features harder.

Constraints:
- Shared-first KMP implementation under `shared/src/commonMain`.
- No new navigation dependency for this slice.
- Preserve current screens and visual design.
- Keep Compose `BackHandler` for now because it already exists in the project and this slice is about state structure.

## Goals / Non-Goals

Goals:
- One explicit in-app stack where `Home` is the root.
- Route entries for Album Detail, Artist Detail, Now Playing, Search, Settings, and Clear Library dialog.
- Back and swipe-back pop exactly one route when possible.
- Search/Settings/Now Playing opened from detail routes return to that detail route on back.
- Pure navigation model covered by common tests.

Non-goals:
- Deep linking.
- Saved-state/process death restoration.
- Native navigation frameworks.
- Screen redesign or playback behavior changes.

## Decisions

### Decision: Pure Kotlin stack model

Create `LibraryRoute` and `LibraryNavigationStack` in a new common file. The stack is immutable from the UI perspective: operations return a new stack. This makes it easy to test and avoids hidden mutations inside Compose state.

Routes:
- `LibraryRoute.Home`
- `LibraryRoute.AlbumDetail(album: String)`
- `LibraryRoute.ArtistDetail(artist: String)`
- `LibraryRoute.NowPlaying`
- `LibraryRoute.Search`
- `LibraryRoute.Settings`
- `LibraryRoute.ClearLibraryDialog`

Operations:
- `current: LibraryRoute`
- `canPop: Boolean`
- `push(route: LibraryRoute): LibraryNavigationStack`
- `replaceTop(route: LibraryRoute): LibraryNavigationStack`
- `pop(): LibraryNavigationStack`
- `popToRoot(): LibraryNavigationStack`

`Home` is always root. Pushing `Home` pops to root. Pushing the current top route is a no-op.

### Decision: Resolve album/artist details from current snapshot

Album and artist routes store the current group display key (`album` or `artist`) because that matches existing grouping behavior. The UI resolves the route by recomputing `groupTracksByAlbum(snapshot.tracks)` or `groupTracksByArtist(snapshot.tracks)`. If no matching group exists after library changes, the UI pops to Home.

This does not make duplicate album names worse because current grouping already groups only by album title.

### Decision: Dialog is a route

`ClearLibraryDialog` sits on top of whichever route opened it. Dismiss/cancel/back pop only the dialog. Confirming clear performs the current repository clear and pops the dialog.

### Decision: Keep screen composables callback-driven

`DrillDownView`, `NowPlayingScreen`, `SearchScreen`, and `SettingsScreen` continue receiving `onBack`/`onDismiss` callbacks. The parent now wires those callbacks to `popRoute()` instead of toggling route-local booleans.

## Risks / Trade-offs

- `App.kt` remains large. This slice avoids a broad split to reduce risk.
- Route keys are display strings. Future stable group IDs can be added when the library model has them.
- Compose BackHandler deprecation remains. Migrating to NavigationEventHandler is a separate technical task.

## Migration / Verification

- Add pure common tests first and verify RED before implementation.
- Refactor `App.kt` to derive rendering from `navigation.current`.
- Run focused navigation tests.
- Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- Record any manual Android back/gesture validation gap in `progress.md`.