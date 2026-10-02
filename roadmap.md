# RhythHaus roadmap

> **Scope.** RhythHaus is a local-first music player for Android, iOS, and macOS/desktop JVM. Windows, Linux, cloud sync, accounts, streaming, and recommendations are intentionally out of scope unless explicitly approved.
>
> **How to use this file.** This is the current product roadmap: active work, priority order, and release evidence. Detailed implementation evidence and historical handoffs live in [`progress.md`](progress.md); durable requirements, designs, and task state live under [`openspec/`](openspec/).

> **Verification ownership.** The user performs manual UI, listening, physical-device, background, and system-media-control acceptance. The implementation owner runs automated behavior tests, native smoke, compilation and static checks. Pending manual acceptance is not a passed check and does not block the next planned implementation unless it exposes a defect.

## Current focus — Phase 1: release closure and first-run usability

| Priority | Outcome | Status | Next evidence or change |
| --- | --- | --- | --- |
| P0 | iOS Files.app import | Completed: Files.app file/directory import, recursion, cancellation, duplicate handling, durable sandbox playback, default `Documents/RhythHaus` source, and scan-only handling of files already under that source. | Archived as [`2026-09-09-ios-files-import`](openspec/changes/archive/2026-09-09-ios-files-import/); no further Phase 1 work remains in this change. |
| P0 | Three-platform playback acceptance | Automated coverage exists; real device/system-control acceptance remains open. | Validate foreground play/pause/seek, previous/next, lock-screen or notification controls, interruption handling, route loss, end-of-track advance, and background continuation where supported on Android, iOS, and macOS. |
| P0 | Android notification denial | Completed implementation: Android 13+ denial now explains the non-blocking system-control degradation and offers re-request or app notification settings. | Complete Android 13+ in-app playback-under-denial and restored system-media-control acceptance, then archive `android-notification-permission-recovery`. |
| P0 | Playback failure recovery | Completed implementation: structured fail-closed platform errors, retry, non-wrapping skip, queue-only removal, generation-safe reconciliation, and error-only localized Now Playing actions. | Native macOS disposable-file recovery acceptance is automated. Android device and iOS runtime interaction remain release-evidence gaps because no safe controllable target was available; archived as [`2026-09-10-playback-failure-recovery`](openspec/changes/archive/2026-09-10-playback-failure-recovery/). |
| P1 | First-run onboarding | Completed: durable one-time Skip/Finish flow, platform-accurate EN/ZH guidance, canonical Back/navigation, compact scrolling, and Settings re-entry. | Archived as [`2026-09-12-first-run-onboarding`](openspec/changes/archive/2026-09-12-first-run-onboarding/); Android/iOS device presentation remains release evidence rather than an implementation blocker. |
| P1 | Favorites | Completed: durable track favorites, authoritative cross-surface state, Favorites browsing, accessible Library/Now Playing actions, and EN/ZH localization. | Archived as [`2026-09-23-favorites`](openspec/changes/archive/2026-09-23-favorites/); physical Android/iOS and system-media acceptance remains general release evidence, not a Favorites implementation blocker. |

### iOS import decision and constraints

- Files.app permits multi-file and folder selection; RhythHaus copies supported audio into its managed sandbox and scans the existing `ios-app-local` source.
- Copying, duplicate handling, cancellation, and source mutation/scan handoff are covered by automated JVM and XCTest regressions.
- Real picker-to-playback acceptance and the default `Documents/RhythHaus` app-local source are complete.
- MusicKit and security-scoped bookmarks remain deferred; do not introduce external URL persistence in this change.

### Explicitly abandoned work

`adopt-miuix-navigation-runtime` is archived at [`openspec/changes/archive/2026-09-09-adopt-miuix-navigation-runtime/`](openspec/changes/archive/2026-09-09-adopt-miuix-navigation-runtime/). Do not revive its conditional tasks unless upstream exposes a compatible Back-delegation API and JVM-target-compatible presentation API.

## Next product phases

