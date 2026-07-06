# Task 2 Report: Replace Kyant Backdrop Wrapper with Miuix Blur

## Files changed

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`
  - Replaced Kyant Backdrop imports with Miuix blur imports.
  - Kept `rememberRhythHausBackdrop()` and `recordRhythHausBackdrop()` wrapper APIs, now backed by Miuix `rememberLayerBackdrop()` / `layerBackdrop()`.
  - Updated `rhythHausLiquidGlass()` to use Miuix `drawBackdrop` with `blur(blurRadius.toPx())` and fallback tint drawing via `drawRect(fallbackColor)`.
  - Preserved `refractionHeight` and `refractionAmount` parameters for call-site compatibility; they are intentionally unused by Miuix blur.

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
  - Replaced `com.kyant.backdrop.backdrops.LayerBackdrop` import with `top.yukonga.miuix.kmp.blur.LayerBackdrop`.

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
  - Replaced `com.kyant.backdrop.backdrops.LayerBackdrop` import with `top.yukonga.miuix.kmp.blur.LayerBackdrop`.

- `shared/build.gradle.kts`
  - Removed `implementation(libs.kyant.backdrop)` and `implementation(libs.kyant.shapes)`.

- `gradle/libs.versions.toml`
  - Removed Kyant version aliases and library aliases: `backdrop`, `kyant-shapes`, `kyant-backdrop`, `kyant-shapes`.

## Verification

Commands run:

```bash
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 14s
25 actionable tasks: 15 executed, 10 up-to-date
Configuration cache entry stored.
```

Command run:

```bash
grep -R "com.kyant.backdrop\|kyant-backdrop\|kyant-shapes" -n gradle shared/src || true
```

Result: no output; no source/catalog Kyant references remain under `gradle` or `shared/src`.

## Notes

- Pre-existing untracked coordinator/spec/plan/OpenSpec docs were present and left untouched.
- No adaptive layout UI changes were made.
