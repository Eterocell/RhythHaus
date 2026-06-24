# Folder-Based Music Import with Album/Artist Browsing

**Status:** approved  
**Date:** 2026-06-24  
**Route:** openspec+superpowers → brainstorming → writing-plans

## Goal

Replace the single-file audio import UI with a folder-based scanning workflow. Group imported tracks by album and artist in a two-level browsing UI (Albums / Artists → drill-down to tracks). Wire the existing Kotlin Multiplatform scanner infrastructure into the Compose UI. Activate the iOS app-local folder import mechanism.

## Scope

- **In:** Wire `LibraryScanner` + `PlatformAudioScanner` into UI, replace old `AudioImportLauncher` with folder picker flow, add album/artist grouping with two-level drill-down, wire iOS app-local folder scanner
- **Out:** New database schema (already exists), Android SAF scanner changes (already built), macOS/JVM scanner changes (already built), playback engine changes, dark mode, Apple Music access

## Current State

The scanner infrastructure is fully built but not wired to the UI:

| Component | Status | Location |
|-----------|--------|----------|
| `LibraryScanner` | ✅ Done | `library/LibraryScanner.kt` |
| `PlatformAudioScanner` | ✅ Done | `library/LibraryScanner.kt:17` |
| `LibrarySource` / `LibraryTrack` / `ScanSession` | ✅ Done | `library/LibraryModels.kt` |
| `LibraryRepository` API | ❌ Not done | OpenSpec task 2.3 |
| Android SAF scanner | ✅ Done | `library/PlatformSourceAccess.android.kt` |
| macOS/JVM filesystem scanner | ✅ Done | `library/PlatformSourceAccess.jvm.kt` |
| iOS app-local scanner | ✅ Done | `library/PlatformSourceAccess.ios.kt` |
| Database schema | ❌ Not done | OpenSpec task 2.2 |
| UI integration | ❌ Not done | OpenSpec task 5.x |

Current UI uses `AudioImportLauncher` (`App.kt`) with `ImportedAudioFile` and manual `enrichImportedAudioFiles` — no persistence, no grouping.

## Architecture

```
┌─────────────────────────────────────────────┐
│ UI Layer (App.kt, shared Compose)           │
│  Albums/Artists segmented picker            │
│  Album grid / Artist list                   │
│  Track list (drill-down)                    │
│  Now-playing card (unchanged)               │
│  Add-folder button                          │
├─────────────────────────────────────────────┤
│ Domain Layer                                │
│  LibraryRepository (interface)              │
│  LibraryScanner (already built)             │
│  Album/Artist grouping (computed client-side)│
├─────────────────────────────────────────────┤
│ Persistence (SQLDelight)                    │
│  sources, tracks, scan_sessions, scan_errors │
├─────────────────────────────────────────────┤
│ Platform Layer                              │
│  Android: SAF tree picker + scanner         │
│  macOS/JVM: native folder picker + scanner   │
│  iOS: app-local folder scanner               │
│  (Documents/RhythHaus Music)                 │
└─────────────────────────────────────────────┘
```

## Component Mapping

### UI Changes (App.kt)

| Old | New |
|-----|-----|
| `AudioImportLauncher` file picker | `PlatformFolderPickerLauncher` (already exists) |
| `ImportedAudioFile` list | `LibraryTrack` list from repository |
| `importedLibrarySnapshot()` | Repository query → client-side grouping |
| Flat `LazyColumn` of tracks | Two-level: Albums/Artists → drill-down to tracks |
| Single "Import local audio" card | "Add music folder" button + scan progress |

### iOS Activation

- Wire existing `rememberPlatformFolderPickerLauncher` (already returns `isAvailable = true`)
- Replace `AudioImportLauncher.isAvailable = false` stub
- App-local folder: `Documents/RhythHaus Music/` — user transfers files via Finder file sharing
- No UIDocumentPickerViewController needed (future scope)

### Album/Artist Grouping

Computed client-side from `LibraryTrack` fields:
```kotlin
val albums: Map<String, List<LibraryTrack>> = tracks.groupBy { it.album }
val artists: Map<String, List<LibraryTrack>> = tracks.groupBy { it.artist }
```

Two-level UI: top level shows album covers (accent colors) or artist names with track counts. Tapping drills into a filtered track list.

## Database Schema

SQLDelight schema (tables already designed in OpenSpec):

```
LibrarySource:  id, platform_kind, display_name, handle, created_at, last_scan_at, access_status
LibraryTrack:   id, source_id, source_local_key, audio_source, display_name,
                title, artist, album, duration_millis, size_bytes, modified_at,
                last_seen_scan_id, created_at, updated_at
ScanSession:    id, source_id, status, started_at, completed_at, folders_visited,
                files_visited, tracks_added, tracks_updated, files_skipped, terminal_message
ScanError:      id, scan_id, source_local_key, display_path, reason, recoverable, created_at
```

## Verification

`./init.sh` must pass: shared JVM tests, shared iOS simulator tests, desktop compile, Android debug build. Platform-specific scanner tests where practical.

## Dependencies

No new external dependencies. Uses existing SQLDelight, taglib, Compose Multiplatform, Miuix.
