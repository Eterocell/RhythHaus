## 1. Playback restart command

- [x] 1.1 Add failing common tests for restarting playing, paused, stopped, loading, error, and absent current-track states.
- [x] 1.2 Implement `PlaybackController.restartCurrentTrack()` with ordered seek-before-play behavior and loading-safe autoplay.
- [x] 1.3 Run the complete `PlaybackControllerTest` suite and record RED/GREEN evidence.

## 2. Shared track-selection policy

- [x] 2.1 Add failing common tests for current-track queue preservation, visible-list queue replacement, invalid IDs, and repeat/shuffle preservation.
- [x] 2.2 Add the shared Library playback-selection helper and make non-current selection auto-play.
- [x] 2.3 Run the focused selection-policy test suite and record RED/GREEN evidence.

## 3. Library and Search wiring

- [ ] 3.1 Route Library home track rows through the shared selection policy.
- [ ] 3.2 Separate drill-down track selection from dedicated play/pause transport and route album/artist rows through the shared policy.
- [ ] 3.3 Route Search results through the shared policy using the current filtered results as the visible queue.
- [ ] 3.4 Run focused controller, selection-policy, Library navigation, and shared JVM compilation checks.

## 4. Verification and durable evidence

- [ ] 4.1 Run strict OpenSpec validation, shared JVM tests, desktop compile, Android debug assembly, Xcode availability, iOS simulator tests, and `git diff --check`.
- [ ] 4.2 Complete task-level and whole-change reviews and resolve all Critical or Important findings.
- [ ] 4.3 Update `roadmap.md`, `progress.md`, and this checklist with exact verification evidence and manual playback-QA limitations.
