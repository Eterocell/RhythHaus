# Stable Architecture Checker Design

## Status And Problem

Task 1.3 is accepted after the independent final Oracle re-review PASS following correction.
The `ArchitectureCheckPluginFunctionalTest` XML reports 46 tests, 0 skipped, 0 failures, and
0 errors, including external canonical repository-built `:architecture-processor` JAR/SPI/KSP
proof and real Android-KMP host/device aggregate identity positive, authored-self, and
fail-closed cardinality cases. Root `architectureCheck` passed twice with configuration-cache
reuse. Strict feature OpenSpec validation, `spotlessCheck`, `detekt`, and `git diff --check`
passed. `init.sh` was manually stopped after more than 9000 seconds at the user's explicit
request and was not rerun; the full JVM/Android/desktop/iOS platform matrix remains uncertain
and is not claimed. Task 1.4 remains next and out of scope. The rejected lexical checker attempted to
infer source ownership by tokenizing Kotlin and building an arbitrary import-provider
index. That model was unstable: it could weaken the dependency DAG, confuse package
overlap with ownership, mistake source layout or driver consumers for SQLDelight
ownership, and make incomplete semantic claims from text. It is not an acceptance gate.

This design records the two cooperating layers implemented for inspection and the remaining
final-acceptance evidence.

## Goals

- Keep `architectureCheck` a manually invokable root task that validates the real Gradle
  project graph and stable build-model policy.
- Preserve Kotlin packages during contract-first moves. A package root is permitted per
  module and matches only the exact root or the root followed by `.`; roots are not
  globally unique.
- Use KSP to report semantic package-root and KDoc violations directly during applicable
  production compilation, without a handwritten Kotlin lexer or unsupported compiler/
  Analysis API integration.
- Remain compatible with Kotlin Gradle Plugin 2.4.10, Android Gradle Plugin 9.3.1, and
  Gradle configuration cache requirements.
- Produce deterministic, complete, stable `ARCH-*` diagnostics and TestKit evidence.
- Make the SQLDelight Gradle plugin a controlled convention-owned dependency so the
  checker can inspect its public model through normalized convention-owned facts.
- Keep all plugin-specific public API access in the convention that owns that plugin;
  make the root checker consume only immutable project-owned architecture-model records.

## Non-Goals

- Kotlin compiler internals, a standalone Kotlin Analysis API process, text tokenizers,
  arbitrary import-provider scanning, or generated-source surface validation.
- Creating feature/product modules, changing application source, changing the application
  dependency graph, or wiring root `check`, `qualityCheck`, or CI. Those entry points
  remain Task 1.4. The narrowly scoped processor module and consumer KSP dependencies
  below are an approved Task 1.3 exception.
- Replacing Kotlin compilation as the authority for symbol resolution.

## Public API Constraints

The implementation uses only public Gradle, KGP, AGP, KMP, Compose Resources, SQLDelight,
and KSP APIs available for the stated toolchain. Shared build logic owns a project-owned
`ArchitectureModelRegistry`; controlled conventions publish normalized immutable records
while they hold their plugin's compatible public API/classpath. The root checker receives
only those records, existing Gradle graph/KMP/JVM inputs, and declared file collections,
never live plugin extension objects. KSP uses supported processor
configuration (`ksp { arg(...) }` and target-specific KSP dependency configurations),
but exposes no stable task-provider or output-directory model for a root task. Public API
incompatibility is a build-logic compatibility failure with a clear diagnostic, not a
reflective fallback to internals.

### SQLDelight Convention Boundary

`build-logic/convention/build.gradle.kts` declares
`implementation("app.cash.sqldelight:gradle-plugin:2.3.2")`; it is not a
`compileOnly` dependency. The new precompiled script
`build-logic/convention/src/main/kotlin/build-logic.sqldelight.gradle.kts` applies and
configures `app.cash.sqldelight`. `shared/build.gradle.kts` applies only the controlled
`build-logic.sqldelight` convention and no longer independently applies or configures
SQLDelight.

This is implementation ownership of Gradle setup, not physical database ownership.
`:shared` remains the sole transitional physical owner, with its existing database,
schema, migrations, package, database name, source layout, and Android/JVM/iOS driver
behavior unchanged. The SQLDelight convention type-safely reads public
`SqlDelightExtension`/database models and publishes normalized ownership records into the
registry. `ArchitectureCheckPlugin` and `ArchitectureCheckTask` consume those immutable
records; they never statically access an extension loaded by a separate plugin classloader.
No layer uses reflection, SQLDelight internals, classloader probing, or build-script parsing.
The root task receives declared SQLDelight artifact file collections as task inputs and derives
the current physical artifacts during task execution; it never snapshots `artifacts.files`
during configuration, including before configuration-cache reuse. Owner driver signals are
direct dependencies in a documented production configuration only, never arbitrary, test, or
spoofed configurations.

