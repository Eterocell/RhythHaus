## Task 5.1: Move Now Playing Feature

**Scope:** Slice 5 first leaf feature; one atomic UI extraction plus the small reusable
`core/ui` moves required to preserve dependency direction. The controller owns the
implementation commit, independent review, staging, evidence ledger, and final conventional
commit; this plan does not mark implementation complete or prescribe intermediate commits.

**Frozen constraints:**

- Create exactly one UI-only implementation module, `:feature:nowplaying`. Do not create an API
  module, presenter, `UiState`/`UiEvent`/`UiEffect` scaffolding, or Koin `Module`; the feature owns
  no injectable bindings and is composed through callable/composable entry points. Only `:shared`
  assembles and starts Koin, and no service-locator back-reference is permitted.
- Target exactly Android-KMP, JVM, `iosArm64`, and `iosSimulatorArm64`. Create no feature
  `iosMain` source and no feature framework export. Preserve the sole Shared framework,
  `MainViewController`, and existing Swift-visible playback bridge identities.
- The only direct project dependencies are exactly `api(:core:playback)` and `api(:core:ui)`.
  The feature must not depend directly on `:core:model`, `:shared`, `:taglib`, Library API,
  Library implementation, another feature implementation, or an app. `Track` comes through the
  approved playback public surface rather than a direct core-model edge.
- Set Android namespace exactly to `com.eterocell.rhythhaus.nowplaying` and Compose resource
  package exactly to `rhythhaus.feature.nowplaying.generated.resources`.
- Preserve Kotlin packages, UI behavior, route/Back behavior, playback bridge identities, Swift
  surface, resource localization, and existing unused compatibility parameters. Compilation,
  linking, packaging, and Swift-consumer evidence do not claim runtime UI or playback validation.

**Exact file map:**

