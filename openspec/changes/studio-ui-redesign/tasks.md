# Tasks

设计全文与[实施计划](../../../docs/superpowers/plans/2026-10-19-studio-ui-redesign.md)已于2026-10-19获用户批准。这里记录实施进度，不把规划完成记作产品完成。按1→8顺序一次执行一个任务，每组自身完成回归、真实烟测、文档与独立审查；具体文件/接口/命令以计划对应任务为准。

## Manual acceptance status — 2026-10-26

At the user's request, all manual Studio UI verification and acceptance is postponed, including acceptance inferred from earlier limited runtime smoke. Those observations remain factual and narrow, but do not count as acceptance or a pass. The full visual/interaction matrix, keyboard and screen-reader acceptance, reduced-motion and large-text acceptance, system-picker flows, audible playback, physical route-loss, and system-media checks remain unverified—not removed from scope. Keep the manual/mixed items (2.2–6.2, 7.2, 8.2, and 8.3) unchecked; postponement does not complete them or authorize archiving.

## 1. Studio tokens and common surfaces

- [x] 1.1 扩展现有palette和排版/尺寸token、Miuix桥及现有公共弹层/顶栏/返回；把既有pulse危险/失败用途迁移到danger角色，维持选中/播放行正文对比度；按计划Task 1观察颜色性质回归RED/GREEN，验证正文/动作≥4.5:1及必要边界≥3:1，保留主题与dismiss行为。
- [x] 1.2 执行Task 1全部受影响Shared/feature JVM检查、desktop compile与浅深真实页面/危险确认烟测；更新progress/roadmap证据、独立审查并提交本组checkpoint，不能宣称所有页面密度已完成。广泛Shared/desktop命令超时且Shared shell既有3个Settings/report断言仍失败，具体命令、失败名及其不在本任务diff中的证据已记录于worktree `progress.md`；不得据此声称完整Shared suite绿色或所有页面密度完成。

## 2. Atomic shell and mini-player separation

- [x] 2.1 按计划Task 2一次迁移三入口/轨/侧栏、Settings工具入口、单栈高亮、净空间详情、nav+dock测量与非空mini API；删除空库mini导航/旧mode及全部旧调用，验证断点719/720/999/1000、净宽1015/1016、单栈Back与真实导航清选择。
- [ ] 2.2 运行Task 2 Shared/NowPlaying测试和desktop compile；实际检查800×600无空详情列、无track仍可导航、resize保留存活页面、选择/滚动/末行与inactive输入，记录文档、独立review后提交。

Task 2.2 evidence update (2026-10-21):
- `./gradlew :shared:jvmTest --tests '*LibraryNavigationTest*' --tests '*LibraryAppShellJvmTest*' --tests '*TrackSelectionSemanticsJvmTest*' :feature:nowplaying:jvmTest --configuration-cache --console=plain` passed in 9s (152 actionable tasks; 27 executed, 125 up-to-date). Latest XML: `LibraryNavigationTest` 79 tests, `LibraryAppShellJvmTest` 18 tests, `TrackSelectionSemanticsJvmTest` 5 tests; zero failures. The regression `searchQueryScrollAndSelectionSurvivesReflowAndRealNavigationClearsSelection` passed.
- `./gradlew :desktopApp:compileKotlin --configuration-cache --console=plain` passed in 320ms (142 actionable tasks; 23 executed, 119 up-to-date).
- A broad Shared/NowPlaying/desktop verification timed out after 900s with `PlaybackSessionCoordinatorTest.terminalCompletionFlushPersistsExactNonWholeSecondDurationWithoutProgress` failing at line 363. The existing worktree ledger records the same expected-1234/actual-0 failure on untouched `main`; the playback implementation and test are unchanged in this diff. The full suite is not green and is not attributed to this Task 2 change.
- The supplied 800×600 and 1728×1084 desktop captures were reviewed and show no empty detail column; the automated shell suite also covers navigation without playback. The remaining user-owned acceptance is manual: 800×600 with no track, 360–390 interaction, Search query/scroll/selection through resize in both directions, last-row clearance, and inert inactive chrome. Orca currently reports granted permissions but cannot read/control the visible Java window because it exposes no accessibility window.
- Independent spec review found no code defect but returned not ready until this manual matrix is recorded; the standards/architecture review found no issue. Keep 2.2 unchecked and do not commit/archive until the remaining acceptance evidence is supplied.

