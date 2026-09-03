## 1. Characterization and dependency boundary

- [ ] 1.1 Capture the current route, modal, editor, selection, Now Playing, dialog, invalidation, predictive-cancel, predictive-commit, and compact/wide Back behavior in `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`, `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt`, `feature/playlists/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistFeatureDismissalTest.kt`, and the relevant Shared route-adapter tests; verify the characterization suite passes before migration.
- [ ] 1.2 Add `miuix-nav` version `0.9.4-rc01` to `gradle/libs.versions.toml` and the Shared implementation dependency in `shared/build.gradle.kts`; verify the dependency resolves with `./gradlew :shared:dependencies --configuration-cache` and no `androidx.navigation3` dependency appears.

## 2. Canonical key and navigator model

- [ ] 2.1 Add Shared-owned serializable `AppNavKey` base-route, overlay, selection, editor, dialog, and Now Playing key types with typed payloads and monotonic appearance identity; add tests for serialization, equal-route identity, invalid payload rejection, and root underflow; verify `./gradlew :shared:jvmTest --tests '*AppNavKey*' --configuration-cache`.
- [ ] 2.2 Add the Shared typed navigator wrapper around Miuix `NavBackStack<AppNavKey>` with `push`, `pop`, `replaceTop`, and `popToRoot`; prove it is the only mutable presentation stack and preserves exact predecessor entries; verify focused navigator tests and architecture source-boundary checks.
- [ ] 2.3 Register concrete Miuix entries with stable `contentKey` values and explicit transition/swipe metadata; add a provider test for every key type and duplicate content-key failure; verify the entry-provider tests and `./gradlew :shared:compileKotlinJvm --configuration-cache`.

## 3. Root renderer and base-route migration

- [ ] 3.1 Compose one Shared-root `NavDisplay` with `onBack = { appNavigator.pop() }`, no second Shared platform Back handler, and route entry providers for Home, Album, Artist, Search, PlaylistHub, PlaylistDetail, Settings, About, and Open Source Libraries; verify root/route push-pop and process-restoration tests.
- [ ] 3.2 Replace `LibraryNavigationStack` route projection in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt` and `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt` with typed stack projections while preserving route invalidation, replace semantics, route permission policy, and compact/list-detail rendering; verify route characterization tests and `:shared:jvmTest`.
- [ ] 3.3 Preserve equal-route replacement and predecessor state by migrating destination instance tokens into `AppNavKey`/`contentKey`; verify replacement, pop, and saveable-state tests fail if identity is route-value-only.

## 4. Overlay and feature-state migration

- [ ] 4.1 Convert playlist modal and editor visibility from `PlaylistFeatureDismissalPublisher`/Shared Back ports into typed modal/editor entries; retain playlist-owned draft/domain state and lifecycle cleanup; verify playlist feature presentation and Back tests.
- [ ] 4.2 Convert track-selection mode into a typed selection entry while retaining selected IDs and reconciliation in the existing selection state owner; verify selection start/toggle/clear and Back tests, including predictive cancellation with no state mutation.
- [ ] 4.3 Convert expanded Now Playing and all current clear-library/about/dialog presentation into typed overlay entries; preserve route permission and bottom-bar policies; verify overlay-order tests prove modal/editor/selection/Now Playing precedence through stack order only.
- [ ] 4.4 Convert domain-driven invalidation such as deleted playlist destinations into explicit typed pop/replace operations that do not invoke Back resolution; verify stale-entry removal and unrelated-state preservation tests.

## 5. Feature contracts and platform ownership

- [ ] 5.1 Replace feature dismissal registration APIs with callback-first navigation requests at each affected feature API boundary; verify feature implementations have no `miuix-nav` or Shared dependency and Shared adapts every request into one typed stack operation.
- [ ] 5.2 Remove Shared `LibraryBackTarget`, `LibraryBackSurfacePort`, `LibraryBackSelectionPort`, `LibraryBackResolutionInput`, `LibraryBackResolution`, predictive session state, and custom settlement dispatch after all consumers migrate; verify no production references remain and architecture graph checks pass.
- [ ] 5.3 Replace Shared edge-swipe/platform Back bridges with Miuix predictive Back and route-configured physical swipe directions; verify platform callback tests for commit, cancellation, interruption, LTR/RTL direction, protected overlays, and root behavior on Android, iOS, and Desktop.

## 6. State restoration, quality, and acceptance

- [ ] 6.1 Verify `@Serializable` AppNavKey restoration across configuration/process-death paths and entry-scoped state retention; verify malformed/unregistered keys fail explicitly without a legacy-stack fallback.
- [ ] 6.2 Verify compact and wide route rendering, clipped transitions, dialogs, editor, selection, Now Playing, and iOS `MainViewController` behavior on the canonical stack; run the relevant JVM/UI tests and platform smoke scenarios.
- [ ] 6.3 Run `./gradlew spotlessApply --configuration-cache`, separate `./gradlew spotlessCheck --configuration-cache`, `./gradlew detekt --configuration-cache`, `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `/usr/bin/xcrun xcodebuild -version`, and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`; record exact evidence without inferring runtime behavior from compile tests.
- [ ] 6.4 Run architecture/dependency checks, `openspec validate replace-shared-back-with-miuix-nav --strict`, and `git diff --check`; update `progress.md`, `roadmap.md`, architecture ADR/design records, and this change’s task status, then create the required conventional implementation commit only after all hard-stop conditions are clear.
