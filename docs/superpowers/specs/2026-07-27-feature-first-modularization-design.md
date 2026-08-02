# Feature-First Modularization Design

## Overview

RhythHaus will move gradually from the current monolithic KMP `:shared` module to a feature-first multi-module layout without changing observable behavior. The refactor establishes canonical architecture guidance and executable Gradle enforcement while preserving Kotlin packages during module moves. Package renames are a later, separate effort.

## Goals

- Preserve behavior, UI, Back behavior, scanning, playback, playlists, settings, database identity, and supported-platform behavior throughout migration.
- Use a contract-first strangler strategy: move contracts before implementations, keep every slice buildable, and never introduce a temporary `feature -> shared -> feature` bridge.
- Establish the target KMP module graph, clear ownership boundaries, Koin composition rules, and CI-enforced architectural gates.
- Provide canonical guidance in `skills/kmp-architecture/SKILL.md`, `docs/architecture.md`, ADRs for module boundaries and shared/iOS exports, and feature READMEs.

## Non-Goals

- UI or behavior redesign, or changes to Back, scanning, playback, playlist, or settings behavior.
- SQL schema, database name, migration-history, or data changes.
- Circuit, Decompose, Molecule, a new DI framework, Windows/Linux support, package renames while moving code, empty modules, or empty pattern classes.
- An empty `:core:network` module, broad iOS framework exports, or a Dependency Analysis Gradle Plugin adoption before graph stabilization and separate version/KMP compatibility evaluation.

## Decision

Adopt a demand-driven, feature-first KMP graph using a contract-first strangler migration. Each slice must compile and pass its focused checks independently. A contract moves before an implementation, feature implementations remain isolated, and any failed atomic slice remains incomplete rather than acquiring an illegal bridge dependency.

The final structure is:

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

Core modules are introduced only when they have real ownership:

- `:core:model`: truly cross-feature immutable projections only.
- `:core:ui`: reusable primitives, theme, and artwork abstractions; never feature UI state.
- `:core:database`: the sole SQLDelight schema, driver, migrations, and generated database.
- `:core:platform`: capabilities used by two or more domains, narrowly scoped.
- `:core:playback`: added when playback engine/contracts are extracted.
- `:core:navigation`: added only when common destination-scoped Back contracts are genuinely required.

There is no `:core:network` until a real cross-feature network concern exists. Features become `:feature:library:api/impl` and `:feature:playlists:api/impl`; Now Playing, Search, and Settings remain single feature modules at first. API/implementation splits happen only for a real stable contract.

` :shared` becomes a thin KMP composition and iOS framework facade. It owns `App()`, the root shell, cross-feature route and Back arbitration, lifecycle, Koin assembly, and the stable `MainViewController` entry. Applications depend on `:shared`. No core or feature module may depend on `:shared` or an app module; a feature may not depend on another feature implementation; cross-feature access is only through API modules; and `:shared` alone composes implementations.

## Ownership And Contracts

Library owns scanner/source access, indexing, repositories, library UI, and its transient UI state. Playlists owns playlist repository/edit/backup/UI, including backup/document behavior. Playback engine and its contracts belong in `:core:playback`. Leaf feature-internal state remains local. Repository/mapping logic stays in feature implementations even though `:core:database` owns the physical SQLDelight database.

### Task 4.1 Library And Playlist API Contracts

Task 4.1 is contract-only: publish the full existing Library and Playlist repository
interfaces while their implementations remain in `:shared`. `:feature:library:api`
owns `LibraryRepository` and every immutable signature type required by its existing
methods. It depends only on `:core:model`; scanner/source access implementations,
transient `ScanProgress`, in-memory and SQLDelight adapters, mappers, UI, and
playback-selection helpers remain implementation-owned in `:shared`.
The published contract retains the complete source, track, artwork, scan-session,
scan-error, and cleanup surface rather than reducing `LibraryRepository` to
`tracks()`. `LibraryTrack` and `TrackArtwork` preserve their existing content-based
`ByteArray` equality and hash semantics.

`:feature:playlists:api` owns `PlaylistRepository`, `PlaylistSummary`, `PlaylistEntry`,
and `PlaylistImportMutation`, with no production project dependency. The handwritten
API type is `PlaylistSummary` because `:core:database` already generates
`com.eterocell.rhythhaus.library.Playlist`; generated `Playlist` remains a
persistence representation owned by the database and `:shared` adapters map it to
`PlaylistSummary`. Every moved declaration preserves the existing
`com.eterocell.rhythhaus.library` Kotlin package. No SQL schema, table name, database
identity, migration history, or physical database ownership changes.
Every `PlaylistRepository` method that currently returns the generated `Playlist`
row returns `PlaylistSummary` at the feature API boundary; ordering, timestamps,
validation, rollback, entry, and import behavior remain unchanged.

