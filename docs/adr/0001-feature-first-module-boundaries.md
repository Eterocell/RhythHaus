# ADR 0001: Feature-First Module Boundaries

## Status

Accepted

## Context

RhythHaus centralizes feature implementations, composition, SQLDelight, and
platform seams in `:shared`. Modularization must preserve behavior, Kotlin package
declarations, the Back contract, and the stable iOS entry without keeping `:shared`
as a dependency hub.

## Decision

Adopt a demand-driven, feature-first KMP graph through buildable contract-first
slices.

```text
:androidApp, :desktopApp, iosApp -> :shared
:shared -> feature implementations, feature APIs, and required core modules
feature implementation -> own API, allowed feature APIs, and required core modules
feature API -> allowed core API/model only
core -> allowed core dependencies only
```

`:shared` is the sole composition root. It owns `App()`, root shell, cross-feature
route and Back arbitration, lifecycle, Koin assembly, and stable iOS
`MainViewController`. Applications depend on `:shared`; core and feature modules
may not depend on `:shared` or an app. Feature implementations may not depend on
another feature implementation. Cross-feature coupling uses explicit feature APIs.

- `:core:model` contains only immutable cross-feature projections.
- `:core:ui` contains reusable primitives, theme, and artwork, never feature state.
- `:core:database` owns one physical SQLDelight schema, drivers, migrations, and
  generated database.
- `:core:platform` exists only for capabilities reused by two or more domains.
- `:core:playback` is added when playback engine/contracts move. `:core:navigation`
  is added only after evidence proves a common destination-scoped Back contract.

Library owns scanner, source access, indexing, repositories, UI, and transient
state. Playlists owns repository, edit, backup, and UI. Feature repositories and
mappings stay feature-owned. A feature implementation exposes a Koin `Module` only
when it owns injectable bindings; UI-only modules use composable/function entry
points and do not create empty modules. Only `:shared` assembles and starts Koin.

Task 6.4 will assign leaf Settings and About presentation to one unexported
Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64` `:feature:settings` module. It will have one common
implementation; no API split, Koin module, `iosMain` production source, or iOS export; preserved
Kotlin package and Android namespace `com.eterocell.rhythhaus.settings`; and resource namespace
`rhythhaus.feature.settings.generated.resources`. Its sole project dependency will be
`api(:core:ui)`. Public Compose/runtime/UI dependencies required by its public declarations will be
API, while Foundation/resources/icons/Miuix/AboutLibraries/coroutines will be implementation-only.
Shared will use only `commonMainImplementation`, never `api`, and will not export Settings. No
Settings dependency on Shared, apps, core database/platform/playback, taglib, a feature module,
Koin, DataStore, or Library API is permitted.

The Settings public boundary will be only KDoc-complete `SettingsSharedLabels(title, addMusicFolder,
folderPickerUnavailable, clearLibrary, cancel, remove)`, `SettingsSourceItem(id, displayName,
accessAvailable, hasBeenScanned)`, `SettingsScreen`, `SettingsAboutScreen`, and
`OpenSourceLibrariesScreen(readCatalogJson: suspend () -> String, ...)`; it exposes no Shared,
Library, Playlist, generated foreign resource, route/Back, repository/scanner/launcher/controller/
job/Koin/DataStore type. Shared will map authoritative sources and supply scalar state, callbacks,
playlist-backup content, nullable scanning content, and nullable clear-library-dialog slots. Settings
will own only rendering, source-removal dialog visibility, and About retry generation, without a
Presenter/ViewModel/Event/Effect rewrite. Shared retains source mutations/guards/errors,
picker/scanning/clear-library orchestration, routes/Back/dismissal, playlist backup control, and
theme persistence/actuals/Koin/root theme application; `RhythHausThemeMode` and palettes remain
core UI. The planned `SettingsScreen` will include picker visibility/availability, mutation and
imported-track booleans, `onRequestClearLibrary`, and a nullable clear-dialog slot. Shared will
resolve source IDs against current authority at invocation, no-op stale IDs, and recheck guards.

Settings will own its appearance/theme, source-management-only, About/AboutLibraries, logo, and
remove-source-dialog resources. Shared will retain/inject Settings route/add-folder/picker-unavailable/
clear-library/generic cancel/remove/clear-library-dialog wording. `RhythHausBuildInfo`
generation/model/verification will move to Settings, while app-wide AboutLibraries generation/config,
manual TagLib attribution, and checked-in Shared catalog JSON remain Shared. Settings will parse and
render supplied JSON, retry read/parse failures, and rethrow cancellation.

The planned Shared resource set will retain `scanning`, `scan_progress_format`,
`scan_complete_format`, `folder_picker_error_access`, `folder_picker_error_select`,
`folder_picker_error_prepare`, and `folder_picker_no_folder_selected` for scanning-card, App, and
platform-picker consumers; these keys will not cross into Settings. Settings will own
`manage_music` with the feature-only source-management resources, subject to EN/ZH parity and
missing/wrong-owner/duplicate/logo controls.

The approved playlists implementation is unexported `:feature:playlists:impl`, targeting
Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64`. It owns saved-playlist/playback-queue UI,
immutable Playlist state/action/reducer/owner equivalents, repository implementations/Koin binding,
backup codec/service/state/UI, a neutral common document-launcher contract, public Android/JVM
launcher factories, and feature
resources. Direct project edges are playlists API, Library API, core model/playback/ui/platform,
and implementation-only core database; other Gradle scopes follow exhaustive public signatures.
Generated DB/SQLDelight/generated Res/shared navigation or shell types are excluded from public
signatures. Koin is excluded except for the public binding-module factory returning
`org.koin.core.module.Module` for shared assembly; shared alone assembles and starts Koin. Both
`com.eterocell.rhythhaus.library` and
`com.eterocell.rhythhaus.playlistbackup` remain governed and package-stable.

