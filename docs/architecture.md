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
`RhythHausBuildInfo` generation/model/verification is owned by the
`build-logic.build-info` convention, which `:feature:settings` applies. AboutLibraries generation,
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

`:core:database` is the sole physical SQLDelight owner: the configured database/plugin,
`.sq`/`.sqm`/schema artifacts in their real source layout, drivers, and the generated
package all live there. The controlled `build-logic.sqldelight` convention applies and
configures `app.cash.sqldelight`, carrying `app.cash.sqldelight:gradle-plugin:2.3.2`
on build-logic `implementation`, not `compileOnly`. A checker recognizes physical
ownership signals, not README text or arbitrary filenames; driver consumers are not
database owners. The single physical database, its schema, database name, migration
history, and foreign keys are preserved; every schema-affecting change verifies
existing-database opening, migrations, and cross-feature FKs.

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
the checker does not scan arbitrary imports or infer providers from text. Root `check`
and `qualityCheck` enforce `architectureCheck`; `qualityCheck` aggregates the Detekt
and Spotless check tasks across all projects through task-provider dependencies. CI
runs `qualityCheck` on PRs and main plus a JVM test leg across the shared/feature/core
modules.

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

The approved Library boundary is [the 2026-08-10 Library design](superpowers/specs/2026-08-10-library-feature-extraction-design.md).
It creates exactly one unexported `:feature:library:impl` plus the `:feature:library:api`
module. The API remains the `:core:model`-only domain/repository contract; the
implementation removes `LibraryTrack.toPlayableTrack()` and every playback type, and
Shared owns that conversion. The implementation targets Android-KMP with host
tests/resources, JVM, `iosArm64`, and `iosSimulatorArm64`, preserves current Kotlin
package roots, uses Android namespace `com.eterocell.rhythhaus.library.impl`, and uses
Compose resource namespace `rhythhaus.feature.library.generated.resources`.

Library owns scanner/indexing, source/platform seams, metadata/TagLib integration,
repository implementations/mappings, leaf browse/detail/scanning/import UI, and local
transient rendering state. Shared retains root coordination (App/root shell,
`LibraryAppState`, browse state, selection/page mapping, visible-ID and scroll policy,
bottom-bar policy), route/Back/predictive and navigation identities, playback/session,
scan Job lifetime/orchestration/publication, cross-feature composition, Koin total
assembly, and the stable iOS facade; Shared alone composes/starts Koin and has only an
implementation-only edge. `LibraryPlaybackSelection.kt` remains Shared so Library does
not depend on core playback for queue/restart policy. The public boundary is
callback-first and KDoc-complete, with no Shared, generated resource, database,
playback, Job, TagLib, or native-handle types; uncertain Shared-rendered keys remain
Shared or are injected as values.

The public Koin boundary is exactly `public fun libraryImplementationModule(): Module`;
all bindings are internal. Its all-`single` graph binds one concrete platform object
behind public access and internal scan roles, `TagLibReader`, the metadata reader, the
database, the repository, and the scanner; Shared resolves repository/access/scanner,
and any temporary Shared TagLib consumer uses the same reader. Impl depends with `api`
only on the Library API, `:core:model`, `:core:ui`, signature-required Compose APIs,
and `org.koin.core` (because the public factory returns a Koin `Module`); core database
and platform-as-needed, TagLib, coroutines, Koin Compose, other Koin not exposed
publicly, and the remaining UI libraries are implementation-only. Shared keeps its API
Library-contract edge and implementation-only impl edge.

`LibraryDatabaseContext` lives physically at
`core/database/src/androidMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabaseContext.android.kt`
with unchanged package/name/public setter and app initialization-before-Koin ordering.
Core database exposes it through Shared's existing API; impl androidMain depends directly
on core database, and the Android-only core-database variant transitivity reaches
androidApp through Shared. This is an existing process-bootstrap placement, not a new
Android Context contract; old-path/duplicate/header/ordering controls apply.

### Home And Detail Declarations

The design's literal `BrowseMode`, `LibrarySelectionPage`, `LibrarySharedLabels`,
`LibraryHomeContent`, and `DrillDownView` declarations are binding, not illustrative.
Every Home/Detail parameter is required with no default; both use `currentTrackId`, not
`selectedTrackId`, and neither receives `isPlaying` or a Detail `onPlayPause`. Both
receive the required suspend artwork loader with the documented eager/key/off-main/
cancellation/stale-result behavior. Home and detail expose only core-model `Track`,
core-UI `RhythHausBackdrop`, Compose types, feature values, and primitive callbacks;
groups, snapshots, `LibraryTrack`, controller/state, Job, Shared state/routes, and
generated resources remain internal. Tracks are authoritative sequences; internal
grouping/order/representative and occurrence identities are exactly as specified in the
design. Each composable remembers its local list/chrome state, emits primitive visible
IDs and index/offset, preserves duplicate occurrences with private
occurrence-index-plus-ID keys, and has exactly one required-padding terminal spacer.
`formatDuration` is internal Library code with characterization coverage.

