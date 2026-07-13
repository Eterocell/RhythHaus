## Context

The local-library model is already source-aware: sources are persisted independently, tracks carry `sourceId`, scanner upserts are keyed by source plus source-local identity, and Android/JVM picker implementations generate stable source IDs from the SAF URI or canonical path. The composition root currently keeps only tracks in UI state, however, and the repository has no operation for deleting one source and its dependent records. iOS deliberately provisions one app-local source and does not provide arbitrary external folder access.

This change spans shared persistence, orchestration, platform capability metadata, and shared Compose management UI. It must preserve the existing single-active-scan model and avoid a schema-shape migration.

## Goals / Non-Goals

**Goals:**

- Add or refresh multiple independent user-selected sources on Android and desktop JVM/macOS.
- Expose persisted sources and source-scoped rescan/removal actions in shared UI.
- Remove a source and its dependent tracks/history atomically without affecting other sources.
- Make iOS exclusion explicit in platform capability rather than UI platform checks.

**Non-Goals:**

- Picker-level multi-select.
- Arbitrary iOS Files/Apple Music source access.
- Concurrent scans or scan queueing.
- Schema table/column/index changes, dependency additions, playback changes, or Windows/Linux support.

## Decisions

### Reuse stable one-folder picker results as additive source commands

Repeated picker launches add sources. Android SAF URI and desktop canonical-path source IDs already make selection idempotent: selecting an existing folder rescans the same source, while selecting another folder adds a separate source. This avoids replacing platform pickers or adding a cross-platform multi-select abstraction unsupported by Android's tree picker.

Alternative: replace the previous source on selection. Rejected because it contradicts additive multi-folder behavior and could unexpectedly remove tracks.

### Keep one active scan and generalize the existing scan entry point

Extract the existing `App()` scan-launch body into a source-accepting orchestration helper/state path used by both picker success and per-source rescan. Add, rescan, and remove controls are disabled while a scan is active. This preserves progress and cancellation behavior and avoids concurrency changes.

Alternative: scan all selected sources concurrently. Rejected because progress and cancellation currently represent one session and concurrent database/native metadata work adds unrequested complexity.

### Treat source capability as platform metadata

Extend the folder picker launcher with an `supportsAdditionalSources` capability. Android and JVM return true; iOS returns false. Shared UI uses that capability to show the add action without importing platform enums or branching on platform names.

Alternative: infer iOS from existing sources or `LibraryPlatformKind`. Rejected because UI should consume capability, not platform identity.

### Add transactional source removal without a schema migration

Add `LibraryRepository.removeSource(sourceId)` and implement it in SQLDelight with an explicit transaction that deletes scan errors associated with the source's sessions, then sessions, tracks, and finally the source. The in-memory implementation mirrors the same semantics. Existing tables and foreign-key relationships remain unchanged.

Alternative: rely on database cascades. Rejected because current schema behavior has not established cascading deletion and changing constraints would require a migration.

### Keep source management in Settings

Settings already owns “Manage Music,” the add-folder action, active scan state, and clear-library action. It will show configured source rows there, with rescan and remove actions. The empty-library home card retains its existing add action. This avoids turning the library browse screen into a management surface.

## Risks / Trade-offs

- [Source deleted while scanning] → Disable source removal and all source mutations while any scan is active.
- [Lost Android permission] → Keep the source visible and report `LostAccess`; users may remove it or restore access by selecting it again.
- [Duplicate selection] → Stable source IDs convert it to an idempotent source refresh/rescan.
- [Partial deletion] → Execute dependent deletion and source deletion in one SQLDelight transaction and test rollback-safe source isolation.
- [Existing repository fakes break] → Update all `LibraryRepository` test doubles to implement the new method and keep it narrow.

## Migration Plan

No schema migration is required. Existing source and track rows remain valid and become visible in the new management list. Rollback removes the new repository/UI capability while leaving existing persisted data unchanged.

## Open Questions

None. The user approved additive sources, independent scan/removal behavior, and iOS exclusion.
