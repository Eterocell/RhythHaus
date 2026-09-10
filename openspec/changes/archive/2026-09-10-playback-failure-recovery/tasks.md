# Tasks: Playback failure recovery

## 1. Shared failure contract and recovery controller

- [x] 1.1 Add RED common tests for structured failure propagation, retry, non-wrapping skip, current-occurrence removal with successor, final removal idle state, persistence checkpoints, and disabled/non-error no-ops.
- [x] 1.2 Add `PlaybackFailureKind`, extend `PlaybackError` compatibly, add structured load-failure transport, and preserve it through controller exception handling.
- [x] 1.3 Add controller retry/skip/remove-failed commands behind existing command and error/current-occurrence gates; serialize removal with queue mutations and preserve occurrence/effective-order semantics.

## 2. Platform classification

- [x] 2.1 Add RED/GREEN Android host tests for Media3/local-path mapping and unknown fallback.
- [x] 2.2 Add RED/GREEN iOS and macOS tests for managed-file absence and opaque native/provider unknown fallback.
- [x] 2.3 Migrate engine load/play failure paths to the shared structured contract without crossing platform error types into common state.

## 3. Now Playing recovery UI

- [x] 3.1 Add RED JVM semantics for absent/error recovery nodes, exact command routing, and removal after replacement state.
- [x] 3.2 Add feature-owned English/Chinese strings, resource-ledger coverage, error-only recovery section, action semantics, and Compose coroutine dispatch for removal.

## 4. Acceptance

- [x] 4.1 Run focused core, Android host, iOS, macOS, and Now Playing regression suites with RED/GREEN records.
- [x] 4.2 Run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, `./init.sh`, strict OpenSpec validation, and diff check. Formatting, Detekt, architecture, strict OpenSpec, and diff checks pass. The supported focused JVM/Android-host/Now Playing matrix passes; iOS test code compiles, while Xcode reports no simulator-test support for `ios_simulator_arm64`. `./init.sh` reaches the existing baseline `LibraryPlaybackSelectionTest.differentSelectionPreservesRepeatAndShuffleModes` timeout, which was previously reproduced on `main`.
- [x] 4.3 Exercise a known playable source, make its selected path unavailable, and confirm retry/skip/remove behavior on each available platform. A real native macOS engine regression proves playing-file invalidation, `MissingFile`, same-occurrence retry, non-wrapping tail behavior, exact duplicate-occurrence preservation, successor autoplay, and survivor-file retention. Android has no connected target; the connected iPhone cannot be safely automated through this harness without mutating its persisted library, and iOS simulator execution is unavailable. These paths are recorded as unverified blockers, not passes. `progress.md` and `roadmap.md` record the evidence before archival.