Task 4.1 adds shared-owned internal transitional `libraryImplementationModule()` and
`playlistsImplementationModule()` Koin factories. They are included and composed only
by `rhythHausModule()` in `:shared`; API modules have no Koin dependency, and no
physical feature implementation modules are created in this slice. The factories move
with their implementations in later extraction tasks.

API-local tests cover interfaces, values, explicit API, and KDoc using private fakes;
existing implementation behavior tests remain in `:shared`. Shared DI tests prove both
abstractions resolve to the existing SQLDelight adapters and that only `:shared`
composes the factories. Architecture fixtures reject API-to-database/shared/
implementation and implementation-to-shared/other-implementation bridges.

Rejected alternatives are exposing generated database `Playlist` through the feature
API, renaming the SQLDelight table-generated row without a schema change, and deferring
Playlist API publication.

Stateful screens use immutable `UiState`, `UiEvent`, and `UiEffect`, coordinated by a Presenter or ViewModel. Stateless UI does not receive empty pattern types. The data flow is:

```text
UI -> Event -> Presenter -> UseCase -> Repository -> DataSource
```

DTOs, database entities, and domain types remain distinct, with mapping close to the boundary that introduces the representation.

Each implementation module publishes a Koin `Module`. Only `:shared` assembles and starts DI; no service locator back-reference is permitted.

## Back And Navigation Invariants

The existing Back contract is preserved exactly. One intent performs one transition in this order: modal, edit mode, active-page selection, Now Playing, then route. Only the active destination participates. Predictive Back latches the exact destination and target. A feature owns its modal/edit state and publishes only its foremost dismissal. Deleting a displayed playlist is destination invalidation, not a Back transition. The root shell remains in `:shared`; introduce the smallest common navigation contract only if destination-scoped Back behavior requires one.

## Database, Resources, And iOS

During Task 1.3, `:shared` was the transitional physical SQLDelight owner while only Gradle application/configuration moved into a dedicated build-logic convention. Accepted Task 3.1 transferred the configured database/plugin ownership, true-layout `.sq`/`.sqm`/schema artifacts, drivers, and generated package atomically to `:core:database`, which is now the sole physical SQLDelight owner. The Task 1.3 convention change preserved the existing database configuration, schema, migrations, package, database name, and platform driver behavior. Runtime/coroutine consumers, README text, and arbitrary filenames do not identify an owner.

Resources move with their feature through recognized KMP/Compose source-set locations; a namespace is enforced only when exposed by the public module model. Each migration verifies Android packaging, desktop runtime resolution, and iOS linking. iOS exports only modules whose declarations enter the Swift/Objective-C public API; broad exports are forbidden. The existing shared framework entry remains stable.

## Migration Strategy

0. Reconcile and verify prior changes before implementation. `architecture-refactor` is 12/12 complete. Package organization implementation exists in commits `f0310e5`, `06f8a16`, and `adb1e3d` despite stale 0/5 tracking; do not redo its package moves.
1. Establish the governance baseline using failing Gradle TestKit architecture tests before convention plugins and executable gates.
2. Extract core model and core UI.
3. Atomically extract the database and narrow platform capabilities.
4. Create library/playlists APIs and core playback contracts while their implementations remain in shared.
5. Extract leaf implementations in order: Now Playing, playlists/backup, Search, then Settings.
6. Extract Library last, separating app shell composition from feature ownership.
7. Finish thin shared cleanup and add a feature scaffold only after successful feature migrations. The scaffold generates real structure only, never empty pattern classes. Package renames remain separate.

## Governance And Gates

Canonical architecture documentation is `skills/kmp-architecture/SKILL.md` and `docs/architecture.md`; ADRs record boundary and shared/iOS export decisions. Feature READMEs explain local ownership. `AGENTS.md` later links to these documents rather than duplicating them.

