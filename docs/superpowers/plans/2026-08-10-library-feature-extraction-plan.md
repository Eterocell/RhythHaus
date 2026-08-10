# Library Feature Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development`.
> One implementation writer executes one TDD checkpoint at a time; an independent reviewer
> approves each checkpoint before the next begins.

**Authority:** the approved Library design, OpenSpec design/spec/tasks, architecture, ADR 0001,
and baseline `6983ce99a648a510388b20b996e9141665efb343`.

## Scope And Lifecycle

Create unexported Android-KMP/JVM/iOS `:feature:library:impl` with namespace
`com.eterocell.rhythhaus.library.impl` and resource package
`rhythhaus.feature.library.generated.resources`; no README or framework export. Public contracts and
factory remain `com.eterocell.rhythhaus.library`; internal scanner/source/metadata declarations are
separate `com.eterocell.rhythhaus.library.impl` files. No implementation starts before a plan-only
`docs: plan library feature extraction` commit, a SHA-bound ignored Task 6.1 brief, and a fail-closed
continuation gate. The SDD brief/controller supplies the planning SHA. At correction preparation,
current `HEAD`, the supplied planning SHA, and the brief SHA must be identical. This repair is plan-only;
after it is committed, the orchestrator rebinds the brief to that actual planning commit SHA before
implementation.

Planning changes are limited to this plan and the canonical planning pointer (two paths total).
This corrective amendment may be committed with only those two authorized plan paths; the existing
dirty correction snapshot, source/tests, and frozen report are neither staged nor rewritten. Before
review and staging, the index must be empty and the known dirty tracked `task-6.1-report.md` must be
recorded at its frozen SHA and prohibited from acceptance. Ignored brief/controller rebinding occurs
only after the amendment commit. The exact planning-commit path set is the two plans, while all
pre-existing dirty paths must remain byte-identical.
The frozen report SHA-256 is exactly
`2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5`; the existing correction
evidence-prefix SHA-256 is exactly
`d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0`. These values are immutable
historical evidence and must be checked, not regenerated from amended bytes.
After the plan-repair commit, pre-existing source/test/report dirt remains preserved while the ignored
brief and controller progress ledger perform their controller-owned transition; the brief contains
exactly one lowercase 40-character `Planning baseline: [0-9a-f]{40}` line equal to `git rev-parse
HEAD`. An empty index is mandatory; the ignored brief/ledger files are the only transition writes.
All implementation, test, report, progress, roadmap, OpenSpec, and other evidence paths are excluded
from the implementation manifest and from this plan-only commit.

## Manifest Gate

Derive, do not target, the literal A/M/D count from live package-consistent records. Every move is
old `D` plus new `A`; adaptations are `M`.

## Checkpoint 1: Governance RED

RED real architecture/TestKit mutations for absent module/targets, package roots, edges/cycles,
KDoc/`@param`/defaults/public surface, resources, holder, ABI, and Koin identity. Build the processor
JAR serially, assert one nonempty JAR/SPI/provider, then forced TestKit using
`-Drhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"`.
Restore one causal mutation at a time for GREEN; reviewer inspects XML counts.

## Checkpoint 2: Module, API, Holder

Register module/targets; delete only API `LibraryTrack.kt` conversion method/import and adapt
`LibraryApiModelsTest.kt`. Move holder D Shared/A core/M core database compatibility setter and
creation read. Preserve SQLDelight. RED is missing module/old API assertion; GREEN proves no API
`PlayableTrack`, one holder, and startup order.

The historical nine-path Checkpoint 2 commit `4943d76` remains immutable and is not accepted as a
complete checkpoint: deleting `LibraryTrack.toPlayableTrack()` while leaving its current consumers
uncompilable is a fail-closed rejection. The next action is the corrective checkpoint below, bound
immediately after `4943d76` (or explicitly rebound as the next checkpoint); it must not rewrite,
amend, or fold changes into that historical commit.

## Corrective Checkpoint 2A: Consumers And Canonical Android Holder

Run this checkpoint before any implementation-family move and accept it independently. Its literal,
mandatory correction path/status map is below. Every listed endpoint is required, including the
architecture functional test and `PlaylistFeatureDismissalTest.kt`; no conditional path exists.
The correction child commit is allowed to touch only these actual minimal endpoints:
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`,
`feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`,
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt`,
`core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt`,
`core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt`,
and `core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt`,
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`,
`feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt`,
and `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt`.
No other endpoint is permitted. `LibraryRouteAdapterJvmTest.kt` remains a Checkpoint 5 endpoint and
its creation and acceptance remain deferred.

Adding SearchRouteAdapterJvmTest extends the correction inventory from eleven to exactly twelve
records, so the correction inventory records and its post-amend correction digest must be recomputed
for the amended twelve-path map. The frozen historical six-line controller prefix and its recorded
`d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0` prefix SHA are not extended,
rewritten, or replaced; the new inventory is appended after that prefix and receives its own current
inventory evidence. A mismatch between the preserved prefix and the recomputed twelve-record
inventory fails closed.

The two authority states are deliberately distinct. **Pre-amend historical/live** means the ledger
still has the old eleven-row correction block (including its malformed 63-character digest); it is
historical evidence only and every positive correction or `RED_RECOVERY` gate MUST reject it.
**Post-amend/rebound** means the lifecycle owner has run the authorized rebind action below, which
preserves ledger lines 1-6 byte-for-byte and replaces/appends only the correction block with the
exact twelve rows from this map. The independent SHA-256 of those exact twelve newline-terminated
`M<TAB>path` rows at lines 106-117 is
`d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0`. The post-amend ledger
block range is marker-bounded from `Correction inventory: BEGIN` through `Correction inventory: END`
(currently lines 7-20); the runner hashes that complete marker range and records its literal expected
digest supplied by the amendment authority. It never permits a positive recovery against the old
eleven-row ledger.

This correction-relative allowlist is separate from the final implementation manifest. The final
manifest remains exactly **A=49, M=26, D=34, total=109, unique=109**. The child correction must be
based on the repaired plan commit, have that commit as its sole parent, and pass a pre/post
allowlist diff check. The already-manifested architecture functional test endpoint(s) may prove
sole declaration/storage and Android startup order, but a core database test must never import
`androidApp`.

## Literal Corrective Checkpoint 2A Map

```text
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt
M	feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt
M	core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt
M	core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt
M	core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt
M	build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
M	feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt
```

`LibraryHomeContent.kt` is deliberately absent from 2A. Do not change it here. `PlaylistTrackBrowserOverlay`
intentionally retains `List<LibraryTrack>`; it neither needs `PlayableTrack` nor an unsafe cast or
browser API change. Only `PlaylistDetailScreen` and playback projection paths require direct
`PlayableTrack` / `playableTracksById`. Defer `LibraryHomeContent.kt`'s `Track`
callback redesign and its manifest D+A move to Checkpoints 4-5, where the feature UI move and
Shared adapter work are accepted together. Do not add endpoints or reconcile arithmetic by
inventing compatibility facades.

Before adapting stale callers, add the missing replacement in manifested
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt`:
`internal fun LibraryTrack.toPlayableTrack(): PlayableTrack`. Its projection must copy exactly
`id`, `title`, `artist`, `album`, `durationMillis`, `audioSource` to `source`, and `artworkBytes`,
with no duration conversion, fallback, or dropped artwork. Adapt callers only after this function
exists. There is no public API compatibility facade. Keep `MusicModels.kt`'s existing
`Track.toPlayableTrack()` unchanged. `PlaylistScreens` must consume an approved Shared-owned
`playableTracksById` projection (or an equivalent callback) and must not convert `LibraryTrack`;
any public-signature change must be both design-authorized and listed in this correction allowlist.
The projection preserves ID-keyed `associate` last-value behavior, source occurrence order, and
selected-occurrence callback semantics. App, PlaybackSessionCoordinator, LibraryRoutes, and the
already-existing LibraryAppShell remain the approved Shared conversion/projection adapters until
Checkpoint 5.

The Search adapter is the sole newly authorized 2A test adaptation. It uses Shared-owned internal
`LibraryTrack.toPlayableTrack` to assert Search-route projection fields; it makes no production
Search behavior/API change and exposes no public conversion seam.

The Android correction has one storage owner only:
`core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt`
is the sole declaration and storage owner. Its `applicationContext` property owns the sole field and
its setter stores `value.applicationContext`. `setLibraryDatabaseAndroidContext(context)` assigns
`LibraryDatabaseContext.applicationContext`; `createLibraryDatabase()` reads that property. Delete
the private duplicate holder from `LibraryDatabase.android.kt`, preserving package/public API and
factory behavior. Core host acceptance covers only direct-setter normalization and property/factory
identity. Existing manifested architecture/TestKit controls prove sole declaration/storage and
Android startup order before `setRhythHausAndroidContext` and Koin; no core test imports `androidApp`.
Preserve `RhythHausApplication` production source unchanged.

### Corrective RED, GREEN, And Acceptance Gate

Before correction, capture causal RED in this literal order. Record each command, exit code, exact
diagnostic, XML/log path, and ordering. A timeout hiding a known compile failure is a blocker and
must be reported as a compile failure, never as an optional timeout:

```zsh
./gradlew :feature:playlists:impl:compileKotlinJvm --rerun-tasks --no-parallel 2>&1 | tee "$TMPDIR/library-2a-playlist-red.log"; red_rc=${pipestatus[1]}; print "exit=$red_rc log=$TMPDIR/library-2a-playlist-red.log"; exit "$red_rc"
```

This first selector must fail on PlaylistScreens' unresolved deleted `LibraryTrack.toPlayableTrack`
conversion. It does not claim to reach Shared. Next prepare only the minimum playlist public-signature
or callback projection adaptation plus the Shared replacement conversion seam, while leaving App,
PlaybackSessionCoordinator, and LibraryRoutes stale. Then capture their independent Shared RED:

```zsh
./gradlew :shared:compileKotlinJvm --rerun-tasks --no-parallel 2>&1 | tee "$TMPDIR/library-2a-shared-red.log"; red_rc=${pipestatus[1]}; print "exit=$red_rc log=$TMPDIR/library-2a-shared-red.log"; exit "$red_rc"
```

The second selector must fail on the exact unresolved conversions in App,
PlaybackSessionCoordinator, and LibraryRoutes. Do not claim the old host-class selector as RED.
Before adding the new direct-setter host test, record the current host baseline only as observation
if run; it is not a RED claim. Then add the direct-setter host test as a temporary test-first file,
record its exact uncommitted `git status --short`, and run that new selector alone expecting RED:

```zsh
./gradlew :core:database:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest.directSetterNormalizesApplicationContext' --rerun-tasks --no-parallel 2>&1 | tee "$TMPDIR/library-2a-direct-setter-red.log"; red_rc=${pipestatus[1]}; print "exit=$red_rc log=$TMPDIR/library-2a-direct-setter-red.log"; exit "$red_rc"
```

The test-first file is explicitly uncommitted only between its creation and this RED capture; the
plan must not pretend its baseline selector failed. Also require the API negative control
`! rg -n 'toPlayableTrack' feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryTrack.kt`.
Add two named test-first architecture/TestKit controls in the already-manifested
`ArchitectureCheckPluginFunctionalTest` before holder production GREEN. The first, named
`libraryDatabaseHolderRejectsDuplicateStorageAndFactoryIdentityThenRestoresGreen`, mutates the
fixture to declare a second `LibraryDatabaseContext` storage field or private
`LibraryDatabaseAndroidContextHolder`, then mutates factory input away from
`LibraryDatabaseContext.applicationContext`; it must assert exact `ARCH-LIBRARY-HOLDER` diagnostics
for duplicate storage and factory identity, restore each mutation, and prove GREEN. The second,
named `libraryAndroidStartupOrderingRejectsDatabaseAfterSharedOrKoinThenRestoresGreen`, mutates the
fixture `RhythHausApplication.onCreate` ordering to place database context after Shared Android
context and after Koin; it must assert exact `ARCH-LIBRARY-ANDROID-STARTUP` ordering diagnostics,
restore each order, and prove GREEN. Neither test imports `androidApp`, adds an app/core dependency,
or changes `RhythHausApplication`. Build the processor JAR and run these exact focused selectors:

```zsh
./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.libraryDatabaseHolderRejectsDuplicateStorageAndFactoryIdentityThenRestoresGreen' --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.libraryAndroidStartupOrderingRejectsDatabaseAfterSharedOrKoinThenRestoresGreen' -Drhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-configuration-cache --no-parallel
```

Each focused architecture selector's own XML must report exactly 1/0/0/0; the combined report is
2/0/0/0. After correction, run exact
existing-or-test-first-created selectors; route-adapter creation is deferred to Checkpoint 5:

```zsh
./gradlew :shared:compileKotlinJvm :feature:playlists:impl:compileKotlinJvm --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaylistLifecycleIntegrationJvmTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:playlists:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissalTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.SearchRouteAdapterJvmTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :core:database:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest.contextSetterNormalizesAndDatabaseFactoryReadsTheHolder' --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest.directSetterNormalizesApplicationContext' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :shared:compileAndroidMain :feature:playlists:impl:compileAndroidMain :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :feature:playlists:impl:compileKotlinIosArm64 :feature:playlists:impl:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Each test task's XML must be inspected separately by its task path; no aggregate or cross-task XML
count may mask a missing selector. The lifecycle selector must report its existing method count with
zero skipped/failures/errors; `PlaylistFeatureDismissalTest` must report its four existing methods
as exactly 4/0/0/0; the Search adapter selector must report its named test-first method with zero
skipped/failures/errors; the two holder methods must each report one executed and zero
skipped/failures/errors; and the architecture selector must report 2/0/0/0. Missing selectors,
timeouts, or a compile task that does not reach its named module are blockers. Report exact output
and XML/log paths; timeout is never optional.
Review and accept Checkpoint 2A only after these counts and all required compile tasks pass.

### Corrective Commit Lifecycle And Evidence Chronology

Before implementation, verify exactly one baseline line, verify `git diff --cached --name-only` is empty, and record the
frozen pre-amend SHA-256 of the dirty tracked
`.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md`. That report is not
an allowlisted correction endpoint, must not be staged, and cannot supply acceptance evidence.
After committing this plan repair, bind the brief and controller to the actual plan commit SHA before
any corrective edit. The single binding rule is: correction requires the supplied planning SHA ==
`HEAD` == brief SHA.
The correction pre-gate requires: `HEAD` equals the rebound brief SHA; the correction child starts
with an empty index; only ignored brief/ledger lifecycle exceptions plus the frozen dirty report
exist; and the dirty report's working-tree SHA still equals the frozen pre-amend value. Otherwise
stop as blocked.

Append, never rewrite, the ignored controller-ledger chronology in this exact causal order: current
two-module compile RED with command/exit/diagnostic/log; direct-setter test creation status; its
single-selector RED with command/exit/diagnostic/XML-or-log; production GREEN; each focused test
task's XML; platform/startup compilation; scope/hash/status/diff review; reviewer decision. Record
timeouts as blockers. Do not use the pre-existing report bytes as a mutable acceptance ledger unless
an owner explicitly moves it into a separately authorized evidence lane.

Immediately before committing the correction child, assert its staged path set equals the
correction-relative allowlist subset actually used, its parent is the repaired plan SHA, the index
contains no report/brief/ledger exception, `git diff --cached --check` passes, and the frozen report
hash is unchanged. Immediately after commit, assert `git rev-parse HEAD^` equals the repaired plan
SHA; compare `git diff --name-only HEAD^ HEAD` to the same allowlist subset; verify
`git diff --check HEAD^ HEAD`; verify an empty index; record `git status --short`; and verify the
tracked report remains dirty at its frozen hash. The final 109-path manifest gate remains untouched
and is not applied to this corrective child.

## Checkpoint 3: Implementation Families

Split public scanner/progress/picker/source contracts and factory into `library` files; place
`PlatformAudioScanner`, events, source scanner helpers, metadata expect/actuals, PathResolver common/
actuals, and internal supported-audio helpers in `library.impl` files. Move scanner/repository/platform
families atomically. D+A implementation tests: artwork lazy loading, repository contract, models,
scanner, collapse, browser, `ArtworkImageTest` renamed `TrackArtworkImageTest`, and SQLDelight JVM.
RED/GREEN covers cancellation/errors/cleanup, picker cases, repository, Koin identity, and targets.

## Checkpoint 4: UI And Resources

Move leaf UI including `LibraryDetailContent.kt` with literal public `DrillDownView`, create feature
`TrackArtworkImage.kt`, delete Shared wrapper, and move exact authority 21 keys through
`values/strings.xml` and `values-zh/strings.xml`. Deterministically test ownership, grouping/order/
duplicates, state, artwork stale/cancellation, loader identity, and spacer.

## Checkpoint 5: Shared Adapters

Checkpoint 5 relinquishes only the consumer adaptations moved into 2A: App, PlaybackSessionCoordinator,
LibraryRoutes, PlaylistScreens, and PlaylistLifecycleIntegrationJvmTest. It retains the Home callback
redesign and LibraryHomeContent D+A move, LibraryRouteAdapterJvmTest creation and acceptance, and the
remaining Shared adapter review. Only Shared `App.kt`, `PlaybackSessionCoordinator.kt`,
`LibraryAppShell.kt`, and `LibraryRoutes.kt` convert/project `LibraryTrack`; `LibraryHomeContent.kt`
returns `Track`; playlist `PlaylistScreens.kt` consumes `playableTracksById`; `MusicModels.kt` remains
unchanged. Add
`LibraryRouteAdapterJvmTest` methods `routeProjectionPreservesPlayableFieldsAndArtworkBytes`,
`routeProjectionUsesIdMapWithoutChangingOccurrenceOrder`, `routeProjectionPreservesSelectedOccurrence`,
`playEntryFailureDoesNotSettle`, and `playEntrySettlementSettlesExactlyOnce`. Adapt only live callers;
retain navigation/state/Back/shell/playback-selection tests.

## Checkpoint 6: Verification And Closeout

## Literal Implementation Manifest

The implementation staging ledger is this fenced record set only. Every record is
`A|M|D<TAB>repository-relative-path`; the 109 literal records are unique and totals are
**A=49, M=26, D=34, total=109, unique=109**.
The controller parses this block, rejects duplicate/status/path drift, and derives these totals from
records; it must stop if the parser produces a different result. Moves are old D plus new A.

```text
M	feature/library/impl/build.gradle.kts
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryImplementationModule.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/impl/PlatformAudioScanner.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/impl/PlatformSourceAccess.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/impl/PathResolver.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/impl/AudioMetadata.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/TrackArtworkImage.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt
A	feature/library/impl/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.android.kt
A	feature/library/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.jvm.kt
A	feature/library/impl/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.ios.kt
A	feature/library/impl/src/androidMain/kotlin/com/eterocell/rhythhaus/library/impl/PlatformSourceAccess.android.kt
A	feature/library/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/impl/PlatformSourceAccess.jvm.kt
A	feature/library/impl/src/iosMain/kotlin/com/eterocell/rhythhaus/library/impl/PlatformSourceAccess.ios.kt
A	feature/library/impl/src/androidMain/kotlin/com/eterocell/rhythhaus/library/impl/PathResolver.android.kt
A	feature/library/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/impl/PathResolver.jvm.kt
A	feature/library/impl/src/iosMain/kotlin/com/eterocell/rhythhaus/library/impl/PathResolver.ios.kt
A	feature/library/impl/src/androidMain/kotlin/com/eterocell/rhythhaus/library/impl/AudioMetadata.android.kt
A	feature/library/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/impl/AudioMetadata.jvm.kt
A	feature/library/impl/src/iosMain/kotlin/com/eterocell/rhythhaus/library/impl/AudioMetadata.ios.kt
A	feature/library/impl/src/commonMain/composeResources/values/strings.xml
A	feature/library/impl/src/commonMain/composeResources/values-zh/strings.xml
A	feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt
A	feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt
A	feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/TrackArtworkImageTest.kt
A	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRouteAdapterJvmTest.kt
A	feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/impl/SupportedAudio.kt
A	feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ArtworkLazyLoadingTest.kt
A	feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryModelsTest.kt
A	feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowserTest.kt
A	feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt
A	feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt
A	feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/LibraryKoinIdentityTest.kt
A	feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/LibraryResourceOwnershipJvmTest.kt
A	feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContentJvmTest.kt
A	feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/DrillDownViewJvmTest.kt
A	feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/TrackArtworkImageJvmTest.kt
A	feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccessJvmTest.kt
A	feature/library/impl/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccessAndroidTest.kt
A	feature/library/impl/src/iosTest/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccessIosTest.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt
M	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt
M	feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt
M	core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt
M	core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt
M	shared/src/commonMain/composeResources/values/strings.xml
M	shared/src/commonMain/composeResources/values-zh/strings.xml
M	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt
M	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt
M	architecture-processor/src/main/kotlin/com/eterocell/rhythhaus/architecture/ArchitectureProcessorProvider.kt
M	build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
M	core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt
M	feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/AppDispatcherJvmTest.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/TrackSelectionSemanticsJvmTest.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt
M	shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlatformPlaybackEngineFactory.ios.kt
M	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt
M	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PathResolver.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SupportedAudio.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryImplementationModule.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ImportLabels.kt
D	shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.android.kt
D	shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.jvm.kt
D	shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.ios.kt
D	shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt
D	shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.jvm.kt
D	shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.ios.kt
D	shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PathResolver.android.kt
D	shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PathResolver.jvm.kt
D	shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PathResolver.ios.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowser.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
D	shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt
D	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt
D	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt
D	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ArtworkLazyLoadingTest.kt
D	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/ArtworkImageTest.kt
D	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryModelsTest.kt
D	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowserTest.kt
D	shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt
D	shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt
```

Run-only, excluded: `LibraryAppState.kt`, `MusicModels.kt`, `LibraryNavigationTest.kt`,
`TrackSelectionStateTest.kt`, `HomeSelectionPoliciesJvmTest.kt`, `PlaylistBackPolicyJvmTest.kt`,
`Task3ReviewSemanticsJvmTest.kt`, `RhythHausDiFactoryJvmTest.kt`, Search/Settings adapters, core DB/UI,
and unchanged platform/app consumers.

## Exact Resource Mutation Ledger

```text
shared/src/commonMain/composeResources/values/strings.xml: delete selected
shared/src/commonMain/composeResources/values-zh/strings.xml: delete selected
```

Move exactly 21 authority keys to the feature catalogs. `library_queue` and `album_artwork` remain
Shared-injected. Assert positive Shared ownership of `library_queue` and `album_artwork`; reject
`selected` in both Shared catalogs; reject `library_queue` and `album_artwork` in both feature
catalogs; assert EN/ZH parity and the 21 moved-key ownership with no duplicate keys across feature
and Shared catalogs; never use `values-zh-rCN`.

<!-- TASK-6.1-MANIFEST-PARSER:START -->
### Task 6.1 Authoritative Lifecycle And Proof

The controller owns only `.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md`.
The following self-contained zsh program is the sole parser and lifecycle authority. It has no parser
artifact: it extracts this literal block itself, parses porcelain only through NUL records, freezes a
deletion-aware SHA-256 inventory, and its `matrix` mode creates and destroys synthetic repositories.
`matrix` never writes the real repository or real index.

```zsh
emulate -L zsh
setopt nounset pipefail

readonly PLAN_REL='docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'
readonly LEDGER_REL='.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md'
readonly BRIEF_REL='.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md'
readonly REPORT_REL='.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md'
readonly CORRECTION_HEADING='## Literal Corrective Checkpoint 2A Map'
readonly HISTORICAL_CHECKPOINT_2_SHA='4943d76c22222c4beaf9b2eb229e33664116daa6'
readonly CLOSEOUT_PATHS=(
  "$BRIEF_REL"
  '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md'
  '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-final-acceptance-report.md'
  "$PLAN_REL"
  'openspec/changes/feature-first-modularization/tasks.md'
  'progress.md'
  'roadmap.md'
  "$LEDGER_REL"
)
typeset -grA CLOSEOUT_STATUS=(
  ["$BRIEF_REL"]='A'
  ['.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md']='M'
  ['.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-final-acceptance-report.md']='A'
  ["$PLAN_REL"]='M'
  ['openspec/changes/feature-first-modularization/tasks.md']='M'
  ['progress.md']='M'
  ['roadmap.md']='M'
  ["$LEDGER_REL"]='A'
)

die() { print -u2 -r -- "task-6.1 proof: $*"; exit 1; }
empty_index() { [[ -z "$(git diff --cached --name-only)" ]]; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }

# Extraction accepts the real shape: one heading, explanatory prose, then one text block. A second
# heading or text opener before the next Markdown heading is a duplicate marked block; later
# unrelated Markdown fences are outside this capture and are allowed.
extract_manifest() {
  local plan="${1:-$PLAN_REL}"
  awk '
    $0 == "## Literal Implementation Manifest" {
      headings++; if (headings != 1 || state != 0) bad=1; state=1; next
    }
    state == 1 && $0 == "```text" { opens++; if (opens != 1) bad=1; state=2; next }
    state == 1 && $0 ~ /^```/ { bad=1; next }
    state == 1 { next }
    state == 2 && $0 == "```" { closes++; if (closes != 1) bad=1; state=3; next }
    state == 2 { rows[++n]=$0; next }
    state == 3 && $0 == "```" { bad=1; next }
    state == 3 && $0 == "```text" { bad=1; next }
    state == 3 && $0 ~ /^#/ { state=4; next }
    state == 3 { next }
    END {
      if (headings != 1 || opens != 1 || (state != 3 && state != 4) || closes != 1 || n == 0 || bad) exit 1
      for (i=1; i<=n; i++) print rows[i]
    }
  ' "$plan"
}

