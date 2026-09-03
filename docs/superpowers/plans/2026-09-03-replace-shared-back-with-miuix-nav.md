# Replace Shared Back with Miuix Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `miuix-nav:0.9.4-rc01` the sole predictive Back, swipe, and presentation-stack authority for RhythHaus while preserving domain ownership, route identity, feature boundaries, restoration, and platform behavior.

**Architecture:** Shared remains the application composition root, but its current `LibraryNavigationStack` and custom Back arbiter are replaced by one serializable `NavBackStack<AppNavKey>` rendered by Miuix `NavDisplay`. Every visible dismissible surface becomes a typed stack entry; feature implementations emit callback-first requests and never depend on Miuix. Miuix owns Back gesture progress, cancellation, commit, transition settling, and approved swipe dismissal; Shared performs only typed stack operations and domain cleanup consequences.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `miuix-nav:0.9.4-rc01`, Kotlin Serialization, Miuix `NavDisplay`, Android/iOS/Desktop predictive Back, Compose UI/JVM tests, Gradle architecture checks.

**Spec:** `openspec/changes/replace-shared-back-with-miuix-nav/design.md` and `openspec/changes/replace-shared-back-with-miuix-nav/specs/miuix-owned-app-back-navigation/spec.md`

## Global Constraints

- `:shared` remains the only app-shell composition root and owns the canonical app-wide stack/controller; `:feature:library:impl` does not own app navigation.
- Feature implementations SHALL NOT depend on `miuix-nav`, `:shared`, or another feature implementation.
- Every visible dismissible surface SHALL have exactly one canonical stack entry; no independent route/overlay compatibility mirror is allowed.
- Miuix types remain inside Shared; feature contracts stay callback-first.
- `AppNavKey` types SHALL be serializable and content keys SHALL include a stable appearance identity.
- Back itself SHALL perform only one canonical top-entry pop; domain cleanup and invalidation are separate consequences/commands.
- No second Shared platform Back handler, predictive session, feature dismissal registration, or custom edge-swipe owner remains after cutover.
- No database, scanner, playback-engine, or unrelated state-ownership migration is allowed.
- Stop immediately if Miuix serialization, iOS dispatch, route identity, or exact overlay behavior cannot satisfy the approved spec.

---

## Task 1: Freeze behavior and add the navigation dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt`
- Test: `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt`

**Interfaces:**
- Consumes: current `LibraryNavigationStack`, `LibraryAppState`, feature dismissal, selection, and Now Playing behavior.
- Produces: characterization evidence and a resolved `top.yukonga.miuix.kmp:miuix-nav:0.9.4-rc01` dependency.

- [ ] **Step 1: Write characterization tests**

  Cover route push/pop/replace, equal-route identity, modal-before-edit, selection, Now Playing, root behavior, invalid playlist deletion, predictive cancellation/commit, and compact/wide route policy using the existing test fixtures. Assert observable state and transition decisions, not implementation names.

- [ ] **Step 2: Run the characterization suite**

  Run:

  ```bash
  ./gradlew :shared:jvmTest --tests '*LibraryNavigationTest*' --tests '*PlaylistBackPolicyJvmTest*' --configuration-cache
  ```

  Expected: PASS on the pre-migration behavior.

- [ ] **Step 3: Add the dependency**

  Add the version-catalog alias at `0.9.4-rc01` and add it only to Shared’s implementation dependencies. Do not add `androidx.navigation3` or expose Miuix through feature APIs.

- [ ] **Step 4: Verify resolution**

  Run:

  ```bash
  ./gradlew :shared:dependencies --configuration-cache
  ```

  Expected: Miuix Nav resolves at `0.9.4-rc01`, and no `androidx.navigation3` dependency is introduced.

## Task 2: Define serializable keys and typed stack operations

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/navigation/AppNavKey.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/navigation/AppNavigator.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/navigation/AppNavKeyTest.kt`
- Create: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/navigation/AppNavigatorTest.kt`

**Interfaces:**
- Consumes: Miuix `NavKey`, `NavBackStack`, and current `LibraryRoute` payloads.
- Produces: `@Serializable sealed interface AppNavKey : NavKey`, `AppNavigator.push`, `pop`, `replaceTop`, `popToRoot`, and stable key/content identity.

- [ ] **Step 1: Write failing key tests**

  Define tests for every base route and overlay payload, serialization round-trips, typed page/destination data, root underflow, fresh appearance identity on re-presentation, and predecessor preservation. The tests must reject route-value-only content identity.

- [ ] **Step 2: Run focused tests to confirm red**

  Run:

  ```bash
  ./gradlew :shared:jvmTest --tests '*AppNavKeyTest*' --tests '*AppNavigatorTest*' --configuration-cache
  ```

  Expected: FAIL because the new key/controller types are absent.

- [ ] **Step 3: Implement the key hierarchy**

  Use strongly typed serializable route/overlay keys. Keep domain data out of keys except the stable identifiers needed to reconstruct presentation. Include a monotonic appearance token in every re-presentable destination or overlay key. Implement a value-derived Miuix `contentKey` that distinguishes equal route values and remains stable through restoration.

