# Shared App Orchestration Decomposition Design

> **Status:** Design approved by the user on 2026-11-12. The executable plan is approved; implementation is proceeding in the isolated Orca worktree.

## Intent

Reduce the maintenance risk of the 2,362-line Shared `App.kt`, whose `App()` body currently spans approximately lines 180–1338, without changing the app-wide ownership boundary or user-visible behavior.

## Current state

`App.kt` composes the app and also wires authoritative library publication, scan/source operations, playlist document flows, preferences/theme, onboarding, platform permissions, desktop drop, and the `LibraryHomeScreen` callback surface. Existing owners already provide the necessary invariants: `AppProcessState` for process startup/history, `AppLibraryOrchestrator` and `AppLibraryOperationCoordinator` for operation admission/tokens, `AuthoritativeLibraryPublicationOwner` for ordered revisions, `LibraryAppState` for route/Back, `PlaylistStateOwner` for playlist state, and preference owners for persisted settings.

The approved July architecture-refactor plan completed its listed tasks. This is a new follow-up to the current source, not a reopening or falsification of that historical status. Shared remains the sole composition root; feature modules keep scanner, repository, playlist, and leaf UI ownership.

## Approaches considered

| Approach | Trade-off | Decision |
| --- | --- | --- |
| Keep everything in `App.kt` and add local helper functions | Lowest churn, but leaves the same concentration and large composable body. | Rejected. |
| Split cohesive orchestration/composition groups into internal Shared files and reuse existing owners | Preserves module and behavior boundaries; requires careful Compose lifetime and source-inventory updates. | **Selected.** |
| Move more policy into feature modules or add an app-composition module | More cross-module contracts and risk of duplicating Shared scan/Back/publication policy. | Rejected for this scope. |

## Design

- Keep `App()` and the stable iOS facade in Shared. `App.kt` becomes the dependency and composition handoff rather than the implementation site for every workflow.
- Extract app-owned library publication and operation adapters into small Shared internal files. The existing publication owner, operation coordinator, and orchestrator remain the sole authorities; extracted code delegates rather than cloning their state machines.
- Isolate playlist document/backup hosting and Shared theme/onboarding host composition only where that gives a cohesive owner. Do not split files solely to reduce line count.
- Keep route/Back state in `LibraryAppState`, feature state in the owning feature, platform access behind the existing seams, and preference persistence behind existing owners.
- Preserve `remember` lifetimes, coroutine scopes, `LaunchedEffect` keys, cancellation identity, publication order, mutation admission, callback behavior, resource ownership, and all external signatures.
- Keep new code internal to `:shared`; introduce no new Gradle module, dependency, generic event/presenter framework, service locator, or compatibility layer.
- Update the exact Shared inventory test and canonical architecture text only after the final source responsibilities are known. First run the current inventory test as a baseline; an existing failure must be understood rather than masked by an expanded allow-list.

## Acceptance

1. `App()` is a small, readable composition/dependency handoff; workflow bodies live in cohesive files with one clear owner each.
2. Existing app/feature/platform owners remain authoritative; no duplicated revision, scan, Back, preference, or playlist state owner is introduced.
3. Existing observable route, state, scan, mutation, playlist-document, permission, and preference behavior is preserved.
4. The exact Shared source inventory and architecture docs match the resulting owned files.
5. Tests prove user-visible behavior and operation invariants; there are no line-count-only tests.
6. Supported JVM, desktop, Android, iOS, architecture, quality, and strict OpenSpec checks pass before implementation closeout. User-only device/audio evidence remains separate.

## Approval record

The user approved this design and the executable plan on 2026-11-12. Implementation is proceeding in the isolated Orca worktree under the approved `executing-plans` workflow; each task remains gated by its baseline and focused verification.
