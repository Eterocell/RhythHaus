# Settings About Page Design

## Summary

RhythHaus will add an About entry at the bottom of Settings. The shared About page will show a common logo, the application name, a generated common version, the project source URL, and a final action that opens a shared AboutLibraries page.

## Navigation and UI

The existing sealed `LibraryRoute` stack gains `SettingsAbout` and `OpenSourceLibraries`. Settings pushes About; About pushes Open Source Libraries; all back controls and system-back events use the existing stack pop operation. Both routes must be registered as overlay/non-detail routes in compact and wide shells, with route-rendering coverage beyond stack mutation. Both pages use `RhythHausTopAppBar`, Settings' existing safe insets, and the current Haus/Miuix visual language.

The About page uses a compact, scan-friendly vertical arrangement rather than a marketing layout: the RhythHaus mark and name establish identity, the version is secondary metadata, and source/libraries are full-width 48 dp actions. The Settings entry is a conventional accessible row, not a nested card or a new preference system.

## Shared Metadata and Resources

`rhythhaus.versionName` remains the source of truth. A cache-safe Gradle task in `shared` uses a provider-backed input and declared generated source output to create a small common Kotlin build-info file; a dedicated Gradle check proves an overridden version property reaches that file. Common Compose code reads it rather than Android `BuildConfig` or a duplicate XML value. A standalone common Compose-compatible logo is created from the launcher mark; it is not a copied Android adaptive-icon resource.

English and Chinese Compose resource catalogs receive matching strings. The source action uses `LocalUriHandler` with the fixed URL `https://github.com/Eterocell/RhythHaus`.

## AboutLibraries

Use AboutLibraries `15.0.3` with the standard plugin and `aboutlibraries-compose-m3`. An explicit maintenance/CI export task writes `aboutlibraries.json` to common Compose resources and the generated JSON is checked in; normal compile/test tasks consume the checked-in file without mutating the worktree. The Open Source Libraries route reads that file with `Res.readBytes`, parses it off the UI thread using AboutLibraries' Compose helper, shows a localized loading state until ready, then renders the Material 3 library/license surface.

The Android-only AboutLibraries plugin is excluded. The common export enables the same attribution UI on Android, desktop JVM/macOS, and iOS.

## Tests and Verification

Common tests cover route-stack pushes/pops, compact/wide route rendering ownership, the generated common version contract, and About screen constants. A JVM resource test proves the checked-in attribution file parses to a nonempty catalog using the pinned API. Verification includes strict OpenSpec validation, an explicit catalog regeneration plus JSON/diff audit, full supported JVM/Android/desktop checks, iOS tooling/simulator checks with the existing common-test `Thread` blocker recorded if still present, and manual source-link/logo QA on target platforms.

## Scope

This is additive UI and build metadata work only. It does not change the database, playback, scanning, source access, app entry points, persistence, or support for Windows/Linux.
