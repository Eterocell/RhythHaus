## Purpose

Demand-driven feature-first module graph, dependency direction, ownership and state contracts, Back/iOS boundaries, and the thin-shared completion.

## Requirements


### Requirement: Demand-driven feature-first module graph

The implementation SHALL migrate RhythHaus through buildable contract-first slices to `:core:model`, `:core:ui`, `:core:database`, and narrowly-scoped `:core:platform`; SHALL add `:core:playback` when playback contracts/engine are extracted; and SHALL add `:core:navigation` only when a common destination-scoped Back contract requires it. It SHALL create library and playlists API/implementation modules, keep Now Playing, Search, and Settings single modules initially, and SHALL NOT create `:core:network`, empty modules, or empty pattern types.

#### Scenario: A slice introduces only demanded structure
- **WHEN** an implementation slice adds a module or API/implementation boundary
- **THEN** the slice demonstrates its real owner or stable contract through code and tests
- **AND** no speculative core/network module or empty UI-state pattern is added.

### Requirement: Dependency direction and shared composition boundary

Applications SHALL depend on `:shared`; `:shared` SHALL own `App()`, root shell, cross-feature route/Back arbitration, lifecycle, Koin assembly, and stable `MainViewController`. No core or feature module SHALL depend on `:shared` or an app module; no feature SHALL depend on another feature implementation; cross-feature access SHALL use feature APIs; and only `:shared` SHALL compose implementation modules.

#### Scenario: Graph rejects a forbidden bridge
- **WHEN** a fixture adds a core/feature-to-shared edge, an app edge, a feature-implementation edge, or a feature-to-shared-to-feature bridge
- **THEN** architecture verification fails with the forbidden dependency reported
- **AND** the normal graph remains acyclic.

### Requirement: Ownership and state contracts remain local

Core model SHALL contain only truly cross-feature immutable projections; core UI SHALL contain reusable primitives/theme/artwork abstractions without feature UI state; core database SHALL own the sole SQLDelight schema/driver/migrations/generated DB; and core platform SHALL contain only capabilities reused by at least two domains. Library SHALL own scanner/source/index/repository/UI/transient state, playlists SHALL own playlist repository/edit/backup/UI, playback contracts/engine SHALL belong to core playback, and feature internal state SHALL remain local.

#### Scenario: Stateful feature screen follows its local contract
- **WHEN** a stateful feature screen is migrated
- **THEN** immutable `UiState`, `UiEvent`, and `UiEffect` are coordinated by its Presenter/ViewModel
- **AND** its data flow is UI -> Event -> Presenter -> UseCase -> Repository -> DataSource with boundary-local representation mapping.

### Requirement: Playlists implementation owns saved playlists and backup without widening the ABI