- Create/register `feature/nowplaying/build.gradle.kts` and the exact source roots
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/`,
  `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/`,
  `feature/nowplaying/src/androidHostTest/kotlin/`, `feature/nowplaying/src/jvmTest/kotlin/`,
  `feature/nowplaying/src/iosTest/kotlin/`, and
  `feature/nowplaying/src/commonMain/composeResources/values/` plus `values-zh/`.
- Move implementation from
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt` and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt` into the
  feature while preserving package `com.eterocell.rhythhaus.nowplaying`. Keep shared's public
  `NowPlayingScreen(track, playbackState, playbackController, tagLibReader, currentLibraryTrack,
  onBack, modifier)` facade and have it delegate to distinctly named feature `NowPlayingContent`.
  `LibraryAppShell.kt` remains the shared composition boundary and route/shell owner.
- Move
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt` to
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`,
  and `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt` to
  `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`.
  Keep both declarations package-stable as `com.eterocell.rhythhaus.ui`; do not rename them.
- Move `NowPlayingAdaptiveLayoutMode` and `nowPlayingAdaptiveLayoutModeFor` from
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt` into
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingAdaptiveLayout.kt`.
  Their new package is `com.eterocell.rhythhaus.nowplaying`; their old Library package is not a
  permitted feature package root. Shared retains route state,
  `LibraryBackTarget.NowPlaying`, Back arbitration/predictive Back, shell measurement/visibility,
  route dispatch, navigation effects, and shell overlay policy.
- Move `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt` package-
  stably to `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt`,
  retaining generic `leftEdgeSwipeBack(onBack: () -> Unit)` and caller-owned callbacks.
- Move `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt` package-
  stably to `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt`,
  and replace Miuix-facing signatures with the opaque core-ui `RhythHausBackdrop` API described
  below. Move `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt`
  to `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt` and
  make its stateless mechanism accept direction and terminal callback from local adapters.
  Name the direction type `VerticalSheetGestureDirection`; do not leave `verticalSheetGesture`
  shared-only or expose navigation APIs from core UI.

**Exact build contract:**

- Add `gradle/libs.versions.toml` alias
  `compose-animation = { module = "org.jetbrains.compose.animation:animation", version.ref = "compose-multiplatform" }`
  under `[libraries]`, following the existing hyphenated Compose alias convention and the
  existing `compose-multiplatform` version key. Create `feature/nowplaying/build.gradle.kts` with
  this exact plugin/convention block:

  ```kotlin
  plugins {
      id("build-logic.kmp.feature.impl")
      id("build-logic.android.kmp.library")
      id("build-logic.compose-resources")
      alias(libs.plugins.compose.compiler)
  }

  extensions.configure<ControlledComposeResourcesExtension>("architectureComposeResources") {
      namespace("rhythhaus.feature.nowplaying.generated.resources")
  }
  ```

  The `build-logic.kmp.feature.impl` convention applies Kotlin Multiplatform and provides the
  general production-KSP wiring specified later; it does not configure this feature's targets,
  source sets, or dependencies.
- In `feature/nowplaying/build.gradle.kts`, inside `kotlin { ... }`, configure exactly:

  ```kotlin
  android {
      namespace = "com.eterocell.rhythhaus.nowplaying"
      compileSdk = libs.versions.android.compileSdk.get().toInt()
      minSdk = libs.versions.android.minSdk.get().toInt()
      compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
      withHostTest {}
      androidResources { enable = true }
  }
  jvm()
  iosArm64()
  iosSimulatorArm64()
  ```

  Use the repository's catalog accessors for the exact existing compile/min SDK keys. Do not use
  legacy Android target DSL, do not apply `com.android.library`, create no feature framework binary/export,
  and create no feature `iosMain` production source.
- The exact `commonMain.dependencies`, `commonTest.dependencies`, and `jvmTest.dependencies`
  blocks below live in `feature/nowplaying/build.gradle.kts`; they are not supplied by the
  `build-logic.kmp.feature.impl` convention. The convention KSP change remains the separate
  general-governance step specified later.
- In `commonMain.dependencies`, declare exactly `api(projects.core.playback)`,
  `api(projects.core.ui)`, `api(libs.compose.runtime)`, `api(libs.compose.ui)`,
  `api(libs.compose.foundation)`, `api(libs.compose.components.resources)`, and
  `api(libs.compose.animation)`. The direct animation dependency is required because
  `Animatable<Float, AnimationVector1D>` is public. Declare implementation-only
  `libs.compose.material.icons.extended`, `libs.compose.material3`, `libs.miuix.ui`, and
  `libs.kotlinx.coroutinesCore`.
- In `commonTest.dependencies`, declare `implementation(libs.kotlin.test)`. In `jvmTest.dependencies`,
  declare the same UI-test artifact and `compose.desktop.currentOs` used by `:core:ui`. Android
  host and iOS tests inherit `commonTest` and receive no extra direct dependency; a task discovery
  or compiler result requiring one is a blocker, not a scope decision.
- **Core UI dependency closure:** in `core/ui/build.gradle.kts`, retain all existing dependencies
  and add one distinct `commonMain.dependencies` subsection with exactly
  `api(libs.compose.animation)`, because `Animatable<Float, AnimationVector1D>` appears in public
  core-ui signatures; `api(libs.kotlinx.coroutinesCore)`, because `CoroutineScope` appears in a
  public core-ui signature; and `implementation(libs.miuix.blur)`, because `LayerBackdrop` and
  blur implementation remain internal. Do not duplicate an existing declaration. The single
  `compose-animation` catalog alias added above supports both `:feature:nowplaying` and `:core:ui`.
- Remove all feature imports of `LayerBackdrop`, `LazyTrackArtworkImage`, shared `Res`,
  `TrackArtwork`, Library, or TagLib types. Keep
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` and App's
  `LocalTrackArtworkLoader` provider in shared for other consumers. Adapt
  `{ trackId -> LocalTrackArtworkLoader.current(trackId)?.bytes }` at the shared facade/shell call
  site; `App.kt` and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` remain unchanged
  and shared-owned.

**Public interfaces and explicit API:**

- Every declaration in the new feature and moved core-ui files uses explicit visibility. Every
  public feature and core-ui symbol has declaration-specific behavioral KDoc explaining its
  observable contract, callback/gesture direction, state/nullability behavior, and preservation
  constraints. Do not use generic file-level KDoc as a substitute.
- Feature public declarations, all in `com.eterocell.rhythhaus.nowplaying`, are exactly:

  ```kotlin
  /** Renders expanded Now Playing and sends the generic left-edge callback to [onBack]. */
  @Composable
  public fun NowPlayingContent(
      track: Track,
      playbackState: PlaybackState,
      playbackController: PlaybackController,
      labels: NowPlayingScreenLabels,
      artworkLoader: suspend (String) -> ByteArray?,
      onBack: () -> Unit,
      modifier: Modifier = Modifier,
  ): Unit

  /** Renders the shell-composed mini-player and emits upward expansion only through [onExpand]. */
  @Composable
  public fun NowPlayingBar(
      track: Track?,
      playbackState: PlaybackState,
      labels: NowPlayingBarLabels,
      artworkLoader: suspend (String) -> ByteArray?,
      onPlayPause: () -> Unit,
      onExpand: () -> Unit,
      onSettings: () -> Unit,
      onSearch: () -> Unit,
      expandProgress: Animatable<Float, AnimationVector1D>,
      isExpanded: Boolean,
      interactive: Boolean = true,
      screenHeightPx: Float = 0f,
      backdrop: RhythHausBackdrop? = null,
      modifier: Modifier = Modifier,
  ): Unit

  /** Selects the mini-player behavior for a loaded track or an empty library. */
  public enum class BottomBarMode { TrackLoaded, EmptyLibraryNavigation }

  /** Returns [BottomBarMode.EmptyLibraryNavigation] only when [track] is null. */
  public fun bottomBarModeFor(track: Track?): BottomBarMode

  /** Immutable shared-resolved labels used by [NowPlayingContent]. */
  public data class NowPlayingScreenLabels(
      public val play: String,
      public val pause: String,
      public val albumArtwork: String,
      public val currentTrackArtistAlbum: String,
  )

  /** Immutable shared-resolved labels used by [NowPlayingBar]. */
  public data class NowPlayingBarLabels(
      public val play: String,
      public val pause: String,
      public val search: String,
      public val settings: String,
      public val albumArt: String,
      public val currentTrackArtistAlbum: String,
  )

  /** Selects the preserved compact or split Now Playing layout. */
  public enum class NowPlayingAdaptiveLayoutMode { Compact, Split }

  /** Returns the preserved Now Playing layout selection for the supplied bounds in dp. */
  public fun nowPlayingAdaptiveLayoutModeFor(
      widthDp: Float,
      heightDp: Float,
  ): NowPlayingAdaptiveLayoutMode
  ```

  `Track` in these signatures is transitively available through `:core:playback`'s public
  `api(:core:model)` surface; `:feature:nowplaying` declares no direct `:core:model` edge. The
  shared compatibility facade remains package-stable with its exact existing signature:

  ```kotlin
  @Composable
  public fun NowPlayingScreen(
      track: Track,
      playbackState: PlaybackState,
      playbackController: PlaybackController,
      tagLibReader: TagLibReader,
      currentLibraryTrack: LibraryTrack?,
      onBack: () -> Unit,
      modifier: Modifier = Modifier,
  ): Unit
  ```

  The facade retains its unused `TagLibReader` and `LibraryTrack?` parameters. It constructs
  exactly `NowPlayingScreenLabels(
  play = shared play,
  pause = shared pause,
  albumArtwork = shared album_artwork,
  currentTrackArtistAlbum =
      if (track != null) {
          shared track_artist_album_format(track.artist, track.album)
      } else {
          ""
      },
  )`, adapts `TrackArtwork?` to the loader returning bytes, and delegates only real inputs to
  `NowPlayingContent`. No feature signature imports TagLib, Library,
  or shared loader types, `Res` handles, or resource types.
- `LibraryAppShell` constructs exactly `NowPlayingBarLabels(
  play = shared play,
  pause = shared pause,
  search = shared search,
  settings = shared settings,
  albumArt = shared album_art,
  currentTrackArtistAlbum =
      if (track != null) {
          shared track_artist_album_format(track.artist, track.album)
      } else {
          ""
      },
  )` for its direct `NowPlayingBar` call. These are exactly the four screen-label fields and six
  bar-label fields; feature-owned resource strings never enter either object. Empty-library mode
  must not read `currentTrackArtistAlbum`; it resolves feature-owned `mini_player_empty_subtitle`
  internally. Shared resolves the injected values from retained resources, no `Res` handle crosses
  the seam, and focused tests prove both loaded mapping and empty-mode ignorance of the inert field.
- The feature owns lazy artwork state over exactly `suspend (String) -> ByteArray?` and renders it
  through public core-ui `ArtworkImage` and `ArtworkImageRole`. It does not import
  `TrackArtwork`, Library API/implementation, `LocalTrackArtworkLoader`, or
  `LazyTrackArtworkImage`. `NowPlayingBarRootTestTag`, `NowPlayingBarPlayPauseTestTag`,
  `NowPlayingBarSearchTestTag`, `NowPlayingBarSettingsTestTag`, and
  `NowPlayingBarContentPadding` remain `internal` because feature tests require them.
  File-local `NowPlayingUiState`, lazy-artwork state, feature-local upward-bar adapter, composable
  layout helpers, and implementation helpers are `private`; the shared-local downward overlay
  adapter remains `private` in its shared owner, `LibraryAppShell.kt` or its existing shared helper
  file. `MusicProgressScrubber`, `formatMillis`, `scrubberFractionForOffset`,
  `scrubberPositionForFraction`, `ScrubFractionState`, and `MusicScrubInteractionState` are all
  explicitly `internal` after the package-stable move; file helpers remain `private`, there are no
  named scrubber test constants, and the four named bar tags plus `NowPlayingBarContentPadding`
  remain `internal`.
- Preserve the current eager-first/lazy-second behavior from shared
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` without moving or
  changing that shared file: non-null `Track.artworkBytes` wins and the injected loader is not
  called; null eager bytes invoke the loader with the current track ID; a null loader result or
  ordinary non-cancellation failure produces the existing unavailable/fallback state;
  `CancellationException` is rethrown unchanged. Changes to track ID, eager bytes, or loader
  identity reset/reload state consistently with current behavior, and stale loader results cannot
  overwrite the current track. Production artwork state remains private. Exact JVM Compose rendering
  ownership belongs to `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingArtworkRenderingJvmTest.kt`,
  which proves bar and expanded eager/lazy/null/ordinary-failure rendering, exact cancellation
  instance through `currentCoroutineContext().job.invokeOnCompletion`, synchronous track/eager-byte/
  loader-identity resets, and stale-result rejection without widening state visibility. The shared
  facade/shell adapter remains the only owner of the current `TrackArtwork?` to bytes conversion.
