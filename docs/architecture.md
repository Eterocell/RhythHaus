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
assembly, Settings layout, generic injected
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
