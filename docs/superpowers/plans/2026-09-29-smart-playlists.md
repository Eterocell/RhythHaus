# Smart Playlists Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist six named smart rules, browse live deterministic members and play them without altering static playlists.

**Architecture:** Playlists API owns typed rule definitions; database owns the separate SQLDelight table/migration. Playlists implementation owns repository, pure membership and UI; Shared supplies authoritative Library inputs, routes, Back and playback. Existing snapshot owner serializes persistence and rejects stale publications.

**Tech Stack:** Kotlin Multiplatform, SQLDelight, Compose/Miuix, existing coroutine/Compose JVM tests; no new dependency.

**Spec:** `docs/superpowers/specs/2026-09-29-smart-playlists-design.md`; `openspec/changes/smart-playlists/specs/smart-playlists/spec.md`.

## Global Constraints
- Six rules: Favorites; RecentlyPlayed(count); RecentlyAdded(count); Artist(artist); Album(artist,album); SavedPlaylist(playlistId).
- Recency counts only 10,25,50; default 25; descending persisted timestamps then track ID, no clock cutoff.
- Other set rules order by case-insensitive title then track ID; SavedPlaylist retains source entry order and duplicates.
- No media/tag/permissions or playback writes from rule management. v1 JSON backup remains static only.
- EN/ZH; reachable and actionable at 600×400 dp. Shared remains sole navigation/Back authority.
- Physical/UI/listening manual acceptance is user-owned; automated checks remain implementation-owned.

## Review Focus
- Old schema/user_version zero must migrate at its actual version, preserving all existing rows.
- A deleted source playlist leaves smart rule present, editable, empty with explicit notice.
- Dialog cancellation/reopening and double confirmation must not permit stale callbacks or duplicate writes.
- Dynamic history/favorite/scan publication must replace derived rows without changing an already-playing queue.
- Smart rules cannot appear in static append pickers, manual entry mutation, or JSON v1 backup.

### Task 1: Typed rules, SQL persistence and snapshot
**Files:** create `feature/playlists/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SmartPlaylist.kt`; modify `PlaylistRepository.kt`, both playlist repositories, `ui/PlaylistState.kt`; create `core/database/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/SmartPlaylist.sq` and `migrations/4.sqm`; modify JVM bootstrap and `ExistingDatabaseMigrationTest.kt`; add repository regressions under playlists impl JVM/common tests.
**Interfaces:** `sealed interface SmartPlaylistRule` with `data object Favorites`, `data class RecentlyPlayed(val count:Int)`, `RecentlyAdded(count)`, `Artist(artist:String)`, `Album(artist:String,album:String)`, `SavedPlaylist(playlistId:String)`; `SmartPlaylistSummary(id:String,name:String,rule:SmartPlaylistRule,createdAtEpochMillis:Long,updatedAtEpochMillis:Long)`. Repository: `smartPlaylists():List<SmartPlaylistSummary>`, `createSmartPlaylist(name:String,rule:SmartPlaylistRule):SmartPlaylistSummary`, `updateSmartPlaylist(id:String,name:String,rule:SmartPlaylistRule):Unit`, `deleteSmartPlaylist(id:String):Unit`. Snapshot adds `smartPlaylists` default empty and `smartPlaylist(id)` lookup.
- [ ] Write failing migration/repository regressions: reopen every rule, preserve static/history rows v4 and legacy v0, reject invalid count/blank arguments/missing static source atomically, reject nonexistent target, retain rule after source deletion, delete rule without media/static deletion; static backup snapshot unchanged.
- [ ] Run `./gradlew :core:database:jvmTest :feature:playlists:impl:jvmTest --configuration-cache` scoped selectors; capture RED.
- [ ] Implement schema/table fields from design and `4.sqm`; update v0 table detection to explicit 4L and new schema set. Decode invalid persisted kind/arguments as read failure. Extend/migrate all repository test doubles.
- [ ] Run focused migration/repository GREEN; SQLDelight generation included in build; inspect consumer-facing behavior and commit checkpoint only after review.

