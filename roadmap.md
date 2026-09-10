# RhythHaus roadmap

> **Scope.** RhythHaus is a local-first music player for Android, iOS, and macOS/desktop JVM. Windows, Linux, cloud sync, accounts, streaming, and recommendations are intentionally out of scope unless explicitly approved.
>
> **How to use this file.** This is the current product roadmap: active work, priority order, and release evidence. Detailed implementation evidence and historical handoffs live in [`progress.md`](progress.md); durable requirements, designs, and task state live under [`openspec/`](openspec/).

## Current focus — Phase 1: release closure and first-run usability

| Priority | Outcome | Status | Next evidence or change |
| --- | --- | --- | --- |
| P0 | iOS Files.app import | Completed: Files.app file/directory import, recursion, cancellation, duplicate handling, durable sandbox playback, default `Documents/RhythHaus` source, and scan-only handling of files already under that source. | Archived as [`2026-09-09-ios-files-import`](openspec/changes/archive/2026-09-09-ios-files-import/); no further Phase 1 work remains in this change. |
| P0 | Three-platform playback acceptance | Automated coverage exists; real device/system-control acceptance remains open. | Validate foreground play/pause/seek, previous/next, lock-screen or notification controls, interruption handling, route loss, end-of-track advance, and background continuation where supported on Android, iOS, and macOS. |
| P0 | Android notification denial | Implemented and automated-accepted: denial now explains the non-blocking system-control degradation and offers re-request or app notification settings. | Complete Android 13+ playback-in-denial and recovered system-media-control acceptance, then archive `android-notification-permission-recovery`. |
| P0 | Playback failure recovery | Error rendering exists; actionable recovery does not. | Classify missing files, lost access, unsupported formats, and decoder failures; offer retry, skip, and queue removal. |
| P1 | First-run onboarding | Not implemented. | Explain local-first storage, platform-specific import, permissions, supported formats, scan behavior, and recovery limits. |

### iOS import decision and constraints

- Files.app permits multi-file and folder selection; RhythHaus copies supported audio into its managed sandbox and scans the existing `ios-app-local` source.
- Copying, duplicate handling, cancellation, and source mutation/scan handoff are covered by automated JVM and XCTest regressions.
- Real picker-to-playback acceptance is complete; the remaining scope is the requested default app-local source/access behavior.
- MusicKit and security-scoped bookmarks remain deferred; do not introduce external URL persistence in this change.

### Explicitly abandoned work

`adopt-miuix-navigation-runtime` is archived at [`openspec/changes/archive/2026-09-09-adopt-miuix-navigation-runtime/`](openspec/changes/archive/2026-09-09-adopt-miuix-navigation-runtime/). Do not revive its conditional tasks unless upstream exposes a compatible Back-delegation API and JVM-target-compatible presentation API.

## Next product phases

| Phase | Goal | Ordered deliverables |
| --- | --- | --- |
| Phase 2 — daily use | Make the existing library and player efficient to use every day. | Favorites; play history/recently played/recently added; sorting and filtering; sleep timer; save Queue as playlist; smart playlists. |
| Phase 3 — library management | Give users safe control over imported metadata and sources. | App-local metadata overrides; M3U/PLS import/export; Android MediaStore source; incremental-scan UX; desktop drag-and-drop and remaining cross-platform import policy. |
| Phase 4 — advanced playback | Improve listening quality without adding network dependence. | ReplayGain/normalization; crossfade; basic EQ; gapless playback; local and embedded lyrics. |

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
| Android notification denial | Provide status, rationale, and a Settings re-entry path rather than ignoring the permission result. |
| Source and file lifecycle | Specify user-facing recovery for deleted/moved files, unavailable disks, revoked SAF grants, and source re-binding. |

### P1 — library and workflow

| Area | First useful version |
| --- | --- |
| Favorites and history | Persist favorites, play count, last played, recently played, and recently added. |
| Sorting and filtering | Sort direction, added/modified date, play count, favorites, artwork, and source filters. |
| Metadata editing | App-local display overrides; do not write original media tags initially. |
| Smart playlists | Favorites, recent tracks/additions, artist, album, and saved Queue rules. |
| Playlist interoperability | M3U/PLS import/export; the existing JSON backup remains a recovery format. |
| Android media discovery | Optional MediaStore source beside SAF folders. |
| Incremental scanning | Explicit unchanged/added/modified/deleted summary and refresh policy. |
| Import ergonomics | Desktop drag-and-drop plus an explicit remaining copy-versus-reference policy; preserve current iOS sandbox-copy decision. |
| Sleep timer | Timed stop, stop after current/N tracks, and fade-out. |

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

Automated tests and compilation are necessary but not sufficient for UI, picker, system-media, accessibility, or audible playback claims. Record manual evidence separately for:

- Android/iOS device behavior and macOS system controls.
- Files/folder selection, scanning, restart, and playable imported media.
- Compact/wide, light/dark, Chinese/English/CJK rendering, accessibility, long press/drag, predictive Back, and system panels.

## Historical delivery record

Completed implementation history, command output, review evidence, and superseded decisions are intentionally maintained in [`progress.md`](progress.md), commit history, and archived OpenSpec changes. Keeping that evidence out of the active roadmap prevents historical acceptance notes from obscuring current release priorities.
