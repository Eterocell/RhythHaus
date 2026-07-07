# Proposal: Package Organization

## Summary

Move the newly split shared Compose/UI components into focused Kotlin packages so package boundaries match feature and helper ownership. This is a behavior-preserving architecture refactor that updates source paths, package declarations, imports, and tests without redesigning screens or changing product behavior.

## Problem

The previous architecture refactor split `App.kt` into focused files, but most shared UI/orchestration files still live directly in `com.eterocell.rhythhaus`. The root package now mixes app entry, library UI shell/routes/components, Now Playing UI, search/settings overlays, playback abstractions, theme helpers, reusable Compose helpers, domain models, and platform-adjacent expect/actual seams.

This makes the project harder to navigate and weakens the architectural boundaries established by the file split.

## Goals

- Keep `App()` stable as the root shared entry point.
- Move library UI/orchestration into a library UI package.
- Move Now Playing, Search, and Settings UI into feature packages.
- Move shared UI/theme helpers into reusable packages where safe.
- Move playback/domain/platform-adjacent packages only when expect/actual and platform entry points can be updated safely.
- Preserve behavior and verification coverage.

## Non-goals

- No visual redesign.
- No route, playback, scanner/import, theme, database, TagLib, or artwork behavior changes.
- No dependency/toolchain/resource changes.
- No native navigation migration.
- No Windows/Linux scope.