The unexported `:feature:playlists:impl` SHALL target Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64`, and SHALL own saved-playlist/playback-queue UI, immutable Playlist state/actions/reducer/owner, repository implementations/Koin binding, backup codec/service/state/UI, the platform-neutral document seam, feature-side launchers, and feature resources. Direct project edges SHALL be limited to playlists API, Library API, core model/playback/ui/platform, and implementation-only core database. Public implementation declarations SHALL be limited to shared-needed state/action/result, owner, composable, dismissal, backup orchestration, launcher contracts, and the sole Koin binding-module factory returning `org.koin.core.module.Module` for shared assembly, all with declaration-specific behavioral KDoc. Generated DB/SQLDelight/generated `Res`/shared navigation or shell types SHALL NOT appear in public signatures; Koin SHALL NOT appear elsewhere in that public surface.

#### Scenario: Existing Playlist state is the Task 5.2-specific equivalent
- **WHEN** Task 5.2 moves the already-characterized immutable `PlaylistState`, `PlaylistStateAction`, reducer, `PlaylistStateOwner`, and backup immutable state/reducer
- **THEN** those established Playlist-specific contracts satisfy this behavior-preserving migration without a new Presenter/ViewModel/`UiEffect`/Event scaffold
- **AND** this exception applies only to Task 5.2 and does not alter the architecture-wide stateful-feature requirement.

#### Scenario: Repository and Shared-test boundaries remain explicit
- **WHEN** Task 5.2 moves playlist implementation ownership
- **THEN** `InMemoryPlaylistRepository` and `SqlDelightPlaylistRepository` move to feature implementation ownership and only `feature/playlists/api/.../PlaylistRepository.kt` remains public
- **AND** retained Shared lifecycle, source-management, and DI tests use that public contract and public feature state ports, never implementation classes or feature-private helpers
- **AND** internal `authoritativePlaylistBackupRevisionGuard(owner: AuthoritativeLibraryPublicationOwner): PlaylistBackupRevisionGuard` is common-test-visible and verifies current/stale delegation through `AuthoritativeLibraryPublicationOwner.withCurrentRevision` plus exact `CancellationException` rethrow.

#### Scenario: Task 5.2 moves adapters only
- **WHEN** playlists implementation ownership moves
- **THEN** `:core:database` remains sole physical owner with no `.sq`, `.sqm`, schema, migration, generated DB, driver, database-name, or FK changes
- **AND** serialization/revision/cancellation, backup exclusivity, exact 4 MiB limits, mappings, stale-library rejection, transactional import, exactly-once native completion, and playback engine/session/lifecycle/root state ownership are preserved.

#### Scenario: Shared and iOS boundaries remain thin
- **WHEN** the feature is composed
- **THEN** shared retains composition, shell/routes/Back, lifecycle, Koin assembly, Settings layout until Task 6.4, generic injected `cancel`, and selection-bar composition
- **AND** the feature owns embeddable backup sections/dialogs and playlist/queue/backup EN/ZH text once without duplicate resource keys/handles
- **AND** Shared retains the exact ABI ledger in the canonical Task 5.2 Superpowers design, including package `com.eterocell.rhythhaus.playlistbackup`, framework `Shared`, status values, Completion/Provider signatures/nullability, Bridge singleton access, MIME/max-size exports, and Swift `Int32` interop; the feature is not exported and Swift files remain app-owned.

### Requirement: Back publication and deletion remain authoritative

The feature SHALL publish exactly one already-resolved foremost immutable dismissal surface, modal before edit, with stable identity per appearance and new identity per re-presentation. Shared SHALL accept only the active destination, prevent stale registrations/disposers from replacing or clearing active state, latch exact destination/appearance for predictive Back, perform no dismissal on cancellation, revalidate completion and dispatch at most once, and clear rejected/stale sessions without same-intent fallthrough. A dispatched non-predictive transition SHALL remain in flight until authoritative state reports its exact latched target inactive or that target explicitly rejects completion; repeated Back SHALL be suppressed meanwhile, and callback return SHALL NOT settle or release suppression. Rejection SHALL release the intent without treating the target as settled; a later Back SHALL be a new intent. Confirmed absence of the exact displayed playlist SHALL be the only deletion invalidation; failed/stale/replayed deletion SHALL not invalidate and unrelated state SHALL be preserved.

#### Scenario: Predictive Back resolves only the latched active appearance
- **WHEN** modal and edit dismissal surfaces are published for an active playlist destination and predictive Back begins
- **THEN** shared latches the modal surface's exact destination and appearance, cancellation changes nothing, and a valid completion dispatches it at most once
- **AND** for non-predictive dispatch, callback return leaves the intent in flight until authoritative inactive observation or explicit rejection; repeated Back is suppressed, rejection releases without settlement, and later Back is a new intent
- **AND** stale registration, disposal, completion, or failed/stale/replayed deletion cannot clear active state, fall through to edit/route, or invalidate unrelated state.

### Requirement: Task 5.2 and OpenSpec 6.2 are accepted

Task 5.2/OpenSpec 6.2 SHALL be recorded as completed by implementation commit `fc1b96f858408c8dfd07221d5fe85ae3e20ced63` and evidence closeout `6e885ef75ada0d6e48b2832cb3852b460a6c62ed`. Its compile/link/tests SHALL NOT be interpreted as runtime, device, visual, picker, or playback validation.

#### Scenario: Accepted playlists evidence remains bounded
- **WHEN** the completed Task 5.2 boundary is referenced
- **THEN** it retains its accepted implementation and evidence-closeout commits
- **AND** it does not reopen OpenSpec 6.2 or infer runtime/device/visual behavior from retained automated evidence.

### Requirement: Approved migration non-goals remain bounded

Task 5.2 SHALL NOT redesign visuals/products, rewrite the state framework, add navigation/core-navigation or generic document modules, rename packages, change database or playback ownership, add illegal bridge/service locator/implementation coupling, export the feature, redesign Swift, duplicate resources, or claim runtime/device behavior from compile/link/tests.

#### Scenario: An implementation proposal stays within the approved move
- **WHEN** a Task 5.2 implementation change is reviewed
- **THEN** it changes ownership only and contains none of the prohibited redesign, module, package, database, playback, bridge, export, Swift, resource, or evidence-claim changes.

### Requirement: Search is a callback-first unexported leaf feature

Task 5.3 SHALL create exactly one unexported `:feature:search` with Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64` targets; one common implementation; Kotlin package and Android namespace `com.eterocell.rhythhaus.search`; and resource namespace `rhythhaus.feature.search.generated.resources`. It SHALL have no API split, Koin module, platform source, iOS export, Shared/core playback/database/platform/taglib/another-implementation/app edge, repository, empty state abstraction, or `feature/search/README.md`. Its `api` SHALL be only Library API plus public Compose runtime/UI requirements; core UI, Foundation, resources, and Miuix SHALL be implementation-only. Shared SHALL depend through exactly `implementation(projects.feature.search)`, SHALL NOT use `api`, and SHALL NOT export Search.

Its only explicit-public, declaration-specific-KDoc boundary SHALL include this value-equal Shared-label declaration:

```kotlin
/**
 * Shared-owned wording consumed by [SearchContent]. Value equality keeps unchanged
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
```

