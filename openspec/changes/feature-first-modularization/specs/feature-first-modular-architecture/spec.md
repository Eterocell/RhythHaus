## ADDED Requirements

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
- **THEN** shared retains composition, shell/routes/Back, lifecycle, Koin assembly, Settings layout, generic injected `cancel`, and selection-bar composition
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
