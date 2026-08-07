# Task 5.3 Implementation Report

## Final Lifecycle Status - 2026-08-07

The historical planning/checkpoint evidence below culminated in atomic implementation commit
`90e330d24b10b9668263002b9cc37945d24e9643` (`refactor: extract search feature`), directly
following bound planning commit `f947724a9a2a29e5863976cb2c17fc16225bd336`. Final
behavior/spec/quality and exact 20-path boundary reviews are `PASS / APPROVED`. The final
post-review acceptance report is authoritative for completion evidence. This documentation
closeout is pending commit; no closeout SHA is asserted. Earlier RED/failure/review entries
remain historical evidence and are not superseded or deleted.

Route: OpenSpec + Superpowers / SDD

## Start Gate

Required planning HEAD: `0f18e5cdb1242efe528290c286779f2f588ad820`.

Observed `HEAD`: `0f18e5cdb1242efe528290c286779f2f588ad820`.

The task cannot begin because the required clean tracked worktree/index precondition is false. Before any implementation command or edit, `git status --short` reported:

```text
 M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md
```

The index is empty and no untracked files were present before this report was created. The tracked modification is a pre-existing addition to the SDD ledger stating that Task 5.3 is in progress. It is not one of the 19 permitted implementation endpoints. Per the task brief, it must not be overwritten, staged, or included in an implementation snapshot.

## Required Absent-Module RED

The required RED was already run before registration and was not rerun. Raw log: `.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.3-absent-module-red.log`.

Command:

```text
./gradlew :feature:search:jvmTest --configuration-cache --configuration-cache-problems=fail
```

Result: exit failure (`BUILD FAILED`; raw log records `BUILD FAILED in 1s`). No Search task or compilation ran.

Exact causal diagnostic block:

```text
* What went wrong:
Selection failed
  Cannot locate tasks that match ':feature:search:jvmTest' as project 'search' not found in project ':feature'.
```

This is the accepted absent-project causal failure. It must not be rerun after registration.

## Path Inventory

No implementation endpoint was created, modified, deleted, staged, or tested in this run. No module registration occurred.

The literal implementation inventory remains the 19 paths specified by `task-5.3-brief.md` lines 401-419. This report is the only file created by this attempt and is an expressly excluded evidence path.

## Commands And Results

| Command | Result |
| --- | --- |
| `git status --short && git rev-parse HEAD` | Blocked: required HEAD matches, but tracked SDD ledger is modified. |
| `git diff -- .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md` | Confirmed the pre-existing ledger-only modification. |
| `git diff --cached --name-status` | Passed: index empty. |
| `git ls-files --others --exclude-standard` | Passed before this report: no untracked files. |

No Gradle test/build/XML result was produced in this run. No target/class XML counts are applicable.

## Deviations And Blockers

Blocker: the worktree was not clean at task start, contrary to the explicit Task 5.3 precondition. Continuing would make it impossible to truthfully establish the prescribed clean baseline and pre-stage manifest state without modifying or discarding another owner’s SDD ledger change.

Required next action: restore a clean tracked worktree and empty index, preserving the ledger change through its owning workflow as appropriate, then restart Task 5.3 from planning HEAD. Do not rerun the absent-module RED after Search registration.

## Self-Review

- No implementation source, Gradle, resource, test, architecture, OpenSpec, plan, progress, or roadmap endpoint was changed.
- No staging or commit occurred.
- The pre-existing tracked change was not reverted, stashed, or altered.
- Runtime, device, visual, accessibility, and playback-engine behavior remain unverified.

## Blocker Resolution

The controller formally resolved the prior start-gate blocker with approved planning commit
`0370d23899f14c3e51e328c1444d9926b83e931c`. Resume state verified before implementation:

```text
HEAD: 0370d23899f14c3e51e328c1444d9926b83e931c
index: empty
tracked worktree:  M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md
```

The sole tracked modification is the controller-owned SDD progress ledger authorized by the
regenerated brief. It remains unstaged and unmodified by this implementation. No implementation
endpoint was changed before the resumed work began. The retained absent-module RED remains accepted
and will not be rerun.

## Partial Implementation Checkpoint

This checkpoint is incomplete and unstaged. Completed endpoints are the new Search module build,
source, EN/ZH resources, common filter tests, and JVM selection tests; module registration; the
Shared dependency, EN/ZH resource removals, direct `LibraryRoutes` adapter and old Search source
removals; the new Home selection test and old Search selection test removal; and the architecture
allow-list update.

Still required before Task 5.3 can continue: Search architecture fixtures in
`ArchitectureCheckPluginFunctionalTest.kt`; Shared production-adapter coverage in
`SearchRouteAdapterJvmTest.kt`; the specified `LibraryAppShell.kt` call-site adaptation; and the
listed `SettingsPlaylistBackupEmbeddingTest.kt` cleanup/adaptation. The moved JVM selection suite
also requires the remaining seven named production-composable tests and the full four-method
contract specified by the brief.

### Test And Build Evidence

The first focused command after relocation failed at `:feature:search:compileKotlinJvm` because an
explicit `weight` import resolved to an internal property. Removing that import was the sole repair.

```text
./gradlew :feature:search:jvmTest :shared:compileKotlinJvm --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Passed: `BUILD SUCCESSFUL in 11s`.

| Target/class | Tests | Skipped | Failures | Errors |
| --- | ---: | ---: | ---: | ---: |
| Feature JVM `SearchFilterTest` | 3 | 0 | 0 | 0 |
| Feature JVM `SearchSelectionPoliciesJvmTest` | 3 | 0 | 0 | 0 |

Android-host/iOS filter XML, Shared route/Home XML, architecture TestKit, quality checks, Xcode,
`./init.sh`, and final diff verification have not run. Nothing has been staged or committed.

Next dependency: complete architecture fixtures and the required Shared adapter tests before any
manifest gate or acceptance command.

## Architecture Fixture Checkpoint

Persistent write scope for this checkpoint was limited to
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`.
The Search fixture adds the four named selectors, uses the feature convention with Android/JVM/iOS
targets and external repository processor JAR, executes all four KSP target tasks, and audits the
five moved Search resource keys across EN/ZH plus generated-resource imports.

