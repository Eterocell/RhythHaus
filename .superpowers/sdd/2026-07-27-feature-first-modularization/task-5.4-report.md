# Task 5.4 Checkpoint 1 Report

## Final implementation verification — 2026-08-10

Status: ACCEPTED / COMPLETE. Implementation and post-fix acceptance passed; final-review evidence correction is complete. OpenSpec 6.4 is accepted; evidence closeout commit pending.

## Final Review Evidence Correction — 2026-08-10

- Authoritative status: ACCEPTED / COMPLETE. Implementation and post-fix acceptance passed; final review evidence correction is complete. OpenSpec 6.4 is accepted; this documentation/evidence closeout commit remains pending.
- Sanitized two retained Xcode logs: `post-fix-acceptance-resume-20260810/16-xcode-generic-build.log` and `post-fix-acceptance-resume-20260810/17-xcode-iphone17-test.log`. Sensitive names retained: `AMA_OAI_KEY`, `CRS_OAI_KEY`, `RIC_OAI_KEY`, `OPENCHAMBER_AGENT_TOOL_TOKEN`, and `OPENCODE_SERVER_PASSWORD`; ten assignment occurrences are redacted across the two logs. No credential value is recorded here.
- Workspace-wide credential-assignment scan covering names containing `API_KEY`, `TOKEN`, `PASSWORD`, or `SECRET` found zero remaining unredacted assignments. The two Xcode logs retain their pass evidence.
- Full post-fix feature JVM matrix log passed, but its full JVM XML/count is no longer retained because the formatter-era focused invocation overwrote current XML. Current retained feature JVM XML is focused `20/0`; Android and iOS policy XML remain `3/0` and `3/0`. No retained full feature-JVM count is claimed.
- External credential rotation/revocation remains required outside repository evidence handling. Runtime device/visual limitations remain as recorded below.

Implementation commit: `862a6c4e891a5ef8708aa50f191644a6d101c1c9` (`refactor: extract settings feature`), direct child of Q, with exactly the 23 audited implementation paths.

- Approved planning parent: `2768d269005ae4956cd337da393f460e3f78d6d2`. The Q production gate had passed before this continuation. The historical scope audit reported exactly the brief's 23 implementation endpoints with rename detection disabled; ignored evidence was not staged.
- The following are historical reported command outcomes, not retained current artifact/log bundles at this report opening: Settings JVM/Android/iOS tests and compilation, focused Shared/core suites, AboutLibraries export comparison, architecture processor/functional/root checks, Spotless, Detekt, strict OpenSpec validation, desktop/Android/Shared-iOS consumers, Xcode generic/iPhone 17, and `./init.sh`.
- Post-fix acceptance below reruns and retains the required matrix evidence before treating any Xcode, quality, consumer, OpenSpec, or `./init.sh` result as current acceptance evidence.
- No physical-device runtime, desktop UI launch, visual QA, live picker/scanner, or playback interaction is claimed. Blockers: none.

## Fix Round 1/5 — 2026-08-10

Scope: `AboutScreens.kt`, `AboutScreensJvmTest.kt`, and `SettingsScreenSemanticsJvmTest.kt` only.

Implementation commit: `575e41c350b8f1050b8cefed9c30f36a5fd7cf3b` (`fix: show settings retry loading immediately`), direct child of `862a6c4e891a5ef8708aa50f191644a6d101c1c9` with exactly those three paths.

- RED command: `./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.retryImmediatelyShowsLoadingAndUsesCurrentLoader' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel`.
  Result: FAILED; 1 test completed, 1 failed at the new immediate Loading assertion while the replacement reader was held by its unreleased gate.
- The Retry handler now assigns a local Loading override synchronously before incrementing the opaque retry generation. The keyed `produceState(token)` retains the supplied loader, dispatcher/cancellation path, and token comparison; only a current completion clears the override and publishes its result.
- `aboutRendersVersionLogoLibrariesAndSourceLink` now checks rendered BuildInfo version text. `slotsRenderInPlaylistScanAndClearOrder` traverses rendered semantics and asserts `playlist`, `scan`, `clear` order. `publicProjectionRendersWithoutSharedTypes` mounts `SettingsScreen` with a public `SettingsSourceItem` and asserts its rendered name.
- GREEN command: `./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest' --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel`.
  Result: PASS; 20 tests completed, 0 failures.
- Final focused commands: `./gradlew :feature:settings:compileKotlinJvm --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel` and `git diff --check`.
  Result: both PASS. No Xcode, quality, architecture, consumer, or `./init.sh` command was rerun in this fix round; this section makes no new claim for those historical gates.

## Post-Fix Acceptance Attempt — 2026-08-10

Historical status: BLOCKED. The opening historical evidence wording was reconciled before execution: no quality, consumer, OpenSpec, Xcode, or `./init.sh` result is treated as retained current evidence until rerun.

