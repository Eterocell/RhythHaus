# Design

## Context

动机见 [proposal.md](proposal.md)。用户已于2026-10-19批准[Studio设计全文](../../../docs/superpowers/specs/2026-10-19-studio-ui-redesign-design.md)与[实施计划](../../../docs/superpowers/plans/2026-10-19-studio-ui-redesign.md)。下面的新增 Kotlin 接口是设计决定；实现与验证状态以任务清单为准。

现状：`Theme.kt/HausColors.kt` 只有七色；Shared `App.kt::RhythHausTheme`装配Miuix与Haus palette。`LibraryAppShell.kt`使用42/58双栏，`LibraryNavigation.kt::libraryAdaptiveLayoutModeFor`在840dp或横向600dp就展开。Search有独立歌曲行；Queue及保存草稿藏在`PlaylistScreens.kt::PlaylistHubScreen`；`NowPlayingContent`本地记忆歌词弹层。Settings已有中立投影、scan/backup插槽与操作门禁。所有这些均保留领域所有者，不以新UI复制状态。

## Goals / Non-Goals

**Goals:** 同一 Studio 视觉系统；按实际剩余空间布局；迁移所有现有入口和完整状态；每个切片可编译、可运行、可独立审查。

**Non-Goals:** 新导航运行时、独立根栈、跨页面选择缓存、新数据库/API/播放器行为、原生系统媒体或CarPlay外观、新依赖/字体/工具链、功能占位或长存双外壳。

## Decisions

### 1. 扩展现有主题而不建立平行主题

保留`HausColorPalette`现有七个有效角色：`ink`主字、`paper`页面、`muted`次字、`line`装饰线、`panel`表面、`panelStrong`选中表面、`pulse`主操作。新增`onPulse/navigation/outline/danger/dangerPanel/onDanger/warning: Color`，严格映射获批设计第4节14个色对。新增`HausTypography/HausSpacing/HausShapes/HausMetrics`常量对象，静态构造TextStyle/Shape，不在每行重建。不得新增旧色别名。

`App.kt`仍是主题装配根：Miuix primary/secondary按钮的文字改用`onPulse`，不是把背景色当作强调色上的文字。扫过所有当前palette消费者，区分主操作和破坏操作；共享按钮/输入/弹层/返回/顶栏首先使用tokens，feature切片完成其余页面密度。对比度验收测试色对性质，不测试色值复制。

替代方案：仅改七色最小，但无法区分危险/选中；引入第二个StudioTheme会让过渡页和弹层出现两套权威，因此不采用。

### 2. 布局留在Shared，不把导航模型放进Core

新增Shared内部`AppShellLayout.kt`：`AppNavigationLayout { Bottom, Rail, Sidebar }`、`appNavigationLayoutFor(widthDp: Float)`，断点720/1000dp。`AppNavigationChrome.kt`渲染底部/轨/侧栏，回调仍进入`pushRoute`，Home仍`popToRoot`。主入口高亮由路由映射得出，Settings/About/Onboarding为null。设置工具入口独立；侧栏收藏/最近仅切换现有Home browse mode。

`libraryAdaptiveLayoutModeFor(widthDp: Float, heightDp: Float, hasDetail: Boolean)`原名保留、签名整体迁移，先减导航208dp和左右32dp，再限正文1440dp；只有窗口≥1000且`hasDetail`且净内容≥744dp才ListDetail。打开新详情时以候选`hasDetail=true`计算既有wide-detail replacement策略，渲染时使用真实当前详情。800×600无空列，1000宽也可能因净空间不足回退。

在一个外壳中按模式重排存活destination内容；不能用全局模式作`key`重建页面。继续现有destination identity、滚动/查询所有权。仅resize不清选择；push/pop仍清选择。与保留三个根栈相比，单栈避免未经批准的状态与返回改变。

### 3. 原子分离导航与迷你播放器

稳定导航由Shared渲染；`LibraryShellBottomBar`只负责互斥的播放/选择停靠区。修改`NowPlayingBar`为非空`Track`，去掉search/settings回调、标签、空库导航分支和装饰backdrop参数；迁移全部调用与删除`BottomBarMode`。现有expand动画/手势协议暂保留；真实下一首和队列按钮在播放器切片接通，不提前显示空动作。

Shared用实际`onSizeChanged`测量导航与当前dock；组合一次系统底部insets，不把navigationBarsPadding重复算两次；所有route得到同一`bottomContentPadding`。切换kind/尺寸后旧测量失效，alpha为0的未测量dock不接受输入或语义动作；滚动只影响dock。扩展播放器/引导/独占层不保留可操作的底层导航。不要把当前visibility bool当作是否有track的证据。

### 4. 一种无领域依赖的歌曲行，适配器留在feature

新增Core `HausSongRow.kt`，仅接收字符串、布尔量、labels、点击/选择回调与artwork/indicator/trailing composable slots；不接收`Track/LibraryTrack/PlaybackState`或生成Res。行渲染不拥有选择、favorite或播放状态，不解析队列。Library `TrackRow`、Search私有结果行、Playlist/Queue occurrence适配器共同使用它，队列继续以occurrence ID作key，不以track ID折叠。

