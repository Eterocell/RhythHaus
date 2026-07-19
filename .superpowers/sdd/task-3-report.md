# Task 3 Report: Migrate Settings dialogs to HausDialog

## Status

DONE

Clear Library and Remove Folder now use the shared solid `HausDialog` shell with separate body and actions slots. Existing resources, typography, action labels, action colors, action order, mutation behavior, callback order, routes, and source-name accessibility semantics are preserved.

## Changed files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt`
  - Replaced Clear Library's custom scrim, transparent card, `rhythHausLiquidGlass`, and `LayerBackdrop` parameter with `HausDialog`.
  - Kept the existing title, message, Cancel/Clear order, button dimensions, colors, labels, and callbacks.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
  - Replaced Remove Folder's custom liquid-glass shell with `HausDialog` body/actions slots.
  - Preserved the two-line ellipsized `name.visual` label and `clearAndSetSemantics { contentDescription = name.accessibility }`.
  - Preserved the `mutationsEnabled` gate on Remove and both callback-before-visible-state-close sequences.
  - Removed `clearLibraryDialogBackdrop` only after both migrated dialogs stopped consuming it.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
  - Removed the dialog-only backdrop parameter and Settings forwarding.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
  - Removed the dialog-only backdrop argument from the overlay route helper and its two callers.
  - Left `rootBackdrop`, `rememberRhythHausBackdrop()`, and both `recordRhythHausBackdrop(rootBackdrop)` uses unchanged.
- `.superpowers/sdd/task-3-report.md`
  - Replaced the stale unrelated report with this Task 3 evidence.

`shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibrarySourceManagementTest.kt` was not changed because it already contains the required regression proving a long source name has a 63-character-plus-ellipsis visual form and the complete accessibility form. Its existing mutation-gate coverage was retained unchanged.

## Verification

Pre-migration characterization command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Exact result summary:

```text
BUILD SUCCESSFUL in 1s
35 actionable tasks: 5 executed, 30 up-to-date
Configuration cache entry stored.
```

Post-migration required command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Exact result summary:

```text
BUILD SUCCESSFUL in 4s
26 actionable tasks: 8 executed, 18 up-to-date
Configuration cache entry reused.
```

Final completion-gate rerun of the same required command:

```text
BUILD SUCCESSFUL in 383ms
26 actionable tasks: 4 executed, 22 up-to-date
Configuration cache entry reused.
```

Independent QA forced fresh compilation and test execution:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache --rerun-tasks
BUILD SUCCESSFUL in 9s
26 actionable tasks: 26 executed
```

Additional checks:

- `GIT_MASTER=1 git diff --check`: pass with no output.
- Scoped search across `LibraryDialogs.kt`, `SettingsScreen.kt`, `LibraryRoutes.kt`, and `LibraryAppShell.kt` found no remaining `clearLibraryDialogBackdrop`, `LayerBackdrop`, or `rhythHausLiquidGlass` references.
- Kotlin LSP diagnostics were unavailable because `kotlin-ls` is not installed and installation was previously declined. The focused Gradle run compiled the changed common source and test code successfully.
- Scoped diff review confirmed Clear Library still invokes `onClearLibrary()` before `showClearLibraryDialog = false`, and Remove Folder still invokes `onRemoveSource(source)` before `sourcePendingRemoval = null`.
- No playlist screen, dependency, palette, route, string, persistence, playback, scan behavior, test, or type suppression was changed.
- No commit was created.

## Review

Five independent post-implementation lanes reviewed goal compliance, focused QA, code quality, security/privacy, and repository context.

- Goal and constraints: PASS, 98% confidence.
- Focused QA: PASS, high confidence; forced fresh compilation and test execution passed.
- Code quality: PASS, 96% confidence; no findings.
- Security/privacy: PASS; maximum severity none.
- Context: all implementation requirements aligned. One initial concern about the old localized semantics dismiss label was adjudicated as not blocking because the approved design centralizes semantics in the fixed review-clean `HausDialog` API, rejects per-feature semantics wrappers, and requires a dismiss action but not a caller-provided label. The visible localized Cancel action and full source-name accessibility remain preserved.

Final review: PASS with no blocking issues.

## Workspace note

The workspace already contained unrelated Task 1/2 and playlist-dialog-polish changes. They were preserved and not modified by Task 3. `HausDialog.kt` remains an untracked Task 2 dependency in the shared workspace.

Route: openspec+superpowers / Task 3 settings-dialog migration
Owner: implementation
Input: `.superpowers/sdd/task-3-brief.md` and Task 2 `HausDialog`
Output: settings dialogs migrated to the shared solid shell; focused source-management tests passed; this report
Next owner: controller/reviewer for Task 3 acceptance
Blockers: none

## Final review regression fix: localized dismiss action label

The whole-change review established that Remove Folder's pre-migration semantics used `dismiss(label = dismissLabel)` with `dismissLabel = stringResource(Res.string.cancel)`. The initial Task 3 migration preserved the dismiss callback but lost that localized action label because `HausDialog` exposed only an unlabeled dismiss action.

This follow-up restores the former semantics without reintroducing a per-feature shell:

- `HausDialog` now accepts optional `dismissLabel: String? = null` and forwards it to `dismiss(label = dismissLabel)`. Existing callers preserve their prior behavior through the null default.
- `RemoveSourceDialog` resolves the existing localized Cancel resource and is the only production caller that supplies `dismissLabel`.
- `HausDialogSemanticsJvmTest` uses the real `runComposeUiTest` semantics tree to assert the actual `SemanticsActions.Dismiss` label is `Cancel`, invoke that action, verify exactly one callback, and verify the dismiss node disappears.

This evidence supersedes the earlier Task 3 report statement that the missing localized dismiss label was not blocking.

### TDD evidence

Clean RED command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache
```

Exact RED result:

```text
e: file:///Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus/shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/HausDialogSemanticsJvmTest.kt:27:21 No parameter with name 'dismissLabel' found.
BUILD FAILED in 560ms
25 actionable tasks: 5 executed, 20 up-to-date
Configuration cache entry reused.
```

Focused GREEN result for the same command after the minimal production change:

```text
BUILD SUCCESSFUL in 7s
26 actionable tasks: 8 executed, 18 up-to-date
Configuration cache entry reused.
```

Combined focused command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
```

Exact combined result:

```text
BUILD SUCCESSFUL in 2s
35 actionable tasks: 5 executed, 30 up-to-date
Configuration cache entry stored.
```

Forced fresh final command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache --rerun-tasks
```

Exact forced result:

```text
BUILD SUCCESSFUL in 8s
26 actionable tasks: 26 executed
Configuration cache entry reused.
```

Final completion-gate rerun after appending this report:

```text
BUILD SUCCESSFUL in 375ms
26 actionable tasks: 4 executed, 22 up-to-date
Configuration cache entry reused.
```

Kotlin LSP diagnostics remained unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compiled and executed the changed common/JVM sources successfully. No callback, palette, layout, dependency, string, route, playlist caller, persistence, playback, or scan behavior changed, and no commit was created.
