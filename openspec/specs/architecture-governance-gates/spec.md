## Purpose

Canonical architecture governance: convention plugins, executable graph/cycle gates, and TDD-oriented migration slices.

## Requirements


### Requirement: Canonical architecture governance exists

The repository SHALL maintain `skills/kmp-architecture/SKILL.md` and `docs/architecture.md` as canonical architecture guidance, ADRs for module boundaries and shared/iOS exports, and feature READMEs for feature ownership. `AGENTS.md` SHALL link to canonical guidance instead of duplicating it.

#### Scenario: A developer locates module rules
- **WHEN** a developer needs module ownership, dependency direction, or iOS export guidance
- **THEN** canonical documentation and the relevant ADR/feature README identify the rule and its owner
- **AND** repository instructions link to rather than restate the canonical rule.

### Requirement: Convention plugins and executable graph gates enforce policy

Build logic SHALL provide core, feature-api, feature-impl, Android, Compose Resources, and dedicated SQLDelight convention plugins. Shared build logic SHALL own an immutable normalized architecture-model registry. Controlled conventions SHALL own plugin application/classpaths and publish public-API facts; `architectureCheck` SHALL consume only registry records plus existing model inputs, never separately loaded plugin extensions. Android SHALL use public Android Components callbacks only for main-production static resource roots. It SHALL obtain module namespace from the concrete public Android DSL `CommonExtension.namespace` through `ApplicationExtension` or `LibraryExtension` as applicable, never AGP 9.3 `Variant`; nested test/`androidTest`/test-fixture components, casts, reflection, AGP task/artifact internals, and per-variant namespace claims are forbidden. Compose SHALL publish standard roots, explicit project-owned custom-root records, and convention-declared configured namespace; blank declarations SHALL remain invalid registry facts for deterministic `ARCH-RESOURCE` diagnostics and SHALL NOT be passed to Compose Resources. The root checker SHALL not access nested `ResourcesExtension` or internal maps, and SHALL make no effective-namespace or independent-introspection claim. The SQLDelight convention SHALL retain `app.cash.sqldelight:gradle-plugin:2.3.2` on build-logic `implementation`, apply/configure SQLDelight, and publish public database records. After Task 3.1, `:core:database` SHALL be the sole physical database, configuration, schema, migration, package/name, and platform-driver owner. Task 1.3's transitional state preserved those artifacts in `:shared` until that move. Core/API conventions SHALL record KSP tooling only after KSP is applied and their real production consumer is registered. Generic `ksp` is tooling only for single-platform JVM/Android; arbitrary or spoofed configuration names SHALL not be trusted. The processor SHALL consume normalized module/root args, filter generated/test/local boundaries where supported APIs and registration permit, and emit sorted deduplicated relative-path diagnostics; it SHALL not claim whole-project incremental aggregation. Task 1.3 root `architectureCheck` remains model-only/cacheable, consumes no KSP outputs, and owns no root `check`, CI, or `qualityCheck` wiring; that wiring is Task 1.4. Reflection, internals, classloader probing, and build-script parsing are forbidden. Detekt and Spotless SHALL continue.

For Task 1.3, the SQLDelight root task SHALL receive declared artifact file collections and
derive current physical artifacts during task execution, never snapshot `artifacts.files`
during configuration; direct owner-driver dependencies SHALL appear only in documented
production configurations. Compose SHALL configure every declared custom root through public
`ResourcesExtension.customDirectory(sourceSetName, Provider<Directory>)` and publish that same
declared-root registry plus its convention-declared configured namespace, without claiming an
effective-custom-root getter, effective namespace, or independent observation of unrelated
custom roots. A blank namespace declaration SHALL remain an invalid registry fact for
deterministic `ARCH-RESOURCE` diagnostics and SHALL NOT be passed to Compose Resources. A controlled Android-KMP convention, separate from the Android application
convention, SHALL use public `KotlinMultiplatformAndroidLibraryExtension` namespace and
`KotlinMultiplatformAndroidComponentsExtension` main-variant sources, publish only main
production roots, and publish none when resources are disabled. The graph SHALL retain self
`ProjectDependency` edges as both `ARCH-EDGE` and a one-node `ARCH-CYCLE`. The processor SHALL
process initial-round `Resolver.getNewFiles()` only and emit same-round deterministic diagnostics
identified by relative path, source position, and qualified name. Real KMP JVM processor/SPI
execution is required; Android/native coverage is target registration only, not compilation.

