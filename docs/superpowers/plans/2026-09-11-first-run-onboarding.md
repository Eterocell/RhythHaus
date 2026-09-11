# First-Run Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add durable, localized first-run guidance for RhythHaus that explains platform-specific local music behavior and can be reopened from Settings without mutating library or playback state.

**Architecture:** `:feature:onboarding` owns a stateless four-page Compose screen and its resources. `:shared` owns the schema-versioned preference, platform guidance projection, lifecycle gate, canonical route payload, and Settings-to-onboarding navigation; existing library and playback initialization continues independently behind the presentation gate.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Preferences DataStore, Koin, MIUIX/Core UI components, Kotlin test, Compose UI test, OpenSpec.

**Spec:** `openspec/changes/first-run-onboarding/design.md` and `openspec/changes/first-run-onboarding/specs/first-run-onboarding/spec.md`

## Global Constraints

- Onboarding schema version is `1`; missing, older, corrupt, or unreadable state requires onboarding.
- Skip and Finish persist completion before the normal library becomes interactive.
- Review mode never writes completion and returns to the same Settings destination.
- Onboarding never requests permissions, launches a picker, mutates sources or scans, or changes playback.
- `:shared` remains the only composition, lifecycle, and cross-feature navigation owner.
- `:feature:onboarding` must not depend on `:shared`, library, playback, database, DataStore, or platform APIs.
- Android, iOS, and macOS/JVM remain the only supported platforms.
- English and Simplified Chinese are required.
- Visible content and controls must remain reachable at 600×400 dp.
- No dependency-version or SQLDelight schema change is permitted.

---

### Task 1: Stateless Onboarding Feature

**Files:**
- Modify: `settings.gradle.kts`
- Create: `feature/onboarding/build.gradle.kts`
- Create: `feature/onboarding/src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingScreen.kt`
- Create: `feature/onboarding/src/commonMain/composeResources/values/strings.xml`
- Create: `feature/onboarding/src/commonMain/composeResources/values-zh/strings.xml`
- Create: `feature/onboarding/src/commonTest/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPolicyTest.kt`
- Create: `feature/onboarding/src/jvmTest/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingScreenJvmTest.kt`
- Create: `feature/onboarding/src/jvmTest/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingResourceOwnershipJvmTest.kt`

**Interfaces:**
- Consumes: `HausColors`, `RhythHausTopAppBar`, MIUIX buttons/surfaces, Compose resources.
- Produces: `OnboardingPlatformGuidance`, `OnboardingScreen(pageIndex, platform, saving, completionError, reviewMode, callbacks)`.

- [ ] **Step 1: Add the compilable module test scaffold**

Add `include(":feature:onboarding")`. Copy the build structure from `:feature:settings`, using namespace `com.eterocell.rhythhaus.onboarding` and resource namespace `rhythhaus.feature.onboarding.generated.resources`. Dependencies are limited to `:core:ui`, Compose runtime/UI/foundation/resources, Material icons, MIUIX UI, and test dependencies. Add empty EN/ZH `<resources>` roots so resource generation is valid; do not add production Kotlin declarations yet.

- [ ] **Step 2: Write the failing policy tests**

Define and test the closed page policy before UI code:

```kotlin
class OnboardingPolicyTest {
    @Test fun schemaOneHasFourOrderedPages() {
        assertEquals(
            listOf(LocalFirst, AddMusic, ScanAndFormats, Recovery),
            onboardingPages,
        )
    }

    @Test fun nextAndBackStayWithinTheFourPageBoundary() {
        assertEquals(1, nextOnboardingPage(0))
        assertEquals(3, nextOnboardingPage(3))
        assertEquals(0, previousOnboardingPage(0))
        assertEquals(2, previousOnboardingPage(3))
    }
}
```

- [ ] **Step 3: Run policy tests to verify RED**

Run:

```bash
./gradlew :feature:onboarding:jvmTest --tests '*OnboardingPolicyTest' --configuration-cache
```

Expected: test compilation fails because the policy declarations do not exist.

- [ ] **Step 4: Add the immutable contract**

Implement the feature-safe API:

```kotlin
public enum class OnboardingPlatformGuidance { Android, IOS, MacOS }

internal enum class OnboardingPage { LocalFirst, AddMusic, ScanAndFormats, Recovery }
internal val onboardingPages = OnboardingPage.entries
internal fun nextOnboardingPage(index: Int) = (index + 1).coerceAtMost(onboardingPages.lastIndex)
internal fun previousOnboardingPage(index: Int) = (index - 1).coerceAtLeast(0)

@Composable
public fun OnboardingScreen(
    pageIndex: Int,
    platform: OnboardingPlatformGuidance,
    saving: Boolean,
    completionError: String?,
    reviewMode: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Clamp only for rendering defensiveness; route construction remains responsible for valid `0..3` payloads. Review mode shows Close and never Skip/Finish. First-run mode shows Skip on all pages, Next on pages `0..2`, and Finish on page `3`. While `saving`, completion actions are disabled and expose disabled semantics.

- [ ] **Step 5: Implement localized pages and responsive layout**

Use a safe-content-padded vertically scrollable content column plus a footer that remains part of the same scroll chain at 600×400 dp. Give the root, page, progress, error, and each action stable test tags. Render only the current page—do not keep hidden pages composed with alpha.

English and Chinese resources must cover:

- local-only/no streaming/no upload;
- Android folder access, iOS `Documents/RhythHaus` copy-or-scan-in-place policy, macOS referenced folder policy;
- WAV, AIFF, AU, MP3, M4A/AAC, FLAC, OGG;
- metadata/artwork scan and skipped-item reports;
- moved/deleted/access-lost recovery, rescan, and Remove missing boundaries;
- page progress and Back/Next/Skip/Finish/Close/retryable save error copy.

- [ ] **Step 6: Add observable UI regressions**

Compose tests must prove:

```kotlin
@Test fun inactivePagesExposeNoSemantics()
@Test fun firstRunActionsDispatchExactlyOnce()
@Test fun reviewShowsCloseWithoutSkipOrFinish()
@Test fun savingDisablesCompletionActions()
@Test fun androidIosAndMacGuidanceAreDistinct()
@Test fun chineseLocaleRendersChineseHeadingAndActions()
@Test fun actionsRemainReachableAt600By400Dp()
```

Any locale mutation must restore the prior locale in `try/finally`.

- [ ] **Step 7: Run Task 1 GREEN checks**

Run:

```bash
./gradlew :feature:onboarding:jvmTest :feature:onboarding:compileKotlinIosSimulatorArm64 --configuration-cache
./gradlew :feature:onboarding:spotlessCheck --configuration-cache
```

Expected: all feature tests and compilation pass.

- [ ] **Step 8: Commit Task 1**

```bash
git add settings.gradle.kts feature/onboarding
git commit -m "feat(onboarding): add first-run guidance screen"
```

### Task 2: Durable Eligibility and Platform Projection

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.kt`
- Create: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.android.kt`
- Create: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.ios.kt`
- Create: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStore.jvm.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.kt`
- Create: `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.android.kt`
- Create: `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.ios.kt`
- Create: `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatform.jvm.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPreferenceStoreJvmTest.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingPlatformTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`

**Interfaces:**
- Consumes: `DataStore<Preferences>` and platform application-support paths.
- Produces: `OnboardingEligibility`, `OnboardingPreferenceStore`, `createOnboardingPreferenceStore()`, `currentOnboardingPlatform()`.

- [ ] **Step 1: Write failing store tests**

Use a temporary Preferences DataStore and test:

```kotlin
@Test fun missingAndOlderVersionsRequireOnboarding()
@Test fun currentAndNewerVersionsAreCompleted()
@Test fun markCompletedWritesSchemaOneIdempotently()
@Test fun corruptFileRecoversToRequiredAndRemainsWritable()
@Test fun readFailureEmitsRequiredInsteadOfCancellingCollector()
@Test fun onboardingStorageDoesNotReadOrOverwriteThemeOrPlaybackKeys()
```

- [ ] **Step 2: Run store tests to verify RED**

```bash
./gradlew :shared:jvmTest --tests '*OnboardingPreferenceStoreJvmTest' --configuration-cache
```

Expected: compilation fails because the store does not exist.

- [ ] **Step 3: Implement the schema-versioned store**

```kotlin
internal const val CurrentOnboardingSchemaVersion = 1
private val CompletedSchemaVersionKey = intPreferencesKey("completed_schema_version")

public enum class OnboardingEligibility { Loading, Required, Completed }

public interface OnboardingPreferenceStore {
    public val eligibility: Flow<OnboardingEligibility>
    public suspend fun markCurrentVersionCompleted()
}

