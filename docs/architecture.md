# RhythHaus Architecture

## Scope

RhythHaus is migrating from a monolithic KMP `:shared` module through buildable,
feature-first, contract-first slices. This is the canonical module and ownership
policy. ADR 0001 defines boundaries; ADR 0002 defines the shared iOS export policy.

## Module Graph

```text
:androidApp, :desktopApp, iosApp
        |
     :shared (composition and iOS facade)
        |
  +-----+---------------------+
  |                           |
:core:*                   :feature:*:impl
                              |
                         :feature:*:api
```

Applications depend on `:shared`. `:shared` alone composes implementation modules
and owns `App()`, root shell, lifecycle, cross-feature route/Back arbitration, Koin
assembly, and stable `MainViewController`. Core and feature modules never depend on
`:shared` or an app. A feature implementation never depends on another feature
implementation; cross-feature interaction is only through an explicit feature API.

## Ownership

| Area | Owner |
| --- | --- |
| Immutable cross-feature projections | `:core:model` |
| Reusable primitives, theme, artwork abstractions | `:core:ui` |
| Physical SQLDelight schema, drivers, migrations, generated database | `:core:database` |
| Capability reused by at least two domains | `:core:platform` when justified |
| Playback engine and contracts | `:core:playback` when extracted |
| Scanner, source access, index, repository, UI, transient state | Library feature |
| Playlist repository, edit flow, backup, UI | Playlists feature |
| Settings and About presentation | `:feature:settings` |
| Composition, root shell, cross-feature navigation and Back | `:shared` |

`core:ui` never owns feature UI state. Repositories and mappings remain in their
feature implementation, even after `:core:database` becomes the sole SQLDelight
owner. Stateful screens use immutable `UiState`, `UiEvent`, and `UiEffect` with a
Presenter/ViewModel when state warrants one; stateless UI gets no empty types.

The approved playlists migration creates unexported `:feature:playlists:impl` for
Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`. It owns saved-playlist/playback-queue UI,
immutable domain-named Playlist state/action/reducer/owner equivalents, repository
implementations/Koin binding, backup codec/service/state/UI, a neutral common document-launcher
contract, public Android/JVM launcher factories, and feature resources. Shared owns the common
`expect`, Android/JVM delegates, and retained iOS actual/ABI adapter; the feature owns no iOS
actual/source or Shared edge. Its direct allowed project edges are playlists API,
Library API, core model/playback/ui/platform, and implementation-only core database. Other Gradle
scopes are derived from exhaustive public signatures; generated DB/SQLDelight/generated
`Res`/shared navigation or shell types cannot be public signature types. Koin is forbidden except
for the public binding-module factory returning `org.koin.core.module.Module` for shared assembly.
Governance preserves
`com.eterocell.rhythhaus.library` and `com.eterocell.rhythhaus.playlistbackup`. The narrow public
implementation surface is the binding-module Koin factory plus shared-needed state/action/result/
owner/composable/dismissal/backup/launcher contracts, each with behavioral KDoc; all other
helpers are internal/private.
The already-characterized immutable `PlaylistState`, `PlaylistStateAction`, reducer,
`PlaylistStateOwner`, and backup immutable state/reducer are the Task 5.2-only equivalent and do
not relax the architecture-wide Presenter/ViewModel rule for other stateful feature migrations.

The approved Search migration creates one unexported `:feature:search` leaf module targeting
Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`, with one common implementation, no API split, and
no iOS export. Its Android namespace and Kotlin package are `com.eterocell.rhythhaus.search`; its
resource namespace is `rhythhaus.feature.search.generated.resources`. Its only public declarations
are explicit-public, declaration-specific-KDoc `SearchSharedLabels` and callback-first
`SearchContent`: Library API tracks, primitive playback state, plain Shared labels and a composable
selection-label formatter that resolves `select_track_format` through structured Compose
`stringResource` during row composition without a generated resource handle, selection/visible-ID/
scroll/play/dismiss callbacks, a playing-indicator slot, and
only `Dp`/`Modifier` layout defaults. No Shared/generated-resource/playback-controller-or-state/
queue/repository/Koin/platform/database/TagLib/other-implementation type is public. Library API
and required Compose runtime/UI are public dependencies; core UI, Foundation, resources, and Miuix
are implementation-only. Shared declares exactly `implementation(projects.feature.search)`, never
`api`, and does not export Search. Search has no edge to Shared, core playback/database/platform,
taglib, Koin, another implementation, app, or iOS export. `feature/search/README.md` is not part
of this migration.

