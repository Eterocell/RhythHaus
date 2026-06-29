# Square Artwork and Swipe Back Design

## Summary

RhythHaus should make shared Compose album artwork consistently square and add a shared Compose swipe-to-back gesture for detail-style screens. This is a UI/UX polish change only: it does not change playback, metadata, persistence, imports, platform wrappers, or native iOS SwiftUI navigation.

## Goals

- Render rectangular album artwork surfaces as square in shared Compose UI.
- Preserve existing compact artwork shapes that are already square or intentionally circular.
- Add a rightward left-edge swipe gesture that calls the same back behavior as existing back buttons.
- Keep the implementation shared Compose-only so it works in the current Android, macOS/desktop, and hosted iOS Compose UI.
- Avoid new dependencies and avoid Material/Material3 migration work.

## Non-Goals

- Do not migrate iOS navigation to SwiftUI or add native iOS edge-swipe navigation.
- Do not redesign the album grid, artist rows, now-playing bar, or full player layout beyond artwork aspect ratio and swipe-back affordance.
- Do not change artwork decoding, caching, metadata extraction, or database fields.
- Do not change playback queues, selected-track behavior, or platform media controls.

## Current Code Context

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` owns the album grid, drill-down views, an older inline now-playing card, and shared navigation state.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt` owns the compact floating now-playing bar; its thumbnail is already square.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt` owns the full-screen player artwork, currently a fixed-height rectangle.
- `iosApp/iosApp/ContentView.swift` only hosts `MainViewControllerKt.MainViewController()`, so native SwiftUI edge-swipe navigation would not affect the current shared Compose navigation stack.

## Design

### Square Artwork

Artwork surfaces that currently use width plus fixed height should use a 1:1 aspect ratio instead:

- Album cards: replace the rectangular `fillMaxWidth().height(120.dp)` artwork area with `fillMaxWidth().aspectRatio(1f)`.
- Full Now Playing screen: replace the rectangular `fillMaxWidth().height(300.dp)` artwork area with `fillMaxWidth().aspectRatio(1f)`.
- Older inline now-playing card in `App.kt`, if still reachable: replace its rectangular `fillMaxWidth().height(220.dp)` artwork image with a square 1:1 surface.

Existing compact shapes remain unchanged:

- `NowPlayingBar` thumbnail remains a 40 dp rounded square.
- `AlbumMark` in track rows remains a 54 dp rounded square.
- `ArtistRow` artwork remains circular because it represents an artist avatar-style affordance rather than album art.

All artwork images should continue using `ContentScale.Crop` so embedded artwork fills the square without letterboxing. Fallback gradient/text placeholders should use the same square container as decoded artwork.

### Swipe To Back

Add a small shared Compose modifier/helper for left-edge rightward drag detection. The helper should:

- attach with `pointerInput` to screen-level containers only;
- only begin considering gestures from a narrow left-edge region;
- require a meaningful rightward drag threshold before firing;
- ignore leftward drags and small accidental horizontal movement;
- call the same `onBack` callback used by visible back buttons;
- avoid interfering with ordinary vertical scrolling in `LazyColumn` or `verticalScroll` content.

Apply it to:

- `DrillDownView`, which covers album and artist detail screens;
- `NowPlayingScreen`, which covers the full-screen player.

The visible back buttons remain available and unchanged for accessibility and discoverability.

## Testing and Verification

Focused verification should include:

- `./gradlew :shared:compileKotlinJvm --configuration-cache` to catch Compose import/API errors.
- A diff review confirming no SwiftUI wrapper files were changed for navigation.
- Manual UI check on at least one Compose target when convenient:
  - album card artwork appears square;
  - full Now Playing artwork appears square;
  - swiping right from the left edge on album/artist detail returns to the library;
  - swiping right from the left edge on full Now Playing returns to the previous screen;
  - vertical scrolling remains usable on track lists and the full player.

## Risks

- Compose pointer gesture tuning can conflict with scrollables if attached too deeply or if thresholds are too aggressive. The implementation should attach at screen-level containers and use conservative left-edge and distance thresholds.
- Square album cards increase vertical space in the album grid compared with the current 120 dp rectangle. This is accepted because the user requested square artwork.
- Existing local uncommitted edits in `App.kt` mean implementation must patch carefully and avoid overwriting unrelated UI changes.
