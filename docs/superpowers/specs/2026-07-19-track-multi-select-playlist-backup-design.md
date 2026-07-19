# Track Multi-Select and Playlist Backup Design

## Summary

RhythHaus will replace per-track `Add to playlist` buttons with a page-scoped long-press multi-selection workflow on Library home song rows, album and artist drill-down rows, and Search results. While selection mode is active, track rows expose checkboxes and the bottom Now Playing bar is temporarily replaced by a contextual action bar whose first action adds every selected track to a playlist.

RhythHaus will also add cross-device playlist backup and restore through a versioned logical interchange file. The file contains playlist structure and portable track metadata rather than a copy of the live SQLDelight database. Import matches entries against the destination device's authoritative local library, creates non-destructive playlist copies, and reports unmatched or ambiguous entries.

The feature remains shared-first across Android, iOS, and desktop JVM/macOS. Common code owns selection state, backup encoding and validation, track matching, import planning, and repository mutations. Platform code owns only the system file save/open integration required to exchange the backup file.

## Goals

- Remove the right-side `Add to playlist` action from track rows on Library home, album and artist drill-down pages, and Search results.
- Enter page-scoped selection mode by long-pressing a track and select one or more visible tracks without starting playback.
- Show a checkbox at the left of every track row while selection mode is active.
- Replace the bottom Now Playing bar with a contextual selection bar while selection mode is active.
- Add all selected tracks to one existing or newly created playlist through the existing playlist picker and one ordered repository mutation.
- Add Settings actions to export all saved playlists and import a playlist backup through platform system file panels.
- Restore playlist order and duplicate occurrences by uniquely matching portable track metadata against the destination library.
- Preserve all existing local playlists and create a uniquely named imported copy when a backup contains a conflicting playlist name.
- Report created playlists, restored entries, unmatched entries, and ambiguous entries after import.

## Non-goals

- Multi-selection on saved-playlist detail, Queue, or Now Playing screens.
- Selection that persists across route or page changes.
- Additional selection actions such as play next, delete, share, or remove from library.
- Cloud synchronization, background synchronization, automatic conflict resolution, or account-based backup.
- Exporting or importing audio files, artwork, local file paths, library sources, scan history, playback queue, playback session, settings, or other application state.
- Copying, replacing, or reopening the live SQLite database as part of playlist backup or restore.
- Guessing between multiple matching destination tracks or creating playlist entries that reference unavailable tracks.
- Windows or Linux platform support.

## Current State

`TrackRow` currently has one `selected` property representing the now-playing visual state and an optional right-side `onAddToPlaylist` button. Library home and album/artist drill-down pages provide that callback. Search uses a separate result row and also supports direct playback. None of these surfaces has a shared long-press selection state or a contextual bottom action bar.

The app already supports ordered bulk playlist mutation through `PlaylistRepository.append(playlistId, trackIds)`. Playlist entries have independent occurrence IDs, so duplicate selected tracks or repeated imported entries can remain distinct and ordered. The existing Add to Playlist picker is currently modeled around one track ID and must be generalized without changing repository ownership.

The SQLDelight database is opened through different drivers on Android, iOS, and desktop JVM. A raw copy of an active SQLite main file is not a safe interchange artifact because committed state can reside in a WAL file, active drivers retain connections, and database replacement requires platform-specific lifecycle reconstruction. SQLDelight does not provide a common backup/import API. A logical playlist format avoids these live-database, WAL/SHM, schema-replacement, and device-local path risks.

## Selection Interaction Design

### Supported surfaces

Selection mode is available on:

- Library home while browsing Songs.
- Album drill-down track lists.
- Artist drill-down track lists.
- Search result track lists.

Saved-playlist detail remains unchanged because its rows already participate in playlist-specific remove and reorder workflows. Queue and Now Playing surfaces remain unchanged.

### Entry and row behavior

In normal mode, tapping a track keeps the existing visible-list playback behavior. Long-pressing a track starts a selection session scoped to the current route instance and selects that track. The long press must not also dispatch the normal playback click.

While selection mode is active:

- Every track row on the current page shows a checkbox before its artwork.
- Tapping the row or its checkbox toggles that track's selected state and does not start playback.
- Long-pressing a row has no additional effect beyond selecting an unselected row.
- The row exposes distinct accessibility semantics for selected and unselected state.
- Selection identity uses authoritative library track IDs; visual order is derived from the current page's visible track order.
- If the selected set becomes empty, selection mode exits automatically.

