# Sorting/filtering full-branch review

Initial verdict: **Incorrect**; three Important findings, no Critical or Minor findings. The reviewer response failed its output schema after duplicating enum fields, but its anchored findings were independently checked against the production code.

1. Artwork-only compared eager `Track.artworkBytes`, while `selectAllTracks` and the in-memory repository intentionally return tracks without artwork bytes. Repaired with a non-blob persisted artwork-presence query, repository contract, and Shared projection. Verified by SQL repository and Home UI tests.
2. Title/Ascending represented both the initial mode default and an explicit choice. Repaired by tracking whether a sort was explicitly selected; recent modes retain their established order only before an explicit selection. Verified by query and Shared state tests.
3. Missing play history was sorted as null instead of a zero play count. Repaired with zero fallback for play count and absent timestamps. Verified by query tests and the updated Home projection expectation.

Post-repair review and cross-platform gates: pending.
