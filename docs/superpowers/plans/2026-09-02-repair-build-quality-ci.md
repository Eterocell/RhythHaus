# Build Quality and CI Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` or `executing-plans` to implement this plan one task at a time. Every task has an explicit RED/GREEN or command-level verification checkpoint.

**Goal:** Restore a trustworthy root quality gate, restore Compose 1.12 JVM UI rendering through Miuix 0.9.4-rc01, and add explicit static-quality plus build-only Android, desktop, and iOS app CI coverage without adding platform test execution to CI.

**Architecture:** `ArchitectureCheckPlugin` continues to suppress only exact `ProjectDependency` objects created by the Ben Manes Versions aggregate. The production failure is an unobserved current plugin ID, not a Gradle lifecycle race: add `io.github.ben-manes.versions` alongside retained legacy `com.github.ben-manes.versions` observation. The version catalog remains the sole Miuix/Compose version authority. GitHub Actions separates static-quality gates and supported-platform builds into independently attributable macOS jobs; Xcode builds the app scheme and its existing Kotlin-framework build phase.

**Tech Stack:** Gradle Kotlin DSL, Gradle TestKit/JUnit 5, version catalog, Compose Multiplatform 1.12.0, Miuix 0.9.4-rc01, GitHub Actions, Xcode Simulator build.

**Spec:** `docs/superpowers/specs/2026-09-02-repair-build-quality-ci-design.md`; `openspec/changes/repair-build-quality-ci/{proposal.md,design.md,specs/**}`.

## Global Constraints

- Preserve the identity-based architecture exemption. Do not suppress dependencies by configuration name, module path, or resolved graph.
- Support both exact plugin IDs: `io.github.ben-manes.versions` and `com.github.ben-manes.versions`.
- Keep KSP registration and AGP synthetic self-dependency suppression unchanged.
- Keep Compose Multiplatform at `1.12.0`; add no direct Skiko coordinate, force, substitution, or resolution rule.
- CI must retain the JVM test battery but must not invoke Android Host Test or iOS Simulator Test tasks.
- The iOS CI job must use `iosApp/iosApp.xcodeproj`, scheme `iosApp`, the Simulator SDK, and `CODE_SIGNING_ALLOWED=NO`. Its existing `Compile Kotlin Framework` Xcode phase calls `:shared:embedAndSignAppleFrameworkForXcode`.
- Do not alter app behavior, module dependencies, database schemas, public APIs, or user-owned `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt`.
- Final documentation/tracking updates are required: `progress.md`, `roadmap.md`, and OpenSpec task status. Commit only after all acceptance evidence is present, with a conventional message.

---

### Task 1: Cover and fix the current Versions plugin ID exclusion

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPlugin.kt`
- Modify: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`

**Interfaces:**
- Consumes: the existing identity set `versionsPluginAggregationDependencies`, aggregate configuration name `dependencyUpdatesAggregation`, and Gradle `Project.pluginManager.withPlugin` callbacks.
- Produces: task input `dependencyEdges` that omits only exact aggregate dependencies for either supported Versions plugin ID.

- [ ] **Step 1: Add the production-ID regression before changing the plugin.**

  Refactor only `versionsPluginFixture` to accept `pluginId: String`, preserving `com.github.ben-manes.versions` as its default. Add `currentVersionsPluginAggregationSelfDependencyIsToolingAndNotAnArchitectureEdge`, which passes `io.github.ben-manes.versions` and asserts the same absence of `ARCH-CYCLE : -> :` and `ARCH-EDGE : [dependencyUpdatesAggregation] -> :`. Add the paired current-ID authored self-dependency negative, asserting the same exact two diagnostics as the existing legacy negative. Keep both existing legacy tests unchanged in intent.

- [ ] **Step 2: Establish the RED boundary.**

  Run:

  ```bash
  ./gradlew :build-logic:convention:test \
    --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.currentVersionsPluginAggregationSelfDependencyIsToolingAndNotAnArchitectureEdge' \
    --configuration-cache --configuration-cache-problems=fail --no-parallel
  ```

  Expected before the implementation: failure containing the root aggregate `ARCH-CYCLE` and `ARCH-EDGE` diagnostics. The new authored-current-ID negative remains a later preservation guard, not the RED assertion.

- [ ] **Step 3: Observe the current ID without widening filtering.**

  In `ArchitectureCheckPlugin.apply`, factor the existing capture body into a small local callback or helper and register it for both exact plugin IDs. Do not change the `IdentityHashMap` set, aggregate configuration name, graph traversal, direct-self cardinality computation, KSP checks, or Android synthetic-self suppression. A duplicate callback must remain harmless because the set is identity-based.

- [ ] **Step 4: Prove both IDs and authored edges.**

  Run the four Versions aggregation functional tests—the legacy clean/negative pair and current clean/negative pair—then run the entire `ArchitectureCheckPluginFunctionalTest` class with the same configuration-cache flags. Expected: clean fixtures succeed; both explicit authored self-dependency fixtures fail with exactly their existing `ARCH-CYCLE` and `ARCH-EDGE` diagnostics.

