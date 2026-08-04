# Task 5.1 implementation report

## Status

**ACCEPTED.** The registered feature module, architecture/KSP fixtures, resource ownership,
selective test ownership, focused JVM/Android-host/iOS-simulator checks, consumer compilation,
formatting/lint, and Swift-consumer checks pass. Final independent scope and behavior approval
accepted implementation commit `28dd2e1`; runtime UI/playback, desktop launch, Android/iOS
device/runtime validation, and `./init.sh` remain intentionally unclaimed.

## Baseline and commits

- Required baseline: `04c66f642025b4fd0edcad9929c4fe6fbae101b8`
- Observed correction-lane `HEAD`: `b0b416d366dd478909f04bb91d297864e62d55ca`
- Implementation commit: `28dd2e150c0671be10ac24ef3ef5ede3c4df9f19`
  (`refactor: extract now playing feature`)

## Current implementation inventory

The exact changed implementation paths are:

```text
build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts
build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt
build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt
core/ui/build.gradle.kts
core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt
core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt
core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt
core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt
feature/nowplaying/build.gradle.kts
feature/nowplaying/src/commonMain/composeResources/values/strings.xml
feature/nowplaying/src/commonMain/composeResources/values-zh/strings.xml
feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingAdaptiveLayout.kt
feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt
feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContent.kt
feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt
feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/BottomBarModeTest.kt
feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingAdaptiveLayoutTest.kt
feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContractsTest.kt
feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt
feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingArtworkRenderingJvmTest.kt
feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBarSemanticsJvmTest.kt
feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt
gradle/libs.versions.toml
settings.gradle.kts
shared/build.gradle.kts
shared/src/commonMain/composeResources/values/strings.xml
shared/src/commonMain/composeResources/values-zh/strings.xml
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt
```

Deleted shared origins are the moved `NowPlayingBar.kt`, `MusicProgressScrubber.kt` and its test,
`LiquidGlassChrome.kt`, `SwipeBackGesture.kt`, and `VerticalSheetGesture.kt`. The ignored report
is not implementation inventory. Canonical planning/OpenSpec/progress/roadmap artifacts remain
untouched; `App.kt`, shared `ui/TrackArtworkImage.kt`, playback bridge/Swift ownership, and other
explicitly excluded paths remain unchanged.

## Required RED evidence

Command run verbatim from the required worktree:

```text
./gradlew :feature:nowplaying:allTests --configuration-cache
```

Brief's expected diagnostic:

```text
Project with path ':feature:nowplaying' could not be found in project ':'
```

Actual Gradle 9.6.1 diagnostic:

```text
Selection failed
Cannot locate tasks that match ':feature:nowplaying:allTests' as project 'nowplaying' not found in project ':feature'.
```

Gradle exited with `BUILD FAILED in 11s`; configuration cache entry was stored. Controller
reconciliation accepted the Gradle 9.6.1 wording as semantically equivalent to the planned
absent-project RED, specifically ruling that it proves `:feature:nowplaying` is unregistered and
absent, that task selection failed, and that no requested feature task or compilation executed.
The planned exact diagnostic did not appear; the version-specific equivalence ruling, rather than
an exact-text claim, is the accepted RED evidence.

## GREEN/final verification

The accepted RED is complete. The first registered-module checkpoint initially failed only on
missing declaration-level KDoc, then shared compilation failed only because composition locals
were read from non-composable lambdas. Both root causes were repaired. `./gradlew
:feature:nowplaying:compileKotlinJvm :shared:compileKotlinJvm --configuration-cache` then passed.
Focused `:feature:nowplaying:jvmTest --tests '*NowPlayingBarSemanticsJvmTest'` and
`:core:ui:jvmTest --tests '*VerticalSheetGestureJvmTest'` passed. The prescribed combined command
passed with all nine requested tasks:

```text
./gradlew :feature:nowplaying:jvmTest :feature:nowplaying:testAndroidHostTest \
  :feature:nowplaying:iosSimulatorArm64Test :shared:jvmTest :desktopApp:compileKotlin \
  :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 \
  :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64 \
  --configuration-cache
```

