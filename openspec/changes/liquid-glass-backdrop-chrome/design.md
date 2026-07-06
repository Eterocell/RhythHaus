# Design: Liquid Glass Backdrop Chrome

## Overview

Add Kyant0 Backdrop as a shared Compose dependency and wrap it in a small RhythHaus-local glass helper. Record route content with `rememberLayerBackdrop` / `Modifier.layerBackdrop`, then render top chrome and bottom bar surfaces with `Modifier.drawBackdrop` using a consistent liquid-glass recipe.

## Dependency decision

Use the upstream Backdrop artifact from AndroidLiquidGlass:

- `io.github.kyant0:backdrop:2.0.0`
- `io.github.kyant0:shapes:1.2.0`

The wrapper keeps raw Backdrop APIs localized so future dependency/API changes affect a small file rather than every UI surface.

## Backdrop recording

`LibraryHomeScreen` and `DrillDownView` will create a `LayerBackdrop` for their main content. The `Surface`/route content layer behind overlays will apply `Modifier.layerBackdrop(backdrop)`. `NestedScrollBlurChrome` and `NowPlayingBar` will receive the same backdrop and draw glass above that recorded layer.

The recorder must not include the glass overlays themselves, otherwise the effect can self-sample or visually double-apply.

## Top chrome behavior

The existing `NestedScrollChromeState` and scroll progress thresholds remain unchanged. The top chrome continues to render only when progress is greater than zero and continues to use `requiredHeight(statusBarHeight + 56.dp)` to prevent full-screen stretching. The visual fill changes from a plain scrim to a Backdrop glass surface with a fallback translucent draw surface, while title fade and divider behavior stay intact.

## Bottom bar behavior

`NowPlayingBar` keeps its public behavior and shape. The existing Miuix `Surface` color is replaced by a Backdrop-backed rounded glass container. The progress bar, artwork thumbnail, title/subtitle, play/pause, Search, Settings, tap-to-expand, vertical sheet gesture, nav bar padding, and hide/show animation remain unchanged.

## Verification

Run focused compile/test checks after dependency and UI changes, then run the repository’s primary supported-platform verification subset:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

If a platform cannot validate Backdrop because of an upstream library limitation, record the exact compiler/runtime blocker and keep fallback UI readable.
