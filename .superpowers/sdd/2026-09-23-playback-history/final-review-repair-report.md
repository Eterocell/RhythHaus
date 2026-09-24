# Final-review repair report

## Outcome

This repair closes the three Important findings from the playback-history final review without changing the playback engine API, Shared history ownership, or the `HomeSongs` flat-page Back identity.

## Root causes and fixes

### Android actual-play admission

`AndroidPlaybackEngine.play()` published `PlaybackStatus.Playing` immediately after `MediaController.play()`. That Media3 call is non-blocking: buffering or a player error can occur before `onIsPlayingChanged(true)`. Shared therefore recorded a history event before Android had confirmed actual playback.

The eager `Playing` publication was removed. Android now publishes `Playing` only through the existing `AndroidPlayerListener.onIsPlayingChanged(true)` path and `AndroidPlaybackEventRouter`; progress-loop startup remains unchanged. `playingPublicationWaitsForMedia3ActualPlayingConfirmation` covers the producer callback boundary: a false actual-playing callback produces `Paused`, and only a subsequent true callback produces `Playing`.

### Flat-home selection projection

All flat browse modes intentionally share `TrackSelectionPageKey.HomeSongs`, but `pageTrackIds(HomeSongs)` rebuilt IDs from the full snapshot. Consequently, Select All and the playlist picker included hidden tracks and ignored Favorites/recent ordering.

`LibraryHomeScreen` now retains the `ReconcileVisible(HomeSongs, ids)` projection in `homeVisibleTrackIds`, mirroring the existing Search treatment. `pageTrackIds(HomeSongs)` uses that retained projection. The Back identity remains `HomeSongs`; only selection action input changed. `flatHomeSelectionUsesEachModeVisibleOrderForPicker` exercises Favorites, Recently played, and Recently added through the rendered UI, Select All, and the emitted picker payload.

### Stale Main-thread publication

`AuthoritativeLibraryPublicationOwner` serializes publication creation, but publications apply later on the Compose main thread. A delayed revision N could therefore apply after revision N+1 and overwrite the newer history projection.

`AppLibraryContentState` now tracks the last applied revision and rejects every non-newer publication. `applyLibraryPublication` updates `libraryRevision` only when the Compose state accepts the same publication. `appLibraryContentStateRejectsOlderPublicationAfterNewerHistory` proves an older history snapshot cannot replace a newer one at that boundary.

## Evidence

The new Shared regressions first failed before the production changes:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest.appLibraryContentStateRejectsOlderPublicationAfterNewerHistory' --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest.flatHomeSelectionUsesEachModeVisibleOrderForPicker' --configuration-cache

2 tests completed, 2 failed
BUILD FAILED
```

Focused verification after the fixes:

```text
./gradlew :core:playback:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' --configuration-cache
BUILD SUCCESSFUL in 1s
34 actionable tasks: 4 executed, 30 up-to-date

./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackHistoryAdmissionTest' --tests 'com.eterocell.rhythhaus.library.ui.HomeSelectionPoliciesJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' --configuration-cache
BUILD SUCCESSFUL in 7s
155 actionable tasks: 25 executed, 130 up-to-date

./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' --configuration-cache
BUILD SUCCESSFUL in 5s
66 actionable tasks: 13 executed, 53 up-to-date
```

No known remaining concern within this repair scope.