Additional passing gates: rebuilt `:architecture-processor:jar`; forced
`KmpConventionPluginsFunctionalTest` and `ArchitectureCheckPluginFunctionalTest` with the external
processor JAR; strict `architectureCheck` twice with `--configuration-cache
--configuration-cache-problems=fail --no-parallel` and reuse on the second run; `spotlessApply`,
separate `spotlessCheck` and `detekt`; strict `openspec validate feature-first-modularization
--strict`; scoped `git diff --check`; and the iOS Simulator Swift-consumer build with
`xcodebuild`. Xcode version is 26.6 (build 17F113).

The following additional focused GREEN commands passed after the artwork-rendering and pointer-input
test additions:

```text
./gradlew :feature:nowplaying:jvmTest --tests '*NowPlayingArtworkRenderingJvmTest' \
  --tests '*NowPlayingContentSemanticsJvmTest' --tests '*NowPlayingContractsTest' --configuration-cache
./gradlew :core:ui:jvmTest --tests '*VerticalSheetGestureJvmTest' --configuration-cache
./gradlew :feature:nowplaying:jvmTest :core:ui:jvmTest :shared:jvmTest --configuration-cache
```

Retained XML evidence for the focused cases reports `NowPlayingArtworkRenderingJvmTest` 12/12,
`NowPlayingContentSemanticsJvmTest` 3/3, `NowPlayingBarSemanticsJvmTest` 3/3,
`VerticalSheetGestureJvmTest` 11/11,
`Task3ReviewSemanticsJvmTest` 7/7, and `PlaylistEditModeSemanticsJvmTest` 12/12, each with
zero failures, errors, and skips. The feature common test XML also contains the moved scrubber,
contract, bottom-bar, adaptive-layout, and artwork suites; Android-host and iOS-simulator XML
exists for the cross-target common suites. The feature semantics XML specifically includes
`staleMeasuredNowPlayingBarExposesNoActionsAndDispatchesNoPointerOrGestureCallbacks`.

The exact forced gesture command was run after the final fixture correction:

```text
./gradlew :core:ui:jvmTest --tests '*VerticalSheetGestureJvmTest' --rerun-tasks --no-configuration-cache
```

It passed with 11 real Compose pointer-input cases. The fixture exposes the modifier's live
`Animatable`, and every case waits for Compose idle before asserting its settled value:

- ordinary upward/downward terminal drags settle to `1f`/`0f` and invoke the terminal callback
  exactly once;
- opposite drags safely beyond slop start from interior progress (`0.2f` upward and `0.8f`
  downward), then fail closed at `0f` upward and `1f` downward with zero callbacks; these
  assertions therefore prove an endpoint transition rather than accepting an unchanged endpoint;
- inactive input preserves both the original `.42f` progress and zero callbacks;
- equality at `0.3f` proves upward uses `>=` (settles at `1f`, callback once) and downward uses
  `<` (settles at `1f`, callback zero);
- the same short drag with `referenceHeight = 50f` settles terminal at `1f`, while
  `referenceHeight = null` uses measured-height fallback and settles at `0f` with zero callback;
- recognised cancellation is produced by a nested real competing pointer-input consumer after the
  drag recognizer accepts movement. The preserved approved cancellation contract is terminal-side
  upward cancellation from `0f` -> `1f`/one callback, and non-terminal downward cancellation from
  interior `0.8f` -> `1f`/zero callback. The interior starting state makes the downward assertion
  fail closed if the cancellation branch is removed or becomes a no-op. The downward cancellation
  branch required a production root-cause correction: it now settles to its direction-specific
  non-terminal endpoint rather than using upward-only logic.

The forced XML result for this final fixture snapshot reports exactly `tests="11" skipped="0"
failures="0" errors="0"`; all 11 named testcases executed.

The corrected snapshot also passed `:core:ui:jvmTest :feature:nowplaying:jvmTest :shared:jvmTest`
and the nine-task consumer/platform command. One initial combined shared-suite run failed only in
the unrelated timing-sensitive `PlaybackSessionCoordinatorTest.newerPlayingProgressSurvivesDelayedMutationCheckpoint`;
its isolated forced rerun passed, and the succeeding combined suite passed.

## Architecture, resource, KSP, and KDoc evidence

