# Change: Coil Artwork Loading and Koin Dependency Injection

## Why

RhythHaus currently decodes artwork in Compose call sites through local `remember { ByteArray.decode... }` helpers and constructs core app services directly in `App()`. That keeps the app working, but it spreads image-loading policy and dependency construction across UI code. The project now needs Coil-backed image loading/caching for project images and embedded album art, plus Koin dependency injection so existing services can be wired consistently and tested/replaced without expanding `App()`.

## What Changes

- Add Coil 3 KMP dependencies and route practical shared Compose artwork surfaces through Coil-backed composables.
- Keep original `artworkBytes` in track/library/playback models so platform media metadata and full-size Now Playing artwork remain available.
- Use stable memory/disk cache keys derived from artwork bytes where Coil supports cache keys, so repeated album-art renders reuse Coil cache instead of ad hoc UI decode cache.
- Add Koin 4 dependencies and a shared app module for current core services: playback controller, library database/repository, platform source access, scanner, TagLib reader, metadata reader, and theme preference store.
- Initialize Koin from Android, desktop, and iOS entry points before rendering shared Compose UI.
- Refactor `App()` to consume dependencies from Koin instead of manually constructing the service graph inline.
- Preserve existing scanner, SQLDelight schema, playback behavior, navigation behavior, and current visual layout.

## Impact

- Shared Compose UI and dependency wiring across Android, desktop JVM/macOS, and iOS.
- Gradle dependency additions for Coil 3.5.0 and Koin 4.2.2.
- No SQLDelight schema migration.
- No scanner, TagLib/native extraction, or playback-engine behavior changes beyond construction moving into DI.
- Manual QA remains useful for confirming artwork cache behavior and visuals on device/simulator; automated verification covers OpenSpec validation, focused shared tests, JVM/desktop/Android builds, and iOS simulator tests.
