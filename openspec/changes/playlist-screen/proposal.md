## Why

RhythHaus has no durable user-created playlists and no screen for inspecting or editing the live playback queue. Adding one shared Playlist destination gives users an ordered collection workflow while preserving the local-first, cross-platform library and the existing playback-session owner.

## What Changes

- Add a Library playlist hub with `Saved` and `Queue` tabs plus a saved-playlist detail route.
- Add durable create, rename, delete, add, remove, and reorder behavior for saved playlists.
- Support duplicate saved entries through stable occurrence identity and remove entries automatically when their library track is deleted.
- Add per-track `Add to playlist` and playlist-side searchable multi-select workflows.
- Make playback queues and persisted sessions occurrence-aware so duplicate tracks remain distinct during selection, shuffle, skip, reconciliation, and process restart.
- Add serialized commands for reordering, removing, and clearing upcoming queue occurrences while the current occurrence remains pinned.
- Add the SQLDelight schema migration and migration-capable platform database opening required for existing installations.

## Capabilities

### New Capabilities

- `saved-playlists`: Durable saved-playlist persistence, navigation, CRUD, ordered duplicate entries, add workflows, cascade cleanup, and user-facing states.
- `occurrence-aware-playback`: Queue occurrence identity, duplicate-preserving visible-list selection, session persistence compatibility, restore, shuffle, skip, and library reconciliation.
- `editable-playback-queue`: Playlist Queue-tab presentation and controller-owned mutation of upcoming occurrences with a pinned current occurrence.

### Modified Capabilities

None.

## Impact

- Shared SQLDelight schema, migrations, repository interfaces/implementations, and Android/iOS/JVM database factories.
- Shared playback state/controller, selection policy, shuffle/skip logic, session snapshot/store/coordinator, and focused platform/session tests.
- Shared Library navigation, app state/shell, home/search/detail row actions, playlist screens, resources, dependency injection, and navigation/UI-state tests.
- No new dependency, platform-specific playlist behavior, cloud service, or Windows/Linux product support.