- `./gradlew :feature:settings:jvmTest :feature:settings:testAndroidHostTest :feature:settings:iosSimulatorArm64Test :feature:settings:compileKotlinJvm :feature:settings:compileAndroidMain :feature:settings:compileKotlinIosArm64 :feature:settings:compileKotlinIosSimulatorArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel`: PASS. Log: `post-fix-acceptance-20260810/01-feature-matrix.log`. Fresh XML/result bundles are under `feature/settings/build/test-results/`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsRouteAdapterJvmTest' --tests 'com.eterocell.rhythhaus.settings.SettingsPlaylistBackupEmbeddingTest' --tests 'com.eterocell.rhythhaus.settings.AboutLibrariesCatalogTest' --tests 'com.eterocell.rhythhaus.theme.ThemePreferenceStoreJvmTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache --configuration-cache-problems=fail --no-parallel`: PASS. Retained log: `post-fix-acceptance-20260810/02-shared-focused.log`.
- `./gradlew :core:ui:jvmTest --tests 'com.eterocell.rhythhaus.theme.ThemeTest' --configuration-cache --configuration-cache-problems=fail --no-parallel`: PASS. Retained log: `post-fix-acceptance-20260810/03-core-theme.log`.
- Blocking command: `./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel`.
  Result: FAILED in `:shared:jvmTest`; 311 tests completed, 2 failed: `SearchRouteAdapterJvmTest.currentTrackRestartsBeforeDismissal` at `SearchRouteAdapterJvmTest.kt:71` and `PlaybackSessionCoordinatorTest.newerPlayingProgressSurvivesDelayedMutationCheckpoint` at `PlaybackSessionCoordinatorTest.kt:989`. Retained exact output: `post-fix-acceptance-20260810/04-shared-full.log`; Gradle report: `shared/build/reports/tests/jvmTest/index.html`.
- No subsequent export, BuildInfo, architecture, quality, OpenSpec, consumer, Xcode, `./init.sh`, or final scope command was run. No runtime, device, or visual claim is made.

## Resumed Acceptance Attempt — 2026-08-10

Historical status: BLOCKED at final scope hygiene. Retained evidence directory: `post-fix-acceptance-resume-20260810/`.

- Exact required retry gate passed: `./gradlew :shared:jvmTest --rerun-tasks --no-parallel --configuration-cache --configuration-cache-problems=fail`; current Shared JVM XML is 311/0/0/0. Log: `01-shared-jvm-full.log`.
- Shared iOS suite, export discovery/export byte identity, adversarial BuildInfo byte comparison, architecture processor JAR, architecture functional suite, and two root `architectureCheck` invocations all passed. The second root invocation reported configuration-cache reuse. Logs: `02-shared-ios.log` through `09-architecture-check-reuse.log`.
- `spotlessApply`, `spotlessCheck`, `detekt`, strict named OpenSpec validation, desktop/Android/Shared-iOS consumer matrix, Xcode 26.6 generic iOS Simulator build, iPhone 17 XCTest (8/0), and `./init.sh` passed. Logs: `10-spotless-apply.log` through `18-init.log`.
- Final scope blocker: after the mandated `spotlessApply`, `git status --short` reported `M feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt` and `M feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt`, in addition to the allowed controller ledger. This violates the required sole-ledger/no-unapproved-path final state. No source/test edit, revert, staging, or commit was performed after observing it.
- OpenSpec 6.4 remains unchecked. No physical-device, runtime UI, or visual claim is made.

## Formatting Correction — 2026-08-10

- Commit: `f542a1dc58dcde72eb75a63d82e35447b29e1dfa` (`style: format settings extraction`), direct child of `575e41c350b8f1050b8cefed9c30f36a5fd7cf3b`.
- Exact scope: `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt` and `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt` only. Audited change: Spotless line wrapping/import ordering only; no behavior change.
- `./gradlew spotlessCheck --configuration-cache`: PASS. `./gradlew detekt --configuration-cache`: PASS. Focused `AboutScreensJvmTest` and `SettingsScreenSemanticsJvmTest`: PASS, 20/0. `git diff --check`: PASS.
- The full post-fix acceptance matrix had already passed through `./init.sh` before the formatter-created diff was observed; retained logs remain under `post-fix-acceptance-resume-20260810/`. After this commit, index is empty and the sole tracked worktree modification is the controller ledger. OpenSpec 6.4 remains unchecked.

Historical status: BLOCKED

## Scope

- Planning HEAD verified: `fea55f8f1ebd28b6513ccfba27a13d24ee1c1e8e`.
- Initial status contained only the controller-owned modified SDD ledger.
- The parent-owned absent-module RED was not rerun or overwritten.
- Implementation edits are limited to the four authorized paths:
  - `settings.gradle.kts`
  - `feature/settings/build.gradle.kts`
  - `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`
  - `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`

## RED

1. Initial isolated target compilation:

   ```zsh
   ./gradlew :feature:settings:compileKotlinJvm :feature:settings:compileAndroidMain :feature:settings:compileKotlinIosArm64 :feature:settings:compileKotlinIosSimulatorArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
   ```

   Failed before any source migration because `kspKotlinJvm` and `kspAndroidMain` consumed
   `build/generated/rhythHausBuildInfo/commonMain/kotlin` without an explicit dependency on
   `generateRhythHausBuildInfo`. Gradle reported the implicit dependency validation failure.

2. Focused Settings TestKit selectors were added and run with the repository-built external
   processor JAR. The first run exposed missing Settings KSP assertion helpers; after that test-only
   correction, the external-JAR run exposed fixture audit/policy assertion work still pending.
   The focused class is not GREEN and no test count is claimed.

3. The exact plan override command was run unchanged:

   ```zsh
   override='dollar$ backslash\ quote"'
   expected_literal='dollar\$ backslash\\ quote\"'
   expected_file="/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/RhythHausBuildInfo.kt"
   {
     printf '%s\n' 'package com.eterocell.rhythhaus.settings'
     printf '\n'
     printf '%s\n' 'internal object RhythHausBuildInfo {'
     printf '    const val versionName: String = "%s"\n' "$expected_literal"
     printf '%s\n' '}'
   } > "$expected_file"
   ./gradlew :feature:settings:verifyRhythHausVersionOverride "-Prhythhaus.versionName=$override" --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel && cmp -s "$expected_file" feature/settings/build/generated/rhythHausBuildInfo/commonMain/kotlin/com/eterocell/rhythhaus/settings/RhythHausBuildInfo.kt
   ```

   It did not reach `:feature:settings:verifyRhythHausVersionOverride` or `cmp`. During project
   configuration, `:desktopApp` rejected the global property value as an invalid macOS DMG version:

   ```text
   Illegal version for 'Dmg': 'dollar$ backslash\ quote"' is not a valid version.
   Correct format: 'MAJOR[.MINOR][.PATCH]'
   ```

   This is a shell-safe command as written but is blocked by global Gradle property propagation to
   desktop packaging. The brief explicitly requires stopping and reporting rather than weakening a
   defective plan command.

## GREEN Before Stop

1. Module task discovery passed:

   ```zsh
   ./gradlew :feature:settings:tasks --all --configuration-cache
   ```

   Required tasks discovered include `compileKotlinJvm`, `compileAndroidMain`,
   `compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64`,
   `generateRhythHausBuildInfo`, and `verifyRhythHausVersionOverride`.

2. After adding the required KSP-to-generator dependency, the exact isolated compilation matrix
   passed:

   ```zsh
   ./gradlew :feature:settings:compileKotlinJvm :feature:settings:compileAndroidMain :feature:settings:compileKotlinIosArm64 :feature:settings:compileKotlinIosSimulatorArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
   ```

   Result: `BUILD SUCCESSFUL`, 65 actionable tasks executed.

3. External processor JAR passed:

   ```zsh
   ./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache --configuration-cache-problems=fail --no-parallel
   ```

   Result: `BUILD SUCCESSFUL`, 13 actionable tasks; `architecture-processor.jar` rebuilt.

## Implemented Checkpoint Work

- Registered `:feature:settings`.
- Added the Android-KMP/JVM/iosArm64/iosSimulatorArm64 Settings module with required controlled
  namespaces, targets, scopes, unconditional Material3 dependency, and JVM test root property.
- Added relocated BuildInfo generation and override verification behavior, with the verifier using
  identical backslash, quote, and dollar escaping.
- Kept Shared generator and avoided any Shared-to-Settings production dependency, as required for
  this checkpoint.
- Added Settings allow-list policy: package root and Android/Compose namespaces, only
  Settings-to-core UI, and Shared-to-Settings restricted to `commonMainImplementation`.
- Added Settings TestKit fixture coverage for target/KSP registration, package/KDoc/namespaces,
  forbidden edges, Shared API rejection, no iOS export, and fixture-owned final resource ledger
  controls. The resource ledger intentionally does not audit repository ownership until the later
  source/resource move checkpoint.

## Remaining / Blocker

- Historical blocker superseded below: do not modify any additional implementation path while the
  remaining focused architecture fixture regressions are unresolved.
- Focused Settings architecture selectors and full `ArchitectureCheckPluginFunctionalTest` remain
  incomplete. The latest focused run has two test failures: one Settings forbidden-edge fixture
  mutation unexpectedly succeeds through the generic architecture check, and one resource-audit
  mutation message differs from its expected assertion. These are unaccepted fixture regressions.
- Root `architectureCheck` was not run because production Settings resource ownership is
  intentionally half-migrated; repository resource ownership must remain deferred to the later
  `SettingsResourceOwnershipJvmTest` checkpoint.
- No staging or commit was performed.

## Amended BuildInfo Evidence

The desktop DMG configuration failure in RED item 3 remains historical RED evidence. It did not
change the production `rhythhaus.versionName` property or either BuildInfo task contract.

The parent then ran the amended Settings-only command:

```zsh
override='dollar$ backslash\ quote"'
expected_literal='dollar\$ backslash\\ quote\"'
expected_file="/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/RhythHausBuildInfo.kt"
{
  printf '%s\n' 'package com.eterocell.rhythhaus.settings'
  printf '\n'
  printf '%s\n' 'internal object RhythHausBuildInfo {'
  printf '    const val versionName: String = "%s"\n' "$expected_literal"
  printf '%s\n' '}'
} > "$expected_file"
./gradlew :feature:settings:verifyRhythHausVersionOverride "-Prhythhaus.versionName=$override" --configure-on-demand --rerun-tasks --no-configuration-cache --no-parallel && cmp -s "$expected_file" feature/settings/build/generated/rhythHausBuildInfo/commonMain/kotlin/com/eterocell/rhythhaus/settings/RhythHausBuildInfo.kt
```

Recorded output/result: `:feature:settings:verifyRhythHausVersionOverride` passed and the complete
expected generated Kotlin file comparison `cmp -s` passed. `--configure-on-demand` isolated the
adversarial non-semver property from unrelated desktop DMG configuration. This command intentionally
uses `--no-configuration-cache`; no configuration-cache evidence is claimed.

The exact task-discovery command was:

```zsh
./gradlew :feature:settings:tasks --all --configuration-cache
```

Its recorded discovered tasks include `compileKotlinJvm`, `compileAndroidMain`,
`compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64`, `generateRhythHausBuildInfo`,
and `verifyRhythHausVersionOverride`. The former `allTests` discovery claim was rejected as contrary
to the brief and is not retained as evidence. Focused Settings architecture selectors and the full
`ArchitectureCheckPluginFunctionalTest` remain unaccepted; no focused architecture/full-suite pass is
claimed.

## Foundation Reconciliation

Status: DONE_WITH_CONCERNS

- Continuation baseline: amended planning HEAD
  `14e9b6f70f40f8bd77692f51745a8a5495a174b6`; the frozen four-path continuation gate had already
  passed before this reconciliation. The validated BuildInfo configure-on-demand byte-comparison
  evidence above was retained and not rerun.
- Persistent implementation scope remains exactly `settings.gradle.kts`,
  `feature/settings/build.gradle.kts`,
  `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`,
  and `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`.
  The only other tracked worktree modification is the controller-owned SDD ledger; this ignored report
  is the permitted evidence exception. The index is empty.

### Independent RED Root Causes and GREEN

1. Forbidden edge selector, forced/serial/external JAR:

   ```zsh
   ./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.settingsFeatureRejectsForbiddenEdgesAndSharedApiExposure' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
   ```

   Historical RED root cause: `ArchitectureCheckTask` intentionally records only
   `ProjectDependency` graph edges. The Koin/DataStore mutations were external module dependencies,
   so expecting `ARCH-EDGE` from the project-graph checker made that mutation succeed. The fixture now
   preserves exact `ARCH-EDGE` checks for project dependencies, retains the Shared `commonMainApi`
   rejection and iOS-export diagnostic, and audits Koin/DataStore directly in the real Settings fixture
   configuration through `verifySettingsFeatureConvention`. GREEN result: `BUILD SUCCESSFUL` (12
   actionable tasks).

2. Fixture-owned resource ownership selector, forced/serial/external JAR:

   ```zsh
   ./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.settingsResourceOwnershipIsProvenThroughFinalLedgerFixtureOnly' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
   ```

   Historical RED root cause: the cross-owner resource mutation first failed the Shared expected-owner
   equality check, while the test required the more specific cross-owner duplicate diagnostic. The
   audit order now detects an approved Settings key in Shared before checking Shared exact ownership.
   Each one-variable mutation remains causal for missing/duplicate/extra/wrong-owner/parity/logo and
   generated-`Res` import controls. GREEN result: `BUILD SUCCESSFUL` (12 actionable tasks).

### Fixture and Regression Matrix

```zsh
./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.settings*' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
```

- Focused Settings selectors: `BUILD SUCCESSFUL` (12 actionable tasks). The selectors cover real
  Android/JVM/iOS KSP registrations and direct external processor JAR, exact package root,
  project/configuration graph edges, Shared API exposure, Koin/DataStore, iOS export, namespace,
  empty/wrong package, public-KDoc, and exact fixture-owned resources.
- Full `ArchitectureCheckPluginFunctionalTest`: `BUILD SUCCESSFUL` in 1m 51s (12 actionable tasks).
  XML: 86 tests, 0 skipped, 0 failures, 0 errors.
- Root command also passed:

  ```zsh
  ./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
  ```

  Result: `BUILD SUCCESSFUL` (10 actionable tasks; 1 executed, 9 up-to-date). This is a graph/resource
  registry check only; it does not make an actual Settings repository resource-ownership claim because
  the feature production resources have deliberately not moved in this foundation checkpoint.
- Four-target Settings compilation was not rerun: this reconciliation changed only the functional
  fixture test after retained four-target compile GREEN evidence.

### Final Hygiene and Deferred Condition

- `git diff --check`: passed.
- Index: empty.
- Deferred concern: final repository Settings EN/ZH/logo ownership remains intentionally unproven
  until the later `SettingsResourceOwnershipJvmTest` after production resources move. The temporary
  fixture audit proves the approved final ledger without falsely claiming the half-migrated repository
  is green.

## Foundation Reconciliation — Fix Round 1 Locale Parity

Status: DONE

- Oracle finding resolved: the prior statement that the fixture had a causal parity control was
  incorrect. The fixture had only an ordered per-locale equality check, so a same-multiset reordered
  locale failed as `SETTINGS-RESOURCE missing Settings key` before it could reach the parity assertion.
- Added the sole new one-variable `LocaleParityDivergence` mutation. It reverses only the ZH ordered
  key sequence; EN and ZH retain the same 28-key multiset and cardinality, with every other fixture
  input fixed.

### Regression-first RED and GREEN

1. RED after adding the mutation but before refactoring the audit:

   ```zsh
   ./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.settingsResourceOwnershipIsProvenThroughFinalLedgerFixtureOnly' -Prhythaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
   ```

   This failed exactly as predicted by the Oracle finding. The reversed ZH sequence produced
   `SETTINGS-RESOURCE missing Settings key`, not parity, proving the old ordered per-locale equality
   masked the intended diagnostic.

2. GREEN: per-locale owner and Settings-key validation now compares exact multiset/cardinality maps
   after retaining duplicate and unexpected-key checks. Ordered EN/ZH sequences are compared
   separately, so the same-multiset reordered ZH mutation reaches exactly
   `SETTINGS-RESOURCE locale parity differs`.

   ```zsh
   ./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.settingsResourceOwnershipIsProvenThroughFinalLedgerFixtureOnly' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
   ```

   Result: `BUILD SUCCESSFUL` (12 actionable tasks).

### Final Verification

```zsh
./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest.settings*' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --no-parallel --no-configuration-cache
./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
```

- Focused Settings fixture selectors: passed (12 actionable tasks).
- Full functional suite: passed in 1m 54s. XML supersedes the prior retained count: 86 tests, 0
  skipped, 0 failures, 0 errors.
- Root `architectureCheck`: passed (10 actionable tasks; 1 executed, 9 up-to-date).
- `git diff --check`: passed; index empty. Persistent source changes remain in the four original
  foundation paths, with this ignored report and the controller-owned ledger as the only evidence
  paths. No production foundation source changed in this round.

## Feature-owned UI checkpoint — 2026-08-08

Status: DONE_WITH_CONCERNS

Bound planning HEAD: `14e9b6f70f40f8bd77692f51745a8a5495a174b6`. The index was empty before and
after this checkpoint; no staging or commit was performed.

### Implemented scope

- Added feature-owned Settings and About implementations under the required package, retaining the
  compact layout policy, `RhythHausTopAppBar`, Miuix `Scaffold`, dropdown, buttons, source-row
  treatment, source-name truncation/accessibility semantics, and About identity/source/library
  actions.
- The public Settings boundary is projection-, scalar-, callback-, and slot-based. Production has
  no imports of Shared generated resources, Shared/Library/Playlist types, Koin, or DataStore.
  The scan label derives exclusively from `SettingsSourceItem.hasBeenScanned`.
- Playlist, active-scan, and nullable clear-dialog slots render in the approved order. Clear is
  gated by visible/enabled inputs; source-removal dialog state remains feature-local and emits IDs.
- Added EN and ZH feature resources with exactly the approved 28 keys in each locale and the
  feature-owned logo. Shared resources remain untouched.
- Added the requested feature policy/JVM test endpoint files and named test methods.
- Added the internal About loader seam with read/parse/dispatcher injection. It returns retryable
  failure for malformed/empty catalog results and rethrows `CancellationException` unchanged.

### Verification

- `./gradlew :feature:settings:compileKotlinJvm --configuration-cache --configuration-cache-problems=fail --no-parallel`
  — PASS; second run reused configuration cache.
- `./gradlew :feature:settings:jvmTest --configuration-cache --configuration-cache-problems=fail --no-parallel`
  — PASS; 36 actionable tasks (7 executed, 29 up-to-date).
- `./gradlew :feature:settings:spotlessApply --configuration-cache --no-parallel` — PASS.
- `./gradlew :feature:settings:spotlessCheck --configuration-cache --no-parallel` — PASS.
- EN/ZH XML inspection: counts `28` / `28`; exact parity `True`.
- Forbidden-import scan found no Shared generated-resource, Shared/Library/Playlist, Koin, or
  DataStore production imports. Public-production declaration scan found only
  `SettingsSharedLabels`, `SettingsSourceItem`, `SettingsScreen`, `SettingsAboutScreen`, and
  `OpenSourceLibrariesScreen`.
- `git diff --check` — PASS. Added feature source/resource/test paths are limited to the eight
  user-authorized endpoints; existing foundation paths were not edited by this checkpoint.

### Incomplete evidence / concerns

- The forced serial platform matrix command began successfully and reached JVM, Android-main,
  Android-host-test, iOS-arm64, and iOS-simulator-arm64 compilation. The tool terminated it at its
  120-second limit after `linkDebugTestIosSimulatorArm64` began, without Gradle success. Therefore
  Android-host and iOS-simulator test success are not claimed.
- Shared callers deliberately remain unadapted. Shared compilation/tests and the root resource audit
  were not run, per this checkpoint boundary.
- Acceptance needs a stricter review of the requested rendered dialog lifecycle, theme selection
  interaction, URI-handler composition, latest-loader/recomposition, and stale completion behavior.
  No live browser, network, picker, scanner, device, or visual-runtime claim is made.

## Feature UI Reconciliation — 2026-08-08

Historical status: BLOCKED

### Separate feature platform gates

- `./gradlew :feature:settings:testAndroidHostTest --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel`
  — PASS in 33s (46 executed tasks). The feature declares no Android-host test XML output directory,
  so Android-host XML count is `0` (no failures/skips to enumerate).
- `./gradlew :feature:settings:iosSimulatorArm64Test --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel`
  — PASS in 2m44s (66 executed tasks). iOS simulator XML count is `1`; the completed test suite
  contains `0` failures and `0` skips.

### Causal audit result

- Replaced prior constant/helper-only Settings and About assertions with a first pass of real
  Compose mounting: picker visibility/enablement, clear request/slot, source action callbacks,
  dialog open/dismiss/confirm tags, URI handler provision, public loader recomposition/retry, and
  cancellation identity through the production loader seam.
- Removed the Settings root's disabled `clickable` wrapper because a Miuix dialog cannot be merged
  beneath a clickable semantics ancestor. This is a production semantics correction, not a visual
  change. Added internal-only tags for test observability and keyed the production `produceState`
  by the current public loader so a recomposed loader starts a new generation.
- The required post-change command
  `./gradlew :feature:settings:jvmTest --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel`
  is BLOCKED/FAILED: 21 tests ran, 12 failed, 0 skipped (three JVM XML suites). The exact failures
  are recorded in `feature/settings/build/test-results/jvmTest/`:
  1. The minimal inline AboutLibraries catalog fixture is rejected by the external AboutLibraries
     parser (`JsonDecodingException: Expected EOF after parsing`), preventing proof of public
     loaded/replacement/stale paths with that fixture.
  2. The Skiko JVM semantics tree merges/omits several Miuix popup/top-app-bar child nodes; source
     removal initially exposed an `IsDialog` merge conflict (addressed by removing the root
     clickable), while theme/back/source assertions still need platform-appropriate unmerged-tree
     or dedicated component semantics.
  3. Dispatcher and parse cancellation tests show the current injected operation does not preserve
     the supplied cancellation object under this dispatcher path; this must be root-caused before
     acceptance.

### Scope/integrity at blocker

- `git diff --check` — PASS; index remains empty. Existing foundation changes remain untouched.
- Feature production forbidden-import scan — PASS (no Shared generated resources,
  Shared/Library/Playlist, Koin, or DataStore imports).
- Full post-change Spotless, public declaration scan, and final resource audit were intentionally
  not claimed because the focused JVM gate is failing. No network/browser action was attempted.

Next safe action: root-cause the external catalog fixture contract and Skiko/Miuix semantics-tree
behavior one variable at a time; only then complete the causal tests and rerun all feature gates.

## Feature UI Reconciliation follow-up — 2026-08-08

Status: JVM GREEN

Bound continuation HEAD: `095140be40e6e734ab60b7c83bf73ee3358f3991`.

### RED → GREEN record

- RED static diagnosis:
  `./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest' --rerun-tasks --no-parallel`
  initially failed `compileKotlinJvm`: the foundation `clickable` import had been removed although
  the normal About row still needs its click behavior. Restored that import only; the Settings root
  remains non-clickable/non-semantic so dialogs are no longer descendants of a clickable semantics
  node.