The required causal RED temporarily removed only the three Search allow-list facts. Command:

```text
./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeatureConventionPublishesRootsAndKspRegistrations' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

The processor build passed. The selector failed as expected (exit failure) with:

```text
ARCH-EDGE :feature:search [architecture] -> :core:ui
ARCH-EDGE :feature:search [architecture] -> :feature:library:api
ARCH-EDGE :shared [architecture] -> :feature:search
```

The allow-list was restored and `cmp -s /tmp/task-5.3-allow-list-before.diff <(git diff -- build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt)` passed, proving its final diff is byte-for-byte unchanged from checkpoint start.

GREEN focused commands each passed with `--rerun-tasks --no-configuration-cache --no-parallel`
and the same external processor property:

- `searchFeatureConventionPublishesRootsAndKspRegistrations`
- `searchFeatureRejectsForbiddenEdgesAndSharedExposure`
- `searchFeatureRejectsWrongPackageNamespaceKoinAndIosExport`
- `searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports`

The full command passed:

```text
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Result: `BUILD SUCCESSFUL in 3m 41s`. XML for
`ArchitectureCheckPluginFunctionalTest`: 75 tests, 0 skipped, 0 failures, 0 errors.

Checkpoint path audit: the only persistent implementation path written in this checkpoint is
`ArchitectureCheckPluginFunctionalTest.kt`; no staging or commit occurred. The report is the only
permitted evidence path changed.

Limitation: the fixture’s resource audit is positive ownership/import coverage; it does not yet
model five independent fixture mutations for missing, duplicate, wrong-owner, wrong-namespace, and
foreign-import failures. Koin and explicit Shared `api` exposure are likewise not distinct fixture
mutations. This checkpoint therefore does not complete the full Task 5.3 architecture contract.

## Final Narrow Architecture Checkpoint

Completed the missing one-at-a-time negative fixture mutations within the existing four Search
selectors:

- `MissingMovedResource`: `SEARCH-RESOURCE missing moved key`
- `DuplicateMovedResource`: `SEARCH-RESOURCE duplicate key across Shared/Search`
- `WrongResourceOwner`: `SEARCH-RESOURCE wrong owner for moved key`
- `InvalidResourceNamespace`: real `ARCH-RESOURCE` failure through `architectureCheck`
- `ForeignFeatureResourceImport`: `SEARCH-RESOURCE feature imports Shared generated resources`
- `ForeignSharedResourceImport`: `SEARCH-RESOURCE Shared imports Search generated resources`
- `KoinUsage`: `SEARCH-KOIN feature source imports Koin`
- `SharedApiExposure`: `SEARCH-SHARED-API Shared exposes Search through api`

The external processor was rebuilt with:

```text
./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
```

Result: exit 0, `BUILD SUCCESSFUL in 15s`.

Focused selectors ran with the external processor JAR and forced nested reruns:

```text
./gradlew :build-logic:convention:test \
  --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeatureRejectsForbiddenEdgesAndSharedExposure' \
  --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeatureRejectsWrongPackageNamespaceKoinAndIosExport' \
  --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports' \
  --rerun-tasks --no-configuration-cache --no-parallel \
  -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Result: exit 0, `BUILD SUCCESSFUL in 41s`; XML: 3 tests, 0 skipped, 0 failures, 0 errors.

Full functional suite command:

```text
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test \
  --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel \
  -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Result: exit 0, `BUILD SUCCESSFUL in 4m 22s`; TestKit XML: 75 tests, 0 skipped, 0 failures, 0 errors.

Scope audit: this checkpoint persistently modified only
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`
and this ignored report. Existing partial Task 5.3 paths and the controller-owned ledger remain
unstaged and unchanged by this checkpoint. No stage or commit occurred.

## Architecture Governance Repair (Bound HEAD `f947724a`)

Continuation gate: passed at `f947724a9a2a29e5863976cb2c17fc16225bd336`. The index was empty;
the retained Search checkpoint and controller-owned SDD ledger were preserved. No staging or commit
occurred.

### RED

The external repository processor artifact was rebuilt before all TestKit KSP runs:

```text
./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
```

Result: exit 0, `BUILD SUCCESSFUL`.

Fixed regressions were added first and executed against the pre-repair checker:

```text
./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeatureRejectsSharedCommonMainApiExposure' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeatureRejectsWrongExpectedNamespaces' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Both exited nonzero as expected. The Shared fixture used real
`commonMainApi(project(":feature:search"))` and `architectureCheck` completed successfully,
demonstrating that the existing `isAllowed(from, to)` ignored the actual configuration. The
namespace fixture likewise completed `:architectureCheck` successfully with deliberately wrong
Android and Compose namespaces. These are the causal REDs for this repair. A parallel invocation
attempt produced local Gradle test-result `EOFException`/missing binary-result artifacts; selectors
were then rerun serially with `cleanTest --no-parallel` and no such collision remained.

### Production Repair

- `ArchitectureAllowList.isAllowed` now receives `from`, actual configuration, and `to`.
  Every existing edge allowance is unchanged; only `:shared -> :feature:search` is accepted for
  `commonMainImplementation`. `commonMainApi` deterministically emits:
  `ARCH-EDGE :shared [commonMainApi] -> :feature:search`.
- Search has code-owned expected namespaces: Android
  `com.eterocell.rhythhaus.search` and Compose
  `rhythhaus.feature.search.generated.resources`. `ArchitectureCheckTask` validates those existing
  registry records and emits deterministic `ARCH-RESOURCE` records for a mismatch while retaining
  the prior `<invalid>` and unsupported-root behavior.

### GREEN

Focused selectors ran serially with `--rerun-tasks --no-configuration-cache --no-parallel` and the
external processor property:

```text
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeature*' --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Result: exit 0, `BUILD SUCCESSFUL`. Coverage includes real Shared
`commonMainImplementation` GREEN and `commonMainApi` RED; wrong Android and Compose namespaces;
exact Search KSP registry records/package roots; exactly one direct project processor dependency on
each real KSP configuration; controlled `kspJvm` project-dependency removal while registry remains;
forbidden edges; iOS export; real production-root Koin audit; and list/multiset EN/ZH resource
partition controls for duplicate EN/ZH, extra, missing, cross-owner duplicate, wrong owner, wrong
namespace, and both foreign generated imports.

Full functional verification:

```text
./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Result: exit 0, `BUILD SUCCESSFUL in 1m 43s`. XML:
`ArchitectureCheckPluginFunctionalTest` = 78 tests, 0 skipped, 0 failures, 0 errors.

Root check:

```text
./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Result: exit 0, `BUILD SUCCESSFUL in 1s`; configuration cache stored.

### Remaining Continuation Work

This checkpoint repairs the configuration-aware edge and exact Search-namespace governance defects.
It does not complete the full Task 5.3 continuation matrix: separate malformed-source
`kspAndroidMain`, `kspKotlinJvm`, `kspKotlinIosArm64`, and `kspKotlinIosSimulatorArm64` failure and
restoration assertions, top-level/member/constructor-property KDoc controls, and the remaining
Task 5.3 production/adapter acceptance work still require implementation before final task
completion can be claimed.

Scope evidence: persistent implementation writes were limited to
`ArchitectureAllowList.kt`, `ArchitectureCheckTask.kt`, and
`ArchitectureCheckPluginFunctionalTest.kt`; this report is the only evidence append. No stage or
commit occurred.

## Search Processor And KDoc Closure Continuation

Scope: only
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`
was persistently modified for this continuation. `ArchitectureAllowList.kt` and
`ArchitectureCheckTask.kt` were not touched. No staging or commit occurred.

The repository processor was rebuilt before each focused/full TestKit run:

```text
./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
```

Result: exit 0, `BUILD SUCCESSFUL`. The Search fixture publishes that external JAR exclusively as
the `:architecture-processor` project artifact. It contains no `files(processorJar)` KSP additions;
`build-logic.kmp.feature.impl` provides the direct project dependency on each real KSP
configuration.

### Four-Target Processor Proof

One malformed fixed common source was used throughout the RED pass:

```kotlin
package outside.fixture
/** Invalid package. */
public class SearchFeature
```

The fixture invoked each target task separately with nested `--rerun-tasks --no-configuration-cache`:

```text
:feature:search:kspAndroidMain
:feature:search:kspKotlinJvm
:feature:search:kspKotlinIosArm64
:feature:search:kspKotlinIosSimulatorArm64
```

Each invocation failed with task outcome `FAILED`, was neither `SKIPPED`, `NO-SOURCE`, nor
`UP-TO-DATE`, and contained exactly the repository-processor diagnostic:

```text
ARCH-PACKAGE :feature:search:SearchFeature.kt (outside.fixture)
```

The same fixture then restored only `SearchFeature.kt` to its valid documented source and reran the
same four tasks separately. All four outcomes were `SUCCESS`, with the same non-skipped,
non-`NO-SOURCE`, non-`UP-TO-DATE` assertions.

### KDoc Closure Controls

Real `:feature:search:kspKotlinJvm` KSP execution now has one-variable fixture controls for:

- undocumented top-level `SearchFeature`:
  `ARCH-KDOC :feature:search:SearchFeature.kt:2 (com.eterocell.rhythhaus.search.SearchFeature)`;
- documented public class with undocumented public member:
  `ARCH-KDOC :feature:search:SearchFeature.kt:5 (com.eterocell.rhythhaus.search.SearchFeature.undocumentedMember)`;
- documented public data class with undocumented public constructor property:
  `ARCH-KDOC :feature:search:SearchFeature.kt:5 (com.eterocell.rhythhaus.search.SearchFeature.undocumentedProperty)`.

Each failure restores only the fixture source to valid documented `SearchFeature` and proves a
subsequent real `kspKotlinJvm` `SUCCESS` outcome.

The exact registry/package-root/direct-project-dependency positive and the causal controlled
`kspJvm` direct-project-dependency removal mismatch remain selected and green.

Focused verification:

```text
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeature*' --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Result: exit 0, `BUILD SUCCESSFUL in 31s`.

Full verification:

```text
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Result: exit 0, `BUILD SUCCESSFUL in 1m 39s`. XML:
`ArchitectureCheckPluginFunctionalTest` = 80 tests, 0 skipped, 0 failures, 0 errors.

`git diff --check` passed. The strict implementation scope audit confirms this continuation added
only the authorized functional-test source path; existing retained Task 5.3 Search checkpoint paths
and the controller-owned ledger were preserved. No stage or commit occurred.

## Fixture Evidence Reconciliation: Real Roots And Exact Processor Diagnostics

This section supersedes the `80 tests` count and the earlier overbroad Koin wording in the preceding
continuation section. The previous Koin helper walked a generated Search fixture source tree; it did
not prove audit of repository Search production roots. The following changes correct that evidence
gap without changing production checker code.

### RED And Causality

- The prior fixture-only Koin audit could pass even if a repository Search production source were
  changed, because it accepted a generated fixture project rather than explicit repository roots.
  The replacement test identifies repository roots from
  `rhythhaus.rootDir/feature/search/src/*Main/kotlin`, selecting only existing production `*Main`
  roots and structurally excluding all test roots. It first audits the actual repository roots, then
  copies precisely those roots to a temporary fixture and adds exactly one `org.koin` import to one
  copied production Kotlin source. The same audit function deterministically fails with:
  `SEARCH-KOIN feature source imports Koin: <copied-source-path>`.