| Phase | Goal | Ordered deliverables |
| --- | --- | --- |
| Phase 2 — daily use | Make the existing library and player efficient to use every day. | ~~Favorites; play history/recently played/recently added; sorting and filtering; sleep timer; save Queue as playlist; smart playlists~~. All implementations complete; pending manual acceptance remains release evidence. |
| Phase 3 — library management | Give users safe control over imported metadata and sources. | ~~App-local metadata overrides; M3U/PLS import/export~~; Android MediaStore source; incremental-scan UX; desktop drag-and-drop and remaining cross-platform import policy. |
| Phase 4 — advanced playback | Improve listening quality without adding network dependence. | ReplayGain/normalization; crossfade; basic EQ; gapless playback; local and embedded lyrics. |

Sorting and filtering is integrated into `main`: Shared owns an ephemeral query, flat Library modes use deterministic sort/filter projections, artwork presence is read without loading image blobs, and accessible EN/ZH controls preserve visible selection and playback order. Android packaging is still blocked by the TagLib native toolchain, and the known full Shared JVM playback-selection timeout prevents claiming a green full-suite gate. Physical-platform acceptance remains a release check; no metadata writes or saved sort presets were added.

Sleep timer is integrated into `main` at `21af63ab`: process-local timed/current/N-track stop, reversible ten-second playback-only fade, localized compact/split controls, and controller-owned native remote commands. Canonical spec: `openspec/specs/sleep-timer/spec.md`; archived change: `openspec/changes/archive/2026-09-29-sleep-timer/`. Post-merge Core JVM tests and desktop compilation pass; OpenSpec validation passes (58/58). Earlier focused Android host regressions, Shared/Now Playing tests, iOS Kotlin compilation and Swift provider XCTest also pass; macOS tests exercise real temporary WAV playback and native gain. Full Android host execution timed out, Android APK assembly remains blocked by TagLib CMake, and no green full `./init.sh` is claimed. User-owned physical/listening/background/system-control acceptance remains pending.

Save Queue as playlist is integrated into `main` at `f9c2fcb4`: the Queue tab captures current plus upcoming occurrences in displayed order when naming opens, excludes the played prefix, preserves duplicates, and creates one atomic saved playlist. Concurrent confirmation is guarded and stale responses from dismissed dialogs cannot affect a new draft. Empty/unselected queues hide the action; playback remains untouched. The canonical `saved-playlists` specification has been updated and the change archived at `openspec/changes/archive/2026-09-29-save-queue-as-playlist/`. Post-merge focused tests and desktop compilation passed; OpenSpec validation passed 58/58. User-owned Android/iOS/macOS UI acceptance remains pending.

Smart playlists are integrated into `main`: persisted single rules for Favorites, Recently Played/Added (10/25/50), Artist, Artist+Album and static saved-playlist membership. Derived rows update from authoritative library inputs; saved-source order and duplicate occurrences are retained. Create/edit/delete and EN/ZH accessible details preserve playback, handle disappearing sources and guard stale modal outcomes. Static v1 JSON backup excludes smart rules and explains this at the export entry. Migration/repository, Playlist/Core JVM suites, focused Shared tests and desktop/Android/iOS Kotlin compilation pass, as do quality gates and independent final review. Desktop smoke observed the creation surface; isolated end-to-end UI automation was blocked by macOS AX window reads despite granted permissions. Full Shared baseline failures and Android native packaging limits remain; manual acceptance is user-owned and pending. Canonical spec: `openspec/specs/smart-playlists/spec.md`; archive: `openspec/changes/archive/2026-09-29-smart-playlists/`.

Phase 3 metadata overrides are integrated into `main` at `1de6262e`: app-local title/artist/album/track/disc corrections use a separate track-ID relationship; rescans retain raw tags; restore-one/all reveals current scanned values; and track deletion cascades corrections. Database, repository, Shared, playback, editor, focused JVM, quality, desktop, and iOS compilation gates passed. Final review repairs cover empty-detail recovery during editing, fresh raw-value projection, cancellation-safe persistence/publication, and stale/nonfatal native label refresh. Canonical spec: `openspec/specs/track-metadata-overrides/spec.md`; archive: `openspec/changes/archive/2026-09-30-track-metadata-overrides/`. Physical UI/listening acceptance remains user-owned.

## Capability baseline

