# Adaptive Layout and Miuix Blur Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add adaptive list-detail UI for tablets/desktops and replace Kyant Backdrop glass with Miuix blur while preserving compact phone behavior.

**Architecture:** Keep `LibraryNavigationStack` as the route source of truth. Add a tested adaptive layout-mode helper, use compact mode for the existing one-pane route renderer, and use `miuix-navigation3-adaptive:0.8.5` `ListDetailPaneScaffold` for wide list/detail rendering if it compiles while current Miuix modules remain on `0.9.2`. Keep blur recording/drawing behind a RhythHaus wrapper so top chrome and bottom bar call sites stay contained.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Miuix `0.9.2`, attempted `miuix-navigation3-adaptive:0.8.5`, Miuix Blur `0.9.2`, existing NavigationEvent back handling, Gradle version catalog.

## Global Constraints

- Compact phone layout and behavior must remain unchanged.
- Wide layout threshold: list-detail at width >= 840dp, or width >= 600dp with height / width < 1.2.
- Try `top.yukonga.miuix.kmp:miuix-navigation3-adaptive:0.8.5` but keep existing Miuix modules at `0.9.2`.
- Do not downgrade current Miuix modules without explicit user approval.
- Replace Kyant Backdrop/Shapes with `top.yukonga.miuix.kmp:miuix-blur:0.9.2` if compilation succeeds.
- Preserve playback, scanner, library persistence, Search, Settings, Clear Library dialog, Now Playing overlay, route stack semantics, gestures, and bottom/top glass single-layer behavior.
- Record exact dependency/compile blockers instead of guessing or silently changing strategy.
- Verification target: `openspec validate adaptive-layout-miuix-blur --strict`, focused JVM tests, `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `/usr/bin/xcrun xcodebuild -version`, and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.

---

### Task 1: Dependency Gate for Miuix Blur and Adaptive

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`

**Interfaces:**
- Produces: catalog aliases `libs.miuix.blur` and `libs.miuix.navigation3.adaptive`.
- Produces: compile evidence proving whether adaptive can be used without downgrading existing Miuix modules.

- [ ] **Step 1: Add version-catalog aliases**

In `gradle/libs.versions.toml`:

```toml
[versions]
miuix = "0.9.2"
miuix-navigation3-adaptive = "0.8.5"

[libraries]
miuix-ui = { module = "top.yukonga.miuix.kmp:miuix-ui", version.ref = "miuix" }
miuix-blur = { module = "top.yukonga.miuix.kmp:miuix-blur", version.ref = "miuix" }
miuix-navigation3-adaptive = { module = "top.yukonga.miuix.kmp:miuix-navigation3-adaptive", version.ref = "miuix-navigation3-adaptive" }
```

Keep existing catalog entries not shown here unchanged for this step.

- [ ] **Step 2: Add dependencies without removing Kyant yet**

In `shared/build.gradle.kts`, add to `commonMain.dependencies`:

```kotlin
implementation(libs.miuix.blur)
implementation(libs.miuix.navigation3.adaptive)
```

Do not remove `libs.kyant.backdrop` or `libs.kyant.shapes` yet; Task 2 removes them after Miuix blur compiles.

- [ ] **Step 3: Run dependency/compile gate**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected if compatible: `BUILD SUCCESSFUL`.

If it fails because `miuix-navigation3-adaptive:0.8.5` pulls incompatible Miuix/Nav3 modules, record the exact output in the task report and stop for coordinator/user decision. Do not downgrade `miuix` from `0.9.2`.

- [ ] **Step 4: Inspect resolved Miuix modules**

Run:

```bash
./gradlew :shared:dependencies --configuration commonMainImplementationDependenciesMetadata | grep -E 'top.yukonga.miuix.kmp:(miuix-ui|miuix-blur|miuix-navigation3-ui|miuix-navigation3-adaptive)'
```

Expected: existing current Miuix modules remain on `0.9.2`; adaptive may show `0.8.5`. If output is too noisy or the configuration name differs, use the closest shared metadata dependency configuration and record the exact command used.