- The first empty-package-root fixture mutation was applied before convention lifecycle completion;
  `kspKotlinJvm` unexpectedly succeeded. This was a causal RED proving the mutation timing was
  ineffective. Moving only the mutation to fixture `afterEvaluate` changed
  `architecture.packageRoots` to the empty string after convention configuration. Its real processor
  run then failed with the exact sole diagnostic:
  `ARCH-PACKAGE :feature:search:SearchFeature.kt (com.eterocell.rhythhaus.search)`.
- The explicit public-constructor property diagnostic was observed at source line 7, not line 5.
  The fixture now accurately asserts the actual repository processor output while retaining the
  documented public class, explicitly declared/documented public constructor, and a sole
  undocumented public constructor property.

### Final Processor Controls

Search KSP diagnostic extraction now collects only
`ARCH-PACKAGE`/`ARCH-KDOC` records for `:feature:search` in output order. Every four-target
malformed-source run and every KDoc negative compares that list for exact equality to a singleton
expected diagnostic, rejecting extra or duplicate processor diagnostics. Valid restoration requires
the target task to be `SUCCESS`, not skipped/no-source/up-to-date, and an empty extracted diagnostic
list.

The final Search fixture controls include:

- four separate real KSP targets under one fixed malformed common source, then four restored-source
  successes;
- exact top-level, public-member, and explicit-public-constructor-property KDoc diagnostics;
- real empty `architecture.packageRoots` processor failure;
- repository-root positive Koin audit and copied-production-root one-import negative through the
  same audit function;
- retained exact package roots, four registry records, one direct processor `ProjectDependency` per
  real KSP configuration, and causal `kspJvm` dependency-removal mismatch.

Verification used the externally rebuilt repository processor JAR only as the
`:architecture-processor` project artifact:

```text
./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchFeature*' --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.searchResourceOwnershipRetainsExactEnZhPartitionsWithoutForeignImports' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
```

Focused result: exit 0, `BUILD SUCCESSFUL in 32s`. Full result: exit 0,
`BUILD SUCCESSFUL in 1m 42s`. Final XML:
`ArchitectureCheckPluginFunctionalTest` = 82 tests, 0 skipped, 0 failures, 0 errors.

Final `git diff --check` passed. Persistent implementation writes in this reconciliation remain
limited to `ArchitectureCheckPluginFunctionalTest.kt`; this ignored report is the only evidence
append. Existing retained Task 5.3 checkpoint changes and the controller-owned ledger were
preserved. No stage or commit occurred.

## Shared Production Adapter/Caller Regression Checkpoint

Continuation gate was observed at bound `HEAD`
`f947724a9a2a29e5863976cb2c17fc16225bd336`; retained checkpoint changes were preserved and the
index remains unstaged. The direct `LibraryRouteOverlays` Search adapter already called production
`SearchContent` with Shared-owned strings, the Shared `EqualizerStrip` slot, Search selection and
scroll callbacks, and real `playSearchTrack` selection. Added the requested Shared JVM adapter
coverage and a Settings embedding preservation control without modifying feature Search paths.

### RED / Blocker

The required focused Shared command was run serially:

```text
./gradlew :shared:jvmTest --tests '*HomeSelectionPoliciesJvmTest' --tests '*SearchRouteAdapterJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
```

It failed before executing either selected class because the retained concurrent/untracked
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/HomeSelectionPoliciesJvmTest.kt`
does not compile:

```text
HomeSelectionPoliciesJvmTest.kt:11:13 Type inference failed. The value of the type parameter 'T'
must be mentioned in input types (argument types, receiver type, or expected type).
```

This path is outside the exclusive write scope for this checkpoint, so it was not changed. No
Shared adapter test XML, Settings control XML, full `:shared:jvmTest`, or iOS simulator result can
be claimed while this prerequisite source compilation failure remains.

`git diff --check` passed after the blocked command. No staging or commit occurred.

## Search Feature Production-Composable Regression Continuation

Bound planning HEAD was retained as
`f947724a9a2a29e5863976cb2c17fc16225bd336`. This continuation preserved the
existing callback-first Search extraction and modified only the permitted Search source and test
paths. No test hook, Shared/library implementation dependency, Gradle file, resource, route, or
architecture path was changed. No staging or commit occurred.

### RED Then GREEN

The initial focused JVM run compiled the retained partial Search source but failed in the newly
expanded test source. The failures were test-only: `LibraryTrack` metadata is non-nullable, the
desktop semantics tree exposes both the field and rows as long-clickable, and Miuix visual text is
not surfaced as a standalone desktop text semantics node. The production composable behavior was
not changed to accommodate testing. Tests were corrected to use the actual row content-description
semantics and production `SearchContent` callbacks, then rerun serially.

```text
./gradlew :feature:search:jvmTest --tests '*SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Final result: exit 0, `BUILD SUCCESSFUL in 4s`; configuration cache reused. The production-content
class has all four migrated methods and the named blank query, filtering, count/no-match state,
focus, clear, primitive scroll/padding, current indicator/Now Playing semantics, visible sequence,
empty metadata, and duplicate occurrence/recomposition cases.

### Feature Matrix

```text
./gradlew :feature:search:jvmTest --tests '*SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:search:jvmTest --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:search:compileAndroidMain :feature:search:compileKotlinIosArm64 :feature:search:compileKotlinIosSimulatorArm64 :feature:search:jvmTest :feature:search:testAndroidHostTest :feature:search:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
```

All three commands exited 0. The final cross-platform command passed in 40 seconds with 143
actionable tasks (98 executed, 45 up-to-date) and stored the configuration cache. It included the
requested Android main compilation, iOS device/simulator compilation, JVM test, Android-host test,
and iOS simulator test.

Final feature XML results:

| Target/class | Tests | Skipped | Failures | Errors |
| --- | ---: | ---: | ---: | ---: |
| Feature JVM `SearchFilterTest` | 3 | 0 | 0 | 0 |
| Feature JVM `SearchSelectionPoliciesJvmTest` | 14 | 0 | 0 | 0 |
| Feature Android host `SearchFilterTest` | 3 | 0 | 0 | 0 |
| Feature iOS simulator `SearchFilterTest` | 3 | 0 | 0 | 0 |

