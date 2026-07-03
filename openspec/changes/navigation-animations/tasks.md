# Tasks

- [x] 1. Add pure navigation transition classification beside `LibraryNavigationStack`.
  - [x] Add transition kinds for push, pop, replace, root, and no-op route changes.
  - [x] Keep `LibraryNavigationStack` as the source of truth; transition metadata must not become a second navigation stack.
  - [x] Add common tests proving push/pop/root/duplicate behavior and preserving existing route-stack expectations.

- [x] 2. Add root-level shared Compose route animation.
  - [x] Wrap the `LibraryHomeScreen` route switch in `AnimatedContent` keyed by `navigation.current`.
  - [x] Apply direction-aware push/pop/root/replace transitions with conservative 220-260ms timing.
  - [x] Preserve existing visible back, left-edge swipe, Android system/predictive back, playback, scanner, library, and theme behavior.

- [x] 3. Verify and record evidence.
  - [x] Run `openspec validate navigation-animations --strict`.
  - [x] Run focused common navigation tests.
  - [x] Run broad JVM/desktop/Android verification.
  - [x] Update `progress.md` with exact commands, outcomes, changed files, risks, and next owner.
