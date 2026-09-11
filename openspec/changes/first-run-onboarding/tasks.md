## 1. Onboarding Feature Surface

- [ ] 1.1 Add `:feature:onboarding` to the Gradle module graph with controlled Compose resources and Android/JVM/iOS targets; verify `:feature:onboarding:compileKotlinJvm`, `:feature:onboarding:compileKotlinIosSimulatorArm64`, and `architectureCheck` pass.
- [ ] 1.2 Implement the immutable four-page presentation contract and stateless localized `OnboardingScreen` with Back/Next/Skip/Finish/Close semantics; verify common policy tests and JVM Compose semantics cover each action and inactive-page absence.
- [ ] 1.3 Add English and Simplified Chinese content for local-first behavior, Android/iOS/macOS import policy, supported formats, scanning, and recovery; verify the feature resource-ownership test and locale-specific UI assertions pass.
- [ ] 1.4 Make current content and actions reachable at 600×400 dp and supported larger layouts with safe-area-aware scrolling; verify a JVM Compose regression can reach progression and exit actions at 600×400 dp.

## 2. Durable Eligibility

- [ ] 2.1 Add the schema-versioned `OnboardingPreferenceStore` contract and DataStore implementation in `:shared`; prove missing/old/current/newer markers, idempotent completion, read failure, and corruption fallback with focused JVM tests.
- [ ] 2.2 Add isolated Android, iOS, and JVM store factories for `onboarding.preferences_pb` without touching theme or playback preference files; verify platform compilation and storage-isolation coverage.
- [ ] 2.3 Register the onboarding store in Shared Koin composition and add a closed Android/iOS/macOS guidance provider; verify DI completeness, all guidance mappings, and all three platform compilations.

## 3. Shared Lifecycle and Navigation

- [ ] 3.1 Gate only the interactive presentation on `Loading`, `Required`, or `Completed` eligibility while existing library/playback startup continues; verify Shared lifecycle tests prove no interactive-library flash and no picker, permission, scan, source, library, or playback mutation from onboarding.
- [ ] 3.2 Add the canonical onboarding route and destination-scoped launch mode/page state to Shared navigation; verify first-run Back cannot bypass page zero, Back decrements later pages, and Review Back/Close returns to the exact Settings destination.
- [ ] 3.3 Persist Skip/Finish before leaving first-run onboarding and keep a retryable localized failure surface on write failure; verify process-death ordering and failed-write behavior with focused tests.
- [ ] 3.4 Render Review onboarding above Settings without changing completion or interrupting playback; verify route, queue, playback-status, and Settings-state preservation tests.

## 4. Settings Re-entry

- [ ] 4.1 Add a localized, accessible Review onboarding row and callback to `SettingsScreen`; verify the row dispatches once, is keyboard/click accessible, and remains reachable in compact Settings layouts.
- [ ] 4.2 Wire the Settings callback in Shared to push Review onboarding through existing navigation and Back arbitration; verify Settings route adapter and navigation-stack regressions.

## 5. Acceptance and Closeout

- [ ] 5.1 Run focused feature, Shared JVM, Android host, and iOS compile/test coverage, then run standalone `spotlessApply`, `spotlessCheck`, `detekt`, and `architectureCheck` with configuration cache.
- [ ] 5.2 Run `./init.sh`, `openspec validate first-run-onboarding --strict`, `openspec validate --specs`, and `git diff --check`; record exact blockers instead of claiming unavailable platform evidence.
- [ ] 5.3 Manually verify fresh install, Skip, Finish, restart suppression, Settings re-entry, English/Chinese, 600×400 desktop reachability, and playback continuity during Review on available runtimes; record unavailable Android/iOS device evidence separately.
- [ ] 5.4 Complete independent specification and code review, update `progress.md` and `roadmap.md`, synchronize the capability spec, archive the OpenSpec change, and create the conventional completion commit only after all required evidence is accepted.