Build logic provides convention plugins for core, feature API, feature implementation, Android,
Compose Resources, and SQLDelight ownership. Shared build logic owns a normalized immutable
`ArchitectureModelRegistry`: controlled conventions apply plugins on their compatible
classpaths and publish public API facts; root `architectureCheck` consumes only its records
plus existing model inputs. Android records main-production static roots through public
Android Components callbacks, while module namespace comes from concrete public Android DSL
`CommonExtension.namespace` (`ApplicationExtension` or `LibraryExtension`), never an AGP 9.3
`Variant`. It excludes test/`androidTest`/test-fixture variants, casts, reflection,
task/artifact internals, and per-variant namespace claims.
Compose records public standard roots, explicit project-owned declared custom roots, and its
convention-declared configured namespace, so the root never reads nested
`ResourcesExtension` or internal maps. A blank declaration remains an invalid registry fact for
deterministic `ARCH-RESOURCE` diagnostics and is not passed to Compose Resources. The
SQLDelight convention retains `app.cash.sqldelight:gradle-plugin:2.3.2` on build-logic
`implementation`, applies/configures SQLDelight, and publishes typed public model facts while
preserving the accepted `:core:database` physical ownership (with `:shared` only the
historical transitional owner during Task 1.3). The root JVM `:architecture-processor` is not the
convention-plugin JAR. Core/API conventions record KSP only after applying it and registering
the real production consumer; generic `ksp` is tooling only for single-platform JVM/Android,
never an inferred/spoofed name. The processor receives normalized module/root arguments,
processes compilation-local initial inputs only, excludes generated/test/local boundaries as
supported, and emits sorted deduplicated relative paths; it does not aggregate a project.
Fixtures cover the exact Android, Compose, KSP, declaration, diagnostic, and cache cases in
the stable checker design. No reflection, internals, classloader probing, build-script
parsing, KSP-output consumption, or Task 1.4 entrypoint wiring is permitted.

Android application conventions publish exact module/project and configuration identities only
from public AGP 9.3.1 `ApplicationVariant` test-component
`Component.compileConfiguration`/`Component.runtimeConfiguration`. Android-KMP library
conventions publish them only from public `KotlinMultiplatformAndroidLibraryTarget.compilations`
and public `KotlinMultiplatformAndroidHostTestCompilation`/
`KotlinMultiplatformAndroidDeviceTestCompilation`
`compileDependencyConfigurationName`/`runtimeDependencyConfigurationName`. After normalization,
root collection suppresses a direct self `ProjectDependency` only when that identity exactly
matches and the configuration contains exactly one distinguishable direct self record; otherwise
it suppresses none.
`Configuration.isCanBeDeclared` is not a predicate because it supplies no dependency provenance.
Authored dependencies in supported declarable buckets, including explicit self edges, remain
checked and emit `ARCH-EDGE` plus one-node `ARCH-CYCLE`. An equal authored mutation on the exact
AGP-owned configuration may collapse into its set record and is unsupported/outside the checker
guarantee because no public provenance exists. No name inference, blanket test filtering,
attribute guessing, reflection, AGP internals, or task/artifact/output/resolved-classpath
inspection is permitted. The Android RED fixture captures the known three synthetic self edges
and cycle; GREEN removes only those records while retaining canonical main resource records,
the authored self-edge negative control, and the fail-closed cardinality control.

## Testing And Acceptance

Every task starts with a characterization or architecture RED test, makes the minimal move or implementation, then runs focused GREEN checks followed by architecture, Detekt, and Spotless checks. Run full `./init.sh` for graph, expect/actual, SQLDelight, resource changes, and final validation. Update `progress.md`, `roadmap.md`, and relevant ADRs during implementation. Make conventional commits per independently reviewable migration slice.

Acceptance requires actual dependency-graph and TestKit illegal-fixture coverage; a thin shared inventory; explicit public APIs; Back regressions; SQLDelight migration/integration verification; Android, desktop, and iOS startup/resource/DI coverage plus key playback and scanning paths; `qualityCheck`; `./init.sh`; strict OpenSpec validation; and `git diff --check`. Documentation and trackers must match the evidence.

## Risks And Mitigations

- Current `App.kt` composition coupling and `LibraryAppState` shell/feature split: characterize before extraction and preserve shell ownership in shared.
- Centralized DI and feature UI internal imports: move contracts first, enforce graph checks, and let shared assemble implementation modules only.
- SQLDelight cross-feature FKs, resources, expect/actual declarations, and Swift exports: use atomic database moves and platform-specific verification in the affected slice.
- Stale OpenSpec tracking: reconcile the known architecture/package changes before migration work.
- Illegal bridge dependencies: prohibit them; leave a failed atomic slice incomplete rather than weakening the graph.
