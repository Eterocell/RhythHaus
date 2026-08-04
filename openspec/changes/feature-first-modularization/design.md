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

Keep Back root arbitration in `:shared`: modal -> edit -> active-page selection -> Now Playing -> route, one transition per intent, active destination only, and predictive target latching. Features publish only their foremost modal/edit dismissal. Displayed playlist deletion remains destination invalidation. Common navigation is introduced only when required by a shared destination-scoped contract.

Move SQLDelight atomically, including `.sq`, migrations, drivers, and generated package, with no schema/name/history changes. Resources move with their feature namespace and are verified on Android, desktop, and iOS. iOS exports only modules whose declarations become Swift/ObjC public API; the shared `MainViewController` stays stable.

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

## Open Questions

There are no unresolved product or architecture decisions. Individual implementation tasks determine only evidence-driven details such as whether a common navigation contract is actually necessary and whether a leaf feature has a stable API contract worth splitting.
