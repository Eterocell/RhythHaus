## Context

See `proposal.md` for motivation. RhythHaus currently keeps `LibraryNavigationStack` and `LibraryAppState` as the application presentation authority. Shared resolves Back precedence across feature modal/edit targets, selection, expanded Now Playing, and routes; feature implementations publish destination-scoped dismissal ports. The current navigation renderer and platform bridges therefore depend on Shared’s custom Back session/latching/settlement behavior.

The researched `miuix-nav:0.9.4-rc01` runtime renders a `NavBackStack<NavKey>` through `NavDisplay`, supports serializable keys/content identity, continuous stack depth, built-in predictive Back, and opt-in directional swipe dismissal. Its `NavDisplay` invokes the supplied `onBack` only at commit; the caller remains responsible for mutating the supplied stack.

## Goals / Non-Goals

**Goals:**

- Make one serializable app-wide Miuix navigation stack the canonical presentation state.
- Make Miuix the sole owner of platform Back, predictive Back, and approved swipe-to-dismiss gestures.
- Represent every visible dismissible surface in stack order so Back precedence requires no separate arbiter.
- Preserve domain ownership, feature module direction, route identity, entry state restoration, compact/wide composition, invalid-destination handling, and the stable iOS facade.
- Replace custom Shared Back machinery only after characterization and migration regressions prove parity.

**Non-Goals:**

- The Library feature implementation will not own app-wide navigation; the Shared composition root remains the host of the canonical stack.
- No feature imports Miuix navigation types or receives a Miuix back-stack object.
- No database, scanner, playback-engine, or domain-model ownership migration.
- No visual redesign beyond transition choices required to reproduce existing route, modal, dialog, editor, selection, and Now Playing presentation.
- No compatibility mirror in which `LibraryNavigationStack` and `NavBackStack` can mutate independently.

## Decisions

### Canonical stack and typed keys

Define a Shared-owned `@Serializable` `AppNavKey : NavKey` hierarchy covering all base routes and all dismissible presentation layers. Use strongly typed keys for route payloads, modal/editor/dialog kinds, selection page identity, and Now Playing. Every re-presentation receives a monotonic appearance token. The Miuix `contentKey` for each entry includes route identity plus appearance token, preserving exact predecessor state while preventing equal-route replacement from reusing the outgoing entry’s state.

The stack owns presentation presence. Feature-owned domain state remains authoritative for playlist contents/edit data, selected track IDs, playback state, scans, and library data. Visibility is derived from the top stack key; feature state must not independently introduce a dismissible surface.

### Shared-level Miuix host

Add `miuix-nav:0.9.4-rc01` to the Shared implementation dependency set. Compose one root `NavDisplay` in Shared with an app-specific typed navigator wrapper. The wrapper is the only navigation-operation API exposed to Shared composition; it does not duplicate the Miuix stack. `NavDisplay(onBack = { backStack.removeLastOrNull() })` is the only platform/predictive Back mutation path.

Register each concrete key type with Miuix’s entry provider. Feature callbacks are adapted by Shared into typed push/pop/replace operations. Feature implementations remain callback-first and have no dependency on `miuix-nav`.

### Presentation entry lifecycle

Base routes use the default horizontal route transition unless characterization requires a RhythHaus-owned equivalent. Modal/dialog entries use the Miuix modal transition. Selection and editor entries use explicit transition metadata and disable swipe dismissal unless a product decision enables it. Expanded Now Playing is represented as an overlay entry and is popped before its underlying route.

Entry lifecycle/disposal and stack observation clear ephemeral feature state after a key disappears. Domain invalidation may explicitly pop or replace a stale route, but Back itself only removes the canonical top key.

### Platform and gesture ownership

Do not install a second Shared platform Back handler. Miuix owns predictive progress, cancellation, commit classification, animated depth, and settle animations. Configure physical swipe directions from layout direction for base routes; opt into downward modal dismissal only where specified. Root and protected overlay entries have swipe dismissal disabled.

### Restoration and failure behavior

All keys must satisfy Miuix serialization requirements and restore across process death/configuration changes. A missing or malformed key/provider registration is a fail-fast configuration error, not a fallback to the old route stack. A route invalidated by domain state is removed/replaced through an explicit Shared navigation command.

### Clean cutover

After the new stack and tests are complete, delete the old Shared route stack as presentation authority, Back resolution/session/settlement machinery, feature dismissal-registration ports, and custom edge-swipe owner. Do not retain a compatibility mirror or a second fallback Back path.

## Risks / Trade-offs

- This is a breaking presentation-state migration across Shared, feature contracts, platform bridges, and iOS-facing adapters.
- Miuix commit happens after its predictive presentation has begun; moving all dismissible surfaces into the canonical stack is required to eliminate veto-after-pop races.
- Key serialization and content identity become an ABI-like compatibility surface for restored entry state.
- Miuix transition defaults may not match current compact/wide visuals; characterization tests and desktop/iOS visual checks are acceptance gates.
- Removing Shared settlement means domain cleanup must be lifecycle/stack-observation-driven and cannot rely on a Back callback veto.

## Open Questions

None for the approved architecture. Any implementation discovery that would require a second Back authority, a feature-to-Miuix dependency, or a mutable compatibility mirror is a hard stop and requires a design revision.
