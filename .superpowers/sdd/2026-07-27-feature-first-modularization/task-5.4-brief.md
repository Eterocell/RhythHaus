## Task 5.4: Move Settings

**Scope:** OpenSpec 6.4. The original implementation started from clean
`86df74a9d5945910315e69821b6552c9f740c68d`; this execution amendment resumes the frozen partial
implementation only after this amended plan is independently approved, committed, and rebound in the
brief. Execute implementation through `subagent-driven-development`; leave OpenSpec 6.4 unchecked
until the independently accepted implementation is complete. No production, test, build, schema,
toolchain, README, product, or platform-support change is authorized by this planning edit.

**Bound planning commit:** `f542a1dc58dcde72eb75a63d82e35447b29e1dfa`

**Boundary:** Create exactly one unexported `:feature:settings` Android-KMP/JVM/`iosArm64`/
`iosSimulatorArm64` implementation module. It has one common implementation, no API split, Koin
module, `iosMain` production source, iOS framework export, Library API dependency, or feature README.
Preserve Kotlin package and Android namespace `com.eterocell.rhythhaus.settings`; use Compose
namespace `rhythhaus.feature.settings.generated.resources`. Shared composes it only through
`commonMainImplementation`, never `api` or framework `export`.

**Literal implementation endpoint manifest (23 unique paths):** feature build `1` + moved feature
production `2` + feature resources `3` + feature tests `4` + Shared adapter test `1` + module/
composition build and route `3` + Shared resources `2` + architecture `2` + modified retained Shared
tests `2` + moved-source/logo deletions `3` = `23`.

**Task 5.4 implementation manifest (23 endpoints):**
```text
feature/settings/build.gradle.kts
feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt
feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt
feature/settings/src/commonMain/composeResources/values/strings.xml
feature/settings/src/commonMain/composeResources/values-zh/strings.xml
feature/settings/src/commonMain/composeResources/drawable/rhythhaus_logo.xml
feature/settings/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPolicyTest.kt
feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsResourceOwnershipJvmTest.kt
feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt
feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutScreensJvmTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsRouteAdapterJvmTest.kt
settings.gradle.kts
shared/build.gradle.kts
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt
shared/src/commonMain/composeResources/values/strings.xml
shared/src/commonMain/composeResources/values-zh/strings.xml
build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt
build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutLibrariesCatalogTest.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt
shared/src/commonMain/composeResources/drawable/rhythhaus_logo.xml
```

- [ ] After this plan is independently approved and committed, generate ignored
  `.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.4-brief.md` from that committed
  plan. Write exactly one anchored metadata line using the lowercase 40-hex output of
  `git rev-parse HEAD`; the executable gate below validates it. Confirm the index is empty and record
  the expected absent-module RED by running
  `./gradlew :feature:settings:jvmTest --configuration-cache`; record Gradle's exact missing-project
  wording, with no requested feature task executed.
- [ ] Add `include(":feature:settings")` only after that RED. Model
  `feature/settings/build.gradle.kts` on `feature/search/build.gradle.kts`: apply
  `build-logic.kmp.feature.impl`, `build-logic.android.kmp.library`,
  `build-logic.compose-resources`, and `alias(libs.plugins.compose.compiler)`; configure controlled
  resources with `namespace("rhythhaus.feature.settings.generated.resources")`; configure Android
  namespace, catalog compile/min SDK, `JvmTarget.JVM_11`, `withHostTest {}`, enabled Android
  resources, `jvm()`, `iosArm64()`, and `iosSimulatorArm64()`. Verify the catalog aliases before
  writing dependencies. `commonMain` uses `api(projects.core.ui)`, `api(libs.compose.runtime)`, and
  `api(libs.compose.ui)`; it uses implementation dependencies
  `libs.compose.foundation`, `libs.compose.components.resources`,
  `libs.compose.material.icons.extended`, `libs.miuix.ui`, `libs.miuix.preference`,
  `libs.aboutlibraries.compose.m3`, `libs.compose.material3`, and `libs.kotlinx.coroutinesCore`.
  `AboutScreens.kt` directly imports `androidx.compose.material3.Icon`; therefore
  `implementation(libs.compose.material3)` is unconditional. `commonTest` uses
  `libs.kotlin.test`; `jvmTest` uses
  `"org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}"` and
  `compose.desktop.currentOs`. Import `org.gradle.api.tasks.testing.Test` and configure exactly
  `tasks.withType<Test>().configureEach { systemProperty("rhythhaus.rootDir", rootProject.projectDir.absolutePath) }`.
  Verify registered tasks with
  `./gradlew :feature:settings:tasks --all --configuration-cache`; do not claim an `allTests` task
  exists. Shared adds `implementation(projects.feature.settings)` and removes
  `libs.miuix.preference` only after an audit proves no other Shared consumer.
- [ ] Move `GenerateRhythHausBuildInfoTask`, `VerifyRhythHausVersionOverrideTask`,
  `rhythHausVersionName`, generated-root declaration, generate-task registration, common-main
  source-directory registration, `KotlinCompilationTask` dependency, and verify-task registration
  byte-for-byte in behavior from `shared/build.gradle.kts` into the feature build. Preserve property
  `rhythhaus.versionName`, task names, generated package/object/constant and backslash, quote, and
  dollar escaping. Update the moved verify task to apply the identical backslash, quote, and dollar
  escape transformation to `expectedVersionName` before constructing `expectedDeclaration`; it must
  compare the generated Kotlin literal rather than unescaped input. Prove it exactly with this zsh-safe
  command after module registration; it uses a quoted property value and `printf`, so shell expansion
  cannot alter dollar/backslash/quote bytes:

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

  The command must succeed and `cmp` must report byte identity, including package, blank lines,
  internal object, escaped constant, and trailing newline. `--configure-on-demand` and
  `--no-configuration-cache` apply only to this adversarial non-semver override: they isolate the
  Settings task so unrelated desktop DMG packaging does not consume that test value. Do not claim
  configuration-cache evidence for this command; final ordinary-semver builds and `./init.sh` remain
  unchanged. Keep the AboutLibraries
  plugin/configuration/export and checked-in `aboutlibraries.json` in Shared.
- [ ] Move whole files `SettingsScreen.kt` and `AboutScreens.kt` to the feature destinations and
  move the logo to the feature drawable destination. The sole public production declarations are
  declaration/property-KDoc-complete `SettingsSharedLabels`, `SettingsSourceItem`,
  `SettingsScreen`, `SettingsAboutScreen`, and `OpenSourceLibrariesScreen`; policy, labels,
  dialogs, parser/load state, URLs, tags, and generated `RhythHausBuildInfo` are internal/private.
  Implement this exact boundary surface, KDoc, signature, and order:

  ```kotlin
  /** Shared-owned wording and actions injected into [SettingsScreen]. */
  public data class SettingsSharedLabels(
      /** Settings route title resolved by Shared. */
      public val title: String,
      /** Shared-owned add-folder action wording. */
      public val addMusicFolder: String,
      /** Shared-owned unavailable-picker wording. */
      public val folderPickerUnavailable: String,
      /** Shared-owned clear-library action wording. */
      public val clearLibrary: String,
      /** Shared-owned generic cancellation wording. */
      public val cancel: String,
      /** Shared-owned generic removal wording. */
      public val remove: String,
  )

  /** Immutable, feature-safe rendering projection for one authoritative Library source. */
  public data class SettingsSourceItem(
      /** Stable source identifier returned to Shared callbacks. */
      public val id: String,
      /** User-visible source name already selected by Shared. */
      public val displayName: String,
      /** Whether the source remains accessible to the platform. */
      public val accessAvailable: Boolean,
      /** Whether the source has completed at least one scan. */
      public val hasBeenScanned: Boolean,
  )

  /**
   * Renders Settings from scalar state, source projections, callbacks, and caller-owned slots. The
   * picker obeys [sourcePickerActionVisible], [sourcePickerAvailable], and [mutationsEnabled]; the
   * clear action is rendered only for [hasImportedTracks], requests Shared dialog state through
   * [onRequestClearLibrary] only when enabled, and renders [clearLibraryDialog] only when supplied.
   * Source callbacks emit IDs only; Shared resolves and guards them at invocation.
   */
  @Composable
  public fun SettingsScreen(
      labels: SettingsSharedLabels,
      currentThemeMode: RhythHausThemeMode,
      sources: List<SettingsSourceItem>,
      sourcePickerActionVisible: Boolean,
      sourcePickerAvailable: Boolean,
      importMessage: String?,
      mutationsEnabled: Boolean,
      hasImportedTracks: Boolean,
      playlistBackupContent: @Composable () -> Unit,
      activeScanContent: (@Composable () -> Unit)?,
      clearLibraryDialog: (@Composable () -> Unit)?,
      onThemeModeSelected: (RhythHausThemeMode) -> Unit,
      onAddMusicFolder: () -> Unit,
      onRescanSource: (String) -> Unit,
      onRemoveSource: (String) -> Unit,
      onRequestClearLibrary: () -> Unit,
      onAboutClick: () -> Unit,
      onDismiss: () -> Unit,
      modifier: Modifier = Modifier,
  )

  /** Renders the feature-owned About page and delegates route actions to Shared. */
  @Composable
  public fun SettingsAboutScreen(
      onOpenLibraries: () -> Unit,
      onDismiss: () -> Unit,
      modifier: Modifier = Modifier,
  )

  /**
   * Reads and renders caller-supplied app-wide attribution JSON, retaining retryable failures and
   * preserving exact injected read/parse callback cancellation identity (parse exits dispatcher work
   * as data), while dispatcher rejection, prompt cancellation, and Job cancellation propagate without
   * `Loaded`/`Failed` publication or an identity promise.
   */
  @Composable
  public fun OpenSourceLibrariesScreen(
      readCatalogJson: suspend () -> String,
      onDismiss: () -> Unit,
      modifier: Modifier = Modifier,
  )
  ```

