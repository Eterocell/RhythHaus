# Tasks

设计全文与[实施计划](../../../docs/superpowers/plans/2026-10-19-studio-ui-redesign.md)已于2026-10-19获用户批准。这里记录实施进度，不把规划完成记作产品完成。按1→8顺序一次执行一个任务，每组自身完成回归、真实烟测、文档与独立审查；具体文件/接口/命令以计划对应任务为准。

## 1. Studio tokens and common surfaces

- [x] 1.1 扩展现有palette和排版/尺寸token、Miuix桥及现有公共弹层/顶栏/返回；把既有pulse危险/失败用途迁移到danger角色，维持选中/播放行正文对比度；按计划Task 1观察颜色性质回归RED/GREEN，验证正文/动作≥4.5:1及必要边界≥3:1，保留主题与dismiss行为。
- [x] 1.2 执行Task 1全部受影响Shared/feature JVM检查、desktop compile与浅深真实页面/危险确认烟测；更新progress/roadmap证据、独立审查并提交本组checkpoint，不能宣称所有页面密度已完成。广泛Shared/desktop命令超时且Shared shell既有3个Settings/report断言仍失败，具体命令、失败名及其不在本任务diff中的证据已记录于worktree `progress.md`；不得据此声称完整Shared suite绿色或所有页面密度完成。

## 2. Atomic shell and mini-player separation

- [ ] 2.1 按计划Task 2一次迁移三入口/轨/侧栏、Settings工具入口、单栈高亮、净空间详情、nav+dock测量与非空mini API；删除空库mini导航/旧mode及全部旧调用，验证断点719/720/999/1000、净宽1015/1016、单栈Back与真实导航清选择。
- [ ] 2.2 运行Task 2 Shared/NowPlaying测试和desktop compile；实际检查800×600无空详情列、无track仍可导航、resize保留存活页面、选择/滚动/末行与inactive输入，记录文档、独立review后提交。

## 3. Content-first Library, Search and shared song rows

- [ ] 3.1 按计划Task 3实现无领域Core行并原子迁移Library/Search/playlist occurrence适配；重设计六视图、具名过滤、网格、详情与metadata菜单，保持投影/顺序/选择/收藏/草稿语义；修改受影响行为回归并删除纯旧样式测试。
- [ ] 3.2 跑Task 3所列Core/Library/Search/Playlists/Shared JVM及desktop compile；真实360×640首排、EN/ZH/200%字、过滤清除/metadata取消/可见顺序播放烟测，更新所属资源与证据，独立review后提交。

## 4. Atomic player and Queue migration

- [ ] 4.1 按计划Task 4整体迁移panel/Shared appearance与Back、full player、歌词/定时、Queue含save/clear草稿及dismissal、mini next/queue；同组移除Hub tabs/PlaylistTab/SelectTab全部引用，保持一个队列权威且不显示虚假音量。通过旧快照/重复occurrence/旧响应/非拖拽重排替代动作/预测Back与层级回归。
- [ ] 4.2 跑Task 4 NowPlaying/Playlists/Shared/Core Playback JVM与desktop compile；真实媒体播放/seek、歌词换曲、timer、重复队列保存/取消、确认→queue→player的Back及短高大字烟测；记录证据，独立review后提交。

## 5. Saved playlists, rules and single document owner

- [ ] 5.1 按计划Task 5完成静态/智能类型与创建、渐进参数编辑、detail/picker及独立JSON/M3U/M3U8/PLS入口；Shared复用同controller/launcher和单preview host，保留Settings入口，验证取消/失败后跨页重开、stale preview拒绝及原子应用。
- [ ] 5.2 跑Task 5 Playlists/Shared JVM与desktop compile；实际长规则、创建/rename/delete取消及各文档格式/preview/取消重开，记录无法控制的系统picker缺口、EN/ZH与文档证据；独立review后提交。

## 6. Settings, source reports and information surfaces

- [ ] 6.1 按计划Task 6重排五组、来源/报告/维护、About/OSL与Onboarding；保持偏好各自gate、unknown output、Android恢复及iOS专属文案，report状态不随回收丢失；现有行为套件验证危险取消/失败和引导/资源加载语义。
- [ ] 6.2 跑Task 6 Settings/Onboarding/Shared JVM与desktop compile；实际偏好、来源scan/取消/报告回收、危险取消、About/许可证/引导及大字返回/确认烟测，更新文档证据，独立review后提交。

## 7. Input, focus, motion and remaining token consumers

- [ ] 7.1 按计划Task 7完成焦点/键盘、各inactive层pointer/gesture/semantics与motion零比例适配，去除剩余旧呈现分叉而不删在用公共能力；通过现有消费边界的隐藏/被覆盖/离场动作、focus恢复与Back一次结算回归。
- [ ] 7.2 运行受影响既有JVM套件，实际Tab/Enter/Space/Esc、读屏、减少动态效果或可丢弃harness零比例、大字/浅深全surface巡检；无新占位快捷键/开关，记录文档、独立review后提交。

## 8. Integrated acceptance and evidence closeout

- [ ] 8.1 分别运行spotlessApply、独立spotlessCheck、独立detekt和architectureCheck（均带--configuration-cache），再跑全部受影响JVM、./init.sh及shared iOS simulator framework链接；记录确切成功/阻塞输出，不把部分构建记作全平台通过。
- [ ] 8.2 执行计划Task 8的真实desktop/可用Android/iOS运行与尺寸×语言×主题×字体全矩阵，逐项核对所有spec scenario和调用/文案/状态完整性；设备/听感/系统控制由用户另验，缺少运行目标不得代签通过。
- [ ] 8.3 获得fresh全范围review、修复证据支持的问题，运行openspec validate studio-ui-redesign --strict；更新progress/roadmap的证据与精确缺口，仅勾实际完成项，满足仓库完成规则后提交整体checkpoint。

## Workflow follow-up

- 用户已批准实施计划；按任务顺序执行并保留独立审查与验证证据。
- 全部实施验收与独立review齐备后按OpenSpec archive流程同步六个新增spec及两个完整修改块，不把归档本身列作提前勾选的实施任务。
- 原Phase 1实体设备/系统媒体验收与本呈现重设计分别保留；不得因mockup或编译通过改变其状态。
