# Coil Artwork Loading and Koin Dependency Injection Design

## Summary

RhythHaus will add Coil 3 KMP as the shared Compose image-loading/cache layer for embedded album art and project images, and Koin 4 as the dependency container for the app service graph. The change keeps existing library models, scanner behavior, playback behavior, and visuals intact while moving image loading and dependency construction out of ad hoc UI code.

## Current state

Artwork rendering currently uses local decode helpers such as `decodeArtworkCached()` and `decodeArtworkThumbnailCached()` directly inside Compose call sites. `App()` constructs app services inline with `remember`, including `PlaybackController`, `AudioMetadataReader`, `createTagLibReader()`, `createLibraryDatabase()`, `SqlDelightLibraryRepository`, `createPlatformSourceAccess()`, `LibraryScanner`, and `createThemePreferenceStore()`.

This works, but it keeps cache policy and service construction scattered in UI code.

## Design

Add Coil 3.5.0 and Koin 4.2.2 through the version catalog. Create a small Coil-backed artwork composable/helper that accepts nullable artwork bytes, content description, content scale, a size role, and fallback content. It derives stable cache keys from artwork content plus role so repeated renders can reuse Coil cache. Route practical artwork surfaces through this helper: track rows, compact now-playing bar, album cards, artist rows, drill-down top bar, and expanded Now Playing artwork.

Add a shared Koin module that binds the existing service graph with singleton lifetimes matching the current `remember` behavior. Platform entry points initialize Koin idempotently before rendering `App()`. `App()` resolves services from Koin and keeps its UI state, scan flow, theme flow, and playback controller disposal semantics unchanged.

## Constraints

- No SQLDelight schema migration.
- No scanner, TagLib/native extraction, or playback-engine behavior changes.
- Preserve existing visual layout, fallback text, content descriptions, shapes, and click behavior.
- Preserve original `artworkBytes` for full-size display and platform media metadata.
- Scope is Android, iOS, and desktop JVM/macOS only.

## Verification

- `openspec validate coil-koin-image-di --strict`
- Focused shared tests for cache-key/helper or DI behavior where practical.
- `./gradlew :shared:compileKotlinJvm --configuration-cache`
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
- `/usr/bin/xcrun xcodebuild -version`
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
- `git diff --check`