- [ ] Put exactly these 28 keys, once per EN and ZH locale, in feature resources:
  `appearance`, `theme_system_label`, `theme_light_label`, `theme_dark_label`,
  `theme_system_description`, `theme_light_description`, `theme_dark_description`, `manage_music`,
  `configured_folders`, `unnamed_folder`, `source_access_available`, `source_access_lost`,
  `source_never_scanned`, `source_last_scanned`, `source_status_format`, `rescan_source_format`,
  `remove_source_format`, `remove_folder`, `remove_folder_message`, `about`, `about_app_name`,
  `about_logo_description`, `about_version_format`, `about_view_source`,
  `about_open_source_libraries`, `open_source_libraries_loading`,
  `open_source_libraries_error`, and `open_source_libraries_retry`; move only
  `rhythhaus_logo` with them. Shared retains exactly `settings`, `add_music_folder`,
  `folder_picker_unavailable`, `clear_library`, `clear_library_message`, `clear`, `cancel`,
  `remove`, `close`, `scanning`, `scan_progress_format`, `scan_complete_format`,
  `folder_picker_error_access`, `folder_picker_error_select`, `folder_picker_error_prepare`, and
  `folder_picker_no_folder_selected`: App/picker/scanning card, clear dialog, and playlist backup
  remain their consumers. Require EN/ZH parity and reject duplicate, missing, wrong-owner, logo, and
  foreign-generated-`Res` controls.
- [ ] Adapt only `LibraryRoutes.kt`: remember Shared clear-dialog visibility; map current sources to
  `SettingsSourceItem`; inject `SettingsSharedLabels`; and resolve emitted IDs against the latest
  `sources` at callback invocation. `SettingsSourceItem.hasBeenScanned` is sufficient for the
  existing NeverScanned/LastScanned behavior because Settings renders no timestamp; do not add one.
  Missing/stale IDs are no-op. After resolution recheck the current scan/job
  `sourceMutationsAllowed` guard, preserving Shared mutation errors. Supply slots for
  `PlaylistBackupSettingsHost`, active `ScanningCard`, and `clearLibraryDialog` only while the
  Shared-owned remembered visibility is true. `onRequestClearLibrary` toggles that visibility only
  when `mutationsEnabled` and `hasImportedTracks` are true; dismiss clears it; confirm rechecks the
  current `sourceMutationsAllowed` guard, invokes existing `onClearLibrary` only when allowed, then
  clears it. Key remembered visibility to the active Settings destination identity and route
  appearance; clear it on Settings route departure/disposal and in the Settings `onDismiss` wrapper
  before delegating. `clearLibraryDialog` is non-null only when the active route is Settings and that
  keyed visibility is true, so Settings-to-About/another route and return-to-Settings cannot reopen
  it. Source-removal state remains feature-local. Supply
  `Res.readBytes("files/aboutlibraries.json").decodeToString()` to
  `OpenSourceLibrariesScreen`. Routes and Back remain Shared. Do not edit `App.kt`,
  `LibraryAppShell.kt`, or `RhythHausDi.kt` unless a source-audited compile-required call change is
  found. ThemePreferenceStore, its actuals, root theme, and Koin remain untouched.
- [ ] Keep feature source-removal dialog visibility and About retry generation local. Define only an
  internal/private test seam, not public API:
  `internal suspend fun loadAboutLibraries(readJson: suspend () -> String, parseJson: (String) -> Libs = { Libs.Builder().withJson(it).build() }, dispatcher: CoroutineDispatcher = Dispatchers.Default): AboutLibrariesLoadState`.
  It returns Loaded only for a nonempty parsed catalog from a fixture with top-level `libraries` and `licenses`; malformed/empty input returns retryable Failed. Injected read callback `CancellationException` is rethrown with exact object identity. Injected parse callback `CancellationException` is captured as data inside dispatcher work and rethrown outside with the exact original object. Genuine dispatcher rejection, `withContext` prompt cancellation, and Job cancellation propagate as cancellation, publish neither Loaded nor Failed, and have no identity promise. `OpenSourceLibrariesScreen` uses `rememberUpdatedState(readCatalogJson)`, synchronous Loading on retry, and an opaque newest request token keyed by current loader identity plus monotonic retry generation; it publishes only when that exact token remains current. An obsolete cancellation-resistant loader completing after replacement cannot overwrite a newer result.

   **About whole-load causal seam amendment.** Authorize an internal, declaration-KDoc-documented whole-load seam on the real production About screen/content implementation: `internal typealias AboutLibrariesStateLoader = suspend (suspend () -> String) -> AboutLibrariesLoadState` (or an equivalent internal function type). Its default delegates to the existing real `loadAboutLibraries` path. Authorize an internal no-op completion-comparison observer `(state: AboutLibrariesLoadState, isCurrent: Boolean) -> Unit`, invoked immediately beside the real `token === currentToken` comparison and before conditional publication; it exposes no token and cannot publish. `OpenSourceLibrariesScreen` public signature/default behavior remains unchanged; keyed `produceState(token)` and production structured cancellation remain intact. Production SHALL NOT use `NonCancellable`, unbounded stale jobs, or a channel actor. Only the injected JVM whole-load test seam may use `NonCancellable` after cancellation to return stale `Loaded(A)` to that real production comparison.

   The causal JVM test uses stable A/B reader identities: start and hold A, replace with B, render B, then release cancellation-resistant A. The observer must record `(Loaded(A), false)` and `(Loaded(B), true)` at the comparison boundary; B remains rendered, A is absent, the retry control is absent, and failure UI is absent. Existing injected read/parse exact-identity cancellation and dispatcher/Job cancellation stay on the real loader in separate tests.

- [ ] Write tests before production moves. Create common
  `SettingsPolicyTest.kt` methods `compactSettingsLayoutPolicyHasApprovedValues`,
  `sourceLabelsDeriveFromSettingsSourceItem`, and `themeOptionsUseSystemLightDarkOrder`. Create JVM
  `SettingsScreenSemanticsJvmTest.kt` methods
  `pickerIsHiddenUnavailableAndEnabledFromExplicitInputs`, `clearIsHiddenRequestsAndRendersNullableSlot`,
  `disabledMutationsDoNotDispatch`, `sourceRowsDispatchRescanAndRemoveById`,
  `slotsRenderInPlaylistScanAndClearOrder`, `themeSelectionDispatchesSelectedMode`,
  `aboutNavigationAndBackDispatchCallbacks`, `sourceRemovalDialogOpensDismissesAndConfirms`, and
  `publicProjectionRendersWithoutSharedTypes`. Create JVM `AboutScreensJvmTest.kt` methods
  `aboutRendersVersionLogoLibrariesAndSourceLink`, `catalogLoadsOnlyWhenNonEmpty`,
  `malformedAndEmptyCatalogsFail`, `retryImmediatelyShowsLoadingAndUsesCurrentLoader`,
  `readCancellationIsRethrownIdentically`, `suppliedDispatcherRunsLoadAndCancellationDoesNotPublishState`,
  `parseCancellationIsRethrownIdentically`, `loaderReplacementUsesNewestLoader`, and
  `cancellationResistantStaleLoaderCannotOverwriteNewerResult`. Create JVM
  `SettingsResourceOwnershipJvmTest.kt` at
  `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsResourceOwnershipJvmTest.kt`;
  it requires system property `rhythhaus.rootDir`, fails when the property or any expected
  Shared/feature EN/ZH/source/logo path is absent, and resolves every such repository path from that
  property. Its repository XML/source/drawable-path methods are
  `settingsResourcesHaveExactEnZhParityAndOwnership` and
  `settingsLogoHasOneFeatureOwnerAndNoForeignResImport`.