Shared directly composes `SearchContent` from `LibraryRoutes` and remains the sole facade. It owns
route/Back, selection reconciliation/clear, scroll storage, playback queue/restart/dismiss,
bottom-bar/Now Playing policy, and `EqualizerStrip`; Search owns query/filter/render/focus/count/
empty behavior and Search-row interaction. For duplicate IDs, Search uses only an internal
rendering identity of filtered occurrence index plus track ID, never `track.id` alone; it does not
alter `LibraryTrack`, selection IDs, visible-ID sequence, playback queue order, or duplicate
semantics. Search owns exactly its five Search-only localized keys. Shared injects title, clear,
Now Playing, and composable select-track formatting as values; no resource key duplication or
generated handle crosses the boundary. The normative Search boundary is
[the approved Search design](superpowers/specs/2026-08-07-search-feature-extraction-design.md).

The approved Settings migration creates one unexported `:feature:settings` leaf module targeting
Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`, with one common implementation, no API split, no
Koin module, no `iosMain` production source, and no iOS export. Its Kotlin package and Android
namespace are `com.eterocell.rhythhaus.settings`; its resource namespace is
`rhythhaus.feature.settings.generated.resources`. Settings has exactly `api(:core:ui)` as a
project dependency. Public Compose/runtime/UI dependencies required by its public API are API;
Foundation/resources/icons/Miuix/AboutLibraries/coroutines are implementation-only. Shared declares
only `commonMainImplementation`, never `api`, and does not export Settings. Settings has no edge to
Shared, apps, core database/platform/playback, taglib, any feature, Koin, DataStore, or an iOS
export, and has no Library API dependency.

Settings exposes only KDoc-complete `SettingsSharedLabels(title, addMusicFolder,
folderPickerUnavailable, clearLibrary, cancel, remove)`, `SettingsSourceItem(id, displayName,
accessAvailable, hasBeenScanned)`, `SettingsScreen`, `SettingsAboutScreen`, and
`OpenSourceLibrariesScreen(readCatalogJson: suspend () -> String, ...)`. Shared maps authoritative
Library sources to the primitive projection and owns source-ID adaptation, mutation guards/errors,
scanner/folder-picker/clear-library policy, routes/Back/dismissal, and clear-library content.
`SettingsScreen` receives scalar state, projections, callbacks, playlist-backup and nullable scan
slots, `sourcePickerActionVisible`, `sourcePickerAvailable`, `mutationsEnabled`,
`hasImportedTracks`, `onRequestClearLibrary`, and a nullable clear-library-dialog slot. Settings owns leaf rendering, source-removal dialog
visibility, and About retry generation only; this Task-6.4 ruling introduces no Presenter/ViewModel/
Event/Effect scaffold. Playlist backup stays playlists implementation-owned and is embedded by
Shared. ThemePreferenceStore, Android/iOS/JVM actuals, system dark preference, Koin binding,
selected-mode persistence/collection, and root theme application stay Shared; theme mode and
palettes stay core UI.

Settings owns appearance/theme, source-management-only, About/AboutLibraries, logo, and
remove-source-dialog resources. Shared retains/injects `settings`, `add_music_folder`,
`folder_picker_unavailable`, `clear_library`, generic `cancel`/`remove`, and clear-library strings
because Shared renders that dialog. No generated resource handle crosses the boundary.
`RhythHausBuildInfo` generation/model/verification moves to Settings. AboutLibraries generation,
configuration, manual TagLib attribution, and checked-in Shared catalog JSON remain Shared;
Settings parses/renders caller-supplied JSON, retries read/parse failures, and rethrows cancellation.
The normative boundary is [the approved Settings design](superpowers/specs/2026-08-07-settings-feature-extraction-design.md).

Shared resolves emitted source IDs against latest authoritative `librarySources` only at callback
invocation, treats stale/missing IDs as no-op, then reevaluates initial-publication and scan/job
guards through its existing error path. It owns clear dialog visibility/dismiss/guarded confirm;
Settings renders clear only for imported tracks and only requests it while mutations are enabled.
The approved design contains the exhaustive EN/ZH shared/feature resource ledger and requires
per-locale parity, missing/wrong-owner/duplicate/logo controls. About requires non-empty parsing,
retry-generation Loading/current-loader behavior on `Dispatchers.Default`, exact cancellation
rethrow, replacement-loader use, and stale-generation suppression.

Shared retains `scanning`, `scan_progress_format`, `scan_complete_format`,
`folder_picker_error_access`, `folder_picker_error_select`, `folder_picker_error_prepare`, and
`folder_picker_no_folder_selected` because Shared scanning-card, App, and platform-picker consumers
render them; they do not cross into Settings. Settings owns `manage_music` with its source-management
resource set under the same EN/ZH parity and ownership controls.

## Contracts And Composition

Every migration is contract-first: relocate an explicit stable contract before its
implementation, keep the slice buildable, and preserve Kotlin package declarations.
Package renames are separate work. Do not introduce a temporary
`feature -> shared -> feature` bridge; an incomplete atomic slice stays incomplete
until its boundary is correct.

Each feature implementation that owns injectable bindings exposes a Koin `Module`.
UI-only modules use composable/function entry points and do not create empty modules.
Only `:shared` assembles and starts Koin. Features receive required contracts through
entry points and must not reach `:shared` through a service locator or dependency-
reversing callback.

API/implementation splits are demand-driven and require a real stable contract.
Do not create `:core:network`, speculative modules, or empty state/event/effect/
presenter scaffolding. An approved scaffold creates only requested real module,
source, resource, and test structure.

## Back And Navigation

`shared` arbitrates one Back transition per intent in this order: modal, edit,
active-page selection, Now Playing, then route. Only the active destination is
eligible, and predictive Back latches the exact destination and appearance. The feature
publishes exactly one already-resolved foremost immutable dismissal surface, modal before
edit, with stable identity per appearance and new identity per re-presentation. Shared owns
identity/mapping/registration; stale registrations/disposers cannot replace or clear active
state. Cancellation does not dismiss; completion revalidates and dispatches at most once;
rejection/staleness clears the pending session without fallthrough. Deleting the displayed
playlist invalidates only after confirmed exact absence, never through Back; failed/stale/
replayed deletion does not invalidate and unrelated state is preserved.

A dispatched non-predictive transition remains in flight until authoritative state reports the
exact latched target inactive or that target explicitly rejects completion. Repeated Back is
suppressed while in flight; callback return alone never settles or releases suppression. Rejection
releases without treating the target as settled, and any later Back is a new intent. Predictive
latching, cancellation, and no-fallthrough behavior remain unchanged.

## Database And Resources

`:shared` is the one transitional physical SQLDelight owner until Task 3.1. Task 1.3
changes only Gradle implementation ownership: `:shared` applies the controlled
`build-logic.sqldelight` convention instead of independently applying SQLDelight. The
convention applies/configures `app.cash.sqldelight` and carries
`app.cash.sqldelight:gradle-plugin:2.3.2` on build-logic `implementation`, not
`compileOnly`. This preserves one physical database and its existing configuration,
schema, migrations, package, database name, and platform driver behavior. Task 3.1 then
moves physical ownership atomically to `:core:database`: configured database/plugin,
`.sq`/`.sqm`/schema artifacts in their real source layout, drivers, and generated
package move together. A checker recognizes those physical signals, not README text
or arbitrary filenames; driver consumers are not database owners. Database moves
preserve schema, database name, migration history, and foreign keys. Each affected
slice verifies existing-database opening, migrations, and cross-feature FKs.

Resources belong to the owning module in recognized KMP or Compose source-set resource
locations. A namespace is validated only after that module build setup exposes a real
namespace; invented top-level resource folders are not architecture policy.
Every resource move verifies Android packaging, desktop runtime resolution, and iOS
linking. Platform code remains behind explicit KMP seams in its source set.

## iOS Facade And Exports

The shared framework remains the sole iOS facade. `MainViewController` remains
stable and enters the shared composition root. The iOS export allow-list is narrow:
export only a module whose declarations are required in the public Swift/Objective-C
API. Do not broadly export core modules or feature implementations.

The thin shared iOS facade retains the exact executable ABI ledger in the canonical Task 5.2
Superpowers design: package `com.eterocell.rhythhaus.playlistbackup`, framework `Shared`,
`IOSPlaylistBackupDocumentStatus` values, Completion/Provider names/signatures/nullability,
Bridge singleton access, MIME/max-size constant exports, and existing Swift `Int32` interop.
Shared adapts this ABI to the Kotlin-only feature seam through its retained iOS actual; the feature
is not exported, owns no iOS source, and Swift application files remain app-owned. Shared retains
composition, shell/routes/Back, lifecycle, Koin
assembly, Settings layout until Task 6.4, generic injected
`cancel`, and selection-bar composition. The feature owns embeddable backup sections/dialogs and
all playlist/queue/backup EN/ZH text once, without duplicate resource keys or generated handles.

## Enforcement And Deferrals

`architectureCheck` uses a code-owned allow-list and actual Gradle `ProjectDependency`
edges and cycle detection, not build-script text parsing. Its source-ownership contract
preserves package declarations during contract-first moves: package roots may overlap
between modules and a root matches only exactly or followed by `.`. It permits each
declared package for its owning module, not one global owner per package. The intended
preserved roots are the current root for core model, `ui` and `theme` for core UI, the
current `library` package for core database, and the current `library` package for both
Library and Playlists API/implementation modules.

The stable checker design is [2026-07-28-stable-architecture-checker-design.md](superpowers/specs/2026-07-28-stable-architecture-checker-design.md).
Shared build logic owns a project-owned normalized `ArchitectureModelRegistry`. Controlled
conventions own plugin application/classpaths and publish immutable public-API facts; the
root task consumes only that registry snapshot plus its existing model inputs. It validates
actual project edges, resources, SQLDelight, exports, and module-level explicit API. Android
conventions use public Android Components callbacks only for main-production static resource
roots; module namespace comes from public Android DSL `CommonExtension.namespace` through the
concrete `ApplicationExtension` or `LibraryExtension`, never an AGP 9.3 `Variant`. Nested
test/`androidTest`/test-fixture components are excluded; casts, reflection, task/artifact
internals, and per-variant namespace claims are forbidden. Compose conventions publish public
standard roots, explicit project-owned custom roots, and convention-declared configured
namespaces; blank declarations remain invalid registry facts for deterministic `ARCH-RESOURCE`
diagnostics and are not passed to Compose Resources. The root task never reads nested
`ResourcesExtension` or internal maps and makes no effective-namespace or independent-
introspection claim. The root JVM `:architecture-processor` is the
supported KSP artifact, not `build-logic:convention`; core/API conventions record real
production KSP registration. Generic `ksp` is tooling only for single-platform JVM/Android;
the root never trusts inferred or spoofed configuration names. The processor consumes
normalized module/root arguments, limits itself to supported compilation inputs, excludes
generated/test/local boundaries where supported APIs permit, and emits sorted deduplicated
relative-path diagnostics; it does not aggregate whole-project KSP state. SQLDelight
conventions publish public model facts to the registry. `architectureCheck` consumes no KSP
facts or outputs. There is no reflection, internals access, classloader probing, or
build-script parsing. Kotlin compilation remains the authority for import symbol resolution;
the checker does not scan arbitrary imports or infer providers from text. Root `check`,
CI, and `qualityCheck` integration remains Task 1.4 work.

The tooling-only `:architecture-processor` is exempt only when the owning KMP core/API
convention has applied KSP and registered the direct production dependency in the registry;
all spoofed or unregistered placements remain `ARCH-EDGE`. Core model and core database are
mutually forbidden. SQLDelight uses convention-published public configured
database/source-root data and fails deterministically for unsupported or inconsistent roots.

Android applications publish exact `(project, configuration)` identities only from public AGP
9.3.1 `ApplicationVariant` test-component `Component.compileConfiguration` and
`Component.runtimeConfiguration` objects for `androidTest`, `unitTest`, `deviceTests`, and
`hostTests`. Android-KMP libraries publish them only from public
`KotlinMultiplatformAndroidLibraryTarget.compilations` and public
`KotlinMultiplatformAndroidHostTestCompilation`/
`KotlinMultiplatformAndroidDeviceTestCompilation`
`compileDependencyConfigurationName` and `runtimeDependencyConfigurationName`. After
normalization, root dependency collection suppresses a direct self `ProjectDependency` only when
the project/configuration identity is exact and exactly one distinguishable direct self record
exists in that configuration; otherwise it suppresses none. `Configuration.isCanBeDeclared` is
not a predicate because declarable status
does not provide dependency provenance. Authored dependencies on supported declarable buckets,
including explicit self dependencies, remain checked, and an explicit self edge emits exact
`ARCH-EDGE` and one-node `ARCH-CYCLE`. An equal authored mutation on the exact AGP-owned
configuration may collapse into AGP's set record; it is unsupported and outside the checker
guarantee because no public provenance API exists. Configuration-name inference, blanket test
filtering, attribute guessing, reflection, AGP internals, and task/artifact/output/resolved-
classpath inspection are forbidden. The real Android RED fixture must capture exactly three
direct self edges and its cycle; GREEN must retain canonical main resource records and assert
convention-published AGP roles/cardinality. An explicit authored self-dependency fixture on a
supported declarable configuration remains negative.

Dependency Analysis Plugin adoption is intentionally deferred until graph
stabilization and a separately approved version and KMP compatibility evaluation.

## Library Implementation Boundary

The approved Slice 6 Library boundary is [the 2026-08-10 Library design](superpowers/specs/2026-08-10-library-feature-extraction-design.md).
It creates exactly one unexported `:feature:library:impl`; the existing Library API retains its
module identity and domain/repository-contract role. The Slice 6 amendment below authorizes only
the approved removal of API playback conversion/type residue. The implementation targets Android-KMP with host
tests/resources, JVM, `iosArm64`, and `iosSimulatorArm64`, preserves current Kotlin package roots,
uses Android namespace `com.eterocell.rhythhaus.library.impl`, and uses Compose resource namespace
`rhythhaus.feature.library.generated.resources`. It owns scanner/indexing, source/platform seams,
metadata/TagLib integration, repository implementations/mappings, Library leaf browse/detail/
scanning/import UI, and local transient rendering state. Shared retains root coordination,
route/Back/playback/session/lifecycle/selection policy, scan Job lifetime/orchestration, Koin,
and the stable iOS facade; Shared alone composes/starts Koin and has only an implementation-only
edge. `LibraryPlaybackSelection.kt` remains Shared so Library does not depend on core playback for
queue/restart policy. The public boundary is callback-first and KDoc-complete, with no Shared,
generated resource, database, playback, Job, TagLib, or native-handle types. The metadata reader
and types are intentionally removed under the f4ae104/30f89ff bridge-retirement amendment, without
exporting impl or adding a compatibility facade. The exhaustive EN/ZH resource ledger and causal
RED/GREEN controls are normative in the approved design; uncertain Shared-rendered keys remain
Shared or are injected as values.

### Slice 6 Amendment

The dedicated design supersedes only conflicting Library-Slice-6 wording above. In particular,
`:feature:library:api` remains the `:core:model`-only domain/repository contract, but its
implementation removes `LibraryTrack.toPlayableTrack()` and every playback type; Shared owns that
conversion. Shared retains its complete shell state: App/root shell, route/Back/predictive and
navigation identities, `LibraryAppState`, browse state, selection/page mapping, visible-ID and
scroll policy, bottom-bar policy, playback/session, scan Job/orchestration/publication,
cross-feature composition, Koin total assembly, and `LibraryPlaybackSelection.kt`.

The Koin public boundary is exactly `public fun libraryImplementationModule(): Module`; bindings
are internal. `LibraryDatabaseContext` moves physically from Shared Android to core-database Android
without changing package/name/public setter or app initialization-before-Koin ordering. This is an
existing process-bootstrap placement, not a new Android Context contract or a classification of all
consumers as database behavior. The dedicated design's exact UI/picker/scanner declarations,
resource ledger, historical intentional ABI contraction, and causal controls are normative.

The dedicated design's literal `BrowseMode`, `LibrarySelectionPage`, `LibrarySharedLabels`,
`LibraryHomeContent`, and `DrillDownView` declarations are binding, not illustrative. Home and
detail expose only core-model `Track`, core-UI `RhythHausBackdrop`, Compose types, feature values,
and primitive callbacks; groups, snapshots, LibraryTrack, controller/state, Job, Shared state/routes,
and generated resources remain internal. Each remembers its local list/chrome state, emits primitive
visible IDs and index/offset, preserves duplicate occurrences with private occurrence-index-plus-ID
keys, and has exactly one required-padding terminal spacer. Shared owns destination resolution and
unavailable-route Back effect, selection/browse/visible/scroll/bottom-bar policy, and playback queue/
restart behavior. `formatDuration` is internal Library code with characterization coverage.

The exhaustive ledger separates the twelve labels injected through `LibrarySharedLabels` from
Shared-only resolver keys, names all moved Library keys, requires EN/ZH multiset/absence controls,
and removes `selected` from both final catalogs only if no production/test consumer exists. API
identity and repository/domain contracts remain, but Slice 6 intentionally deletes only
`LibraryTrack.toPlayableTrack()`, its `PlayableTrack` import/dependency, and all API playback types;
the earlier byte/API assertion is historical and superseded for that declaration. Metadata moves
internal to `com.eterocell.rhythhaus.library.impl`; required header negatives are
`SharedAudioMetadata`, `SharedAudioMetadataReader`, `SharedAudioMetadataKt`,
`readAudioMetadata(path:)`, and `readAudioMetadataPath:`, while MainViewController symbols remain.
The one Android holder path is
`core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt`;
core database exposes it through Shared's existing API, impl directly depends on core database, and
the existing application-context-before-Koin order is enforced.

### Shared Playlist Playback Projection Amendment

The six baseline production Library conversion sites are `App.kt`, `LibraryAppShell.kt`, moving
`LibraryHomeContent.kt`, `LibraryRoutes.kt`, `PlaybackSessionCoordinator.kt`, and playlist impl
`PlaylistScreens.kt`. All post-extraction `LibraryTrack`-to-`PlayableTrack` conversion is owned by
retained Shared adapters. `PlayableTrack` belongs to `:core:model`, not `:core:playback`; Library
API remains `:core:model`-only and cannot expose that type or a conversion. Moving leaf UI returns
`Track` callback data for Shared conversion.

For playlist detail, Shared `LibraryRoutes.kt` constructs and passes
`playableTracksById: Map<String, PlayableTrack>` from authoritative library tracks. The feature
uses it directly instead of internally associating `LibraryTrack.toPlayableTrack()`. The map has
ordinary ID-keyed `associate` duplicate-key behavior; `SavedPlaylistPlaybackRequest` still owns
ordered duplicate occurrences and `selectedOccurrenceId`, and `onPlayEntry` is unchanged. Playlist
browser overlays may retain `List<LibraryTrack>` metadata input. The existing playlists impl
`:core:model` API visibility remains sufficient; no project edge, local mapper, or callback change
is introduced. Required causal controls cover field/artwork-byte projection, direct detail input,
queue occurrence order/selection, and callback failure/settlement.

### Third-Review Literal Amendment

### Oracle Conversion Inventory Correction

The six paths named above are historical/baseline inventory only. Post-extraction exactly four retained Shared production files own conversion/projection: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`. Moving feature-owned `LibraryHomeContent.kt` returns `Track` callback data and contains no conversion; playlist impl `PlaylistScreens.kt` consumes `playableTracksById` and contains no conversion. Task 7 deletes only `LibraryTrack.kt` method/import residue, adapts/removes `LibraryApiModelsTest.kt` conversion assertions, adapts `PlaylistLifecycleIntegrationJvmTest.kt` to Shared projection, and retains unrelated `MusicModels.kt` `Track.toPlayableTrack()` plus `SearchRouteAdapterJvmTest.kt` use.

