# Track Multi-Select Playlist Backup — Task 5 Report

## Scope

Implemented pure common-main playlist backup export, portable metadata matching, deterministic conflict naming, and revision-bound import planning. Task 4 models and codec were consumed unchanged. No repository mutation, platform document integration, UI/orchestration, schema, dependency, or raw database work was added.

## RED / GREEN TDD

### RED

Added `PlaylistBackupMatcherTest` and `PlaylistBackupServiceTest` before production code, then ran:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupMatcherTest' \
  --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupServiceTest' \
  --configuration-cache
```

Result: expected `:shared:compileTestKotlinJvm` failure. The errors were unresolved Task 5 contracts only: `normalizePortableText`, matcher/result types, typed export result/error, import plan/count/issue models, `exportPlaylistBackup`, and `planPlaylistImport`. No production implementation existed.

### GREEN

Implemented the minimum common-main contracts and reran the same focused command.

Result: `BUILD SUCCESSFUL in 5s`; 26 actionable tasks, 8 executed and 18 up-to-date; configuration cache reused.

Then ran the required sequential full JVM suite:

```bash
./gradlew :shared:jvmTest --configuration-cache
```

Result: `BUILD SUCCESSFUL in 7s`; 26 actionable tasks, 5 executed and 21 up-to-date; configuration cache reused.

`GIT_MASTER=1 git diff --check` also passed with no output. Kotlin LSP diagnostics were requested for all four Kotlin files, but `kotlin-ls` is not installed and installation was previously declined; Gradle compilation and tests are the executable language checks.

## Delivered Behavior

- `normalizePortableText` trims Unicode whitespace, collapses internal Unicode whitespace runs to one ASCII space, and lowercases without a device locale while retaining punctuation, diacritics, and compatibility distinctions.
- `PlaylistBackupMatcher` builds one normalized text-key index over destination metadata. It requires exact normalized title/artist/album plus known whole-second duration within inclusive ±2 seconds. Null destination duration is excluded; results are explicit unique, unmatched, or ambiguous and never select the first ambiguous candidate.
- Export consumes confirmed `PlaylistSnapshot` playlist/entry order and authoritative `LibraryTrack` metadata, preserves duplicate occurrences, emits only title/artist/album/whole duration through the accepted Task 4 codec, and returns typed failure before bytes for missing track, missing duration, invalid duration, or codec bounds.
- Import planning records `libraryRevision`, transaction-ready eligible playlists, complete per-source-playlist reports, aggregate counts, and ordered issue records containing portable entry metadata. It preserves backup/entry order and duplicate resolved IDs, and skips source playlists with no restorable entries.
- Conflict naming compares normalized existing and earlier-planned names, appends the caller-supplied localized imported suffix, and increments deterministic repeats. Tests cover a repeated import producing suffixes 3 and 4.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcher.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupService.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcherTest.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt`
- `.superpowers/sdd/track-multi-select-playlist-backup-task-5-report.md`

Controller-owned `.superpowers/sdd/progress.md`, generic Task 1/2 reports, and `openspec/changes/track-multi-select-playlist-backup/tasks.md` were pre-modified and were neither edited nor staged by Task 5.

## Commit

Planned commit: `feat: add playlist backup matching and planning`, with the required Sisyphus footer and co-author. The implementation and direct tests are one atomic Task 5 contract; this report records its evidence.

## Self-Review

- Confirmed no IDs, paths, sources, artwork, or platform state enter backup bytes.
- Confirmed no fuzzy matching, locale-dependent casing, punctuation/diacritic stripping, or ambiguous first-candidate fallback.
- Confirmed matcher lookup is indexed by normalized text rather than scanning all destination tracks for every backup entry.
- Confirmed name reservation includes existing and earlier planned names and repeated imports.
- Confirmed Task 4 codec/model files are unchanged and no forbidden repository/platform/UI/schema/dependency files were touched.

## Concerns / Handoff

- Task 6 can consume `PlaylistImportPlan.playlists` as ordered transaction mutations and enforce non-empty track IDs.
- Task 8 must compare `PlaylistImportPlan.libraryRevision` with the current authoritative library revision immediately before invoking the Task 6 transaction.
- Localized suffix text is deliberately supplied by the caller; common planning remains localization-framework independent.
- No unresolved Task 5 implementation or JVM-test blocker. Kotlin LSP remains unavailable as noted above.