- [ ] Create Shared `SettingsRouteAdapterJvmTest.kt` against real `LibraryRouteOverlays` with methods
  `projectsSourcesAndSuppliesPickerScanningPlaylistAndClearSlots`,
  `currentStaleAndReplacedIdsResolveAtInvocation`, `guardChangesAndErrorsRemainSharedOwned`,
  `injectsSharedLabelsAndCurrentCatalogLoader`, `clearDialogRequestDismissAndConfirmFollowSharedLifecycle`,
  `settingsDismissAndSettingsToAboutClearDialogAndReturnDoesNotReopen`, and
  `settingsRoutesAndBackRemainSharedOwned`. The clear-lifecycle methods assert request, dismiss,
  confirm, route dismissal, Settings-to-About, and return-to-Settings never reopening. Split
  `AboutLibrariesCatalogTest.kt`: retain only `checkedInCatalogParsesAndContainsLibraries`,
  `uiConsumedCatalogJsonParsesAndContainsDisplayableLibraries`, and
  `checkedInCatalogAttributesNativeTagLibDependency`; move its four `loadAboutLibraries` tests into
  feature About coverage and add generation/replacement cases. Modify
  `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt` only to remove
  Settings policy imports/assertions and method `sourceManagementLabelsMapAccessAndLastScanState`;
  recreate equivalent access/scanned projection assertions in
  `SettingsPolicyTest.sourceLabelsDeriveFromSettingsSourceItem`, preserving every other Library source
  management test. Do not invent a `SettingsScreenTest` move. Retain and verify
  `SettingsPlaylistBackupEmbeddingTest` and `ThemePreferenceStoreJvmTest` under Shared JVM tests;
  retain exact root-theme coverage at
  `core/ui/src/commonTest/kotlin/com/eterocell/rhythhaus/theme/ThemeTest.kt` through
  `./gradlew :core:ui:jvmTest --tests 'com.eterocell.rhythhaus.theme.ThemeTest' --configuration-cache --configuration-cache-problems=fail --no-parallel`.
  Do not add unchanged files to the manifest.
- [ ] Extend `ArchitectureAllowList` with Settings package/Android/Compose policy and only
  Settings-to-core-UI plus Shared-to-Settings `commonMainImplementation`. Extend the real Settings
  fixture in `ArchitectureCheckPluginFunctionalTest` with all targets, direct processor registrations,
  package roots, forbidden Settings edges, Shared `api`, Koin/DataStore, iOS export, wrong/empty
  package, Android/Compose namespace, public-KDoc closure, exact resource multiset/parity/logo, and
  foreign-`Res` controls. Build the external processor JAR as Search does; run fixture RED/GREEN one
  variable at a time with `--rerun-tasks`. Do not change `ArchitectureCheckTask` production code.
- [ ] Run the following literal GREEN matrix in this order; every failure blocks acceptance and is
  recorded with the exact command/output:

  ```zsh
  ./gradlew :feature:settings:jvmTest :feature:settings:testAndroidHostTest :feature:settings:iosSimulatorArm64Test :feature:settings:compileKotlinJvm :feature:settings:compileAndroidMain :feature:settings:compileKotlinIosArm64 :feature:settings:compileKotlinIosSimulatorArm64 --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.settings.SettingsRouteAdapterJvmTest' --tests 'com.eterocell.rhythhaus.settings.SettingsPlaylistBackupEmbeddingTest' --tests 'com.eterocell.rhythhaus.settings.AboutLibrariesCatalogTest' --tests 'com.eterocell.rhythhaus.theme.ThemePreferenceStoreJvmTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :core:ui:jvmTest --tests 'com.eterocell.rhythhaus.theme.ThemeTest' --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :shared:tasks --all --configuration-cache | rg -F 'exportLibraryDefinitions'
  ./gradlew :shared:exportLibraryDefinitions --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel && git diff --exit-code -- shared/src/commonMain/composeResources/files/aboutlibraries.json
  ./gradlew :architecture-processor:clean :architecture-processor:jar --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ArchitectureCheckPluginFunctionalTest' -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar" --rerun-tasks --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail --no-parallel
  ./gradlew spotlessApply --configuration-cache
  ./gradlew spotlessCheck --configuration-cache
  ./gradlew detekt --configuration-cache
  PATH="$HOME/.nvm/versions/node/v26.7.0/bin:$PATH" openspec validate feature-first-modularization --strict
  ./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug :shared:iosSimulatorArm64Test --configuration-cache --configuration-cache-problems=fail --no-parallel
  /usr/bin/xcrun xcodebuild -version
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
  /usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17' test
  perl -e 'alarm 1200; exec @ARGV' ./init.sh
  ```

  The second `architectureCheck` must report configuration-cache reuse. The discovery command must
  emit `exportLibraryDefinitions`, freezing that exact export task before the byte-identity command.
  This plan makes no runtime, visual, device, browser, picker, scanner, or live mutation claim.
- [ ] Before staging, independently review behavior/spec and exact scope/resource/test ownership.

### Task 5.4 Authoritative Q Lifecycle And Proof

**Consumed history.** Parser evidence, the 23-endpoint frozen inventory, and the post-P deleted-endpoint RED are consumed historical evidence. The catalog-only maintenance baseline is exactly `453be164c6b7ea02f7eda3c6c7b1ee28739cdebf -> d1bf3f6996543a78746463f5848a24df26f1c58b`; P, `ab652b3273f8d24ebe00cb38483864e01ff3e490`, is its direct plan-only child and the causal parser-repair RED authority. No production post-Q pass is claimed while current HEAD remains P.

**Current authority.** The sole lifecycle is `453be164c6b7ea02f7eda3c6c7b1ee28739cdebf -> d1bf3f6996543a78746463f5848a24df26f1c58b -> P ab652b3273f8d24ebe00cb38483864e01ff3e490 -> Q -> I`. Q is exactly one direct plan-only child of P. The ignored brief is uniquely bound to P before Q and is rebound uniquely to Q after Q. I is the direct child of Q. There is no generic rebind authority and no current P-to-I authority.

The following is the sole executable authoritative proof. It contains the static 23-entry inventory, independent synthetic fixture oracle, fail-closed actual-hash handling, strict-child runner, parser/pre-Q/post-Q gates, 13 producer controls, 15 mutations, and all three modes. It was verified at SHA `b590e215ec8bdf5d736a334f166560303c4ce54fb310ea0d66675e452a89e9a5`.

```zsh
emulate -L zsh
setopt errexit nounset pipefail
export PATH=/usr/bin:/bin:/usr/sbin:/sbin

readonly repository_root='/Users/eterocell/Git/self/RhythHaus/.worktrees/feature-first-modularization'
readonly plan_path="$repository_root/docs/superpowers/plans/2026-07-27-feature-first-modularization.md"
readonly manifest_marker='**Task 5.4 implementation manifest (23 endpoints):**'
readonly ledger_path='.superpowers/sdd/2026-07-27-feature-first-modularization/progress.md'
readonly temp_root='/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode'

# Canonical manifest/status/brief helpers.
manifest_paths() {
  local source_plan="${1:-$plan_path}"
  /usr/bin/awk -v marker="$manifest_marker" '
    /^## Task 5\.4:/ { in_task = 1; next }
    /^## Task 6\.1:/ {
      if (in_task) {
        reached_task_end = 1
        in_task = 0
      }
      next
    }
    !in_task { next }
    $0 == marker {
      marker_count++
      if (marker_count != 1 || state != 0) invalid = 1
      else state = 1
      next
    }
    state == 1 {
      if ($0 != "```text") invalid = 1
      else state = 2
      next
    }
    state == 2 && $0 == "```" {
      closing_fence_count++
      state = 3
      pending_terminator = 1
      next
    }
    state == 2 { paths[++path_count] = $0; next }
    state == 3 && pending_terminator {
      if ($0 == "```") invalid = 1
      pending_terminator = 0
    }
    END {
      if (!reached_task_end || marker_count != 1 || state != 3 || closing_fence_count != 1 || invalid || path_count == 0) exit 1
      for (i = 1; i <= path_count; i++) print paths[i]
    }
  ' "$source_plan"
}

capture_manifest_paths() {
  local output_path="$1"
  shift
  manifest_paths "$@" > "$output_path" || { /bin/rm -f -- "$output_path"; return 1; }
  test -s "$output_path" || { /bin/rm -f -- "$output_path"; return 1; }
}

real_status_records_producer() {
  /usr/bin/git -C "$repository_root" -c status.renames=false -c core.quotepath=false status --porcelain=v1 -z --untracked-files=all
}

status_records_producer() { real_status_records_producer; }

status_records() {
  setopt localoptions pipefail
  status_records_producer |
    /usr/bin/perl -0ne 'for (split /\0/) { next unless length; /^(.{2}) (.*)\z/s or die "malformed porcelain record\n"; ($s,$p)=($1,$2); $s =~ /^(?: M|M |MM|A |AM| D|D |\?\?)$/ or die "unsupported status [$s] for $p\n"; print "$s\t$p\n"; }'
}

