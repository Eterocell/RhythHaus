# Task 1 Fix Report

**Date:** 2026-06-24  
**Branch:** `feat/folder-import-album-browse`  
**Base commit:** `c5f0a06`

## Issues Fixed

Three code quality issues from the Task 1 review have been addressed.

### 1. Restored FOREIGN KEY constraints

Three foreign key relationships were restored with `ON DELETE CASCADE` semantics:

| File | Column | References |
|------|--------|------------|
| `LibraryTrack.sq` | `sourceId` | `library_source(id) ON DELETE CASCADE` |
| `ScanSession.sq` | `sourceId` | `library_source(id) ON DELETE CASCADE` |
| `ScanError.sq` | `scanId` | `scan_session(id) ON DELETE CASCADE` |

These ensure that:
- Deleting a `library_source` cascades to its related `library_track` and `scan_session` rows.
- Deleting a `scan_session` cascades to its `scan_error` rows.

### 2. Restored UNIQUE INDEX on LibraryTrack

Added to `LibraryTrack.sq`:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS library_track_source_key ON library_track(sourceId, sourceLocalKey);
```

This enforces the business rule that a track is uniquely identified by `(sourceId, sourceLocalKey)` at the database level, preventing duplicate inserts and providing fast indexed lookups.

### 3. Added `selectTrackBySourceKey` query and optimized `upsertTrack()`

**SQL change** (`LibraryTrack.sq`):

```sql
selectTrackBySourceKey:
SELECT * FROM library_track WHERE sourceId = ? AND sourceLocalKey = ?;
```

**Kotlin change** (`SqlDelightLibraryRepository.kt`):

`upsertTrack()` previously loaded **all tracks** via `selectAllTracks()` and filtered in-memory with `.firstOrNull`. This was an O(n) scan that would degrade with library size.

Now it uses the new `selectTrackBySourceKey` query with `executeAsOneOrNull()`, which:
- Uses the `library_track_source_key` index for O(log n) lookup.
- Returns `DomainTrackRow?` — if non-null, it's an UPDATE (preserving `id` + `createdAt`); if null, it's an INSERT.

## Files Modified

| File | Change |
|------|--------|
| `shared/src/commonMain/sqldelight/.../LibraryTrack.sq` | FK constraint, UNIQUE INDEX, `selectTrackBySourceKey` query |
| `shared/src/commonMain/sqldelight/.../ScanSession.sq` | FK constraint on `sourceId` |
| `shared/src/commonMain/sqldelight/.../ScanError.sq` | FK constraint on `scanId` |
| `shared/src/commonMain/kotlin/.../SqlDelightLibraryRepository.kt` | `upsertTrack()` uses indexed lookup |

## Verification

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Result: **BUILD SUCCESSFUL** in 2s. No new warnings or errors.
