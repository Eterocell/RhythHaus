## 1. Cross-platform tag extraction
- [x] 1.1 Extract four raw ReplayGain tags through native path/FD metadata and all Kotlin adapters; parse independently with malformed-field regressions.
- [x] 1.2 Prove real tagged-media path/FD extraction and unchanged media bytes; compile Android/iOS readers.

## 2. Persistence and projections
- [x] 2.1 Add v7→v8 nullable facts migration and version-zero detection; prove file-backed migration/reopen preserving user state.
- [x] 2.2 Carry facts through scanner, all repositories, display overrides and playable mappings; prove rescan clearing and unreadable preservation.

## 3. Playback policy
- [x] 3.1 Implement finite attenuation-only Track/Album selection and peak protection with numeric boundary regressions.
- [x] 3.2 Compose with sleep attenuation under existing ownership; prove pre-play gain, cancellation, replacement, metadata/reconcile and remote-command paths without reload or transport change.

## 4. Settings and persistence
- [x] 4.1 Persist accepted mode using Shared platform stores, safe default and serialized failure/cancellation publication.
- [x] 4.2 Wire localized accessible Settings controls, explanation and recoverable errors; prove selection, restart and failed-write behavior.

## 5. Acceptance and lifecycle
- [x] 5.1 Run tagged-media scan/reopen/playback smoke, platform matrix and independent quality gates; record user-owned physical acceptance separately.
- [x] 5.2 Obtain independent task/whole-change review and repair concrete findings.
- [x] 5.3 Sync canonical specs, archive, update roadmap/progress with exact evidence and commit without implicit merge/push.
