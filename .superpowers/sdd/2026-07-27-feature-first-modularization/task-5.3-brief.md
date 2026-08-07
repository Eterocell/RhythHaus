## Task 5.3: Move Search

## Final Lifecycle Status - 2026-08-07

This brief's planning baseline remains historical: `f947724a9a2a29e5863976cb2c17fc16225bd336`.
The atomic implementation now exists at
`90e330d24b10b9668263002b9cc37945d24e9643` (`refactor: extract search feature`), directly
following that planning commit. The final behavior/spec/quality and exact 20-path scope
reviews are `PASS / APPROVED`. The final acceptance report records the complete automated
matrix and limitations. This separate evidence closeout remains uncommitted; no evidence
closeout SHA is asserted. Historical start-gate, RED, checkpoint, and continuation records
below are retained as evidence and are not current task status.

**Bound planning commit:** `f947724a9a2a29e5863976cb2c17fc16225bd336`.
Resume the exact frozen 16-endpoint checkpoint from this `HEAD`; the one-time continuation gate
below must pass before any further implementation write. Historical planning commits remain
evidence for the accepted absent-module RED and partial checkpoint only.

**Route, baseline, and approval gate.** Route: OpenSpec + Superpowers; execution route: SDD.
The approved production/design baseline is `0066c409d0fcc039aa3657e3e5efca2d223ee78f`.
Production is frozen until this replacement plan is approved. Task 5.3 is one atomic
implementation/review/commit deliverable followed by a separate evidence closeout commit; there
are no interim commits. The only implementation commit subject is exactly
`refactor: extract search feature`. Authority order is
`docs/superpowers/specs/2026-08-07-search-feature-extraction-design.md`, canonical design and
OpenSpec at the baseline, then this plan, `docs/architecture.md`, and ADR 0001.

**Goal.** Extract the Search leaf into exactly one unexported Android-KMP Compose implementation
module, `:feature:search`, without changing Search behavior or Shared policy ownership.

**Architecture and tech stack.** The graph is exactly
`:shared -> :feature:search -> :feature:library:api` and `:feature:search -> :core:ui`.
Use Kotlin Multiplatform, Compose Multiplatform resources/compiler, existing KSP architecture
processor/TestKit fixtures, Kotlin test, Compose JVM UI test, Detekt, Spotless, Gradle, and the
existing Android/JVM/iOS target conventions. Shared remains the sole facade, iOS framework, shell,
route/Back arbiter, playback/selection/scroll owner, and Koin assembler.

**Global constraints.** Do not create a README, Search API split, Koin module/registration,
platform-specific production source file, iOS framework export, empty state/presenter/event/effect abstraction,
`:core:navigation`, `:core:playback` dependency, Shared reverse edge, or compatibility
`SearchScreen`. Do not change core playback, playback controller/state, repositories, platform
types, TagLib ownership, SQLDelight/schema/migration, Swift/app entry, `LibraryNavigation`,
`TrackSelectionState`, `LibraryPlaybackSelection`, `LibraryRows`, or `LibraryAppShell` except its
listed call-site adaptation. Do not add Shared/generated `Res` handles, playback controller/state,
repository, queue, Koin, or platform types to the feature public boundary. Runtime/device/visual/
accessibility/playback-engine claims remain open.

### Exact Public Boundary And Ownership

- [ ] Expose exactly two public declarations in package `com.eterocell.rhythhaus.search`; every
  public declaration, constructor property, parameter, and public closure has the exact KDoc below
  or declaration-specific equivalent with no omitted behavior. All other feature declarations are
  `internal` or `private`. Preserve this signature exactly, including
  `selectTrackLabel: @Composable (String) -> String`; only layout defaults are permitted.

  ```kotlin
  /**
   * Shared-owned wording consumed by [SearchContent]. Value equality makes unchanged
   * labels stable across recomposition; callers provide already-localized text.
   *
   * @property title Search route title.
   * @property clear Label for the query-clear action.
   * @property nowPlaying Accessibility state for the current result.
   */
  public data class SearchSharedLabels(
      /** Search route title resolved by Shared. */
      public val title: String,
      /** Query-clear action label resolved by Shared. */
      public val clear: String,
      /** Current-result accessibility state resolved by Shared. */
      public val nowPlaying: String,
  )

  /**
   * Renders and locally controls Search over [libraryTracks], delegating application
   * policy through callbacks. It does not own navigation, playback, shared selection
   * state, scroll storage, or bottom-bar policy.
   *
   * @param libraryTracks Tracks searched in their supplied order.
   * @param currentTrackId Current playback track ID, or null when no track is current.
   * @param isPlaying Whether the current track is actively playing.
   * @param labels Shared-owned localized Search title, clear, and Now Playing labels.
   * @param selectTrackLabel Composably resolves the localized long-press/content description for a
   * title using Shared's structured `stringResource(select_track_format, title)` when Search
   * composes a row; no generated resource handle crosses the boundary.
   * @param selectionModeActive Whether Search rows currently select rather than play.
   * @param selectedTrackIds Immutable selected IDs effective for the Search page.
   * @param onStartSelection Requests Search selection beginning with the given track ID.
   * @param onToggleSelection Requests one toggle of the given Search track ID.
   * @param onVisibleTrackIdsChanged Receives filtered IDs whenever their sequence changes.
   * @param onScrollPositionChanged Receives first visible item index and pixel offset.
   * @param onPlayTrack Requests playback of ordered filtered results at the selected result.
   * @param onDismiss Requests Shared route dismissal.
   * @param playingIndicator Composes Shared-owned current-playing indication in a playing row.
   * @param bottomContentPadding Reserved trailing list space for Shared shell chrome.
   * @param modifier Modifier applied to the Search root.
   */
  @Composable
  public fun SearchContent(
      libraryTracks: List<LibraryTrack>,
      currentTrackId: String?,
      isPlaying: Boolean,
      labels: SearchSharedLabels,
      selectTrackLabel: @Composable (String) -> String,
      selectionModeActive: Boolean,
      selectedTrackIds: Set<String>,
      onStartSelection: (String) -> Unit,
      onToggleSelection: (String) -> Unit,
      onVisibleTrackIdsChanged: (List<String>) -> Unit,
      onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
      onPlayTrack: (orderedResults: List<LibraryTrack>, selectedTrack: LibraryTrack) -> Unit,
      onDismiss: () -> Unit,
      playingIndicator: @Composable () -> Unit,
      bottomContentPadding: Dp = 0.dp,
      modifier: Modifier = Modifier,
  )
  ```

