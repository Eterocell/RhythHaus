# Studio UI redesign implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变本地音乐业务与平台所有权的前提下，将所有共享Compose界面完整切换到用户批准的A · Studio设计。

**Architecture:** `:core:ui`扩展现有主题并承载无状态公共原语；各feature保留领域与局部呈现职责，`:shared`继续独占外壳、路由、Back和跨feature组合。采用八个依赖有序、每个可编译且可审查的切片；外壳/迷你播放器与完整播放器/队列分别原子切换，不保留兼容外壳。

**Tech Stack:** 现有Kotlin/Compose Multiplatform、Miuix基础组件、Compose资源与JVM行为测试；Android/iOS/macOS。不更改`gradle/libs.versions.toml`、Gradle插件、JVM目标或依赖。

**Spec:** [已批准系统设计](../specs/2026-10-19-studio-ui-redesign-design.md)；[OpenSpec技术设计](../../../openspec/changes/studio-ui-redesign/design.md)；[规格集合](../../../openspec/changes/studio-ui-redesign/specs/)。执行者必须同时阅读三者。

**Approval:** 系统设计全文与本计划已于2026-10-19获用户批准。下面所有代码接口、测试名和命令仍是待执行要求，不是已实现/已通过证据。现按1→8顺序实施；不提前归档。

## Global Constraints

- “不加入推荐、流媒体、账号、联网封面/歌词、均衡器、输出设备选择或新的音乐管理能力。”
- “不改变数据库schema、播放引擎、扫描协调、iOS导入ABI、媒体文件、CarPlay模板/系统媒体外观或平台权限所有权。”
- “不使用透明度来‘禁用’仍可点击或仍被读屏发现的退场界面。”
- “字体200%时不丢失播放、返回、确认与错误动作；EN/ZH、无封面、极长标题、横屏短高必须验证。”
- “所有新文案进入所属特性的EN/ZH资源，不把品牌式英文副标题强制用作中文操作提示。”
- 断点严格为`<720dp / 720–999dp / ≥1000dp`；导航宽度`0 / 80 / 208dp`，页边距`16 / 24 / 32dp`；正文最大`1440dp`。
- 双栏必须有真实详情且净空间满足`320+24+400=744dp`；800×600不留空详情列。仅resize保持同一destination状态，真实路由仍按旧规则清选择/编辑。
- 导航最小`12sp`；歌曲行/封面下限为紧凑`64/48dp`、桌面`56/40dp`；正常文本对比`4.5:1`，大文字/必要图标/焦点/边界`3:1`；触达`48dp`，iOS不低于`44pt`。
- Home根单栈、Shared Back预测/结算、现有播放事务、queue occurrence和单文档操作所有者不变；无feature→feature impl依赖或新增iOS导出。
- 原型音量不是真实能力：当前没有应用内用户音量API，内部`setPlaybackGain`不可当作用户滑杆。正式界面不显示未接通控制；新增音量不在本计划。
- 一次一个实施任务；每次公开接口变化前查全引用，有LSP必须使用，无LSP时记录后用目标搜索。修改签名与全部消费者在同一任务落地，公开声明有KDoc，不建空模块/状态/兼容别名。
- 测试针对消费者行为/边界；删除旧文案、字号、模糊或源码文本钉死测试，不换值重新钉死。视觉变化主要由真实应用烟测证明；只为不确定边界补永久行为回归，复用并修复受影响的现有套件。
- 每个任务完成自身focused checks、真实运行、独立审查及`progress.md`/`roadmap.md`记录后才提交checkpoint；任何未运行/受阻项写明命令和输出。原来的用户设备/听感验收不可被重设计验收替代。

## Review Focus

1. 窗口跨断点时搜索查询、滚动、选择与destination identity不被布局重建清除；Task 2在生产shell路径验证，真实导航仍应清选择。
2. 离场/未测量/被覆盖的dock、导航、手势与语义动作不得泄漏；Task 2更新现有shell交互回归，Task 7完成所有最高层/焦点输入验证。
3. Queue保存命名中发生换曲、重复occurrence或旧响应时，捕获范围、草稿身份和Back顺序不变；Task 4迁移现有QueueSave与Shared Back边界回归。
4. 从两个入口打开文档操作、取消/失败后换页再重开时，只能有一个owner和一个有效preview/确认；Task 5在现有controller/嵌入集成测试覆盖。
5. 200%字号、短高、长中文/英文、空标签和来源错误时，确认/播放/返回可达且报告状态不随回收丢失；Tasks 3/5/6用现有UI行为边界和真实大字运行验证。

## File structure and ownership

下列是已定位的现有文件或明确要创建的文件；路径均相对仓库根。每任务列出的source前缀与文件名共同构成完整路径，不允许到其他层另建平行实现。