Contract-first migration preserves Kotlin packages. Move a stable contract before
its implementation. A failed atomic slice must not acquire a
`feature -> shared -> feature` bridge. Resources move with their feature and have a
feature-owned module namespace only when the module build setup exposes one. Until
then, resource ownership is determined by the owning module and recognized KMP/Compose
source-set locations, never invented top-level folders. Package roots may be shared by
modules; validation is exact-root or root-plus-dot and validates permitted declarations
per module, not global uniqueness. Core model keeps its current root; core UI keeps
`ui` and `theme`; core database and both Library/Playlists API/impl retain their current
`library` package roots.

`:shared` is the sole transitional physical SQLDelight owner. Task 1.3 centralizes only
SQLDelight Gradle application/configuration in `build-logic.sqldelight`: build logic uses
`implementation("app.cash.sqldelight:gradle-plugin:2.3.2")`, and `:shared` applies that
convention instead of independently applying SQLDelight. This grants the checker the same
implementation classpath to type-safely inspect public `SqlDelightExtension` models. It
does not move physical ownership or alter the existing database configuration, schema,
migrations, package, database name, or platform driver behavior. Task 3.1 transfers that
single physical owner atomically to `:core:database`, including configured database/plugin,
real-layout `.sq`/`.sqm`/schema artifacts, drivers, and generated package. Readmes,
arbitrary filenames, and runtime/coroutine driver consumers do not establish ownership.

Back arbitration stays in `:shared`: modal, edit, active-page selection, Now
Playing, then route. Features own modal/edit state and publish only their foremost
dismissal: exactly one already-resolved immutable surface, modal before edit, with stable
identity per appearance and new identity per re-presentation. Shared accepts only the active
destination and identity-guards registration/disposal. Predictive Back latches exact destination
and appearance; cancellation does not dismiss; completion revalidates and dispatches at most once;
rejection/staleness clears the pending session without fallthrough. Displayed-playlist deletion
remains authoritative exact-destination invalidation after confirmed absence, never Back;
failed/stale/replayed deletion does not invalidate and unrelated state is preserved.

