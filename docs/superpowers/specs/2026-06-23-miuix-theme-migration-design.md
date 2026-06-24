# Migrate Compose MaterialTheme to Miuix Theme

**Status:** approved  
**Date:** 2026-06-23  
**Route:** openspec+superpowers → brainstorming → writing-plans

## Goal

Replace `MaterialTheme` with `MiuixTheme` from the `compose-miuix-ui/miuix` library. Swap Material3 components for their Miuix equivalents in `App.kt`. The app's visual identity remains consistent — we map existing custom colors into Miuix's color scheme.

## Scope

- **In:** `shared/build.gradle.kts`, `gradle/libs.versions.toml`, `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- **Out:** No structural refactors, no new features, no theme toggle UI, no dark mode
- **Module:** `miuix-ui` only (core theme + components)

## Architecture

Miuix wraps Compose similarly to Material3: `MiuixTheme` is the theme container, and components like `Card`, `Button`, `Slider`, `Surface` have Miuix equivalents with the same or similar API surfaces. The `MiuixTheme` takes a `ColorScheme` object matching `lightColorScheme()` or `darkColorScheme()`.

## Component Mapping

| Material3 Import            | Miuix Replacement          | Notes                                  |
| --------------------------- | -------------------------- | -------------------------------------- |
| `MaterialTheme`             | `MiuixTheme`               | Theme container                        |
| `Surface`                   | Miuix `Surface`            | Same composable name, different import |
| `Card` / `CardDefaults`     | Miuix `Card`               | API may differ — adapt inline          |
| `Button` / `ButtonDefaults` | Miuix `Button`             | API may differ — adapt inline          |
| `Slider`                    | Miuix `Slider`             | Same composable name, different import |

## Color Mapping

Current custom colors → Miuix `colorScheme()`:

| App Constant    | Miuix Role      | Hex        |
| --------------- | --------------- | ---------- |
| `HausPaper`     | `background`    | `0xFFFFFAF1` |
| `HausPanel`     | `surface`       | `0xFFF5EBDD` |
| `HausInk`       | `primary`       | `0xFF111018` |
| `HausPulse`     | `secondary`     | `0xFFFF5E3A` |

## Verification

`./init.sh` must pass: shared JVM tests, shared iOS simulator tests, desktop compile, Android debug build.

## Dependencies

```kotlin
// NEW
implementation("top.yukonga.miuix.kmp:miuix-ui:<latest>")

// KEPT (for now — remove after verification)
implementation(libs.compose.material3)
```

Miuix targets Kotlin 2.4.0 and Compose Multiplatform 1.11.1 — matches RhythHaus current versions.
