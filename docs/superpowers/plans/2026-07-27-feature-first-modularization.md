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

- [x] Create `ModelContractTest.kt` first by relocating/copying only existing assertions for `Track`/`PlayableTrack` byte-array equality, hash code, and `AudioSource.stableKey`; the initial `./gradlew :core:model:allTests --configuration-cache` RED failed because the module was absent, and the later focused equality RED failed at both equal-content assertions before the historical equality/hash-code overrides were restored.
- [x] Add `include(":core:model")` and apply the core convention. Move only immutable cross-feature values. `librarySnapshot` (`MusicModels.kt:L83-L104`) remains in `:shared` because it imports `LibraryTrack`, and controller behavior remains in `Playback.kt:L209-L1114`.
- [x] New cross-feature contracts retain packages and are explicit, for example:
  ```kotlin
  public sealed interface AudioSource { public val stableKey: String }
  ```
- [x] Run `./gradlew :core:model:allTests --configuration-cache`; GREEN passed twice, with the repeat reporting `Reusing configuration cache`. JVM, Android host, and iOS simulator arm64 XMLs each record 6 tests, 0 skipped, 0 failures, and 0 errors. `./gradlew architectureCheck :shared:jvmTest --configuration-cache --configuration-cache-problems=fail --no-parallel` and separate quality checks passed.
- [x] Acceptance inventory: only model sources/tests move; no repository, scanner, UI state, engine, `:shared` dependency, package rename, or behavior change. `:shared` uses `api(projects.core.model)`; controller behavior, mapping/formatting helpers, repositories, scanners, UI state, engines, and feature-owned types remain in `:shared`.
- [x] Commit atomically: the accepted file set is committed by `refactor: extract core model` in the commit containing this ledger/report; no SHA is asserted before that commit exists.

## Task 2.2: Extract Core UI