The three `SearchFilterTest` target XML files each contain a positive count. The common test owns
exactly `blankAndWhitespaceQueriesHaveNoResults`,
`caseInsensitiveTitleArtistAndAlbumFilteringPreservesInputOrder`, and
`duplicateIdsAndEmptyMetadataArePreserved`, all through the internal production filter.

### Required Shared Gate Blocker

The required serial Shared adapter/Home selector was rerun unchanged:

```text
./gradlew :shared:jvmTest --tests '*HomeSelectionPoliciesJvmTest' --tests '*SearchRouteAdapterJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
```

It exited nonzero before either selected test class executed. The exact blocking task was
`:shared:compileTestKotlinJvm`, with three Kotlin errors in the out-of-scope retained file
`shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPlaylistBackupEmbeddingTest.kt`
at lines 34, 40, and 47: `This testing API is experimental and is likely to be changed or removed
entirely`. That path is outside this continuation's exclusive write scope and was not modified.
Consequently, the chained Shared/iOS/desktop/Android host command did not run, and no Shared XML
counts, Xcode validation, `./init.sh`, architecture/quality reruns, or full final platform claim is
made by this continuation.

### Scope And Diff Review

`git diff --check` passed before the Shared gate. The feature changes in this continuation are:

- `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`: approved
  public KDoc wording now explicitly describes Shared's structured composable label formatting;
  no UI hierarchy, callback, key, resource, or behavior change.
- `feature/search/src/commonTest/kotlin/com/eterocell/rhythhaus/search/SearchFilterTest.kt`:
  expanded formatting and coverage of the three exact common filter cases.
- `feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt`:
  production `SearchContent` regression matrix, including duplicate-safe remount interaction.

All other visible modified/deleted/untracked paths are retained checkpoint work owned by other Task
5.3 continuations. This report is the only evidence append. No files were staged or committed.

Status: `DONE_WITH_CONCERNS` for the authorized Search source/test continuation; the feature matrix
is green, but Task 5.3's required Shared and downstream acceptance matrix remains blocked by the
out-of-scope Shared test compilation errors above.

### Final Formatting Check

`./gradlew :feature:search:spotlessCheck --configuration-cache --configuration-cache-problems=fail
--no-parallel` exited nonzero at `:feature:search:spotlessKotlinGradleCheck`. Its only reported
violations are retained checkpoint formatting in the out-of-scope
`feature/search/build.gradle.kts` (the `ControlledComposeResourcesExtension` wrapping and the
long JVM test dependency declaration). No authorized Kotlin source/test path was reported. The
exclusive write scope prohibits correcting that Gradle file. Final `git diff --check` remained
successful; no stage or commit occurred.

### Shared Search Adapter Regression Repair - 2026-08-07

Route: regression-first repair of the Task 5.3 Shared Search adapter checkpoint.

RED:

- The first focused selector was green but did not select `SearchRouteAdapterJvmTest` because its
  fully qualified selector contained a package typo. The corrected selector exposed the pre-repair
  placeholder tests: direct `SearchSharedLabels` construction, ReconcileVisible-only equalizer
  evidence, and replacement `setContent` rather than one mutable production composition.
- During the repair, the corrected focused gate was intentionally RED first for the production
  semantics assertions: it exposed the actual localized JVM `select_track_format` output
  (`选择曲目 ...`), the equalizer tag's unmerged-tree location, and an off-screen selected row after
  the real lazy-list scroll. Each was corrected in the mounted adapter test; no production
  contract was changed for these failures.
- The first combined full Shared JVM/iOS command reached Shared JVM execution and failed one
  unrelated timing-sensitive playback test,
  `PlaybackSessionCoordinatorTest.newerPlayingProgressSurvivesDelayedMutationCheckpoint`, after
  311 tests. A standalone retry of the unchanged full Shared JVM suite was GREEN.

GREEN implementation:

- `LibraryRouteOverlays` no longer accepts `tagLibReader`; all overlay call sites are within the
  authorized paths. TagLib remains in `LibraryRouteContent`, details, and Now Playing paths.
- The real Search route keeps Shared formatting through
  `stringResource(Res.string.select_track_format, title)`, maps selection and scroll callbacks,
  and wraps the Shared-owned `EqualizerStrip` in the internal
  `shared-search-equalizer` test tag.
- `SearchRouteAdapterJvmTest` mounts production `LibraryRouteOverlays` and verifies localized row
  semantics, matching-playing and paused equalizer visibility, exact Start/Toggle/ReconcileVisible
  callbacks including duplicate IDs and unrelated recomposition, primitive list scroll position,
  and inactive non-Search selection semantics/behavior. The four accepted playback tests remain.
- `HomeSelectionPoliciesJvmTest` retains the explicit `List<TrackSelectionAction>` generic and now
  verifies exactly one clear plus the exact Albums/Artists browse destination. The Settings control
  retains its causal no-Search-action assertion and its `ExperimentalTestApi` opt-in; all six
  backup embedding tests remain.

Verification:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.HomeSelectionPoliciesJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.SearchRouteAdapterJvmTest' --tests 'com.eterocell.rhythhaus.settings.SettingsPlaylistBackupEmbeddingTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. XML: HomeSelectionPoliciesJvmTest 1/0/0/0;
SearchRouteAdapterJvmTest 7/0/0/0; SettingsPlaylistBackupEmbeddingTest 6/0/0/0.

./gradlew :shared:jvmTest --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN on retry. XML aggregate: 311/0/0/0.

./gradlew :shared:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. XML aggregate: 236/0/0/0.

