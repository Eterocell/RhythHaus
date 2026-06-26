# Apply RhythHaus App Icons Across Platforms

**Status:** approved  
**Date:** 2026-06-26  
**Route:** openspec+superpowers → brainstorming → writing-plans

## Goal

Replace default placeholder app icons on all three platforms with the RhythHaus brand icons from `icons/`.

## Source assets

| File | Content | Use |
|------|---------|-----|
| `icons/dark_mode.svg` | 1200×1200, dark background (#111827), house + waveform, purple→pink→orange gradient | Primary icon on all platforms |
| `icons/light_mode.svg` | 1200×1200, light background, house + waveform | iOS dark appearance variant |
| `icons/ic_rhythhaus_monochrome.xml` | Android vector drawable, single-path outline | Android adaptive icon foreground + monochrome themed icon |

## Platform designs

### Android

- Replace `drawable-v24/ic_launcher_foreground.xml` — point to `@drawable/ic_rhythhaus_monochrome` or replace the file content
- Add `<monochrome android:drawable="@drawable/ic_rhythhaus_monochrome"/>` to `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` for Android 13+ themed icons
- Keep `drawable/ic_launcher_background.xml` as-is (dark gradient matches the icon)
- Regenerate mipmap raster PNGs from `dark_mode.svg` for API < 26 fallback:
  - mdpi: 48×48, hdpi: 72×72, xhdpi: 96×96, xxhdpi: 144×144, xxxhdpi: 192×192
  - Same for round variant with circular mask

### iOS

- Convert `dark_mode.svg` → `app-icon-dark.png` (1024×1024) — primary universal icon
- Convert `light_mode.svg` → `app-icon-light.png` (1024×1024) — dark appearance icon
- Update `Assets.xcassets/AppIcon.appiconset/Contents.json`:
  - Entry 1 (primary): filename = `app-icon-dark.png`
  - Entry 2 (dark): filename = `app-icon-light.png`
  - Entry 3 (tinted): keep unset or reference `app-icon-dark.png`
- Xcode auto-generates all required sizes from the 1024px source

### macOS (desktop JVM)

- Generate `.icns` from `dark_mode.svg`:
  - Render PNGs at 16, 32, 64, 128, 256, 512, 1024 px (1x and 2x where applicable)
  - Package into `RhythHaus.icns` via `iconutil`
- Place `RhythHaus.icns` in `desktopApp/src/main/resources/`
- Reference in `desktopApp/build.gradle.kts` if not auto-resolved from resources

## Tools

- SVG → PNG: `rsvg-convert` (librsvg) or Python `cairosvg`
- PNG → ICNS: `mkdir tmp.iconset && sips -z ... && iconutil -c icns tmp.iconset`

## Files changed

| File | Action |
|------|--------|
| `androidApp/src/main/res/drawable-v24/ic_launcher_foreground.xml` | Replace with monochrome reference |
| `androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Add monochrome layer |
| `androidApp/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Add monochrome layer |
| `androidApp/src/main/res/mipmap-*/ic_launcher*.png` | Regenerate from SVG |
| `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-*.png` | Replace with dark/light PNGs |
| `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json` | Update filenames |
| `desktopApp/src/main/resources/RhythHaus.icns` | New file |

## Verification

```bash
./gradlew :androidApp:assembleDebug --configuration-cache
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
./gradlew :desktopApp:compileKotlin --configuration-cache
```

All must pass; verify icons appear in built outputs.