The forced convention fixture executes the real feature implementation convention with the
repository Android-KMP model plus JVM, iosArm64, and iosSimulatorArm64 targets. It asserts
`KSP_MODULE=:feature:nowplaying`, normalized package roots
`com.eterocell.rhythhaus.nowplaying,com.eterocell.rhythhaus.ui`, exact normalized production source
roots, four non-metadata KSP configurations including `kspAndroid`, four registry registrations
including `:feature:nowplaying|kspAndroid|:architecture-processor`, and `EXPLICIT_API=null`. The
same external processor JAR fixture executes real `kspKotlinJvm` with authored mutations: an
outside-root production declaration fails with exact
`ARCH-PACKAGE :feature:nowplaying:InvalidFeature.kt (outside.feature)`, and an undocumented public
declaration fails with exact `ARCH-KDOC :feature:nowplaying:InvalidFeature.kt:2
(com.eterocell.rhythhaus.nowplaying.MissingFeatureKDoc)`. Both require that KSP executes rather
than reporting `SKIPPED` or `NO-SOURCE`. The architecture fixture executes positive
`:shared -> :feature:nowplaying` and feature -> `:core:playback`/`:core:ui` edges, resource
ownership/namespace records, and forbidden shared/taglib/Library/app/core-model/playlists
implementation edges plus iOS export mutations with required `ARCH-EDGE`, `ARCH-RESOURCE`, and
`ARCH-IOS-EXPORT` diagnostics; cycle diagnostics consequential to forbidden edges are tolerated
only alongside the required line. The fixture also covers invalid resource namespace and
production-root policy inputs. The real
feature source declares production package roots `com.eterocell.rhythhaus.nowplaying` and
`com.eterocell.rhythhaus.ui`, Android namespace `com.eterocell.rhythhaus.nowplaying`, and Compose
namespace `rhythhaus.feature.nowplaying.generated.resources`. Feature KSP validates public
declaration KDoc. Resource generation/processing and packaging passed, and shared retains reused
keys including `album_artwork` and `track_artist_album_format`.

## Self-review

- Confirmed worktree root was `/Users/eterocell/Git/self/RhythHaus/.worktrees/feature-first-modularization`.
- Confirmed required baseline SHA before partial implementation began.
- Reconciliation on 2026-08-04 confirmed the worktree root, task brief, repository instructions,
  current full tracked/untracked diff, and accepted absent-module RED.
- Restored all five generic Library adaptive-layout tests and removed only the five Now Playing
  adaptive tests from shared; retained shared shell-policy assertions and the authorized playlist
  import/tag adjustment.
- Repaired cross-module backdrop signatures to use opaque `RhythHausBackdrop`, injected shared
  labels/artwork loaders at the feature seam, and split direct feature bar semantics coverage.
- The full combined build was rerun with a 15-minute timeout and passed.
- `NowPlayingArtworkRenderingJvmTest.kt` and `NowPlayingContractsTest.kt` exercise observable
  rendered artwork behavior and public immutable presentation contracts without widening the public API.
- `LibraryDetailContent.kt` was inspected as an approved inventory path and already used the
  core-ui backdrop API; it required no Task 5.1 edit. `ArtworkImage.kt` was likewise an
  inspected-but-unchanged approved path. Neither was included in the changed/staged 38-path
  implementation commit.
- The complete gesture matrix is contained in `VerticalSheetGestureJvmTest.kt`; it does not use a
  copied policy seam. Its cancellation cases use a competing real pointer-input consumer to reach
  `detectVerticalDragGestures` cancellation after slop recognition.
- Final corrected-snapshot gates passed: focused forced gesture execution, affected JVM suites,
  the prescribed nine-task consumer/platform command, `spotlessApply`, `spotlessCheck`, `detekt`,
  `openspec validate feature-first-modularization --strict`, `git diff --check`, and
  `xcodebuild` iOS Simulator Debug build. Xcode was rerun because core UI source is consumed by
  the shared Apple framework.

## Deferrals and blocker

No unrelated implementation blocker remained after final independent scope and behavior approval.
Runtime UI/playback, desktop runtime launch, Android/iOS device runtime, and `./init.sh` evidence
remain intentionally deferred because they were not run. This documentation closeout does not stage
or amend implementation commit `28dd2e1`.