git diff --check
GREEN.
```

The initial full-suite playback failure is recorded above as a retryable unrelated failure; the
successful retry is the final JVM result. No network failure occurred. No files were staged or
committed. This repair writes only its five authorized source/test paths and this evidence append;
other Task 5.3 worktree changes remain pre-existing checkpoint work.

### Shared Search Adapter Assertion Reconciliation - 2026-08-07

Route: test-only Oracle re-review repair. Production, feature, architecture, Home, and Settings
paths were not modified.

RED:

- The initial exact `SearchRouteAdapterJvmTest` run was GREEN at `7/0/0/0`, proving the three
  Oracle items were missing assertions rather than a current production failure.
- After adding the mounted partial lazy-list swipe, the exact focused gate was RED once at
  `:shared:compileTestKotlinJvm`: `Unresolved reference 'swipe'`. Adding the existing Compose test
  API import was the sole correction; no production code or test hook changed.

GREEN:

- `equalizerSlotIsSharedOwned` keeps one mounted composition and now proves the Shared equalizer is
  present only for a matching playing current track, absent after pause, and still absent for a
  playing nonmatching current track.
- `adaptsSelectionAndScrollFromProductionSearchContent` now proves the exact active Search set by
  observing `ToggleableState.Off` on a distinct rendered `other` row before selected-row
  interaction. It performs a real partial `LazyColumn` swipe after index 20, then requires an
  emitted `LibraryScrollPosition` with index 20 and positive offset, so an adapter that always
  maps offset zero cannot pass.

Verification:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.SearchRouteAdapterJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. XML: SearchRouteAdapterJvmTest 7/0/0/0.

./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.HomeSelectionPoliciesJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.SearchRouteAdapterJvmTest' --tests 'com.eterocell.rhythhaus.settings.SettingsPlaylistBackupEmbeddingTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. XML: HomeSelectionPoliciesJvmTest 1/0/0/0;
SearchRouteAdapterJvmTest 7/0/0/0; SettingsPlaylistBackupEmbeddingTest 6/0/0/0.

./gradlew :shared:jvmTest --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. Shared JVM XML aggregate: 311/0/0/0.

git diff --check
GREEN.
```

iOS was intentionally not rerun because this is JVM-test-only; the retained prior Shared iOS
simulator evidence is `236/0/0/0`. No network failure occurred. No files were staged or committed.

## Final Controller Acceptance Verification - 2026-08-07

Route: controller-owned final serial acceptance verification. Bound planning `HEAD` was
`f947724a9a2a29e5863976cb2c17fc16225bd336` before and after verification. The index remained
empty. Before commands, all 20 literal implementation endpoints were hashed (two required deleted
sources recorded as `DELETED`); after all commands their 20-entry hash manifest compared
byte-identically. The only nonimplementation worktree record remained the allowed unstaged
` M .superpowers/sdd/2026-07-27-feature-first-modularization/progress.md` ledger.

### Interrupted-session reconciliation

- The prior external-JAR TestKit invocation was interrupted by the closed subagent. Reconciliation
  found no active Gradle launcher, Xcode, or `init.sh` process, only idle Gradle/Kotlin daemons; no
  process was killed. Its XML had a fresh timestamp but no retained launcher exit status, so the
  exact TestKit command was rerun rather than inferred from XML.
- No implementation endpoint changed during either the interruption or restart; the final 20-endpoint
  hash comparison passed.

### Fresh serial acceptance commands

All Gradle commands below used `--no-parallel` and were run serially.

```text
./gradlew :feature:search:jvmTest --tests '*SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache stored.

./gradlew :shared:jvmTest --tests '*HomeSelectionPoliciesJvmTest' --tests '*SearchRouteAdapterJvmTest' --tests '*SettingsPlaylistBackupEmbeddingTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache stored.

./gradlew :feature:search:compileAndroidMain :feature:search:compileKotlinIosArm64 :feature:search:compileKotlinIosSimulatorArm64 :feature:search:jvmTest :feature:search:testAndroidHostTest :feature:search:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache stored.

./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache stored.

./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
PASS; 13 actionable tasks executed.

./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
PASS; BUILD SUCCESSFUL in 1m 45s; 13 actionable tasks executed.

./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache stored.

./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; output explicitly reported `Reusing configuration cache` and `Configuration cache entry reused`.

./gradlew spotlessCheck --configuration-cache --no-parallel
PASS; configuration cache stored. `spotlessApply` was controller-completed before this lane and was not rerun.

./gradlew detekt --configuration-cache --no-parallel
PASS; configuration cache stored.

PATH="$HOME/.nvm/versions/node/v26.7.0/bin:$PATH" openspec validate feature-first-modularization --strict
PASS: Change 'feature-first-modularization' is valid.

/usr/bin/xcrun xcodebuild -version
PASS: Xcode 26.6, build 17F113.

/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Debug CODE_SIGNING_ALLOWED=NO build
PASS: ** BUILD SUCCEEDED **.

/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' test
PASS: ** TEST SUCCEEDED **; iosAppTests executed 8 tests, 0 failures.

./init.sh
PASS within the controller hard 20-minute timeout; Shared JVM tests, desktop compile, Android debug build, Xcode probe, and Shared iOS simulator tests completed; `=== Harness verification complete ===` printed.

git diff --check
PASS.
```

The full consumer matrix emitted a non-fatal GitHub API rate-limit notice while resolving no new
dependencies; its Gradle command still completed `BUILD SUCCESSFUL`. No transient network retry
was required.

### Retained XML results

All values are `tests/skipped/failures/errors` from final retained XML:

| Target/class | Result |
| --- | ---: |
| Feature JVM `SearchFilterTest` | 3/0/0/0 |
| Feature JVM `SearchSelectionPoliciesJvmTest` | 14/0/0/0 |
| Feature Android host `SearchFilterTest` | 3/0/0/0 |
| Feature iOS simulator `SearchFilterTest` | 3/0/0/0 |
| Shared `HomeSelectionPoliciesJvmTest` | 1/0/0/0 |
| Shared `SearchRouteAdapterJvmTest` | 7/0/0/0 |
| Shared `SettingsPlaylistBackupEmbeddingTest` | 6/0/0/0 |
| Shared JVM aggregate | 311/0/0/0 |
| Shared iOS simulator aggregate | 236/0/0/0 |
| `ArchitectureCheckPluginFunctionalTest` | 82/0/0/0 |

