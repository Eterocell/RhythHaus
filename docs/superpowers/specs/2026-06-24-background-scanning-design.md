# Background Scanning with Progress

**Status:** approved  
**Date:** 2026-06-24  
**Route:** openspec+superpowers → brainstorming → writing-plans

## Goal

Move folder scanning off the main thread to prevent UI freeze when importing music. Show scan progress (folders visited, files found) in the UI.

## Scope

- Launch `scanner.scan()` on `Dispatchers.Default` via a `CoroutineScope`
- Feed `ScanProgress` state as Compose `MutableState` for reactive UI updates
- Show a progress card ("Scanning… N folders, M files") replacing the import card while scanning
- Reload tracks from repository on the main thread when scan completes
- Add cancellation: user can abort a running scan

## Architecture

```
folder picker callback → launch(Dispatchers.Default) {
    scanner.scan(source)          // background: recursive filesystem traversal + metadata reads
    withContext(Dispatchers.Main) {
        libraryTracks = repository.tracks()
        scanProgress = null
    }
}
scanProgress = MutableState(ScanProgress(session = Scanning, latestItem = "folder/name")) // updated from background
```

`ScanProgress` already exists in `LibraryModels.kt` — reuse it. Pass `isCancelled` lambda to `scanner.scan()` (already supported).

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` — replace synchronous scan with coroutine
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt` — delegate `isCancelled` through, log progress (no API change needed)
- No new files, no new dependencies

## Verification

`./gradlew :shared:jvmTest :shared:compileKotlinIosSimulatorArm64 --configuration-cache` must pass.
