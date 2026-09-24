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

## Green implementation and race handling

`LibraryContentState` now captures immutable play-history and created-time maps
alongside sources, tracks, and favorites. `loadLibraryContent` obtains both
projections for startup, scans, favorite changes, source removal,
remove-missing, clear, and default-source refreshes.

`AuthoritativeLibraryPublicationOwner.publishWithFavoriteReconciliation` now
reloads favorites, history, and created-time projections while holding its
publication mutex. Therefore, a lifecycle content value assembled before an
accepted history event cannot publish its stale history or created-time map:
the lifecycle publication re-reads those projections once it owns the mutex.
If it publishes first, the subsequent history mutation takes the same mutex
and publishes the accepted reload afterward.

App installs one `LaunchedEffect(controller)` collector for
`PlaybackController.playbackStarted`. It captures the epoch timestamp in
Shared, then calls `recordPlaybackHistoryAndPublish`. That helper performs
`recordTrackPlayed`, content reload, and publication inside
`mutateAndPublish`; it never consults the scan coordinator, requests scan
cancellation, or acquires operation admission. A false repository write
(including a track deleted before the atomic write) returns `null` from the
mutation, so it advances no revision and produces no stale publication.

The intended Green selector is:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' \
  --configuration-cache
```

Per task constraints, this selector was not run by this dispatch.

## Review follow-up: App retention and collector resilience

Review found two production-boundary gaps: `applyLibraryPublication` discarded
the new history projections after the authoritative owner had published them,
and an ordinary repository/reload exception escaped the sole
`playbackStarted` collector and terminated it for that App lifetime.

`AppLibraryContentState` is now remembered by App and stores the complete
`LibraryContentState`; `applyLibraryPublication` applies each authoritative
publication to it before updating the revision. All existing downstream App
consumers read the retained content projection, preserving the history and
created-time maps for downstream library consumers as they are added.

`collectPlaybackHistoryAndPublish` is the sole collector run from App's
`LaunchedEffect(controller)`. It timestamps each event in Shared, catches and
reports ordinary event-local persistence/reload failures with
`appFailureMessage()`, and rethrows `CancellationException`. That keeps the
stream available for later events without changing scan admission or history
publication serialization.

New Shared regressions prove that App-owned content replaces both authoritative
history projections, that a failed write is reported while the next event is
still recorded and published, and that cancellation remains propagated.

The added regressions first failed at `:shared:compileTestKotlinJvm` because
the App state projection and resilient collector seam did not exist. After the
minimal implementation, the requested selector completed successfully:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest' \
  --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' \
  --configuration-cache
```

`BUILD SUCCESSFUL in 7s` (146 actionable tasks: 28 executed, 118 up-to-date).

## Final focused verification

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 571ms
146 actionable tasks: 24 executed, 122 up-to-date
```