Android namespace evidence remains at the real Android-KMP configuration/build layer; no invented
`ARCH-*` Android-namespace rule was added. Package-root and KDoc mutations are now exercised by the
real external-processor fixture above. No unresolved correction-lane blocker remains.

## Commit boundary

Historical correction-lane snapshot: no implementation boundary had yet been committed. Final
independent approval subsequently accepted implementation commit `28dd2e1`.

## Architecture fixture correction lane

The synthetic `nowPlayingFixture()` in
`ArchitectureCheckPluginFunctionalTest.kt` was replaced by a real feature implementation fixture.
It applies `build-logic.kmp.feature.impl`, `build-logic.android.kmp.library`, and
`build-logic.compose-resources`; configures Android-KMP, JVM, iosArm64, and iosSimulatorArm64;
uses Android namespace `com.eterocell.rhythhaus.nowplaying` and Compose resource namespace
`rhythhaus.feature.nowplaying.generated.resources`; and creates documented production declarations
under both approved package roots. The feature resource record is now produced by the controlled
resource convention. It is no longer manually published by fixture code.

The fixture consumes the externally supplied prebuilt architecture processor JAR through its
fixture `:architecture-processor` project. Its positive case runs
`:feature:nowplaying:compileKotlinJvm` and `architectureCheck`, verifies `kspKotlinJvm` is present
and neither `SKIPPED` nor `NO-SOURCE`, and verifies the real convention-derived commonMain Compose
resource record. Controlled mutations retain required diagnostics for forbidden edges, invalid
resource namespace, and iOS export. Package-root and public-KDoc mutations compile through the
real processor and require `ARCH-PACKAGE` and `ARCH-KDOC` respectively.

RED evidence: before this replacement, the synthetic fixture used `kmpModule(..., strict = false)`
and manual `ArchitectureModelRegistry.publishResources`; its positive architecture assertion passed
without compiling feature production declarations or executing feature KSP. The real-fixture
conversion initially failed on isolated generated-fixture setup defects (processor default
configuration, Compose plugin/runtime classpath, valid resource XML, explicit-API core-ui fixture,
and a configuration-cache-unsafe observation task). Those setup defects were repaired without
changing production conventions or architecture policy.

GREEN evidence, run from the Task 5.1 worktree:

```text
./gradlew :architecture-processor:jar --rerun-tasks --no-configuration-cache
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test \
  --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache \
  -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Both commands passed. Retained XML reports `ArchitectureCheckPluginFunctionalTest` with
`tests="64" skipped="0" failures="0" errors="0"`. Root `architectureCheck` then passed twice
with `--configuration-cache --configuration-cache-problems=fail --no-parallel`; the second run
reported `Configuration cache entry reused.` `spotlessApply`, separate `spotlessCheck`, separate
`detekt`, `openspec validate feature-first-modularization --strict`, and `git diff --check` passed.

Final independent scope and behavior approval closed implementation review. Commit `28dd2e1`
contains the atomic implementation; this ignored evidence report is updated only by the separate
documentation closeout.

## Superseded Task 5.1 UI Evidence

The following UI evidence was superseded by the current re-review record below. It is retained
only as historical context; the current XML counts, scope, and cancellation limitation are in the
`Task 5.1 UI Re-review` section.

This correction lane was performed from worktree
`/Users/eterocell/Git/self/RhythHaus/.worktrees/feature-first-modularization` against
`HEAD b0b416d366dd478909f04bb91d297864e62d55ca`. The approved Task 5.1 brief and plan were
read before editing. Only feature/shared Now Playing production and directly related feature/shared
tests were edited, plus this ignored report. No build-logic, Gradle definition/catalog/settings,
architecture policy/fixtures, canonical docs/OpenSpec/progress/roadmap, staging, or commit changes
were made by this lane. Architecture fixture completion is not claimed.

### Correction scope

- Restored the expanded feature Now Playing implementation from the `HEAD` shared behavior:
  compact and split adaptive layouts, safe-content padding, artwork/accent/card rendering, track
  number, bounded progress (`positionMillis.coerceIn(0L, durationMillis)`), status/loading/
  buffering/error labels, shuffle/repeat, transport controls, overflow bounds, and left-edge Back
  callback wiring. The shared facade remains the compatibility/routing/composition owner and injects
  the existing labels and artwork loader.
- Restored the feature mini-player behavior: eager artwork bytes win, lazy loading is used only when
  eager bytes are absent, failures retain ordinary fallback/accent behavior, artwork content labels,
  progress and safe/nav padding are preserved, and the upward-only expansion adapter emits only
  `onExpand`. The shared-owned progress `Animatable` and `interactive=false` suppression remain
  intact.
- Kept lazy artwork state private in production. The obsolete white-box artwork-state test was
  removed rather than widening production visibility; characterization is through public rendered
  composables and existing immutable/pure contracts.
- Repaired direct Task 3.1 semantic mappings for unmeasured, stale, and matching Now Playing
  presentations; tightened mini-player tests to physically attempt disabled controls/gesture and to
  invoke enabled controls with exact callback assertions; removed the vacuous playlist mismatch
  against `now-playing-bar`.
- Added focused expanded-content JVM characterization for error/status rendering and distinct
  shuffle/repeat/transport behavior. Compact/split adaptive selection and progress bound behavior
  remain covered by the focused feature tests. A first expanded surface attempt went RED on a
  desktop test-host merged-semantics limitation for track-number lookup; it was corrected to use
  the stable observable control surface without changing production semantics.

### RED/GREEN evidence

Initial correction RED evidence:

```text
./gradlew :feature:nowplaying:jvmTest --tests '*NowPlayingContentSemanticsJvmTest' \
  --rerun-tasks --no-configuration-cache
