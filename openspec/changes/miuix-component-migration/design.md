# Design: Miuix Component Migration

## Strategy

Use a selective Miuix-first migration. Standard controls should use Miuix components where the component API fits the current behavior. Product-specific music UI remains custom.

This keeps the migration incremental and behavior-preserving: migrate the obvious settings/dropdown and standard surface cases first, verify, then migrate additional row/dialog pieces only when the Miuix component preserves semantics without awkward workarounds.

## Dependencies

The current project already uses `miuix-ui` and `miuix-blur` at `0.9.3`.

Implementation may add these modules if used:

- `miuix-preference` for settings/preference/dropdown components.
- `miuix-icons` only if a task actually uses Miuix icons.

Every added Miuix module must use `version.ref = "miuix"` in `gradle/libs.versions.toml`.

Do not add `miuix-navigation3-adaptive`; previous Android verification showed it can pull incompatible older Miuix artifacts into the dependency graph.

## Migration rules

### Settings

Replace the custom `AppearanceDropdown` with a Miuix preference/dropdown component if it compiles in `commonMain`. The new implementation must preserve the current theme options, selected state, labels, descriptions, and `onThemeModeSelected` callback.

If the selected Miuix component needs a popup host, wrap the Settings content in the required Miuix host such as `Scaffold`, while preserving the route-level Settings screen behavior.

### Search

Keep `BasicTextField` unless a suitable Miuix text input is available and preserves focus request, placeholder, single-line input, cursor color, clear action, and filtering behavior. Standard surrounding surfaces and result rows may migrate to Miuix components if now-playing highlighting and equalizer behavior remain intact.

### Library rows and dialog

`TrackRow` and `ArtistRow` can migrate only if custom artwork, selected state, content descriptions, click behavior, duration/metadata display, and compact spacing are preserved. If Miuix `BasicComponent` or similar components make the row less clear or less capable, keep the custom rows.

The clear-library dialog may use a Miuix overlay dialog only if it still participates in the existing in-window route animation. Otherwise keep the route overlay shell and migrate only inner Miuix components.

### Product-specific UI

Do not broadly replace Now Playing artwork, playback controls, equalizer visuals, music scrubber, bottom-bar sheet gestures, edge-swipe gestures, adaptive shell, or glass wrappers. These are app-specific and may stay custom.

## Verification

- Validate OpenSpec: `openspec validate miuix-component-migration --strict`.
- Run focused shared compile after source edits: `./gradlew :shared:compileKotlinJvm --configuration-cache`.
- Run `./gradlew :androidApp:assembleDebug --configuration-cache` after dependency changes to catch duplicate classes.
- Run existing JVM tests relevant to touched behavior.
- Before completion, run broad JVM/desktop/Android and iOS simulator verification or record exact blockers.