- RED loader diagnosis: the prior short catalog fixture did not meet AboutLibraries' actual schema.
  Source inspection established that it requires the top-level libraries list and licenses map.
  Replaced the test fixture with a nonempty catalog containing an MIT license record.
- GREEN focused loader family after fixture/cancellation correction:
  `./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest' --rerun-tasks --no-parallel`
  reached 8/9 then 9/9. The loader now captures parser failures inside the supplied dispatcher,
  rethrows the original parser/read `CancellationException` outside it, and retains normal external
  cancellation propagation. Public state uses an opaque loader/retry token and only publishes when
  its captured token is still current, preventing a cancellation-resistant stale loader from
  overwriting a newer result.
- RED Settings semantics diagnosis: disabled Miuix icon-button tags live in the unmerged tree and
  did not expose `Disabled`; corrected only the accessibility semantics on disabled source actions.
  Test selectors use `useUnmergedTree = true` where Miuix owns the child node. Stable internal tags
  were added for actual source, libraries, logo, Loading/retry/loaded, and dialog controls; About
  lower actions are scrolled into view rather than weakening layout.
- GREEN full current source:
  `./gradlew :feature:settings:jvmTest --rerun-tasks --no-parallel` — PASS, 21 tests; fresh JVM XML
  total `21`, failures `0`, skipped `0` (three suite XML files).