### Completed-snapshot and static gates

- Exact NUL-safe pre-stage manifest gate: PASS. Empty index; literal manifest total `20`, unique
  `20`; exact implementation categories `8` create/move, `10` modified, and `2` removed; no
  extra tracked or untracked record; exactly one permitted unstaged SDD ledger record.
- Final 20-endpoint before/after hash manifest: PASS, byte-identical.
- Static ownership checks: PASS. No iOS framework export of `:feature:search`; no Search production
  Koin usage; exactly two feature public declarations (`SearchSharedLabels`, `SearchContent`);
  generated feature resources are used internally and no generated resource handle crosses the
  feature public boundary. The five moved resource keys exist only in feature Search EN/ZH and are
  absent from Shared; Shared retains adapter-owned structured `select_track_format` formatting.
- Final `git diff --check`: PASS. No staging or commit occurred.

### Remaining limitations

This is automated build/test and static-architecture evidence only. Android/iOS physical-device
runtime, desktop UI launch, rendered visual QA, accessibility/screen-reader behavior, live local
media scanning, and playback-engine/runtime interaction remain unverified.

## Final Task 5.3 Search UI Evidence Regression Closure - 2026-08-07

Route: regression-first, production-composable evidence polish. Exclusive implementation writes
were limited to `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
and `feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt`.
No public API, KDoc, callback ownership, resource ownership, architecture, Gradle, Shared source,
common test, staging, or commit changed.

### RED

The fixed feature JVM selector was run before production semantics support:

```text
./gradlew :feature:search:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
```

It failed at `:feature:search:compileTestKotlinJvm` because the new regression referenced absent
internal production tags `SearchResultCountTestTag`, `SearchNoMatchTestTag`, and
`SearchBottomSpacerTestTag`; the initial assertion imports also required alignment with the local
Compose JVM test API. This was the causal RED: counts/no-match rendering and terminal spacer had
no deterministic production-node identity. After tags were added, the same selector reached tests
and failed 3 of 14: the count/no-match and spacer tags existed only in the Miuix unmerged semantics
tree, and the indicator test initially used a second matching current ID. Those test failures were
resolved by querying the unmerged production nodes and changing only the nonmatching current ID.

The spacer's actual layout bounds are zero after terminal lazy-list placement, so a direct bounds
height assertion was not valid evidence of the supplied padding. The existing production spacer now
publishes its received `Dp.value` through a private internal semantics property; the regression
asserts exactly `96f`, so ignoring or hardcoding the supplied padding to zero fails causally.

### GREEN

`SearchContent` now gives only its existing result-count, no-match, and terminal spacer nodes
internal test tags and private semantic values. The user-visible text and accessibility output are
unchanged. The strengthened mounted regressions prove:

- all zero, singular, and plural count output plus query-formatted no-match output come from the
  actual feature resource rendering (the JVM resource environment selected the feature ZH values);
- a real `LazyColumn` index-20 partial swipe reports a positive primitive production offset, and
  its terminal spacer receives the supplied `96.dp` value;
- one mounted `SearchContent` starts focused, then remains unfocused after force-clear and an
  unrelated recomposition, with no second `setContent` or production hook;
- a tagged test indicator exists only for current plus playing, then is absent for paused and for a
  playing nonmatching current ID in the same mutable mounted composition.

```text
./gradlew :feature:search:spotlessKotlinApply :feature:search:spotlessCheck :feature:search:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. SearchSelectionPoliciesJvmTest XML: 14/0/0/0.

./gradlew :feature:search:compileAndroidMain :feature:search:compileKotlinIosArm64 :feature:search:compileKotlinIosSimulatorArm64 :feature:search:jvmTest :feature:search:testAndroidHostTest :feature:search:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. Feature JVM SearchFilterTest: 3/0/0/0; SearchSelectionPoliciesJvmTest: 14/0/0/0;
Android-host SearchFilterTest: 3/0/0/0; iOS-simulator SearchFilterTest: 3/0/0/0.

./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN.

/usr/bin/xcrun xcodebuild -version
GREEN: Xcode 26.6, build 17F113.

git diff --check
GREEN.
```

Test count did not change: `SearchSelectionPoliciesJvmTest` remains 14 JVM cases. No transient
network error occurred. No files were staged or committed. Runtime/device/visual/accessibility
acceptance remains outside this automated evidence closure.

## Final Oracle Spacer-Measurement Reconciliation - 2026-08-07

Route: regression-first correction for the final Oracle finding. Exclusive implementation writes
remained `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt` and
`feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchSelectionPoliciesJvmTest.kt`.
The ignored report is the only evidence append. No public API, visual, accessibility, architecture,
resource, common-test, Shared, Gradle, staging, or commit change occurred.

### RED

`SearchBottomSpacerHeight` and its `bottomContentPadding.value` semantics assignment were removed.
The first bounds-based test conversion failed to compile because the local Compose test receiver has
no direct `Dp.toPx()` API. It was corrected to convert `96.dp.value` through the mounted spacer
node's `layoutInfo.density.density`.

The resulting direct `boundsInRoot.height` assertion was RED with exact values `expected 96.0`,
`actual 0.0`: the terminal spacer is clipped at the lazy viewport edge, so root-space bounds cannot
represent its full measured height. `performScrollTo()` retained the same clipped root result.
The production semantics hook was not restored. The regression instead reads the real tagged spacer
node's measured layout coordinates, which are the deterministic rendered-size equivalent for this
terminal clipped node.

One-variable causality was then verified by temporarily removing only
`.height(bottomContentPadding)` from the actual production terminal `Spacer`. The same focused test
failed at the tagged terminal spacer lookup after the list terminal scroll, proving that zero/removed
padding cannot pass this production-composable regression. The exact production modifier was
restored unchanged.

### GREEN

The real mounted `LazyColumn` test still scrolls to index 20, performs a partial swipe, and requires
a positive production primitive offset. It then scrolls to the terminal spacer, performs a real
`performScrollTo`, fetches its semantics node, converts `96.dp` using that node's Compose test
density, and asserts `layoutInfo.coordinates.size.height` equals the expected pixels within `0.5f`.
The internal tag remains only on the actual spacer; no custom spacer-height semantics value exists.

```text
./gradlew :feature:search:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest.reportsPrimitiveScrollAndBottomPadding' --configuration-cache --configuration-cache-problems=fail --no-parallel
RED after removing only .height(bottomContentPadding): terminal spacer lookup failed.

