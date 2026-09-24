# Task 4 RED library recent-browsing specification report

## Scope

This dispatch adds only failing test specifications for playback-history Task 4. Production sources, resources, and unrelated files were not changed. The existing Library and Shared test suites now specify recent-mode projections, visible playback queues, empty-state semantics, compact reachability, selected semantics, selection retention, and flat-page Back policy.

## Tests

| File | RED behavior specified |
| --- | --- |
| `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowserTest.kt` | Recently played ordering by last-played descending with title/artist ties; recently added ordering by created-at descending with title/artist ties; immutable history and created-time projections are passed explicitly. |
| `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContentJvmTest.kt` | Recent-mode taps queue only the displayed, deterministically ordered tracks; empty recently-played history has a normal localized empty message and no import action; compact controls expose all modes and mark Recently played selected. |
| `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt` | Recent modes are flat surfaces: switching from either recent mode to grouped modes clears selection, switching among flat/recent modes preserves selection, and both recent modes resolve to the HomeSongs selection page for Back. |

## Expected RED selector and failure

The approved selector is intentionally not run because commands were prohibited:

```bash
./gradlew :feature:library:impl:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' \
  :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.HomeSelectionPoliciesJvmTest' \
  --configuration-cache
```

Expected result before Task 4 production implementation: compilation fails because `BrowseMode.RecentlyPlayed`/`RecentlyAdded`, the history-aware `visibleTracksForBrowseMode` overload, `LibraryHomeContent` history projections, recent labels/empty resources, and recent flat-page policy are not yet present.

No Gradle task, formatter, linter, test, or other command was run.

## Production Green implementation

- Added `RecentlyPlayed` and `RecentlyAdded` as flat browse modes. The shared
  visible-track projection filters recently played entries to authoritative
  history IDs and sorts by last-played time descending, then lowercase title
  and artist. Recently added sorts by the immutable creation-time projection
  with the same tie order and rejects a missing creation-time entry rather
  than deriving or defaulting one.
- Threaded the immutable history and creation-time maps from App's retained
  authoritative `LibraryContentState`, through `LibraryHomeScreen`, into both
  compact and list/detail `LibraryHomeContent` compositions.
- Kept rows, playback queue requests, visible-ID reconciliation, selection,
  and Back on the existing flat-home path. The shared flat-mode predicate now
  includes both recent modes, so recent-to-recent changes retain selection and
  a change to albums or artists clears it.
- Added English and Simplified Chinese recent-mode and empty-state resources.
  Empty favorites and both recent surfaces suppress library import and scan
  cards; recent empty states render a normal localized message. Browse-mode
  buttons now publish selected semantics.

## Intended Green selector

```bash
./gradlew :feature:library:impl:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' \
  :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.HomeSelectionPoliciesJvmTest' \
  --configuration-cache
```

Per Task 4 constraints, the selector is recorded for the controller and was
not run by this dispatch.