| 位置 | 职责 / 变更 |
| --- | --- |
| `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/Theme.kt`、`HausColors.kt`；新`HausTypography.kt`、`HausDimensions.kt` | 既有palette扩展、排版、间距、形状和尺寸常量 |
| `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/HausDialog.kt`、`BackChip.kt`、`RhythHausTopAppBar.kt`、`HausClickable.kt` | 现有公共交互/弹层使用tokens，保留dismiss契约 |
| 同UI目录新`HausSongRow.kt`、`HausMotion.kt` | 无领域歌曲行；宿主motion比例为0时的自定义动效适配 |
| `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` | Miuix/Haus装配、现有文档owner向路由提供同一host |
| `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`、`LibraryRoutes.kt`、`LibraryAppState.kt`、`LibraryNavigation.kt`、`TrackSelectionBar.kt` | 外壳、测量、Shared路由/Back、跨feature适配；不动App scan admission |
| 同Shared UI目录新`AppShellLayout.kt`、`AppNavigationChrome.kt`、`NowPlayingPanelPresentation.kt` | 导航尺寸/高亮、导航渲染、panel的Shared appearance/Back注册 |
| `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt` | Shared播放器facade；参数与feature同步迁移 |
| `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`、`LibraryRows.kt`、`LibraryDetailContent.kt`、`LibraryBrowser.kt`、`LibraryChrome.kt`、`TrackMetadataEditDialog.kt` | 内容优先Home、六视图、查询入口、网格/行/详情/编辑；保留`LibraryBrowseQuery.kt`投影语义 |
| `feature/search/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt` | 搜索字段/计数/空态及Core行适配；查询仍feature所有 |
| `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt`、`NowPlayingContent.kt`、`NowPlayingAdaptiveLayout.kt`、`SleepTimerPanel.kt`；新`NowPlayingPanel.kt` | mini/full播放器、歌词/定时呈现及无路由含义的panel枚举 |
| `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`、`PlaylistState.kt`、`SmartPlaylistScreens.kt`、`PlaylistPresentationPolicy.kt`；新`PlaybackQueueContent.kt` | saved列表、规则编辑；提取queue及其完整确认/保存生命周期；删除tab状态 |
| `feature/playlists/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/playlistbackup/PlaylistBackupDialogs.kt` | 单文档host与具名操作入口，不重写codec/service/平台桥 |
| `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`、`AboutScreens.kt` | 五组设置、来源/维护、About/OSL呈现 |
| `feature/onboarding/src/commonMain/kotlin/com/eterocell/rhythhaus/onboarding/OnboardingScreen.kt` | 引导样式/滚动/错误，不改持久化/权限 |

各feature EN/ZH catalog沿用其现有目录：Library/Search为模块`src/commonMain/composeResources/values{,-zh}/strings.xml`；NowPlaying为`src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/composeResources/values{,-zh}/strings.xml`；Playlists为`.../library/ui/composeResources/values{,-zh}/strings.xml`及`playlist_interoperability.xml`；Settings/Onboarding为所属Kotlin包下`composeResources/values{,-zh}/strings.xml`。Shared全局导航词用现有`shared/src/commonMain/composeResources/values{,-zh}/strings.xml`。不新建第二套资源根，不修改Gradle资源注册。

## Task 1: Studio tokens and existing common surfaces

**Files:** 上表Core theme四文件（后两新建）、Core既有公共组件、`shared/.../App.kt::RhythHausTheme`。测试：`core/ui/src/commonTest/kotlin/com/eterocell/rhythhaus/theme/ThemeTest.kt`和`ui/HausDialogTest.kt`；既有`core/ui/src/jvmTest/.../ui/HausDialogSemanticsJvmTest.kt`。

**Interfaces:**
- Consumes: `HausColorPalette`、`resolveHausPalette(mode, systemIsDark)`、`LocalHausColors`、Miuix当前theme桥。
- Produces: palette新增`onPulse/navigation/outline/danger/dangerPanel/onDanger/warning: Color`，七个旧角色仍有真实用途而非兼容别名。`HausTypography`公开TextStyle属性`pageTitle/compactTitle/section/title/body/secondary/label`；`HausSpacing`为`xs/sm/md/lg/xl/xxl/section`=`4/8/12/16/24/32/48dp`；`HausShapes.artwork/control/group/overlay`圆角=`8/10/12/20dp`；`HausMetrics.compactSongRowMinHeight/compactArtworkMinSize/desktopSongRowMinHeight/desktopArtworkMinSize/compactMiniPlayerMinHeight/desktopMiniPlayerMinHeight`分别为`64/48/56/40/64/80dp`。

