# Design

## Context

See [`proposal.md`](proposal.md) for motivation. `App.kt` currently combines the Shared composition entry, preference projections, library publication/scan workflows, playlist-document adaptation, platform picker/drop callbacks, and a large `LibraryHomeScreen` binding. Existing owners already cover process lifetime (`AppProcessState`), operation admission/tokens (`AppLibraryOrchestrator` and `AppLibraryOperationCoordinator`), route/Back state (`LibraryAppState`), feature playlist state (`PlaylistStateOwner`), and serialized preference writes (`ThemePreferenceOwner`).

The architecture boundary is not changing: Shared remains the sole application composition root and keeps cross-feature policy. `ThinSharedInventoryTest` asserts an exact Shared source set, so every source move must deliberately update the inventory and the `Thin Shared Role` record; a baseline failure must not be hidden by broadening the allow-list.

## Goals / Non-Goals

**Goals:**
- Make `App.kt` an entry/composition handoff, not the body for all Shared orchestration and screen bindings.
- Split app-owned publication, operation workflows, playlist document hosting, and theme/onboarding hosting along their existing ownership seams.
- Preserve every current public declaration, package, dependency edge, state owner, and observable behavior.
- Keep changes incremental and maintain focused behavior tests for cancellation, stale publication, destructive-operation admission, preference persistence, playlist document revisions, and route composition.

**Non-Goals:**
- No feature behavior, UX, copy, state transition, persistence, or platform behavior change.
- No feature migration, new Gradle module, public API, dependency, generic state framework, service locator, or app-wide second coordinator.
- No changes to playback, scanning semantics, database schema, platform access, iOS exports, route/Back ownership, or feature contracts.

## Decisions

1. **Keep the Shared boundary; split implementation by existing owner.** `App()` stays the public entry point. Internal Shared files host the app-level composition bridge, authoritative library publication, library scan/source-mutation workflow adapters, playlist document host, and Shared theme/onboarding gate. Candidate files are `AppLibraryPublication.kt`, `AppLibraryOperations.kt`, `AppPlaylistBackupHost.kt`, `theme/RhythHausTheme.kt`, and `AppOnboardingGate.kt`; the implementation plan must make the smallest set that leaves one clear owner per responsibility.
   - Rejected: moving feature state or repositories into Shared helpers; this would reverse documented feature ownership.
   - Rejected: moving cross-feature policy into feature modules or adding a composition module; both expand scope and weaken the established root boundary.
   - Rejected: only renaming or wrapping the current 1,100-line composable; that reduces no meaningful responsibility coupling.

2. **Reuse existing owners instead of introducing parallel state machines.** New internal adapters call `AppProcessState`, `AuthoritativeLibraryPublicationOwner`, `AppLibraryOrchestrator`, `AppLibraryOperationCoordinator`, `LibraryAppState`, `PlaylistStateOwner`, and the preference owners. They must not own copies of their revisions, operation tokens, Back state, drafts, or persisted preferences.

3. **Preserve Compose lifetime and cancellation boundaries.** Keep each `remember`, `LaunchedEffect`, coroutine scope, and cleanup under the same logical composition lifetime. Extracted composables receive explicit immutable projections and callbacks; stateful owners remain at their current authoritative scope unless a regression proves the existing lifetime is invalid. Preserve exact `CancellationException`, publication ordering, and non-cancellable cleanup behavior.

4. **Keep the physical Shared source inventory exact.** Run the existing `ThinSharedInventoryTest` before code edits. Update its approved set only with the final, specifically owned Shared facade files and update architecture documentation to match. If the current inventory test fails before edits, stop and record the exact baseline failure; do not use an allow-list change to conceal it.

5. **Use behavior-level tests, not file-size assertions.** Existing App scan/cancellation, mutation, source management, import, onboarding, preference, playlist backup, and Shared route-adapter tests remain authoritative. Each move keeps the relevant owning tests and adds a regression only where an observable state/lifetime/cancellation boundary is not already covered. No test pins line counts or merely asserts helper forwarding.

## Risks / Trade-offs

- **Compose state/effect lifetime shifts during extraction** → move one ownership group at a time; preserve keys and effect inputs; verify the existing production-boundary regressions after each slice.
- **Stale publication or scan/mutation races change** → do not reimplement token admission; retain the current coordinator and publication owners and run their existing cancellation/stale-token suites.
- **The source inventory or architecture docs drift** → update the allow-list and canonical documentation in the same slice, then run the exact inventory test and architecture gate.
- **A new class merely hides the giant composable** → require responsibilities to map to the existing owner list and review the final call graph, state ownership, and file contents rather than declaring success from LOC reduction alone.
- **The wide route/feature callback surface gains accidental churn** → preserve the current entry contracts and route adapters; use named, cohesive internal values only where they reduce repeated parameter clumps without hiding policy.

## Migration Plan

1. Record baseline results for `ThinSharedInventoryTest` and the focused Shared tests selected by the approved executable plan.
2. Extract one coherent group at a time, keeping a compiling Shared module after each task.
3. After each move, run the smallest test that proves the behavior boundary; retain Shared as the only composition root.
4. Update `ThinSharedInventoryTest`, `docs/architecture.md`, and the relevant ADR after the final owned source set is known.
5. Run the approved final JVM, desktop, Android, iOS, architecture, quality, and strict OpenSpec checks. User-only device/audio acceptance is not implied by this presentation/orchestration refactor.

Rollback is a source-level revert of the individual extraction slice; no data or schema migration is involved.

## Open Questions

None for the approved scope. Exact task boundaries and signatures belong in the implementation plan after design approval.
