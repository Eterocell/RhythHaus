# Change: Miuix Nested-Scroll TopAppBar

> **Superseded — 2026-11-12.** Do not implement or sync this proposal. The Library nested-scroll overlay described here was removed from the current UI: [`library-home-settings-spacing`](../library-home-settings-spacing/proposal.md) removed the home overlay, and current `LibraryDetailContent.kt` / `LibraryChrome.kt` render no Miuix nested-scroll top bar. The listed device-visual QA for that removed chrome is not an active acceptance gate. Keep the checkboxes as historical implementation evidence; do not revive the overlay.

## Why

The ordinary Search, Settings, and Library drill-down back/title rows already use Miuix `SmallTopAppBar` through `RhythHausTopAppBar`, but the Library nested-scroll collapsed chrome still hand-builds a toolbar row with raw Compose primitives.

Migrating that collapsed chrome to a Miuix top app bar improves consistency while preserving RhythHaus' custom glass overlay, status-bar coverage, and scroll-triggered reveal behavior.

## What Changes

- Extend `RhythHausTopAppBar` only as needed to support transparent/glass-backed nested-scroll usage while keeping current defaults unchanged.
- Replace `NestedScrollBlurChrome`'s custom collapsed title row with a Miuix top app bar rendering path.
- Keep the existing glass/backdrop shell, status-bar height handling, divider, and `nestedScrollChromeStateFor(...)` state derivation.
- Preserve Library home and drill-down list behavior, scroll reporting, Now Playing bar visibility, route transitions, and all existing Miuix TopAppBar call sites.

## Impact

- No dependency changes.
- No `miuix-navigation3-adaptive`.
- Shared Compose UI only.
- Automated verification covers OpenSpec validity, shared JVM compile, Library navigation/scroll tests, desktop compile, Android assemble, and iOS simulator tests.
- Manual QA remains needed for visual seam/fade behavior on target devices.
