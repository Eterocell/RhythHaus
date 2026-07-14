## 1. Build and resource foundation

- [x] 1.1 Add AboutLibraries `15.0.3` plugin/dependency aliases and configure an explicit regular-plugin export that produces a checked-in common JSON catalog without mutating normal builds.
- [x] 1.2 Generate common Kotlin version metadata from `rhythhaus.versionName` with cache-safe Gradle inputs/outputs and override-property regression coverage.
- [x] 1.3 Add a standalone shared Compose logo drawable, English/Chinese resource strings, and a regression test that validates the pinned attribution catalog parser/resource.

## 2. Settings navigation and About page

- [x] 2.1 Add About and Open Source Libraries routes to the existing route stack and prove stack push/pop plus compact/wide overlay rendering behavior with common tests.
- [x] 2.2 Add the accessible Settings About entry while preserving existing layout and source-management behavior.
- [x] 2.3 Implement the shared About screen with logo, name, generated version, source URL action, and Open Source Libraries action.

## 3. Open-source attributions

- [x] 3.1 Implement the shared Open Source Libraries screen with loading, recoverable failure/retry, and AboutLibraries Material 3 catalog rendering.
- [x] 3.2 Regenerate and review the checked-in attribution catalog after dependency changes, including Android/JVM/iOS attribution completeness policy.

## 4. Verification and durable evidence

- [x] 4.1 Run focused regression tests, full supported JVM/Android/desktop verification, OpenSpec validation, iOS toolchain/simulator validation, and diff hygiene.
- [x] 4.2 Update OpenSpec completion state, roadmap item 19, and `progress.md` with actual validation evidence and any retained iOS blocker.
- [x] 4.3 Add CI enforcement that regenerates, tests, and rejects drift in the checked-in AboutLibraries catalog.