- Core UI public declarations, all in `com.eterocell.rhythhaus.ui`, are exactly:

  ```kotlin
  /** Visual role used to keep artwork cache entries distinct by presentation size. */
  public enum class ArtworkImageRole(internal val keySuffix: String) {
      Thumbnail("thumbnail"),
      Card("card"),
      Hero("hero"),
  }

  /** Renders artwork bytes with the existing cache and fallback behavior. */
  @Composable
  public fun ArtworkImage(
      artworkBytes: ByteArray?,
      contentDescription: String,
      role: ArtworkImageRole,
      modifier: Modifier = Modifier,
      contentScale: ContentScale = ContentScale.Crop,
      fallback: @Composable () -> Unit,
  ): Unit

  /** Opaque handle for a supported Miuix backdrop; Miuix storage remains internal. */
  public class RhythHausBackdrop internal constructor(
      internal val layerBackdrop: LayerBackdrop,
  )

  /** Returns a backdrop handle, or null when render effects are unavailable. */
  @Composable
  public fun rememberRhythHausBackdrop(): RhythHausBackdrop?

  /** Records [backdrop] for later glass drawing and returns this modifier unchanged without one. */
  public fun Modifier.recordRhythHausBackdrop(
      backdrop: RhythHausBackdrop?,
  ): Modifier

  /** Draws glass from [backdrop] or the fallback surface while preserving existing visual values. */
  public fun Modifier.rhythHausLiquidGlass(
      backdrop: RhythHausBackdrop?,
      shape: Shape,
      fallbackColor: Color,
      blurRadius: Dp = 10.dp,
      refractionHeight: Dp = 16.dp,
      refractionAmount: Dp = 24.dp,
  ): Modifier

  /** Invokes [onBack] after the preserved generic left-edge swipe threshold. */
  public fun Modifier.leftEdgeSwipeBack(onBack: () -> Unit): Modifier

  /** Restricts the stateless drag mechanism to one locally owned terminal direction. */
  public enum class VerticalSheetGestureDirection { Upward, Downward }

  /** Mutates [expandProgress] with preserved drag physics and invokes [onTerminal] only at [direction]'s terminal state. */
  public fun Modifier.verticalSheetGesture(
      expandProgress: Animatable<Float, AnimationVector1D>,
      isActive: Boolean,
      scope: CoroutineScope,
      direction: VerticalSheetGestureDirection,
      onTerminal: () -> Unit,
      threshold: Float = 0.7f,
      referenceHeight: Float? = null,
  ): Modifier

  /** Alpha applied to the public glass fallback surface. */
  public const val RhythHausGlassSurfaceAlpha: Float = 0.72f
  ```

  `RhythHausBackdrop?` explicitly represents unavailable render effects. The internal constructor
  and `layerBackdrop` property may use Miuix storage while no public signature exposes
  `LayerBackdrop`. `rememberRhythHausBackdrop()` wraps `rememberLayerBackdrop()`; public modifier
  APIs unwrap `layerBackdrop` internally. Change `LibraryHomeContent`, `LibraryChrome`,
  `LibraryDetailContent`, `LibraryAppShell`, and `NowPlayingBar` caller signatures from
  `LayerBackdrop?` to `RhythHausBackdrop?`. Miuix `LayerBackdrop` is internal storage and absent
  from every public or cross-module signature. `RhythHausGlassBlurRadius`,
  `RhythHausGlassRefractionHeight`, and `RhythHausGlassRefractionAmount` are `internal`; the
  public defaults are their current `10.dp`, `16.dp`, and `24.dp` values, while the internal
  constants preserve implementation-only naming. No other visual constant is public.
  The feature-local adapter calls `verticalSheetGesture` with `Upward`, `threshold = 0.3f`, and
  `onTerminal = onExpand`; the shared-local overlay adapter calls it with `Downward`, its existing
  `0.7f` threshold, and `onTerminal = nowPlayingSwipeCollapseAction(onBack)`. `Upward` invokes
  its terminal callback after progress reaches or exceeds `threshold`; `Downward` invokes its
  terminal callback after progress falls below `threshold`. The mechanism springs to the opposite
  endpoint without invoking a callback, so no expansion callback reaches shared and no
  collapse/Back callback reaches the feature.