- [x] **Step 1: 建立必要颜色性质回归。** 在`ThemeTest`保留主题解析行为，删除仅复制旧色值的断言；增加`activePalettesMeetReadableActionContrast`，对两套palette的正文/页面、次字/导航、`onPulse/pulse`、`onDanger/danger`断言≥4.5，对`outline/panel`（包含焦点边界用途）断言≥3。不测试setter回显或属性存在。
- [x] **Step 2: 观察RED。** 运行`./gradlew :core:ui:jvmTest --tests '*ThemeTest*' --configuration-cache`；应因缺少新角色/旧色对不足而失败，记录实际输出，不把无关构建失败当目标RED。
- [x] **Step 3: 实现token和公共呈现。** 按获批设计第4节逐项写入14角色浅深值；正文/歌曲16sp/22、次字14/20、标签12/16、区块18/24、页标题28/34、紧凑24/30。`RhythHausTheme`按钮底色用pulse、其上文字用onPulse；dialog保持当前dismiss语义与深色exterior，圆角20。返回、顶栏、输入/按钮消费者改用共享值；不导入新UI库。
- [x] **Step 4: 运行与实际观察。** 运行`./gradlew :core:ui:jvmTest :feature:library:impl:jvmTest :feature:search:jvmTest :feature:nowplaying:jvmTest :feature:playlists:impl:jvmTest :feature:settings:jvmTest :feature:onboarding:jvmTest :desktopApp:compileKotlin --configuration-cache`。启动`./gradlew :desktopApp:run --configuration-cache`，浅/深遍历现有屏幕与一个危险确认，确认无不可读的强调文字或亮色scrim。仅为受影响既有行为修复测试，移除纯旧样式断言。全任务命令曾超时；本任务各feature/core suites及desktop compile fresh通过，Shared danger-dialog专项通过。浅/深Settings和source-removal确认、playlist空态经真实桌面截图观察；Now Playing变更控件通过渲染像素回归。3个Shared shell Settings/report断言仍失败且不在本任务修改的shell/routes文件中，具体证据记录于worktree `progress.md`。未宣称全Shared suite通过或全部页面密度完成。
- [x] **Step 5: 审查并记录checkpoint。** 记录运行证据到progress、roadmap的Studio条目和OpenSpec第1组；独立review后仅提交本任务文件，消息`feat(ui): establish Studio design tokens`。

## Task 2: Atomic shell and mini-player separation

**Files:** 新`shared/.../library/ui/AppShellLayout.kt`、`AppNavigationChrome.kt`；改同目录`LibraryAppShell.kt/LibraryRoutes.kt/LibraryNavigation.kt/LibraryAppState.kt/TrackSelectionBar.kt`；`feature/nowplaying/.../NowPlayingBar.kt`。测试：现有`shared/src/commonTest/.../library/ui/LibraryNavigationTest.kt`、`shared/src/jvmTest/.../library/ui/LibraryAppShellJvmTest.kt`、`TrackSelectionSemanticsJvmTest.kt`、`feature/nowplaying/src/jvmTest/.../NowPlayingBarSemanticsJvmTest.kt`。删除被废弃的`feature/nowplaying/src/commonTest/.../BottomBarModeTest.kt`。

**Interfaces:**
- Consumes: Task 1 tokens、`LibraryNavigationStack.push/popToRoot`、`LibraryBottomBarPresentation`与既有Shared Back协议。
- Produces: 内部`AppNavigationLayout { Bottom, Rail, Sidebar }`、`appNavigationLayoutFor(widthDp: Float): AppNavigationLayout`、`primaryDestinationFor(route: LibraryRoute): LibraryPrimaryDestination?`（Library/Search/Playlists三值）；`libraryAdaptiveLayoutModeFor(widthDp: Float, heightDp: Float, hasDetail: Boolean)`整体更新。现有`NowPlayingBar`改`track: Track`，删search/settings与backdrop参数和空库mode；label仅保留play/pause/albumArt/currentTrackArtistAlbum。不保留旧签名重载。

- [ ] **Step 1: 更新真实外壳边界回归。** `LibraryNavigationTest`添加断点719/720/999/1000，以及净空间边界1015/1016、无detail永不双栏；加入`Library→Album→Search→PlaylistHub`逐次Back及Home回根、当前路由重复不push。`LibraryAppShellJvmTest`沿现有生产入口覆盖无track导航、选择替换停靠、未测量/隐藏层无动作；大/小窗口重排保留当前destination与选择，真实导航清选择。移除旧固定双栏和空库mini导航断言。
- [ ] **Step 2: 观察RED。** 运行`./gradlew :shared:jvmTest --tests '*LibraryNavigationTest*' --tests '*LibraryAppShellJvmTest*' --tests '*TrackSelectionSemanticsJvmTest*' --configuration-cache`；记录新行为不满足的输出。
- [ ] **Step 3: 一次切换外壳与所有mini消费者。** 按接口完成三种导航和一致Settings入口；主入口高亮只由真实route得出。侧栏收藏/最近回Home并改原browseMode；不新增根栈。取消42/58和空detail placeholder；以净宽计算双栏。组合实际nav/dock测量和一次safe inset，向每个route传bottomContentPadding。导航不随scroll隐藏；full player/独占层下方不留活动导航。无track不组成mini；移除`BottomBarMode`、旧labels/callbacks/死文案及相关仅旧样式测试，所有调用同任务迁移。
- [ ] **Step 4: 验证与桌面烟测。** 运行`./gradlew :shared:jvmTest :feature:nowplaying:jvmTest :desktopApp:compileKotlin --configuration-cache`。实际启动，800×600确认无空右列，1280宽打开/返回detail，360/390逻辑宽验证三入口不依赖播放、选择/scroll后最后一行可达；迷你播放器最小高度为紧凑64dp、桌面80dp。切窗口保留查询/scroll/选择，出入真实route仍按旧规则清选择；记录图像与实际输入结果。
- [ ] **Step 5: 审查并提交。** review完整入口可达、无双重insets/透明交互层、所有导出消费者已迁移；更新本任务证据，提交`feat(ui): separate navigation from playback chrome`。

