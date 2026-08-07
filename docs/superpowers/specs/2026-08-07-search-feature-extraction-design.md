# Task 5.3 Search Feature Extraction Design

**Route:** OpenSpec + Superpowers

## Context

Search is currently implemented by Shared at
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`.
That screen combines leaf rendering and query state with Shared-owned navigation,
playback, selection, scroll, resource, and UI dependencies. `LibraryRoutes` invokes
it from the `LibraryRoute.Search` overlay and currently passes a `TagLibReader` that
Search does not use. The only mixed Search-focused JVM test class also contains a
Home browse-mode selection-clear test.

The current UI deliberately treats a blank or whitespace-only query as no results;
filters titles, artists, and albums case-insensitively without changing input order;
and keeps duplicate IDs and empty metadata intact. It does not load artwork or use a
repository asynchronously.

## Goals

- Extract Search into one unexported implementation module, `:feature:search`, while
  preserving its Kotlin package: `com.eterocell.rhythhaus.search`.
- Keep Shared as the only facade and composition owner. It retains route and Back
  ownership, playback decisions, selection state/reconciliation, bottom-bar scroll
  storage and policy, Now Playing bar policy, and `EqualizerStrip` composition.
- Make the feature a callback-first Search leaf. It owns query/filtering, rendering,
  input focus, result count, empty result presentation, and Search row interaction.
- Move only Search-owned localized resources. Keep common wording at Shared and pass
  it as immutable text/function inputs.
- Preserve Android, JVM desktop, iosArm64, and iosSimulatorArm64 compilation and test
  support without exposing the feature through the Shared iOS framework.

## Non-Goals

- No Search API module, public facade module, Koin module, feature README, or empty
  state/presenter/view-model/event abstraction.
- No `:core:navigation` module and no dependency on `:core:playback`; primitive
  playback state and callbacks are sufficient at this boundary.
- No platform source set, database/repository/scanner move, SQLDelight change, TagLib
  move, Shared source migration beyond route composition, iOS framework export, or
  platform-specific behavior.
- No artwork, remote/local asynchronous lookup, repository error UI, or callback
  failure UI. Shared owns callback failures and their policy.
- No runtime-device, visual, accessibility-device, or playback-engine behavior claim
  from this extraction.

## Selected Approach

Create one Android-KMP Compose implementation module:

```text
:shared -> :feature:search -> :feature:library:api
                         -> :core:ui