Now-playing state and multi-selection state remain independent. A currently playing row can also be selected, and its playback indicator must not be reused as the selection checkbox or selection color contract.

### Selection lifetime and dismissal

A selection session never crosses route boundaries. It is cleared when the user:

- activates an explicit cancel action;
- presses the applicable system or app back action;
- navigates to another Library route;
- changes away from the current page or search surface; or
- deselects the final selected track.

Changing the Search query remains within the same Search route. Selected IDs that are no longer present in the current result set are removed immediately; if none remain, selection mode exits. This prevents hidden selections from being submitted.

### Contextual bottom bar

Selection mode and the Now Playing bar are mutually exclusive on the affected route. When selection starts, the bottom area renders a contextual selection bar in the same safe-area and responsive-width boundary used by Now Playing. The bar displays the selected count, a cancel action, and one enabled action: `Add to playlist`.

Activating `Add to playlist` opens the existing picker flow generalized to an ordered list of selected track IDs. The picker continues to support choosing an existing playlist or creating one inline. Confirmation appends one occurrence per selected track in the current visible order through one `PlaylistRepository.append` or `createWithEntries` mutation.

After a successful mutation, the picker and selection mode close. If the mutation fails, the picker reports the existing recoverable mutation error and the selected IDs remain available for retry. Dismissing the picker without confirming returns to the unchanged selection session.

## Selection State and Component Boundaries

Add a pure common-main selection state model containing the owning route/page key and selected track IDs. Its reducer owns start, toggle, visible-set reconciliation, cancel, navigation dismissal, successful-completion, and ordered-confirmation behavior. Composables dispatch events and render state; they do not assemble repository mutations directly.

Track-row presentation receives separate inputs for:

- whether the track is currently playing;
- whether selection mode is active;
- whether this track is selected;
- normal click;
- long press; and
- selection toggle.

Library home, drill-down, and Search adapt their visible `Track` lists into this shared contract. The selection bar is one reusable common-main component rather than three screen-local implementations. The app shell owns the route-aware choice between the selection bar and Now Playing bar so both cannot be interactive or visible simultaneously.

## Logical Backup Format

### Scope and envelope

Settings adds two localized actions:

- `Export playlist backup`
- `Import playlist backup`

Export includes all saved playlists in deterministic repository order. The first backup format is canonical UTF-8 JSON, uses the `.rhythhaus-playlists.json` extension, and advertises `application/vnd.rhythhaus.playlists+json` where a platform file API accepts a media type. Its envelope contains:

- a fixed RhythHaus playlist-backup discriminator;
- an integer interchange-format version;
- an export timestamp;
- an ordered list of playlists; and
- a CRC32 integrity checksum over the canonical payload.

Each playlist contains its user-visible name and ordered entries. Each entry contains only portable matching metadata:

- title;
- artist;
- album; and
- duration in whole seconds.

Database IDs, playlist-entry IDs, track IDs, local paths, source roots, timestamps used only by the local database, and binary artwork are not exported. Duplicate occurrences are represented as duplicate ordered entries.

The first format version is `1` and is intentionally minimal. Object keys are emitted in the documented model order, arrays preserve playlist and entry order, strings use JSON escaping without presentation whitespace, and the checksum is calculated over the canonical UTF-8 payload with the checksum field omitted. CRC32 detects accidental truncation or corruption; it is not authentication and must not cause an imported file to be treated as trusted. New optional fields may be introduced only under a later explicit format-version compatibility policy. Version 1 rejects unknown fields, missing required fields, and every format version other than `1` rather than partially interpreting them.

### Serialization safety limits

The common decoder validates the complete document before creating an import plan. Version 1 accepts at most 4 MiB of UTF-8 input, 1,000 playlists, 10,000 entries in one playlist, 100,000 entries across the file, and 1,024 Unicode code points in each playlist-name or track-metadata string. Duration must be an integer from 0 through 604,800 seconds. Empty or whitespace-only playlist names, missing or unknown fields, checksum mismatch, malformed UTF-8 or JSON, duplicate object keys, non-integer numeric fields, and trailing unparsed content are rejected.

