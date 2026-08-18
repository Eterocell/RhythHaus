## 1. Home manager-section removal

- [ ] 1.1 Add failing home tests: import card hidden with tracks, scanning card hidden with tracks, import card visible when empty, scanning card visible only when empty and active.
- [ ] 1.2 Remove `LibraryManagerCard`, `SourceManagerRow`, and their lazy item from `LibraryHomeContent`; remove now-unused parameters from `LibraryHomeContent` and both `LibraryAppShell` call sites; remove dead source-management strings from library-impl resources.
- [ ] 1.3 Remove obsolete home manager/scan-outcome tests and helpers from `LibraryHomeContentJvmTest`.
- [ ] 1.4 Run focused library home JVM tests and record RED/GREEN evidence.

## 2. Scan-outcome panel moved to Settings

- [ ] 2.1 Make `ScanOutcomePanel` a public stateless composable in `:feature:library:impl` with KDoc and `@param` entries.
- [ ] 2.2 Add a failing Settings test for the `scanOutcomeContent` slot rendering after `activeScanContent`.
- [ ] 2.3 Add the `scanOutcomeContent` slot parameter to `SettingsScreen` and render it after the active-scan slot.
- [ ] 2.4 Add failing route-adapter tests: outcome panel renders in Settings with summary/report/remove-missing callbacks, report expansion survives item recreation, report collapses on session change, disabled coordinator disables mutation controls.
- [ ] 2.5 Wire the Shared route layer: report-expansion state keyed by session ID, source resolution, `onRemoveMissingTracks` parameter, and slot supply through `LibraryRouteOverlays`.
- [ ] 2.6 Run focused Settings and route-adapter tests and record RED/GREEN evidence.

## 3. Verification and durable evidence

- [ ] 3.1 Run strict OpenSpec validation, shared JVM tests, desktop compile, Android debug assembly, Xcode availability, iOS simulator tests, architectureCheck, Spotless, Detekt, and `git diff --check`.
- [ ] 3.2 Review the scoped diff for home, Settings, accessibility, scan-state, navigation, and callback regressions.
- [ ] 3.3 Update `roadmap.md`, `progress.md`, and this task checklist with verification evidence and remaining manual visual-QA limits; commit with a semantic message.

## Verification Evidence

- Placeholder: exact command outputs recorded after execution.
