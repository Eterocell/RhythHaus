# iOS "Scan for Music" Label + Clear Library Button

**Status:** approved  
**Date:** 2026-06-25  
**Route:** openspec+superpowers → brainstorming → writing-plans

## Goal

1. Rename the import button on iOS from "Add music folder" to "Scan for Music"
2. Add a "Clear Library" button with confirmation dialog

## Scope

- iOS: change `ImportAudioCard` title/description to iOS-specific copy
- All platforms: add "Clear Library" button that shows confirmation dialog, then deletes all tracks from repository and refreshes UI
- App.kt only — no new files

## Design

### iOS Labels

Use an `expect/actual` string constant to provide platform-specific button text:

| Platform | Primary title | Description |
|----------|--------------|-------------|
| iOS | `"Scan for Music"` / `"Manage Music"` | `"Scan the app's music folder for tracks."` |
| Android, macOS/JVM | `"Add music folder"` / `"Manage music folders"` | (unchanged) |

Implementation: single `expect val importCardTitle: String` in `AudioImport.kt` (or new file), or inline check via `Platform.kind`.

### Clear Library Button

- Button: "Clear Library", red/destructive style, visible only when `libraryTracks.isNotEmpty()`
- On tap: show confirmation dialog using Compose `AlertDialog`
- On confirm: delete the database file via `repository` (add `clearAll()` method or delegate to `platformLog` + file deletion on JVM/iOS/Android)
- After clearing: set `libraryTracks = emptyList()`, show toast/message "Library cleared"

Simplest approach: add `fun clearAll()` to `LibraryRepository` and `SqlDelightLibraryRepository` that drops all rows from all tables. Then call `repository.clearAll()`, reload tracks.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` — rename labels, add Clear button + dialog
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt` — add `clearAll()` to interface + `InMemoryLibraryRepository`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt` — implement `clearAll()` with DELETE queries

## Verification

`./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 --configuration-cache` must pass.
