# Tasks: Playback failure recovery

## 1. Shared failure contract and recovery controller

- [ ] 1.1 Add RED common tests for structured failure propagation, retry, non-wrapping skip, current-occurrence removal with successor, final removal idle state, persistence checkpoints, and disabled/non-error no-ops.
- [ ] 1.2 Add `PlaybackFailureKind`, extend `PlaybackError` compatibly, add structured load-failure transport, and preserve it through controller exception handling.
- [ ] 1.3 Add controller retry/skip/remove-failed commands behind existing command and error/current-occurrence gates; serialize removal with queue mutations and preserve occurrence/effective-order semantics.

## 2. Platform classification

- [ ] 2.1 Add RED/GREEN Android host tests for Media3/local-path mapping and unknown fallback.
- [ ] 2.2 Add RED/GREEN iOS and macOS tests for managed-file absence and opaque native/provider unknown fallback.
- [ ] 2.3 Migrate engine load/play failure paths to the shared structured contract without crossing platform error types into common state.

## 3. Now Playing recovery UI

- [ ] 3.1 Add RED JVM semantics for absent/error recovery nodes, exact command routing, and removal after replacement state.
- [ ] 3.2 Add feature-owned English/Chinese strings, resource-ledger coverage, error-only recovery section, action semantics, and Compose coroutine dispatch for removal.

## 4. Acceptance

- [ ] 4.1 Run focused core, Android host, iOS, macOS, and Now Playing regression suites with RED/GREEN records.
- [ ] 4.2 Run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, `./init.sh`, strict OpenSpec validation, and diff check.
- [ ] 4.3 Exercise a known playable source, make its selected path unavailable, and confirm retry/skip/remove behavior on each available platform. Update `progress.md` and `roadmap.md`, then commit/archive.