**Scope:** Slice 2. Reusable visual primitives/theme/generic-artwork abstractions only; shared retains library artwork loading.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkImage.kt`; `ui/ArtworkDecoder.kt`; `ui/BackChip.kt`; `ui/HausClickable.kt`; `ui/HausDialog.kt`; `ui/RhythHausTopAppBar.kt`; `theme/Theme.kt`; `theme/HausColors.kt`; English and Chinese `back`/`back_button` Compose resources; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt:L16-L336`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt:L185-L330`; `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkDecoder.android.kt`; `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkDecoder.jvm.kt`; `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkDecoder.ios.kt`.

**Target files:** `core/ui/build.gradle.kts`; matching `core/ui/src/{commonMain,androidMain,jvmMain,iosMain}/kotlin/...`; `core/ui/src/commonMain/composeResources/` for its controlled `back`/`back_button` namespace; `core/ui/src/{commonTest,jvmTest}/kotlin/...` artwork/dialog contracts; package-stable shared-side artwork-loader source/tests; updated consumers/allow-list.

- [x] Failing-first contracts were created from the existing generic-artwork, theme, dialog, JVM semantics, BackChip, and top-app-bar coverage; `:core:ui` was initially absent, producing the required RED. `:shared` retains and continues loader contracts. Full RED/GREEN history is preserved in `.superpowers/sdd/2026-07-27-feature-first-modularization/task-2.2-implementation-report.md`.
- [x] The bounded inventory moved only reusable primitive/theme/generic-artwork files, `ui/HausClickable.kt`, `ui/HausDialog.kt`, paired `ArtworkDecoder` actuals, matching dialog tests, and English/Chinese `back`/`back_button` resources; `library/ui/**` was excluded.
- [x] `ArtworkImage.kt` ownership was split without package renaming: `ArtworkImageRole`, `artworkMemoryCacheKey`, and generic `ArtworkImage` are in `:core:ui`; `LocalTrackArtworkLoader`, `TrackArtworkLoadState`, its helpers, `LazyTrackArtworkImage`, `TrackArtwork`, and loader tests remain in package-stable shared `TrackArtworkImage.kt`.
- [x] `:core:ui` owns generic `ArtworkImage` role/cache-key rendering, the full `ArtworkDecoder` expect/actual/cache family, `BackChip`, `RhythHausTopAppBar`, `HausClickable`, `RhythHausThemeMode`, Haus palette resolution/locals, `HausColors`, the `HausDialog` family, localized Back resources, and public generated `Res`. `:shared` uses `api(projects.core.ui)`; Compose Foundation and Compose Resources are API dependencies; the iOS framework does not export core UI. The private `RhythHausTheme` app composition, theme preference storage, feature UI/state/routes/gestures/scrubber/glass chrome remain shared.
- [x] Earlier acceptance gates passed: `spotlessApply`; strict-cache `:core:ui:allTests :shared:jvmTest architectureCheck`; `:desktopApp:compileKotlin`; `:androidApp:assembleDebug` including packaged core UI resources; `:shared:compileKotlinIosSimulatorArm64`; Xcode 26.6 availability; standalone `spotlessCheck`; standalone `detekt`; strict OpenSpec validation; and diff checks. Retained XML aggregates are `:core:ui` 51 tests and shared JVM 562 tests, with zero failures/errors/skips. After two nonblocking cleanup corrections, the focused JVM/architecture gate passed with 31 tasks, then `./gradlew :core:ui:allTests :shared:jvmTest architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` passed with 93 tasks and configuration cache stored. Final cleanup-ledger re-review PASS found no Critical, Important, or Minor findings.
- [x] Task 2.2 acceptance is recorded in `.superpowers/sdd/2026-07-27-feature-first-modularization/task-2.2-final-acceptance-report.md`. Base commit was `53cc75c`; no Task 2.2 commit exists yet. The planned commit boundary is `refactor: extract core ui`.

## Task 3.1: Move The Sole SQLDelight Owner Atomically

**Scope:** Slice 3 database. No schema/name/history/FK changes.

**Existing files:** `shared/build.gradle.kts:L142-L152,L195-L207,L246-L248`; `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt:L5-L10`; `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/{RhythHausDatabase,LibrarySource,LibraryTrack,Playlist,ScanSession,ScanError}.sq`; `shared/src/commonMain/sqldelight/migrations/1.sqm`; `shared/src/commonMain/sqldelight/databases/1.db`; platform `LibraryDatabase.{android,jvm,ios}.kt`; database tests `LibraryDatabaseAndroidHostTest.kt` and `LibraryDatabaseIosTest.kt`; and shared repository behavior tests `SqlDelightLibraryRepositoryJvmTest.kt` and `PlaylistSqlDelightRepositoryJvmTest.kt`.

**Target files:** `core/database/build.gradle.kts`; matching common/platform database sources and database-owned tests; moved SQLDelight inputs under `core/database/src/commonMain/sqldelight/`; `core/database/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ExistingDatabaseMigrationTest.kt`; updated settings/shared build files and allow-list. Repository mapping, mutation, and persistence behavior tests remain in `:shared`.

- [x] RED ran in both required stages: absent `:core:database`, then absent seam/generated API compilation; owner fixtures were inverted while `:shared` remained owner. Core tests do not import `:shared` repositories.
- [x] The core migration suite uses v1 fixture/JDBC seeding and production/generated APIs to cover rows, versions, valid and invalid FK behavior, cascade, generated identity, and filename; legacy-v0, Android callback, and iOS FK coverage moved while repository behavior remains shared.
- [x] The six SQL files, `1.sqm`, v1 `1.db`, package config, seam, and drivers moved atomically to `:core:database`; package/name/dialect/schema/FK/history/filename bytes and shared `-lsqlite3` are preserved, and shared's unused coroutine extension is removed.
- [x] Public shared `LibraryDatabaseContext` forwards Android application context to the documented public core initializer with private core storage; Task 3.2 was not implemented.
- [x] `:shared` exposes core database via `api(projects.core.database)`; no app-to-core edge or shared iOS framework export exists.
- [x] Owner policy/TestKit baseline is `:core:database`; policy-derived checker expectations retain one/missing/two/arbitrary/spoofed and cache-reuse coverage.
- [x] The database seam remains explicit and package-stable:
  ```kotlin
  public expect class LibraryDatabase { public val driver: SqlDriver; public val database: RhythHausDatabase }
  ```
- [x] Core JVM/generation, Android-host, iOS simulator, architecture/quality, shared JVM, and consuming desktop/Android/iOS compilation gates passed; retained database XML is JVM 3, Android host 1, iOS simulator 1, and shared JVM 559, all zero failure/error/skip. Task 3.2 and later modules remain out of scope.
- [x] Acceptance inventory confirms the SQL inputs, fixture, generated/runtime surface, seam/actuals, and database tests are core-owned unchanged; reviewer re-review accepted the production boundary and byte compatibility.
- [x] Planned atomic commit boundary: the accepted set will be committed as `refactor: extract core database` in the commit containing this ledger; no SHA is asserted before that commit exists.

## Task 3.2: Conditional Core Platform

**Scope:** Slice 3 conditional; do not create an empty module.

**Existing files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Platform.kt`; `shared/src/{androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/Platform.{android,jvm,ios}.kt`; `AudioMetadata.kt` and platform actuals; `library/PlatformSourceAccess.kt` and its actuals.

**Target files:** only if justified, `core/platform/build.gradle.kts`, complete matching source sets, and `core/platform/src/commonTest/kotlin/.../PlatformCapabilityBoundaryTest.kt`; otherwise `docs/adr/` decision added in its tracker task.

- [x] Inventory qualified the package-stable `currentTimeMillis()` / `uuid4()` expect/actual family: time serves Library scanning and Playlist backup, while UUID serves Library scanning and Playback. Scanner/source access and backup document access did not qualify.
- [x] Not applicable: the no-candidate branch was not taken because the qualifying family meets the two-domain threshold; no ADR deferral or `docs: defer core platform extraction` commit was created.
- [x] Created `PlatformCapabilityBoundaryTest.kt` before module registration; `./gradlew :core:platform:allTests --configuration-cache` recorded the expected absent-project RED, then passed GREEN after the move.
- [x] Moved only the complete `currentTimeMillis()` / `uuid4()` expect/actual family to the core convention, preserving `com.eterocell.rhythhaus.library`; Android/JVM/iOS tests and compilation plus architectureCheck passed.
- [x] Committed the conditional implementation as `07da78e refactor: extract core platform capability`. `:shared` uses `api(projects.core.platform)`; there is no iOS export and core platform has no production dependencies.

## Task 4.1: Publish Library And Playlist APIs

**Scope:** Slice 4 contracts only. Create `:feature:library:api` and
`:feature:playlists:api`, but create no physical feature implementation module. Until later
migration tasks, repository implementations, persistence adapters/mappers, scanner/platform
seams, backup, UI state, validation helpers, playback-selection helpers, and Koin remain in
`:shared`.

**Existing files:** `settings.gradle.kts`; `shared/build.gradle.kts`;
`build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`;
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`;
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`;
`LibraryModels.kt`; `LibraryPlaybackSelection.kt`; `SqlDelightLibraryRepository.kt`; `PlaylistRepository.kt`;
`SqlDelightPlaylistRepository.kt`; and
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`. Existing behavior
coverage remains in `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt`,
`LibraryModelsTest.kt`, `LibraryPlaybackSelectionTest.kt`, and `PlaylistRepositoryContractTest.kt`,
plus `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`
and `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`.

**Target files:** `feature/library/api/build.gradle.kts`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibrarySource.kt`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryTrack.kt`,
`feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanModels.kt`,
and `feature/library/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/{LibraryApiContractTest,LibraryApiModelsTest}.kt`;
`feature/playlists/api/build.gradle.kts`,
`feature/playlists/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`,
and `feature/playlists/api/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistApiContractTest.kt`;
updated `settings.gradle.kts`, `shared/build.gradle.kts`, `ArchitectureAllowList.kt`, and
`ArchitectureCheckPluginFunctionalTest.kt`; and the listed shared contracts, implementations,
call sites, and contract/DI tests. The affected PlaylistSummary production call sites are
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`, and
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`. The affected tests are
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibrarySourceManagementTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt`,
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt`,
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt`,
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt`,
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`, and
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`.
Source directories are module-local, but all API declarations retain
`package com.eterocell.rhythhaus.library`; do not introduce a
`com.eterocell.rhythhaus.playlists.api` package.

- [x] RED API boundary/value tests first: author API-only tests in both new module test source sets before settings registration, then run `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache`; expected RED: absent-project failure. The Library API inventory is exact: `LibraryRepository.kt` contains `LibraryRepository` and `TrackUpsertResult`; `LibrarySource.kt` contains `LibraryPlatformKind`, `LibrarySourceAccessStatus`, and `LibrarySource`; `LibraryTrack.kt` contains `LibraryTrack`, `TrackArtwork`, and `toPlayableTrack()`; `LibraryScanModels.kt` contains `ScanStatus`, `ScanSession`, and `ScanError`. `LibraryRepository` exposes exactly `upsertSource`, `sources`, `upsertTrack`, `tracks`, `tracksForSource`, `artworkForTrack`, `insertScanSession`, `updateScanSession`, `insertScanError`, `scanErrors`, `removeMissingTracks`, `removeSource`, and `clearAll`. Include `LibraryPlatformKind` wherever the model requires it; use `LibrarySourceAccessStatus`, never nonexistent `SourceAccessStatus`. Keep shared `LibraryModels.kt` for `AudioScanCandidate` and `ScanProgress`.
- [x] `LibraryModelsTest` currently mixes shared scanner policy with API mappings. Keep `supportedAudioExtensionsAreCaseInsensitive` in shared. Move its two `LibraryTrack` mapping tests into API `LibraryApiModelsTest`, add `LibraryTrack` and `TrackArtwork` nullable-`ByteArray` content-equality/hash-code tests there, and do not duplicate those API-owned assertions in shared. Shared `LibraryRepositoryContractTest` and other repository contract tests remain shared because they exercise shared implementations. Playlist API tests must assert public `PlaylistEntry`, `PlaylistImportMutation`, `PlaylistSummary(id, name, createdAtEpochMillis, updatedAtEpochMillis)`, and all 11 `PlaylistRepository` methods; `playlists`, `playlist`, `create`, `createWithEntries`, and `importPlaylists` return `PlaylistSummary`, never generated `Playlist`.
- [x] RED architecture policy next: make the fixture's valid baseline omit the unconditional `:feature:playlists:api -> :core:model` dependency. Replace its misleading `Playlist` source under `:feature:library:api` with representative Library API source, add `PlaylistSummary` under `:feature:playlists:api`, and retain only valid shared dependencies in the baseline. Add the edge only in a dedicated `playlistsApiCannotDependOnCoreModel` mutation. While the allow-list still permits that edge, assert its expected failure, run `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest.playlistsApiCannotDependOnCoreModel' --configuration-cache`, and record expected RED because the build succeeds and misses `ARCH-EDGE`; then remove only the `:feature:playlists:api -> :core:model` allow-list entry and rerun that exact selector for GREEN. Retain `:feature:library:api -> :core:model`.
- [x] Keep fixture-only negative controls for API -> database/shared/implementation and implementation -> shared/other implementation, plus a positive shared -> implementation composition control; do not create production implementation modules. For API -> shared and API -> implementation reverse-edge controls, isolate construction of the tested edge or assert the exact accompanying `ARCH-CYCLE` diagnostics so the fixture is deterministic.
- [x] RED shared DI tests in `RhythHausDiTest` for absent internal factories and composition. The tests must resolve `LibraryRepository` to `SqlDelightLibraryRepository` and `PlaylistRepository` to `SqlDelightPlaylistRepository`, asserting both singleton identities. Separately prove factory ownership: the library factory owns the existing `TagLibReader`, `AudioMetadataReader`, `LibraryDatabase`, `LibraryRepository`, `PlatformSourceAccess`, and `LibraryScanner` bindings; the playlist factory owns `PlaylistRepository`. Preserve existing override behavior and reliably cancel the Koin scope/stop Koin in cleanup.
- [x] GREEN module wiring: register `:feature:library:api` and `:feature:playlists:api` in settings. Apply `build-logic.kmp.feature.api` and `build-logic.android.kmp.library`; configure JVM, Android host, `iosArm64`, and `iosSimulatorArm64` targets to match `:core:model`. Library API has only `:core:model` as a production project dependency. Playlist API has no production project dependency and exposes no `:core:database`, generated `Playlist`, `:core:model`, `:shared`, Koin, or SQLDelight type. Add public visibility and KDoc required by explicit API.
- [x] Move only the complete stable contracts/models into the API modules. Library moves the four-file inventory above, including `LibraryPlatformKind` and `LibrarySourceAccessStatus`, preserving nullable-byte-array equality/hash code and `toPlayableTrack()`. Keep `AudioScanCandidate`, `ScanProgress`, scanner/platform seams, playback-selection helpers, SQLDelight and in-memory implementations in shared. Playlist moves `PlaylistEntry`, `PlaylistImportMutation`, `PlaylistSummary`, and the 11-method `PlaylistRepository`; keep `PlaylistSnapshot`, backup models/snapshots, UI state, validation helpers, and repository implementations in shared.
- [x] Wire `:shared` through `api(projects.feature.library.api)` and `api(projects.feature.playlists.api)`, without exporting either API from the iOS framework. Adapt `SqlDelightPlaylistRepository` with a generated-`Playlist` -> `PlaylistSummary` mapper; update `InMemoryPlaylistRepository` and every listed production/test call site to the summary boundary while preserving persistence behavior.
- [x] Refactor only shared DI composition: add internal `libraryImplementationModule()` and `playlistsImplementationModule()` in `RhythHausDi.kt`, compose both from public `rhythHausModule()`, and leave shared as the sole Koin assembly/startup owner. The library factory owns the current TagLibReader, AudioMetadataReader, LibraryDatabase, LibraryRepository, PlatformSourceAccess, and LibraryScanner bindings; the playlist factory owns PlaylistRepository. API modules have no Koin dependency.
- [x] Run `./gradlew :feature:library:api:allTests :feature:playlists:api:allTests --configuration-cache`, then `./gradlew :shared:jvmTest --tests '*LibraryRepositoryContractTest' --tests '*PlaylistRepositoryContractTest' --tests '*RhythHausDiTest' --configuration-cache`; expected GREEN: exact API signatures/value behavior compile on their declared targets and shared implementation behavior/override behavior remains shared and passes.
- [x] Run `./gradlew :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --configuration-cache`. For full processor/convention integration, run `./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache`, then `./gradlew :build-logic:convention:test -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --configuration-cache`; build logic maps that project property to its test system property. Then run `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` twice and require strict configuration-cache reuse on the second run. Run `./gradlew :shared:jvmTest :feature:library:api:allTests :feature:playlists:api:allTests :feature:library:api:testAndroidHostTest :feature:playlists:api:testAndroidHostTest :feature:library:api:iosSimulatorArm64Test :feature:playlists:api:iosSimulatorArm64Test :androidApp:assembleDebug :desktopApp:compileKotlin :shared:compileKotlinIosSimulatorArm64 --configuration-cache --configuration-cache-problems=fail --no-parallel`. Then run `./gradlew spotlessApply --configuration-cache`, followed by standalone `./gradlew spotlessCheck --configuration-cache` and `./gradlew detekt --configuration-cache`. Run `openspec validate feature-first-modularization --strict` and `git diff --check -- docs/superpowers/plans/2026-07-27-feature-first-modularization.md` only for this planning update; task implementation must use its actual changed-file diff. Do not claim runtime UI or `./init.sh` unless run; OpenSpec 4.4 remains separate.
- [x] Independent review closed the evidence ledger; the actual implementation paths were committed as `9bd972f` (`refactor: publish library and playlist APIs`). No physical implementation modules were published in this task.

## Task 4.2: Extract Core Playback Contracts

**Scope:** Slice 4 playback. Create and register `:core:playback` without changing
package names, controller/engine/session behavior, Android service identity, Swift-facing
symbols, JNI symbols, native artifact names, or resource paths. `:shared` remains the sole
composition root and Xcode framework. The earlier Superpowers 4.4 wording remains separate;
OpenSpec item 5.3 is this extraction and item 5.4 is broader verification.

**Production inventory and ownership:** Move
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt` to
`core/playback/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`, keeping
`FakePlaybackEngine` unchanged in that production file for compatibility and whole-file/test
consumption. Do not move, duplicate, or redefine `PlayableTrack` or `AudioSource`: consume
both from `:core:model`. Move
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionController.kt`
and `PlaybackSessionSnapshot.kt` as complete files, including the behavioral
`PlaybackSessionController` port, `RevisionedPlaybackSessionSnapshot`,
`PlaybackSessionSnapshot`, `SessionQueueEntry`, `PlaybackSessionCodec`,
`PlaybackCheckpoint`, `ProgressCheckpointKey`, and normalization/value invariants; make the
port and types/methods shared composition consumes public. Move platform engine and
dispatcher files into matching core source sets:
`shared/src/androidMain/kotlin/com/eterocell/rhythhaus/{PlaybackEngine.android,PlaybackDispatchers.android}.kt`,
`shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/{PlaybackEngine.jvm,PlaybackDispatchers.jvm}.kt`,
and `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/{PlaybackEngine.ios,PlaybackDispatchers.ios}.kt`.
Move Android `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/RhythHausPlaybackService.kt`
and `RhythHausTransportBridge.kt`, iOS
`shared/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridge.kt` and
`NowPlayingArtworkBridge.kt`, JVM/macOS engine/bridge files, and native
`shared/src/nativeInterop/macos/rhythhaus_audio.mm` with their playback
resource/build-task wiring from shared to core. Preserve no cinterop; the existing
`clang++` `Exec` task remains, while shared retains unrelated build wiring. Preserve manifest-relative
`.RhythHausPlaybackService`, FQCN `com.eterocell.rhythhaus.RhythHausPlaybackService`,
transport FQCN `com.eterocell.rhythhaus.RhythHausTransportBridge`,
`MacAudioPlayerBridge`, JNI exports
`Java_com_eterocell_rhythhaus_MacAudioPlayerBridge_*`,
`librhythhaus_audio.dylib`, and resource roots `/native/macos-aarch64/` and
`/native/macos-x64/`.

**Shared retention and construction seam:** Keep
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`
(including reconciler/result/phase), `PlaybackSessionStore.kt`,
`shared/src/{androidMain,jvmMain,iosMain}/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionStore.{android,jvm,ios}.kt`,
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlaybackProcessLifecycle.kt`,
`Logger.kt`, `di/RhythHausDi.kt`, DataStore adapters, Koin composition, App/root
orchestration, artwork-loader composition, and the `LibraryTrack` adapter in shared. Split
the package-stable `createPlatformPlaybackEngine()` `expect`/`actual` family out of the
otherwise moved `Playback.kt` into explicit new
`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/PlatformPlaybackEngineFactory.kt` and
matching `PlatformPlaybackEngineFactory.{android,jvm,ios}.kt` facade files. It is solely a
composition facade: its Android actual delegates to public core
`createAndroidPlaybackEngine()`, JVM actual delegates to
`createJvmPlaybackEngine()`, and iOS actual delegates to
`createIOSPlaybackEngine(IOSRelativeFilePathResolver)`. Shared supplies
`IOSRelativeFilePathResolver` with `appLocalMusicFolderPath()` only for relative
`AudioSource.FilePath`; core handles absolute paths, URIs, and unsupported descriptors.
Engine implementation classes and helpers remain private/internal where current consumers
permit. `PlaybackController` requires an explicit
`PlatformPlaybackEngine` and has no default shared factory. Shared Koin calls its facade,
injects one engine into one controller, and proves `PlaybackSessionController` resolves to
that exact controller singleton; core never calls shared.

**Public API, explicit API, and KDoc inventory:** Preserve and document the common
cross-module surface in `Playback.kt`: `PlaybackStatus.{Idle,Loading,Buffering,Playing,Paused,Stopped,Error}`;
`RepeatMode.{RepeatOne,RepeatPlaylist,StopAfterCurrent,StopAfterQueue}`;
`ShuffleMode.{Off,On}`; `PlaybackError.{message,cause}`; `QueueOccurrence.{id,track}`;
`QueueMutationResult.Applied`; `QueueMutationResult.Rejected.reason`;
`QueueMutationRejection.{CurrentOccurrence,StaleOccurrence,InvalidTargetIndex,CommandsDisabled}`;
and `PlaybackState.{currentOccurrenceId,queue,status,positionMillis,durationMillis,repeatMode,shuffleMode,error,currentOccurrence,currentTrack,canPlay,isPlaying,progressFraction}`.
Do not expose internal `checkpointRevision`. Preserve `LoadedPlayback.{generation,durationMillis}`
and every `PlaybackEngineListener` callback: `onPlaybackStatus`, `onPlaybackProgress`,
`onPlaybackCompleted`, `onPlaybackError`, `onSkipToNext`, and `onSkipToPrevious`. Preserve
every `PlatformPlaybackEngine` member: `listener`, `loadPaused`, `clear`,
`setUserTransportEnabled`, `play`, `pause`, `stop`, `seekTo`, and `release`.

`PlaybackController` retains its explicit `PlatformPlaybackEngine` constructor boundary and
every public member: `state`, `checkpoints`, `setQueue`, `setOccurrenceQueue`, `selectTrack`,
`selectOccurrence`, `setRepeatMode`, `cycleRepeatMode`, `setShuffleMode`,
`toggleShuffleMode`, `play`, `pause`, `stop`, `seekTo`, `togglePlayPause`,
`restartCurrentTrack`, `skipToNext`, `skipToPrevious`, `reorderUpcoming`, `removeUpcoming`,
`clearUpcoming`, `release`, `setCommandsEnabled`, `sessionSnapshot`,
`awaitCheckpointFence`, `restoreSession`, and `reconcileSession`, plus its six public
`PlaybackEngineListener` overrides named above. Public production `FakePlaybackEngine`
remains unchanged with `listener`, `released`, all nine `PlatformPlaybackEngine`
members/overrides, `fail`, `complete`, and `activeGenerationForTest`. It stays
production/public. `playbackEngineDispatcher` remains internal.

Document public `createAndroidPlaybackEngine`, `createJvmPlaybackEngine`, and
`createIOSPlaybackEngine(IOSRelativeFilePathResolver)`. The retained shared
`createPlatformPlaybackEngine` facade remains outside the core public-KDoc gate, while its
package and signature remain stable. `PlaybackSessionController` is currently internal but
becomes public and documented as the cross-module behavioral port, with `checkpoints`,
`sessionSnapshot`, `restoreSession`, `reconcileSession`, `awaitCheckpointFence`, and
`setCommandsEnabled`. Preserve/document `RevisionedPlaybackSessionSnapshot.{snapshot,revision}`;
`PlaybackSessionSnapshot` primary properties
`{queue,currentOccurrenceId,positionMillis,repeatMode,shuffleMode}`, legacy constructor
parameters `{queueIds,currentTrackId,positionMillis,repeatMode,shuffleMode,legacyTrackIds}`,
and derived `{queueIds,currentTrackId}`; `SessionQueueEntry.{occurrenceId,trackId}`;
`PlaybackSessionCodec` constants `{maxIds,maxIdCharacters,maxIdUtf8Bytes,maxEncodedUtf8Bytes}`
and functions `{encodeSnapshot,decodeSnapshot,encodeQueue,decodeQueue,encodeIds,decodeIds}`;
`PlaybackCheckpoint.{snapshot,revision}`; `Immediate.{snapshot,revision}`;
`PlayingProgress.{key,snapshot,revision}`; and
`ProgressCheckpointKey.{generation,currentOccurrenceId,secondBucket}`.

On Android, document `setRhythHausAndroidContext`, `RhythHausPlaybackService`, its public
overrides `onCreate`, `onGetSession`, `onTaskRemoved`, and `onDestroy`, and
`createAndroidPlaybackEngine`; retain the current service FQCN and manifest. Transport,
controller, token, and request helpers remain internal/private and are not widened. Preserve
and document the iOS Swift surface:
`IOSAudioPlayerCompletionHandler.onPlaybackCompleted`;
`IOSAudioPlayerProvider.{completionHandler,load,play,pause,stop,seekTo,currentPositionMillis,currentDurationMillis,isPlaying,fadeOutAndStop}`;
`IOSAudioPlayerBridge.provider`; `NowPlayingArtworkProvider.setArtwork`;
`NowPlayingArtworkBridge.provider`; `IOSRelativeFilePathResolver.resolve`; and
`createIOSPlaybackEngine`. Engine, remote-command, and teardown helpers remain
internal/private. On JVM, document `createJvmPlaybackEngine`; the native engine,
`MacAudioPlayerBridge`, its methods/native declarations, progress helpers, and loader remain
internal/private with unchanged JNI identities.

Every public production declaration/member in the new strict core module, including overrides
and public constructor properties, requires explicit visibility/types and succinct
behavior-preserving KDoc regardless of whether shared currently consumes it. Internal/private
declarations do not need widening or public KDoc. Do not expand behavior or signatures.

**Build, dependency, and policy inventory:** Add `:core:playback` to
`settings.gradle.kts` and create `core/playback/build.gradle.kts` with the controlled core
and Android-KMP conventions used by existing core modules. Configure JVM, Android with
host tests, `iosArm64`, and `iosSimulatorArm64`, including the existing JVM 11 and Android
compile/min-SDK policy. Its common API dependencies are `api(projects.core.model)` and
`api(libs.kotlinx.coroutinesCore)`; implementation dependencies are
`implementation(projects.core.platform)` and `implementation(libs.kermit)`; Android owns
its Media3 dependencies. Move `nativeAudioResourceRoot`, `macosAudioResourceArch`,
`macosAudioHelperOutputFile`, `macosAudioHelperSourceFile`, `javaHomePath`,
`buildMacosAudioHelper`, the `jvmMain` generated-resource source directory, and the
`jvmProcessResources`/`processJvmMainResources` dependencies from shared to core. Preserve
the generated output
`build/generated/nativeAudioResources/jvmMain/native/$macosAudioResourceArch/librhythhaus_audio.dylib`.
Enable strict explicit API and add the private/internal core playback logger using
`Logger.withTag("RhythHaus")` so moved controller/iOS engine logging preserves tag and
behavior; shared `Logger.kt` remains compatibility-owned and core must not import shared
`log`. Core must never depend on shared, features, DataStore, Koin, or apps. Update
`shared/build.gradle.kts` to use `api(projects.core.playback)` and both iOS framework
declarations to export only this new core module in addition to existing allow-listed
exports. Update `ArchitectureAllowList.kt` for only
`:core:playback -> :core:model`/`:core:platform` and `:shared -> :core:playback`.

**Test inventory:** Move these tests into matching `core/playback` source sets while
preserving packages and adapting platform construction to the corresponding core factory,
never shared `createPlatformPlaybackEngine()`: common
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt` and
`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionSnapshotTest.kt`;
Android host `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt`
and `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/RhythHausTransportBridgeTest.kt`;
JVM `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`; and iOS
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt`,
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingBridgingTest.kt`,
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingDiagnosticTest.kt`,
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`, and
`shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSCommandEnabledAfterTargetTest.kt`. Add
`core/playback/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackContractTest.kt` for
public controller/session/factory contract characterization. Retain shared integration and
composition tests: `PlaybackSessionCoordinatorTest`, `PlaybackSessionStoreJvmTest`,
`RhythHausDiTest`, `RhythHausDiFactoryJvmTest`, `PlaylistLifecycleIntegrationJvmTest`,
`AppDispatcherJvmTest`, `AppScanCancellationTest`, and `LibraryPlaybackSelectionTest`.

- [x] Inventory the exact production, native, Gradle, test, and consumer paths above before
  edits. Include `settings.gradle.kts`, `shared/build.gradle.kts`,
  `core/playback/build.gradle.kts`, `ArchitectureAllowList.kt`, and
  `ArchitectureCheckPluginFunctionalTest.kt`; classify each as core move, shared retention,
  build/policy update, moved test, or unchanged consumer invariant. Confirm the Android app
  manifest/application and Swift consumers remain unchanged, and establish a clean baseline
  with `git status --short` and the relevant existing focused tests.
- [x] Before registering the module, create the core-owned test directories, relocate the
  listed module-owned tests, and add `PlaybackContractTest.kt`. Run
  `./gradlew :core:playback:allTests :core:playback:compileKotlinJvm --configuration-cache`.
  Expected RED: Gradle reports that `:core:playback` is absent. Do not relocate
  `FakePlaybackEngine` to test source.
- [x] Add a focused `ArchitectureCheckPluginFunctionalTest` functional fixture module that
  models `:core:playback`, strict explicit API, and a preserved-package
  `com.eterocell.rhythhaus`/`.session` source. Its valid candidate graph has
  `:core:playback -> :core:model`, `:core:playback -> :core:platform`,
  `:shared -> :core:playback`, and a positive `:shared` iOS export of
  `:core:playback`. This is architecture-policy RED, distinct from the absent-module
  compilation RED: before allow-list completion, run the positive-policy selector and require
  `ARCH-EDGE :core:playback [architecture] -> :core:model`,
  `ARCH-EDGE :core:playback [architecture] -> :core:platform`, `ARCH-PACKAGE` for the
  preserved `com.eterocell.rhythhaus`/session source under current `.playback`-only
  ownership, and `ARCH-IOS-EXPORT :shared -> :core:playback`.
- [x] GREEN that policy by adding only core-playback outgoing edges to model/platform,
  retaining existing `:shared -> :core:playback`, changing the core-playback package-root
  policy to package-stable `com.eterocell.rhythhaus` covering `.session` and existing
  subpackages, and making `allowsIosExport` permit only
  `modulePath == ":shared" && exportedProjectPath == ":core:playback"`. The checker is
  fail-closed: add and run `corePlaybackCannotDependOnShared` as immediate characterization
  GREEN with `./gradlew :build-logic:convention:test --tests
  '*ArchitectureCheckPluginFunctionalTest.corePlaybackCannotDependOnShared'
  --configuration-cache`; expected GREEN because the malformed fixture is rejected with
  exactly `ARCH-EDGE :core:playback [architecture] -> :shared`. Retain
  `UnapprovedIosExport` of `:core:model` as a failing negative control.
- [x] Before production construction changes, add compilable shared characterization for the
  explicit-engine `PlaybackController` shape and one-engine/one-controller/exact-session-
  controller identity in `RhythHausDiTest`/`RhythHausDiFactoryJvmTest`. Run `./gradlew
  :shared:jvmTest --tests '*RhythHausDiTest' --tests '*RhythHausDiFactoryJvmTest'
  --configuration-cache`; expected baseline GREEN because this captures the existing
  singleton behavior before relocation. Do not attempt an impossible Kotlin compile-negative
  test; the absent-module and architecture-policy cases provide the task's RED evidence, and
  explicit API plus these composition tests characterize the constructor boundary.
- [x] GREEN the module wiring and moves: register and configure `:core:playback`; apply the
  KMP/core conventions and exact API/implementation dependencies; move the complete common,
  session, Android/JVM/iOS, native, and test inventories; move native resource/build tasks;
  add the Kermit logger; and preserve every Kotlin package. Keep `FakePlaybackEngine` in
  core production `Playback.kt`. Create public core factories, private/internal engine
  implementations, and the core iOS resolver port. Add the explicit shared facade files and
  delegate each platform actual to its core factory. Adapt shared Koin to inject the facade
  singleton into explicit-engine `PlaybackController`; retain shared store/coordinator/
  lifecycle/DataStore/App/logger/adapter ownership.
- [x] Run focused GREEN checks: `./gradlew :core:playback:allTests
  :core:playback:compileKotlinJvm --configuration-cache`; `./gradlew
  :core:playback:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest'
  --configuration-cache`; `./gradlew :core:playback:testAndroidHostTest
  --configuration-cache`; and `./gradlew :core:playback:iosSimulatorArm64Test
  --configuration-cache`. Run retained shared JVM selectors where applicable:
  `./gradlew :shared:jvmTest --tests '*PlaybackSessionCoordinatorTest' --tests
  '*PlaybackSessionStoreJvmTest' --tests '*RhythHausDiTest' --tests
  '*RhythHausDiFactoryJvmTest' --tests '*PlaylistLifecycleIntegrationJvmTest' --tests
  '*AppDispatcherJvmTest' --tests '*AppScanCancellationTest' --tests
  '*LibraryPlaybackSelectionTest' --configuration-cache`.
- [x] Run full architecture verification: `./gradlew :architecture-processor:clean
  :architecture-processor:jar --configuration-cache`, then `./gradlew
  :build-logic:convention:test
  -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
  --configuration-cache`. Run `./gradlew architectureCheck --configuration-cache
  --configuration-cache-problems=fail --no-parallel` twice and require configuration-cache
  reuse on the second invocation.
- [x] Run the cross-target consumer matrix: `./gradlew :core:playback:allTests
  :shared:jvmTest :androidApp:assembleDebug :desktopApp:compileKotlin
  :shared:compileKotlinIosSimulatorArm64 :core:playback:compileKotlinIosArm64
  :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64
  --configuration-cache`. The two link tasks are Kotlin/Native Shared framework/export
  linkage evidence, not Swift compilation. Then run `/usr/bin/xcrun xcodebuild -version`,
  followed by `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp
  -configuration Debug -destination 'generic/platform=iOS Simulator'
  CODE_SIGNING_ALLOWED=NO build` as Swift consumer compilation evidence. If it is unavailable
  or fails, record the exact blocker and do not claim it passed. Then run `./gradlew
  :core:playback:iosSimulatorArm64Test :shared:iosSimulatorArm64Test
  --configuration-cache`. Confirm Android packaging, desktop compilation, Kotlin/Native
  framework linkage/export, Swift consumer compilation when successful, and iOS simulator
  coverage; do not claim runtime launch, runtime UI, or `./init.sh` unless actually run.
- [x] Run `./gradlew spotlessApply --configuration-cache`, followed by standalone
  `./gradlew spotlessCheck --configuration-cache` and `./gradlew detekt
  --configuration-cache`. Before acceptance, independently review the plan, design, and
  OpenSpec alignment: `openspec/changes/feature-first-modularization/design.md` distinguishes
  Library/Playlist shared implementations from atomic core playback implementation ownership.
  Run `openspec validate feature-first-modularization --strict`; OpenSpec items 5.3 and 5.4
  and Superpowers acceptance 4.4 remain unchecked until their actual evidence. Run actual
  changed-file `git diff --check`;
  this planning amendment only uses
  `git diff --check -- docs/superpowers/plans/2026-07-27-feature-first-modularization.md`.
- [x] Final durability sequence: implementation commit `ab1768c` (`refactor: extract playback contracts`) exists; the separate docs closeout commit carries tracked ledgers and force-added ignored evidence files, with no docs SHA claimed.
   The following sequence replaces the legacy single-commit workflow.
   Step 1 is complete as `ab1768c`; Step 2 is the current accompanying docs closeout action.
   1. Complete implementation-only commit from the approved implementation paths (done as `ab1768c`):
      `git add core/playback shared settings.gradle.kts build-logic && git commit -m
      "refactor: extract playback contracts"`. Exclude planning, ledger, and evidence paths.
   2. Update SHA references, then force-add ignored
      `.superpowers/sdd/2026-07-27-feature-first-modularization/task-4.2-brief.md`,
      `task-4.2-report.md`, and `task-4.2-final-acceptance-report.md`; commit them with the
      tracked plan, OpenSpec, root/Superpowers progress, and roadmap closeout changes using a
      separate conventional `docs:` commit.

## Task 5.1: Move Now Playing Feature

**Scope:** Slice 5 first leaf feature; one atomic UI extraction plus the small reusable
`core/ui` moves required to preserve dependency direction. Accepted in implementation commit
`28dd2e1` (`refactor: extract now playing feature`) after independent scope and behavior approval.
The separate documentation closeout records retained automation evidence only; runtime UI/playback,
desktop launch, Android/iOS device/runtime validation, and `./init.sh` remain unclaimed.

**Frozen constraints:**

- Create exactly one UI-only implementation module, `:feature:nowplaying`. Do not create an API
  module, presenter, `UiState`/`UiEvent`/`UiEffect` scaffolding, or Koin `Module`; the feature owns
  no injectable bindings and is composed through callable/composable entry points. Only `:shared`
  assembles and starts Koin, and no service-locator back-reference is permitted.
- Target exactly Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64`. Create no feature
  `iosMain` source and no feature framework export. Preserve the sole Shared framework,
  `MainViewController`, and existing Swift-visible playback bridge identities.
- The only direct project dependencies are exactly `api(:core:playback)` and `api(:core:ui)`.
  The feature must not depend directly on `:core:model`, `:shared`, `:taglib`, Library API,
  Library implementation, another feature implementation, or an app. `Track` comes through the
  approved playback public surface rather than a direct core-model edge.
- Set Android namespace exactly to `com.eterocell.rhythhaus.nowplaying` and Compose resource
  package exactly to `rhythhaus.feature.nowplaying.generated.resources`.
- Preserve Kotlin packages, UI behavior, route/Back behavior, playback bridge identities, Swift
  surface, resource localization, and existing unused compatibility parameters. Compilation,
  linking, packaging, and Swift-consumer evidence do not claim runtime UI or playback validation.

**Exact file map:**

- Create/register `feature/nowplaying/build.gradle.kts` and the exact source roots
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/`,
  `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/`,
  `feature/nowplaying/src/androidHostTest/kotlin/`, `feature/nowplaying/src/jvmTest/kotlin/`,
  `feature/nowplaying/src/iosTest/kotlin/`, and
  `feature/nowplaying/src/commonMain/composeResources/values/` plus `values-zh/`.
- Move implementation from
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt` and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt` into the
  feature while preserving package `com.eterocell.rhythhaus.nowplaying`. Keep shared's public
  `NowPlayingScreen(track, playbackState, playbackController, tagLibReader, currentLibraryTrack,
  onBack, modifier)` facade and have it delegate to distinctly named feature `NowPlayingContent`.
  `LibraryAppShell.kt` remains the shared composition boundary and route/shell owner.
- Move
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt` to
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`,
  and `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt` to
  `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`.
  Keep both declarations package-stable as `com.eterocell.rhythhaus.ui`; do not rename them.
- Move `NowPlayingAdaptiveLayoutMode` and `nowPlayingAdaptiveLayoutModeFor` from
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt` into
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingAdaptiveLayout.kt`.
  Their new package is `com.eterocell.rhythhaus.nowplaying`; their old Library package is not a
  permitted feature package root. Shared retains route state,
  `LibraryBackTarget.NowPlaying`, Back arbitration/predictive Back, shell measurement/visibility,
  route dispatch, navigation effects, and shell overlay policy.
- Move `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt` package-
  stably to `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt`,
  retaining generic `leftEdgeSwipeBack(onBack: () -> Unit)` and caller-owned callbacks.
- Move `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt` package-
  stably to `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt`,
  and replace Miuix-facing signatures with the opaque core-ui `RhythHausBackdrop` API described
  below. Move `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt`
  to `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt` and
  make its stateless mechanism accept direction and terminal callback from local adapters.
  Name the direction type `VerticalSheetGestureDirection`; do not leave `verticalSheetGesture`
  shared-only or expose navigation APIs from core UI.

**Exact build contract:**

- Add `gradle/libs.versions.toml` alias
  `compose-animation = { module = "org.jetbrains.compose.animation:animation", version.ref = "compose-multiplatform" }`
  under `[libraries]`, following the existing hyphenated Compose alias convention and the
  existing `compose-multiplatform` version key. Create `feature/nowplaying/build.gradle.kts` with
  this exact plugin/convention block:

  ```kotlin
  plugins {
      id("build-logic.kmp.feature.impl")
      id("build-logic.android.kmp.library")
      id("build-logic.compose-resources")
      alias(libs.plugins.compose.compiler)
  }

  extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") {
      namespace("rhythhaus.feature.nowplaying.generated.resources")
  }
  ```

  The `build-logic.kmp.feature.impl` convention applies Kotlin Multiplatform and provides the
  general production-KSP wiring specified later; it does not configure this feature's targets,
  source sets, or dependencies.
- In `feature/nowplaying/build.gradle.kts`, inside `kotlin { ... }`, configure exactly:

  ```kotlin
  android {
      namespace = "com.eterocell.rhythhaus.nowplaying"
      compileSdk = libs.versions.android.compileSdk.get().toInt()
      minSdk = libs.versions.android.minSdk.get().toInt()
      compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
      withHostTest {}
      androidResources { enable = true }
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  ```

  Use the repository's catalog accessors for the exact existing compile/min SDK keys. Do not use
  legacy Android target DSL, do not apply `com.android.library`, create no feature framework binary/export,
  and create no feature `iosMain` production source.
- The exact `commonMain.dependencies`, `commonTest.dependencies`, and `jvmTest.dependencies`
  blocks below live in `feature/nowplaying/build.gradle.kts`; they are not supplied by the
  `build-logic.kmp.feature.impl` convention. The convention KSP change remains the separate
  general-governance step specified later.
- In `commonMain.dependencies`, declare exactly `api(projects.core.playback)`,
  `api(projects.core.ui)`, `api(libs.compose.runtime)`, `api(libs.compose.ui)`,
  `api(libs.compose.foundation)`, `api(libs.compose.components.resources)`, and
  `api(libs.compose.animation)`. The direct animation dependency is required because
  `Animatable<Float, AnimationVector1D>` is public. Declare implementation-only
  `libs.compose.material.icons.extended`, `libs.compose.material3`, `libs.miuix.ui`, and
  `libs.kotlinx.coroutinesCore`.
- In `commonTest.dependencies`, declare `implementation(libs.kotlin.test)`. In `jvmTest.dependencies`,
  declare the same UI-test artifact and `compose.desktop.currentOs` used by `:core:ui`. Android
  host and iOS tests inherit `commonTest` and receive no extra direct dependency; a task discovery
  or compiler result requiring one is a blocker, not a scope decision.
- **Core UI dependency closure:** in `core/ui/build.gradle.kts`, retain all existing dependencies
  and add one distinct `commonMain.dependencies` subsection with exactly
  `api(libs.compose.animation)`, because `Animatable<Float, AnimationVector1D>` appears in public
  core-ui signatures; `api(libs.kotlinx.coroutinesCore)`, because `CoroutineScope` appears in a
  public core-ui signature; and `implementation(libs.miuix.blur)`, because `LayerBackdrop` and
  blur implementation remain internal. Do not duplicate an existing declaration. The single
  `compose-animation` catalog alias added above supports both `:feature:nowplaying` and `:core:ui`.
- Remove all feature imports of `LayerBackdrop`, `LazyTrackArtworkImage`, shared `Res`,
  `TrackArtwork`, Library, or TagLib types. Keep
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` and App's
  `LocalTrackArtworkLoader` provider in shared for other consumers. Adapt
  `{ trackId -> LocalTrackArtworkLoader.current(trackId)?.bytes }` at the shared facade/shell call
  site; `App.kt` and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` remain unchanged
  and shared-owned.

**Public interfaces and explicit API:**

- Every declaration in the new feature and moved core-ui files uses explicit visibility. Every
  public feature and core-ui symbol has declaration-specific behavioral KDoc explaining its
  observable contract, callback/gesture direction, state/nullability behavior, and preservation
  constraints. Do not use generic file-level KDoc as a substitute.
- Feature public declarations, all in `com.eterocell.rhythhaus.nowplaying`, are exactly:

  ```kotlin
  /** Renders expanded Now Playing and sends the generic left-edge callback to [onBack]. */
  @Composable
  public fun NowPlayingContent(
      track: Track,
      playbackState: PlaybackState,
      playbackController: PlaybackController,
      labels: NowPlayingScreenLabels,
      artworkLoader: suspend (String) -> ByteArray?,
      onBack: () -> Unit,
      modifier: Modifier = Modifier,
  ): Unit

  /** Renders the shell-composed mini-player and emits upward expansion only through [onExpand]. */
  @Composable
  public fun NowPlayingBar(
      track: Track?,
      playbackState: PlaybackState,
      labels: NowPlayingBarLabels,
      artworkLoader: suspend (String) -> ByteArray?,
      onPlayPause: () -> Unit,
      onExpand: () -> Unit,
      onSettings: () -> Unit,
      onSearch: () -> Unit,
      expandProgress: Animatable<Float, AnimationVector1D>,
      isExpanded: Boolean,
      interactive: Boolean = true,
      screenHeightPx: Float = 0f,
      backdrop: RhythHausBackdrop? = null,
      modifier: Modifier = Modifier,
  ): Unit

  /** Selects the mini-player behavior for a loaded track or an empty library. */
  public enum class BottomBarMode { TrackLoaded, EmptyLibraryNavigation }

  /** Returns [BottomBarMode.EmptyLibraryNavigation] only when [track] is null. */
  public fun bottomBarModeFor(track: Track?): BottomBarMode

  /** Immutable shared-resolved labels used by [NowPlayingContent]. */
  public data class NowPlayingScreenLabels(
      public val play: String,
      public val pause: String,
      public val albumArtwork: String,
      public val currentTrackArtistAlbum: String,
  )

  /** Immutable shared-resolved labels used by [NowPlayingBar]. */
  public data class NowPlayingBarLabels(
      public val play: String,
      public val pause: String,
      public val search: String,
      public val settings: String,
      public val albumArt: String,
      public val currentTrackArtistAlbum: String,
  )

  /** Selects the preserved compact or split Now Playing layout. */
  public enum class NowPlayingAdaptiveLayoutMode { Compact, Split }

  /** Returns the preserved Now Playing layout selection for the supplied bounds in dp. */
  public fun nowPlayingAdaptiveLayoutModeFor(
      widthDp: Float,
      heightDp: Float,
  ): NowPlayingAdaptiveLayoutMode
  ```

  `Track` in these signatures is transitively available through `:core:playback`'s public
  `api(:core:model)` surface; `:feature:nowplaying` declares no direct `:core:model` edge. The
  shared compatibility facade remains package-stable with its exact existing signature:

  ```kotlin
  @Composable
  public fun NowPlayingScreen(
      track: Track,
      playbackState: PlaybackState,
      playbackController: PlaybackController,
      tagLibReader: TagLibReader,
      currentLibraryTrack: LibraryTrack?,
      onBack: () -> Unit,
      modifier: Modifier = Modifier,
  ): Unit
  ```

  The facade retains its unused `TagLibReader` and `LibraryTrack?` parameters. It constructs
  exactly `NowPlayingScreenLabels(
  play = shared play,
  pause = shared pause,
  albumArtwork = shared album_artwork,
  currentTrackArtistAlbum =
      if (track != null) {
          shared track_artist_album_format(track.artist, track.album)
      } else {
          ""
      },
  )`, adapts `TrackArtwork?` to the loader returning bytes, and delegates only real inputs to
  `NowPlayingContent`. No feature signature imports TagLib, Library,
  or shared loader types, `Res` handles, or resource types.
- `LibraryAppShell` constructs exactly `NowPlayingBarLabels(
  play = shared play,
  pause = shared pause,
  search = shared search,
  settings = shared settings,
  albumArt = shared album_art,
  currentTrackArtistAlbum =
      if (track != null) {
          shared track_artist_album_format(track.artist, track.album)
      } else {
          ""
      },
  )` for its direct `NowPlayingBar` call. These are exactly the four screen-label fields and six
  bar-label fields; feature-owned resource strings never enter either object. Empty-library mode
  must not read `currentTrackArtistAlbum`; it resolves feature-owned `mini_player_empty_subtitle`
  internally. Shared resolves the injected values from retained resources, no `Res` handle crosses
  the seam, and focused tests prove both loaded mapping and empty-mode ignorance of the inert field.
- The feature owns lazy artwork state over exactly `suspend (String) -> ByteArray?` and renders it
  through public core-ui `ArtworkImage` and `ArtworkImageRole`. It does not import
  `TrackArtwork`, Library API/implementation, `LocalTrackArtworkLoader`, or
  `LazyTrackArtworkImage`. `NowPlayingBarRootTestTag`, `NowPlayingBarPlayPauseTestTag`,
  `NowPlayingBarSearchTestTag`, `NowPlayingBarSettingsTestTag`, and
  `NowPlayingBarContentPadding` remain `internal` because feature tests require them.
  File-local `NowPlayingUiState`, lazy-artwork state, feature-local upward-bar adapter, composable
  layout helpers, and implementation helpers are `private`; the shared-local downward overlay
  adapter remains `private` in its shared owner, `LibraryAppShell.kt` or its existing shared helper
  file. `MusicProgressScrubber`, `formatMillis`, `scrubberFractionForOffset`,
  `scrubberPositionForFraction`, `ScrubFractionState`, and `MusicScrubInteractionState` are all
  explicitly `internal` after the package-stable move; file helpers remain `private`, there are no
  named scrubber test constants, and the four named bar tags plus `NowPlayingBarContentPadding`
  remain `internal`.
- Preserve the current eager-first/lazy-second behavior from shared
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` without moving or
  changing that shared file: non-null `Track.artworkBytes` wins and the injected loader is not
  called; null eager bytes invoke the loader with the current track ID; a null loader result or
  ordinary non-cancellation failure produces the existing unavailable/fallback state;
  `CancellationException` is rethrown unchanged. Changes to track ID, eager bytes, or loader
  identity reset/reload state consistently with current behavior, and stale loader results cannot
  overwrite the current track. Production artwork state remains private. Exact JVM Compose rendering
  ownership belongs to `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingArtworkRenderingJvmTest.kt`,
  which proves bar and expanded eager/lazy/null/ordinary-failure rendering, exact cancellation
  instance through `currentCoroutineContext().job.invokeOnCompletion`, synchronous track/eager-byte/
  loader-identity resets, and stale-result rejection without widening state visibility. The shared
  facade/shell adapter remains the only owner of the current `TrackArtwork?` to bytes conversion.
