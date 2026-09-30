# Design

## Context

See `proposal.md`. `LibraryScanner` writes source tags through `LibraryRepository.upsertTrack`; `library_track` holds those raw fields and has stable IDs across upserts by `(sourceId, sourceLocalKey)`. Shared `App.kt` owns the Library projection and its revision-serialized publication boundary. Playback queues copy `PlayableTrack` metadata; merely replacing Library rows will leave already queued and system-media metadata stale. The existing static-playlist backup v1 is intentionally unchanged.

## Goals / Non-Goals

**Goals:** Preserve raw tags, durable per-field corrections, scan-safe authoritative publication, consistent visible/grouped/search/playlist metadata, and non-interrupting propagation to active queue/system media controls.

**Non-Goals:** Modifying source media, embedded artwork, filenames, duration, bulk tag edits, cross-device sync, exporting overrides in static-playlist JSON, or a new media scanner.

## Decisions

1. **Separate physical relationship.** Add `track_metadata_override(trackId PRIMARY KEY REFERENCES library_track(id) ON DELETE CASCADE, title, artist, album, trackNumber, discNumber)` in `:core:database` and `migrations/5.sqm` (the smart-playlists branch ends at migration 4). Nullable columns mean “use scanned value”; empty rows are removed. A value is normalized (trim text, blank → null, positive integers only). No scanner changes to tag extraction; upsert keeps the same track ID, so corrections survive re-scan. Add JVM legacy-version-zero table recognition for the new physical schema. SQLDelight migration snapshots and database migration tests must cover real older schema upgrade and cascade cleanup.

2. **Repository owns the effective view.** Introduce a small `TrackMetadataOverride` value contract (nullable fields), `TrackMetadataEditorData(scannedTrack, overrides)`, `LibraryRepository.metadataForTrack(trackId)` and `setTrackMetadataOverride(trackId, overrides): Boolean`. The latter checks track existence and replaces all five fields transactionally; `false` means deleted/unknown track, not an empty successful write. `tracks()`/`tracksForSource()` return effective metadata sorted on effective title/artist, while scanner's keyed upsert still reads and stores raw tags. `metadataForTrack` reads current raw fields and overrides for the editor; do not load artwork bytes. In-memory repository matches SQL behavior. Public repository test doubles must be migrated, not given no-op defaults.

3. **App owns write ordering.** Add a metadata user-state operation alongside favorite writes, not inside scan admission. Under `AuthoritativeLibraryPublicationOwner.mutateAndPublish`, perform the repository write and load a complete fresh `LibraryContentState` on the IO dispatcher; publish the result on Main with revision protection. A scan's publication must reconcile both favorites/history and effective metadata by re-reading a fresh Library projection inside the publication owner's lock; stale scan payloads cannot roll back the correction. A failed write retains the editor draft and emits an error. Closing or navigating away invalidates callbacks scoped to the editor instance.

4. **UI leaf stays in Library feature.** Provide a Songs-row secondary edit action (disabled during multiselect) and a shared Compose editor that displays the latest scanned field alongside each editable override; blank text and cleared number mean inherit. Restore-one and Restore-all clear overrides in the draft; Save is one atomic replacement. Shared owns editor destination/dialog lifecycle, loading, callbacks and error; `:feature:library:impl` owns labels/resources and drawing. Preserve minimum 600×400 dp scroll/accessibility behavior and localization for EN/ZH.

5. **Playback updates are metadata-only.** Add a controller method to replace the metadata of matching queue occurrences by track ID without changing occurrence IDs, engine generation, playback status, position, repeat/shuffle, timer or current audio. Publish one checkpoint only if the persisted queue representation actually changed. Platform engines update loaded system metadata only where supported without load/seek/play: iOS refreshes Now Playing info in its serialized boundary and macOS updates existing MPNowPlayingInfo. Check Android Media3's in-place metadata API against the actual session/player; if changing the loaded item would reload it, defer the system label until the next load. App Now Playing updates immediately. Record any platform lag in acceptance rather than interrupting playback. On restore or future selections, queues use effective metadata naturally. Guard against a concurrent selection replacing the queue before a stale metadata update.

## Risks / Trade-offs

- Metadata edit and scan can race; raw read and effective projection must be performed after the committed write and revision-serialized with scan publication. Tests need an intentionally delayed scan publication.
- A re-scan may change the raw value under a correction; restore must reveal the newest raw value, not the value cached when the editor opened. If the scanned track has been deleted, saving fails without resurrection.
- Platform media-session metadata updates may require different native APIs; verify loaded playback remains uninterrupted on each platform and leave real-device observation to the user.
- Running a migration on the smart-playlists branch (not the older `main`) is intentional. Do not ship this change independently of its prerequisite branch unless the smart change has first been integrated.