**Resource ownership:**

- Move these exact 17 EN/ZH keys from
  `shared/src/commonMain/composeResources/values/strings.xml` and
  `shared/src/commonMain/composeResources/values-zh/strings.xml` to
  `feature/nowplaying/src/commonMain/composeResources/values/strings.xml` and
  `feature/nowplaying/src/commonMain/composeResources/values-zh/strings.xml`:
  `mini_player_empty_subtitle`, `next_track`, `previous_track`,
  `playback_status_buffering`, `playback_status_error`, `playback_status_loading`,
  `playback_status_paused`, `playback_status_playing`, `playback_status_ready`,
  `playback_status_stopped`, `repeat_mode_repeat_one`, `repeat_mode_repeat_playlist`,
  `repeat_mode_stop_after_current`, `repeat_mode_stop_after_queue`, `shuffle_off`, `shuffle_on`,
  `track_number_format`.
- Retain shared `album_artwork` because Library UI uses it, and retain shared
  `track_artist_album_format`; shared formats the current track artist/album value and injects
  the resulting String. Retain every other shared key still used by Search, Settings, or Library;
  never remove shared copies required by those consumers and never duplicate shared-owned keys in
  feature resources.
- After registering the module and resource namespace, discover the generated resource tasks with
  `./gradlew :feature:nowplaying:tasks --all --configuration-cache` and run the discovered
  feature resource generation, Android packaging, JVM resource processing, and iOS resource/link
  tasks. Record exact discovered task names and outputs; do not claim a task existed before module
  registration.