read_manifest() {
  local plan="${1:-$PLAN_REL}" line record_kind endpoint a=0 m=0 d=0 manifest_count=0
  typeset -gA MANIFEST
  MANIFEST=()
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" == [AMD]$'\t'* ]] || die "malformed record"
    record_kind="${line[1]}"; endpoint="${line[3,-1]}"
    [[ -n "$endpoint" && "$endpoint" != /* && "$endpoint" != *'..'* && "$endpoint" != *$'\t'* ]] || die "unsafe path"
    (( ! ${+MANIFEST[$endpoint]} )) || die "duplicate path"
    MANIFEST[$endpoint]="$record_kind"
    case "$record_kind" in A) ((++a));; M) ((++m));; D) ((++d));; esac
  done < <(extract_manifest "$plan")
  manifest_count=${#MANIFEST[@]}
  (( a == 49 && m == 26 && d == 34 && manifest_count == 109 )) || die "counts are not A=49 M=26 D=34 total=109 unique=109"
}

extract_correction_map() {
  local plan="${1:-$PLAN_REL}"
  if [[ -n "${TASK_6_1_CORRECTION_FIXTURE-}" ]]; then print -r -- "$TASK_6_1_CORRECTION_FIXTURE"; return; fi
  awk -v heading="$CORRECTION_HEADING" '
    $0 == heading { headings++; state=1; next }
    state == 1 && $0 == "```text" { opens++; state=2; next }
    state == 1 && $0 ~ /^```/ { bad=1; next }
    state == 2 && $0 == "```" { closes++; state=3; next }
    state == 2 { rows[++n]=$0; next }
    state == 3 && $0 ~ /^#/ { state=4; next }
    state == 3 && $0 ~ /^```/ { bad=1; next }
    END { if (headings != 1 || opens != 1 || closes != 1 || n != 12 || bad) exit 1; for (i=1;i<=n;i++) print rows[i] }
  ' "$plan"
}

read_correction_map() {
  local line record_state endpoint
  typeset -gA CORRECTION
  CORRECTION=()
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" == M$'\t'* ]] || die "malformed corrective record"
    record_state="${line[1]}"; endpoint="${line[3,-1]}"
    [[ -n "$endpoint" && "$endpoint" != /* && "$endpoint" != *'..'* && "$endpoint" != *$'\t'* ]] || die "unsafe corrective path"
    (( ! ${+CORRECTION[$endpoint]} )) || die "duplicate corrective path"
    CORRECTION[$endpoint]="$record_state"
  done < <(extract_correction_map)
  (( ${#CORRECTION} == 12 )) || die 'corrective count is not 12'
}

# The -z stream is never line split. The fixture seam feeds real porcelain records through this
# exact parser; production always reads Git directly. Renames/conflicts/unknown combinations fail.
status_stream() {
  if [[ -n "${TASK_6_1_STATUS_FIXTURE-}" ]]; then print -rn -- "$TASK_6_1_STATUS_FIXTURE"$'\0'
  else git -c status.renames=false -c core.quotepath=false status --porcelain=v1 -z --untracked-files=all --no-renames
  fi
}
read_status() {
  typeset -gA STATUS
  STATUS=()
  local raw code endpoint normalized
  while IFS= read -r -d '' raw; do
    [[ ${#raw} -ge 4 && "${raw[3]}" == ' ' ]] || die "malformed porcelain"
    code="${raw[1,2]}"; endpoint="${raw[4,-1]}"
    case "$code" in
      '??') normalized=A;; ' M') normalized=M;; ' D') normalized=D;;
      'A ') normalized=A;; 'M ') normalized=M;; 'D ') normalized=D;;
      *) die "unsupported porcelain status [$code]";;
    esac
    (( ! ${+STATUS[$endpoint]} )) || die "duplicate porcelain path"
    STATUS[$endpoint]="$normalized"
  done < <(status_stream)
}

same_records() {
  local endpoint
  (( ${#MANIFEST} == ${#STATUS} )) || return 1
  for endpoint in "${(@k)MANIFEST}"; do [[ "${STATUS[$endpoint]-}" == "${MANIFEST[$endpoint]}" ]] || return 1; done
}

same_records_with_ledger() {
  local endpoint nonledger=0
  for endpoint in "${(@k)STATUS}"; do
    [[ "$endpoint" == "$LEDGER_REL" ]] && continue
    ((++nonledger)); [[ "${MANIFEST[$endpoint]-}" == "${STATUS[$endpoint]}" ]] || return 1
  done
  (( nonledger == ${#MANIFEST} ))
}

brief_sha() {
  local file="$1" count line
  count="$(awk '/^Planning baseline: [0-9a-f]{40}$/ {n++} END {print n+0}' "$file")"
  [[ "$count" == 1 ]] || return 1
  line="$(awk '/^Planning baseline: [0-9a-f]{40}$/ {print; exit}' "$file")"
  print -r -- "${line#Planning baseline: }"
}

check_baseline() {
  local planning_sha="$1" endpoint state
  [[ "$(brief_sha "$BRIEF_REL")" == "$planning_sha" ]] || die "brief SHA mismatch"
  for endpoint in "${(@k)MANIFEST}"; do
    state="${MANIFEST[$endpoint]}"
    if [[ "$state" == D ]]; then
      git cat-file -e "$planning_sha:$endpoint" 2>/dev/null || die "D absent at baseline: $endpoint"
    else
      if [[ "$state" == A ]]; then
        ! git cat-file -e "$planning_sha:$endpoint" 2>/dev/null || die "A present at baseline: $endpoint"
      else
        git cat-file -e "$planning_sha:$endpoint" 2>/dev/null || die "M absent at baseline: $endpoint"
      fi
    fi
  done
}

write_inventory() {
  local planning_sha="$1" endpoint state digest evidence
  [[ "$(brief_sha "$BRIEF_REL")" == "$planning_sha" ]] || die "brief SHA mismatch"
  mkdir -p -- "${LEDGER_REL:h}"
  [[ -f "$LEDGER_REL" ]] || print -r -- "Planning baseline: $planning_sha" > "$LEDGER_REL"
  [[ "$(grep -c '^Implementation inventory: BEGIN$' "$LEDGER_REL")" == 0 ]] || die 'inventory already frozen'
  {
    print -r -- 'Implementation inventory: BEGIN'
    for endpoint in "${(@k)MANIFEST}"; do
      state="${MANIFEST[$endpoint]}"
      if [[ "$state" == D ]]; then
        [[ ! -e "$endpoint" ]] || die "D remains present: $endpoint"
        digest=DELETED
      else
        [[ -f "$endpoint" ]] || die "missing nondeleted endpoint: $endpoint"
        digest="$(sha256 "$endpoint")"
      fi
      print -r -- "$endpoint"$'\t'"$state"$'\t'"$digest"
    done
    print -r -- 'Implementation inventory: END'
  } >> "$LEDGER_REL"
}

read_inventory() {
  local planning_sha="$1" require_live_report="${2:-0}" line endpoint state digest correction_state committed_cleanup_successor_sha='' simplified_successor_sha='' diagnostic_successor_sha='' warnings_successor_sha='' comprehensive_successor_sha='' multiline_successor_sha='' direct_successor_sha='' brace_successor_sha='' record_successor_sha='' manifest_successor_sha='' scope_successor_sha='' reconcile_successor_sha='' lineage begin=0 end=0 correction_begin=0 correction_end=0 sha_count=0 evidence_begin=0 evidence_end=0 report_count=0 correction_prefix_count=0 correction_map_count=0 library_blob_count=0 pointer_blob_count=0 committed_cleanup_successor_count=0 simplified_successor_count=0 diagnostic_successor_count=0 warnings_successor_count=0 comprehensive_successor_count=0 multiline_successor_count=0 direct_successor_count=0 brace_successor_count=0 record_successor_count=0 manifest_successor_count=0 scope_successor_count=0 reconcile_successor_count=0 frozen_begin=0 historical=1 rebound_inventory=0 failure_block=0
  typeset -gA FROZEN
  FROZEN=()
  typeset -g FROZEN_REPORT_SHA=''
  read_manifest
  read_correction_map
  [[ -f "$LEDGER_REL" ]] || die "missing controller ledger"
  validate_historical_prefix "$planning_sha"
  while IFS= read -r line || [[ -n "$line" ]]; do
    # Failure authority is opaque to this inventory grammar only while bounded by its exact opener
    # and closer. successor_failed_attempt_authority validates the literal record after this pass.
    if (( failure_block == 1 )); then
      [[ "$line" == 'Failed RED_RECOVERY attempt: END' ]] && { failure_block=0; continue; }
      [[ "$line" != 'Failed RED_RECOVERY attempt: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt 2: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt 2: END' ]] || die 'nested or unmatched failed-attempt boundary'
      continue
    elif (( failure_block == 2 )); then
      [[ "$line" == 'Failed RED_RECOVERY attempt 2: END' ]] && { failure_block=0; continue; }
      [[ "$line" != 'Failed RED_RECOVERY attempt: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt 2: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt: END' ]] || die 'nested or unmatched failed-attempt boundary'
      continue
    elif [[ "$line" == 'Failed RED_RECOVERY attempt: BEGIN' ]]; then
      failure_block=1; continue
    elif [[ "$line" == 'Failed RED_RECOVERY attempt 2: BEGIN' ]]; then
      failure_block=2; continue
    elif [[ "$line" == 'Stage: PRE_WORKTREE' || "$line" == 'Observed error:'* || "$line" == 'Primary error:'* || "$line" == 'Secondary error:'* || "$line" == 'No Gradle build client: PASS' || "$line" == 'No detached worktree: PASS' || "$line" == 'Cleanup outcome:'* || "$line" == 'Log:'* || "$line" == 'Summary:'* || "$line" == 'Outer summary:'* || "$line" == 'Fixture summary:'* || "$line" == 'Failed RED_RECOVERY attempt:'* || "$line" == 'Failed RED_RECOVERY attempt 2:'* ]]; then
      die 'orphan failed-attempt authority row'
    elif (( historical == 2 )); then
      if [[ "$line" == 'Correction inventory: BEGIN' ]]; then
        historical=0; ((++correction_begin)); (( correction_begin == 1 && correction_end == 0 )) || die 'malformed correction inventory opener'
      elif [[ "$line" == 'Frozen inventory: END' ]]; then
        die 'historical Frozen inventory is an open prefix; END is not permitted'
      elif [[ "$line" == 'Checkpoint 1 Governance RED: PASS / APPROVED' || "$line" == 'Checkpoint 2 Module, API, Holder: IN PROGRESS' || "$line" == 'Checkpoint 1 commits:'* || "$line" == 'Checkpoint 1 verification:'* || -z "$line" ]]; then
        :
      else
        die 'unrecognized historical controller-ledger content before correction boundary'
      fi
    elif (( historical )); then
      if [[ "$line" == Planning\ baseline:* ]]; then
        ((++sha_count)); [[ "$line" =~ '^Planning baseline: [0-9a-f]{40}$' ]] || die 'malformed historical planning line'
      elif [[ "$line" == 'Frozen inventory: BEGIN' ]]; then
        ((++frozen_begin)); (( frozen_begin == 1 )) || die 'duplicate historical Frozen inventory opener'
        historical=2
      elif [[ "$line" == 'Correction inventory: BEGIN' ]]; then
        historical=0; ((++correction_begin)); (( correction_begin == 1 && correction_end == 0 )) || die 'malformed correction inventory opener'
      elif [[ "$line" == 'Implementation inventory: BEGIN' ]]; then
        historical=0; ((++begin)); (( begin == 1 && end == 0 )) || die "duplicate inventory opener"
      elif [[ "$line" == 'Checkpoint 1 Governance RED: PASS / APPROVED' || "$line" == 'Checkpoint 2 Module, API, Holder: IN PROGRESS' || "$line" == 'Checkpoint 1 commits:'* || "$line" == 'Checkpoint 1 verification:'* || -z "$line" ]]; then
        :
      else
        die 'unrecognized historical controller-ledger prefix'
      fi
    elif [[ "$line" == Planning\ baseline:* ]]; then
      ((++sha_count)); [[ "$line" =~ '^Planning baseline: [0-9a-f]{40}$' ]] || die 'malformed planning line'
    elif [[ "$line" == 'Implementation inventory: BEGIN' ]]; then
      ((++begin)); (( begin == 1 && end == 0 )) || die "duplicate inventory opener"
    elif [[ "$line" == 'Implementation inventory: END' ]]; then
      ((++end)); (( begin == 1 && end == 1 )) || die "malformed inventory close"
    elif [[ "$line" == 'Correction inventory: BEGIN' ]]; then
      ((++correction_begin)); (( correction_begin == 1 && correction_end == 0 )) || die 'malformed correction inventory opener'
    elif [[ "$line" == 'Correction inventory: END' ]]; then
      ((++correction_end)); (( correction_begin == 1 && correction_end == 1 )) || die 'malformed correction inventory close'
    elif [[ "$line" == 'Frozen report SHA-256:'* ]]; then
      ((++report_count)); [[ "$line" =~ '^Frozen report SHA-256: [0-9a-f]{64}$' ]] || die 'malformed report hash'
      typeset -g FROZEN_REPORT_SHA="${line#Frozen report SHA-256: }"
    elif [[ "$line" == 'Correction evidence prefix SHA-256:'* ]]; then
      ((++correction_prefix_count)); CORRECTION_EVIDENCE_PREFIX_SHA="${line#Correction evidence prefix SHA-256: }"
      [[ "$CORRECTION_EVIDENCE_PREFIX_SHA" =~ '^[0-9a-f]{64}$' ]] || die 'malformed correction prefix hash'
    elif [[ "$line" == 'Correction map SHA-256:'* ]]; then
      ((++correction_map_count)); [[ "$line" == 'Correction map SHA-256: d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0' ]] || die 'malformed correction map hash'
    elif [[ "$line" == 'Amended library plan blob SHA-256:'* ]]; then
      ((++library_blob_count)); [[ "$line" =~ '^Amended library plan blob SHA-256: [0-9a-f]{64}$' ]] || die 'malformed amended library plan blob hash'
    elif [[ "$line" == 'Amended pointer plan blob SHA-256:'* ]]; then
      ((++pointer_blob_count)); [[ "$line" =~ '^Amended pointer plan blob SHA-256: [0-9a-f]{64}$' ]] || die 'malformed amended pointer plan blob hash'
    elif [[ "$line" == 'Committed cleanup successor plan SHA:'* ]]; then
      ((++committed_cleanup_successor_count)); committed_cleanup_successor_sha="${line#Committed cleanup successor plan SHA: }"; [[ "$line" =~ '^Committed cleanup successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed committed cleanup successor SHA'
    elif [[ "$line" == 'Simplified recovery successor plan SHA:'* ]]; then
      ((++simplified_successor_count)); simplified_successor_sha="${line#Simplified recovery successor plan SHA: }"; [[ "$line" =~ '^Simplified recovery successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed simplified successor SHA'
    elif [[ "$line" == 'Diagnostic successor plan SHA:'* ]]; then
      ((++diagnostic_successor_count)); diagnostic_successor_sha="${line#Diagnostic successor plan SHA: }"; [[ "$line" =~ '^Diagnostic successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed diagnostic successor SHA'
    elif [[ "$line" == 'Warnings successor plan SHA:'* ]]; then
      ((++warnings_successor_count)); warnings_successor_sha="${line#Warnings successor plan SHA: }"; [[ "$line" =~ '^Warnings successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed warnings successor SHA'
    elif [[ "$line" == 'Comprehensive successor plan SHA:'* ]]; then
      ((++comprehensive_successor_count)); comprehensive_successor_sha="${line#Comprehensive successor plan SHA: }"; [[ "$line" =~ '^Comprehensive successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed comprehensive successor SHA'
    elif [[ "$line" == 'Multiline successor plan SHA:'* ]]; then
      ((++multiline_successor_count)); multiline_successor_sha="${line#Multiline successor plan SHA: }"; [[ "$line" =~ '^Multiline successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed multiline successor SHA'
    elif [[ "$line" == 'Direct successor plan SHA:'* ]]; then
      ((++direct_successor_count)); direct_successor_sha="${line#Direct successor plan SHA: }"; [[ "$line" =~ '^Direct successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed direct successor SHA'
    elif [[ "$line" == 'Brace successor plan SHA:'* ]]; then
      ((++brace_successor_count)); brace_successor_sha="${line#Brace successor plan SHA: }"; [[ "$line" =~ '^Brace successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed brace successor SHA'
    elif [[ "$line" == 'Record successor plan SHA:'* ]]; then
      ((++record_successor_count)); record_successor_sha="${line#Record successor plan SHA: }"; [[ "$line" =~ '^Record successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed record successor SHA'
    elif [[ "$line" == 'Manifest successor plan SHA:'* ]]; then
      ((++manifest_successor_count)); manifest_successor_sha="${line#Manifest successor plan SHA: }"; [[ "$line" =~ '^Manifest successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed manifest successor SHA'
    elif [[ "$line" == 'Scope successor plan SHA:'* ]]; then
      ((++scope_successor_count)); scope_successor_sha="${line#Scope successor plan SHA: }"; [[ "$line" =~ '^Scope successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed scope successor SHA'
    elif [[ "$line" == 'Reconcile successor plan SHA:'* ]]; then
      ((++reconcile_successor_count)); reconcile_successor_sha="${line#Reconcile successor plan SHA: }"; [[ "$line" =~ '^Reconcile successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed reconcile successor SHA'
    elif (( correction_begin == 1 && correction_end == 0 )); then
      endpoint="${line%%$'\t'*}"; state="${line#*$'\t'}"; digest="${state#*$'\t'}"; state="${state%%$'\t'*}"
      correction_state=''; (( ${+CORRECTION[$endpoint]} )) && correction_state="${CORRECTION[$endpoint]}"
      [[ -n "$endpoint" && "$line" == *$'\t'*$'\t'* && "$digest" != *$'\t'* && "$correction_state" == M && "$state" == M && "$digest" =~ '^[0-9a-f]{64}$' ]] || die 'malformed correction inventory record'
    elif [[ "$line" == $'Correction inventory:'* ]]; then
      :
    elif (( begin == 1 && end == 0 )); then
      IFS=$'\t' read -r endpoint state digest <<< "$line"
      [[ -n "$endpoint" && "$state" == [AMD] && ( "$digest" == DELETED || "$digest" =~ '^[0-9a-f]{64}$' ) ]] || die "malformed inventory record"
      [[ "$state" == "${MANIFEST[$endpoint]-}" ]] || die "inventory endpoint/status drift"
      (( ! ${+FROZEN[$endpoint]} )) || die "duplicate inventory endpoint"
      [[ "$state" == D && "$digest" == DELETED || "$state" != D && "$digest" != DELETED ]] || die "inventory deletion/hash drift"
      FROZEN[$endpoint]="$digest"
    elif [[ "$line" == Event:* ]]; then
      [[ "$line" =~ '^Event: [0-9]+ CORRECTION [A-Z_]+ (PASS|RED|BLOCKED) [^[:space:]]+$' ]] || die 'malformed append-only Evidence event'
    elif [[ "$line" == 'Evidence: BEGIN' ]]; then
      ((++evidence_begin)); (( evidence_begin == 1 && end == 1 && evidence_end == 0 )) || die 'malformed Evidence opener'
    elif [[ "$line" == 'Evidence: END' ]]; then
      ((++evidence_end)); (( evidence_begin == 1 && evidence_end == 1 )) || die 'malformed Evidence close'
    elif (( evidence_begin == 1 && evidence_end == 0 )); then
      if [[ "$line" == Event:* ]]; then
        [[ "$line" =~ '^Event: [0-9]+ CORRECTION [A-Z_]+ (PASS|RED|BLOCKED) [^[:space:]]+$' ]] || die 'malformed Evidence event'
      else
        die 'malformed Evidence record'
      fi
    elif [[ "$line" == 'Closeout evidence:'* ]]; then
      [[ "$line" == 'Closeout evidence: '*(*) ]] || die 'malformed closeout evidence'
    else
      die "unexpected controller-ledger content"
    fi
  done < "$LEDGER_REL"
  (( failure_block == 0 )) || die 'unterminated failed-attempt block'
  lineage="$(successor_lineage "$planning_sha")" || die 'unknown successor lineage'
  if [[ "$lineage" == cleanup-committed ]]; then
    (( committed_cleanup_successor_count == 1 )) && [[ "$committed_cleanup_successor_sha" == "$planning_sha" ]] || die 'committed cleanup successor authority mismatch'
  elif [[ "$lineage" == simplified ]]; then
    (( simplified_successor_count == 1 )) && [[ "$simplified_successor_sha" == "$planning_sha" ]] || die 'simplified successor authority mismatch'
  elif [[ "$lineage" == diagnostic ]]; then
    (( diagnostic_successor_count == 1 )) && [[ "$diagnostic_successor_sha" == "$planning_sha" ]] || die 'diagnostic successor authority mismatch'
  elif [[ "$lineage" == warnings ]]; then
    (( warnings_successor_count == 1 )) && [[ "$warnings_successor_sha" == "$planning_sha" ]] || die 'warnings successor authority mismatch'
  elif [[ "$lineage" == comprehensive ]]; then
    (( comprehensive_successor_count == 1 )) && [[ "$comprehensive_successor_sha" == "$planning_sha" ]] || die 'comprehensive successor authority mismatch'
  elif [[ "$lineage" == multiline ]]; then
    (( multiline_successor_count == 1 )) && [[ "$multiline_successor_sha" == "$planning_sha" ]] || die 'multiline successor authority mismatch'
  elif [[ "$lineage" == direct ]]; then
    (( direct_successor_count == 1 )) && [[ "$direct_successor_sha" == "$planning_sha" ]] || die 'direct successor authority mismatch'
  elif [[ "$lineage" == brace ]]; then
    (( brace_successor_count == 1 )) && [[ "$brace_successor_sha" == "$planning_sha" ]] || die 'brace successor authority mismatch'
  elif [[ "$lineage" == record ]]; then
    (( record_successor_count == 1 )) && [[ "$record_successor_sha" == "$planning_sha" ]] || die 'record successor authority mismatch'
  elif [[ "$lineage" == manifest ]]; then
    (( manifest_successor_count == 1 )) && [[ "$manifest_successor_sha" == "$planning_sha" ]] || die 'manifest successor authority mismatch'
  elif [[ "$lineage" == scope ]]; then
    (( scope_successor_count == 1 )) && [[ "$scope_successor_sha" == "$planning_sha" ]] || die 'scope successor authority mismatch'
  elif [[ "$lineage" == reconcile ]]; then
    (( reconcile_successor_count == 1 )) && [[ "$reconcile_successor_sha" == "$planning_sha" ]] || die 'reconcile successor authority mismatch'
  else
    (( committed_cleanup_successor_count == 0 && simplified_successor_count == 0 && diagnostic_successor_count == 0 && warnings_successor_count == 0 && comprehensive_successor_count == 0 && multiline_successor_count == 0 && direct_successor_count == 0 && brace_successor_count == 0 && record_successor_count == 0 && manifest_successor_count == 0 && scope_successor_count == 0 && reconcile_successor_count == 0 )) || die 'unexpected successor authority row'
  fi
  # A rebound planning ledger precedes producer inventory freezing. Its correction inventory is the
  # authoritative preservation record until `Implementation inventory` is written later.
  if (( begin == 0 && end == 0 )); then
    (( correction_begin == 1 && correction_end == 1 && correction_prefix_count == 1 && correction_map_count == 1 && library_blob_count == 1 && pointer_blob_count == 1 && report_count == 1 )) || die 'incomplete rebound correction inventory'
    read_correction_inventory "$planning_sha" "$LEDGER_REL"
    [[ "$require_live_report" != 1 ]] || report_hash_matches || die 'frozen report hash drift'
    successor_failed_attempt_authority "$planning_sha" || die 'successor failed-attempt authority mismatch'
    return 0
  fi
  (( sha_count >= 1 && frozen_begin == 1 && begin == 1 && end == 1 && ${#FROZEN} == ${#MANIFEST} )) || die "incomplete inventory"
  rebound_inventory=$(( correction_begin || correction_end || correction_prefix_count || correction_map_count || library_blob_count || pointer_blob_count ))
  if (( rebound_inventory )); then
    (( correction_prefix_count == 1 && correction_map_count == 1 && library_blob_count == 1 && pointer_blob_count == 1 && correction_begin == 1 && correction_end == 1 )) || die 'incomplete correction evidence prefix'
    [[ "$(correction_prefix_sha)" == "$CORRECTION_EVIDENCE_PREFIX_SHA" ]] || die 'correction evidence prefix drift'
  else
    # A pre-rebind correction checkpoint may have its inventory/report state, but no rebound authority rows.
    (( correction_prefix_count == 0 && correction_map_count == 0 && library_blob_count == 0 && pointer_blob_count == 0 )) || die 'malformed pre-rebind correction state'
  fi
  if [[ "$require_live_report" == 1 ]]; then
    (( rebound_inventory && report_count == 1 )) || die 'rebound frozen report authority is incomplete'
    report_hash_matches || die 'frozen report hash drift'
  fi
  successor_failed_attempt_authority "$planning_sha" || die 'successor failed-attempt authority mismatch'
  if (( evidence_begin == 0 )); then
    validate_correction_events correction-commit-post
  fi
}

validate_evidence() {
  local evidence_begin="$1" evidence_end="$2" report_count="$3" line sequence type event_status artifact expected=1
  local -a required=(RED TEST_CREATED RED GREEN XML PLATFORM SCOPE REVIEW)
  (( evidence_begin == 1 && evidence_end == 1 && report_count == 1 )) || die 'incomplete Evidence section'
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" == Event:* ]]; then
      [[ "$line" =~ '^Event: ([0-9]+) ([A-Z_]+) ([A-Z]+) ([^[:space:]]+)$' ]] || die 'malformed Evidence event'
    else
      continue
    fi
    sequence="${match[1]}"; type="${match[2]}"; event_status="${match[3]}"; artifact="${match[4]}"
    (( sequence == expected )) || die 'Evidence sequence/order drift'
    [[ "$type" == "${required[$expected]}" ]] || die 'Evidence required event missing/out of order'
    [[ "$event_status" == PASS || "$event_status" == RED || "$event_status" == BLOCKED ]] || die 'malformed Evidence status'
    [[ "$artifact" != *'..'* && "$artifact" != /* ]] || die 'unsafe Evidence artifact'
    ((++expected))
  done < <(awk '/^Evidence: BEGIN$/ {on=1} on {print} /^Evidence: END$/ {exit}' "$LEDGER_REL")
  (( expected == 9 )) || die 'required Evidence event missing'
}

verify_inventory() {
  local endpoint state actual
  for endpoint in "${(@k)MANIFEST}"; do
    state="${MANIFEST[$endpoint]}"
    if [[ "$state" == D ]]; then [[ ! -e "$endpoint" && "${FROZEN[$endpoint]}" == DELETED ]] || return 1
    else
      actual="$(sha256 "$endpoint")" || return 1; [[ "$actual" == "${FROZEN[$endpoint]}" ]] || return 1
    fi
  done
}

verify_inventory_index() {
  local endpoint state actual
  for endpoint in "${(@k)MANIFEST}"; do
    state="${MANIFEST[$endpoint]}"
    if [[ "$state" == D ]]; then ! git cat-file -e ":$endpoint" 2>/dev/null || return 1
    else
      actual="$(git show ":$endpoint" | shasum -a 256 | awk '{print $1}')" || return 1
      [[ "$actual" == "${FROZEN[$endpoint]}" ]] || return 1
    fi
  done
}

verify_inventory_commit() {
  local commit_sha="$1" endpoint state actual
  for endpoint in "${(@k)MANIFEST}"; do
    state="${MANIFEST[$endpoint]}"
    if [[ "$state" == D ]]; then
      ! git cat-file -e "$commit_sha:$endpoint" 2>/dev/null || return 1
    else
      git cat-file -e "$commit_sha:$endpoint" 2>/dev/null || return 1
      actual="$(git show "$commit_sha:$endpoint" | shasum -a 256 | awk '{print $1}')" || return 1
      [[ "$actual" == "${FROZEN[$endpoint]}" ]] || return 1
    fi
  done
}

accept_implementation() {
  local planning_sha="$1" implementation_sha="$2"
  git merge-base --is-ancestor "$planning_sha" "$implementation_sha" || return 1
  local -A actual; actual=()
  local state endpoint
  while IFS=$'\t' read -r state endpoint; do
    [[ "${MANIFEST[$endpoint]-}" == "$state" ]] || return 1
    actual[$endpoint]="$state"
  done < <(git diff --name-status --no-renames "$planning_sha" "$implementation_sha")
  (( ${#actual} == ${#MANIFEST} )) || return 1
  for endpoint in "${(@k)MANIFEST}"; do [[ "${actual[$endpoint]-}" == "${MANIFEST[$endpoint]}" ]] || return 1; done
  read_rebound_inventory "$planning_sha" && successor_failed_attempt_authority "$planning_sha" && verify_inventory_commit "$implementation_sha"
}

accept_cumulative() {
  local planning_sha="$1" correction_sha="$2" final_sha="$3" state endpoint
  [[ "$(git rev-parse "$correction_sha^")" == "$planning_sha" ]] || return 1
  git merge-base --is-ancestor "$correction_sha" "$final_sha" || return 1
  git diff --name-status --no-renames "$planning_sha" "$final_sha" | while IFS=$'\t' read -r state endpoint; do
    [[ "${MANIFEST[$endpoint]-}" == "$state" ]] || exit 1
  done || return 1
  local -A actual; actual=()
  while IFS=$'\t' read -r state endpoint; do
    [[ -z "${actual[$endpoint]-}" ]] || return 1
    actual[$endpoint]="$state"
  done < <(git diff --name-status --no-renames "$planning_sha" "$final_sha")
  (( ${#actual} == ${#MANIFEST} )) || return 1
  for endpoint in "${(@k)MANIFEST}"; do [[ "${actual[$endpoint]-}" == "${MANIFEST[$endpoint]}" ]] || return 1; done
  read_rebound_inventory "$planning_sha" && successor_failed_attempt_authority "$planning_sha" && verify_inventory_commit "$final_sha"
}

same_correction_records() {
  local endpoint
  (( ${#STATUS} == ${#CORRECTION} + 1 )) || return 1
  [[ "${STATUS[$REPORT_REL]-}" == M ]] || return 1
  for endpoint in "${(@k)CORRECTION}"; do [[ "${STATUS[$endpoint]-}" == "${CORRECTION[$endpoint]}" ]] || return 1; done
}

report_hash_matches() { [[ "$(sha256 "$REPORT_REL")" == "${FROZEN_REPORT_SHA-}" ]]; }

# Every post-rebind consumer enters through this named strict mode; pre-rebind correction checkpoints
# continue to use read_inventory without the live-report requirement.
read_rebound_inventory() { read_inventory "$1" 1; }

successor_lineage() {
  local planning_sha="$1" parent plan_paths=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'
  [[ "$planning_sha" == 2e199950da3fa518c2491b3168cbb5fb86c4cefd ]] && { print -r -- baseline; return 0; }
  parent="$(git rev-parse "$planning_sha^" 2>/dev/null)" || return 1
  case "$parent" in
    2e199950da3fa518c2491b3168cbb5fb86c4cefd) print -r -- baseline;;
    608626fe8827c3c920e36dd71c97339ad42f3de6) print -r -- first;;
    89a65070434c6c1f03880412e9a741653b85d1a3) print -r -- cleanup;;
    fe9b565de72417a2b1bf584370d2eab29bbfc73e)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- cleanup-committed || print -r -- invalid;;
    4f850915b6686a8486c6b41a4e7e6b7dce655ef8)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- simplified || print -r -- invalid;;
    6f580fadd10c4bf63b79165a899b9dd31df9ee1b)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- diagnostic || print -r -- invalid;;
    bf16d36fc7198bef1c35a3229130d8870bad71f1)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- warnings || print -r -- invalid;;
    0a0ebe2f382cb9ab903ee50b21cf16cea2304784)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- comprehensive || print -r -- invalid;;
    e2606e7f143a3062b4bbcd67ad7982ca50bd10ae)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- multiline || print -r -- invalid;;
    5a07209d71b605951bd1576c5c1898b642cec9d9)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- direct || print -r -- invalid;;
    b48a8be41e19358e09dc3bfc360d3fc86e1ce943)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- brace || print -r -- invalid;;
    d4c1f615aedeaf400be271c7d864ef73b566571a)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- record || print -r -- invalid;;
    1e3652f3df65325550f17678dd56950ba7a72da5)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- manifest || print -r -- invalid;;
    b71819d067c74e5287c31b122f46a600f97539f8)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- scope || print -r -- invalid;;
    9bdf6874ac94785827542cfffb88b60370906229)
      [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$planning_sha" | LC_ALL=C sort)" == "$plan_paths" ]] && print -r -- reconcile || print -r -- invalid;;
    *) print -r -- invalid;;
  esac
}

# The bounded records are historical evidence, never inventory rows or correction events.
first_failed_attempt_authority() {
  local ledger_path="${1:-$LEDGER_REL}" first_log="${TASK_6_1_FIRST_LOG:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log}" first_summary="${TASK_6_1_FIRST_SUMMARY:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime.q9vybg/rhythhaus-red-recovery-summary.g4hQK1}" expected block
  expected=$'Failed RED_RECOVERY attempt: BEGIN\nStage: PRE_WORKTREE\nObserved error: cleanup_retry_fixture:1: fixture_root: parameter not set\nNo Gradle build client: PASS\nNo detached worktree: PASS\nLog: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log SHA-256: be69ba885c0f14dc609f030e9425ca65be5b6c74483becdca972bb29c4326454\nSummary: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime.q9vybg/rhythhaus-red-recovery-summary.g4hQK1 SHA-256: 3028afef6648544c7e07ee7269b3ef99e6bcb993a0a8002ae995aba32c8a4e90\nFailed RED_RECOVERY attempt: END'
  block="$(awk '/^Failed RED_RECOVERY attempt: BEGIN$/{n++;on=1} on{print} /^Failed RED_RECOVERY attempt: END$/{if(on){end++;on=0}} END{exit !(n==1 && end==1 && !on)}' "$ledger_path")" || return 1
  [[ "$block" == "$expected" && "$(grep -c '^Failed RED_RECOVERY attempt: BEGIN$' "$ledger_path")" == 1 && "$(grep -c '^Failed RED_RECOVERY attempt: END$' "$ledger_path")" == 1 && -f "$first_log" && -f "$first_summary" && "$(sha256 "$first_log")" == be69ba885c0f14dc609f030e9425ca65be5b6c74483becdca972bb29c4326454 && "$(sha256 "$first_summary")" == 3028afef6648544c7e07ee7269b3ef99e6bcb993a0a8002ae995aba32c8a4e90 ]]
}

successor_failed_attempt_authority() {
  local planning_sha="$1" ledger_path="${2:-$LEDGER_REL}" lineage
  lineage="$(successor_lineage "$planning_sha")" || return 1
  case "$lineage" in
    baseline) return 0;;
    first) first_failed_attempt_authority "$ledger_path" && [[ "$(grep -c '^Failed RED_RECOVERY attempt 2: BEGIN$' "$ledger_path")" == 0 ]];;
    cleanup|cleanup-committed|simplified|diagnostic|warnings|comprehensive|multiline|direct|brace|record|manifest|scope|reconcile) first_failed_attempt_authority "$ledger_path" && second_failed_attempt_authority "$ledger_path" && [[ "$(awk '/^Failed RED_RECOVERY attempt: END$/{print NR;exit}' "$ledger_path")" -lt "$(awk '/^Failed RED_RECOVERY attempt 2: BEGIN$/{print NR;exit}' "$ledger_path")" ]];;
    *) return 1;;
  esac
}

second_failed_attempt_authority() {
  local ledger_path="${1:-$LEDGER_REL}" second_log="${TASK_6_1_SECOND_LOG:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output-fresh.JM8mHw}" second_outer="${TASK_6_1_SECOND_OUTER_SUMMARY:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-recovery-summary.h8gzST}" second_fixture="${TASK_6_1_SECOND_FIXTURE_SUMMARY:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ}" expected block
  expected=$'Failed RED_RECOVERY attempt 2: BEGIN\nStage: PRE_WORKTREE\nPrimary error: RED_RECOVERY: cleanup evidence retry fixture failed\nSecondary error: fixture_remove: attempts: parameter not set\nSecondary error: fixture_remove: second: parameter not set\nNo Gradle build client: PASS\nNo detached worktree: PASS\nCleanup outcome: FAIL\nLog: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output-fresh.JM8mHw SHA-256: bc3fa401adaeaf96846a0266cea2e2c1bbbaa3c967e9eba5795875f3595ccd9e\nOuter summary: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-recovery-summary.h8gzST SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\nFixture summary: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ SHA-256: a958917a3dc2495d28e4475e141aeaa48d9bf8b0a7104be7c4a4d3123a6ed44c\nFailed RED_RECOVERY attempt 2: END'
  block="$(awk '/^Failed RED_RECOVERY attempt 2: BEGIN$/{n++;on=1} on{print} /^Failed RED_RECOVERY attempt 2: END$/{if(on){end++;on=0}} END{exit !(n==1 && end==1 && !on)}' "$ledger_path")" || return 1
  [[ "$block" == "$expected" && "$(grep -c '^Failed RED_RECOVERY attempt 2: BEGIN$' "$ledger_path")" == 1 && "$(grep -c '^Failed RED_RECOVERY attempt 2: END$' "$ledger_path")" == 1 && -f "$second_log" && -f "$second_outer" && -f "$second_fixture" && "$(sha256 "$second_log")" == bc3fa401adaeaf96846a0266cea2e2c1bbbaa3c967e9eba5795875f3595ccd9e && "$(sha256 "$second_outer")" == e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855 && "$(sha256 "$second_fixture")" == a958917a3dc2495d28e4475e141aeaa48d9bf8b0a7104be7c4a4d3123a6ed44c ]]
}

successor_planning_sha() { local lineage="$(successor_lineage "$1")"; [[ "$lineage" == first ]] || [[ "$lineage" == cleanup ]] || [[ "$lineage" == cleanup-committed ]] || [[ "$lineage" == simplified ]] || [[ "$lineage" == diagnostic ]] || [[ "$lineage" == warnings ]] || [[ "$lineage" == comprehensive ]] || [[ "$lineage" == multiline ]] || [[ "$lineage" == direct ]] || [[ "$lineage" == brace ]] || [[ "$lineage" == record ]] || [[ "$lineage" == manifest ]] || [[ "$lineage" == scope ]] || [[ "$lineage" == reconcile ]]; }

is_failure_authority_row() {
  case "$1" in
    'Failed RED_RECOVERY attempt:'*|'Failed RED_RECOVERY attempt 2:'*|'Stage: PRE_WORKTREE'|'Observed error:'*|'Primary error:'*|'Secondary error:'*|'No Gradle build client: PASS'|'No detached worktree: PASS'|'Cleanup outcome: FAIL'|'Log:'*|'Summary:'*|'Outer summary:'*|'Fixture summary:'*) return 0;;
    *) return 1;;
  esac
}

correction_evidence_prefix() {
  /usr/bin/awk 'BEGIN { ORS="\n" } { print; if ($0 == "Evidence: END") exit }' "$LEDGER_REL"
}

correction_prefix_sha() {
  local ledger_path="${1:-$LEDGER_REL}"
  perl -0ne 'if (/(.*?)^Correction inventory: BEGIN\n/ms) { print $1 } else { print $_ }' "$ledger_path" | shasum -a 256 | awk '{print $1}'
}

read_correction_inventory() {
  local planning_sha="$1" ledger_path="${2:-$LEDGER_REL}" line='' endpoint='' state='' digest='' expected_state='' committed_cleanup_successor_sha='' simplified_successor_sha='' diagnostic_successor_sha='' warnings_successor_sha='' comprehensive_successor_sha='' multiline_successor_sha='' direct_successor_sha='' brace_successor_sha='' record_successor_sha='' manifest_successor_sha='' scope_successor_sha='' reconcile_successor_sha='' lineage seen_endpoints=$'\n' begin=0 end=0 frozen_count=0 sha_count=0 report_count=0 prefix_sha_count=0 map_sha_count=0 library_blob_count=0 pointer_blob_count=0 committed_cleanup_successor_count=0 simplified_successor_count=0 diagnostic_successor_count=0 warnings_successor_count=0 comprehensive_successor_count=0 multiline_successor_count=0 direct_successor_count=0 brace_successor_count=0 record_successor_count=0 manifest_successor_count=0 scope_successor_count=0 reconcile_successor_count=0 events=0 frozen_begin=0 historical=1 failure_block=0 correction_endpoint
  typeset -gA CORRECTION_FROZEN
  CORRECTION_FROZEN=()
  read_correction_map
  [[ -f "$ledger_path" ]] || die "missing controller ledger"
  validate_historical_prefix "$planning_sha" "$ledger_path"
  while IFS= read -r line || [[ -n "$line" ]]; do
    # Failure-record lines are legal only inside their own bounded range. Exact record grammar and
    # artifact hashes are checked after inventory parsing by successor_failed_attempt_authority.
    if (( failure_block == 1 )); then
      [[ "$line" == 'Failed RED_RECOVERY attempt: END' ]] && { failure_block=0; continue; }
      [[ "$line" != 'Failed RED_RECOVERY attempt: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt 2: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt 2: END' ]] || die 'nested or unmatched failed-attempt boundary'
      continue
    elif (( failure_block == 2 )); then
      [[ "$line" == 'Failed RED_RECOVERY attempt 2: END' ]] && { failure_block=0; continue; }
      [[ "$line" != 'Failed RED_RECOVERY attempt: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt 2: BEGIN' && "$line" != 'Failed RED_RECOVERY attempt: END' ]] || die 'nested or unmatched failed-attempt boundary'
      continue
    elif [[ "$line" == 'Failed RED_RECOVERY attempt: BEGIN' ]]; then
      failure_block=1; continue
    elif [[ "$line" == 'Failed RED_RECOVERY attempt 2: BEGIN' ]]; then
      failure_block=2; continue
    elif is_failure_authority_row "$line"; then
      die 'orphan failed-attempt authority row'
    elif (( historical == 2 )); then
      if [[ "$line" == 'Correction inventory: BEGIN' ]]; then
        historical=0; ((++begin)); (( begin == 1 && end == 0 )) || die 'malformed correction inventory opener'
      elif [[ "$line" == 'Frozen inventory: END' ]]; then
        die 'historical Frozen inventory is an open prefix; END is not permitted'
      elif [[ "$line" == 'Checkpoint 1 Governance RED: PASS / APPROVED' || "$line" == 'Checkpoint 2 Module, API, Holder: IN PROGRESS' || "$line" == 'Checkpoint 1 commits:'* || "$line" == 'Checkpoint 1 verification:'* || -z "$line" ]]; then
        :
      else
        die 'unrecognized historical controller-ledger content before correction boundary'
      fi
    elif (( historical )); then
      if [[ "$line" == Planning\ baseline:* ]]; then
        ((++sha_count)); [[ "$line" =~ '^Planning baseline: [0-9a-f]{40}$' ]] || die 'malformed historical planning line'
      elif [[ "$line" == 'Frozen inventory: BEGIN' ]]; then
        ((++frozen_begin)); (( frozen_begin == 1 )) || die 'duplicate historical Frozen inventory opener'
        historical=2
      elif [[ "$line" == 'Correction inventory: BEGIN' ]]; then
        historical=0; ((++begin)); (( begin == 1 && end == 0 )) || die 'malformed correction inventory opener'
      elif [[ "$line" == 'Checkpoint 1 Governance RED: PASS / APPROVED' || "$line" == 'Checkpoint 2 Module, API, Holder: IN PROGRESS' || "$line" == 'Checkpoint 1 commits:'* || "$line" == 'Checkpoint 1 verification:'* || -z "$line" ]]; then
        :
      else
        die 'unrecognized historical controller-ledger prefix'
      fi
    elif [[ "$line" == Planning\ baseline:* ]]; then
      ((++sha_count)); [[ "$line" =~ '^Planning baseline: [0-9a-f]{40}$' ]] || die 'malformed planning line'
    elif [[ "$line" == 'Correction inventory: BEGIN' ]]; then
      ((++begin)); (( begin == 1 && end == 0 )) || die 'duplicate correction inventory opener'
    elif [[ "$line" == 'Correction inventory: END' ]]; then
      ((++end)); (( begin == 1 && end == 1 )) || die 'malformed correction inventory close'
    elif (( begin == 1 && end == 0 )); then
      endpoint="${line%%$'\t'*}"; state="${line#*$'\t'}"; digest="${state#*$'\t'}"; state="${state%%$'\t'*}"
      [[ -n "$endpoint" && "$line" == *$'\t'*$'\t'* && "$digest" != *$'\t'* ]] || die 'malformed correction inventory record'
      expected_state=''; for correction_endpoint in "${(@k)CORRECTION}"; do [[ "$correction_endpoint" == "$endpoint" ]] && expected_state="${CORRECTION[$correction_endpoint]}"; done
      [[ "$expected_state" == M && "$state" == M && "$digest" =~ '^[0-9a-f]{64}$' ]] || die 'malformed correction inventory record'
      [[ "$seen_endpoints" != *$'\n'"$endpoint"$'\n'* ]] || die 'duplicate correction inventory endpoint'
      seen_endpoints+="$endpoint"$'\n'; CORRECTION_FROZEN[$endpoint]="$digest"; ((++frozen_count))
    elif [[ "$line" == 'Correction evidence prefix SHA-256:'* ]]; then
      ((++prefix_sha_count)); CORRECTION_EVIDENCE_PREFIX_SHA="${line#Correction evidence prefix SHA-256: }"
      [[ "$CORRECTION_EVIDENCE_PREFIX_SHA" =~ '^[0-9a-f]{64}$' ]] || die 'malformed correction evidence prefix hash'
    elif [[ "$line" == 'Correction map SHA-256:'* ]]; then
      ((++map_sha_count)); CORRECTION_MAP_SHA="${line#Correction map SHA-256: }"
      [[ "$CORRECTION_MAP_SHA" == d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0 ]] || die 'correction map authority mismatch'
    elif [[ "$line" == 'Frozen report SHA-256:'* ]]; then
      ((++report_count)); FROZEN_REPORT_SHA="${line#Frozen report SHA-256: }"
      [[ "$FROZEN_REPORT_SHA" =~ '^[0-9a-f]{64}$' ]] || die 'malformed report hash'
    elif [[ "$line" == 'Amended library plan blob SHA-256:'* ]]; then
      ((++library_blob_count)); AMENDED_LIBRARY_PLAN_BLOB_SHA="${line#Amended library plan blob SHA-256: }"
      [[ "$AMENDED_LIBRARY_PLAN_BLOB_SHA" =~ '^[0-9a-f]{64}$' ]] || die 'malformed amended library plan blob hash'
    elif [[ "$line" == 'Amended pointer plan blob SHA-256:'* ]]; then
      ((++pointer_blob_count)); AMENDED_POINTER_PLAN_BLOB_SHA="${line#Amended pointer plan blob SHA-256: }"
      [[ "$AMENDED_POINTER_PLAN_BLOB_SHA" =~ '^[0-9a-f]{64}$' ]] || die 'malformed amended pointer plan blob hash'
    elif [[ "$line" == 'Committed cleanup successor plan SHA:'* ]]; then
      ((++committed_cleanup_successor_count)); committed_cleanup_successor_sha="${line#Committed cleanup successor plan SHA: }"
      [[ "$line" =~ '^Committed cleanup successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed committed cleanup successor SHA'
    elif [[ "$line" == 'Simplified recovery successor plan SHA:'* ]]; then
      ((++simplified_successor_count)); simplified_successor_sha="${line#Simplified recovery successor plan SHA: }"
      [[ "$line" =~ '^Simplified recovery successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed simplified successor SHA'
    elif [[ "$line" == 'Diagnostic successor plan SHA:'* ]]; then
      ((++diagnostic_successor_count)); diagnostic_successor_sha="${line#Diagnostic successor plan SHA: }"
      [[ "$line" =~ '^Diagnostic successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed diagnostic successor SHA'
    elif [[ "$line" == 'Warnings successor plan SHA:'* ]]; then
      ((++warnings_successor_count)); warnings_successor_sha="${line#Warnings successor plan SHA: }"
      [[ "$line" =~ '^Warnings successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed warnings successor SHA'
    elif [[ "$line" == 'Comprehensive successor plan SHA:'* ]]; then
      ((++comprehensive_successor_count)); comprehensive_successor_sha="${line#Comprehensive successor plan SHA: }"
      [[ "$line" =~ '^Comprehensive successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed comprehensive successor SHA'
    elif [[ "$line" == 'Multiline successor plan SHA:'* ]]; then
      ((++multiline_successor_count)); multiline_successor_sha="${line#Multiline successor plan SHA: }"
      [[ "$line" =~ '^Multiline successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed multiline successor SHA'
    elif [[ "$line" == 'Direct successor plan SHA:'* ]]; then
      ((++direct_successor_count)); direct_successor_sha="${line#Direct successor plan SHA: }"
      [[ "$line" =~ '^Direct successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed direct successor SHA'
    elif [[ "$line" == 'Brace successor plan SHA:'* ]]; then
      ((++brace_successor_count)); brace_successor_sha="${line#Brace successor plan SHA: }"
      [[ "$line" =~ '^Brace successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed brace successor SHA'
    elif [[ "$line" == 'Record successor plan SHA:'* ]]; then
      ((++record_successor_count)); record_successor_sha="${line#Record successor plan SHA: }"
      [[ "$line" =~ '^Record successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed record successor SHA'
    elif [[ "$line" == 'Manifest successor plan SHA:'* ]]; then
      ((++manifest_successor_count)); manifest_successor_sha="${line#Manifest successor plan SHA: }"
      [[ "$line" =~ '^Manifest successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed manifest successor SHA'
    elif [[ "$line" == 'Scope successor plan SHA:'* ]]; then
      ((++scope_successor_count)); scope_successor_sha="${line#Scope successor plan SHA: }"
      [[ "$line" =~ '^Scope successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed scope successor SHA'
    elif [[ "$line" == 'Reconcile successor plan SHA:'* ]]; then
      ((++reconcile_successor_count)); reconcile_successor_sha="${line#Reconcile successor plan SHA: }"
      [[ "$line" =~ '^Reconcile successor plan SHA: [0-9a-f]{40}$' ]] || die 'malformed reconcile successor SHA'
    elif [[ "$line" == 'Evidence: BEGIN' || "$line" == 'Evidence: END' ]]; then
      :
    elif [[ "$line" == Event:* ]]; then
      ((++events)); [[ "$line" =~ '^Event: [0-9]+ CORRECTION [A-Z_]+ (PASS|RED|BLOCKED) [^[:space:]]+$' ]] || die 'malformed correction Evidence event'
    elif [[ -n "$line" ]]; then
      die 'unrecognized correction ledger line'
    fi
  done < "$ledger_path"
  (( failure_block == 0 )) || die 'unterminated failed-attempt block'
  (( sha_count >= 1 && frozen_begin == 1 && begin == 1 && end == 1 && frozen_count == 12 && report_count == 1 && prefix_sha_count == 1 && map_sha_count == 1 && library_blob_count == 1 && pointer_blob_count == 1 )) || die 'incomplete correction inventory'
  lineage="$(successor_lineage "$planning_sha")" || die 'unknown successor lineage'
  if [[ "$lineage" == cleanup-committed ]]; then
    (( committed_cleanup_successor_count == 1 )) && [[ "$committed_cleanup_successor_sha" == "$planning_sha" ]] || die 'committed cleanup successor authority mismatch'
  elif [[ "$lineage" == simplified ]]; then
    (( simplified_successor_count == 1 )) && [[ "$simplified_successor_sha" == "$planning_sha" ]] || die 'simplified successor authority mismatch'
  elif [[ "$lineage" == diagnostic ]]; then
    (( diagnostic_successor_count == 1 )) && [[ "$diagnostic_successor_sha" == "$planning_sha" ]] || die 'diagnostic successor authority mismatch'
  elif [[ "$lineage" == warnings ]]; then
    (( warnings_successor_count == 1 )) && [[ "$warnings_successor_sha" == "$planning_sha" ]] || die 'warnings successor authority mismatch'
  elif [[ "$lineage" == comprehensive ]]; then
    (( comprehensive_successor_count == 1 )) && [[ "$comprehensive_successor_sha" == "$planning_sha" ]] || die 'comprehensive successor authority mismatch'
  elif [[ "$lineage" == multiline ]]; then
    (( multiline_successor_count == 1 )) && [[ "$multiline_successor_sha" == "$planning_sha" ]] || die 'multiline successor authority mismatch'
  elif [[ "$lineage" == direct ]]; then
    (( direct_successor_count == 1 )) && [[ "$direct_successor_sha" == "$planning_sha" ]] || die 'direct successor authority mismatch'
  elif [[ "$lineage" == brace ]]; then
    (( brace_successor_count == 1 )) && [[ "$brace_successor_sha" == "$planning_sha" ]] || die 'brace successor authority mismatch'
  elif [[ "$lineage" == record ]]; then
    (( record_successor_count == 1 )) && [[ "$record_successor_sha" == "$planning_sha" ]] || die 'record successor authority mismatch'
  elif [[ "$lineage" == manifest ]]; then
    (( manifest_successor_count == 1 )) && [[ "$manifest_successor_sha" == "$planning_sha" ]] || die 'manifest successor authority mismatch'
  elif [[ "$lineage" == scope ]]; then
    (( scope_successor_count == 1 )) && [[ "$scope_successor_sha" == "$planning_sha" ]] || die 'scope successor authority mismatch'
  elif [[ "$lineage" == reconcile ]]; then
    (( reconcile_successor_count == 1 )) && [[ "$reconcile_successor_sha" == "$planning_sha" ]] || die 'reconcile successor authority mismatch'
  else
    (( committed_cleanup_successor_count == 0 && simplified_successor_count == 0 && diagnostic_successor_count == 0 && warnings_successor_count == 0 && comprehensive_successor_count == 0 && multiline_successor_count == 0 && direct_successor_count == 0 && brace_successor_count == 0 && record_successor_count == 0 && manifest_successor_count == 0 && scope_successor_count == 0 && reconcile_successor_count == 0 )) || die 'unexpected successor authority row'
  fi
  [[ "$(correction_prefix_sha "$ledger_path")" == "$CORRECTION_EVIDENCE_PREFIX_SHA" ]] || die 'correction evidence prefix drift'
  [[ "$AMENDED_LIBRARY_PLAN_BLOB_SHA" == "$(git show "$planning_sha:$PLAN_REL" | shasum -a 256 | awk '{print $1}')" && "$AMENDED_POINTER_PLAN_BLOB_SHA" == "$(git show "$planning_sha:$AMENDMENT_POINTER_PLAN" | shasum -a 256 | awk '{print $1}')" ]] || die 'amended plan/pointer blob authority mismatch'
  successor_failed_attempt_authority "$planning_sha" "$ledger_path" || die 'successor failed-attempt authority mismatch'
  return 0
}

# This fixture mutates only a temporary ledger copy. Each amended-blob failure must fail closed while
# the untouched rebound ledger remains accepted by the same parser.
parser_blob_negative_fixtures() {
  local planning_sha="$1" saved_ledger="$LEDGER_REL" fixture rc lineage
  fixture="$(mktemp "${TMPDIR:-/tmp}/task-6.1-blob-parser.XXXXXX")" || return 1
  cp -- "$LEDGER_REL" "$fixture"
  for mutation in missing-library duplicate-library malformed-library wrong-library missing-pointer duplicate-pointer malformed-pointer wrong-pointer; do
    cp -- "$saved_ledger" "$fixture"
    case "$mutation" in
      missing-library) perl -0pi -e 's/^Amended library plan blob SHA-256: .*\n//m' "$fixture";;
      duplicate-library) print -r -- 'Amended library plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000' >> "$fixture";;
      malformed-library) perl -0pi -e 's/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: malformed/m' "$fixture";;
      wrong-library) perl -0pi -e 's/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$fixture";;
      missing-pointer) perl -0pi -e 's/^Amended pointer plan blob SHA-256: .*\n//m' "$fixture";;
      duplicate-pointer) print -r -- 'Amended pointer plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000' >> "$fixture";;
      malformed-pointer) perl -0pi -e 's/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: malformed/m' "$fixture";;
      wrong-pointer) perl -0pi -e 's/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$fixture";;
    esac
    setopt noerrexit; ( read_correction_inventory "$planning_sha" "$fixture" ) >/dev/null 2>&1; rc=$?; setopt errexit
    (( rc != 0 )) || { rm -f -- "$fixture"; return 1; }
  done
  rm -f -- "$fixture"; read_correction_inventory "$planning_sha" "$saved_ledger"
}

# The committed-cleanup row is accepted only by the exact committed-delta lineage and is bound to its
# planning SHA. These mutations enter the real production correction parser, not a shadow validator.
committed_cleanup_successor_row_negative_fixtures() {
  local planning_sha="$1" saved_ledger="$LEDGER_REL" fixture rc
  [[ "$(successor_lineage "$planning_sha")" == cleanup-committed ]] || return 1
  fixture="$(mktemp "${TMPDIR:-/tmp}/task-6.1-committed-cleanup-row.XXXXXX")" || return 1
  cp -- "$saved_ledger" "$fixture"
  for mutation in missing malformed wrong duplicate; do
    cp -- "$saved_ledger" "$fixture"
    case "$mutation" in
      missing) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*\n//m' "$fixture";;
      malformed) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*/Committed cleanup successor plan SHA: malformed/m' "$fixture";;
      wrong) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*/Committed cleanup successor plan SHA: 0000000000000000000000000000000000000000/m' "$fixture";;
      duplicate) print -r -- "Committed cleanup successor plan SHA: $planning_sha" >> "$fixture";;
    esac
    setopt noerrexit; ( read_correction_inventory "$planning_sha" "$fixture" ) >/dev/null 2>&1; rc=$?; setopt errexit
    (( rc != 0 )) || { rm -f -- "$fixture"; return 1; }
  done
  rm -f -- "$fixture"; read_correction_inventory "$planning_sha" "$saved_ledger"
}