- Core UI public declarations, all in `com.eterocell.rhythhaus.ui`, are exactly:

  ```kotlin
  /** Visual role used to keep artwork cache entries distinct by presentation size. */
  public enum class ArtworkImageRole(internal val keySuffix: String) {
      Thumbnail("thumbnail"),
      Card("card"),
      Hero("hero"),
  }

  /** Renders artwork bytes with the existing cache and fallback behavior. */
  @Composable
  public fun ArtworkImage(
      artworkBytes: ByteArray?,
      contentDescription: String,
      role: ArtworkImageRole,
      modifier: Modifier = Modifier,
      contentScale: ContentScale = ContentScale.Crop,
      fallback: @Composable () -> Unit,
  ): Unit

  /** Opaque handle for a supported Miuix backdrop; Miuix storage remains internal. */
  public class RhythHausBackdrop internal constructor(
      internal val layerBackdrop: LayerBackdrop,
  )

  /** Returns a backdrop handle, or null when render effects are unavailable. */
  @Composable
  public fun rememberRhythHausBackdrop(): RhythHausBackdrop?

  /** Records [backdrop] for later glass drawing and returns this modifier unchanged without one. */
  public fun Modifier.recordRhythHausBackdrop(
      backdrop: RhythHausBackdrop?,
  ): Modifier

  /** Draws glass from [backdrop] or the fallback surface while preserving existing visual values. */
  public fun Modifier.rhythHausLiquidGlass(
      backdrop: RhythHausBackdrop?,
      shape: Shape,
      fallbackColor: Color,
      blurRadius: Dp = 10.dp,
      refractionHeight: Dp = 16.dp,
      refractionAmount: Dp = 24.dp,
  ): Modifier

  /** Invokes [onBack] after the preserved generic left-edge swipe threshold. */
  public fun Modifier.leftEdgeSwipeBack(onBack: () -> Unit): Modifier

  /** Restricts the stateless drag mechanism to one locally owned terminal direction. */
  public enum class VerticalSheetGestureDirection { Upward, Downward }

  /** Mutates [expandProgress] with preserved drag physics and invokes [onTerminal] only at [direction]'s terminal state. */
  public fun Modifier.verticalSheetGesture(
      expandProgress: Animatable<Float, AnimationVector1D>,
      isActive: Boolean,
      scope: CoroutineScope,
      direction: VerticalSheetGestureDirection,
      onTerminal: () -> Unit,
      threshold: Float = 0.7f,
      referenceHeight: Float? = null,
  ): Modifier

  /** Alpha applied to the public glass fallback surface. */
  public const val RhythHausGlassSurfaceAlpha: Float = 0.72f
  ```

  `RhythHausBackdrop?` explicitly represents unavailable render effects. The internal constructor
  and `layerBackdrop` property may use Miuix storage while no public signature exposes
  `LayerBackdrop`. `rememberRhythHausBackdrop()` wraps `rememberLayerBackdrop()`; public modifier
  APIs unwrap `layerBackdrop` internally. Change `LibraryHomeContent`, `LibraryChrome`,
  `LibraryDetailContent`, `LibraryAppShell`, and `NowPlayingBar` caller signatures from
  `LayerBackdrop?` to `RhythHausBackdrop?`. Miuix `LayerBackdrop` is internal storage and absent
  from every public or cross-module signature. `RhythHausGlassBlurRadius`,
  `RhythHausGlassRefractionHeight`, and `RhythHausGlassRefractionAmount` are `internal`; the
  public defaults are their current `10.dp`, `16.dp`, and `24.dp` values, while the internal
  constants preserve implementation-only naming. No other visual constant is public.
  The feature-local adapter calls `verticalSheetGesture` with `Upward`, `threshold = 0.3f`, and
  `onTerminal = onExpand`; the shared-local overlay adapter calls it with `Downward`, its existing
  `0.7f` threshold, and `onTerminal = nowPlayingSwipeCollapseAction(onBack)`. `Upward` invokes
  its terminal callback after progress reaches or exceeds `threshold`; `Downward` invokes its
  terminal callback after progress falls below `threshold`. The mechanism springs to the opposite
  endpoint without invoking a callback, so no expansion callback reaches shared and no
  collapse/Back callback reaches the feature.

**Resource ownership:**

- Move these exact 17 EN/ZH keys from
  `shared/src/commonMain/composeResources/values/strings.xml` and
  `shared/src/commonMain/composeResources/values-zh/strings.xml` to
  `feature/nowplaying/src/commonMain/composeResources/values/strings.xml` and
  `feature/nowplaying/src/commonMain/composeResources/values-zh/strings.xml`:
  `mini_player_empty_subtitle`, `next_track`, `previous_track`,
  `playback_status_buffering`, `playback_status_error`, `playback_status_loading`,
  `playback_status_paused`, `playback_status_playing`, `playback_status_ready`,
  `playback_status_stopped`, `repeat_mode_repeat_one`, `repeat_mode_repeat_playlist`,
  `repeat_mode_stop_after_current`, `repeat_mode_stop_after_queue`, `shuffle_off`, `shuffle_on`,
  `track_number_format`.
- Retain shared `album_artwork` because Library UI uses it, and retain shared
  `track_artist_album_format`; shared formats the current track artist/album value and injects
  the resulting String. Retain every other shared key still used by Search, Settings, or Library;
  never remove shared copies required by those consumers and never duplicate shared-owned keys in
  feature resources.
- After registering the module and resource namespace, the planned discovery command was
  `./gradlew :feature:nowplaying:tasks --all --configuration-cache`, followed by discovered
  feature resource generation, Android packaging, JVM resource processing, and iOS resource/link
  tasks. The retained closeout evidence does not claim that literal discovery command ran; it
  records successful real feature task execution and compilation instead. Do not claim a task
  existed before module registration.

**Complete Task 5.1 path inventory:**

- Modify `settings.gradle.kts`, `gradle/libs.versions.toml`, `shared/build.gradle.kts`,
  `core/ui/build.gradle.kts`, `feature/nowplaying/build.gradle.kts`,
  `build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts`,
  `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`,
  `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt`,
  and `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`.
- Move or modify feature sources under
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/` and
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`,
  plus the exact resource files
  `feature/nowplaying/src/commonMain/composeResources/values/strings.xml` and
  `feature/nowplaying/src/commonMain/composeResources/values-zh/strings.xml`. Create
  `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/BottomBarModeTest.kt`,
  `NowPlayingAdaptiveLayoutTest.kt`, `NowPlayingContractsTest.kt`,
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingArtworkRenderingJvmTest.kt`,
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt`,
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBarSemanticsJvmTest.kt`,
  and `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`,
  `shared/src/commonMain/composeResources/values/strings.xml`, and
  `shared/src/commonMain/composeResources/values-zh/strings.xml`. Modify mixed tests
  `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt`,
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt`,
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`,
  and `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`.
  `Task3ReviewSemanticsJvmTest.kt` retains shell-policy assertions for unmeasured, stale-measured,
  and matching `LibraryBottomBarMeasurement` mappings to expected `isInteractive`; only rendering
  assertions move or are recreated in the feature. The path map contains `shared/build.gradle.kts`
  exactly once.
  Keep `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` unchanged and
  shared-owned; adapt `{ trackId -> LocalTrackArtworkLoader.current(trackId)?.bytes }` at the
  shared facade/shell call site. `TrackArtworkImage.kt` remains for other consumers.
- Remove the source origins `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`,
  `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt`, and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt`; their
  destinations are the feature/core-ui paths listed above. Do not move `NowPlayingArtworkBridge.kt`.
- Core-ui destination files are `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkImage.kt`,
  `SwipeBackGesture.kt`, `LiquidGlassChrome.kt`, and `VerticalSheetGesture.kt`, plus the exact
  regression test `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt`.

**TDD and selective test inventory:**

- [x] Before creating the module, run
  `./gradlew :feature:nowplaying:allTests --configuration-cache`; the planned diagnostic was
  `Project with path ':feature:nowplaying' could not be found in project ':'`. The actual Gradle
  9.6.1 task-selection wording differed, but the controller accepted it as semantically equivalent
  because task selection failed while `:feature:nowplaying` was absent; no requested feature task
  or compilation executed. Do not claim the planned exact string appeared.
- [x] Add the `compose-animation` catalog alias and extend
  `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt`
  with a feature-implementation KSP-arguments/registration case before convention implementation
  where the fixture can execute independently. Expected RED is the absent feature-implementation
  convention behavior, not an absent project path. Extend
  `ArchitectureCheckPluginFunctionalTest.kt` with the feature production-root positive policy and
  controlled forbidden mutations before production relocation; expected RED is the missing policy
  or registration. Stop on unrelated failures.
- [x] Register an empty `:feature:nowplaying` in `settings.gradle.kts`, create its build file,
  exact targets, conventions, dependencies, Android/resource namespaces, common/jvm/androidHost/
  ios test source sets, and positive architecture policy without feature production implementation.
  The module applies `build-logic.kmp.feature.impl` but has no feature `iosMain` source or
  framework export. Expected convention RED remains independently executable through functional
  fixtures before production KSP roots exist; stop on unrelated failures.
- [x] Move or recreate only these tests under feature ownership:
  `emptyLibraryStillUsesBottomBarNavigationMode`,
  `unmeasuredNowPlayingBarExposesNoActions`,
  `staleMeasuredNowPlayingBarExposesNoActionsAndDispatchesNoPointerOrGestureCallbacks`,
  `matchingMeasuredNowPlayingBarRestoresExpectedActions`, the five Now Playing adaptive tests
  currently in `LibraryNavigationTest`:
  `nowPlayingAdaptiveLayoutUsesCompactForPhonePortrait`,
  `nowPlayingAdaptiveLayoutUsesCompactForNarrowPortraitTablet`,
  `nowPlayingAdaptiveLayoutUsesSplitForWideTablet`,
  `nowPlayingAdaptiveLayoutUsesSplitForLandscapeMediumWidth`, and
  `nowPlayingAdaptiveLayoutUsesSplitForDesktopWidth`; and `MusicProgressScrubberTest`. Retain
  shared Library route/Back/shell tests and all non-Now-Playing mixed tests in shared. Expected
  RED is missing feature/core declarations or implementation, never a missing project path; stop
  for unrelated failures.
- [x] Split `Task3ReviewSemanticsJvmTest.kt` instead of moving its shared policy types literally:
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBarSemanticsJvmTest.kt`
  passes `interactive = false` and `interactive = true` directly and verifies only feature
  rendering, absence/presence of semantics/actions, and callback dispatch/non-dispatch. Shared
  `Task3ReviewSemanticsJvmTest.kt` retains or adds assertions that unmeasured, stale-measured, and
  matching `LibraryBottomBarMeasurement` values map to expected `isInteractive`, plus playlist,
  selection, and every other shared shell-policy case. No feature test imports
  `libraryBottomBarPresentation`, `LibraryBottomBarMeasurement`, or another shared policy
  declaration. Expected RED is missing feature rendering declarations for the feature test and
  missing core/shared policy fixtures for the shared test; do not proceed on unrelated failures.
- [x] Add focused characterization tests for the public feature contracts, exact label values,
  loaded and empty label mapping, inert empty label-field ignorance, artwork eager/lazy/error/
  cancellation/reset/stale-result behavior, progress/status/repeat/shuffle presentation, left-edge
  callback identity, upward gesture callback identity, and no interaction when unmeasured or
  stale. Keep private artwork state unobservable except through actual rendering:
  `NowPlayingArtworkRenderingJvmTest.kt` owns eager/lazy/null/ordinary-failure artwork and fallback
  rendering for both bar and expanded content, exact cancellation-instance observation through
  `currentCoroutineContext().job.invokeOnCompletion`, track/eager-byte/loader-identity synchronous
  resets, and stale-result rejection. `NowPlayingContentSemanticsJvmTest.kt` owns compact/split
  branches, stable tag/count identity, all transport/mode callbacks, left-edge Back, bounded
  progress, and metadata/status. `NowPlayingBarSemanticsJvmTest.kt` retains its existing ownership.
  Do not add presenter, ViewModel, or empty-pattern tests. Run the discovered focused
  feature JVM/Android-host/iOS test tasks; expected RED identifies the missing moved APIs or
  implementation, not an unrelated regression.
- [x] Before changing the core gesture production signature, characterize current math in the JVM
  Compose UI integration test
  `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt`.
  Use core UI's existing JVM Compose UI-test harness and dependencies, deterministic pointer input
  sequences against the actual public `Modifier.verticalSheetGesture` path, and deterministic
  animation-clock/idle advancement from that harness. Cover upward terminal callback, downward
  terminal callback, opposite-endpoint spring with no terminal callback, pointer cancellation,
  inactive behavior, exact threshold boundary for each direction, and reference-height behavior.
  Keep domain/navigation types and callback meanings out of core tests. Do not introduce a pure
  gesture policy/math seam or coroutine-test dependency; if the existing harness cannot execute
  this integration test, stop with that exact blocker. Expected RED is the missing moved/public
  core-ui gesture API before production extraction; GREEN is this JVM integration test passing
  after extraction with unchanged drag/rubber-band/spring/threshold math; the separately approved
  direction-specific cancellation contract is characterized by the same suite.
- [x] Add architecture RED fixtures and mutations before production relocation: absent module
  registration; feature-to-`:shared`, `:taglib`, Library API, Library implementation, app, and
  feature-implementation edges; feature iOS export; missing namespace/resource ownership; and
  missing package roots. The positive fixture must require actual production KSP package roots,
  not only a synthetic graph. Add the expected positive package/resource requirements or
  controlled forbidden mutations so fail-closed tests never pass by making production KSP roots
  empty. Supply the external processor JAR property where the convention fixture requires it and
  force fixture execution with `--rerun-tasks`; do not accept UP-TO-DATE as fixture evidence.
- [x] Implement the general feature-implementation KSP convention and architecture fixtures, then
  add the exact positive `:shared -> :feature:nowplaying` and
  `:feature:nowplaying -> :core:playback/:core:ui` entries to `ArchitectureAllowList` and the
  functional fixture/module inventory. Permit both feature package roots
  `com.eterocell.rhythhaus.nowplaying` and `com.eterocell.rhythhaus.ui` for the moved scrubber,
  exact resource namespace ownership, and exact Android namespace. Keep all negative mutations
  failing with the expected architecture diagnostics. Update
  `build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts` by reusing the
  core/API lifecycle: apply KSP when `:architecture-processor` exists, pass
  `architecture.module`, `architecture.packageRoots`, and `architecture.sourceRoots`, add the
  processor to every non-metadata main-target KSP configuration, and publish
  `ArchitectureModelRegistry` registrations. Do not call `explicitApi()` in this convention.
  Extend `KmpConventionPluginsFunctionalTest.kt` to prove production KSP arguments and
  registrations and preserve `EXPLICIT_API=null`. Extend
  `ArchitectureCheckPluginFunctionalTest.kt` with real feature-implementation production roots,
  positive module/resource policy, forbidden mutations, external processor JAR input, and forced
  execution. This is general governance for current and future feature implementation modules,
  not a Now Playing workaround.
- [x] Move the minimum core-ui, feature, and shared production slice and rerun the exact focused
  tests; expected GREEN covers
  the listed moved tests, public contract characterization, artwork seam, presentation, callback
  identity, and unmeasured/stale interaction behavior. Do not proceed when a GREEN result depends
  on weakening an assertion or adding a forbidden dependency.

**Gesture and composition implementation:**

- [x] Keep shared as owner of the `Animatable`, expansion progress state, shell measurement and
  visibility, overlay, collapse callback/Back, route dispatch, and navigation effects. The
  feature receives the shared-owned progress object only for display and mutation by its
  feature-local upward-only `NowPlayingBar` adapter, which emits only `onExpand`; it cannot invoke
  collapse, Back, or navigation. The shared-local downward-only overlay adapter emits only the
  existing shared collapse/Back callback and cannot route expansion.
- [x] Move the generic vertical drag/rubber-band/spring/threshold mechanism to core UI without
  changing exact math, thresholds, or spring stiffness. The approved cancellation contract is
  direction-specific: terminal-side cancellation may emit the direction-owned terminal callback
  and settle terminal; nonterminal-side cancellation settles opposite with no callback. The
  accepted 11-case suite characterizes this contract. The core entry point receives direction and
  terminal callback from the local adapters and has no domain or navigation API. Preserve generic
  caller-owned `leftEdgeSwipeBack` behavior.
- [x] Expose only the opaque `RhythHausBackdrop?` core-ui handle through
  `rememberRhythHausBackdrop`, `recordRhythHausBackdrop`, and `rhythHausLiquidGlass`; preserve
  existing visual behavior and keep Miuix storage and blur/refraction constants internal except
  explicit-public `RhythHausGlassSurfaceAlpha`.

**Verification and evidence boundary:**

- [x] After task discovery and module registration, run focused feature JVM, Android host, and
  iOS simulator tests and compilations; shared JVM tests; desktop compile; Android assemble; shared
  iOS compile; simulator/device framework links; core-ui consumer tests/compilations; discovered
  resource generation/packaging tasks; and the Xcode Swift-consumer build. Use
  `./gradlew :feature:nowplaying:jvmTest :feature:nowplaying:testAndroidHostTest
  :feature:nowplaying:iosSimulatorArm64Test :shared:jvmTest :desktopApp:compileKotlin
  :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64
  :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64
  --configuration-cache`. Retained evidence supports successful real feature task execution and
  compilation, including reconciliation to the actual `compileAndroidMain` task; it does not claim
  the literal `:feature:nowplaying:tasks :shared:tasks :core:ui:tasks --all` discovery command.
  Apply `--tests '*NowPlayingArtworkRenderingJvmTest'`,
  `--tests '*NowPlayingContentSemanticsJvmTest'`, and
  `--tests '*NowPlayingBarSemanticsJvmTest'` to the discovered feature JVM test task, and apply
  `--tests '*VerticalSheetGestureJvmTest'` to the discovered core-ui JVM test task. Do not claim
  this pointer integration test executes in common, Android-host, or iOS simulator test tasks;
  Android/iOS consumer compilation verifies that the public mechanism compiles cross-target. Run
  the discovered feature common/Android-host/iOS simulator tests separately, then run
  `/usr/bin/xcrun xcodebuild -version`, then `/usr/bin/xcrun xcodebuild -project
  iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination
  'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`; record an unavailable or
  failing Xcode command as its exact blocker rather than passing it.
- [x] Run the architecture functional test with a rebuilt processor JAR and real architecture
  check using retained commands: `./gradlew :architecture-processor:jar --rerun-tasks
  --no-configuration-cache`; then `./gradlew :build-logic:convention:cleanTest
  :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks
  --no-configuration-cache
  -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"`;
  then run
  `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail
  --no-parallel` twice and require strict configuration-cache reuse on the second run. The
  architecture processor JAR property is mandatory fixture input where required.