- [ ] **Step 4: Implement typed stack operations**

  Wrap one `NavBackStack<AppNavKey>`. `push` appends, `pop` removes only the final entry when size exceeds one, `replaceTop` replaces with a fresh key identity, and `popToRoot` preserves the root. Do not create a second list or mirror.

- [ ] **Step 5: Run focused tests to confirm green**

  Run the command from Step 2. Expected: PASS.

## Task 3: Install the Shared-root Miuix renderer

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Create/modify: Shared navigation entry-provider source adjacent to `App.kt` following repository file-size/convention constraints.
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/navigation/AppNavDisplayJvmTest.kt`

**Interfaces:**
- Consumes: `AppNavigator`, `AppNavKey`, existing Shared dependency injection and route adapters.
- Produces: one root `NavDisplay(backStack = navigator.backStack, onBack = { navigator.pop() })` and a concrete entry provider for every key.

- [ ] **Step 1: Write failing renderer/provider tests**

  Assert every concrete key has an entry, duplicate content keys fail, root Back does not underflow, and committed Back removes exactly one key. Add a cancellation test that leaves the stack unchanged.

- [ ] **Step 2: Run focused tests to confirm red**

  Run:

  ```bash
  ./gradlew :shared:jvmTest --tests '*AppNavDisplayJvmTest*' --configuration-cache
  ```

  Expected: FAIL because no Miuix root host/provider exists.

- [ ] **Step 3: Compose the root host**

  Install `NavDisplay` at the existing Shared shell root. Use the typed entry DSL and `contentKey` for each route. Pass an `onBack` that only invokes `navigator.pop()`. Do not install `NavigationEventHandler`, predictive Back handling, or a second edge-swipe recognizer in Shared.

- [ ] **Step 4: Register route transitions**

  Use Miuix default route transitions for base destinations, modal transition metadata for overlays, explicit disabled swipe metadata for protected entries, and layout-direction-derived physical swipe directions for approved route entries.

- [ ] **Step 5: Run focused tests to confirm green**

  Run the command from Step 2 plus:

  ```bash
  ./gradlew :shared:compileKotlinJvm --configuration-cache
  ```

  Expected: PASS.

## Task 4: Migrate base routes and route identity

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsRouteAdapterJvmTest.kt`

**Interfaces:**
- Consumes: current `LibraryRoute`, `LibraryNavigationAction`, route permission and invalidation rules.
- Produces: typed `AppNavKey` route projection and Shared route callbacks that mutate only `AppNavigator`.

- [ ] **Step 1: Add failing migration assertions**

  Extend characterization tests to assert route callbacks produce the expected `AppNavKey`, equal replacement gets a new appearance token, predecessor state survives pop, and deleted playlist routes pop/replace without Back arbitration.

- [ ] **Step 2: Implement base-route adapters**

  Replace the authoritative use of `LibraryNavigationStack` with `AppNavigator`. Preserve route payloads, route permission policy, route invalidation, and compact/list-detail rendering. Keep domain state and playlist repository ownership unchanged.

- [ ] **Step 3: Remove route-stack mutation paths**

  Delete or convert `applyNavigation`, `replaceTopRoute`, `LibraryNavigationAction`, and route preview code once all callers use typed navigator operations. Do not leave a mutable compatibility stack.

- [ ] **Step 4: Run migration tests**

  Run:

  ```bash
  ./gradlew :shared:jvmTest --tests '*LibraryNavigationTest*' --tests '*SettingsRouteAdapterJvmTest*' --configuration-cache
  ```

  Expected: PASS with no route-stack mirror.

## Task 5: Migrate modal, editor, selection, and Now Playing entries

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: `feature/playlists/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissal.kt`
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
- Modify: `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt`
- Test: Shared selection/Now Playing/overlay tests in their existing test locations.

**Interfaces:**
- Consumes: playlist dismissal callbacks, `TrackSelectionState`, playback state, clear-library/about dialogs, and current bottom-bar presentation.
- Produces: typed overlay keys and lifecycle-driven cleanup with stack presence as the sole visibility authority.

- [ ] **Step 1: Write failing overlay-order tests**

  Assert modal, editor, selection, Now Playing, and dialogs push one key above the route; Back removes only the top key; selection cancellation and Now Playing collapse occur after the corresponding key disappears; predictive cancellation changes neither stack nor domain state.

- [ ] **Step 2: Convert feature presentation requests**

  Replace dismissal registration with callback-first requests. Shared receives requests and pushes typed keys. Feature code must not know `NavBackStack` or Miuix types.

- [ ] **Step 3: Derive presentation from stack entries**

  Make overlay visibility derive from the top `AppNavKey`. Keep selected IDs, drafts, playback state, and repository data in their owning state owners. Use Miuix entry lifecycle/state-holder disposal and stack observation for cleanup consequences.

- [ ] **Step 4: Preserve invalidation paths**

  Keep playlist deletion and domain invalidation as explicit typed pop/replace commands. They must not call Back resolution or remove unrelated overlay/domain state.