The other and final public declaration SHALL be:

```kotlin
@Composable
public fun SearchContent(
    libraryTracks: List<LibraryTrack>, currentTrackId: String?, isPlaying: Boolean,
    labels: SearchSharedLabels, selectTrackLabel: @Composable (String) -> String,
    selectionModeActive: Boolean, selectedTrackIds: Set<String>,
    onStartSelection: (String) -> Unit, onToggleSelection: (String) -> Unit,
    onVisibleTrackIdsChanged: (List<String>) -> Unit,
    onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
    onPlayTrack: (orderedResults: List<LibraryTrack>, selectedTrack: LibraryTrack) -> Unit,
    onDismiss: () -> Unit, playingIndicator: @Composable () -> Unit,
    bottomContentPadding: Dp = 0.dp, modifier: Modifier = Modifier,
)
```

No Shared/generated resource/playback controller or state/repository/Koin/platform/database/TagLib/queue type SHALL occur in that boundary. `selectTrackLabel` SHALL composably resolve Shared's `select_track_format` with structured Compose `stringResource` while Search composes a row; no generated resource handle crosses the boundary. `LibraryRoutes` SHALL compose `SearchContent` directly and delete Shared compatibility `SearchScreen` and unused `TagLibReader`. Shared SHALL retain route/Back, selection/reconciliation/clear, scroll storage, playback queue/restart/dismiss, bottom-bar/Now Playing policy, and `EqualizerStrip`; Search SHALL retain query/filter/render/focus/count/empty/row interaction. Search SHALL preserve blank-query no results; case-insensitive title/artist/album matching; order, duplicates, and empty metadata; and a private rendering-only LazyColumn occurrence identity of filtered occurrence index plus track ID, never `track.id` alone. It SHALL be unique without altering public `LibraryTrack`, selection IDs, visible-ID sequence, playback queue order, or duplicate semantics. Search SHALL focus exactly once; clear reset to blank; visible-ID emission only on sequence change; normal activation requesting ordered filtered playback; long press never playing; selection-mode row and checkbox each toggling once without playback; current-row highlight and Now Playing semantics; an indicator only for current+playing; and no artwork/error state. Search SHALL own exactly the five approved EN/ZH keys; Shared SHALL inject title/clear/Now Playing/composable select-track formatting without duplicate keys or resource handles. The approved design is [2026-08-07-search-feature-extraction-design.md](../../../../../docs/superpowers/specs/2026-08-07-search-feature-extraction-design.md).

#### Scenario: Search ownership and behavior are verified
- **WHEN** Task 5.3 is implemented
- **THEN** feature production-composable tests cover Search behavior and the four moved mixed-suite cases, including two equal-ID occurrences rendering/activating distinctly with keys surviving unrelated recomposition and visible/playback callbacks preserving duplicate order, while real Shared route-adapter tests prove queue order, current-track restart, dismissal, and callback-failure ownership
- **AND** RED/GREEN rejects an absent `:feature:search` module/target before registration with failure caused solely by absence; feature-to-Shared/core-playback/database/core-platform/taglib/another-implementation/app edges; Koin; iOS export; Shared `api` or exported Search exposure; wrong package/Android/resource namespace; a wrong Search resource-ownership control where a moved key is missing, duplicated, or owned by the wrong module, distinct from wrong namespace and generated-handle controls; resource duplicates/generated handles; and missing public KDoc/public-surface closure
- **AND** supported-platform, architecture, quality, strict named OpenSpec, Xcode, and `./init.sh` evidence are recorded without runtime/device/visual claims.

### Requirement: Task 6.3 remains executable and unchecked

Task 6.3 SHALL remain unchecked until the complete Task 5.3 module, public-boundary, dependency, ownership, behavior, resource, test-split, RED/GREEN, and bounded-evidence requirements are accepted. The later executable plan SHALL own detailed exact paths and commands.

#### Scenario: Pending Search acceptance is bounded
- **WHEN** Task 6.3 is closed
- **THEN** it records cross-platform, architecture, quality, strict OpenSpec, Xcode, and `./init.sh` evidence for one atomic direct-Shared-composition Search move
- **AND** it does not infer runtime, device, visual, accessibility-device, playback-engine, desktop-launch, or iOS runtime-resource behavior.

### Requirement: Settings is a callback-first unexported leaf feature