**Complete Task 5.1 path inventory:**

- Modify `settings.gradle.kts`, `gradle/libs.versions.toml`, `shared/build.gradle.kts`,
  `core/ui/build.gradle.kts`, `feature/nowplaying/build.gradle.kts`,
  `build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts`,
  `build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt`,
  `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt`,
  and `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt`.
- Move or modify feature sources under
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/` and
  `feature/nowplaying/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`,
  plus the exact resource files
  `feature/nowplaying/src/commonMain/composeResources/values/strings.xml` and
  `feature/nowplaying/src/commonMain/composeResources/values-zh/strings.xml`. Create
  `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/nowplaying/BottomBarModeTest.kt`,
  `NowPlayingAdaptiveLayoutTest.kt`, `NowPlayingContractsTest.kt`,
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingArtworkRenderingJvmTest.kt`,
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt`,
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBarSemanticsJvmTest.kt`,
  and `feature/nowplaying/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`.
- Modify `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`,
  `shared/src/commonMain/composeResources/values/strings.xml`, and
  `shared/src/commonMain/composeResources/values-zh/strings.xml`. Modify mixed tests
  `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt`,
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt`,
  `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`,
  and `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`.
  `Task3ReviewSemanticsJvmTest.kt` retains shell-policy assertions for unmeasured, stale-measured,
  and matching `LibraryBottomBarMeasurement` mappings to expected `isInteractive`; only rendering
  assertions move or are recreated in the feature. The path map contains `shared/build.gradle.kts`
  exactly once.
  Keep `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/TrackArtworkImage.kt` unchanged and
  shared-owned; adapt `{ trackId -> LocalTrackArtworkLoader.current(trackId)?.bytes }` at the
  shared facade/shell call site. `TrackArtworkImage.kt` remains for other consumers.
- Remove the source origins `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt`,
  `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt`,
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt`, and
  `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt`; their
  destinations are the feature/core-ui paths listed above. Do not move `NowPlayingArtworkBridge.kt`.
- Core-ui destination files are `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkImage.kt`,
  `SwipeBackGesture.kt`, `LiquidGlassChrome.kt`, and `VerticalSheetGesture.kt`, plus the exact
  regression test `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt`.

**TDD and selective test inventory:**

- [ ] Before creating the module, run
  `./gradlew :feature:nowplaying:allTests --configuration-cache`; the planned diagnostic is
  `Project with path ':feature:nowplaying' could not be found in project ':'`. For the recorded
  Gradle 9.6.1 run, controller acceptance permits semantically equivalent task-selection wording
  only when task selection fails because `:feature:nowplaying` is absent and no requested feature
  task or compilation executes; do not claim the planned exact string appeared.
- [ ] Add the `compose-animation` catalog alias and extend
  `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt`
  with a feature-implementation KSP-arguments/registration case before convention implementation
  where the fixture can execute independently. Expected RED is the absent feature-implementation
  convention behavior, not an absent project path. Extend
  `ArchitectureCheckPluginFunctionalTest.kt` with the feature production-root positive policy and
  controlled forbidden mutations before production relocation; expected RED is the missing policy
  or registration. Stop on unrelated failures.
