# Proposal

## Why

Sources can become inaccessible because files move, disks disconnect, or platform grants are revoked, but recovery is split across platform-specific states and existing rescan/remove-missing controls. Users need a safe, explicit way to regain a source without losing source identity, tracks, favorites, history, or metadata corrections; absent media must remain non-destructive until the user confirms removal.

## What Changes

- Provide a coherent Settings recovery surface for inaccessible sources, distinguishing recoverable Android permission state, missing/unavailable folder handles, and missing files reported by completed scans.
- Allow users to rebind a lost folder source to a newly selected folder while preserving stable source identity and all existing library records; define duplicate/conflict handling explicitly.
- Keep source-specific platform capabilities intact: Android SAF tree grants, macOS folder references, iOS managed `Documents/RhythHaus`, and the stable Android MediaStore source.
- Ensure recovery, cancellation, and failed scans never remove tracks or user state; removal remains an explicit, separately confirmed operation after authoritative completed scan evidence.
- Update localized Settings copy and regression coverage for access-loss, rebind, cancellation, conflict, and recovery transitions.

## Capabilities

### New Capabilities
- `source-lifecycle-recovery`: User-visible, non-destructive recovery for inaccessible sources and missing media while preserving persisted library identity and state.

### Modified Capabilities
- `local-library-scanning`: specify rebind, lost-access, unavailable-root, and missing-media recovery behavior within existing source/scanner/coordinator contracts.

## Impact

- Library source identity, platform-access adapters, App-owned scan/mutation coordinator, Settings source management, and local-library-scanning specs.
- No media-file writes, automatic source deletion, automatic missing-track deletion, new platform support, or changes to playlist formats.
- Database schema changes only if current source identity/handle contracts cannot represent safe rebind; implementation planning must first prove whether the existing source row can be transactionally updated without migration.