Task 6.4 SHALL create exactly one unexported Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`
`:feature:settings` with one common implementation, no API/implementation split, no Koin module,
no `iosMain` production source, and no Shared-framework export. Its Kotlin package and Android
namespace SHALL remain `com.eterocell.rhythhaus.settings`; its resource namespace SHALL be
`rhythhaus.feature.settings.generated.resources`. Its only project edge SHALL be
`api(:core:ui)`; public Compose/runtime/UI dependencies required by public signatures SHALL be API,
and Foundation/resources/icons/Miuix/AboutLibraries/coroutines SHALL be implementation-only.
Shared SHALL use only `commonMainImplementation`, never `api` or export. Settings SHALL NOT depend
on Shared, apps, core database/platform/playback, taglib, any feature module, Koin, DataStore, or
an iOS export; it SHALL NOT use Library API.

Its exact KDoc-complete public declarations SHALL be `SettingsSharedLabels(title, addMusicFolder,
folderPickerUnavailable, clearLibrary, cancel, remove)`, value-equal
`SettingsSourceItem(id, displayName, accessAvailable, hasBeenScanned)`, `SettingsScreen`,
`SettingsAboutScreen`, and `OpenSourceLibrariesScreen(readCatalogJson: suspend () -> String, ...)`.
No other Settings production declaration SHALL be public. No public signature SHALL contain Shared,
Library, Playlist, generated foreign `Res`, route/Back, repository, scanner, launcher, controller,
job, Koin, or DataStore types.

The exact `SettingsScreen` boundary SHALL include `sourcePickerActionVisible: Boolean`,
`sourcePickerAvailable: Boolean`, `mutationsEnabled: Boolean`, `hasImportedTracks: Boolean`,
`onRequestClearLibrary: () -> Unit`, and nullable
`clearLibraryDialog: (@Composable () -> Unit)?`, along with scalar theme/source inputs, source-ID
callbacks, playlist-backup and nullable scan slots, About/dismiss callbacks, and `Modifier`.
Function KDoc SHALL document parameter behavior in prose/contracts, while declaration-specific KDoc
is required for every public declaration and every public data property. The picker SHALL hide when
not visible, use unavailable wording when unavailable, and respect mutation disablement; the clear
action SHALL render only with imported tracks, request Shared visibility only when enabled, and
render the nullable slot.

#### Scenario: Settings composes presentation without receiving Library ownership
- **WHEN** Shared composes Settings
- **THEN** it maps authoritative current Library sources to `SettingsSourceItem`, supplies scalar
  values, callbacks, `playlistBackupContent`, nullable `activeScanContent`, and a
  `clearLibraryDialog` slot
- **AND** Settings owns only presentation-local source-removal dialog visibility and About retry
  generation, without Presenter/ViewModel/Event/Effect scaffolding
- **AND** Shared retains authoritative source-ID adaptation, mutation guards/errors, scanner/picker/
  clear-library orchestration, routes/Back/dismissal, playlist-backup controller/dialog behavior,
  theme persistence/actuals/Koin/root theme application, and `RhythHausThemeMode`/palettes in core UI.

#### Scenario: Settings source callbacks resolve current authority
- **WHEN** Settings emits a source ID
- **THEN** Shared resolves it against latest authoritative `librarySources` at invocation and then
  reevaluates initial-publication and scan/job guards
- **AND** a missing/stale ID is a no-op with no mutation, scan, access release, or unrelated
  dismissal; errors use the existing Shared import/mutation path
- **AND** tests cover current/stale IDs, authoritative replacement between composition and click,
  and changed guards.

#### Scenario: Settings preserves app-wide attribution and resource ownership
- **WHEN** Task 6.4 moves Settings/About presentation and `RhythHausBuildInfo` generation/model/
  verification to Settings
- **THEN** Shared retains AboutLibraries generation/configuration/manual TagLib attribution and its
  checked-in `aboutlibraries.json`, and Settings parses/renders caller-supplied JSON with retryable
  read/parse failures; injected read and parse callback cancellation preserve exact object identity (parse is carried as data out of dispatcher work), while dispatcher rejection, prompt cancellation, and Job cancellation propagate without `Loaded`/`Failed` publication or identity promise
- **AND** Settings owns appearance/theme, source-management-only, About/AboutLibraries, logo, and
  remove-source-dialog resources, while Shared retains/injects its clear-dialog and generic wording;
  no generated resource handle crosses the boundary.

The Shared EN/ZH ledger SHALL retain `settings`, `add_music_folder`,
`folder_picker_unavailable`, `clear_library`, `clear_library_message`, `clear`, `cancel`, `remove`,
and `close`, plus `scanning`, `scan_progress_format`, `scan_complete_format`,
`folder_picker_error_access`, `folder_picker_error_select`, `folder_picker_error_prepare`, and
`folder_picker_no_folder_selected` for Shared scanning-card, App, and platform-picker consumers;
those keys SHALL NOT cross into Settings. Labels carry `settings`, `add_music_folder`,
`folder_picker_unavailable`, `clear_library`, `cancel`, and `remove`, while Shared resolves playlist
`close` and clear-dialog strings internally. Settings SHALL own exactly once in each locale
`manage_music`, all appearance/theme, source-row/remove-dialog, About/AboutLibraries keys, and
`rhythhaus_logo` listed by the approved Settings design. Architecture controls SHALL reject parity,
missing, wrong-owner, duplicate, and logo violations. Non-empty catalog parsing is required; the
fixture has a top-level `libraries` array and `licenses` map, and malformed/empty data is the same
retryable Failure. Retry immediately enters Loading and uses `Dispatchers.Default`. An injected read
`CancellationException` SHALL rethrow with exact identity; injected parse cancellation SHALL be
captured inside dispatcher work and rethrown outside with its original identity. Genuine dispatcher
rejection, `withContext` prompt cancellation, and Job cancellation SHALL propagate without
`Loaded`/`Failed` publication and without an identity guarantee. The current request is an opaque
token of current loader identity plus monotonic retry generation; only exact-current-token completion
publishes, so an obsolete cancellation-resistant loader cannot overwrite a newer replacement result.
The root SHALL retain visual background/root semantics and use an input-only full-size non-semantic
pointer shield as its first background child. Opaque Settings `Surface`/`Scaffold`/`LazyColumn` SHALL
be a later foreground sibling and clear-library/source-removal dialogs later siblings above both. The
shield SHALL consume at Initial only when sibling hit testing selects it, SHALL never be a controls
ancestor, and SHALL have no clickable/focusable semantics; `AboutRow` remains normally clickable. The
lower Library shell and Settings overlay are layered siblings, so the shield catches uncovered overlay
coordinates while foreground controls win sibling hit testing. Causal tests SHALL mount a behind
full-size clickable sibling first then production Settings: a physical blank-coordinate tap proves
behind=0; a physical picker-center tap proves picker=1 and behind=0; root/shield have no click/focus
semantics; and scroll/children work. Tests SHALL use stable tags/unmerged Miuix trees; disabled
mutations prove no click action and zero callbacks unless an approved accessibility contract explicitly
requires disabled semantics. No parent-pass inference, background-ancestor test, `nestedScroll`,
`pointerInterop`, disabled clickable, or custom sibling-sharing node is allowed.

### Requirement: Task 6.4 remains executable and unchecked

Task 6.4 SHALL remain unchecked until Settings feature layout/policy/source-row/local-dialog/theme
callback/slot/About/resource/build-info tests; retained Shared route/source-ID/mutation/theme/
backup-Back/catalog-TagLib tests; architecture RED/GREEN; supported-platform and quality checks;
strict named OpenSpec validation under Node 26.7.0; Xcode validation; `./init.sh`; and diff hygiene
are recorded. The later executable plan SHALL own exact paths and commands without claiming this
documentation amendment executed them.

#### Scenario: Pending Settings acceptance remains bounded
- **WHEN** Task 6.4 is closed
- **THEN** it records the required architecture, platform, quality, strict OpenSpec, Xcode, and
  init evidence for one atomic direct-Shared-composition Settings move
- **AND** it does not infer runtime/device/visual/picker/scanner/playback/iOS-framework behavior.

### Requirement: Back behavior is preserved through modular moves

Shared root arbitration SHALL preserve the existing ordering modal -> edit -> active-page selection -> Now Playing -> route, with exactly one transition per intent and only the active destination eligible. Predictive Back SHALL latch the exact destination/target. Features SHALL own modal/edit state and publish only foremost dismissal; displayed-playlist deletion SHALL remain destination invalidation rather than Back.

#### Scenario: Modular feature state cannot change Back precedence
- **WHEN** a migrated active destination has a foremost modal, edit state, selection, Now Playing, and a route transition
- **THEN** one Back intent dismisses only the modal
- **AND** inactive destination state and playlist deletion do not consume that intent.

### Requirement: Database, resources, and iOS compatibility are preserved

SQLDelight moves SHALL atomically transfer `.sq` files, existing migrations, drivers, and generated package with no schema/name/history changes. Feature resources SHALL move with a module namespace and be verified for Android packaging, desktop runtime, and iOS linking. iOS SHALL export only modules whose declarations enter the Swift/Objective-C public API, and the existing shared framework entry SHALL remain stable.

#### Scenario: A platform-affecting move preserves compatibility
- **WHEN** a migration moves database, resources, expect/actual code, or public iOS declarations
- **THEN** tests verify existing database/migrations/foreign keys and supported platform startup/resource/DI behavior
- **AND** the iOS export allow-list admits only required public declarations.

### Requirement: Library implementation is one unexported callback-first leaf

Task 7.1-7.3 SHALL create exactly one unexported `:feature:library:impl`; existing
`:feature:library:api` SHALL retain its module identity and domain/repository-contract role. The
later corrected-Library authority explicitly authorizes removing API playback conversion/type
residue. The
implementation SHALL target Android-KMP with host tests/resources, JVM, `iosArm64`, and
`iosSimulatorArm64`, preserve current Kotlin package roots, use Android namespace
`com.eterocell.rhythhaus.library.impl`, and use Compose resource namespace
`rhythhaus.feature.library.generated.resources`. It SHALL have no framework binary or iOS export.
Shared SHALL use an implementation-only edge and alone compose/start Koin.

Impl SHALL own Library scanner/indexing, repository implementations/mappings, scan events/progress
implementation models, platform source access/folder picker/path resolver expect/actuals,
`AudioMetadata`/`AudioMetadataReader` and platform metadata actuals, TagLib integration/bindings,
Library leaf home/album/artist/scanning/import UI, browser/grouping/rows/chrome/artwork-collapse
helpers, and Library-local transient rendering state. Core database SHALL remain the sole physical
SQLDelight schema/driver/migration/generated owner. Impl SHALL NOT depend on core playback solely
for queue/restart behavior: `LibraryPlaybackSelection.kt` remains Shared, and Shared adapts ordered
Library track/selection callbacks to PlaybackController policy.

The public surface SHALL be limited to KDoc-complete browse-mode and shared-label values,
callback-first Library home/album-detail/artist-detail/scanning content, plain folder-picker
result/launcher/`rememberPlatformFolderPickerLauncher`, `PlatformSourceAccess`, a scan-service
contract, and `libraryImplementationModule(): org.koin.core.module.Module`. Public signatures may
use Library API/core:model only when unavoidable, Compose runtime/UI/Dp/Modifier, and Module only
for the factory. They SHALL NOT expose Shared route/AppState/Back/navigation/destination types,
PlaybackController/PlaybackState/QueueOccurrence, generated Res, Job, TagLibReader, core database/
SQLDelight, other feature types, or platform-native handles. Every public declaration/property and
function behavior SHALL have KDoc. LibraryAppShell SHALL remain a Shared coordinator; existing
reducers/remember state remain under the explicit Task 7.2 characterization exception.

#### Scenario: Library ownership moves without widening Shared
- **WHEN** the Library implementation is extracted
- **THEN** scanner/source/index/repository/metadata/platform/UI tests move with their subjects and
  Shared retains App scan cancellation/job/publication/source authority/DI/route/Back/selection/
  playback/session/cross-feature adapter tests
- **AND** all platform common expect and Android/JVM/iOS actuals move atomically while preserving
  SAF/persisted permissions/DocumentFile, canonical JVM traversal, and iOS app-local Documents
- **AND** physical SQLDelight inputs and generated ownership remain in core database.

#### Scenario: Library resources and intentional metadata ABI contraction
- **WHEN** Library resources and scanner metadata move
- **THEN** an exhaustive named EN/ZH current-consumer ledger proves exact parity, no missing,
  wrong-owner, or duplicate key, with `scan_complete_format` and
  `adaptive_detail_placeholder` retained by Shared and uncertain shell-rendered keys retained or
  injected as values
- **AND** current `readAudioMetadata(path)` and `AudioMetadata` Shared exports are removed as an
  intentional unsupported ABI contraction, with no impl export or compatibility facade, and source
  consumer, generated Shared-header, MainViewController, and approved ABI checks pass.

### Requirement: Library extraction remains unchecked until bounded evidence

Tasks 7.1-7.3 SHALL remain unchecked until causal RED controls cover absent module/targets,
forbidden edges, Shared API/export, namespace/resource ownership, package/public surface/KDoc,
accidental ABI export, platform actuals, physical SQLDelight ownership, and stale Shared residue;
GREEN preserves scanner cancellation/cleanup/fallback/progress/errors, source behavior, grouping/
order/duplicates, detail invalidation, selection adaptation, queue/restart, and Back through real
production boundaries. Required evidence includes focused feature common/JVM/Android-host/iOS tests
and compiles, retained Shared JVM/iOS, core database integration, Android assemble, desktop compile/
automatable runtime, generic Xcode/simulator checks, TestKit architecture, twice-reused root
architectureCheck, Spotless/Detekt, strict named OpenSpec under Node 26.7.0, AboutLibraries byte
identity, diff hygiene, and `./init.sh`. It SHALL NOT claim physical-device, picker/scanner runtime,
playback-engine runtime, desktop visual launch, or visual/accessibility QA.

#### Scenario: Pending Library acceptance is bounded
- **WHEN** Slice 6 is reviewed
- **THEN** the review confirms one atomic direct-Shared-composition Library move and unchanged
  Library API/MainViewController/approved ABI boundaries
- **AND** it does not infer product/UI changes, package renames, schema/migration changes, iOS
  export, compatibility bridges, or runtime/device/visual behavior from compile/link evidence.

### Requirement: Corrected Library authority is executable

The Library requirements above are amended by the dedicated approved Library design. The retained
API module SHALL depend only on core model and SHALL contain no playback type after
`LibraryTrack.toPlayableTrack()` moves to Shared. Shared SHALL use `commonMainImplementation` for
one unexported impl and retain App/root shell, route/Back/predictive/navigation identities,
`LibraryAppState`, Back-required browse state, selection/page mapping, visible-ID reconciliation,
scroll and bottom-bar policy, playback/session, scan Job/orchestration/publication, cross-feature
composition, Koin startup/total assembly, and `LibraryPlaybackSelection.kt`. Impl SHALL own only
repository/scanner/metadata/TagLib/platform seams/leaf UI/local rendering/resources; no
Presenter/ViewModel/Event/Effect layer is permitted.

Its exact public surface SHALL be the KDoc-complete `PlatformSourceAccess` access/release methods,
internal-constructor `LibraryScanner.scan(source, isCancelled, onProgress)`, `ScanProgress`, folder
picker result/launcher/expect, neutral browse/group/selection-page projections, callback-first
`LibraryHomeContent`/`DrillDownView`, and exactly `public fun libraryImplementationModule(): Module`.
Public signatures SHALL satisfy the dedicated design's prohibited-type and callback rules. The
Android `LibraryDatabaseContext` holder SHALL move physically to core database without changing its
package/name/public setter or application-before-Koin initialization order. The exhaustive EN/ZH
ledger, `selected` unused-removal proof, public KDoc/boundary controls, and intentional metadata
ABI contraction based on the documented `f4ae104`/`30f89ff` consumer history SHALL be enforced.

The exact public UI declarations SHALL be the dedicated design's Kotlin declarations for
`BrowseMode`, `LibrarySelectionPage`, `LibrarySharedLabels`, `LibraryHomeContent`, and
`DrillDownView`, including every parameter name/type/default. They SHALL NOT be generalized into
unnamed parameter groups. Home/detail SHALL use only `Track`, `RhythHausBackdrop`, Compose,
feature values, and primitive callbacks; private occurrence-index-plus-ID keys preserve duplicate
rendering and ordered visible reports; each emits primitive index/offset and has exactly one
required-padding terminal spacer. Shared SHALL resolve unavailable detail before composition and
perform existing route-level Back, while local state follows the exact dedicated state ledger and
`formatDuration` is internal Library code.

#### Scenario: Oracle conversion inventory correction
- **WHEN** Slice 6 is implemented
- **THEN** the six paths previously named are historical/baseline inventory only
- **AND** post-extraction exactly four retained Shared production files own conversion/projection:
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`.
- **AND** moving feature-owned `LibraryHomeContent.kt` returns `Track` callback data and contains no conversion; playlist impl `PlaylistScreens.kt` consumes `playableTracksById` and contains no conversion.
- **AND** Task 7 deletes only `LibraryTrack.kt` method/import residue, adapts/removes `LibraryApiModelsTest.kt` conversion assertions, adapts `PlaylistLifecycleIntegrationJvmTest.kt` to Shared projection, and retains unrelated `MusicModels.kt` `Track.toPlayableTrack()` plus `SearchRouteAdapterJvmTest.kt` use.