- [ ] Register an empty `:feature:nowplaying` in `settings.gradle.kts`, create its build file,
  exact targets, conventions, dependencies, Android/resource namespaces, common/jvm/androidHost/
  ios test source sets, and positive architecture policy without feature production implementation.
  The module applies `build-logic.kmp.feature.impl` but has no feature `iosMain` source or
  framework export. Expected convention RED remains independently executable through functional
  fixtures before production KSP roots exist; stop on unrelated failures.
- [ ] Move or recreate only these tests under feature ownership:
  `emptyLibraryStillUsesBottomBarNavigationMode`,
  `unmeasuredNowPlayingBarExposesNoActions`,
  `staleMeasuredNowPlayingBarExposesNoActionsAndDispatchesNoPointerOrGestureCallbacks`,
  `matchingMeasuredNowPlayingBarRestoresExpectedActions`, the five Now Playing adaptive tests
  currently in `LibraryNavigationTest`:
  `nowPlayingAdaptiveLayoutUsesCompactForPhonePortrait`,
  `nowPlayingAdaptiveLayoutUsesCompactForNarrowPortraitTablet`,
  `nowPlayingAdaptiveLayoutUsesSplitForWideTablet`,
  `nowPlayingAdaptiveLayoutUsesSplitForLandscapeMediumWidth`, and
  `nowPlayingAdaptiveLayoutUsesSplitForDesktopWidth`; and `MusicProgressScrubberTest`. Retain
  shared Library route/Back/shell tests and all non-Now-Playing mixed tests in shared. Expected
  RED is missing feature/core declarations or implementation, never a missing project path; stop
  for unrelated failures.
- [ ] Split `Task3ReviewSemanticsJvmTest.kt` instead of moving its shared policy types literally:
  `feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBarSemanticsJvmTest.kt`
  passes `interactive = false` and `interactive = true` directly and verifies only feature
  rendering, absence/presence of semantics/actions, and callback dispatch/non-dispatch. Shared
  `Task3ReviewSemanticsJvmTest.kt` retains or adds assertions that unmeasured, stale-measured, and
  matching `LibraryBottomBarMeasurement` values map to expected `isInteractive`, plus playlist,
  selection, and every other shared shell-policy case. No feature test imports
  `libraryBottomBarPresentation`, `LibraryBottomBarMeasurement`, or another shared policy
  declaration. Expected RED is missing feature rendering declarations for the feature test and
  missing core/shared policy fixtures for the shared test; do not proceed on unrelated failures.
- [ ] Add focused characterization tests for the public feature contracts, exact label values,
  loaded and empty label mapping, inert empty label-field ignorance, artwork eager/lazy/error/
  cancellation/reset/stale-result behavior, progress/status/repeat/shuffle presentation, left-edge
  callback identity, upward gesture callback identity, and no interaction when unmeasured or
  stale. Keep private artwork state unobservable except through actual rendering:
  `NowPlayingArtworkRenderingJvmTest.kt` owns eager/lazy/null/ordinary-failure artwork and fallback
  rendering for both bar and expanded content, exact cancellation-instance observation through
  `currentCoroutineContext().job.invokeOnCompletion`, track/eager-byte/loader-identity synchronous
  resets, and stale-result rejection. `NowPlayingContentSemanticsJvmTest.kt` owns compact/split
  branches, stable tag/count identity, all transport/mode callbacks, left-edge Back, bounded
  progress, and metadata/status. `NowPlayingBarSemanticsJvmTest.kt` retains its existing ownership.
  Do not add presenter, ViewModel, or empty-pattern tests. Run the discovered focused
  feature JVM/Android-host/iOS test tasks; expected RED identifies the missing moved APIs or
  implementation, not an unrelated regression.
- [ ] Before changing the core gesture production signature, characterize current math in the JVM
  Compose UI integration test
  `core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt`.
  Use core UI's existing JVM Compose UI-test harness and dependencies, deterministic pointer input
  sequences against the actual public `Modifier.verticalSheetGesture` path, and deterministic
  animation-clock/idle advancement from that harness. Cover upward terminal callback, downward
  terminal callback, opposite-endpoint spring with no terminal callback, pointer cancellation,
  inactive behavior, exact threshold boundary for each direction, and reference-height behavior.
  Keep domain/navigation types and callback meanings out of core tests. Do not introduce a pure
  gesture policy/math seam or coroutine-test dependency; if the existing harness cannot execute
  this integration test, stop with that exact blocker. Expected RED is the missing moved/public
  core-ui gesture API before production extraction; GREEN is this JVM integration test passing
  after extraction with unchanged drag/rubber-band/spring/threshold math; the separately approved
  direction-specific cancellation contract is characterized by the same suite.