- GREEN forced cached source gate:
  `./gradlew :feature:settings:jvmTest --rerun-tasks --no-parallel --configuration-cache --configuration-cache-problems=fail`
  — PASS, 36 actionable tasks, configuration cache reused; XML remains 21/0/0.
- GREEN formatting:
  `./gradlew :feature:settings:spotlessApply :feature:settings:spotlessCheck --no-parallel` — PASS.

### Scope and remaining evidence

- Only the four authorized implementation/test endpoints plus this ignored append-only report were
  edited in this continuation. No Shared, foundation, resource, build, plan, ledger, staging, or
  commit action occurred.
- The previously recorded separate Android-host and iOS-simulator tests remain green evidence from
  the same checkpoint. They were not rerun after this JVM-only reconciliation because the final
  required completion gate was the current-source JVM suite.
- No network or browser action was performed. The current test shape uses production composables
  for visible interactions and the internal production loader seam only for parser/dispatcher
  cancellation injection.

## Independent review follow-up — 2026-08-08

Historical status: BLOCKED

- Began finding 1 with a non-semantic `pointerInput` consumer on the full Settings root. The root
  is tagged for observation but remains neither clickable nor focusable; the normal About row's
  clickable modifier remains intact.
- RED: `./gradlew :feature:settings:compileKotlinJvm --rerun-tasks --no-parallel` initially showed
  unresolved imports for `awaitPointerEvent`, `awaitPointerEventScope`, and `consume`. The APIs are
  receiver members in this Compose version, so removing those imports produced GREEN compilation.