- [ ] **Step 5: Run overlay tests**

  Run:

  ```bash
  ./gradlew :shared:jvmTest --tests '*PlaylistBackPolicyJvmTest*' --tests '*LibraryNavigationTest*' --configuration-cache
  ./gradlew :feature:playlists:impl:jvmTest --tests '*PlaylistFeatureDismissalTest*' --configuration-cache
  ```

  Expected: PASS.

## Task 6: Replace platform Back and edge swipe ownership

**Files:**
- Modify/delete Shared platform Back bridge files identified by characterization references to `NavigationEventHandler`/custom predictive handlers.
- Modify: Shared root navigation host and route transition metadata.
- Test: Android/iOS/Desktop platform Back and swipe tests at their existing platform test locations.

**Interfaces:**
- Consumes: Miuix `PredictiveBackHandlerWithSessions`, `NavDisplay`, `NavSwipeDirection`, and current platform bridge events.
- Produces: one Miuix-owned platform Back/predictive/swipe path.

- [ ] **Step 1: Write failing platform ownership tests**

  Assert commit pops once, cancel leaves the stack untouched, entry-animation Back interrupts as one transition, protected overlays do not swipe-dismiss, route swipe direction mirrors physical LTR/RTL policy, and root Back does not underflow.

- [ ] **Step 2: Remove competing handlers**

  Delete Shared predictive session/latching/settlement state and custom edge-swipe owner after all overlay callers migrate. Do not retain a fallback handler or callback veto after Miuix commit.

- [ ] **Step 3: Configure Miuix ownership**

  Configure `NavDisplay` and per-entry swipe metadata. Use `onBack = { navigator.pop() }` as the only commit mutation. Let cancellation and settle remain entirely inside Miuix.

- [ ] **Step 4: Run platform-focused tests**

  Run the exact Android, iOS, and Desktop focused Back/swipe suites identified in Step 1. Expected: PASS with no Shared platform handler references.

## Task 7: Delete legacy Back contracts and verify module boundaries

**Files:**
- Modify/delete: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`
- Modify/delete: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- Modify: affected feature API/implementation contracts and tests.
- Test: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt` or the repository’s affected architecture tests.

**Interfaces:**
- Consumes: completed canonical stack and migrated feature callbacks.
- Produces: clean cutover with no old route/Back authority.

- [ ] **Step 1: Search all legacy references**

  Use CodeGraph and repository search to enumerate `LibraryBackTarget`, `resolveLibraryBack`, `pendingBackSession`, dismissal registration, custom edge-swipe, and `LibraryNavigationStack` production references. Every reference must be migrated or deleted.

- [ ] **Step 2: Delete obsolete contracts**

  Remove legacy Back resolution/session/settlement code, registration ports, and compatibility route-stack mutation. Preserve domain invalidation and state reducers that are not Back authorities.

- [ ] **Step 3: Run architecture checks**

  Run the repository’s architecture/dependency checks and focused Shared/feature tests. Expected: no feature-to-Shared bridge, no feature-to-Miuix edge, no second navigation stack, and no cycle.

## Task 8: Restoration, platform facade, and visual acceptance

**Files:**
- Modify: Shared navigation entry/provider and iOS adapter files only where required by the canonical key model.
- Test: Shared restoration tests, `iosApp` tests, Desktop/JVM UI tests, and affected feature tests.
- Modify: `docs/architecture.md`, relevant ADR, `progress.md`, `roadmap.md`, OpenSpec task status.

**Interfaces:**
- Consumes: complete Miuix-owned navigation implementation.
- Produces: verified cross-platform migration evidence and updated architecture records.

- [ ] **Step 1: Add restoration and boundary regressions**

  Test serializable route/overlay keys, restored entry state, invalid key failure, iOS `MainViewController`, compact/wide clipping, dialogs, editors, selection, Now Playing, and route invalidation.

- [ ] **Step 2: Run quality and platform verification**

  Run separately:

  ```bash
  ./gradlew spotlessApply --configuration-cache
  ./gradlew spotlessCheck --configuration-cache
  ./gradlew detekt --configuration-cache
  ./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
  /usr/bin/xcrun xcodebuild -version
  ./gradlew :shared:iosSimulatorArm64Test --configuration-cache
  ```

  Record exact outputs. Do not infer runtime/device/visual behavior from compile or unit-test success.

- [ ] **Step 3: Perform actual visual smoke checks**

  Launch Desktop and inspect base push/pop, modal transition, Now Playing, selection, and compact/wide layouts. Run the corresponding iOS simulator surface if available. Record unavailable surfaces as blockers instead of claiming visual success.

- [ ] **Step 4: Complete documentation and strict acceptance**

  Update architecture/ADR and handoff records with the ownership change, delete no-go wording that is superseded by this approved migration, then run:

  ```bash
  openspec validate replace-shared-back-with-miuix-nav --strict
  git diff --check
  ```

  Mark OpenSpec tasks complete only with command and reviewed-diff evidence.

- [ ] **Step 5: Commit the completed migration**

  After all acceptance gates pass, stage only the approved migration files and create:

  ```bash
  git commit -m "feat: make Miuix the app Back owner"
  ```
