# Task 3 report: accessible sorting and filtering controls

## Scope

Recovered and verified the partial Task 3 implementation after the original implementation channel failed during its response. The changes add feature-owned localized controls and passive filtered-empty behavior without persistence, repository, scanner, playback, or platform changes.

## Implementation

- Added localized English and Simplified Chinese labels for the controls, sort keys, directions, filter states, and filtered-empty results.
- Added horizontally scrollable, compact-safe `LibraryBrowseControls` below the browse-mode picker.
- Exposed all seven sort keys and both directions with selected semantics.
- Added favorite-only and artwork-only toggle controls with explicit checked/state semantics.
- Added source selection and clear-all-sources behavior; source controls are omitted when no source IDs exist.
- Added filtered-empty messaging that remains passive and does not render import or clear-library affordances.
- Added resource ownership and Compose UI tests covering control callbacks, state semantics, source omission/clearing, localized resources, and filtered-empty behavior.

## Verification evidence

```text
./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowseControlsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' --tests 'com.eterocell.rhythhaus.library.LibraryResourceOwnershipJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL`; 57 actionable tasks, 13 executed, 44 up-to-date. Focused controls, Home, and resource ownership tests passed with no failures. `git diff --check` produced no output.

No project-wide formatter, linter, or full suite was run at this task boundary.