- [x] Run `./gradlew spotlessApply --configuration-cache`, then separate
  `./gradlew spotlessCheck --configuration-cache` and `./gradlew detekt --configuration-cache`.
  Run `openspec validate feature-first-modularization --strict` and
  `git diff --check -- gradle/libs.versions.toml settings.gradle.kts
  feature/nowplaying/build.gradle.kts feature/nowplaying/src
  core/ui/build.gradle.kts core/ui/src
  shared/build.gradle.kts
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt
  shared/src/commonMain/composeResources/values/strings.xml
  shared/src/commonMain/composeResources/values-zh/strings.xml
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt
  shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt
  shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt
  feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingArtworkRenderingJvmTest.kt
  feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt
  feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBarSemanticsJvmTest.kt
  core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt
  build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt
  build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
  docs/superpowers/plans/2026-07-27-feature-first-modularization.md`; record exact outputs in the
  evidence ledger. Compare the output against this inventory and block acceptance on any
  unexpected path; the final staging audit repeats the exact path set before the one atomic
  implementation commit.
- [x] Do not claim `./init.sh`, desktop runtime launch, iOS device/runtime UI, or runtime
  playback validation unless explicitly run. This boundary remains open: compile, link, packaging,
  and Swift-consumer results are non-runtime evidence. The controller completed independent scope
  and behavior approval, then committed the implementation atomically as `28dd2e1`; this separate
  conventional `docs:` closeout follows the established ignored-evidence durability pattern.

**Plan self-review before handoff:**

- [x] Confirm every approved design constraint and every listed source, test, package, resource,
  dependency, API, gesture, bridge, and evidence boundary is covered without adding scope. This
  includes inspected-but-unchanged approved paths: `LibraryDetailContent.kt` already used the
  core-ui backdrop API, and `ArtworkImage.kt` required no Task 5.1 edit. They were not part of the
  changed/staged 38-path implementation commit.
- [x] Scan the replacement section for placeholders, unresolved conditional dependency language,
  stale `NowPlayingArtworkBridge` ownership, or feature `iosMain` claims.
- [x] Check type/name consistency for label objects, artwork loader, backdrop nullability,
  direction type, gesture entry points, namespaces, and resource package.
- [x] Check test ownership against the exact selective inventory and confirm no empty presenter or
  pattern-class tests were prescribed.
- [x] Record verification command existence and exact results from successful real feature task
  execution and compilation rather than assuming generated task names. Retained evidence records
  the actual available `compileAndroidMain` task rather than the nonexistent `compileKotlinAndroid`
  spelling; it does not claim the literal `:feature:nowplaying:tasks --all --configuration-cache`
  command ran.

**Task 5.1 closeout evidence:** The actual Gradle 9.6.1 absent-module task-selection diagnostic
did not match the planned exact string; controller ruling accepted it as semantically equivalent
because task selection failed while the feature project was absent and no requested feature task or
compilation executed. Final focused XML is 18/18 Now Playing JVM tests: Content 3/3,
Artwork Rendering 12/12, and Bar Semantics 3/3, all zero skipped/failures/errors. The retained
core gesture XML is 11/11, all zero skipped/failures/errors. The final architecture fixture XML is
65/65, all zero skipped/failures/errors; root strict-cache `architectureCheck` passed twice with
reuse. The feature platform matrix passed: JVM, Android host, iOS simulator, Android/main and iOS
consumer compilation; the final feature/core platform command reported 141 actionable tasks
(13 executed, 128 up-to-date). Xcode 26.6 (17F113), `spotlessApply`, separate `spotlessCheck`,
separate `detekt`, named strict `openspec validate feature-first-modularization --strict`, and diff
hygiene passed. The all-change strict validation remains unclaimed because its recorded 44/45
result has the unrelated pre-existing `spec/ios-now-playing-info` failure. The final independent
review approved scope and behavior after the nondeterministic shuffle/Previous test finding was
closed by deterministic Off-shuffle transport assertions; no production behavior changed. Scope
reviewer `ses_032e868eeffei1GpxKbH327EB1` returned PASS against baseline `96cb487` and authorized
the exact 38-path implementation staging set after plan amendments. Behavioral reviewer
`ses_0328e9e86ffeRFAqPeTnpxV5pX` first returned REJECT, then returned `Findings: None. PASS /
APPROVED` after the test-only correction, verifying Content 3/3, Artwork 12/12, Bar 3/3, diff
hygiene, and empty staging. That reviewed worktree snapshot was subsequently staged as the
38-path implementation commit `28dd2e1`; neither review examined a post-commit SHA.

## Task 5.2: Move Playlists And Backup

**Route, authority, and scope.** Route: `writing-plans`; implementation route: SDD, one atomic Task 5.2 migration. The production baseline is `28dd2e1`; the branch/planning HEAD before the current documentation snapshot is `8843a88`; implementation begins only after the seven-document planning snapshot is independently approved and committed cleanly. Authority order is the approved Task 5.2 design in `docs/superpowers/specs/2026-07-27-feature-first-modularization-design.md`, including its exact Shared iOS ABI ledger, then approved OpenSpec change/spec/tasks, `docs/architecture.md`, and ADR 0001. Preserve the supplied approved canonical artifacts. Create unexported `:feature:playlists:impl`; it owns saved-playlist/playback-queue UI, immutable state/reducer/owner, repository implementation/binding, backup codec/service/state/UI, neutral document seam, Android/JVM launcher implementations, resources, and feature tests. Shared owns composition, shell/routes/Back, Settings layout, lifecycle, Koin assembly, generic labels, and the retained iOS ABI facade plus its injected launcher adapter. Core database remains sole physical SQLDelight owner; iosApp Swift remains app-owned. No product redesign, state rewrite, package rename, schema/migration/driver/generated DB move, playback ownership move, navigation/core-navigation or generic-document module, feature export, generated resource handle crossing, duplicate key, runtime/device claim, or feature README.

### Exact Atomic File Map