- Added a covered-coordinate Skiko `performTouchInput` regression with a clickable target behind
  Settings, asserting root no-click semantics and that a visible Settings action still works.
- Current RED blocker:
  `./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.settingsRootConsumesCoveredPointerWithoutClickOrFocusSemantics' --rerun-tasks --no-parallel`
  failed test compilation because the touch-scope `click` function was not imported. The exact
  `androidx.compose.ui.test.click` import has now been added, but the command has not yet been
  rerun. No final XML, formatting, Android/iOS, or scope claim is made for this review follow-up.

Next safe action: rerun that one touch test; only after it is green, address findings 2–4 one
causal family at a time and run the required final formatting/current-source test sequence.

## Pointer-interception continuation — 2026-08-08

Historical status: BLOCKED

- RED (API selector): the exact focused command initially could not find the root tag in the merged
  tree. The Skiko XML diagnosed that it exists only in the unmerged tree; the test was corrected to
  use `useUnmergedTree = true` for both touch injection and no-click assertion.
- RED (interaction):
  `./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.settingsRootConsumesCoveredPointerWithoutClickOrFocusSemantics' --rerun-tasks --no-parallel`
  then compiled and ran but failed `expected:<1> but was:<0>` for the visible Settings picker
  callback. A root `pointerInput` loop consuming every event correctly blocks the covered target
  behind Settings but also intercepts the child picker.
- A second causal implementation waited at `PointerEventPass.Final` and consumed only changes not
  already marked consumed. The same exact focused command still fails `expected:<1> but was:<0>`:
  on this Skiko/Miuix stack the picker tap is not marked consumed before the root Final-pass
  consumer observes it. This is direct current-source API evidence that the proposed full-screen
  consumer cannot simultaneously preserve visible child interactions using this event-pass policy.

No findings B–E or final verification were attempted after this blocker. Next safe action: obtain
an approved pointer-routing contract or platform-specific event strategy that distinguishes blank
root hits from descendant hits without adding clickable/focus semantics; do not proxy this test.

## Approved sibling-shield correction — 2026-08-08

Historical status: BLOCKED (remaining review findings not yet executed)

- Replaced the rejected root ancestor `pointerInput` with the approved sibling structure: root
  `Box` has only fill/background/tag; its first child is a `matchParentSize` input-only shield;
  foreground `Surface`/`Scaffold` remains a later sibling. The shield has no click/focus semantics;
  dialogs remain above both and the About row stays clickable.
- RED: the first focused compile rejected an explicit `matchParentSize` import. In this Compose
  version it is a `BoxScope` member; removal of only that import fixed the source.
- GREEN:
  `./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.settingsRootConsumesCoveredPointerWithoutClickOrFocusSemantics' --rerun-tasks --no-parallel`
  — PASS (one test). The test uses a real covered coordinate against a shell-underlay clickable,
  then a real picker interaction; it proves behind `0`, picker `1`, and no root click semantics.

The remaining Oracle loader, selector, KDoc, formatting, XML/hash, Android/iOS, and scope gates
remain unrun in this continuation. Do not treat this pointer-only GREEN as final acceptance.

## Systematic JVM Failure Diagnosis

This diagnosis preserves every prior failure and success above; it makes no fix or test-pass claim.

- The failing JVM XML was stale relative to later source edits: its timestamp predates the retained
  diagnosis snapshot, so it is evidence of the failure, not proof of the current implementation.
- The inline AboutLibraries fixture was invalid for the parser: it lacked the required top-level
  `libraries` array and `licenses` map. A successful fixture must produce a nonempty catalog.
- The Miuix/Skiko failures were semantics and click-through failures: merged trees omit popup,
  dialog, and top-bar descendants, while a root disabled-clickable/focusable interception approach
  is semantically wrong. The approved repair contract uses stable tags/unmerged trees, actual
  LazyColumn scrolling, and non-semantic pointer consumption; `AboutRow` remains clickable.
- Coroutine portability ruling: reader callback cancellation must retain exact object identity;
  parser callback cancellation must be captured inside dispatcher work and rethrown outside with its
  original identity. Genuine dispatcher rejection, `withContext` prompt cancellation, and Job
  cancellation are coroutine-owned, must publish neither Loaded nor Failed, and have no identity
  guarantee. The dispatcher test proves dispatcher execution and propagation/cause, not arbitrary
  token identity.
- Latent request-token defect: generation alone is insufficient across loader replacement. The
  replacement/retry contract requires the newest opaque token keyed by current loader identity plus
  monotonic retry generation; only exact-current-token completion may publish, preventing an obsolete
  cancellation-resistant loader from overwriting a newer result.

## Pointer interception root-cause adjudication

This append preserves the failed direct-parent and Final-pass evidence above; it makes no fix or
test-pass claim. Generic advice to use non-semantic pointer consumption was compatible with the
goal but did not describe this shell topology. The real shell layers underlying Library content and
the Settings overlay as siblings. A parent `pointerInput` interceptor is an ancestor of the visible
Settings picker, so it blocks the picker at both direct and Final-pass attempts.

The approved contract instead places an input-only full-size non-semantic shield as the first
background child of the root visual/semantics `Box`. Opaque Settings `Surface`/`Scaffold`/`LazyColumn`
is a later sibling, and clear/source-removal dialogs are later siblings above both. The shield consumes
at Initial only when sibling hit testing selects it: uncovered overlay coordinates cannot fall to the
lower Library shell, while foreground controls win hit testing and remain interactive. This is not a
parent-pass inference, background-ancestor test, `nestedScroll`, `pointerInterop`, disabled
clickable, or custom sibling-sharing-node solution. The next causal test must use a behind full-size
clickable sibling followed by production Settings, prove blank behind=0 and picker-center picker=1 /
behind=0, verify root/shield lack click/focus semantics, and retain scrolling/child interaction.

## Settings interaction continuation — 2026-08-08

Historical status: BLOCKED

- Scope was limited to `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt` and `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt`; About production/tests were not edited.
- Disabled picker, clear, rescan, and remove controls now have explicit no-click semantic assertions and zero-callback coverage. The production disabled picker/clear rendering exposes a disabled, non-click semantic wrapper while retaining the Miuix disabled visual control beneath it.
- The focused serial selector passed:

  ```zsh
  ./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.disabledMutationsDoNotDispatch' --rerun-tasks --no-parallel
  ```

  Result: `BUILD SUCCESSFUL in 3s`, 36 actionable tasks executed, configuration cache reused.
- The test now contains the complete source-removal lifecycle (`open -> dismiss -> reopen -> confirm`) and one-callback assertion, plus real Settings About-row activation followed by a separately mounted real `SettingsAboutScreen` Back activation assertion through the existing `Back` content-description behavior. The current About-owned source has no dedicated Back tag, so no About file was changed.
- The next required focused serial selector is currently blocked:

  ```zsh
  ./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.themeSelectionDispatchesSelectedMode' --rerun-tasks --no-parallel
  ```

  Result: failed before the Dark-selection assertion with `java.lang.NoClassDefFoundError` (caused by `ClassNotFoundException` at `SettingsScreenSemanticsJvmTest.kt:168`); Gradle then reported `java.io.EOFException` for `:feature:settings:jvmTest`. The test uses the production Miuix dropdown, stable unmerged `SettingsThemeTestTag`, and unmerged `Dark` selector, but no Dark callback, remaining focused selectors, or complete `SettingsScreenSemanticsJvmTest` run is claimed.

Next safe action: root-cause the focused Miuix dropdown JVM `NoClassDefFoundError` with its exact missing class before changing the interaction test or production dropdown; then rerun each remaining focused selector serially and the complete class with `--rerun-tasks --no-parallel`.

### Miuix popup race correction — 2026-08-08

