## Why

RhythHaus already persists source-aware tracks, but its management flow treats folder selection as a one-shot scan and does not expose configured sources. Android and desktop users need to build one library from multiple folders without replacing or clearing previously scanned folders; iOS remains limited to its single app-local source.

## What Changes

- Let Android and desktop users add multiple music folders through repeated use of the existing platform folder picker.
- Persist each distinct selected folder as an independent `LibrarySource` and keep tracks isolated by source identity.
- Show configured sources with access state and last-scan information in shared management UI.
- Add per-source rescan and removal actions; removing a source removes only that source's tracks, scan sessions, and scan errors.
- Keep iOS on the existing single app-local source and do not expose an add-another-folder action there.
- Preserve existing scanning, playback, artwork, navigation, and clear-library behavior outside source management.

## Capabilities

### New Capabilities

- `multi-library-sources`: Additive source selection, source-scoped rescanning and removal, and Android/desktop management UI with iOS exclusion.

### Modified Capabilities

None.

## Impact

- Shared library repository and SQLDelight source lifecycle queries.
- Shared app scan orchestration and source management UI/state.
- Android SAF and desktop JVM picker capability metadata; no new picker API or dependency.
- Shared/JVM repository and orchestration tests plus focused platform compilation.
- No new dependencies, no SQLDelight schema shape change, and no Windows/Linux product support.