```

The first attempt reached composition but failed closed because the desktop host could not expose
the track-number text through the merged Miuix semantics tree. The subsequent control-surface test
also initially failed closed because the custom mode buttons do not expose child icon descriptions
as selectable nodes in that host. The final test uses the existing clickable-node surface and then
passed; no production test-only tag or visibility widening was introduced.

Final focused GREEN:

```text
./gradlew :feature:nowplaying:jvmTest :shared:jvmTest --configuration-cache
```

Result: `BUILD SUCCESSFUL`.

Final feature/platform GREEN:

```text
./gradlew :feature:nowplaying:jvmTest :feature:nowplaying:testAndroidHostTest \
  :feature:nowplaying:iosSimulatorArm64Test :feature:nowplaying:compileAndroidMain \
  :feature:nowplaying:compileKotlinIosSimulatorArm64 :core:ui:compileAndroidMain \
  :core:ui:compileKotlinIosSimulatorArm64 --configuration-cache
```

Result: `BUILD SUCCESSFUL` (`141 actionable tasks: 13 executed, 128 up-to-date`). The requested
`compileKotlinAndroid` spelling does not exist in this repository; the actual feature task is
`compileAndroidMain`, which was run successfully. Android host tests, iOS simulator tests, and
feature/core Android+iOS compilation all passed.

Final quality gates:

```text
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
git diff --check
```

All passed. `detekt` reported `NO-SOURCE` for the feature/shared modules and exited successfully.
The final `git diff --check` passed.

Strict OpenSpec validation was run as:

```text
openspec validate --all --strict
```

Result: `44 passed, 1 failed (45 items)`. The single failure is the unrelated pre-existing
`spec/ios-now-playing-info` item; its exact diagnostic was `Details: openspec validate
ios-now-playing-info --type spec`. No OpenSpec artifact was changed.

### Final XML counts

The final affected JVM XML reports contain zero skipped tests, failures, or errors:

```text
NowPlayingBarSemanticsJvmTest       tests="3"  skipped="0" failures="0" errors="0"
NowPlayingContentSemanticsJvmTest  tests="1"  skipped="0" failures="0" errors="0"
NowPlayingContractsTest             tests="4"  skipped="0" failures="0" errors="0"
Task3ReviewSemanticsJvmTest         tests="10" skipped="0" failures="0" errors="0"
PlaylistEditModeSemanticsJvmTest    tests="12" skipped="0" failures="0" errors="0"
```

The shared Task 3 and playlist XML was produced by the preceding focused JVM run and remained
green in the final combined `:feature:nowplaying:jvmTest :shared:jvmTest` verification. Existing
adaptive-layout and scrubber XML suites are included in the feature JVM run and passed.

### Boundary confirmation

Historical pre-approval snapshot: the worktree was unstaged and uncommitted. `git diff --cached
--quiet` returned success (`cached_status=0`), and unrelated modularization changes remained
untouched. Final independent review subsequently approved and committed the implementation as
`28dd2e1`.

## Task 5.1 UI Re-review

Scope: only feature Now Playing production/tests, the shared shell placement boundary/tests, and
this ignored report. No build logic, Gradle configuration, architecture fixtures, canonical docs,
staging, or commits were changed by this UI lane. Architecture fixture completion is not claimed.

Both private artwork owners synchronously key and initialize state from track ID, a ByteArray
identity/content snapshot, and loader reference identity. Eager artwork initializes immediately and
does not invoke the loader. Lazy artwork requests the current track ID; `null` and ordinary
`Exception` use fallback; `CancellationException` is explicitly rethrown; obsolete completions are
rejected.

`NowPlayingArtworkRenderingJvmTest` is the observable JVM ownership for private artwork state.
Common test has no Compose rendering host that can
observe private state without widening production visibility. The JVM suite covers both bar and
expanded paths for eager/no-loader, lazy exact-ID success, null and ordinary-failure fallback,
track/eager/loader identity resets before deferred replacement completion, and stale deferred
completion rejection. It also now proves exact cancellation identity for both paths: each injected
loader registers `currentCoroutineContext().job.invokeOnCompletion`, stores its completion cause in
an `AtomicReference`, throws one sentinel `CancellationException`, waits for completion, and uses
`assertSame` on the sentinel and observed cause. This observes the private production
`LaunchedEffect` job without widening production visibility.

Expanded content has internal-only stable tags for compact/split branches, root, transport/mode
controls, progress, metadata, and status. Its JVM test asserts one node per tagged surface, exact
control effects, one left-edge Back callback, track/title/artist-album rendering, normal/error
status anchors, and both physical progress endpoints. Artwork/fallback is covered by the rendering
suite.

`LibraryShellBottomBar` is the real shared branch used by `LibraryHomeScreen`. Its shared-owned
placement tag wraps the actual mini-player call. The playlist regression proves positive Now Playing
placement, then selection/edit placement absence while the shared selection bar and clearance remain;
the direct `PlaylistDetailScreen` clearance assertion is retained only as a detail-layout check.

RED/GREEN: Focused RED runs used `--rerun-tasks --no-configuration-cache`; initial test-only
failures were a missing `Track` import, a queue-fixture expectation (`first` vs actual `third`), and
an invalid expectation that valid eager artwork should fall back. The tests were corrected to the
preserved behavior without weakening production. The cancellation controlled RED intentionally
asserted `completionCause == null`; both paths failed with their exact sentinels (`bar artwork
sentinel` and `expanded artwork sentinel`). Final focused GREEN:

```text
./gradlew :feature:nowplaying:jvmTest --tests '*NowPlayingArtworkRenderingJvmTest' \
  --tests '*NowPlayingContentSemanticsJvmTest' :shared:jvmTest \
  --tests '*PlaylistEditModeSemanticsJvmTest' --rerun-tasks --no-configuration-cache
