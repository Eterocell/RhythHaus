# Platform-Adaptive UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the approved full RhythHaus adaptive UI redesign across Android, iOS, and macOS without changing existing playback, scanning, storage, route, or Back ownership.

**Architecture:** `:shared` remains the sole application composition root and owns responsive shell geometry, the common route header, overlay/input arbitration, and platform-facing shell decisions. `:core:ui` supplies the shared semantic visual system; feature modules render their existing domain states and actions inside explicit Shared-owned bounds. Complete the separate `theme-selection` change first, then adapt every named route and state in this plan.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, existing Miuix/Material UI adapters, existing `WindowInsets` and navigation-event APIs, existing feature UI tests and Gradle verification. No new UI framework or dependency.

**Spec:** [Approved adaptive design](../specs/2026-10-29-platform-adaptive-ui-redesign-design.md); [OpenSpec change](../../../openspec/changes/platform-adaptive-ui-redesign/); [theme-selection plan prerequisite](2026-10-30-theme-selection-palettes.md).

## Global Constraints

- Keep one Home-rooted navigation stack; compact bottom navigation is below 720dp, rail is 720–999dp, and sidebar is at least 1000dp. Navigation remains independent of playback; Settings remains a global tool action.
- Classify using safe-area-adjusted window width before subtracting app navigation. A two-pane layout requires at least 320dp browse width, 400dp detail width, and a 24dp gap; otherwise use one pane. Maximum content width is 1440dp; page margins are 16/24/32dp for compact/rail/sidebar.
- Use the approved 4/8/12/16/24/32/48dp spacing steps; 28/34sp title, 24/30sp compact title, 18/24sp section, 16/22sp body, 14/20sp secondary, and 12/16sp auxiliary roles; starting corner radii are 8/10/12/20dp for artwork/controls/groups/overlays. Grow for system text; do not shrink to conceal content.
- Preserve minimum Android touch targets of 48dp and iOS targets of 44pt. Desktop may be visually denser only with usable keyboard focus and pointer/context actions.
- Keep Shared as the only owner of cross-feature routes, Insets, overlays, Back arbitration, total DI assembly, and iOS framework surface. Feature modules do not depend on Shared or resolve dependencies through a service locator.
- Preserve Library, Search, playlist, Now Playing, Settings, onboarding, and platform operation owners. No new scanning, playback, persistence, or navigation authority; no SQLDelight schema changes.
- Preserve existing iOS explicit/application-level return gestures; add no iOS native-style interactive Back, trackpad Back, or Cmd+[ behavior. Do not enable the abandoned Miuix navigation runtime.
- Keep Android, iOS, and macOS/desktop JVM as the supported scope. Add no Windows/Linux packaging or product support.
- Complete `openspec/changes/theme-selection/` first in `.worktrees/theme-selection-palettes/`; integrate it before starting this plan in `.worktrees/platform-adaptive-ui-redesign/`. No production UI implementation before the user approves both plans.
- Update `docs/architecture.md` and the relevant feature-first ADR if a public Shared UI entry point or resource owner changes. Preserve the staged `WATCHDOG.yml`, pre-existing edits, and index state when creating worktrees.
- Agent-run build/test/review evidence does not satisfy user-only Android/iOS/macOS visual, accessibility, system-Inset, or audio acceptance. Record those as unverified until the user performs them.

## Review Focus

1. Real system Insets, cutouts, IME overlap, and rapid route transitions: `LibraryAppShellJvmTest.routeTransitionInsetsRemainStableThroughPushPopAndCancellation` plus user-owned Android/iOS window checks.
2. Exact 720dp/1000dp boundaries, landscape/tablet/desktop widths, English/Simplified Chinese, and 200% text: `LibraryAppShellJvmTest.navigationAndHeaderReflowAtApprovedBreakpoints` and owning feature UI tests.
3. Gesture competition among Controls, Queue reorder/scroll, Lyrics/Timer scroll, buttons, horizontal seek, pointer cancellation, and Back: `LibraryAppShellJvmTest.controlsDismissalDoesNotStealNestedPlayerInput` and `LibraryAppShellJvmTest.platformBackKeepsExistingLayerPrecedence`.
4. State retention during resize/theme changes, especially Search query/scroll, selection, metadata edits, playlist drafts, and queue-save drafts: `SearchScreenJvmTest.searchStateSurvivesResizeAndThemeChange` and `PlaylistQueueSaveJvmTest.saveDraftSurvivesTrackChangeAndResize`.
5. Unsupported platform capability and accessibility behavior: Monet stays owned by the separate theme plan; this plan tests `LibraryAppShellJvmTest.reducedMotionUsesLatestPlatformProjection`, `sharedHeaderPreservesFullAccessibleTitleAndOverflowActions`, and `exitingSurfaceExposesNoInputOrAccessibilityActions` without adding new iOS gesture behavior.

---

## File Map

| Owner | Existing files | Responsibility in this plan |
| --- | --- | --- |
| Shared shell | `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`; `library/ui/AppShellLayout.kt`; `library/ui/LibraryAppShell.kt`; `library/ui/LibraryRoutes.kt`; `library/ui/AppNavigationChrome.kt` | One stable safe rectangle; common route header; measured chrome/IME clearance; active-layer ownership; route and overlay presentation. |
| Shared shell tests | `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt`; `AppNavigationChromeJvmTest.kt`; `LibraryRouteAdapterJvmTest.kt`; `SearchRouteAdapterJvmTest.kt` | Consumer-visible transition, navigation, chrome, route-adapter, focus, and gesture regressions. |
| Core UI | `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausMotion.kt`; `HausWindowOverlayHost.kt`; `VerticalSheetGesture.kt`; `RhythHausTopAppBar.kt`; `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/` | Shared motion, overlay/gesture primitives, common header visual implementation, and neutral Compose `PaddingValues` passed by Shared (never an app-specific type that reverses module dependencies). |
| Library/Search | `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`; `LibraryDetailContent.kt`; `LibraryBrowser.kt`; `LibraryChrome.kt`; `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt` | Six Library views, details, Search, selection, metadata editing, and list-tail presentation. |
| Playlist/document | `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`; `SmartPlaylistScreens.kt`; `PlaybackQueueContent.kt`; `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogs.kt`; Shared playlist/document route adapters | Static/smart playlist, rule, queue, and document presentation without changing owners or formats. |
| Playback | `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingPanel.kt`; `NowPlayingContent.kt`; `NowPlayingAdaptiveLayout.kt`; `SleepTimerPanel.kt`; Shared `LibraryAppShell.kt` | Player Controls, Queue, Lyrics, Timer, failure/loading/paused/ended states, and dismissal gesture arbitration. |
| Settings/tool routes | `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`; `AboutScreens.kt`; `feature/onboarding/src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingScreen.kt`; Shared `LibraryRoutes.kt`, `DesktopDropTargetPresentation.kt` | Settings/source/scan/report/maintenance, About/licenses, onboarding, and desktop-drop feedback. |
| Platform shell | Existing `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausMotion.kt`; `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt` (`enableEdgeToEdge()` remains); create `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.kt` and its Android/iOS/JVM actuals only for the one Shared-owned system-bar writer | Verify the existing host-provided `MotionDurationScale` against live OS reduced-motion settings; Android bar-icon style follows the final visible edge surface. No platform service locator or second UI root. |
| Governance | `docs/architecture.md`, `docs/adr/0001-feature-first-module-boundaries.md`, `progress.md`, `roadmap.md` | Public boundary/resource-owner updates and evidence-based handoff. |

Feature declarations are public in several of these modules. Before changing a public declaration, run LSP references for the exact symbol, migrate every caller in the same task, and remove obsolete parameters/headers rather than adding compatibility shims.

---

### Task 1: Verify prerequisite and capture failing baselines

**Files:**
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt`
- Test: `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt`
- Record: `progress.md`

**Interfaces:**
- Consumes: completed theme-selection implementation and its persisted resolved appearance contract.
- Produces: reproducible failing Insets and Controls-dismissal scenarios for Tasks 2–4; no implementation changes in this task.

- [ ] **Step 1: Verify the separate theme-selection change is complete and integrated**

Run `openspec validate theme-selection --strict` and the theme plan's focused `:core:ui:jvmTest`, `:feature:settings:jvmTest`, `:shared:jvmTest`, `:androidApp:assembleDebug`, and `./init.sh` gates. Record actual XML test counts and output. Do not start shell implementation if the dependency is incomplete.

- [ ] **Step 2: Reproduce the reported Insets and player-dismissal behavior on the actual Compose route**

Record platform, window bounds, route, inset/IME state, exact drag origin/trajectory, nested surface, and observed result in `progress.md`. Compare settled and entering/exiting/cancelled route frames; for the player compare Controls artwork/empty space against buttons, seek, Queue body, Lyrics/Timer, protected overlays, and Back.

- [ ] **Step 3: Add consumer-visible failing regressions before production changes**

Add `routeTransitionInsetsRemainStableThroughPushPopAndCancellation` and `controlsDownSwipeDismissesToDockWithoutChangingPlayback` to the Shared production-shell tests. The first asserts one content safe rectangle at each sampled transition frame and after cancellation; the second asserts the previous route/dock are restored while selected track, playback status, queue occurrences, and route stack remain unchanged. If a JVM test cannot model a user-reported platform condition, add the narrowest supported platform test or preserve an exact reproducible manual scenario; do not replace it with a helper-only assertion.

- [ ] **Step 4: Run the regressions before fixing the implementation**

Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --configuration-cache` and any added player test class. Expected: each new regression demonstrates the reported consumer-visible defect before its fix; existing tests remain attributable separately. Record the failing assertion/trace, not just the Gradle summary.

---

### Task 2: Establish Shared safe geometry and the measured route header

**Files:**
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppShellHeader.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppShellLayout.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt`
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/AppNavigationChromeJvmTest.kt`

**Interfaces:**
- Consumes: `AppNavigationLayout`, `appNavigationLayoutFor(widthDp)`, existing Shared route and Back state.
- Produces: internal `AppShellRouteHeader(title: String, onBack: (() -> Unit)?, actions: List<AppShellHeaderAction>)`; `AppShellHeaderAction(label: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit)`; one `AppShellHeader(model, layout, safeTopInset, modifier)` composable. Shared passes only neutral Compose `PaddingValues`/resolved content bounds through feature entry points. Later feature tasks opt routes in only when their duplicate app header is removed; full-window onboarding and expanded-player layers keep their own controls.

- [ ] **Step 1: Add Shared route/header boundary regressions**

Add `sharedRouteHeaderRetainsHeightAcrossRoutesAtSameLayout`, `sharedHeaderPreservesFullAccessibleTitleAndOverflowActions`, `navigationAndHeaderReflowAtApprovedBreakpoints`, and assertions to `routeTransitionInsetsRemainStableThroughPushPopAndCancellation`. Exercise widths immediately below, at, and above 720dp and 1000dp; long English/Chinese titles; zero/one/many actions; and transition cancellation.

- [ ] **Step 2: Run the new tests and confirm the current shell violates the new assertions**

Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --tests '*AppNavigationChromeJvmTest' --configuration-cache`. Expected: the new header assertions fail before the component/slot exists; an Insets regression must reproduce under the environment recorded in Task 1. If a JVM Insets case passes, preserve the test and use the supported platform reproduction rather than forcing a speculative shell change.

- [ ] **Step 3: Resolve safe geometry once outside route animation**

In `AppShellLayout.kt`, compute the safe-area-adjusted available width and physical safe edges once for the active window composition; retain the 720dp/1000dp classifications and margins. Keep route content, header, navigation, measured player/selection chrome, and the active overlay inside the same safe rectangle for every frame. Pass only resolved content bounds/standard `PaddingValues` through existing feature entry-point seams; feature routes must not independently read global `WindowInsets`. Recompute only when actual window bounds, orientation, system Insets, or text class changes. Do not compensate with fixed pixel constants or duplicate feature padding.

- [ ] **Step 4: Add the Shared header and route projection**

Implement `AppShellRouteHeader` and `AppShellHeader` in the new file. Measure one title line and one common height per layout/text class; preserve the full accessible title when display text ellipsizes. Render excess actions in a named menu in original order. Route title/action changes within the same class must not alter header height. Keep global Settings semantics in navigation, not as a fourth primary destination.

- [ ] **Step 5: Add an optional Shared route-header slot without stacking it over legacy feature headers**

Compose the header adjacent to—not inside—the animated route body in `LibraryAppShell.kt`/`LibraryRoutes.kt`. Keep it absent for routes that still render their own app header; enable it route-by-route in Tasks 5, 6, and 8 in the same change that removes that feature header. Ensure transition/cancel states retain the same header bounds and that the exposed action is the active route's action.

- [ ] **Step 6: Run focused Shared regressions**

Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --tests '*AppNavigationChromeJvmTest' --configuration-cache`. Expected: all new header/safe-rectangle assertions and the existing Shared route/back tests pass; the player-dismissal regression remains assigned to Task 4.

---

### Task 3: Reconcile chrome clearance, keyboard, foreground, and platform shell behavior

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/AppShellLayout.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.kt`, `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.android.kt`, `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.ios.kt`, and `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/systemui/AppSystemBarAppearance.jvm.kt`
- Modify: `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausMotion.kt` and `HausWindowOverlayHost.kt` only if the reproduced behavior requires it
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt`; `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/HausMotionJvmTest.kt`

**Interfaces:**
- Consumes: Task 2's Shared geometry/header and existing overlay/Back owner.
- Produces: one measured active chrome-clearance policy; a foreground-only input/accessibility policy; current platform reduced-motion projection; Android system-bar appearance from the final surface underneath each bar. No feature service lookup or new persisted motion preference.

- [ ] **Step 1: Add failure tests for overlapping Insets and inactive layers**

Add `chromeClearanceCountsOverlappingInsetsOnce`, `staleChromeMeasurementExposesNoAction`, `focusedOverlayConsumesImeWithoutResizingCoveredRoute`, `exitingSurfaceExposesNoInputOrAccessibilityActions`, and `reducedMotionUsesLatestPlatformProjection`. Assert the final row's actual visible bounds, navigation/player/selection overlap, active measurement replacement after resize, IME show/hide, foreground semantics, and latest reduced-motion behavior without an app preference.

- [ ] **Step 2: Run the focused tests before changing clearance ownership**

Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --configuration-cache`. Record whether each assertion reproduces a consumer-visible gap. If an assertion passes, retain it as a guard and trace the reported mismatch on the actual supported platform before making any corresponding code change.

- [ ] **Step 3: Apply active measured chrome and IME geometry once**

Keep safe-area and IME rectangles distinct until overlap is resolved; do not add the full IME height to an already overlapping navigation-bar height. Hide the compact dock/navigation while a foreground text editor owns the IME and restore them after dismissal using the latest geometry. Keep route bottom padding equal to only the currently occupied measured surfaces. A stale width/content measurement may neither reserve clearance nor expose hit targets.

- [ ] **Step 4: Make foreground ownership authoritative at transition boundaries**

Ensure only the top eligible route/overlay receives pointer, gesture, focus, keyboard, and accessibility actions. Remove those actions from hidden/exiting layers before visual alpha reaches zero; restore the previous route's focus only after its authoritative state is visible. Preserve existing Shared Back precedence and cancellation/settlement behavior.

- [ ] **Step 5: Integrate reduced motion and Android bar appearance through platform seams**

Use existing `rememberHausReducedMotion()`/host-provided `MotionDurationScale` as the motion policy; verify live Android/iOS/macOS changes. Add a narrow platform adapter only if the actual host fails to provide/update the OS setting; do not store another preference. Add one Shared-owned `AppSystemBarAppearance` expect/actual seam whose Android actual sets light/dark system-bar icons from the final composited edge surfaces; iOS/JVM actuals do nothing because those hosts have no Android system bars. `MainActivity` stays thin and retains `enableEdgeToEdge()`.

- [ ] **Step 6: Run focused tests and Android compilation**

Run `./gradlew :shared:jvmTest :core:ui:jvmTest :androidApp:assembleDebug --configuration-cache`. Expected: Shared inset/foreground and core motion tests pass and Android host compiles; device-specific system-bar/reduced-motion acceptance remains user-owned.

---

### Task 4: Restore Controls-only downward Now Playing dismissal

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- Modify: `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContent.kt` only if a child surface needs a narrow explicit gesture boundary
- Modify: `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt` only if the reproduced threshold/cancellation defect is in the shared primitive
- Test: `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShellJvmTest.kt`
- Test: `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt`
- Test: `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt`

**Interfaces:**
- Consumes: `NowPlayingPanel.Controls/Lyrics/Timer/Queue`, existing `LibraryAppState.hideNowPlaying`, `nowPlayingSwipeCollapseAction(appState)`, and `verticalSheetGesture`.
- Produces: a dismissal recognizer active only for Controls non-interactive surface; Queue's own handle closes Queue to Controls; existing explicit close, Back, and queue mutation paths remain authoritative.

- [ ] **Step 1: Add gesture competition regressions**

Add `controlsDismissalStartsOnArtworkAndUnusedSurface`, `controlsDismissalPreservesTapSeekAndShortDrag`, `controlsDismissalDoesNotStealNestedPlayerInput`, `queueBodyScrollAndReorderDoNotDismissPlayer`, `lyricsAndTimerScrollDoNotDismissPlayer`, `protectedOverlayBlocksUnderlyingPlayerDrag`, and `platformBackKeepsExistingLayerPrecedence`. Assert route, selected track, playback status, full queue occurrence order, panel state, and focus after completion/cancel.

- [ ] **Step 2: Run tests and verify the reported dismissal failure before implementation**

Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --configuration-cache` and then `./gradlew :feature:nowplaying:jvmTest :core:ui:jvmTest --configuration-cache`. Expected: the Controls close regression fails on the pre-change production route; unrelated nested-panel tests document currently preserved behavior.

- [ ] **Step 3: Scope vertical dismissal to Controls and keep child gestures local**

Use the existing Shared hide action exactly once after the current vertical threshold/fling rule settles. Disable whole-player dismissal when `panelPresentation.panel != NowPlayingPanel.Controls`. Keep the recognizer off buttons, seek, and queue rows; do not change the Queue handle's close-to-Controls behavior or content scrolling/reorder. Preserve the visible named close action as the accessible alternative.

- [ ] **Step 4: Verify cancellation, resize, and protected-layer precedence**

Test short drag snap-back, pointer cancellation, window resize during drag, an arriving protected dialog, queue body scroll/reorder, Lyrics/Timer scroll, and platform Back. The previous route and scroll position must return only after settlement; playback and queue state must be identical.

- [ ] **Step 5: Run focused Shared/core/player suites**

Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --configuration-cache` and then `./gradlew :feature:nowplaying:jvmTest :core:ui:jvmTest --configuration-cache`. Expected: all named gestures and existing playback/Back/focus tests pass.

---

### Task 5: Adapt Library and Search presentation

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`, `LibraryDetailContent.kt`, `LibraryBrowser.kt`, `LibraryChrome.kt`
- Modify: `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- Modify: Shared `LibraryAppShell.kt` and `LibraryRoutes.kt` for route-header/action projection and exact body bounds
- Test: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContentJvmTest.kt`, `DrillDownViewJvmTest.kt`, `LibraryBrowseControlsJvmTest.kt`; `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryBrowserTest.kt`; create `feature/search/src/jvmTest/kotlin/com/eterocell/rhythhaus/search/SearchScreenJvmTest.kt` for the production Compose screen

**Interfaces:**
- Consumes: Task 2's Shared header and safe content frame; existing `LibraryHomeContent`, `SearchContent`, selection, scroll, and metadata callbacks.
- Produces: adaptive six-view browsing, album/artist detail, Search, selection, and metadata editing that preserve existing list order and feature-owned state. Migrate all public screen references discovered by LSP; no old-header flags or aliases.

- [ ] **Step 1: Add production-route reflow/state-retention tests**

Add `LibraryHomeContentJvmTest.visibleBrowseAndFilteredEmptyStatesReflowWithoutLosingMode`, `SearchScreenJvmTest.searchStateSurvivesResizeAndThemeChange`, and `LibraryHomeContentJvmTest.selectionAndMetadataDraftStayReachableAt200Percent`. Assert coverage of BrowseMode All, Albums, Artists, Favorites, RecentlyPlayed, and RecentlyAdded; Search query/zero-results; active filters; selection; missing artwork/long labels; metadata draft cancel/failure; list final-row reachability; and row accessibility at compact, rail, sidebar, English, Simplified Chinese, and 200% text scale.

- [ ] **Step 2: Run owning tests and identify current failing presentation**

Run `./gradlew :feature:library:impl:jvmTest :feature:library:impl:allTests :feature:search:jvmTest :shared:jvmTest --configuration-cache`. Expected: new assertions expose only the routes that do not yet match the approved adaptive surface; existing repository/scanner tests remain unchanged.

- [ ] **Step 3: Migrate Library/Search route chrome and adaptive content**

Use Shared's route header for page title/return/actions; remove each feature-owned duplicate app-level top bar and redundant full-window `safeDrawing` read. Keep content headings only when they add distinct identity. Preserve the approved 320/400/24dp two-pane minimum and 1440dp maximum; use continuous song rows, adaptive artwork grids, existing browse filters, and no new domain behavior.

- [ ] **Step 4: Keep state and actions owned by existing callbacks**

Resize and theme changes must not reset browse mode, filters, selection, Search query, scroll, metadata draft, playback ordering, or errors. Apply 48dp Android/44pt iOS target minimums; keep keyboard, desktop focus, hover, and context actions reachable.

- [ ] **Step 5: Run focused Library/Search/Shared suites**

Run `./gradlew :feature:library:impl:jvmTest :feature:library:impl:allTests :feature:search:jvmTest :shared:jvmTest --configuration-cache`. Expected: every new route/state assertion passes without a Library/Search storage or navigation authority change.

---

### Task 6: Adapt playlist, smart-rule, queue, and document surfaces

**Files:**
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`, `SmartPlaylistScreens.kt`, `PlaybackQueueContent.kt`
- Modify: `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogs.kt`
- Modify: Shared playlist and document adapters in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt` and `LibraryRoutes.kt`
- Test: existing `PlaylistScreensTest.kt`, `SmartPlaylistScreensJvmTest.kt`, `PlaylistQueueSaveJvmTest.kt`, `PlaylistEditModeSemanticsJvmTest.kt`, `PlaylistBackupDialogsSemanticsJvmTest.kt`, `PlaylistBackupIntegrationJvmTest.kt`, and Shared playlist route tests

**Interfaces:**
- Consumes: Task 2's `AppShellRouteHeader`; existing `PlaylistStateOwner`, playlist repositories, `PlaylistBackupController`, document launcher, and callbacks.
- Produces: responsive playlist hub/detail/rule/document surfaces with unchanged static/smart rule, backup, matching, cancellation, draft, and Back contracts.

- [ ] **Step 1: Add layout/draft regressions to the production playlist routes**

Add `PlaylistScreensTest.playlistDraftAndVisibleActionsSurviveReflow`, `PlaylistBackupIntegrationJvmTest.previewCancelAndFailureKeepWholeLibraryAtomic`, and `PlaylistQueueSaveJvmTest.saveDraftSurvivesTrackChangeAndResize`. Assert long smart-rule candidate scrolling with confirmation reachable; static vs. smart type clarity; detail edit/reorder/remove/delete actions; import preview; ambiguous/expired rejection; cancel/failure atomicity; and queue-save draft retention after selected-track changes and viewport reflow.

- [ ] **Step 2: Run focused playlist/document tests before presentation changes**

Run `./gradlew :feature:playlists:impl:jvmTest :feature:playlists:impl:allTests :shared:jvmTest --configuration-cache`. Expected: new presentation assertions reproduce any route-level gaps; existing codec, matching, transaction, and repository behavior stays passing.

- [ ] **Step 3: Adapt feature-owned screens and remove duplicate route headers**

Use the Shared route header for ordinary playlist/document routes. Keep rule editing near-full-height and independently scrollable on compact windows; use a bounded dialog on wide windows. Preserve explicit distinction between JSON full-library/static-list backups and single-list M3U/M3U8/PLS interchange.

- [ ] **Step 4: Verify callbacks and state owners remain unchanged**

Retain every existing confirmed/failed/cancelled result and draft through route resize; do not change file formats, repositories, SQLDelight schema, or playback queue transactions. Add no compatibility API.

- [ ] **Step 5: Run playlist/document/Shared regressions**

Run `./gradlew :feature:playlists:impl:jvmTest :feature:playlists:impl:allTests :shared:jvmTest --configuration-cache`. Expected: new route-level assertions and existing playback/document ownership tests pass.

---

### Task 7: Adapt Now Playing content without weakening gesture boundaries

**Files:**
- Modify: `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContent.kt`, `NowPlayingAdaptiveLayout.kt`, `NowPlayingArtwork.kt`, `SleepTimerPanel.kt`
- Modify: Shared `LibraryAppShell.kt` only for the existing explicit shell/overlay boundary
- Test: `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingAdaptiveLayoutTest.kt`; `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt`, `NowPlayingBarSemanticsJvmTest.kt`, `SleepTimerPanelSemanticsJvmTest.kt`; Shared player tests from Task 4

**Interfaces:**
- Consumes: Task 4's Controls-only dismissal; existing playback/queue/timer state and callbacks; full-window player remains its own foreground layer, not an ordinary route header.
- Produces: compact/wide Controls, Queue, Lyrics, Timer, and loading/paused/ended/failure presentation with preserved focus, queue occurrence, error, favorite, lyric, and sleep-timer behavior.

- [ ] **Step 1: Add responsive state tests for the real Now Playing surface**

Add `NowPlayingAdaptiveLayoutTest.compactShortHeightPrioritizesControlsOverArtwork`, `NowPlayingAdaptiveLayoutTest.queuePanelUsesOnlyAvailableWideWidth`, and `NowPlayingContentSemanticsJvmTest.failureAndEmptyLyricStatesExposeOnlyValidActions`. Cover all player status states, failed-track recovery actions, missing artwork, no-lyrics entry absence, timestamped/plain long lyrics, Queue empty/current/previous/next/reorder/save states, timer modes, compact short height, and wide split only when required content widths fit. Assert playback and draft outcomes, not only visibility.

- [ ] **Step 2: Run the player suite before visual adaptation**

Run `./gradlew :feature:nowplaying:jvmTest :feature:nowplaying:allTests --configuration-cache` and then `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --configuration-cache`. Expected: any newly specified visual-state gaps fail at the consumer surface while established playback and queue semantics remain intact.

- [ ] **Step 3: Reflow Controls, Queue, Lyrics, and Timer within their existing owner**

Keep the compact player a separate overlay with a named close action; prioritize controls over cover size on short windows; place Queue in a side column only when at least 280dp remains after the other required columns; otherwise use the compact panel. Queue rows keep scroll/reorder ownership; Lyrics/Timer scroll locally; no child uses the Controls dismissal recognizer.

- [ ] **Step 4: Preserve accessible states and recovery actions**

Keep localized track identity, selected/favorite state, playback failure text and existing retry/skip/remove actions, lyric provenance behavior, timer wording, 48dp/44pt targets, and keyboard/focus order. Do not add internet lyrics, automatic retries, or playback-engine behavior.

- [ ] **Step 5: Run focused player and Shared route tests**

Run `./gradlew :feature:nowplaying:jvmTest :feature:nowplaying:allTests --configuration-cache` and then `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest' --configuration-cache`. Expected: new state/reflow tests pass, and every Task 4 gesture regression still passes.

---

### Task 8: Adapt Settings, source management, onboarding, About, and desktop feedback

**Files:**
- Modify: `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`, `AboutScreens.kt`
- Modify: `feature/onboarding/src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingScreen.kt`
- Modify: Shared `LibraryRoutes.kt`, `LibraryAppShell.kt`, and existing desktop-drop presentation adapter
- Test: `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt`, `AboutScreensJvmTest.kt`; `feature/onboarding/src/jvmTest/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingScreenJvmTest.kt`; Shared production-route/source/drop tests

**Interfaces:**
- Consumes: completed theme-selection state and Task 2 header; existing scan coordinator, source recovery, maintenance, onboarding and desktop import owners.
- Produces: global tool pages and onboarding with correct compact/wide navigation, complete failure/recovery states, and no obscured or stale actions.

- [ ] **Step 1: Add Settings/tool route state and hit-target regressions**

Add `SettingsScreenSemanticsJvmTest.toolRouteKeepsCorrectNavigationAndHeaderAcrossWidths`, `OnboardingScreenJvmTest.onboardingUsesFullSafeContentAndExposesOnlyExistingActions`, `AboutScreensJvmTest.longLicenseContentRemainsScrollableAt200Percent`, and `LibraryAppShellJvmTest.desktopDropFeedbackDoesNotOccludeChrome`. Assert five Settings groups, independent playback settings and their save failures, active scan/cancel/report/remove-missing states, inaccessible-source recovery, destructive confirmation semantics, no false primary-destination highlight, onboarding skip/platform import copy, About/license long-text scrolling, and all four desktop-drop feedback states. Use English/Simplified Chinese and default/200% text where strings or controls expand.

- [ ] **Step 2: Run current owning UI tests**

Run `./gradlew :feature:settings:jvmTest :feature:onboarding:jvmTest :shared:jvmTest --configuration-cache`. Expected: focused new assertions identify presentation gaps without changing scan/report, preference, import, or onboarding outcomes.

- [ ] **Step 3: Apply the Shared route header only to ordinary tool routes**

Settings/About/licenses use the common Shared title/return/action header; compact tool routes hide bottom primary navigation and wide tool routes preserve the sidebar without selecting a primary destination. Onboarding remains a full-window layer with its existing step/exit controls, but consumes Shared-resolved safe geometry instead of independently reading window Insets.

- [ ] **Step 4: Reflow tool content and preserve existing operation semantics**

Keep detailed scan/report/recovery in Settings, maintain terminal report identity, retain separate playback preference save/errors, explain destructive effects in the existing confirmation owners, keep About/license content scrollable, and ensure desktop-drop feedback does not cover navigation/player or admit duplicate submissions. Update the Shared route projection; remove obsolete feature-owned app bars and parameters rather than leaving hidden duplicates.

- [ ] **Step 5: Run focused Settings/onboarding/Shared tests**

Run `./gradlew :feature:settings:jvmTest :feature:onboarding:jvmTest :shared:jvmTest --configuration-cache`. Expected: all specified tool states and existing feature/Shared operations pass.

---

### Task 9: Verify full adaptive coverage, update evidence, review, and hand off

**Files:**
- Modify: `docs/architecture.md` and `docs/adr/0001-feature-first-module-boundaries.md` only when required by actual public API/resource-owner changes
- Modify: `roadmap.md` and `progress.md`
- Verify: all files from Tasks 1–8 and OpenSpec artifacts

**Interfaces:**
- Consumes: all completed implementation tasks and reviewable test evidence.
- Produces: strict OpenSpec-valid change, independently reviewed implementation, exact verification evidence, pending user-only acceptance record, and conventional commit.

- [ ] **Step 1: Run the approved full window/text/state matrix**

Exercise 360×640, 390×844, 844×390, 800×600, 800×1280, 1024×1366, and 1280×800dp at 720dp/1000dp boundaries. Cover English/Simplified Chinese, default/200% text, reduced motion, focus, IME, route transitions/cancel, active overlays, and all 51 named states in the interactive prototype. Assert no unintended horizontal overflow, clipped required action, stale interactive layer, or unreachable list tail.

- [ ] **Step 2: Run focused tests and inspect generated result XML**

Run the focused modules from Tasks 2–8. Inspect XML executed/failure counts rather than relying only on Gradle's cached/success summary. Run architecture dependency checks and formatting/static checks using repository task names.

- [ ] **Step 3: Run full repository verification and OpenSpec validation**

Run `./init.sh` and `openspec validate platform-adaptive-ui-redesign --strict`. Resolve every failing gate; record exact command and output. Verify all referenced repository paths and relative Markdown links exist.

- [ ] **Step 4: Run an independent review of production behavior and the diff**

Review repository authority and Shared orchestration separately; confirm all route callers, tests, docs, error states, accessibility actions, and public surface changes are included. Confirm no theme-selection code was duplicated in this change and no user edits/index state were overwritten.

- [ ] **Step 5: Record user-only acceptance as pending, update roadmap/progress, and commit**

Record automated evidence, platform blockers, and unperformed Android/iOS/macOS visual, accessibility, system-Inset, and audio checks as `unverified/user-performed`. Complete OpenSpec tasks only where evidence exists. Commit the reviewed work with a conventional message; do not archive this OpenSpec change until the stated acceptance gates are satisfied.

## Execution Gate

This plan is draft until the user approves it together with the separate [theme-selection plan](2026-10-30-theme-selection-palettes.md). Once both are approved, implement theme-selection first in its isolated worktree, integrate it, then execute this plan sequentially in `.worktrees/platform-adaptive-ui-redesign/`. No production UI edits are authorized before that gate.
