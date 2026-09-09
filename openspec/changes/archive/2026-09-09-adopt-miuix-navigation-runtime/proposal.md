## Why

Miuix v0.9.4-rc01 introduces `miuix-nav`, the replacement for the removed Navigation3 integration. RhythHaus has no legacy Miuix Navigation3 dependency to rename, so adopting it is a deliberate migration of the Shared-owned Library navigation renderer; it must preserve the existing authoritative route model, adaptive layout, and Back settlement guarantees.

## Current Decision

Do not add `miuix-nav` or alter Library rendering on `0.9.4-rc01`. Its mandatory platform predictive-Back handler conflicts with the existing Shared arbiter before that arbiter can choose an authoritative target.

## Conditional Future Design

If upstream later exposes an explicit platform-Back opt-out/delegation API, then:

- Add the Miuix navigation runtime to `:shared`.
- Introduce an internal Miuix key projection for every presented Library navigation entry. The projection carries the existing entry instance token so equal route values cannot collide in Miuix entry state. It uses Miuix's in-memory stack because RhythHaus does not currently restore Library navigation across process death.
- Render compact Library destinations through `NavDisplay` and route-specific entries, retaining `LibraryAppState` as the only authority for route admission, replacement, invalidation, and Back settlement.
- Keep feature modals, edits, selection, Now Playing expansion, and destructive-operation dialogs outside Miuix's flat stack. `miuix-nav` v1 explicitly does not support dialog/bottom-sheet scene strategies.
- Preserve the existing wide master-detail route policy. The Miuix display is pane-clipped, never consumes the persistent master rail, and receives no duplicate system insets.
- Preserve the existing Shared Back precedence (`modal -> edit -> active-page selection -> Now Playing -> route`), target latching, cancellation, rejection, and authoritative in-flight settlement.
- Add production-boundary regressions for the Miuix key projection, duplicate route-entry identity, stack synchronization, compact and wide rendering, and every Back precedence/predictive settlement path.
- Refresh checked-in AboutLibraries Miuix metadata from `0.9.3` to the already-resolved `0.9.4-rc01` artifacts.

## Capabilities

### New Capabilities

- `shared-miuix-navigation-runtime`: Shared-owned rendering integration between the authoritative Library route stack and Miuix's navigation runtime.

### Modified Capabilities

- `feature-first-modular-architecture`: Clarify that the Miuix renderer remains a Shared implementation detail and cannot take route or Back authority from Shared.

## Impact

- Current changed scope: research and planning artifacts only; no Gradle, source, metadata, or test code changes are allowed by this proposal on `0.9.4-rc01`.
- Potential future scope, conditional on an upstream API: `shared` Gradle configuration; `LibraryAppState`, `LibraryNavigationStack`, `LibraryAppShell`, `LibraryRoutes`, Android/JVM/iOS Back adapters, and their common/JVM/Android/iOS tests.
- No user-visible behavior changes and no new runtime dependency are accepted while the no-go remains in force.
- Current disposition: no implementation against `0.9.4-rc01`. Its `NavDisplay` source always installs `PredictiveBackHandlerWithSessions(enabled = backStack.size > 1)` and animates a route gesture before it invokes `onBack`; its public API has no platform-Back disablement switch. That violates Shared's required first refusal for modal, edit, selection, and Now Playing targets. This change can resume only if Miuix adds an upstream platform-Back opt-out/delegation API; it must not ship dual Back owners.