- [ ] Feature owns query, case-insensitive title/artist/album filtering, input order, focus,
  result count/no-match presentation, list rendering, and row interaction. Blank/whitespace input
  yields no results; empty metadata and duplicate IDs are retained. Import
  `androidx.compose.foundation.lazy.itemsIndexed` and render exactly
  `itemsIndexed(filtered, key = { occurrenceIndex, track -> searchOccurrenceKey(occurrenceIndex, track.id) })`.
  Define `private fun searchOccurrenceKey(index: Int, trackId: String): String = "$index\u0000$trackId"`.
  The index prefix plus NUL separator makes distinct filtered occurrences unique, the resulting
  String is Bundle-saveable, and the key remains rendering-only: it preserves visible duplicate IDs,
  playback order, selection IDs, and independent duplicate render/activation after unrelated
  recomposition. Do not use a custom data-class key, `track.id` alone, or a two-argument lambda with
  `items`.
- [ ] Define one `internal` pure filtering function in the feature source and make production
  `SearchContent` call it. It accepts `libraryTracks` and query text, returns empty for blank or
  whitespace-only input, otherwise filters title/artist/album ignoring case in supplied order, and
  preserves duplicate IDs and nullable/empty metadata. It is not a new public declaration.
- [ ] Shared owns route/Back, effective selection and `ReconcileVisible(Search, ids)`, scroll
  storage/policy, current playback decision, queue/restart, error policy, dismissal policy, and
  `EqualizerStrip`. `LibraryRoutes.kt` composes `SearchContent` directly. Its internal Search
  adapter maps primitive scroll values to `LibraryScrollPosition`, maps visible IDs to the existing
  Search reconciliation action, resolves `SearchSharedLabels`, and supplies
  `selectTrackLabel = { title -> stringResource(Res.string.select_track_format, title) }` during
  row composition. It supplies `playingIndicator = { EqualizerStrip(active = true) }` only through
  the slot and uses `onDismiss` for the top app bar.
- [ ] Adapt Shared selection exactly in the `SearchContent` call:
  ```kotlin
  selectionModeActive =
      trackSelectionState.pageKey == TrackSelectionPageKey.Search &&
          trackSelectionState.selectedTrackIds.isNotEmpty()
  selectedTrackIds =
      if (trackSelectionState.pageKey == TrackSelectionPageKey.Search) {
          trackSelectionState.selectedTrackIds
      } else {
          emptySet()
      }
  onStartSelection = { id ->
      onTrackSelectionAction(TrackSelectionAction.Start(TrackSelectionPageKey.Search, id))
  }
  onToggleSelection = { id ->
      onTrackSelectionAction(TrackSelectionAction.Toggle(TrackSelectionPageKey.Search, id))
  }
  onVisibleTrackIdsChanged = { ids ->
      onTrackSelectionAction(
          TrackSelectionAction.ReconcileVisible(TrackSelectionPageKey.Search, ids),
      )
  }
  ```
- [ ] Define this internal production helper in `LibraryRoutes.kt`; the `SearchContent`
  `onPlayTrack` callback calls it directly. Do not add a test hook, injected selector, or public
  abstraction. Search invokes only `onPlayTrack`; it never dismisses itself after play.
  ```kotlin
  internal fun playSearchTrack(
      playbackController: PlaybackController,
      orderedResults: List<LibraryTrack>,
      selectedTrack: LibraryTrack,
      onDismiss: () -> Unit,
  ) {
      selectLibraryTrackForPlayback(
          playbackController = playbackController,
          visibleQueue = orderedResults.map(LibraryTrack::toPlayableTrack),
          selectedTrackId = selectedTrack.id,
      )
      onDismiss()
  }
  ```
  It maps ordered results, calls the real selector, and only then dismisses. A synchronous exception
  propagates unchanged and does not dismiss.

### Build, Resources, And Closed File Manifest

- [ ] Register `include(":feature:search")`. Create `feature/search/build.gradle.kts` using exactly
  `build-logic.kmp.feature.impl`, `build-logic.android.kmp.library`,
  `build-logic.compose-resources`, and `alias(libs.plugins.compose.compiler)`. Configure
  `ControlledComposeResourcesExtension` namespace
  `rhythhaus.feature.search.generated.resources`; configure Android namespace
  `com.eterocell.rhythhaus.search`, catalog compile/min SDK, JVM 11, `withHostTest {}`,
  `androidResources { enable = true }`, `jvm()`, `iosArm64()`, and `iosSimulatorArm64()`. No
  feature framework/export or production platform source exists.
- [ ] Use this exact feature skeleton, retaining the existing catalog SDK accessors and imports for
  `ControlledComposeResourcesExtension` and `JvmTarget`:
  ```kotlin
  plugins {
      id("build-logic.kmp.feature.impl")
      id("build-logic.android.kmp.library")
      id("build-logic.compose-resources")
      alias(libs.plugins.compose.compiler)
  }

  extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") {
      namespace("rhythhaus.feature.search.generated.resources")
  }

  kotlin {
      android {
          namespace = "com.eterocell.rhythhaus.search"
          compileSdk = libs.versions.android.compileSdk.get().toInt()
          minSdk = libs.versions.android.minSdk.get().toInt()
          compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
          withHostTest {}
          androidResources { enable = true }
      }
      jvm()
      iosArm64()
      iosSimulatorArm64()
  }
  ```
