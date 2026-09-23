# Branch review repair report

Base: `3fda5ec3`

## Repairs

- Favorite mutations no longer preempt or cancel scans. The operation coordinator admits favorites independently, while scan/destructive admission retains existing cancellation and join behavior.
- Concurrent favorite mutations are admitted through a single semaphore and each mutation publishes against the authoritative revision at execution time rather than a rendered revision snapshot.
- Now Playing favorite callbacks are guarded at the composition boundary with the latest playback state, so a rendered stale track node cannot mutate favorites after playback advances.
- Library favorite action labels now resolve from `feature:library:impl` resources; duplicated Shared resource entries and paths were removed.

## Evidence

- `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.
- `./gradlew :shared:compileTestKotlinJvm --configuration-cache` passed.
- `git diff --check` passed.
- The focused JVM test command for `AppScanCancellationTest` and Now Playing semantics was launched but hung without output and was cancelled; no focused test pass is claimed.

No formatter, linter, or broad quality gate was run.

## Remaining repair evidence

- `AppLibraryOperationCoordinator` publication ordering is preserved while
  `AuthoritativeLibraryPublicationOwner.publishWithFavoriteReconciliation`
  reloads repository favorite IDs inside the publication mutex. Scan and
  destructive snapshots therefore cannot overwrite a favorite projection
  committed before their publication boundary.
- Now Playing favorite actions compare the requested ID with
  `playbackController.state.value.currentTrack?.id` at invocation time;
  retained nodes do not dispatch after playback advances.
- `./gradlew :shared:jvmTest --tests com.eterocell.rhythhaus.AppScanCancellationTest.scanPublicationReconcilesFavoritesWrittenWhileScanWasActive --tests com.eterocell.rhythhaus.AppScanCancellationTest.concurrentFavoriteWritesPublishBothIdsInRevisionOrder --tests com.eterocell.rhythhaus.AppScanCancellationTest.destructivePublicationCannotOverwriteFavoriteProjection --configuration-cache --configuration-cache-problems=fail` passed.
- `./gradlew :shared:jvmTest --tests com.eterocell.rhythhaus.AppScanCancellationTest --configuration-cache --configuration-cache-problems=fail` passed.
- `./gradlew :feature:nowplaying:jvmTest --tests com.eterocell.rhythhaus.nowplaying.NowPlayingContentSemanticsJvmTest --configuration-cache --configuration-cache-problems=fail` passed.
- `./gradlew :shared:compileKotlinJvm --configuration-cache --configuration-cache-problems=fail` passed.
- `git diff --check` passed.
