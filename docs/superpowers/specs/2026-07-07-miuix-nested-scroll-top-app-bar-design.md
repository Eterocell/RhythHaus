# Miuix Nested-Scroll TopAppBar Migration Design

## Purpose

Migrate the Library nested-scroll collapsed chrome from a hand-built `Row` + `Text` toolbar to Miuix `SmallTopAppBar`, while preserving RhythHaus' existing glass/backdrop shell and scroll-triggered reveal behavior.

This is a follow-up to `fefa11c feat: migrate top bars to Miuix`. The ordinary Search, Settings, and Library drill-down back/title bars already use `RhythHausTopAppBar`; this change targets only the collapsed nested-scroll chrome rendered by `NestedScrollBlurChrome` for Library home and drill-down pages.

## Current State

`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt` currently implements `NestedScrollBlurChrome` as:

- a top overlay `Box` whose height is `statusBarHeight + 56.dp`;
- a `rhythHausLiquidGlass` surface over a recorded backdrop;
- a bottom-aligned 56.dp toolbar area;
- a custom `Row` with an 8.dp pulse dot and a 14sp black title that fades in after scroll progress reaches about 0.68;
- a 1.dp bottom divider whose alpha follows scroll progress.

Call sites:

- `LibraryHomeContent.kt` passes the Library title and home backdrop.
- `LibraryDetailContent.kt` passes the drill-down title and detail backdrop.

The scroll trigger is pure state derived from `LazyListState.toLibraryScrollPosition()` via `nestedScrollChromeStateFor(...)`. It does not currently use Miuix `ScrollBehavior.nestedScrollConnection`.

## Decision

Use Miuix `SmallTopAppBar` for the visible collapsed nested-scroll toolbar content inside the existing RhythHaus glass overlay.

This is intentionally a partial Miuix migration:

- Miuix owns the toolbar layout and title rendering.
- RhythHaus keeps owning backdrop recording, glass drawing, status-bar coverage, scroll progress thresholding, and the bottom divider.
- Do not switch Library lists to Miuix `MiuixScrollBehavior` in this change, because the existing Library scroll state also drives bottom-bar visibility, route scroll restoration, and both home/detail chrome progress. Wiring nested scroll behavior directly would be a larger behavior change.

## Approach

1. Extend the shared `RhythHausTopAppBar` wrapper with optional parameters required by the nested-scroll chrome:
   - `onBack` remains nullable and existing call sites remain source-compatible.
   - Add optional `color`, `titleColor`, `subtitleColor`, `defaultWindowInsetsPadding`, and padding parameters if needed.
   - Keep defaults identical to the current wrapper behavior so Search, Settings, and drill-down back/title bars do not change.

2. Replace the custom title `Row` inside `NestedScrollBlurChrome` with `RhythHausTopAppBar` or direct Miuix `SmallTopAppBar` usage.
   - Preferred: use `RhythHausTopAppBar` so all RhythHaus Miuix compact app bars share one wrapper.
   - The nested-scroll instance should pass `onBack = null` and `defaultWindowInsetsPadding = false`.
   - The top bar background should be transparent or glass-compatible so the existing `rhythHausLiquidGlass` surface remains visible.
   - The top bar should be alpha-controlled by the existing `titleProgress` threshold.

3. Preserve the overlay box and divider.
   - Keep `chromeHeight = statusBarHeight + NestedScrollChromeToolbarHeight`.
   - Keep the early return when `progress <= 0f`.
   - Keep `rhythHausLiquidGlass(...)` on the outer overlay.
   - Keep the bottom divider alpha based on `progress`.

4. Preserve all scroll-state behavior.
   - Keep `nestedScrollChromeStateFor(...)` activation semantics unless tests explicitly cover a refactor.
   - Keep `LibraryHomeContent` and `DrillDownView` list state wiring and scroll reporting.
   - Keep Now Playing bar visibility logic untouched.

## Non-goals

- No new Miuix dependencies.
- No `miuix-navigation3-adaptive`.
- No replacement of `NestedScrollBlurChrome`'s glass/backdrop implementation with Miuix blur primitives.
- No adoption of `MiuixScrollBehavior.nestedScrollConnection` for Library lists in this change.
- No changes to route transitions, Library navigation stack, bottom bar visibility, track rows, artwork, equalizer, or Now Playing controls.
- No changes to `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`.

## Validation

Task-level validation:

- `./gradlew :shared:compileKotlinJvm --configuration-cache`
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`
- `openspec validate miuix-nested-scroll-top-app-bar --strict`
- `git diff --check`

Final validation:

- `openspec validate miuix-nested-scroll-top-app-bar --strict`
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
- `/usr/bin/xcrun xcodebuild -version`
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
- `git diff --check`

Manual QA after automation:

- Library home: scroll from top; the glass top chrome appears over the status bar and Miuix title fades in without a seam.
- Library drill-down: same behavior with album/artist title.
- Search, Settings, and drill-down back/title bars still look unchanged after wrapper extension.
