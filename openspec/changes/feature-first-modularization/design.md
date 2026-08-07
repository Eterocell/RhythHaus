## Context

The current KMP application concentrates feature implementations, composition, SQLDelight access, and platform-facing seams in `:shared`. Earlier architecture work is complete, while package organization implementation exists in commits `f0310e5`, `06f8a16`, and `adb1e3d` despite stale 0/5 OpenSpec tracking. This change must reconcile those facts before implementation and must not repeat package moves.

The change is a gradual, behavior-preserving feature-first multi-module refactor. Android, desktop, and iOS continue to depend on the stable `:shared` facade. Existing Back ordering, scanner, playback, playlist, settings, SQLDelight schema/history, resources, expect/actual declarations, and Swift entry point are constraints rather than redesign opportunities.

## Goals / Non-Goals

**Goals:**

- Establish demand-driven core and feature modules through contract-first, independently buildable migration slices.
- Keep `:shared` as thin composition, root shell, lifecycle, Koin assembly, and stable iOS framework facade.
- Make module boundaries executable through convention plugins and Gradle architecture checks.
- Preserve package names during moves and verify behavior on every supported platform.

**Non-Goals:**

- Product/UI changes; Back, scanning, playback, playlist, or settings redesign; SQL schema/name/history changes; package renames during moves.
- New navigation/state/DI frameworks, Windows/Linux support, empty modules/types, `:core:network`, or temporary illegal bridge dependencies.
- Dependency Analysis Gradle Plugin adoption before graph stabilization and separate version/KMP compatibility evaluation.

## Decisions

### Contract-first strangler graph

Create `:core:model`, `:core:ui`, `:core:database`, and narrowly-scoped `:core:platform` only when their stated ownership is real. Add `:core:playback` with extracted playback engine/contracts; add tiny `:core:navigation` only when common destination-scoped Back contracts require it. Create `:feature:library:api/impl` and `:feature:playlists:api/impl`; keep Now Playing, Search, and Settings as single modules until a stable contract proves an API/implementation split is useful.

This avoids a big-bang move and avoids speculative module/pattern scaffolding. The rejected alternative is a broad technical-layer split or empty module creation: neither encodes feature ownership nor produces meaningful contracts.

### Dependency direction and composition

Apps depend on `:shared`; `:shared` alone composes feature implementations and starts Koin. Core/feature modules cannot depend on `:shared` or apps. Features cannot depend on another feature implementation; cross-feature calls use API modules only. A feature implementation publishes a Koin `Module` only when it owns injectable bindings; UI-only modules use composable/function entry points and do not create empty modules. No module may use a service-locator back-reference.

This retains a stable app/iOS entry while preventing the monolith from surviving as a dependency hub. The rejected alternative, feature-to-shared-to-feature bridges, masks ownership and would make every move non-atomic.

### Explicit ownership

`core:model` holds only truly cross-feature immutable projections; `core:ui` holds reusable primitives, theme, and artwork abstractions, never feature UI state; `core:database` owns the one physical SQLDelight schema/driver/migrations/generated DB; `core:platform` holds only capabilities reused by at least two domains. Feature implementation modules retain repositories and representation mappings. Library owns scanner/source/index/repository/UI/transient state. Playlists owns repository/edit/backup/UI. Playback belongs in `core:playback`.

Stateful screens follow immutable `UiState`, `UiEvent`, `UiEffect` with a Presenter/ViewModel, but stateless UI gets no empty types. Data direction is UI -> Event -> Presenter -> UseCase -> Repository -> DataSource; DTO/entity/domain mapping occurs beside its boundary.

### Stable Back and iOS/database boundaries

Keep Back root arbitration in `:shared`: modal -> edit -> active-page selection -> Now Playing -> route, one transition per intent, active destination only, and predictive target latching. The feature publishes exactly one already-resolved foremost immutable dismissal surface, modal before edit, with stable identity per appearance and new identity per re-presentation. Shared owns destination identity/mapping/registration; only the active destination is accepted, and stale registrations/disposers cannot replace or clear active state. Cancellation does not dismiss; completion revalidates exact destination/appearance and dispatches at most once; rejection/staleness clears the pending session with no same-intent fallthrough. A dispatched non-predictive transition remains in flight until authoritative state reports the exact latched target inactive or it explicitly rejects completion; repeated Back is suppressed meanwhile and callback return never settles/releases suppression. Rejection releases without treating the target as settled, so a later Back is a new intent. Displayed playlist deletion remains authoritative exact-destination invalidation after confirmed absence, never Back; failed, stale, or replayed deletion does not invalidate and unrelated state is preserved. Common navigation is introduced only when required by a shared destination-scoped contract.