```

`settings.gradle.kts` includes `:feature:search`. Its build uses
`build-logic.kmp.feature.impl`, `build-logic.android.kmp.library`, Compose resources,
and the Compose compiler. It declares Android-KMP, JVM, iosArm64, and
iosSimulatorArm64 targets. Android uses namespace
`com.eterocell.rhythhaus.search`; Compose resources use
`rhythhaus.feature.search.generated.resources`.

The feature has one `commonMain` implementation and no platform-specific production
source file.
It applies `withHostTest {}` and Android resources, mirroring the existing leaf
feature target model. Feature tests use common Kotlin test dependencies and JVM
Compose UI test dependencies. Its resource roots are
`feature/search/src/commonMain/composeResources/values/strings.xml` and
`feature/search/src/commonMain/composeResources/values-zh/strings.xml`.

`commonMain` dependencies are deliberately derived from the public contract and
implementation:

- `api(projects.feature.library.api)` because `LibraryTrack` appears in the public
  composable contract.
- `api(libs.compose.runtime)` and `api(libs.compose.ui)` because public Compose
  annotations, `Modifier`, `Dp`, and the composable lambda appear in the contract.
- `implementation(projects.core.ui)` for Shared UI primitives/theme use inside the
  feature, and `implementation` Compose Foundation, Compose resources, and Miuix
  dependencies for its implementation-only UI and generated resource access.
- Other Compose/Miuix scopes follow the resolved public declaration requirements;
  no generated `Res` handle appears in a public signature.

There is no Koin dependency or Koin registration. There is no direct feature edge to
Shared, `:core:database`, `:core:platform`, `:core:playback`, `:taglib`, an app,
another feature implementation, or an iOS export. Shared adds only an
implementation dependency on `:feature:search`; it remains the sole framework and
composition surface.

### Rejected Alternatives

- **Feature API plus implementation modules:** rejected because Search has one
  Shared consumer and a small, stable UI callback contract. A second published
  module would add ownership and verification cost without a second consumer.
- **Expose playback controller/state to Search:** rejected because it couples a leaf
  UI module to core playback semantics. `currentTrackId`, `isPlaying`, and playback
  request callbacks fully express Search's needs.
- **Move selection models/reducer to Search:** rejected because selection spans Home,
  detail pages, routes, and Shared dismissal policy. Search receives only its
  effective selection inputs and emits Search-specific intents.
- **Keep a Shared compatibility `SearchScreen`:** rejected because it leaves a
  duplicate facade and conceals the ownership migration. Shared calls the feature's
  public `SearchContent` directly and deletes the old Shared `SearchScreen`.
- **Inject Shared resources or generated resource handles:** rejected because this
  creates a resource-module boundary leak. Shared passes common strings and the
  composable selection label formatter that resolves `select_track_format` with structured
  Compose `stringResource` while Search composes a row.

## Public Boundary

The feature exposes exactly `SearchSharedLabels` and `SearchContent` from
`com.eterocell.rhythhaus.search`. Both declarations are public solely because Shared
composes the unexported module. Every public declaration and every public parameter
has declaration KDoc describing the behavior below.

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

The signature has no Shared type, generated resource type, `PlaybackController`,
`PlaybackState`, `TagLibReader`, repository, Koin, platform type, or playback queue
type. `LibraryTrack` is supplied by `:feature:library:api`. `SearchSharedLabels` is a
data class, so it has value equality. The label function lets Shared derive the
existing `select_track_format` text with structured Compose `stringResource` while Search
composes a row, without exporting a resource handle. No default
is supplied for behavior-bearing callbacks or state; the only defaults are normal
layout customization values.

## Ownership And Interaction Flow

1. `LibraryRoute.Search` remains a Shared overlay. `LibraryRoutes` directly calls
   `SearchContent`, passing `libraryTracks`, primitive current playback state,
   `SearchSharedLabels`, `selectTrackLabel`, effective Search selection state, and
   the list padding. It removes the unused `TagLibReader` parameter/import from the
   Search route path.
2. Search holds its query, lazy list state, and one `FocusRequester` locally. On its
   first composition it requests focus once. The clear action resets the query to
   the empty string.
3. A blank or whitespace-only query returns no results. A nonblank query filters
   title, artist, and album case-insensitively, preserves the incoming list order,
   and retains duplicate IDs and empty metadata exactly as supplied. Its internal, non-public
   LazyColumn occurrence identity is composed from filtered occurrence index plus track ID, never
   `track.id` alone. It is unique for duplicate occurrences and does not alter `LibraryTrack`,
   selection IDs, visible-ID sequences, playback queue order, or duplicate semantics.
4. When the ordered filtered ID sequence changes, Search calls
   `onVisibleTrackIdsChanged`. Shared converts that to
   `TrackSelectionAction.ReconcileVisible(TrackSelectionPageKey.Search, ids)` and
   continues to own the reducer and route selection-clear behavior.
5. Lazy list position emits primitive index and offset through
   `onScrollPositionChanged`. Shared stores `LibraryScrollPosition` and applies its
   existing Now Playing bottom-bar visibility policy.
6. A normal row click, outside selection mode, invokes `onPlayTrack(filtered, track)`.
   Shared adapts ordered `LibraryTrack` values to its playback queue, restarts when
   the selected ID is current, otherwise sets the queue and starts playback, then
   applies the existing dismiss policy. Callback failures remain Shared-owned.
7. A long press invokes `onStartSelection(track.id)` without playback. In selection
   mode, row activation and the checkbox each invoke exactly one
   `onToggleSelection(track.id)` and never invoke playback.
8. Search highlights the current row and supplies current-playing semantics from
   `labels.nowPlaying`. It invokes `playingIndicator` only for a current, playing
   row, allowing Shared to retain `EqualizerStrip`; Search never renders artwork.
9. Back and the top-app-bar dismissal use `onDismiss`; Search does not register a
   Back surface or decide route arbitration.

## Resources

Move exactly these Search-only keys from Shared to feature Search, in both English
and Chinese resource files:

- `search_placeholder`
- `search_results_count_zero`
- `search_results_count_one`
- `search_results_count_many`
- `search_no_tracks_match_format`

The feature owns rendering those five values through its own generated resource
namespace. Shared retains `search`, `clear`, `now_playing_badge`, and
`select_track_format`; it injects title, clear, Now Playing text, and the
composable title-to-selection-label formatter. No resource key is duplicated and no generated
resource handle crosses the module boundary.

## Test Plan

Split `SearchSelectionPoliciesJvmTest`. Move these four Search cases into feature
JVM production-composable tests: normal click plays outside selection, long click
starts selection without playback, selection-mode row and checkbox each toggle once,
and filtered-ID reconciliation dispatches on sequence changes. Keep the Home
browse-mode selection-clear test in Shared because it tests Shared Home policy.

Add production-composable tests owned by `:feature:search` for:

- case-insensitive title/artist/album filtering, blank-query no results, result
  count forms, and no-match text;
- one-time focus, clear behavior, primitive scroll callback, and bottom padding;
- ordered-result playback request, selected-track identity, Shared-driven dismiss,
  and current-track restart behavior through route adapter tests;
- current-row highlight, semantics, and supplied indicator slot;
- long-press selection, single-toggle row/checkbox behavior, and filtered visible-ID
  reconciliation;
- empty metadata and duplicate-ID preservation, including two equal-ID occurrences rendering and
  activating distinctly with keys surviving unrelated recomposition while visible/playback
  callbacks retain duplicate order.

Shared retains real `LibraryRoutes` composition tests, Back/route tests, selection
reducer and route-clear tests, playback adapter/restart/dismiss tests, scroll storage
and Now Playing bar-policy tests. Tests do not manufacture a feature-owned playback
controller or repository.

## Architecture, Staging, And Verification

Begin with RED controls: the absent module/target test, then architecture fixtures
that reject an illegal `:feature:search -> :shared` edge, forbidden implementation/
database/platform/iOS-export dependencies, a wrong package, wrong Android namespace,
wrong resource namespace or ownership, missing public declaration KDoc, and Koin
registration. Confirm the architecture allow-list and KSP registration recognize the
new module before relocation.

GREEN creates the exact target/dependency/resource graph, moves Search source and
Search-owned tests atomically, updates Shared composition to the direct public call,
and deletes the old Shared `SearchScreen`. Do not retain a compatibility wrapper.
Stage the implementation independently from documentation and evidence closeout;
the implementation commit contains only the implementation task's code, Gradle,
resource, test, and architecture changes.

Run and record:

```text
./gradlew :feature:search:jvmTest :feature:search:testAndroidHostTest :feature:search:iosSimulatorArm64Test --configuration-cache
./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test --configuration-cache
# Run focused Search route/adapter tests, then the full Shared JVM and iOS suites.
./gradlew :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache
/usr/bin/xcrun xcodebuild -version
# Run the established Shared iOS framework build plus iosApp Xcode Simulator build and tests.
./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache
./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail
./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail
./gradlew :architecture-processor:test --configuration-cache
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
openspec validate feature-first-modularization --strict
git diff --check -- <changed paths>
./init.sh
```

The second architecture run must demonstrate configuration-cache reuse. The final
documentation/evidence lane additionally applies an exact changed-path gate. Record
an unavailable Xcode tool or failed command as a blocker rather than treating it as
passed.

## Migration Risks And Acceptance Limits

The main risk is falsely transferring Shared policy into the feature: a Search leaf
must not decide playback restart/dismiss behavior, route/Back handling, selection
reduction, bottom-bar visibility, or equalizer ownership. The explicit primitive
contract and route adapter tests are the acceptance control. A second risk is
resource duplication; ownership is accepted only when the five moved keys exist once
in Search and Shared continues to own every injected common key.

Acceptance is limited to compile/test/architecture/quality evidence from the stated
commands. It does not establish actual device playback, physical touch/long-press
behavior, desktop application launch, visual rendering, screen-reader behavior, or
iOS runtime resource lookup.