The decoder returns a typed validation result and never mutates the database. Platform file access supplies bounded bytes to the decoder; it does not deserialize unbounded streams directly into repository objects.

## Destination Track Matching

Import matches each backup entry against the current authoritative local library snapshot. It never imports source track IDs because those IDs and file paths are device-local.

The matcher normalizes title, artist, and album consistently by trimming leading and trailing Unicode whitespace, replacing each non-empty run of Unicode whitespace with one ASCII space, and applying locale-independent lowercase conversion. Version 1 does not remove punctuation, diacritics, or compatibility distinctions. Duration is compared with an inclusive tolerance of plus or minus 2 whole seconds. A candidate must match every normalized text field and fall within that duration tolerance.

Matching outcomes are explicit:

- Exactly one candidate: the entry is restorable and resolves to that local track ID.
- No candidates: the entry is unmatched and is excluded from mutation.
- More than one candidate: the entry is ambiguous and is excluded from mutation.

The importer does not choose the first candidate, use local file paths, or silently weaken the matching key. Duplicate backup occurrences that uniquely resolve to the same local track remain duplicate ordered playlist entries.

## Import Preview, Conflict Policy, and Mutation

After parsing and matching, the app presents a preview before any write. The preview reports the backup's playlist count and, per playlist and in total, restorable, unmatched, and ambiguous entry counts. A valid backup with no restorable entries can be inspected but cannot confirm a no-op import.

Import never overwrites, merges into, renames, or deletes an existing local playlist. Every imported playlist is created as a new playlist. If its name conflicts with any existing or earlier planned name, a shared deterministic naming policy appends a localized import suffix and, when required, an incrementing number until the name is unique. Name comparison for conflict detection uses the same documented normalization on every platform.

On confirmation, all planned playlists with at least one restorable entry are created in one repository transaction, preserving playlist order, entry order, and duplicate occurrences. A playlist containing only unmatched or ambiguous entries is not created and is reported as skipped. If any database operation fails, the complete import rolls back and the previously confirmed playlist snapshot remains authoritative.

After success, the app refreshes playlist state and reports:

- playlists created;
- playlists skipped because no entries matched;
- entries restored;
- entries unmatched; and
- entries ambiguous.

The source file is not modified. The user may import it again after changing the destination library; a repeated import creates new uniquely named copies rather than mutating previous imports.

## Platform File Integration

Common code defines file-operation results for success, user cancellation, unavailable platform integration, and failure. Platform implementations use the system document picker/save panel so the user controls the destination or source:

- Android uses the system document create/open contracts and persists no broad storage permission.
- iOS uses the system document export/import flow appropriate for an app-generated data file.
- Desktop JVM/macOS uses the native save/open file dialog conventions already established for platform file selection.

Cancellation is not an error and produces no failure notice. Export first builds and validates the complete bounded payload, then asks the platform layer to write it. Import reads only the selected file and passes bounded bytes to common validation. Platform code does not open, copy, checkpoint, close, replace, or migrate `rhythhaus.db`.

## Data Flow

### Multi-select add

```text
long press row
  -> page-scoped selection reducer
  -> selection bar replaces Now Playing bar
  -> Add to playlist picker
  -> ordered selected track IDs
  -> existing playlist mutation launcher
  -> PlaylistRepository append/createWithEntries transaction
  -> confirmed playlist snapshot
  -> picker and selection session close
```

### Export

```text
Settings export action
  -> confirmed playlist snapshot + authoritative track metadata
  -> logical backup model
  -> canonical encoder + checksum
  -> platform save panel
  -> success/cancel/failure notice
```

### Import

```text
Settings import action
  -> platform open panel
  -> bounded bytes
  -> decoder + format/integrity validation
  -> pure matcher against authoritative library snapshot
  -> conflict-free import plan and preview
  -> user confirmation
  -> one playlist repository transaction
  -> refreshed playlist snapshot + result report
```

## Error Handling

- A long press never triggers playback in addition to selection.
- A failed playlist append retains the current selection and last confirmed playlist state.
- Dismissing the Add to Playlist picker returns to selection mode; successful confirmation exits it.
- File-panel cancellation is silent and does not appear as a failure.
- Unavailable file integration, read failure, write failure, oversized input, malformed content, checksum failure, unsupported version, and invalid field bounds produce distinct recoverable outcomes.
- Import validation and preview perform no writes.
- A repository failure during confirmed import rolls back every playlist created by that import attempt.
- A library refresh between preview and confirmation invalidates the preview. Confirmation must revalidate against the authoritative library revision or reject the stale plan and request a new preview.
- A stale route or navigation event clears selection rather than applying selected IDs on another page.
- Export failure does not alter playlists or leave an app-managed partial database artifact.

