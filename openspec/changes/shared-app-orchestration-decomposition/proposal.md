# Proposal

## Why

`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` is 2,362 lines, with `App()` spanning approximately lines 180–1338 and composing dependency collection, scan/source operations, playlist document workflows, theme and preference callbacks, and the route shell. The July architecture-refactor tasks are complete, but the current entrypoint has accumulated another large orchestration body; a focused behavior-preserving split will make future changes reviewable without changing the documented Shared ownership boundary.

## What Changes

- Keep `:shared` as the sole app composition root and owner of cross-feature policies; reduce `App.kt` to dependency collection and composition handoff.
- Move cohesive app-owned state/effect groups from the `App()` body and file-level helper section into focused internal Shared files, reusing `AppProcessState`, `AppLibraryOrchestrator`, `LibraryOperationCoordinator`, `LibraryAppState`, and the existing shell rather than introducing parallel owners.
- Preserve public declarations, package names, dependency graph, persistent state, operation serialization, cancellation/publication behavior, navigation/Back authority, resource ownership, and all user-visible behavior.
- Update the exact Shared source inventory test and architecture guidance only for files proven to remain Shared-owned.
- Add or retain behavior-boundary regressions for extracted orchestration and run the affected verification matrix before marking implementation tasks complete.

## Capabilities

### New Capabilities

None. This is an internal, behavior-preserving architecture refactor.

### Modified Capabilities

None. Shared remains the composition root and retains the same cross-feature ownership. The OpenSpec change declares `skip_specs: true` because it introduces no new or changed system behavior; source-file inventory changes are tracked as implementation acceptance, not a new product requirement.

## Impact

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` and focused internal Shared source files.
- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ThinSharedInventoryTest.kt` and focused Shared JVM tests.
- `docs/architecture.md`, the relevant architecture ADR, and the repository handoff/roadmap evidence.
- No Gradle modules, public feature API, dependencies, database schema, platform seams, iOS exports, or UI/product behavior.
