# Task 1.4 Final Acceptance Report

## Decision

Task 1.4 is accepted. Independent Oracle review returned PASS with no Critical, Important, or
Minor findings. Task 1.3 is unchanged. Task 1.5 and later work, plus OpenSpec 8.4, remain
pending.

## Accepted Implementation

- Root `check` and `qualityCheck` depend on `architectureCheck`.
- Root `qualityCheck` provider-aggregates the actual `detekt` and `spotlessCheck` providers for
  every project.
- TestKit valid and illegal fixtures preserve exact `ARCH-*` diagnostic behavior through both
  entrypoints.
- The real child-project regression fixture uses built-in `Copy` sentinels. It proves child
  `detekt` and `spotlessCheck` execution via exact marker files, confirms `:architectureCheck`
  `TaskOutcome`, and reuses the configuration cache on its second inner `qualityCheck` build.
- Dedicated unfiltered `.github/workflows/quality.yml` runs for pull requests and pushes to
  `main`. It has exactly one Gradle quality command:
  `./gradlew qualityCheck --configuration-cache --configuration-cache-problems=fail --no-parallel`.
  It contains no direct `architectureCheck`, `spotlessCheck`, or `detekt` command.

## Verification Evidence

- Serial `:architecture-processor:clean :architecture-processor:jar` and full
  `:build-logic:convention:test` with the canonical JAR property passed with
  configuration-cache reuse. `ArchitectureCheckPluginFunctionalTest` XML records 50 tests, zero
  skipped, failures, and errors.
- Canonical production `qualityCheck` passed with 85 tasks. Standalone `spotlessCheck` and
  `detekt` passed; `spotlessApply`, strict OpenSpec validation, and `git diff --check` passed.

## Limits And Deferrals

Repeated root `qualityCheck` configuration-cache reuse is not claimed. The unchanged standalone
Spotless/precompiled-script input invalidation stores valid cache entries but does not reuse them.
Independent review accepted that as a nonblocking performance baseline; the child fixture proves
the new wiring is configuration-cache serializable and reusable.

`./init.sh` was not rerun. It was previously manually stopped after more than 9000 seconds at the
user's explicit request, and the user authorized skipping it. The full JVM, Android, desktop, and
iOS platform matrix therefore remains uncertain and is not claimed.

The accepted Task 1.4 file set is committed conventionally as part of this change.
