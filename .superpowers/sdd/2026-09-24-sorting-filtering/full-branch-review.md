# Sorting/filtering full-branch review

Initial verdict: **Incorrect**; three Important findings, no Critical or Minor findings. The reviewer response failed its output schema after duplicating enum fields, but its anchored findings were independently checked against the production code.

1. Artwork-only compared eager `Track.artworkBytes`, while `selectAllTracks` and the in-memory repository intentionally return tracks without artwork bytes. Repaired with a non-blob persisted artwork-presence query, repository contract, and Shared projection. Verified by SQL repository and Home UI tests.
2. Title/Ascending represented both the initial mode default and an explicit choice. Repaired by tracking whether a sort was explicitly selected; recent modes retain their established order only before an explicit selection. Verified by query and Shared state tests.
3. Missing play history was sorted as null instead of a zero play count. Repaired with zero fallback for play count and absent timestamps. Verified by query tests and the updated Home projection expectation.

Independent repair review found no remaining Critical or Important issue, but identified one Minor accessibility mismatch: recent modes announced Title/Ascending selected while using the initial date order. Home now passes the active mode to its controls and leaves sort/direction unselected until an explicit choice; the rendered controls test covers both states. Full Library JVM and focused Shared JVM tests passed after that repair. Final independent re-review of `4e539ff1` returned Ready with no actionable Critical or Important defect. Canonical specs were synced and the change archived at `openspec/changes/archive/2026-09-28-sorting-filtering/`.