./gradlew :feature:search:jvmTest --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. SearchSelectionPoliciesJvmTest XML: 14/0/0/0.

./gradlew :feature:search:jvmTest --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN. Feature JVM SearchFilterTest: 3/0/0/0; SearchSelectionPoliciesJvmTest: 14/0/0/0.

./gradlew :feature:search:spotlessKotlinApply :feature:search:spotlessCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
GREEN.

git diff --check
GREEN.
```

Test count remains unchanged. No transient network error occurred. No files were staged or
committed.

## Final Post-Review Snapshot Acceptance Verification - 2026-08-07

This section supersedes the earlier `Final Controller Acceptance Verification` section for the
post-Oracle-review Search production/test snapshot. The earlier section remains historical evidence
only. Final behavior review is PASS/APPROVED.

Bound planning `HEAD` remained `f947724a9a2a29e5863976cb2c17fc16225bd336`; index remained empty.
All 20 literal implementation endpoints were hashed before root formatting. The explicitly
authorized command `./gradlew spotlessApply --configuration-cache --no-parallel` passed
(`BUILD SUCCESSFUL in 22s`). It produced no hash delta among the 20 endpoints. Its resulting status
set remained exactly the approved implementation records plus the sole controller-owned unstaged
SDD progress ledger. A second final hash capture after every command was byte-identical to the
post-formatter set.

All Gradle/TestKit/Xcode commands ran serially with no overlap. No transient network failure
required retry.

```text
./gradlew :feature:search:jvmTest --tests '*SearchSelectionPoliciesJvmTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache reused.

./gradlew :shared:jvmTest --tests '*HomeSelectionPoliciesJvmTest' --tests '*SearchRouteAdapterJvmTest' --tests '*SettingsPlaylistBackupEmbeddingTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache reused.

./gradlew :feature:search:compileAndroidMain :feature:search:compileKotlinIosArm64 :feature:search:compileKotlinIosSimulatorArm64 :feature:search:jvmTest :feature:search:testAndroidHostTest :feature:search:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache reused.

./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; configuration cache reused.

./gradlew :architecture-processor:clean :architecture-processor:jar --rerun-tasks --no-configuration-cache --no-parallel
PASS; 13 actionable tasks executed.

./gradlew :build-logic:convention:cleanTest :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks --no-configuration-cache --no-parallel -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"
PASS; BUILD SUCCESSFUL in 1m 44s; XML 82/0/0/0.

./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; output explicitly reported `Reusing configuration cache` and `Configuration cache entry reused`.

./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
PASS; output explicitly reported `Reusing configuration cache` and `Configuration cache entry reused`.

./gradlew spotlessCheck --configuration-cache --no-parallel
PASS; configuration cache stored.

./gradlew detekt --configuration-cache --no-parallel
PASS; configuration cache reused.

PATH="$HOME/.nvm/versions/node/v26.7.0/bin:$PATH" openspec validate feature-first-modularization --strict
PASS: Change 'feature-first-modularization' is valid.

/usr/bin/xcrun xcodebuild -version
PASS: Xcode 26.6, build 17F113.

/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -configuration Debug CODE_SIGNING_ALLOWED=NO build
PASS: ** BUILD SUCCEEDED **.

/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17' test
PASS: ** TEST SUCCEEDED **; iosAppTests executed 8 tests, 0 failures.

./init.sh
PASS within the controller hard 20-minute timeout; all phases completed and `=== Harness verification complete ===` printed.

git diff --check
PASS.
```

### Final XML counts

All values are `tests/skipped/failures/errors` from the current retained XML:

| Target/class | Result |
| --- | ---: |
| Feature JVM `SearchFilterTest` | 3/0/0/0 |
| Feature JVM `SearchSelectionPoliciesJvmTest` | 14/0/0/0 |
| Feature Android host `SearchFilterTest` | 3/0/0/0 |
| Feature iOS simulator `SearchFilterTest` | 3/0/0/0 |
| Shared `HomeSelectionPoliciesJvmTest` | 1/0/0/0 |
| Shared `SearchRouteAdapterJvmTest` | 7/0/0/0 |
| Shared `SettingsPlaylistBackupEmbeddingTest` | 6/0/0/0 |
| Shared JVM aggregate | 311/0/0/0 |
| Shared iOS simulator aggregate | 236/0/0/0 |
| `ArchitectureCheckPluginFunctionalTest` | 82/0/0/0 |

### Final scope and static gates

- `git diff --check`: PASS.
- Exact NUL-safe pre-stage gate: PASS. Manifest total/unique `20/20`; exact implementation
  categories `8` create/move, `10` modified, `2` removed; empty index; no extra tracked or
  untracked paths; exactly one allowed unstaged SDD progress ledger.
- No formatting content changes were made by the authorized root `spotlessApply`; its post-format
  20-endpoint hash set was byte-identical to the final post-verification set.
- Static checks: PASS. No Search iOS framework export; no production Search Koin usage; exactly two
  public Search declarations; the moved resource keys exist only in Search EN/ZH and remain absent
  from Shared. Search-generated resource handles remain internal and do not cross the public API.
- No active Gradle launcher, `xcodebuild`, or `init.sh` process remained. No staging or commit
  occurred.

### Unverified limitations

Physical-device runtime, desktop UI launch, visual/rendered QA, accessibility/screen-reader
behavior, live local-media scanning, and playback-engine/runtime behavior are not claimed by this
automated verification.