require_one_ledger() {
  local records_file count
  records_file="$(/usr/bin/mktemp "$temp_root/task-5.4-ledger.XXXXXX")" || return 1
  status_records | /usr/bin/grep -Fx " M	$ledger_path" > "$records_file" || { /bin/rm -f -- "$records_file"; return 1; }
  count="$(/usr/bin/wc -l < "$records_file" | /usr/bin/tr -d ' ')"
  /bin/rm -f -- "$records_file"
  test "$count" = 1
}

cached_paths_producer() { /usr/bin/git -C "$repository_root" diff --cached --name-only; }
cached_paths() { cached_paths_producer; }

parse_bound_planning_sha() {
  local file="$1" prefix_count line
  prefix_count="$(/usr/bin/awk '/^\*\*Bound planning commit:\*\*/ { count++ } END { print count + 0 }' "$file")" || return 1
  test "$prefix_count" = 1 || return 1
  line="$(/usr/bin/awk '/^\*\*Bound planning commit:\*\*/ { print; exit }' "$file")" || return 1
  printf '%s\n' "$line" | /usr/bin/grep -Eq '^\*\*Bound planning commit:\*\* `[0-9a-f]{40}`$' || return 1
  printf '%s\n' "$line" | /usr/bin/sed -E 's/^\*\*Bound planning commit:\*\* `([0-9a-f]{40})`$/\1/'
}

require_bound_planning_sha() {
  local file="${1:-$repository_root/.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.4-brief.md}"
  local expected_head="${2:-$(/usr/bin/git -C "$repository_root" rev-parse HEAD)}" actual_sha
  actual_sha="$(parse_bound_planning_sha "$file")" || return 1
  test "$actual_sha" = "$expected_head"
}

manifest_proof_consumer() {
  local manifest_file="$1"
  shift
  "$@" "$manifest_file"
}

run_manifest_consumer() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local source_plan="$1" output_path="$2" consumer="$3"
  shift 3
  capture_manifest_paths "$output_path" "$source_plan"
  local callback_rc=0
  setopt noerrexit
  "$consumer" "$output_path" "$@"
  callback_rc=$?
  set -e
  /bin/rm -f -- "$output_path"
  return "$callback_rc"
)

assert_manifest_line_count() {
  local expected_count="$1" manifest_file="$2"
  test "$(/usr/bin/wc -l < "$manifest_file" | /usr/bin/tr -d ' ')" = "$expected_count"
}

assert_two_intended_paths() {
  local manifest_file="$1"
  /usr/bin/diff -u <(printf '%s\n' intended/one.kt intended/two.kt) "$manifest_file"
  test "$(LC_ALL=C /usr/bin/sort -u "$manifest_file" | /usr/bin/wc -l | /usr/bin/tr -d ' ')" = 2
}

failing_callback() { return 61; }
naked_early_failure_then_success() { /usr/bin/false; /usr/bin/true; }

assert_current_manifest_and_status() {
  local status_file="$1" manifest_file="$2"
  test "$(/usr/bin/wc -l < "$manifest_file" | /usr/bin/tr -d ' ')" = 23
  test "$(LC_ALL=C /usr/bin/sort -u "$manifest_file" | /usr/bin/wc -l | /usr/bin/tr -d ' ')" = 23
  status_records | /usr/bin/awk -F '\t' -v ledger="$ledger_path" -v plan='docs/superpowers/plans/2026-07-27-feature-first-modularization.md' -v catalog='shared/src/commonMain/composeResources/files/aboutlibraries.json' '$2 != ledger && $2 != plan && $2 != catalog { print $2 }' > "$status_file"
  test "$(LC_ALL=C /usr/bin/sort -u "$status_file" | /usr/bin/wc -l | /usr/bin/tr -d ' ')" = 23
  /usr/bin/diff -u <(LC_ALL=C /usr/bin/sort -u "$manifest_file") <(LC_ALL=C /usr/bin/sort -u "$status_file")
}

run_manifest_proof_core() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local root="$1" synthetic_plan="$root/synthetic-plan.md" status_file="$root/current-status.out"
  /bin/mkdir -p -- "$root"
  {
    printf '%s\n' '## Task 5.4: Move Settings' '```text' 'decoy/before.kt' '```'
    printf '%s\n' "$manifest_marker" '```text' 'intended/one.kt' 'intended/two.kt' '```'
    printf '%s\n' '```text' 'decoy/after.kt' '```' '## Task 6.1: Extract Library Implementation Last'
  } > "$synthetic_plan"
  run_manifest_consumer "$synthetic_plan" "$root/valid-capture.out" manifest_proof_consumer assert_two_intended_paths
  test ! -e "$root/valid-capture.out"
  run_manifest_consumer "$plan_path" "$root/current-capture.out" manifest_proof_consumer assert_current_manifest_and_status "$status_file"
  test ! -e "$root/current-capture.out"
)

run_manifest_proof() {
  setopt localoptions noerrexit
  local root core_rc=0 cleanup_rc=0 residue_count
  local -a residue_paths
  root="$(/usr/bin/mktemp -d "$temp_root/task-5.4-manifest.XXXXXX")" || return 1
  setopt noerrexit
  run_manifest_proof_core "$root"
  core_rc=$?
  set -e
  /bin/rm -rf -- "$root" || cleanup_rc=$?
  residue_paths=("$temp_root"/task-5.4-manifest.*(N))
  residue_count="${#residue_paths}"
  test "$residue_count" = 0 || cleanup_rc=1
  test "$core_rc" = 0 || return "$core_rc"
  return "$cleanup_rc"
}

apply_sabotage() {
  case "${MANIFEST_PROOF_SABOTAGE:-}" in
    '') ;;
    capture) capture_manifest_paths() { return 75; } ;;
    parser) manifest_paths() { return 76; } ;;
    consumer) run_manifest_consumer() { return 77; } ;;
    external) status_records_producer() { return 78; } ;;
    *) return 2 ;;
  esac
}

