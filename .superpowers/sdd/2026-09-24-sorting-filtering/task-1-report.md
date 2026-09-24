# Task 1 report: Library browse query projection

## Scope

Implemented the pure Library sorting/filtering projection for the sorting-filtering OpenSpec change. The implementation is feature-owned by `:feature:library:impl`, keeps query state ephemeral, and leaves grouped album/artist presentation untouched.

## Implementation

- Added `LibrarySort` with `Title`, `Artist`, `Album`, `Added`, `Modified`, `PlayCount`, and `Favorite` keys.
- Added `LibrarySortDirection` with `Ascending` and `Descending`.
- Added immutable `LibraryBrowseQuery` with defaults of title/ascending and all filters disabled.
- Added `visibleTracksForBrowseQuery(...)`, which:
  - passes Albums and Artists through unchanged;
  - preserves Favorites, RecentlyPlayed, and RecentlyAdded membership;
  - applies favorite-only, artwork-only, and source-ID filters before sorting;
  - sorts every flat key in both directions;
  - uses case-insensitive title/artist/album/ID tie-breakers with a final case-sensitive ID fallback;
  - handles absent created/modified/history values without throwing, placing absent values last in ascending order and first after complete comparator reversal;
  - preserves the established recent-mode ordering for the default title/ascending query and applies explicit alternate query sorting to eligible recent-mode tracks.
- Adapted the legacy `visibleTracksForBrowseMode` recent/favorite paths to the pure projection while preserving Songs/Albums/Artists authoritative pass-through behavior and avoiding the previous missing-created-time crash.
- Added focused tests for all sort keys and directions, filters, unknown sources, tie-breakers, missing metadata, mode membership/order, and grouped pass-through.

## Verification evidence

Command:

```text
./gradlew :feature:library:impl:jvmTest --tests '*LibraryBrowseQueryTest*' --tests '*LibraryBrowserTest*' --configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 2s
57 actionable tasks: 16 executed, 41 up-to-date
```

The generated focused reports confirm:

- `LibraryBrowseQueryTest[jvm]`: 7 tests, 0 skipped, 0 failures, 0 errors.
- `LibraryBrowserTest[jvm]`: 8 tests, 0 skipped, 0 failures, 0 errors.

Additional repository hygiene check:

```text
git diff --check
```

Result: no whitespace errors.

No project-wide formatter, linter, or full test suite was run per the task boundary.

## Concerns / follow-up boundary

- Shared query state and source/metadata projection wiring remain Task 2 work.
- UI controls, selection reconciliation, and cross-platform verification remain later OpenSpec tasks.
