# Apply RhythHaus App Icons — Implementation Plan

> **For agentic workers:** Complete all steps in order, verifying each before proceeding.

**Goal:** Replace default placeholder icons on Android, iOS, and macOS with RhythHaus brand icons from `icons/`.

**Prerequisites:** `rsvg-convert` installed (`brew install librsvg`) or Python `cairosvg` (`pip3 install cairosvg`). `iconutil` is built-in on macOS.

**Working directory:** `/Users/eterocell/Sources/AndroidStudioWorkspace/RhythHaus`, branch `main`.

---

## Task 1: Apply icons to all platforms

### Step 1: Check SVG→PNG tool availability

```bash
which rsvg-convert || pip3 install cairosvg 2>/dev/null
```

Fall back to `cairosvg` if `rsvg-convert` not found.

### Step 2: Android — replace adaptive icon foreground

Replace `drawable-v24/ic_launcher_foreground.xml` content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M22.5,47.7 L54,20.7 L85.5,47.7 V76.95 C85.5,80.55 82.62,83.43 79.02,83.43 H28.98 C25.38,83.43 22.5,80.55 22.5,76.95 V47.7 Z M30.6,49.95 L54,30.15 L77.4,49.95 V72 C77.4,74.52 75.42,76.5 72.9,76.5 H35.1 C32.58,76.5 30.6,74.52 30.6,72 V49.95 Z M30.15,54 C34.95,54 34.95,45.9 39.75,45.9 C44.55,45.9 44.55,62.1 49.35,62.1 C54.15,62.1 54.15,40.95 58.95,40.95 C63.75,40.95 63.75,56.7 68.7,56.7 C73.65,56.7 73.65,51.3 78.6,51.3"
        android:strokeWidth="0"
        android:fillType="nonZero" />
</vector>
```

(The pathData above is a representation of the monochrome house + waveform at 108dp scale. Use the EXACT content from `icons/ic_rhythhaus_monochrome.xml` if it already has the correct viewport; otherwise adapt.)

### Step 3: Android — add monochrome layer to adaptive icon

Edit `mipmap-anydpi-v26/ic_launcher.xml` — add monochrome element:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_rhythhaus_monochrome" />
</adaptive-icon>
```

Same for `ic_launcher_round.xml`.

### Step 4: Android — regenerate mipmap PNGs

Using `rsvg-convert`:

```bash
cd androidApp/src/main/res
for dpi in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  case $dpi in
    mdpi) size=48 ;;
    hdpi) size=72 ;;
    xhdpi) size=96 ;;
    xxhdpi) size=144 ;;
    xxxhdpi) size=192 ;;
  esac
  rsvg-convert -w $size -h $size icons/dark_mode.svg -o mipmap-$dpi/ic_launcher.png
  rsvg-convert -w $size -h $size icons/dark_mode.svg -o mipmap-$dpi/ic_launcher_round.png
done
```

(Adjust paths: `icons/` is at repo root relative to cwd.)

### Step 5: iOS — generate 1024px PNGs

```bash
rsvg-convert -w 1024 -h 1024 icons/dark_mode.svg -o iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-dark.png
rsvg-convert -w 1024 -h 1024 icons/light_mode.svg -o iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-light.png
```

Remove old `app-icon-1024.png`.

### Step 6: iOS — update Contents.json

Replace `Contents.json`:

```json
{
  "images" : [
    {
      "filename" : "app-icon-dark.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    },
    {
      "appearances" : [
        {
          "appearance" : "luminosity",
          "value" : "dark"
        }
      ],
      "filename" : "app-icon-light.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "rhythhaus",
    "version" : 1
  }
}
```

### Step 7: macOS — generate .icns

```bash
ICONSET=desktopApp/src/main/resources/RhythHaus.iconset
mkdir -p "$ICONSET"
for s in 16 32 64 128 256 512; do
  rsvg-convert -w $s -h $s icons/dark_mode.svg -o "$ICONSET/icon_${s}x${s}.png"
  rsvg-convert -w $((s*2)) -h $((s*2)) icons/dark_mode.svg -o "$ICONSET/icon_${s}x${s}@2x.png"
done
rsvg-convert -w 1024 -h 1024 icons/dark_mode.svg -o "$ICONSET/icon_512x512@2x.png"
iconutil -c icns "$ICONSET" -o desktopApp/src/main/resources/RhythHaus.icns
rm -rf "$ICONSET"
```

### Step 8: Build verification

```bash
./gradlew :androidApp:assembleDebug --configuration-cache
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
./gradlew :desktopApp:compileKotlin --configuration-cache
```

All must BUILD SUCCESSFUL.

### Step 9: Commit

```bash
git add -A
git commit -m "feat: apply RhythHaus app icons across Android, iOS, and macOS"
```