- [ ] Add architecture RED fixtures and mutations before production relocation: absent module
  registration; feature-to-`:shared`, `:taglib`, Library API, Library implementation, app, and
  feature-implementation edges; feature iOS export; missing namespace/resource ownership; and
  missing package roots. The positive fixture must require actual production KSP package roots,
  not only a synthetic graph. Add the expected positive package/resource requirements or
  controlled forbidden mutations so fail-closed tests never pass by making production KSP roots
  empty. Supply the external processor JAR property where the convention fixture requires it and
  force fixture execution with `--rerun-tasks`; do not accept UP-TO-DATE as fixture evidence.
- [ ] Implement the general feature-implementation KSP convention and architecture fixtures, then
  add the exact positive `:shared -> :feature:nowplaying` and
  `:feature:nowplaying -> :core:playback/:core:ui` entries to `ArchitectureAllowList` and the
  functional fixture/module inventory. Permit both feature package roots
  `com.eterocell.rhythhaus.nowplaying` and `com.eterocell.rhythhaus.ui` for the moved scrubber,
  exact resource namespace ownership, and exact Android namespace. Keep all negative mutations
  failing with the expected architecture diagnostics. Update
  `build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts` by reusing the
  core/API lifecycle: apply KSP when `:architecture-processor` exists, pass
  `architecture.module`, `architecture.packageRoots`, and `architecture.sourceRoots`, add the
  processor to every non-metadata main-target KSP configuration, and publish
  `ArchitectureModelRegistry` registrations. Do not call `explicitApi()` in this convention.
  Extend `KmpConventionPluginsFunctionalTest.kt` to prove production KSP arguments and
  registrations and preserve `EXPLICIT_API=null`. Extend
  `ArchitectureCheckPluginFunctionalTest.kt` with real feature-implementation production roots,
  positive module/resource policy, forbidden mutations, external processor JAR input, and forced
  execution. This is general governance for current and future feature implementation modules,
  not a Now Playing workaround.
- [ ] Move the minimum core-ui, feature, and shared production slice and rerun the exact focused
  tests; expected GREEN covers
  the listed moved tests, public contract characterization, artwork seam, presentation, callback
  identity, and unmeasured/stale interaction behavior. Do not proceed when a GREEN result depends
  on weakening an assertion or adding a forbidden dependency.

**Gesture and composition implementation:**

- [ ] Keep shared as owner of the `Animatable`, expansion progress state, shell measurement and
  visibility, overlay, collapse callback/Back, route dispatch, and navigation effects. The
  feature receives the shared-owned progress object only for display and mutation by its
  feature-local upward-only `NowPlayingBar` adapter, which emits only `onExpand`; it cannot invoke
  collapse, Back, or navigation. The shared-local downward-only overlay adapter emits only the
  existing shared collapse/Back callback and cannot route expansion.
- [ ] Move the generic vertical drag/rubber-band/spring/threshold mechanism to core UI without
  changing exact math, thresholds, or spring stiffness. The approved cancellation contract is
  direction-specific: terminal-side cancellation may emit the direction-owned terminal callback
  and settle terminal; nonterminal-side cancellation settles opposite with no callback, as covered
  by the accepted 11-case suite. The core entry point receives direction and terminal callback from
  the local adapters and has no domain or navigation API. Preserve generic caller-owned
  `leftEdgeSwipeBack` behavior.
- [ ] Expose only the opaque `RhythHausBackdrop?` core-ui handle through
  `rememberRhythHausBackdrop`, `recordRhythHausBackdrop`, and `rhythHausLiquidGlass`; preserve
  existing visual behavior and keep Miuix storage and blur/refraction constants internal except
  explicit-public `RhythHausGlassSurfaceAlpha`.

**Verification and evidence boundary:**

- [ ] After task discovery and module registration, run focused feature JVM, Android host, and
  iOS simulator tests and compilations; shared JVM tests; desktop compile; Android assemble; shared
  iOS compile; simulator/device framework links; core-ui consumer tests/compilations; discovered
  resource generation/packaging tasks; and the Xcode Swift-consumer build. Use
  `./gradlew :feature:nowplaying:jvmTest :feature:nowplaying:testAndroidHostTest
  :feature:nowplaying:iosSimulatorArm64Test :shared:jvmTest :desktopApp:compileKotlin
  :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64
  :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64
  --configuration-cache`. Retained evidence supports successful real feature task execution and
  compilation, including reconciliation to `compileAndroidMain`; it does not claim the literal
  `:feature:nowplaying:tasks :shared:tasks :core:ui:tasks --all` discovery command.
  Apply `--tests '*NowPlayingArtworkRenderingJvmTest'`,
  `--tests '*NowPlayingContentSemanticsJvmTest'`, and
  `--tests '*NowPlayingBarSemanticsJvmTest'` to the discovered feature JVM test task, and apply
  `--tests '*VerticalSheetGestureJvmTest'` to the discovered core-ui JVM test task. Do not claim
  this pointer integration test executes in common, Android-host, or iOS simulator test tasks;
  Android/iOS consumer compilation verifies that the public mechanism compiles cross-target. Run
  the discovered feature common/Android-host/iOS simulator tests separately, then run
  `/usr/bin/xcrun xcodebuild -version`, then `/usr/bin/xcrun xcodebuild -project
  iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination
  'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`; record an unavailable or
  failing Xcode command as its exact blocker rather than passing it.
