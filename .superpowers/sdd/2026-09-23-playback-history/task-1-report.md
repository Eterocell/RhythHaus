# Task 1 RED persistence specification report

## Scope

This dispatch adds only the failing Task 1 persistence and repository specifications. It does not add the history SQLDelight relation, migration, generated schema fixture, public API, or either repository implementation.

## Test-first changes

| File | RED behavior specified |
| --- | --- |
| `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/TrackPlayHistoryDatabaseTest.kt` | The current schema is version 4; an existing track receives one history row whose count increments and timestamp advances; missing IDs create no row; metadata upserts retain history; track and source deletion cascade history cleanup. |
| `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ExistingDatabaseMigrationTest.kt` | A version-3 fixture migrates without losing its track and begins with empty history. An unversioned pre-history database with the version-3 table set is recognized as version 3 before migration. Existing migration expectations now require schema version 4. |
| `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/TrackFavoriteDatabaseTest.kt` | The unchanged Favorites schema behavior is expected at the new current schema version, 4. |
| `feature/library/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryApiContractTest.kt` | The API fixture calls and models `TrackPlayHistory`, `playHistory()`, and `recordTrackPlayed(trackId, playedAtEpochMillis)`, including absent-track rejection. |
| `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt` | The future in-memory implementation must increment atomically, reject absent tracks without mutation, retain history through metadata upserts, and clear it through remove-missing, source removal, and clear-all lifecycle paths. |
| `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt` | The future SQLDelight repository must expose empty/new state, incremented history, absent-ID rejection, reopen persistence, rescan preservation, and FK lifecycle cleanup for source removal, remove-missing, clear-all, and clear-all rollback. |

No production file was changed. In particular, this dispatch did not create `TrackPlayHistory.sq`, `3.sqm`, or `4.db`, and did not modify `LibraryRepository`, `SqlDelightLibraryRepository`, `InMemoryLibraryRepository`, or JVM database bootstrap code.

## Expected RED validation

The approved Task 1 RED selector is intentionally not run in this dispatch:

```bash
./gradlew :core:database:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.TrackPlayHistoryDatabaseTest' \
  :feature:library:impl:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' \
  --configuration-cache
```

Expected result: compilation fails because `trackPlayHistoryQueries`, `TrackPlayHistory`, `LibraryRepository.playHistory()`, and `LibraryRepository.recordTrackPlayed(...)` do not yet exist.

The controller should also execute the changed API and common-contract tests after the Green implementation provides the public contract:

```bash
./gradlew :feature:library:api:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.LibraryApiContractTest' \
  :feature:library:impl:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.LibraryRepositoryContractTest' \
  --configuration-cache
```

## Expected Green validation

After the separate Green implementation, run the approved Task 1 coverage selector:

```bash
./gradlew :core:database:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ExistingDatabaseMigrationTest' \
  --tests 'com.eterocell.rhythhaus.library.TrackPlayHistoryDatabaseTest' \
  :feature:library:impl:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.LibraryRepositoryContractTest' \
  --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' \
  --configuration-cache
```

Also include the modified existing database/API test classes:

```bash
./gradlew :core:database:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.TrackFavoriteDatabaseTest' \
  :feature:library:api:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.LibraryApiContractTest' \
  --configuration-cache
```

## Concerns for the Green implementation

- The RED tests establish the generated SQLDelight query surface `trackPlayHistoryQueries`, `selectPlayHistory()`, and `recordTrackPlayed(trackId, playedAtEpochMillis)`. The implementation must provide those names or deliberately update the tests before claiming the contract is Green.
- The existing JVM legacy-version-zero bootstrap must identify the version-3 Favorites table set as version 3, rather than treating it as the new current schema. The new unversioned version-3 migration test exposes that compatibility requirement.
- `4.db` is intentionally absent from this RED-only change. Generate it only after the relation and `3.sqm` exist, following the established sequential SQLDelight generation workflow.
- No Gradle command, test, formatter, linter, or project-wide validation was run, per dispatch constraint. This report makes no passing-build or passing-test claim.

## Green implementation

Implemented the production persistence boundary in commit `124387de`:

- Added the `track_play_history` SQLDelight relation with a cascading foreign key to
  `library_track`, deterministic history selection, and an existence-guarded atomic
  insert/upsert that increments exactly once and replaces the latest timestamp.
- Added migration `3.sqm` and checked-in schema fixture `databases/4.db` using the
  sequential fixture-generation approach (copy version 3, add the relation, set
  `PRAGMA user_version = 4`).
- Added the public `TrackPlayHistory` model and repository methods, plus matching
  in-memory and SQLDelight implementations. Lifecycle deletion paths remove history
  through the in-memory model and database foreign-key cascade.
- Updated JVM legacy version-zero table detection so Favorites-only databases are
  recognized as version 3 and the new relation is recognized as version 4.
- Updated explicit Shared test repositories to implement the expanded contract.

Expected covering commands (not run in this implementation lane):

```bash
./gradlew :core:database:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ExistingDatabaseMigrationTest' \
  --tests 'com.eterocell.rhythhaus.library.TrackPlayHistoryDatabaseTest' \
  :feature:library:impl:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.LibraryRepositoryContractTest' \
  --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' \
  --configuration-cache
```

The controller should also run the API and Favorites regression selectors listed
above. No Gradle validation, formatter, linter, or project-wide test command was
run here by instruction.

## Concerns

The generated SQLDelight source and schema fixture must remain synchronized if the
database schema changes again. The checked-in fixture was created directly from
the version-3 fixture after adding the history table; controller Green validation
is still required to prove generated query compilation and all migration,
restart, and lifecycle cases.

Follow-up correction: the checked-in version-3 fixture had an unversioned SQLite
header, so its `PRAGMA user_version` was set to `3` while retaining the
Favorites-era table set. The focused migration selector then passed:

```text
./gradlew :core:database:jvmTest --tests \
  'com.eterocell.rhythhaus.library.ExistingDatabaseMigrationTest.migrationFromVersionThreePreservesTracksAndStartsWithNoHistory' \
  --configuration-cache
BUILD SUCCESSFUL
```
