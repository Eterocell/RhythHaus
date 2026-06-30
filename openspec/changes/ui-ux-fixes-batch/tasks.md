# Tasks

## 1. Empty Home onboarding and adaptive album grid

- [ ] Add common tests for album-grid column breakpoints.
- [ ] Add the pure column-count helper in shared common code.
- [ ] Show `ImportAudioCard` on Home when the library is empty.
- [ ] Render album rows using the adaptive column count instead of hardcoded two-column chunks.
- [ ] Run focused common tests and JVM compile.

## 2. Songs browse mode

- [ ] Extend `BrowseMode` with `Songs`.
- [ ] Render all tracks as `TrackRow` entries in Songs mode.
- [ ] Wire song-row clicks to select the track and use the full-library playable queue to start/toggle playback.
- [ ] Preserve Albums and Artists behavior.
- [ ] Run focused common tests and JVM compile.

## 3. Search and compact controls polish

- [ ] Add Search query clear action.
- [ ] Dismiss Search after a result starts playback.
- [ ] Increase `BackChip` effective hit target to at least 44dp height.
- [ ] Increase bottom-bar Search and Settings effective hit targets to at least 44dp.
- [ ] Run JVM compile.

## 4. Remove user-facing developer panels

- [ ] Remove normal UI rendering of TagLib developer panels.
- [ ] Remove now-unused developer-only composables/imports when they become dead code.
- [ ] Verify source no longer contains user-facing `DEV · TagLib` text.
- [ ] Run broad JVM/desktop/Android verification.

## 5. Handoff

- [ ] Run `openspec validate ui-ux-fixes-batch --strict`.
- [ ] Run final relevant verification or record exact blockers.
- [ ] Update this task list with evidence.
- [ ] Update `progress.md` with route, scope, verification, changed files, next owner, blockers, and commits.
- [ ] Commit with a semantic message after successful OpenSpec + Superpowers execution.