- [x] Register `:feature:playlists:impl` in `settings.gradle.kts`; add `feature/playlists/impl/build.gradle.kts`; update only `shared/build.gradle.kts`, `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`, and `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt` for module composition/policy/fixtures.
- [x] Move `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt` and `PlaylistsImplementationModule.kt` to `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/`; move whole `PlaylistState.kt`, `PlaylistPresentationPolicy.kt`, and `PlaylistScreens.kt` to `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/`; move whole `Crc32.kt`, `PlaylistBackupCodec.kt`, `PlaylistBackupDialogs.kt`, `PlaylistBackupMatcher.kt`, `PlaylistBackupModels.kt`, `PlaylistBackupService.kt`, `PlaylistBackupUiState.kt`, and `StrictJsonParser.kt` to `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/`.
- [x] Move and rename `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt` to `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/InMemoryPlaylistRepository.kt`. Both repository implementations are feature-owned and neither is exposed through Shared; `feature/playlists/api/.../PlaylistRepository.kt` remains the public contract.
- [x] Add `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDocumentLauncher.kt` for the neutral feature launcher and terminal result declarations. Add `feature/playlists/impl/src/androidMain/kotlin/com/eterocell/rhythhaus/playlistbackup/AndroidPlaylistBackupDocumentLauncher.android.kt` and `feature/playlists/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/playlistbackup/JvmPlaylistBackupDocumentLauncher.jvm.kt` for the public Android/JVM launcher factories and platform behavior. These renames preserve the public factory names while giving feature-specific generated JVM facades, avoiding duplicate Shared/feature facade classes. Do not add feature iOS source or an iOS actual.
- [x] Retain/adapt `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.kt` as the common `expect` and literal Shared MIME/size ABI facade; retain/adapt its Android, JVM, and iOS `actual` files as Shared adapters. Retain/adapt `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `di/RhythHausDi.kt`, `library/ui/LibraryAppShell.kt`, `LibraryAppState.kt`, `LibraryNavigation.kt`, `LibraryRoutes.kt`, and `settings/SettingsScreen.kt`. `RhythHausDi.kt` includes exactly one feature Koin module; `LibraryAppState.kt` adapts the Shared Back/dismissal registration seam. Shared passes immutable feature ports/plain labels and remains the Back authority.
- [x] Move playlist/queue/backup values from `shared/src/commonMain/composeResources/values/strings.xml` and `values-zh/strings.xml` to `feature/playlists/impl/src/commonMain/composeResources/values/strings.xml` and `values-zh/strings.xml`. Retain Shared `compose-multiplatform.xml`, `rhythhaus_logo.xml`, and `aboutlibraries.json`.
- [x] Move these 14 feature-owned tests unchanged where no adaptation is required. Retain/adapt the eight Shared tests in the Test Ledger through public PlaylistRepository and feature state ports; add the three named Shared tests.
- [x] Move `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt` to `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt`; preserve explicit duplicate ordering, validation-before-mutation, and rollback coverage.
- [x] Retain/adapt `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt`, `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt`, `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`, and `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiFactoryJvmTest.kt`. They use public `PlaylistRepository` and feature state ports only: source management uses a test-local recording repository and `PlaylistStateOwner.refresh()` instead of `InMemoryPlaylistRepository`/`loadPlaylistSnapshot`; lifecycle preserves queue/playlist reconciliation; DI preserves singleton and factory behavior.
- [x] Add `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaylistBackupRevisionGuardAdapterTest.kt`. Its `currentRevisionDelegatesControllerConfirmationBlockThroughOwner`, `staleRevisionDoesNotInvokeControllerConfirmationBlock`, and `cancellationFromControllerConfirmationBlockIsRethrownExactly` methods exercise internal `authoritativePlaylistBackupRevisionGuard(owner: AuthoritativeLibraryPublicationOwner): PlaylistBackupRevisionGuard` through `AuthoritativeLibraryPublicationOwner.withCurrentRevision`.
- [x] Inspect but never move `core/database/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.kt`, all core database SQLDelight/schema/migration/generated-input paths and tests, `iosApp/iosApp/App/RhythHausAppBootstrapper.swift`, `iosApp/iosApp/Documents/RhythHausPlaylistBackupDocumentProvider.swift`, `iosApp/iosApp/Documents/PlaylistBackupDocumentPolicies.swift`, and `iosApp/iosAppTests/PlaylistBackupDocumentPoliciesTests.swift`.

### Module Model And Dependencies

- [x] Follow `feature/nowplaying/build.gradle.kts`: plugins `build-logic.kmp.feature.impl`, `build-logic.android.kmp.library`, `build-logic.compose-resources`, and `alias(libs.plugins.compose.compiler)`; Android-KMP `namespace = "com.eterocell.rhythhaus.playlists"`, catalog compile/min SDK, JVM 11, `withHostTest {}`, Android resources enabled, `jvm()`, `iosArm64()`, and `iosSimulatorArm64()`. Configure `ControlledComposeResourcesExtension` namespace `rhythhaus.feature.playlists.generated.resources`.
- [x] Feature `commonMain` direct dependencies are `api(projects.feature.playlists.api)`, `api(projects.feature.library.api)`, `api(projects.core.model)`, `api(projects.core.playback)`, `api(libs.compose.runtime)`, `api(libs.compose.ui)`, `api(libs.compose.foundation)`, `api(libs.compose.components.resources)`, `api(libs.kotlinx.coroutinesCore)`, and `api(libs.koin.core)`; the last is required because the public `playlistsImplementationModule(): Module` signature exposes `Module`. Its direct implementation dependencies are `implementation(projects.core.ui)`, `implementation(projects.core.platform)`, `implementation(projects.core.database)`, `implementation(libs.compose.material.icons.extended)`, `implementation(libs.compose.material3)`, and `implementation(libs.miuix.ui)`. Feature `androidMain` directly depends on `implementation(libs.androidx.activity.compose)` and `implementation(libs.androidx.documentfile)`; `commonTest` on `implementation(libs.kotlin.test)`; and `jvmTest` on `implementation("org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}")` and `implementation(compose.desktop.currentOs)`.
- [x] Shared `commonMain` adds only `implementation(projects.feature.playlists.impl)` for this feature. There is no feature-to-Shared edge, feature framework export, direct export, SQLDelight/generated DB/driver/`Res` type, Shared shell/navigation type, or Android/JVM/iOS platform type crossing the feature boundary. `Module` is the sole Koin type crossing it.
- [x] Allow-list only `:feature:playlists:impl -> :feature:playlists:api`, `:feature:library:api`, `:core:model`, `:core:playback`, `:core:ui`, `:core:platform`, and `:core:database`; package roots are `com.eterocell.rhythhaus.library`, `com.eterocell.rhythhaus.library.ui`, and `com.eterocell.rhythhaus.playlistbackup`.

 - [ ] File-map correction: retain/adapt `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt` in Shared because it directly exercises `LibraryAppState`, routes, navigation, and `registerBackSurface`; retain/adapt `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsIosTest.kt` in Shared as ABI-adapter coverage. The later literal manifest is authoritative for its one retained iOS test path and total.

### Literal Closed Public Boundary

- [x] Every declaration listed here is `public`, in the named package, and has declaration-specific KDoc. Immutable values document value equality; identity values document stable-per-appearance/new-on-representation semantics; owner/controller methods document serialized operation, cancellation rethrow, stale-result rejection, and transactionality; composables document that Shared controls navigation. Every moved declaration not listed is `internal` or `private`, except the explicitly listed `SavedPlaylistPlaybackRequest`, `QueueMutationFeedback`, and their transitive public core-playback types `QueueOccurrence` and `PlaybackState`: specifically keep `PlaylistBackupCodec`, `PlaylistBackupDecodeResult`, `PlaylistBackupEntry`, `PlaylistImportIssue`, `PlaylistImportPlan`, all codec document/payload/model types, all presentation helpers, and actual implementation helpers internal or private.
- [x] `com.eterocell.rhythhaus.library`: `public fun playlistsImplementationModule(): org.koin.core.module.Module`. Its KDoc says Shared includes this binding and the returned module binds only `PlaylistRepository` to `SqlDelightPlaylistRepository` and creates `PlaylistStateOwner`; it neither constructs a launcher nor a backup controller and exposes no database types.
- [x] `com.eterocell.rhythhaus.library.ui`: preserve source signatures verbatim for `PlaylistTab`, `PlaylistSnapshot`, `PlaylistPickerState`, `PlaylistBrowserState`, `PlaylistState`, `PlaylistStateAction` with `LoadStarted`, `SnapshotConfirmed(snapshot: PlaylistSnapshot, revision: Long = 0L)`, `ReadFailed(message: String, revision: Long = 0L)`, `MutationFailed(message: String, revision: Long = 0L)`, `SelectTab(tab: PlaylistTab)`, `ShowRecoverableMessage(message: String)`, `OpenPicker(picker: PlaylistPickerState)`, `ClosePicker`, `OpenBrowser(browser: PlaylistBrowserState)`, `CloseBrowser`, and `ClearMessages`, plus `reducePlaylistState(state: PlaylistState, action: PlaylistStateAction): PlaylistState`. `PlaylistSnapshot` fields are `playlists: List<PlaylistSummary> = emptyList()` and `entriesByPlaylistId: Map<String, List<PlaylistEntry>> = emptyMap()`, with `playlist(id: String): PlaylistSummary?` and `entries(id: String): List<PlaylistEntry>`; `PlaylistPickerState(trackIds: List<String>, selectedPlaylistId: String? = null, enteredName: String = "")`; `PlaylistBrowserState(playlistId: String, query: String = "", visibleTrackIds: List<String> = emptyList(), selectedTrackIds: Set<String> = emptySet())`; `PlaylistState(selectedTab: PlaylistTab = PlaylistTab.Saved, confirmedSnapshot: PlaylistSnapshot = PlaylistSnapshot(), isLoading: Boolean = false, readErrorMessage: String? = null, mutationErrorMessage: String? = null, recoverableMessage: String? = null, picker: PlaylistPickerState? = null, browser: PlaylistBrowserState? = null, hasConfirmedSnapshot: Boolean = false, publicationRevision: Long = 0L)`.
- [x] `PlaylistStateOwner` is public with `public constructor(repository: PlaylistRepository, dispatcher: CoroutineDispatcher)`, `public suspend fun refresh(failureMessage: String = PlaylistReadFailedMessage): PlaylistStateAction`, and `public suspend fun mutate(failureMessage: String = PlaylistMutationFailedMessage, mutation: PlaylistRepository.() -> Unit): PlaylistStateAction`. Make `importPlaylists` internal because its current failure result exposes `Throwable`; no owner method leaks that implementation result.
- [x] In `com.eterocell.rhythhaus.playlistbackup`, expose this closed lossless dialog graph: `public enum class PlaylistBackupOperation { Idle, Exporting, Saving, Opening, Planning, Importing }`; `public enum class PlaylistBackupUiError { Unavailable, ReadFailed, WriteFailed, Oversized, Malformed, InvalidData, Checksum, UnsupportedVersion, StalePreview, ExportMissingTrack, ExportMissingDuration, ExportInvalidDuration, ExportInvalidData, RepositoryFailed }`; `public enum class PlaylistBackupIssueKind { Unmatched, Ambiguous }`; `public data class PlaylistBackupEntryView(val title: String, val artist: String, val album: String, val durationSeconds: Int)`; `public data class PlaylistBackupCounts(val restorable: Int, val unmatched: Int, val ambiguous: Int)`; `public data class PlaylistBackupPlaylistReport(val sourcePlaylistIndex: Int, val sourceName: String, val counts: PlaylistBackupCounts)`; `public data class PlaylistBackupIssue(val playlistIndex: Int, val entryIndex: Int, val kind: PlaylistBackupIssueKind, val entry: PlaylistBackupEntryView)`; `public data class PlaylistBackupPreview(val libraryRevision: Long, val reports: List<PlaylistBackupPlaylistReport>, val issues: List<PlaylistBackupIssue>, val totals: PlaylistBackupCounts, val canConfirm: Boolean)`; `public data class PlaylistBackupImportResult(val playlistsToCreate: Int, val playlistsSkipped: Int, val entries: PlaylistBackupCounts)`; `public data class PlaylistBackupUiState(val operation: PlaylistBackupOperation = PlaylistBackupOperation.Idle, val preview: PlaylistBackupPreview? = null, val result: PlaylistBackupImportResult? = null, val error: PlaylistBackupUiError? = null) { public val isBusy: Boolean }`; `public sealed interface PlaylistBackupUiAction { public data class OperationStarted(val operation: PlaylistBackupOperation) : PlaylistBackupUiAction; public data object PanelCancelled : PlaylistBackupUiAction; public data object OperationCancelled : PlaylistBackupUiAction; public data class PreviewReady(val preview: PlaylistBackupPreview) : PlaylistBackupUiAction; public data object DismissPreview : PlaylistBackupUiAction; public data object DismissResult : PlaylistBackupUiAction; public data class ImportSucceeded(val result: PlaylistBackupImportResult) : PlaylistBackupUiAction; public data class Failed(val error: PlaylistBackupUiError) : PlaylistBackupUiAction; public data object ClearError : PlaylistBackupUiAction }`; `public data class PlaylistBackupSettingsLabels(val cancel: String, val close: String)`; and `public data class PlaylistBackupImportConfirmation(val state: PlaylistBackupUiState, val confirmedSnapshot: PlaylistSnapshot?, val playlistPublicationRevision: Long?)`. KDoc must state that the view graph maps internal plans losslessly: every report, issue, entry title, counts, result count, and accessibility input remains available without exposing codec types.
- [x] Add the generic, public, KDoc-complete atomic boundary exactly as `public interface PlaylistBackupRevisionGuard { public suspend fun <T> withCurrentRevision(expectedRevision: Long, block: suspend () -> T): PlaylistBackupRevisionGuardResult<T> }` and `public sealed interface PlaylistBackupRevisionGuardResult<out T> { public data class Current<T>(val value: T) : PlaylistBackupRevisionGuardResult<T>; public data object Stale : PlaylistBackupRevisionGuardResult<Nothing> }`. It runs `block` only while the expected authoritative revision is current, settles one `Current` or `Stale`, and rethrows cancellation; `Current` documents value equality and `Stale` documents revision rejection.
- [x] `public interface PlaylistBackupDocumentLauncher { public val isAvailable: Boolean; public fun save(suggestedFileName: String, bytes: ByteArray): Unit; public fun open(): Unit }`; `public sealed interface PlaylistBackupDocumentSaveResult { public data object Success : PlaylistBackupDocumentSaveResult; public data object Cancelled : PlaylistBackupDocumentSaveResult; public data class Unavailable(val message: String) : PlaylistBackupDocumentSaveResult; public data class Failure(val message: String) : PlaylistBackupDocumentSaveResult }`; `public sealed interface PlaylistBackupDocumentOpenResult { public data class Success(val bytes: ByteArray) : PlaylistBackupDocumentOpenResult; public data object Cancelled : PlaylistBackupDocumentOpenResult; public data class Unavailable(val message: String) : PlaylistBackupDocumentOpenResult; public data class TooLarge(val maxBytes: Int) : PlaylistBackupDocumentOpenResult; public data class Failure(val message: String) : PlaylistBackupDocumentOpenResult }`; and `public fun createPlaylistBackupController(owner: PlaylistStateOwner, dispatcher: CoroutineDispatcher, launcher: PlaylistBackupDocumentLauncher, revisionGuard: PlaylistBackupRevisionGuard): PlaylistBackupController`. Launcher methods deliberately have no callbacks; factory construction receives them. `PlaylistBackupController` is public with an internal constructor; factory KDoc requires serialized operation ownership, operation gate, duplicate-completion suppression, exactly one terminal callback settlement, 4 MiB bound, ordinary failure mapping, and cancellation rethrow.
- [x] `PlaylistBackupController` signatures are: `public suspend fun beginExport(state: PlaylistBackupUiState, snapshot: PlaylistSnapshot, authoritativeTracks: List<LibraryTrack>, exportedAtEpochMillis: Long): PlaylistBackupUiState`; `public fun beginOpen(state: PlaylistBackupUiState): PlaylistBackupUiState`; `public fun receiveSave(state: PlaylistBackupUiState, result: PlaylistBackupDocumentSaveResult): PlaylistBackupUiState`; `public suspend fun receiveOpen(state: PlaylistBackupUiState, result: PlaylistBackupDocumentOpenResult, destinationTracks: List<LibraryTrack>, existingPlaylistNames: List<String>, importedSuffix: String, libraryRevision: Long): PlaylistBackupUiState`; `public suspend fun confirm(state: PlaylistBackupUiState, lastConfirmedSnapshot: PlaylistSnapshot): PlaylistBackupImportConfirmation`; and `public fun reduce(state: PlaylistBackupUiState, action: PlaylistBackupUiAction): PlaylistBackupUiState`. `beginExport` completes encode/decode validation before `launcher.save`; `beginOpen` calls `launcher.open`; Shared routes each terminal construction callback exactly once into `receiveSave` or `receiveOpen`. `receiveOpen` uses every input, maps Cancelled to `PanelCancelled`, Unavailable/TooLarge/Failure to `Failed`, and Success through Planning to PreviewReady or Failed. `confirm` calls internal owner import only inside `revisionGuard.withCurrentRevision(preview.libraryRevision)`, with no preflight comparison or internal owner-result type leak; it maps `Stale` to `StalePreview`, success to `ImportSucceeded` plus snapshot/revision, ordinary failure to `RepositoryFailed`, and rethrows `CancellationException`. All public transitions settle through `reduce`.
- [x] `playlistsImplementationModule()` binds repository plus `PlaylistStateOwner` only. `App.kt` constructs the Shared platform launcher, a private `PlaylistBackupRevisionGuard` adapter over `AuthoritativeLibraryPublicationOwner`, and the feature controller; the adapter owns the authoritative transaction and cannot leak its internal type. Koin constructs neither launcher nor controller. The Shared common `expect` retains construction-time `onSaveResult` and `onOpenResult` callbacks returning the neutral feature launcher type. Shared Android/JVM `actual`s delegate to public, KDoc-complete feature `rememberAndroidPlaylistBackupDocumentLauncher(onSaveResult, onOpenResult)` and `rememberJvmPlaylistBackupDocumentLauncher(onSaveResult, onOpenResult)` composable factories. The Shared iOS `actual` exclusively adapts the retained literal ABI facade to neutral feature launcher results. Feature declares no `expect`, no `actual`, no iOS actual, no Shared type, and no Swift export. Common `App` calls only the Shared `expect`.
- [x] `public data class PlaylistBackupSettingsLabels(val cancel: String, val close: String)` is KDoc-complete, documents value equality, and both fields are rendered. Publish exact Settings surfaces: `@Composable public fun PlaylistBackupSettingsSection(state: PlaylistBackupUiState, launcherAvailable: Boolean, labels: PlaylistBackupSettingsLabels, onExport: () -> Unit, onOpen: () -> Unit, onAction: (PlaylistBackupUiAction) -> Unit, modifier: Modifier = Modifier): Unit`; `@Composable public fun PlaylistBackupSettingsHost(state: PlaylistBackupUiState, launcherAvailable: Boolean, destination: PlaylistFeatureDestination, appearanceSource: PlaylistFeatureAppearanceSource, dismissalPublisher: PlaylistFeatureDismissalPublisher, labels: PlaylistBackupSettingsLabels, onExport: () -> Unit, onOpen: () -> Unit, onAction: (PlaylistBackupUiAction) -> Unit, onDismissPreview: () -> Unit, onConfirmPreview: () -> Unit, onDismissResult: () -> Unit, modifier: Modifier = Modifier): Unit`; `@Composable public fun PlaylistBackupPreviewDialog(preview: PlaylistBackupPreview, isBusy: Boolean, destination: PlaylistFeatureDestination, appearance: PlaylistDismissalAppearance, dismissalPublisher: PlaylistFeatureDismissalPublisher, labels: PlaylistBackupSettingsLabels, onDismiss: () -> Unit, onConfirm: () -> Unit): Unit`; and `@Composable public fun PlaylistBackupResultDialog(result: PlaylistBackupImportResult, destination: PlaylistFeatureDestination, appearance: PlaylistDismissalAppearance, dismissalPublisher: PlaylistFeatureDismissalPublisher, labels: PlaylistBackupSettingsLabels, onDismiss: () -> Unit): Unit`. `SettingsScreen` supplies its Shared-owned destination, retained destination-lifetime appearance source, and real `backupDocumentAvailable`, passes them unchanged into the host, and the host passes `launcherAvailable` unchanged to `PlaylistBackupSettingsSection`; availability is supplied, never inferred from operation state. Feature resolves feature-owned labels, while `cancel` and `close` are its only injected Shared Settings-label strings.
- [x] Public feature UI ports are `public data class PlaylistFeatureDestination(val value: String)`, `public data class PlaylistDismissalAppearance(val value: String)`, `public sealed interface PlaylistFeatureDismissal { public val destination: PlaylistFeatureDestination; public val appearance: PlaylistDismissalAppearance; public data class Modal(override val destination: PlaylistFeatureDestination, override val appearance: PlaylistDismissalAppearance) : PlaylistFeatureDismissal; public data class Edit(override val destination: PlaylistFeatureDestination, override val appearance: PlaylistDismissalAppearance) : PlaylistFeatureDismissal }`, `public enum class PlaylistFeatureDismissalDispatch { Started, Rejected }`, and `public interface PlaylistFeatureDismissalPublisher { public fun publish(dismissal: PlaylistFeatureDismissal?, dispatch: (PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch): () -> Unit }`, each with KDoc/value or identity semantics.
- [x] `PlaylistFeatureAppearanceSource` and `rememberPlaylistFeatureAppearanceSource` are literal public ports with declaration-specific KDoc: `public class PlaylistFeatureAppearanceSource internal constructor(destination: PlaylistFeatureDestination)` and `@Composable public fun rememberPlaylistFeatureAppearanceSource(destination: PlaylistFeatureDestination): PlaylistFeatureAppearanceSource`. Their KDoc says Shared creates one source for one active destination; the source monotonically allocates checked `Long` tokens, throws before `Long.MAX_VALUE` overflow, remains stable through recomposition and temporary overlay absence, and is discarded only when that destination lifetime ends. Its `next` allocator remains internal. The composable KDoc requires Shared to retain this one source for the active destination.
- [x] Public route entries preserve current outcome callbacks exactly: `@Composable public fun PlaylistHubScreen(state: PlaylistState, playbackState: PlaybackState, destination: PlaylistFeatureDestination, appearanceSource: PlaylistFeatureAppearanceSource, dismissalPublisher: PlaylistFeatureDismissalPublisher, playlistsLabel: String, loadingLabel: String, loadFailedLabel: String, retryLabel: String, mutationFailedLabel: String, onBack: () -> Unit, onOpenPlaylist: (String) -> Unit, onSelectTab: (PlaylistTab) -> Unit, onCreate: (String, (PlaylistStateAction) -> Unit) -> Unit, onRetry: () -> Unit, onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback, onRemoveUpcoming: suspend (String) -> QueueMutationFeedback, onClearUpcoming: suspend () -> QueueMutationFeedback, bottomContentPadding: Dp = 0.dp): Unit`; `@Composable public fun PlaylistDetailScreen(playlist: PlaylistSummary, entries: List<PlaylistEntry>, libraryTracks: List<LibraryTrack>, state: PlaylistState, destination: PlaylistFeatureDestination, appearanceSource: PlaylistFeatureAppearanceSource, dismissalPublisher: PlaylistFeatureDismissalPublisher, mutationFailedLabel: String, onBack: () -> Unit, onRetry: () -> Unit, onRename: (String, (PlaylistStateAction) -> Unit) -> Unit, onDelete: ((PlaylistStateAction) -> Unit) -> Unit, onDeleteConfirmed: (PlaylistSnapshot) -> Unit, onOpenBrowser: () -> Unit, onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit, onRemoveEntry: (String) -> Unit, onReorder: (List<String>) -> Unit, bottomContentPadding: Dp = 0.dp, listState: LazyListState = rememberLazyListState(), onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> }, initialEditMode: Boolean = false): Unit`; `@Composable public fun AddToPlaylistPickerOverlay(playlists: List<PlaylistSummary>, state: PlaylistPickerState, destination: PlaylistFeatureDestination, appearanceSource: PlaylistFeatureAppearanceSource, dismissalPublisher: PlaylistFeatureDismissalPublisher, onStateChange: (PlaylistPickerState) -> Unit, onDismiss: () -> Unit, onAppend: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit, onInlineCreate: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit): Unit`; and `@Composable public fun PlaylistTrackBrowserOverlay(playlistName: String, libraryTracks: List<LibraryTrack>, state: PlaylistBrowserState, destination: PlaylistFeatureDestination, appearanceSource: PlaylistFeatureAppearanceSource, dismissalPublisher: PlaylistFeatureDismissalPublisher, onStateChange: (PlaylistBrowserState) -> Unit, onDismiss: () -> Unit, onConfirm: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit): Unit`. Shared injects `mutationFailedLabel` into both route entries, passes the route outcome callbacks, and owns every navigation transition. Every listed parameter is used; `LibraryAppShell`, not Hub, hosts picker/browser overlays because Home/Search/album/artist selection routes open them. Every Shared or test caller creates or retains one source per active destination and passes that same source to the hub/detail and picker/browser overlay entries. No public entry may call `rememberPlaylistFeatureAppearanceSource` as a default argument or allocate a destination-local source internally.
- [x] `SavedPlaylistPlaybackRequest` is exactly `public data class SavedPlaylistPlaybackRequest(val occurrences: List<QueueOccurrence>, val selectedOccurrenceId: String)` and `QueueMutationFeedback` is exactly `public data class QueueMutationFeedback(val refreshedState: PlaybackState, val showQueueChanged: Boolean)`; both are KDoc-complete public value-equality types and their transitive `QueueOccurrence` and `PlaybackState` core-playback types remain public.
- [x] Stable dismissal identities are picker, browser, queue, create, rename, delete, remove, edit, settings-preview, and settings-result. Route/overlay entries allocate from the supplied destination-lifetime source only on false-to-true presentation transitions, retain the token while visible, clear it on dismissal, and allocate a strictly new token on re-presentation even when the composable or overlay was removed and remounted. `PlaylistBackupSettingsHost` owns conditional settings-preview/settings-result appearance allocation from its supplied `PlaylistFeatureAppearanceSource`: it allocates and retains the exact preview/result appearance only on false-to-true visibility, clears it on close, allocates a new identity on re-presentation, passes that supplied appearance to the dialog, and the dialogs publish that supplied identity without allocating or fabricating one. The host publishes through Shared registration and waits for authoritative port removal after callback return. The production chain is literal: `LibraryAppShell` passes active Settings `destinationId`, the Shell-retained `PlaylistFeatureAppearanceSource`, and `LibraryAppState::registerBackSurface` into `LibraryRouteOverlays`; `LibraryRouteOverlays` accepts those internal parameters, derives `PlaylistFeatureDestination(destinationId.instanceToken)`, creates `featureDismissalPublisher(destinationId, registerBackSurface)`, and passes destination, source, real `backupDocumentAvailable`, and publisher to `SettingsScreen`; `SettingsScreen` passes them unchanged into `PlaylistBackupSettingsHost`. Hub owns and consumes the destination-lifetime appearance source, publishes null at rest, and publishes only create or queue modals. Detail owns and consumes the same destination-lifetime source, publishes rename/delete/remove modals, publishes edit only while edit mode is active, and otherwise publishes null. Hub and Detail are route entries/destinations, never appearance stems. The foremost visible modal publishes as `Modal`, otherwise active edit as `Edit`, otherwise null; queue/create/rename/delete/remove/picker/browser/settings-preview/settings-result modal precedence always outranks edit. `DisposableEffect(destination, appearance)` disposes its exact registration on update, replacement, route change, and composition disposal; stale null publication or disposal cannot remove a newer identity. `Started` invokes only that appearance callback, while `Rejected` leaves it visible.
- [x] Shared creates the destination from its route identity and remains the Back-policy authority: it registers the published target, rejects inactive/hidden/outgoing/stale targets, enforces at-most-once dispatch and repeated-input suppression, and invalidates a confirmed-deleted destination. It owns predictive latch and cancellation. For non-predictive Back, a started callback is not settlement: Shared waits callback-independently for authoritative inactive/port removal before settling; rejection releases suppression without fallthrough and a later intent is new. This applies equally to feature route entries and Settings host/dialog ports.
- [x] Shared iOS ABI facade remains literal in `com.eterocell.rhythhaus.playlistbackup` in framework `Shared`: `public object IOSPlaylistBackupDocumentStatus { public const val SUCCESS: Int = 0; public const val CANCELLED: Int = 1; public const val TOO_LARGE: Int = 2; public const val FAILURE: Int = 3; public const val UNAVAILABLE: Int = 4 }`; `public interface IOSPlaylistBackupDocumentCompletion { public fun complete(status: Int, bytes: ByteArray?, message: String?): Unit }`; `public interface IOSPlaylistBackupDocumentProvider { public fun saveDocument(fileName: String, bytes: ByteArray, completion: IOSPlaylistBackupDocumentCompletion): Unit; public fun openDocument(maxBytes: Int, completion: IOSPlaylistBackupDocumentCompletion): Unit }`; `public object IOSPlaylistBackupDocumentBridge { public var provider: IOSPlaylistBackupDocumentProvider? }`; `public const val PlaylistBackupMimeType: String = "application/vnd.rhythhaus.playlists+json"`; `public const val PlaylistBackupMaxBytes: Int = 4 * 1024 * 1024`. The public constants live in Shared common `PlatformPlaylistBackupDocuments.kt`, so Kotlin/Native exports the literal `PlatformPlaylistBackupDocumentsKt`; `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.ios.kt` retains only the actual adapter plus status/completion/provider/bridge declarations and internal terminal-result mapping. Swift access remains `IOSPlaylistBackupDocumentBridge.shared.provider`; constants remain on `PlatformPlaylistBackupDocumentsKt`; Kotlin `Int` remains Swift `Int32`, including `openDocument(maxBytes: Int32, completion: IOSPlaylistBackupDocumentCompletion)`. Swift call sites and names remain unchanged. Feature has no iOS export.

### EN/ZH Resource Ownership

- [x] EN/ZH table, current owner `shared`, final owner/resolver/injection: `playlists | shared | shared | shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt:PlaylistRoutePlaceholder and App feature labels | none | playlistsLabel`; `playlists_accessibility | shared | shared | shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt | none | none`; `playlist_saved_tab | shared | feature | PlaylistHubScreen | none | none`; `playlist_queue_tab | shared | feature | PlaylistHubScreen | none | none`; `playlist_create | shared | feature | PlaylistHubScreen | none | none`; `playlist_create_name | shared | feature | PlaylistHubScreen | none | none`; `playlist_rename | shared | feature | PlaylistDetailScreen | none | none`; `playlist_delete | shared | feature | PlaylistDetailScreen | none | none`; `playlist_exit_editing | shared | feature | PlaylistDetailScreen | none | none`; `playlist_empty_saved | shared | feature | PlaylistHubScreen | none | none`; `playlist_empty_detail | shared | feature | PlaylistDetailScreen | none | none`; `playlist_empty_queue | shared | feature | PlaylistHubScreen | none | none`; `playlist_loading | shared | shared | LibraryRoutes:PlaylistRoutePlaceholder | none | loadingLabel`; `playlist_load_failed | shared | shared | LibraryRoutes:PlaylistRoutePlaceholder | none | loadFailedLabel`; `playlist_retry | shared | shared | LibraryRoutes:PlaylistRoutePlaceholder | none | retryLabel`; `playlist_changed | shared | shared | LibraryRoutes:PlaylistRoutePlaceholder | none | none`; `playlist_mutation_failed | shared | shared | LibraryRoutes:PlaylistRoutePlaceholder | PlaylistHubScreen and PlaylistDetailScreen | mutationFailedLabel`; `playlist_add_to | shared | feature | AddToPlaylistPickerOverlay | none | none`; `playlist_add_tracks | shared | feature | PlaylistTrackBrowserOverlay | none | none`; `playlist_choose_existing | shared | feature | AddToPlaylistPickerOverlay | none | none`; `playlist_create_inline | shared | feature | AddToPlaylistPickerOverlay | none | none`; `playlist_track_browser_search | shared | feature | PlaylistTrackBrowserOverlay | none | none`; `playlist_confirm_add | shared | feature | AddToPlaylistPickerOverlay and PlaylistTrackBrowserOverlay | none | none`; `playlist_entry_state | shared | feature | PlaylistDetailScreen | none | none`; `playlist_selected_state | shared | feature | PlaylistTrackBrowserOverlay | none | none`; `queue_current | shared | feature | PlaylistHubScreen | none | none`; `queue_upcoming | shared | feature | PlaylistHubScreen | none | none`; `queue_current_state | shared | feature | PlaylistHubScreen | none | none`; `queue_upcoming_state | shared | feature | PlaylistHubScreen | none | none`; `queue_changed | shared | feature | PlaylistHubScreen | none | none`; `queue_clear_upcoming | shared | feature | PlaylistHubScreen | none | none`; `queue_clear_confirmation | shared | feature | PlaylistHubScreen | none | none`; `queue_clear_confirm | shared | feature | PlaylistHubScreen | none | none`.
- [x] EN/ZH format rows, current owner `shared`, final owner `feature`: `playlist_delete_confirmation_format | PlaylistDetailScreen | String playlistName | none`; `playlist_add_track_accessibility_format | PlaylistTrackBrowserOverlay | String title | none`; `playlist_remove_track_format | PlaylistDetailScreen | String title | none`; `playlist_move_up_format | PlaylistDetailScreen | String title | none`; `playlist_move_down_format | PlaylistDetailScreen | String title | none`; `playlist_drag_format | PlaylistDetailScreen | String title | none`; `playlist_row_accessibility_format | PlaylistDetailScreen | String title, Int position | none`; `queue_remove_format | PlaylistHubScreen | String title | none`; `queue_move_up_format | PlaylistHubScreen | String title | none`; `queue_move_down_format | PlaylistHubScreen | String title | none`; `queue_drag_format | PlaylistHubScreen | String title | none`.
- [x] EN/ZH backup no-format rows, current owner `shared`: final `feature`, resolver `PlaylistBackupSettingsSection` or `PlaylistBackupPreviewDialog`/`PlaylistBackupResultDialog`, no injected field: `playlist_backup_section`, `playlist_backup_export`, `playlist_backup_exporting`, `playlist_backup_import`, `playlist_backup_importing`, `playlist_backup_preview_title`, `playlist_backup_confirm_import`, `playlist_backup_unmatched`, `playlist_backup_ambiguous`, `playlist_backup_result_title`, `playlist_backup_unavailable_error`, `playlist_backup_read_error`, `playlist_backup_write_error`, `playlist_backup_oversized_error`, `playlist_backup_malformed_error`, `playlist_backup_import_invalid_data_error`, `playlist_backup_checksum_error`, `playlist_backup_version_error`, `playlist_backup_stale_error`, `playlist_backup_missing_track_error`, `playlist_backup_missing_duration_error`, `playlist_backup_invalid_duration_error`, `playlist_backup_invalid_data_error`, and `playlist_backup_repository_error`. `playlist_backup_imported_suffix | shared | shared | App.kt import preparation | none | receiveOpen.importedSuffix` remains Shared because App prepares it before feature UI renders it.
- [x] Move to feature, resolve in backup dialogs with exact format arguments: `playlist_backup_totals(Int, Int, Int)`, `playlist_backup_preview_playlist_counts(String, Int, Int, Int)`, `playlist_backup_issue_accessibility(String, String, String)`, `playlist_backup_result_created(Int)`, `playlist_backup_result_created_one(Int)`, `playlist_backup_result_skipped(Int)`, `playlist_backup_result_skipped_one(Int)`, `playlist_backup_result_restored(Int)`, `playlist_backup_result_restored_one(Int)`, `playlist_backup_result_unmatched(Int)`, and `playlist_backup_result_ambiguous(Int)`.
- [x] Remaining literal rows: `cancel | shared | shared | SettingsScreen | none | PlaylistBackupSettingsLabels.cancel`; `close | shared | shared | SettingsScreen | none | PlaylistBackupSettingsLabels.close`; `selection_add_to_playlist | shared | shared | TrackSelectionBar in LibraryAppShell | none | direct String`; `settings | shared | shared | SettingsScreen | none | none`. Use direct `String` injection: App passes `playlistsLabel`, `loadingLabel`, `loadFailedLabel`, and `retryLabel` to `PlaylistHubScreen`; `LibraryRoutes` passes `mutationFailedLabel` to `PlaylistHubScreen` and `PlaylistDetailScreen`; and App passes `importedSuffix` to `receiveOpen`; each signature parameter is rendered or used. Each key is removed from exactly one of Shared or feature EN and ZH XML, never both; Shared never imports feature `Res`, feature never imports Shared `Res`, and architecture/source tests assert no duplicate name and no foreign generated-resource import.

### TDD, Verification, And Closeout

- [x] RED 1 before registration: `./gradlew :feature:playlists:impl:jvmTest --configuration-cache --configuration-cache-problems=fail`; accept only absent project wording with no feature task execution. GREEN 1 registers settings/build/allow-list then runs `./gradlew :feature:playlists:impl:tasks --all --configuration-cache --configuration-cache-problems=fail`, requiring `jvmTest`, `testAndroidHostTest`, `iosSimulatorArm64Test`, Compose resources, and KSP tasks.
- [x] Architecture RED/GREEN changes one variable at a time. RED 2 method `playlistsFeatureConventionPublishesRootsAndKspRegistrations` runs with `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.playlistsFeatureConventionPublishesRootsAndKspRegistrations' --rerun-tasks --configuration-cache --configuration-cache-problems=fail`. Its only RED mutation changes one expected fixture fact to `com.eterocell.rhythhaus.library`; it fails exactly once because `ArchitectureAllowList.packageRoots(project.path)` returns these three sorted roots: `com.eterocell.rhythhaus.library`, `com.eterocell.rhythhaus.library.ui`, and `com.eterocell.rhythhaus.playlistbackup`. GREEN restores those three roots only. The outer test and nested `:architecture-fixture:kspKotlinJvm` are `SUCCESS`; the fixture separately asserts exactly four registrations in source-set order: `kspKotlinAndroid`, `kspKotlinJvm`, `kspKotlinIosArm64`, and `kspKotlinIosSimulatorArm64`. RED 3 `rejectsPlaylistsFeatureOutsidePackageRoot` adds only `outside.fixture.InvalidFeature`; nested `:architecture-fixture:kspKotlinJvm` is `FAILED` with exactly one `ARCH-PACKAGE: :feature:playlists:impl source outside approved roots: outside.fixture.InvalidFeature`; GREEN changes only its package to `com.eterocell.rhythhaus.playlistbackup`, yielding `SUCCESS`. RED 4 `rejectsPlaylistsFeatureUndocumentedPublicDeclaration` removes only KDoc from `public class MissingFeatureKDoc`; nested KSP is `FAILED` with exactly one `ARCH-KDOC: :feature:playlists:impl public declaration MissingFeatureKDoc lacks KDoc`; GREEN restores only KDoc, yielding `SUCCESS`. Every nested TestKit invocation includes `--rerun-tasks`. KSP enforcement is package/KDoc only; `ARCH-RESOURCE` separately checks registry, imports, and key ownership.
- [x] Full Test Ledger -- move these 14 feature-owned tests unchanged where no adaptation is required. `PlaylistRepositoryContractTest` retains duplicate ordering, validation-before-mutation, and rollback coverage. (1) Old `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`; new `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt`; class `PlaylistStateTest`; methods `staleRefreshPublicationCannotOverwriteNewerMutationPublication` and `staleMutationFailureCannotAddNoticeAfterNewerSuccess`; fixture is an older refresh/failure action after a higher-revision confirmed snapshot; assertion is that the newer snapshot and its notices remain unchanged. (2) Old `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`; new `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`; class `PlaylistScreensTest`; methods `createModalPresentationRetainsDraftAndShowsFailureAfterRevisionedOutcome`, `failedDeleteOutcomeRetainsConfirmationAndPlaylistSnapshot`, `rejectedQueueCommandRefreshesFromStateFlowAndShowsQueueChangedNotice`, and `clearUpcomingDispatchesOnlyAfterExplicitConfirmation`; fixtures are a create draft with `MutationFailed`, a displayed `playlist-1` delete with `MutationFailed`, a stale queue occurrence returned by a `MutableStateFlow`, and an unconfirmed queue-clear presentation; assertions retain the draft/delete confirmation and snapshot, refresh the queue and expose the changed notice, and dispatch clear only after `confirm()`. (3) Old `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupCodecTest.kt`; new `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupCodecTest.kt`; class `PlaylistBackupCodecTest`; methods `roundTripPreservesPlaylistOrderEntryOrderAndDuplicates` and `byteLimitIsCheckedBeforeParsing`; fixtures are `Second` then `First` playlists with a duplicate entry and a 4 MiB whitespace byte array plus one byte; assertions preserve order/duplicates and return `MALFORMED_JSON` at 4 MiB but `INPUT_TOO_LARGE` at 4 MiB plus one. (4) Old `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcherTest.kt`; new `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcherTest.kt`; class `PlaylistBackupMatcherTest`; method `matcherReturnsUniqueUnmatchedAndAmbiguousWithoutChoosingFirstCandidate`; fixture is destination tracks with one normalized unique match, no match, and two equal candidates; assertion classifies unique, unmatched, and ambiguous entries without selecting either ambiguous candidate. (5) Old `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt`; new `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt`; class `PlaylistBackupServiceTest`; methods `exportUsesConfirmedPlaylistAndEntryOrderWithOnlyAuthoritativePortableMetadata` and `planPreservesOrderAndDuplicateResolvedIdsWhileCountingAndRecordingIssues`; fixtures are a confirmed snapshot with ordered duplicate entries and destination tracks containing unique, unmatched, and ambiguous metadata matches; assertions export only authoritative portable fields in confirmed order and preserve resolved duplicate IDs while counting/reporting issues. (6) Old `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt`; new `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt`; class `PlaylistBackupUiStateTest`; methods `serializedConfirmationUsesOneOwnerCallThatReturnsTheRefreshedSnapshot`, `confirmationRethrowsCancellation`, and `cancellationCleanupReturnsIdleAndRetainsPreviewWithoutError`; fixtures are a preview with a recording owner, an owner throwing `CancellationException`, and an interrupted operation with a preview; assertions make one owner call and publish its refreshed snapshot, rethrow cancellation, and return idle while retaining the preview without an error. (7) Old `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsTest.kt`; new `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsTest.kt`; class `PlatformPlaylistBackupDocumentsTest`; method `operationGateRejectsOverlapUntilCompletion`; fixture is a pending save followed by open, then its terminal completion; assertion rejects the overlap and permits the next operation only after completion. (8) Old `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`; new `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt`; class `PlaylistSqlDelightRepositoryJvmTest`; method `importPlaylistsRollsBackEverySqlRowOnSecondPlaylistEntryFailureAndRetryCreatesAllOnce`; fixture is an existing playlist plus `track-a`, `track-b`, and `track-c`, with an import whose second playlist contains a failing entry; assertion rolls back every inserted SQL row and a retry creates each requested playlist and entry exactly once. (9) Old `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`; new `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`; class `PlaylistEditModeSemanticsJvmTest`; method `failedDeleteRetainsTheDisplayedDetailAndItsConfirmation`; fixture is a Compose `PlaylistDetailScreen` for `playlist-1` whose delete callback returns `MutationFailed`; assertion retains the detail route and delete confirmation. (10) Old `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogsSemanticsJvmTest.kt`; new `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogsSemanticsJvmTest.kt`; class `PlaylistBackupDialogsSemanticsJvmTest`; method `previewExposesIssueRowsAndDisablesConfirmationWithoutRestorableEntries`; fixture is a preview containing unmatched and ambiguous issue rows with zero restorable entries; assertion exposes both issue rows and disables confirm. (11) Old `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupIntegrationJvmTest.kt`; new `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupIntegrationJvmTest.kt`; class `PlaylistBackupIntegrationJvmTest`; method `orderedSelectionRoundTripsThroughAtomicBackupRestoreAndRepeatImport`; fixture is a temporary database with selected `export-beta`, `export-alpha`, and a duplicate selection plus normalized destination tracks; assertion restores ordered duplicates atomically and repeat import observes the specified duplicate-import outcome. (12) Old `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsJvmTest.kt`; new `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsJvmTest.kt`; class `PlatformPlaylistBackupDocumentsJvmTest`; method `openAcceptsExactLimitAndRejectsLimitPlusOne`; fixture is a file of `PlaylistBackupMaxBytes` bytes and a file one byte larger; assertion returns `Success` with exact bytes and `TooLarge` for the larger file. (13) Old `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsAndroidTest.kt`; new `feature/playlists/impl/src/androidHostTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsAndroidTest.kt`; class `PlatformPlaylistBackupDocumentsAndroidTest`; method `saveThenOverlappingOpenUsesSaveChannelAndReleasesPayloadOnCancellation`; fixture is an `AndroidPlaylistBackupDocumentCoordinator` with a pending `byteArrayOf(1, 2)` save followed by open and cancellation; assertion reports the overlap through the save channel and clears the payload/gate after cancellation.
- [x] Extend the moved `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt` with `revisionChangeWhileConfirmationIsWaitingDoesNotImport`, `revisionChangeWhileTransactionIsAttemptedDoesNotImport`, `revisionGuardRunsBlockOnlyWhenCurrent`, `staleRevisionDoesNotImport`, and `confirmationRethrowsCancellationExactly`. A recording guard changes the revision while confirmation waits and while its transaction/import is attempted; both assertions observe zero import calls. The Current fixture invokes the block once; the Stale fixture invokes neither the block nor import; the cancellation fixture rethrows the exact `CancellationException`.
- [x] Extend the moved `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsTest.kt` with `duplicateSaveTerminalCallbacksSettleOnceWithoutDuplicateAction` and `duplicateOpenTerminalCallbacksSettleOnceWithoutDuplicateAction`. Together with `operationGateRejectsOverlapUntilCompletion`, they prove duplicate save/open delivery settles one terminal callback/action and operation overlap is gated until settlement.
- [x] Retain/adapt `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt` as Shared test (8). Adapt only its four playlist picker/browser cases to the public `AddToPlaylistPickerOverlay` and `PlaylistTrackBrowserOverlay` APIs, with `PlaylistFeatureDestination`, a recording/no-op `PlaylistFeatureDismissalPublisher`, and `(playlistId, trackIds)` callback values. Preserve all non-playlist Shared tests and their assertions unchanged. Do not expose the old internal helper APIs.
- [x] Retain/adapt exactly eight Shared tests. (1) `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt`; class `PlaylistBackPolicyJvmTest`; methods `coreNavigationEventHandlerLatchesStartBeforeImmediateCompletionAndNeverRetargets`, `presentedDestinationPublishesEditAndAFeatureModalPrecedesIt`, `inactiveHiddenAndOutgoingPortsAreRejectedAndStaleDisposersAreSafe`, `featureCallbackReturnKeepsTheExactSessionPendingUntilItsPortDisappears`, `explicitFeatureRejectionImmediatelyReleasesSuppressionWithoutRouteFallThrough`, `predictiveCancellationReleasesTheLatchedFeatureTarget`, and `nonPredictiveSettlementWaitsForAuthoritativeInactivityWithoutCallbackReturn`; fixtures are immediate-completing ports, a destination with edit and modal surfaces, inactive/hidden/outgoing ports, a callback-returning feature port, explicit rejection, predictive cancellation, and authoritative port removal; assertions prove the latch cannot retarget, modal outranks edit, stale disposers are harmless, the exact pending session waits for port removal, rejection releases suppression without route fallthrough, predictive cancellation releases the latch, and non-predictive callback return does not settle. (2) `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsIosTest.kt`; class `PlatformPlaylistBackupDocumentsIosTest`; method `completionMapsSuccessCancellationFailureAndOversized`; fixture is an injected iOS provider completing each status with nullable bytes/message; assertion maps success, cancellation, failure, and oversized statuses to their specific feature launcher results while retaining the Shared ABI adapter. (7) `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt`; class `AppScanCancellationTest`; method `backupOrchestrationPublishesIdleRetainedPreviewBeforeRethrowingCancellation`; adapt its fixture from the removed internal `PlaylistImportPlan` constructor to the neutral feature preview/controller boundary while preserving the exact cancellation rethrow and retained-preview assertion. Tests (3)-(6) remain `LibrarySourceManagementTest`, `PlaylistLifecycleIntegrationJvmTest`, `RhythHausDiTest`, and `RhythHausDiFactoryJvmTest` as listed in the literal manifest.
- [x] Add four feature tests. (1) `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt`; class `PlaylistFeatureDismissalTest`; production-composable methods are `hubCreateAndQueueClearPresentationsPublishFromProduction`, `detailRenameDeleteRemoveAndEditPresentationsPublishFromProduction`, `pickerAndBrowserOverlaysPublishFromProduction`, and `detailEditCloseReopenAllocatesNewAppearanceWithSharedDestinationSource`. Fixtures mount the actual production composables/overlays, retain one externally created source per destination, and prove recomposition stability plus open/close/reopen new identities for create, queue, rename, delete, remove, picker, browser, and edit. `pickerAndBrowserOverlaysPublishFromProduction` includes the stale-disposer/replacement assertions. Settings evidence is in the Shared Settings test ledger. `PlaylistBackupSettingsSectionTest.injectedCancelAndCloseDispatchCurrentActions` proves callback forwarding only. (2) `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupControllerTest.kt`; class `PlaylistBackupControllerTest`; methods `exportAwaitsPreparationAndCancellationRethrows` and `terminalCallbacksSettleControllerOperationOnce`; fixture is a recording launcher and a controller whose preparation suspends or throws `CancellationException`; assertion launches save only after preparation, rethrows cancellation, and settles a terminal callback once. (3) `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupSettingsSectionTest.kt`; class `PlaylistBackupSettingsSectionTest`; method `injectedCancelAndCloseDispatchCurrentActions`; fixture is the settings section with injected `cancel` and `close` labels and a current `PlaylistBackupUiState`; assertion dispatches the current panel/result actions through `onAction`. (4) `feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistDesktopResourceResolutionTest.kt`; class `PlaylistDesktopResourceResolutionTest`; method `featureLocaleResolvesAllBackupDialogKeys`; fixture is the feature desktop locale resource resolver for EN and ZH backup dialogs; assertion resolves every listed backup-dialog key from feature resources. Feature tests own feature publication/controller behavior.
- [x] Controlled appearance-source RED/GREEN: before production edits, add or finalize the fixed production-composable `detailEditCloseReopenAllocatesNewAppearanceWithSharedDestinationSource` fixture with one externally retained source. If it is already GREEN against partial code, temporarily restore only the rejected old edit-appearance reuse behavior in production while leaving the final fixture unchanged, for example by making reopened edit reuse its prior appearance identity. Run `./gradlew :feature:playlists:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissalTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail`; RED must fail specifically because the second edit identity equals the first. Restore the valid allocation immediately, then remove all four public-entry defaults, adapt every caller to one retained source per destination, and rerun GREEN requiring a distinct second identity, recomposition stability, and all four production-composable methods passing. The temporary mutation is never staged or committed and is reverted before implementation verification; do not use helper-only RED evidence. This test-method addition uses the existing manifest path and does not change the exact 95-path count.
- [x] Add three Shared tests. (1) `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaylistBackupRevisionGuardAdapterTest.kt`; class `PlaylistBackupRevisionGuardAdapterTest`; methods `currentRevisionDelegatesControllerConfirmationBlockThroughOwner`, `staleRevisionDoesNotInvokeControllerConfirmationBlock`, and `cancellationFromControllerConfirmationBlockIsRethrownExactly`; fixtures use the internal `authoritativePlaylistBackupRevisionGuard(owner: AuthoritativeLibraryPublicationOwner): PlaylistBackupRevisionGuard`; assertions delegate only through `withCurrentRevision`, skip stale blocks, and rethrow the exact cancellation. (2) `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/playlistbackup/IOSPlaylistBackupAbiFacadeTest.kt`; class `IOSPlaylistBackupAbiFacadeTest`; method `statusesAndNullabilityMapToInjectedLauncher`; fixture is `IOSPlaylistBackupDocumentBridge.provider` returning every status with null/non-null byte and message values; assertion preserves ABI status/nullability mapping to the injected launcher. (3) `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt`; class `SettingsPlaylistBackupEmbeddingTest`; methods `settingsHostEmbedsSectionPreviewAndResultWithCurrentCallbacks`, `settingsPreviewCloseReopenAllocatesNewAppearance`, `settingsResultCloseReopenAllocatesNewAppearance`, `settingsBackWaitsForAuthoritativePortRemoval`, and `settingsStaleDisposerCannotRemoveReplacement`; fixture mounts actual `LibraryRouteOverlays` with `LibraryRoute.Settings`, not a directly mounted `SettingsScreen` or test-built publisher, using the active Settings `destinationId`, Shell-retained `PlaylistFeatureAppearanceSource`, real `LibraryAppState::registerBackSurface`, and real `backupDocumentAvailable`; `LibraryRouteOverlays` derives `PlaylistFeatureDestination(destinationId.instanceToken)`, creates `featureDismissalPublisher(destinationId, registerBackSurface)`, and the production chain passes destination, source, availability, and publisher unchanged through `SettingsScreen` into `PlaylistBackupSettingsHost`; assertions prove recomposition-stable identities, distinct reopen identities, callback-independent pending settlement, and stale-disposer safety. This reuses the existing manifest path and the total remains exactly 95.
- [x] Focused feature JVM: `./gradlew :feature:playlists:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissalTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupControllerTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupIntegrationJvmTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDialogsSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsSectionTest' --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistDesktopResourceResolutionTest' --configuration-cache --configuration-cache-problems=fail`; focused Shared JVM: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --tests 'com.eterocell.rhythhaus.PlaylistLifecycleIntegrationJvmTest' --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --tests 'com.eterocell.rhythhaus.di.RhythHausDiFactoryJvmTest' --tests 'com.eterocell.rhythhaus.PlaylistBackupRevisionGuardAdapterTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.Task3ReviewSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.settings.SettingsPlaylistBackupEmbeddingTest' --configuration-cache --configuration-cache-problems=fail`; focused iOS ABI: `./gradlew :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.playlistbackup.IOSPlaylistBackupAbiFacadeTest' --configuration-cache --configuration-cache-problems=fail`. Then run unfiltered `:feature:playlists:impl:jvmTest`, `:feature:playlists:impl:testAndroidHostTest`, and `:feature:playlists:impl:iosSimulatorArm64Test`.
- [x] Run `./gradlew :core:database:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :shared:jvmTest :shared:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail`; then `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` twice, requiring the second output to state configuration-cache reuse; then `./gradlew :architecture-processor:test --configuration-cache --configuration-cache-problems=fail`; then `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail`; then `./gradlew spotlessApply`; then separately `./gradlew spotlessCheck --configuration-cache --configuration-cache-problems=fail`; then separately `./gradlew detekt --configuration-cache --configuration-cache-problems=fail`; then strict `openspec validate feature-first-modularization --strict`; and mandatory `./init.sh`. Any unavailable/failing command is recorded exactly and blocks acceptance/closeout.
- [x] Run `/usr/bin/xcrun xcodebuild -version`; `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`; and `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' test`. These prove compile/link/unit/Swift-consumer evidence only, never desktop launch, simulator/device interaction, visual behavior, playback runtime, or picker runtime.
- [x] The inline implementation manifest has exactly **95** unique paths: **5** build/governance paths, **28** endpoints for 14 moved production sources, **3** new feature-source paths, **11** retained/adapted Shared production paths, **4** EN/ZH resource endpoints, **28** endpoints for 14 moved tests, **9** retained/adapted-or-deleted Shared test paths, **4** new feature-test paths, and **3** new Shared-test paths: `5 + 28 + 3 + 11 + 4 + 28 + 9 + 4 + 3 = 95`. The renamed feature Android/JVM source entries are source-map, verification, staging, and path-set inputs; they preserve public factory names while generating feature-specific JVM facades, with no duplicate Shared/feature facade class. The final implementation set is **ninety-four non-deleted paths plus one explicitly listed tracked deletion**.
  The retained/adapted manifest paths `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`, and `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsIosTest.kt` are required: include exactly one feature Koin module, adapt the Shared Back/dismissal registration seam, and retain the iOS ABI/launcher result-mapping test, respectively. The listed `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt` is an explicit tracked deletion: its obsolete backup-helper assertions are superseded by feature-owned production-route tests, and no Shared compatibility shim is permitted.