checkpoint_core() (
  emulate -L zsh
  setopt errexit nounset pipefail
  if (( $# == 1 )); then
    MANIFEST_PROOF_SABOTAGE="$1"
  fi
  apply_sabotage
  run_manifest_proof
)

# A generated child has its own strict shell. The parent temporarily disables errexit only to retain its rc.
run_child() (
  emulate -L zsh
  setopt noerrexit nounset pipefail
  local child rc=0 definition name
  local -a function_names
  child="$(/usr/bin/mktemp "$temp_root/task-5.4-child.XXXXXX")" || return 1
  trap '/bin/rm -f -- "$child"' EXIT
  function_names=(manifest_paths capture_manifest_paths real_status_records_producer status_records_producer status_records require_one_ledger cached_paths_producer cached_paths parse_bound_planning_sha require_bound_planning_sha manifest_proof_consumer run_manifest_consumer assert_manifest_line_count assert_two_intended_paths failing_callback naked_early_failure_then_success assert_current_manifest_and_status run_manifest_proof_core run_manifest_proof apply_sabotage checkpoint_core frozen_inventory capture_status_records post_q_actual_hashes capture_post_q_actual_hashes pre_q_head pre_q_parent pre_q_grandparent assert_pre_q_commit_paths assert_pre_q_commits assert_pre_q_index assert_authoritative_q_state run_pre_q_gate_core run_pre_q_gate apply_pre_q_sabotage pre_q_checkpoint_core assert_missing_baseline post_manifest_paths post_status_records_producer post_head post_parent post_grandparent post_planning post_index_paths post_brief post_actual_hashes_for post_q_fixture_oracle post_q_assert_commit_paths post_q_assert_ledger post_q_assert_clean_plan_catalog post_q_assert_status post_q_assert_hashes run_post_q_gate_core run_post_q_gate apply_post_q_sabotage post_q_proof_core)
  {
    print -r -- 'emulate -L zsh'
    print -r -- 'setopt errexit nounset pipefail'
    print -r -- 'export PATH=/usr/bin:/bin:/usr/sbin:/sbin'
    print -r -- "readonly repository_root=${(q)repository_root}"
    print -r -- "readonly plan_path=${(q)plan_path}"
    print -r -- "readonly manifest_marker=${(q)manifest_marker}"
    print -r -- "readonly ledger_path=${(q)ledger_path}"
    print -r -- "readonly temp_root=${(q)temp_root}"
    print -r -- "readonly planning_commit=${(q)planning_commit}"
    print -r -- "readonly catalog_commit=${(q)catalog_commit}"
    print -r -- "readonly catalog_parent_commit=${(q)catalog_parent_commit}"
    print -r -- "readonly brief_path=${(q)brief_path}"
    for name in "${function_names[@]}"; do
      definition="$(typeset -f "$name")" || { /bin/rm -f -- "$child"; return 1; }
      print -r -- "$definition"
    done
    print -r -- '"$@"'
  } > "$child"
  setopt noerrexit
  /bin/zsh -e "$child" "$@"
  rc=$?
  set -e
  /bin/rm -f -- "$child"
  trap - EXIT
  return "$rc"
)

expect_nonzero_child() {
  setopt localoptions noerrexit
  local rc=0
  setopt noerrexit
  run_child "$@" > /dev/null 2>&1
  rc=$?
  set -e
  test "$rc" != 0
}

write_malformed_cases() {
  local root="$1"
  printf '%s\n' '## Task 5.4: Move Settings' "$manifest_marker" 'not-a-text-fence' '## Task 6.1: Extract Library Implementation Last' > "$root/wrong-fence.md"
  printf '%s\n' '## Task 5.4: Move Settings' "$manifest_marker" '```text' 'first.kt' '```' "$manifest_marker" '```text' 'second.kt' '```' '## Task 6.1: Extract Library Implementation Last' > "$root/duplicate.md"
  printf '%s\n' '## Task 5.4: Move Settings' "$manifest_marker" '```text' 'first.kt' '```' '```' '## Task 6.1: Extract Library Implementation Last' > "$root/duplicate-close.md"
  printf '%s\n' '## Task 5.4: Move Settings' "$manifest_marker" '```text' 'unclosed.kt' '## Task 6.1: Extract Library Implementation Last' > "$root/unclosed.md"
  printf '%s\n' '## Task 5.4: Move Settings' '```text' 'no-marker.kt' '```' '## Task 6.1: Extract Library Implementation Last' > "$root/missing.md"
  printf '%s\n' '## Task 5.4: Move Settings' "$manifest_marker" '```text' '```' '## Task 6.1: Extract Library Implementation Last' > "$root/empty.md"
}

checkpoint_parser() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local root index_file index_before index_after negative_count=0
  local -a stale_paths
  stale_paths=("$temp_root"/task-5.4-checkpoint.*(N) "$temp_root"/task-5.4-child.*(N) "$temp_root"/task-5.4-manifest.*(N))
  if (( ${#stale_paths} > 0 )); then
    /bin/rm -rf -- "${stale_paths[@]}"
  fi
  root="$(/usr/bin/mktemp -d "$temp_root/task-5.4-checkpoint.XXXXXX")"
  trap 'if [[ -n "${root:-}" ]]; then /bin/rm -rf -- "$root"; fi' EXIT
  index_file="$(/usr/bin/git -C "$repository_root" rev-parse --git-path index)"
  index_before="$(/usr/bin/shasum -a 256 "$index_file")"
  run_child checkpoint_core
  write_malformed_cases "$root"
  local case_file
  for case_file in wrong-fence duplicate duplicate-close unclosed missing empty; do
    expect_nonzero_child run_manifest_consumer "$root/$case_file.md" "$root/$case_file.out" manifest_proof_consumer assert_manifest_line_count 23
    test ! -e "$root/$case_file.out"
    (( negative_count += 1 ))
  done
  expect_nonzero_child run_manifest_consumer "$plan_path" "$root/callback.out" manifest_proof_consumer failing_callback
  test ! -e "$root/callback.out"
  (( negative_count += 1 ))
  expect_nonzero_child naked_early_failure_then_success
  (( negative_count += 1 ))
  expect_nonzero_child checkpoint_core capture
  (( negative_count += 1 ))
  expect_nonzero_child checkpoint_core parser
  (( negative_count += 1 ))
  expect_nonzero_child checkpoint_core consumer
  (( negative_count += 1 ))
  expect_nonzero_child checkpoint_core external
  (( negative_count += 1 ))
  index_after="$(/usr/bin/shasum -a 256 "$index_file")"
  /usr/bin/diff -u <(printf '%s\n' "$index_before") <(printf '%s\n' "$index_after")
  /bin/rm -rf -- "$root"
  root=''
  trap - EXIT
  local -a residue_paths
  residue_paths=("$temp_root"/task-5.4-checkpoint.*(N) "$temp_root"/task-5.4-child.*(N) "$temp_root"/task-5.4-manifest.*(N))
  test "${#residue_paths}" = 0
  printf 'checkpoint=parser normal_rc=0 negatives=%s manifest_count=23 current_status_count=23 residue=0 index_byte_identical=yes\n' "$negative_count"
)

# Checkpoint 2: authoritative pre-Q / post-Q proof and fixture gate.
readonly planning_commit='ab652b3273f8d24ebe00cb38483864e01ff3e490'
readonly catalog_commit='d1bf3f6996543a78746463f5848a24df26f1c58b'
readonly catalog_parent_commit='453be164c6b7ea02f7eda3c6c7b1ee28739cdebf'
readonly brief_path='.superpowers/sdd/2026-07-27-feature-first-modularization/task-5.4-brief.md'

# status<TAB>path<TAB>frozen post-Q state; this is intentionally a static production inventory.
frozen_inventory() {
  print -r -- $'??\tfeature/settings/build.gradle.kts\tPRESENT@971575390f711c3d095056c95607c63c9c80c9ff'
  print -r -- $'??\tfeature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt\tPRESENT@be74049fa7f82d0263ec3be52bf6a48659be44f5'
  print -r -- $'??\tfeature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt\tPRESENT@af64e6ccde80e7e3fae833e3d8413a532038c977'
  print -r -- $'??\tfeature/settings/src/commonMain/composeResources/values/strings.xml\tPRESENT@9323d388cf83265f122f364a23a2ec5beee7c842'
  print -r -- $'??\tfeature/settings/src/commonMain/composeResources/values-zh/strings.xml\tPRESENT@91f4e703cd2b7dd27dee981ae8aebe167976baf3'
  print -r -- $'??\tfeature/settings/src/commonMain/composeResources/drawable/rhythhaus_logo.xml\tPRESENT@34be87cf4194e450a46cccb595f64836ca620a99'
  print -r -- $'??\tfeature/settings/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsPolicyTest.kt\tPRESENT@1b9f3ddfc02dfb587ae5f501f3b5306e875ab6f4'
  print -r -- $'??\tfeature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsResourceOwnershipJvmTest.kt\tPRESENT@db7fe7d6eed628ff6798da2aa2c5420e4f35ba6b'
  print -r -- $'??\tfeature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt\tPRESENT@dceae10a053f0c0293d83b67052769ab96353803'
  print -r -- $'??\tfeature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutScreensJvmTest.kt\tPRESENT@8a81661b7a65be1810d4976757a6e32b4939d85b'
  print -r -- $'??\tshared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsRouteAdapterJvmTest.kt\tPRESENT@7a040031bb734257a2f7e10c486f5f55980f4d38'
  print -r -- $' M\tsettings.gradle.kts\tPRESENT@14328be00ced0aa603cfb0e3219dd75bcb237e6a'
  print -r -- $' M\tshared/build.gradle.kts\tPRESENT@95a9943ef04233c4356913a393592ec0740ad6f8'
  print -r -- $' M\tshared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt\tPRESENT@33829b760e1d3198dd4bf1f3785c00d328eb311e'
  print -r -- $' M\tshared/src/commonMain/composeResources/values/strings.xml\tPRESENT@e6dec2ac99a1147a21ee6ad0546798d07df8f4d1'
  print -r -- $' M\tshared/src/commonMain/composeResources/values-zh/strings.xml\tPRESENT@65b8899697dd90768d50b223094fa8ad868d4f02'
  print -r -- $' M\tbuild-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt\tPRESENT@1a4dd1d6d1036d0b1fd9787e2e8311cd72acdf51'
  print -r -- $' M\tbuild-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt\tPRESENT@76f31b90def945bc9372b41c95c83a398ef1ce7c'
  print -r -- $' M\tshared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/AboutLibrariesCatalogTest.kt\tPRESENT@082ae051041b38a4e3963eeb0e6f06de537b3ddd'
  print -r -- $' M\tshared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt\tPRESENT@05022f0b52ce5af655767231833692af56ed8ff8'
  print -r -- $' D\tshared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt\tDELETED@3ddbff9729c2447c08b0dcebc68048f89dcfce59'
  print -r -- $' D\tshared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/AboutScreens.kt\tDELETED@a12e41402e54fba8ed68d2a75eab3e4f0b4a9a97'
  print -r -- $' D\tshared/src/commonMain/composeResources/drawable/rhythhaus_logo.xml\tDELETED@1b1da6d174890beaff95ed5c23cbe8954872482c'
}

capture_status_records() {
  local raw_file="$1" records_file="$2" producer_rc=0
  setopt localoptions noerrexit
  status_records_producer > "$raw_file"
  producer_rc=$?
  test "$producer_rc" = 0 || return "$producer_rc"
  /usr/bin/perl -0ne 'for (split /\0/) { next unless length; /^(.{2}) (.*)\z/s or die "malformed porcelain record\n"; ($s,$p)=($1,$2); $s =~ /^(?: M|M |MM|A |AM| D|D |\?\?)$/ or die "unsupported status [$s] for $p\n"; print "$s\t$p\n"; }' "$raw_file" > "$records_file"
}

post_q_actual_hashes() {
  local manifest_file="$1" path oid
  while IFS= read -r path || [[ -n "$path" ]]; do
    if [[ -e "$repository_root/$path" ]]; then
      oid="$(/usr/bin/git -C "$repository_root" hash-object -- "$path")" || return 1
      print -r -- "$path"$'\t'"PRESENT@$oid"
    elif oid="$(/usr/bin/git -C "$repository_root" rev-parse "HEAD:$path" 2>/dev/null)"; then
      print -r -- "$path"$'\t'"DELETED@$oid"
    else
      print -u2 -r -- "MISSING@$path"
      return 1
    fi
  done < "$manifest_file"
}

# Do not put the producer in a pipeline: retain its status before sorting regardless of caller options.
capture_post_q_actual_hashes() {
  local manifest_file="$1" raw_file="$2" sorted_file="$3" producer_rc=0
  setopt localoptions noerrexit
  post_q_actual_hashes "$manifest_file" > "$raw_file"
  producer_rc=$?
  test "$producer_rc" = 0 || return "$producer_rc"
  LC_ALL=C /usr/bin/sort "$raw_file" > "$sorted_file"
}

pre_q_head() { /usr/bin/git -C "$repository_root" rev-parse HEAD; }
pre_q_parent() { /usr/bin/git -C "$repository_root" rev-parse HEAD^; }
pre_q_grandparent() { /usr/bin/git -C "$repository_root" rev-parse HEAD^^; }

assert_pre_q_commit_paths() {
  /usr/bin/diff -u <(printf '%s\n' 'docs/superpowers/plans/2026-07-27-feature-first-modularization.md') <(/usr/bin/git -C "$repository_root" diff --name-only HEAD^ HEAD)
  /usr/bin/diff -u <(printf '%s\n' 'shared/src/commonMain/composeResources/files/aboutlibraries.json') <(/usr/bin/git -C "$repository_root" diff --name-only HEAD^^ HEAD^)
}

assert_pre_q_commits() {
  test "$(pre_q_head)" = "$planning_commit"
  test "$(pre_q_parent)" = "$catalog_commit"
  test "$(pre_q_grandparent)" = "$catalog_parent_commit"
  require_bound_planning_sha "$repository_root/$brief_path" "$planning_commit"
  assert_pre_q_commit_paths
}

assert_pre_q_index() {
  local index_paths="$1"
  cached_paths > "$index_paths"
  if test ! -s "$index_paths"; then return 0; fi
  /usr/bin/diff -u <(printf '%s\n' 'docs/superpowers/plans/2026-07-27-feature-first-modularization.md') "$index_paths"
}

assert_authoritative_q_state() {
  local root="$1" manifest_file="$root/manifest" raw_status="$root/status.raw" status_file="$root/status" actual_raw="$root/actual.raw" actual_file="$root/actual" expected_file="$root/expected"
  capture_manifest_paths "$manifest_file"
  assert_manifest_line_count 23 "$manifest_file"
  test "$(LC_ALL=C /usr/bin/sort -u "$manifest_file" | /usr/bin/wc -l | /usr/bin/tr -d ' ')" = 23
  capture_status_records "$raw_status" "$status_file"
  /usr/bin/awk -F '\t' -v ledger="$ledger_path" -v plan='docs/superpowers/plans/2026-07-27-feature-first-modularization.md' -v catalog='shared/src/commonMain/composeResources/files/aboutlibraries.json' '$2 != ledger && $2 != plan && $2 != catalog { print }' "$status_file" | LC_ALL=C /usr/bin/sort > "$root/status.filtered"
  frozen_inventory | /usr/bin/awk -F '\t' '{ print $1 "\t" $2 }' | LC_ALL=C /usr/bin/sort > "$root/status.expected"
  /usr/bin/diff -u "$root/status.expected" "$root/status.filtered"
  require_one_ledger
  test "$(/usr/bin/grep -Fc $' M\t'$ledger_path "$status_file")" = 1
  test "$(/usr/bin/awk -F '\t' -v plan='docs/superpowers/plans/2026-07-27-feature-first-modularization.md' '$2 == plan { count++ } END { print count + 0 }' "$status_file")" = 1
  test "$(/usr/bin/grep -Fc $' M\tshared/src/commonMain/composeResources/files/aboutlibraries.json' "$status_file")" = 0
  capture_post_q_actual_hashes "$manifest_file" "$actual_raw" "$actual_file"
  frozen_inventory | /usr/bin/awk -F '\t' '{ print $2 "\t" $3 }' | LC_ALL=C /usr/bin/sort > "$expected_file"
  /usr/bin/diff -u "$expected_file" "$actual_file"
  test "$(/usr/bin/wc -l < "$actual_file" | /usr/bin/tr -d ' ')" = 23
  test "$(/usr/bin/grep -c $'\tPRESENT@' "$actual_file")" = 20
  test "$(/usr/bin/grep -c $'\tDELETED@' "$actual_file")" = 3
}

run_pre_q_gate_core() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local root="$1"
  /bin/mkdir -p -- "$root"
  assert_pre_q_commits
  assert_pre_q_index "$root/index"
  assert_authoritative_q_state "$root"
)

run_pre_q_gate() {
  setopt localoptions noerrexit
  local root core_rc=0 cleanup_rc=0
  local -a residue_paths
  root="$(/usr/bin/mktemp -d "$temp_root/task-5.4-preq.XXXXXX")" || return 1
  run_pre_q_gate_core "$root"
  core_rc=$?
  /bin/rm -rf -- "$root" || cleanup_rc=$?
  residue_paths=("$temp_root"/task-5.4-preq.*(N))
  test "${#residue_paths}" = 0 || cleanup_rc=1
  test "$core_rc" = 0 || return "$core_rc"
  return "$cleanup_rc"
}

apply_pre_q_sabotage() {
  case "${PRE_Q_SABOTAGE:-}" in
    '') ;;
    head) pre_q_head() { print -r -- 0000000000000000000000000000000000000000; } ;;
    parent) pre_q_parent() { print -r -- 0000000000000000000000000000000000000000; } ;;
    brief) require_bound_planning_sha() { return 83; } ;;
    commit) assert_pre_q_commit_paths() { return 84; } ;;
    path) frozen_inventory() { print -r -- $'??\twrong/path.kt\tPRESENT@0000000000000000000000000000000000000000'; } ;;
    status) status_records_producer() { return 86; } ;;
    hash) post_q_actual_hashes() { print -r -- $'wrong\tPRESENT@0000000000000000000000000000000000000000'; } ;;
    ledger) require_one_ledger() { return 88; } ;;
    index) cached_paths_producer() { print -r -- wrong/index; } ;;
    *) return 2 ;;
  esac
}