A dispatched non-predictive transition stays in flight until authoritative state reports its exact
latched target inactive or the target explicitly rejects completion. Repeated Back is suppressed
while in flight, and callback return never settles or releases suppression. Rejection releases the
intent without treating the target as settled; any later Back is a new intent. Predictive latch,
cancellation, and no-fallthrough semantics remain unchanged.

Task 5.2 moves adapters only: `:core:database` remains sole physical owner with no `.sq`, `.sqm`,
schema, migration, generated DB, driver, database-name, or FK changes. Serialization/revision/
cancellation, backup exclusivity, exact 4 MiB limits, mappings, stale-library rejection,
transactional import, exactly-once native completion, and playback engine/session/lifecycle/root
playback ownership remain unchanged. Shared retains composition, shell/routes/Back, lifecycle,
Koin assembly, Settings layout until Task 6.4, generic injected `cancel`, and selection-bar composition. The
feature owns embeddable backup sections/dialogs and all playlist/queue/backup EN/ZH text once.

The public implementation surface is limited to the binding-module Koin factory and shared-needed
state/action/result, owner, composable, dismissal, backup orchestration, and launcher contracts,
all with declaration-specific behavioral KDoc; other helpers are internal/private. The Shared iOS
facade retains the exact executable ABI ledger in the canonical Task 5.2 Superpowers design: package
`com.eterocell.rhythhaus.playlistbackup`, framework `Shared`, status values, Completion/Provider
names/signatures/nullability, Bridge singleton access, MIME/max-size constant exports, and Swift
`Int32` interop; feature owns no iOS actual/source or Shared edge. Shared owns the common `expect`,
Android/JVM delegates, and retained iOS actual/ABI adapter; the feature is not exported, and Swift
files remain app-owned. The already-characterized immutable `PlaylistState`,
`PlaylistStateAction`, reducer, `PlaylistStateOwner`, and backup immutable state/reducer are a
Task 5.2-only equivalent and do not alter the general Presenter/ViewModel rule. No visual/
product redesign, state-framework rewrite, navigation/core-navigation or generic document module,
package rename, database/playback ownership change, illegal bridge/service-locator/implementation
coupling, Swift redesign, resource duplication, or runtime/device claim from compile/link/tests is
authorized. ADR 0002 remains untouched.