<!-- task-5-2-implementation-manifest:start -->
```text
[build-governance]
settings.gradle.kts
feature/playlists/impl/build.gradle.kts
shared/build.gradle.kts
build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt
build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
[moved-production-endpoints]
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistRepository.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/InMemoryPlaylistRepository.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightPlaylistRepository.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistsImplementationModule.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlaylistsImplementationModule.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistState.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistPresentationPolicy.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistPresentationPolicy.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/Crc32.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/Crc32.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupCodec.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupCodec.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogs.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogs.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcher.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcher.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupModels.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupModels.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupService.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupService.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiState.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiState.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/StrictJsonParser.kt
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/StrictJsonParser.kt
[new-feature-source]
feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDocumentLauncher.kt
feature/playlists/impl/src/androidMain/kotlin/com/eterocell/rhythhaus/playlistbackup/AndroidPlaylistBackupDocumentLauncher.android.kt
feature/playlists/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/playlistbackup/JvmPlaylistBackupDocumentLauncher.jvm.kt
[retained-adapted-shared-production]
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.kt
shared/src/androidMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.android.kt
shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.jvm.kt
shared/src/iosMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocuments.ios.kt
[resource-endpoints]
shared/src/commonMain/composeResources/values/strings.xml
feature/playlists/impl/src/commonMain/composeResources/values/strings.xml
shared/src/commonMain/composeResources/values-zh/strings.xml
feature/playlists/impl/src/commonMain/composeResources/values-zh/strings.xml
[moved-test-endpoints]
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/PlaylistRepositoryContractTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistStateTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupCodecTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupCodecTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcherTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupMatcherTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupServiceTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupUiStateTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsTest.kt
feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/PlaylistSqlDelightRepositoryJvmTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogsSemanticsJvmTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogsSemanticsJvmTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupIntegrationJvmTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupIntegrationJvmTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsJvmTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsJvmTest.kt
shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsAndroidTest.kt
feature/playlists/impl/src/androidHostTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsAndroidTest.kt
[retained-adapted-or-deleted-shared-tests]
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt
shared/src/iosTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsIosTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiFactoryJvmTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt
[new-feature-tests]
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupControllerTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupSettingsSectionTest.kt
feature/playlists/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistDesktopResourceResolutionTest.kt
[new-shared-tests]
shared/src/iosTest/kotlin/com/eterocell/rhythhaus/playlistbackup/IOSPlaylistBackupAbiFacadeTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaylistBackupRevisionGuardAdapterTest.kt
```
<!-- task-5-2-implementation-manifest:end -->

- [ ] **Planning gate before implementation.** Require an empty index, `git diff --check`, strict named OpenSpec validation, and an independent plan-review verdict over exactly these seven changed documentation paths: `docs/adr/0001-feature-first-module-boundaries.md`, `docs/architecture.md`, `docs/superpowers/plans/2026-07-27-feature-first-modularization.md`, `docs/superpowers/specs/2026-07-27-feature-first-modularization-design.md`, `openspec/changes/feature-first-modularization/design.md`, `openspec/changes/feature-first-modularization/specs/feature-first-modular-architecture/spec.md`, and `openspec/changes/feature-first-modularization/tasks.md`. Use this temp-file-free scope check before review and staging: `current_paths() { git status --porcelain=v1 --untracked-files=all | perl -ne '$p = substr($_, 3); $p =~ s/^.* -> //; print $p'; }; expected_planning_paths() { printf '%s\n' docs/adr/0001-feature-first-module-boundaries.md docs/architecture.md docs/superpowers/plans/2026-07-27-feature-first-modularization.md docs/superpowers/specs/2026-07-27-feature-first-modularization-design.md openspec/changes/feature-first-modularization/design.md openspec/changes/feature-first-modularization/specs/feature-first-modular-architecture/spec.md openspec/changes/feature-first-modularization/tasks.md; }; test -z "$(git diff --cached --name-only)" && diff -u <(expected_planning_paths | LC_ALL=C sort) <(current_paths | LC_ALL=C sort) && git diff --check && openspec validate feature-first-modularization --strict`. After approval, stage only those seven paths and create a standalone semantic documentation/planning commit. Require a clean index and worktree. Create `.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-brief.md` before production edits and record the full planning commit SHA on a literal `Planning baseline: <40-hex-sha>` line.
- [x] **Implementation manifest gate before staging.** Run the following temp-file-free check only after the clean planning commit is `HEAD` and the implementation worktree diff exists. It extracts only the marker-delimited fenced block, derives category counts from marker-delimited entries, requires the exact ordered category/count ledger, strips markers, requires 95 total and unique paths, validates the brief's planning baseline as the current ancestral `HEAD`, requires an empty index, and compares the manifest against the implementation worktree paths emitted by `git status --porcelain=v1 --untracked-files=all`. The status parser is fail-closed: every record must match `^( M|M |MM|A |AM| D|D |R.|RM|\?\?) `, then it extracts the path after the two-column status using a real tab delimiter so porcelain path records are preserved safely; for renames it uses the destination after ` -> `. `D ` and ` D` records are explicitly accepted as implementation paths, and a listed deleted path need not exist on disk but must appear in porcelain status. Only the documented planning/evidence ledger paths are excluded; arbitrary unlisted files are not ignored. The three unchanged retained paths are accounted for explicitly through `retained_baseline_paths`: each must exist at the approved planning baseline and at current `HEAD`, and the final comparison is `manifest_paths = status_paths ∪ retained_baseline_paths`. The retained-adapted-or-deleted Shared-test category may represent a stale listed test as a tracked deletion when its behavior moves to feature-owned production-route tests; `git status --porcelain=v1 --untracked-files=all` must include that deletion:

  ```bash
  PLAN=docs/superpowers/plans/2026-07-27-feature-first-modularization.md
  BRIEF=.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-brief.md
  extract_manifest() { perl -0ne 'if (/<!-- task-5-2-implementation-manifest:start -->\n```text\n(.*?)\n```\n<!-- task-5-2-implementation-manifest:end -->/s) { print $1, "\n" } else { exit 1 }' "$PLAN"; }
  manifest_categories() { extract_manifest | awk '/^\[[^][]+\]$/ { category = substr($0, 2, length($0) - 2); order[++n] = category; next } NF { count[category]++ } END { for (i = 1; i <= n; i++) print order[i] "=" count[order[i]] }'; }
  manifest_paths() { extract_manifest | awk '!/^\[[^][]+\]$/ && NF'; }
  ledger_paths() { printf '%s\n' docs/superpowers/plans/2026-07-27-feature-first-modularization.md .superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-brief.md .superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-report.md .superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-final-acceptance-report.md .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md progress.md roadmap.md; }
  status_paths() { git status --porcelain=v1 --untracked-files=all | awk 'BEGIN { valid = "^( M|M |MM|A |AM| D|D |R.|RM|\\?\\?)$" } { code = substr($0, 1, 2); if (code !~ valid || substr($0, 3, 1) != " ") exit 1; path = substr($0, 4); if (code ~ /^R/ && path ~ / -> /) sub(/^.* -> /, "", path); print code "\t" path; }'; }
  retained_baseline_paths() { printf '%s\n' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt shared/src/iosTest/kotlin/com/eterocell/rhythhaus/playlistbackup/PlatformPlaylistBackupDocumentsIosTest.kt; }
  worktree_paths() { excluded="$(ledger_paths)"; status_paths | while IFS="$(printf '\t')" read -r code path; do case "$(printf '%s\n' "$excluded" | awk -v path="$path" '$0 == path { found = 1 } END { print found + 0 }')" in 1) continue ;; esac; printf '%s\n' "$path"; done; }
  baseline_paths() { retained_baseline_paths | while IFS= read -r path; do git cat-file -e "$(perl -ne 'print $1 if /^Planning baseline: ([0-9a-f]{40})$/' "$BRIEF"):$path" && git cat-file -e "HEAD:$path" || exit 1; printf '%s\n' "$path"; done; }
  implementation_paths() { { worktree_paths; baseline_paths; } | LC_ALL=C sort -u; }
  deleted_manifest_path='shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt'
  expected_categories() { printf '%s\n' build-governance=5 moved-production-endpoints=28 new-feature-source=3 retained-adapted-shared-production=11 resource-endpoints=4 moved-test-endpoints=28 retained-adapted-or-deleted-shared-tests=9 new-feature-tests=4 new-shared-tests=3; }
  PLANNING_BASELINE="$(perl -ne 'print $1 if /^Planning baseline: ([0-9a-f]{40})$/' "$BRIEF")"
  test "${#PLANNING_BASELINE}" -eq 40
  test "$(git rev-parse HEAD)" = "$PLANNING_BASELINE"
  git merge-base --is-ancestor "$PLANNING_BASELINE" HEAD
  test -z "$(git diff --cached --name-only)"
  test "$(retained_baseline_paths | wc -l | tr -d ' ')" -eq 3
  test "$(retained_baseline_paths | LC_ALL=C sort -u | wc -l | tr -d ' ')" -eq 3
  test "$(baseline_paths | LC_ALL=C sort -u | wc -l | tr -d ' ')" -eq 3
  diff -u <(expected_categories) <(manifest_categories)
  test "$(manifest_paths | wc -l | tr -d ' ')" -eq 95
  test "$(manifest_paths | LC_ALL=C sort -u | wc -l | tr -d ' ')" -eq 95
  test "$(status_paths | awk -v path="$deleted_manifest_path" '((substr($0, 1, 2) == "D ") || (substr($0, 1, 2) == " D")) && substr($0, 4) == path { found = 1 } END { print found + 0 }')" -eq 1
  diff -u <(manifest_paths | LC_ALL=C sort) <(implementation_paths)
  ```

- [x] **Implementation review and commit.** After all required verification, obtain an independent behavioral review and a separate exact path/ownership audit over the unchanged 95-path snapshot (ninety-four non-deleted paths plus the listed `SettingsScreenTest.kt` deletion). Stage only the 95 paths emitted by `manifest_paths`, repeat cached diff hygiene and path-set equality, and commit `refactor: extract playlists feature`. Never stage planning or evidence paths in the implementation commit.
- [x] **Evidence and ledger closeout.** After the implementation commit, update and independently review exactly these eight paths: `progress.md`, `roadmap.md`, `openspec/changes/feature-first-modularization/tasks.md`, `docs/superpowers/plans/2026-07-27-feature-first-modularization.md`, `.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md`, `.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-brief.md`, `.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-report.md`, and `.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.2-final-acceptance-report.md`. The retained reports record exact command output, implementation SHA and reviewed range, independent findings/verdicts, deferred runtime/device/visual claims, and the OpenSpec 6.2 boundary. Only after the independent ledger-integrity review passes, stage exactly those eight paths, repeat cached diff/path hygiene, and create a semantic documentation closeout commit. Require final `HEAD` subjects/SHAs, empty index, clean worktree, and strict named OpenSpec validation before declaring Task 5.2 closed. No plan or OpenSpec checkbox is checked before its retained evidence exists. Completed by `6e885ef75ada0d6e48b2832cb3852b460a6c62ed` (`docs: close playlists feature extraction`), directly following `fc1b96f858408c8dfd07221d5fe85ae3e20ced63`.