- [ ] **Step 5: Commit**

If compile gate passes:

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "chore: add miuix adaptive and blur dependencies"
```

### Task 2: Replace Kyant Backdrop Wrapper with Miuix Blur

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`

**Interfaces:**
- Consumes: `libs.miuix.blur` from Task 1.
- Produces: `rememberRhythHausBackdrop(): top.yukonga.miuix.kmp.blur.LayerBackdrop`.
- Produces: `Modifier.recordRhythHausBackdrop(backdrop: LayerBackdrop): Modifier`.
- Produces: `Modifier.rhythHausLiquidGlass(backdrop: LayerBackdrop, shape: Shape, fallbackColor: Color, blurRadius: Dp = RhythHausGlassBlurRadius): Modifier`.

- [ ] **Step 1: Update wrapper imports and implementation**

Replace Kyant imports in `LiquidGlassChrome.kt` with Miuix blur imports:

```kotlin
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
```

Implement:

```kotlin
@Composable
internal fun rememberRhythHausBackdrop(): LayerBackdrop = rememberLayerBackdrop()

internal fun Modifier.recordRhythHausBackdrop(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)

internal const val RhythHausGlassSurfaceAlpha = 0.72f
internal val RhythHausGlassBlurRadius = 10.dp
internal val RhythHausGlassRefractionHeight = 16.dp
internal val RhythHausGlassRefractionAmount = 24.dp

internal fun Modifier.rhythHausLiquidGlass(
    backdrop: LayerBackdrop,
    shape: Shape,
    fallbackColor: Color,
    blurRadius: Dp = RhythHausGlassBlurRadius,
    refractionHeight: Dp = RhythHausGlassRefractionHeight,
    refractionAmount: Dp = RhythHausGlassRefractionAmount,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        blur(blurRadius.toPx())
    },
    onDrawSurface = {
        drawRect(fallbackColor)
    },
)
```

Keep `refractionHeight` and `refractionAmount` parameters temporarily for call-site compatibility even though Miuix blur does not use them. Suppress unused parameters only if Kotlin requires it; otherwise leave them for a future cleanup.

- [ ] **Step 2: Update call-site imports/types**

In `App.kt` and `NowPlayingBar.kt`, replace:

```kotlin
import com.kyant.backdrop.backdrops.LayerBackdrop
```

with:

```kotlin
import top.yukonga.miuix.kmp.blur.LayerBackdrop
```

- [ ] **Step 3: Remove Kyant dependencies**

In `shared/build.gradle.kts`, remove:

```kotlin
implementation(libs.kyant.backdrop)
implementation(libs.kyant.shapes)
```

In `gradle/libs.versions.toml`, remove these entries if no longer used anywhere:

```toml
backdrop = "2.0.0"
kyant-shapes = "1.2.0"
kyant-backdrop = { module = "io.github.kyant0:backdrop", version.ref = "backdrop" }
kyant-shapes = { module = "io.github.kyant0:shapes", version.ref = "kyant-shapes" }
```

- [ ] **Step 4: Verify compile and Kyant removal**

Run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

Run:

```bash
grep -R "com.kyant.backdrop\|kyant-backdrop\|kyant-shapes" -n gradle shared/src || true
```

Expected: no source/catalog references remain.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt
git commit -m "feat: replace backdrop glass with miuix blur"
```

### Task 3: Adaptive Layout Mode Helper and Tests

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- Modify: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`

**Interfaces:**
- Produces: `enum class LibraryAdaptiveLayoutMode { Compact, ListDetail }`.
- Produces: `fun libraryAdaptiveLayoutModeFor(widthDp: Float, heightDp: Float): LibraryAdaptiveLayoutMode`.

- [ ] **Step 1: Write failing tests**

Add tests to `LibraryNavigationTest.kt`:

```kotlin
@Test
fun adaptiveLayoutUsesCompactForPhonePortrait() {
    assertEquals(
        LibraryAdaptiveLayoutMode.Compact,
        libraryAdaptiveLayoutModeFor(widthDp = 390f, heightDp = 844f),
    )
}

@Test
fun adaptiveLayoutUsesCompactForNarrowPortraitTablet() {
    assertEquals(
        LibraryAdaptiveLayoutMode.Compact,
        libraryAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 1000f),
    )
}

@Test
fun adaptiveLayoutUsesListDetailForWideTablet() {
    assertEquals(
        LibraryAdaptiveLayoutMode.ListDetail,
        libraryAdaptiveLayoutModeFor(widthDp = 840f, heightDp = 1180f),
    )
}

@Test
fun adaptiveLayoutUsesListDetailForLandscapeMediumWidth() {
    assertEquals(
        LibraryAdaptiveLayoutMode.ListDetail,
        libraryAdaptiveLayoutModeFor(widthDp = 700f, heightDp = 500f),
    )
}

@Test
fun adaptiveLayoutUsesListDetailForDesktopWidth() {
    assertEquals(
        LibraryAdaptiveLayoutMode.ListDetail,
        libraryAdaptiveLayoutModeFor(widthDp = 1200f, heightDp = 800f),
    )
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected before implementation: compile/test failure because `LibraryAdaptiveLayoutMode` and `libraryAdaptiveLayoutModeFor` do not exist.

- [ ] **Step 3: Implement helper**

Add to `LibraryNavigation.kt` near other pure UI/navigation helpers:

```kotlin
enum class LibraryAdaptiveLayoutMode {
    Compact,
    ListDetail,
}

fun libraryAdaptiveLayoutModeFor(
    widthDp: Float,
    heightDp: Float,
): LibraryAdaptiveLayoutMode {
    if (widthDp >= 840f) return LibraryAdaptiveLayoutMode.ListDetail
    if (widthDp >= 600f && widthDp > 0f && heightDp / widthDp < 1.2f) return LibraryAdaptiveLayoutMode.ListDetail
    return LibraryAdaptiveLayoutMode.Compact
}
```

- [ ] **Step 4: Run focused tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt
git commit -m "test: add adaptive layout mode rules"
```

### Task 4: Wide List-Detail Library Rendering

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt` if helper behavior changes are needed

**Interfaces:**
- Consumes: `libraryAdaptiveLayoutModeFor(widthDp, heightDp)`.
- Consumes: `androidx.navigation3.adaptive.ListDetailPaneScaffold` if Task 1 dependency gate passed.
- Preserves: `LibraryNavigationStack`, `pushRoute`, `popRoute`, `replaceTop`, `NowPlayingBar`, `NowPlayingExpandOverlay`.

- [ ] **Step 1: Add Compose window-size mode calculation**

In `LibraryHomeScreen`, use `BoxWithConstraints` or `LocalWindowInfo` at the root to calculate width/height dp and derive `LibraryAdaptiveLayoutMode` via the pure helper. Prefer `BoxWithConstraints` because `App.kt` already imports it.

Implementation shape:

```kotlin
BoxWithConstraints(
    modifier = modifier
        .fillMaxSize()
        .background(HausColors.current.paper)
        .onSizeChanged { screenHeightPx = it.height.toFloat() },
) {
    val adaptiveLayoutMode = libraryAdaptiveLayoutModeFor(
        widthDp = maxWidth.value,
        heightDp = maxHeight.value,
    )
    // render compact or wide root content here
}
```

- [ ] **Step 2: Extract/reuse compact content without behavior changes**

Move the existing `AnimatedContent` route rendering, fixed bottom bar, and Now Playing overlay into the compact branch with no behavior changes. Keep existing predictive-back previous-route rendering in compact mode.

Expected compact branch still contains:

```kotlin
AnimatedContent(
    targetState = navigation.current,
    transitionSpec = { routeContentTransform(lastNavigationTransition) },
    label = "LibraryRouteTransition",
    modifier = Modifier
        .fillMaxSize()
        .recordRhythHausBackdrop(rootBackdrop)
        .offset(x = predictiveBackOffset.value.dp),
) { currentRoute ->
    RouteContent(route = currentRoute)
}
```

- [ ] **Step 3: Add wide list-detail branch**

Import:

```kotlin
import androidx.navigation3.adaptive.ListDetailPaneScaffold
```

In list-detail mode, render:

```kotlin
ListDetailPaneScaffold(
    list = {
        RouteContent(route = LibraryRoute.Home)
    },
    detail = when (val route = navigation.current) {
        is LibraryRoute.AlbumDetail, is LibraryRoute.ArtistDetail -> { RouteContent(route = route) }
        else -> null
    },
    detailPlaceholder = {
        AdaptiveDetailPlaceholder()
    },
)
```

Then render Search, Settings, Clear Library dialog, fixed bottom bar, and Now Playing overlay above the scaffold using existing route checks/state. If directly reusing `RouteContent(Home)` would also draw overlays in the list pane, split only the minimal overlay rendering out of the Home route branch so overlays are not duplicated.

- [ ] **Step 4: Make wide album/artist selection replace active detail**

Add helper inside `LibraryHomeScreen`:

```kotlin
fun openDetailRoute(route: LibraryRoute) {
    val isWideDetail = adaptiveLayoutMode == LibraryAdaptiveLayoutMode.ListDetail &&
        (navigation.current is LibraryRoute.AlbumDetail || navigation.current is LibraryRoute.ArtistDetail) &&
        (route is LibraryRoute.AlbumDetail || route is LibraryRoute.ArtistDetail)
    if (isWideDetail) updateNavigation(navigation.replaceTop(route)) else pushRoute(route)
}
```

Use `openDetailRoute(...)` for AlbumCard and ArtistRow clicks. Keep Search/Settings/Clear Library using `pushRoute`.

If scoping prevents this exact helper placement, implement an equivalent that uses `replaceTop` only for detail-to-detail selection in list-detail mode.

- [ ] **Step 5: Add placeholder composable**

Add a private composable in `App.kt`:

```kotlin
@Composable
private fun AdaptiveDetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(Res.string.library),
                color = HausColors.current.ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Select an album or artist to show details here.",
                color = HausColors.current.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