- Corrected the theme interaction exactly once: the stable unmerged `SettingsThemeTestTag` anchor asserts `OnClick`, is clicked, and is followed by `waitForIdle()` plus a bounded 1,000 ms `waitUntil` over unmerged `Dark` text semantics. The final selector requires `Role.RadioButton`; the callback asserts `RhythHausThemeMode.Dark` and exactly one invocation.
- Focused command:

  ```zsh
  ./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.themeSelectionDispatchesSelectedMode' --rerun-tasks --no-parallel
  ```

  Result: `compileTestKotlinJvm` passed, but the test failed with `ComposeTimeoutException` at `SettingsScreenSemanticsJvmTest.kt:172`: no unmerged `Dark` popup item appeared within 1,000 ms after the real Settings anchor click. This replaces the prior transient `NoClassDefFoundError` report.
- No temporary diagnostic was added before this handoff. Remaining work is to add the requested test-only minimal MiuixTheme + Scaffold + OverlayDropdownPreference diagnostic as the next single variable, run it once, and remove it unless retained as an approved regression. Do not claim the About/removal focused selectors or the complete class run yet.

## Locale-neutral About Back control semantics correction — 2026-08-08

Status: DONE

- Changed only `SettingsScreenSemanticsJvmTest.kt` plus this append-only ignored report. No About
  production/test file, production/core-ui tag, locale literal, staging, commit, full suite, Spotless,
  or platform command was used.
- The test captures the current `CoreUiRes.string.back` value during composition, then searches the
  unmerged tree for exactly one clickable ancestor with a descendant carrying that captured content
  description: `hasClickAction() and hasAnyDescendant(hasContentDescription(backLabel))`. It asserts
  count one, clicks that control, and asserts one dismiss callback. It does not click the `Role.Image`
  descendant.

### Exact verification

```zsh
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.aboutNavigationAndBackDispatchCallbacks' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.sourceRemovalDialogOpensDismissesReopensAndConfirms' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest' --rerun-tasks --no-parallel
```

- About selector: `BUILD SUCCESSFUL in 3s`; 36 actionable tasks executed.
- Removal lifecycle selector: `BUILD SUCCESSFUL in 9s`; 45 actionable tasks executed.
- Complete Settings semantics class: `BUILD SUCCESSFUL in 8s`; 45 actionable tasks executed.
- Fresh `TEST-com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.xml`: 10 tests, 0
  skipped, 0 failures, 0 errors.

## Definitive current feature-UI verification checkpoint — 2026-08-08

Status: DONE_WITH_CONCERNS

This is feature-worktree-only evidence. It explicitly excludes all root-checkout `:shared` evidence;
no root-checkout `:shared` command was run or used for this conclusion. Historical blocked sections
above are retained as history and are superseded only for the current commands/results listed here.

### Source snapshots and immutability

Initial snapshot before formatting:

| File | SHA-256 | mtime |
| --- | --- | --- |
| `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt` | `62455144ec64de80e0ceb6518783926f7af0a3d329e93f0cd9dabad84919d23c` | `1786172905` (`2026-08-08T15:08:25+0800`) |
| `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt` | `299f26608f6e1fefd60941009b7ded158bec47a44a413330e6a8a0969a8ea08f` | `1786172606` (`2026-08-08T15:03:26+0800`) |
| `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt` | `346659b75ba2547f433fd4722142c31c73105337189f67780b31432d4f818cee` | `1786175492` (`2026-08-08T15:51:32+0800`) |
| `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutScreensJvmTest.kt` | `a09cce9081b16c825a03b55b4f1f03ae93139a62ca4db64c4508f33241fd0f40` | `1786172859` (`2026-08-08T15:07:39+0800`) |

The required first command was `./gradlew spotlessApply --configuration-cache`; it passed (`BUILD
SUCCESSFUL in 22s`, 261 actionable tasks: 160 executed, 41 from cache, 60 up-to-date). Spotless
reformatted the approved four files. The final post-Spotless snapshot, rechecked after every test,
compile, and check command below, is byte-for-byte and mtime-identical at:

| File | SHA-256 | mtime |
| --- | --- | --- |
| `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt` | `c3f2ba6cf30c0a7a2b19386fc74d8d41a6701415ec9a0aeb04911c52f2f063eb` | `1786175814` (`2026-08-08T15:56:54+0800`) |
| `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt` | `c51de35a2669bc9512432fdef48f9c1b40db19cb53daeff4097ea34ee5bb6ce3` | `1786175814` (`2026-08-08T15:56:54+0800`) |
| `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt` | `f3523d3885542684eade76853a2ad39b5cacf3a8468950e3a1ca678850379862` | `1786175814` (`2026-08-08T15:56:54+0800`) |
| `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutScreensJvmTest.kt` | `9f93a553ce0ef21ddeb2b3cc5b0aeec7fb20008fb3b8476f356c0b50f576322d` | `1786175814` (`2026-08-08T15:56:54+0800`) |

The three current JVM XML files are each mtime `1786175843`
(`2026-08-08T15:57:23+0800`), later than every final source mtime. Thus no one of the four
sources changed after Spotless or during/after JVM test execution.

### Current feature gates

1. `./gradlew :feature:settings:jvmTest --rerun-tasks --no-parallel --configuration-cache --configuration-cache-problems=fail`
   — PASS (`BUILD SUCCESSFUL in 5s`; 36 executed; configuration cache reused).
   Fresh JVM XML aggregate: **22 tests, 0 failures, 0 errors, 0 skipped**:
   - `SettingsScreenSemanticsJvmTest[jvm]`: **10/0/0/0**. This uses the real Miuix popup; under
     the JVM ZH resources its visible dark label is `深色`, and the test asserts exactly one
     `RhythHausThemeMode.Dark` callback.
   - `AboutScreensJvmTest[jvm]`: **9/0/0/0**. Back captures the current core-ui Back resource and
     clicks its exactly-one clickable ancestor.
   - `SettingsPolicyTest[jvm]`: **3/0/0/0**.
   Pointer shield, disabled mutation, and removal lifecycle assertions are real Compose/Miuix
   interaction coverage, not proxies.
2. `./gradlew :feature:settings:testAndroidHostTest --rerun-tasks --no-parallel --configuration-cache --configuration-cache-problems=fail`
   — PASS (`BUILD SUCCESSFUL in 3s`; 37 executed; configuration cache reused). Fresh Android-host
   XML aggregate: **3 tests, 0 failures, 0 errors, 0 skipped** (`SettingsPolicyTest`).
3. `./gradlew :feature:settings:iosSimulatorArm64Test --rerun-tasks --no-parallel --configuration-cache --configuration-cache-problems=fail`
   — PASS (`BUILD SUCCESSFUL in 39s`; 57 executed; configuration cache reused). Fresh iOS-simulator
   XML aggregate: **3 tests, 0 failures, 0 errors, 0 skipped** (`SettingsPolicyTest`).
4. The JVM, Android-main, and iOS-simulator compilation tasks were implied by their respective
   forced test gates. The remaining plan target was run separately:
   `./gradlew :feature:settings:compileKotlinIosArm64 --rerun-tasks --no-parallel --configuration-cache --configuration-cache-problems=fail`
   — PASS (`BUILD SUCCESSFUL in 8s`; 37 executed).
5. `./gradlew spotlessCheck --configuration-cache` — PASS (`BUILD SUCCESSFUL in 20s`; 261
   actionable tasks: 8 executed, 253 up-to-date).
6. Feature-worktree `git diff --check` — PASS. Its index is empty. Its existing non-index scope is
   unchanged: controller-owned `.superpowers/sdd/.../progress.md`, the three foundation paths, and
   untracked `feature/settings/`; this permitted ignored report is the only file appended here.
   Root checkout `git diff --check` and `git status --short` both produced no output, and its index
   is empty.

### Concerns / boundary

- This checkpoint deliberately did not run project-wide acceptance or `./init.sh`.
- It does not claim root-checkout `:shared` validation, root-checkout feature evidence, runtime UI,
  visual/device behavior, or historical blocked attempts as current results.
