# Design

## Context

See `proposal.md` for motivation. `LibrarySource` already persists stable `id`, platform kind, handle, access status, and creation/scan timestamps. `PlatformSourceAccess.accessStatus` classifies host access; Android MediaStore and dropped-file sources already have specialized recovery paths. `LibraryScanner` records failed scans separately from completed scan summaries, and repository remove-missing accepts only a source's latest authoritative completed scan. The App coordinator serializes scans and destructive source/library mutations.

## Goals / Non-Goals

**Goals:**
- Add an explicit recovery action appropriate to each persisted source kind.
- Rebind eligible folder source handles while retaining source IDs and owned track relationships.
- Publish access/rebind/recovery outcomes through existing Shared state and operation coordination.
- Keep a failed or inaccessible scan from authorizing missing-track deletion.

**Non-Goals:**
- Automatic file move detection, content hashing, folder merging, or duplicate-track consolidation.
- Rebinding Android MediaStore or the fixed iOS managed source to an arbitrary folder.
- Deleting tracks or repairing playback records automatically.
- Replacing the existing persisted scan report/remove-missing workflow.

## Decisions

### Preserve source identity and update only folder handles

Use `LibraryRepository.upsertSource(existing.copy(handle = newHandle, displayName = newDisplayName, accessStatus = Available, lastScanAtEpochMillis = null))` only after the platform adapter confirms a readable selection and the Shared operation owner admits the mutation. `id`, `platformKind`, `createdAtEpochMillis`, tracks, playlist references, and user-state relationships remain unchanged. The existing SQLDelight source upsert persists the new handle; no schema migration is needed. Rebind refuses a handle that is already assigned to another source. The check and source update occur in one coordinator-owned operation, and Shared publishes the refreshed library only after accepted persistence.

### Keep platform recovery explicit and platform-specific

- Android SAF: invoke the existing picker with the target source identity; persist the new tree grant before updating the row. Cancellation or grant persistence failure leaves the old row untouched. Android MediaStore retains its existing permission recovery; permission restoration does not add or scan.
- macOS folder: select a replacement directory, validate readability, then rebind the existing source ID.
- iOS `ios-app-local`: it is the fixed managed `Documents/RhythHaus` source, not an arbitrary external folder. Recovery ensures/rechecks that managed directory and offers a scan; Files.app import remains the way to add external content.
- Desktop dropped-file sets retain their existing original-set rescan recovery. Do not reinterpret them as folder sources.

### Keep missing-media evidence separate from source access failure

An inaccessible root must produce a failed scan, not a completed empty scan. Rebind triggers no automatic remove-missing. Once the user explicitly scans an accessible replacement and receives a completed summary, the existing explicit remove-missing confirmation remains the only deletion path. Source-local keys continue to match existing tracks where paths are the same; no fuzzy filename matching is introduced.

### Reuse Settings source management and App operation owner

Add recovery affordances to the existing source row/recovery UI. Picker outcomes carry the source being rebound so the Settings route can report cancellation, conflict, grant failure, or success without a parallel navigation or scan system. The App-owned coordinator owns source update admission and publication; recovery cannot race with scan/remove/clear operations. Existing MediaStore permission recovery and dropped-file recovery remain distinct actions.

## Risks / Trade-offs

- A replacement directory may contain different files at matching relative paths. Preserving source-local identity can therefore update existing rows as normal scans do; the UI must clearly state that rebind changes the folder backing that source and must not imply automatic merging.
- Android may grant a tree URI before repository admission fails. On rejection, release only the newly acquired grant if no existing source owns it; never release the old source's grant.
- iOS cannot recover an externally removed app sandbox container as a user-selected folder; only the managed directory can be recreated, while platform/container loss remains an explicit unrecoverable state rather than silently importing elsewhere.
- Folder permission and filesystem checks can race with later scanning; the scanner remains authoritative and a subsequent failure is displayed without deleting records.