./gradlew :feature:nowplaying:jvmTest :shared:jvmTest --configuration-cache
```

Both passed. Final XML:

```text
NowPlayingArtworkRenderingJvmTest  tests="12" skipped="0" failures="0" errors="0"
NowPlayingContentSemanticsJvmTest  tests="3"  skipped="0" failures="0" errors="0"
PlaylistEditModeSemanticsJvmTest    tests="13" skipped="0" failures="0" errors="0"
```

The platform matrix passed after replacing JVM-only `System.identityHashCode` with portable hashing
while retaining reference equality:

```text
./gradlew :feature:nowplaying:testAndroidHostTest :feature:nowplaying:iosSimulatorArm64Test \
  :feature:nowplaying:compileAndroidMain :feature:nowplaying:compileKotlinIosSimulatorArm64 \
  :core:ui:compileAndroidMain :core:ui:compileKotlinIosSimulatorArm64 --configuration-cache
```

`spotlessApply`, separate `spotlessCheck`, separate `detekt`,
`openspec validate feature-first-modularization --strict`, and `git diff --check` passed. Detekt
reported `NO-SOURCE` for feature/shared. Named strict OpenSpec validation reported
`Change 'feature-first-modularization' is valid`. Historical pre-approval `git diff --cached
--quiet` returned success (`cached_status=0`); final independent review subsequently approved
implementation commit `28dd2e1`.

### Architecture Fixture Reconciliation

The architecture fixture now applies `build-logic.kmp.feature.impl`,
`build-logic.android.kmp.library`, and `build-logic.compose-resources`; configures Android-KMP,
JVM, `iosArm64`, and `iosSimulatorArm64`; creates documented production declarations under
`com.eterocell.rhythhaus.nowplaying` and `com.eterocell.rhythhaus.ui`; and supplies the repository-
built external processor JAR through `rhythhaus.architectureProcessorJar`. The feature resource
record is convention-derived; no feature `ResourceRecord` is manually published. The real
processor task executes for positive and mutation fixtures and is asserted neither skipped nor
no-source. Android namespace evidence remains at the Android-KMP configuration/build layer, with
no invented `ARCH-*` namespace diagnostic.

RED/GREEN evidence:

- RED baseline: the previous synthetic fixture passed without compiling feature production sources
  or executing feature production KSP.
- GREEN: real-root positive, outside-root package, undocumented-public-KDoc, invalid resource-
  namespace, and empty configured-package-root mutations all passed through real convention,
  processor, and architecture paths.
- `./gradlew :architecture-processor:jar --rerun-tasks --no-configuration-cache`: passed.
- Forced full `ArchitectureCheckPluginFunctionalTest` with
  `-Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"`,
  `--rerun-tasks --no-configuration-cache`: passed. Retained XML:
  `tests="65" skipped="0" failures="0" errors="0"`.
