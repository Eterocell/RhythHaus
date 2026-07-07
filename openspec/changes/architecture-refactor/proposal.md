# Proposal: Architecture Refactor

## Summary

Refactor the shared Compose app architecture so `App.kt` becomes a thin composition/dependency entry point, library orchestration moves behind a named state/coordinator boundary, and library UI is split into focused files. The change is behavior-preserving and incremental.

## Problem

`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` has grown into a large mixed-responsibility file. It currently owns dependency construction, navigation state, scanner callbacks, playback actions, adaptive layout routing, Now Playing overlay state, route rendering, top chrome, dialogs, and many presentational rows/cards.

This makes future work risky because small behavior changes require editing a file that also owns unrelated state and UI. The code already has good examples of pure tested helpers in `LibraryNavigation.kt`; the broader app architecture should follow that pattern.

## Goals

- Keep app behavior and visuals unchanged.
- Make `App.kt` a small entry point that wires dependencies and hands off to a library app shell/state boundary.
- Introduce a shared state/coordinator layer for route, selection, scan/import, bottom-bar, and Now Playing overlay orchestration.
- Split library UI into focused common-source files.
- Add or preserve tests around extracted pure architecture decisions.
- Keep the work shared-first and incremental.

## Non-goals

- No visual redesign.
- No scanner, repository, database, playback engine, TagLib, source-access, or artwork-cache rewrite.
- No platform-native navigation migration.
- No dependency/toolchain changes.
- No Windows/Linux scope.
