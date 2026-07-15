# Track-list artwork collapse — Task 2 report

## Status

Implemented SDD Task 2 from `.superpowers/sdd/task-2-brief.md`: the shared album/artist `DrillDownView` now selects one nested-scroll owner per artwork branch, remembers app-owned artwork collapse state, and gives the list and chrome one current `ArtworkCollapseSnapshot`. The no-artwork branch retains Miuix `ScrollBehavior` geometry and visuals.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
  - Added pure scroll-owner and shared-height seams.
  - Added `ArtworkCollapseState` with one mutable offset, a stable `NestedScrollConnection`, and current geometry through `rememberUpdatedState`.
  - Added `rememberArtworkCollapseState`; current geometry produces the immediate snapshot and `SideEffect` persists resize clamping.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
  - Selects exactly one nested-scroll connection for artwork or Miuix mode.
  - Replaced the fixed `maxWidth + 20.dp` artwork reservation with current snapshot height.
  - Passes the same snapshot to the chrome.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
  - Made `NestedScrollChromeToolbarHeight` internal for shared collapsed geometry.
  - Added nullable `artworkCollapseSnapshot`; artwork mode requires it and uses it for progress and height.
  - Preserved no-artwork Miuix collapsed fraction and `TopAppBar` behavior.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`
  - Added branch-selection and shared-snapshot coupling coverage.
- `.superpowers/sdd/track-list-artwork-collapse-task-2-report.md`
  - This Task 2 evidence report.

`LibraryRoutes.kt`, OpenSpec coordinator state, `.superpowers/sdd/progress.md`, `roadmap.md`, and the Task 1 report were not edited by this task.

## Strict RED / GREEN evidence

### RED — branch and coupling seams absent

After adding only the two brief-specified tests, before production edits:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
```

Result: `BUILD FAILED in 1s` at `:shared:compileTestKotlinJvm`, for the intended missing seams:

- `Unresolved reference 'DrillDownScrollOwner'`
- `Unresolved reference 'drillDownScrollOwner'`
- `Unresolved reference 'artworkListTopPaddingPx'`
- `Unresolved reference 'artworkChromeHeightPx'`

The compiler also emitted generic `assertEquals` inference errors downstream of the unresolved owner type. No production source had changed at this point.

### First GREEN — pure seams only

After adding only the enum and three pure functions:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 6s`; 26 actionable tasks, 8 executed and 18 up-to-date.

### Integrated GREEN — remembered adapter and UI wiring

After adding the remembered state, connection, branch wiring, and shared snapshot consumption:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 5s`; 26 actionable tasks, 8 executed and 18 up-to-date.

### Fresh final verification

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
GIT_MASTER=1 git diff --check
```

Results:

- `ArtworkCollapseTest`: `BUILD SUCCESSFUL in 10s`.
- `LibraryNavigationTest`: `BUILD SUCCESSFUL in 372ms`.
- `:shared:compileKotlinJvm`: `BUILD SUCCESSFUL in 360ms`.
- `GIT_MASTER=1 git diff --check`: exit 0, no output.

## Integration review

- Album and artist routes still converge on the one shared `DrillDownView`; CodeGraph found one `DrillDownView` caller in `LibraryRoutes.kt`, so no route edit was necessary.
- `drillDownScrollOwner(hasTopBarArtwork)` selects `Artwork` only when `topBarArtworkTrack != null`; otherwise it selects `Miuix`.
- `LazyColumn` has one `.nestedScroll(drillDownNestedScrollConnection)` attachment. The selected connection is app-owned for artwork and Miuix-owned for fallback; both are never attached together.
- App-owned upward consumption occurs only in `onPreScroll`; positive/symmetric expansion occurs only in `onPostScroll`.
- `ArtworkCollapseState` stores only the mutable offset. Its stable connection reads current geometry from `rememberUpdatedState`.
- The current geometry generates `artworkSnapshot` during composition. Both `artworkListTopPaddingPx(snapshot)` and `artworkChromeHeightPx(snapshot)` return that snapshot's `headerHeightPx`; resize clamping therefore reaches list and chrome in the same composition, while `SideEffect` persists the clamp afterward.
- No fixed expanded artwork reservation remains. Artwork list top padding and chrome height both use the current snapshot.
- Artwork progress drives chrome height, title chips, paper fade, and back-button fade. Miuix `collapsedFraction` is not used for artwork-owned geometry or visual progress.
- The no-artwork branch keeps the existing `ScrollBehavior` parameter, collapsed title/large-title/divider behavior, glass chrome, and fallback top content padding.
- Preserved visual/accessibility behavior: lazy artwork loading, crop, scrim, title-chip formulas, animated paper fade, safe-start inset, 44 dp back target, 20 dp horizontal list padding, 18 dp item spacing, rows, scrollbar, callbacks, and Now Playing spacer.
- Navigation and playback behavior are unchanged.

## Diagnostics and visual QA

- `lsp_diagnostics` was attempted for all four changed Kotlin files. Every attempt reported that `kotlin-ls` is not installed and installation was previously declined. The focused tests and `:shared:compileKotlinJvm` are the executable Kotlin checks.
- Source-level visual review passed for the scoped behavior: existing tokens/primitives and all visual formulas remain in place; only the collapse state source changed.
- No live desktop/device screenshot or gesture capture was produced, so runtime visual confirmation of drag feel, resize transitions, and long/CJK title rendering remains manual. No text, typography, color, or accessibility implementation changed in Task 2.

## Commit

Planned as one user-required atomic commit containing the Task 2 adapter, its two consumers, focused tests, and this evidence report:

```text
fix: coordinate track list artwork collapse
```

The final commit hash is reported by the task runner after commit creation; a commit cannot contain its own hash without rewriting it, and amend is prohibited.

## Concerns

- Remaining manual QA only: validate artwork collapse/expansion gesture feel and resize behavior on a running target, plus long/CJK title chips.
- Kotlin LSP diagnostics remain unavailable by prior user decision.
- No code, test, or compilation concern remains in the focused Task 2 scope.

## Final Oracle Important follow-up — resolved artwork availability

### Finding and root cause

The final Oracle found that `LibraryRoutes.kt` correctly supplies the first album/artist track as a lazy-artwork lookup representative, but Task 2 incorrectly treated that non-null track identity as proof that artwork existed. Two checks diverged from actual availability:

- `DrillDownView` used `topBarArtworkTrack != null` to select the app-owned owner, snapshot, and list padding.
- `DrillDownMiuixScrollChrome` used a non-null `topBarArtworkTrackId` as artwork mode even when lazy lookup later returned `null` or failed.

Routine tracks intentionally omit artwork bytes, and the prior nullable-byte state conflated loading with resolved absence. A representative ID must remain only a loader key.

### Strict RED

Tests were added before production edits to require an explicit loading/available/unavailable state and to prove both shared route kinds ignore representative identity for ownership:

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --configuration-cache
```

