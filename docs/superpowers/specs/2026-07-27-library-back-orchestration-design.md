# Library Back Orchestration Design

## Summary

Deepen the existing shared `LibraryAppState` module into the single orchestration boundary for Library Back intent. The module will expose one destination-scoped surface seam and create a stable, latched Back session before any adapter performs work. The session selects exactly one transition in this order: destination-owned foremost modal dismissal, destination-owned edit exit, active-page selection cancellation, Now Playing dismissal, then route navigation.

Ordinary interaction Back, system Back, and predictive Back will all adapt the same session semantics. A session never retargets or falls through: it remains in flight until authoritative state proves its target settled or explicitly rejects it. At the root with no eligible transition, the module reports an unhandled intent and the invoking adapter retains its platform or interaction default. Deletion-driven loss of the displayed playlist is separate exact-destination invalidation, not Back.

## Context and problem

`LibraryAppState` already owns shared navigation, selected-track, Now Playing, and bottom-bar state. Back policy is currently split across `LibraryNavigation.kt`, `PlaylistBackRegistrationState`, `PlaylistBackDispatchController`, callback factories, and `LibraryAppShell`. The current registration seam stores one modal and one edit callback independently of a stable active destination; completion can re-evaluate policy; and playlist deletion currently has a direct clear-selection/pop completion path.

That shallow ownership makes the behavior difficult to reason about under recomposition, retained/outgoing destination composition, repeated Back input, and predictive gestures. In particular, an adapter must not select one target at gesture start and then let completion resolve a different target. A hidden, stale, or outgoing surface must not consume Back merely because it is still registered.

The terminology in `CONTEXT.md` is normative: a Back intent is distinct from displayed playlist invalidation; active destination means the presented navigation target only; a predictive target is stable for the entire gesture; and a Back transition remains in flight until authoritative settlement or explicit rejection.

## Goals

- Deepen the existing `LibraryAppState` module rather than create another global Back coordinator.
- Establish a narrow, destination-scoped interface seam so a feature-owned active surface can offer its foremost modal dismissal or edit exit without exposing a global modal stack.
- Give every destination and every registered target stable identities with explicit lifetime and ownership.
- Resolve one Back intent to one latched, cancellable, completable session and one transition only.
- Enforce precedence: foremost modal, edit exit, active-page selection, Now Playing, route navigation.
- Make ordinary, system, and predictive adapters semantic peers over the same module interface.
- Provide predictive route preview from the latched route session only.
- Suppress repeated Back while a transition is awaiting authoritative settlement, while permitting a new request after explicit rejection.
- Keep displayed playlist deletion as an exact-destination invalidation that preserves unrelated Library state.
- Make the module interface—not composable callback timing—the primary common-test surface.

## Non-goals

- No visual, layout, animation, or navigation-product redesign.
- No platform-native navigation migration or new platform dependency.
- No global registry of all composed dialogs, pages, or destinations.
- No adapter-specific Back semantics, completion-time target lookup, retargeting, or fall-through dismissal.
- No change to playback, scanning, persistence schema, playlist repository semantics, or adaptive routing policy.
- No claim that a playlist mutation completion is a Back intent.

## Module, interface, and ownership

`LibraryAppState` becomes the shared module that owns Back orchestration depth. It owns:

- the active `LibraryDestinationId` derived from the current navigation target and its destination instance;
- a destination-scoped `LibraryBackSurfacePort` registration for that active destination only;
- page-scoped selection identity and cancellation eligibility;
- current Now Playing visibility and route navigation eligibility;
- the current `LibraryBackSession?`, including its latched target, lifecycle, and route preview state; and
- exact-destination invalidation for a displayed deleted playlist.

The port is a feature seam, not a second coordinator. A route surface owns the ordering of its own confirmations and other foremost modal experiences, and publishes at most one dismissible foremost action plus its edit-exit action. The shell and adapters do not inspect feature modal internals.

Illustrative signatures only:

```kotlin
interface LibraryBackSurfacePort {
    val destinationId: LibraryDestinationId
    fun foremostBackTarget(): LibrarySurfaceBackTarget?
}

interface LibraryBackSession {
    val target: LibraryBackTarget
    val routePreview: LibraryNavigationStack?
    fun cancel()
    fun complete()
    fun reject()
}

fun LibraryAppState.beginBack(): LibraryBackBeginResult
```