RhythHaus already provides:

- Recursive local-library scanning: Android SAF, macOS folders, and the iOS app-local source.
- SQLDelight persistence for sources, tracks, scan sessions, and scan errors; cancellation, rescan, remove-missing, and access-loss recovery.
- WAV, AIFF, AU, MP3, M4A/AAC, FLAC, and OGG recognition; TagLib metadata and embedded artwork.
- Album, artist, song, and search browsing; queue, repeat, shuffle, session persistence, and saved/editable playlists with backup/restore.
- Android Media3, iOS AVAudioPlayer, and macOS native audio bridges with partial system-media integration.
- Chinese/English resources, themes, About/open-source pages, long-press selection, and compact/wide layouts.

## Prioritized product backlog

### P0 — release blockers and lifecycle gaps

| Gap | Product decision |
| --- | --- |
| Real playback-system acceptance | Treat device/system controls as release evidence. Compilation and unit tests do not prove Bluetooth/wired controls, lock-screen/notification surfaces, route loss, interruptions, or background continuation. |
| Android notification denial | Implemented; Android 13+ physical playback-under-denial and restored media-control acceptance remain open. |
| Source and file lifecycle | Specify user-facing recovery for deleted/moved files, unavailable disks, revoked SAF grants, and source re-binding. |

### P1 — library and workflow

| Area | First useful version |
| --- | --- |
| Favorites and history | Implemented: favorites, play count, last played, recently played, and recently added. |
| Sorting and filtering | Implemented: sort direction, added/modified date, play count, favorites, artwork, and source filters. |
| Metadata editing | Integrated: app-local display overrides without writing original media tags; manual acceptance pending. |
| Smart playlists | Integrated: Favorites, recent tracks/additions, artist, album, and static saved-playlist rules; user-owned manual acceptance pending. |
| Playlist interoperability | Integrated: M3U/M3U8/PLS import/export; the existing JSON backup remains a recovery format. User-owned device/file-picker acceptance remains pending. |
| Android media discovery | Optional MediaStore source beside SAF folders. |
| Incremental scanning | Explicit unchanged/added/modified/deleted summary and refresh policy. |
| Import ergonomics | Desktop drag-and-drop plus an explicit remaining copy-versus-reference policy; preserve current iOS sandbox-copy decision. |
| Sleep timer | Implemented: timed/current/N-track stop and reversible ten-second fade. Physical/listening/background/system-control acceptance remains user-owned and pending. |

### P1 — advanced player

| Area | First useful version |
| --- | --- |
| Equalizer and effects | Replace visual-only `EqualizerStrip` with a real basic EQ; defer larger DSP scope until platform seams are designed. |
| Normalization and transitions | ReplayGain/volume normalization, crossfade, and gapless playback. |
| Lyrics | Local `.lrc` and embedded lyrics only; remote lyrics requires a separate privacy/network decision. |
| Output and session settings | Output/session status, automatic-resume policy, cache clearing, and interruption preferences. |

### P2 — intentionally later

- Cloud or multi-device synchronization.
- Streaming services, accounts, social sharing, and recommendations.
- Windows/Linux packaging.
- Share/export/open-in capabilities beyond the current playlist backup flow.

## Release evidence rule

Automated tests and compilation are necessary but not sufficient for UI, picker, system-media, accessibility, or audible playback claims. **Verification ownership:** the user performs and reports manual device/UI/listening/system-control acceptance; the implementation owner runs automated tests, builds, and static checks. Until the user reports a scenario's result, record it as pending rather than passed. Manual ownership does not waive automated verification or silently close an OpenSpec acceptance task. Record manual evidence separately for:

- Android/iOS device behavior and macOS system controls.
- Files/folder selection, scanning, restart, and playable imported media.
- Compact/wide, light/dark, Chinese/English/CJK rendering, accessibility, long press/drag, predictive Back, and system panels.

## Historical delivery record

Completed implementation history, command output, review evidence, and superseded decisions are intentionally maintained in [`progress.md`](progress.md), commit history, and archived OpenSpec changes. Keeping that evidence out of the active roadmap prevents historical acceptance notes from obscuring current release priorities.
