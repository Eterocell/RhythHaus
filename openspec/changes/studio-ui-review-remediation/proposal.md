# Proposal

## Why

Static review found behavior and contract gaps in the completed Studio UI redesign, including queue-save draft loss on track replacement, missing row actions, undersized maintenance actions, and stale architecture/resource ledgers. A focused follow-up will close those gaps without reopening postponed manual GUI/device acceptance or broadening into unrelated refactors.

## What Changes

- Preserve the active Queue surface and its immutable save draft/confirmation state when playback changes tracks; retain the existing current-plus-upcoming snapshot, duplicate occurrence, cancel, failure, Back, and retry semantics.
- Provide a named secondary row-action menu and non-gesture selection entry on Library and Search rows; expose Search metadata editing through the existing app-local override flow; distinguish the row's accessible identity/primary action from its selection action.
- Bring destructive maintenance action targets up to the already-approved mobile and iOS minimums.
- Pass the window-overlay publisher through the explicit Shared-to-Queue entry point and use one root-owned density projection instead of feature-local thresholds and redundant row parameters.
- Remove the production-dead queue mutation helper and unread layout-policy field, placing behavioral regression coverage at the real Shared/feature interaction boundary.
- Update the binding Library UI API/resource ledgers and Settings resource-key ledger to match the accepted redesign; do not add compatibility shims or change visible localized copy solely to preserve old key names.
- Keep the four broad P3 cleanup refactors out of scope. Keep manual acceptance items in `studio-ui-redesign` postponed/unverified.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `saved-playlists` — preserve the captured queue-save draft and dialog through current-track replacement.
- `track-multi-selection` — provide explicit accessible/non-gesture selection and a discoverable desktop/menu entry.
- `track-metadata-overrides` — make metadata editing reachable from Search through the named row menu.
- `feature-first-modular-architecture` — supersede the old Library UI signature with the redesigned exact signature and make the queue overlay publisher an explicit Shared-to-feature entry-point contract.

## Non-Goals

- No playback, scanning, persistence schema, queue transaction, metadata storage, navigation-stack, or Back-precedence redesign.
- No implementation of the deferred P3 navigation/BrowseMode deduplication, queue row-policy deduplication, or `HausSongRow` parameter-object refactor.
- No GUI/device/audio acceptance claim and no change to the postponed manual acceptance status.