#### Scenario: Shared owns every Library playback projection
- **WHEN** Slice 6 removes the Library API conversion
- **THEN** the six listed paths are historical/baseline inventory only:
  `App.kt`, `LibraryAppShell.kt`, moving `LibraryHomeContent.kt`, `LibraryRoutes.kt`,
  `PlaybackSessionCoordinator.kt`, and playlist impl `PlaylistScreens.kt`
- **AND** `PlayableTrack` remains a core-model type; Library API remains core-model-only and exposes
  neither it nor a conversion, while moving leaf UI returns `Track` callback data.
- **AND** Shared `LibraryRoutes.kt` passes `PlaylistDetailScreen` an authoritative
  `playableTracksById: Map<String, PlayableTrack>` with current ID-keyed `associate` behavior;
  playlist detail performs no local map, the map does not represent duplicate occurrences, and
  unchanged `SavedPlaylistPlaybackRequest(occurrences, selectedOccurrenceId)` and `onPlayEntry`
  preserve queue occurrence order, identity, selected occurrence, and callback settlement.
- **AND** playlists impl retains existing core-model API visibility only; no new project/Shared edge
  or callback-payload redesign is introduced, and browser overlays may retain `List<LibraryTrack>`
  metadata input.

The ledger SHALL list exactly the twelve injected label keys, all Shared-only non-injected keys, all
Library-moved keys, and core UI `back`; it SHALL require EN/ZH multiset equality plus duplicate,
missing, wrong-owner, foreign-key, rendered-resource, and selected-absence controls. Metadata SHALL
be internal at `com.eterocell.rhythhaus.library.impl`; headers SHALL omit `SharedAudioMetadata`,
`SharedAudioMetadataReader`, `SharedAudioMetadataKt`, `readAudioMetadata(path:)`, and
`readAudioMetadataPath:` while retaining all named MainViewController symbols. The only Android
holder declaration SHALL be the dedicated core-database path, with old-path/duplicate/visibility/
direct-impl-dependency/init-order negatives.

