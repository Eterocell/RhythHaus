# Task 3 RED Shared history specification report

## Scope

This dispatch adds only Shared test specifications for Task 3. It does not change `commonMain`, DI assembly, repository/playback APIs, Tasks 4–5, OpenSpec artifacts, or roadmap/progress files.

## Tests

| File | RED behavior specified |
| --- | --- |
| `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackHistoryAdmissionTest.kt` | An admitted `PlaybackStarted` event records an indexed track and republishes its count, last-played time, and created-time projection; history recording leaves an admitted scan running and never requests scan cancellation; an event for a source-removed track writes and publishes nothing. |
| `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt` | Startup content snapshots include isolated play-history and track-created-time maps; a scan snapshot captured before an accepted history write cannot publish a stale history map; a destructive source removal publishes no removed track, history, or created-time entry. |

The race specifications use the existing `AuthoritativeLibraryPublicationOwner` seam and require its lifecycle publication path to reconcile the current favorite, history, and created-time projections while holding authoritative publication ownership. The admission specifications require the planned internal `recordPlaybackHistoryAndPublish` helper to mutate and reload inside that ownership boundary without taking scan admission.

## Expected RED selector and failure

The approved selector is intentionally not run because this is a RED-only dispatch and commands were prohibited:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --configuration-cache
```

Expected result before Task 3 production implementation: test compilation fails because `recordPlaybackHistoryAndPublish` is absent, `LibraryContentState` has no `playHistory` or `createdAtByTrackId` projections, and `AuthoritativeLibraryPublicationOwner.publishWithFavoriteReconciliation` does not yet accept the history and created-time reconciliation loaders.

## Intended production boundary

The tests pin these internal Shared seams for the Green implementation:

- `LibraryContentState.playHistory: Map<String, TrackPlayHistory>` and `createdAtByTrackId: Map<String, Long>` are snapshot projections loaded with every library content refresh.
- `recordPlaybackHistoryAndPublish(event, publicationOwner, repository, platformAccess, playedAtEpochMillis, ioDispatcher, publish)` records only a still-indexed ID, then reloads and publishes the combined projection in `mutateAndPublish`.
- Lifecycle publication reconciliation reloads the current favorite, history, and created-time maps while the publication owner is locked, so a delayed scan cannot replace an accepted history value and source deletion removes all dependent projections.

No Gradle task, formatter, linter, or test/verification command was run.