## 3. Content-first Library, Search and shared song rows

- [x] 3.1 按计划Task 3实现无领域Core行并原子迁移Library/Search/playlist occurrence适配；重设计六视图、具名过滤、网格、详情与metadata菜单，保持投影/顺序/选择/收藏/草稿语义；修改受影响行为回归并删除纯旧样式测试。
- [ ] 3.2 跑Task 3所列Core/Library/Search/Playlists/Shared JVM及desktop compile；真实360×640首排、EN/ZH/200%字、过滤清除/metadata取消/可见顺序播放烟测，更新所属资源与证据，独立review后提交。

## 4. Atomic player and Queue migration

- [x] 4.1 按计划Task 4整体迁移panel/Shared appearance与Back、full player、歌词/定时、Queue含save/clear草稿及dismissal、mini next/queue；同组移除Hub tabs/PlaylistTab/SelectTab全部引用，保持一个队列权威且不显示虚假音量。通过旧快照/重复occurrence/旧响应/非拖拽重排替代动作/预测Back与层级回归。
- [ ] 4.2 跑Task 4 NowPlaying/Playlists/Shared/Core Playback JVM与desktop compile；真实媒体播放/seek、歌词换曲、timer、重复队列保存/取消、确认→queue→player的Back及短高大字烟测；记录证据，独立review后提交。

Task 4.2 evidence update (2026-10-21):
- `:feature:nowplaying:jvmTest` and `:feature:playlists:impl:jvmTest` passed with `--rerun-tasks` after the final Queue and dialog changes. Focused Shared tests for downward player collapse, Queue→Player→Library Back, wide Queue modal layering, and outside-click blocking passed. `:core:ui:jvmTest`, `:desktopApp:compileKotlin`, `spotlessApply`, separate `spotlessCheck`, separate `detekt`, and `architectureCheck` also passed.
- The complete Shared JVM run still reports `PlaybackSessionCoordinatorTest.terminalCompletionFlushPersistsExactNonWholeSecondDurationWithoutProgress` (the same failure is recorded on untouched `main`). The Core Playback JVM run completed 221 tests with 14 `PlaybackControllerTest` failures; `core/playback` has no changes in this worktree, but those failures are not claimed as baseline-proven. Neither complete suite is claimed green.
- Isolated desktop smoke at 800×600 displayed controlled local WAV tracks and exercised the player, seek UI, queue, and Queue → Player → Library Back. This does not establish audible playback or cover timer, lyrics replacement, duplicate save/cancel, clear confirmation, 1280×800, or 200% text. Orca could not inspect the visible Compose window (`permission_denied`).
- Fresh independent read-only review verified the shared panel dismissal and full-window save/clear overlay contracts, including a visible-card width assertion above the 280dp rail; no actionable findings. Task 4.2 remains unchecked pending the remaining user-performed acceptance and unresolved full-suite results.

## 5. Saved playlists, rules and single document owner

- [x] 5.1 按计划Task 5完成静态/智能类型与创建、渐进参数编辑、detail/picker及独立JSON/M3U/M3U8/PLS入口；Shared复用同controller/launcher和单preview host，保留Settings入口，验证取消/失败后跨页重开、stale preview拒绝及原子应用。
- [ ] 5.2 跑Task 5 Playlists/Shared JVM与desktop compile；实际长规则、创建/rename/delete取消及各文档格式/preview/取消重开，记录无法控制的系统picker缺口、EN/ZH与文档证据；独立review后提交。