#### Scenario: Corrected Library implementation is accepted only with causal evidence
- **WHEN** Tasks 7.1-7.3 are implemented
- **THEN** causal production-composable, scanner/source/picker/repository/metadata, process-init,
  header-contraction, dependency/namespace/resource/Koin, and malformed-negative controls prove the
  dedicated design across required targets
- **AND** no task is checked until all bounded platform/quality/ABI/OpenSpec evidence is recorded.

### Requirement: Library literal authority has no selectable alternatives

The dedicated Library design's Kotlin declarations SHALL apply exactly. Every Home/Detail parameter
SHALL be required with no default; use `currentTrackId` and no `isPlaying` or `onPlayPause`; both
SHALL accept the required suspend artwork loader and preserve its documented behavior. Impl SHALL
use API core-model/core-UI/Compose signature dependencies and implementation-only remaining UI
dependencies; Shared SHALL use implementation-only impl dependency and no export. The authoritative
track/group ordering, occurrence keys, state/reset, one-spacer, routes, KDoc controls, and twelve
field resource resolver table SHALL apply.

`libraryImplementationModule()` SHALL be the only public Koin-shaped declaration; all stated
bindings SHALL be singletons and use one concrete platform object for public access/internal scan
roles. Metadata retirement is intentional after the documented f4ae104/30f89ff history, not an
accidental-export claim. Android holder compilation/transitivity/direct-androidMain dependency/sole
path/init ordering and exact header absence/retention controls SHALL apply.

