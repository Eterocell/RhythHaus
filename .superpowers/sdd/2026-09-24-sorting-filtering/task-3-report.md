# Task 3 report: accessible sorting and filtering controls

## Scope

Recovered and verified the partial Task 3 implementation after the original implementation channel failed during its response. The changes add feature-owned localized controls and passive filtered-empty behavior without persistence, repository, scanner, playback, or platform changes.

## Implementation

- Added localized English and Simplified Chinese labels for the controls, sort keys, directions, filter states, and filtered-empty results.
- Added horizontally scrollable, compact-safe `LibraryBrowseControls` below the browse-mode picker.
- Exposed all seven sort keys and both directions with selected semantics.
- Added favorite-only and artwork-only toggle controls with explicit checked/state semantics.
- Added source selection and clear-all-sources behavior from configured sources.
- Added filtered-empty messaging that remains passive and does not render import or clear-library affordances.
- Added resource ownership and Compose UI tests covering control callbacks, state semantics, source omission/clearing, localized resources, and filtered-empty behavior.

## Review repair

- Unchecked favorite and artwork controls now announce the localized
  `Not selected` state while retaining `ToggleableState.Off` and their click
  actions; the incorrect disabled-state resources were removed.
- Introduced `LibraryBrowseSourceOption`, carrying the stable callback ID and
  authoritative `LibrarySource.displayName` separately. Shared projects every
  configured source into that narrow UI contract for both compact and wide
  Home renderings, so opaque IDs are never displayed.
- Source choices now retain configured zero-track sources. `All sources`
  remains available to clear a non-null (including stale) source query even
  when no configured choice remains.
- Flat sort/filter controls are omitted for Albums and Artists, where query
  changes do not affect the grouped rendering.
- Choice and toggle controls use a growable 48dp minimum target, and their
  labels no longer force single-line ellipsis; horizontal scrolling remains
  available on compact widths.

## Verification evidence

```text
./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowseControlsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' --tests 'com.eterocell.rhythhaus.library.LibraryResourceOwnershipJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL`; 57 actionable tasks, 13 executed, 44 up-to-date. Focused controls, Home, and resource ownership tests passed with no failures. `git diff --check` produced no output.

### Review-repair verification

```text
./gradlew :feature:library:impl:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowseControlsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryHomeContentJvmTest' --tests 'com.eterocell.rhythhaus.library.LibraryResourceOwnershipJvmTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL`; 57 actionable tasks, 13 executed, 44 up-to-date.

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryAppShellJvmTest.configuredZeroTrackSourceUsesDisplayNameInHomeFilters' --configuration-cache
```

Result: `BUILD SUCCESSFUL`; 146 actionable tasks, 24 executed, 122 up-to-date. This covers the configured zero-track source display-name projection through the Shared Home boundary. `git diff --check` produced no output after the repair.

No project-wide formatter, linter, or full suite was run at this task boundary.