`LibraryBackSurfacePort` is registered and unregistered by the feature-owned destination surface with its stable `LibraryDestinationId`. Registration is accepted only when that identity equals the module's active destination. Disposal is identity-checked, so an old composition cannot clear a newer surface registration. The module owns all cross-surface ordering and latching; feature surfaces own executing and authoritatively reporting their specific modal/edit state change.

The route shell owns the active-destination feed into `LibraryAppState`. It must update it from the currently presented navigation target, not from every composed route. Retained, hidden, stale, and outgoing destinations have no Back eligibility and no registered port leverage.

## State and identity lifecycle

### Stable identities

`LibraryDestinationId` identifies a particular destination instance, not merely a route class. It combines a stable route identity with an instance token so replacement, adaptive detail substitution, and returning to an equivalent route cannot accidentally reuse a stale feature registration. A `LibraryBackTargetId` identifies the selected target within that destination or within module-owned state. Modal and edit targets must use stable feature-owned identities rather than callback object identity alone.

Selection identity remains page-scoped. It is eligible only when its page identity equals the active destination's eligible page. Selection retained for another route is reconciled as stale state and cannot consume Back.

### Session lifecycle

`beginBack()` is the sole decision point. If no session is in flight, it snapshots the active destination and resolves exactly one `LibraryBackTarget` in precedence order. It returns either:

- an unhandled result, with no Library state mutation; or
- a newly latched session with a stable target identity and, for a route target, a precomputed route preview.

The session starts pending. `cancel()` ends a predictive gesture without transition. `complete()` asks only the latched target to execute; it does not recompute precedence, choose a new target, or use a second target if the first fails. The target remains pending until authoritative state shows its exact target is no longer foremost/active, or until the target explicitly reports rejection. Settlement clears the session. Rejection clears the session without fall-through, so a later distinct Back intent may be resolved from fresh authoritative state.

While pending, `beginBack()` returns a suppressed result. This includes repeated ordinary Back, system callbacks, and a second predictive-start attempt. Suppression ends only on settlement or explicit rejection, not merely because an event callback returned.

## Resolution order

For the active destination only, `beginBack()` resolves one target in this fixed order:

1. The active destination port's foremost modal dismissal.
2. The active destination port's edit-mode exit.
3. Cancellation of selection whose page identity is eligible for that same active destination.
4. Hiding expanded Now Playing.
5. Popping the current Library route when the navigation stack can pop.
6. Unhandled at the Library root.

Only the selected target is invoked. Modal completion does not exit edit mode; edit completion does not clear selection; selection completion does not hide Now Playing; and no non-route completion pops a route. This preserves locality: each target changes only the state it owns, while the module centrally controls selection and in-flight depth.

## Adapter behavior

### Ordinary interaction adapter

An in-app Back affordance calls `beginBack()`. For a handled session it calls `complete()` once; subsequent interaction attempts are suppressed until settlement or rejection. For an unhandled result, the interaction adapter applies its existing local default, if any. It must not synthesize a Library no-op as handled.

### System Back adapter

The system adapter calls the same `beginBack()` and completes the returned session. It has no parallel policy and does not directly invoke route pop, selection clear, Now Playing hide, modal callbacks, or edit callbacks. If `beginBack()` is unhandled, the adapter delegates to the platform's normal default behavior.

### Predictive Back adapter

At gesture start, the predictive adapter calls `beginBack()` once and retains that session for the gesture. Its progress and preview derive from that session alone. A route target exposes its precomputed `routePreview` for predictive navigation presentation; non-route targets do not acquire a route preview or a different route action.

Gesture cancellation calls `cancel()` and performs no transition. Gesture completion calls `complete()` on the same session. Completion performs no action if the exact latched target is no longer foremost and valid; that is settlement/no-op for the session, not an opportunity to retarget to a lower-precedence target. A newly started gesture after settlement or rejection begins a new session from current authoritative state.

## Displayed playlist invalidation

Deleting the playlist displayed by `LibraryRoute.PlaylistDetail(playlistId)` is not resolved through `beginBack()` and never consumes a Back session. Once the authoritative playlist snapshot confirms that exact playlist no longer exists, `LibraryAppState` performs exact-destination invalidation:

- only if the active destination is the matching displayed playlist-detail instance;
- discard state owned by that destination, including its port registration, edit/modal ownership, and selection whose page identity belongs to it;
- replace or return that exact destination to the Playlist hub using the established route behavior; and
- retain unrelated Library state, including navigation outside that exact destination, other page selection state, Now Playing state, and unrelated feature registrations.