internal class DataStoreOnboardingPreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : OnboardingPreferenceStore {
    override val eligibility =
        dataStore.data
            .map { preferences ->
                if ((preferences[CompletedSchemaVersionKey] ?: 0) >= CurrentOnboardingSchemaVersion)
                    OnboardingEligibility.Completed
                else OnboardingEligibility.Required
            }
            .catch { failure ->
                if (failure is CancellationException) throw failure
                emit(OnboardingEligibility.Required)
            }
            .onStart { emit(OnboardingEligibility.Loading) }

    override suspend fun markCurrentVersionCompleted() {
        dataStore.edit { preferences ->
            val existing = preferences[CompletedSchemaVersionKey] ?: 0
            if (existing < CurrentOnboardingSchemaVersion) {
                preferences[CompletedSchemaVersionKey] = CurrentOnboardingSchemaVersion
            }
        }
    }
}
```

Do not catch `CancellationException`; the flow fallback must rethrow cancellation before emitting Required.

- [ ] **Step 4: Add isolated platform factories**

Each actual uses `onboarding.preferences_pb`, `ReplaceFileCorruptionHandler { emptyPreferences() }`, `SupervisorJob`, and the same root-directory convention as the existing theme store. Android may use `LibraryDatabaseContext.applicationContext` because Shared already owns that host bridge; do not make the onboarding feature depend on it.

- [ ] **Step 5: Add platform projection and Koin binding**

```kotlin
internal enum class OnboardingPlatform { Android, IOS, MacOS }
internal expect fun currentOnboardingPlatform(): OnboardingPlatform

internal fun OnboardingPlatform.toGuidance() = when (this) {
    Android -> OnboardingPlatformGuidance.Android
    IOS -> OnboardingPlatformGuidance.IOS
    MacOS -> OnboardingPlatformGuidance.MacOS
}
```

Register `single<OnboardingPreferenceStore> { createOnboardingPreferenceStore() }` in the Shared Koin module and add `implementation(projects.feature.onboarding)` to `shared`.

- [ ] **Step 6: Run Task 2 GREEN checks**

```bash
./gradlew :shared:jvmTest --tests '*Onboarding*' --tests '*RhythHausDiTest' --configuration-cache
./gradlew :shared:compileKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinJvm --configuration-cache
```

Expected: store, projection, DI, and all target compilations pass.

- [ ] **Step 7: Commit Task 2**

```bash
git add shared feature/onboarding/build.gradle.kts
git commit -m "feat(onboarding): persist first-run eligibility"
```

### Task 3: Canonical Lifecycle and Back Semantics

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- Create: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/onboarding/AppOnboardingLifecycleJvmTest.kt`
- Modify: affected Shared shell/route adapter test call sites.

**Interfaces:**
- Consumes: Task 1 `OnboardingScreen`, Task 2 store and guidance.
- Produces: `OnboardingLaunchMode`, `LibraryRoute.Onboarding(launchMode, pageIndex)`, lifecycle completion callbacks.

- [ ] **Step 1: Write failing navigation tests**

Cover canonical route behavior:

```kotlin
@Test fun firstRunPageZeroSuppressesRouteBack()
@Test fun firstRunLaterPageBackReplacesTopWithPreviousPage()
@Test fun reviewPageZeroPopsToExactSettingsEntry()
@Test fun onboardingNeverPermitsNowPlayingBar()
@Test fun onboardingRendersAsActiveOverlayInCompactAndWideModes()
@Test fun invalidPagePayloadIsRejectedBeforeComposition()
```

The route payload is:

```kotlin
internal enum class OnboardingLaunchMode { FirstRun, Review }
internal data class Onboarding(
    val launchMode: OnboardingLaunchMode,
    val pageIndex: Int,
) : LibraryRoute
```

- [ ] **Step 2: Run navigation tests to verify RED**

```bash
./gradlew :shared:jvmTest --tests '*LibraryNavigationTest' --configuration-cache
```

Expected: compilation/test failure because the route policy is absent.

- [ ] **Step 3: Extend canonical navigation**

Update every exhaustive `when (LibraryRoute)` branch. For `pageIndex > 0`, Back must produce a route preview whose next stack is `replaceTop(copy(pageIndex = pageIndex - 1))`. At Review page zero, use ordinary pop. At FirstRun page zero, return `Suppressed`. Next uses `replaceTopRoute(copy(pageIndex = pageIndex + 1))`; bounds are validated before construction. Do not install a feature-local Back handler.