- [ ] Configure source-set dependency scopes exactly as follows. Foundation, Compose resources,
  core UI, and Miuix remain `implementation`; no `api` dependency for any of those types is
  permitted.
  ```kotlin
  kotlin {
      sourceSets {
          commonMain.dependencies {
              api(projects.feature.library.api)
              api(libs.compose.runtime)
              api(libs.compose.ui)
              implementation(projects.core.ui)
              implementation(libs.compose.foundation)
              implementation(libs.compose.components.resources)
              implementation(libs.miuix.ui)
          }
          commonTest.dependencies {
              implementation(libs.kotlin.test)
          }
          jvmTest.dependencies {
              implementation("org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}")
              implementation(compose.desktop.currentOs)
          }
      }
  }
  ```
  Shared adds exactly
  `implementation(projects.feature.search)`, never `api` or export. Test task discovery must show
  `jvmTest`, `testAndroidHostTest`, `iosSimulatorArm64Test`, Compose resource, and KSP tasks. The
  feature convention registers exactly `kspAndroid`, `kspJvm`, `kspIosArm64`, and
  `kspIosSimulatorArm64` against `:architecture-processor`; fixtures assert these four registrations
  and their `kspAndroidMain`, `kspKotlinJvm`, `kspKotlinIosArm64`, and
  `kspKotlinIosSimulatorArm64` tasks.
- [ ] Move exactly `search_placeholder`, `search_results_count_zero`,
  `search_results_count_one`, `search_results_count_many`, and
  `search_no_tracks_match_format` in both EN/ZH XML files to Search. Search resolves those through
  feature `Res`. Shared retains `search`, `clear`, `now_playing_badge`, and
  `select_track_format`; it injects title/clear/Now Playing text and structured formatting through
  the composable callback. Use existing repository XML/import-audit test pattern, with five
  distinct failures: missing moved key, duplicate key, wrong owner, wrong namespace, and foreign
  generated-resource import.
- [ ] Add configuration-aware Search governance without changing any other module-edge policy.
  `ArchitectureAllowList.isAllowed` (or an equivalently narrow policy API) receives the actual
  configuration and preserves every existing module-edge rule. Only
  `:shared -> :feature:search` is allowed for declared production `commonMainImplementation`; it
  rejects `commonMainApi` and `api` with the deterministic diagnostic
  `ARCH-EDGE :shared [commonMainApi] -> :feature:search`. Do not globally reinterpret existing
  edges or configurations. `ArchitectureCheckTask` passes the edge configuration into that policy.
- [ ] Add Search-specific expected namespaces through `ArchitectureAllowList` and validate their
  records in `ArchitectureCheckTask`: Android namespace
  `com.eterocell.rhythhaus.search` and Compose namespace
  `rhythhaus.feature.search.generated.resources`. Modules with no Search-specific expected
  namespace retain current behavior. Wrong Android and wrong Compose namespace each emit a
  deterministic executable `ARCH-RESOURCE` diagnostic; existing blank/invalid validation remains.
  This adds no project/module dependency or public app API.
- [ ] The implementation manifest is exactly 20 unique endpoints, counting both sides of every
  move. Create/move destinations (8):
  `feature/search/build.gradle.kts`;
  `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`;
  `feature/search/src/commonMain/composeResources/values/strings.xml`;
  `feature/search/src/commonMain/composeResources/values-zh/strings.xml`;
  `feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt`;
  `feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt`;
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt`;
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt`.
  Modified (10): `settings.gradle.kts`; `shared/build.gradle.kts`;
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`;
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`;
  shared EN/ZH `strings.xml`; `ArchitectureAllowList.kt`; `ArchitectureCheckTask.kt` at
  `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt`;
  `ArchitectureCheckPluginFunctionalTest.kt`; and
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt`.
  Removed move sources (2): shared `SearchScreen.kt` and shared
  `SearchSelectionPoliciesJvmTest.kt` at their current paths.
- [ ] Retain unchanged: `LibraryNavigation.kt`, `TrackSelectionState`,
  `LibraryPlaybackSelection.kt`, `LibraryRows.kt`, core playback, all SQLDelight/schema/migration
  paths, Swift/app entries, iOS exports, generated sources/build outputs, and all unlisted tests.
  The implementation excludes the canonical plan, OpenSpec, root `progress.md`, and `roadmap.md`;
  the ignored SDD brief/report/final-report files are outside the implementation snapshot. The
  controller-owned SDD progress ledger is the sole exception: it remains a tracked unstaged ` M`
  path until closeout. Generated/non-source outputs are excluded; the evidence closeout excludes all
  20 implementation endpoints.

### Dependency-Ordered TDD And Test Ledger

- [ ] RED 1 before registration: run
  `./gradlew :feature:search:jvmTest --configuration-cache --configuration-cache-problems=fail`.
  Accept Gradle 9.6.1's actual absent-project/task-selection wording only when it proves
  `:feature:search` is absent and no requested feature task or compilation ran. Record the command,
  exit status, and exact causal diagnostic block verbatim in `task-5.3-report.md`; retain the full
  raw log separately if needed. If wording differs, reconcile the report against the causal
  missing-project/no-feature-task expectation rather than rewriting the command.
  Then register the module and run
  `./gradlew :feature:search:tasks --all --configuration-cache --configuration-cache-problems=fail`.
- [ ] RED architecture controls before relocation, one fixture mutation per run, using the exact
  selector pattern
  `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeature...' --rerun-tasks --no-configuration-cache --no-parallel`.
  Add recommended selectors `searchFeatureConventionPublishesRootsAndKspRegistrations`,
  `searchFeatureRejectsForbiddenEdgesAndSharedExposure`,
  `searchFeatureRejectsWrongPackageNamespaceKoinAndIosExport`, and
  `searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports`,
  `searchFeatureRejectsSharedCommonMainApiExposure`, and
  `searchFeatureRejectsWrongExpectedNamespaces`. Because the partial fixture/allow-list exists,
  first add focused failing regressions against the current production checker for
  `commonMainApi` and each Search namespace mismatch, run the focused RED selectors, then implement
  the production checker changes and GREEN them. Build the external
  processor first with `./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel`, pass its JAR through the
  established property, and require nested TestKit KSP tasks to be `SUCCESS` or `FAILED`, never
  `UP-TO-DATE`, `NO-SOURCE`, or skipped. Every outer and nested TestKit invocation passes
  `--rerun-tasks`. Diagnostics/controls cover every forbidden feature edge
  (Shared, core playback/database/platform, taglib, another implementation, app), Shared `api` or
  export, wrong package, Android namespace, resource namespace, missing/duplicate/wrong-owner
  resources, foreign generated import, Koin, empty roots, and missing KDoc/public closure.