#### Scenario: Literal Library boundary rejects substituted plumbing
- **WHEN** implementation supplies a defaulted or renamed UI parameter, an `isPlaying` or
  `onPlayPause` leaf argument, a second scanner/access/TagLib singleton, nonliteral artwork loading,
  or an alternative holder/export path
- **THEN** public-surface, production-composable, Koin, Gradle, and ABI controls fail
- **AND** only the dedicated declarations, singleton graph, holder path/order, and intentional
  metadata-retirement symbols are accepted.

### Requirement: Library detail formatting and exposed dependency graph are exact

Impl SHALL declare `api` dependencies on Library API, core model, core UI, and Compose artifacts
needed by public signatures. `DrillDownView` SHALL accept required `LibraryDetailSummary`, never a
formatted subtitle; Shared SHALL supply only raw title/route/tracks/counts and feature SHALL resolve
the moved unknown-artist/detail subtitle keys. The dedicated internal scanner/event/access/metadata
signatures, all-single Koin declarations with `===` identity, 12-field resource resolver table,
public metadata retirement, and retained Shared consumers SHALL apply exactly.

Because the sole public factory returns Koin `Module`, impl SHALL declare exactly
`api(libs.koin.core)`; Koin Compose and non-signature Koin/UI dependencies SHALL remain
implementation-only. The dedicated signature authority is binding pseudocode. Production source
SHALL contain declaration-specific KDoc on every listed public declaration/member/constructor
property/function and `@param` entries for each public callable parameter/callback; KSP SHALL inspect
source and reject missing or generic-placeholder documentation.