## Task 3: Shared song rows and content-first Library/Search

**Files:** 新Core `HausSongRow.kt`；Library上表Home/Rows/Detail/Browser/Chrome/Metadata文件，Search `SearchScreen.kt`；Shared `LibraryRoutes.kt/LibraryAppShell.kt`的labels/回调适配；playlist `PlaylistScreens.kt`里的静态/queue occurrence行适配（暂不搬queue）。资源按上表所属目录。测试：现有Library `LibraryBrowserTest/LibraryBrowseQueryTest/LibraryHomeContentJvmTest/LibraryBrowseControlsJvmTest/DrillDownViewJvmTest/TrackMetadataEditDialogJvmTest`、Search `SearchFilterTest/SearchSelectionPoliciesJvmTest`、Shared `HomeSelectionPoliciesJvmTest/SearchRouteAdapterJvmTest/TrackSelectionSemanticsJvmTest`。

**Interfaces:**
- Consumes: Tasks 1–2，现有query/visible projection与TrackRow activation契约。
- Produces: 下述Core公开无状态行；Library `TrackRow`保留领域适配职责，所有共同列表使用同一原语。新内部`LibraryViewChooser`、`LibraryQueryControls`只接受已有browse/query与callback，不造业务owner。

```kotlin
@Composable
public fun HausSongRow(
    title: String,
    subtitle: String,
    selectionMode: Boolean,
    selected: Boolean,
    playing: Boolean,
    interactive: Boolean,
    onActivate: () -> Unit,
    onSelect: (() -> Unit)?,
    selectLabel: String?,
    playingLabel: String?,
    desktopDensity: Boolean,
    artwork: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
): Unit
```

- [ ] **Step 1: 修订消费行为回归。** 保留排序/缺元数据/默认recent/duplicate测试；以旧公开screen入口覆盖六视图至多两次动作、活动filter具名清除、metadata经更多菜单进入且取消不save。保持普通行播放可见顺序、选择行/checkbox只toggle一次、favorite与now-playing独立、Search空query不列全部与clear/query selection reconcile；桌面行的更多/等价指针动作仍可发现且不会触发行点击。删除旧brand/import/queue标题位置、8sp分类及重卡片/blur固定断言；不改钉新文案。
- [ ] **Step 2: 观察RED并建立runtime基线。** 运行`./gradlew :feature:library:impl:jvmTest :feature:search:jvmTest :shared:jvmTest --tests '*LibraryHomeContentJvmTest*' --tests '*LibraryBrowseControlsJvmTest*' --tests '*DrillDownViewJvmTest*' --tests '*TrackMetadataEditDialogJvmTest*' --tests '*SearchSelectionPoliciesJvmTest*' --tests '*HomeSelectionPoliciesJvmTest*' --tests '*SearchRouteAdapterJvmTest*' --tests '*TrackSelectionSemanticsJvmTest*' --configuration-cache`。记录行为失败；视觉不新增永久像素测试。
- [ ] **Step 3: 实现行与浏览。** Core行只负责呈现、primary/long-press/accessible selection；selection checkbox不冒泡产生第二次toggle，trailing动作不触发行点击。Library/Search/playlist适配器保留各自labels和domain keys。Home删常驻非空导入/brand/playlist CTA，compact具名chooser、wide侧栏与appState同步；网格用min128/160和gap16/20推导。保留投影函数，排序/筛选只重排UI。编辑更多菜单复用原draft保存/restore；Search继续本地过滤/计数/焦点。所有空态区分，不复制App source/scan state。
- [ ] **Step 4: 验证行为及真实内容。** 跑`./gradlew :core:ui:jvmTest :feature:library:impl:jvmTest :feature:search:jvmTest :feature:playlists:impl:jvmTest :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`。实际运行360×640非空页首排专辑可见；EN/ZH、浅深、200%字体观察长标题/无封面/metadata取消/无结果清除、detail可见顺序播放和末行。保留符合真实保留实现的artwork lazy identity/cancellation测试，删除随旧chrome退役的几何测试。
- [ ] **Step 5: 审查并提交。** reviewer查Core无domain imports、feature无新impl边、资源无跨所有权、选中不播放与重复occurrence不折叠；记录证据，提交`feat(ui): redesign library and search browsing`。