Detail accepts the required raw `LibraryDetailSummary`, not subtitle text: the feature
resolves its moved unknown-artist/detail-format keys, while Shared supplies
route/title/tracks/raw counts only.

Shared owns destination resolution and unavailable-route Back effect,
selection/browse/visible/scroll/bottom-bar policy, and playback queue/restart behavior.

### Resources And Metadata

The exhaustive resource ledger separates the twelve labels injected through
`LibrarySharedLabels` from Shared-only resolver keys, names all moved Library keys, and
requires EN/ZH multiset/absence controls; `selected` is removed from both final catalogs
only if no production/test consumer exists. The design's twelve-row resolver table,
including retained album-artwork and track-artist-album consumers, the no-action library
queue heading, and Shared-only `play`/`pause`, is exhaustive and normative. The design's
exact UI/picker/scanner declarations, intentional ABI contraction, and causal controls
are normative.

Metadata source declarations are retired from the public API but move internally to
`com.eterocell.rhythhaus.library.impl`; no accidental-export characterization remains
active. The Swift bridge added by `f4ae104` and consumed by `30f89ff` is intentionally
retired; required header negatives are `SharedAudioMetadata`, `SharedAudioMetadataReader`,
`SharedAudioMetadataKt`, `readAudioMetadata(path:)`, and `readAudioMetadataPath:`, while
`MainViewController` symbols remain.

### Playback Projection

`PlayableTrack` belongs to `:core:model`, not `:core:playback`; the Library API remains
`:core:model`-only and cannot expose that type or a conversion. Exactly four retained
Shared production files own `LibraryTrack`-to-`PlayableTrack` conversion/projection:
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`, and
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`.
Feature-owned `LibraryHomeContent.kt` returns `Track` callback data and contains no
conversion; playlist impl `PlaylistScreens.kt` consumes `playableTracksById` and
contains no conversion. Slice 7 removed the remaining `LibraryTrack.kt` conversion
method/import residue and adapted `LibraryApiModelsTest.kt` and
`PlaylistLifecycleIntegrationJvmTest.kt` to Shared projection; `MusicModels.kt`
`Track.toPlayableTrack()` and `SearchRouteAdapterJvmTest.kt` retain their unrelated uses.

For playlist detail, Shared `LibraryRoutes.kt` constructs and passes
`playableTracksById: Map<String, PlayableTrack>` from authoritative library tracks. The
map has ordinary ID-keyed `associate` duplicate-key behavior; `SavedPlaylistPlaybackRequest`
owns ordered duplicate occurrences and `selectedOccurrenceId`, and `onPlayEntry` is
unchanged. Playlist browser overlays may retain `List<LibraryTrack>` metadata input. No
project edge, local mapper, or callback change is introduced. Required causal controls
cover field/artwork-byte projection, direct detail input, queue occurrence order/
selection, and callback failure/settlement.

### Public Signature And KDoc Authority

The design's public signature authority is binding pseudocode, not production-source
inline KDoc. Production source requires declaration-specific KDoc on every listed public
declaration/member/constructor property/function and KDoc `@param` entries for every
public callable parameter/callback; KSP inspects source and rejects missing or
generic-placeholder documentation.

### Thin Shared Role

`:shared` is thin: it owns only `App()` composition, the root shell
(`LibraryAppShell`/`LibraryDialogs`), cross-feature route/Back arbitration
(`LibraryAppState`/`LibraryNavigation`/`LibraryRoutes`), lifecycle
(`PlaybackProcessLifecycle`), Koin assembly (`di/RhythHausDi`), and the stable
`MainViewController` iOS facade, plus the intentionally-retained session
coordination/persistence, theme persistence, package-stable playback engine factory,
playlist-backup ABI seam, Library selection integration, track selection state/bar,
Now Playing shell placement, and shared formatting helpers. A thin-shared inventory test
asserts this exact source-file set. The unused `Logger` Kermit singleton and the unused
`Platform`/`getPlatform` expect-actual family are removed without any bridge dependency.
A real-structure-only scaffold generates a requested feature-module skeleton. Package
renames and Dependency Analysis Gradle Plugin evaluation remain deferred.