Result: `BUILD FAILED in 2s` at `:shared:compileTestKotlinJvm` for the intended missing APIs:

- unresolved `TrackArtworkLoadState`;
- unresolved `initialTrackArtworkLoadState` and `loadTrackArtworkState`;
- no availability-based `drillDownScrollOwner` contract.

No production source had changed before this RED run.

### Fix

- Added `TrackArtworkLoadState.Loading`, `Available(bytes)`, and `Unavailable` in `ArtworkImage.kt`.
- Eager bytes resolve immediately to `Available`; a non-null loader key with no eager bytes starts `Loading`; no key starts `Unavailable`.
- Lazy lookup resolves bytes to `Available`, and resolves `null` or ordinary loader failure to `Unavailable`; coroutine cancellation is rethrown.
- `LazyTrackArtworkImage` continues to use the same loader and displays bytes after an available resolution, preserving lazy artwork on all existing call sites.
- `DrillDownView` performs one representative lookup and constructs `DrillDownArtwork(representativeTrackId, state)`. Only `state is Available` selects the app-owned branch.
- That one owner decision determines the nested-scroll connection, snapshot, list padding, and the resolved bytes passed to chrome.
- Chrome no longer treats track identity as availability and does not perform a duplicate hero lookup; it uses the already resolved bytes and snapshot. Loading/unavailable states wholly use the Miuix branch, while later available resolution recomposes into the coordinated artwork branch.
- `LibraryRoutes.kt`, route behavior, playback behavior, dependencies, and repository lazy-loading behavior remain unchanged.

### Regression coverage

- `ArtworkImageTest.trackIdentityStartsLoadingWithoutClaimingArtworkAvailability`: identity starts `Loading`, not available.
- `ArtworkImageTest.eagerAndLazyArtworkResolveToAvailableBytes`: eager and successful lazy bytes remain displayable.
- `ArtworkImageTest.absentOrFailedLazyArtworkResolvesUnavailable`: null and failure converge to resolved absence.
- `ArtworkCollapseTest.albumAndArtistRepresentativeIdentityDoesNotOwnScrollWithoutResolvedArtwork`: both `AlbumDetail` and `ArtistDetail` with non-null representative IDs use Miuix while loading/unavailable and switch only for `Available`.

### GREEN and diagnostics

```bash
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' \
  --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' \
  --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' \
  --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
GIT_MASTER=1 git diff --check
```

Results:

- Focused tests: `BUILD SUCCESSFUL in 3s`; 26 actionable tasks, 7 executed and 19 up-to-date.
- Shared JVM compilation: `BUILD SUCCESSFUL in 319ms`; 17 actionable tasks, 3 executed and 14 up-to-date.
- Diff hygiene: exit 0, no output.
- `lsp_diagnostics` was attempted for all six changed Kotlin files; `kotlin-ls` remains unavailable because installation was previously declined.

### Follow-up commit

Planned as a new atomic commit containing only the resolved-artwork state, drill-down/chrome integration, direct tests, and this appended report evidence:

```text
fix: classify drill-down artwork after lazy loading
```

The commit hash is reported after creation because amend is prohibited.

### Remaining concerns

- Runtime gesture/resize and long/CJK title visual QA remains manual, unchanged from the original Task 2 report.
- No focused code, test, or JVM compilation blocker remains.