The approved Search move is one unexported `:feature:search` leaf module, not an API/implementation
pair. It targets Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`, has one common implementation,
no platform source or iOS export, preserves `com.eterocell.rhythhaus.search` as its Android
namespace and Kotlin package, and uses resource namespace
`rhythhaus.feature.search.generated.resources`. Its public dependencies are Library API and required
Compose runtime/UI; core UI, Foundation, resources, and Miuix are implementation details. Shared
declares exactly `implementation(projects.feature.search)`, never `api`, and does not export Search.
Search has no forbidden outbound edge to Shared, core playback, database, core platform, taglib,
another implementation, or app, and has no Koin or iOS export. `feature/search/README.md` is not
part of this migration.

Search exposes exactly explicit-public, declaration-specific-KDoc `SearchSharedLabels` and
callback-first `SearchContent`: Library API tracks; primitive current-track/playing state; plain
Shared labels and a composable selection-label formatter that resolves `select_track_format` with
structured Compose `stringResource` during row composition without a generated resource handle;
selection/visible-ID/scroll/ordered-play/dismiss callbacks; a Shared playing-indicator slot; and
only `Dp`/`Modifier` layout defaults. It exposes no
Shared/generated-resource/playback-controller-or-state/queue/repository/Koin/platform type. Shared
remains sole facade and directly composes `SearchContent` from `LibraryRoutes`; no Shared
compatibility screen or unused TagLib reader remains. Shared retains route/Back, selection
reconciliation/clear, scroll storage, playback queue/restart/dismiss, bottom-bar/Now Playing policy,
and `EqualizerStrip`; Search owns local query/filter/render/focus/count/empty behavior and Search
row interaction. Search uses a private rendering identity of filtered occurrence index plus track
ID, never `track.id` alone, which is unique for duplicate rows and does not alter `LibraryTrack`,
selection IDs, visible-ID sequence, playback queue order, or duplicate semantics. Search-only
strings are feature-owned exactly once, while Shared injects title, clear, Now Playing, and
composable select-track formatting as values; no resource handles cross. The
normative boundary is [the approved Search design](../superpowers/specs/2026-08-07-search-feature-extraction-design.md).

## Consequences

The graph retains a stable application and iOS facade while making ownership and
allowed dependencies explicit. API modules are introduced only for real stable
contracts. No empty modules, `:core:network`, speculative core/navigation modules,
or empty `UiState`, `UiEvent`, `UiEffect`, or Presenter/ViewModel scaffolding are
allowed.

`architectureCheck` validates actual Gradle `ProjectDependency` edges and cycles against
a code-owned allow-list. Shared build logic owns a normalized immutable
`ArchitectureModelRegistry`; plugin-owning conventions publish public-model facts and the
root task consumes the records, never separately loaded extensions. Android uses public Android
Components callbacks only for main-production static resource roots; module namespace is read
from concrete public Android DSL `CommonExtension.namespace` (`ApplicationExtension` or
`LibraryExtension`), not an AGP 9.3 `Variant`. Test, `androidTest`, and test-fixture components
are excluded, as are casts, reflection, task/artifact internals, and per-variant namespace
claims. Compose publishes standard/custom roots and convention-declared configured namespaces;
blank declarations remain invalid registry facts for deterministic `ARCH-RESOURCE` diagnostics
and are not passed to Compose Resources. It makes no effective-namespace or independent-
introspection claim. The root JVM `:architecture-processor`, not the
convention-plugin JAR, reports package/KDoc violations through convention-recorded production
KSP wiring and provides no root-task facts. Generic `ksp` is tooling only for single-platform
JVM/Android; arbitrary configuration names are not trusted. The processor has supported
compilation-local incremental scope only, filters generated/test/local boundaries where
possible, and reports sorted deduplicated relative paths rather than aggregating a project.
SQLDelight public models are similarly convention-published. Reflection, internals,
classloader probing, and build-script parsing are forbidden. Kotlin compilation remains the
import-resolution authority. See the stable architecture-checker design.
Android applications publish exact identities only from public AGP 9.3.1 `ApplicationVariant`
test-component `Component.compileConfiguration` and `Component.runtimeConfiguration` objects
for `androidTest`, `unitTest`, `deviceTests`, and `hostTests`. Android-KMP libraries publish them
only from public `KotlinMultiplatformAndroidLibraryTarget.compilations` and public
`KotlinMultiplatformAndroidHostTestCompilation`/
`KotlinMultiplatformAndroidDeviceTestCompilation`
`compileDependencyConfigurationName` and `runtimeDependencyConfigurationName`. After
normalization, only a direct self `ProjectDependency` whose consumer/configuration exactly
matches a published identity and is the single distinguishable direct self record in that
configuration is excluded; otherwise none is excluded. `Configuration.isCanBeDeclared` provides
no dependency provenance and is not a
predicate. Authored dependencies in supported declarable buckets, including explicit authored
self-dependencies, remain enforcement inputs, with the latter emitting exact `ARCH-EDGE` and
one-node `ARCH-CYCLE`. An equal authored mutation on the exact AGP-owned configuration can
collapse into AGP's set record and is unsupported/outside the checker guarantee because no
public provenance API exists. Name inference, blanket test filtering, attribute guessing, AGP
internals/reflection, and task/artifact/output/resolved-classpath inspection are prohibited. The
real Android app RED fixture captures the three AGP synthetic self edges and cycle before repair;
GREEN retains canonical main resources and the authored self-edge fixture remains the negative
control.
Dependency Analysis Plugin adoption is deferred until graph stabilization and separate
version/KMP compatibility evaluation.

## Slice 6 Library Boundary

The approved Library move creates exactly one unexported Android-KMP
`:feature:library:impl`, targeting Android-KMP with host tests/resources, JVM,
`iosArm64`, and `iosSimulatorArm64`. Existing `:feature:library:api` retains its module identity
and domain/repository-contract role; the amendment below authorizes removal of its playback
conversion/type residue. The implementation preserves current package roots, uses Android
namespace `com.eterocell.rhythhaus.library.impl`, and uses resource namespace
`rhythhaus.feature.library.generated.resources`; it is never exported from Shared or iOS. It owns
scanner/index/repository implementations and mappings, metadata/TagLib integration, source access,
folder picker/path resolver seams and actuals, Library leaf UI/helpers/resources, and local transient
rendering state. Core database remains the sole physical SQLDelight owner.

Library's public surface is callback-first: browse mode, Shared labels, home/album/artist/scanning
content, plain folder-picker result/launcher/remember contract, `PlatformSourceAccess`, scan
service, and the KDoc-complete `libraryImplementationModule(): Module` factory. No Shared route,
AppState, Back/navigation/destination, playback controller/state/queue, generated resource, Job,
TagLib, database, other feature, or native handle may appear in public signatures. Shared retains
`LibraryPlaybackSelection.kt` and adapts ordered feature callbacks to playback, so the feature does
not gain a core playback dependency. LibraryAppShell is a Shared coordinator, not a wholesale move;
existing reducers/remember state remain under the explicit Task 7.2 characterization exception.

Move Library-only EN/ZH resources once using an exhaustive current-consumer ledger. Shared retains
App/shell-rendered and cross-feature labels including `scan_complete_format`,
`adaptive_detail_placeholder`, and keys used by Settings, Now Playing, Search, selection, and
playback shell; uncertain Shared-rendered keys remain Shared or are injected as values. The
current `readAudioMetadata`/`AudioMetadata` exports are intentionally retired under the documented
f4ae104/30f89ff bridge-consumer history; move them into impl without an export or compatibility facade. Require
source-consumer, generated-header, approved-ABI, platform-actual, resource parity/ownership,
physical-database-owner, and stale-Shared-residue controls.

### Amendment: Executable Library Boundary

The dedicated 2026-08-10 Library design is the authoritative executable amendment for Slice 6 and
preserves accepted text above as historical context. It corrects the API statement: module identity
is unchanged, but `:feature:library:api` stays a `:core:model`-only domain/repository contract and
loses `LibraryTrack.toPlayableTrack()` and every playback type; Shared keeps conversion and
`LibraryPlaybackSelection.kt`. The sole public Koin declaration is
`public fun libraryImplementationModule(): Module`.

Shared retains root/shell/route/Back/predictive/navigation identity, `LibraryAppState`, browse state
needed by Back, selection/page/visible-ID/scroll/bottom-bar policy, playback/session, scan
Job/orchestration/publication, cross-feature composition, and total Koin assembly. Impl owns only
the repository/scanner/metadata/TagLib/platform seams/leaf UI/resources and local rendering state.
The Android `LibraryDatabaseContext` holder moves physically to core-database Android while
preserving its public setter and initialization-before-Koin order; no Koin Android/native Context
contract is added. The explicit resource ledger, exact scanner/picker/UI contracts, and approved
intentional removal of the formerly Swift-consumed metadata ABI are binding as written in the
dedicated design.

That design's literal UI declarations are the only Slice 6 public UI authority: `BrowseMode`,
`LibrarySelectionPage`, `LibrarySharedLabels`, `LibraryHomeContent`, and `DrillDownView`. No
generalized parameter-group reading is permitted. It fixes local versus Shared state, duplicate
occurrence identity, primitive report callbacks, required one-spacer clearance, resolved-unavailable
detail Back behavior, and internal `formatDuration`. Its exhaustive EN/ZH ledger distinguishes all
twelve injected labels, all Shared-only keys, all moved keys, and `selected` absence; names do not
create ownership. The API's module/repository-domain role remains, while only the historic
`toPlayableTrack` playback residue is intentionally removed. Metadata becomes internal impl code;
the exact forbidden/retained generated-header symbols and the exact single core-database Android
`LibraryDatabaseContext` path/init-order controls are mandatory.

### Third-Review Literal Amendment

The dedicated design controls every alternative: Home and Detail have only its required no-default
parameters, use `currentTrackId` only, omit `isPlaying` and `onPlayPause`, and receive the required
artwork loader. Public core-model/core-UI/Compose signature dependencies are API; other UI libraries
are implementation-only. Its precise authoritative-sequence/group ordering, duplicate occurrence,
state/reset, one-spacer, resource resolver table, Koin all-single/same-concrete-role graph, Android
variant/direct-dependency controls, and source/header-proven intentional metadata retirement apply.
The old accidental-export characterization is historical and superseded by the `f4ae104`/`30f89ff`
consumer history. API module/repository-domain identity remains; only `toPlayableTrack` playback
residue is intentionally removed.

### Fifth-Review Shared Playback Projection Amendment

The complete baseline conversion inventory is six production sites: Shared `App.kt`,
`LibraryAppShell.kt`, moving `LibraryHomeContent.kt`, `LibraryRoutes.kt`, and
`PlaybackSessionCoordinator.kt`, plus playlist impl `PlaylistScreens.kt`. After extraction,
retained Shared adapters perform every `LibraryTrack`-to-`PlayableTrack` conversion; moving leaf UI
returns `Track` callback data. `PlayableTrack` remains `:core:model`, never `:core:playback`, and
Library API remains core-model-only with no playback type or conversion.

Shared `LibraryRoutes.kt` supplies playlist detail an authoritative
`playableTracksById: Map<String, PlayableTrack>`. `PlaylistDetailScreen` consumes that map directly
and has no local mapper. The ID map uses current `associate` duplicate-key semantics, while
unchanged `SavedPlaylistPlaybackRequest(occurrences, selectedOccurrenceId)` preserves playlist
occurrence identity/order and unchanged `onPlayEntry` carries it. Browser overlays may still use
`List<LibraryTrack>` metadata. Playlists impl retains existing `api(projects.core.model)` visibility;
there is no new project edge, Shared edge, or callback redesign. Causal checks cover projection
fields/artwork bytes, direct detail input, occurrence order/selection, and failure/settlement.

### Fourth-Review Literal Amendment

### Oracle Conversion Inventory Correction

The six paths named above are historical/baseline inventory only. Post-extraction exactly four retained Shared production files own conversion/projection: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`. Moving feature-owned `LibraryHomeContent.kt` returns `Track` callback data and contains no conversion; playlist impl `PlaylistScreens.kt` consumes `playableTracksById` and contains no conversion. Task 7 deletes only `LibraryTrack.kt` method/import residue, adapts/removes `LibraryApiModelsTest.kt` conversion assertions, adapts `PlaylistLifecycleIntegrationJvmTest.kt` to Shared projection, and retains unrelated `MusicModels.kt` `Track.toPlayableTrack()` plus `SearchRouteAdapterJvmTest.kt` use.

Impl exposes Library API/core model/core UI/signature Compose APIs through `api`, and keeps the
remaining declared dependencies implementation-only; Shared retains API Library contracts and an
implementation-only impl edge. Detail receives only required raw `LibraryDetailSummary`; feature
formats its moved detail strings. The dedicated internal scanner/event/access/metadata declarations,
exact all-single no-wrapper Koin graph, full twelve-field resolver table, source-level metadata
retirement, and retained Shared resource consumers are binding. No canonical Slice 6 text authorizes
an alternate formatter, API scope, scanner object, or resource owner.

Because the sole public Koin factory returns `Module`, impl uses exactly `api(libs.koin.core)`;
Koin Compose and all other Koin/UI dependencies absent from public signatures remain implementation-
only. The dedicated signature authority is pseudocode, while production-source KDoc is mandatory on
every public declaration/member/constructor property/function and uses `@param` entries for each
callable parameter/callback; KSP checks source, rejecting missing or placeholder documentation.