Task 5.1 evidence update (2026-10-22):
- Focused Playlist regressions (`PlaylistFeatureDismissalTest`, `SmartPlaylistScreensJvmTest`, `PlaylistBackupInteroperabilitySemanticsJvmTest`, `PlaylistInteroperabilityControllerJvmTest`) passed. Focused Shared document-host regressions (`SettingsPlaylistBackupEmbeddingTest`, `LibraryRouteAdapterJvmTest`) passed; `:desktopApp:compileKotlin` passed.
- `spotlessApply`, separate `spotlessCheck`, `detekt`, and `architectureCheck` passed after the final callback documentation and remove-admission cleanup. The feature compiler used Gradle's fallback strategy after a transient Kotlin incremental-cache `NoSuchFileException`; compilation and tests completed successfully.
- The full `:feature:playlists:impl:jvmTest :shared:jvmTest :desktopApp:compileKotlin` run timed out after 1500s after reporting the recorded `PlaybackSessionCoordinatorTest.terminalCompletionFlushPersistsExactNonWholeSecondDurationWithoutProgress` failure at line 363. No full Shared-suite pass is claimed.
- Read-only `DocumentWorkflowReview` verified current-appearance/snapshot confirmation ownership and single remove admission; it found no merge-blocking defect. The tautological appearance comparison was removed and a rapid double-confirm removal regression was added.
- The isolated desktop launch did not surface a RhythHaus app in `orca computer list-apps`; no visual or system-document-picker acceptance is claimed. Task 5.2 remains open for the real long-rule and JSON/M3U/M3U8/PLS interactions, including cancel/retry and return-to-origin behavior.

## 6. Settings, source reports and information surfaces

- [x] 6.1 按计划Task 6重排五组、来源/报告/维护、About/OSL与Onboarding；保持偏好各自gate、unknown output、Android恢复及iOS专属文案，report状态不随回收丢失；现有行为套件验证危险取消/失败和引导/资源加载语义。
- [ ] 6.2 跑Task 6 Settings/Onboarding/Shared JVM与desktop compile；实际偏好、来源scan/取消/报告回收、危险取消、About/许可证/引导及大字返回/确认烟测，更新文档证据，独立review后提交。

Task 6.1 evidence update (2026-10-22):
- Added and observed RED for `configuredEmptySourceRemainsClearable`: a configured source with zero indexed tracks had no `settings-clear` node. Settings now renders clear when tracks or sources exist; Shared applies that same current-state eligibility to request and confirm, preserving the existing mutation gate and public API.
- The route regression passes with one source/zero tracks, the source/track confirmation counts, and one confirmed clear callback. Existing source removal and remove-missing cancel/failure/retry/stale-session regressions remain in the complete `SettingsRouteAdapterJvmTest` pass.
- The plan-selected Settings/About/Onboarding/Shared behavior command passed (`BUILD SUCCESSFUL` in 18s; 158 tasks executed). Full Settings and Onboarding JVM suites passed (`BUILD SUCCESSFUL` in 7s; 56 tasks executed). `spotlessApply`, separate `spotlessCheck`, `detekt`, `architectureCheck`, and `:desktopApp:compileKotlin` passed as separate Gradle commands.
- About and onboarding tests contain no pixel-capture/color assertions; the Settings-only disabled-danger boundary retains its palette and disabled-semantics check.
- The full Shared-suite attempt did not return success: it timed out after 1200s, with the known playback-duration baseline failure and an in-scope danger-color assertion reported. The corrected danger boundary passes in the focused Task 6 suite; no full Shared-suite pass is claimed.
- An isolated 800×600 desktop launch exposed the Settings route and Chinese Settings labels in the accessibility tree. Scrolling fell back to synthetic input without verified movement; About/OSL, onboarding, five-group traversal, locale/size/font matrix, and destructive UI interaction remain unverified. The `:desktopApp:run` process was intentionally terminated after the smoke and exited 143; this is not claimed as a successful Gradle run.
- Task 6.2 remains unchecked for full Shared verification, user-owned visual/interaction acceptance, independent review, and commit.