### Self-Review

- [x] Compare this full section to approved design/ABI; recursively audit every public signature and dependency closure; verify every moved/retained/new test path and every resource key.
- [x] Run `git diff --check -- docs/superpowers/plans/2026-07-27-feature-first-modularization.md`, `openspec validate feature-first-modularization --strict`, and Task-5.2-only scans for forbidden placeholders, unresolved type names, abbreviated paths, and wrong package spelling.
- [ ] Run `git status --short`; require exactly seven modified documentation files, no staged files, no production/test/build change, and this plan changed only between Task 5.2 and Task 5.3.
## Task 5.3: Move Search

**Route, baseline, and approval gate.** Route: OpenSpec + Superpowers; execution route: SDD.
The approved production/design baseline is `0066c409d0fcc039aa3657e3e5efca2d223ee78f`.
Production is frozen until this replacement plan is approved. Task 5.3 is one atomic
implementation/review/commit deliverable followed by a separate evidence closeout commit; there
are no interim commits. The only implementation commit subject is exactly
`refactor: extract search feature`. Authority order is
`docs/superpowers/specs/2026-08-07-search-feature-extraction-design.md`, canonical design and
OpenSpec at the baseline, then this plan, `docs/architecture.md`, and ADR 0001.

**Goal.** Extract the Search leaf into exactly one unexported Android-KMP Compose implementation
module, `:feature:search`, without changing Search behavior or Shared policy ownership.

**Architecture and tech stack.** The graph is exactly
`:shared -> :feature:search -> :feature:library:api` and `:feature:search -> :core:ui`.
Use Kotlin Multiplatform, Compose Multiplatform resources/compiler, existing KSP architecture
processor/TestKit fixtures, Kotlin test, Compose JVM UI test, Detekt, Spotless, Gradle, and the
existing Android/JVM/iOS target conventions. Shared remains the sole facade, iOS framework, shell,
route/Back arbiter, playback/selection/scroll owner, and Koin assembler.

**Global constraints.** Do not create a README, Search API split, Koin module/registration,
platform-specific production source file, iOS framework export, empty state/presenter/event/effect abstraction,
`:core:navigation`, `:core:playback` dependency, Shared reverse edge, or compatibility
`SearchScreen`. Do not change core playback, playback controller/state, repositories, platform
types, TagLib ownership, SQLDelight/schema/migration, Swift/app entry, `LibraryNavigation`,
`TrackSelectionState`, `LibraryPlaybackSelection`, `LibraryRows`, or `LibraryAppShell` except its
listed call-site adaptation. Do not add Shared/generated `Res` handles, playback controller/state,
repository, queue, Koin, or platform types to the feature public boundary. Runtime/device/visual/
accessibility/playback-engine claims remain open.

### Exact Public Boundary And Ownership

- [x] Expose exactly two public declarations in package `com.eterocell.rhythhaus.search`; every
  public declaration, constructor property, parameter, and public closure has the exact KDoc below
  or declaration-specific equivalent with no omitted behavior. All other feature declarations are
  `internal` or `private`. Preserve this signature exactly, including
  `selectTrackLabel: @Composable (String) -> String`; only layout defaults are permitted.

  ```kotlin
  /**
   * Shared-owned wording consumed by [SearchContent]. Value equality makes unchanged
   * labels stable across recomposition; callers provide already-localized text.
   *
   * @property title Search route title.
   * @property clear Label for the query-clear action.
   * @property nowPlaying Accessibility state for the current result.
   */
  public data class SearchSharedLabels(
      /** Search route title resolved by Shared. */
      public val title: String,
      /** Query-clear action label resolved by Shared. */
      public val clear: String,
      /** Current-result accessibility state resolved by Shared. */
      public val nowPlaying: String,
  )

  /**
   * Renders and locally controls Search over [libraryTracks], delegating application
   * policy through callbacks. It does not own navigation, playback, shared selection
   * state, scroll storage, or bottom-bar policy.
   *
   * @param libraryTracks Tracks searched in their supplied order.
   * @param currentTrackId Current playback track ID, or null when no track is current.
   * @param isPlaying Whether the current track is actively playing.
   * @param labels Shared-owned localized Search title, clear, and Now Playing labels.
   * @param selectTrackLabel Composably resolves the localized long-press/content description for a
   * title using Shared's structured `stringResource(select_track_format, title)` when Search
   * composes a row; no generated resource handle crosses the boundary.
   * @param selectionModeActive Whether Search rows currently select rather than play.
   * @param selectedTrackIds Immutable selected IDs effective for the Search page.
   * @param onStartSelection Requests Search selection beginning with the given track ID.
   * @param onToggleSelection Requests one toggle of the given Search track ID.
   * @param onVisibleTrackIdsChanged Receives filtered IDs whenever their sequence changes.
   * @param onScrollPositionChanged Receives first visible item index and pixel offset.
   * @param onPlayTrack Requests playback of ordered filtered results at the selected result.
   * @param onDismiss Requests Shared route dismissal.
   * @param playingIndicator Composes Shared-owned current-playing indication in a playing row.
   * @param bottomContentPadding Reserved trailing list space for Shared shell chrome.
   * @param modifier Modifier applied to the Search root.
   */
  @Composable
  public fun SearchContent(
      libraryTracks: List<LibraryTrack>,
      currentTrackId: String?,
      isPlaying: Boolean,
      labels: SearchSharedLabels,
      selectTrackLabel: @Composable (String) -> String,
      selectionModeActive: Boolean,
      selectedTrackIds: Set<String>,
      onStartSelection: (String) -> Unit,
      onToggleSelection: (String) -> Unit,
      onVisibleTrackIdsChanged: (List<String>) -> Unit,
      onScrollPositionChanged: (firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) -> Unit,
      onPlayTrack: (orderedResults: List<LibraryTrack>, selectedTrack: LibraryTrack) -> Unit,
      onDismiss: () -> Unit,
      playingIndicator: @Composable () -> Unit,
      bottomContentPadding: Dp = 0.dp,
      modifier: Modifier = Modifier,
  )
  ```

- [x] Feature owns query, case-insensitive title/artist/album filtering, input order, focus,
  result count/no-match presentation, list rendering, and row interaction. Blank/whitespace input
  yields no results; empty metadata and duplicate IDs are retained. Import
  `androidx.compose.foundation.lazy.itemsIndexed` and render exactly
  `itemsIndexed(filtered, key = { occurrenceIndex, track -> searchOccurrenceKey(occurrenceIndex, track.id) })`.
  Define `private fun searchOccurrenceKey(index: Int, trackId: String): String = "$index\u0000$trackId"`.
  The index prefix plus NUL separator makes distinct filtered occurrences unique, the resulting
  String is Bundle-saveable, and the key remains rendering-only: it preserves visible duplicate IDs,
  playback order, selection IDs, and independent duplicate render/activation after unrelated
  recomposition. Do not use a custom data-class key, `track.id` alone, or a two-argument lambda with
  `items`.
- [x] Define one `internal` pure filtering function in the feature source and make production
  `SearchContent` call it. It accepts `libraryTracks` and query text, returns empty for blank or
  whitespace-only input, otherwise filters title/artist/album ignoring case in supplied order, and
  preserves duplicate IDs and nullable/empty metadata. It is not a new public declaration.
- [x] Shared owns route/Back, effective selection and `ReconcileVisible(Search, ids)`, scroll
  storage/policy, current playback decision, queue/restart, error policy, dismissal policy, and
  `EqualizerStrip`. `LibraryRoutes.kt` composes `SearchContent` directly. Its internal Search
  adapter maps primitive scroll values to `LibraryScrollPosition`, maps visible IDs to the existing
  Search reconciliation action, resolves `SearchSharedLabels`, and supplies
  `selectTrackLabel = { title -> stringResource(Res.string.select_track_format, title) }` during
  row composition. It supplies `playingIndicator = { EqualizerStrip(active = true) }` only through
  the slot and uses `onDismiss` for the top app bar.
- [x] Adapt Shared selection exactly in the `SearchContent` call:
  ```kotlin
  selectionModeActive =
      trackSelectionState.pageKey == TrackSelectionPageKey.Search &&
          trackSelectionState.selectedTrackIds.isNotEmpty()
  selectedTrackIds =
      if (trackSelectionState.pageKey == TrackSelectionPageKey.Search) {
          trackSelectionState.selectedTrackIds
      } else {
          emptySet()
      }
  onStartSelection = { id ->
      onTrackSelectionAction(TrackSelectionAction.Start(TrackSelectionPageKey.Search, id))
  }
  onToggleSelection = { id ->
      onTrackSelectionAction(TrackSelectionAction.Toggle(TrackSelectionPageKey.Search, id))
  }
  onVisibleTrackIdsChanged = { ids ->
      onTrackSelectionAction(
          TrackSelectionAction.ReconcileVisible(TrackSelectionPageKey.Search, ids),
      )
  }
  ```
- [x] Define this internal production helper in `LibraryRoutes.kt`; the `SearchContent`
  `onPlayTrack` callback calls it directly. Do not add a test hook, injected selector, or public
  abstraction. Search invokes only `onPlayTrack`; it never dismisses itself after play.
  ```kotlin
  internal fun playSearchTrack(
      playbackController: PlaybackController,
      orderedResults: List<LibraryTrack>,
      selectedTrack: LibraryTrack,
      onDismiss: () -> Unit,
  ) {
      selectLibraryTrackForPlayback(
          playbackController = playbackController,
          visibleQueue = orderedResults.map(LibraryTrack::toPlayableTrack),
          selectedTrackId = selectedTrack.id,
      )
      onDismiss()
  }
  ```
  It maps ordered results, calls the real selector, and only then dismisses. A synchronous exception
  propagates unchanged and does not dismiss.

### Build, Resources, And Closed File Manifest

- [x] Register `include(":feature:search")`. Create `feature/search/build.gradle.kts` using exactly
  `build-logic.kmp.feature.impl`, `build-logic.android.kmp.library`,
  `build-logic.compose-resources`, and `alias(libs.plugins.compose.compiler)`. Configure
  `ControlledComposeResourcesExtension` namespace
  `rhythhaus.feature.search.generated.resources`; configure Android namespace
  `com.eterocell.rhythhaus.search`, catalog compile/min SDK, JVM 11, `withHostTest {}`,
  `androidResources { enable = true }`, `jvm()`, `iosArm64()`, and `iosSimulatorArm64()`. No
  feature framework/export or production platform source exists.
- [x] Use this exact feature skeleton, retaining the existing catalog SDK accessors and imports for
  `ControlledComposeResourcesExtension` and `JvmTarget`:
  ```kotlin
  plugins {
      id("build-logic.kmp.feature.impl")
      id("build-logic.android.kmp.library")
      id("build-logic.compose-resources")
      alias(libs.plugins.compose.compiler)
  }

  extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") {
      namespace("rhythhaus.feature.search.generated.resources")
  }

  kotlin {
      android {
          namespace = "com.eterocell.rhythhaus.search"
          compileSdk = libs.versions.android.compileSdk.get().toInt()
          minSdk = libs.versions.android.minSdk.get().toInt()
          compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
          withHostTest {}
          androidResources { enable = true }
      }
      jvm()
      iosArm64()
      iosSimulatorArm64()
  }
  ```
- [x] Configure source-set dependency scopes exactly as follows. Foundation, Compose resources,
  core UI, and Miuix remain `implementation`; no `api` dependency for any of those types is
  permitted.
  ```kotlin
  kotlin {
      sourceSets {
          commonMain.dependencies {
              api(projects.feature.library.api)
              api(libs.compose.runtime)
              api(libs.compose.ui)
              implementation(projects.core.ui)
              implementation(libs.compose.foundation)
              implementation(libs.compose.components.resources)
              implementation(libs.miuix.ui)
          }
          commonTest.dependencies {
              implementation(libs.kotlin.test)
          }
          jvmTest.dependencies {
              implementation("org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}")
              implementation(compose.desktop.currentOs)
          }
      }
  }
  ```
  Shared adds exactly
  `implementation(projects.feature.search)`, never `api` or export. Test task discovery must show
  `jvmTest`, `testAndroidHostTest`, `iosSimulatorArm64Test`, Compose resource, and KSP tasks. The
  feature convention registers exactly `kspAndroid`, `kspJvm`, `kspIosArm64`, and
  `kspIosSimulatorArm64` against `:architecture-processor`; fixtures assert these four registrations
  and their `kspAndroidMain`, `kspKotlinJvm`, `kspKotlinIosArm64`, and
  `kspKotlinIosSimulatorArm64` tasks.
- [x] Move exactly `search_placeholder`, `search_results_count_zero`,
  `search_results_count_one`, `search_results_count_many`, and
  `search_no_tracks_match_format` in both EN/ZH XML files to Search. Search resolves those through
  feature `Res`. Shared retains `search`, `clear`, `now_playing_badge`, and
  `select_track_format`; it injects title/clear/Now Playing text and structured formatting through
  the composable callback. Use existing repository XML/import-audit test pattern, with five
  distinct failures: missing moved key, duplicate key, wrong owner, wrong namespace, and foreign
  generated-resource import.
- [x] Add configuration-aware Search governance without changing any other module-edge policy.
  `ArchitectureAllowList.isAllowed` (or an equivalently narrow policy API) receives the actual
  configuration and preserves every existing module-edge rule. Only
  `:shared -> :feature:search` is allowed for declared production `commonMainImplementation`; it
  rejects `commonMainApi` and `api` with the deterministic diagnostic
  `ARCH-EDGE :shared [commonMainApi] -> :feature:search`. Do not globally reinterpret existing
  edges or configurations. `ArchitectureCheckTask` passes the edge configuration into that policy.
- [x] Add Search-specific expected namespaces through `ArchitectureAllowList` and validate their
  records in `ArchitectureCheckTask`: Android namespace
  `com.eterocell.rhythhaus.search` and Compose namespace
  `rhythhaus.feature.search.generated.resources`. Modules with no Search-specific expected
  namespace retain current behavior. Wrong Android and wrong Compose namespace each emit a
  deterministic executable `ARCH-RESOURCE` diagnostic; existing blank/invalid validation remains.
  This adds no project/module dependency or public app API.
- [x] The implementation manifest is exactly 20 unique endpoints, counting both sides of every
  move. Create/move destinations (8):
  `feature/search/build.gradle.kts`;
  `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`;
  `feature/search/src/commonMain/composeResources/values/strings.xml`;
  `feature/search/src/commonMain/composeResources/values-zh/strings.xml`;
  `feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt`;
  `feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt`;
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt`;
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt`.
  Modified (10): `settings.gradle.kts`; `shared/build.gradle.kts`;
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`;
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`;
  shared EN/ZH `strings.xml`; `ArchitectureAllowList.kt`; `ArchitectureCheckTask.kt` at
  `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt`;
  `ArchitectureCheckPluginFunctionalTest.kt`; and
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt`.
  Removed move sources (2): shared `SearchScreen.kt` and shared
  `SearchSelectionPoliciesJvmTest.kt` at their current paths.
- [x] Retain unchanged: `LibraryNavigation.kt`, `TrackSelectionState`,
  `LibraryPlaybackSelection.kt`, `LibraryRows.kt`, core playback, all SQLDelight/schema/migration
  paths, Swift/app entries, iOS exports, generated sources/build outputs, and all unlisted tests.
  The implementation excludes the canonical plan, OpenSpec, root `progress.md`, and `roadmap.md`;
  the ignored SDD brief/report/final-report files are outside the implementation snapshot. The
  controller-owned SDD progress ledger is the sole exception: it remains a tracked unstaged ` M`
  path until closeout. Generated/non-source outputs are excluded; the evidence closeout excludes all
  20 implementation endpoints.

### Dependency-Ordered TDD And Test Ledger

- [x] RED 1 before registration: run
  `./gradlew :feature:search:jvmTest --configuration-cache --configuration-cache-problems=fail`.
  Accept Gradle 9.6.1's actual absent-project/task-selection wording only when it proves
  `:feature:search` is absent and no requested feature task or compilation ran. Record the command,
  exit status, and exact causal diagnostic block verbatim in `task-5.3-report.md`; retain the full
  raw log separately if needed. If wording differs, reconcile the report against the causal
  missing-project/no-feature-task expectation rather than rewriting the command.
  Then register the module and run
  `./gradlew :feature:search:tasks --all --configuration-cache --configuration-cache-problems=fail`.
- [x] RED architecture controls before relocation, one fixture mutation per run, using the exact
  selector pattern
  `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeature...' --rerun-tasks --no-configuration-cache --no-parallel`.
  Add recommended selectors `searchFeatureConventionPublishesRootsAndKspRegistrations`,
  `searchFeatureRejectsForbiddenEdgesAndSharedExposure`,
  `searchFeatureRejectsWrongPackageNamespaceKoinAndIosExport`, and
  `searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports`,
  `searchFeatureRejectsSharedCommonMainApiExposure`, and
  `searchFeatureRejectsWrongExpectedNamespaces`. Because the partial fixture/allow-list exists,
  first add focused failing regressions against the current production checker for
  `commonMainApi` and each Search namespace mismatch, run the focused RED selectors, then implement
  the production checker changes and GREEN them. Build the external
  processor first with `./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel`, pass its JAR through the
  established property, and require nested TestKit KSP tasks to be `SUCCESS` or `FAILED`, never
  `UP-TO-DATE`, `NO-SOURCE`, or skipped. Every outer and nested TestKit invocation passes
  `--rerun-tasks`. Diagnostics/controls cover every forbidden feature edge
  (Shared, core playback/database/platform, taglib, another implementation, app), Shared `api` or
  export, wrong package, Android namespace, resource namespace, missing/duplicate/wrong-owner
  resources, foreign generated import, Koin, empty roots, and missing KDoc/public closure.
- [x] GREEN architecture registration updates the allow-list to permit only Library API and core UI
  for Search and Shared-to-Search composition, publishes Search package/resources/KSP registrations,
  and preserves all negative controls. The real KMP Shared fixture uses
  `commonMainImplementation(project(":feature:search"))` for GREEN, then mutates only that
  declaration to `commonMainApi(project(":feature:search"))` for RED; it must run through
  `architectureCheck`, never synthetic `architecture` configuration. Run each selector with
  `--rerun-tasks`; only then run the full architecture TestKit class with the external processor
  JAR.
- [x] In the positive Search fixture, publish the externally built processor JAR as the
  `:architecture-processor` project artifact; remove independent per-configuration file-JAR
  additions. Let only `build-logic.kmp.feature.impl` register it. Add
  `verifySearchFeatureConvention` using the existing registry/KSP-option pattern and assert exactly
  `KSP_PACKAGE_ROOTS=com.eterocell.rhythhaus.search` plus exactly these four sorted records:
  ```text
  KSP_REGISTRATION=:feature:search|kspAndroid|:architecture-processor
  KSP_REGISTRATION=:feature:search|kspIosArm64|:architecture-processor
  KSP_REGISTRATION=:feature:search|kspIosSimulatorArm64|:architecture-processor
  KSP_REGISTRATION=:feature:search|kspJvm|:architecture-processor
  ```
  It independently verifies all three facts: those four exact registry records; each real
  `kspAndroid`, `kspJvm`, `kspIosArm64`, and `kspIosSimulatorArm64` configuration has exactly one
  direct `ProjectDependency` with path `:architecture-processor` and no file-dependency substitute;
  and KSP argument `architecture.packageRoots` equals exactly
  `com.eterocell.rhythhaus.search`. After convention configuration, the negative fixture removes
  only the direct `ProjectDependency(:architecture-processor)` from `kspJvm` dependencies while
  retaining the registry record and all targets, then runs `verifySearchFeatureConvention`; it must
  fail deterministically with a registry/configuration mismatch. This is fixture configuration
  dependency removal, not a production convention edit. Task outcome alone is not processor proof:
  add one malformed common source and separately run `kspAndroidMain`, `kspKotlinJvm`,
  `kspKotlinIosArm64`, and `kspKotlinIosSimulatorArm64`; every task must be `FAILED` with the exact
  expected repository-built-processor `ARCH-PACKAGE` or `ARCH-KDOC` diagnostic. Restore valid
  source and require all four tasks `SUCCESS`. The external JAR remains only the
  `:architecture-processor` project artifact; never add `files(processorJar)` directly to KSP
  configurations.
- [x] Use actual Gradle/KSP diagnostics for one-variable Search mutations: wrong Android namespace,
  empty configured package roots, undocumented public member, and undocumented public constructor
  property. Top-level missing KDoc alone is insufficient. Retain controls for wrong package, iOS
  export, Compose namespace, and existing public closure.
- [x] Define one reusable production-source Koin audit over real non-test Search production roots.
  Its positive audit runs against the repository Search roots. A copied/mutated fixture adds one
  `org.koin` import and fails through this same audit function and diagnostic; do not use a
  self-thrown/caught assertion or require KSP to diagnose Koin.
- [x] Use the existing XML/import audit code path with lists/multisets, never sets. Search EN and ZH
  each contain exactly the five moved keys with no extra or duplicate declaration and have locale
  parity; Shared contains none of those keys and retains the four Shared-owned keys. Add one-variable
  controls for same-owner duplicate in Search EN, same-owner duplicate in Search ZH, extra Search
  key, missing moved key, cross-owner duplicate, wrong owner, wrong namespace, and both foreign
  generated imports. Preserve the five original failure categories while making same-owner/extra
  exactness executable.
- [x] Write behavior RED tests before moving production code. Feature
  `SearchFilterTest` in `commonTest` owns exactly `blankAndWhitespaceQueriesHaveNoResults`,
  `caseInsensitiveTitleArtistAndAlbumFilteringPreservesInputOrder`, and
  `duplicateIdsAndEmptyMetadataArePreserved`. These execute through the production internal filter
  on JVM, Android host, and iOS simulator; feature JVM/Android-host/iOS-simulator XML must each
  report a positive `SearchFilterTest` count, and none of those target test tasks is accepted with
  zero `SearchFilterTest` cases.
  `SearchSelectionPoliciesJvmTest` retains intentional production-composable duplication at the
  public rendering/callback boundary and owns exactly the four migrated methods
  `normalClickPlaysOnlyOutsideSelection`, `longClickStartsSelectionWithoutPlayback`,
  `selectionModeRowAndCheckboxEachToggleExactlyOnceWithoutPlayback`, and
  `changingFilteredIdsDispatchesEachSearchReconciliation`, plus named production-composable tests
  for `blankQueryHasNoResults`, `filtersTitleArtistAndAlbumIgnoringCase`,
  `resultCountsAndNoMatchTextUseFeatureResources`, `requestsFocusOnce`, `clearResetsQuery`,
  `reportsPrimitiveScrollAndBottomPadding`, `currentIndicatorAndNowPlayingSemanticsAreScoped`,
  `selectionAndVisibleSequenceUseProductionContent`, `emptyMetadataIsRetained`, and
  `duplicateOccurrencesRenderAndActivateDistinctlyAcrossUnrelatedRecomposition`.
  The duplicate-occurrence test remounts after unrelated recomposition and asserts distinct row
  activation/click payloads while visible and playback callbacks retain duplicate order. These mount
  `SearchContent`/production rows, not DTO-only helpers.
