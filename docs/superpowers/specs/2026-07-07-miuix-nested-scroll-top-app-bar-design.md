# Miuix Nested-Scroll TopAppBar Migration Design

## Purpose

Migrate Library nested-scroll top chrome toward Miuix app-bar behavior. The final drill-down track-list correction adopts Miuix `MiuixScrollBehavior` and Miuix `TopAppBar` large-title/collapsed-title behavior, while preserving RhythHaus' existing glass/backdrop shell.

This is a follow-up to `fefa11c feat: migrate top bars to Miuix`. The ordinary Search, Settings, and Library drill-down back/title bars already use `RhythHausTopAppBar`; this change targets only the collapsed nested-scroll chrome rendered by `NestedScrollBlurChrome` for Library home and drill-down pages.

## Current State

`shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt` currently implements `NestedScrollBlurChrome` as:

- a top overlay `Box` whose height is `statusBarHeight + 56.dp`;
- a `rhythHausLiquidGlass` surface over a recorded backdrop;
- a bottom-aligned 56.dp toolbar area;
- a custom `Row` with an 8.dp pulse dot and a 14sp black title that fades in after scroll progress reaches about 0.68;
- a 1.dp bottom divider whose alpha follows scroll progress.

Original call sites:

- `LibraryHomeContent.kt` passes the Library title and home backdrop.
- `LibraryDetailContent.kt` passes the drill-down title and detail backdrop.

The original scroll trigger was pure state derived from `LazyListState.toLibraryScrollPosition()` via `nestedScrollChromeStateFor(...)`. The drill-down track-list correction replaces that specific path with Miuix `MiuixScrollBehavior().nestedScrollConnection`.

## Decision

Use Miuix `TopAppBar` with `MiuixScrollBehavior` for the drill-down track-list title transition so the expanded large title and collapsed title are one Miuix behavior.

This is intentionally a partial Miuix migration:

- Miuix owns the drill-down top app bar layout, title transition, and nested-scroll collapse behavior.
- RhythHaus keeps owning backdrop recording, glass drawing, list scroll reporting, route transitions, Now Playing visibility, and the home-screen nested-scroll chrome until explicitly migrated.

## Approach

1. Extend the shared `RhythHausTopAppBar` wrapper with optional parameters required by the nested-scroll chrome:
   - `onBack` remains nullable and existing call sites remain source-compatible.
   - Add optional `color`, `titleColor`, `subtitleColor`, `defaultWindowInsetsPadding`, and padding parameters if needed.
   - Keep defaults identical to the current wrapper behavior so Search, Settings, and drill-down back/title bars do not change.

2. For drill-down track lists, render `TopAppBar(title = title, largeTitle = title, scrollBehavior = MiuixScrollBehavior(), ...)` in the RhythHaus glass overlay.
   - Attach `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` to the drill-down `LazyColumn`.
   - Provide the same back callback as the navigation icon.
   - Keep the app-bar background transparent/glass-compatible so `rhythHausLiquidGlass` remains visible.

3. Preserve the overlay box and divider.
   - Keep `chromeHeight = statusBarHeight + NestedScrollChromeToolbarHeight`.
   - Keep the early return when `progress <= 0f`.
   - Keep `rhythHausLiquidGlass(...)` on the outer overlay.
   - Keep the bottom divider alpha based on `progress`.

4. Preserve surrounding behavior.
   - Keep `LibraryHomeContent` and its current `nestedScrollChromeStateFor(...)` wiring unless a future change migrates home too.
   - Keep `DrillDownView` list-state scroll reporting for Now Playing visibility.
   - Keep Now Playing bar visibility logic untouched.

## Non-goals

- No new Miuix dependencies.
- No `miuix-navigation3-adaptive`.
- No replacement of `NestedScrollBlurChrome`'s glass/backdrop implementation with Miuix blur primitives.
- No dependency additions.
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
