## Context

RhythHaus shares its Compose UI across Android, desktop JVM/macOS, and iOS. Settings is rendered through the custom sealed `LibraryRoute` stack, while version properties and launcher artwork are currently platform/build-specific. The requested attribution page needs AboutLibraries metadata available to every supported target.

## Goals / Non-Goals

**Goals:**

- Add Settings-derived About and Open Source Libraries screens with existing stack-based push/pop behavior.
- Render identity and version information from shared resources and generated common Kotlin source.
- Generate a single checked-in AboutLibraries catalog into common Compose resources and render it on all supported targets.
- Preserve Settings layout, safe insets, source-management behavior, and existing platform entry points.

**Non-Goals:**

- Do not redesign Settings, add telemetry, alter playback/library persistence, or introduce Windows/Linux support.
- Do not use Android `BuildConfig`, Android launcher resources, or the Android-only AboutLibraries plugin from common UI.
- Do not add platform-specific library catalogs unless a concrete attribution defect requires them.

## Decisions

### Use Settings-derived custom routes

Add `SettingsAbout` and `OpenSourceLibraries` to the existing `LibraryRoute` sealed interface. `LibraryAppShell` will push these routes from Settings and About, and every return action will use the existing stack pop behavior. Both routes are overlay routes in `LibraryRouteOverlays` and non-detail routes in `LibraryRouteContent`, with compact and wide layout handling explicitly covered by a shell-level navigation test. This preserves back navigation from the system and visible controls without introducing a second navigation framework.

Alternative considered: local Boolean state in `SettingsScreen`. This would not represent the nested libraries screen in the app navigation stack and would make system-back behavior less consistent.

### Generate shared build metadata

`shared/build.gradle.kts` will register a cache-safe task with a provider-backed `rhythhaus.versionName` input and declared generated Kotlin output, then register that output directory with `commonMain`. The About screen consumes this common value. A dedicated Gradle verification proves an overridden `-Prhythhaus.versionName` reaches the generated source.

Alternative considered: duplicate the version in Compose XML resources. That creates a second manual version source. Android `BuildConfig` is rejected because it is unavailable to iOS and desktop common code.

### Make the logo a common Compose drawable

Create a standalone common Compose-compatible RhythHaus logo resource based on the existing launcher mark and render it in the About screen. Android adaptive icon foreground/background resources remain unchanged; the common logo is not an adaptive-icon XML copy. Manual QA verifies it on light and dark Android, desktop, and iOS.

Alternative considered: platform-specific logo providers. That adds expect/actual maintenance without a platform-specific requirement.

### Use AboutLibraries common Compose integration

Use AboutLibraries `15.0.3`, the regular `com.mikepenz.aboutlibraries.plugin`, and `aboutlibraries-compose-m3`. Configure the plugin to export `aboutlibraries.json` into `shared/src/commonMain/composeResources/files/`, check that artifact into source control, and load it through Compose Resources in the shared libraries screen. Export is an explicit maintenance/CI task, never a normal compilation dependency; CI regenerates intentionally and fails when the tracked catalog diff is uncommitted.

The Android-only plugin is rejected because it does not serve desktop and iOS. The checked-in artifact makes third-party attribution changes reviewable and avoids runtime scanning.

## Risks / Trade-offs

- [Generated metadata can omit native dependency attribution] -> Parse the exact checked-in catalog with the pinned 15.0.3 parser, review every regenerated JSON diff and Android/JVM/iOS dependency attribution coverage, and add explicit overrides only for verified omissions.
- [The logo conversion can differ from the launcher icon] -> Preserve the existing mark/colors and require manual light/dark visual QA.
- [External URLs can behave differently per platform] -> Use common `LocalUriHandler` and record device-level source-link QA as a manual verification item.
- [The shared catalog may list dependencies not shipped on a target] -> Accept a project-wide attribution catalog for this change; only split it if legal review identifies a concrete problem.

## Migration Plan

The change is additive. Gradle configuration generates the version source and catalog during builds; source checkout includes the generated catalog. Rollback consists of removing the routes, common screens, resource files, and dependency/plugin declarations together. No persisted user data or database migration is involved.

## Open Questions

None. The requested scope, source URL, common-platform catalog policy, and in-place workspace choice are recorded.
