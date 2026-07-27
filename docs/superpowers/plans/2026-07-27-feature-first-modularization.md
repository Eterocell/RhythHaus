# Feature-First Modularization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` or `superpowers:subagent-driven-development` to execute this plan task-by-task. Complete one independently reviewable task at a time, with RED-to-GREEN evidence, before beginning the next task.

**Goal:** Gradually make RhythHaus a behavior-preserving feature-first KMP multi-module application, with canonical architecture guidance and executable Gradle architecture gates.

**Architecture:** Use a contract-first strangler. Every migration slice compiles before the next starts; contracts move before implementations; Kotlin package declarations remain unchanged during module moves. The final `:shared` is a thin KMP composition/iOS-framework facade owning `App()`, root Shell, route/Back arbitration, lifecycle, Koin assembly, and stable `MainViewController()`. Apps depend only on shared and shared alone composes feature implementations.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Gradle convention plugins/TestKit, SQLDelight, Koin, Detekt, Spotless, NavigationEvent, Android, desktop JVM/macOS, and iOS.

## Non-Negotiable Constraints

- Preserve UI and runtime behavior. Do not change Back, scanner, playback, playlist, backup, settings, schema/database name/migration history/FKs, Kotlin packages, DI framework, dependencies, toolchain, or supported platforms.
- No core/feature module may depend on `:shared` or an app. No feature may depend on another feature implementation. Cross-feature calls go through feature APIs only. No temporary `feature -> shared -> feature` bridge.
- Final core modules are demand-driven `:core:model`, `:core:ui`, `:core:database`, narrowly scoped `:core:platform`, and `:core:playback` when playback moves. Add `:core:navigation` only after a failing graph test proves a common destination-scoped contract is needed. Never create `:core:network`, an empty module, or empty state/event/effect/presenter types.
- Final features are `:feature:library:api/impl`, `:feature:playlists:api/impl`, and single modules for Now Playing, Search, and Settings. API/impl is justified only by a real stable contract.
- `:core:model` has immutable cross-feature projections only; `:core:ui` has reusable primitives/theme/artwork only; `:core:database` is sole owner of SQLDelight schema/driver/migrations/generated DB; `:core:platform` needs two-domain reuse. Library owns scanner/source/index/repository/UI/transient state; playlists owns repository/edit/backup/UI; playback engine/contracts belong to core playback.
- Preserve Back resolution exactly: modal -> edit -> active-page selection -> Now Playing -> route; exactly one transition per intent; active destination only; predictive gestures latch the exact destination and target; features publish only foremost dismissal; displayed-playlist deletion is destination invalidation, not Back. Shared root arbitration remains the owner.
- Every implementation module publishes a Koin `Module`; `:shared` alone assembles/starts Koin. SQLDelight `.sq`, migrations, drivers, and generated package move atomically. Resources move with feature namespace. iOS exports only public Swift/ObjC-facing modules, while existing `Shared`/`MainViewController()` remains stable.

## Module Dependency Contract

```text
:androidApp, :desktopApp -> :shared
:shared -> feature implementations + feature APIs + needed core modules
feature:impl -> own :api, allowed feature APIs, needed core modules
feature:api -> allowed core API/model only
core -> allowed core dependencies only
```

## Task 0.1: Reconcile Existing OpenSpec Work

**Scope:** Slice 0. Documentation/tracker evidence only; do not modify source or repeat package moves.

**Existing files:** `openspec/changes/architecture-refactor/tasks.md`; `openspec/changes/package-organization/tasks.md`; commits `f0310e5`, `06f8a16`, `adb1e3d`.

**Target files:** `progress.md`, `roadmap.md`, `docs/superpowers/specs/2026-07-27-feature-first-modularization-design.md`, `docs/superpowers/plans/2026-07-27-feature-first-modularization.md`, and the complete `openspec/changes/feature-first-modularization/` artifact set.

- [ ] Run `git show --stat --oneline f0310e5 06f8a16 adb1e3d` and `openspec status --change architecture-refactor --json`; record the 12/12 architecture-refactor result and the package-organization implementation evidence.
- [ ] This task cannot create a normal unit test because it reconciles historical artifacts. Create `docs/architecture.md` only in Task 1.1; for this task use `git diff --name-only f0310e5^..adb1e3d -- shared/src` as the concrete evidence script and save its exact output in tracker evidence.
- [ ] Confirm the package-organization task count is stale without changing it or moving any package. The acceptance inventory is the commit SHA, the affected path list, and the stated reason the already-landed move must not be repeated.
- [ ] Run `openspec validate feature-first-modularization --strict`; expected GREEN: exit 0 after tracker evidence is consistent.
- [ ] Run `git add progress.md roadmap.md docs/superpowers/specs/2026-07-27-feature-first-modularization-design.md docs/superpowers/plans/2026-07-27-feature-first-modularization.md openspec/changes/feature-first-modularization && git commit -m "docs: reconcile modularization prerequisites"` after review. This is the user-approved initial commit for the complete design, plan, and OpenSpec implementation contract.

## Task 1.1: Canonical Architecture Documents And ADRs

**Scope:** Slice 1 governance baseline. Documentation artifacts require a concrete shell validation, not a fictional Kotlin test.

**Existing files:** `AGENTS.md` (link-only update is deferred until documents exist); `docs/harness-engineering.md`.

**Target files:** `skills/kmp-architecture/SKILL.md`; `docs/architecture.md`; `docs/adr/0001-feature-first-module-boundaries.md`; `docs/adr/0002-shared-ios-export-policy.md`.

- [ ] Create the four target files with required headings, then run `test -s skills/kmp-architecture/SKILL.md && test -s docs/architecture.md && test -s docs/adr/0001-feature-first-module-boundaries.md && test -s docs/adr/0002-shared-ios-export-policy.md`; expected RED before creation: nonzero because targets do not exist.
- [ ] Document the graph, ownership, contract-first/package-preservation policy, resource namespace policy, Koin composition rule, narrow iOS export allow-list, no-empty-scaffold rule, and Dependency Analysis Plugin deferral.
- [ ] In the architecture skill require `AGENTS.md` to link to canonical guidance rather than duplicate it; do not edit `AGENTS.md` until its later dedicated implementation task.
- [ ] Run the same `test -s` command and `rg -n 'feature-first|ProjectDependency|MainViewController|SQLDelight' skills/kmp-architecture/SKILL.md docs/architecture.md docs/adr`; expected GREEN: each required concern has a concrete matching line.
- [ ] Run `./gradlew spotlessApply --configuration-cache`, then `./gradlew spotlessCheck --configuration-cache`, then `./gradlew detekt --configuration-cache`; expected GREEN: formatting is applied before separate Spotless and Detekt verification.
- [ ] Commit with `git add skills/kmp-architecture docs/architecture.md docs/adr && git commit -m "docs: define feature-first architecture governance"`.