### Task 2: Pure membership
**Files:** create `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/SmartPlaylistProjection.kt`; create common tests `SmartPlaylistProjectionTest.kt`.
**Consumes:** Task1 rule/summary and static snapshot; LibraryTrack, favorite ID set, TrackPlayHistory map.
**Produces:** `SmartPlaylistRow(occurrenceId:String,trackId:String)`; `SmartPlaylistProjection(rows:List<SmartPlaylistRow>,sourceMissing:Boolean)`; `projectSmartPlaylist(playlist:SmartPlaylistSummary,tracks:List<LibraryTrack>,favoriteTrackIds:Set<String>,playHistory:Map<String,TrackPlayHistory>,snapshot:PlaylistSnapshot):SmartPlaylistProjection`. Occurrence IDs scoped to smart-list ID and track/static-entry ID, collision-free across duplicate rows.
- [ ] Write regressions for every rule: title/time ties by ID; counts 10/25/50; unplayed exclusion; exact album artist isolation; source A,B,A; removal/missing source; repeat projection after changed favorite/history/scan/static inputs; stable occurrence ID across reorder.
- [ ] Run `:feature:playlists:impl:jvmTest --tests '*SmartPlaylistProjectionTest'` RED.
- [ ] Implement deterministic pure projection without database/clock/artwork-byte reads.
- [ ] Run GREEN and review membership/order invariants.

### Task 3: Feature create/editor/detail
**Files:** create `ui/SmartPlaylistScreens.kt` and JVM `SmartPlaylistScreensJvmTest.kt`; modify `ui/PlaylistScreens.kt`; add owned EN/ZH strings.
**Consumes:** typed definitions, latest Library track/favorite/history inputs, static snapshot, projection, existing feature destination/appearance/dismissal publisher.
**Produces:** public callback-first `SmartPlaylistDetailScreen` with rule editing/deletion and `SavedPlaylistPlaybackRequest`; extend `PlaylistHubScreen` with smart create/open callbacks and current rule options. Editor needs name, rule kind, counts, exact artist/album/static options; parameters remain selected if original option disappeared so listener can repair.
- [ ] Write failing Compose regressions at 600×400 for creation of six rules, persistence failure/retry, editable missing source, EN/ZH semantics, read-only detail (no append/remove/reorder), playback A,B,A second occurrence, stale/double callbacks and Back registration.
- [ ] Run focused UI RED; implement scrolling rule editor and detail with existing HausDialog/feature dismissal pattern, async pending and appearance guard, explicit backup-exclusion note.
- [ ] Run feature JVM GREEN, verify no static controls exported for smart lists, independent feature review.

### Task 4: Shared routes and authoritative composition
**Files:** modify `LibraryNavigation.kt`, `LibraryAppState.kt` where exact deletion ownership is needed, `LibraryAppShell.kt`, `LibraryRoutes.kt`, `App.kt` only for required authoritative inputs; add/extend Shared route JVM/navigation tests.
**Consumes:** Task3 callback-first UI plus immutable Library content, snapshot and controller.
**Produces:** `LibraryRoute.SmartPlaylistDetail(smartPlaylistId:String)` integrated in exhaustive route render/selection/overlay policy. Mutation goes only through existing PlaylistStateOwner; deletion pops only the exact origin after snapshot confirms absence. Playback via existing selectOccurrenceForPlayback.
- [ ] Write RED for create/edit real owner route, dynamic favorite/history/static/source publication, repeat rows queue identity, mutation leaves controller state/queue/timer untouched, failed/stale delete and modal-first Back.
- [ ] Implement all route branches, no feature->Shared dependency and no no-op production callbacks.
- [ ] Run focused Shared GREEN plus compile desktop/Shared Android/iOS to catch exhaustive branches and ABI leaks.

### Task 5: Acceptance and lifecycle
- [ ] Run `:core:database:jvmTest :feature:playlists:impl:jvmTest` and change-focused Shared suite, desktop compilation and Shared Android/iOS test-code compilation. Do not rerun known full Shared baseline failures just to confirm them.
- [ ] Separately run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck` with configuration cache. Validate smart-playlists strict and all canonical specs.
- [ ] Exercise actual desktop surface with disposable local media/profile; if GUI control unavailable run a throwaway real-database projection/playback-selection smoke and record visual limit. Never use user's media for destructive scenarios.
- [ ] Independent final review against all spec requirements; repair findings and verify once after edits settle.
- [ ] Sync canonical smart-playlists spec, archive, mark OpenSpec tasks from evidence, update roadmap/progress with exact results and user-owned manual gaps, remove throwaway files and commit `feat: add smart playlists`. Keep branch separate unless user requested this feature's integration.