## Task 4: Atomic full player and Queue migration

**Files:** 新`feature/nowplaying/.../NowPlayingPanel.kt`、`feature/playlists/impl/.../library/ui/PlaybackQueueContent.kt`、`shared/.../library/ui/NowPlayingPanelPresentation.kt`；改NowPlaying Content/AdaptiveLayout/Bar/SleepTimerPanel，Shared facade/Shell/State/Navigation/Routes，以及PlaylistScreens/PlaylistState和所属资源。测试：现有NowPlaying `NowPlayingContentSemanticsJvmTest/SleepTimerPanelSemanticsJvmTest/NowPlayingBarSemanticsJvmTest/NowPlayingAdaptiveLayoutTest`；Playlists `PlaylistQueueSaveJvmTest/PlaylistFeatureDismissalTest/PlaylistStateTest/PlaylistScreensTest`；Shared `LibraryNavigationTest/PlaylistBackPolicyJvmTest/LibraryAppShellJvmTest`。

**Interfaces:**
- Consumes: Task 3行、现有PlaybackState/Controller/QueueMutationFeedback与PlaylistFeature dismissal publisher。
- Produces: `public enum class NowPlayingPanel { Controls, Lyrics, Timer, Queue }`；`NowPlayingContent`、Shared `NowPlayingScreen`整体增加必填`activePanel: NowPlayingPanel`、`onPanelChange: (NowPlayingPanel) -> Unit`、`queueContent: @Composable () -> Unit`。
- 新公开`PlaybackQueueContent`参数：`playbackState: PlaybackState`、`destination: PlaylistFeatureDestination`、`appearanceSource: PlaylistFeatureAppearanceSource`、`dismissalPublisher: PlaylistFeatureDismissalPublisher`、`onSaveQueueAsPlaylist: (String, List<String>, (PlaylistStateAction) -> Unit) -> Unit`、`onReorderUpcoming: suspend (String, Int) -> QueueMutationFeedback`、`onRemoveUpcoming: suspend (String) -> QueueMutationFeedback`、`onClearUpcoming: suspend () -> QueueMutationFeedback`、`artworkLoader: suspend (String) -> ByteArray?`、`onClose: () -> Unit`、`modifier: Modifier = Modifier`。命名/clear草稿随该surface保留，不上移为第二个queue owner。
- Shared内部panel呈现以destination、appearance和当前track身份约束，注册现有`LibraryBackSurfacePort`；临时确认→panel→player→route。同一active destination的队列modal不被panel port遮掉。

- [ ] **Step 1: 迁移真实Queue保存与Back回归。** `PlaylistQueueSaveJvmTest`改由新queue public surface触发，保持A/B/A重复、played prefix排除、命名时queue变化仍旧snapshot、取消、失败保留draft、重复确认和旧响应隔离；明确覆盖队列重排的非拖拽上移/下移及移除替代动作。Shared既有生产shell回归加queue命名Back后仍queue、queue Back后仍player、再Back关player及预测取消；换曲无歌词清reader。保留seek/timer/recovery既有契约；删仅旧tabs/本地lyrics bool/封面绘制细节测试。
- [ ] **Step 2: 观察RED。** 运行`./gradlew :feature:playlists:impl:jvmTest --tests '*PlaylistQueueSaveJvmTest*' --tests '*PlaylistFeatureDismissalTest*' --configuration-cache`与`./gradlew :shared:jvmTest --tests '*PlaylistBackPolicyJvmTest*' --tests '*LibraryAppShellJvmTest*' --configuration-cache`，预期新surface/Back层级尚未满足，记录输出。
- [ ] **Step 3: 原子实现并迁移。** queue行、save命名/结果和clear确认一并提取；Shared注入queueContent，feature不加Playlists impl边。full player只一组transport；定时改具名入口/状态，歌词仅非空可进；panel可见性移交Shared，删除旧`lyricsVisible`。窄屏queue近全高，宽屏controls/artwork各至少320/320、gap24、总内边距48；不足回纵向；侧栏再需280+24净宽，非以机型判断。mini真实next使用`skipToNext`、desktop queue展开player+Queue，不显示虚假音量。删除PlaylistTab/SelectTab/Hub tab UI及所有引用；保留其余state/mutation/保存事务。
- [ ] **Step 4: 验证并实际播放。** 运行`./gradlew :feature:nowplaying:jvmTest :feature:playlists:impl:jvmTest :shared:jvmTest :core:playback:jvmTest :desktopApp:compileKotlin --configuration-cache`。启动真实应用，用可控本地媒体播放/暂停/seek、纯文本/时间歌词、timer、重复queue保存/取消、clear确认和连续Back。800×600与1280×800、200%字体验证核心控制可达及无第二套transport；无歌词换曲不得显示旧reader。
- [ ] **Step 5: 审查并提交。** reviewer检查controller identity revalidation、保存快照/旧响应、foreground port先后、预测取消和依赖图；记录证据，提交`feat(ui): integrate Studio player and dedicated queue`。