## 7. Input, focus, motion and remaining token consumers

- [x] 7.1 按计划Task 7完成焦点/键盘、各inactive层pointer/gesture/semantics与motion零比例适配，去除剩余旧呈现分叉而不删在用公共能力；通过现有消费边界的隐藏/被覆盖/离场动作、focus恢复与Back一次结算回归。
- [ ] 7.2 运行受影响既有JVM套件，实际Tab/Enter/Space/Esc、读屏、减少动态效果或可丢弃harness零比例、大字/浅深全surface巡检；无新占位快捷键/开关，记录文档、独立review后提交。

Task 7 automated evidence (2026-10-23):
- RED: the focused `TrackSelectionSemanticsJvmTest.focusingSelectionActionShowsContrastingOutline` failed against a one-color focus boundary; its outer-edge pixel was white instead of the required outline color. GREEN: the same test passed with separate outer-surface and inner-fill focus bands.
- Fresh affected Core UI and feature JVM suites passed with `--rerun-tasks` (`BUILD SUCCESSFUL`, 170 tasks executed). Fresh Shared interaction suites (`LibraryAppShellJvmTest`, `TrackSelectionSemanticsJvmTest`, `PlaylistBackPolicyJvmTest`, `AppNavigationChromeJvmTest`) passed with `--rerun-tasks` (`BUILD SUCCESSFUL`, 155 tasks executed).
- `spotlessApply`, separate `spotlessCheck`, separate `detekt`, separate `architectureCheck`, and `:desktopApp:compileKotlin` each passed with configuration cache. No full Shared-suite pass is claimed; the existing playback-duration timeout/failure remains documented above.
- Pixel assertions exercise both focus bands; a numerical regression checks the 3:1 focus-color pairs against paper/panel/navigation, selected ink, pulse, and danger surfaces in light and dark palettes.
- Independent Task 7 review: the Standards axis found no documented-standard breach and recorded two non-blocking Minor smells, deferred in the SDD ledger. The Spec axis flagged the covered-player focus claim and the token-consumer sweep. The token finding is fixed: LibraryRows uses `HausShapes.control`/`artwork`, and LibraryDialogs uses `HausTypography.section`/`secondary`.
- The covered-player Tab/Shift-Tab finding was not reproduced. `HausWindowOverlayHost`'s overlay focus group cancels any focus exit; `playerDialogRestoresItsInvokerAndTrackReplacementReturnsFocusToPlayer` checks eight forward Tabs and Shift-Tab and finds no focused player descendant. The technical ruling and residual risk are recorded in the SDD ledger.
- Fresh post-review verification (2026-10-23): separate `spotlessApply`, `spotlessCheck`, `detekt`, and `architectureCheck` commands passed. The focused disabled-action pointer regression passed with `--rerun-tasks`; Core UI and Library JVM suites, desktop and Android Kotlin compilation, and Shared iOS simulator main compilation passed with `--rerun-tasks`; the four Shared interaction suites passed with `--rerun-tasks`. `openspec validate studio-ui-redesign --strict` reported the change valid. No full Shared-suite, Android package assembly, iOS simulator test, or user-performed GUI acceptance is claimed.
- Orca exposes no accessibility window. Tab/Shift-Tab/Enter/Space/Esc, screen reader, reduced-motion host setting, large-text, and full light/dark surface acceptance remain user-owned and unverified. Task 7.2 remains open; no commit.

## 8. Integrated acceptance and evidence closeout

