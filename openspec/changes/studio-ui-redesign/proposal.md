# Proposal

## Why

RhythHaus 已具备完整的本地音乐能力，但现有页面的空间分配、导航与播放职责、视觉密度和状态表达缺乏统一体系。用户已批准 [Studio 系统设计](../../../docs/superpowers/specs/2026-10-19-studio-ui-redesign-design.md)，本变更将其落实为共享 Compose 的完整呈现重设计，而非单页换肤。

## What Changes

- 建立 Studio 语义颜色、排版、间距、组件密度、危险/焦点状态和浅深主题规则，在现有 `:core:ui` 主题中完成切换。
- 分离稳定导航与播放停靠区；紧凑底部导航、中等导航轨和宽屏侧栏共用现有 Home 根单栈，不引入新的导航/Back 引擎。
- 内容优先的资料库六视图、详情、搜索和共享无状态歌曲行；保留所有查询、排序、选择、收藏、编辑和可见顺序播放行为。
- 重排迷你/完整播放器、歌词、定时与队列；队列从播放列表一级标签迁到播放器入口，保存队列事务不变。
- 统一静态/智能列表、规则编辑、设置/来源/维护、关于和引导的呈现，保留文档互操作、备份、权限和恢复能力。
- 完整覆盖无内容、扫描、筛选无结果、不可访问、加载/失败、编辑与确认；EN/ZH、字体放大、键盘/读屏和非活动层交互纳入验收。
- 删除被替代的重复视觉实现与旧入口；不提供长期 A/B 外壳或兼容 UI。

## Capabilities

### New Capabilities

- `ui-design-system`: 全应用统一的视觉语义、组件尺寸、输入/危险状态、主题和可访问呈现。
- `adaptive-app-shell`: 稳定主入口、现有单栈返回、按可用空间适配、停靠清除区与唯一交互权威。
- `music-library-presentation`: 六视图/详情/搜索的内容层级、统一歌曲行、可发现的筛选与编辑及有针对性的空态。
- `player-presentation`: 迷你/完整播放层级、空间自适应及歌词/定时/队列的按需呈现。
- `playlist-presentation`: 保存列表与临时队列分离、静态/智能类型识别、长规则编辑与独立文档工作流。
- `settings-presentation`: 外观/播放/来源/维护/信息分组及对应恢复、错误、关于与引导呈现。

### Modified Capabilities

- `saved-playlists`: “Save the visible playback queue as a playlist” 的入口由 Queue tab 改为播放器队列面板；保存范围、顺序、重复、草稿与事务全部不变。
- `playlist-scroll-chrome`: 将原混合 Bottom Bar 的滚动与清除区合约拆为播放/选择停靠区和稳定导航；继续使用共享测量，不让滚动隐藏主导航。

## Impact

- 修改 `:core:ui`、`:shared` 外壳及 Library/Search/Now Playing/Playlists/Settings/Onboarding 的呈现、所属 EN/ZH 资源及受影响的现有测试。
- 所有跨 feature 装配仍归 Shared；Core 行原语不依赖 Library/Playback 类型，feature 不依赖其他 feature 实现。UI 合约的每次修改与所有调用方在同一可编译任务中迁移。
- 不修改数据库/迁移、媒体文件、扫描/播放事务、权限适配、iOS ABI、原生引擎或 CarPlay 模板；不增加依赖、字体、联网能力或工具链升级。
- 不接纳未完成的 Miuix 导航替换提案；与既有 release/device 验收分别记录。实施计划已于2026-10-19获用户批准，产品修改按八个依赖有序任务执行。
