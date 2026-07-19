# Task 4 Report: Playlist Dialog Migration and Presentation Policies

## Status

DONE_WITH_MANUAL_QA_LIMITATIONS

Task 4 migrated every playlist modal shell to `HausDialog`, removed `ModalCard`, and applied the approved playlist frame, tab, and compact-action policies. Focused playlist tests pass. No runtime visual state is claimed by this task.

## Scope

Route: openspec+superpowers / playlist-dialog-polish Task 4

Preserved behavior:

- Existing string resources, `PlaylistNameDraft`, picker and browser selection state, mutation outcomes/notices, and callbacks.
- Saved-playlist rename, delete, and remove confirmation behavior.
- Clear Upcoming confirmation and queue mutation behavior.
- Playlist route/back callbacks and both `NowPlayingBarContentPadding` bottom spacers.
- Existing pulse-colored destructive/remove glyphs and failure notices.

Explicitly unchanged:

- Settings and Library dialog callers.
- Routes, persistence, playback, dependencies, palette definitions, source management, and localized strings.

## Changed Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistPresentationPolicy.kt`
  - Added the 18dp playlist item rhythm to the existing pure policy.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`
  - Replaced create/rename, Add to Playlist picker, playlist track browser, saved destructive confirmations, and Clear Upcoming confirmation shells with `HausDialog` body/actions slots.
  - Removed the obsolete `ModalCard` and playlist `safeContentPadding()` usage.
  - Applied system-top plus 20dp horizontal frame padding and 18dp item spacing.
  - Applied explicit selected/unselected palette colors and 40dp height, 6dp vertical inside margin, and 20sp line height to tabs and compact actions.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreensTest.kt`
  - Added the 18dp frame-rhythm assertion and dark-palette contrast coverage while preserving all existing tests.
- `.superpowers/sdd/task-4-report.md`
  - Replaced stale unrelated scratch evidence at the Task 4-owned path with this report.
- `.superpowers/sdd/progress.md`
  - Recorded Task 4 completion, focused verification, clean re-review, and the Task 5 visual-QA handoff.

## TDD Evidence

### Pre-change Task 1/2 baseline

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Result: PASS.

```text
BUILD SUCCESSFUL in 868ms
26 actionable tasks: 5 executed, 21 up-to-date
Configuration cache entry reused.
```

### RED

After adding the required 18dp policy assertion and before production implementation:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --configuration-cache
```

Result: expected test-compilation failure.

```text
PlaylistScreensTest.kt:28:56 Unresolved reference 'itemSpacing'.
> Task :shared:compileTestKotlinJvm FAILED
BUILD FAILED in 2s
```

### Final focused GREEN

Required Task 4 command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --configuration-cache
```

Result: PASS after implementation and the dark-palette review fix.

```text
BUILD SUCCESSFUL in 2s
26 actionable tasks: 6 executed, 20 up-to-date
Configuration cache entry reused.
```

An earlier post-implementation run of the same command also passed with `BUILD SUCCESSFUL in 6s`.

The final completion-gate rerun also passed:

```text
BUILD SUCCESSFUL in 429ms
26 actionable tasks: 4 executed, 22 up-to-date
Configuration cache entry reused.
```

## Static Verification and Review

- `GIT_MASTER=1 git diff --check`: PASS with no output.
- Targeted source search for `ModalCard|safeContentPadding` in `PlaylistScreens.kt`: no matches.
- Kotlin `lsp_diagnostics` was attempted on all three changed Kotlin files, but `kotlin-ls` is not installed and installation was previously declined. No LSP-clean claim is made; focused Gradle compilation/tests are the executable Kotlin check.
- Read-only Task 4 review found no behavioral or scope defect. It identified missing dark-palette contrast coverage; that test was added, and the focused suite passed again.
- Scoped diff review confirmed no callback, mutation decision, resource, route, queue confirmation, or Now Playing clearance removal.

## Manual QA Limitations

Task 4 did not launch the desktop app or capture screenshots. The approved plan assigns compact/wide, light/dark, localized text-fit, keyboard/focus, panel-opacity, scrim, and Now Playing overlap inspection to Task 5. Therefore none of the following is claimed as passed here:

- Compact and wide playlist layouts.
- Light and dark Saved/Queue contrast in rendered pixels.
- Create, rename, picker/inline-create, browser, destructive, and Clear Upcoming dialogs in a running app.
- Long localized labels, CJK glyph fit, descenders, keyboard focus/submit, or target-device touch behavior.
- Pixel-level dialog opacity, scrim brightness, or Now Playing clearance.

Next owner: Task 5 verification and visual QA.

Blockers: none for Task 4 focused tests or source migration. Runtime visual acceptance remains pending by plan.

Commit: skipped because the user explicitly prohibited commits.
