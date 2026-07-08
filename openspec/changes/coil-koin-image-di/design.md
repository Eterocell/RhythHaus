# Design: Coil Artwork Loading and Koin Dependency Injection

## Goals

1. Use Coil as the shared Compose image-loading/cache layer for RhythHaus project images and embedded album art where practical.
2. Keep artwork data model compatibility: `LibraryTrack.artworkBytes`, `Track.artworkBytes`, and platform media metadata paths remain unchanged.
3. Introduce Koin as the app dependency container and remove direct service graph construction from `App()`.
4. Keep the first pass schema-free and behavior-preserving outside image loading and dependency construction.

## Non-goals

- No SQLDelight migration or persisted thumbnail metadata table.
- No scanner/TagLib/native metadata extraction changes.
- No playback engine behavior changes.
- No visual redesign of rows, album cards, artist rows, top bars, now-playing bar, or now-playing screen.
- No Windows/Linux product or packaging work.

## Current context

The previous `track-art-thumbnails` change added `ArtworkCache`, `decodeArtworkCached()`, and `decodeArtworkThumbnailCached()`. Those helpers improved in-memory decode reuse, but they remain RhythHaus-specific and require UI call sites to synchronously decode `ByteArray` artwork into `ImageBitmap` during composition.

`App()` currently constructs services inline:

- `PlaybackController()`
- `AudioMetadataReader()`
- `createTagLibReader()`
- `createLibraryDatabase()`
- `SqlDelightLibraryRepository(libraryDb)`
- `createPlatformSourceAccess()`
- `LibraryScanner(...)`
- `createThemePreferenceStore()`

This construction graph belongs in a dependency module rather than in UI composition.

## Dependency choices

Use the latest versions confirmed from Maven metadata during design:

- Coil: `3.5.0`
- Koin: `4.2.2`

Add version-catalog aliases for:

- `io.coil-kt.coil3:coil-compose`
- `io.coil-kt.coil3:coil-core`
- `io.insert-koin:koin-core`
- `io.insert-koin:koin-compose`

If implementation discovers a platform-specific Coil artifact is required for ByteArray decoding or disk cache configuration, add the smallest necessary Coil artifact and document the reason in the task evidence.

## Coil design

Create a focused shared UI helper, for example `ArtworkImage`, that accepts:

- nullable `ByteArray` artwork
- content description
- modifier
- content scale
- requested size role: thumbnail, card, hero/full
- fallback composable content

The helper builds a Coil model/request from the original bytes and supplies stable cache keys derived from a deterministic content hash plus size role. It should not remove or transform `artworkBytes` in domain models.

Route these surfaces through the helper:

- track row `AlbumMark` thumbnail
- compact `NowPlayingBar` artwork
- album card artwork
- artist row artwork
- drill-down top-bar artwork background
- expanded Now Playing artwork, while preserving full-size/hero behavior

Fallback initials/text and selected-row overlays remain the responsibility of the existing call sites or small wrapper content so visuals do not regress.

The old `ArtworkCache` helpers can remain temporarily if platform code or tests still use them, but new Compose artwork rendering should prefer Coil. Remove only clearly unused imports/call sites after verification.

## Koin design

Create a shared DI module, for example `RhythHausModules.kt`, with a function that returns Koin modules for the app service graph. Use singleton bindings for process/app-lifetime services:

- `PlaybackController`
- `AudioMetadataReader`
- `TagLibReader`
- `LibraryDatabase`
- `LibraryRepository` implemented by `SqlDelightLibraryRepository`
- `PlatformSourceAccess`
- `PlatformAudioScanner` backed by the same `PlatformSourceAccess` instance
- `LibraryScanner`
- `ThemePreferenceStore`

Use factory only for objects that must be recreated per use. For current services, singleton scope matches existing `remember { ... }` process-lifetime behavior.

Add an id factory binding or function binding for `uuid4()` and a time binding for `currentTimeMillis()` if that makes `LibraryScanner` construction cleaner. Keep names/types explicit to avoid ambiguous primitive/function injection.

Initialize Koin once from each platform entry point before rendering `App()`:

- Android `MainActivity.onCreate()` after `setRhythHausAndroidContext(this)` and before `setContent`.
- Desktop `main()` before `Window { App() }` renders.
- iOS `MainViewController()` before `ComposeUIViewController { App() }` composes.

Initialization must be idempotent so previews/re-entry do not crash if Koin is already started.

## Testing and verification

Add common tests for any pure helper that derives artwork cache keys/request keys. Add DI tests that start Koin with the app module and resolve the core bindings using test-safe platform seams where possible. If a binding cannot be started in common tests because it requires platform context, keep the test focused on module construction on JVM/Android-compatible targets and document the limitation.

Verification commands:

- `openspec validate coil-koin-image-di --strict`
- focused shared tests for artwork key helpers and/or DI module construction
- `./gradlew :shared:compileKotlinJvm --configuration-cache`
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
- `/usr/bin/xcrun xcodebuild -version`
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
- `git diff --check`

## Risks

- Coil ByteArray model support and disk-cache behavior may differ by platform. The implementation should verify compilation/tests on JVM, Android, and iOS and keep fallbacks readable.
- Koin startup can fail if called more than once. The initializer must guard existing Koin state.
- Moving service construction out of `App()` can accidentally change lifetimes. Bindings should mirror existing `remember` process-lifetime behavior.
