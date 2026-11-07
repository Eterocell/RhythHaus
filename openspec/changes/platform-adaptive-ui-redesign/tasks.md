# Tasks

## 1. Satisfy dependencies and capture failing baselines

- [x] 1.1 Complete `theme-selection` first and verify its palette/store/Settings tests, `openspec validate theme-selection --strict`, and required platform builds pass before beginning adaptive shell implementation. Reconciled against pre-production Task 1 ledger dated 2026-11-04: theme-prerequisite-tests, theme-openspec-validate, adaptive-baseline-init all exited 0; Core UI JVM 44, Settings JVM 53, Shared JVM 544, iOS simulator 340, no failures/errors/skips. Integrated theme baseline predates b9be0c3f. The user later confirmed the appearance/device review items complete; see the 2026-11-07 acceptance handoff.
- [x] 1.2 The user confirms the device review of settled/transition/canceled route geometry, IME, and resize is complete. This records user-reported acceptance; no per-scenario observations were supplied in this session.
- [x] 1.3 The user confirms the device review of Controls dismissal and competing taps, seek, Queue, nested panels, overlays, Back, and cancellation is complete. This records user-reported acceptance; no per-scenario observations were supplied in this session.

## 2. Establish the Shared adaptive-shell geometry

- [x] 2.1 Implement one current safe-area geometry policy and Shared-owned route header in Shared; keep header geometry stable within each layout/text class and route/overlay geometry stable through transitions/cancellation. Pass Insets/header regressions and run the desktop Compose route-transition smoke.
- [x] 2.2 Reconcile measured navigation, mini-player/selection bar, IME, and content clearance so each occupied edge is applied exactly once and stale measurements remain inert; pass final-row, resize, and active-chrome regressions.
- [x] 2.3 Implement foreground-layer input/accessibility ownership, active-editor keyboard avoidance, Android system-bar appearance from the visible under-bar surface, and platform reduced-motion projection without a new preference; pass Shared UI tests and route/overlay/IME smoke.

## 3. Restore safe expanded-player dismissal

- [x] 3.1 Implement Controls-only downward drag using Shared's existing close authority and visible close alternative; pass the failing-before regressions for artwork/empty-space drag, threshold/fling, tap, horizontal seek, short-drag return, and pointer cancellation without changing playback/queue/route state.
- [x] 3.2 Keep Queue handle dismissal separate from Queue body scroll/reorder and preserve Lyrics/Timer scroll, protected-overlay priority, Back arbitration, resize, and focus restoration; pass production Now Playing and Shared player/Back UI tests.

## 4. Migrate browsing and playlist presentation by feature owner

- [x] 4.1 Adapt Library's six views, album/artist detail, Search, multi-selection, and metadata editing to the approved compact/wide layouts using existing feature entry points; pass owning Library/Search UI suites for route, selection, visible ordering, draft/error, and final-row behavior in English and Chinese. Evidence: Task 5 result log and XML report counts in `.superpowers/sdd/2026-10-30-platform-adaptive-ui-redesign/`.
- [x] 4.2 Adapt static/smart playlist hub/detail/rule editing and JSON/M3U/M3U8/PLS document flows without changing their existing authorities; pass Playlists JVM/common UI and Shared document-host regressions for long rules, scrolling, preview, cancel/failure, stale revision, and saved-draft retention. Evidence: Playlists JVM 181/181, Playlists iOS simulator 101/101, Shared JVM 560/560; no failures/errors/skips. Covered static/smart hub and detail routes with one Shared header and no feature Back/title toolbar; long smart-rule candidates with Save/Cancel reachable at 200% text; queue-save draft across track change and resize; document cancellation/read failure/stale confirmation while preserving the live playlist snapshot. ArchitectureCheck, strict OpenSpec validation, and diff hygiene pass.
- [x] 4.3 Update `docs/architecture.md` and the feature-first ADR if a public feature entry point or resource owner changes; otherwise record that ownership is unchanged and confirm `architectureCheck` passes. Evidence: Shared route-header ownership, Search label/API boundary, and `LibrarySharedLabels` membership synchronized in architecture, ADR, and canonical feature spec; `architectureCheck` and strict change validation pass.