## Task 1.1a: Link Root AGENTS To Canonical Architecture Guidance

**Scope:** Slice 1 governance follow-up. Preserve all repository-specific startup, OpenSpec/Superpowers, platform, verification, completion, and Nowledge Mem rules; add only a concise link to the canonical architecture guidance.

**Existing file:** `AGENTS.md:L1-L138`.

**Target file:** `AGENTS.md`.

- [ ] After Task 1.1 has created the canonical files, run `test -s skills/kmp-architecture/SKILL.md && test -s docs/architecture.md && test -s docs/adr/0001-feature-first-module-boundaries.md && test -s docs/adr/0002-shared-ios-export-policy.md`; expected RED before Task 1.1 completes: nonzero because the canonical files are absent.
- [ ] Add one short architecture-guidance link section to root `AGENTS.md` that points to `skills/kmp-architecture/SKILL.md`, `docs/architecture.md`, and both ADRs. Do not reproduce the architecture graph, ownership rules, or migration policy there; do not alter existing repository startup/OpenSpec/Superpowers instructions.
- [ ] Run `rg -n 'kmp-architecture|docs/architecture\.md|0001-feature-first-module-boundaries|0002-shared-ios-export-policy' AGENTS.md`; expected GREEN: one canonical-link section identifies all four targets.
- [ ] Run `git diff --word-diff=plain -- AGENTS.md` and verify the diff changes only that concise link section; expected GREEN: no existing startup, OpenSpec, verification, completion, or platform rule is removed/rewritten.
- [ ] Commit with `git add AGENTS.md && git commit -m "docs: link architecture guidance from agents"`.

## Task 1.2: TestKit Fixtures For KMP Convention Plugins

**Scope:** Slice 1 governance baseline. Build logic only.

**Existing files:** `build-logic/convention/build.gradle.kts`; `build-logic/convention/src/main/kotlin/build-logic.root-project.gradle.kts:L3-L7`; `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractPluginFunctionalTest.kt`.

**Target files:** `build-logic/convention/src/main/kotlin/build-logic.kmp.core.gradle.kts`; `build-logic/convention/src/main/kotlin/build-logic.kmp.feature.api.gradle.kts`; `build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts`; `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt`.

- [ ] Create the functional test and its embedded TestKit fixture project first. Assert that plugin ids `build-logic.kmp.core`, `build-logic.kmp.feature.api`, and `build-logic.kmp.feature.impl` are resolvable and that core/API have explicit API enabled.
- [ ] Run `./gradlew :build-logic:convention:test --tests '*KmpConventionPluginsFunctionalTest' --configuration-cache`; expected RED: plugin ids cannot resolve before scripts are created.
- [ ] Add only the three convention scripts and plugin registrations. The core/API convention contract is:
  ```kotlin
  kotlin { explicitApi() }
  ```
  The implementation convention configures KMP/quality but does not require leaf-internal declarations to be public.
- [ ] Run the same TestKit command; expected GREEN: fixture `help` succeeds and assertions see explicit API only where required.
- [ ] Run `./gradlew :build-logic:convention:test --configuration-cache`, then `./gradlew spotlessApply --configuration-cache`, then `./gradlew spotlessCheck --configuration-cache`, then `./gradlew detekt --configuration-cache`.
- [ ] Commit with `git add build-logic/convention && git commit -m "build: add KMP module conventions"`.

## Task 1.3: Stable Architecture Checker (Accepted)

**Scope:** Slice 1 governance baseline. After the required approval, shared build-logic
architecture-model registry; controlled Android/Compose/SQLDelight/KMP conventions and their
public-model records; root checker; normal JVM `:architecture-processor`; and focused
TestKit/KSP integration tests. No product module is automatically migrated. Application
source, root build/entrypoint wiring, `qualityCheck`, CI, and Task 1.4 are out of scope.

**Existing files:** root `settings.gradle.kts:L38-L43`; `gradle/libs.versions.toml` only if an existing KSP-line alias is required; `build-logic/convention/src/main/kotlin/build-logic.root-project.gradle.kts:L3-L7`; `build-logic/convention/src/main/kotlin/build-logic.kmp.core.gradle.kts`; `build-logic/convention/src/main/kotlin/build-logic.kmp.feature.api.gradle.kts`; `build-logic/convention/src/main/kotlin/build-logic.detekt.gradle.kts`; `build-logic/convention/src/main/kotlin/build-logic.spotless.gradle.kts`.

**Target files:** `build-logic/convention/build.gradle.kts`; controlled convention scripts
including `build-logic.sqldelight.gradle.kts`; relevant Android/Compose/KMP core/API
convention scripts; `shared/build.gradle.kts` (convention declaration only);
`build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureModelRegistry.kt`;
`ArchitectureCheckPlugin.kt`; `ArchitectureCheckTask.kt`; `ArchitectureAllowList.kt`; focused
architecture TestKit tests; root `settings.gradle.kts`; `architecture-processor` build/source/SPI;
and focused KSP integration tests. No application source, automatic product-module migration,
root `build.gradle.kts`/entrypoint, `qualityCheck`, CI, or Task 1.4 file is a target.