- [ ] GREEN architecture registration updates the allow-list to permit only Library API and core UI
  for Search and Shared-to-Search composition, publishes Search package/resources/KSP registrations,
  and preserves all negative controls. The real KMP Shared fixture uses
  `commonMainImplementation(project(":feature:search"))` for GREEN, then mutates only that
  declaration to `commonMainApi(project(":feature:search"))` for RED; it must run through
  `architectureCheck`, never synthetic `architecture` configuration. Run each selector with
  `--rerun-tasks`; only then run the full architecture TestKit class with the external processor
  JAR.
- [ ] In the positive Search fixture, publish the externally built processor JAR as the
  `:architecture-processor` project artifact; remove independent per-configuration file-JAR
  additions. Let only `build-logic.kmp.feature.impl` register it. Add
  `verifySearchFeatureConvention` using the existing registry/KSP-option pattern and assert exactly
  `KSP_PACKAGE_ROOTS=com.eterocell.rhythhaus.search` plus exactly these four sorted records:
  ```text
  KSP_REGISTRATION=:feature:search|kspAndroid|:architecture-processor
  KSP_REGISTRATION=:feature:search|kspIosArm64|:architecture-processor
  KSP_REGISTRATION=:feature:search|kspIosSimulatorArm64|:architecture-processor
  KSP_REGISTRATION=:feature:search|kspJvm|:architecture-processor
  ```
  It independently verifies all three facts: those four exact registry records; each real
  `kspAndroid`, `kspJvm`, `kspIosArm64`, and `kspIosSimulatorArm64` configuration has exactly one
  direct `ProjectDependency` with path `:architecture-processor` and no file-dependency substitute;
  and KSP argument `architecture.packageRoots` equals exactly
  `com.eterocell.rhythhaus.search`. After convention configuration, the negative fixture removes
  only the direct `ProjectDependency(:architecture-processor)` from `kspJvm` dependencies while
  retaining the registry record and all targets, then runs `verifySearchFeatureConvention`; it must
  fail deterministically with a registry/configuration mismatch. This is fixture configuration
  dependency removal, not a production convention edit. Task outcome alone is not processor proof:
  add one malformed common source and separately run `kspAndroidMain`, `kspKotlinJvm`,
  `kspKotlinIosArm64`, and `kspKotlinIosSimulatorArm64`; every task must be `FAILED` with the exact
  expected repository-built-processor `ARCH-PACKAGE` or `ARCH-KDOC` diagnostic. Restore valid
  source and require all four tasks `SUCCESS`. The external JAR remains only the
  `:architecture-processor` project artifact; never add `files(processorJar)` directly to KSP
  configurations.
- [ ] Use actual Gradle/KSP diagnostics for one-variable Search mutations: wrong Android namespace,
  empty configured package roots, undocumented public member, and undocumented public constructor
  property. Top-level missing KDoc alone is insufficient. Retain controls for wrong package, iOS
  export, Compose namespace, and existing public closure.
- [ ] Define one reusable production-source Koin audit over real non-test Search production roots.
  Its positive audit runs against the repository Search roots. A copied/mutated fixture adds one
  `org.koin` import and fails through this same audit function and diagnostic; do not use a
  self-thrown/caught assertion or require KSP to diagnose Koin.
- [ ] Use the existing XML/import audit code path with lists/multisets, never sets. Search EN and ZH
  each contain exactly the five moved keys with no extra or duplicate declaration and have locale
  parity; Shared contains none of those keys and retains the four Shared-owned keys. Add one-variable
  controls for same-owner duplicate in Search EN, same-owner duplicate in Search ZH, extra Search
  key, missing moved key, cross-owner duplicate, wrong owner, wrong namespace, and both foreign
  generated imports. Preserve the five original failure categories while making same-owner/extra
  exactness executable.
- [ ] Write behavior RED tests before moving production code. Feature
  `SearchFilterTest` in `commonTest` owns exactly `blankAndWhitespaceQueriesHaveNoResults`,
  `caseInsensitiveTitleArtistAndAlbumFilteringPreservesInputOrder`, and
  `duplicateIdsAndEmptyMetadataArePreserved`. These execute through the production internal filter
  on JVM, Android host, and iOS simulator; feature JVM/Android-host/iOS-simulator XML must each
  report a positive `SearchFilterTest` count, and none of those target test tasks is accepted with
  zero `SearchFilterTest` cases.
  `SearchSelectionPoliciesJvmTest` retains intentional production-composable duplication at the
  public rendering/callback boundary and owns exactly the four migrated methods
  `normalClickPlaysOnlyOutsideSelection`, `longClickStartsSelectionWithoutPlayback`,
  `selectionModeRowAndCheckboxEachToggleExactlyOnceWithoutPlayback`, and
  `changingFilteredIdsDispatchesEachSearchReconciliation`, plus named production-composable tests
  for `blankQueryHasNoResults`, `filtersTitleArtistAndAlbumIgnoringCase`,
  `resultCountsAndNoMatchTextUseFeatureResources`, `requestsFocusOnce`, `clearResetsQuery`,
  `reportsPrimitiveScrollAndBottomPadding`, `currentIndicatorAndNowPlayingSemanticsAreScoped`,
  `selectionAndVisibleSequenceUseProductionContent`, `emptyMetadataIsRetained`, and
  `duplicateOccurrencesRenderAndActivateDistinctlyAcrossUnrelatedRecomposition`.
  The duplicate-occurrence test remounts after unrelated recomposition and asserts distinct row
  activation/click payloads while visible and playback callbacks retain duplicate order. These mount
  `SearchContent`/production rows, not DTO-only helpers.