## 5. Migrate playback and global/tool surfaces

- [x] 5.1 Adapt the mini-player, expanded Controls, Queue, Lyrics, Timer, loading/paused/ended/failure states with the existing playback/queue owners; pass Now Playing and Shared integration suites and a real desktop playback/player gesture smoke. Owner suites: Now Playing JVM 60, iOS simulator 13, Shared LibraryAppShell 50 pass. Real desktop smoke now verified at 800×600 and 390×768 using asserted synthetic HOME: generated silent sample progresses, Pause settles, artwork drag and named Close return to Library with same paused dock/track, Queue-body drag remains local, Timer round-trip preserves off state, compact scroll reaches full transport row. The user confirms the listed device playback/player behavior was reviewed; no per-scenario observations were supplied in this session.
- [x] 5.2 Adapt Settings, source/scan/report/maintenance, onboarding, About/licenses, and desktop-drop feedback using their existing state and error owners; pass Settings, Onboarding, source/import, and Shared production-route UI suites, including protected confirmation and failure states. Automated Settings JVM 54/54, Onboarding JVM 17/17, Shared JVM 563/563; architectureCheck and strict OpenSpec pass. The user confirms the listed device visual/accessibility review was completed. Physical system-picker interactions were not in the review checklist and remain unverified.
- [x] 5.3 Update any behavior/architecture documentation whose public entry point or resource ownership changed in this migration; validate referenced paths and run the focused affected feature suites. Settings canonical API/ownership docs now describe feature-localized About/license title payloads; focused affected suites pass.

Task 5.2 follow-up: the Settings feature renders the existing app icon in the About row and alongside the About logo without changing route actions or launcher assets. See the About artwork checkpoint in `progress.md` for verification. This does not close additional acceptance gates.

## 6. Verify complete adaptive coverage and hand off

- [x] 6.1 The user confirms manual review of the listed adaptive layout/state, localization, text-scale, reduced-motion, transition/input, accessibility, appearance, and playback items. This is user-reported acceptance; no per-size, per-state, platform, or screenshot observations were supplied in this session.

- [x] 6.2 Run focused affected tests with XML count/failure inspection, `./init.sh`, formatting/static/architecture checks, `openspec validate platform-adaptive-ui-redesign --strict`, and an independent review; record exact output and unperformed user-only Android/iOS/macOS visual, accessibility, system-Inset, and audio acceptance as unverified in `progress.md`. Final corrected Shared JVM 567/iOS simulator 339; affected suites, standalone qualityCheck, init.sh, strict OpenSpec, diff hygiene, and local Markdown links pass. Independent review findings were corrected; bounded re-review reports no residual findings in that scope. User-reported manual review is recorded in the 2026-11-07 handoff; unreviewed system-picker flows remain separate.
- [x] 6.3 Complete all OpenSpec tasks and handoff evidence, then commit the approved implementation with a conventional message after review; do not mark manual acceptance complete or archive until the stated acceptance gates are satisfied. Implementation committed as `3d9dfe09` (`feat(ui): complete platform-adaptive redesign`) and fast-forwarded to local `main`; no push or archive was requested.

Task 6.1 follow-up: rendered Search retained its query and selection at both exact layout thresholds; production Queue final-row reachability and occurrence reorder passed all seven actual Compose host sizes in EN/zh-CN at 100%/200% text. See the Task 9 continuation in `progress.md`. The user separately confirms the listed manual device review is complete; this host evidence does not add per-device observations to that confirmation.

## Workflow follow-up

- Once both the theme-selection palette plan and this adaptive redesign plan are approved together, execute `theme-selection` first, integrate its completed baseline, and only then begin this change in `.worktrees/platform-adaptive-ui-redesign/`.
- Preserve the pre-existing `WATCHDOG.yml`, local edits, and index state when creating or switching implementation worktrees.
- Archive the OpenSpec change only after its implementation, verification, review, and acceptance evidence are complete. User reports the listed device and accessibility review complete; preserve that provenance and do not invent per-scenario evidence.