- No production/test logic edit, staging, commit, ledger/doc/report edit outside this append-only
  ignored report, or root-checkout modification occurred in this verification.

## Oracle findings 1, 3, and 4 correction — 2026-08-08

Status: DONE (focused Settings JVM evidence only)

### Exact scope and correction

- This continuation inspected only
  `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt` and
  `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt`,
  then appended this ignored report. No About file, build file, shared file, formatter, broad/platform
  command, staging, or commit was touched.
- Finding 1 is present in production: the Settings root is a non-clickable/non-focusable outer `Box`;
  its first full-size sibling is the non-semantic `SettingsPointerShieldTestTag` shield, which consumes
  only at `PointerEventPass.Initial`; the opaque `Surface`/`Scaffold` foreground is the later sibling.
  Thus the shield wins only blank hit tests, while foreground controls remain eligible. Dialogs remain
  later siblings above both.
- The real Compose test now uses a full-size clickable *sibling* behind production Settings, physically
  taps the shield center and the picker center, verifies `behind == 0` and picker callback `== 1`, and
  verifies both root and shield have neither click nor focus semantics. It also adds a tall real slot,
  physically swipes the real `LazyColumn`, and asserts the probe's root Y coordinate decreases.
- Finding 3 is corrected as the exact named lifecycle
  `sourceRemovalDialogOpensDismissesReopensAndConfirms`: every removal/dialog selector uses the unmerged
  tree, and the callback ledger is asserted first as `emptyList()` after dismiss and finally as
  `listOf("one")` after reopen/confirm.
- Finding 4 remains feature-resource-captured: the theme test captures
  `SettingsRes.string.theme_dark_label` during composition, locates that unmerged label after the real
  popup interaction, and asserts the complete callback list is exactly
  `listOf(RhythHausThemeMode.Dark)`.

### One-variable compile correction

- The first pointer selector failed only at `compileTestKotlinJvm` because the new test helper lacked
  the Compose `getOrNull` extension import (and the local test imports added by the partial edit were
  incomplete). Added only the required test imports; the rerun passed.
- The first exact removal selector then reported no matching test because the partial edit had not
  actually renamed the method. Renamed only that test method and its first source-row selector to the
  unmerged tree; the rerun passed.

### Required serial selectors and current class result

```zsh
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.settingsRootConsumesCoveredPointerWithoutClickOrFocusSemantics' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.sourceRemovalDialogOpensDismissesReopensAndConfirms' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.themeSelectionDispatchesSelectedMode' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest' --rerun-tasks --no-parallel
```

- Pointer selector: PASS (`BUILD SUCCESSFUL in 3s`; 36 executed tasks; configuration cache reused).
- Exact removal selector: PASS (`BUILD SUCCESSFUL in 3s`; 36 executed tasks; configuration cache
  reused).
- Theme selector: PASS (`BUILD SUCCESSFUL in 3s`; 36 executed tasks; configuration cache reused).
- Complete Settings semantics class: PASS (`BUILD SUCCESSFUL in 4s`; 36 executed tasks;
  configuration cache reused).
- Fresh `TEST-com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.xml` records
  **10 tests, 0 skipped, 0 failures, 0 errors**, including the exact renamed removal lifecycle,
  pointer/shield test, and resource-captured theme callback test.

### Final focused hygiene

- `git diff --check` passed. Worktree status remains the pre-existing controller ledger and foundation
  edits plus the untracked Settings feature directory; this continuation added no out-of-scope path.
- The user explicitly prohibited broad/platform/Spotless/stage/commit work, so none is claimed.

## Task 5.4 stale whole-load comparison boundary — 2026-08-08

Status: DONE

### RED

```zsh
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.cancellationResistantStaleLoaderCannotOverwriteNewerResult' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
```

- The focused test failed causally at `compileTestKotlinJvm`: unresolved
  `AboutLibrariesLoader` and `OpenSourceLibrariesContent`. The requested whole-load seam and
  comparison observer did not yet exist, so stale completion comparison was unobservable.

### GREEN

- Added internal `AboutLibrariesLoader` and non-throwing-documented
  `AboutLibrariesLoadComparisonObserver` seams. The unchanged public wrapper delegates to internal
  production content; its default loader remains `loadAboutLibraries(readCatalogJson)`.
- The content retains keyed `produceState(token)` and structured cancellation. It invokes the current
  observer immediately beside the current-token comparison before terminal-state publication.
- The real production-content stale test uses stable valid A/B reader identities and catalogs. It
  observes `(Loaded(B), true)`, then a cancellation-resistant test-only `NonCancellable` A completion
  as `(Loaded(A), false)`; B remains visible while A, retry, and failure UI are absent.
- Focused stale GREEN command above: PASS (`BUILD SUCCESSFUL in 3s`; 36 executed tasks).

### Verification

```zsh
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.readCancellationIsRethrownIdentically' --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.parseCancellationIsRethrownIdentically' --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.suppliedDispatcherRunsLoadAndCancellationDoesNotPublishState' --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.retryImmediatelyShowsLoadingAndUsesCurrentLoader' --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.loaderReplacementUsesNewestLoader' --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest.cancellationResistantStaleLoaderCannotOverwriteNewerResult' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.AboutScreensJvmTest' --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
./gradlew :feature:settings:compileTestKotlinJvm --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
```

- All three commands passed. Fresh `TEST-com.eterocell.rhythhaus.settings.AboutScreensJvmTest.xml`:
  **9 tests, 0 skipped, 0 failures, 0 errors**.
- `git diff --check` passed. Production `AboutScreens.kt` has no `NonCancellable` and no public
  whole-load seam.

## Definitive Settings semantics evidence correction — 2026-08-08

Status: DONE (supersedes only the earlier false Settings evidence; historical entries above are retained)

- The prior Settings evidence did **not** land: the current pre-correction test still had the behind
  clickable as a root ancestor, performed a semantic picker click, had no production lazy-scroll
  assertion, omitted the shield semantics assertion, used the wrong removal method name, and used
  merged source-row selectors. Its stale XML must not be treated as Settings proof; it had been
  overwritten by an About run.
- Corrected only
  `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt`
  plus this append-only ignored report. No Settings production, About, plan, brief, ledger, build,
  formatting, broad/platform, staging, or commit action occurred.
- Current source lines 50-69 establish the reviewed topology and interactions: outer full-size Box;
  first full-size behind clickable sibling; second sibling production `content`; unmerged production
  root and `SettingsPointerShieldTestTag` queries each call `assertNoClickOrFocusSemantics()`; a
  physical shield-space `performTouchInput` blank tap leaves `behind == 0`; the real picker is brought
  into view through `performScrollTo()`, then receives a physical center tap, yielding exactly one
  visible callback and still zero behind callbacks. No copied/proxy semantics were added.
- Current lines 137-155 use `useUnmergedTree = true` for causal rescan, remove, and confirm IDs.
  Current lines 160 onward restore the committed ledger method name
  `sourceRemovalDialogOpensDismissesAndConfirms`, preserve the full accessibility name and
  dismiss/reopen flow, and assert the exact final `listOf("one")` callback ledger.
- Back attribution is corrected: `aboutNavigationAndBackDispatchCallbacks` is in
  `SettingsScreenSemanticsJvmTest`, not `AboutScreensJvmTest`.

### Serial verification and current XML

```zsh
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.settingsRootConsumesCoveredPointerWithoutClickOrFocusSemantics' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.sourceRemovalDialogOpensDismissesAndConfirms' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.sourceRowsDispatchRescanAndRemoveById' --rerun-tasks --no-parallel
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest' --rerun-tasks --no-parallel
```