- [x] The lexical/declaration-index checker remains rejected. Independent final Oracle re-review PASSed after correction, accepting Task 1.3. `ArchitectureCheckPluginFunctionalTest` XML records 46 tests, 0 skipped, 0 failures, and 0 errors, including external canonical repository-built `:architecture-processor` JAR/SPI/KSP proof and real Android-KMP host/device aggregate identity positive, authored-self, and fail-closed cardinality cases. Root `architectureCheck` passed twice with configuration-cache reuse; strict feature OpenSpec validation, `spotlessCheck`, `detekt`, and `git diff --check` passed. `./init.sh` was manually stopped after more than 9000 seconds at user request and was not rerun, so the full JVM/Android/desktop/iOS matrix remains uncertain and is not claimed. Task 1.4 remains next and out of scope.
- [ ] Start with a focused RED TestKit fixture that demonstrates the former SQLDelight classloading failure when the checker attempts to inspect the public SQLDelight model without the convention-owned `implementation("app.cash.sqldelight:gradle-plugin:2.3.2")` classpath. Then add a GREEN physical-ownership fixture and real-root `./gradlew architectureCheck --configuration-cache` success after the convention owns application/configuration. Do not use reflection, internals, classloader probing, or build-script parsing in either production code or tests.
- [ ] After approval, write RED TestKit/KSP integration fixtures first. Standalone `architectureCheck` fixtures cover public-model graph/resource/SQLDelight/export/module-level explicit-API facts and cache reuse. Relevant production compilation fixtures cover KSP package-root and KDoc errors directly; they are not root-task diagnostics or KSP fact artifacts.
- [ ] Retain the rejected checker only as the baseline; do not delete or revert it. Run `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache` after adding focused TestKit/KSP integration regressions. Expected RED: package-root and public-KDoc production-compilation checks are absent or fail as specified because `:architecture-processor` and supported core/API KSP wiring do not yet exist. Gradle-model regressions may be RED against legacy behavior only where it violates this approved stable public-model contract. Do not assert a nonexistent plugin/allow-list precondition or require a broad rewrite merely to manufacture RED.
- [ ] Implement only the approved split lifecycle: a configuration-cache-safe public Gradle model checker plus a KSP processor that reports directly during production compilation through supported `ksp { arg(...) }`/target configurations. `architectureCheck` consumes no KSP output or task model. Do not parse `build.gradle.kts`, token-scan Kotlin, use compiler internals/standalone Analysis API, or scan arbitrary imports. Register a manually invokable root `architectureCheck`; root `check`, CI, and `qualityCheck` wiring belongs exclusively to Task 1.4.
- [ ] Create the shared build-logic-owned normalized `ArchitectureModelRegistry` first.
  Conventions, not the root checker, apply plugin APIs/classpaths and publish immutable facts;
  the root checker consumes a snapshot plus existing model inputs, never separately loaded
  plugin extensions. No reflection, internals, classloader probing, build-script parsing, or
  KSP-output consumption is permitted.
- [ ] Add the dedicated `build-logic.sqldelight` precompiled convention. Its build-logic dependency is `implementation("app.cash.sqldelight:gradle-plugin:2.3.2")`, never `compileOnly`; it applies/configures `app.cash.sqldelight`. `:shared` applies this convention instead of independently applying SQLDelight. Preserve one physical database and the existing configuration, schema, migrations, package, database name, and platform driver behavior. The shared build-logic implementation classpath must let `ArchitectureCheckPlugin`/`ArchitectureCheckTask` type-safely read public `SqlDelightExtension`/database models only. The root task receives declared artifact file collections and derives current physical artifacts during task execution; it must not snapshot `artifacts.files` during configuration, including across cache reuse. Owner driver signals are direct dependencies in a documented production configuration, never arbitrary/test/spoofed configurations.
- [ ] Task 1.3 permitted implementation scope after approval: root `settings.gradle.kts`; normal JVM `:architecture-processor` build/source/SPI descriptor; catalog aliases only if required for the existing KSP 2.3.10 line; relevant core/API conventions and production KSP consumer wiring; checker build logic; and focused TestKit/KSP integration tests. `build-logic:convention` is not a processor artifact, `:shared` receives no KSP source-policy gate, and no application/root-build/Task 1.4 file is in scope.
- [ ] The checker contract must collect and sort immutable `(consumerPath, configurationName, providerPath)` `ProjectDependency` records, including self edges. KMP core/API conventions apply KSP and register actual production consumers in the registry; only those direct processor dependencies are tooling. Generic `ksp` is valid only for single-platform JVM/Android; never infer or trust arbitrary configuration names. Runtime, implementation, API, test, spoofed, or unregistered placements remain `ARCH-EDGE`. `:core:model` and `:core:database` are mutually forbidden until separately approved migration changes the policy. A fixture that creates their cycle must receive both `ARCH-EDGE` and `ARCH-CYCLE`, with no permissive edge added merely to suppress the cycle; a self edge must likewise produce `ARCH-EDGE` and a one-node `ARCH-CYCLE`.
- [ ] Android applications publish exact `(project, configuration)` identities only from public AGP 9.3.1 `ApplicationVariant` test-component `Component.compileConfiguration`/`runtimeConfiguration` objects (`androidTest`, `unitTest`, `deviceTests`, `hostTests`). Android-KMP libraries publish them only from public `KotlinMultiplatformAndroidLibraryTarget.compilations` and public `KotlinMultiplatformAndroidHostTestCompilation`/`KotlinMultiplatformAndroidDeviceTestCompilation` `compileDependencyConfigurationName`/`runtimeDependencyConfigurationName`. After normalization, root collection suppresses a direct self `ProjectDependency` only for an exact project/configuration identity with exactly one distinguishable direct self record in that configuration; otherwise it suppresses none. Do not use `Configuration.isCanBeDeclared`: declarable status provides no dependency provenance. Authored dependencies on supported declarable buckets, including explicit self dependencies, remain checked and explicit self edges emit exact `ARCH-EDGE` plus one-node `ARCH-CYCLE`. An equal authored mutation on the exact AGP-owned configuration may collapse into AGP's set record and is unsupported/outside the checker guarantee because public APIs provide no provenance. Do not infer names, blanket-filter tests, guess attributes, use reflection/AGP internals, or inspect tasks/artifacts/outputs/resolved classpaths.
- [ ] The checker contract must include a testable edge predicate equivalent to:
  ```kotlin
  fun isAllowed(from: String, to: String): Boolean = to in allowList[from].orEmpty()
  ```
  plus public-model checks. Package roots are module-permitted and may overlap; compare an exact root or root followed by `.`. KSP validates those roots and KDoc during production compilation. Preserve the current root for core model, `ui`/`theme` for core UI, and the current `library` package for core database plus Library/Playlists API/impl. Kotlin compilation resolves symbols through its classpath; architectureCheck validates actual declared project edges and does not infer import providers. Discover production Kotlin/resources through public KMP `sourceSets`, JVM `SourceSetContainer`, and Android Components/variant APIs; exclude test variants and report missing public model elements as unsupported-project diagnostics.