If deletion is confirmed while a session targets that exact destination, the session settles because its latched target is no longer active/valid. It does not complete a different Back transition. Deleting a non-displayed playlist must not alter the active destination, unrelated selection, or any other feature surface.

## Testing strategy

The `LibraryAppState` Back interface is the test surface. Common unit tests should construct the module with fake destination ports and explicit authoritative-state acknowledgements; they should not depend on Compose disposal order or platform Back callbacks.

Required contract coverage includes:

- one-session precedence for modal, edit, selection, Now Playing, route, and root-unhandled states;
- registration locality: inactive, hidden, stale, outgoing, and stale-disposer surfaces cannot become or clear the active target;
- stable target identities across replacement and re-registration;
- pending-session suppression, explicit rejection, and authoritative settlement;
- no retargeting and no fall-through when a latched target becomes invalid or rejects;
- route preview derived at predictive start and unchanged through progress, cancellation, and completion;
- semantic equivalence of ordinary, system, and predictive adapters over a fake module/session;
- unhandled root delegation remaining adapter-owned; and
- selection consuming Back only for its active page identity.

Add a focused deletion test that starts with a displayed playlist detail plus unrelated Library state, confirms deletion of that exact playlist, and proves only the displayed destination's owned state is invalidated. A companion assertion should prove deletion of another playlist does not navigate, clear unrelated state, or consume a Back target.

Existing JVM Compose semantics tests may verify the thin surface-port wiring after the pure/module contract tests are in place. Platform adapter tests should stay narrow and demonstrate only that each adapter invokes the common session protocol.

## Incremental migration

1. Introduce destination, target, and session identities plus pure session-resolution tests adjacent to `LibraryAppState` without changing adapters.
2. Move current selection, Now Playing, and route Back decisions behind `LibraryAppState.beginBack()`; retain current behavior through a compatibility adapter seam.
3. Replace `PlaylistBackRegistrationState`'s global-looking modal/edit callback storage with active-destination `LibraryBackSurfacePort` wiring, preserving feature-local modal ordering.
4. Migrate ordinary interaction and system adapters to the shared begin/complete protocol; remove direct Back decision and direct-pop paths only after equivalent tests pass.
5. Migrate predictive handling so preview and completion share the one latched session; remove completion-time policy evaluation.
6. Separate playlist deletion completion from Back callbacks and implement exact-destination invalidation with the focused deletion regression.
7. Delete superseded callback factories and registrations once all adapters use the module interface, then run focused common/JVM tests and the supported platform build matrix.

Each slice preserves compileability and tests one seam at a time. The work deepens an existing module for leverage and locality instead of broadening the shell or adding a global event bus.

## Risks and mitigations

- **Authoritative settlement may arrive asynchronously.** Keep the session pending until an explicit acknowledgement or observed state change for the exact target; never treat callback return as completion.
- **Compose retention can leave outgoing surfaces registered.** Gate all ports by active destination identity and make disposal identity-checked.
- **Route replacement can resemble the same route.** Use destination instance tokens rather than route equality alone.
- **Predictive adapters can accidentally recompute at completion.** Make the session object the only adapter-held gesture state and expose preview from it.
- **Migration can duplicate policy temporarily.** Use one compatibility seam and delete it promptly after adapter migration; tests must show one resolution source.
- **Playlist deletion can over-clear state.** Make invalidation exact-destination scoped and test preserved unrelated state explicitly.

## Acceptance criteria

- `LibraryAppState` is the named shared module owning Library Back resolution, in-flight state, and exact-destination invalidation.
- Feature-owned modal dismissal and edit exit enter through a destination-scoped surface port tied to a stable active destination identity.
- Every Back request creates at most one latched session; it selects exactly one transition in modal → edit → active-page selection → Now Playing → route order.
- A pending session suppresses repeated Back until authoritative settlement or explicit rejection.
- Sessions do not retarget, recompute precedence at completion, or fall through to another transition.
- Ordinary, system, and predictive adapters use the same session semantics; predictive route preview is generated from the latched session.
- Predictive cancellation is transition-free, and completion does nothing when its exact latched target is no longer foremost and valid.
- Root-unhandled Back is reported to the invoking adapter, which keeps its default behavior.
- Displayed-playlist deletion is a separate exact-destination invalidation and preserves unrelated Library state.
- Common tests use the module/interface seam as the primary contract surface, including the focused displayed-playlist deletion case.
- No new dependencies, schema changes, platform product behavior, or visual redesign are introduced.