validate_historical_prefix() {
  local ignored_planning_sha="$1" ledger_path="${2:-$LEDGER_REL}" line index=1
  local -a expected=(
    'Planning baseline: 2e199950da3fa518c2491b3168cbb5fb86c4cefd'
    'Frozen inventory: BEGIN'
    'Checkpoint 1 Governance RED: PASS / APPROVED'
    'Checkpoint 1 commits: 3ae4a85ae30934e110064b33b5f1cb14d4694e32 fcbfa74c704b1b37926335b911054a1b61b9b879 3f2e164 e8b9934'
    'Checkpoint 1 verification: ArchitectureCheckPluginFunctionalTest 90/0/0/0; KmpConventionPluginsFunctionalTest 6/0/0/0; exact forced -D processor JAR invocation PASS'
    'Checkpoint 2 Module, API, Holder: IN PROGRESS'
  )
  [[ -f "$ledger_path" ]] || return 1
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" == 'Correction inventory: BEGIN' || "$line" == 'Implementation inventory: BEGIN' ]] && break
    (( index <= ${#expected} )) || die 'historical prefix has extra content'
    [[ "$line" == "${expected[$index]}" ]] || die 'historical prefix is missing, reordered, unknown, or malformed'
    ((++index))
  done < "$ledger_path"
  (( index == ${#expected} + 1 )) || die 'historical prefix is incomplete'
  [[ "$(awk 'NR<=6 { print }' "$ledger_path" | shasum -a 256 | awk '{print $1}')" == d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0 ]] || die 'historical prefix digest mismatch'
}

validate_correction_events() {
  local phase="$1" line sequence event_phase type event_status artifact expected=1
  local -a required=(PREPARE PRE STAGE_PRE STAGE_POST COMMIT_PRE COMMIT_POST)
  local limit=0
  case "$phase" in correction-prepare) limit=1;; correction-pre) limit=2;; correction-stage-pre) limit=3;; correction-stage-post) limit=4;; correction-commit-pre) limit=5;; correction-commit-post) limit=6;; *) die "unknown correction event phase [$phase]";; esac
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" == Event:* ]] || continue
    [[ "$line" =~ '^Event: ([0-9]+) ([A-Z_]+) ([A-Z_]+) (PASS|RED|BLOCKED) ([^[:space:]]+)$' ]] || die 'malformed correction Evidence event'
    sequence="${match[1]}"; event_phase="${match[2]}"; type="${match[3]}"; event_status="${match[4]}"; artifact="${match[5]}"
    (( sequence == expected && sequence <= limit )) || die 'correction Evidence sequence/order drift'
    [[ "$event_phase" == CORRECTION && "$type" == "${required[$expected]}" && "$event_status" == PASS && "$artifact" != *'..'* && "$artifact" != /* ]] || die 'correction Evidence type/artifact drift'
    ((++expected))
  done < "$LEDGER_REL"
  (( expected == limit + 1 )) || die 'required correction Evidence event missing'
}

write_correction_inventory() {
  local planning_sha="$1" endpoint digest evidence_prefix prefix_sha library_blob pointer_blob
  [[ "$(git rev-parse HEAD)" == "$planning_sha" ]] || die 'HEAD is not correction planning SHA'
  [[ "$(brief_sha "$BRIEF_REL")" == "$planning_sha" ]] || die 'brief SHA mismatch'
  empty_index || die 'correction prepare requires empty index'
  [[ "${STATUS[$REPORT_REL]-}" == M && ${#STATUS} == 1 ]] || die 'correction prepare requires report-only dirty worktree'
  [[ -f "$LEDGER_REL" ]] || die 'missing controller ledger'
  validate_historical_prefix "$planning_sha"
  evidence_prefix="$(/usr/bin/awk '{ print }' "$LEDGER_REL")"
  prefix_sha="$(correction_prefix_sha)"
  library_blob="$(git show "$planning_sha:$PLAN_REL" | shasum -a 256 | awk '{print $1}')"
  pointer_blob="$(git show "$planning_sha:$AMENDMENT_POINTER_PLAN" | shasum -a 256 | awk '{print $1}')"
  {
    print -r -- 'Correction inventory: BEGIN'
    for endpoint in "${(@k)CORRECTION}"; do
      git cat-file -e "$planning_sha:$endpoint" 2>/dev/null || die "correction endpoint absent at baseline: $endpoint"
      digest="$(git show "$planning_sha:$endpoint" | shasum -a 256 | awk '{print $1}')"
      print -r -- "$endpoint"$'\tM\t'"$digest"
    done
    print -r -- 'Correction inventory: END'
    print -r -- 'Correction map SHA-256: d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0'
    print -r -- 'Correction evidence prefix SHA-256: '"$prefix_sha"
    print -r -- 'Amended library plan blob SHA-256: '"$library_blob"
    print -r -- 'Amended pointer plan blob SHA-256: '"$pointer_blob"
    print -r -- 'Frozen report SHA-256: '"$(sha256 "$REPORT_REL")"
  } >> "$LEDGER_REL"
}

correction_baseline_valid() {
  local planning_sha="$1" endpoint
  read_correction_inventory "$planning_sha"
  report_hash_matches || return 1
  for endpoint in "${(@k)CORRECTION}"; do
    [[ "$(git show "$planning_sha:$endpoint" | shasum -a 256 | awk '{print $1}')" == "${CORRECTION_FROZEN[$endpoint]}" ]] || return 1
  done
}

append_correction_event() {
  local sequence="$1" type="$2" event_status="$3" artifact="$4"
  [[ "$sequence" == <-> && "$type" == [A-Z_]* && "$event_status" == (PASS|RED|BLOCKED) ]] || die 'malformed append event'
  [[ "$artifact" != *'..'* && "$artifact" != /* && "$artifact" != *$'\n'* ]] || die 'unsafe append artifact'
  print -r -- "Event: $sequence CORRECTION $type $event_status $artifact" >> "$LEDGER_REL"
}

append_correction_phase_event() {
  local phase="$1" sequence type
  case "$phase" in
    correction-prepare) sequence=1; type=PREPARE;;
    correction-pre) sequence=2; type=PRE;;
    correction-stage-pre) sequence=3; type=STAGE_PRE;;
    correction-stage-post) sequence=4; type=STAGE_POST;;
    correction-commit-pre) sequence=5; type=COMMIT_PRE;;
    correction-commit-post) sequence=6; type=COMMIT_POST;;
    *) die "unknown correction event phase [$phase]";;
  esac
  print -r -- "Event: $sequence CORRECTION $type PASS $phase" >> "$LEDGER_REL"
}

staged_correction_only() {
  local endpoint
  [[ -z "$(git diff --cached --name-only -- "$REPORT_REL")" ]] || return 1
  [[ "$(git diff --cached --name-only | LC_ALL=C sort | tr '\n' '\0')" == "$(printf '%s\n' "${(@k)CORRECTION}" | LC_ALL=C sort | tr '\n' '\0')" ]] || return 1
  for endpoint in "${(@k)CORRECTION}"; do
    [[ "$(git diff --cached --name-status --no-renames -- "$endpoint")" == $'M\t'"$endpoint" ]] || return 1
  done
}

verify_correction_commit() {
  local correction_sha="$1" endpoint committed working
  for endpoint in "${(@k)CORRECTION}"; do
    git cat-file -e "$correction_sha:$endpoint" 2>/dev/null || return 1
    committed="$(git show "$correction_sha:$endpoint" | shasum -a 256 | awk '{print $1}')"
    working="$(sha256 "$endpoint")" || return 1
    [[ "$committed" == "$working" && "$committed" != "${CORRECTION_FROZEN[$endpoint]}" ]] || return 1
  done
}

correction_gate() {
  local phase="$1" planning_sha="$2" correction_sha="${3:-}"
  read_correction_map; read_status; successor_failed_attempt_authority "$planning_sha" || return 1
  case "$phase" in
    correction-prepare)
      [[ "$(git rev-parse HEAD)" == "$planning_sha" ]] && [[ "$(brief_sha "$BRIEF_REL")" == "$planning_sha" ]] && empty_index || return 1
      [[ "${STATUS[$REPORT_REL]-}" == M ]] && (( ${#STATUS} == 1 )) || return 1
      for endpoint in "${(@k)CORRECTION}"; do [[ -z "${STATUS[$endpoint]-}" ]] || return 1; done
      if successor_planning_sha "$planning_sha"; then
        correction_baseline_valid "$planning_sha" && [[ -z "$(grep '^Event:' "$LEDGER_REL")" ]] || return 1
      else
        write_correction_inventory "$planning_sha"
      fi
      append_correction_phase_event correction-prepare;;
    correction-pre)
      [[ "$(git rev-parse HEAD)" == "$planning_sha" ]] && [[ "$(brief_sha "$BRIEF_REL")" == "$planning_sha" ]] && [[ -f "$REPORT_REL" ]] && [[ -z "$(git diff --cached --name-only)" ]] || return 1
      correction_baseline_valid "$planning_sha" && [[ "${STATUS[$REPORT_REL]-}" == M ]] && (( ${#STATUS} == 1 )) && validate_correction_events correction-prepare || return 1
      append_correction_phase_event correction-pre;;
    correction-stage-pre)
      [[ "$(git rev-parse HEAD)" == "$planning_sha" ]] && empty_index && correction_baseline_valid "$planning_sha" && same_correction_records && validate_correction_events correction-pre || return 1
      append_correction_phase_event correction-stage-pre;;
    correction-stage-post)
      [[ "$(git rev-parse HEAD)" == "$planning_sha" ]] && ! empty_index && same_correction_records && correction_baseline_valid "$planning_sha" && staged_correction_only && validate_correction_events correction-stage-pre || return 1
      append_correction_phase_event correction-stage-post;;
    correction-commit-pre)
      [[ "$(git rev-parse HEAD)" == "$planning_sha" ]] && ! empty_index && same_correction_records && correction_baseline_valid "$planning_sha" && staged_correction_only && validate_correction_events correction-stage-post || return 1
      append_correction_phase_event correction-commit-pre;;
    correction-commit-post)
      [[ -n "$correction_sha" ]] && [[ "$(git rev-parse HEAD)" == "$correction_sha" ]] && [[ "$(git rev-parse "$correction_sha^")" == "$planning_sha" ]] && empty_index && correction_baseline_valid "$planning_sha" || return 1
      local -A correction_actual; correction_actual=(); local correction_state correction_endpoint
      while IFS=$'\t' read -r correction_state correction_endpoint; do
        [[ "${CORRECTION[$correction_endpoint]-}" == "$correction_state" ]] || return 1
        correction_actual[$correction_endpoint]="$correction_state"
      done < <(git diff --name-status --no-renames "$planning_sha" "$correction_sha")
      (( ${#correction_actual} == ${#CORRECTION} )) || return 1
      [[ -z "$(git diff --name-only "$planning_sha" "$correction_sha" -- "$REPORT_REL")" ]] || return 1
      [[ "$(git diff --name-only "$planning_sha" "$correction_sha" | LC_ALL=C sort | tr '\n' '\0')" == "$(printf '%s\n' "${(@k)CORRECTION}" | LC_ALL=C sort | tr '\n' '\0')" ]] || return 1
      verify_correction_commit "$correction_sha"
      append_correction_phase_event correction-commit-post; validate_correction_events correction-commit-post;;
    *) die "unknown correction phase [$phase]";;
  esac
}

# Executed only by the successor authority's disposable clone. It drives the real correction-pre gate
# with a successor rebound ledger, then proves each failure-record mutation is rejected by that gate.
successor_correction_pre_fixture() {
  local planning_sha="$1" saved prepared ledger_case endpoint lineage output controls=0 inventory_count prepare_count pre_count artifact_root ledger_before artifact_before
  local -a first_cases
  lineage="$(successor_lineage "$planning_sha")"; { [[ "$lineage" == first ]] || [[ "$lineage" == cleanup ]] || [[ "$lineage" == cleanup-committed ]] || [[ "$lineage" == simplified ]] || [[ "$lineage" == diagnostic ]] || [[ "$lineage" == warnings ]] || [[ "$lineage" == comprehensive ]] || [[ "$lineage" == multiline ]] || [[ "$lineage" == direct ]] || [[ "$lineage" == brace ]] || [[ "$lineage" == record ]] || [[ "$lineage" == manifest ]] || [[ "$lineage" == scope ]] || [[ "$lineage" == reconcile ]]; } || die 'successor correction fixture requires known successor lineage'
  read_correction_map
  for endpoint in "${(@k)CORRECTION}"; do git restore -- "$endpoint"; done
  read_status; [[ "${STATUS[$REPORT_REL]-}" == M && ${#STATUS} == 1 ]] || die 'successor correction fixture did not retain report-only state'
  saved="$(mktemp "${TMPDIR:-/tmp}/task-6.1-successor-correction.XXXXXX")"; prepared="$(mktemp "${TMPDIR:-/tmp}/task-6.1-successor-correction-prepared.XXXXXX")"; artifact_root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-successor-artifacts.XXXXXX")"; cp -- "$LEDGER_REL" "$saved"
  cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log "$artifact_root/first-log"; cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime.q9vybg/rhythhaus-red-recovery-summary.g4hQK1 "$artifact_root/first-summary"; cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output-fresh.JM8mHw "$artifact_root/second-log"; cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-recovery-summary.h8gzST "$artifact_root/second-outer"; cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ "$artifact_root/second-fixture"
  export TASK_6_1_FIRST_LOG="$artifact_root/first-log" TASK_6_1_FIRST_SUMMARY="$artifact_root/first-summary" TASK_6_1_SECOND_LOG="$artifact_root/second-log" TASK_6_1_SECOND_OUTER_SUMMARY="$artifact_root/second-outer" TASK_6_1_SECOND_FIXTURE_SUMMARY="$artifact_root/second-fixture"
  perl -0pi -e 's/^Event: .*\n//mg' "$LEDGER_REL"
  correction_gate correction-prepare "$planning_sha"; cp -- "$LEDGER_REL" "$prepared"
  inventory_count="$(grep -c '^Correction inventory: BEGIN$' "$prepared")"; prepare_count="$(grep -c '^Event: 1 CORRECTION PREPARE PASS ' "$prepared")"
  [[ "$inventory_count" == 1 && "$prepare_count" == 1 ]] || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
  read_correction_inventory "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }
  awk '/^Correction inventory: BEGIN$/{copy=1} copy{print} /^Frozen report SHA-256: /{exit}' "$prepared" >> "$LEDGER_REL"
  expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
  cp -- "$prepared" "$LEDGER_REL"
  first_cases=(missing malformed wrong duplicate reorder)
  [[ "$lineage" == cleanup || "$lineage" == cleanup-committed || "$lineage" == simplified || "$lineage" == diagnostic || "$lineage" == warnings || "$lineage" == comprehensive || "$lineage" == multiline || "$lineage" == direct || "$lineage" == brace || "$lineage" == record || "$lineage" == manifest || "$lineage" == scope || "$lineage" == reconcile ]] && first_cases+=(orphan-shared orphan-artifact orphan-cleanup unmatched-end nested)
  for ledger_case in "${first_cases[@]}"; do
    cp -- "$prepared" "$LEDGER_REL"
    case "$ledger_case" in
      missing) perl -0pi -e 's/^Failed RED_RECOVERY attempt: BEGIN\n.*?^Failed RED_RECOVERY attempt: END\n//ms' "$LEDGER_REL";;
      malformed) perl -0pi -e 's/^Stage: PRE_WORKTREE$/Stage: RECOVERY/m' "$LEDGER_REL";;
      wrong) perl -0pi -e 's/^Log: .*/Log: wrong SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";;
      duplicate) print -r -- 'Failed RED_RECOVERY attempt: END' >> "$LEDGER_REL";;
      reorder) perl -0pi -e 's/(Stage: PRE_WORKTREE\n)(Observed error: cleanup_retry_fixture:1: fixture_root: parameter not set\n)/$2$1/' "$LEDGER_REL";;
      orphan-shared) print -r -- 'Stage: PRE_WORKTREE' >> "$LEDGER_REL";;
      orphan-artifact) print -r -- 'Log: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log SHA-256: be69ba885c0f14dc609f030e9425ca65be5b6c74483becdca972bb29c4326454' >> "$LEDGER_REL";;
      orphan-cleanup) print -r -- 'Cleanup outcome: FAIL' >> "$LEDGER_REL";;
      unmatched-end) print -r -- 'Failed RED_RECOVERY attempt: END' >> "$LEDGER_REL";;
      nested) print -r -- 'Failed RED_RECOVERY attempt: BEGIN' >> "$LEDGER_REL";;
    esac
    expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
  done
  cp -- "$prepared" "$LEDGER_REL"
  if [[ "$lineage" == first ]]; then
    print -r -- 'Failed RED_RECOVERY attempt 2: BEGIN' >> "$LEDGER_REL"
    expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    cp -- "$prepared" "$LEDGER_REL"
  else
    for ledger_case in missing malformed wrong duplicate reorder cleanup-outcome artifact-byte near-miss-summary; do
      cp -- "$prepared" "$LEDGER_REL"
      cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ "$artifact_root/second-fixture"
      ledger_before="$(sha256 "$LEDGER_REL")"; artifact_before="$(sha256 "$artifact_root/second-fixture")"
      [[ "$artifact_before" == a958917a3dc2495d28e4475e141aeaa48d9bf8b0a7104be7c4a4d3123a6ed44c ]] || return 1
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Failed RED_RECOVERY attempt 2: BEGIN\n.*?^Failed RED_RECOVERY attempt 2: END\n//ms' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Primary error: .*/Primary error: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Log: .*fresh\.JM8mHw.*/Log: wrong SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) awk '/^Failed RED_RECOVERY attempt 2: BEGIN$/{on=1}on{print}/^Failed RED_RECOVERY attempt 2: END$/{exit}' "$prepared" >> "$LEDGER_REL";;
        reorder) perl -0pi -e 's/(Primary error:.*\n)(Secondary error: fixture_remove: attempts: parameter not set\n)/$2$1/' "$LEDGER_REL";;
        cleanup-outcome) perl -0pi -e 's/^Cleanup outcome: FAIL$/Cleanup outcome: PASS/m' "$LEDGER_REL";;
        artifact-byte) print -r -- mutation >> "$artifact_root/second-fixture";;
        near-miss-summary) perl -0pi -e 's/rhythhaus-red-cleanup-summary\.vTAoIQ/rhythhaus-red-cleanup-summary.near-miss.vTAoIQ/' "$LEDGER_REL";;
      esac
      [[ "$(sha256 "$LEDGER_REL")" != "$ledger_before" || "$(sha256 "$artifact_root/second-fixture")" != "$artifact_before" ]] || return 1
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
      cp -- "$prepared" "$LEDGER_REL"; cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ "$artifact_root/second-fixture"
      [[ "$(sha256 "$LEDGER_REL")" == "$(sha256 "$prepared")" && "$(sha256 "$artifact_root/second-fixture")" == "$artifact_before" ]] || return 1
    done
    cp -- /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ "$artifact_root/second-fixture"
    cp -- "$prepared" "$LEDGER_REL"
  fi
  if [[ "$lineage" == cleanup-committed ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*/Committed cleanup successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*/Committed cleanup successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Committed cleanup successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == simplified ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Simplified recovery successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Simplified recovery successor plan SHA: .*/Simplified recovery successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Simplified recovery successor plan SHA: .*/Simplified recovery successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Simplified recovery successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == diagnostic ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Diagnostic successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Diagnostic successor plan SHA: .*/Diagnostic successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Diagnostic successor plan SHA: .*/Diagnostic successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Diagnostic successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == warnings ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Warnings successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Warnings successor plan SHA: .*/Warnings successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Warnings successor plan SHA: .*/Warnings successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Warnings successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == comprehensive ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Comprehensive successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Comprehensive successor plan SHA: .*/Comprehensive successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Comprehensive successor plan SHA: .*/Comprehensive successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Comprehensive successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == multiline ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Multiline successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Multiline successor plan SHA: .*/Multiline successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Multiline successor plan SHA: .*/Multiline successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Multiline successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == direct ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Direct successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Direct successor plan SHA: .*/Direct successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Direct successor plan SHA: .*/Direct successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Direct successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == brace ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Brace successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Brace successor plan SHA: .*/Brace successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Brace successor plan SHA: .*/Brace successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Brace successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == record ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Record successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Record successor plan SHA: .*/Record successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Record successor plan SHA: .*/Record successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Record successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == manifest ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Manifest successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Manifest successor plan SHA: .*/Manifest successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Manifest successor plan SHA: .*/Manifest successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Manifest successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == scope ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Scope successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Scope successor plan SHA: .*/Scope successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Scope successor plan SHA: .*/Scope successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Scope successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  elif [[ "$lineage" == reconcile ]]; then
    for ledger_case in missing malformed wrong duplicate; do
      cp -- "$prepared" "$LEDGER_REL"
      case "$ledger_case" in
        missing) perl -0pi -e 's/^Reconcile successor plan SHA: .*\n//m' "$LEDGER_REL";;
        malformed) perl -0pi -e 's/^Reconcile successor plan SHA: .*/Reconcile successor plan SHA: malformed/m' "$LEDGER_REL";;
        wrong) perl -0pi -e 's/^Reconcile successor plan SHA: .*/Reconcile successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
        duplicate) print -r -- "Reconcile successor plan SHA: $planning_sha" >> "$LEDGER_REL";;
      esac
      expect_fail correction_gate correction-pre "$planning_sha" || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
    done
    cp -- "$prepared" "$LEDGER_REL"
  fi
  correction_gate correction-pre "$planning_sha"; pre_count="$(grep -c '^Event: 2 CORRECTION PRE PASS ' "$LEDGER_REL")"; [[ "$pre_count" == 1 ]] || { rm -f -- "$saved" "$prepared"; return 1; }; ((++controls))
  unset TASK_6_1_FIRST_LOG TASK_6_1_FIRST_SUMMARY TASK_6_1_SECOND_LOG TASK_6_1_SECOND_OUTER_SUMMARY TASK_6_1_SECOND_FIXTURE_SUMMARY; rm -rf -- "$artifact_root"; rm -f -- "$saved" "$prepared"
  if [[ "$lineage" == first ]]; then
    [[ "$controls" == 9 ]] || return 1; print -r -- "historical_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == cleanup ]]; then
    [[ "$controls" == 21 ]] || return 1; print -r -- "cleanup_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == cleanup-committed ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "committed_cleanup_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == simplified ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "simplified_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == diagnostic ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "diagnostic_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == warnings ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "warnings_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == comprehensive ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "comprehensive_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == multiline ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "multiline_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == direct ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "direct_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == brace ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "brace_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == record ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "record_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == manifest ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "manifest_successor_correction_fixture=PASS controls=$controls"
  elif [[ "$lineage" == reconcile ]]; then
    [[ "$controls" == 25 ]] || return 1; print -r -- "reconcile_successor_correction_fixture=PASS controls=$controls"
  else
    [[ "$controls" == 25 ]] || return 1; print -r -- "scope_successor_correction_fixture=PASS controls=$controls"
  fi
}

correct() {
  local planning_sha="$1" correction_sha="$2"; shift 2
  correction_gate correction-prepare "$planning_sha" "$correction_sha"
  correction_gate correction-pre "$planning_sha" "$correction_sha"
  if "$@"; then :; else return $?; fi
  correction_gate correction-stage-pre "$planning_sha" "$correction_sha"
  for endpoint in "${(@k)CORRECTION}"; do git add -- "$endpoint"; done
  correction_gate correction-stage-post "$planning_sha" "$correction_sha"
  correction_gate correction-commit-pre "$planning_sha" "$correction_sha"
  if git commit -m 'fix: apply library extraction correction'; then :; else return $?; fi
  correction_gate correction-commit-post "$planning_sha" "$(git rev-parse HEAD)"
}

final_cumulative() {
  local planning_sha="$1" correction_sha="$2" implementation_sha="$3"; shift 3
  [[ "$(git rev-parse HEAD^)" == "$implementation_sha" ]] || return 1
  accept_cumulative "$planning_sha" "$correction_sha" "$implementation_sha" && "$@"
}

run_correction() {
  local planning_sha="$1"; shift
  correction_gate correction-pre "$planning_sha"
  "$@"
}

only_ledger_exception() {
  local endpoint
  for endpoint in "${(@k)STATUS}"; do
    [[ "$endpoint" == "$LEDGER_REL" || ${+MANIFEST[$endpoint]} || ( "$endpoint" == "$REPORT_REL" && "$(sha256 "$endpoint")" == "2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5" ) ]] || return 1
  done
}
only_closeout_paths() {
  local endpoint allowed=0 candidate
  for endpoint in "${(@k)STATUS}"; do
    allowed=0; for candidate in "${CLOSEOUT_PATHS[@]}"; do [[ "$endpoint" == "$candidate" ]] && allowed=1; done
    (( allowed )) || return 1
  done
}
exact_closeout_commit() {
  local implementation_sha="$1" state endpoint
  typeset -A actual
  actual=()
  while IFS=$'\t' read -r state endpoint; do
    [[ -n "$state" && -n "$endpoint" && ( "$state" == A || "$state" == M ) ]] || return 1
    (( ! ${+actual[$endpoint]} )) || return 1
    actual[$endpoint]="$state"
  done < <(git diff --name-status --no-renames "$implementation_sha" HEAD)
  (( ${#actual} == ${#CLOSEOUT_STATUS} )) || return 1
  for endpoint in "${(@k)CLOSEOUT_STATUS}"; do
    [[ "${actual[$endpoint]-}" == "${CLOSEOUT_STATUS[$endpoint]}" && -f "$endpoint" ]] || return 1
  done
}

gate() {
  local phase="$1" planning_sha="$2" implementation_sha="${3:-}" endpoint
  read_manifest; read_status
  local current_sha="$(git rev-parse HEAD)" correction_sha="${TASK_6_1_CORRECTION_SHA:-}"
  local committed_sha="${implementation_sha:-${TASK_6_1_IMPLEMENTATION_SHA:-}}"
  [[ "$(brief_sha "$BRIEF_REL")" == "$planning_sha" ]] || return 1
  case "$phase" in
    producer-pre) empty_index && [[ "$current_sha" == "${correction_sha:-$planning_sha}" ]] && check_baseline "$planning_sha" && only_ledger_exception || return 1
      for endpoint in "${(@k)STATUS}"; do [[ "$endpoint" == "$LEDGER_REL" || ( "$endpoint" == "$REPORT_REL" && "$(sha256 "$endpoint")" == "2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5" ) || "${STATUS[$endpoint]}" == "${MANIFEST[$endpoint]-}" ]] || return 1; done;;
    producer-post) empty_index && [[ "$current_sha" == "${correction_sha:-$planning_sha}" ]] && read_rebound_inventory "$planning_sha" && successor_failed_attempt_authority "$planning_sha" && only_ledger_exception && verify_inventory;;
    staging-pre) empty_index && [[ "$current_sha" == "${correction_sha:-$planning_sha}" ]] && read_rebound_inventory "$planning_sha" && successor_failed_attempt_authority "$planning_sha" && same_records && verify_inventory;;
    staging-post)
      [[ "$current_sha" == "${correction_sha:-$planning_sha}" ]] || return 1
      ! empty_index || return 1; same_records_with_ledger || return 1; read_rebound_inventory "$planning_sha" || return 1; successor_failed_attempt_authority "$planning_sha" || return 1; verify_inventory_index || return 1
      [[ -z "$(git diff --name-only)" ]] || return 1;;
    commit-pre)
      [[ "$current_sha" == "${correction_sha:-$planning_sha}" ]] || return 1
      ! empty_index || return 1; same_records_with_ledger || return 1; read_rebound_inventory "$planning_sha" || return 1; successor_failed_attempt_authority "$planning_sha" || return 1; verify_inventory_index || return 1
      [[ -z "$(git diff --name-only)" ]] || return 1;;
    commit-post) [[ -n "$committed_sha" ]] && empty_index && [[ -z "$(git diff --name-only)" ]] && [[ "$current_sha" == "$committed_sha" ]] && [[ "$(git rev-parse HEAD^)" == "${correction_sha:-$planning_sha}" ]] && read_rebound_inventory "$planning_sha" && successor_failed_attempt_authority "$planning_sha" && verify_inventory_commit "$committed_sha";;
    closeout-pre) empty_index && [[ -n "$implementation_sha" ]] && [[ "$(git rev-parse HEAD)" == "$implementation_sha" ]] && accept_implementation "$planning_sha" "$implementation_sha" && only_closeout_paths;;
    closeout-post) empty_index && [[ -n "$implementation_sha" ]] && [[ "$(git rev-parse HEAD^)" == "$implementation_sha" ]] && exact_closeout_commit "$implementation_sha" && accept_implementation "$planning_sha" "$implementation_sha" && [[ -f '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md' && -f '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-final-acceptance-report.md' ]];;
    final) [[ -n "${TASK_6_1_CORRECTION_SHA-}" ]] && [[ -n "$implementation_sha" ]] && [[ "$(git rev-parse HEAD^)" == "$implementation_sha" ]] && exact_closeout_commit "$implementation_sha" && accept_cumulative "$planning_sha" "$TASK_6_1_CORRECTION_SHA" "$implementation_sha" && [[ -f '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-final-acceptance-report.md' ]];;
    *) die "unknown distinct phase [$phase]";;
  esac
}