pre_q_checkpoint_core() (
  emulate -L zsh
  setopt errexit nounset pipefail
  if (( $# == 1 )); then PRE_Q_SABOTAGE="$1"; fi
  apply_pre_q_sabotage
  run_pre_q_gate
)

assert_missing_baseline() {
  local root="$1" option="$2" manifest="$root/missing.manifest" raw="$root/missing.raw" sorted="$root/missing.sorted" rc=0
  print -r -- 'missing/baseline.kt' > "$manifest"
  setopt localoptions noerrexit
  if [[ "$option" = pipefail ]]; then setopt pipefail; else setopt nopipefail; fi
  capture_post_q_actual_hashes "$manifest" "$raw" "$sorted" 2> "$root/missing.err"
  rc=$?
  test "$rc" != 0
  /usr/bin/diff -u <(printf '%s\n' 'MISSING@missing/baseline.kt') "$root/missing.err"
}

checkpoint_pre_q() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local root index_file index_before index_after negative_count=0
  local -a stale_paths
  stale_paths=("$temp_root"/task-5.4-preq-checkpoint.*(N) "$temp_root"/task-5.4-preq.*(N) "$temp_root"/task-5.4-child.*(N))
  (( ${#stale_paths} == 0 )) || /bin/rm -rf -- "${stale_paths[@]}"
  root="$(/usr/bin/mktemp -d "$temp_root/task-5.4-preq-checkpoint.XXXXXX")"
  trap '[[ -n "${root:-}" ]] && /bin/rm -rf -- "$root"' EXIT
  index_file="$(/usr/bin/git -C "$repository_root" rev-parse --git-path index)"
  index_before="$(/usr/bin/shasum -a 256 "$index_file")"
  run_child pre_q_checkpoint_core
  GIT_INDEX_FILE="$root/plan.index" /usr/bin/git -C "$repository_root" read-tree HEAD
  GIT_INDEX_FILE="$root/plan.index" /usr/bin/git -C "$repository_root" add -- docs/superpowers/plans/2026-07-27-feature-first-modularization.md
  GIT_INDEX_FILE="$root/plan.index" run_child pre_q_checkpoint_core
  local sabotage
  for sabotage in head parent brief commit path status hash ledger index; do
    expect_nonzero_child pre_q_checkpoint_core "$sabotage"
    (( negative_count += 1 ))
  done
  expect_nonzero_child naked_early_failure_then_success
  (( negative_count += 1 ))
  assert_missing_baseline "$root" pipefail
  assert_missing_baseline "$root" nopipefail
  index_after="$(/usr/bin/shasum -a 256 "$index_file")"
  /usr/bin/diff -u <(printf '%s\n' "$index_before") <(printf '%s\n' "$index_after")
  /bin/rm -rf -- "$root"
  root=''
  trap - EXIT
  local -a residue_paths
  residue_paths=("$temp_root"/task-5.4-preq-checkpoint.*(N) "$temp_root"/task-5.4-preq.*(N) "$temp_root"/task-5.4-child.*(N))
  test "${#residue_paths}" = 0
  printf 'checkpoint=pre-q empty_index_rc=0 sole_plan_index_rc=0 negatives=%s actual_frozen=23/23 present_deleted=20/3 missing_pipefail_rc=nonzero missing_nopipefail_rc=nonzero residue=0 index_byte_identical=yes\n' "$negative_count"
)

# Checkpoint 3: synthetic post-Q authority proof. No production post-Q state is assumed.
post_manifest_paths() { manifest_paths "$1/plan.md"; }
post_status_records_producer() { /usr/bin/git -C "$1" -c status.renames=false -c core.quotepath=false status --porcelain=v1 -z --untracked-files=all; }
post_head() { /usr/bin/git -C "$1" rev-parse HEAD; }
post_parent() { /usr/bin/git -C "$1" rev-parse HEAD^; }
post_grandparent() { /usr/bin/git -C "$1" rev-parse HEAD^^; }
post_planning() { /usr/bin/git -C "$1" rev-parse HEAD^^^; }
post_index_paths() { /usr/bin/git -C "$1" diff --cached --name-only; }
post_brief() { parse_bound_planning_sha "$1/$brief_path"; }
post_actual_hashes_for() {
  local repo="$1" manifest="$2" endpoint oid
  while IFS= read -r endpoint || [[ -n "$endpoint" ]]; do
    [[ -n "$endpoint" ]] || continue
    if [[ -e "$repo/$endpoint" ]]; then
      oid="$(/usr/bin/shasum -a 256 "$repo/$endpoint" | /usr/bin/awk '{ print $1 }')" || return 1
      print -r -- "$endpoint"$'\t'"PRESENT@$oid"
    elif oid="$(/usr/bin/git -C "$repo" rev-parse "HEAD:$endpoint" 2>/dev/null)"; then
      print -r -- "$endpoint"$'\t'"DELETED@$oid"
    else
      print -u2 -r -- "MISSING@$endpoint"
      return 1
    fi
  done < "$manifest"
}

# This oracle deliberately does not call post_actual_hashes_for or any production helper.
post_q_fixture_oracle() {
  local repo="$1" manifest="$2" endpoint digest oid present=0 deleted=0
  while IFS= read -r endpoint || [[ -n "$endpoint" ]]; do
    [[ -n "$endpoint" ]] || continue
    if [[ -e "$repo/$endpoint" ]]; then
      digest="$(/usr/bin/shasum -a 256 "$repo/$endpoint" | /usr/bin/awk '{ print $1 }')" || return 1
      print -r -- "$endpoint"$'\t'"PRESENT@$digest"
      (( present += 1 ))
    elif oid="$(/usr/bin/git -C "$repo" rev-parse "HEAD:$endpoint" 2>/dev/null)"; then
      print -r -- "$endpoint"$'\t'"DELETED@$oid"
      (( deleted += 1 ))
    else
      print -u2 -r -- "MISSING@$endpoint"
      return 1
    fi
  done < "$manifest"
  test "$present/$deleted" = 20/3
}

make_post_q_fixture() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local repo="$1" manifest path n=0 q
  manifest="$repo/.git/post-q-manifest"
  /bin/mkdir -p -- "$repo"
  /usr/bin/git -C "$repo" init -q
  /usr/bin/git -C "$repo" config user.email fixture@example.invalid
  /usr/bin/git -C "$repo" config user.name fixture
  capture_manifest_paths "$manifest"
  {
    print -r -- '## Task 5.4: Move Settings'
    print -r -- "$manifest_marker"
    print -r -- '```text'
    /bin/cat "$manifest"
    print -r -- '```'
    print -r -- '## Task 6.1: Extract Library Implementation Last'
  } > "$repo/plan.md"
  print -r -- "$brief_path" > "$repo/.gitignore"
  print -r -- 'catalog baseline' > "$repo/catalog.json"
  /bin/mkdir -p -- "$repo/${ledger_path:h}"
  print -r -- 'ledger baseline' > "$repo/$ledger_path"
  while IFS= read -r path; do
    /bin/mkdir -p -- "$repo/${path:h}"
    print -r -- "baseline:$path" > "$repo/$path"
  done < "$manifest"
  /usr/bin/git -C "$repo" add -- . ':!manifest'
  /usr/bin/git -C "$repo" -c commit.gpgsign=false commit -qm planning
  /usr/bin/git -C "$repo" -c tag.gpgSign=false tag post-planning
  print -r -- 'catalog changed' > "$repo/catalog.json"
  /usr/bin/git -C "$repo" add -- catalog.json
  /usr/bin/git -C "$repo" -c commit.gpgsign=false commit -qm catalog
  /usr/bin/git -C "$repo" -c tag.gpgSign=false tag post-catalog
  print -r -- 'P plan-only marker' >> "$repo/plan.md"
  /usr/bin/git -C "$repo" add -- plan.md
  /usr/bin/git -C "$repo" -c commit.gpgsign=false commit -qm P
  /usr/bin/git -C "$repo" -c tag.gpgSign=false tag post-p
  print -r -- 'Q plan-only marker' >> "$repo/plan.md"
  /usr/bin/git -C "$repo" add -- plan.md
  /usr/bin/git -C "$repo" -c commit.gpgsign=false commit -qm Q
  /usr/bin/git -C "$repo" -c tag.gpgSign=false tag post-q
  q="$(post_head "$repo")"
  print -r -- "**Bound planning commit:** \`$q\`" > "$repo/$brief_path"
  print -r -- 'ledger post-Q' > "$repo/$ledger_path"
  while IFS= read -r path; do
    (( n += 1 ))
    if (( n <= 20 )); then print -r -- "post-q:$path" > "$repo/$path"; else /bin/rm -f -- "$repo/$path"; fi
  done < "$manifest"
  post_q_fixture_oracle "$repo" "$manifest" > "$repo/.git/post-q-oracle"
  test -z "$(post_index_paths "$repo")"
)

post_q_assert_commit_paths() {
  local repo="$1"
  /usr/bin/diff -u <(printf '%s\n' plan.md) <(/usr/bin/git -C "$repo" diff --name-only HEAD^ HEAD)
  /usr/bin/diff -u <(printf '%s\n' plan.md) <(/usr/bin/git -C "$repo" diff --name-only HEAD^^ HEAD^)
  /usr/bin/diff -u <(printf '%s\n' catalog.json) <(/usr/bin/git -C "$repo" diff --name-only HEAD^^^ HEAD^^)
}
post_q_assert_ledger() {
  local repo="$1" records="$2"
  test "$(/usr/bin/grep -Fc $' M\t'$ledger_path "$records")" = 1
}
post_q_assert_clean_plan_catalog() {
  local records="$2"
  test "$(/usr/bin/awk -F '\t' '$2 == "plan.md" || $2 == "catalog.json" { count++ } END { print count + 0 }' "$records")" = 0
}
post_q_assert_status() {
  local repo="$1" manifest="$2" records="$3" expected="$4"
  /usr/bin/perl -0ne 'for (split /\0/) { next unless length; /^(.{2}) (.*)\z/s or die "malformed porcelain\n"; print "$1\t$2\n"; }' "$records" > "$records.parsed"
  /usr/bin/awk -F '\t' -v ledger="$ledger_path" '$2 != ledger { print }' "$records.parsed" | LC_ALL=C /usr/bin/sort > "$records.filtered"
  /usr/bin/awk '{ if (NR <= 20) print " M\t" $0; else print " D\t" $0 }' "$manifest" | LC_ALL=C /usr/bin/sort > "$expected"
  /usr/bin/diff -u "$expected" "$records.filtered"
  post_q_assert_ledger "$repo" "$records.parsed"
  post_q_assert_clean_plan_catalog "$repo" "$records.parsed"
}
post_q_assert_hashes() {
  local repo="$1" manifest="$2" root="$3" supplied="$1/.git/post-q-oracle"
  post_actual_hashes_for "$repo" "$manifest" > "$root/actual"
  post_q_fixture_oracle "$repo" "$manifest" > "$root/oracle"
  /usr/bin/diff -u "$supplied" "$root/oracle"
  /usr/bin/diff -u "$supplied" "$root/actual"
  test "$(/usr/bin/wc -l < "$root/actual" | /usr/bin/tr -d ' ')" = 23
  test "$(/usr/bin/grep -c $'\tPRESENT@' "$root/actual")" = 20
  test "$(/usr/bin/grep -c $'\tDELETED@' "$root/actual")" = 3
}
run_post_q_gate_core() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local repo="$1" root="$2" manifest records
  manifest="$root/manifest"
  records="$root/status.raw"
  /bin/mkdir -p -- "$root"
  post_manifest_paths "$repo" > "$manifest"
  test "$(/usr/bin/wc -l < "$manifest" | /usr/bin/tr -d ' ')" = 23
  test "$(post_head "$repo")" = "$(/usr/bin/git -C "$repo" rev-parse post-q)"
  test "$(post_parent "$repo")" = "$(/usr/bin/git -C "$repo" rev-parse post-p)"
  test "$(post_grandparent "$repo")" = "$(/usr/bin/git -C "$repo" rev-parse post-catalog)"
  test "$(post_planning "$repo")" = "$(/usr/bin/git -C "$repo" rev-parse post-planning)"
  post_q_assert_commit_paths "$repo"
  test "$(post_brief "$repo")" = "$(post_head "$repo")"
  test -z "$(post_index_paths "$repo")"
  post_status_records_producer "$repo" > "$records"
  post_q_assert_status "$repo" "$manifest" "$records" "$root/expected"
  post_q_assert_hashes "$repo" "$manifest" "$root"
)
run_post_q_gate() {
  setopt localoptions noerrexit
  local repo="$1" root="$2" rc=0
  run_post_q_gate_core "$repo" "$root"
  rc=$?
  return "$rc"
}

apply_post_q_sabotage() {
  case "${POST_Q_SABOTAGE:-}" in
    '') ;;
    head) post_head() { print -r -- bad-head; } ;;
    parent) post_parent() { print -r -- bad-parent; } ;;
    grandparent) post_grandparent() { print -r -- bad-grandparent; } ;;
    planning) post_planning() { print -r -- bad-planning; } ;;
    manifest) post_manifest_paths() { return 91; } ;;
    status) post_status_records_producer() { return 92; } ;;
    hashes) post_actual_hashes_for() { return 93; } ;;
    oracle) post_q_fixture_oracle() { return 94; } ;;
    index) post_index_paths() { print -r -- staged; } ;;
    brief) post_brief() { print -r -- bad-brief; } ;;
    paths) post_q_assert_commit_paths() { return 95; } ;;
    ledger) post_q_assert_ledger() { return 96; } ;;
    clean) post_q_assert_clean_plan_catalog() { return 97; } ;;
    *) return 2 ;;
  esac
}
post_q_proof_core() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local repo="$1" root="$2"
  if (( $# == 3 )); then POST_Q_SABOTAGE="$3"; fi
  apply_post_q_sabotage
  run_post_q_gate "$repo" "$root"
)
post_q_mutate() {
  local repo="$1" kind="$2" first
  first="$(post_manifest_paths "$repo" | /usr/bin/awk 'NR == 1 { print; exit }')"
  case "$kind" in
    wrong-q-lineage) /usr/bin/git -C "$repo" -c tag.gpgSign=false tag -f post-p post-catalog >/dev/null ;;
    wrong-p-lineage) /usr/bin/git -C "$repo" -c tag.gpgSign=false tag -f post-catalog post-planning >/dev/null ;;
    wrong-catalog-lineage) /usr/bin/git -C "$repo" -c tag.gpgSign=false tag -f post-planning post-catalog >/dev/null ;;
    wrong-commit-path) print -r -- bad >> "$repo/catalog.json" ;;
    wrong-brief-bound) print -r -- '**Bound planning commit:** `0000000000000000000000000000000000000000`' > "$repo/$brief_path" ;;
    missing-ledger) /bin/rm -f -- "$repo/$ledger_path" ;;
    extra-ledger) print -r -- extra > "$repo/extra-ledger" ;;
    endpoint-status) /usr/bin/git -C "$repo" add -- "$first" ;;
    endpoint-hash) print -r -- tampered >> "$repo/$first" ;;
    restore-deleted) post_manifest_paths "$repo" | /usr/bin/awk 'NR == 21 { print; exit }' | while IFS= read -r p; do print -r -- restored > "$repo/$p"; done ;;
    nonempty-index) /usr/bin/git -C "$repo" add -- "$first" ;;
    missing-baseline) /usr/bin/perl -0pi -e 's/feature\/settings\/build\.gradle\.kts/missing\/baseline\.kt/' "$repo/plan.md" ;;
    duplicate-close) /usr/bin/perl -0pi -e 's/(\*\*Task 5\.4 implementation manifest \(23 endpoints\):\*\*\n```text\n)/$1```\n/' "$repo/plan.md" ;;
    plan-dirty) print -r -- dirty >> "$repo/plan.md" ;;
    catalog-dirty) print -r -- dirty >> "$repo/catalog.json" ;;
    *) return 2 ;;
  esac
}
checkpoint_post_q() (
  emulate -L zsh
  setopt errexit nounset pipefail
  local root repo case_root control='' mutation='' control_count=0 mutation_count=0 index_file index_before index_after
  local -a stale_paths
  stale_paths=("$temp_root"/task-5.4-postq-*(N))
  (( ${#stale_paths} == 0 )) || /bin/rm -rf -- "${stale_paths[@]}"
  root="$(/usr/bin/mktemp -d "$temp_root/task-5.4-postq-proof.XXXXXX")"
  trap '[[ -n "${root:-}" ]] && /bin/rm -rf -- "$root"' EXIT
  index_file="$(/usr/bin/git -C "$repository_root" rev-parse --git-path index)"
  index_before="$(/usr/bin/shasum -a 256 "$index_file")"
  repo="$root/normal"
  make_post_q_fixture "$repo"
  run_child post_q_proof_core "$repo" "$root/normal-gate"
  for control in head parent grandparent planning manifest status hashes oracle index brief paths ledger clean; do
    expect_nonzero_child post_q_proof_core "$repo" "$root/control-$control" "$control"
    (( control_count += 1 ))
  done
  for mutation in wrong-q-lineage wrong-p-lineage wrong-catalog-lineage wrong-commit-path wrong-brief-bound missing-ledger extra-ledger endpoint-status endpoint-hash restore-deleted nonempty-index missing-baseline duplicate-close plan-dirty catalog-dirty; do
    case_root="$root/mutation-$mutation"
    make_post_q_fixture "$case_root"
    post_q_mutate "$case_root" "$mutation"
    expect_nonzero_child post_q_proof_core "$case_root" "$root/mutation-gate-$mutation"
    (( mutation_count += 1 ))
  done
  expect_nonzero_child naked_early_failure_then_success
  expect_nonzero_child post_q_proof_core "$repo" "$root/external" status
  index_after="$(/usr/bin/shasum -a 256 "$index_file")"
  /usr/bin/diff -u <(printf '%s\n' "$index_before") <(printf '%s\n' "$index_after")
  /bin/rm -rf -- "$root"
  root=''
  trap - EXIT
  stale_paths=("$temp_root"/task-5.4-postq-*(N))
  test "${#stale_paths}" = 0
  printf 'checkpoint=post-q normal_rc=0 controls=%s/13_nonzero mutations=%s/15_nonzero early_failure=nonzero external_sabotage=nonzero actual_oracle=23/23 present_deleted=20/3 residue=0 index_byte_identical=yes\n' "$control_count" "$mutation_count"
)

main() {
  local mode="${1:-parser}"
  case "$mode" in
    parser) checkpoint_parser ;;
    pre-q) checkpoint_parser && checkpoint_pre_q ;;
    post-q) checkpoint_parser && checkpoint_pre_q && checkpoint_post_q ;;
    *) print -u2 -- "usage: $0 [parser|pre-q|post-q]"; return 2 ;;
  esac
}

main "$@"
```

**Execution amendment and closeout.** Before Q, run only the real pre-Q gate and the proof modes; it permits an empty real index or a temporary index containing only this plan and never mutates the real index. Independently review the Q plan-only diff before staging. Commit Q with this plan as its sole path, then rebind the ignored brief to Q. Before implementation, run the Q production post gate: it requires `Q^ == P`, the catalog lineage above, Q and P plan-only, catalog-only maintenance, a Q-bound brief, clean plan/catalog, empty index, exact 23 endpoint records plus the sole ledger, and no missing endpoint. Until Q is committed, use only the synthetic post-Q fixture; do not claim the current production repository passes a post-Q gate.

I is the direct implementation child of Q and owns exactly the preserved manifest endpoints. After I, perform the specified evidence closeout and independently verify scope, resource/test ownership, the 23-path inventory, the Q-bound brief, the implementation commit relationship, strict OpenSpec validation, one-file plan diff hygiene before staging this amendment, and an empty real index. Historical parser/P/post-P evidence does not authorize additional catalog, rebind, or lifecycle changes.
