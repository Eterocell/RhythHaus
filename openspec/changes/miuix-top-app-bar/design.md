# Design: Miuix TopAppBar Migration

## Strategy

Use Miuix `SmallTopAppBar` for ordinary compact page chrome. Keep one RhythHaus wrapper so screen code does not duplicate the same color, inset, navigation icon, and title choices.

`SmallTopAppBar` is preferred over full `TopAppBar` for this change because the replacement targets are compact back/title rows. The existing Library drill-down large title remains page content below the top app bar instead of being forced into Miuix's collapsible large-title model, because that view already has nested-scroll blur chrome and custom route behavior.

## Wrapper

Create `RhythHausTopAppBar` under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/`.

The wrapper uses:

- `top.yukonga.miuix.kmp.basic.SmallTopAppBar`
- `top.yukonga.miuix.kmp.basic.IconButton`
- Material `Icons.AutoMirrored.Filled.ArrowBack` or the available equivalent from existing Compose Material icons
- `HausColors.current.paper`, `ink`, and `muted`
- `defaultWindowInsetsPadding = false`

The wrapper accepts:

```kotlin
@Composable
fun RhythHausTopAppBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String = "",
)
```

## Screen Migration

### Settings

Replace the current `Row` containing `BackChip`, `Spacer`, and Settings `Text` with:

```kotlin
RhythHausTopAppBar(
    title = stringResource(Res.string.settings),
    onBack = onDismiss,
)
```

Settings keeps its existing Miuix `Scaffold`, content padding, appearance dropdown, scanning card, add-folder button, import message, and clear-library button.

### Search

Replace the current `Row` containing `BackChip`, `Spacer`, and Search `Text` with:

```kotlin
RhythHausTopAppBar(
    title = stringResource(Res.string.search),
    onBack = onDismiss,
)
```

Search keeps its current Miuix `TextField`, focus requester, query/filtering, placeholder, clear action, result rows, now-playing highlight, equalizer, and dismiss behavior.

### Library Drill-Down

Replace only the `BackChip` + subtitle row inside `DrillDownHeader` with:

```kotlin
RhythHausTopAppBar(
    title = subtitle,
    onBack = onBack,
)
```

Keep the large drill-down `Text(title)` below the top app bar. Do not change `DrillDownView`, `NestedScrollBlurChrome`, `SectionLabel`, track rows, left-edge swipe back, scroll state reporting, or Now Playing bar behavior.

## Verification

- `openspec validate miuix-top-app-bar --strict`
- `./gradlew :shared:compileKotlinJvm --configuration-cache`
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`
- `git diff --check`
- Final broad verification before commit, or exact blocker recording.