- [ ] Move only the Home method
  `leavingHomeSongsForAlbumsOrArtistsClearsSelectionExactlyOnce` into Shared
  `HomeSelectionPoliciesJvmTest`; it owns Home clear behavior only. Add Shared
  `SearchRouteAdapterJvmTest` production composition/adapter methods
  `orderedQueueAndSelectedTrackUseRealPlaybackSelection`,
  `currentTrackRestartsBeforeDismissal`, `dismissesOnlyAfterSuccessfulSelection`,
  `sentinelFailurePropagatesAndDoesNotDismiss`,
  `sharedLabelsUseStructuredFormatting`, `equalizerSlotIsSharedOwned`, and
  `adaptsSelectionAndScrollFromProductionSearchContent`. Mount production `SearchContent` through
  the real adapter: a Search page with nonempty selected IDs supplies
  `selectionModeActive = true` and that exact selected-ID set; a non-Search page supplies false
  and `emptySet()`. A duplicate visible-ID list emits exactly one ordered duplicate-preserving
  `TrackSelectionAction.ReconcileVisible(TrackSelectionPageKey.Search, ids)`, while unrelated
  recomposition with the same sequence emits no second action. Assert `Start`, `Toggle`,
  `ReconcileVisible`, and primitive scroll mappings each exactly through production callback wiring.
  Successful queue/restart coverage uses the real
  `PlaybackController` and `selectLibraryTrackForPlayback`. For
  `sentinelFailurePropagatesAndDoesNotDismiss`, define exactly:
  ```kotlin
  val sentinel = IllegalStateException("search mapping sentinel")
  val failingOrderedResults = object : AbstractList<LibraryTrack>() {
      override val size: Int = 1
      override fun get(index: Int): LibraryTrack {
          check(index == 0)
          throw sentinel
      }
  }
  ```
  Use a normal separate `selectedTrack`, call real internal `playSearchTrack`, assert the thrown
  object is reference-identical, and assert dismiss count is zero. Mapping therefore fails
  synchronously without subclassing `PlaybackController` or injecting a test seam. Existing Shared
  route/Back/selection tests remain controls. No test manufactures a feature playback controller or
  repository.
- [ ] GREEN relocates Search source/resources and the four Search tests, removes both old Shared
  move sources, implements the private occurrence key, and adapts the direct route call. Run
  feature focused tests, then Shared adapter/Home controls, then full feature/Shared target suites.

### Verification, Review, And Commit Boundaries

- [ ] Run exactly these acceptance commands serially (`--no-parallel`) after the final code change:
  ```bash
  ./gradlew :feature:search:jvmTest --tests '*SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :shared:jvmTest --tests '*HomeSelectionPoliciesJvmTest' --tests '*SearchRouteAdapterJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :feature:search:compileAndroidMain :feature:search:compileKotlinIosArm64 :feature:search:compileKotlinIosSimulatorArm64 :feature:search:jvmTest :feature:search:testAndroidHostTest :feature:search:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
  ./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
  ./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew spotlessApply --configuration-cache --no-parallel
  ./gradlew spotlessCheck --configuration-cache --no-parallel
  ./gradlew detekt --configuration-cache --no-parallel
  PATH="$HOME/.nvm/versions/node/v26.7.0/bin:$PATH" openspec validate feature-first-modularization --strict
  /usr/bin/xcrun xcodebuild -version
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Debug CODE_SIGNING_ALLOWED=NO build
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' test
  ./init.sh
  git diff --check
  ```
  Run Xcode generic unsigned build/iPhone 17 tests when Xcode and that simulator are available;
  record unavailability as a blocker. Do not substitute `compileKotlinAndroid`, `allTests`, or a
  full OpenSpec sweep for named acceptance commands. Require second `architectureCheck` cache reuse.
  Record XML per target/class as tests/skipped/failures/errors, never actionable task counts. Record
  positive `SearchFilterTest` counts separately for feature JVM, Android host, and iOS simulator,
  and record JVM UI-class counts for `SearchSelectionPoliciesJvmTest` separately.
- [ ] After the plan-only amendment commit and brief rebind, resume the existing partial checkpoint
  from that new `HEAD`; do not require or create a pristine implementation start. This one-time resume
  gate supersedes the clean-start gate only for the retained checkpoint. The rebound planning SHA
  must equal `HEAD`; the index must be empty; and the sole controller-owned tracked evidence change
  must be `.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md` with porcelain
  ` M`. The approved design SHA remains historical authority only, not a worktree diff base. The
  retained absent-module RED and historical 75-test architecture checkpoint remain valid evidence.
  This documentation-only amendment must preserve all frozen checkpoint blobs byte-identically; the
  independent reviewer verifies that preservation. No implementation commit may exist between the
  rebound planning commit and the one atomic implementation commit.