- Focused pointer: PASS (`BUILD SUCCESSFUL in 4s`; 36 executed tasks).
- Exact removal: PASS (`BUILD SUCCESSFUL in 9s`; 45 executed tasks).
- Source IDs: PASS (`BUILD SUCCESSFUL in 7s`; 45 executed tasks).
- Full class: PASS (`BUILD SUCCESSFUL in 4s`; 36 executed tasks).
- Current XML inspected directly at
  `feature/settings/build/test-results/jvmTest/TEST-com.eterocell.rhythhaus.settings.SettingsScreenSemanticsJvmTest.xml`:
  **10 tests, 0 skipped, 0 failures, 0 errors**; timestamp `2026-08-08T08:45:22.109Z`. It explicitly
  lists the pointer, source-row, exact removal, theme, and Settings-owned Back tests.
- Final `git diff --check`: PASS.

### Coexisting current Settings/About JVM evidence

The prior filtered runs replaced each other's XML output. The final focused
verification therefore selected both classes in one `--rerun-tasks`,
`--no-configuration-cache`, `--no-parallel` invocation. It completed
`BUILD SUCCESSFUL in 11s` with 45 executed tasks. Direct inspection of the
coexisting current XML files records `SettingsScreenSemanticsJvmTest` at
**10/0/0/0** (`2026-08-08T08:49:02.575Z`) and `AboutScreensJvmTest` at
**9/0/0/0** (`2026-08-08T08:49:01.550Z`) for tests/skips/failures/errors.

## Task 5.4 evidence-only runtime/resource reconciliation — 2026-08-08

Classification: **historical terminal evidence** except where explicitly identified as a
current on-disk XML observation. This append does not relabel resource evidence as current if a
concurrent XML writer has overwritten files. Earlier historical-failure sections and the current
UI dual-XML section above are preserved unchanged.

### A. Shared runtime lane

Historical RED command:

```zsh
./gradlew :shared:compileKotlinJvm --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Historical output: `:shared:compileKotlinJvm FAILED` on unresolved moved Settings-adapter
imports/references in `LibraryRoutes.kt` (`sourceMutationsAllowed`, generated `remove`/`close`,
and Compose `setValue`). A task count and wall-clock timestamp for this RED are **unavailable from
current on-disk artifacts**.

Historical compile-test GREEN command:

```zsh
./gradlew :shared:compileTestKotlinJvm --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Historical output: `BUILD SUCCESSFUL`. Its task count and wall-clock timestamp are **unavailable
from current on-disk artifacts**.

Historical focused Shared JVM GREEN command:

```zsh
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.settings.SettingsRouteAdapterJvmTest' \
  --tests 'com.eterocell.rhythhaus.settings.SettingsPlaylistBackupEmbeddingTest' \
  --tests 'com.eterocell.rhythhaus.settings.AboutLibrariesCatalogTest' \
  --tests 'com.eterocell.rhythhaus.settings.ThemePreferenceStoreJvmTest' \
  --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' \
  --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Historical output: `BUILD SUCCESSFUL`. The current XML files retained from that focused result
provide these directly inspected counts/timestamps (tests/skips/failures/errors):

- `SettingsRouteAdapterJvmTest[jvm]`: **7/0/0/0**, timestamp
  `2026-08-08T09:02:50.830Z`.
- `SettingsPlaylistBackupEmbeddingTest[jvm]`: **6/0/0/0**, timestamp
  `2026-08-08T09:02:49.547Z`.
- `AboutLibrariesCatalogTest[jvm]`: **3/0/0/0**, timestamp
  `2026-08-08T09:02:49.506Z`.
- `LibrarySourceManagementTest[jvm]`: **38/0/0/0**, timestamp
  `2026-08-08T09:02:49.415Z`.
- No current `ThemePreferenceStoreJvmTest` XML is present; its XML count/timestamp are
  **unavailable**. The focused-command task count is also **unavailable from current on-disk
  artifacts**.

Historical iOS simulator compile GREEN command:

```zsh
./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache --configuration-cache-problems=fail --no-parallel
```

Historical output: `BUILD SUCCESSFUL`. Its task count and wall-clock timestamp are **unavailable
from current on-disk artifacts**.

Historical scope/diff evidence: `git diff --check` passed after the runtime lane. Current status
contains concurrent controller/foundation/resource-lane changes, so a current status snapshot
cannot establish the runtime lane's historical exclusive-write scope. No production/test/resource
byte was changed by this reconciliation append.

### B. Resource lane terminal evidence (supplied historical terminal evidence)

- RED focused `SettingsResourceOwnershipJvmTest` with
  `-Drhythhaus.rootDir="$PWD" --no-parallel` had both **2/2 fail** due Shared duplicate keys/logo.
- GREEN, the same selector with `--rerun-tasks --no-parallel`, was `BUILD SUCCESSFUL` with
  **36 executed** tasks.
- Feature compile was successful.
- The exact four resource-lane endpoints were: test add; EN XML modification; ZH XML modification;
  and logo delete. `git diff --check` passed.

This is historical terminal evidence. If current XML was overwritten, it is not labeled current by
this append.

## Task 5.4 BuildInfo ownership and adapter correction — 2026-08-08

Status: DONE. The earlier “Coexisting current Settings/About JVM evidence” section is relabeled
**HISTORICAL**: later resource-lane runs replaced its XML outputs, so it is not current evidence.

### BuildInfo ownership

RED:

```zsh
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsResourceOwnershipJvmTest' -Drhythhaus.rootDir="$PWD" --no-parallel
```

Result: `BUILD FAILED`; 2 tests completed, 1 failed. The causal failure was
`settingsLogoHasOneFeatureOwnerAndNoForeignResImport` at
`SettingsResourceOwnershipJvmTest.kt:120`, reporting Shared-owned `RhythHausBuildInfo` generator
clauses. `36 actionable tasks: 6 executed, 2 from cache, 28 up-to-date`.

GREEN after removing all Shared generator/task/wiring/version-override clauses and its generated
BuildInfo root:

```zsh
./gradlew :feature:settings:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsResourceOwnershipJvmTest' -Drhythhaus.rootDir="$PWD" --rerun-tasks --no-parallel
```

Result: `BUILD SUCCESSFUL in 11s`; `45 actionable tasks: 45 executed`.

### Adapter evidence

- Replaced proxy source-ID and guard helper checks with production `LibraryRouteOverlays` composition
  and unmerged Settings semantics. The seven literal plan method names are retained. Current/replaced
  source callbacks, stale removal no-op, scan guard transition, real clear request/dismiss/confirm,
  Settings-to-About return, and actual About/open-library route navigation are exercised.
- `LibraryRoutes.kt` did not change in this correction; no production defect was exposed.
- Current route-test command:

  ```zsh
  ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsRouteAdapterJvmTest' --no-parallel
  ```

  Result: `BUILD SUCCESSFUL`; `120 actionable tasks: 20 executed, 1 from cache, 99 up-to-date`.
  Current XML `TEST-com.eterocell.rhythhaus.settings.SettingsRouteAdapterJvmTest.xml` records
  **7 tests, 0 skipped, 0 failures, 0 errors**, timestamp `2026-08-08T09:45:02.272Z`.
- Retained focused Shared selectors:

  ```zsh
  ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsPlaylistBackupEmbeddingTest' --tests 'com.eterocell.rhythhaus.settings.AboutLibrariesCatalogTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --no-parallel
  ```

  Result: `BUILD SUCCESSFUL in 13s`; `129 actionable tasks: 21 executed, 108 up-to-date`.
- Compile verification:

  ```zsh
  ./gradlew :shared:compileKotlinJvm :shared:compileKotlinIosSimulatorArm64 --no-parallel
  ```

  Result: `BUILD SUCCESSFUL in 10s`; `185 actionable tasks: 28 executed, 157 up-to-date`.
- `git diff --check` passed. Correction writes are limited to `shared/build.gradle.kts`,
  `SettingsRouteAdapterJvmTest.kt`, and this ignored report; `LibraryRoutes.kt` was unchanged by this
  correction. No broad acceptance, staging, or commit was run.