- Root `architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel`
  passed twice; both runs reported configuration-cache reuse.
- `spotlessApply`, separate `spotlessCheck`, separate `detekt`,
  `openspec validate feature-first-modularization --strict`, and `git diff --check`: passed.

The processor intentionally treats an empty configured source-root list as no production files
owned by that processor and has no source-root `ARCH-*` diagnostic. No unsupported source-root
diagnostic was invented; the fail-closed mutation is the empty package-root case above. This is
historical correction-lane evidence; final independent review subsequently approved `28dd2e1`.

### Nested KSP rerun reconciliation

The real Now Playing GradleRunner path now passes `--rerun-tasks` directly to its nested builds;
the outer JUnit invocation is not relied on for task execution. The positive fixture requires
`:feature:nowplaying:kspKotlinJvm` to finish with `TaskOutcome.SUCCESS`. Controlled
`ARCH-PACKAGE` and `ARCH-KDOC` processor failures require the same task to finish with
`TaskOutcome.FAILED`, which proves execution while preserving Gradle's correct failure semantics
for an intentionally failing processor. Exact required diagnostics remain asserted.

- RED baseline: nested Now Playing fixture arguments omitted `--rerun-tasks` and accepted any
  non-skipped/non-no-source KSP result.
- GREEN: forced processor JAR rebuild and focused real-root fixture passed with direct nested
  reruns; positive KSP is `SUCCESS`, while processor mutation KSP tasks are `FAILED` after emitting
  their required `ARCH-*` diagnostics.
- Re-ran forced full `ArchitectureCheckPluginFunctionalTest` with the absolute external processor
  JAR: passed. Retained XML remains `tests="65" skipped="0" failures="0" errors="0"`.
- Re-ran root strict-cache `architectureCheck` twice: passed with cache reuse reported on both
  invocations. Re-ran `spotlessApply`, separate `spotlessCheck`, separate `detekt`, strict OpenSpec
  validation, and `git diff --check`: passed.

## Oracle Rejection and Bounded Correction - 2026-08-04

The final Oracle review rejected the prior focused evidence because the forced
`NowPlayingContentSemanticsJvmTest` execution failed at line 89: it expected
`third` after Previous but observed `first`. The root cause was test ordering:
shuffle was enabled before Previous, so the controller's randomized shuffle order
made the transport expectation nondeterministic.

The bounded correction changed only
`feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt`.
Previous and Next now execute while shuffle remains Off and assert the deterministic
queue sequence `second -> first -> second`. Shuffle and repeat callbacks execute
after those transport assertions, so no randomized ordering is assumed. The
existing terminal assertions continue to prove play/pause dispatch, both scrub
boundaries, shuffle, repeat, and left-edge Back callbacks; no production code or
other test behavior was changed.

