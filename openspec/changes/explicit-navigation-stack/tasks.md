## 1. Navigation model

- [x] 1.1 Add `LibraryRoute` sealed interface with Home, AlbumDetail, ArtistDetail, NowPlaying, Search, Settings, and ClearLibraryDialog routes.
- [x] 1.2 Add immutable `LibraryNavigationStack` with Home root, `current`, `canPop`, `push`, `replaceTop`, `pop`, and `popToRoot`.
- [x] 1.3 Add common tests proving root cannot pop, duplicate top push is a no-op, push/pop preserves origins, and dialog routes pop back to their opener.

## 2. App route rendering

- [x] 2.1 Replace `selectedAlbum`, `selectedArtist`, and `showNowPlayingScreen` in `LibraryHomeScreen` with a single remembered `LibraryNavigationStack`.
- [x] 2.2 Render Album Detail and Artist Detail from `LibraryRoute.AlbumDetail` / `LibraryRoute.ArtistDetail` by resolving groups from the current snapshot.
- [x] 2.3 Render Now Playing from `LibraryRoute.NowPlaying` and pop back to the previous route through `onBack`.
- [x] 2.4 If an album/artist route no longer resolves after library changes, pop back toward Home instead of rendering stale content.

## 3. Overlay/dialog route integration

- [x] 3.1 Replace `showSettings` and `showSearch` with `LibraryRoute.Settings` and `LibraryRoute.Search` pushes.
- [x] 3.2 Replace clear-dialog visibility with `LibraryRoute.ClearLibraryDialog` while preserving cancel/dismiss/confirm behavior.
- [x] 3.3 Wire bottom bar, drill-down header, settings, search, clear dialog, Android BackHandler, and left-edge swipe callbacks to stack push/pop operations.

## 4. Verification and handoff

- [x] 4.1 Run focused navigation tests.
- [x] 4.2 Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- [x] 4.3 Run `openspec validate explicit-navigation-stack --strict`.
- [x] 4.4 Record implementation evidence, changed files, verification results, and manual Android back/gesture validation recommendation in `progress.md`.
