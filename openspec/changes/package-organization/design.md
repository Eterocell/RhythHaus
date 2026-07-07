# Design: Package Organization

## Overview

Use a staged hybrid feature-first package organization. The root package keeps the stable `App()` entry point, existing library infrastructure remains under `library`, and user-facing UI moves into feature packages. Shared Compose helpers and theme support move into reusable packages only when all expect/actual declarations and platform references can be updated safely.

## Target packages

```text
com.eterocell.rhythhaus
  App.kt
  library/
  library/ui/
  nowplaying/
  search/
  settings/
  ui/
  theme/
  playback/
  model/
```

## Root package

`App.kt` remains in `com.eterocell.rhythhaus` to avoid changing platform app entry points and Swift/framework-facing assumptions. It imports feature packages and keeps the current dependency/theme handoff behavior.

## Library UI package

`com.eterocell.rhythhaus.library.ui` owns the library feature shell, coordinator, routes, navigation decisions, home/detail content, chrome, dialog, and row/card components. It may depend on:

- `com.eterocell.rhythhaus.library` for scanner/repository/source data types;
- playback/model abstractions;
- shared UI/theme helpers;
- feature overlays such as search/settings/nowplaying.

It must not absorb scanner, repository, database, or platform source-access implementation.

## Feature UI packages

`nowplaying`, `search`, and `settings` own their feature screens. Route composition may still be initiated from `library.ui`, but the implementation of each screen belongs to the feature package.

## Shared UI and theme packages

`ui` owns reusable Compose primitives and gestures. `theme` owns Haus palette/theme preferences. Expect/actual files must move together so package declarations continue to match on common, Android, JVM, and iOS source sets.

## Playback/model packages

Playback/domain moves are lower priority and higher risk because they touch platform actuals and iOS/media bridge behavior. The implementation plan should move them after UI packages, or explicitly defer them if the churn is not worth the organization benefit for this change.

## Staging

1. Move library UI/navigation files and tests.
2. Move Now Playing/Search/Settings feature UI files.
3. Move shared UI/theme helper files with expect/actual alignment.
4. Move playback/model files only if safe; otherwise document deferral.
5. Run final verification and record evidence.

## Verification

Each stage must compile the affected source set. Final verification must include OpenSpec validation, focused package-correct tests, broad JVM/desktop/Android build, iOS simulator test, and diff hygiene.
