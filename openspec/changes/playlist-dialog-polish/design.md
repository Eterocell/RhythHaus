## Context

RhythHaus shared Compose UI contains multiple independently implemented modal shells. Clear Library and Remove Folder use a liquid-glass panel, while playlist flows use a separate `ModalCard`; each also owns its own scrim, panel sizing, and action metrics. The playlist hub's frame applies safe-content padding on top of page padding, unlike Library home, and tab/action colors are partly inherited from Miuix defaults.

The change is shared-first and must render consistently on Android, iOS, and desktop JVM/macOS. No state reducer, mutation callback, navigation route, string resource, dependency, or palette value changes are required.

## Goals / Non-Goals

**Goals:**

- Establish one opaque, accessible, bounded dialog shell for every existing modal.
- Preserve every modal's current behavior and localized copy while eliminating duplicated shell logic.
- Align playlist hub spacing with Library home and make its labels legible without text clipping in both themes.

**Non-Goals:**

- Redesigning the application palette, adding a new Material/Miuix dependency, or changing dialog-triggering actions.
- Migrating future dialogs that do not exist at implementation time.
- Changing underlying library, playlist, queue, or playback semantics.

The only permitted dependency change is a JVM-test-only Compose UI-test harness required to invoke and verify the existing `SemanticsActions.Dismiss` contract. It must not alter production, Android, or iOS dependencies.

## Decisions

### Use one slot-based common-main HausDialog

`HausDialog` will own the scrim, semantics, outer inset, solid panel, touch containment, maximum height, and scrollable body boundary. Callers provide content slots and continue to own their title text, validation/failure UI, selection list, and actions.

This is selected over a fully parameterized confirmation API because existing dialogs have heterogeneous bodies and action sets. It is selected over retaining per-feature wrappers because shared surface/semantics behavior is the actual design-system contract.

### Use a solid panel and remove liquid-glass inputs

The panel color will be `HausColors.current.panel` with no alpha and no `rhythHausLiquidGlass` modifier or `LayerBackdrop` parameter. The Clear Library layout remains the visual base: centered panel, title/body hierarchy, and right-aligned or context-appropriate action row.

Keeping liquid glass would preserve the transparency conflict and make output backdrop-dependent. Adding a new palette token is unnecessary because the existing panel already supplies a theme-aware opaque surface.

### Derive the scrim from the active palette

Light theme continues to use a restrained ink dim. Dark theme derives a translucent light scrim from the active paper/ink palette so it reads as a light overlay rather than a black veil. The mapping is exposed as a pure presentation policy for deterministic tests.

Using one fixed neutral color would be less responsive to the active theme; changing global palette values would affect unrelated surfaces.

### Share only presentation policy for playlist layout and controls

The playlist frame will consume a pure layout policy matching Library home's 20dp horizontal inset and system-top-only content padding. Tabs and compact actions will consume pure colors/metrics that explicitly set Miuix foreground/background colors, stable line height, and adequate internal vertical padding.

Copying Library home directly would incorrectly couple the playlist back affordance and list content; retaining Miuix defaults would leave contrast undefined.

## Risks / Trade-offs

- [Migrating all dialogs changes a broad visual surface] → Preserve each caller's body/callback code and add focused policy tests before the full compile matrix.
- [Long localized content may exceed compact window height] → Bound the panel height and make the content region scrollable.
- [Scrim tap propagation can accidentally dismiss a dialog when interacting with its panel] → Consume panel pointer input and test the shared dismissal boundary through the same semantics contract.
- [Miuix text metrics vary by platform] → Use explicit action height, line height, and inside margins; include compact CJK/descender visual QA.

## Migration Plan

1. Add the shared dialog and pure presentation policies with tests.
2. Migrate settings dialogs, removing unused backdrop wiring only from dialog paths.
3. Migrate playlist dialogs and apply playlist frame/tab/action policy.
4. Run focused and platform verification, then manual visual QA where runtime access exists.
5. Add the JVM-only semantics-action regression using Compose `ui-test` and the desktop test host if executable dismiss coverage is otherwise absent.

Rollback is source-only: callers can be restored to their prior local shells without schema, data, or dependency migration.

## Open Questions

None. The user explicitly chose migration of all current dialogs, and the existing palette supplies the required solid-panel and theme-derived scrim colors.