- [x] Move only the Home method
  `leavingHomeSongsForAlbumsOrArtistsClearsSelectionExactlyOnce` into Shared
  `HomeSelectionPoliciesJvmTest`; it owns Home clear behavior only. Add Shared
  `SearchRouteAdapterJvmTest` production composition/adapter methods
  `orderedQueueAndSelectedTrackUseRealPlaybackSelection`,
  `currentTrackRestartsBeforeDismissal`, `dismissesOnlyAfterSuccessfulSelection`,
  `sentinelFailurePropagatesAndDoesNotDismiss`,
  `sharedLabelsUseStructuredFormatting`, `equalizerSlotIsSharedOwned`, and
  `adaptsSelectionAndScrollFromProductionSearchContent`. Mount production `SearchContent` through
  the real adapter: a Search page with nonempty selected IDs supplies
  `selectionModeActive = true` and that exact selected-ID set; a non-Search page supplies false
  and `emptySet()`. A duplicate visible-ID list emits exactly one ordered duplicate-preserving
  `TrackSelectionAction.ReconcileVisible(TrackSelectionPageKey.Search, ids)`, while unrelated
  recomposition with the same sequence emits no second action. Assert `Start`, `Toggle`,
  `ReconcileVisible`, and primitive scroll mappings each exactly through production callback wiring.
  Successful queue/restart coverage uses the real
  `PlaybackController` and `selectLibraryTrackForPlayback`. For
  `sentinelFailurePropagatesAndDoesNotDismiss`, define exactly:
  ```kotlin
  val sentinel = IllegalStateException("search mapping sentinel")
  val failingOrderedResults = object : AbstractList<LibraryTrack>() {
      override val size: Int = 1
      override fun get(index: Int): LibraryTrack {
          check(index == 0)
          throw sentinel
      }
  }
  ```
  Use a normal separate `selectedTrack`, call real internal `playSearchTrack`, assert the thrown
  object is reference-identical, and assert dismiss count is zero. Mapping therefore fails
  synchronously without subclassing `PlaybackController` or injecting a test seam. Existing Shared
  route/Back/selection tests remain controls. No test manufactures a feature playback controller or
  repository.
- [x] GREEN relocates Search source/resources and the four Search tests, removes both old Shared
  move sources, implements the private occurrence key, and adapts the direct route call. Run
  feature focused tests, then Shared adapter/Home controls, then full feature/Shared target suites.

### Verification, Review, And Commit Boundaries

- [x] Run exactly these acceptance commands serially (`--no-parallel`) after the final code change:
  ```bash
  ./gradlew :feature:search:jvmTest --tests '*SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :shared:jvmTest --tests '*HomeSelectionPoliciesJvmTest' --tests '*SearchRouteAdapterJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :feature:search:compileAndroidMain :feature:search:compileKotlinIosArm64 :feature:search:compileKotlinIosSimulatorArm64 :feature:search:jvmTest :feature:search:testAndroidHostTest :feature:search:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
  ./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
  ./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew spotlessApply --configuration-cache --no-parallel
  ./gradlew spotlessCheck --configuration-cache --no-parallel
  ./gradlew detekt --configuration-cache --no-parallel
  PATH="$HOME/.nvm/versions/node/v26.7.0/bin:$PATH" openspec validate feature-first-modularization --strict
  /usr/bin/xcrun xcodebuild -version
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Debug CODE_SIGNING_ALLOWED=NO build
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' test
  ./init.sh
  git diff --check
  ```
  Run Xcode generic unsigned build/iPhone 17 tests when Xcode and that simulator are available;
  record unavailability as a blocker. Do not substitute `compileKotlinAndroid`, `allTests`, or a
  full OpenSpec sweep for named acceptance commands. Require second `architectureCheck` cache reuse.
  Record XML per target/class as tests/skipped/failures/errors, never actionable task counts. Record
  positive `SearchFilterTest` counts separately for feature JVM, Android host, and iOS simulator,
  and record JVM UI-class counts for `SearchSelectionPoliciesJvmTest` separately.
- [x] After the plan-only amendment commit and brief rebind, resume the existing partial checkpoint
  from that new `HEAD`; do not require or create a pristine implementation start. This one-time resume
  gate supersedes the clean-start gate only for the retained checkpoint. The rebound planning SHA
  must equal `HEAD`; the index must be empty; and the sole controller-owned tracked evidence change
  must be `.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md` with porcelain
  ` M`. The approved design SHA remains historical authority only, not a worktree diff base. The
  retained absent-module RED and historical 75-test architecture checkpoint remain valid evidence.
  This documentation-only amendment must preserve all frozen checkpoint blobs byte-identically; the
  independent reviewer verifies that preservation. No implementation commit may exist between the
  rebound planning commit and the one atomic implementation commit.
- [x] Run this one-time NUL-safe continuation gate immediately after rebind. It accepts only the
  exact frozen 16 implementation records below plus the one ` M` SDD ledger; it rejects another
  status, hash, path, or untracked file. `D` records must be absent on disk; their identity is
  established by the rebound `HEAD`. The committed continuation gate always rejects the canonical
  plan path.
  ```bash
  task_5_3_frozen_records() {
      printf '%s\0' \
          ' M|build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt|4b141d0e78670aafa0d1ef0e402b8afcaad69b43' \
          ' M|build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt|732d6045395d3cd3f5e0b2f11f5d42292c6fa24d' \
          ' M|settings.gradle.kts|d68aa9c12a653714771bbc3d9a7640c8782f6606' \
          ' M|shared/build.gradle.kts|fd23a96b8e9b9b910c1c9b49198b54e25348585d' \
          ' M|shared/src/commonMain/composeResources/values-zh/strings.xml|a29f6c56d5fdf528937b9920709b5c4b3571009c' \
          ' M|shared/src/commonMain/composeResources/values/strings.xml|8ce83312b7e5661b1db27552b9ee7095026a2ec5' \
          ' M|shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt|8c76cf9580c23c4b70b2f1da40b0bda20d963049' \
          ' D|shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt|DELETED' \
          ' D|shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt|DELETED' \
          '??|feature/search/build.gradle.kts|0df6657bf8a257b5b1613dad6c17a7997d3be7d9' \
          '??|feature/search/src/commonMain/composeResources/values-zh/strings.xml|516b871ca9def199c6c4f11f171560b577b35342' \
          '??|feature/search/src/commonMain/composeResources/values/strings.xml|e121e6714d33cb9e068ee8096e3ade1abeb20dc5' \
          '??|feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt|66450aea2f38ec34b9bb8b19fa96dc57296d2eb5' \
          '??|feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt|86fc5b1a52374949d438676e2b04440a12e521e5' \
          '??|feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt|bf26a8a638e564f3f252ce21178ebdaa23348478' \
          '??|shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt|6ecb8b6fd828f643dec39515baf12d88c5c9bbca'
  }

  task_5_3_verify_frozen_records() {
      perl -0777ne '
          my %expected = map { my ($status, $path, $hash) = split /\|/, $_, 3; $path => [$status, $hash] }
              grep length, split /\0/, $_;
          my $raw = qx{git -c status.renames=false -c core.quotepath=false status --porcelain=v1 -z --untracked-files=all};
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $seen_ledger = 0;
          while (length $raw) {
              $raw =~ s/\A( M| D|\?\?) // or die "unexpected checkpoint porcelain status\n";
              my $status = $1;
              $raw =~ s/\A([^\0]*)\0// or die "truncated checkpoint porcelain path\n";
              my $path = $1;
              if ($path eq $ledger) { die "bad ledger status\n" unless $status eq " M"; ++$seen_ledger; next; }
              my $want = delete $expected{$path} or die "unexpected checkpoint path $path\n";
              die "bad checkpoint status $path\n" unless $status eq $want->[0];
              if ($status eq " D") { die "deleted checkpoint path exists\n" if -e $path; next; }
              my $hash = qx{git hash-object -- "$path"}; $hash =~ s/\s+\z//;
              die "bad checkpoint hash $path\n" unless $hash eq $want->[1];
          }
          die "missing checkpoint record\n" if keys %expected;
          die "expected one ledger\n" unless $seen_ledger == 1;
      '
  }

  bound_planning_sha="$(awk -F'`' '/^\*\*Bound planning commit:\*\*/ { print $2; exit }' .superpowers/sdd/2026-07-27-feature-first-modularization/task-5.3-brief.md)"
  test "$(awk '/^\*\*Bound planning commit:\*\*/ { count++ } END { print count + 0 }' .superpowers/sdd/2026-07-27-feature-first-modularization/task-5.3-brief.md)" = 1
  printf '%s\n' "$bound_planning_sha" | grep -Eq '^[0-9a-f]{40}$'
  test "$bound_planning_sha" = "$(git rev-parse HEAD)"
  synthetic_bound_sha="$(printf '%s\n' '**Bound planning commit:** `0123456789abcdef0123456789abcdef01234567`.' 'bound_planning_sha="$(awk -F'\''`'\'' '\''/^\*\*Bound planning commit:\*\*/ { print $2; exit }'\'' task-5.3-brief.md)"' | awk -F'`' '/^\*\*Bound planning commit:\*\*/ { print $2; exit }')"
  test "$synthetic_bound_sha" = 0123456789abcdef0123456789abcdef01234567
  test -z "$(git diff --cached --name-only)"
  task_5_3_frozen_records | task_5_3_verify_frozen_records
  for path in \
      build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt \
      shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt \
      shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt \
      shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt; do
      case "$path" in
          *SearchRouteAdapterJvmTest.kt) test ! -e "$path" ;;
          *) test -e "$path" && test -z "$(git diff --name-only HEAD -- "$path")" ;;
      esac
  done
  ```
  `ArchitectureCheckTask.kt`, `LibraryAppShell.kt`, and
  `SettingsPlaylistBackupEmbeddingTest.kt` must exist clean at `HEAD`; the Search route-adapter
  test must remain absent/untracked. The final completed-snapshot 20-endpoint pre-stage and
  post-stage gates below remain unchanged. The exact NUL-safe frozen-record parser itself rejects
  every unlisted tracked or untracked path, including any untracked path outside or inside
  `feature/search`; no weaker secondary untracked-path check exists.
  During this uncommitted documentation review only, the controller separately compares the current
  worktree while manually excluding only the edited canonical plan. That temporary review comparison
  is not sourced, called, or enabled by the committed continuation gate and is deleted after the
  plan commit.
- [x] Define and use this fail-closed NUL-safe manifest gate. `--no-renames` represents a move as
  its tracked delete and add endpoints. It accepts only `A`, `M`, and `D` status records; every
  other status fails before path comparison.
  ```bash
  task_5_3_manifest() {
      printf '%s\0' \
          feature/search/build.gradle.kts \
          feature/search/src/commonMain/composeResources/values/strings.xml \
          feature/search/src/commonMain/composeResources/values-zh/strings.xml \
          feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt \
          feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt \
          feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt \
          settings.gradle.kts \
          shared/build.gradle.kts \
          shared/src/commonMain/composeResources/values/strings.xml \
          shared/src/commonMain/composeResources/values-zh/strings.xml \
          shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt \
          shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt \
          shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt \
          shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt \
          build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt \
          build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt \
          build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
  }

  task_5_3_parse_name_status_records() {
      perl -0777ne '
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A([AMD])\0// or die "unsupported or malformed name-status record\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing or truncated name-status path\n";
              my $path = $1;
              print "$status\0$path\0";
          }
      '
  }

  task_5_3_parse_name_status() {
      task_5_3_parse_name_status_records | perl -0777ne '
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status code\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status path\n";
              my $path = $1;
              print "$path\0";
          }
      '
  }

  task_5_3_changed_endpoints() {
      git -c core.quotepath=off diff "$@" --no-renames --name-status -z |
          task_5_3_parse_name_status | LC_ALL=C sort -zu
  }

  task_5_3_assert_only_unstaged_progress_ledger() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          my $ledger_count = 0;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status code\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status path\n";
              my $path = $1;
              die "unexpected unstaged diff path\n" unless $path eq $ledger && $status eq "M";
              $ledger_count += 1;
          }
          die "expected exactly one unstaged progress ledger\n" unless $ledger_count == 1;
      '
  }

  task_5_3_assert_no_progress_ledger_in_cached_diff() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status code\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing name-status path\n";
              my $path = $1;
              die "progress ledger staged\n" if $path eq $ledger;
          }
      '
  }

  task_5_3_parse_porcelain_records() {
      perl -0777ne '
          my $stream = $_;
          while (length $stream) {
              $stream =~ s/\A( M| D|\?\?) // or die "unsupported porcelain state\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing or truncated porcelain path\n";
              my $path = $1;
              print "$status\0$path\0";
          }
      '
  }

  task_5_3_without_progress_ledger() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          my $ledger_count = 0;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain status\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain path\n";
              my $path = $1;
              if ($path eq $ledger) {
                  die "unexpected ledger status\n" unless $status eq " M";
                  $ledger_count += 1;
                  next;
              }
              print "$path\0";
          }
          die "expected exactly one unstaged progress ledger\n" unless $ledger_count == 1;
      '
  }

  task_5_3_assert_only_progress_ledger() {
      perl -0777ne '
          my $ledger = ".superpowers/sdd/2026-07-27-feature-first-modularization/progress.md";
          my $stream = $_;
          my $ledger_count = 0;
          while (length $stream) {
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain status\n";
              my $status = $1;
              $stream =~ s/\A([^\0]*)\0// or die "missing porcelain path\n";
              my $path = $1;
              die "unexpected unstaged path\n" unless $path eq $ledger && $status eq " M";
              $ledger_count += 1;
          }
          die "expected exactly one unstaged progress ledger\n" unless $ledger_count == 1;
      '
  }

  task_5_3_prestage_endpoints() {
      git -c status.renames=false -c core.quotepath=false status --porcelain=v1 -z --untracked-files=all |
          task_5_3_parse_porcelain_records |
          task_5_3_without_progress_ledger | LC_ALL=C sort -zu
  }

  printf 'M\0path with space\0A\0new\0D\0old\0' |
      task_5_3_parse_name_status | LC_ALL=C sort -zu > /tmp/task-5.3-name-status-actual
  printf '%s\0' new old 'path with space' | LC_ALL=C sort -zu > /tmp/task-5.3-name-status-expected
  cmp -s /tmp/task-5.3-name-status-expected /tmp/task-5.3-name-status-actual
  ! printf 'R\0old\0new\0' | task_5_3_parse_name_status > /dev/null
  ! printf 'X\0path\0' | task_5_3_parse_name_status > /dev/null
  ! printf 'M\0truncated' | task_5_3_parse_name_status > /dev/null

  { task_5_3_manifest | perl -0777ne 'my $stream = $_; while (length $stream) { $stream =~ s/\A([^\0]*)\0// or die; print "?? $1\0"; }'; printf ' M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0'; } |
      task_5_3_parse_porcelain_records | task_5_3_without_progress_ledger | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-actual
  task_5_3_manifest | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-expected
  cmp -s /tmp/task-5.3-porcelain-expected /tmp/task-5.3-porcelain-actual
  printf ' M path with space\0 D old\0?? new\0 M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_porcelain_records | task_5_3_without_progress_ledger | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-mixed-actual
  printf '%s\0' new old 'path with space' | LC_ALL=C sort -zu > /tmp/task-5.3-porcelain-mixed-expected
  cmp -s /tmp/task-5.3-porcelain-mixed-expected /tmp/task-5.3-porcelain-mixed-actual
  printf ' M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger
  ! printf 'M  .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' | task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger > /dev/null
  ! printf ' D .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' | task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger > /dev/null
  ! printf 'R  .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0old\0' | task_5_3_parse_porcelain_records > /dev/null
  ! printf '?? truncated' | task_5_3_parse_porcelain_records > /dev/null
  ! printf ' M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0?? stray\0' | task_5_3_parse_porcelain_records | task_5_3_assert_only_progress_ledger > /dev/null

  printf 'A\0cached-added\0M\0cached-modified\0D\0cached-deleted\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_no_progress_ledger_in_cached_diff
  printf 'M\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger
  ! printf 'A\0cached-added\0M\0cached-modified\0D\0cached-deleted\0M\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_no_progress_ledger_in_cached_diff > /dev/null
  ! printf 'M\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0M\0second-unstaged\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger > /dev/null
  ! printf 'D\0.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger > /dev/null
  ! printf 'A  cached-added\0M  cached-modified\0D  cached-deleted\0 M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md\0' |
      task_5_3_parse_porcelain_records > /dev/null

  task_5_3_manifest | LC_ALL=C sort -zu > /tmp/task-5.3-manifest
  test "$(tr -cd '\0' < /tmp/task-5.3-manifest | wc -c | tr -d ' ')" = 20
  ```
- [x] Pre-stage gate: require an empty index, remove the exact single controller-owned ledger record,
  then compare every remaining tracked/untracked path to the literal 20 endpoints. The untracked-aware
  porcelain parser accepts only exact ` M`, ` D`, and `??` states; `status.renames=false` represents
  moves as tracked `D` plus untracked `??` destinations, and NUL parsing preserves paths with spaces.
  A different ledger status, a second ledger record, or any other nonmanifest path fails:
  ```bash
  test -z "$(git diff --cached --name-only)"
  task_5_3_prestage_endpoints > /tmp/task-5.3-prestage
  cmp -s /tmp/task-5.3-manifest /tmp/task-5.3-prestage
  ```
- [x] Post-stage gate: after explicit staging, do not require an empty index. Instead prove cached
  equality against the same planning `HEAD`, prove the ledger is absent from cached name-status
  records, inspect only unstaged tracked changes through the functional A/M/D name-status parser,
  require exactly one `M` record for the ledger, reject all untracked files, and check the cached
  diff. Do not pass full post-stage porcelain through the pre-stage parser; it contains staged
  implementation `A`/`M`/`D` records by design.
  ```bash
  task_5_3_changed_endpoints --cached HEAD > /tmp/task-5.3-cached
  cmp -s /tmp/task-5.3-manifest /tmp/task-5.3-cached
  git diff --cached --name-status -z --no-renames |
      task_5_3_parse_name_status_records |
      task_5_3_assert_no_progress_ledger_in_cached_diff
  git diff --name-status -z --no-renames |
      task_5_3_parse_name_status_records |
      task_5_3_assert_only_unstaged_progress_ledger
  test "$(git ls-files --others --exclude-standard -z | wc -c | tr -d ' ')" = 0
  git diff --cached --check
  ```
  Fail closed if a path is outside the literal list, a tracked delete is omitted, the pre-stage
  index is nonempty, an unsupported/mixed/rename/unknown porcelain state occurs, a stray untracked
  path exists, cached equality differs, the ledger is staged/different/duplicated, or another
  tracked/untracked implementation path remains unstaged. Canonical plan/OpenSpec/root
  `progress.md`/`roadmap.md` remain unchanged until closeout; ignored SDD brief/report/final-report
  files are excluded without widening the tracked ledger exception.
- [x] Independently review the unstaged 20-endpoint snapshot twice: a scope/ownership/path review
  and a behavior/API/architecture review. Each must confirm the literal public signature/KDoc,
  resource partition, no reverse/export/API exposure, Shared adapter causal semantics, duplicate
  behavior, retained exclusions, and all test evidence before the implementation commit.
- [x] Stage only explicit implementation endpoints, never broad `git add`: pass the pre-stage gate,
  use this exact command, then run the post-stage gate. It excludes the progress ledger from staging
  and from the staged review:
  ```bash
  git add -- feature/search/build.gradle.kts feature/search/src/commonMain/composeResources/values/strings.xml feature/search/src/commonMain/composeResources/values-zh/strings.xml feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt settings.gradle.kts shared/build.gradle.kts shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-zh/strings.xml shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SearchRouteAdapterJvmTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckTask.kt build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
  ```
  then commit exactly `git commit -m "refactor: extract search feature"`. The planning commit is
  the implementation comparison base; the plan and progress ledger are never in the implementation
  commit. After that commit, the ledger remains the expected sole tracked evidence diff until the
  separate closeout.
- [x] Evidence closeout begins only after that commit. Its exact eight paths are canonical plan;
  `openspec/changes/feature-first-modularization/tasks.md`; `progress.md`; `roadmap.md`;
  `.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md`;
  `task-5.3-brief.md`; `task-5.3-report.md`; and `task-5.3-final-acceptance-report.md` under that
  same SDD directory. `task-5.3-brief.md`, `task-5.3-report.md`, and
  `task-5.3-final-acceptance-report.md` are ignored and excluded until closeout; force-add them
  there. Keep OpenSpec 6.3 unchecked until evidence, leave 6.4/7/8 open, and update roadmap only
  after implementation acceptance.
- [x] Acceptance review includes the focused configuration/namespace regression selectors, the full
  `ArchitectureCheckPluginFunctionalTest` TestKit class, and independent checkpoint re-review after
  those checks. Preserve historical 75-test checkpoint evidence as history only; final counts
  supersede it. Do not claim acceptance until that re-review passes.

### Task 5.3 Completion Record

Implementation commit `90e330d24b10b9668263002b9cc37945d24e9643`
(`refactor: extract search feature`) directly follows bound planning commit
`f947724a9a2a29e5863976cb2c17fc16225bd336`. Final behavior/spec/quality review and the
independent exact 20-path boundary review are `PASS / APPROVED`. Final acceptance ran on the
uncommitted 20-endpoint snapshot; before/after hashes proved those bytes unchanged, and the
identical 20 endpoints were then committed as `90e330d24b10b9668263002b9cc37945d24e9643`.

Final retained XML is feature Search filter JVM/Android/iOS `3/0/0/0` each, Search JVM UI
`14/0/0/0`, Shared focused Home/SearchRoute/Settings `1/7/6` with zero failures, Shared JVM
`311/0/0/0`, Shared iOS `236/0/0/0`, and architecture functional `82/0/0/0`. Twice-reused
`architectureCheck`, Spotless, Detekt, strict named OpenSpec validation, Android assembly,
desktop compilation, generic Xcode Simulator build, iPhone 17 tests `8/0`, `./init.sh` within
20 minutes, diff hygiene, and exact 20-path gates passed. This separate evidence closeout commit
is pending; no closeout SHA is asserted. Physical-device runtime, desktop UI launch, rendered
visual/accessibility QA, live local-media scanning, and playback-engine runtime interaction remain
unverified. OpenSpec 6.3 is complete; 6.4, 7.*, 8.*, runtime evidence, and final-facade work
remain open. Next executable implementation task: 6.4, Settings extraction.

### Plan Self-Review

- [ ] Map each design/OpenSpec requirement to this section; scan for incomplete instructions,
  ambiguity, stale tokens, inconsistent types/signatures, wrong namespaces, and prohibited broad
  staging. Confirm the implementation parser has no pre-plan SHA, the post-stage gate does not
  reuse the empty-index assertion, no command mixes `--tests` with iOS tasks, no broad TestKit
  command is accepted, and the deprecated production-source wording is absent. Confirm the former
  manifest count is absent from Task 5.3; the common-test path occurs in the manifest, parser, staging command, and
  test ledger; no pre-stage `git diff HEAD` collector exists; porcelain includes
  `--untracked-files=all`; the new planning `HEAD` is rebound in the brief; the one-time continuation
  gate requires exactly the frozen 16 implementation records/hashes plus sole ` M` SDD ledger and
  four untouched endpoints; completed pre-stage requires exactly the 20 implementation endpoints
  plus that sole ledger; and post-stage requires cached 20 endpoints plus sole unstaged ` M` ledger
  with no untracked file. Post-stage uses `git diff --name-status -z --no-renames` and the A/M/D
  name-status parser, never the full porcelain stream. Confirm parser self-tests accept the 20
  manifest paths plus that exact ledger and reject staged, deleted, renamed, duplicate-ledger, and
  extra-path records. Confirm canonical plan/OpenSpec/root `progress.md`/`roadmap.md` remain
  unchanged until closeout and ignored SDD brief/report/final-report paths are not broadened into a
  generic tracked-evidence exclusion. For this governance-plan amendment only, do not run the
  implementation path comparator because the partial implementation already exists: prove literal
  manifest total/unique is 20, plan and brief mirror the governance amendment, and before/after
  hashes of every dirty implementation endpoint are byte-identical. Only the canonical plan and the
  ignored Task 5.3 brief may change in this lane; do not modify report, ledger, OpenSpec, design,
  root progress, or roadmap.
- [ ] Prove the manifest count is 20 with the parser, verify named test selectors exist or are
  explicitly created by this plan, validate command/task names against current Gradle conventions,
  run strict named OpenSpec validation and `git diff --check`, and inspect `git status --short`.
  Confirm this documentation amendment modifies only this plan section and ignored brief, then do
  not claim tests or builds ran.

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