The dedicated Library design's exact declarations are mandatory: every Home/Detail parameter is
required with no default; both use `currentTrackId`, not `selectedTrackId`, and neither receives
`isPlaying` or Detail `onPlayPause`. Both receive the required suspend artwork loader with the
documented eager/key/off-main/cancellation/stale-result behavior. `RhythHausBackdrop` remains a
public core-UI type, so impl exposes core model/UI and signature-required Compose artifacts through
`api`; all remaining UI libraries stay implementation-only. Tracks are authoritative sequences;
internal grouping/order/representative and occurrence identities are exactly as specified there.

The public Koin factory is the sole Koin-shaped declaration. Its all-single graph binds one concrete
platform object behind public access and internal scan roles, TagLibReader, metadata reader, database,
repository, and scanner; Shared resolves repository/access/scanner and any temporary Shared TagLib
consumer uses the same reader. The prior phrase describing metadata as accidental is historical and
superseded: `f4ae104` intentionally added the Swift bridge, `30f89ff` removed its consumer, and
Slice 6 intentionally retires it after source/header proof. The dedicated twelve-field resolver table,
including Shared-only `play`/`pause`, is normative. Android holder feasibility is exact: Android-only
core-database variant transitivity reaches androidApp through Shared, and impl androidMain has a
direct core-database dependency; old-path/duplicate/header/ordering controls apply.