### Processor Artifact Boundary

`build-logic:convention` must not become a processor artifact: its JAR is a Gradle plugin
classpath, not a supported KSP processor consumption boundary. The smallest supported
artifact is a normal root JVM subproject, `:architecture-processor`, included from root
`settings.gradle.kts`. It uses the existing Kotlin 2.4.10 and KSP 2.3.10 lines, changes
no toolchain, and is not Maven-published.

Its planned implementation owns the `symbol-processing-api` implementation dependency,
the `SymbolProcessorProvider` implementation, and the Java SPI descriptor at
`META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`. The
processor module does not apply the KSP Gradle plugin. Task 1.3 alone wires consumers with
the public KSP extension and documented target-specific dependency configurations (KMP
target configurations and JVM/Android `ksp`). Wiring is production-only and applies only
to core/API conventions; transitional `:shared` does not acquire this source-policy gate.

## Two-Layer Design

### Layer 1: Pure Gradle Model Checker

The root convention applies the checker and registers standalone `architectureCheck`.
Shared build logic creates and owns the project-scoped `ArchitectureModelRegistry`.
During configuration, the root collects every declared `ProjectDependency` from every
project configuration into immutable `(consumerPath, configurationName, providerPath)`
records; controlled conventions publish their plugin-specific normalized records to the
same registry. The task consumes only an immutable registry snapshot plus its existing
model inputs.
It preserves configuration names for diagnostics, deduplicates only `(consumer, provider)`
pairs for cycle detection, and sorts all records before task input assignment.

`:architecture-processor` is tooling, not an architectural module dependency. A KMP
core/API convention publishes a production-KSP registration record only after it applies
KSP and registers the processor dependency itself. Generic `ksp` is valid only for
single-platform JVM or Android conventions; KMP production registrations use the actual
registered target configuration. The checker excludes the processor edge only when that
registry record matches the direct dependency. It never infers tooling status from arbitrary
configuration names, task names, or outputs. The same dependency on runtime,
implementation, API, test, spoofed, or unregistered configuration remains `ARCH-EDGE`.
`:core:model` and `:core:database` are mutually
forbidden until a separately approved migration changes the graph. A cycle reports both
`ARCH-EDGE` and `ARCH-CYCLE` whenever both rules apply.
Android application conventions obtain exact `(project, configuration)` identities only from
public AGP 9.3.1 `ApplicationVariant` test-component `Component.compileConfiguration` and
`Component.runtimeConfiguration` objects (`androidTest`, `unitTest`, `deviceTests`, and
`hostTests`). Android-KMP library conventions instead obtain them only through public
`KotlinMultiplatformAndroidLibraryTarget.compilations` and public
`KotlinMultiplatformAndroidHostTestCompilation`/
`KotlinMultiplatformAndroidDeviceTestCompilation`
`compileDependencyConfigurationName` and `runtimeDependencyConfigurationName` values. After
normalization, root dependency collection suppresses a direct self `ProjectDependency` only when
its project and configuration exactly match a published identity and exactly one distinguishable
direct self record exists in that configuration. It suppresses none when any condition is not
met, so ambiguity fails closed.
`Configuration.isCanBeDeclared` is not a predicate because declarable status provides no
dependency provenance. The checker must not infer configuration names, guess attributes, use
reflection or AGP internals, or inspect tasks, artifacts, outputs, or resolved classpaths.

Authored dependencies on supported declarable buckets, including explicit self dependencies,
remain checked. Those self `ProjectDependency` edges remain observable as exact `ARCH-EDGE` and
one-node `ARCH-CYCLE` diagnostics. An equal authored mutation on the exact AGP-owned
configuration can collapse into AGP's set record; public AGP/Gradle APIs expose no provenance to
distinguish it, so that case is unsupported and outside the checker guarantee rather than
silently attributed to AGP.

The task owns these checks:

- `ARCH-CYCLE`: directed cycles in deduplicated actual project edges.
- `ARCH-EDGE`: actual edges not allowed by the policy map. The policy never adds an edge
  merely to suppress a cycle fixture.
- `ARCH-RESOURCE`: resources outside the owning module's recognized source-set locations
  or a configured namespace, when one exists.
- `ARCH-EXPLICIT-API`: a required core/API module whose public
  `KotlinBaseExtension.explicitApi` is not `ExplicitApiMode.Strict`.
- `ARCH-SQLDELIGHT`: physical SQLDelight ownership is not exactly the transitional
  `:shared` owner, or after Task 3.1 not exactly `:core:database`.
- `ARCH-IOS-EXPORT`: a KMP framework export is not explicitly allowed, or is transitive.