## Task 5: Saved lists, smart editor and one document workflow

**Files:** PlaylistScreens/SmartPlaylistScreens/PlaylistPresentationPolicy、PlaylistBackupDialogs；Shared App/LibraryRoutes；Playlists所属strings与playlist_interoperability资源。测试：既有`SmartPlaylistScreensJvmTest/PlaylistEditModeSemanticsJvmTest/PlaylistFeatureDismissalTest`、`PlaylistBackupInteroperabilitySemanticsJvmTest/PlaylistBackupControllerTest/PlaylistInteroperabilityControllerJvmTest`、Shared `settings/SettingsPlaylistBackupEmbeddingTest.kt`和`library/ui/LibraryRouteAdapterJvmTest.kt`。

**Interfaces:**
- Consumes: Task 4的saved-only hub与queue切走后的state，现有SmartPlaylistRule和单PlaylistBackupController/launcher。
- Produces: Hub的`documentActions: @Composable () -> Unit`插槽（对应Shared facade同步）；`PlaylistBackupSettingsHost`增加必填`showEntryActions: Boolean`，modal/preview仍单份。Shared只在活动Settings或Playlist destination组成host，`showEntryActions`决定入口由本页插槽显示还是host显示；不复制controller/preview state。原JSON/format callbacks原样复用。

- [ ] **Step 1: 更新已有工作流边界回归。** 断言static/smart可分别打开/创建；切rule只显示其参数、长列表确认可达；已有source失效语义不变。现有document测试加入从Playlist开操作、取消后去Settings重开、失败重试、旧preview失效拒绝，断言单一活动操作、原子导入、未匹配/歧义计数和smart不进静态JSON。删除旧“Saved tab”存在断言。
- [ ] **Step 2: 观察RED。** 运行`./gradlew :feature:playlists:impl:jvmTest --tests '*SmartPlaylistScreensJvmTest*' --tests '*PlaylistBackupInteroperabilitySemanticsJvmTest*' --tests '*PlaylistInteroperabilityControllerJvmTest*' --configuration-cache`和`./gradlew :shared:jvmTest --tests '*SettingsPlaylistBackupEmbeddingTest*' --tests '*LibraryRouteAdapterJvmTest*' --configuration-cache`。
- [ ] **Step 3: 完成saved呈现和单host适配。** 列表类型具名、创建先选类型，静态detail/picker用Core行；规则分组但不新增规则/组合。菜单/编辑保留原appearance/mutation gate。JSON全静态备份/恢复和单静态列表格式导入导出分别可达，Settings入口仍在；Shared将同owner操作/host/dismissal注入活动页，host不能在两个route同时持有有效确认。
- [ ] **Step 4: 验证与实际文档交互。** 运行`./gradlew :feature:playlists:impl:jvmTest :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`。实际打开长参数列表、命名/rename/delete取消；用临时目的目录执行JSON与M3U/M3U8/PLS选择、预览、取消/失败后再次请求、Back返回原页。未能自动控制的系统picker如实记缺口，不用假的成功结果代替。
- [ ] **Step 5: 审查并提交。** 查一个controller/bridge、无双host、格式和覆盖文案准确、无smart规则/权限新行为；记录证据，提交`feat(ui): redesign saved playlist workflows`。

## Task 6: Settings, source reports, About and onboarding

**Files:** SettingsScreen/AboutScreens、OnboardingScreen、Shared LibraryRoutes/LibraryDialogs/LibraryAppShell的scan/清库slot（不动App协调器）及所属资源。测试：现有Settings `SettingsScreenSemanticsJvmTest/SettingsPolicyTest/AboutScreensJvmTest`、Onboarding `OnboardingScreenJvmTest/OnboardingPolicyTest`、Shared `settings/SettingsRouteAdapterJvmTest.kt`、`onboarding/AppOnboardingLifecycleJvmTest.kt`和`library/ui/Task3ReviewSemanticsJvmTest.kt`。

**Interfaces:**
- Consumes: 现有Settings labels/projections、`sourcePickerActionVisible/sourcePickerAvailable/mutationsEnabled`、scan/outcome/backup slots与Task 5单host。
- Produces: 不新增领域/偏好API；原public signatures以现有callback为准。报告展开/清除确认state固定在Settings destination，不移入LazyColumn item；持久偏好与source ID适配仍Shared所有。

