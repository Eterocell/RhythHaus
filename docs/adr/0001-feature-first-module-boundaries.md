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
dismissal. Displayed-playlist deletion remains destination invalidation, not Back.

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