# Pre -> action -> cleanup -> post. Action failure wins; otherwise cleanup then post-gate wins.
run_gated() {
  local pre="$1" post="$2" planning_sha="$3" implementation_sha="$4"; shift 4
  local action_rc cleanup_rc=0 post_rc=0 gate_rc post_sha cleanup_command
  local had_impl=${+TASK_6_1_IMPLEMENTATION_SHA} had_cleanup=${+TASK_6_1_RUN_GATED_CLEANUP_COMMAND}
  local saved_impl="${TASK_6_1_IMPLEMENTATION_SHA-}" saved_cleanup="${TASK_6_1_RUN_GATED_CLEANUP_COMMAND-}"
  typeset -g TASK_6_1_RUN_GATED_TMP=''
  typeset -g TASK_6_1_RUN_GATED_CLEANUP_STATUS='NOT_RUN'
  typeset -g TASK_6_1_RUN_GATED_CLEANUP_ATTEMPTS=0
  cleanup_command="${TASK_6_1_RUN_GATED_CLEANUP_COMMAND:-/bin/rm}"
  run_gated_cleanup() {
    local cleanup_path="${TASK_6_1_RUN_GATED_TMP-}"
    (( TASK_6_1_RUN_GATED_CLEANUP_ATTEMPTS == 0 )) || return 0
    TASK_6_1_RUN_GATED_CLEANUP_ATTEMPTS=1
    trap - EXIT
    [[ -z "$cleanup_path" ]] && return 0
    if command "$cleanup_command" -rf -- "$cleanup_path"; then TASK_6_1_RUN_GATED_CLEANUP_STATUS='PASS'; else TASK_6_1_RUN_GATED_CLEANUP_STATUS="FAIL:$?"; fi
    TASK_6_1_RUN_GATED_TMP=''
  }
  trap 'run_gated_cleanup' EXIT
  if gate "$pre" "$planning_sha" "$implementation_sha"; then
    :
  else
    gate_rc=$?
    trap - EXIT
    if (( had_impl )); then TASK_6_1_IMPLEMENTATION_SHA="$saved_impl"; else unset TASK_6_1_IMPLEMENTATION_SHA; fi
    if (( had_cleanup )); then TASK_6_1_RUN_GATED_CLEANUP_COMMAND="$saved_cleanup"; else unset TASK_6_1_RUN_GATED_CLEANUP_COMMAND; fi
    return "$gate_rc"
  fi
  TASK_6_1_RUN_GATED_TMP="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-wrapper.XXXXXX")" || {
    trap - EXIT
    if (( had_impl )); then TASK_6_1_IMPLEMENTATION_SHA="$saved_impl"; else unset TASK_6_1_IMPLEMENTATION_SHA; fi
    if (( had_cleanup )); then TASK_6_1_RUN_GATED_CLEANUP_COMMAND="$saved_cleanup"; else unset TASK_6_1_RUN_GATED_CLEANUP_COMMAND; fi
    return 1
  }
  if "$@"; then action_rc=0; else action_rc=$?; fi
  run_gated_cleanup
  [[ "$TASK_6_1_RUN_GATED_CLEANUP_STATUS" == PASS ]] || cleanup_rc=1
  if (( action_rc != 0 )); then gate_rc="$action_rc"
  elif (( cleanup_rc != 0 )); then gate_rc="$cleanup_rc"
  else
    post_sha="${TASK_6_1_IMPLEMENTATION_SHA:-$implementation_sha}"
    if gate "$post" "$planning_sha" "$post_sha"; then post_rc=0; else post_rc=$?; fi
    gate_rc="$post_rc"
  fi
  trap - EXIT
  if (( had_impl )); then TASK_6_1_IMPLEMENTATION_SHA="$saved_impl"; else unset TASK_6_1_IMPLEMENTATION_SHA; fi
  if (( had_cleanup )); then TASK_6_1_RUN_GATED_CLEANUP_COMMAND="$saved_cleanup"; else unset TASK_6_1_RUN_GATED_CLEANUP_COMMAND; fi
  return "$gate_rc"
}

