# Miuix TopAppBar Migration Design

## Purpose

Replace RhythHaus ordinary custom back/title bars with Miuix `SmallTopAppBar` while preserving current navigation, screen layout intent, and app-specific chrome behavior.

This is a scoped follow-up to the Miuix component migration. It targets the plain `BackChip` + title/header rows that act as normal page top bars. It does not redesign nested-scroll glass chrome, Now Playing, playback controls, adaptive layout, or other product-specific music surfaces.

## Current Context

The current ordinary custom top bars are:

- `SettingsScreen.kt`: a route-level row with `BackChip`, spacer, and right-aligned Settings title.
- `SearchScreen.kt`: a route-level row with `BackChip`, spacer, and right-aligned Search title.
- `LibraryRows.kt` / `DrillDownHeader`: a drill-down header with `BackChip`, subtitle row, and large custom title.

Miuix 0.9.3 provides:

```kotlin
SmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitle: String = "",
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = TopAppBarDefaults.NavigationIconPadding,
    actionIconPadding: Dp = TopAppBarDefaults.ActionIconPadding,
    bottomContent: @Composable () -> Unit = {},
)
```

`SmallTopAppBar` is a better fit than full `TopAppBar` for these replacement targets because the current Search and Settings bars are compact single-row chrome. Drill-down keeps its large content title in the page content and uses `SmallTopAppBar` only for the back/subtitle header role.

## Approach

Create a small shared RhythHaus wrapper, `RhythHausTopAppBar`, around Miuix `SmallTopAppBar`. This keeps screen code concise and centralizes the Miuix color/padding/back icon choices.

The wrapper will:

- use `SmallTopAppBar` from `top.yukonga.miuix.kmp.basic`;
- accept `title: String`, optional `subtitle: String`, and `onBack: (() -> Unit)?`;
- use `HausColors.current.paper`, `ink`, and `muted` so it visually matches existing screens;
- set `defaultWindowInsetsPadding = false` because callers already apply `safeContentPadding()` or explicit status-bar content padding;
- expose `modifier` for width/placement;
- provide a Miuix `IconButton` navigation icon with a Material arrow-back icon and `Res.string.back` content description when `onBack` is present.

The existing `BackChip` composable may remain in the codebase if other app-specific surfaces still need it later, but targeted top-bar usages should stop depending on it.

## Target Changes

### Settings

Replace the custom title row with `RhythHausTopAppBar(title = stringResource(Res.string.settings), onBack = onDismiss)`.

The screen keeps its existing `Scaffold`, `safeContentPadding()`, content spacing, Settings dropdown behavior, scan controls, and clear-library action.

### Search

Replace the custom title row with `RhythHausTopAppBar(title = stringResource(Res.string.search), onBack = onDismiss)`.

The screen keeps focus request, Miuix `TextField`, stable outer field border, query filtering, result count, clear action, result selection, now-playing highlight, equalizer, and dismiss behavior.

### Library Drill-Down

Replace the `DrillDownHeader` top row (`BackChip` + subtitle) with `RhythHausTopAppBar(title = subtitle, onBack = onBack)` above the existing large drill-down title.

This preserves the current large page title, list content, left-edge swipe back gesture, nested-scroll blur chrome overlay, `SectionLabel`, track rows, Now Playing bar, and route animation semantics.

## Non-Goals

- No full app redesign.
- No changes to playback, scanning, library state, route stack, persistence, or native platform code.
- No `miuix-navigation3-adaptive` dependency.
- No Miuix migration of nested-scroll glass chrome, Now Playing screen, Now Playing bar, music scrubber, artwork/equalizer visuals, adaptive shell, or blur/backdrop wrappers.
- No changes to the pre-existing modified `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`.

## Verification

- Validate OpenSpec: `openspec validate miuix-top-app-bar --strict`.
- Run focused shared compile after source edits: `./gradlew :shared:compileKotlinJvm --configuration-cache`.
- Run the relevant navigation test: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`.
- Run `git diff --check` for whitespace hygiene.
- Before completion, run the repository's broad verification subset or record exact blockers.