- [x] 8.1 分别运行spotlessApply、独立spotlessCheck、独立detekt和architectureCheck（均带--configuration-cache），再跑全部受影响JVM、./init.sh及shared iOS simulator framework链接；记录确切成功/阻塞输出，不把部分构建记作全平台通过。
- [ ] 8.2 执行计划Task 8的真实desktop/可用Android/iOS运行与尺寸×语言×主题×字体全矩阵，逐项核对所有spec scenario和调用/文案/状态完整性；设备/听感/系统控制由用户另验，缺少运行目标不得代签通过。
- [ ] 8.3 获得fresh全范围review、修复证据支持的问题，运行openspec validate studio-ui-redesign --strict；更新progress/roadmap的证据与精确缺口，仅勾实际完成项，满足仓库完成规则后提交整体checkpoint。

Task 8.1 evidence (2026-10-23; verification execution recorded, not full-platform acceptance):
- `./gradlew spotlessApply --configuration-cache`, separate `./gradlew spotlessCheck --configuration-cache`, separate `./gradlew detekt --configuration-cache`, and `./gradlew architectureCheck --configuration-cache` passed.
- `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --configuration-cache --console=plain` passed in 36 seconds. Kotlin/Native emitted a bundle-ID inference warning and used `Shared`.
- `/usr/bin/xcrun xcodebuild -version` reported Xcode 27.1 (27A9275); this is the toolchain check only, not an iOS simulator test.
- The broad affected Core UI / Core Playback / feature JVM command timed out at 3600 seconds. Previously recorded Core Playback failures remain unresolved; unchanged files do not establish baseline provenance for every failure. The independently bounded `:core:ui:jvmTest :feature:library:impl:jvmTest :feature:search:jvmTest :feature:nowplaying:jvmTest :feature:playlists:impl:jvmTest :feature:settings:jvmTest :feature:onboarding:jvmTest :desktopApp:compileKotlin --configuration-cache --console=plain` passed (`BUILD SUCCESSFUL in 671ms`; 184 tasks, 30 executed).
- Nine Shared presentation integration classes passed together in 14s (155 tasks, 25 executed): `LibraryNavigationTest`, `LibraryAppShellJvmTest`, `TrackSelectionSemanticsJvmTest`, `PlaylistBackPolicyJvmTest`, `AppNavigationChromeJvmTest`, `SettingsRouteAdapterJvmTest`, `SettingsPlaylistBackupEmbeddingTest`, `LibraryRouteAdapterJvmTest`, and `AppOnboardingLifecycleJvmTest`.
- The full `:shared:jvmTest --rerun-tasks --configuration-cache --console=plain` timed out at 3600 seconds after reporting `PlaybackSessionCoordinatorTest.terminalCompletionFlushPersistsExactNonWholeSecondDurationWithoutProgress` (line 363) and `CarPlayFacadeTest.transportCommandsRouteThroughTheSharedPlaybackController` (wrapper line 402). Only the former has prior baseline evidence; the CarPlay failure is unclassified. A scoped diff against the redesign base is empty for CarPlay production/tests and `core/playback`, but that alone is not baseline proof. No full Shared pass is claimed.
- The latest `./init.sh` exited 1 (`BUILD FAILED in 1s`, three Android TagLib helper failures) before its iOS test stage. Native helper builds for `arm64-v8a`, `x86_64`, and `armeabi-v7a` cannot find `/Users/eterocell/Library/Android/sdk/ndk/30.0.15729638/build/cmake/android.toolchain.cmake`; CMake also reports no Unix Makefiles build program. No toolchain was installed or changed.
- Standalone `:shared:iosSimulatorArm64Test --configuration-cache --console=plain` compiled and linked the native test executable, then timed out at 3600 seconds after reaching `:shared:iosSimulatorArm64Test`. No test-success summary was returned; iOS tests are not passed.
- `openspec validate studio-ui-redesign --strict` passed. Task 8.1 checks off command execution and exact blocker recording only; 8.2/8.3 and the overall redesign remain incomplete.