### Fourth-Review Literal Amendment

Impl uses `api(projects.feature.library.api)`, `api(projects.core.model)`,
`api(projects.core.ui)`, signature-required Compose APIs, and `api(libs.koin.core)` because public
`libraryImplementationModule(): Module` returns Koin `Module`; core database/platform-as-needed,
TagLib, coroutines, Koin Compose, other Koin not exposed publicly, and remaining UI libraries are
implementation-only. Shared keeps its API
Library-contract edge and implementation-only impl edge. Detail accepts required raw
`LibraryDetailSummary`, not subtitle text: feature resolves its moved unknown-artist/detail-format
keys, while Shared supplies route/title/tracks/raw counts only. The dedicated internal scanner/event/
metadata declarations and exact all-`single` same-object Koin bindings are mandatory. Its twelve-row
resource resolver table, including retained album-artwork and track-artist-album consumers and
no-action library queue heading, is exhaustive. Metadata source declarations are retired publicly but
move internally; no accidental-export characterization remains active.

The dedicated public signature authority is binding pseudocode, not production-source inline KDoc.
Production source requires declaration-specific KDoc on every listed public declaration/member/
constructor property/function and KDoc `@param` entries for every public callable parameter/callback;
KSP inspects source and rejects missing or generic-placeholder documentation.
