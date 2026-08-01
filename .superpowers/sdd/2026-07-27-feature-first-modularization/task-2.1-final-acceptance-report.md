# Task 2.1 Final Acceptance Report

## Decision

Accepted. Independent final Oracle re-review PASSed with no Critical, Important, or Minor findings. All prior equality, API, and evidence findings are resolved.

## Accepted Implementation

- `:core:model` owns the package-stable, explicit public immutable `Track`, `TrackAccent`, `LibrarySnapshot`, `PlayableTrack`, and `AudioSource` contracts.
- `Track` and `PlayableTrack` remain data classes and preserve content-based `equals`/`hashCode` behavior for nullable artwork `ByteArray` values.
- `AudioSource.stableKey` remains. No `PlayableTrack.stableKey` was added.
- `:shared` declares `api(projects.core.model)`. Controller behavior, mapping/formatting helpers, repositories, scanners, UI state, engines, and feature-owned types remain in `:shared`.

## Prerequisite Checker Repairs

- Architecture KDoc filtering skips exactly KSP `Origin.SYNTHETIC` members.
- Normalized standard KMP resource roots below the owning project's `buildDirectory` are excluded; authored custom roots remain checked.
- Full checker functional XML records 52 tests, zero skipped, zero failures, and zero errors.

## Verification Evidence

- Equality RED: focused `ModelContractTest` failed at both equal-content assertions before the historical `equals`/`hashCode` behavior was restored.
- Final `./gradlew :core:model:allTests --configuration-cache` passed twice; the repeat reported `Reusing configuration cache`. JVM, Android host, and iOS simulator arm64 XMLs each record 6 tests, 0 skipped, 0 failures, and 0 errors.
- Final `./gradlew architectureCheck :shared:jvmTest --configuration-cache --configuration-cache-problems=fail --no-parallel` passed.
- `spotlessApply`, separate `spotlessCheck`, separate `detekt`, strict `openspec validate feature-first-modularization --strict`, and `git diff --check` passed.
- Earlier desktop compilation and Android assembly passed after extraction.

## Limits And Deferrals

- A separate shared iOS simulator verification was not completed after the user requested termination of the stuck agent lane. This does not affect the passed `:core:model` iOS simulator tests.
- `./init.sh` remains intentionally not rerun after the prior user-directed stop beyond 9000 seconds. The full platform matrix is not claimed.
- OpenSpec 3.1, 3.2, and 3.3 remain pending because they also require `:core:ui` and/or broader platform verification. Task 2.2+ remains pending.

## Commit Boundary

The accepted Task 2.1 file set is committed atomically by `refactor: extract core model` in the commit containing this ledger/report. No commit SHA is asserted before that commit exists.
