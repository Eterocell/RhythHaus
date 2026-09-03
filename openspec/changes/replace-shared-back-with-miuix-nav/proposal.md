## Why

RhythHaus’s current Shared Back arbiter resolves modal, edit, selection, Now Playing, and route precedence with a custom predictive-Back state machine. `miuix-nav:0.9.4-rc01` now provides the continuous-depth navigation, predictive Back, popup-animation interruption, and swipe-to-dismiss behavior needed to make one canonical navigation stack own those presentation layers instead of maintaining a second gesture/settlement engine.

## What Changes

- **BREAKING** Replace `LibraryNavigationStack` as the canonical presentation stack with a serializable Shared `NavBackStack<AppNavKey>` rendered by `miuix-nav`.
- **BREAKING** Make Miuix the sole application Back and predictive-Back authority; Shared `onBack` only removes the canonical top key.
- Represent base routes, dialogs, modals, editors, selection mode, and expanded Now Playing as typed navigation entries with unique appearance identity.
- Replace Shared Back precedence, predictive latching/settlement, feature dismissal registration, and custom edge-swipe ownership with Miuix stack order, predictive Back, and opt-in swipe dismissal.
- Keep feature implementations Miuix-independent through callback-first navigation contracts.
- Preserve feature-owned domain state while deriving presentation visibility from the canonical navigation stack.
- Preserve route predecessor state, equal-route replacement identity, invalid-destination handling, compact/wide rendering, and the stable iOS facade.
- Add characterization and migration regressions for every current Back target, predictive cancellation/commit, swipe ownership, state restoration, and platform behavior.
- Update architecture and handoff documentation with the intentional replacement of Shared Back ownership.

## Capabilities

### New Capabilities

- `miuix-owned-app-back-navigation`: Canonical app-wide navigation stack, overlay presentation entries, Miuix-owned Back/predictive Back, swipe policy, and restoration behavior.

### Modified Capabilities

- `feature-first-modular-architecture`: Shared no longer owns the Back arbiter; Shared remains the app-shell composition root and owns the typed navigation stack/controller while feature implementations remain callback-first and Miuix-independent.

## Impact

- Affects `:shared`, Library and Playlist feature contracts/implementations, Settings/Now Playing composition, platform Back bridges, iOS framework-facing adapters, and navigation tests.
- Adds the already researched `top.yukonga.miuix.kmp:miuix-nav:0.9.4-rc01` dependency; no `androidx.navigation3` dependency is introduced.
- Deletes the custom Shared Back state machine and feature dismissal ports after the canonical-stack migration is complete.
- Requires a full platform verification pass; this change must stop if Miuix serialization, iOS dispatch, or exact route/overlay identity cannot be preserved.