Android applications SHALL publish exact `(project, configuration)` identities only from public
AGP 9.3.1 `ApplicationVariant` test-component `Component.compileConfiguration` and
`Component.runtimeConfiguration` objects (`androidTest`, `unitTest`, `deviceTests`, and
`hostTests`). Android-KMP libraries SHALL publish them only from public
`KotlinMultiplatformAndroidLibraryTarget.compilations` and public
`KotlinMultiplatformAndroidHostTestCompilation`/
`KotlinMultiplatformAndroidDeviceTestCompilation`
`compileDependencyConfigurationName` and `runtimeDependencyConfigurationName`. After
normalization, root dependency collection SHALL suppress a direct self `ProjectDependency` only
when its project/configuration identity exactly matches a published identity and exactly one
distinguishable direct self record exists in that configuration; otherwise it SHALL suppress
none. `Configuration.isCanBeDeclared` SHALL NOT be a
predicate because declarable status provides no dependency provenance. Authored dependencies on
supported declarable buckets, including explicit self dependencies, SHALL remain checked; an
explicit self edge SHALL emit exact `ARCH-EDGE` and one-node `ARCH-CYCLE`. An equal authored
mutation on the exact AGP-owned configuration may collapse into AGP's set record and is
unsupported/outside the checker guarantee because no public provenance API exists.
Configuration-name inference, blanket test filtering, attribute guessing, reflection, AGP
internals, and task/artifact/output/resolved-classpath inspection are forbidden.

#### Scenario: TestKit detects an illegal architecture fixture
- **WHEN** Gradle TestKit runs the former SQLDelight classloader RED/GREEN fixture, cache reuse and cache-invalidated artifact ownership, direct production-driver placement, Android-KMP main/disabled resources, Compose `customDirectory` declared roots, the real Android app fixture, explicit authored self-dependency fixture, real KMP JVM processor/SPI execution, Android/native target registration, pre-existing generated source, generated/test/local exclusion, KDoc overload/path identity, default/public property/annotation-applied/multiline/backticked/raw-string, illegal graph, ownership, or export fixtures
- **THEN** the real Android RED fixture captures exactly three direct self edges and their cycle; GREEN suppresses only qualifying records while retaining canonical main resource records and asserting convention-published AGP role/cardinality plus fail-closed single-record behavior; the explicit authored self-dependency fixture on a supported declarable configuration remains negative, a cardinality control suppresses none, and RED/GREEN fixtures assert exact raw diagnostic sequence/counts and duplicate absence; the former classloading fixture fails before convention ownership; physical ownership and real-root cacheable `architectureCheck` GREEN fixtures pass without an Android/native KSP compilation claim
- **AND** registry facts are immutable, no KSP output is consumed, and a repeated root task reuses the configuration cache.

### Requirement: Migration slices are TDD-oriented and evidence-backed

Each migration task SHALL begin with a characterization or architecture RED test, apply the minimal GREEN move, run focused verification followed by architecture/Detekt/Spotless checks, and run `./init.sh` for graph, expect/actual, SQLDelight, resource changes, and final verification. Implementation SHALL update progress, roadmap, and ADR evidence and use conventional commits per independently reviewable slice.

#### Scenario: A completed slice records executable evidence
- **WHEN** an independently reviewable migration slice is completed
- **THEN** its task evidence identifies the RED/GREEN coverage and focused/full verification results
- **AND** strict OpenSpec validation and `git diff --check` pass before the change is presented as apply-ready or complete.

### Requirement: Prior architecture tracking is reconciled before migration

Before new modularization implementation, the change SHALL verify that `architecture-refactor` is 12/12 complete and reconcile the stale package-organization tracking against implementation commits `f0310e5`, `06f8a16`, and `adb1e3d`. It SHALL NOT redo those package moves.

#### Scenario: Migration begins from verified prior work
- **WHEN** the first modularization implementation task starts
- **THEN** its evidence records the status of the prior architecture/package work and the referenced implementation commits
- **AND** no duplicate package-move task is introduced.