#### Scenario: Detail formatting remains feature-owned
- **WHEN** album and artist detail routes are adapted
- **THEN** Shared supplies both raw summary variants and no detail formatter/resource handle
- **AND** feature tests render EN/ZH detail copy for null/non-null artist and unchanged counts while
  Koin/resource controls reject second objects, wrong API scope, or a moved-key Shared import.

#### Scenario: Public Koin type receives API scope
- **WHEN** Gradle and KDoc governance inspect Library impl
- **THEN** `libs.koin.core` is an `api` dependency because factory `Module` is public
- **AND** implementation-only Koin core, Koin Compose public leakage, source-block-comment KDoc, or
  missing callable `@param` documentation fails.

### Requirement: Thin shared owns only facade responsibilities

The `:shared` module SHALL own only `App()` composition, the root shell, cross-feature route/Back
arbitration, lifecycle, Koin assembly, and the stable `MainViewController` iOS facade, plus the
intentionally-retained session coordination/persistence, theme persistence, package-stable playback
engine factory, playlist-backup ABI seam, Library selection integration, track selection state/bar,
Now Playing shell placement, and shared formatting helpers. A thin-shared inventory test SHALL assert
the exact `:shared` source-file set and fail on any migrated implementation ownership. Dead code left
by extraction (the unused `Logger` Kermit singleton and the unused `Platform`/`getPlatform`
expect-actual family) SHALL be removed without introducing any bridge dependency. A
real-structure-only scaffold SHALL generate a requested feature-module structure; package renames and
Dependency Analysis Gradle Plugin evaluation SHALL remain deferred.

#### Scenario: Thin-shared inventory is asserted
- **WHEN** the architecture convention inventory test inspects `:shared`
- **THEN** the approved facade/retained source set is present exactly
- **AND** any source file outside that set, or any approved file missing, fails the test.

#### Scenario: Dead migrated ownership is removed without a bridge
- **WHEN** the unused `Logger` and `Platform`/`getPlatform` declarations are deleted
- **THEN** `:shared` compiles on all targets with no new dependency edge
- **AND** the architecture allow-list/cycle checks remain GREEN.

#### Scenario: Scaffold generates only real requested structure
- **WHEN** the scaffold instantiates a new feature module
- **THEN** it produces only the requested module skeleton (build.gradle.kts, source dirs, package
  root, README, KDoc placeholder) with no speculative packages or full Gradle plugin
- **AND** package renames and Dependency Analysis Gradle Plugin evaluation remain deferred.
