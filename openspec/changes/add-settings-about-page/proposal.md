## Why

RhythHaus has no in-app surface that identifies the application, exposes its version and source repository, or attributes the open-source software it uses. A shared Settings flow is needed so these facts and attributions are consistent across Android, desktop JVM/macOS, and iOS.

## What Changes

- Add an About entry to Settings that opens a shared About page.
- Show the RhythHaus logo, app name, version, and a source-code link to `https://github.com/Eterocell/RhythHaus` on the About page.
- Add an Open Source Libraries entry at the bottom of the About page.
- Add a cross-platform AboutLibraries catalog generated into a shared Compose resource and render it in a dedicated Settings-derived screen.
- Add shared version and logo resources so the common Compose UI does not read platform-only launcher or build metadata.
- Add localized English and Chinese strings plus focused navigation and metadata regression coverage.

## Capabilities

### New Capabilities
- `settings-about-page`: Provides the Settings-derived About page, shared identity metadata, source link, and navigation to open-source attributions.
- `open-source-attributions`: Provides the AboutLibraries-generated, cross-platform catalog of third-party libraries and license information.

### Modified Capabilities

- None.

## Impact

- Shared Compose Settings and custom route-stack UI in `shared/src/commonMain`.
- Shared Compose resource strings and a new shared logo resource.
- Gradle version catalog, root/shared plugin declarations, and generated common AboutLibraries JSON metadata.
- Common Settings and route-stack tests.
- No database schema, playback, scanning, source-access, or platform-specific app entry-point behavior changes.
