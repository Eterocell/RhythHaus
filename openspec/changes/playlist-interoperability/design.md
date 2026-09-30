# Playlist Interoperability Design

## Boundary

Keep the existing JSON backup codec and document launcher unchanged. Add a separate format-neutral interoperability codec in `feature/playlists/impl` that decodes bounded M3U/M3U8 and PLS documents into ordered metadata entries and encodes static playlist snapshots into the selected format. The codec has no repository or platform dependencies.

## Matching and persistence

Reuse the existing playlist import planning/matching boundary, extending its source projection to accept format-neutral `(title, artist, album, durationSeconds)` entries. Matching continues to use effective LibraryTrack metadata and the existing revision guard. Paths are retained only for export and diagnostics; imports never resolve or persist external paths.

The existing `createWithEntries` operation remains the atomic commit boundary. Smart playlists are omitted from export and cannot be created by import. Duplicate source entries produce duplicate track IDs in the mutation.

## Documents and errors

The platform document launcher advertises M3U/M3U8 and PLS MIME/extension filters while retaining the JSON option. Strict parsers enforce UTF-8, line/field limits, entry/count limits, integer bounds, and malformed-grammar errors before repository access. Export builds the complete byte array before invoking the platform save callback, preventing partial success.

## UI

The existing playlist backup surface gains a format selector and interoperable import/export actions. Existing JSON wording remains explicit as the complete recovery format; interoperability wording states that smart rules and unmatched entries are not exported/restored. Preview and result state reuse the current accessible report, issue, cancellation, and stale-revision patterns.

## Verification

Codec tests cover canonical M3U/M3U8 and PLS round trips, comments, CRLF, escaped/Unicode metadata, malformed and oversized input, duplicate entries, path non-authority, and integer limits. Repository/planner tests cover mixed match results, stable order, duplicate preservation, cancellation, stale revisions, and atomic failure. JVM semantic tests cover EN/ZH format controls, issue summaries, and accessibility. Android, iOS, and desktop document-filter compilation is required; physical picker acceptance remains user-owned.