- [ ] **Step 1: 修订现有行为回归。** 五组重排后仍分别操作ReplayGain/crossfade/gapless/resume；unknown output无伪设备；通知恢复只适用Android；picker visibility≠availability≠mutation gate。报告展开→滚出→滚回仍保持状态，scan失败不得覆盖前次完成结果，remove-missing只可用于原eligible scan。clear/remove取消无 mutation、pending禁二次confirm、失败保留真实结果；About重试与引导review/skip/finish不变。
- [ ] **Step 2: 观察RED。** 运行`./gradlew :feature:settings:jvmTest :feature:onboarding:jvmTest :shared:jvmTest --tests '*SettingsScreenSemanticsJvmTest*' --tests '*AboutScreensJvmTest*' --tests '*OnboardingScreenJvmTest*' --tests '*SettingsRouteAdapterJvmTest*' --tests '*AppOnboardingLifecycleJvmTest*' --tests '*Task3ReviewSemanticsJvmTest*' --configuration-cache`。
- [ ] **Step 3: 完成五组与状态。** 保留每个偏好保存/失败，危险动作使用danger tokens与对象/真实文件影响说明；来源恢复与报告slot保持原owner和寿命。About/OSL/Onboarding套用排版/间距/输入层级及可滚动操作，不改加载、权限或持久化。iOS文案只在iOS marker，unknown output不可变设备选择器；cache动作不动library。
- [ ] **Step 4: 验证与实际状态。** 跑`./gradlew :feature:settings:jvmTest :feature:onboarding:jvmTest :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`。实际进入五组、About/许可证、复看引导，360×640和800×600、EN/ZH与200%字体验证返回/确认/错误可达；以可控临时source观察scan/取消/报告回收和危险操作取消，不能删除用户真实资料库来做验收。
- [ ] **Step 5: 审查并提交。** 查无第二偏好存储、报告不绑recyclable item、平台恢复不误现；记录证据，提交`feat(ui): unify settings and recovery surfaces`。

## Task 7: Input, focus and reduced-motion closeout

**Files:** 新Core `ui/HausMotion.kt`；改Core HausClickable/HausDialog/BackChip/RhythHausTopAppBar，Shared AppNavigationChrome/NowPlayingPanelPresentation/LibraryAppShell/TrackSelectionBar，已迁移各feature菜单/reader/editor的enabled/focus边界。仅这些输入/动效与漏用tokens的呈现，不扩大业务重构。测试：现有Core `HausDialogSemanticsJvmTest/VerticalSheetGestureJvmTest/NavigationPrimitivesJvmTest`、Shared `LibraryAppShellJvmTest/TrackSelectionSemanticsJvmTest/PlaylistBackPolicyJvmTest`以及受影响既有feature交互测试。

**Interfaces:**
- Consumes: Tasks 1–6所有真实surface与既有requestLibraryBack；不加平台Back handler。
- Produces: `@Composable public fun rememberHausReducedMotion(): Boolean`读取宿主Compose `MotionDurationScale`的零比例；自定义动画零比例snap、正常150–220ms，不能再次乘以已由Compose应用的比例。无新native bridge、存储或UI开关。既有leaf `interactive/enabled`实参和Shared最高层判断一致，焦点请求以appearance与存活入口为界。

- [ ] **Step 1: 补消费边界的输入回归。** 在现有shell/dialog测试覆盖退出中/未测量/covered层的click、long-press、dismiss/expand与可访问自定义action不触发；关闭foreground层后存活入口可聚焦；零motion比例完成/取消Back仍只结算一次。纯alpha或“未抛异常”不是断言。若发现已存在同路径覆盖，不新增重复行。
- [ ] **Step 2: 观察RED。** 对修改的既有测试类用所属`jvmTest --tests '<class>' --configuration-cache`运行并保存确切目标失败；已满足的合约不造假RED。
- [ ] **Step 3: 完成输入/动效收口。** inactive内容若必须保留离场绘制则逐一移除leaf click/gesture/semantics action与焦点资格，不能以透明度或仅给父层禁用掩盖。Tab顺序和Enter/Space激活跟随视觉任务，Esc进入Shared仲裁；modal关闭按当前appearance恢复焦点。无已接通能力不画⌘K。按consumer清单去除旧圆角/字号/颜色分叉及无消费者的decorative glass代码；不删除仍有其他用途的手势或公共组件。
- [ ] **Step 4: 验证与实际输入。** 跑全部受影响JVM套件，实际桌面键盘Tab/Shift-Tab/Enter/Space/Esc，读屏检查命名和最高层；开启宿主可用减少动态效果或在可丢弃运行harness注入`MotionDurationScale=0`观察最终状态与Back，不留测试专用产品开关。大字/浅深巡检每个surface的焦点和危险状态对比，不把永久截图加入suite。
- [ ] **Step 5: 审查并提交。** 独立检查pointer/gesture/semantic三类输入、focus与pending Back，一并核查所有token消费者已完成迁移；记录证据，提交`feat(ui): complete Studio accessibility and motion`。