- [ ] Android-KMP resource publication is a controlled convention separate from the Android application convention. It uses public `KotlinMultiplatformAndroidLibraryExtension` namespace and `KotlinMultiplatformAndroidComponentsExtension` main-variant source model, publishes only main production roots, and publishes no roots when resources are disabled. It never uses variant namespaces, legacy AGP properties, AGP internals, or reflection. Compose configures each declared custom root through public `ResourcesExtension.customDirectory(sourceSetName, Provider<Directory>)`, publishes that same declared-root registry, and records only its convention-declared configured namespace; there is no supported effective-custom-root getter or effective-namespace claim, and no claim to observe independently configured roots. A blank namespace declaration is retained as an invalid registry fact for deterministic `ARCH-RESOURCE` diagnostics and is not passed to Compose Resources. Unsupported namespace/root must fail deterministically. SQLDelight conventions publish public configured/default/explicit source-root facts and preserve sole physical `:shared` ownership until Task 3.1. Read strict explicit API from `KotlinBaseExtension.explicitApi == ExplicitApiMode.Strict`.
- [ ] KSP conventions pass normalized module/root arguments and register real production consumers. The processor is compilation-local and initial-input-only through `Resolver.getNewFiles()`: filter generated/test/local/out-of-boundary declarations where supported APIs/registration permit, emit same-round sorted/deduplicated diagnostics with relative path, source position, and qualified-name identity, and do not promise whole-project aggregation or later-round diagnostics.
- [ ] The focused matrix must include the former SQLDelight classloader RED/GREEN fixture; cache reuse and cache-invalidated artifact ownership; direct production-driver placement; Android-KMP main/disabled resources; Compose `customDirectory` declared roots; the real Android RED fixture with exactly three direct self edges and its cycle; GREEN suppression retaining canonical main resource records; convention-published AGP test-component role/cardinality and fail-closed single-record assertions; an explicit authored self-dependency fixture on a supported declarable configuration; a cardinality fail-closed control; real KMP JVM processor execution and SPI; Android/native target registration only; pre-existing generated source; generated/test/local exclusions; KDoc overload/path identity; default/public property/annotation-applied/multiline/backticked/raw-string declarations; exact raw diagnostic sequence/counts. Retain physical-ownership GREEN, real-root `architectureCheck` GREEN, and existing graph/SQLDelight/iOS fixtures.
- [ ] Run the targeted TestKit/KSP commands; expected GREEN: each invalid fixture emits only its exact applicable diagnostic, including both cycle diagnostics where required, and valid fixtures succeed. Then run `./gradlew architectureCheck --configuration-cache` without adding root `check`, `qualityCheck`, CI, shared KSP, or Task 1.4 wiring.
- [ ] After confirming the actual changed paths, stage only changed permitted paths from the Task 1.3 inventory (for example: `git add -- settings.gradle.kts architecture-processor gradle/libs.versions.toml build-logic/convention/src/main/kotlin/build-logic.kmp.core.gradle.kts build-logic/convention/src/main/kotlin/build-logic.kmp.feature.api.gradle.kts build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture`) and commit with `git commit -m "build: enforce architecture dependency gates"`; omit every listed path that did not change and do not stage excluded Task 1.4/root/application paths.

## Task 1.4: Wire Architecture Gate Into Quality Entry Points (Accepted)

**Scope:** Slice 1 governance baseline, after Task 1.3. Root quality integration only.

**Existing files:** `build-logic/convention/src/main/kotlin/build-logic.root-project.gradle.kts`; `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPlugin.kt`; architecture TestKit fixture; existing CI entrypoint if present.

**Target files:** root convention/root build files as required; `ArchitectureCheckPluginFunctionalTest.kt`; existing CI file only when it is the actual quality entrypoint.

- [x] RED/GREEN TestKit coverage proves root `check` and `qualityCheck` execute `architectureCheck`; valid and illegal entrypoints preserve exact `ARCH-*` diagnostics. A real child-project fixture uses built-in `Copy` sentinels to prove provider-aggregated child `detekt` and `spotlessCheck` execution, and its second inner `qualityCheck` build reuses the configuration cache.
- [x] Root `qualityCheck` provider-aggregates actual Detekt and Spotless check task providers from every project while retaining `architectureCheck`; dedicated unfiltered `.github/workflows/quality.yml` covers pull requests and pushes to `main` with exactly `./gradlew qualityCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` and no direct duplicate architecture/Spotless/Detekt command.
- [x] Acceptance verification: independent Oracle review PASS with no Critical/Important/Minor findings; serial `:architecture-processor:clean :architecture-processor:jar` and full `:build-logic:convention:test` with the canonical JAR property passed with configuration-cache reuse; `ArchitectureCheckPluginFunctionalTest` XML is 50/0/0/0. Production `qualityCheck` passed with 85 tasks; standalone `spotlessCheck`, `detekt`, `spotlessApply`, strict OpenSpec validation, and `git diff --check` passed. Root `qualityCheck` cache reuse is not claimed because unchanged Spotless/precompiled-script input invalidation stores valid entries without reuse.
- [x] Acceptance recorded in `.superpowers/sdd/2026-07-27-feature-first-modularization/task-1.4-final-acceptance-report.md` and committed conventionally with the accepted Task 1.4 file set. `./init.sh` was not rerun after its user-requested stop beyond 9000 seconds, so the full JVM/Android/desktop/iOS matrix remains uncertain. Task 1.3 is unchanged; Task 1.5+ remain pending.

## Task 2.1: Extract Core Model

**Scope:** Slice 2. Create `:core:model` only; preserve package declarations.

**Existing files:** `settings.gradle.kts:L38-L43`; `shared/build.gradle.kts:L120-L127,L191-L249`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt:L6-L115`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt:L28-L205`; `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`.