```

If project string-resource policy requires no hardcoded new user-facing strings, add a string resource instead of hardcoding this copy.

- [ ] **Step 6: Verify focused compile/tests**

Run:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Expected: both `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt
git commit -m "feat: add adaptive library list detail layout"
```

### Task 5: Final Verification and Evidence

**Files:**
- Modify: `openspec/changes/adaptive-layout-miuix-blur/tasks.md`
- Modify: `progress.md`

**Interfaces:**
- Consumes: all previous task outputs.
- Produces: recorded verification evidence and completed OpenSpec task status.

- [ ] **Step 1: Validate OpenSpec change**

Run:

```bash
openspec validate adaptive-layout-miuix-blur --strict
```

Expected: `Change 'adaptive-layout-miuix-blur' is valid`.

- [ ] **Step 2: Run broad JVM/desktop/Android verification**

Run:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run iOS verification**

Run:

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Expected: Xcode version prints successfully and iOS simulator tests return `BUILD SUCCESSFUL`. If unavailable, record the exact blocker.

- [ ] **Step 4: Check diff hygiene and forbidden imports**

Run:

```bash
git diff --check
grep -R "com.kyant.backdrop\|kyant-backdrop\|kyant-shapes" -n gradle shared/src || true
```

Expected: `git diff --check` has no output; grep has no source/catalog references.

- [ ] **Step 5: Update OpenSpec tasks and progress evidence**

Mark tasks complete in `openspec/changes/adaptive-layout-miuix-blur/tasks.md` with exact command evidence.

Append to `progress.md` using the AGENTS.md handoff format:

```text
Route: openspec+superpowers
Owner: implementation
Input: adaptive-layout-miuix-blur spec/plan
Output: adaptive wide layout + Miuix blur replacement
Next owner: user for manual tablet/desktop visual validation
Blockers: <none or exact blocker>
```

- [ ] **Step 6: Commit final evidence**

```bash
git add openspec/changes/adaptive-layout-miuix-blur/tasks.md progress.md
git commit -m "docs: record adaptive layout verification"
```