Fresh verification evidence:

```text
./gradlew :feature:nowplaying:jvmTest --tests 'com.eterocell.rhythhaus.nowplaying.NowPlayingContentSemanticsJvmTest' --rerun-tasks
```

PASS. All 3 methods in `NowPlayingContentSemanticsJvmTest` executed with
`tests="3" skipped="0" failures="0" errors="0"`.

```text
./gradlew :feature:nowplaying:jvmTest --tests 'com.eterocell.rhythhaus.nowplaying.NowPlayingContentSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.nowplaying.NowPlayingArtworkRenderingJvmTest' --tests 'com.eterocell.rhythhaus.nowplaying.NowPlayingBarSemanticsJvmTest' --rerun-tasks
```

PASS. Fresh XML counts are:

```text
NowPlayingContentSemanticsJvmTest   tests="3" skipped="0" failures="0" errors="0"
NowPlayingArtworkRenderingJvmTest  tests="12" skipped="0" failures="0" errors="0"
NowPlayingBarSemanticsJvmTest      tests="3" skipped="0" failures="0" errors="0"
```

`git diff --check` also passed. No files were staged or committed.

## Final Acceptance Reconciliation

Final independent scope and behavior approval accepted the implementation in `28dd2e1`
(`refactor: extract now playing feature`). The former nondeterministic control-test finding is
closed: the `NowPlayingContentSemanticsJvmTest` Previous/Next assertions now run while shuffle is
Off and prove the deterministic `second -> first -> second` sequence before shuffle/repeat callback
checks. Fresh focused XML is Content 3/3, Artwork Rendering 12/12, and Bar Semantics 3/3, all zero
skipped/failures/errors (18/18 total); no production behavior changed.

The accepted automation evidence is retained above: gesture XML 11/11; final architecture fixture
XML 65/65; twice-passed strict-cache root `architectureCheck` with reuse; Android-host and iOS
simulator feature tests; the feature/core Android+iOS compilation matrix (141 actionable tasks: 13
executed, 128 up-to-date); Xcode 26.6 (17F113); `spotlessApply`, separate `spotlessCheck`, separate
`detekt`, named strict `openspec validate feature-first-modularization --strict`, and `git diff
--check`. `openspec validate --all --strict` is not a passing claim: retained output is 44 passed,
1 failed because of unrelated pre-existing `spec/ios-now-playing-info`.

No runtime UI/playback, desktop launch, Android/iOS device/runtime validation, or `./init.sh`
evidence exists or is claimed. OpenSpec 4.4 remains open for those broader deferred validations.

## Independent Review Provenance

Scope reviewer session `ses_032e868eeffei1GpxKbH327EB1` returned `PASS` against baseline
`96cb487`, authorizing the exact implementation staging set after the required plan amendments.
Behavioral reviewer session `ses_0328e9e86ffeRFAqPeTnpxV5pX` first returned `REJECT` for the
nondeterministic shuffle/Previous control test. After the test-only correction, it returned the
exact verdict `Findings: None. PASS / APPROVED`; it verified Content 3/3, Artwork 12/12, Bar 3/3,
`git diff --check`, and empty staging, then stated that the already scope-approved implementation
could be staged and committed. These reviews examined the pre-commit reviewed worktree snapshot,
not a post-commit SHA. The subsequently staged exact 38-path implementation scope became commit
`28dd2e1` (`refactor: extract now playing feature`).

## Scope-Authorization Note

Independent implementation review authorizes the actual JVM Compose rendering ownership in
`NowPlayingArtworkRenderingJvmTest.kt` and `NowPlayingContentSemanticsJvmTest.kt`; no white-box
artwork-state test is authorized. Artwork state remains private, while the rendering suite proves
bar and expanded eager/lazy/null/ordinary-failure behavior, exact cancellation instance through
`currentCoroutineContext().job.invokeOnCompletion`, synchronous track/eager-byte/loader-identity
resets, stale-result rejection, and artwork/fallback rendering. The content semantics suite owns
compact/split branches, stable tag/count identity, all transport/mode callbacks, left-edge Back,
bounded progress, and metadata/status. `NowPlayingBarSemanticsJvmTest.kt` ownership is unchanged.
