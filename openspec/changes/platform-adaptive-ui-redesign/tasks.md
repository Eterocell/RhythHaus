# Tasks

## 1. Satisfy dependencies and capture failing baselines

- [ ] 1.1 Complete `theme-selection` first and verify its palette/store/Settings tests, `openspec validate theme-selection --strict`, and required platform builds pass before beginning adaptive shell implementation.
- [ ] 1.2 On an actual Compose surface, reproduce and record stable-window Insets/safe-boundary geometry for settled routes, entering/exiting frames, cancellation, IME, and window resize; identify the production owner and keep a failing consumer-visible regression scenario before changing shell code.
- [ ] 1.3 On an actual Compose Now Playing surface, reproduce and record downward-drag dismissal failure and the competing tap, horizontal seek, Queue reorder/scroll, nested panel, overlay, Back, and cancel paths; keep failing consumer-visible regression scenarios before changing gesture code.

## 2. Establish the Shared adaptive-shell geometry

- [ ] 2.1 Implement one current safe-area geometry policy and Shared-owned route header in Shared; keep header geometry stable within each layout/text class and route/overlay geometry stable through transitions/cancellation. Pass Insets/header regressions and run the desktop Compose route-transition smoke.
- [ ] 2.2 Reconcile measured navigation, mini-player/selection bar, IME, and content clearance so each occupied edge is applied exactly once and stale measurements remain inert; pass final-row, resize, and active-chrome regressions.
- [ ] 2.3 Implement foreground-layer input/accessibility ownership, active-editor keyboard avoidance, Android system-bar appearance from the visible under-bar surface, and platform reduced-motion projection without a new preference; pass Shared UI tests and route/overlay/IME smoke.

## 3. Restore safe expanded-player dismissal

- [ ] 3.1 Implement Controls-only downward drag using Shared's existing close authority and visible close alternative; pass the failing-before regressions for artwork/empty-space drag, threshold/fling, tap, horizontal seek, short-drag return, and pointer cancellation without changing playback/queue/route state.
- [ ] 3.2 Keep Queue handle dismissal separate from Queue body scroll/reorder and preserve Lyrics/Timer scroll, protected-overlay priority, Back arbitration, resize, and focus restoration; pass production Now Playing and Shared player/Back UI tests.

## 4. Migrate browsing and playlist presentation by feature owner

- [ ] 4.1 Adapt Library's six views, album/artist detail, Search, multi-selection, and metadata editing to the approved compact/wide layouts using existing feature entry points; pass owning Library/Search UI suites for route, selection, visible ordering, draft/error, and final-row behavior in English and Chinese.
- [ ] 4.2 Adapt static/smart playlist hub/detail/rule editing and JSON/M3U/M3U8/PLS document flows without changing their existing authorities; pass Playlists JVM/common UI and Shared document-host regressions for long rules, scrolling, preview, cancel/failure, and saved-draft retention.
- [ ] 4.3 Update `docs/architecture.md` and the feature-first ADR if a public feature entry point or resource owner changes; otherwise record that ownership is unchanged and confirm `architectureCheck` passes.

## 5. Migrate playback and global/tool surfaces

- [ ] 5.1 Adapt the mini-player, expanded Controls, Queue, Lyrics, Timer, loading/paused/ended/failure states with the existing playback/queue owners; pass Now Playing and Shared integration suites and a real desktop playback/player gesture smoke.
- [ ] 5.2 Adapt Settings, source/scan/report/maintenance, onboarding, About/licenses, and desktop-drop feedback using their existing state and error owners; pass Settings, Onboarding, source/import, and Shared production-route UI suites, including protected confirmation and failure states.
- [ ] 5.3 Update any behavior/architecture documentation whose public entry point or resource ownership changed in this migration; validate referenced paths and run the focused affected feature suites.

## 6. Verify complete adaptive coverage and hand off

- [ ] 6.1 Run the approved layout matrix at 360×640, 390×844, 844×390, 800×600, 800×1280, 1024×1366, and 1280×800dp; cover English/Simplified Chinese, default/200% text, reduced motion, route/overlay/input transitions, and all 51 named design states with no unintended horizontal overflow or unreachable final actions.
- [ ] 6.2 Run focused affected tests with XML count/failure inspection, `./init.sh`, formatting/static/architecture checks, `openspec validate platform-adaptive-ui-redesign --strict`, and an independent review; record exact output and unperformed user-only Android/iOS/macOS visual, accessibility, system-Inset, and audio acceptance as unverified in `progress.md`.
- [ ] 6.3 Complete all OpenSpec tasks and handoff evidence, then commit the approved implementation with a conventional message after review; do not mark manual acceptance complete or archive until the stated acceptance gates are satisfied.

## Workflow follow-up

- Once both the theme-selection palette plan and this adaptive redesign plan are approved together, execute `theme-selection` first, integrate its completed baseline, and only then begin this change in `.worktrees/platform-adaptive-ui-redesign/`.
- Preserve the pre-existing `WATCHDOG.yml`, local edits, and index state when creating or switching implementation worktrees.
- Archive the OpenSpec change only after its implementation, verification, review, and acceptance evidence are complete; keep user-performed device and accessibility checks explicitly pending until the user performs them.