`ARCH-IMPORT` is intentionally not an arbitrary source scanner rule. Kotlin compilation
already resolves imports only when a symbol is available through the compilation classpath,
which requires the dependency relationship. `architectureCheck` validates that actual
declared `ProjectDependency` graph; compilation validates symbols. KSP does not supply
root-task facts or substitute an unsupported cross-module import resolver.

### Layer 2: KSP Semantic Processor

A KSP processor from `:architecture-processor` is wired through supported KSP dependency configurations
for applicable KMP production compilations. Its `ksp { arg(...) }` options contain module
identity and approved package roots. It uses KSP declarations and documentation APIs, not
source text, to report stable package-root and public-KDoc violations as compilation
errors. Test compilations are excluded. Generated sources are neither scanned nor made
part of the architecture source surface.

The processor does not write facts for `architectureCheck`, and the root task does not
depend on KSP task names, consume KSP output directories, or use KSP internals. KDoc
policy applies to public core/API declarations in the KSP-visible production source scope;
generated, local, and test declarations are out of scope.

Production KSP consumer configuration is recorded by the owning core/API convention, not
rediscovered by the root task. The processor receives normalized module and package-root
arguments. It processes only `Resolver.getNewFiles()` for the initial round and filters
generated, test, local, and out-of-boundary declarations as far as supported KSP
origin/location APIs and convention registration permit. Diagnostics are sorted, deduplicated,
and rendered with stable relative source paths plus an overload-safe declaration identity: at
minimum relative path, source position, and qualified name. It emits same-round diagnostics
before returning because KSP nodes need not remain valid through a later generated round.
KSP incremental processing is limited to the compiler-provided files for the current
compilation/round: it does not aggregate or validate a whole project, cross-module source
set, generated round, or arbitrary filesystem. Generated files and later rounds create no
diagnostics. Fixtures use the real built processor artifact and SPI under repository Kotlin
2.4.10/KSP 2.3.10 lines for KMP JVM execution. Android/native coverage is target-registration
only and does not claim Android/native KSP compilation.

## Discovery And Inputs

Source and resource discovery is model-driven:

- KMP projects use public `KotlinMultiplatformExtension.sourceSets` and production
  compilations to collect Kotlin, resources, and Compose resource directories.
- JVM projects use public `SourceSetContainer` production source sets.
- Android application and Android-KMP resource conventions are separate. The controlled
  Android-KMP convention uses public `KotlinMultiplatformAndroidLibraryExtension` namespace
  and `KotlinMultiplatformAndroidComponentsExtension` main-variant source model, publishing
  only main production Android resource roots; disabled resources publish none. It makes no
  variant namespace claim and uses no legacy AGP properties, AGP internals, or reflection.
  The existing Android application convention remains separate.

The checker normalizes module path, source-set/variant name, kind, and root-relative path.
It uses declared input files with relative path sensitivity. A missing public model element
is reported as an unsupported-project diagnostic; it is not discovered by parsing build
scripts or walking arbitrary source directories.

Resource ownership is module-local. Recognized KMP and Compose resource locations are
valid only for their discovered owning source set. A namespace field is checked only once
the module's public build model exposes a real namespace; no invented `library/` or
`model/` folder convention exists.

Compose uses public `ResourcesExtension.customDirectory(sourceSetName, Provider<Directory>)`
to configure each declared custom root. The convention-owned/project-owned registry publishes
those same declared roots; it does not call them effective roots and makes no claim to observe
independently configured effective custom roots. No internals or reflection are used.

## Policy

### Supported Resource And SQLDelight Inputs

The Compose convention owns Compose Resources public API access. It publishes standard
`src/<sourceSet>/composeResources` roots, explicit project-owned custom-root registry entries,
and each convention-declared configured namespace as immutable records. A blank declaration is
retained as an invalid registry fact for deterministic `ARCH-RESOURCE` diagnostics, but is not
passed to Compose Resources. The root checker never reads nested `ResourcesExtension` instances
or internal custom-directory maps. A custom root or namespace not represented by a
convention-published record is unsupported; Compose task/output maps and internals are not used.

SQLDelight discovery uses normalized records the controlled convention derives from public
`SqlDelightExtension.databases`, documented source-set `sqldelight` roots, and explicit
public `srcDirs`. Unknown, inconsistent, or unrepresentable effective roots produce a
deterministic unsupported/inconsistent diagnostic, never a silent ownership claim.

Package preservation is the contract: core model retains its current root, core UI retains
`ui` and `theme`, core database retains the current `library` root, and Library and
Playlists API/implementation retain their current `library` root as Task 4.1 requires.
Several modules may therefore permit the same root. Package validation asks only whether
the KSP-observed declaration package is permitted for that module by exact-or-dot match;
the processor reports violations during compilation.