- [ ] **Step 4: Write failing lifecycle tests**

Use fake preference, picker, permission, scanner, repository, and playback collaborators to prove:

```kotlin
@Test fun loadingShowsOnlyNonInteractiveGateWhileStartupContinues()
@Test fun requiredPushesFirstRunPageZeroWithoutInteractiveLibraryFlash()
@Test fun completedStartsAtNormalLibrary()
@Test fun skipPersistsBeforeHomeBecomesActive()
@Test fun failedCompletionWriteKeepsOnboardingAndShowsError()
@Test fun onboardingNavigationInvokesNoMusicOrPermissionCallbacks()
@Test fun reviewingOnboardingPreservesPlayingStateAndQueue()
```

- [ ] **Step 5: Implement App lifecycle gating**

Inject `OnboardingPreferenceStore` from Koin and collect eligibility with initial `Loading`. Keep all existing `LaunchedEffect` initialization outside the presentation branch. Inside the existing theme/provider boundary, render `OnboardingLoadingSurface` only for `Loading`. For `Required` and `Completed`, preserve the complete existing `LibraryHomeScreen` argument list and add four exact arguments: `initialOnboarding = OnboardingLaunchMode.FirstRun` only for `Required` (otherwise `null`), `onboardingSaving`, `onboardingCompletionError`, and `onCompleteOnboarding = ::persistOnboardingCompletion`.

`persistOnboardingCompletion` sets saving, clears the prior error, calls `markCurrentVersionCompleted`, and relies on observed `Completed` eligibility to reveal Home. On failure it publishes localized error and leaves eligibility/route unchanged. Re-throw cancellation.

The first-run route is inserted exactly once when the corresponding `LibraryAppState` is created; eligibility recomposition must not stack duplicate routes.

- [ ] **Step 6: Render and connect onboarding route**

`LibraryRouteOverlays` maps the current immutable route payload into Task 1 callbacks. First-run Skip and Finish call Shared completion. Review Close requests ordinary route Back. Review Next/Back only replace the top payload. Do not pass picker, permission, scan, library-mutation, or playback callbacks into `OnboardingScreen`.

- [ ] **Step 7: Run Task 3 GREEN checks**

```bash
./gradlew :shared:jvmTest --tests '*LibraryNavigationTest' --tests '*AppOnboardingLifecycleJvmTest' --tests '*RouteAdapterJvmTest' --configuration-cache
./gradlew :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache
```

Expected: lifecycle, route, call-site, and platform compilation checks pass.

- [ ] **Step 8: Commit Task 3**

```bash
git add shared androidApp desktopApp
git commit -m "feat(onboarding): gate first-run presentation"
```

### Task 4: Settings Re-entry

**Files:**
- Modify: `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/commonMain/composeResources/values/strings.xml`
- Modify: `feature/settings/src/commonMain/composeResources/values-zh/strings.xml`
- Modify: `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt`
- Modify: `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsResourceOwnershipJvmTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/SettingsRouteAdapterJvmTest.kt`

**Interfaces:**
- Consumes: `onReviewOnboarding: () -> Unit` in Settings and Task 3 route.
- Produces: one accessible Settings row that pushes Review page zero.

- [ ] **Step 1: Write failing Settings behavior tests**

```kotlin
@Test fun reviewOnboardingRowIsAccessibleAndDispatchesExactlyOnce()
@Test fun reviewOnboardingRowIsReachableInCompactHeight()
@Test fun settingsAdapterPushesReviewPageZero()
@Test fun closingReviewReturnsToSameSettingsDestination()
```

Assert role, localized content description, click action, and route identity; do not assert implementation class names.

- [ ] **Step 2: Run Settings tests to verify RED**

```bash
./gradlew :feature:settings:jvmTest --tests '*SettingsScreenSemanticsJvmTest' :shared:jvmTest --tests '*SettingsRouteAdapterJvmTest' --configuration-cache
```

Expected: the row/action is absent.

- [ ] **Step 3: Add the Settings row and Shared adapter**

Add `onReviewOnboarding` to `SettingsScreen`. Render a `Review onboarding` / `查看使用指南` row near About using the existing row style, keyboard/click semantics, and a stable `settings-review-onboarding` tag. Shared passes:

```kotlin
onReviewOnboarding = {
    pushRoute(LibraryRoute.Onboarding(OnboardingLaunchMode.Review, pageIndex = 0))
}
```

Do not expose onboarding completion state to Settings.

- [ ] **Step 4: Synchronize resource ledgers and call sites**

Update exact English/Chinese resource ownership expectations and every production/test `SettingsScreen` invocation. Do not add compatibility overloads or default callbacks to hide missed call sites.

- [ ] **Step 5: Run Task 4 GREEN checks**

```bash
./gradlew :feature:settings:jvmTest :shared:jvmTest --tests '*SettingsRouteAdapterJvmTest' --configuration-cache
./gradlew :feature:settings:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 --configuration-cache
```

Expected: Settings, route, locale, and iOS compilation pass.

- [ ] **Step 6: Commit Task 4**

```bash
git add feature/settings shared
git commit -m "feat(settings): add onboarding review entry"
```

### Task 5: Cross-Platform Acceptance and Lifecycle Closeout

**Files:**
- Modify: `openspec/changes/first-run-onboarding/tasks.md`
- Modify: `progress.md`
- Modify: `roadmap.md`
- Synchronize: `openspec/specs/first-run-onboarding/spec.md`
- Archive: `openspec/changes/archive/2026-09-11-first-run-onboarding/`

**Interfaces:**
- Consumes: completed Tasks 1–4.
- Produces: verified and archived capability plus exact release/manual evidence.

- [ ] **Step 1: Run focused behavioral verification**

```bash
./gradlew :feature:onboarding:jvmTest :feature:settings:jvmTest :shared:jvmTest :androidApp:testDebugUnitTest --rerun-tasks --configuration-cache
./gradlew :feature:onboarding:compileKotlinIosSimulatorArm64 :feature:settings:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: all focused tests and target compilations pass. If the known unrelated Shared timeout recurs, run the exact onboarding selectors separately and record the baseline blocker without claiming the aggregate pass.

- [ ] **Step 2: Run independent quality gates**

```bash
./gradlew spotlessApply --configuration-cache
./gradlew spotlessCheck --configuration-cache
./gradlew detekt --configuration-cache
./gradlew architectureCheck --configuration-cache
```

Expected: each command independently succeeds.

- [ ] **Step 3: Smoke-test actual surfaces**

Desktop/JVM minimum:

1. Point the onboarding preference factory at a disposable profile or remove only its disposable test profile.
2. Launch the actual desktop app.
3. Verify page 0 appears before an interactive library, all four pages and actions are reachable at 600×400, Skip/Finish survives restart, and Settings reopens Review at page 0.
4. Start playback from a real available track, open Settings → Review onboarding, and confirm playback state/queue continues unchanged.
5. Switch English/Chinese and light/dark appearance; inspect headings, action reachability, clipping, and semantics.

On available Android/iOS targets, repeat first install, restart suppression, Settings re-entry, safe-area behavior, and Back policy. Do not alter a user's existing library to manufacture evidence. Record unavailable targets as explicit manual release gaps.

- [ ] **Step 4: Run full repository and OpenSpec verification**

```bash
./init.sh
openspec validate first-run-onboarding --strict
openspec validate --specs
git diff --check
```

Expected: all pass, or exact unrelated baseline blockers are recorded with focused changed-path evidence.

- [ ] **Step 5: Perform review against the specification**

Review every scenario in `specs/first-run-onboarding/spec.md` against production behavior and tests. Explicitly inspect every `LibraryRoute` exhaustive branch, every `SettingsScreen` call site, preference-file isolation, cancellation propagation, inactive semantics, and platform wording. Repair findings through focused RED/GREEN tests before proceeding.

- [ ] **Step 6: Update lifecycle records and archive**

Check all OpenSpec tasks only after their evidence exists. Update `progress.md` with route, exact commands/results, manual evidence, blockers, and next owner. Mark the roadmap First-run onboarding row Completed only if required acceptance is met; otherwise distinguish completed implementation from remaining device evidence. Synchronize the delta spec, validate canonical specs, archive with the repository OpenSpec command, and verify `openspec list --json` no longer reports the change.

- [ ] **Step 7: Commit completion**

```bash
git add openspec docs/superpowers/plans/2026-09-11-first-run-onboarding.md progress.md roadmap.md
git commit -m "chore: finalize first-run onboarding"
```

Record the actual implementation and lifecycle commit hashes in `progress.md`.