### Task 2: Upgrade Miuix at the catalog boundary and restore JVM rendering

**Files:**
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: the catalog's existing `miuix` version reference and Compose Multiplatform `1.12.0`.
- Produces: Miuix `0.9.4-rc01` for all existing Miuix aliases, without a direct Skiko dependency.

- [ ] **Step 1: Preserve the real rendering regression boundary.**

  Before editing, record the known failing commands:

  ```bash
  ./gradlew :core:ui:jvmTest :feature:search:jvmTest \
    --configuration-cache --configuration-cache-problems=fail --no-parallel
  ```

  The expected pre-change defect is Miuix Squircle's `NoSuchMethodError` for `org.jetbrains.skia.Image.makeShader`; do not replace the production-composable test cases with a dependency-resolution-only assertion.

- [ ] **Step 2: Change only the Miuix catalog version.**

  Replace `0.9.3` with the user-selected `0.9.4-rc01`. Leave the Compose, Kotlin, Skiko, plugin, and repository declarations unchanged. Do not add a force, constraint, substitute, or compatibility shim.

- [ ] **Step 3: Verify UI rendering and resolution.**

  Re-run the focused UI test command from Step 1 and require success. Run `:core:ui:dependencyInsight --dependency org.jetbrains.skiko --configuration jvmTestRuntimeClasspath` to confirm that no direct Skiko workaround was introduced. Then run the full existing JVM test battery from `.github/workflows/quality.yml`.

### Task 3: Make quality and supported-platform builds explicit in GitHub Actions

**Files:**
- Modify: `.github/workflows/quality.yml`

**Interfaces:**
- Consumes: the existing `pull_request`/`main` triggers, macOS runner, checkout, Java 21, Gradle setup, and JVM test job.
- Produces: independent static-quality, Android build, desktop build, and Xcode iOS app build jobs; no platform test selection.

- [ ] **Step 1: Split the quality gate into visible authority checks.**

  Keep the `quality` job's shared setup. Replace its aggregate `qualityCheck` invocation with separate named steps that run `architectureCheck`, `spotlessCheck`, and `detekt`, each with `--configuration-cache --configuration-cache-problems=fail --no-parallel`. `spotlessCheck` is the repository's ktfmt-backed format gate. Do not remove `qualityCheck` from the Gradle build; it remains the local aggregate.

- [ ] **Step 2: Add Android and desktop build-only jobs.**

  Add separate `android-build` and `desktop-build` macOS jobs, each reproducing checkout, Temurin Java 21, and Gradle setup. Run, respectively:

  ```bash
  ./gradlew :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :desktopApp:compileKotlin --configuration-cache --configuration-cache-problems=fail --no-parallel
  ```

  Do not select Android Host Test tasks.

- [ ] **Step 3: Add the linked iOS app build-only job.**

  Add a separate `ios-build` macOS job with checkout and Temurin Java 21. Invoke:

  ```bash
  xcodebuild build \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination 'generic/platform=iOS Simulator' \
    CODE_SIGNING_ALLOWED=NO
  ```

  This selects the application build action only; do not use `xcodebuild test`, `:shared:iosSimulatorArm64Test`, or any Android Host Test task in the workflow.

- [ ] **Step 4: Review workflow contract.**

  Validate YAML structure and inspect the final workflow. Confirm there is one retained JVM test job, three explicit static-quality invocations, and all three supported platform build jobs. Confirm searches for `androidHostTest`, `iosSimulatorArm64Test`, and `xcodebuild test` return no workflow match.

### Task 4: Run acceptance gates and complete the OpenSpec handoff

**Files:**
- Modify: `openspec/changes/repair-build-quality-ci/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`

- [ ] **Step 1: Run static and architecture acceptance.**

  Run `./gradlew qualityCheck --configuration-cache --configuration-cache-problems=fail --no-parallel`, then independently run `./gradlew spotlessCheck --configuration-cache --configuration-cache-problems=fail --no-parallel` and `./gradlew detekt --configuration-cache --configuration-cache-problems=fail --no-parallel`. Run `openspec validate repair-build-quality-ci --strict`.

- [ ] **Step 2: Run supported-platform verification.**

  Run the Android and desktop commands from Task 3 plus the unsigned Simulator `xcodebuild build` command. Follow the repository's final baseline `./init.sh`; this local harness verification may run its existing iOS Simulator shared tests, but those tests must not be added to CI.

- [ ] **Step 3: Complete tracking and review the final change.**

  Check off only verified OpenSpec tasks. Update `progress.md` with route, root cause, exact changed files, command outputs, and next safe action; add the concise completed build/CI repair entry to `roadmap.md`. Review the final diff against this plan, preserve the pre-existing user test change, then commit the completed OpenSpec workflow with a conventional message such as `fix: restore build quality and platform CI gates`.