Task 8.2 runtime evidence (2026-10-23; user-only acceptance deferred by request):
- Real unsigned simulator host build passed: `xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /tmp/rhythhaus-studio-ui-simulator-20261023 CODE_SIGNING_ALLOWED=NO build` (`BUILD SUCCEEDED`, 26.20s). Future Xcode log capture should use `-hideShellScriptEnvironment`; raw output is not a shareable evidence artifact.
- Installed/launched on the existing iOS 27.1 iPhone Duo simulator. Its boot status reported `Data Migration Failed`, but install/launch succeeded and a settled screenshot visibly shows the real Chinese dark empty-library screen, source action, view filters, Settings, and all three primary navigation actions with no playing track. This is a 466×678-point launch smoke only, not the prescribed size matrix, interaction acceptance, or an iOS test-suite pass. The first immediate screenshot showed SpringBoard and is not application evidence.
- `adb devices -l` listed no Android target; Android packaging remains blocked above. Existing desktop smoke evidence remains limited as recorded in Tasks 2/4/6/7.
- Every cell in `{360×640,390×844,800×600,1280×800} × {EN,ZH} × {light,dark} × {default,200% font}` remains unaccepted for each platform. Per user instruction, do not request intervention or mark these cells passed. All-surface traversal, system pickers, keyboard/screen reader, audible playback, physical route loss, and system-media acceptance remain deferred, not removed from scope.

Task 8.2/8.3 review and changed-path evidence (2026-10-23):
- Fresh whole-change read-only `StudioRedesignReview` covered the current tracked/untracked worktree, approved design/plan and all eight delta specs. Its scenario-to-production/regression map is in `.superpowers/sdd/2026-10-19-studio-ui-redesign/progress.md`; this is source review, not a claim of full-matrix runtime acceptance.
- The one Important requirement finding was missing active timer status in Controls. Root cause: only setup consumed `sleepTimerState`. A production-boundary regression failed before the fix and passes afterward: compact 390×844 and wide 1280×800, timed arm/return, completion-count replacement, cancellation clearing visible/accessibility status. Both layouts now pass authoritative state into Controls; the existing localized summary wraps below the secondary row and becomes the Timer action's accessibility state. Keeping the short action label avoids crowding Queue. Fresh read-only re-review found no blocker.
- Post-fix `./gradlew :feature:nowplaying:jvmTest :desktopApp:compileKotlin --configuration-cache --console=plain` passed (`BUILD SUCCESSFUL in 23s`; 148 tasks, 31 executed); separate `spotlessApply`, `spotlessCheck`, and `detekt` commands passed. The native Swift/Compose host rebuilt successfully in 40.72s using the preceding Xcode command plus `-hideShellScriptEnvironment`.
- A new isolated iOS 27.1 simulator completed four-page English onboarding/Finish and scanned a sandbox WAV. The real player exposed duration status after returning from setup, replaced it with count/fade status, and cleared visual and AX state after cancellation. Primary-display screenshot `/tmp/rhythhaus-studio-timer-active-primary-20261023.png` shows a subsequently re-armed three-track/fade-off status at 466×678 points in English/light. The generated WAV failed playback with visible recovery actions: no audible-playback/seek success is claimed. An optional Settings probe hit simulator system controls, not app Settings, and is excluded. The isolated device/helper/media sandbox were shut down and deleted; existing simulator data was untouched.
- Full Shared/Core Playback results, Android native packaging and native iOS test execution remain non-green/blocked as recorded above. The prescribed full matrix and user-only acceptance remain deferred. Keep mixed Tasks 2.2–7.2, 8.2 and 8.3 unchecked; fresh review alone does not satisfy their runtime/commit clauses. No final checkpoint commit or archive was made in this continuation.

## Workflow follow-up

- 用户已批准实施计划；按任务顺序执行并保留独立审查与验证证据。
- 全部实施验收与独立review齐备后按OpenSpec archive流程同步六个新增spec及两个完整修改块，不把归档本身列作提前勾选的实施任务。
- 原Phase 1实体设备/系统媒体验收与本呈现重设计分别保留；不得因mockup或编译通过改变其状态。
