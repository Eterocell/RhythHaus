## 1. Slice 0 - Reconcile Prior Work

- [x] 1.1 Record verification that `architecture-refactor` is 12/12 complete and reconcile package-organization's stale 0/5 tracking with commits `f0310e5`, `06f8a16`, and `adb1e3d`; add a RED tracker/architecture assertion that prevents duplicate package moves.
- [x] 1.2 Update the relevant OpenSpec/progress/roadmap evidence to distinguish reconciled prior package work from this new module migration; run the focused reconciliation check and strict OpenSpec validation.

## 2. Slice 1 - Governance Baseline

- [x] 2.1 Independent final Oracle re-review PASS after correction. `ArchitectureCheckPluginFunctionalTest` XML reports 46 tests, 0 skipped, 0 failures, and 0 errors, including external canonical repository-built `:architecture-processor` JAR/SPI/KSP proof and real Android-KMP host/device aggregate identity positive, authored-self, and fail-closed cardinality cases. Root `architectureCheck` passed twice with configuration-cache reuse. Strict feature OpenSpec validation, `spotlessCheck`, `detekt`, and `git diff --check` passed. `./init.sh` was manually stopped after more than 9000 seconds at user request and was not rerun; the full JVM/Android/desktop/iOS platform matrix remains uncertain and is not claimed. Task 1.4 remains separate and out of scope.
- [x] 2.2 Add the canonical architecture skill, architecture document, boundary/shared-iOS-export ADRs, and feature README conventions; link AGENTS guidance without duplicating policy.
- [x] 2.3 Independent final Oracle re-review PASS after correction. Shared build logic's immutable normalized architecture-model registry and controlled conventions are accepted. Android application test identities come from public `ApplicationVariant` test `Component.compileConfiguration`/`runtimeConfiguration`; Android-KMP library identities are distinct and come from public `KotlinMultiplatformAndroidLibraryTarget.compilations` host/device APIs. The 46-test functional XML and twice-run configuration-cache-reused root `architectureCheck` evidence are recorded in the authoritative Task 1.3 final acceptance report. Task 1.4 wiring remains out of scope.
- [x] 2.4 Independent Oracle review PASS with no Critical/Important/Minor findings. Root `check`/`qualityCheck` depend on `architectureCheck`; `qualityCheck` provider-aggregates Detekt and Spotless checks across all projects. Exact-diagnostic TestKit entrypoints and a real child `Copy`-sentinel fixture (including second-run inner cache reuse) passed. Dedicated unfiltered `quality.yml` invokes only canonical `qualityCheck` for PRs and `main` pushes. Serial processor JAR/full convention tests, production quality gates, and diff hygiene passed; root quality cache reuse is not claimed because of the unchanged Spotless baseline. `./init.sh` was not rerun after the user-requested >9000-second stop, so the platform matrix remains uncertain.

## 3. Slice 2 - Core Model And UI

- [x] 3.1 Task 2.1 and Task 2.2 provide the required RED characterization/dependency coverage for immutable projections and reusable UI/theme/generic-artwork boundaries; Task 2.2 retains feature UI state and track-artwork loading in `:shared`.
- [x] 3.2 Task 2.1 created `:core:model`; Task 2.2 created `:core:ui` with package preservation. Focused tests, architectureCheck, Detekt, Spotless, and strict configuration-cache acceptance gates are GREEN. Initial final quality review PASS found no Critical/Important findings; cleanup verification confirmed the two source corrections and required the ledger to distinguish core theme policy/palettes from the private shared `RhythHausTheme` composition. The corrected source internalizes `ArtworkImageRole.keySuffix` and derives the core UI JVM ui-test version from `libs.versions.compose.multiplatform`; final cleanup-ledger re-review PASS found no findings.
- [x] 3.3 Task 2.2 passed explicit Android packaging/resource, desktop compilation, and iOS simulator compilation checks; Xcode 26.6 was available. Existing `docs/architecture.md` and ADR 0001 already describe the exact accepted boundary, so no speculative canonical-document churn was needed. Runtime Chinese rendering, Android/iOS interaction, platform decoder correctness, and linked iOS-app resource lookup remain unverified limits.

Checkpoint: Task 2.1 accepted the `:core:model` half and Task 2.2 accepted `:core:ui`; OpenSpec 3.1, 3.2, and 3.3 are complete. `./init.sh` remains intentionally not rerun after the prior user-directed stop beyond 9000 seconds, so the full init matrix is not claimed.

## 4. Slice 3 - Database And Platform