Home与详情仍调用`visibleTracksForBrowseQuery/visibleTracksForBrowseMode`，Search仍本地query/filter。控件改为具名视图/排序筛选弹层及活动条件；元数据菜单继续用现有draft/editor。保留资源所有权；删除被取代的纯样式/文案钉死测试，不把旧字号换个数继续钉死。普通点击/长按/可访问选择的分派仍是原有业务合约。

### 5. 播放器通过slot组合队列，绝不新增feature实现依赖

新增NowPlaying公开呈现枚举`NowPlayingPanel { Controls, Lyrics, Timer, Queue }`。`NowPlayingContent`与Shared facade整体新增`activePanel/onPanelChange/queueContent`参数；Shared拥有当前panel及appearance身份，关闭播放器或替换当前曲目时归一化至Controls，无歌词时不可进入Lyrics。feature只画reader/timer/slot，不安装平台Back、不持有第二个可见性bool。

Shared将panel作为当前destination的`LibraryBackSurfacePort`注册：临时队列确认/命名先于panel关闭，panel关闭先于player关闭，之后才路由；所有完成仍走现有pending/settlement协议。队列feature的已有`PlaylistFeatureDismissalPublisher`经Shared adapter接入，不扩散到NowPlaying类型中。焦点恢复仅在相应appearance仍存活时进行。

从`PlaylistHubScreen`提取公开`PlaybackQueueContent`（同包新`PlaybackQueueContent.kt`），将已有`QueueTabScreen`、命名快照/结果、clear确认及其dismissal一并迁移；复用当前PlaybackState和suspend mutation反馈，不复制队列权威。Shared将该内容作为`queueContent`传给NowPlaying。队列入口、完整播放器布局、叶层Back、移除旧Queue/Saved tabs与`PlaylistTab/SelectTab`在同一可编译切片完成；不能只移动队列行而遗失保存草稿/门禁。

替代方案：NowPlaying依赖Playlists impl违反依赖图；另建队列repo或queue ViewModel复制业务权威；保留Queue tab会造成双入口分叉，均不采用。

用户音量不在现有NowPlaying/Shared合约中，`setPlaybackGain`为内部增益组合，不可借用为滑杆。原型音量不作为正式功能；桌面队列接通，音量仍用系统控制。

### 6. 保存列表和文档操作沿用一个所有者

Playlist页呈现静态/智能类型、类型选择创建和渐进规则编辑；没有新增规则。JSON与M3U/M3U8/PLS依然分别调用App现有`PlaylistBackupController`与系统launcher。将现有`PlaylistBackupSettingsHost`的单份渲染/foremost dismissal绑定到当前Settings或Playlist destination，入口回调共享原门禁；不能同时组成两个host来竞争同一preview。仅公开所需slot/回调，Settings仍不依赖Playlist类型。

### 7. 设置和状态保持feature/Shared所有权

五组仅重排现有Settings参数与slots；来源恢复、scan报告与清库仍由Shared映射，报告状态不放进recyclable list item。About/OSL与Onboarding直接复用tokens/组件，不动资源加载、持久化和平台权限。所有新文案在实际feature的EN/ZH catalog；删除旧入口的死文案，不移动整套资源。

## Risks / Trade-offs

- 全局palette影响所有页面 → 第一任务跑所有受影响既有JVM行为套件与真实桌面遍历；其后每切片迁移剩余固定样式，不把中间状态宣称完成。
- 800dp宽不再双栏 → 内容利用率提升但详情为单任务；断点与净空间边界测试防止再次固定比例。
- panel增加现有Back目标 → 在Shared public/production路径验证确认→panel→player→route及预测取消/结算，不只测试内部枚举。
- 字体/安全区使固定最小高度不足 → 所有尺寸是下限；实际测量、可滚动内容及200%字体运行矩阵验收。
- 组合slot可能重复document host/报告状态 → 保留单操作owner与destination级状态；UI集成验证取消/失败后重开与列表回收。
- 系统减少动态效果当前无项目自有bridge → 使用Compose的`MotionDurationScale`及宿主已有动画比例；自定义位移/手势settlement在比例为0时直接到最终值，不造偏好存储或新增native bridge。实际平台验证不能由HTML代替。
- 整体重设计不能等于已完成Phase 1设备/听感验收 → 分开记录，原来的manual项保持原状态。

## Migration Plan

按实施计划的八个可审查任务顺序推进：主题基础 → 外壳/迷你播放器原子切换 → Library/Search/共用行 → 完整播放器/Queue原子迁移 → 保存列表/规则/文档 → Settings/报告/信息/引导 → 输入/动效/全量token收口 → 三平台集成验收与closeout。一个活跃产品变更；研究可并行，实现与共享接口按依赖顺序。

每任务：先在既有消费边界建立或更新真正行为回归，删除过时纯样式测试，观察必要RED；一次实现所有调用；执行focused行为检查和真实UI烟测；审查后才commit checkpoint。视觉布局用实际应用/截图证明，不增加永久像素或源码文本测试。无数据库/依赖迁移。退回上个已验证checkpoint只可由用户批准的非破坏性修复/提交完成，不执行reset/clean/丢弃用户修改。未全部验收前不归档、不把OpenSpec任务勾完。