- [ ] Run the architecture functional test with a rebuilt processor JAR and real architecture
  check using the retained commands: `./gradlew :architecture-processor:jar --rerun-tasks
  --no-configuration-cache`; then `./gradlew :build-logic:convention:cleanTest
  :build-logic:convention:test --tests '*ArchitectureCheckPluginFunctionalTest' --rerun-tasks
  --no-configuration-cache
  -Prhythhaus.architectureProcessorJar="$PWD/architecture-processor/build/libs/architecture-processor.jar"`;
  then run
  `./gradlew architectureCheck --configuration-cache --configuration-cache-problems=fail
  --no-parallel` twice and require strict configuration-cache reuse on the second run. The
  architecture processor JAR property is mandatory fixture input where required.
- [ ] Run `./gradlew spotlessApply --configuration-cache`, then separate
  `./gradlew spotlessCheck --configuration-cache` and `./gradlew detekt --configuration-cache`.
  Run `openspec validate feature-first-modularization --strict` and
  `git diff --check -- gradle/libs.versions.toml settings.gradle.kts
  feature/nowplaying/build.gradle.kts feature/nowplaying/src
  core/ui/build.gradle.kts core/ui/src
  shared/build.gradle.kts
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt
  shared/src/commonMain/composeResources/values/strings.xml
  shared/src/commonMain/composeResources/values-zh/strings.xml
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/BottomBarModeTest.kt
  shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/Task3ReviewSemanticsJvmTest.kt
  shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt
  feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingArtworkRenderingJvmTest.kt
  feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingContentSemanticsJvmTest.kt
  feature/nowplaying/src/jvmTest/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBarSemanticsJvmTest.kt
  core/ui/src/jvmTest/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGestureJvmTest.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubber.kt
  shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/MusicProgressScrubberTest.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/SwipeBackGesture.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/LiquidGlassChrome.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/VerticalSheetGesture.kt
  shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt
  build-logic/convention/src/main/kotlin/com/eterocell/gradle/architecture/ArchitectureAllowList.kt
  build-logic/convention/src/main/kotlin/build-logic.kmp.feature.impl.gradle.kts
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/KmpConventionPluginsFunctionalTest.kt
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ArchitectureCheckPluginFunctionalTest.kt
  docs/superpowers/plans/2026-07-27-feature-first-modularization.md`; record exact outputs in the
  evidence ledger. Compare the output against this inventory and block acceptance on any
  unexpected path; the final staging audit repeats the exact path set before the one atomic
  implementation commit.
- [ ] Do not claim `./init.sh`, desktop runtime launch, iOS device/runtime UI, or runtime
  playback validation unless explicitly run. Compile, link, packaging, and Swift-consumer
  results remain non-runtime evidence. The controller owns independent review, staging, ledger
  updates, and one atomic implementation commit after acceptance, followed by a separate
  conventional `docs:` closeout commit only when the established ignored-evidence durability
  pattern requires it.

**Plan self-review before handoff:**

- [ ] Confirm every approved design constraint and every listed source, test, package, resource,
  dependency, API, gesture, bridge, and evidence boundary is covered without adding scope. Record
  inspected-but-unchanged approved paths separately from actual changed/staged paths:
  `LibraryDetailContent.kt` already uses the core-ui backdrop API and `ArtworkImage.kt` required no
  Task 5.1 edit; neither belongs in the 38-path implementation commit.
- [ ] Scan the replacement section for placeholders, unresolved conditional dependency language,
  stale `NowPlayingArtworkBridge` ownership, or feature `iosMain` claims.
- [ ] Check type/name consistency for label objects, artwork loader, backdrop nullability,
  direction type, gesture entry points, namespaces, and resource package.
- [ ] Check test ownership against the exact selective inventory and confirm no empty presenter or
  pattern-class tests were prescribed.
- [ ] Record verification command existence and exact results from successful real feature task
  execution and compilation rather than assuming generated task names. The retained Task 5.1
  evidence reconciles `compileAndroidMain`; it does not claim the literal
  `:feature:nowplaying:tasks --all --configuration-cache` command ran.