**Target files:** `core/model/build.gradle.kts`; `core/model/src/commonMain/kotlin/com/eterocell/rhythhaus/model/` only if package preservation permits the current package path; `core/model/src/commonTest/kotlin/com/eterocell/rhythhaus/model/ModelContractTest.kt`; updated `settings.gradle.kts`, `shared/build.gradle.kts`, and allow-list.

- [ ] Create `ModelContractTest.kt` first by relocating/copying only existing assertions for `Track`/`PlayableTrack` byte-array equality, hash code, and `AudioSource.stableKey`; run `./gradlew :core:model:allTests --configuration-cache`; expected RED: module/task does not exist.
- [ ] Add `include(":core:model")` and apply the core convention. Move only immutable cross-feature values. Do not move `librarySnapshot` (`MusicModels.kt:L83-L104`) until it no longer imports `LibraryTrack`, and do not move controller behavior from `Playback.kt:L209-L1114`.
- [ ] New cross-feature contracts retain packages and are explicit, for example:
  ```kotlin
  public sealed interface AudioSource { public val stableKey: String }
  ```
- [ ] Run `./gradlew :core:model:allTests --configuration-cache`; expected GREEN: model test passes. Then run `./gradlew architectureCheck :shared:jvmTest --configuration-cache` and quality checks.
- [ ] Acceptance inventory: only model sources/tests move; no repository, scanner, UI state, engine, `:shared` dependency, package rename, or behavior change.
- [ ] Commit with `git add settings.gradle.kts shared core/model build-logic && git commit -m "refactor: extract core model"`.

## Task 2.2: Extract Core UI