SQLDelight ownership uses real physical signals together: configured SQLDelight plugin and
database, `.sq`/`.sqm`/schema artifacts in discovered SQLDelight source locations, and
owner driver configuration. Runtime/coroutine consumers, README text, and arbitrary file
names are not ownership signals. The owner transfer from `:shared` to `:core:database`
is atomic in Task 3.1.

Explicit API is a module-level public KGP setting:
`KotlinBaseExtension.explicitApi == ExplicitApiMode.Strict`. KGP applies that project
default to all non-test compilations. Effective per-compilation/task introspection is not
a supported checker contract and is out of scope.

iOS export validation reads public KMP Native framework export configuration and actual
project dependencies. The current allow-list is empty. Transitive framework exports fail.

## Diagnostics, Errors, And Cache Behavior

Every registry/model input collection and `architectureCheck` diagnostic is sorted.
Diagnostics retain rule ID, module, source-set/variant or configuration, and relevant
path/edge. KSP compilation errors are deterministic processor diagnostics and separate
from root-task diagnostics. Neither layer falls back to text scanning or partial
acceptance.

The Gradle checker must be configuration-cache compatible: no task action reads a live
Gradle model, mutable global state, or undeclared file. TestKit invokes standalone valid
and invalid `architectureCheck` fixtures twice with
`--configuration-cache --configuration-cache-problems=fail` and asserts applicable reuse.
KSP integration is verified through its supported production compilation invocation; the
root task makes no KSP cache or output-artifact claim.

## Verification

TestKit covers a valid transitional graph and isolated `architectureCheck` failures for
cycle, forbidden edge, resource, explicit API, SQLDelight, and iOS export. Android fixtures
prove main-variant static resource/namespace records are accepted while nested test,
`androidTest`, and test-fixture variants are excluded. The real Android RED fixture captures
exactly three direct self edges and their cycle; GREEN suppresses only the qualifying AGP
records while retaining canonical main resource records. Convention-published Android
role/cardinality assertions prove the public test-component configuration identities and
fail-closed single-record suppression. A separate explicit authored self-dependency fixture uses
a supported declarable configuration and remains negative. Compose fixtures prove standard roots,
registered custom roots, and unsupported namespace/root failures. KSP fixtures prove real
JVM/Android/native convention registrations are tooling while spoofed configuration names are
not; generated/test/local declarations are excluded. Production compilation fixtures prove
default/public property/annotation/multiline/backticked/raw-string declarations. Assertions count
and sequence exact relative-path diagnostics, including duplicate absence. Integration coverage
proves cache reuse for the standalone task and stable ordering. Full build-logic tests,
standalone `architectureCheck`, relevant production compilation/KSP integration commands,
Spotless, Detekt, strict OpenSpec validation, and `git diff --check` remain required.

Combined tests assert exact diagnostics including duplicate absence; public KMP/JVM/Android
records; Android main-versus-test variants; Compose standard/registered/unsupported
namespace/root cases; a RED proof of the former SQLDelight classloading failure; then GREEN
real-root `architectureCheck` success and physical-ownership fixtures for the
convention-owned plugin application; SQLDelight configured/default/explicit/rogue roots;
real KSP JVM/Android/native registrations versus spoofed configurations; generated/test/local
exclusions; KSP default/public properties, annotations, multiline/backticked forms, and raw
strings; exact diagnostic sequence/counts; and configuration-cache reuse.

## Migration And Rollback

Task 1.3 is accepted after the independent final Oracle re-review PASS. Its 46-test functional
XML, external canonical repository-built processor JAR/SPI/KSP proof, Android-KMP host/device
aggregate identity cases, and root configuration-cache reuse are recorded in the authoritative
Task 1.3 final acceptance report. The manually stopped (>9000 seconds) `init.sh` run was not
rerun and is not evidence of a full platform matrix. Task 1.4 remains next and out of scope.
A failed follow-up slice is reverted to the last passing checker boundary; it never gains a
permissive edge, text fallback, or Task 1.4 entrypoint wiring.

## Task 1.3 Implementation Scope

After the required follow-on approval, expected Task 1.3 implementation files are
`build-logic/convention/build.gradle.kts`,
`build-logic/convention/src/main/kotlin/build-logic.sqldelight.gradle.kts`,
`shared/build.gradle.kts` (the convention plugin declaration only),
`build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureModelRegistry.kt`,
`build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPlugin.kt`,
`ArchitectureCheckTask.kt`, `ArchitectureAllowList.kt`, and
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`.
The existing permitted root `settings.gradle.kts`, `architecture-processor`, KSP, and
relevant core/API convention paths remain limited to their approved responsibilities.
There is no automatic product-module migration. This scope excludes application source,
app module graph changes, root `build.gradle.kts`, root task wiring, `qualityCheck`, and CI.