## Task 8: Three-platform integration acceptance

**Files:** 仅修复本次真实集成缺陷及对应消费者；记录`progress.md`、`roadmap.md`和`openspec/changes/studio-ui-redesign/tasks.md`。本任务不代替前七任务自己的测试/烟测。

**Interfaces:** Consumes全部前述完成界面；Produces可审计的构建、真实运行矩阵与独立整分支review证据，不新增API。

- [ ] **Step 1: 执行独立质量门禁。** 逐条运行`./gradlew spotlessApply --configuration-cache`、单独`./gradlew spotlessCheck --configuration-cache`、单独`./gradlew detekt --configuration-cache`、`./gradlew architectureCheck --configuration-cache`。formatter输出若改变非本任务文件，先核对用户修改，不擅自覆盖；所有失败进入明确根因调查。
- [ ] **Step 2: 三平台构建与既有行为总回归。** 跑所有本次受影响feature/Core UI JVM任务及`./init.sh`；额外`./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --configuration-cache`验证framework链接。`./init.sh`包括Shared JVM、desktop compile、Android assemble、xcodebuild版本和iOS simulator tests。不把只有desktop compile写作三平台通过；任何既有阻塞记录原始命令/输出与受影响门禁。
- [ ] **Step 3: 实际运行矩阵。** 启动desktop真实应用及可用Android emulator/iOS simulator宿主；尺寸360×640、390×844、800×600、1280×800dp × EN/ZH × 浅/深 × 默认/200%字体，记录逐格结果，不能只截两个页面。每平台遍历Home六视图、detail/search、static/smart与规则、full/mini/lyrics/queue/timer、Settings来源/维护、About/OSL/onboarding；检查最后一行、键盘/安全区/返回和最高层输入。缺模拟器或平台工具时记录缺口，不宣称完成相应自动烟测。
- [ ] **Step 4: 全范围功能/状态审查。** 以每个OpenSpec scenario逐项对照：visible-order playback、多选/收藏同步、空query/无结果、重复queue保存范围/旧响应、长规则、JSON与各互操作格式、无/纯文本/时间歌词、timer/recovery、unknown output、source access/scan终态/危险取消；确认无假内容、stub、死入口、旧tab或兼容路径。单独标记用户负责的手动UI/听感/实体路由与系统媒体验收，不重置既有roadmap状态。
- [ ] **Step 5: 独立review和交接。** fresh reviewer对获批设计、八个spec delta、全部实际产品入口及测试/资源/调用方完整性审查；修复证据支持的问题。`openspec validate studio-ui-redesign --strict`通过后才能勾相应实现/验证项。记录全部证据、精确阻塞和下一owner，完成既定workflow后按仓库要求提交`feat(ui): complete Studio redesign`；无证据不勾完成，不偷偷缩矩阵。

## Design coverage and dependency check

| 获批设计章节 | 执行任务 | 核心证据 |
| --- | --- | --- |
| §1目标/反目标、§8所有权 | 全部；8整体验收 | 全入口功能保留、依赖/ABI/无schema变化审查 |
| §2信息架构、§3尺寸 | 2、3、4 | 单栈Back、净宽/测量/resize、compact/wide实际运行 |
| §4颜色/排版/组件 | 1、3、5、6、7 | 对比度性质、全surface与弹层浅深实际观察 |
| §5Library/Search/编辑 | 3 | 投影/选择/顺序/取消行为与首排可见 |
| §5播放/歌词/队列/定时 | 4 | queue-save/stale/Back、真实播放/reader/恢复 |
| §5静态/智能/文档 | 5 | 两类格式独立、一个操作owner与原子preview |
| §5Settings/来源/About | 6 | report寿命/门禁/平台投影及引导持久语义 |
| §6状态表 | 3–6、8 | 每个状态对应真实可达surface而非空组件 |
| §7无障碍/输入/动效 | 各任务自身；7统一收口 | 200%字体、键盘/读屏、inactive与减少动态效果 |
| §9矩阵、§10证据边界 | 8 | 原生执行与mockup证据分开，设备/听感不冒领 |

依赖固定`1→2→3→4→5→6→7→8`；公开接口在生产者任务内部同时迁移全部消费者，不把不能编译的“合约先行”留给下任务。每个task有独立review门，纯研究可并行但不同时修改Shared。计划已获用户批准；对照规格逐项实施并按每个task的证据门提交检查点。

## Workflow follow-up

- 用户已批准计划；按1→8顺序逐任务实施。OpenSpec planning complete不代表产品任务已完成。
- 全部任务证据和独立review齐备后，由OpenSpec按archive流程同步六个新增规格及两个完整修改块；归档不是本计划中的已完成实施项。
- 用户设备/听感/系统控制验收保持其既有独立记录；自动UI烟测不可代签这些验收。