- [ ] Run this one-time NUL-safe continuation gate immediately after rebind. It accepts only the
  exact frozen 16 implementation records below plus the one ` M` SDD ledger; it rejects another
  status, hash, path, or untracked file. `D` records must be absent on disk; their identity is
  established by the rebound `HEAD`. The committed continuation gate always rejects the canonical
  plan path.
  ```bash
  task_5_3_frozen_records() {
      printf '%s\0' \
          ' M|build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt|4b141d0e78670aafa0d1ef0e402b8afcaad69b43' \
          ' M|build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt|732d6045395d3cd3f5e0b2f11f5d42292c6fa24d' \
          ' M|settings.gradle.kts|d68aa9c12a653714771bbc3d9a7640c8782f6606' \
          ' M|shared/build.gradle.kts|fd23a96b8e9b9b910c1c9b49198b54e25348585d' \
          ' M|shared/src/commonMain/composeResources/values-zh/strings.xml|a29f6c56d5fdf528937b9920709b5c4b3571009c' \
          ' M|shared/src/commonMain/composeResources/values/strings.xml|8ce83312b7e5661b1db27552b9ee7095026a2ec5' \
          ' M|shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt|8c76cf9580c23c4b70b2f1da40b0bda20d963049' \
          ' D|shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt|DELETED' \
          ' D|shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt|DELETED' \
          '??|feature/search/build.gradle.kts|0df6657bf8a257b5b1613dad6c17a7997d3be7d9' \
          '??|feature/search/src/commonMain/composeResources/values-zh/strings.xml|516b871ca9def199c6c4f11f171560b577b35342' \
          '??|feature/search/src/commonMain/composeResources/values/strings.xml|e121e6714d33cb9e068ee8096e3ade1abeb20dc5' \
          '??|feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt|66450aea2f38ec34b9bb8b19fa96dc57296d2eb5' \
          '??|feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt|86fc5b1a52374949d438676e2b04440a12e521e5' \
          '??|feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt|bf26a8a638e564f3f252ce21178ebdaa23348478' \
          '??|shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt|6ecb8b6fd828f643dec39515baf12d88c5c9bbca'
  }

  task_5_3_verify_frozen_records() {
      perl -0777ne '
          my %expected = map { my ($status, $path, $hash) = split /\|/, $_, 3; $path => [$status, $hash] }
              grep length, split /\0/, $_;
          my $raw = qx{git -c status.renames=false -c core.quotepath=false status --porcelain=v1 -z --untracked-files=all};
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $seen_ledger = 0;
          while (length $raw) {
              $raw =~ s/\A( M| D|\?\?) // or die "unexpected checkpoint porcelain status\n";
              my $status = $1;
              $raw =~ s/\A([^\0]*)\0// or die "truncated checkpoint porcelain path\n";
              my $path = $1;
              if ($path eq $ledger) { die "bad ledger status\n" unless $status eq " M"; ++$seen_ledger; next; }
              my $want = delete $expected{$path} or die "unexpected checkpoint path $path\n";
              die "bad checkpoint status $path\n" unless $status eq $want->[0];
              if ($status eq " D") { die "deleted checkpoint path exists\n" if -e $path; next; }
              my $hash = qx{git hash-object -- "$path"}; $hash =~ s/\s+\z//;
              die "bad checkpoint hash $path\n" unless $hash eq $want->[1];
          }
          die "missing checkpoint record\n" if keys %expected;
          die "expected one ledger\n" unless $seen_ledger == 1;
      '
  }

  bound_planning_sha="$(awk -F'`' '/^\*\*Bound planning commit:\*\*/ { print $2; exit }' .superpowers/sdd/2026-07-27-feature-first-modularization/task-5.3-brief.md)"
  test "$(awk '/^\*\*Bound planning commit:\*\*/ { count++ } END { print count + 0 }' .superpowers/sdd/2026-07-27-feature-first-modularization/task-5.3-brief.md)" = 1
  printf '%s\n' "$bound_planning_sha" | grep -Eq '^[0-9a-f]{40}$'
  test "$bound_planning_sha" = "$(git rev-parse HEAD)"
  synthetic_bound_sha="$(printf '%s\n' '**Bound planning commit:** `0123456789abcdef0123456789abcdef01234567`.' 'bound_planning_sha="$(awk -F'\''`'\'' '\''/^\*\*Bound planning commit:\*\*/ { print $2; exit }'\'' task-5.3-brief.md)"' | awk -F'`' '/^\*\*Bound planning commit:\*\*/ { print $2; exit }')"
  test "$synthetic_bound_sha" = 0123456789abcdef0123456789abcdef01234567
  test -z "$(git diff --cached --name-only)"
  task_5_3_frozen_records | task_5_3_verify_frozen_records
  for path in \
      build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt \
      shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt \
      shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt \
      shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt; do
      case "$path" in
          *SearchRouteAdapterJvmTest.kt) test ! -e "$path" ;;
          *) test -e "$path" && test -z "$(git diff --name-only HEAD -- "$path")" ;;
      esac
  done
  ```
  `ArchitectureCheckTask.kt`, `LibraryAppShell.kt`, and
  `SettingsPlaylistBackupEmbeddingTest.kt` must exist clean at `HEAD`; the Search route-adapter
  test must remain absent/untracked. The final completed-snapshot 20-endpoint pre-stage and
  post-stage gates below remain unchanged. The exact NUL-safe frozen-record parser itself rejects
  every unlisted tracked or untracked path, including any untracked path outside or inside
  `feature/search`; no weaker secondary untracked-path check exists.
  During this uncommitted documentation review only, the controller separately compares the current
  worktree while manually excluding only the edited canonical plan. That temporary review comparison
  is not sourced, called, or enabled by the committed continuation gate and is deleted after the
  plan commit.
- [ ] Define and use this fail-closed NUL-safe manifest gate. `--no-renames` represents a move as
  its tracked delete and add endpoints. It accepts only `A`, `M`, and `D` status records; every
  other status fails before path comparison.
  ```bash
  task_5_3_manifest() {
      printf '%s\0' \
          feature/search/build.gradle.kts \
          feature/search/src/commonMain/composeResources/values/strings.xml \
          feature/search/src/commonMain/composeResources/values-zh/strings.xml \
          feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt \
          feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt \
          feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt \
          settings.gradle.kts \
          shared/build.gradle.kts \
          shared/src/commonMain/composeResources/values/strings.xml \
          shared/src/commonMain/composeResources/values-zh/strings.xml \
          shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt \
          shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt \
          shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt \
          build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt \
          build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt \
          build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
  }

  task_5_3_parse_name_status_records() {
      perl -0777ne '
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A([AMD])\0// or die "unsupported or malformed name-status record\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing or truncated name-status path\n";
              my $path = $1;
              print "$status\0$path\0";
          }
      '
  }

  task_5_3_parse_name_status() {
      task_5_3_parse_name_status_records | perl -0777ne '
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status code\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status path\n";
              my $path = $1;
              print "$path\0";
          }
      '
  }

  task_5_3_changed_endpoints() {
      git -c core.quotepath=off diff "$@" --no-renames --name-status -z |
          task_5_3_parse_name_status | LC_ALL=C sort -zu
  }

  task_5_3_assert_only_unstaged_progress_ledger() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          my $ledger_count = 0;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status code\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status path\n";
              my $path = $1;
              die "unexpected unstaged diff path\n" unless $path eq $ledger && $status eq "M";
              $ledger_count += 1;
          }
          die "expected exactly one unstaged progress ledger\n" unless $ledger_count == 1;
      '
  }

  task_5_3_assert_no_progress_ledger_in_cached_diff() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status code\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status path\n";
              my $path = $1;
              die "progress ledger staged\n" if $path eq $ledger;
          }
      '
  }

  task_5_3_parse_porcelain_records() {
      perl -0777ne '
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A( M| D|\?\?) // or die "unsupported porcelain state\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing or truncated porcelain path\n";
              my $path = $1;
              print "$status\0$path\0";
          }
      '
  }

  task_5_3_without_progress_ledger() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          my $ledger_count = 0;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain status\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain path\n";
              my $path = $1;
              if ($path eq $ledger) {
                  die "unexpected ledger status\n" unless $status eq " M";
                  $ledger_count += 1;
                  next;
              }
              print "$path\0";
          }
          die "expected exactly one unstaged progress ledger\n" unless $ledger_count == 1;
      '
  }

  task_5_3_assert_only_progress_ledger() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          my $ledger_count = 0;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain status\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain path\n";
              my $path = $1;
              die "unexpected unstaged path\n" unless $path eq $ledger && $status eq " M";
              $ledger_count += 1;
          }
          die "expected exactly one unstaged progress ledger\n" unless $ledger_count == 1;
      '
  }

  task_5_3_prestage_endpoints() {
      git -c status.renames=false -c core.quotepath=false status --porcelain=v1 -z --untracked-files=all |
          task_5_3_parse_porcelain_records |
          task_5_3_without_progress_ledger | LC_ALL=C sort -zu
  }

  printf 'M\0path with space\0A\0new\0D\0old\0' |
      task_5_3_parse_name_status | LC_ALL=C sort -zu > /tmp/task-5.3-name-status-actual
  printf '%s\0' new old 'path with space' | LC_ALL=C sort -zu > /tmp/task-5.3-name-status-expected
  cmp -s /tmp/task-5.3-name-status-expected /tmp/task-5.3-name-status-actual
  ! printf 'R\0old\0new\0' | task_5_3_parse_name_status > /dev/null
  ! printf 'X\0path\0' | task_5_3_parse_name_status > /dev/null
  ! printf 'M\0truncated' | task_5_3_parse_name_status > /dev/null

  { task_5_3_manifest | perl -0777ne 'my $stream = $_; while (length $stream) { $stream =~ s/\A([^\0]*)\0// or die; print "?? $1\0"; }'; printf ' M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0'; } |
      task_5_3_parse_porcelain_records | task_5_3_without_progress_ledger | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-actual
  task_5_3_manifest | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-expected
  cmp -s /tmp/task-5.3-porcelain-expected /tmp/task-5.3-porcelain-actual
  printf ' M path with space\0 D old\0?? new\0 M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_porcelain_records | task_5_3_without_progress_ledger | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-mixed-actual
  printf '%s\0' new old 'path with space' | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-mixed-expected
  cmp -s /tmp/task-5.3-porcelain-mixed-expected /tmp/task-5.3-porcelain-mixed-actual
  printf ' M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger
  ! printf 'M  .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' | task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger > /dev/null
  ! printf ' D .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' | task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger > /dev/null
  ! printf 'R  .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0old\0' | task_5_3_parse_porcelain_records > /dev/null
  ! printf '?? truncated' | task_5_3_parse_porcelain_records > /dev/null
  ! printf ' M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0?? stray\0' | task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger > /dev/null

  printf 'A\0cached-added\0M\0cached-modified\0D\0cached-deleted\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_no_progress_ledger_in_cached_diff
  printf 'M\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger
  ! printf 'A\0cached-added\0M\0cached-modified\0D\0cached-deleted\0M\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_no_progress_ledger_in_cached_diff > /dev/null
  ! printf 'M\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0M\0second-unstaged\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger > /dev/null
  ! printf 'D\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger > /dev/null
  ! printf 'A  cached-added\0M  cached-modified\0D  cached-deleted\0 M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_porcelain_records > /dev/null

  task_5_3_manifest | LC_ALL=C sort -zu > /tmp/task-5.3-manifest
  test "$(tr -cd '\0' < /tmp/task-5.3-manifest | wc -c | tr -d ' ')" = 20
  ```
- [ ] Pre-stage gate: require an empty index, remove the exact single controller-owned ledger record,
  then compare every remaining tracked/untracked path to the literal 20 endpoints. The untracked-aware
  porcelain parser accepts only exact ` M`, ` D`, and `??` states; `status.renames=false` represents
  moves as tracked `D` plus untracked `??` destinations, and NUL parsing preserves paths with spaces.
  A different ledger status, a second ledger record, or any other nonmanifest path fails:
  ```bash
  test -z "$(git diff --cached --name-only)"
  task_5_3_prestage_endpoints > /tmp/task-5.3-prestage
  cmp -s /tmp/task-5.3-manifest /tmp/task-5.3-prestage
  ```
- [ ] Post-stage gate: after explicit staging, do not require an empty index. Instead prove cached
  equality against the same planning `HEAD`, prove the ledger is absent from cached name-status
  records, inspect only unstaged tracked changes through the functional A/M/D name-status parser,
  require exactly one `M` record for the ledger, reject all untracked files, and check the cached
  diff. Do not pass full post-stage porcelain through the pre-stage parser; it contains staged
  implementation `A`/`M`/`D` records by design.
  ```bash
  task_5_3_changed_endpoints --cached HEAD > /tmp/task-5.3-cached
  cmp -s /tmp/task-5.3-manifest /tmp/task-5.3-cached
  git diff --cached --name-status -z --no-renames |
      task_5_3_parse_name_status_records |
      task_5_3_assert_no_progress_ledger_in_cached_diff
  git diff --name-status -z --no-renames |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger
  test "$(git ls-files --others --exclude-standard -z | wc -c | tr -d ' ')" = 0
  git diff --cached --check
  ```
  Fail closed if a path is outside the literal list, a tracked delete is omitted, the pre-stage
  index is nonempty, an unsupported/mixed/rename/unknown porcelain state occurs, a stray untracked
  path exists, cached equality differs, the ledger is staged/different/duplicated, or another
  tracked/untracked implementation path remains unstaged. Canonical plan/OpenSpec/root
  `progress.md`/`roadmap.md` remain unchanged until closeout; ignored SDD brief/report/final-report
  files are excluded without widening the tracked ledger exception.
- [ ] Independently review the unstaged 20-endpoint snapshot twice: a scope/ownership/path review
  and a behavior/API/architecture review. Each must confirm the literal public signature/KDoc,
  resource partition, no reverse/export/API exposure, Shared adapter causal semantics, duplicate
  behavior, retained exclusions, and all test evidence before the implementation commit.
- [ ] Stage only explicit implementation endpoints, never broad `git add`: pass the pre-stage gate,
  use this exact command, then run the post-stage gate. It excludes the progress ledger from staging
  and from the staged review:
  ```bash
  git add -- feature/search/build.gradle.kts feature/search/src/commonMain/composeResources/values/strings.xml feature/search/src/commonMain/composeResources/values-zh/strings.xml feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt settings.gradle.kts shared/build.gradle.kts shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-zh/strings.xml shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
  ```
  then commit exactly `git commit -m "refactor: extract search feature"`. The planning commit is
  the implementation comparison base; the plan and progress ledger are never in the implementation
  commit. After that commit, the ledger remains the expected sole tracked evidence diff until the
  separate closeout.
- [ ] Evidence closeout begins only after that commit. Its exact eight paths are canonical plan;
  `openspec/changes/feature-first-modularization/tasks.md`; `progress.md`; `roadmap.md`;
  `.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md`;
  `task-5.3-brief.md`; `task-5.3-report.md`; and `task-5.3-final-acceptance-report.md` under that
  same SDD directory. `task-5.3-brief.md`, `task-5.3-report.md`, and
  `task-5.3-final-acceptance-report.md` are ignored and excluded until closeout; force-add them
  there. Keep OpenSpec 6.3 unchecked until evidence, leave 6.4/7/8 open, and update roadmap only
  after implementation acceptance.
- [ ] Acceptance review includes the focused configuration/namespace regression selectors, the full
  `ArchitectureCheckPluginFunctionalTest` TestKit class, and independent checkpoint re-review after
  those checks. Preserve historical 75-test checkpoint evidence as history only; final counts
  supersede it. Do not claim acceptance until that re-review passes.

### Plan Self-Review

- [ ] Map each design/OpenSpec requirement to this section; scan for incomplete instructions,
  ambiguity, stale tokens, inconsistent types/signatures, wrong namespaces, and prohibited broad
  staging. Confirm the implementation parser has no pre-plan SHA, the post-stage gate does not
  reuse the empty-index assertion, no command mixes `--tests` with iOS tasks, no broad TestKit
  command is accepted, and the deprecated production-source wording is absent. Confirm the former
  manifest count is absent from Task 5.3; the common-test path occurs in the manifest, parser, staging command, and
  test ledger; no pre-stage `git diff HEAD` collector exists; porcelain includes
  `--untracked-files=all`; the new planning `HEAD` is rebound in the brief; the one-time continuation
  gate requires exactly the frozen 16 implementation records/hashes plus sole ` M` SDD ledger and
  four untouched endpoints; completed pre-stage requires exactly the 20 implementation endpoints
  plus that sole ledger; and post-stage requires cached 20 endpoints plus sole unstaged ` M` ledger
  with no untracked file. Post-stage uses `git diff --name-status -z --no-renames` and the A/M/D
  name-status parser, never the full porcelain stream. Confirm parser self-tests accept the 20
  manifest paths plus that exact ledger and reject staged, deleted, renamed, duplicate-ledger, and
  extra-path records. Confirm canonical plan/OpenSpec/root `progress.md`/`roadmap.md` remain
  unchanged until closeout and ignored SDD brief/report/final-report paths are not broadened into a
  generic tracked-evidence exclusion. For this governance-plan amendment only, do not run the
  implementation path comparator because the partial implementation already exists: prove literal
  manifest total/unique is 20, plan and brief mirror the governance amendment, and before/after
  hashes of every dirty implementation endpoint are byte-identical. Only the canonical plan and the
  ignored Task 5.3 brief may change in this lane; do not modify report, ledger, OpenSpec, design,
  root progress, or roadmap.
- [ ] Prove the manifest count is 20 with the parser, verify named test selectors exist or are
  explicitly created by this plan, validate command/task names against current Gradle conventions,
  run strict named OpenSpec validation and `git diff --check`, and inspect `git status --short`.
  Confirm this documentation amendment modifies only this plan section and ignored brief, then do
  not claim tests or builds ran.