Move SQLDelight atomically, including `.sq`, migrations, drivers, and generated package, with no schema/name/history changes. Resources move with their feature namespace and are verified on Android, desktop, and iOS. iOS exports only modules whose declarations become Swift/ObjC public API; the shared `MainViewController` stays stable.

### Approved Task 5.2 playlists implementation repair

Create unexported `:feature:playlists:impl` for Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64`. It owns saved-playlist/playback-queue UI, preserving the already-characterized immutable `PlaylistState`, `PlaylistStateAction`, reducer, `PlaylistStateOwner`, and backup immutable state/reducer as the Task 5.2-specific equivalent without a new Presenter/ViewModel/`UiEffect`/Event scaffold; repository implementations/Koin binding; backup codec/service/state/UI; the neutral common document-launcher contract; public Android/JVM launcher factories; and feature resources. Feature has no iOS actual/source and no Shared edge. Shared owns the common `expect`, Android/JVM delegates, and retained iOS actual/ABI adapter. Its API remains a clean repository/model contract. Direct edges are playlists API, Library API, core model/playback/ui/platform, and implementation-only core database. Remaining Gradle scopes follow exhaustive public signatures. Generated DB/SQLDelight/generated `Res`/shared navigation or shell types are excluded from public signatures; Koin is excluded except for the public binding-module factory returning `org.koin.core.module.Module` for shared assembly.

The move changes adapters only. `:core:database` remains sole physical owner: no `.sq`, `.sqm`, schema, migration, generated DB, driver, database-name, or FK changes. Preserve serialization/revision/cancellation, backup exclusivity, exact 4 MiB limits, mappings, stale-library rejection, transactional import, and exactly-once native completion. Queue UI does not transfer playback engine/session/lifecycle/root playback state. Shared retains composition, shell/routes/Back, lifecycle, Koin assembly/start, and Settings layout; the feature owns embeddable backup sections/dialogs and all playlist/queue/backup EN/ZH text once. Shared injects generic `cancel` and composes the feature-owned add-to-playlist plain `String` into the selection bar, with no duplicate keys/generated handles across the boundary.

The Shared framework retains the exact ABI ledger in the canonical Task 5.2 Superpowers design: package `com.eterocell.rhythhaus.playlistbackup`, framework `Shared`, status values, Completion/Provider signatures and nullability, Bridge singleton access, MIME/max-size constants and exports, and Swift `Int32` interop. It adapts to the Kotlin-only feature seam through its retained iOS actual without feature export; the feature owns no iOS source and Swift files remain app-owned. Both `InMemoryPlaylistRepository` and `SqlDelightPlaylistRepository` move to feature implementation ownership; `feature/playlists/api/.../PlaylistRepository.kt` remains the only public repository contract. Retained Shared consumers use that contract and public feature state ports, never direct implementation classes or `loadPlaylistSnapshot`. Shared exposes the internal test-visible factory `authoritativePlaylistBackupRevisionGuard(owner: AuthoritativeLibraryPublicationOwner): PlaylistBackupRevisionGuard`; its adapter regression proves current/stale delegation through `AuthoritativeLibraryPublicationOwner.withCurrentRevision` and exact cancellation rethrow. Public implementation declarations are limited to the Koin factory, shared-needed state/action/result types, owner, composable entries, dismissal contracts, backup orchestration contracts, and launcher seam, each with declaration-specific behavioral KDoc; all other helpers are internal/private. Governance covers `com.eterocell.rhythhaus.library` and `com.eterocell.rhythhaus.playlistbackup`. Enforcement changes are deferred.

### Executable governance

Create canonical guidance and feature READMEs. Shared build logic owns a normalized immutable
`ArchitectureModelRegistry`; controlled conventions own plugin application/classpaths and
publish public facts, while root `architectureCheck` consumes only its records plus existing
model inputs. Android conventions use public Android Components callbacks only for
main-production static roots and public Android DSL `CommonExtension.namespace` through
concrete `ApplicationExtension`/`LibraryExtension` for module namespace, never AGP 9.3
`Variant`; test/`androidTest`/test-fixtures, casts, reflection, AGP task/artifact internals,
and per-variant namespace claims are excluded. Compose
conventions publish standard roots and explicit declared custom-root records; the registry
configures those roots through public `ResourcesExtension.customDirectory(sourceSetName,
Provider<Directory>)` and does not claim an effective-custom-root getter;
the root never reads nested extensions/internal maps. Compose records only a
convention-declared configured namespace; a blank declaration remains an invalid registry fact
for deterministic `ARCH-RESOURCE` diagnostics and is not passed to Compose Resources.
The SQLDelight convention retains the approved `implementation` convention and publishes public
database facts. During Task 1.3, `:shared` remained the transitional physical owner; after Task
3.1, `:core:database` is the sole physical owner. Core/API conventions record KSP only after applying it and registering the
actual production consumer; generic `ksp` is tooling only for single-platform JVM/Android,
not arbitrary names. The processor uses normalized module/root args, supported compilation
inputs only, and sorted deduplicated relative-path diagnostics; it does not aggregate whole
projects. RED/GREEN covers Android variants, Compose roots/namespaces, real/spoofed KSP,
generated/test/local exclusions, declaration forms, exact sequence/counts, SQLDelight
ownership, real-root success, and cache reuse. No reflection, internals, classloader probing,
build-script parsing, KSP-output consumption, or Task 1.4 wiring is allowed.

The Android-KMP resource convention is separate from the Android application convention. It
uses public `KotlinMultiplatformAndroidLibraryExtension` namespace and
`KotlinMultiplatformAndroidComponentsExtension` main-variant sources, publishes only main
production roots, and publishes none when resources are disabled. SQLDelight artifact file
collections are declared root-task inputs and current artifacts are derived during task
execution, including after configuration-cache reuse; driver signals are direct documented
production dependencies. KSP processes initial-round `getNewFiles()` only and emits sorted,
deduplicated diagnostics in that round with relative path, source position, and qualified name.
Android applications publish exact `(project, configuration)` identities only from public AGP
9.3.1 `ApplicationVariant` test-component `Component.compileConfiguration` and
`Component.runtimeConfiguration` objects (`androidTest`, `unitTest`, `deviceTests`, and
`hostTests`). Android-KMP libraries publish them only from public
`KotlinMultiplatformAndroidLibraryTarget.compilations` and public
`KotlinMultiplatformAndroidHostTestCompilation`/
`KotlinMultiplatformAndroidDeviceTestCompilation`
`compileDependencyConfigurationName`/`runtimeDependencyConfigurationName`. After normalization,
root collection suppresses a direct self `ProjectDependency` only when the project/configuration
identity is exact and exactly one distinguishable direct self record exists in that configuration;
otherwise it suppresses none.
`Configuration.isCanBeDeclared` is not a predicate because declarable status provides no
dependency provenance. Authored dependencies on supported declarable buckets, including explicit
self dependencies, remain checked; explicit self edges emit exact `ARCH-EDGE` and one-node
`ARCH-CYCLE`. An equal authored mutation on the exact AGP-owned configuration may collapse into
AGP's set record and is unsupported/outside the checker guarantee because no public provenance
API exists.
Configuration-name inference, blanket test filtering, attribute guessing, reflection, AGP
internals, and task/artifact/output/resolved-classpath inspection are forbidden. The fixture matrix
distinguishes real KMP JVM processor/SPI execution from Android/native target registration,
captures exactly three direct self edges and their cycle in the real Android RED fixture, retains
canonical main resource records GREEN after suppression, asserts convention-published AGP
role/cardinality and fail-closed single-record behavior, and retains an explicit authored
self-dependency fixture on a supported declarable configuration plus a cardinality fail-closed
control, alongside classloader RED/GREEN,
cache-invalidated artifacts, Compose declared roots, Android-KMP disabled resources, KDoc
overload/path identity, and pre-existing generated source boundaries.

Gradle TestKit illegal fixtures begin RED before gate implementation. The rejected alternative is documentation-only policy, which cannot detect real graph drift.

## Risks / Trade-offs

- [App.kt composition coupling and LibraryAppState shell/feature ownership] -> characterize current behavior before moving contracts; retain root shell composition in `:shared`.
- [Centralized DI and feature UI internal imports] -> publish narrow APIs first and enforce graph checks; only shared composes implementations.
- [SQLDelight cross-feature FKs and old databases] -> make database transfers atomic and verify migration/foreign-key integration per slice.
- [Resources, expect/actual, and Swift exports] -> verify Android packaging, desktop runtime, iOS linking, and export allow-list in each affected slice.
- [Stale OpenSpec tracking] -> reconcile completed architecture/package evidence before new work; do not replay moves.
- [Pressure to bridge a failing slice] -> no illegal bridge is allowed; leave the atomic slice incomplete until its boundary is correct.

## Migration Plan

0. Reconcile/verify old architecture and package-organization tracking and implementation evidence.
1. Add governance baseline: TestKit RED fixtures, convention plugins, documentation/ADRs, and `architectureCheck`/`qualityCheck`.
2. Extract core model/UI contracts and implementations.
3. Atomically move SQLDelight and extract narrow platform capabilities.
4. Introduce library/playlists APIs while their implementations remain in `:shared`; Task 4.2 atomically extracts playback contracts, controller, and session value contracts plus Android/iOS/JVM/native playback engines, service, bridges, and native helper into `:core:playback`, while `:shared` retains playback session coordination/store/process lifecycle, Koin/App orchestration, and the package-stable platform-factory facade.
5. Extract leaf implementations: Now Playing, playlists/backup, Search, Settings.
6. Extract Library last, splitting shell from feature ownership.
7. Clean shared to its thin final role; add a real-structure-only scaffold after successful migrations; schedule package renames separately.

Every task follows RED characterization/architecture test, minimal GREEN move, focused verification, architecture/Detekt/Spotless gates, and full `./init.sh` for graph, expect/actual, SQLDelight, resource, and final slices. Failed slices are rolled back to their last passing boundary rather than bridged. Independently reviewable slices use conventional commits. Implementation updates progress, roadmap, and ADR evidence.

### Approved Task 5.3 Search implementation boundary

Task 5.3 creates one unexported `:feature:search` implementation module with no API split, Koin module, platform source, repository, state abstraction, or iOS export. It targets Android-KMP/JVM/`iosArm64`/`iosSimulatorArm64`, has one common implementation, preserves Kotlin package and Android namespace `com.eterocell.rhythhaus.search`, and uses `rhythhaus.feature.search.generated.resources`. `feature/search/README.md` is out of scope.

The exact public boundary is only explicit-public, declaration-specific-KDoc `SearchSharedLabels(title: String, clear: String, nowPlaying: String)` and:

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

It carries primitive playback state and callback-first selection/visible-ID/scroll/play/dismiss behavior, a composable formatter that resolves Shared's `select_track_format` with structured Compose `stringResource` while a row is composed, an indicator slot, and layout defaults. No generated resource handle crosses the boundary. It has no Shared/generated `Res`/playback controller or state/repository/Koin/platform/database/TagLib/queue type. `api` is only Library API plus public Compose runtime/UI; core UI, Foundation, resources, and Miuix are implementation-only. Shared uses exactly `implementation(projects.feature.search)`, never `api`, and never exports Search. No core playback, Shared, database, platform, taglib, another implementation, Koin, or iOS-export edge is allowed.

Shared is sole facade/composition owner: `LibraryRoutes` directly composes `SearchContent`, deletes Shared `SearchScreen`, and removes unused `TagLibReader`. Shared retains route/Back, selection reconciliation/clear, scroll storage, playback queue/restart/dismiss, bottom-bar/Now Playing, and `EqualizerStrip`; Search owns query/filter/render/focus/count/empty/row interaction. Search preserves blank-query no results; case-insensitive title/artist/album filtering; order, duplicates, and empty metadata; and an internal, non-public LazyColumn occurrence identity of filtered occurrence index plus track ID, never `track.id` alone. That rendering-only identity is unique and cannot change `LibraryTrack`, selection IDs, visible-ID sequence, playback queue order, or duplicate semantics. Search focuses once, clear-to-blank, visible-ID emission only on sequence change, ordered playback on normal activation, long press without playback, one-toggle selection row/checkbox behavior without playback, current-row highlight/Now Playing semantics, and indicator only for current+playing; it has no artwork/error state. It owns exactly the five approved Search EN/ZH keys, while Shared injects title/clear/Now Playing/composable select-track formatting with no duplicate key or resource handle. Feature production-composable tests own Search behavior and the four moved mixed-suite cases, including two equal-ID occurrences rendering/activating distinctly with stable keys across unrelated recomposition and duplicate ordered visible/playback callbacks. Real Shared route-adapter tests prove queue order, current-track restart, dismissal, and callback-failure ownership. RED/GREEN rejects feature-to-Shared/core-playback/database/core-platform/taglib/another-implementation/app, Koin, iOS export, Shared `api`/export, namespace/resource/resource-handle, and public-KDoc/closure violations. Evidence covers cross-platform, architecture, quality, strict named OpenSpec, Xcode, and `./init.sh` without runtime/device/visual claims; see [the approved Search design](../../../docs/superpowers/specs/2026-08-07-search-feature-extraction-design.md). Detailed paths and commands remain in the later executable plan.

## Open Questions

After this approved amendment, no Task 5.2 architecture decision remains unresolved. Implementation details, exhaustive public-signature scope derivation, exact commands, and path ledger remain owned by the later executable plan.