# Lifecycle amendment is deliberately a two-state transition.  Before the planning commit, only the
# two plan paths are allowed in the working-tree delta and the unrelated dirty report/source/test
# paths are merely preserved.  After the exact two-path commit, those plans are clean and rebind
# writes the controller-owned brief/ledger state.  No gate requires the two plans to remain dirty
# after their commit.
readonly AMENDMENT_LIBRARY_PLAN=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly AMENDMENT_POINTER_PLAN=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly GENERATED_IGNORED_OUTPUT_RULE='ignored generated outputs: .gradle/**, .idea/**, .kotlin/**, .opencode/node_modules/**, **/build/**, and taglib Android native libraries'
exact_two_plan_set() {
  local -a changed=("$@")
  (( ${#changed} == 2 )) || return 1
  (( changed[(Ie)$AMENDMENT_LIBRARY_PLAN] > 0 && changed[(Ie)$AMENDMENT_POINTER_PLAN] > 0 ))
}

generated_ignored_output() {
  local endpoint_path="$1"
  case "$endpoint_path" in
    .gradle/*|.idea/*|.kotlin/*|.opencode/node_modules/*|build/*|*/build/*|taglib/src/androidMain/jniLibs/*/*.so)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

planning_amendment_gate() {
  local -a allowed=("$AMENDMENT_LIBRARY_PLAN" "$AMENDMENT_POINTER_PLAN") changed
  [[ -z "$(git diff --cached --name-only)" ]] || return 1
  changed=("${(@f)$(git diff --name-only -- "${allowed[@]}")}")
  exact_two_plan_set "${changed[@]}" || return 1
  # Other pre-existing report/source/test dirt is excluded from this two-plan working-tree gate.
  return 0
}

planning_amendment_membership_fixture() {
  local -a changed
  changed=("$AMENDMENT_LIBRARY_PLAN" "$AMENDMENT_POINTER_PLAN"); exact_two_plan_set "${changed[@]}" || return 1
  changed=("$AMENDMENT_POINTER_PLAN" "$AMENDMENT_LIBRARY_PLAN"); exact_two_plan_set "${changed[@]}" || return 1
  changed=("$AMENDMENT_LIBRARY_PLAN" "$AMENDMENT_LIBRARY_PLAN"); exact_two_plan_set "${changed[@]}" && return 1
  changed=("$AMENDMENT_POINTER_PLAN"); exact_two_plan_set "${changed[@]}" && return 1
  changed=("$AMENDMENT_LIBRARY_PLAN" "$AMENDMENT_POINTER_PLAN" unexpected-path); exact_two_plan_set "${changed[@]}" && return 1
  return 0
}

snapshot_nonplan_untracked() {
  local output="$1" endpoint_path
  : > "$output"
  while IFS= read -r -d '' endpoint_path; do
    case "$endpoint_path" in "$AMENDMENT_LIBRARY_PLAN"|"$AMENDMENT_POINTER_PLAN"|"$BRIEF_REL"|"$LEDGER_REL"|"$TASK_6_1_PREEXISTING_DIRTY_ROOT"/*) continue;; esac
    if [[ -L "$endpoint_path" ]]; then
      # Perl reads the link payload directly: command substitution must never trim a legal trailing LF.
      print -r -- "$endpoint_path=SYMLINK:$(perl -MDigest::SHA=sha256_hex -e 'defined($t = readlink $ARGV[0]) or exit 1; print sha256_hex($t)' "$endpoint_path")" >> "$output"
    elif [[ -f "$endpoint_path" ]]; then
      print -r -- "$endpoint_path=FILE:$(sha256 "$endpoint_path")" >> "$output"
    else
      print -r -- "$endpoint_path=OTHER" >> "$output"
    fi
  done < <(git ls-files --others --exclude-standard -z)
  while IFS= read -r -d '' endpoint_path; do
    case "$endpoint_path" in "$AMENDMENT_LIBRARY_PLAN"|"$AMENDMENT_POINTER_PLAN"|"$BRIEF_REL"|"$LEDGER_REL"|"$TASK_6_1_PREEXISTING_DIRTY_ROOT"/*) continue;; esac
    generated_ignored_output "$endpoint_path" && continue
    if [[ -L "$endpoint_path" ]]; then
      print -r -- "$endpoint_path=SYMLINK:$(perl -MDigest::SHA=sha256_hex -e 'defined($t = readlink $ARGV[0]) or exit 1; print sha256_hex($t)' "$endpoint_path")" >> "$output"
    elif [[ -f "$endpoint_path" ]]; then
      print -r -- "$endpoint_path=FILE:$(sha256 "$endpoint_path")" >> "$output"
    else
      print -r -- "$endpoint_path=OTHER" >> "$output"
    fi
  done < <(git ls-files --others --ignored --exclude-standard -z | LC_ALL=C sort -z -u)
}

snapshot_preexisting_nonplan_dirt() {
  typeset -g TASK_6_1_PREEXISTING_DIRTY_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-preexisting.XXXXXX")" || return 1
  git status --porcelain=v1 -z --ignored=matching -- . ':(exclude)'"$AMENDMENT_LIBRARY_PLAN" ':(exclude)'"$AMENDMENT_POINTER_PLAN" ':(exclude)'"$BRIEF_REL" ':(exclude)'"$LEDGER_REL" > "$TASK_6_1_PREEXISTING_DIRTY_ROOT/status.z"
  git diff --binary -- . ':(exclude)'"$AMENDMENT_LIBRARY_PLAN" ':(exclude)'"$AMENDMENT_POINTER_PLAN" ':(exclude)'"$BRIEF_REL" ':(exclude)'"$LEDGER_REL" > "$TASK_6_1_PREEXISTING_DIRTY_ROOT/tracked.patch"
  snapshot_nonplan_untracked "$TASK_6_1_PREEXISTING_DIRTY_ROOT/untracked-ignored.sha"
  cp -- "$BRIEF_REL" "$TASK_6_1_PREEXISTING_DIRTY_ROOT/brief.before"; cp -- "$LEDGER_REL" "$TASK_6_1_PREEXISTING_DIRTY_ROOT/ledger.before"
  typeset -g TASK_6_1_PREEXISTING_DIRTY_SHA="$(cd "$TASK_6_1_PREEXISTING_DIRTY_ROOT" && shasum -a 256 status.z tracked.patch untracked-ignored.sha | shasum -a 256 | awk '{print $1}')"
}

assert_preexisting_nonplan_dirt() {
  local after_root after before_status before_patch before_untracked after_status after_patch after_untracked
  [[ -n "${TASK_6_1_PREEXISTING_DIRTY_ROOT-}" && -d "$TASK_6_1_PREEXISTING_DIRTY_ROOT" ]] || die 'missing pre-existing dirt snapshot'
  after_root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-preexisting-after.XXXXXX")" || die 'cannot create post-amendment dirt snapshot'
  git status --porcelain=v1 -z --ignored=matching -- . ':(exclude)'"$AMENDMENT_LIBRARY_PLAN" ':(exclude)'"$AMENDMENT_POINTER_PLAN" ':(exclude)'"$BRIEF_REL" ':(exclude)'"$LEDGER_REL" > "$after_root/status.z"
  git diff --binary -- . ':(exclude)'"$AMENDMENT_LIBRARY_PLAN" ':(exclude)'"$AMENDMENT_POINTER_PLAN" ':(exclude)'"$BRIEF_REL" ':(exclude)'"$LEDGER_REL" > "$after_root/tracked.patch"
  snapshot_nonplan_untracked "$after_root/untracked-ignored.sha"
  after="$(cd "$after_root" && shasum -a 256 status.z tracked.patch untracked-ignored.sha | shasum -a 256 | awk '{print $1}')"
  before_status="$(sha256 "$TASK_6_1_PREEXISTING_DIRTY_ROOT/status.z")"; before_patch="$(sha256 "$TASK_6_1_PREEXISTING_DIRTY_ROOT/tracked.patch")"; before_untracked="$(sha256 "$TASK_6_1_PREEXISTING_DIRTY_ROOT/untracked-ignored.sha")"
  after_status="$(sha256 "$after_root/status.z")"; after_patch="$(sha256 "$after_root/tracked.patch")"; after_untracked="$(sha256 "$after_root/untracked-ignored.sha")"
  if cmp -s "$TASK_6_1_PREEXISTING_DIRTY_ROOT/status.z" "$after_root/status.z" && cmp -s "$TASK_6_1_PREEXISTING_DIRTY_ROOT/tracked.patch" "$after_root/tracked.patch" && cmp -s "$TASK_6_1_PREEXISTING_DIRTY_ROOT/untracked-ignored.sha" "$after_root/untracked-ignored.sha" && [[ "$after" == "$TASK_6_1_PREEXISTING_DIRTY_SHA" ]]; then
    rm -rf -- "$after_root"
    return 0
  fi
  print -u2 -r -- "task-6.1 proof: pre-existing non-plan dirt changed status.z=$before_status/$after_status tracked.patch=$before_patch/$after_patch untracked-ignored.sha=$before_untracked/$after_untracked aggregate=$TASK_6_1_PREEXISTING_DIRTY_SHA/$after"
  rm -rf -- "$after_root"
  return 1
}

assert_rebound_authorized_state() {
  local plan_sha="$1" prefix marker rows brief_count
  [[ "$(git rev-parse HEAD)" == "$plan_sha" ]] || die 'authorized rebind HEAD mismatch'
  [[ "$(awk '/^Planning baseline: [0-9a-f]{40}$/{print $3; n++} END{exit n == 1 ? 0 : 1}' "$BRIEF_REL")" == "$plan_sha" ]] || die 'brief planning-baseline transition mismatch'
  prefix="$(awk 'NR <= 6 {print}' "$LEDGER_REL" | shasum -a 256 | awk '{print $1}')"; [[ "$prefix" == d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0 ]] || die 'rebind changed fixed historical prefix'
  [[ "$(awk 'NR == 7 { print }' "$LEDGER_REL")" == 'Correction inventory: BEGIN' ]] || die 'rebind append-only ordering mismatch'
  rows="$(awk '/^Correction inventory: BEGIN$/{on=1} on{print} /^Correction inventory: END$/{exit}' "$LEDGER_REL")"; [[ "$(printf '%s\n' "$rows" | shasum -a 256 | awk '{print $1}')" == 8614b4de3e124c47cefb635f814f1882db3f4ffa13001325b89f83a93cd09984 && "$(printf '%s\n' "$rows" | awk 'NR > 1 && NR < 14 { n++ } END { print n+0 }')" == 12 ]] || die 'rebind correction marker mismatch'
  [[ "$(awk '/^Correction map SHA-256: /{print $4; n++} END{exit n == 1 ? 0 : 1}' "$LEDGER_REL")" == d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0 && "$(awk '/^Correction evidence prefix SHA-256: /{print $5; n++} END{exit n == 1 ? 0 : 1}' "$LEDGER_REL")" == d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0 && "$(awk '/^Amended library plan blob SHA-256: /{print $6; n++} END{exit n == 1 ? 0 : 1}' "$LEDGER_REL")" == "$(git show "$plan_sha:$PLAN_REL" | sha_bytes)" && "$(awk '/^Amended pointer plan blob SHA-256: /{print $6; n++} END{exit n == 1 ? 0 : 1}' "$LEDGER_REL")" == "$(git show "$plan_sha:$AMENDMENT_POINTER_PLAN" | sha_bytes)" && "$(awk '/^Frozen report SHA-256: /{print $4; n++} END{exit n == 1 ? 0 : 1}' "$LEDGER_REL")" == "$(sha256 "$REPORT_REL")" ]] || die 'rebind fixed authority row mismatch'
}

rebind_authority_negative_fixtures() {
  local plan_sha="$1" brief_saved ledger_saved frozen_report_case
  brief_saved="$(mktemp "${TMPDIR:-/tmp}/task-6.1-brief-sabotage.XXXXXX")"; ledger_saved="$(mktemp "${TMPDIR:-/tmp}/task-6.1-ledger-sabotage.XXXXXX")"
  cp -- "$BRIEF_REL" "$brief_saved"; cp -- "$LEDGER_REL" "$ledger_saved"
  perl -0pi -e 's/^Planning baseline: [0-9a-f]{40}$/Planning baseline: 0000000000000000000000000000000000000000/m' "$BRIEF_REL"
  expect_fail assert_rebound_authorized_state "$plan_sha" || return 1
  cp -- "$brief_saved" "$BRIEF_REL"
  perl -0pi -e 's/^Correction inventory: BEGIN$/Correction map SHA-256: d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0\nCorrection inventory: BEGIN/m' "$LEDGER_REL"
  expect_fail assert_rebound_authorized_state "$plan_sha" || return 1
  for frozen_report_case in missing malformed wrong duplicate; do
    cp -- "$ledger_saved" "$LEDGER_REL"
    case "$frozen_report_case" in
      missing) perl -0pi -e 's/^Frozen report SHA-256: .*\n//m' "$LEDGER_REL";;
      malformed) perl -0pi -e 's/^Frozen report SHA-256: .*/Frozen report SHA-256: malformed/m' "$LEDGER_REL";;
      wrong) perl -0pi -e 's/^Frozen report SHA-256: .*/Frozen report SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";;
      duplicate) print -r -- 'Frozen report SHA-256: 0000000000000000000000000000000000000000000000000000000000000000' >> "$LEDGER_REL";;
    esac
    expect_fail assert_rebound_authorized_state "$plan_sha" || return 1
  done
  cp -- "$ledger_saved" "$LEDGER_REL"; rm -f -- "$brief_saved" "$ledger_saved"
  assert_rebound_authorized_state "$plan_sha"
}

commit_planning_amendment() {
  planning_amendment_gate || die 'planning amendment working tree is not exactly the two authorized plans'
  git add -- "$AMENDMENT_LIBRARY_PLAN" "$AMENDMENT_POINTER_PLAN"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || die 'planning amendment index is not exact'
  git commit -m 'docs: amend library extraction recovery authority'
}

planning_amendment_dispatch() {
  local planning_head pre_commit_parent
  [[ "$(git rev-parse HEAD)" == 2e199950da3fa518c2491b3168cbb5fb86c4cefd ]] || die 'legacy planning-amendment is consumed; use successor-planning-amendment'
  planning_amendment_gate || die 'pre-commit amendment gate failed'
  snapshot_preexisting_nonplan_dirt || die 'cannot snapshot pre-existing non-plan dirt'
  pre_commit_parent="$(git rev-parse HEAD)"
  commit_planning_amendment >/dev/null || return $?
  planning_head="$(git rev-parse HEAD)"; [[ "$planning_head" =~ '^[0-9a-f]{40}$' ]] || die 'planning HEAD is not one SHA'
  [[ "$(git rev-parse HEAD)" == "$planning_head" && "$(git rev-parse "$planning_head^")" == "$pre_commit_parent" ]] || die 'planning commit parent mismatch'
  [[ "$(git diff-tree --no-commit-id --name-only -r "$planning_head" | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || die 'planning commit path set is not exact'
  [[ -z "$(git diff --name-only -- "$AMENDMENT_LIBRARY_PLAN" "$AMENDMENT_POINTER_PLAN")" ]] || die 'planning plans remain dirty after commit'
  assert_preexisting_nonplan_dirt
  rebind_after_planning_amendment "$planning_head"
  assert_rebound_authorized_state "$planning_head"
  rebind_authority_negative_fixtures "$planning_head" || die 'rebind authority negative fixture failed'
  assert_preexisting_nonplan_dirt
}

rebind_after_planning_amendment() {
  local plan_sha="$1" correction_map_sha correction_rows ledger_prefix brief_tmp ledger_tmp before_prefix after_prefix marker_sha baseline_count
  [[ "$(git rev-parse HEAD)" == "$plan_sha" ]] || die 'rebind requires amendment HEAD'
  [[ -z "$(git diff --cached --name-only)" ]] || die 'rebind requires empty index'
  [[ "$(git diff --name-only -- "$REPORT_REL")" == "$REPORT_REL" ]] || die 'rebind requires preserved pre-existing report dirt'
  correction_rows="$(awk '/^## Literal Corrective Checkpoint 2A Map$/{seen=1; next} seen && /^```text$/{open=1; next} open && /^```$/{exit} open {print}' "$PLAN_REL")"
  correction_map_sha="$(printf '%s\n' "$correction_rows" | shasum -a 256 | awk '{print $1}')"
  [[ "$correction_map_sha" == d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0 ]] || die 'amended twelve-row correction map digest mismatch'
  [[ "$(printf '%s\n' "$correction_rows" | wc -l | tr -d ' ')" == 12 ]] || die 'amended correction map count mismatch'
  before_prefix="$(awk 'NR <= 6 {print}' "$LEDGER_REL" | shasum -a 256 | awk '{print $1}')"
  [[ "$before_prefix" == d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0 ]] || die 'historical prefix mismatch before rebind'
  brief_tmp="$(mktemp "${TMPDIR:-/tmp}/task-6.1-brief.XXXXXX")" || die 'cannot create brief transition file'
  baseline_count="$(awk '/^Planning baseline: [0-9a-f]{40}$/{n++} END{print n+0}' "$TASK_6_1_PREEXISTING_DIRTY_ROOT/brief.before")"
  [[ "$baseline_count" == 1 ]] || die 'brief planning-baseline field count is not one'
  awk -v sha="$plan_sha" '/^Planning baseline: [0-9a-f]{40}$/ { print "Planning baseline: " sha; next } { print }' "$BRIEF_REL" > "$brief_tmp"
  [[ "$(awk '/^Planning baseline: [0-9a-f]{40}$/{print $3; n++} END{exit n == 1 ? 0 : 1}' "$brief_tmp")" == "$plan_sha" ]] || die 'brief transition replacement mismatch'
  mv -- "$brief_tmp" "$BRIEF_REL"
  ledger_prefix="$(mktemp "${TMPDIR:-/tmp}/task-6.1-ledger-prefix.XXXXXX")" || die 'cannot capture ledger prefix'
  awk 'NR <= 6 {print}' "$TASK_6_1_PREEXISTING_DIRTY_ROOT/ledger.before" > "$ledger_prefix"
  ledger_tmp="$(mktemp "${TMPDIR:-/tmp}/task-6.1-ledger.XXXXXX")" || die 'cannot create ledger transition file'
  {
    awk 'NR <= 6 {print}' "$LEDGER_REL"
    print -r -- 'Correction inventory: BEGIN'
    while IFS=$'\t' read -r state endpoint_path; do [[ "$state" == M && -n "$endpoint_path" ]] || die 'invalid correction row'; print -r -- "$endpoint_path"$'\tM\t'"$(git show "$plan_sha:$endpoint_path" | shasum -a 256 | awk '{print $1}')"; done <<< "$correction_rows"
    print -r -- 'Correction inventory: END'; print -r -- 'Correction map SHA-256: d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0'; print -r -- 'Correction evidence prefix SHA-256: d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0'; print -r -- "Amended library plan blob SHA-256: $(git show "$plan_sha:$PLAN_REL" | shasum -a 256 | awk '{print $1}')"; print -r -- "Amended pointer plan blob SHA-256: $(git show "$plan_sha:$AMENDMENT_POINTER_PLAN" | shasum -a 256 | awk '{print $1}')"; print -r -- "Frozen report SHA-256: $(sha256 "$REPORT_REL")"
  } > "$ledger_tmp"
  marker_sha="$(awk '/^Correction inventory: BEGIN$/{on=1} on{print} /^Correction inventory: END$/{exit}' "$ledger_tmp" | shasum -a 256 | awk '{print $1}')"
  [[ "$marker_sha" == 8614b4de3e124c47cefb635f814f1882db3f4ffa13001325b89f83a93cd09984 ]] || die 'approved twelve-row marker-block digest mismatch'
  mv -- "$ledger_tmp" "$LEDGER_REL"
  rm -f -- "$ledger_prefix"
  after_prefix="$(awk 'NR <= 6 {print}' "$LEDGER_REL" | shasum -a 256 | awk '{print $1}')"
  [[ "$after_prefix" == "$before_prefix" ]] || die 'rebind changed historical six-line prefix'
  [[ "$(awk '/^Correction inventory: BEGIN$/{on=1} on{n++} /^Correction inventory: END$/{exit} END{print n-2}' "$LEDGER_REL")" == 12 ]] || die 'rebind did not install exact twelve-row inventory'
  parser_blob_negative_fixtures "$plan_sha" || die 'amended blob parser negative fixture failed'
}
produce_and_inventory() { local sha="$1"; shift; "$@"; write_inventory "$sha"; }
produce() { local sha="$1"; shift; run_gated producer-pre producer-post "$sha" '' produce_and_inventory "$sha" "$@"; }
stage_manifest() { local sha="$1"; shift; run_gated staging-pre staging-post "$sha" '' "$@"; }
commit_manifest_action() { "$@"; TASK_6_1_IMPLEMENTATION_SHA="$(git rev-parse HEAD)"; }
commit_manifest() {
  local sha="$1"; shift
  local had_impl=${+TASK_6_1_IMPLEMENTATION_SHA} saved_impl="${TASK_6_1_IMPLEMENTATION_SHA-}"
  typeset -g TASK_6_1_IMPLEMENTATION_SHA=''
  local rc
  if run_gated commit-pre commit-post "$sha" '' commit_manifest_action "$@"; then rc=0; else rc=$?; fi
  if (( had_impl )); then TASK_6_1_IMPLEMENTATION_SHA="$saved_impl"; else unset TASK_6_1_IMPLEMENTATION_SHA; fi
  return "$rc"
}
closeout() { local sha="$1" implementation="$2"; shift 2; run_gated closeout-pre closeout-post "$sha" "$implementation" "$@"; }
finalize() { local sha="$1" implementation="$2"; shift 2; run_gated final final "$sha" "$implementation" "$@"; }

expect_fail() { local rc; if ( set -e; "$@" ) >/dev/null 2>&1; then rc=0; else rc=$?; fi; (( rc != 0 )); }
expect_rc() { local want="$1" rc; shift; if ( set -e; "$@" ) >/dev/null 2>&1; then rc=0; else rc=$?; fi; (( rc == want )); }
fixture_manifest() {
  local output="$1" i
  {
  print -r -- '## Literal Implementation Manifest'
  print -r -- '```text'
  for i in {1..51}; do print -r -- $'A\tnew/path-'"$i"'.kt'; done
  for endpoint in \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt \
    feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt \
    shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt \
    core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt \
    core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt \
    core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt \
    build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt \
    feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt \
    shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt; do
    print -r -- $'M\t'"$endpoint"
  done
  for i in {13..28}; do print -r -- $'M\tmodified/path-'"$i"'.kt'; done
  for i in {1..34}; do print -r -- $'D\tdeleted/path-'"$i"'.kt'; done
  print -r -- '```'
  print -r -- '## Literal Corrective Checkpoint 2A Map'
  print -r -- '```text'
  for endpoint in \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt \
    shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt \
    feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt \
    shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt \
    core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt \
    core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt \
    core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt \
    build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt \
    feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt \
    shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt; do
    print -r -- $'M\t'"$endpoint"
  done
  print -r -- '```'
  } > "$output"
}
make_fixture() {
  local repo="$1" authority_repo="$PWD" p i planning
  mkdir -p -- "$repo"; git -C "$repo" init -q; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  mkdir -p -- "$repo/${PLAN_REL:h}" "$repo/${LEDGER_REL:h}" "$repo/${BRIEF_REL:h}"
  fixture_manifest "$repo/$PLAN_REL"
  print -r -- baseline > "$repo/$AMENDMENT_POINTER_PLAN"
  mkdir -p -- "$repo/openspec/changes/feature-first-modularization" "$repo/.superpowers/sdd/2026-07-27-feature-first-modularization"
  print -r -- baseline > "$repo/openspec/changes/feature-first-modularization/tasks.md"
  print -r -- baseline > "$repo/progress.md"
  print -r -- baseline > "$repo/roadmap.md"
  print -r -- baseline > "$repo/$REPORT_REL"
  local -a correction_paths=(
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt'
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt'
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt'
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt'
    'feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt'
    'shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt'
    'core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt'
    'core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt'
    'core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt'
    'build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt'
    'feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt'
    'shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt'
  )
  for p in "${correction_paths[@]}"; do mkdir -p -- "$repo/${p:h}"; print -r -- baseline > "$repo/$p"; done
  for i in {13..28}; do p="modified/path-$i.kt"; mkdir -p -- "$repo/${p:h}"; print -r -- baseline > "$repo/$p"; done
  for i in {1..34}; do p="deleted/path-$i.kt"; mkdir -p -- "$repo/${p:h}"; print -r -- baseline > "$repo/$p"; done
  { print -r -- "$BRIEF_REL"; print -r -- "$LEDGER_REL"; print -r -- '/build/'; print -r -- '/undeclared-ignored/'; } > "$repo/.gitignore"
  git -C "$repo" add .; git -C "$repo" fetch -q "$authority_repo" 2e199950da3fa518c2491b3168cbb5fb86c4cefd
  planning="$(git -C "$repo" commit-tree "$(git -C "$repo" write-tree)" -p 2e199950da3fa518c2491b3168cbb5fb86c4cefd -m planning)"; git -C "$repo" reset -q --hard "$planning"
  print -r -- "Planning baseline: $planning" > "$repo/$BRIEF_REL"
  {
    print -r -- 'Planning baseline: 2e199950da3fa518c2491b3168cbb5fb86c4cefd'
    print -r -- 'Frozen inventory: BEGIN'
    print -r -- 'Checkpoint 1 Governance RED: PASS / APPROVED'
    print -r -- 'Checkpoint 1 commits: 3ae4a85ae30934e110064b33b5f1cb14d4694e32 fcbfa74c704b1b37926335b911054a1b61b9b879 3f2e164 e8b9934'
    print -r -- 'Checkpoint 1 verification: ArchitectureCheckPluginFunctionalTest 90/0/0/0; KmpConventionPluginsFunctionalTest 6/0/0/0; exact forced -D processor JAR invocation PASS'
    print -r -- 'Checkpoint 2 Module, API, Holder: IN PROGRESS'
  } > "$repo/$LEDGER_REL"
}

startup_architecture_fixture() {
  local source='build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt'
  mkdir -p -- "${source:h}"
  print -r -- 'fun startupOrder() { setLibraryDatabaseAndroidContext(); setRhythHausAndroidContext(); startKoin() }' > "$source"
  rg -n 'setLibraryDatabaseAndroidContext\(\).*setRhythHausAndroidContext\(\).*startKoin\(\)' "$source" >/dev/null || return 1
  perl -0pi -e 's/setLibraryDatabaseAndroidContext\(\); setRhythHausAndroidContext\(\); startKoin\(\)/setRhythHausAndroidContext(); setLibraryDatabaseAndroidContext(); startKoin()/' "$source"
  ! rg -n 'setLibraryAndroidContext\(\).*setLibraryDatabaseAndroidContext\(\)' "$source" >/dev/null
  rg -n 'setRhythHausAndroidContext\(\); setLibraryDatabaseAndroidContext\(\)' "$source" >/dev/null || return 1
  perl -0pi -e 's/setRhythHausAndroidContext\(\); setLibraryDatabaseAndroidContext\(\); startKoin\(\)/setLibraryDatabaseAndroidContext(); setRhythHausAndroidContext(); startKoin()/' "$source"
  rg -n 'setLibraryDatabaseAndroidContext\(\); setRhythHausAndroidContext\(\); startKoin\(\)' "$source" >/dev/null
  git checkout -- "$source"
}
matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo real_index before after host_index host_before host_after planning implementation casefile count=0 prefix_before prefix_after
  local -a residue_paths
  local -a correction_paths=(
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt'
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt'
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt'
    'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt'
    'feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt'
    'shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt'
    'core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.android.kt'
    'core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt'
    'core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt'
    'build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt'
    'feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt'
    'shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt'
  )
  host_index="$(git rev-parse --git-path index)"; host_before="$(sha256 "$host_index")"
  residue_paths=("${TMPDIR:-/tmp}"/task-6.1-{matrix,wrapper}.*(N)); for residue in "${residue_paths[@]}"; do rm -rf -- "$residue"; done
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  # This is the first parser control: it reads the literal plan shape, not a synthetic substitute.
  read_manifest "$PLAN_REL"; (( ${#MANIFEST} == 109 )); ((++count))
  planning_amendment_membership_fixture; ((++count))
  repo="$root/repo"; make_fixture "$repo"; cd "$repo"; planning="$(git rev-parse HEAD)"
  read_manifest; read_status; check_baseline "$planning"
  # These are causal: ordinary bytes and a symlink payload ending in a legal LF are independently
  # rejected by the same preservation assertion that brackets the real plan commit and rebind.
  print -r -- ordinary-before > ordinary-untracked.txt
  perl -e 'symlink "ordinary-target\n", "ordinary-untracked-link" or die $!'
  snapshot_preexisting_nonplan_dirt
  print -r -- ordinary-mutated > ordinary-untracked.txt
  expect_fail assert_preexisting_nonplan_dirt
  print -r -- ordinary-before > ordinary-untracked.txt
  assert_preexisting_nonplan_dirt
  rm -f -- ordinary-untracked-link
  perl -e 'symlink "ordinary-target-mutated\n", "ordinary-untracked-link" or die $!'
  expect_fail assert_preexisting_nonplan_dirt
  rm -f -- ordinary-untracked-link
  perl -e 'symlink "ordinary-target\n", "ordinary-untracked-link" or die $!'
  assert_preexisting_nonplan_dirt
  rm -f -- ordinary-untracked.txt ordinary-untracked-link
  ((++count))
  mkdir -p -- build undeclared-ignored
  print -r -- generated-before > build/generated-output.bin
  print -r -- undeclared-before > undeclared-ignored/important.txt
  snapshot_preexisting_nonplan_dirt
  print -r -- generated-after > build/generated-output.bin
  assert_preexisting_nonplan_dirt
  print -r -- undeclared-after > undeclared-ignored/important.txt
  expect_fail assert_preexisting_nonplan_dirt
  print -r -- undeclared-before > undeclared-ignored/important.txt
  assert_preexisting_nonplan_dirt
  rm -rf -- build undeclared-ignored
  startup_architecture_fixture; ((++count))
  # Correction uses a real report-only prepare, then a pre-gate, an external edit of exactly 12 paths,
  # stage-pre, staging, stage-post, commit-pre, commit, and commit-post. The wrong-state pre-gate is
  # intentionally rejected before the edit.
  print -r -- correction >> "$REPORT_REL"
  correction_action() { for endpoint in "${correction_paths[@]}"; do print -r -- correction >> "$endpoint"; done; }
  cp "$LEDGER_REL" "$root/ledger-historical-valid"
  for historical_case in duplicate-planning missing-planning reordered unknown malformed closed; do
    cp "$root/ledger-historical-valid" "$LEDGER_REL"
    case "$historical_case" in
      duplicate-planning) perl -0pi -e 's/(Planning baseline: [0-9a-f]{40}\n)/$1$1/' "$LEDGER_REL";;
      missing-planning) perl -0pi -e 's/^Planning baseline: [^\n]*\n//' "$LEDGER_REL";;
      reordered) perl -0pi -e 's/(Frozen inventory: BEGIN\n)(Checkpoint 1 Governance RED: PASS \/ APPROVED\n)/$2$1/' "$LEDGER_REL";;
      unknown) perl -0pi -e 's/(Frozen inventory: BEGIN\n)/$1Unknown historical record\n/' "$LEDGER_REL";;
      malformed) perl -0pi -e 's/Checkpoint 1 Governance RED: PASS \/ APPROVED/Checkpoint 1 Governance RED: malformed/' "$LEDGER_REL";;
      closed) print -r -- 'Frozen inventory: END' >> "$LEDGER_REL";;
    esac
    before="$(sha256 "$LEDGER_REL")"
    expect_fail validate_historical_prefix "$planning"
    after="$(sha256 "$LEDGER_REL")"
    [[ "$before" == "$after" ]] || die "rejected historical prefix changed ledger: $historical_case"
    cp "$root/ledger-historical-valid" "$LEDGER_REL"
    ((++count))
  done
  expect_fail read_correction_inventory 0000000000000000000000000000000000000000; ((++count))
  perl -0ne 'if (/(.*?)^Correction inventory: BEGIN\n/ms) { print $1 } else { print $_ }' "$LEDGER_REL" > "$root/ledger-prefix-before"
  prefix_before="$(shasum -a 256 "$root/ledger-prefix-before" | awk '{print $1}')"
  correction_gate correction-prepare "$planning"
  perl -0ne 'if (/(.*?)^Correction inventory: BEGIN\n/ms) { print $1 } else { print $_ }' "$LEDGER_REL" > "$root/ledger-prefix-after"
  prefix_after="$(shasum -a 256 "$root/ledger-prefix-after" | awk '{print $1}')"
  cmp -s "$root/ledger-prefix-before" "$root/ledger-prefix-after" && [[ "$prefix_before" == "$prefix_after" ]] || die 'historical ledger prefix changed during correction prepare'
  ((++count))
  correction_gate correction-pre "$planning"; ((++count))
  expect_fail correction_gate correction-stage-pre "$planning"; ((++count))
  correction_action; correction_gate correction-stage-pre "$planning"
  for endpoint in "${correction_paths[@]}"; do git add -- "$endpoint"; done
  correction_gate correction-stage-post "$planning"; correction_gate correction-commit-pre "$planning"
  git commit -qm correction; correction_sha="$(git rev-parse HEAD)"
  correction_gate correction-commit-post "$planning" "$correction_sha"; ((++count))
  export TASK_6_1_CORRECTION_SHA="$correction_sha"
  git restore -- "$REPORT_REL"
  TASK_6_1_REPORT_SHA="$(sha256 "$REPORT_REL")" perl -0pi -e 's/^Frozen report SHA-256: [0-9a-f]{64}$/"Frozen report SHA-256: $ENV{TASK_6_1_REPORT_SHA}"/me' "$LEDGER_REL"
  # The original planning SHA remains the final-manifest baseline; correction is an intervening
  # child and its 12 records are included by the later cumulative gate.
  git switch -q --detach "$correction_sha"
  for i in {1..34}; do rm -f -- "deleted/path-$i.kt"; done
  produce_action() {
    for i in {1..51}; do mkdir -p -- "new"; print -r -- new > "new/path-$i.kt"; done
    for endpoint in "${correction_paths[@]}"; do print -r -- changed >> "$endpoint"; done
    for i in {13..28}; do print -r -- changed >> "modified/path-$i.kt"; done
  }
  cp "$LEDGER_REL" "$root/ledger-before-produce"
  produce "$planning" produce_action; ((++count))
  # `read_inventory` parses the post-produce/rebound ledger; its correction boundary makes the four
  # authority rows mandatory. Exercise its own parser against removal of the whole group and every
  # individual row, then prove the restored rebound ledger remains accepted.
  cp "$LEDGER_REL" "$root/ledger-rebound"
  cp "$LEDGER_REL" "$root/ledger-authority"
  TASK_6_1_REPORT_SHA="$(sha256 "$REPORT_REL")" perl -0pi -e 's/^Frozen report SHA-256: [0-9a-f]{64}$/"Frozen report SHA-256: $ENV{TASK_6_1_REPORT_SHA}"/me' "$root/ledger-authority"
  perl -0pi -e 's/^Correction (map|evidence prefix) SHA-256: .*\n//mg; s/^Amended (library plan|pointer plan) blob SHA-256: .*\n//mg' "$LEDGER_REL"
  expect_fail read_inventory "$planning"; cp "$root/ledger-authority" "$LEDGER_REL"; ((++count))
  for authority_case in correction-prefix correction-map library-blob pointer-blob; do
    cp "$root/ledger-authority" "$LEDGER_REL"
    case "$authority_case" in
      correction-prefix) perl -0pi -e 's/^Correction evidence prefix SHA-256: .*\n//m' "$LEDGER_REL";;
      correction-map) perl -0pi -e 's/^Correction map SHA-256: .*\n//m' "$LEDGER_REL";;
      library-blob) perl -0pi -e 's/^Amended library plan blob SHA-256: .*\n//m' "$LEDGER_REL";;
      pointer-blob) perl -0pi -e 's/^Amended pointer plan blob SHA-256: .*\n//m' "$LEDGER_REL";;
    esac
    expect_fail read_inventory "$planning" 1; ((++count))
  done
  cp "$root/ledger-authority" "$LEDGER_REL"; read_inventory "$planning" 1; ((++count))
  for report_case in missing malformed wrong duplicate; do
    cp "$root/ledger-authority" "$LEDGER_REL"
    case "$report_case" in
      missing) perl -0pi -e 's/^Frozen report SHA-256: .*\n//m' "$LEDGER_REL";;
      malformed) perl -0pi -e 's/^Frozen report SHA-256: .*/Frozen report SHA-256: malformed/m' "$LEDGER_REL";;
      wrong) perl -0pi -e 's/^Frozen report SHA-256: .*/Frozen report SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";;
      duplicate) print -r -- 'Frozen report SHA-256: 0000000000000000000000000000000000000000000000000000000000000000' >> "$LEDGER_REL";;
    esac
    expect_fail gate staging-pre "$planning"; ((++count))
  done
  cp "$root/ledger-authority" "$LEDGER_REL"; gate staging-pre "$planning"; ((++count))
  correction_fixture="M\tshared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt"
  TASK_6_1_CORRECTION_FIXTURE="$correction_fixture"; expect_fail read_correction_map; unset TASK_6_1_CORRECTION_FIXTURE; ((++count))
  TASK_6_1_CORRECTION_FIXTURE="M\tshared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt\nM\tshared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt"; expect_fail read_correction_map; unset TASK_6_1_CORRECTION_FIXTURE; ((++count))
  cp "$LEDGER_REL" "$root/ledger-evidence"; perl -0pi -e 's/Frozen report SHA-256: [0-9a-f]{64}/Frozen report SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/' "$LEDGER_REL"; expect_fail correction_gate correction-pre "$planning"; cp "$root/ledger-evidence" "$LEDGER_REL"; ((++count))
  cp "$LEDGER_REL" "$root/ledger-malformed"; print -r -- 'Evidence: broken' >> "$LEDGER_REL"; expect_fail read_inventory "$planning"; cp "$root/ledger-malformed" "$LEDGER_REL"; ((++count))
  for evidence_case in ordering-drift missing-event duplicate-sequence malformed-event; do
    cp "$LEDGER_REL" "$root/ledger-$evidence_case"
    case "$evidence_case" in
      ordering-drift) perl -0pi -e 's/Event: 4 CORRECTION STAGE_POST/Event: 5 CORRECTION STAGE_POST/' "$LEDGER_REL";;
      missing-event) perl -0pi -e 's/Event: 5 CORRECTION COMMIT_PRE[^\n]*\n//' "$LEDGER_REL";;
      duplicate-sequence) perl -0pi -e 's/Event: 5 CORRECTION COMMIT_PRE/Event: 4 CORRECTION COMMIT_PRE/' "$LEDGER_REL";;
      malformed-event) perl -0pi -e 's/Event: 6 CORRECTION COMMIT_POST[^\n]*\n/Event: six CORRECTION COMMIT_POST PASS bad.txt\n/' "$LEDGER_REL";;
    esac
    expect_fail read_inventory "$planning"; cp "$root/ledger-$evidence_case" "$LEDGER_REL"; ((++count))
  done
  # Literal malformed heading/marker/fence/block/count grammar controls.
  for casefile in duplicate-heading duplicate-fence duplicate-fence-after-prose partial empty malformed duplicate-path wrong-count; do
    cp "$PLAN_REL" "$root/$casefile.md"
    case "$casefile" in
      duplicate-heading) print -r -- '## Literal Implementation Manifest' >> "$root/$casefile.md";;
      duplicate-fence) perl -0pi -e 's/(D\tdeleted\/path-34\.kt\n```\n)/$1```\n/' "$root/$casefile.md";;
      duplicate-fence-after-prose) perl -0pi -e 's/(D\tdeleted\/path-34\.kt\n```\n)/$1\nprose\n```text\nA\tforbidden.kt\n```\n/' "$root/$casefile.md";;
      partial) perl -0pi -e 's/(## Literal Implementation Manifest\n```text\n.*?)(```\n)/$1/s' "$root/$casefile.md";;
      empty) perl -0pi -e 's/(## Literal Implementation Manifest\n```text\n).*?\n```/$1```/s' "$root/$casefile.md";;
      malformed) perl -0pi -e 's/A\tnew\/path-1\.kt/X new\/path-1.kt/' "$root/$casefile.md";;
      duplicate-path) perl -0pi -e 's/A\tnew\/path-2\.kt/A\tnew\/path-1.kt/' "$root/$casefile.md";;
      wrong-count) perl -0pi -e 's/A\tnew\/path-51\.kt\n//' "$root/$casefile.md";;
    esac
    expect_fail read_manifest "$root/$casefile.md"; ((++count))
  done
  # Newline and space filename survives NUL parsing. R/UU/unknown all enter read_status itself.
  print -r -- newline > $'new/path with space\nand newline.kt'; read_status; expect_fail same_records; rm -f -- $'new/path with space\nand newline.kt'; ((++count))
  for casefile in 'R  renamed.kt' 'UU conflicted.kt' 'ZZ unknown.kt'; do
    TASK_6_1_STATUS_FIXTURE="$casefile"; expect_fail read_status; unset TASK_6_1_STATUS_FIXTURE; ((++count))
  done
  gate producer-pre "$planning" ''; ((++count))
  cp "$LEDGER_REL" "$root/ledger-prefix"; TASK_DIGEST="$(printf '0%.0s' {1..64})" perl -0pi -e 's/(Correction evidence prefix SHA-256: )[0-9a-f]{64}/$1 . $ENV{TASK_DIGEST}/e' "$LEDGER_REL"; expect_fail read_correction_inventory "$planning"; cp "$root/ledger-prefix" "$LEDGER_REL"; ((++count))
  print -r -- extra > extra-path; expect_fail gate producer-pre "$planning" ''; rm -f -- extra-path; ((++count))
  cp "$LEDGER_REL" "$root/ledger"; digest="$(printf '0%.0s' {1..64})"; TASK_DIGEST="$digest" perl -0pi -e 's{(new/path-1\.kt\tA\t)[0-9a-f]{64}}{$1 . $ENV{TASK_DIGEST}}e' "$LEDGER_REL"; read_inventory "$planning"; expect_fail verify_inventory; cp "$root/ledger" "$LEDGER_REL"; ((++count))
  git rm -q -- deleted/path-1.kt; git commit -qm baseline-without-deleted -- deleted/path-1.kt; missing_planning="$(git rev-parse HEAD)"; print -r -- "Planning baseline: $missing_planning" > "$BRIEF_REL"; expect_fail check_baseline "$missing_planning"; git switch -q --detach "$correction_sha"; rm -f -- deleted/path-1.kt; print -r -- "Planning baseline: $planning" > "$BRIEF_REL"; export TASK_6_1_CORRECTION_SHA="$correction_sha"; ((++count))
  git reset -q; gate staging-pre "$planning" ''; git add -A -- .; gate staging-post "$planning" ''
  # Stage/index invariant: stage-pre requires a clean index; stage-post requires the manifest in the index.
  git reset -q; expect_fail gate staging-post "$planning" ''; git add -A -- .; ((++count))
  expect_fail gate commit-pre 0000000000000000000000000000000000000000 ''; ((++count))
  git add -A -- .
  bypass_implementation_commit() { git commit -qm bypass-implementation; }
  bypass_implementation_commit; expect_fail gate commit-post "$planning" ''; ((++count))
  git switch -q --detach "$correction_sha"
  git clean -fdq -- .
  cp "$root/ledger-before-produce" "$LEDGER_REL"
  for i in {1..34}; do rm -f -- "deleted/path-$i.kt"; done
  produce "$planning" produce_action
  implementation_action() { git commit -qm implementation; }
  git add -A -- .; commit_manifest "$planning" implementation_action
  implementation="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$implementation^")" == "$correction_sha" ]] || die 'implementation parent is not correction SHA'
  git merge-base --is-ancestor "$correction_sha" "$implementation" || die 'implementation is not a correction descendant'
  print -r -- extra > extra-path; expect_fail gate closeout-pre "$planning" "$implementation"; rm -f -- extra-path; ((++count))
  for casefile in missing duplicate mismatch; do
    cp "$BRIEF_REL" "$root/brief"; case "$casefile" in missing) : > "$BRIEF_REL";; duplicate) print -r -- "Planning baseline: $planning" >> "$BRIEF_REL";; mismatch) print -r -- 'Planning baseline: 0000000000000000000000000000000000000000' > "$BRIEF_REL";; esac
    expect_fail check_baseline "$planning"; cp "$root/brief" "$BRIEF_REL"; ((++count))
  done
  print -r -- tamper >> new/path-1.kt; expect_fail verify_inventory; git checkout -- new/path-1.kt; ((++count))
  expect_rc 1 run_gated closeout-pre bogus "$planning" "$implementation" false
  [[ -z "${TASK_6_1_RUN_GATED_TMP-}" ]]
  residue_paths=("${TMPDIR:-/tmp}"/task-6.1-wrapper.*(N)); (( ${#residue_paths} == 0 )); ((++count))
  print -r -- wrapper >> new/path-1.kt
  # Injected cleanup failure proves cleanup beats a post-gate failure when the action succeeds.
  cleanup_fixture() { return 9; }
  local saved_gate="${functions[gate]}"
  gate() { return 0; }
  print -r -- '#!/bin/sh' > "$root/cleanup-failure"; print -r -- '/bin/rm -rf -- "$@"; exit 9' >> "$root/cleanup-failure"; chmod +x "$root/cleanup-failure"
  TASK_6_1_RUN_GATED_CLEANUP_COMMAND="$root/cleanup-failure"; expect_rc 1 run_gated pre post "$planning" '' :; ((++count))
  # The same wrapper proves action failure wins over both cleanup and post-gate failures.
  TASK_6_1_RUN_GATED_CLEANUP_COMMAND="$root/cleanup-failure"; expect_rc 1 run_gated pre post "$planning" '' false; functions[gate]="$saved_gate"; unset TASK_6_1_RUN_GATED_CLEANUP_COMMAND; ((++count))
  [[ -z "${TASK_6_1_IMPLEMENTATION_SHA-}" && -z "${TASK_6_1_RUN_GATED_CLEANUP_COMMAND-}" ]] || die 'wrapper state leaked after cleanup controls'
  TASK_6_1_IMPLEMENTATION_SHA=stale-sha; TASK_6_1_RUN_GATED_CLEANUP_COMMAND=stale-cleanup
  expect_rc 1 run_gated closeout-pre bogus "$planning" "$implementation"
  [[ "$TASK_6_1_IMPLEMENTATION_SHA" == stale-sha && "$TASK_6_1_RUN_GATED_CLEANUP_COMMAND" == stale-cleanup ]] || die 'wrapper state was not restored'
  unset TASK_6_1_IMPLEMENTATION_SHA TASK_6_1_RUN_GATED_CLEANUP_COMMAND
  ((++count))
  print -r -- closeout >> "$PLAN_REL"; print -r -- closeout >> openspec/changes/feature-first-modularization/tasks.md; print -r -- closeout >> progress.md; print -r -- closeout >> roadmap.md
  print -r -- 'Closeout evidence: complete' >> "$BRIEF_REL"; print -r -- 'Closeout evidence: complete' >> "$LEDGER_REL"
  print -r -- closeout > '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md'
  TASK_6_1_REPORT_SHA="$(sha256 "$REPORT_REL")" perl -0pi -e 's/^Frozen report SHA-256: [0-9a-f]{64}$/"Frozen report SHA-256: $ENV{TASK_6_1_REPORT_SHA}"/me' "$LEDGER_REL"
  print -r -- final > '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-final-acceptance-report.md'
  git add -f "$BRIEF_REL" "$LEDGER_REL" "$PLAN_REL" openspec/changes/feature-first-modularization/tasks.md progress.md roadmap.md .superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-{report,final-acceptance-report}.md; git commit -qm closeout
  closeout_sha="$(git rev-parse HEAD)"
  print -r -- extra > extra-path; git add extra-path; git commit -qm extra-closeout; extra_closeout_sha="$(git rev-parse HEAD)"; expect_fail finalize "$planning" "$implementation" :; git switch -q --detach "$closeout_sha"; rm -f -- extra-path; ((++count))
  git rm -q -- '.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-final-acceptance-report.md'; git commit -qm missing-final-report; missing_report_sha="$(git rev-parse HEAD)"; expect_fail finalize "$planning" "$implementation" :; git switch -q --detach "$closeout_sha"; ((++count))
  closeout_tree="$(git rev-parse HEAD^{tree})"; wrong_closeout_sha="$(git commit-tree "$closeout_tree" -p "$correction_sha" -m wrong-closeout)"; git switch -q --detach "$wrong_closeout_sha"; expect_fail finalize "$planning" "$implementation" :; git switch -q --detach "$closeout_sha"; ((++count))
  TASK_6_1_CORRECTION_SHA='0000000000000000000000000000000000000000'; expect_fail finalize "$planning" "$implementation" :; export TASK_6_1_CORRECTION_SHA="$correction_sha"; ((++count))
  finalize "$planning" "$implementation" :; ((++count))
  git add -A -- .; expect_fail accept_cumulative "$planning" "$implementation" "$implementation"; git reset -q; ((++count))
  real_index="$(git rev-parse --git-path index)"; before="$(sha256 "$real_index")"; after="$(sha256 "$real_index")"; [[ "$before" == "$after" ]]
  cd /; rm -rf -- "$root"; root=''; trap - EXIT
  host_after="$(sha256 "$host_index")"; [[ "$host_before" == "$host_after" ]]
  residue_paths=("${TMPDIR:-/tmp}"/task-6.1-{matrix,wrapper}.*(N)); (( ${#residue_paths} == 0 ))
  print -r -- "matrix=PASS controls=$count manifest=49/26/34/109 correction=12 real_plan_parser=PASS status_R_UU_unknown=PASS ledger_mismatch=PASS absent_D_baseline=PASS action_cleanup_post_precedence=PASS residue=0 real_index_byte_identical=yes"
)

case "${1:-matrix}" in
  matrix) (( $# <= 1 )) || die 'usage: extracted-proof matrix'; matrix;;
  producer) (( $# >= 3 )) || die 'usage: extracted-proof producer PLANNING_SHA ACTION [ARG...]'; produce "$2" "${@:3}";;
  stage) (( $# >= 3 )) || die 'usage: extracted-proof stage PLANNING_SHA ACTION [ARG...]'; stage_manifest "$2" "${@:3}";;
  commit) (( $# >= 3 )) || die 'usage: extracted-proof commit PLANNING_SHA ACTION [ARG...]'; commit_manifest "$2" "${@:3}";;
  planning-amendment) (( $# == 1 )) || die 'usage: extracted-proof planning-amendment'; planning_amendment_dispatch;;
  correction-inventory) (( $# == 2 )) || die 'usage: extracted-proof correction-inventory PLANNING_SHA'; read_correction_inventory "$2";;
  rebound-inventory) (( $# == 2 )) || die 'usage: extracted-proof rebound-inventory PLANNING_SHA'; read_rebound_inventory "$2";;
  correction) (( $# >= 4 )) || die 'usage: extracted-proof correction PLANNING_SHA CORRECTION_SHA ACTION [ARG...]'; correct "$2" "$3" "${@:4}";;
  successor-correction-pre-fixture) (( $# == 2 )) || die 'usage: extracted-proof successor-correction-pre-fixture PLANNING_SHA'; successor_correction_pre_fixture "$2";;
  closeout) (( $# >= 4 )) || die 'usage: extracted-proof closeout PLANNING_SHA IMPLEMENTATION_SHA ACTION [ARG...]'; closeout "$2" "$3" "${@:4}";;
  final) (( $# >= 5 )) || die 'usage: extracted-proof final PLANNING_SHA CORRECTION_SHA IMPLEMENTATION_SHA ACTION [ARG...]'; final_cumulative "$2" "$3" "$4" "${@:5}";;
  *) die 'usage: extracted-proof {matrix|planning-amendment|producer|stage|commit|correction|closeout|final} ...';;
esac
```

Run the literal authority without creating a parser file: extract the zsh fence into a temporary
file, invoke `zsh -n`, then invoke its default `matrix` mode. The matrix is mandatory evidence.
Its producer, staging, commit, closeout, and final controls cover malformed literal structure/counts,
all required porcelain forms and rejected statuses, baseline/brief/ledger binding, A/M/D baseline
presence, frozen hashes/DELETED records, full-union staging, commit lineage, closeout allowlist,
wrapper failure precedence, cleanup failure, temporary-residue removal, and byte-identical real index.
<!-- TASK-6.1-MANIFEST-PARSER:END -->

### RED_RECOVERY Authority

`RED_RECOVERY` is mandatory evidence after amendment approval, the two-plan commit, and brief/controller
rebind, before any live Search test adaptation. It must run against this repository's real source and
real Gradle wrapper, never a generated substitute. The runner derives the approved pre-correction baseline by
validating that `4943d76c22222c4beaf9b2eb229e33664116daa6` is an ancestor of the amended plan HEAD and
that its tree contains the rejected Checkpoint 2 source state. It creates a detached worktree at that
exact commit and fails closed if baseline identity, ancestry, source hashes, or patch preconditions do
not match, requiring Checkpoint 2A discard/restart.

At entry it captures the absolute live top-level, branch, HEAD, amended-plan hash, SHA-256 of exact
`git status --porcelain=v1 -z` bytes, linked-worktree index bytes/hash from `git rev-parse --git-path
index`, frozen report SHA `2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5`, protected
source/test hashes or `DELETED` records, both plan hashes, and complete controller-ledger bytes. It
reads the actual ledger, validates the ordered six historical lines against the historical planning
SHA `2e199950da3fa518c2491b3168cbb5fb86c4cefd`, and compares their real-byte digest to an
independently captured literal expected digest. It never derives an expected historical digest from
the ledger under test or requires the historical planning line to equal amended `HEAD`. Current lineage
is separately proved by `HEAD == brief planning baseline` and the canonical pointer hash. The existing
controller's evidence-prefix SHA is the historical-prefix SHA
`d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0`; it is not a second byte range.
Recovery records the distinct literal correction inventory block SHA
authority-supplied amended marker-block SHA and rejects overlapping or equal historical/correction
ranges.

```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "RED_RECOVERY: $*"; exit 1; }
sha_file() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly BASELINE=4943d76c22222c4beaf9b2eb229e33664116daa6
readonly BASELINE_TREE=b82a0e689a6fcb2fb76d7f6813cb33c24fe72de8
readonly HISTORICAL_PREFIX_SHA=d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0
readonly CORRECTION_MAP_SHA=d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0
readonly FROZEN_REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
# Exact marker bytes: BEGIN, twelve path<TAB>M<TAB>64hex rows, END, each newline-terminated.
readonly CORRECTION_MARKER_SHA=8614b4de3e124c47cefb635f814f1882db3f4ffa13001325b89f83a93cd09984
root_live="$(git rev-parse --show-toplevel)"; branch_live="$(git branch --show-current)"
[[ "$root_live" == "$PWD" && "$branch_live" == feature/feature-first-modularization ]] || die 'wrong live root or branch'
ledger=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
brief=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
report=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
plan=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
pointer=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
plan_sha="$(git rev-parse HEAD)"; brief_sha="$(awk '/^Planning baseline: [0-9a-f]{40}$/{print $3; n++} END{exit n == 1 ? 0 : 1}' "$brief")"
ledger_library_blob="$(awk '/^Amended library plan blob SHA-256: /{print $6; n++} END{exit n == 1 ? 0 : 1}' "$ledger")"; ledger_pointer_blob="$(awk '/^Amended pointer plan blob SHA-256: /{print $6; n++} END{exit n == 1 ? 0 : 1}' "$ledger")"
[[ "$plan_sha" == "$brief_sha" && "$ledger_library_blob" =~ '^[0-9a-f]{64}$' && "$ledger_pointer_blob" =~ '^[0-9a-f]{64}$' && "$(sha_file "$plan")" == "$ledger_library_blob" && "$(sha_file "$pointer")" == "$ledger_pointer_blob" && "$ledger_library_blob" == "$(git show "$plan_sha:$plan" | sha_bytes)" && "$ledger_pointer_blob" == "$(git show "$plan_sha:$pointer" | sha_bytes)" ]] || die 'amended plan/pointer/brief/ledger authority mismatch'
git merge-base --is-ancestor "$BASELINE" "$plan_sha" && [[ "$(git rev-parse "$BASELINE^{tree}")" == "$BASELINE_TREE" ]] || die 'baseline authority mismatch'
evidence="$(mktemp -d "${TMPDIR:-/tmp}/rhythhaus-red-recovery.XXXXXX")"; summary="$(mktemp "${TMPDIR:-/tmp}/rhythhaus-red-recovery-summary.XXXXXX")"; logs="$evidence/logs"; record="$evidence/recovery-record"; mkdir -p "$logs"; : > "$record"
parent_index="$(git rev-parse --git-path index)"; status_file="$evidence/status.z"; index_file="$evidence/index"; ledger_file="$evidence/ledger"; protected_file="$evidence/protected"; historical_file="$evidence/historical"; correction_file="$evidence/correction"; brief_file="$evidence/brief"; report_file="$evidence/report"
git status --porcelain=v1 -z > "$status_file"; cp "$parent_index" "$index_file"; cp "$ledger" "$ledger_file"; cp "$brief" "$brief_file"; cp "$report" "$report_file"; awk 'NR <= 6 {print}' "$ledger" > "$historical_file"
# Old eleven-row controller state is an explicit pre-rebind blocker; only rebind can install this format.
awk '/^Correction inventory: BEGIN$/{on=1} on{print} /^Correction inventory: END$/{exit}' "$ledger" > "$correction_file"
[[ "$(sha_file "$historical_file")" == "$HISTORICAL_PREFIX_SHA" && "$(sha_file "$correction_file")" == "$CORRECTION_MARKER_SHA" && "$(awk 'NR>1 && NR<14{n++} END{print n+0}' "$correction_file")" == 12 && "$(awk '/^Correction map SHA-256: /{print $4}' "$ledger")" == "$CORRECTION_MAP_SHA" && "$(sha_file "$report_file")" == "$FROZEN_REPORT_SHA" ]] || die 'old eleven-row pre-rebind ledger is blocked or correction authority mismatches'
for endpoint_path in shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt; do [[ -e "$endpoint_path" ]] && print -r -- "$endpoint_path=$(sha_file "$endpoint_path")" || print -r -- "$endpoint_path=DELETED"; done > "$protected_file"
protected_snapshot() { local endpoint_path; for endpoint_path in shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt; do [[ -e "$endpoint_path" ]] && print -r -- "$endpoint_path"$'\tPRESENT\t'"$(sha_file "$endpoint_path")" || print -r -- "$endpoint_path"$'\tDELETED\t-'; done; }
protected_snapshot > "$protected_file"
typeset -a worktrees; worktrees=(); worktree_add=NOT_RUN; worktree_remove=NOT_RUN; worktree_prune=NOT_RUN; cleanup_status=NOT_RUN; cleanup_attempts=0
cleanup_remove_worktree() { git worktree remove --force "$1"; }
cleanup_prune_worktrees() { git worktree prune; }
cleanup_remove_root() { rm -rf "$1"; }
dense_survivors_after_remove() {
  local remove_fn="$1" target="${2:-}" wt rc=0
  shift 2
  local -a survivors=()
  for wt in "$@"; do
    [[ -n "$wt" ]] || return 1
    if [[ -n "$target" && "$wt" != "$target" ]]; then
      survivors+=("$wt")
    elif "$remove_fn" "$wt"; then
      :
    else
      rc=$?; survivors+=("$wt")
    fi
  done
  worktrees=("${survivors[@]}")
  return "$rc"
}
cleanup_evidence() {
  local target_root="$1" target_record="$2" remove_fn="$3" prune_fn="$4" root_fn="$5" remove_rc=0 prune_rc=0 root_rc=0 record_hash
  (( ++cleanup_attempts )); record_hash="$(sha_file "$target_record")" || return 1; cd "$root_live" || remove_rc=1
  dense_survivors_after_remove "$remove_fn" '' "${worktrees[@]}" || remove_rc=$?
  "$prune_fn" || prune_rc=$?
  if (( remove_rc == 0 && prune_rc == 0 )); then "$root_fn" "$target_root" || root_rc=$?; [[ ! -e "$target_root" ]] || root_rc=1; else root_rc=1; fi
  worktree_remove=$([[ "$remove_rc" == 0 ]] && print PASS || print FAIL); worktree_prune=$([[ "$prune_rc" == 0 ]] && print PASS || print FAIL); cleanup_status=$([[ "$root_rc" == 0 ]] && print PASS || print FAIL)
  printf '%s\n' "record_sha=$record_hash" "cleanup_attempts=$cleanup_attempts" "worktree_remove=$worktree_remove" "worktree_prune=$worktree_prune" "cleanup_status=$cleanup_status" "root_removed=$([[ ! -e "$target_root" ]] && print PASS || print FAIL)" > "$summary"
  (( remove_rc == 0 && prune_rc == 0 && root_rc == 0 ))
}
cleanup() { local action=$? cleanup_rc=0; trap - EXIT; cleanup_evidence "$evidence" "$record" cleanup_remove_worktree cleanup_prune_worktrees cleanup_remove_root || cleanup_rc=$?; (( action != 0 )) && exit "$action"; (( cleanup_rc != 0 )) && exit "$cleanup_rc"; exit 0; }
trap cleanup EXIT
add_worktree() { local name="$1"; worktree_path="$evidence/worktrees/$name"; mkdir -p "$evidence/worktrees"; git worktree add --detach "$worktree_path" "$BASELINE" >/dev/null || return $?; worktrees+=("$worktree_path"); worktree_add=PASS; }
remove_registered_worktree() { local endpoint_path="$1" remove_fn="$2"; dense_survivors_after_remove "$remove_fn" "$endpoint_path" "${worktrees[@]}"; }
patch_path_set() { awk '/^diff --git a\//{sub(/^a\//,"",$3); print $3}' "$1" | LC_ALL=C sort; }
reconstruct_tree() { local patch_file="$1" expected_paths="$2" idx="$evidence/reconstruct.index" actual_paths; rm -f -- "$idx"; actual_paths="$(patch_path_set "$patch_file")"; [[ "$actual_paths" == "$expected_paths" ]] || return 1; GIT_INDEX_FILE="$idx" git read-tree "$BASELINE" && GIT_INDEX_FILE="$idx" git apply --cached --check "$patch_file" && GIT_INDEX_FILE="$idx" git apply --cached "$patch_file" && GIT_INDEX_FILE="$idx" git write-tree; }
# This never reads the live dirty worktree. It treats fixed-baseline source/blob artifacts and
# retained detached-worktree post sources as the independent authority for the playlist patch.
rebuild_playlist_from_sources() {
  local base="$1" expected_paths="$2" first_path="$3" second_path="$4" first_baseline_source="$5" second_baseline_source="$6" first_baseline_blob="$7" second_baseline_blob="$8" first_post_source="$9" second_post_source="${10}" first_post_blob="${11}" second_post_blob="${12}" idx="${13}" rebuilt_patch="${14}" first_post_id second_post_id rebuilt_tree
  [[ "$(git rev-parse "$base:$first_path")" == "$(<"$first_baseline_blob")" && "$(git rev-parse "$base:$second_path")" == "$(<"$second_baseline_blob")" ]] || return 1
  cmp -s <(git show "$base:$first_path") "$first_baseline_source" && cmp -s <(git show "$base:$second_path") "$second_baseline_source" || return 1
  first_post_id="$(git hash-object -w "$first_post_source")" && second_post_id="$(git hash-object -w "$second_post_source")" || return 1
  [[ "$first_post_id" == "$(<"$first_post_blob")" && "$second_post_id" == "$(<"$second_post_blob")" ]] || return 1
  rm -f -- "$idx" "$rebuilt_patch"
  GIT_INDEX_FILE="$idx" git read-tree "$base" && GIT_INDEX_FILE="$idx" git update-index --add --cacheinfo "100644,$first_post_id,$first_path" && GIT_INDEX_FILE="$idx" git update-index --add --cacheinfo "100644,$second_post_id,$second_path" || return 1
  rebuilt_tree="$(GIT_INDEX_FILE="$idx" git write-tree)" || return 1
  GIT_INDEX_FILE="$idx" git diff --cached --binary "$base" -- "$first_path" "$second_path" > "$rebuilt_patch" || return 1
  [[ "$(patch_path_set "$rebuilt_patch")" == "$expected_paths" ]] || return 1
  print -r -- "$rebuilt_tree"
}
write_literal_diagnostic_contracts() { cat > "$evidence/expected-playlist.diagnostic" <<'EOF'
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt|UNRESOLVED_REFERENCE|toPlayableTrack
EOF
cat > "$evidence/expected-shared.diagnostic" <<'EOF'
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt|UNRESOLVED_REFERENCE|toPlayableTrack
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt|UNRESOLVED_REFERENCE|toPlayableTrack
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt|UNRESOLVED_REFERENCE|toPlayableTrack
EOF
cat > "$evidence/expected-direct.diagnostic" <<'EOF'
core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt|TEST_FAILURE|directSetterNormalizesApplicationContext|UninitializedPropertyAccessException|storedApplicationContext
EOF
}
normalize_compiler_diagnostics() {
  # Emit canonical records ONLY for unresolved `toPlayableTrack` references in the known consumer
  # files. The compiler emits these in two formats: single-line (`Unresolved reference 'toPlayableTrack'`)
  # and multi-line (the `e:` line ends with `Unresolved reference. None of the following candidates …`
  # and the symbol `fun Track.toPlayableTrack(): PlayableTrack` appears on the next continuation line).
  # A `pending` endpoint is tracked so a continuation `toPlayableTrack` line is attributed to the
  # preceding unresolved-reference `e:` line. Every other diagnostic — `w:` warnings, `Cannot infer
  # type` noise, and downstream named-parameter/arity errors — is ignored as mixed-state noise, because
  # the GREEN compile (exit 0 after applying the patch) is the fail-closed guarantee. An unresolved
  # `toPlayableTrack` in an unknown file still fails closed via the case fallthrough.
  local input="$1" output="$2" expected="$3" line endpoint symbol pending=''
  : > "$output"
  while IFS= read -r line; do
    if [[ "$line" == e:*'.kt:'* ]]; then
      pending=''
      if [[ "$line" == *'toPlayableTrack'* && ( "$line" == *'Unresolved reference'* || "$line" == *'unresolved reference'* ) ]]; then
        case "$line" in
          *PlaylistScreens.kt:*) endpoint='feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt'; symbol=toPlayableTrack;;
          *App.kt:*) endpoint='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt'; symbol=toPlayableTrack;;
          *PlaybackSessionCoordinator.kt:*) endpoint='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt'; symbol=toPlayableTrack;;
          *LibraryRoutes.kt:*) endpoint='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt'; symbol=toPlayableTrack;;
          *) return 1;;
        esac
        print -r -- "$endpoint|UNRESOLVED_REFERENCE|$symbol" >> "$output"
      elif [[ "$line" == *'Unresolved reference'* || "$line" == *'unresolved reference'* ]]; then
        case "$line" in
          *PlaylistScreens.kt:*) pending='feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt';;
          *App.kt:*) pending='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt';;
          *PlaybackSessionCoordinator.kt:*) pending='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt';;
          *LibraryRoutes.kt:*) pending='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt';;
          *) pending='';;
        esac
      fi
    elif [[ -n "$pending" && "$line" == *'toPlayableTrack'* ]]; then
      print -r -- "$pending|UNRESOLVED_REFERENCE|toPlayableTrack" >> "$output"
      pending=''
    fi
  done < "$input"
  LC_ALL=C sort "$output" -o "$output"
  [[ "$(wc -l < "$output" | tr -d ' ')" == "$(wc -l < "$expected" | tr -d ' ')" ]] && cmp -s "$output" "$expected"
}
normalize_test_failure() {
  # Gradle's task framing may vary; the selected test and the baseline holder failure are stable facts.
  # The `> Task :... FAILED` task summary and `BUILD FAILED in Ns` build summary lines are framing, not
  # unrelated test failures, so they are allowed; `w:` warnings are benign and ignored; only an `e:`
  # compile diagnostic or another test's ` FAILED` line fails closed. Gradle's console summary for a
  # single failing test prints the exception type and file:line
  # (`kotlin.UninitializedPropertyAccessException at LibraryDatabaseAndroidHostTest.kt:21`) but not the
  # property name, so the holder is detected on the exception type alone; the failing test name, the
  # exactly-one-failed-line cardinality, and the GREEN exit-0 gate are the binding guarantees.
  local input="$1" output="$2" expected="$3" seen_test=0 seen_holder=0 failure_line_count=0 line
  : > "$output"
  while IFS= read -r line; do
    if [[ "$line" == *'directSetterNormalizesApplicationContext'*' FAILED'* ]]; then seen_test=1; (( ++failure_line_count )); fi
    [[ "$line" == *'UninitializedPropertyAccessException'* ]] && seen_holder=1
    [[ "$line" == e:*'.kt:'* ]] && return 1
    [[ "$line" == *' FAILED'* && "$line" != *'directSetterNormalizesApplicationContext'* && "$line" != *'> Task :'* && "$line" != *'BUILD FAILED'* ]] && return 1
  done < "$input"
  (( seen_test && seen_holder && failure_line_count == 1 )) || return 1
  print -r -- 'core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt|TEST_FAILURE|directSetterNormalizesApplicationContext|UninitializedPropertyAccessException|storedApplicationContext' > "$output"
  [[ "$(wc -l < "$output" | tr -d ' ')" == 1 ]] && cmp -s "$output" "$expected"
}
write_literal_diagnostic_contracts
add_worktree playlist || die 'playlist worktree add failed'; playlist_wt="$worktree_path"; playlist_red_tree="$(git -C "$playlist_wt" rev-parse HEAD^{tree})"; print -r -- "$playlist_red_tree" > "$evidence/playlist-red-tree"; [[ "$playlist_red_tree" == "$BASELINE_TREE" ]] || die 'playlist RED tree mismatch'; playlist_log="$logs/playlist.log"
setopt noerrexit; (cd "$playlist_wt" && ./gradlew :feature:playlists:impl:compileKotlinJvm --rerun-tasks --no-parallel) 2>&1 | tee "$playlist_log"; playlist_rc=${pipestatus[1]}; setopt errexit
(( playlist_rc == 1 )) || die 'playlist RED exit contract mismatch'; normalize_compiler_diagnostics "$playlist_log" "$logs/playlist.diagnostic" "$evidence/expected-playlist.diagnostic" || die 'playlist exact diagnostic mismatch'
playlist_first_path='feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt'; playlist_second_path='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt'; playlist_paths="$playlist_first_path"$'\n'"$playlist_second_path"; playlist_first_baseline_source="$evidence/playlist-first-baseline.source"; playlist_second_baseline_source="$evidence/playlist-second-baseline.source"; playlist_first_baseline_blob="$evidence/playlist-first-baseline.blob"; playlist_second_baseline_blob="$evidence/playlist-second-baseline.blob"; playlist_first_post_source="$evidence/playlist-first-post.source"; playlist_second_post_source="$evidence/playlist-second-post.source"; playlist_first_post_blob="$evidence/playlist-first-post.blob"; playlist_second_post_blob="$evidence/playlist-second-post.blob"; git show "$BASELINE:$playlist_first_path" > "$playlist_first_baseline_source" && git show "$BASELINE:$playlist_second_path" > "$playlist_second_baseline_source" && git rev-parse "$BASELINE:$playlist_first_path" > "$playlist_first_baseline_blob" && git rev-parse "$BASELINE:$playlist_second_path" > "$playlist_second_baseline_blob" || die 'playlist baseline artifact capture failed'
patch="$evidence/playlist.patch"; git -C "$root_live" diff --binary -- "$playlist_first_path" "$playlist_second_path" > "$patch"; [[ -s "$patch" && "$(patch_path_set "$patch")" == "$playlist_paths" ]] || die 'playlist patch path set mismatch'; git -C "$playlist_wt" apply --check "$patch" && git -C "$playlist_wt" apply "$patch" || die 'patch apply failed'; cp "$playlist_wt/$playlist_first_path" "$playlist_first_post_source" && cp "$playlist_wt/$playlist_second_path" "$playlist_second_post_source" && git hash-object -w "$playlist_first_post_source" > "$playlist_first_post_blob" && git hash-object -w "$playlist_second_post_source" > "$playlist_second_post_blob" || die 'playlist post artifact capture failed'; git -C "$playlist_wt" add -A; git -C "$playlist_wt" write-tree > "$evidence/shared-tree"; playlist_green_rc=0; (cd "$playlist_wt" && ./gradlew :feature:playlists:impl:compileKotlinJvm --rerun-tasks --no-parallel) || playlist_green_rc=$?; (( playlist_green_rc == 0 )) || die 'playlist GREEN failed'
shared_log="$logs/shared.log"; setopt noerrexit; (cd "$playlist_wt" && ./gradlew :shared:compileKotlinJvm --rerun-tasks --no-parallel) 2>&1 | tee "$shared_log"; shared_rc=${pipestatus[1]}; setopt errexit
(( shared_rc == 1 )) || die 'Shared RED exit contract mismatch'; normalize_compiler_diagnostics "$shared_log" "$logs/shared.diagnostic" "$evidence/expected-shared.diagnostic" || die 'Shared exact diagnostic mismatch'
add_worktree direct || die 'direct worktree add failed'; direct_wt="$worktree_path"; direct_patch="$evidence/direct.patch"; direct_path='core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt'; git show "$BASELINE:$direct_path" > "$evidence/direct-baseline.source"; git -C "$root_live" diff --binary -- "$direct_path" > "$direct_patch"; [[ -s "$direct_patch" && "$(patch_path_set "$direct_patch")" == "$direct_path" ]] || die 'direct patch path set mismatch'; git -C "$direct_wt" apply --check "$direct_patch" && git -C "$direct_wt" apply "$direct_patch" || die 'direct patch apply failed'; git -C "$direct_wt" status --porcelain=v1 > "$evidence/direct.status"; cp "$direct_wt/$direct_path" "$evidence/direct-post.source"; git -C "$direct_wt" diff --binary > "$evidence/direct.diff"; grep -Fx " M $direct_path" "$evidence/direct.status" >/dev/null || die 'direct status mismatch'; git -C "$direct_wt" add -A; direct_tree="$(git -C "$direct_wt" write-tree)"; print -r -- "$direct_tree" > "$evidence/direct-tree"
direct_log="$logs/direct.log"; setopt noerrexit; (cd "$direct_wt" && ./gradlew :core:database:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest.directSetterNormalizesApplicationContext' --rerun-tasks --no-parallel) 2>&1 | tee "$direct_log"; direct_rc=${pipestatus[1]}; setopt errexit
(( direct_rc == 1 )) || die 'direct RED exit contract mismatch'; normalize_test_failure "$direct_log" "$logs/direct.diagnostic" "$evidence/expected-direct.diagnostic" || die 'direct exact runtime failure mismatch'
# Direct evidence is retained before removal. Successful removals unregister immediately, so EXIT cleanup
# retries only still-live worktrees.
remove_registered_worktree "$playlist_wt" cleanup_remove_worktree || die 'playlist worktree removal failed'; remove_registered_worktree "$direct_wt" cleanup_remove_worktree || die 'direct worktree removal failed'; worktree_remove=PASS; git worktree prune || die 'worktree prune failed'; worktree_prune=PASS
record="$evidence/recovery-record"
printf '%s\n' "baseline=$BASELINE" "baseline_tree=$BASELINE_TREE" "root=$root_live" "branch=$branch_live" "plan=$plan_sha" "plan_sha=$(sha_file "$plan")" "pointer_sha=$(sha_file "$pointer")" "playlist_rc=$playlist_rc" "shared_rc=$shared_rc" "direct_rc=$direct_rc" "status=$status_file:$(sha_file "$status_file")" "index=$index_file:$(sha_file "$index_file")" "brief=$brief_file:$(sha_file "$brief_file")" "report=$report_file:$(sha_file "$report_file")" "ledger=$ledger_file:$(sha_file "$ledger_file")" "protected=$protected_file:$(sha_file "$protected_file")" "historical=$historical_file:$(sha_file "$historical_file")" "correction=$correction_file:$(sha_file "$correction_file")" "playlist_log=$playlist_log:$(sha_file "$playlist_log")" "shared_log=$shared_log:$(sha_file "$shared_log")" "direct_log=$direct_log:$(sha_file "$direct_log")" "playlist_diagnostic=$logs/playlist.diagnostic:$(sha_file "$logs/playlist.diagnostic")" "shared_diagnostic=$logs/shared.diagnostic:$(sha_file "$logs/shared.diagnostic")" "direct_diagnostic=$logs/direct.diagnostic:$(sha_file "$logs/direct.diagnostic")" "playlist_tree=$evidence/playlist-red-tree:$(sha_file "$evidence/playlist-red-tree")" "shared_tree=$evidence/shared-tree:$(sha_file "$evidence/shared-tree")" "patch=$patch:$(sha_file "$patch")" "playlist_first_baseline_source=$playlist_first_baseline_source:$(sha_file "$playlist_first_baseline_source")" "playlist_second_baseline_source=$playlist_second_baseline_source:$(sha_file "$playlist_second_baseline_source")" "playlist_first_baseline_blob=$playlist_first_baseline_blob:$(sha_file "$playlist_first_baseline_blob")" "playlist_second_baseline_blob=$playlist_second_baseline_blob:$(sha_file "$playlist_second_baseline_blob")" "playlist_first_post_source=$playlist_first_post_source:$(sha_file "$playlist_first_post_source")" "playlist_second_post_source=$playlist_second_post_source:$(sha_file "$playlist_second_post_source")" "playlist_first_post_blob=$playlist_first_post_blob:$(sha_file "$playlist_first_post_blob")" "playlist_second_post_blob=$playlist_second_post_blob:$(sha_file "$playlist_second_post_blob")" "direct_patch=$direct_patch:$(sha_file "$direct_patch")" "direct_tree=$evidence/direct-tree:$(sha_file "$evidence/direct-tree")" "direct_status=$evidence/direct.status:$(sha_file "$evidence/direct.status")" "direct_baseline_source=$evidence/direct-baseline.source:$(sha_file "$evidence/direct-baseline.source")" "direct_post_source=$evidence/direct-post.source:$(sha_file "$evidence/direct-post.source")" "direct_diff=$evidence/direct.diff:$(sha_file "$evidence/direct.diff")" "worktree_add=$worktree_add" "worktree_remove=$worktree_remove" "worktree_prune=$worktree_prune" "cleanup_status=$cleanup_status" > "$record"
validate_ledger_blob_authority() { [[ "$ledger_library_blob" == "$(git show "$plan_sha:$plan" | sha_bytes)" && "$ledger_pointer_blob" == "$(git show "$plan_sha:$pointer" | sha_bytes)" ]]; }
validate_reconstructed_artifacts() { local expected_playlist_paths=$'feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt\nshared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt' expected_direct_path='core/database/src/androidHostTest/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseAndroidHostTest.kt' rebuilt_playlist="$evidence/playlist.patch.rebuilt" rebuilt_playlist_tree expected_shared_tree direct_rebuilt; [[ "$(git rev-parse "$BASELINE^{tree}")" == "$BASELINE_TREE" && "$(<"${f[playlist_tree]%%:*}")" == "$BASELINE_TREE" ]] || return 47; [[ "$(patch_path_set "${f[patch]%%:*}")" == "$expected_playlist_paths" && "$(patch_path_set "${f[direct_patch]%%:*}")" == "$expected_direct_path" ]] || return 47; rebuilt_playlist_tree="$(rebuild_playlist_from_sources "$BASELINE" "$expected_playlist_paths" 'feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt' 'shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt' "${f[playlist_first_baseline_source]%%:*}" "${f[playlist_second_baseline_source]%%:*}" "${f[playlist_first_baseline_blob]%%:*}" "${f[playlist_second_baseline_blob]%%:*}" "${f[playlist_first_post_source]%%:*}" "${f[playlist_second_post_source]%%:*}" "${f[playlist_first_post_blob]%%:*}" "${f[playlist_second_post_blob]%%:*}" "$evidence/playlist-reconstruct.index" "$rebuilt_playlist")" || return 47; [[ "$rebuilt_playlist_tree" == "$(<"${f[shared_tree]%%:*}")" ]] || return 47; cmp -s "$rebuilt_playlist" "${f[patch]%%:*}" || return 47; expected_shared_tree="$(reconstruct_tree "${f[patch]%%:*}" "$expected_playlist_paths")" || return 47; [[ "$expected_shared_tree" == "$rebuilt_playlist_tree" ]] || return 47; direct_rebuilt="$(reconstruct_tree "${f[direct_patch]%%:*}" "$expected_direct_path")" || return 47; [[ "$direct_rebuilt" == "$(<"${f[direct_tree]%%:*}")" ]] || return 47; git show "$BASELINE:$expected_direct_path" > "$evidence/direct-baseline.rebuilt" && git show "$direct_rebuilt:$expected_direct_path" > "$evidence/direct-post.rebuilt" && cmp -s "$evidence/direct-baseline.rebuilt" "${f[direct_baseline_source]%%:*}" && cmp -s "$evidence/direct-post.rebuilt" "${f[direct_post_source]%%:*}" && cmp -s "${f[direct_patch]%%:*}" "${f[direct_diff]%%:*}"; }
record_file() { local pair="$1" file hash; file="${pair%%:*}"; hash="${pair##*:}"; [[ -f "$file" && "$hash" =~ '^[0-9a-f]{64}$' && "$(sha_file "$file")" == "$hash" ]] || return 1; [[ "$file" != "$protected_file" ]] || { [[ "$(protected_snapshot)" == "$(<"$protected_file")" ]] || return 1; }; [[ "$file" != "$ledger_file" ]] || { [[ "$(sha_file "$ledger_file")" == "$(sha_file "$ledger")" ]] || return 1; }; [[ "$file" != "$brief_file" ]] || { [[ "$(sha_file "$brief_file")" == "$(sha_file "$brief")" ]] || return 1; }; [[ "$file" != "$report_file" ]] || { [[ "$(sha_file "$report_file")" == "$(sha_file "$report")" && "$(sha_file "$report")" == "$FROZEN_REPORT_SHA" ]] || return 1; }; [[ "$file" != "$logs/playlist.diagnostic" ]] || { cmp -s "$file" "$evidence/expected-playlist.diagnostic" || return 1; }; [[ "$file" != "$logs/shared.diagnostic" ]] || { cmp -s "$file" "$evidence/expected-shared.diagnostic" || return 1; }; [[ "$file" != "$logs/direct.diagnostic" ]] || { cmp -s "$file" "$evidence/expected-direct.diagnostic" || return 1; }; }
expect_validator_rc() { local expected="$1" actual; shift; setopt noerrexit; "$@" >/dev/null 2>&1; actual=$?; setopt errexit; (( actual == expected )); }
validate_live_preservation() { local protected_pair="$1" protected_baseline; protected_baseline="${protected_pair%%:*}"; [[ -f "$protected_baseline" && "$(protected_snapshot)" == "$(<"$protected_baseline")" && "$(sha_file "$ledger_file")" == "$(sha_file "$ledger")" && "$(sha_file "$brief_file")" == "$(sha_file "$brief")" && "$(sha_file "$report_file")" == "$(sha_file "$report")" && "$(sha_file "$report")" == "$FROZEN_REPORT_SHA" ]]; }
validate_recovery_record() {
  local input="$1" key value item
  local -A f=()
  local -a required=(baseline baseline_tree root branch plan plan_sha pointer_sha playlist_rc shared_rc direct_rc status index brief report ledger protected historical correction playlist_log shared_log direct_log playlist_diagnostic shared_diagnostic direct_diagnostic playlist_tree shared_tree patch playlist_first_baseline_source playlist_second_baseline_source playlist_first_baseline_blob playlist_second_baseline_blob playlist_first_post_source playlist_second_post_source playlist_first_post_blob playlist_second_post_blob direct_patch direct_tree direct_status direct_baseline_source direct_post_source direct_diff worktree_add worktree_remove worktree_prune cleanup_status)
  [[ -f "$input" ]] || return 41
  while IFS="=" read -r key value; do [[ "$key" =~ "^[a-z_]+$" && -z "${f[$key]-}" ]] || return 41; f[$key]="$value"; done < "$input"
  (( ${#f} == ${#required} )) || return 41
  for key in "${required[@]}"; do [[ -n "${f[$key]-}" ]] || return 41; done
  [[ "${f[baseline]}" == "$BASELINE" && "${f[baseline_tree]}" == "$BASELINE_TREE" && "${f[root]}" == "$root_live" && "${f[branch]}" == "$branch_live" && "${f[plan]}" == "$plan_sha" && "${f[plan_sha]}" == "$(git show "$plan_sha:$plan" | sha_bytes)" && "${f[pointer_sha]}" == "$(git show "$plan_sha:$pointer" | sha_bytes)" ]] || return 42
  [[ "${f[playlist_rc]}" == 1 && "${f[shared_rc]}" == 1 && "${f[direct_rc]}" == 1 ]] || return 43
  for item in historical correction; do record_file "${f[$item]}" || return 44; done
  [[ "${f[historical]##*:}" == "$HISTORICAL_PREFIX_SHA" && "${f[correction]##*:}" == "$CORRECTION_MARKER_SHA" ]] || return 44
  for item in playlist_log shared_log direct_log playlist_diagnostic shared_diagnostic direct_diagnostic; do record_file "${f[$item]}" || return 45; done
  for item in status index brief report ledger protected; do record_file "${f[$item]}" || return 46; done
  [[ "${f[worktree_add]}" == PASS && "${f[worktree_remove]}" == PASS && "${f[worktree_prune]}" == PASS && "${f[cleanup_status]}" == NOT_RUN ]] || return 47
  for item in playlist_tree shared_tree patch playlist_first_baseline_source playlist_second_baseline_source playlist_first_baseline_blob playlist_second_baseline_blob playlist_first_post_source playlist_second_post_source playlist_first_post_blob playlist_second_post_blob direct_patch direct_tree direct_status direct_baseline_source direct_post_source direct_diff; do record_file "${f[$item]}" || return 47; done
  validate_reconstructed_artifacts || return 47
}
validate_cleanup_summary() { local expected_record_sha="$1" key value; typeset -A f; [[ -f "$summary" && ! -e "$evidence" ]] || return 48; while IFS='=' read -r key value; do [[ "$key" =~ '^(record_sha|cleanup_attempts|worktree_remove|worktree_prune|cleanup_status|root_removed)$' && -z "${f[$key]-}" ]] || return 48; f[$key]="$value"; done < "$summary"; (( ${#f} == 6 )) && [[ "${f[record_sha]-}" == "$expected_record_sha" && "${f[cleanup_attempts]-}" == 1 && "${f[worktree_remove]-}" == PASS && "${f[worktree_prune]-}" == PASS && "${f[cleanup_status]-}" == PASS && "${f[root_removed]-}" == PASS ]]; }
typeset -i recovery_controls=0
validate_r14_reconstruction() { local key value; local -A f=(); [[ -f "$record" ]] || return 41; while IFS="=" read -r key value; do [[ "$key" =~ "^[a-z_]+$" && -z "${f[$key]-}" ]] || return 41; f[$key]="$value"; done < "$record"; validate_reconstructed_artifacts; }
validate_r17_diagnostics() { cmp -s "$logs/playlist.diagnostic" "$evidence/expected-playlist.diagnostic" && cmp -s "$logs/shared.diagnostic" "$evidence/expected-shared.diagnostic" && cmp -s "$logs/direct.diagnostic" "$evidence/expected-direct.diagnostic" && [[ "$(wc -l < "$logs/playlist.diagnostic" | tr -d ' ')" == 1 && "$(wc -l < "$logs/shared.diagnostic" | tr -d ' ')" == 3 && "$(wc -l < "$logs/direct.diagnostic" | tr -d ' ')" == 1 ]]; }
control() { (( ++recovery_controls )); "$@" || die "R$(printf '%02d' "$recovery_controls") assertion failed"; }
# R01 baseline tree; R02 root/branch; R03 plan-to-brief lineage; R04/R05 committed plan blobs.
control test "$(git rev-parse "$BASELINE^{tree}")" = "$BASELINE_TREE"
control test "${root_live}:${branch_live}" = "${PWD}:feature/feature-first-modularization"
control test "$plan_sha:$brief_sha" = "$plan_sha:$plan_sha"
control test "$(sha_file "$plan")" = "$ledger_library_blob"
control test "$(sha_file "$pointer")" = "$ledger_pointer_blob"
# R06/R07 captured status/index; R08-R10 captured brief/report/ledger; R11 protected live state.
control test "$(sha_file "$status_file")" = "$(git status --porcelain=v1 -z | sha_bytes)"
control test "$(sha_file "$index_file")" = "$(sha_file "$parent_index")"
control test "$(sha_file "$brief_file")" = "$(sha_file "$brief")"
control test "$(sha_file "$report_file")" = "$FROZEN_REPORT_SHA"
control test "$(sha_file "$ledger_file")" = "$(sha_file "$ledger")"
control validate_live_preservation "$protected_file:$(sha_file "$protected_file")"
# R12 historical prefix; R13 correction marker; R14 reconstruction; R15 playlist GREEN.
control test "$(sha_file "$historical_file"):$(( $(awk 'NR>1 && NR<14{n++} END{print n+0}' "$correction_file") ))" = "$HISTORICAL_PREFIX_SHA:12"
control test "$(sha_file "$correction_file")" = "$CORRECTION_MARKER_SHA"
control validate_r14_reconstruction
control test "$playlist_rc:$playlist_green_rc" = '1:0'
# R16 exact three RED exits; R17 literal diagnostic bytes/counts; R18 pre-cleanup lifecycle.
control test "$playlist_rc:$shared_rc:$direct_rc" = '1:1:1'
control validate_r17_diagnostics
control test "$worktree_add:$worktree_remove:$worktree_prune:$cleanup_status" = PASS:PASS:PASS:NOT_RUN
(( recovery_controls == 19 - 1 )) || die 'pre-cleanup control count mismatch'
validate_recovery_record "$record" || die 'positive recovery record rejected'
# Same validator and record schema drive all six mutations.
typeset -i sabotage_controls=0
sabotage="$evidence/sabotage-record"; cp "$record" "$sabotage"; perl -0pi -e 's/^baseline=.*/baseline=wrong/m' "$sabotage"; expect_validator_rc 42 validate_recovery_record "$sabotage" || die 'S01 code drift'; (( ++sabotage_controls ))
cp "$record" "$sabotage"; perl -0pi -e 's#^playlist_log=.*#playlist_log=missing:0000000000000000000000000000000000000000000000000000000000000000#m' "$sabotage"; expect_validator_rc 45 validate_recovery_record "$sabotage" || die 'S02 code drift'; (( ++sabotage_controls ))
cp "$record" "$sabotage"; perl -0pi -e 's#^historical=.*#historical=missing:0000000000000000000000000000000000000000000000000000000000000000#m' "$sabotage"; expect_validator_rc 44 validate_recovery_record "$sabotage" || die 'S03 code drift'; (( ++sabotage_controls ))
mv "$playlist_log" "$logs/missing"; expect_validator_rc 45 validate_recovery_record "$record" || die 'S04 code drift'; mv "$logs/missing" "$playlist_log"; (( ++sabotage_controls ))
cp "$report" "$evidence/report.restore"; print -r -- mutation >> "$report"; expect_validator_rc 46 validate_recovery_record "$record" || die 'S05 code drift'; cp "$evidence/report.restore" "$report"; (( ++sabotage_controls ))
cp "$record" "$sabotage"; perl -0pi -e 's/^direct_rc=.*/direct_rc=0/m' "$sabotage"; expect_validator_rc 43 validate_recovery_record "$sabotage" || die 'S06 direct-exit mutation code drift'; cp "$record" "$sabotage"; perl -0pi -e 's/^cleanup_status=.*/cleanup_status=PASS/m' "$sabotage"; expect_validator_rc 47 validate_recovery_record "$sabotage" || die 'S06 cleanup-status mutation code drift'; (( ++sabotage_controls ))
(( sabotage_controls == 6 )) || die 'S01-S06 count mismatch'
validate_recovery_record "$record" || die 'post-sabotage recovery record rejected'
record_sha_before_cleanup="$(sha_file "$record")"
trap - EXIT
cleanup_evidence "$evidence" "$record" cleanup_remove_worktree cleanup_prune_worktrees cleanup_remove_root || die 'final cleanup failed'
control validate_cleanup_summary "$record_sha_before_cleanup"
(( recovery_controls == 19 )) || die 'R01-R19 final count mismatch'
print -r -- "RED_RECOVERY PASS recovery_controls=$recovery_controls sabotage_controls=$sabotage_controls summary=$summary"
```


The direct baseline test patch introduces `directSetterNormalizesApplicationContext`; it compiles
against the retained compatibility setter, then fails at runtime because that setter initializes only
the legacy database-holder while `LibraryDatabaseContext.applicationContext` still reads its separate
uninitialized canonical holder. Its exact canonical evidence is one selected test failure with process
exit `1` and the `UninitializedPropertyAccessException` type; Gradle's console summary for a single
failing test prints the exception type and `file:line` but not the property name, so log detection is
on the exception type alone while the canonical contract record retains the `storedApplicationContext`
property; it is not a Kotlin unresolved-reference diagnostic. Compiler normalization emits canonical
records only for unresolved `toPlayableTrack` references in the known consumer files; every other
diagnostic (`w:` warnings, `Cannot infer type` inference noise, and downstream named-parameter/arity
errors from the corrective signature change) is ignored as mixed-state noise, and the GREEN compile
(exit 0 after applying the patch) is the fail-closed guarantee that no unrelated error survives the fix.

The positive runner must retain one evidence root through record construction, validator, all six
sabotages, cleanup assertions, and final record validation. It may use separate
detached worktrees below that one root, but must not remove or recreate the evidence root while any
recorded artifact remains needed. For the exact two playlist endpoints it retains fixed-baseline
source/blob pairs and detached-worktree post-source/blob pairs; R14 rebuilds a fresh baseline index
from those post sources, derives its canonical binary patch and tree, requires byte equality with the
recorded patch and recorded shared tree, and never regenerates authority from the live dirty worktree.
The validator recomputes every recorded log, diagnostic extract, source-tree, patch, direct-source,
direct-diff, status, index, ledger, protected-source, report, and plan/pointer hash from retained
artifacts. Plan/pointer expectations come only from independent authority inputs, never values
computed by the same run. It records actual exit codes, branch/root
identity, worktree add/remove/prune results, and cleanup status. Cleanup removes every detached
worktree and the evidence root exactly once after validation, with zero worktree registration or root residue.

The recovery sabotage matrix invokes the same validator over a completed positive record, never a
substitute for that run. S01 mutates baseline/tree or source authority and expects `42`; S02 mutates a
log/hash and expects `45`; S03 mutates historical prefix or correction block and expects `44`; S04
removes a recorded log and expects `45`; S05 mutates live status/index/report/protected source and
expects `46`; S06 separately mutates `direct_rc` (expects `43`) and `cleanup_status` (expects `47`). The literal positive inventory is
R01-R19 (baseline/tree, plan/pointer authority, brief, status, index, report, ledger, protected,
historical, correction, playlist rc/log/diagnostic/source, patch, Shared rc/log/diagnostic/source,
direct status/rc/log/diagnostic/source/diff, worktree lifecycle, cleanup) and asserts
`recovery_controls == 19`; S01-S06 assert `sabotage_controls == 6`. The pre-cleanup record records
`cleanup_status=NOT_RUN`; only the retained external summary records post-cleanup status, root removal,
one cleanup attempt, and the validated record hash. Missing exact diagnostics or
unreproducible real states fail closed and require Checkpoint 2A discard/restart.

The mandatory order is: amendment approval; commit only the two plans; rebind brief/controller; real
`RED_RECOVERY`; live SearchRoute test adaptation; current named selectors/XML; platform/startup checks;
append truthful `RED_RECOVERY` and GREEN evidence after the unchanged historical prefix; scope/hash/
status/diff review; then independent review. No source change or staging occurs before amendment
approval.

The correction phase is mandatory and precedes producer work. Rebind the ignored brief to the current
corrected plan HEAD before correction-pre or any producer action. The frozen dirty report SHA is
recorded in the ledger Evidence section before implementation and is read by every correction gate.
Correction actions use `correction-prepare`, `correction-pre`, `correction-stage-pre`,
`correction-stage-post`, `correction-commit-pre`, and `correction-commit-post`; each gate rejects a stale plan/brief, wrong
parent, nonempty index, frozen-report drift, extra/missing/wrong-status endpoint, staged ledger drift,
or post-commit scope/hash/status mismatch. The controller's precedence is action failure first,
cleanup failure second, post-gate failure third; cleanup is attempted exactly once and timeout is a
blocker. The correction child must be directly parented by the supplied planning SHA.

Final acceptance is cumulative: `accept_cumulative` requires the supplied planning SHA, correction
parent equal to that SHA, and final implementation SHA descendant of the correction SHA. It validates
the combined diff from the planning SHA through the final implementation SHA, allowing endpoints owned by intervening
correction commits, and rejects missing, duplicated, extra, or status-mismatched endpoints. A later
109-path implementation commit therefore need not be a direct child of the plan. The cumulative
endpoint union must be exactly the separate final manifest arithmetic **A=49/M=26/D=34, total=109,
unique=109**; the 12-path correction map is never substituted for this final manifest.

The causal compile RED chronology is literal. First run playlist implementation compilation and
capture its unresolved deleted `LibraryTrack` conversion diagnostic. After the minimum playlist
signature/projection adaptation and replacement Shared conversion seam are test-first prepared, run
Shared compilation before adapting stale Shared callers and require its exact unresolved conversion
diagnostics. Record each command, exit code, diagnostic, XML/log path, and ordering; do not claim one
initial command reaches both modules.

The required RED commands are:

```zsh
./gradlew :feature:playlists:impl:compileKotlinJvm --rerun-tasks --no-parallel 2>&1 | tee "$TMPDIR/library-playlist-compile-red.log"; rc=${pipestatus[1]}; print "playlist-red-exit=$rc log=$TMPDIR/library-playlist-compile-red.log"; exit "$rc"
./gradlew :shared:compileKotlinJvm --rerun-tasks --no-parallel 2>&1 | tee "$TMPDIR/library-shared-compile-red.log"; rc=${pipestatus[1]}; print "shared-red-exit=$rc log=$TMPDIR/library-shared-compile-red.log"; exit "$rc"
```

The first diagnostic must identify unresolved deleted `LibraryTrack` conversion in playlist
implementation. The second must identify unresolved conversion calls in stale Shared callers after
the replacement seam exists and before those callers are adapted. GREEN runs each task separately:

```zsh
./gradlew :feature:playlists:impl:compileKotlinJvm :shared:compileKotlinJvm --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:playlists:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissalTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaylistLifecycleIntegrationJvmTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :core:database:testAndroidHostTest --tests 'com.eterocell.rhythhaus.library.LibraryDatabaseAndroidHostTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Inspect each task's own XML: playlist dismissal is exactly 4/0/0/0; lifecycle and holder selectors
must report their named existing/test-first methods with zero skipped/failures/errors; architecture
duplicate-storage and startup-order controls are each exactly 1/0/0/0. A missing selector, timeout,
wrong diagnostic, wrong XML count, or command that silently skips compilation is a blocker, never
optional. The final cross-platform gate remains Shared/playlist JVM compilation, focused tests,
architecture/TestKit selectors, Shared Android/iOS common compilation, and `:androidApp:assembleDebug`
when startup wiring is part of the gate.

Architecture/TestKit controls are test-first and use only the already-manifested architecture
functional-test endpoint. Control A mutates the source to declare a second
`LibraryDatabaseContext` storage field, expects the exact duplicate-storage diagnostic, restores it,
then runs the focused architecture selector and requires 1/0/0/0 XML. Control B mutates the
`RhythHausApplication.onCreate` ordering so Shared Android context precedes database context, expects
the exact startup-order diagnostic, restores it, then runs the focused startup selector and requires
1/0/0/0 XML. These controls add no androidApp/core dependency and do not modify production
`RhythHausApplication`. The mandatory correction map includes
`feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt`;
run its exact class and require its existing four methods at 4/0/0/0 XML. Every other focused test
must name an existing or explicitly test-first-created selector, with per-task XML counts and no
cross-task masking. Checkpoint 5 relinquishes only the consumer adaptations moved to 2A; it retains
Home callback redesign/D+A and route-adapter test creation/acceptance.

For each checkpoint, run a mutation-specific RED selector, assert its named diagnostic, make the
bounded production/test change, run its GREEN selector, restore the mutation, verify manifest hash/
scope, and obtain reviewer approval. Feature tests include scanner cancellation/source picker, Koin
identity, resource ownership, home/detail/artwork/platform tests; route test uses the five literal
methods above. Do not use "adapt only" as a scope rule.

```zsh
./gradlew :feature:library:impl:jvmTest :feature:library:impl:testAndroidHostTest :feature:library:impl:iosSimulatorArm64Test :feature:library:impl:compileKotlinJvm :feature:library:impl:compileAndroidMain :feature:library:impl:compileKotlinIosArm64 :feature:library:impl:compileKotlinIosSimulatorArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:library:api:allTests :shared:jvmTest :shared:iosSimulatorArm64Test :core:database:allTests :core:database:testAndroidHostTest --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:library:impl:compileAndroidMain :feature:library:api:compileAndroidMain :shared:compileAndroidMain :desktopApp:compileKotlin :androidApp:assembleDebug :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
catalog=shared/src/commonMain/composeResources/files/aboutlibraries.json
catalog_before="$(shasum -a 256 "$catalog" | cut -d ' ' -f 1)"
./gradlew :shared:exportLibraryDefinitions --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
test "$(shasum -a 256 "$catalog" | cut -d ' ' -f 1)" = "$catalog_before"
git diff --exit-code -- "$catalog"
desktop_log="$TMPDIR/library-desktop-runtime.log"
desktop_before="$(pgrep -f 'com\.eterocell\.rhythhaus\.MainKt' || true)"
./gradlew :desktopApp:run --configuration-cache --configuration-cache-problems=fail --no-parallel > "$desktop_log" 2>&1 &
desktop_gradle_pid=$!
desktop_app_pid=''
cleanup_desktop_runtime() {
  [[ -z "$desktop_app_pid" ]] || kill -TERM -- "$desktop_app_pid" 2>/dev/null || true
  kill -TERM -- "$desktop_gradle_pid" 2>/dev/null || true
}
trap cleanup_desktop_runtime EXIT
for attempt in {1..60}; do
  desktop_app_pid="$(comm -13 <(print -r -- "$desktop_before" | LC_ALL=C sort) <(pgrep -f 'com\.eterocell\.rhythhaus\.MainKt' | LC_ALL=C sort) | /usr/bin/awk 'NR == 1 { print; exit }')"
  [[ -n "$desktop_app_pid" ]] && break
  sleep 1
done
test -n "$desktop_app_pid"
kill -TERM -- "$desktop_app_pid"
kill -TERM -- "$desktop_gradle_pid" 2>/dev/null || true
for attempt in {1..30}; do
  kill -0 -- "$desktop_app_pid" 2>/dev/null || break
  sleep 1
done
! kill -0 -- "$desktop_app_pid" 2>/dev/null
for attempt in {1..30}; do
  kill -0 -- "$desktop_gradle_pid" 2>/dev/null || break
  sleep 1
done
! kill -0 -- "$desktop_gradle_pid" 2>/dev/null
wait "$desktop_gradle_pid" || true
trap - EXIT
rg -F ':desktopApp:run' "$desktop_log"
header=shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework/Headers/Shared.h
test -s "$header"
! rg -n 'SharedAudioMetadata|SharedAudioMetadataReader|SharedAudioMetadataKt|readAudioMetadata|readAudioMetadataPath' "$header"
rg -n 'SharedMainViewControllerKt' "$header"
rg -n 'MainViewController' "$header"
rg -n 'AudioSource' "$header"
api_track=feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryTrack.kt
! rg -n 'PlayableTrack|toPlayableTrack' "$api_track"
! rg -n 'toPlayableTrack' feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt
rg -n 'fun[[:space:]]+LibraryTrack\.toPlayableTrack' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryPlaybackSelection.kt
rg -n 'fun[[:space:]]+Track\.toPlayableTrack' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt
rg -n 'LibraryRepository|LibraryTrack|LibraryScanner|MainViewController' feature/library/api shared feature/library/impl iosApp
./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest' --tests 'com.eterocell.gradle.architecture.KmpConventionPluginsFunctionalTest' -Drhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-configuration-cache --no-parallel
./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel | tee "$TMPDIR/library-architecture-second.log"
rg -F 'Reusing configuration cache' "$TMPDIR/library-architecture-second.log"
./gradlew spotlessApply --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew spotlessCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew detekt --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew qualityCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
PATH="/Users/eterocell/.nvm/versions/node/v26.7.0/bin:$PATH" node --version | rg -Fx 'v26.7.0'
PATH="/Users/eterocell/.nvm/versions/node/v26.7.0/bin:$PATH" openspec validate feature-first-modularization --strict
/usr/bin/xcrun xcodebuild -version
/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' test
./init.sh
rg -n '<string name="(library_queue|album_artwork)"' shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-zh/strings.xml
! rg -n '<string name="selected"' shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-zh/strings.xml
! rg -n '<string name="(library_queue|album_artwork)"' feature/library/impl/src/commonMain/composeResources/values/strings.xml feature/library/impl/src/commonMain/composeResources/values-zh/strings.xml
./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryResourceOwnershipJvmTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
git diff --check
```

Stage exact manifest endpoints only; the implementation commit follows the mandatory correction child
of the exact bound planning SHA. Evidence closeout changes exactly these paths:

```text
.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-final-acceptance-report.md
docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
openspec/changes/feature-first-modularization/tasks.md
progress.md
roadmap.md
.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
```

Do not check 7.1-7.3 before final acceptance. Claim no runtime/device/visual/playback/picker/scanner
validation unless actually run.

### RED_RECOVERY Nounset Repair Successor Lifecycle

The consumed amendment is commit `608626fe8827c3c920e36dd71c97339ad42f3de6`, a direct child of
`2e199950da3fa518c2491b3168cbb5fb86c4cefd`. Its rebind already installed the exact twelve-row
correction marker (`8614b4de3e124c47cefb635f814f1882db3f4ffa13001325b89f83a93cd09984`), correction
map (`d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0`), and frozen report
(`2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5`). The successor below is
the only authority for the failed pre-worktree recovery repair. It commits exactly the two planning
paths as a direct child of `608626fe8827c3c920e36dd71c97339ad42f3de6`, then rebinds only the ignored
brief/controller ledger. It preserves the six historical ledger lines byte-for-byte, the exact
twelve-row correction block, all report/source/test dirt, and the empty index. The correction commit
must directly parent the successor planning SHA, not `608626f` or `2e199950`.

The failed recovery attempt is retained as historical failure evidence, never correction or GREEN
evidence. Its exact external artifacts are required at rebind and every later strict parser/gate:
the log `/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log`
has SHA-256 `be69ba885c0f14dc609f030e9425ca65be5b6c74483becdca972bb29c4326454` and sole content
`cleanup_retry_fixture:1: fixture_root: parameter not set`; the summary
`/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime.q9vybg/rhythhaus-red-recovery-summary.g4hQK1`
has SHA-256 `3028afef6648544c7e07ee7269b3ef99e6bcb993a0a8002ae995aba32c8a4e90` and records an empty
recovery-record hash, `attempts=1`, and PASS remove/prune/cleanup/root removal. Missing or changed
artifacts block rebind and later gates; no evidence is reconstructed.

<!-- TASK-6.1-RED-RECOVERY-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly SUCCESSOR_PARENT=608626fe8827c3c920e36dd71c97339ad42f3de6
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly PREFIX_SHA=d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0
readonly MARKER_SHA=8614b4de3e124c47cefb635f814f1882db3f4ffa13001325b89f83a93cd09984
readonly MAP_SHA=d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly FAILED_LOG=/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log
readonly FAILED_LOG_SHA=be69ba885c0f14dc609f030e9425ca65be5b6c74483becdca972bb29c4326454
readonly FAILED_SUMMARY=/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime.q9vybg/rhythhaus-red-recovery-summary.g4hQK1
readonly FAILED_SUMMARY_SHA=3028afef6648544c7e07ee7269b3ef99e6bcb993a0a8002ae995aba32c8a4e90

exact_plan_delta() {
  local -a changed=("${(@f)$(git diff --name-only -- docs/superpowers/plans)}")
  (( ${#changed} == 2 )) && [[ "${changed[(Ie)$PLAN_REL]}" != 0 && "${changed[(Ie)$POINTER_REL]}" != 0 && -z "$(git ls-files --others --exclude-standard -- docs/superpowers/plans)" ]]
}
failed_artifacts_valid_paths() {
  local log="$1" summary="$2" log_sha="$3" summary_sha="$4"
  [[ -f "$log" && -f "$summary" && "$(sha256 "$log")" == "$log_sha" && "$(sha256 "$summary")" == "$summary_sha" ]] || return 1
  [[ "$(cat -- "$log")" == 'cleanup_retry_fixture:1: fixture_root: parameter not set' ]] || return 1
  grep -Eq '^record_sha=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855$|^recovery_record_sha=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855$' "$summary" && grep -Eq '^cleanup_attempts=1$|^attempts=1$' "$summary" && grep -Eq '^worktree_remove=PASS$|^remove=PASS$' "$summary" && grep -Eq '^worktree_prune=PASS$|^prune=PASS$' "$summary" && grep -Eq '^cleanup_status=PASS$|^cleanup=PASS$' "$summary" && grep -Eq '^root_removed=PASS$|^root_removed=PASS$' "$summary"
}
failed_artifacts_valid() { failed_artifacts_valid_paths "$FAILED_LOG" "$FAILED_SUMMARY" "$FAILED_LOG_SHA" "$FAILED_SUMMARY_SHA"; }
capture_nonplan_dirt() {
  typeset -g SNAPSHOT_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-successor.XXXXXX")" || return 1
  git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" > "$SNAPSHOT_ROOT/tracked.patch"
  git diff --cached --binary > "$SNAPSHOT_ROOT/index.patch"
  cp -- "$BRIEF_REL" "$SNAPSHOT_ROOT/brief"; cp -- "$LEDGER_REL" "$SNAPSHOT_ROOT/ledger"; cp -- "$REPORT_REL" "$SNAPSHOT_ROOT/report"
  sha256 "$SNAPSHOT_ROOT/tracked.patch" > "$SNAPSHOT_ROOT/tracked.sha"; sha256 "$SNAPSHOT_ROOT/index.patch" > "$SNAPSHOT_ROOT/index.sha"; sha256 "$SNAPSHOT_ROOT/report" > "$SNAPSHOT_ROOT/report.sha"
}
assert_nonplan_dirt() {
  local after
  [[ -d "${SNAPSHOT_ROOT-}" && -z "$(git diff --cached --name-only)" && "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]] || return 1
  after="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-successor-after.XXXXXX")" || return 1
  git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" > "$after/tracked.patch"
  [[ "$(sha256 "$after/tracked.patch")" == "$(<"$SNAPSHOT_ROOT/tracked.sha")" && "$(sha256 "$REPORT_REL")" == "$(<"$SNAPSHOT_ROOT/report.sha")" ]] || { rm -rf -- "$after"; return 1; }
  rm -rf -- "$after"
}
correction_authority_valid() {
  local ledger="$1" marker
  [[ "$(awk 'NR <= 6 {print}' "$ledger" | sha_bytes)" == "$PREFIX_SHA" ]] || return 1
  marker="$(awk '/^Correction inventory: BEGIN$/{on=1} on{print} /^Correction inventory: END$/{exit}' "$ledger")"
  [[ "$(print -r -- "$marker" | sha_bytes)" == "$MARKER_SHA" && "$(print -r -- "$marker" | awk 'NR > 1 && NR < 14 {n++} END {print n+0}')" == 12 ]] || return 1
  [[ "$(awk '/^Correction map SHA-256: /{print $4; n++} END{exit n == 1 ? 0 : 1}' "$ledger")" == "$MAP_SHA" && "$(awk '/^Frozen report SHA-256: /{print $4; n++} END{exit n == 1 ? 0 : 1}' "$ledger")" == "$REPORT_SHA" ]]
}
failed_attempt_valid() {
  local ledger="$1" begin=0 end=0 stage=0 error=0 no_gradle=0 no_worktree=0 log=0 summary=0
  while IFS= read -r line || [[ -n "$line" ]]; do
    case "$line" in
      'Failed RED_RECOVERY attempt: BEGIN') ((++begin));;
      'Stage: PRE_WORKTREE') ((++stage));;
      'Observed error: cleanup_retry_fixture:1: fixture_root: parameter not set') ((++error));;
      'No Gradle build client: PASS') ((++no_gradle));;
      'No detached worktree: PASS') ((++no_worktree));;
      "Log: $FAILED_LOG SHA-256: $FAILED_LOG_SHA") ((++log));;
      "Summary: $FAILED_SUMMARY SHA-256: $FAILED_SUMMARY_SHA") ((++summary));;
      'Failed RED_RECOVERY attempt: END') ((++end));;
    esac
  done < "$ledger"
  (( begin == 1 && end == 1 && stage == 1 && error == 1 && no_gradle == 1 && no_worktree == 1 && log == 1 && summary == 1 )) && failed_artifacts_valid
}
successor_ledger_valid() {
  local successor="$1"
  correction_authority_valid "$LEDGER_REL" && failed_attempt_valid "$LEDGER_REL" &&
    [[ "$(awk '/^Amended library plan blob SHA-256: /{print $6; n++} END{exit n == 1 ? 0 : 1}' "$LEDGER_REL")" == "$(git show "$successor:$PLAN_REL" | sha_bytes)" ]] &&
    [[ "$(awk '/^Amended pointer plan blob SHA-256: /{print $6; n++} END{exit n == 1 ? 0 : 1}' "$LEDGER_REL")" == "$(git show "$successor:$POINTER_REL" | sha_bytes)" ]]
}
successor_rebound_authority_valid() {
  local successor="$1"
  [[ "$(awk '/^Planning baseline: /{print $3; n++} END{exit n == 1 ? 0 : 1}' "$BRIEF_REL")" == "$successor" ]] &&
    grep -Fq "successor plan commit \`$successor\`" "$BRIEF_REL" &&
    grep -Fq 'twelve-path correction allowlist' "$BRIEF_REL" &&
    grep -Fq '113-path manifest' "$BRIEF_REL" &&
    successor_ledger_valid "$successor"
}
rebind_successor() {
  local successor="$1" prefix marker tmp
  prefix="$(mktemp "${TMPDIR:-/tmp}/task-6.1-successor-prefix.XXXXXX")"; marker="$(mktemp "${TMPDIR:-/tmp}/task-6.1-successor-marker.XXXXXX")"; tmp="$(mktemp "${TMPDIR:-/tmp}/task-6.1-successor-ledger.XXXXXX")"
  awk 'NR <= 6 {print}' "$LEDGER_REL" > "$prefix"; awk '/^Correction inventory: BEGIN$/{on=1} on{print} /^Correction inventory: END$/{exit}' "$LEDGER_REL" > "$marker"
  correction_authority_valid "$LEDGER_REL" || die 'current correction authority drift'
  perl -0pi -e 's/approved plan commit `2e199950da3fa518c2491b3168cbb5fb86c4cefd`/successor plan commit `SUCCESSOR_HEAD`/; s/successor plan commit `[0-9a-f]{40}`/successor plan commit `SUCCESSOR_HEAD`/; s/eleven-path correction allowlist/twelve-path correction allowlist/g; s/112-path manifest/113-path manifest/g' "$BRIEF_REL"
  perl -0pi -e "s/SUCCESSOR_HEAD/$successor/g" "$BRIEF_REL"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  { cat -- "$prefix"; cat -- "$marker"; print -r -- "Correction map SHA-256: $MAP_SHA"; print -r -- "Correction evidence prefix SHA-256: $PREFIX_SHA"; print -r -- "Amended library plan blob SHA-256: $(git show "$successor:$PLAN_REL" | sha_bytes)"; print -r -- "Amended pointer plan blob SHA-256: $(git show "$successor:$POINTER_REL" | sha_bytes)"; print -r -- "Frozen report SHA-256: $REPORT_SHA"; print -r -- 'Failed RED_RECOVERY attempt: BEGIN'; print -r -- 'Stage: PRE_WORKTREE'; print -r -- 'Observed error: cleanup_retry_fixture:1: fixture_root: parameter not set'; print -r -- 'No Gradle build client: PASS'; print -r -- 'No detached worktree: PASS'; print -r -- "Log: $FAILED_LOG SHA-256: $FAILED_LOG_SHA"; print -r -- "Summary: $FAILED_SUMMARY SHA-256: $FAILED_SUMMARY_SHA"; print -r -- 'Failed RED_RECOVERY attempt: END'; } > "$tmp"
  cmp -s "$prefix" <(awk 'NR <= 6 {print}' "$tmp") || die 'historical prefix changed'
  mv -- "$tmp" "$LEDGER_REL"; rm -f -- "$prefix" "$marker"
}
successor_gate() {
  [[ "$(git rev-parse HEAD)" == "$SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] && exact_plan_delta && [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]] && correction_authority_valid "$LEDGER_REL" && failed_artifacts_valid
}
successor_dispatch() {
  local parent successor
  successor_gate || die 'successor pre-commit gate failed'
  capture_nonplan_dirt || die 'cannot capture protected dirt'
  parent="$(git rev-parse HEAD)"; git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || die 'successor index is not exact'
  git commit -m 'docs: repair red recovery nounset authority' >/dev/null
  successor="$(git rev-parse HEAD)"; [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$successor" | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || die 'successor commit path mismatch'
  rebind_successor "$successor"
  successor_rebound_authority_valid "$successor" && assert_nonplan_dirt || die 'successor rebind authority mismatch'
  rm -rf -- "$SNAPSHOT_ROOT"; unset SNAPSHOT_ROOT
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
successor_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo script primary_script host_root="$PWD" sha count=0 source ledger_copy log_copy summary_copy correction_output
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-successor-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  git diff --binary --no-renames "$SUCCESSOR_PARENT" 89a65070434c6c1f03880412e9a741653b85d1a3 -- "$PLAN_REL" "$POINTER_REL" > "$root/plans.patch"; git -C "$repo" apply --whitespace=nowarn "$root/plans.patch"; cp -- "$repo/$PLAN_REL" "$root/plan"; cp -- "$repo/$POINTER_REL" "$root/pointer"
  for source in "$BRIEF_REL" "$LEDGER_REL" "$REPORT_REL"; do mkdir -p -- "$repo/${source:h}"; cp -- "$source" "$repo/$source"; done
  cd "$repo"; exact_plan_delta && successor_gate; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next} m && /^```zsh$/{c=1;next} m && c && /^```$/{exit} c{print}' "$host_root/$PLAN_REL" > "$primary_script"; expect_fail zsh "$primary_script" planning-amendment; ((++count))
  git restore --source=HEAD -- "$POINTER_REL"; expect_fail successor_gate; cp -- "$root/pointer" "$POINTER_REL"; ((++count))
  print -r -- unexpected > docs/superpowers/plans/unexpected-successor.md; expect_fail successor_gate; rm -f -- docs/superpowers/plans/unexpected-successor.md; ((++count))
  cp -- "$LEDGER_REL" "$root/predecessor-ledger"; perl -0pi -e 's/(\tM\t)[0-9a-f]{64}/$1 . "0" x 64/e' "$LEDGER_REL"; expect_fail successor_gate; cp -- "$root/predecessor-ledger" "$LEDGER_REL"; ((++count))
  perl -0pi -e 's/(\t)M(\t[0-9a-f]{64})/$1A$2/' "$LEDGER_REL"; expect_fail successor_gate; cp -- "$root/predecessor-ledger" "$LEDGER_REL"; ((++count))
  cp -- "$REPORT_REL" "$root/report"; print -r -- mutation >> "$REPORT_REL"; expect_fail successor_gate; cp -- "$root/report" "$REPORT_REL"; ((++count))
  source='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt'; cp -- "$source" "$root/source"; capture_nonplan_dirt; print -r -- mutation >> "$source"; expect_fail assert_nonplan_dirt; mv -- "$root/source" "$source"; rm -rf -- "$SNAPSHOT_ROOT"; unset SNAPSHOT_ROOT; ((++count))
  git add -f -- "$REPORT_REL"; expect_fail successor_gate; git reset -q -- "$REPORT_REL"; ((++count))
  git commit --allow-empty -qm wrong-parent; expect_fail successor_gate; git reset --hard -q "$SUCCESSOR_PARENT"; git apply --whitespace=nowarn "$root/plans.patch"; cp -- "$root/report" "$REPORT_REL"; ((++count))
  cp -- "$LEDGER_REL" "$root/ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Failed RED_RECOVERY attempt: BEGIN\n.*?^Failed RED_RECOVERY attempt: END\n//ms' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Stage: PRE_WORKTREE$/Stage: RECOVERY/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Log: .*/Log: wrong SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- 'Failed RED_RECOVERY attempt: END' >> "$LEDGER_REL";; esac; expect_fail failed_attempt_valid "$LEDGER_REL"; ((++count)); done; cp -- "$root/ledger" "$LEDGER_REL"; ((++count))
  log_copy="$root/log"; summary_copy="$root/summary"; cp -- "$FAILED_LOG" "$log_copy"; cp -- "$FAILED_SUMMARY" "$summary_copy"; print -r -- mutation >> "$log_copy"; expect_fail failed_artifacts_valid_paths "$log_copy" "$summary_copy" "$FAILED_LOG_SHA" "$FAILED_SUMMARY_SHA"; cp -- "$FAILED_LOG" "$log_copy"; print -r -- mutation >> "$summary_copy"; expect_fail failed_artifacts_valid_paths "$log_copy" "$summary_copy" "$FAILED_LOG_SHA" "$FAILED_SUMMARY_SHA"; ((count+=2))
  successor_dispatch; sha="$(git rev-parse HEAD)"; successor_rebound_authority_valid "$sha"; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$sha")"; [[ "$correction_output" == 'historical_successor_correction_fixture=PASS controls=9' ]] || die 'historical successor correction fixture mismatch'; ((count+=9))
  cp -- "$LEDGER_REL" "$root/rebound"; for row in stale-library stale-pointer map-drift report-drift eleven-rows; do cp -- "$root/rebound" "$LEDGER_REL"; case "$row" in stale-library) perl -0pi -e 's/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";; stale-pointer) perl -0pi -e 's/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";; map-drift) perl -0pi -e 's/^Correction map SHA-256: .*/Correction map SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";; report-drift) perl -0pi -e 's/^Frozen report SHA-256: .*/Frozen report SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";; eleven-rows) perl -0pi -e 's/^.*SearchRouteAdapterJvmTest\.kt\tM\t.*\n//m' "$LEDGER_REL";; esac; expect_fail successor_ledger_valid "$sha"; ((++count)); done; cp -- "$root/rebound" "$LEDGER_REL"
  perl -0pi -e 's/^Checkpoint 1 Governance RED: PASS \/ APPROVED$/Checkpoint 1 Governance RED: changed/m' "$LEDGER_REL"; expect_fail successor_ledger_valid "$sha"; cp -- "$root/rebound" "$LEDGER_REL"; ((++count))
  cp -- "$BRIEF_REL" "$root/brief"; print -r -- 'Planning baseline: wrong' > "$BRIEF_REL"; expect_fail successor_rebound_authority_valid "$sha"; cp -- "$root/brief" "$BRIEF_REL"; successor_rebound_authority_valid "$sha"; ((++count))
  for prose in incorrect missing; do cp -- "$root/brief" "$BRIEF_REL"; case "$prose" in incorrect) perl -0pi -e 's/twelve-path correction allowlist/eleven-path correction allowlist/; s/113-path manifest/112-path manifest/' "$BRIEF_REL";; missing) perl -0pi -e 's/successor plan commit `[^`]+`//; s/twelve-path correction allowlist//; s/113-path manifest//' "$BRIEF_REL";; esac; expect_fail successor_rebound_authority_valid "$sha"; ((++count)); done; cp -- "$root/brief" "$BRIEF_REL"; successor_rebound_authority_valid "$sha"
  git diff --cached --quiet; git diff --check; [[ -z "${SNAPSHOT_ROOT-}" ]]; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "successor_matrix=PASS controls=$count residue=0 index=empty"
)
case "${1:-successor-matrix}" in
  successor-planning-amendment) (( $# == 1 )) || die 'usage: successor-planning-amendment'; successor_dispatch;;
  successor-matrix) (( $# == 1 )) || die 'usage: successor-matrix'; successor_matrix;;
  *) die 'usage: successor authority {successor-matrix|successor-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-SUCCESSOR:END -->

Extract this successor fence independently. Before its real execution, its disposable fixture must run
the actual `successor_dispatch` and reject wrong parent, stale plan blobs, changed prefix, changed or
eleven-row correction block, map/report drift, changed protected source dirt, nonempty index, each
missing/malformed/wrong/duplicate failed-attempt row, changed failed artifacts, wrong brief baseline,
and extra/missing planning paths. The positive fixture proves the direct two-plan commit, rebind, empty
index, retained non-plan dirt, and zero temporary residue. No later producer, staging, acceptance, or
correction gate may consume a successor planning SHA unless this failed-attempt grammar and its external
artifact hashes validate; the correction child then directly parents that successor SHA.

<!-- TASK-6.1-RED-RECOVERY-CLEANUP-ISOLATION-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 cleanup-isolation successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly CLEANUP_SUCCESSOR_PARENT=89a65070434c6c1f03880412e9a741653b85d1a3
readonly COMMITTED_CLEANUP_SUCCESSOR=fe9b565de72417a2b1bf584370d2eab29bbfc73e
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly PREFIX_SHA=d18afa3cf33bc812fb8aa9180eb338fe8f5f3202038fefb0d5e4a7a0225073f0
readonly MARKER_SHA=8614b4de3e124c47cefb635f814f1882db3f4ffa13001325b89f83a93cd09984
readonly MAP_SHA=d484ea85990b3040b2acdb56080a02d3b1eb85683c35805587ca188edef621e0
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly FIRST_LOG="${TASK_6_1_FIRST_LOG:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log}"
readonly FIRST_SUMMARY="${TASK_6_1_FIRST_SUMMARY:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime.q9vybg/rhythhaus-red-recovery-summary.g4hQK1}"
readonly SECOND_LOG="${TASK_6_1_SECOND_LOG:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output-fresh.JM8mHw}"
readonly SECOND_OUTER_SUMMARY="${TASK_6_1_SECOND_OUTER_SUMMARY:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-recovery-summary.h8gzST}"
readonly SECOND_FIXTURE_SUMMARY="${TASK_6_1_SECOND_FIXTURE_SUMMARY:-/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ}"

exact_plan_delta() {
  local -a paths=("${(@f)$(git diff --name-only -- docs/superpowers/plans)}")
  (( ${#paths} == 2 )) && [[ "${paths[(Ie)$PLAN_REL]}" != 0 && "${paths[(Ie)$POINTER_REL]}" != 0 && -z "$(git ls-files --others --exclude-standard -- docs/superpowers/plans)" ]]
}
correction_authority_valid() {
  local ledger="$1" marker
  [[ "$(awk 'NR<=6{print}' "$ledger" | sha_bytes)" == "$PREFIX_SHA" ]] || return 1
  marker="$(awk '/^Correction inventory: BEGIN$/{on=1}on{print}/^Correction inventory: END$/{exit}' "$ledger")"
  [[ "$(print -r -- "$marker" | sha_bytes)" == "$MARKER_SHA" && "$(print -r -- "$marker" | awk 'NR>1&&NR<14{n++}END{print n+0}')" == 12 ]] || return 1
  [[ "$(awk '/^Correction map SHA-256:/{print $4;n++}END{exit n==1?0:1}' "$ledger")" == "$MAP_SHA" && "$(awk '/^Frozen report SHA-256:/{print $4;n++}END{exit n==1?0:1}' "$ledger")" == "$REPORT_SHA" ]]
}
first_failure_valid() {
  local ledger="$1" expected
  expected=$'Failed RED_RECOVERY attempt: BEGIN\nStage: PRE_WORKTREE\nObserved error: cleanup_retry_fixture:1: fixture_root: parameter not set\nNo Gradle build client: PASS\nNo detached worktree: PASS\nLog: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output.XXXXXX.log SHA-256: be69ba885c0f14dc609f030e9425ca65be5b6c74483becdca972bb29c4326454\nSummary: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime.q9vybg/rhythhaus-red-recovery-summary.g4hQK1 SHA-256: 3028afef6648544c7e07ee7269b3ef99e6bcb993a0a8002ae995aba32c8a4e90\nFailed RED_RECOVERY attempt: END'
  [[ "$(awk '/^Failed RED_RECOVERY attempt: BEGIN$/{on=1}on{print}/^Failed RED_RECOVERY attempt: END$/{exit}' "$ledger")" == "$expected" && "$(grep -c '^Failed RED_RECOVERY attempt: BEGIN$' "$ledger")" == 1 && "$(sha256 "$FIRST_LOG")" == be69ba885c0f14dc609f030e9425ca65be5b6c74483becdca972bb29c4326454 && "$(sha256 "$FIRST_SUMMARY")" == 3028afef6648544c7e07ee7269b3ef99e6bcb993a0a8002ae995aba32c8a4e90 ]]
}
second_failure_valid() {
  local ledger="$1" expected
  expected=$'Failed RED_RECOVERY attempt 2: BEGIN\nStage: PRE_WORKTREE\nPrimary error: RED_RECOVERY: cleanup evidence retry fixture failed\nSecondary error: fixture_remove: attempts: parameter not set\nSecondary error: fixture_remove: second: parameter not set\nNo Gradle build client: PASS\nNo detached worktree: PASS\nCleanup outcome: FAIL\nLog: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-output-fresh.JM8mHw SHA-256: bc3fa401adaeaf96846a0266cea2e2c1bbbaa3c967e9eba5795875f3595ccd9e\nOuter summary: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-recovery-summary.h8gzST SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\nFixture summary: /var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-red-recovery-runtime-fresh.bDJGfX/rhythhaus-red-cleanup-summary.vTAoIQ SHA-256: a958917a3dc2495d28e4475e141aeaa48d9bf8b0a7104be7c4a4d3123a6ed44c\nFailed RED_RECOVERY attempt 2: END'
  [[ "$(awk '/^Failed RED_RECOVERY attempt 2: BEGIN$/{on=1}on{print}/^Failed RED_RECOVERY attempt 2: END$/{exit}' "$ledger")" == "$expected" && "$(grep -c '^Failed RED_RECOVERY attempt 2: BEGIN$' "$ledger")" == 1 && "$(sha256 "$SECOND_LOG")" == bc3fa401adaeaf96846a0266cea2e2c1bbbaa3c967e9eba5795875f3595ccd9e && "$(sha256 "$SECOND_OUTER_SUMMARY")" == e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855 && "$(sha256 "$SECOND_FIXTURE_SUMMARY")" == a958917a3dc2495d28e4475e141aeaa48d9bf8b0a7104be7c4a4d3123a6ed44c ]]
}
capture_nonplan_dirt() {
  typeset -g SNAPSHOT_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-cleanup-successor.XXXXXX")"
  git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" > "$SNAPSHOT_ROOT/tracked.patch"; cp -- "$REPORT_REL" "$SNAPSHOT_ROOT/report"
  typeset -g PRESERVED_NONPLAN_DIFF_SHA="$(<"$SNAPSHOT_ROOT/tracked.patch" | sha_bytes)" PRESERVED_REPORT_SHA="$(sha256 "$REPORT_REL")"
}
assert_nonplan_dirt() { [[ -d "${SNAPSHOT_ROOT-}" && -z "$(git diff --cached --name-only)" && "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "${PRESERVED_NONPLAN_DIFF_SHA-}" && "$(sha256 "$REPORT_REL")" == "${PRESERVED_REPORT_SHA-}" ]]; }
assert_retained_nonplan_dirt() { [[ -z "$(git diff --cached --name-only)" && "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "${PRESERVED_NONPLAN_DIFF_SHA-}" && "$(sha256 "$REPORT_REL")" == "${PRESERVED_REPORT_SHA-}" ]]; }
rebind_cleanup_successor() {
  local successor="$1" prefix marker tmp
  prefix="$(mktemp -t task61-cleanup-prefix)"; marker="$(mktemp -t task61-cleanup-marker)"; tmp="$(mktemp -t task61-cleanup-ledger)"
  correction_authority_valid "$LEDGER_REL" && first_failure_valid "$LEDGER_REL" && second_failure_valid "$LEDGER_REL" && die 'second failure block already present'
  awk 'NR<=6{print}' "$LEDGER_REL" > "$prefix"; awk '/^Correction inventory: BEGIN$/{on=1}on{print}/^Correction inventory: END$/{exit}' "$LEDGER_REL" > "$marker"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m; s/successor plan commit \`[0-9a-f]{40}\`/cleanup-isolation successor plan commit \`$successor\`/; s/current source snapshot contains the twelve-path/current source snapshot retains the twelve-path/" "$BRIEF_REL"
  { cat "$prefix"; cat "$marker"; print -r -- "Correction map SHA-256: $MAP_SHA"; print -r -- "Correction evidence prefix SHA-256: $PREFIX_SHA"; print -r -- "Amended library plan blob SHA-256: $(git show "$successor:$PLAN_REL" | sha_bytes)"; print -r -- "Amended pointer plan blob SHA-256: $(git show "$successor:$POINTER_REL" | sha_bytes)"; print -r -- "Frozen report SHA-256: $REPORT_SHA"; awk '/^Failed RED_RECOVERY attempt: BEGIN$/{on=1}on{print}/^Failed RED_RECOVERY attempt: END$/{exit}' "$LEDGER_REL"; print -r -- 'Failed RED_RECOVERY attempt 2: BEGIN'; print -r -- 'Stage: PRE_WORKTREE'; print -r -- 'Primary error: RED_RECOVERY: cleanup evidence retry fixture failed'; print -r -- 'Secondary error: fixture_remove: attempts: parameter not set'; print -r -- 'Secondary error: fixture_remove: second: parameter not set'; print -r -- 'No Gradle build client: PASS'; print -r -- 'No detached worktree: PASS'; print -r -- 'Cleanup outcome: FAIL'; print -r -- "Log: $SECOND_LOG SHA-256: bc3fa401adaeaf96846a0266cea2e2c1bbbaa3c967e9eba5795875f3595ccd9e"; print -r -- "Outer summary: $SECOND_OUTER_SUMMARY SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; print -r -- "Fixture summary: $SECOND_FIXTURE_SUMMARY SHA-256: a958917a3dc2495d28e4475e141aeaa48d9bf8b0a7104be7c4a4d3123a6ed44c"; print -r -- 'Failed RED_RECOVERY attempt 2: END'; } > "$tmp"
  cmp -s "$prefix" <(awk 'NR<=6{print}' "$tmp") || die 'historical prefix changed'; mv -- "$tmp" "$LEDGER_REL"; rm -f -- "$prefix" "$marker"
}
cleanup_successor_gate() { [[ "$(git rev-parse HEAD)" == "$CLEANUP_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] && exact_plan_delta && correction_authority_valid "$LEDGER_REL" && first_failure_valid "$LEDGER_REL" && [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]; }
primary_inventory_valid() {
  local successor="$1" script
  script="$(mktemp -t task61-cleanup-primary)" || return 1
  awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$script"
  zsh "$script" rebound-inventory "$successor" >/dev/null; local rc=$?; rm -f -- "$script"; return "$rc"
}
cleanup_successor_authority_valid() {
  local successor="$1"
  [[ "$(git rev-parse HEAD)" == "$successor" && "$(git rev-parse "$successor^")" == "$CLEANUP_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$successor" | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || return 1
  [[ "$(awk '/^Planning baseline: /{print $3;n++}END{exit n==1?0:1}' "$BRIEF_REL")" == "$successor" ]] || return 1
  correction_authority_valid "$LEDGER_REL" && first_failure_valid "$LEDGER_REL" && second_failure_valid "$LEDGER_REL" && [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]] || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(awk '/^Amended library plan blob SHA-256:/{print $6;n++}END{exit n==1?0:1}' "$LEDGER_REL")" && "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(awk '/^Amended pointer plan blob SHA-256:/{print $6;n++}END{exit n==1?0:1}' "$LEDGER_REL")" ]] || return 1
  primary_inventory_valid "$successor"
}
cleanup_successor_head_index_valid() { [[ "$(git rev-parse HEAD)" == "$1" && -z "$(git diff --cached --name-only)" ]]; }
cleanup_successor_parent_valid() { [[ "$(git rev-parse "$1^")" == "$CLEANUP_SUCCESSOR_PARENT" ]]; }
cleanup_successor_exact_paths_valid() { [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]]; }
authenticated_committed_cleanup_successor() {
  [[ "$(git rev-parse "$COMMITTED_CLEANUP_SUCCESSOR^")" == "$CLEANUP_SUCCESSOR_PARENT" ]] && cleanup_successor_exact_paths_valid "$COMMITTED_CLEANUP_SUCCESSOR"
}
cleanup_successor_post_rebind_valid() {
  cleanup_successor_authority_valid "$1" && [[ -d "${SNAPSHOT_ROOT-}" ]] && assert_nonplan_dirt
}
cleanup_successor_post_cleanup_valid() {
  local successor="$1" captured_snapshot_path="$2"
  [[ -n "$captured_snapshot_path" && "$captured_snapshot_path" == ${TMPDIR:-/tmp}/task-6.1-cleanup-successor.* && ! -e "$captured_snapshot_path" ]] || return 1
  cleanup_successor_authority_valid "$successor" && [[ -z "${SNAPSHOT_ROOT-}" ]] && assert_retained_nonplan_dirt
}
cleanup_successor_dispatch() {
  local parent successor captured_snapshot_path
  [[ "$(git rev-parse HEAD)" == "$CLEANUP_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || die 'cleanup successor parent/index mismatch'
  exact_plan_delta && correction_authority_valid "$LEDGER_REL" && first_failure_valid "$LEDGER_REL" && [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]] || die 'cleanup successor pre-commit gate failed'
  capture_nonplan_dirt; parent="$(git rev-parse HEAD)"; git add -- "$PLAN_REL" "$POINTER_REL"; [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || die 'cleanup successor index is not exact'; git commit -m 'docs: repair red recovery cleanup isolation' >/dev/null
  successor="$(git rev-parse HEAD)"; [[ "$(git rev-parse "$successor^")" == "$parent" ]] || die 'cleanup successor parent mismatch'; [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$successor" | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || die 'cleanup successor paths mismatch'
  rebind_cleanup_successor "$successor"; cleanup_successor_post_rebind_valid "$successor" || die 'cleanup successor rebound authority mismatch'; captured_snapshot_path="$SNAPSHOT_ROOT"; typeset -g CLEANUP_SUCCESSOR_CAPTURED_SNAPSHOT_PATH="$captured_snapshot_path"; rm -rf -- "$captured_snapshot_path"; [[ ! -e "$captured_snapshot_path" ]] || die 'cleanup successor snapshot residue'; unset SNAPSHOT_ROOT; cleanup_successor_post_cleanup_valid "$successor" "$captured_snapshot_path" || die 'cleanup successor post-cleanup authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
cleanup_successor_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo sha count=0 source primary_script correction_output baseline_ledger baseline_brief baseline_report baseline_diff baseline_preserved_diff baseline_preserved_report artifact_root candidate wrong_parent missing_sha captured_snapshot_path committed_patch committed_plan_blob committed_pointer_blob
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-cleanup-successor-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$CLEANUP_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  authenticated_committed_cleanup_successor || die 'committed cleanup successor authentication failed'; git diff --binary --no-renames "$CLEANUP_SUCCESSOR_PARENT" "$COMMITTED_CLEANUP_SUCCESSOR" -- "$PLAN_REL" "$POINTER_REL" > "$root/plans.patch"; [[ -s "$root/plans.patch" && "$(git diff --name-only "$CLEANUP_SUCCESSOR_PARENT" "$COMMITTED_CLEANUP_SUCCESSOR" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == $'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md' ]] || die 'committed cleanup delta is empty or not exact'; committed_plan_blob="$(git show "$COMMITTED_CLEANUP_SUCCESSOR:$PLAN_REL" | sha_bytes)"; committed_pointer_blob="$(git show "$COMMITTED_CLEANUP_SUCCESSOR:$POINTER_REL" | sha_bytes)"; git -C "$repo" apply --whitespace=nowarn "$root/plans.patch"; [[ "$(sha256 "$repo/$PLAN_REL")" == "$committed_plan_blob" && "$(sha256 "$repo/$POINTER_REL")" == "$committed_pointer_blob" ]] || die 'committed cleanup delta blobs mismatch'; cp -- "$repo/$PLAN_REL" "$root/plan"; cp -- "$repo/$POINTER_REL" "$root/pointer"; for source in "$BRIEF_REL" "$LEDGER_REL" "$REPORT_REL"; do mkdir -p -- "$repo/${source:h}"; cp -- "$source" "$repo/$source"; done; perl -0pi -e 's/^Failed RED_RECOVERY attempt 2: BEGIN\n.*?^Failed RED_RECOVERY attempt 2: END\n?//ms' "$repo/$LEDGER_REL"; artifact_root="$root/artifacts"; mkdir -p -- "$artifact_root"; cp -- "$FIRST_LOG" "$artifact_root/first-log"; cp -- "$FIRST_SUMMARY" "$artifact_root/first-summary"; cp -- "$SECOND_LOG" "$artifact_root/second-log"; cp -- "$SECOND_OUTER_SUMMARY" "$artifact_root/second-outer"; cp -- "$SECOND_FIXTURE_SUMMARY" "$artifact_root/second-fixture"; export TASK_6_1_FIRST_LOG="$artifact_root/first-log" TASK_6_1_FIRST_SUMMARY="$artifact_root/first-summary" TASK_6_1_SECOND_LOG="$artifact_root/second-log" TASK_6_1_SECOND_OUTER_SUMMARY="$artifact_root/second-outer" TASK_6_1_SECOND_FIXTURE_SUMMARY="$artifact_root/second-fixture"; cd "$repo"; ((++count))
  exact_plan_delta && correction_authority_valid "$LEDGER_REL" && first_failure_valid "$LEDGER_REL"; ((++count))
  git restore --source=HEAD -- "$POINTER_REL"; expect_fail exact_plan_delta; cp -- "$root/pointer" "$POINTER_REL"; ((++count))
  print -r -- extra > docs/superpowers/plans/extra-cleanup-successor.md; expect_fail exact_plan_delta; rm -f docs/superpowers/plans/extra-cleanup-successor.md; ((++count))
  cp "$LEDGER_REL" "$root/ledger"; for mutation in reorder alter missing duplicate; do cp "$root/ledger" "$LEDGER_REL"; case "$mutation" in reorder) perl -0pi -e 's/(Stage: PRE_WORKTREE\n)(Observed error: cleanup_retry_fixture:1: fixture_root: parameter not set\n)/$2$1/' "$LEDGER_REL";; alter) perl -0pi -e 's/No detached worktree: PASS/No detached worktree: FAIL/' "$LEDGER_REL";; missing) perl -0pi -e 's/^Summary: .*\n//m' "$LEDGER_REL";; duplicate) awk '/^Failed RED_RECOVERY attempt: BEGIN$/{on=1}on{print}/^Failed RED_RECOVERY attempt: END$/{exit}' "$root/ledger" >> "$LEDGER_REL";; esac; expect_fail first_failure_valid "$LEDGER_REL"; ((++count)); done; cp "$root/ledger" "$LEDGER_REL"
  cleanup_successor_dispatch; sha="$(git rev-parse HEAD)"; captured_snapshot_path="$CLEANUP_SUCCESSOR_CAPTURED_SNAPSHOT_PATH"; cleanup_successor_post_cleanup_valid "$sha" "$captured_snapshot_path"; ((++count))
  cp "$LEDGER_REL" "$root/rebound"; cp "$BRIEF_REL" "$root/brief"; cp "$REPORT_REL" "$root/report"; baseline_diff="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; baseline_preserved_diff="$PRESERVED_NONPLAN_DIFF_SHA"; baseline_preserved_report="$PRESERVED_REPORT_SHA"
  # Every validator case starts from the same rebound authority, changes one class, then proves the restored positive.
  for mutation in wrong-parent extra-path missing-path dirty-index wrong-brief prefix marker map report library-blob pointer-blob first-block second-block order artifact nonplan-dirt residue; do
    cp "$root/rebound" "$LEDGER_REL"; cp "$root/brief" "$BRIEF_REL"; cp "$root/report" "$REPORT_REL"; PRESERVED_NONPLAN_DIFF_SHA="$baseline_preserved_diff"; PRESERVED_REPORT_SHA="$baseline_preserved_report"; git reset -q; unset SNAPSHOT_ROOT
    cp "$artifact_root/second-fixture" "$root/second-fixture.baseline"; cp "$SECOND_FIXTURE_SUMMARY" "$root/second-fixture.baseline"
    case "$mutation" in
      wrong-parent)
        wrong_parent="$(git commit-tree "$(git rev-parse "$CLEANUP_SUCCESSOR_PARENT^{tree}")" -p "$(git rev-parse "$CLEANUP_SUCCESSOR_PARENT^")" -m wrong-parent-base)"
        candidate="$(git commit-tree "$(git rev-parse "$sha^{tree}")" -p "$wrong_parent" -m wrong-parent)"; git checkout -q --detach "$candidate"
        perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $candidate/m; s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $(git show "$candidate:$PLAN_REL" | sha_bytes)/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $(git show "$candidate:$POINTER_REL" | sha_bytes)/m" "$BRIEF_REL" "$LEDGER_REL"
        cleanup_successor_head_index_valid "$candidate" && cleanup_successor_exact_paths_valid "$candidate" && [[ "$(awk '/^Planning baseline: /{print $3}' "$BRIEF_REL")" == "$candidate" ]] || die 'wrong-parent fixture did not reach ancestry'
        expect_fail cleanup_successor_parent_valid "$candidate" || die 'wrong parent predicate accepted'; expect_fail cleanup_successor_post_cleanup_valid "$candidate" "$captured_snapshot_path" || die 'wrong parent accepted';;
      extra-path)
        git checkout -q --detach "$CLEANUP_SUCCESSOR_PARENT"; git checkout "$sha" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-cleanup-successor.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-cleanup-successor.md; git commit -qm extra-path; candidate="$(git rev-parse HEAD)"
        perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $candidate/m; s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $(git show "$candidate:$PLAN_REL" | sha_bytes)/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $(git show "$candidate:$POINTER_REL" | sha_bytes)/m" "$BRIEF_REL" "$LEDGER_REL"
        cleanup_successor_head_index_valid "$candidate" && cleanup_successor_parent_valid "$candidate" && [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$candidate" | LC_ALL=C sort | wc -l | tr -d ' ')" == 3 ]] || die 'extra-path fixture did not reach exact-path predicate'
        expect_fail cleanup_successor_exact_paths_valid "$candidate" || die 'extra path predicate accepted'; expect_fail cleanup_successor_post_cleanup_valid "$candidate" "$captured_snapshot_path" || die 'extra path lineage accepted';;
      missing-path)
        git checkout -q --detach "$CLEANUP_SUCCESSOR_PARENT"; git checkout "$sha" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing-path; missing_sha="$(git rev-parse HEAD)"
        perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $missing_sha/m; s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $(git show "$missing_sha:$PLAN_REL" | sha_bytes)/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $(git show "$missing_sha:$POINTER_REL" | sha_bytes)/m" "$BRIEF_REL" "$LEDGER_REL"
        cleanup_successor_head_index_valid "$missing_sha" && cleanup_successor_parent_valid "$missing_sha" && [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$missing_sha" | wc -l | tr -d ' ')" == 1 ]] || die 'missing-path fixture did not reach exact-path predicate'
        expect_fail cleanup_successor_exact_paths_valid "$missing_sha" || die 'missing path predicate accepted'; expect_fail cleanup_successor_post_cleanup_valid "$missing_sha" "$captured_snapshot_path" || die 'missing path lineage accepted';;
      dirty-index) git add -f -- "$REPORT_REL";;
      wrong-brief) perl -0pi -e 's/^Planning baseline: .*/Planning baseline: 0000000000000000000000000000000000000000/m' "$BRIEF_REL";;
      prefix) perl -0pi -e 's/^Planning baseline: .*/Planning baseline: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";;
      marker) perl -0pi -e 's/^Correction inventory: BEGIN$/Correction inventory: BROKEN/m' "$LEDGER_REL";;
      map) perl -0pi -e 's/^Correction map SHA-256: .*/Correction map SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";;
      report) print -r -- drift >> "$REPORT_REL";;
      library-blob) perl -0pi -e 's/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";;
      pointer-blob) perl -0pi -e 's/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: 0000000000000000000000000000000000000000000000000000000000000000/m' "$LEDGER_REL";;
      first-block) perl -0pi -e 's/^Observed error: .*/Observed error: broken/m' "$LEDGER_REL";;
      second-block) perl -0pi -e 's/^Cleanup outcome: FAIL$/Cleanup outcome: PASS/m' "$LEDGER_REL";;
      order) perl -0pi -e 's/(Failed RED_RECOVERY attempt: END\n)(Failed RED_RECOVERY attempt 2: BEGIN\n)/$2$1/' "$LEDGER_REL";;
      artifact) print -r -- drift >> "$SECOND_FIXTURE_SUMMARY";;
      nonplan-dirt) source='shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt'; cp -- "$source" "$root/nonplan-dirt.source"; print -n -- x >> "$source";;
      residue)
        [[ -z "${SNAPSHOT_ROOT-}" && ! -e "$captured_snapshot_path" && "$captured_snapshot_path" == ${TMPDIR:-/tmp}/task-6.1-cleanup-successor.* ]] || die 'residue fixture lost authentic captured path'
        mkdir -p -- "$captured_snapshot_path"
        cleanup_successor_authority_valid "$sha" && assert_retained_nonplan_dirt || die 'residue fixture changed non-path authority';;
    esac
    expect_fail cleanup_successor_post_cleanup_valid "$sha" "$captured_snapshot_path" || die "cleanup post-validator accepted $mutation"; ((++count))
    [[ "$mutation" != residue ]] || { rm -rf -- "$captured_snapshot_path"; [[ ! -e "$captured_snapshot_path" ]] || die 'residue fixture did not remove authentic captured path'; }
    cp "$root/rebound" "$LEDGER_REL"; cp "$root/brief" "$BRIEF_REL"; cp "$root/report" "$REPORT_REL"; cp "$root/second-fixture.baseline" "$SECOND_FIXTURE_SUMMARY"; PRESERVED_NONPLAN_DIFF_SHA="$baseline_preserved_diff"; PRESERVED_REPORT_SHA="$baseline_preserved_report"; git reset -q; unset SNAPSHOT_ROOT
    [[ "$mutation" != nonplan-dirt ]] || mv -- "$root/nonplan-dirt.source" "$source"
    git checkout -q --detach "$sha"; rm -f -- docs/superpowers/plans/extra-cleanup-successor.md
    [[ "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$baseline_diff" ]] || die "cleanup post-validator restore drift: $mutation"
    cleanup_successor_post_cleanup_valid "$sha" "$captured_snapshot_path" || die "cleanup post-validator restore failed: $mutation"
  done
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next} m && /^```zsh$/{c=1;next} m && c && /^```$/{exit} c{print}' "$PLAN_REL" > "$primary_script"
  correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$sha")"; [[ "$correction_output" == 'cleanup_successor_correction_fixture=PASS controls=21' ]] || die 'cleanup successor correction fixture mismatch'; ((count+=21))
  for mutation in reorder alter missing duplicate; do cp "$LEDGER_REL" "$root/rebound"; case "$mutation" in reorder) perl -0pi -e 's/(Primary error:.*\n)(Secondary error: fixture_remove: attempts: parameter not set\n)/$2$1/' "$LEDGER_REL";; alter) perl -0pi -e 's/Cleanup outcome: FAIL/Cleanup outcome: PASS/' "$LEDGER_REL";; missing) perl -0pi -e 's/^Fixture summary: .*\n//m' "$LEDGER_REL";; duplicate) awk '/^Failed RED_RECOVERY attempt 2: BEGIN$/{on=1}on{print}/^Failed RED_RECOVERY attempt 2: END$/{exit}' "$root/rebound" >> "$LEDGER_REL";; esac; expect_fail second_failure_valid "$LEDGER_REL"; cp "$root/rebound" "$LEDGER_REL"; ((++count)); done
  git diff --cached --quiet; git diff --check; [[ -z "${SNAPSHOT_ROOT-}" ]]; unset TASK_6_1_FIRST_LOG TASK_6_1_FIRST_SUMMARY TASK_6_1_SECOND_LOG TASK_6_1_SECOND_OUTER_SUMMARY TASK_6_1_SECOND_FIXTURE_SUMMARY; cd /; rm -rf -- "$root"; trap - EXIT; print -r -- "cleanup_successor_matrix=PASS controls=$count residue=0 index=empty"
)
case "${1:-cleanup-successor-matrix}" in
  cleanup-isolation-successor-planning-amendment) (( $# == 1 )) || die 'usage: cleanup-isolation-successor-planning-amendment'; cleanup_successor_dispatch;;
  cleanup-successor-matrix) (( $# == 1 )) || die 'usage: cleanup-successor-matrix'; cleanup_successor_matrix;;
  *) die 'usage: cleanup-isolation successor authority {cleanup-successor-matrix|cleanup-isolation-successor-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-CLEANUP-ISOLATION-SUCCESSOR:END -->

### RED_RECOVERY Cleanup-Isolation Committed-Delta Repair Successor

The committed cleanup-isolation successor `fe9b565de72417a2b1bf584370d2eab29bbfc73e` is a direct
child of `89a65070434c6c1f03880412e9a741653b85d1a3`, but its dispatcher is disabled permanently: it
captures `plans.patch` from the live working tree after those two plans have already been committed.
That source is therefore empty and `git apply` correctly fails closed with `No valid patches in input
(allow with "--allow-empty")`. Do not retry or rebind that dispatcher in place.

The following is the sole replacement planning dispatcher. It creates a new direct child of `fe9b565`,
then and only then rebinds the ignored brief and controller ledger. It authenticates the rebound brief
SHA, its direct parent, the exact two-path commit, and the two committed plan blobs before deriving a
patch. The disposable matrix must prove the old live source empty and non-applicable; derive its new
patch only from `git diff --binary --no-renames "$CLEANUP_SUCCESSOR_PARENT"
"$COMMITTED_SUCCESSOR" -- "$PLAN_REL" "$POINTER_REL"`; prove that delta nonempty, exact, and
applicable to a clone detached at `CLEANUP_SUCCESSOR_PARENT`; and compare the applied plan and pointer
blobs to the committed successor blobs. It retains the existing historical cleanup residue, captured
path, ancestry/path, one-byte non-plan dirt, failed-attempt, correction-pre, host-preservation, twelve
row/map, frozen-report, two PRE_WORKTREE blocks, and five-failure-artifact controls. No recovery
success is claimed by this plan repair.

<!-- TASK-6.1-RED-RECOVERY-COMMITTED-DELTA-REPAIR-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 committed-delta successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly CLEANUP_SUCCESSOR_PARENT=fe9b565de72417a2b1bf584370d2eab29bbfc73e
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_successor() {
  awk '/^Committed cleanup successor plan SHA: [0-9a-f]{40}$/ {print $6; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
committed_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_successor)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$CLEANUP_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$CLEANUP_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_committed_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Committed cleanup successor plan SHA:' "$LEDGER_REL" && die 'cleanup successor SHA already rebound'
  print -r -- "Committed cleanup successor plan SHA: $successor" >> "$LEDGER_REL"
}
committed_delta_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: repair red recovery committed delta authority' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$CLEANUP_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_committed_successor "$successor"
  committed_successor_valid "$successor" || die 'rebound committed successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
committed_delta_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-committed-delta-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$CLEANUP_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  # The source used by disabled fe9b565 is demonstrably empty and invalid for git apply.
  git diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/live.patch"
  [[ ! -s "$root/live.patch" ]] && expect_fail git apply --whitespace=nowarn "$root/live.patch" || die 'broken live-diff source was accepted'; ((++count))
  # Only after proving the historical source do we install this successor's current two-plan payload.
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  # Construct the new two-plan child through its real precommit and rebind gates.
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; committed_delta_dispatch; successor="$(git rev-parse HEAD)"; committed_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*/Committed cleanup successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Committed cleanup successor plan SHA: .*/Committed cleanup successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Committed cleanup successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "committed successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'committed_cleanup_successor_correction_fixture=PASS controls=25' ]] || die 'committed successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$CLEANUP_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$CLEANUP_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$CLEANUP_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail committed_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$CLEANUP_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_committed_successor "$wrong_parent"; expect_fail committed_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$CLEANUP_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-committed-delta.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-committed-delta.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_committed_successor "$extra_sha"; expect_fail committed_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$CLEANUP_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_committed_successor "$missing_sha"; expect_fail committed_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "committed_delta_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-committed-delta-matrix}" in
  committed-delta-planning-amendment) (( $# == 1 )) || die 'usage: committed-delta-planning-amendment'; committed_delta_dispatch;;
  committed-delta-matrix) (( $# == 1 )) || die 'usage: committed-delta-matrix'; committed_delta_matrix;;
  *) die 'usage: committed-delta successor authority {committed-delta-matrix|committed-delta-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-COMMITTED-DELTA-REPAIR-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-SIMPLIFICATION-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 simplification successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly SIMPLIFICATION_SUCCESSOR_PARENT=4f850915b6686a8486c6b41a4e7e6b7dce655ef8
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_simplified() {
  awk '/^Simplified recovery successor plan SHA: [0-9a-f]{40}$/ {print $6; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
simplified_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_simplified)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$SIMPLIFICATION_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$SIMPLIFICATION_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_simplified_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Simplified recovery successor plan SHA:' "$LEDGER_REL" && die 'simplified successor SHA already rebound'
  print -r -- "Simplified recovery successor plan SHA: $successor" >> "$LEDGER_REL"
}
simplification_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: simplify red recovery harness' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$SIMPLIFICATION_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_simplified_successor "$successor"
  simplified_successor_valid "$successor" || die 'rebound simplified successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
simplification_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-simplification-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$SIMPLIFICATION_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  # Install this successor's current two-plan payload into the parent checkout.
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  # Construct the new two-plan child through its real precommit and rebind gates.
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; simplification_dispatch; successor="$(git rev-parse HEAD)"; simplified_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Simplified recovery successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Simplified recovery successor plan SHA: .*/Simplified recovery successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Simplified recovery successor plan SHA: .*/Simplified recovery successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Simplified recovery successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "simplified successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'simplified_successor_correction_fixture=PASS controls=25' ]] || die 'simplified successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$SIMPLIFICATION_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$SIMPLIFICATION_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$SIMPLIFICATION_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail simplified_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$SIMPLIFICATION_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_simplified_successor "$wrong_parent"; expect_fail simplified_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$SIMPLIFICATION_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-simplification.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-simplification.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_simplified_successor "$extra_sha"; expect_fail simplified_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$SIMPLIFICATION_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_simplified_successor "$missing_sha"; expect_fail simplified_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "simplification_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-simplification-matrix}" in
  simplification-planning-amendment) (( $# == 1 )) || die 'usage: simplification-planning-amendment'; simplification_dispatch;;
  simplification-matrix) (( $# == 1 )) || die 'usage: simplification-matrix'; simplification_matrix;;
  *) die 'usage: simplification successor authority {simplification-matrix|simplification-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-SIMPLIFICATION-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-DIAGNOSTIC-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 diagnostic successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly DIAGNOSTIC_SUCCESSOR_PARENT=6f580fadd10c4bf63b79165a899b9dd31df9ee1b
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_diagnostic() {
  awk '/^Diagnostic successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
diagnostic_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_diagnostic)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$DIAGNOSTIC_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$DIAGNOSTIC_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_diagnostic_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Diagnostic successor plan SHA:' "$LEDGER_REL" && die 'diagnostic successor SHA already rebound'
  print -r -- "Diagnostic successor plan SHA: $successor" >> "$LEDGER_REL"
}
diagnostic_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: repair red recovery diagnostic normalization' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$DIAGNOSTIC_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_diagnostic_successor "$successor"
  diagnostic_successor_valid "$successor" || die 'rebound diagnostic successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
diagnostic_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-diagnostic-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$DIAGNOSTIC_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; diagnostic_dispatch; successor="$(git rev-parse HEAD)"; diagnostic_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Diagnostic successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Diagnostic successor plan SHA: .*/Diagnostic successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Diagnostic successor plan SHA: .*/Diagnostic successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Diagnostic successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "diagnostic successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'diagnostic_successor_correction_fixture=PASS controls=25' ]] || die 'diagnostic successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$DIAGNOSTIC_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$DIAGNOSTIC_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$DIAGNOSTIC_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail diagnostic_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$DIAGNOSTIC_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_diagnostic_successor "$wrong_parent"; expect_fail diagnostic_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$DIAGNOSTIC_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-diagnostic.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-diagnostic.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_diagnostic_successor "$extra_sha"; expect_fail diagnostic_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$DIAGNOSTIC_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_diagnostic_successor "$missing_sha"; expect_fail diagnostic_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "diagnostic_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-diagnostic-matrix}" in
  diagnostic-planning-amendment) (( $# == 1 )) || die 'usage: diagnostic-planning-amendment'; diagnostic_dispatch;;
  diagnostic-matrix) (( $# == 1 )) || die 'usage: diagnostic-matrix'; diagnostic_matrix;;
  *) die 'usage: diagnostic successor authority {diagnostic-matrix|diagnostic-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-DIAGNOSTIC-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-WARNINGS-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 warnings successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly WARNINGS_SUCCESSOR_PARENT=bf16d36fc7198bef1c35a3229130d8870bad71f1
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_warnings() {
  awk '/^Warnings successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
warnings_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_warnings)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$WARNINGS_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$WARNINGS_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_warnings_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Warnings successor plan SHA:' "$LEDGER_REL" && die 'warnings successor SHA already rebound'
  print -r -- "Warnings successor plan SHA: $successor" >> "$LEDGER_REL"
}
warnings_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: ignore benign warnings in red recovery' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$WARNINGS_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_warnings_successor "$successor"
  warnings_successor_valid "$successor" || die 'rebound warnings successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
warnings_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-warnings-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$WARNINGS_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; warnings_dispatch; successor="$(git rev-parse HEAD)"; warnings_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Warnings successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Warnings successor plan SHA: .*/Warnings successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Warnings successor plan SHA: .*/Warnings successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Warnings successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "warnings successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'warnings_successor_correction_fixture=PASS controls=25' ]] || die 'warnings successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$WARNINGS_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$WARNINGS_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$WARNINGS_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail warnings_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$WARNINGS_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_warnings_successor "$wrong_parent"; expect_fail warnings_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$WARNINGS_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-warnings.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-warnings.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_warnings_successor "$extra_sha"; expect_fail warnings_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$WARNINGS_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_warnings_successor "$missing_sha"; expect_fail warnings_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "warnings_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-warnings-matrix}" in
  warnings-planning-amendment) (( $# == 1 )) || die 'usage: warnings-planning-amendment'; warnings_dispatch;;
  warnings-matrix) (( $# == 1 )) || die 'usage: warnings-matrix'; warnings_matrix;;
  *) die 'usage: warnings successor authority {warnings-matrix|warnings-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-WARNINGS-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-COMPREHENSIVE-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 comprehensive successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly COMPREHENSIVE_SUCCESSOR_PARENT=0a0ebe2f382cb9ab903ee50b21cf16cea2304784
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_comprehensive() {
  awk '/^Comprehensive successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
comprehensive_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_comprehensive)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$COMPREHENSIVE_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$COMPREHENSIVE_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_comprehensive_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Comprehensive successor plan SHA:' "$LEDGER_REL" && die 'comprehensive successor SHA already rebound'
  print -r -- "Comprehensive successor plan SHA: $successor" >> "$LEDGER_REL"
}
comprehensive_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: simplify red recovery diagnostic normalization' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$COMPREHENSIVE_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_comprehensive_successor "$successor"
  comprehensive_successor_valid "$successor" || die 'rebound comprehensive successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
comprehensive_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-comprehensive-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$COMPREHENSIVE_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; comprehensive_dispatch; successor="$(git rev-parse HEAD)"; comprehensive_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Comprehensive successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Comprehensive successor plan SHA: .*/Comprehensive successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Comprehensive successor plan SHA: .*/Comprehensive successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Comprehensive successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "comprehensive successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'comprehensive_successor_correction_fixture=PASS controls=25' ]] || die 'comprehensive successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$COMPREHENSIVE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$COMPREHENSIVE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$COMPREHENSIVE_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail comprehensive_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$COMPREHENSIVE_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_comprehensive_successor "$wrong_parent"; expect_fail comprehensive_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$COMPREHENSIVE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-comprehensive.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-comprehensive.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_comprehensive_successor "$extra_sha"; expect_fail comprehensive_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$COMPREHENSIVE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_comprehensive_successor "$missing_sha"; expect_fail comprehensive_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "comprehensive_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-comprehensive-matrix}" in
  comprehensive-planning-amendment) (( $# == 1 )) || die 'usage: comprehensive-planning-amendment'; comprehensive_dispatch;;
  comprehensive-matrix) (( $# == 1 )) || die 'usage: comprehensive-matrix'; comprehensive_matrix;;
  *) die 'usage: comprehensive successor authority {comprehensive-matrix|comprehensive-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-COMPREHENSIVE-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-MULTILINE-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 multiline successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly MULTILINE_SUCCESSOR_PARENT=e2606e7f143a3062b4bbcd67ad7982ca50bd10ae
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_multiline() {
  awk '/^Multiline successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
multiline_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_multiline)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$MULTILINE_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$MULTILINE_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_multiline_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Multiline successor plan SHA:' "$LEDGER_REL" && die 'multiline successor SHA already rebound'
  print -r -- "Multiline successor plan SHA: $successor" >> "$LEDGER_REL"
}
multiline_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: handle multiline unresolved references in red recovery' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$MULTILINE_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_multiline_successor "$successor"
  multiline_successor_valid "$successor" || die 'rebound multiline successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
multiline_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-multiline-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$MULTILINE_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; multiline_dispatch; successor="$(git rev-parse HEAD)"; multiline_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Multiline successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Multiline successor plan SHA: .*/Multiline successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Multiline successor plan SHA: .*/Multiline successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Multiline successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "multiline successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'multiline_successor_correction_fixture=PASS controls=25' ]] || die 'multiline successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$MULTILINE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$MULTILINE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$MULTILINE_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail multiline_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$MULTILINE_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_multiline_successor "$wrong_parent"; expect_fail multiline_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$MULTILINE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-multiline.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-multiline.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_multiline_successor "$extra_sha"; expect_fail multiline_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$MULTILINE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_multiline_successor "$missing_sha"; expect_fail multiline_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "multiline_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-multiline-matrix}" in
  multiline-planning-amendment) (( $# == 1 )) || die 'usage: multiline-planning-amendment'; multiline_dispatch;;
  multiline-matrix) (( $# == 1 )) || die 'usage: multiline-matrix'; multiline_matrix;;
  *) die 'usage: multiline successor authority {multiline-matrix|multiline-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-MULTILINE-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-DIRECT-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 direct successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly DIRECT_SUCCESSOR_PARENT=5a07209d71b605951bd1576c5c1898b642cec9d9
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_direct() {
  awk '/^Direct successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
direct_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_direct)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$DIRECT_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$DIRECT_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_direct_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Direct successor plan SHA:' "$LEDGER_REL" && die 'direct successor SHA already rebound'
  print -r -- "Direct successor plan SHA: $successor" >> "$LEDGER_REL"
}
direct_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: fix direct red exception detection in red recovery' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$DIRECT_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_direct_successor "$successor"
  direct_successor_valid "$successor" || die 'rebound direct successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
direct_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-direct-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$DIRECT_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; direct_dispatch; successor="$(git rev-parse HEAD)"; direct_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Direct successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Direct successor plan SHA: .*/Direct successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Direct successor plan SHA: .*/Direct successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Direct successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "direct successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'direct_successor_correction_fixture=PASS controls=25' ]] || die 'direct successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$DIRECT_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$DIRECT_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$DIRECT_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail direct_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$DIRECT_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_direct_successor "$wrong_parent"; expect_fail direct_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$DIRECT_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-direct.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-direct.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_direct_successor "$extra_sha"; expect_fail direct_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$DIRECT_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_direct_successor "$missing_sha"; expect_fail direct_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "direct_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-direct-matrix}" in
  direct-planning-amendment) (( $# == 1 )) || die 'usage: direct-planning-amendment'; direct_dispatch;;
  direct-matrix) (( $# == 1 )) || die 'usage: direct-matrix'; direct_matrix;;
  *) die 'usage: direct successor authority {direct-matrix|direct-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-DIRECT-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-BRACE-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 brace successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly BRACE_SUCCESSOR_PARENT=b48a8be41e19358e09dc3bfc360d3fc86e1ce943
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_brace() {
  awk '/^Brace successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
brace_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_brace)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$BRACE_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$BRACE_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_brace_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Brace successor plan SHA:' "$LEDGER_REL" && die 'brace successor SHA already rebound'
  print -r -- "Brace successor plan SHA: $successor" >> "$LEDGER_REL"
}
brace_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: fix r02 pwd expansion in red recovery' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$BRACE_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_brace_successor "$successor"
  brace_successor_valid "$successor" || die 'rebound brace successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
brace_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-brace-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$BRACE_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; brace_dispatch; successor="$(git rev-parse HEAD)"; brace_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Brace successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Brace successor plan SHA: .*/Brace successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Brace successor plan SHA: .*/Brace successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Brace successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "brace successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'brace_successor_correction_fixture=PASS controls=25' ]] || die 'brace successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$BRACE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$BRACE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$BRACE_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail brace_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$BRACE_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_brace_successor "$wrong_parent"; expect_fail brace_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$BRACE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-brace.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-brace.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_brace_successor "$extra_sha"; expect_fail brace_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$BRACE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_brace_successor "$missing_sha"; expect_fail brace_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "brace_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-brace-matrix}" in
  brace-planning-amendment) (( $# == 1 )) || die 'usage: brace-planning-amendment'; brace_dispatch;;
  brace-matrix) (( $# == 1 )) || die 'usage: brace-matrix'; brace_matrix;;
  *) die 'usage: brace successor authority {brace-matrix|brace-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-BRACE-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-RECORD-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 record successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly RECORD_SUCCESSOR_PARENT=d4c1f615aedeaf400be271c7d864ef73b566571a
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_record() {
  awk '/^Record successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
record_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_record)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$RECORD_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$RECORD_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_record_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Record successor plan SHA:' "$LEDGER_REL" && die 'record successor SHA already rebound'
  print -r -- "Record successor plan SHA: $successor" >> "$LEDGER_REL"
}
record_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: fix r14 record array in red recovery' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$RECORD_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_record_successor "$successor"
  record_successor_valid "$successor" || die 'rebound record successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
record_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-record-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$RECORD_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; record_dispatch; successor="$(git rev-parse HEAD)"; record_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Record successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Record successor plan SHA: .*/Record successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Record successor plan SHA: .*/Record successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Record successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "record successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'record_successor_correction_fixture=PASS controls=25' ]] || die 'record successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$RECORD_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$RECORD_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$RECORD_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail record_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$RECORD_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_record_successor "$wrong_parent"; expect_fail record_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$RECORD_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-record.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-record.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_record_successor "$extra_sha"; expect_fail record_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$RECORD_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_record_successor "$missing_sha"; expect_fail record_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "record_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-record-matrix}" in
  record-planning-amendment) (( $# == 1 )) || die 'usage: record-planning-amendment'; record_dispatch;;
  record-matrix) (( $# == 1 )) || die 'usage: record-matrix'; record_matrix;;
  *) die 'usage: record successor authority {record-matrix|record-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-RECORD-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-MANIFEST-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 manifest successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly MANIFEST_SUCCESSOR_PARENT=1e3652f3df65325550f17678dd56950ba7a72da5
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_manifest() {
  awk '/^Manifest successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
manifest_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_manifest)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$MANIFEST_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$MANIFEST_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_manifest_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Manifest successor plan SHA:' "$LEDGER_REL" && die 'manifest successor SHA already rebound'
  print -r -- "Manifest successor plan SHA: $successor" >> "$LEDGER_REL"
}
manifest_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: correct implementation manifest to 112 endpoints' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$MANIFEST_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_manifest_successor "$successor"
  manifest_successor_valid "$successor" || die 'rebound manifest successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
manifest_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-manifest-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$MANIFEST_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  print -r -- '' >> "$PLAN_REL"; print -r -- '' >> "$POINTER_REL"; manifest_dispatch; successor="$(git rev-parse HEAD)"; manifest_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Manifest successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Manifest successor plan SHA: .*/Manifest successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Manifest successor plan SHA: .*/Manifest successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Manifest successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "manifest successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'manifest_successor_correction_fixture=PASS controls=25' ]] || die 'manifest successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$MANIFEST_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$MANIFEST_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$MANIFEST_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail manifest_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$MANIFEST_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_manifest_successor "$wrong_parent"; expect_fail manifest_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$MANIFEST_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-manifest.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-manifest.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_manifest_successor "$extra_sha"; expect_fail manifest_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$MANIFEST_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_manifest_successor "$missing_sha"; expect_fail manifest_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "manifest_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-manifest-matrix}" in
  manifest-planning-amendment) (( $# == 1 )) || die 'usage: manifest-planning-amendment'; manifest_dispatch;;
  manifest-matrix) (( $# == 1 )) || die 'usage: manifest-matrix'; manifest_matrix;;
  *) die 'usage: manifest successor authority {manifest-matrix|manifest-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-MANIFEST-SUCCESSOR:END -->

<!-- TASK-6.1-RED-RECOVERY-SCOPE-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 scope successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly SCOPE_SUCCESSOR_PARENT=b71819d067c74e5287c31b122f46a600f97539f8
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_scope() {
  awk '/^Scope successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
scope_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_scope)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$SCOPE_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$SCOPE_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_scope_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Scope successor plan SHA:' "$LEDGER_REL" && die 'scope successor SHA already rebound'
  print -r -- "Scope successor plan SHA: $successor" >> "$LEDGER_REL"
}
scope_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: reconcile library extraction scope gaps' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$SCOPE_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_scope_successor "$successor"
  scope_successor_valid "$successor" || die 'rebound scope successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
scope_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-scope-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$SCOPE_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  scope_dispatch; successor="$(git rev-parse HEAD)"; scope_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Scope successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Scope successor plan SHA: .*/Scope successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Scope successor plan SHA: .*/Scope successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Scope successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "scope successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'scope_successor_correction_fixture=PASS controls=25' ]] || die 'scope successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$SCOPE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$SCOPE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$SCOPE_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail scope_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$SCOPE_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_scope_successor "$wrong_parent"; expect_fail scope_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$SCOPE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-scope.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-scope.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_scope_successor "$extra_sha"; expect_fail scope_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$SCOPE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_scope_successor "$missing_sha"; expect_fail scope_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "scope_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-scope-matrix}" in
  scope-planning-amendment) (( $# == 1 )) || die 'usage: scope-planning-amendment'; scope_dispatch;;
  scope-matrix) (( $# == 1 )) || die 'usage: scope-matrix'; scope_matrix;;
  *) die 'usage: scope successor authority {scope-matrix|scope-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-SCOPE-SUCCESSOR:END -->
<!-- TASK-6.1-RED-RECOVERY-RECONCILE-SUCCESSOR:START -->
```zsh
emulate -L zsh
setopt errexit nounset pipefail
die() { print -u2 -r -- "task-6.1 reconcile successor: $*"; exit 1; }
sha256() { shasum -a 256 -- "$1" | awk '{print $1}'; }
sha_bytes() { shasum -a 256 | awk '{print $1}'; }
readonly RECONCILE_SUCCESSOR_PARENT=9bdf6874ac94785827542cfffb88b60370906229
readonly PLAN_REL=docs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md
readonly POINTER_REL=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
readonly BRIEF_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-brief.md
readonly LEDGER_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-controller-progress.md
readonly REPORT_REL=.superpowers/sdd/2026-07-27-feature-first-modularization/task-6.1-report.md
readonly REPORT_SHA=2852fcd75fafc505f9f189ec68f039e8ba714bfcae34e068bf8cff8de7b211a5
readonly PLAN_PATHS=$'docs/superpowers/plans/2026-07-27-feature-first-modularization.md\ndocs/superpowers/plans/2026-08-10-library-feature-extraction-plan.md'

brief_successor() {
  awk '/^Planning baseline: [0-9a-f]{40}$/ {print $3; n++} END {exit n == 1 ? 0 : 1}' "$BRIEF_REL"
}
ledger_blob() {
  local label="$1"
  awk -v label="$label" '$0 ~ "^" label ": [0-9a-f]{64}$" {print $NF; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
ledger_reconcile() {
  awk '/^Reconcile successor plan SHA: [0-9a-f]{40}$/ {print $5; n++} END {exit n == 1 ? 0 : 1}' "$LEDGER_REL"
}
exact_paths() {
  [[ "$(git diff-tree --no-commit-id --name-only --no-renames -r "$1" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
reconcile_successor_valid() {
  local successor="$1"
  [[ "$successor" =~ '^[0-9a-f]{40}$' && "$(brief_successor)" == "$successor" && "$(ledger_reconcile)" == "$successor" ]] || return 1
  [[ "$(git rev-parse "$successor^")" == "$RECONCILE_SUCCESSOR_PARENT" ]] || return 1
  exact_paths "$successor" || return 1
  [[ "$(git show "$successor:$PLAN_REL" | sha_bytes)" == "$(ledger_blob 'Amended library plan blob SHA-256')" ]] || return 1
  [[ "$(git show "$successor:$POINTER_REL" | sha_bytes)" == "$(ledger_blob 'Amended pointer plan blob SHA-256')" ]] || return 1
  [[ "$(sha256 "$REPORT_REL")" == "$REPORT_SHA" ]]
}
precommit_gate() {
  [[ "$(git rev-parse HEAD)" == "$RECONCILE_SUCCESSOR_PARENT" && -z "$(git diff --cached --name-only)" ]] || return 1
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]]
}
rebind_reconcile_successor() {
  local successor="$1" plan_blob pointer_blob
  plan_blob="$(git show "$successor:$PLAN_REL" | sha_bytes)"
  pointer_blob="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  perl -0pi -e "s/^Planning baseline: .*/Planning baseline: $successor/m" "$BRIEF_REL"
  perl -0pi -e "s/^Amended library plan blob SHA-256: .*/Amended library plan blob SHA-256: $plan_blob/m; s/^Amended pointer plan blob SHA-256: .*/Amended pointer plan blob SHA-256: $pointer_blob/m" "$LEDGER_REL"
  grep -q '^Reconcile successor plan SHA:' "$LEDGER_REL" && die 'reconcile successor SHA already rebound'
  print -r -- "Reconcile successor plan SHA: $successor" >> "$LEDGER_REL"
}
reconcile_dispatch() {
  local parent successor
  precommit_gate || die 'pre-commit parent/index/two-plan gate failed'
  parent="$(git rev-parse HEAD)"
  git add -- "$PLAN_REL" "$POINTER_REL"
  [[ "$(git diff --cached --name-only | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'index path set is not exact'
  git commit -m 'docs: reconcile library extraction baseline arithmetic' >/dev/null
  successor="$(git rev-parse HEAD)"
  [[ "$(git rev-parse "$successor^")" == "$parent" && "$parent" == "$RECONCILE_SUCCESSOR_PARENT" ]] || die 'successor parent mismatch'
  exact_paths "$successor" || die 'successor path set mismatch'
  rebind_reconcile_successor "$successor"
  reconcile_successor_valid "$successor" || die 'rebound reconcile successor authority mismatch'
}
expect_fail() { local rc; setopt noerrexit; ( "$@" ) >/dev/null 2>&1; rc=$?; setopt errexit; (( rc != 0 )); }
reconcile_matrix() (
  emulate -L zsh; setopt errexit nounset pipefail
  local root repo successor applied_plan applied_pointer primary_script correction_output count=0 wrong_parent wrong_sha extra_sha missing_sha host_root="$PWD" host_index host_status host_nonplan host_report
  host_index="$(sha256 "$(git rev-parse --git-path index)")"; host_status="$(git status --porcelain=v1 -z | sha_bytes)"; host_nonplan="$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)"; host_report="$(sha256 "$REPORT_REL")"
  root="$(mktemp -d "${TMPDIR:-/tmp}/task-6.1-reconcile-matrix.XXXXXX")"; trap 'rm -rf -- "$root"' EXIT
  repo="$root/repo"; git clone -q --no-local "$PWD" "$repo"; git -C "$repo" checkout -q --detach "$RECONCILE_SUCCESSOR_PARENT"; git -C "$repo" config user.email fixture@example.invalid; git -C "$repo" config user.name fixture
  cp -- "$BRIEF_REL" "$root/brief"; cp -- "$LEDGER_REL" "$root/ledger"; cp -- "$REPORT_REL" "$root/report"
  cd "$repo"; cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; cp -- "$root/report" "$REPORT_REL"
  git -C "$host_root" diff --binary --no-renames -- "$PLAN_REL" "$POINTER_REL" > "$root/current-plan.patch"
  [[ -s "$root/current-plan.patch" ]] || die 'current two-plan successor payload is empty'
  git apply --whitespace=nowarn "$root/current-plan.patch"
  [[ "$(git diff --name-only -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'current two-plan successor payload is not exact'
  reconcile_dispatch; successor="$(git rev-parse HEAD)"; reconcile_successor_valid "$successor"; ((++count))
  primary_script="$root/primary.zsh"; awk '/<!-- TASK-6.1-MANIFEST-PARSER:START -->/{m=1;next}m&&/^```zsh$/{c=1;next}m&&c&&/^```$/{exit}c{print}' "$PLAN_REL" > "$primary_script"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; ((++count)); cp -- "$LEDGER_REL" "$root/rebound-ledger"; for row in missing malformed wrong duplicate; do cp -- "$root/rebound-ledger" "$LEDGER_REL"; case "$row" in missing) perl -0pi -e 's/^Reconcile successor plan SHA: .*\n//m' "$LEDGER_REL";; malformed) perl -0pi -e 's/^Reconcile successor plan SHA: .*/Reconcile successor plan SHA: malformed/m' "$LEDGER_REL";; wrong) perl -0pi -e 's/^Reconcile successor plan SHA: .*/Reconcile successor plan SHA: 0000000000000000000000000000000000000000/m' "$LEDGER_REL";; duplicate) print -r -- "Reconcile successor plan SHA: $successor" >> "$LEDGER_REL";; esac; expect_fail zsh "$primary_script" rebound-inventory "$successor" || die "reconcile successor rebound row accepted: $row"; ((++count)); done; cp -- "$root/rebound-ledger" "$LEDGER_REL"; zsh "$primary_script" rebound-inventory "$successor" >/dev/null; correction_output="$(zsh "$primary_script" successor-correction-pre-fixture "$successor")"; [[ "$correction_output" == 'reconcile_successor_correction_fixture=PASS controls=25' ]] || die 'reconcile successor production correction-pre probe mismatch'; ((++count))
  git diff --binary --no-renames "$RECONCILE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" > "$root/committed.patch"
  [[ -s "$root/committed.patch" ]] || die 'committed delta is empty'
  [[ "$(git diff --name-only "$RECONCILE_SUCCESSOR_PARENT" "$successor" -- "$PLAN_REL" "$POINTER_REL" | LC_ALL=C sort)" == "$PLAN_PATHS" ]] || die 'committed delta paths are not exact'
  applied_plan="$(git show "$successor:$PLAN_REL" | sha_bytes)"; applied_pointer="$(git show "$successor:$POINTER_REL" | sha_bytes)"
  git clone -q --no-local "$repo" "$root/apply"; git -C "$root/apply" checkout -q --detach "$RECONCILE_SUCCESSOR_PARENT"; git -C "$root/apply" apply --whitespace=nowarn "$root/committed.patch"
  [[ "$(sha256 "$root/apply/$PLAN_REL")" == "$applied_plan" && "$(sha256 "$root/apply/$POINTER_REL")" == "$applied_pointer" ]] || die 'applied blobs differ from committed successor'; ((++count))
  wrong_sha=0000000000000000000000000000000000000000; expect_fail reconcile_successor_valid "$wrong_sha" || die 'wrong SHA accepted'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; wrong_parent="$(git commit-tree "$(git rev-parse "$successor^{tree}")" -p "$(git rev-parse "$RECONCILE_SUCCESSOR_PARENT^")" -m wrong-parent)"; rebind_reconcile_successor "$wrong_parent"; expect_fail reconcile_successor_valid "$wrong_parent" || die 'wrong parent passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$RECONCILE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL" "$POINTER_REL"; print -r -- extra > docs/superpowers/plans/extra-reconcile.md; git add -- "$PLAN_REL" "$POINTER_REL" docs/superpowers/plans/extra-reconcile.md; git commit -qm extra; extra_sha="$(git rev-parse HEAD)"; rebind_reconcile_successor "$extra_sha"; expect_fail reconcile_successor_valid "$extra_sha" || die 'extra path passed the real gate'; ((++count))
  cp -- "$root/brief" "$BRIEF_REL"; cp -- "$root/ledger" "$LEDGER_REL"; git checkout -q --detach "$RECONCILE_SUCCESSOR_PARENT"; git checkout "$successor" -- "$PLAN_REL"; git add -- "$PLAN_REL"; git commit -qm missing; missing_sha="$(git rev-parse HEAD)"; rebind_reconcile_successor "$missing_sha"; expect_fail reconcile_successor_valid "$missing_sha" || die 'missing path passed the real gate'; ((++count))
  git diff --cached --quiet; cd "$host_root"; [[ "$(sha256 "$(git rev-parse --git-path index)")" == "$host_index" && "$(git status --porcelain=v1 -z | sha_bytes)" == "$host_status" && "$(git diff --binary -- . ':(exclude)'"$PLAN_REL" ':(exclude)'"$POINTER_REL" | sha_bytes)" == "$host_nonplan" && "$(sha256 "$REPORT_REL")" == "$host_report" ]] || die 'host preservation mismatch'; cd /; rm -rf -- "$root"; trap - EXIT
  print -r -- "reconcile_matrix=PASS controls=$count recovery=not-run"
)
case "${1:-reconcile-matrix}" in
  reconcile-planning-amendment) (( $# == 1 )) || die 'usage: reconcile-planning-amendment'; reconcile_dispatch;;
  reconcile-matrix) (( $# == 1 )) || die 'usage: reconcile-matrix'; reconcile_matrix;;
  *) die 'usage: reconcile successor authority {reconcile-matrix|reconcile-planning-amendment}';;
esac
```
<!-- TASK-6.1-RED-RECOVERY-RECONCILE-SUCCESSOR:END -->


## Task 6.1 Closeout

Implementation commit `741f5eb` over correction `c48f11d` on baseline `1c7ad37`.