## Accessibility and Responsive Behavior

Long press is a convenience gesture, not the only usable selection operation. Each eligible track row exposes an accessibility custom action to enter selection mode, and selection mode exposes checkbox state and a clear toggle action for every row. The selection bar announces the selected count and gives its cancel and Add to Playlist actions localized labels.

Checkboxes do not rely on color alone. Now-playing and selected states have separate semantics. Keyboard users on desktop can enter selection through the row action, move focus across checkboxes and the contextual bar, toggle with the standard activation key, cancel with the applicable back/Escape contract, and submit without invoking playback.

The selection bar uses the existing bottom safe-area policy and must not overlap list content at compact or wide widths. Lists retain sufficient bottom content padding for whichever bottom bar is active. Long localized labels, CJK text, checkboxes, artwork, metadata, duration, and the now-playing indicator must fit without clipping or unintended horizontal scrolling.

Settings backup actions and import preview use the shared app dialog and control patterns. The preview remains readable when reports are long, and individual unmatched or ambiguous entries are available to accessibility navigation rather than communicated only as aggregate colors.

## Testing and Verification

Development follows strict TDD. Pure common tests cover:

- selection start, toggle, zero-selection exit, cancellation, route dismissal, and visible-result reconciliation;
- normal-click versus selection-toggle dispatch, including proof that long press does not start playback;
- independent now-playing and selected states;
- ordered selected IDs from Library home, drill-down, and Search result order;
- picker dismissal, append failure retention, and successful selection cleanup;
- backup encode/decode round trips, canonical ordering, checksum validation, and format-version rejection;
- all byte/count/string/duration bounds and representative malformed inputs;
- text normalization and duration tolerance boundaries;
- unique, unmatched, and ambiguous matching;
- duplicate occurrence preservation and deterministic conflict-free imported names;
- preview counts, all-unmatched playlist skipping, stale-preview rejection, transaction rollback, and repeated import behavior.

Repository integration tests use the real SQLDelight database to prove that one confirmed import transaction creates every planned playlist and ordered entry or creates none. Platform-focused tests cover success, cancellation, unavailable integration, and read/write failure mapping without requiring common code to know platform paths.

Focused Compose/JVM semantics tests prove checkbox exposure, accessible selection entry, click-mode dispatch, selected-count announcements, contextual-bar/Now-Playing mutual exclusion, picker dismissal behavior, and navigation cleanup. Existing playlist duplicate-entry and lifecycle tests remain passing.

Completion evidence must include:

```bash
openspec validate track-multi-select-playlist-backup --strict
./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
GIT_MASTER=1 git diff --check
```

Manual runtime QA covers long press versus tap, checkbox and row toggles, current-track selection, query changes, back/navigation cleanup, contextual-bar replacement and safe-area clearance, existing and inline-created playlist targets, append failure retry, system save/open cancellation, valid and invalid files, import preview, duplicate entries, unmatched and ambiguous reporting, and transaction-failure recovery. Run affected UI checks at compact and wide desktop sizes, light and dark themes, and Android/iOS targets where available. Automated and semantic checks do not substitute for rendered visual and touch/gesture acceptance.

## Implementation Slices

The approved design should be implemented through independently reviewed subagent-driven tasks:

1. Add the pure selection state and dispatch contracts with RED/GREEN tests.
2. Adapt shared track rows, Library home, drill-down, and Search to long-press selection and checkbox semantics.
3. Add the contextual selection bar, route-aware Now Playing replacement, and generalized multi-track playlist picker flow.
4. Add the bounded versioned backup model, canonical codec, validation, checksum, and matching/import-plan tests.
5. Add transactional playlist import and export snapshot services with real repository integration tests.
6. Add Android, iOS, and desktop system file save/open integrations plus Settings export/import and preview/result UI.
7. Run strict OpenSpec and supported platform verification, execute runtime/visual QA where available, update roadmap/progress evidence, and perform final review.