**Scope:** Slice 2. Reusable visual primitives/theme/artwork abstractions only.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkImage.kt`; `ui/ArtworkDecoder.kt`; `ui/BackChip.kt`; `ui/HausDialog.kt`; `ui/RhythHausTopAppBar.kt`; `theme/Theme.kt`; `theme/HausColors.kt`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt:L16-L336`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt:L185-L330`; `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkDecoder.android.kt`; `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkDecoder.jvm.kt`; `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkDecoder.ios.kt`.

**Target files:** `core/ui/build.gradle.kts`; matching `core/ui/src/{commonMain,androidMain,jvmMain,iosMain}/kotlin/...`; `core/ui/src/commonTest/kotlin/.../ArtworkContractTest.kt`; updated consumers/allow-list.

- [ ] Create the artwork contract test first from existing `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/ArtworkImageTest.kt` and `ArtworkCacheTest.kt`; run `./gradlew :core:ui:allTests --configuration-cache`; expected RED: absent module/task.
- [ ] Run bounded inventory discovery before moves: `rg --files shared/src/commonMain/kotlin/com/eterocell/rhythhaus/{ui,theme} shared/src/{androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/ui`. Acceptance inventory is reusable primitive/theme/artwork files plus every paired `ArtworkDecoder` actual; exclude all `library/ui/**`.
- [ ] Add module/build dependencies and move exactly the accepted inventory without package rename. Never move feature state, route, `LibraryAppState`, or `LibraryNavigation`.
- [ ] Run `./gradlew :core:ui:allTests :core:ui:compileKotlinJvm --configuration-cache`; expected GREEN. Run Android/desktop/iOS compilation, architectureCheck, Detekt, and Spotless.
- [ ] Commit with `git add core/ui shared settings.gradle.kts build-logic && git commit -m "refactor: extract core UI primitives"`.

## Task 3.1: Move The Sole SQLDelight Owner Atomically

**Scope:** Slice 3 database. No schema/name/history/FK changes.

**Existing files:** `shared/build.gradle.kts:L142-L152,L195-L207,L246-L248`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt:L5-L10`; `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/{RhythHausDatabase,LibrarySource,LibraryTrack,Playlist,ScanSession,ScanError}.sq`; `shared/src/commonMain/sqldelight/migrations/1.sqm`; `shared/src/commonMain/sqldelight/databases/1.db`; platform `LibraryDatabase.{android,jvm,ios}.kt`; existing tests `LibraryDatabaseIosTest.kt`, `SqlDelightLibraryRepositoryJvmTest.kt`, `PlaylistSqlDelightRepositoryJvmTest.kt`.

**Target files:** `core/database/build.gradle.kts`; matching common/platform database sources; moved SQLDelight inputs under `core/database/src/commonMain/sqldelight/`; `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ExistingDatabaseMigrationTest.kt`; updated settings/shared/feature build files and allow-list.

- [ ] Create the integration test first using the existing schema fixture and repositories, then run `./gradlew :core:database:jvmTest --tests '*ExistingDatabaseMigrationTest' --configuration-cache`; expected RED: module/test task absent.
- [ ] Move `.sq`, `1.sqm`, `1.db`, SQLDelight package config, `LibraryDatabase` expect/actuals, and driver dependencies in one change. Keep `SqlDelightLibraryRepository.kt` and `SqlDelightPlaylistRepository.kt` outside core database.
- [ ] The database seam remains explicit and package-stable:
  ```kotlin
  public expect class LibraryDatabase { public val driver: SqlDriver; public val database: RhythHausDatabase }
  ```
- [ ] Run `./gradlew :core:database:jvmTest :core:database:generateCommonMainRhythHausDatabaseInterface --configuration-cache`; expected GREEN. Then run iOS database test, architectureCheck, and `./init.sh`.
- [ ] Acceptance inventory: the six `.sq` files, migration, schema DB, package config, expect/actual drivers move together; SQL statements, database name, generated package, FKs, and migration history are unchanged.
- [ ] Commit with `git add core/database shared settings.gradle.kts build-logic && git commit -m "refactor: extract core database"`.

## Task 3.2: Conditional Core Platform

**Scope:** Slice 3 conditional; do not create an empty module.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Platform.kt`; `shared/src/{androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/Platform.{android,jvm,ios}.kt`; `AudioMetadata.kt` and platform actuals; `library/PlatformSourceAccess.kt` and its actuals.

**Target files:** only if justified, `core/platform/build.gradle.kts`, complete matching source sets, and `core/platform/src/commonTest/kotlin/.../PlatformCapabilityBoundaryTest.kt`; otherwise `docs/adr/` decision added in its tracker task.

- [ ] Run `rg -n '^expect |^actual ' shared/src/{commonMain,androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/{Platform.kt,AudioMetadata.kt,library/PlatformSourceAccess.kt}`. Acceptance inventory must name a candidate and two consuming domains; scanner/source access and backup document access do not count as core candidates.
- [ ] If no candidate meets the two-domain threshold, write the ADR decision and run `test ! -d core/platform`; expected GREEN: no speculative module. Commit `docs: defer core platform extraction`.
- [ ] If a candidate qualifies, first create `PlatformCapabilityBoundaryTest.kt`, run `./gradlew :core:platform:allTests --configuration-cache`; expected RED: absent module/task.
- [ ] Move the complete expect/actual family with the core convention, then run the same command; expected GREEN. Run Android/JVM/iOS compilation and architectureCheck.
- [ ] Commit conditional implementation with `git add core/platform shared settings.gradle.kts build-logic && git commit -m "refactor: extract core platform capability"`.

## Task 4.1: Publish Library And Playlist APIs

**Scope:** Slice 4 contracts only; implementations remain in shared until their migration tasks.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt:L3-L29`; `PlaylistRepository.kt:L16-L38`; `LibraryModels.kt`; `LibraryPlaybackSelection.kt`; `SqlDelightLibraryRepository.kt`; `SqlDelightPlaylistRepository.kt`; contract tests `LibraryRepositoryContractTest.kt`, `PlaylistRepositoryContractTest.kt`.

**Target files:** `feature/library/api/build.gradle.kts`; `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/`; `feature/playlists/api/build.gradle.kts`; `feature/playlists/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/`; matching API test directories under the same preserved Kotlin package path; settings/shared build dependencies and allow-list. The source directory is module-local, but every public declaration retains its current `com.eterocell.rhythhaus.library` Kotlin package; this task performs no source/package rename.

- [ ] Create API contract tests first, porting existing contract assertions, then run `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache`; expected RED: modules absent.
- [ ] Move only stable public interfaces/projections and add public KDoc, retaining `package com.eterocell.rhythhaus.library` for both Library and Playlist API declarations. Keep SQLDelight repositories, scanner, UI state, and mappers in current implementation ownership; do not create a `com.eterocell.rhythhaus.playlists.api` package.
- [ ] The intended stable shape is:
  ```kotlin
  public interface LibraryRepository { public fun tracks(): List<LibraryTrack> }
  public interface PlaylistRepository { public fun playlists(): List<Playlist> }
  ```
- [ ] Run the same API test command; expected GREEN. Run `./gradlew architectureCheck :shared:jvmTest --configuration-cache` to prove APIs do not depend on implementations.
- [ ] Commit with `git add feature/library/api feature/playlists/api shared settings.gradle.kts build-logic && git commit -m "refactor: publish library and playlist APIs"`.

## Task 4.2: Extract Core Playback Contracts

**Scope:** Slice 4 playback; preserve engine/controller behavior.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt:L28-L205,L209-L1114,L1132-L1197`; `PlaybackProcessLifecycle.kt`; platform `PlaybackEngine.{android,jvm,ios}.kt`; `PlaybackDispatchers.{android,jvm,ios}.kt`; `PlaybackControllerTest.kt`; `JvmPlaybackEngineTest.kt`; session tests under `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/`.

**Target files:** `core/playback/build.gradle.kts`; common/platform playback source sets; `core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackContractTest.kt`; dependent build files/allow-list.

- [ ] Copy/relocate controller and session characterization tests first and run `./gradlew :core:playback:allTests --configuration-cache`; expected RED: missing module.
- [ ] Split model values to core model where already eligible, engine seam/controller to core playback, and `FakePlaybackEngine` to playback test sources. Preserve all package names and no command/state behavior.
- [ ] Preserve signatures such as:
  ```kotlin
  public interface PlatformPlaybackEngine { public suspend fun loadPaused(track: PlayableTrack, generation: Long): LoadedPlayback }
  ```
- [ ] Run `./gradlew :core:playback:allTests :core:playback:compileKotlinJvm --configuration-cache`; expected GREEN, then run existing JVM engine/session tests, architectureCheck, and all target compilation.
- [ ] Commit with `git add core/playback shared settings.gradle.kts build-logic && git commit -m "refactor: extract playback contracts"`.

## Task 5.1: Move Now Playing Feature

**Scope:** Slice 5 first leaf feature.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt`; `NowPlayingBar.kt`; `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/NowPlayingArtworkBridge.kt`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt:L207-L330` for Back contract context.

**Target files:** `feature/nowplaying/build.gradle.kts`; matching common/iOS source sets; `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContractTest.kt`; shared composition changes only.

- [ ] Run `rg --files shared/src | rg '/nowplaying/|NowPlayingArtworkBridge'`; acceptance inventory is exactly the listed nowplaying source/test/resource files before moving them.
- [ ] Create `NowPlayingContractTest.kt` by asserting current commands/state behavior, then run `./gradlew :feature:nowplaying:allTests --configuration-cache`; expected RED: module absent.
- [ ] Add a presenter/state/event/effect only if existing screen state requires it; otherwise move composables directly. Feature entry must receive contracts from shared, never query shared or service-locate it.
- [ ] Run the focused command; expected GREEN. Then verify `:androidApp:assembleDebug`, `:desktopApp:compileKotlin`, iOS compilation, architectureCheck, and resource packaging.
- [ ] Commit with `git add feature/nowplaying shared settings.gradle.kts build-logic && git commit -m "refactor: extract now playing feature"`.

## Task 5.2: Move Playlists And Backup

**Scope:** Slice 5 second leaf feature; API already exists.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/{PlaylistScreens,PlaylistState,PlaylistPresentationPolicy}.kt`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/{PlaylistBackupService,PlaylistBackupCodec,PlaylistBackupUiState,PlatformPlaylistBackupDocuments}.kt`; platform `PlatformPlaylistBackupDocuments.{android,jvm,ios}.kt`; tests under `shared/src/{commonTest,jvmTest,iosTest}/kotlin/com/eterocell/rhythhaus/playlistbackup/`; Back tests `PlaylistBackPolicyJvmTest.kt`, `PlaylistEditModeSemanticsJvmTest.kt`.

**Target files:** `feature/playlists/impl/build.gradle.kts`; matching common/platform/test source sets; shared composition changes; feature README.

- [ ] Run `rg --files shared/src | rg '/(playlistbackup|library/ui/Playlist)|Playlist(BackPolicy|EditMode)'`; acceptance inventory is every playlist/backup source, platform actual, and matching test returned.
- [ ] Move the existing Back, edit, codec, and backup tests first; run `./gradlew :feature:playlists:impl:jvmTest --configuration-cache`; expected RED: module absent.
- [ ] Move repository implementation/edit/backup/UI together. Feature publishes its foremost modal/edit target only; shared keeps resolution. Keep displayed-playlist deletion as invalidation, not a Back callback.
- [ ] Run focused playlist/backup tests; expected GREEN. Run SQLDelight integration, Android packaging, desktop runtime resources, iOS backup/link tests, architectureCheck.
- [ ] Commit with `git add feature/playlists/impl shared settings.gradle.kts build-logic docs && git commit -m "refactor: extract playlists feature"`.

## Task 5.3: Move Search

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`; `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt`; route context `LibraryNavigation.kt:L5-L27,L169-L183`.

**Target files:** `feature/search/build.gradle.kts`; common/test source sets; shared composition updates; feature README.

- [ ] Create/relocate `SearchSelectionPoliciesJvmTest.kt` to feature test sources and run `./gradlew :feature:search:jvmTest --configuration-cache`; expected RED: module absent.
- [ ] Move the search screen and its direct dependencies found by `rg -n '^import com\.eterocell\.rhythhaus' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`; acceptance inventory is that file plus imports that are neither core nor a stable feature API.
- [ ] Keep route/Back arbitration shared and keep the feature single-module; do not create empty API/state classes.
- [ ] Run focused test; expected GREEN. Run Android/desktop/iOS compile/resource checks and architectureCheck.
- [ ] Commit with `git add feature/search shared settings.gradle.kts build-logic docs && git commit -m "refactor: extract search feature"`.

## Task 5.4: Move Settings

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/{SettingsScreen,AboutScreens}.kt`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.kt`; `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt`; `shared/build.gradle.kts:L15-L58,L80-L93,L129-L140`; resource `shared/src/commonMain/composeResources/files/aboutlibraries.json`.

**Target files:** `feature/settings/build.gradle.kts`; feature common/test/resources; settings build-info generator ownership; shared composition changes; feature README.

- [ ] Move/create settings test source first and run `./gradlew :feature:settings:allTests --configuration-cache`; expected RED: module absent.
- [ ] Run `rg -n 'RhythHausBuildInfo|aboutlibraries|ThemePreferenceStore' shared/build.gradle.kts shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme`; acceptance inventory names generator, about-libraries output, settings screens/tests, and only required preference seam.
- [ ] Relocate build-info/about libraries/resource ownership with Settings while preserving generated package/signature. Keep reusable theme primitives in core UI and preference platform implementation only where ownership is proven.
- [ ] Run focused settings tests; expected GREEN. Run generated build-info verification, Android/desktop/iOS resource checks, architectureCheck.
- [ ] Commit with `git add feature/settings shared settings.gradle.kts build-logic docs && git commit -m "refactor: extract settings feature"`.

## Task 6.1: Extract Library Implementation Last

**Scope:** Slice 6; no temporary reverse edge.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/{LibraryScanner,LibraryRepository,SqlDelightLibraryRepository,LibraryModels,PlatformSourceAccess,LibraryPlaybackSelection}.kt`; `library/ui/LibraryAppState.kt:L16-L336`; `LibraryNavigation.kt:L207-L330`; platform `PlatformSourceAccess.*`, `PathResolver.*`; tests `LibraryScannerTest.kt`, `LibraryRepositoryContractTest.kt`, `LibraryNavigationTest.kt`, `SqlDelightLibraryRepositoryJvmTest.kt`; `App.kt:L80-L120,L163-L180`. `LibraryDatabase.kt`, `LibraryDatabase.{android,jvm,ios}.kt`, SQLDelight inputs, migrations, and database integration tests are explicitly excluded because Task 3.1 moves them to `:core:database`.

**Target files:** `feature/library/impl/build.gradle.kts`; matching common/platform/test/resource source sets; shared root composition sources; feature README.

- [ ] Run `rg --files shared/src | rg '/library/|Library(SourceManagement|PlaybackSelection|Scanner|RepositoryContract|Navigation)Test'`; acceptance inventory is all returned library source/actual/test files classified as API or implementation, excluding `LibraryDatabase*.kt`, `sqldelight/**`, migrations, schema databases, and database integration tests already owned by Task 3.1.
- [ ] Relocate scanner/repository/Back characterization tests first; run `./gradlew :feature:library:impl:jvmTest --configuration-cache`; expected RED: module absent.
- [ ] Move only scanner/source/index/repositories/UI/transient state with complete `PlatformSourceAccess`/`PathResolver` families. `LibraryAppState` may publish a destination port, but shared remains resolver. Replace any `feature:library:impl -> :shared` callback with an API/event injected by shared. Do not move any `LibraryDatabase` common/platform driver source.
- [ ] Run focused library scanner/repository/Back tests; expected GREEN. Consume the already-extracted `:core:database` boundary for SQLDelight integration, then run Android/desktop/iOS startup/resources/DI smoke checks and architectureCheck; do not relocate or re-own database tests in this task.
- [ ] Commit with `git add feature/library/impl shared settings.gradle.kts build-logic docs && git commit -m "refactor: extract library feature"`.

## Task 6.2: Conditional Core Navigation

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt:L185-L330`; `LibraryAppState.kt:L87-L142,L262-L325`; `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`; playlist Back tests.

**Target files:** only if proven, `core/navigation/build.gradle.kts`, immutable contract source/test files; otherwise ADR update only.

- [ ] Create an architecture fixture that makes the alleged cross-feature contract dependency illegal, then run its TestKit test; expected RED: the current graph needs an illegal edge.
- [ ] If no such RED proof exists, do not create `:core:navigation`; document the decision and commit `docs: retain shared navigation arbitration`.
- [ ] If proven, create a test for immutable target identity/latching and run `./gradlew :core:navigation:allTests --configuration-cache`; expected RED: module absent.
- [ ] Extract only immutable contracts such as `DestinationId`/`BackTarget`; retain root precedence, predictive lifecycle, routes, and modal/edit state in current owners.
- [ ] Run navigation and playlist Back regressions; expected GREEN. Run architectureCheck and all targets.
- [ ] Commit conditional implementation with `git add core/navigation shared settings.gradle.kts build-logic && git commit -m "refactor: extract navigation contracts"`.

## Task 7.1: Thin Shared And Stable iOS Exports

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt:L80-L120`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt:L36-L85`; `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt:L7-L11`; `shared/build.gradle.kts:L159-L171`; entrypoints `androidApp/src/main/kotlin/com/eterocell/rhythhaus/{MainActivity,RhythHausApplication}.kt`; `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/main.kt`.

**Target files:** shared root/DI/iOS sources; shared build export configuration; architecture checker fixtures; feature Koin module files; final shared inventory test.

- [ ] Create TestKit/shared-inventory fixture first, asserting shared cannot own scanner/repository/feature UI/state and that export configuration rejects non-public modules; run targeted architecture TestKit test; expected RED: no final inventory/export policy exists.
- [ ] Replace centralized registrations with feature implementation module functions and have shared compose them. Preserve the shared start function contract:
  ```kotlin
  public fun startRhythHausKoin() { startKoin { modules(sharedAssemblyModule()) } }
  ```
- [ ] Reduce `App()` to composition/root Shell/arbitration. Preserve `MainViewController()` name and its `ComposeUIViewController { App() }` relationship. Export only modules with actual Swift/ObjC-facing declarations.
- [ ] Run TestKit/shared inventory; expected GREEN. Run Android/desktop/iOS startup, iOS link/framework public API check, architectureCheck, and `./init.sh`.
- [ ] Commit with `git add shared feature androidApp desktopApp build-logic && git commit -m "refactor: thin shared composition root"`.

## Task 7.2: Add Scaffold After Successful Migrations

**Existing files:** `build-logic/convention/build.gradle.kts`; convention scripts from Task 1.2; canonical skill `skills/kmp-architecture/SKILL.md`.

**Target files:** `build-logic/convention/src/main/kotlin/com/eterocell/gradle/scaffold/FeatureScaffoldPlugin.kt`; `build-logic/convention/src/test/kotlin/com/eterocell/gradle/scaffold/FeatureScaffoldPluginFunctionalTest.kt`; scaffold documentation in the canonical skill.

- [ ] Create a TestKit fixture first, run `./gradlew :build-logic:convention:test --tests '*FeatureScaffoldPluginFunctionalTest' --configuration-cache`; expected RED: scaffold plugin/task missing.
- [ ] Implement a command that creates only requested real module build/source/resource/test directories. Reject API generation unless the caller supplies an actual contract name; never generate empty `UiState`, `UiEvent`, `UiEffect`, or presenter classes.
- [ ] Run the targeted TestKit command; expected GREEN: a selected feature skeleton is created and empty-pattern assertions pass.
- [ ] Run full build-logic tests, architectureCheck, Detekt, and Spotless.
- [ ] Commit with `git add build-logic/convention skills/kmp-architecture && git commit -m "build: add feature module scaffold"`.

## Task 7.3: Final Evidence And Deferred Package Rename

**Existing files:** OpenSpec change tasks/specs, `progress.md`, `roadmap.md`, architecture docs/ADRs, all module build files and final architecture fixtures.

**Target files:** tracker/OpenSpec evidence and documentation only; no package rename sources.

- [ ] Add final negative TestKit fixtures for every forbidden edge/cycle and run `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache`; expected RED before the final allow-list has removed transitional edges.
- [ ] Remove only completed transitional allow-list entries and update architecture/ADR/feature README inventories. Record package renames and Dependency Analysis evaluation as deferred follow-ups, never as this change's work.
- [ ] Run targeted fixture tests; expected GREEN. Run `./gradlew architectureCheck qualityCheck --configuration-cache`, focused Back/scanner/playback/playlist tests, SQLDelight migration/FK integration, Android/desktop/iOS startup/resource/DI checks, and `./init.sh`.
- [ ] Run `openspec validate feature-first-modularization --strict` and `git diff --check`; record exact commands/results, module graph, thin-shared inventory, iOS export inventory, commit hashes, and blockers in tracker evidence.
- [ ] Commit with `git add docs openspec progress.md roadmap.md && git commit -m "docs: record modularization verification"`.

## Requirement-To-Task Coverage Matrix

| OpenSpec delta requirement | Tasks |
| --- | --- |
| `feature-first-modular-architecture` SHALL: buildable contract-first core/feature modules; conditional playback/navigation; no empty network/modules/types | 2.1, 2.2, 3.2, 4.1, 4.2, 5.1-5.4, 6.1, 6.2, 7.2 |
| `feature-first-modular-architecture` SHALL: shared/app dependency direction, composition, Koin, no forbidden feature edges | 1.3, 4.1, 5.1-5.4, 6.1, 7.1, 7.3 |
| `feature-first-modular-architecture` SHALL: core/feature ownership boundaries | 2.1, 2.2, 3.1, 3.2, 4.1, 4.2, 5.2, 6.1 |
| `feature-first-modular-architecture` SHALL: exact Back ordering/latching/invalidation | 5.2, 6.1, 6.2, 7.1, 7.3 |
| `feature-first-modular-architecture` SHALL: atomic SQLDelight, feature resources, narrow stable iOS exports | 3.1, 5.1-5.4, 6.1, 7.1, 7.3 |
| `architecture-governance-gates` SHALL: canonical skill/docs/ADRs/feature README and AGENTS linking | 1.1, 1.1a, 5.2-5.4, 6.1, 7.2, 7.3 |
| `architecture-governance-gates` SHALL: convention plugins, allow-list architectureCheck, quality/Detekt/Spotless | 1.2, 1.3, 1.4, 7.3 |
| `architecture-governance-gates` SHALL: RED/GREEN task discipline, focused/full verification, evidence, conventional commits | 0.1-7.3 |
| `architecture-governance-gates` SHALL: reconcile 12/12 architecture-refactor and stale package tracking without redo | 0.1 |
