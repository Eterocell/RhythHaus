# Thin Shared And Completion (Slice 7) — Design

Status: draft (awaiting review/approval)
Route: openspec+superpowers
OpenSpec slice: 7 (tasks 8.1–8.4)

## Context

Slices 5.1 (Now Playing), 5.2 (playlists/backup), 5.3 (Search), 6.4 (Settings), and
Task 6.1 (Library-last) have extracted leaf implementations into unexported feature
modules and physical capabilities into `:core:*`. This final slice cleans `:shared` to
its thin composition/facade role and completes the modularization with a thin-shared
inventory assertion (8.1), dead migrated-ownership removal (8.2), a real-structure-only
scaffold (8.3), and the completion evidence (8.4).

## Thin-shared target

`:shared` owns **only** these responsibilities:

1. App composition — `App.kt` (`App()`).
2. Root shell — `library/ui/LibraryAppShell.kt`, `LibraryDialogs.kt` (shell chrome/dialogs).
3. Cross-feature route/Back arbitration — `library/ui/LibraryAppState.kt`,
   `LibraryNavigation.kt`, `LibraryRoutes.kt`.
4. Lifecycle — `PlaybackProcessLifecycle.kt`.
5. Koin assembly — `di/RhythHausDi.kt`.
6. Stable iOS facade — `iosMain/.../MainViewController.kt` (Swift-visible `MainViewControllerKt.MainViewController()`).

Plus **intentionally retained** Shared responsibilities established by prior slices:

- Playback session coordination/persistence — `session/PlaybackSessionCoordinator.kt`,
  `session/PlaybackSessionStore.kt` (ADR `:shared` retains coordination, persistence, DataStore adapters).
- Theme persistence — `theme/ThemePreferenceStore.kt` (commit `5dad051` keeps the private theme store in `:shared`).
- Platform playback engine factory — `PlatformPlaybackEngineFactory.kt` (package-stable facade returning `PlatformPlaybackEngine`).
- Playlist-backup ABI/launcher seam — `playlistbackup/PlatformPlaylistBackupDocuments.kt` (+ android/jvm/ios actuals).
- Library selection integration — `library/LibraryPlaybackSelection.kt` (uses `PlaybackController`; stays in `:shared`).
- Track selection state/bar — `library/ui/TrackSelectionBar.kt`, `TrackSelectionState.kt`.
- Now Playing shell placement — `nowplaying/NowPlayingScreen.kt`.
- Shared formatting helpers — `MusicModels.kt` (`formatDuration`, `accentForIndex`,
  `librarySnapshot`, `Track.toPlayableTrack()`).

## 8.1 — Thin-shared inventory test

Add a JVM test in the architecture convention test suite (next to
`ArchitectureCheckPluginFunctionalTest`) that asserts the `:shared` source-set inventory
equals the approved thin-shared set. The test:

- enumerates the approved `:shared` `commonMain`/`androidMain`/`jvmMain`/`iosMain`
  source files (the facade + retained files above) as a canonical path set;
- fails when `:shared` contains any source file outside that set (migrated implementation
  ownership), and fails when an approved file is missing;
- is package-agnostic for retained shell/selection files (they legitimately live under
  `library/ui`) but rejects any *new* package root not enumerated.

The assertion is a **declared-inventory** check (source-file existence), not a
byte-level behavioral test; behavioral responsibilities remain covered by the existing
per-feature and Shared route/lifecycle tests.

## 8.2 — Remove migrated implementation ownership

Remove dead code (0 call sites, confirmed by grep) that the extraction left behind:

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Logger.kt` (`val log` Kermit singleton, unused).
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Platform.kt` (`interface Platform`, `expect fun getPlatform()`).
- `shared/src/androidMain/.../Platform.android.kt`, `shared/src/jvmMain/.../Platform.jvm.kt`,
  `shared/src/iosMain/.../Platform.ios.kt` (the `getPlatform()` actuals + platform classes).

No bridge dependency is introduced: `:shared` remains the sole composition/facade module,
and the architecture allow-list/cycle checks continue to enforce the frozen graph. Public
APIs and KDoc requirements for the retained declarations are unchanged.

## 8.3 — Real-structure-only scaffold

Add a `FeatureScaffoldPlugin` convention in `build-logic/convention`
(`com.eterocell.gradle.scaffold`) that generates only a real requested module structure:
build.gradle.kts applying the existing `android.kmp.library` / `compose-resource` /
`architecture` conventions, the `commonMain`/`androidMain`/`jvmMain`/`iosMain` source
skeleton, a package-root `README`, and a KDoc'd public-surface placeholder. A TestKit
functional test drives the generator and asserts that only requested real module
directories are created, that API generation requires an actual contract name, and that
no empty `UiState`/`UiEvent`/`UiEffect`/presenter class is ever generated. Scaffold
documentation goes in the canonical `skills/kmp-architecture/SKILL.md`. Explicitly
**deferred**: package renames and Dependency Analysis Gradle Plugin evaluation (per
design.md Migration Plan step 7).

## 8.4 — Completion

Run final `qualityCheck` (Spotless + Detekt), `./init.sh`, strict OpenSpec validation,
and `git diff --check`; align `progress.md`, `roadmap.md`, ADRs, feature READMEs, and
OpenSpec task evidence; commit each completed slice conventionally. Runtime/device/visual
claims remain out of scope unless independently demonstrated.