- [x] 4.1 RED characterization passed: absent module, absent generated/seam compilation, and owner-fixture REDs preceded the atomic move. Core v1/legacy migration coverage proves versions, preserved rows, FK rejection/cascade, generated identity, and filename without `:shared`/model dependencies; repository behavior remains shared. Final architecture fixture XML is 52 tests, 0 failures/errors, 1 expected skip.
- [x] 4.2 Accepted atomic ownership move: `:core:database` owns six `.sq`, `1.sqm`, v1 `1.db`, generated/runtime surface, seam/actual drivers, and database tests unchanged. Shared retains repositories, DI, `LibraryDatabaseContext`, and `-lsqlite3`, exposes core through `api`, has no app direct edge/iOS export, and checker policy is core-owned. Reviewer re-review PASS found no Critical/Important/Minor findings.
- [x] 4.3 Task 3.2 accepted: only the package-stable `currentTimeMillis()` / `uuid4()` expect/actual family moved to narrow `:core:platform` in `07da78e`, serving Library scanning + Playlist backup and Library scanning + Playback respectively. `:shared` uses `api(projects.core.platform)`; no iOS export or core production dependency exists, and feature-specific platform seams remain out. Independent reviewer PASS/APPROVED with no Critical/Important/Minor findings.
- [ ] 4.4 Run affected Android packaging, desktop runtime, iOS linking/tests, architecture/quality gates, and `./init.sh`; record migration and platform evidence.

Checkpoint: 4.1, 4.2, and 4.3 are accepted. 4.4 remains unchecked for later Android packaging, desktop runtime, iOS linking/runtime, and full-init evidence; `./init.sh` was intentionally not rerun after the prior user-directed stop beyond 9000 seconds.

## 5. Slice 4 - Feature APIs And Playback Contracts

- [x] 5.1 Add RED API boundary/value tests before settings registration, expecting `:feature:library:api`/`:feature:playlists:api` absent-project failure; cover Library's complete 13-method repository/model contract and Playlist's complete 11-method contract with `PlaylistSummary` rather than generated persistence `Playlist`. Add RED architecture controls for API -> database/shared/implementation, implementation -> shared/other implementation, and the currently wrong Playlist API -> core:model edge; add RED shared DI tests for the internal transitional factories/composition while preserving override behavior.
- [x] 5.2 Introduce `:feature:library:api` with only `:core:model` as its production project dependency and `:feature:playlists:api` with no production project dependencies. Keep both APIs package-stable and explicit/KDoc-complete; keep Playlist persistence isolated behind `PlaylistSummary`. No physical implementation modules are created: repositories, mappers, scanner, backup, UI state, and Koin remain shared, where `libraryImplementationModule()` and `playlistsImplementationModule()` are composed only by public `rhythHausModule()`.
- [x] 5.3 Extract real playback engine/contracts into `:core:playback` with characterization coverage; do not create core navigation unless common destination-scoped Back contracts are required. Evidence: Task 4.2 acceptance report records approved ownership, architecture policy, focused/cross-target verification, and independent re-review with no findings.
- [x] 5.4 Verify Back precedence, predictive target latching, foremost feature dismissal, playlist destination invalidation, DI, architecture/quality checks, and supported-platform focused tests. Evidence: retained shared Back/DI selectors plus Task 4.2 architecture, quality, consumer-build, Xcode, and iOS simulator evidence passed; `./init.sh` and runtime/device validation remain deferred under OpenSpec 4.4.

## 6. Slice 5 - Leaf Feature Implementations

- [ ] 6.1 Characterize and migrate Now Playing into one feature implementation module with local state and unchanged Back/playback behavior; run focused and architecture/quality verification.
- [ ] 6.2 Characterize and migrate playlists plus backup/documents into its implementation module; verify database FKs, resources, Back/edit/modal behavior, Android/desktop/iOS document paths, and architecture/quality checks.
- [ ] 6.3 Characterize and migrate Search into one feature implementation module without creating an API split or empty state types; run focused and architecture/quality verification.
- [ ] 6.4 Characterize and migrate Settings into one feature implementation module without changing settings behavior; run focused and architecture/quality verification.

## 7. Slice 6 - Library Last

- [ ] 7.1 Add RED characterization coverage separating `:shared` root shell responsibilities from Library scanner/source/index/repository/UI/transient ownership, including existing scanner and playback paths.
- [ ] 7.2 Migrate Library implementation last, preserving package names, local feature state, resources, and feature API boundaries while keeping root route/Back arbitration and lifecycle in shared.
- [ ] 7.3 Verify scanner/source access, playback integration, destination-scoped Back regressions, SQLDelight integration, DI, Android packaging, desktop runtime, iOS startup/linking, architecture/quality gates, and `./init.sh`.

## 8. Slice 7 - Thin Shared And Completion

- [ ] 8.1 Add a thin-shared inventory test/documentation assertion proving shared owns only `App()`, root shell, cross-feature route/Back arbitration, lifecycle, Koin assembly, and stable `MainViewController` facade responsibilities.
- [ ] 8.2 Remove migrated implementation ownership from shared without adding bridge dependencies; make inventory/graph tests GREEN and verify explicit public APIs and KDoc requirements.
- [ ] 8.3 After successful feature migrations, add a scaffold that generates only real requested module structure; defer package renames and Dependency Analysis Gradle Plugin evaluation to separately approved work.
- [ ] 8.4 Run final `qualityCheck`, `./init.sh`, strict OpenSpec validation, and `git diff --check`; align progress, roadmap, ADRs, feature READMEs, and OpenSpec task evidence, then commit each independently reviewable completed slice conventionally.
