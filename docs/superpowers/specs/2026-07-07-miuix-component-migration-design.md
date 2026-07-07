# Miuix Component Migration Design

## Status

Approved for planning and implementation.

## Route

OpenSpec + Superpowers: brainstorming -> design spec -> implementation plan -> subagent-driven development.

## Summary

RhythHaus will migrate suitable shared Compose UI from bare/custom Row, Box, background, border, and hand-rolled popup patterns to Miuix components. The migration is selective: standard app controls and containers should use Miuix where the fit is good, while product-specific music UI, artwork, gestures, adaptive shell behavior, and glass/backdrop wrappers remain custom where needed.

## Current context

- RhythHaus already uses Miuix `0.9.3` through `miuix-ui` and `miuix-blur`.
- `shared/build.gradle.kts` currently depends on `libs.miuix.ui` and `libs.miuix.blur` in `commonMain`.
- `gradle/libs.versions.toml` defines `miuix = "0.9.3"` and aliases for `miuix-ui` and `miuix-blur`.
- The Settings screen still has a custom `AppearanceDropdown` built from `Column`, `Row`, `background`, `border`, and local expansion state.
- Search uses a custom search field built on `BasicTextField` and custom result row surfaces.
- Library rows (`TrackRow`, `ArtistRow`) and the clear-library route dialog use a mixture of Miuix components and custom layout/drawing.
- Now Playing UI, artwork rendering, the equalizer strip, music scrubber, edge/vertical gestures, adaptive two-pane shell, and liquid-glass wrapper are product-specific and should not be blindly replaced.
- A previous adaptive migration proved `miuix-navigation3-adaptive` incompatible with the current Android dependency graph. This migration must not reintroduce it.

## Goals

- Prefer Miuix built-in components for ordinary shared UI: settings rows/dropdowns, preference-like controls, buttons, cards, text, dialogs, popup/dropdown hosts, and standard clickable rows where suitable.
- Add Miuix modules as needed, pinned to the existing `miuix` version reference, when they provide a better fit than custom code.
- Replace the Settings appearance dropdown with a Miuix dropdown/preference component if the Miuix API is available and compiles in commonMain.
- Audit and migrate Search, Library rows, and Clear Library dialog selectively without changing their user-visible behavior.
- Preserve current navigation, route animation, search focus/filter behavior, library selection behavior, playback behavior, scanner behavior, and glass/status-bar behavior.
- Keep the implementation incremental and behavior-preserving.

## Non-goals

- No full app redesign.
- No migration to native platform UI.
- No rewrite of playback, scanner, database, media controls, or route stack ownership.
- No reintroduction of `miuix-navigation3-adaptive`.
- No Windows/Linux product or packaging scope.
- No replacement of custom product-specific controls where Miuix does not provide a clear semantic fit.
- No visual tuning beyond preserving existing layout and using Miuix defaults/overrides where appropriate.

## Dependency strategy

The migration may add Miuix modules when they are used by implementation tasks. Candidate modules include:

- `top.yukonga.miuix.kmp:miuix-preference` for preference rows and dropdown preferences.
- `top.yukonga.miuix.kmp:miuix-icons` only if a task uses Miuix-provided icons and the dependency compiles cleanly.

Rules:

- Use the existing `miuix = "0.9.3"` version reference for every added Miuix module.
- Add only modules that are actually used.
- Do not add `miuix-navigation3-adaptive` or any module that pulls incompatible older Miuix artifacts into Android.
- After adding any Miuix dependency, run Android debug assembly to catch duplicate-class or manifest issues.

## Component migration policy

### Settings

Replace `AppearanceDropdown` with a Miuix component if possible. The preferred target is a Miuix preference/dropdown component from `miuix-preference` because it matches the settings-row use case better than the current hand-rolled expanding card.

The replacement must preserve:

- existing theme options: System, Light, Dark;
- existing label and description resources;
- selected theme state and callback behavior;
- closing/dismiss behavior after selection;
- Settings screen back/dismiss behavior;
- disabled or unavailable states for music folder scanning controls.

If the chosen Miuix dropdown/popup requires `Scaffold`/popup host support, Settings must include the required Miuix host structure without changing route semantics.

### Search

Search should be migrated cautiously:

- Keep `BasicTextField` if no suitable Miuix text input is available in commonMain or if replacing it would risk focus, placeholder, or clear-button behavior.
- Replace ordinary containers/buttons around search with Miuix components when behavior remains identical.
- Search result rows may use Miuix row/container components if they can preserve now-playing highlighting and equalizer display.

### Library rows and cards

Library album cards already use Miuix `Card` and should remain mostly unchanged except for small Miuix-default improvements if needed.

`TrackRow` and `ArtistRow` may be migrated only if the replacement preserves:

- custom artwork/artist marks;
- selected/now-playing visual state;
- content descriptions;
- click behavior;
- duration and metadata display;
- spacing and compactness.

If Miuix `BasicComponent` cannot express the row without awkward workarounds, keep the custom row and document why.

### Clear Library dialog

The current clear-library dialog is intentionally an in-window route overlay so it participates in route animation. A Miuix overlay dialog may replace it only if it preserves that route-level animation behavior. Otherwise, migrate only its inner card/buttons/text styling to Miuix components and keep the route overlay shell.

### Now Playing and glass UI

Do not broadly replace Now Playing, NowPlayingBar, artwork, equalizer, scrubber, or gesture code. These are product-specific music controls. Only standard subcontrols may be migrated where Miuix offers a clear fit and existing gestures/semantics remain intact.

Miuix blur usage must continue to follow existing runtime gating rules:

- backdrop recording gated by `isRenderEffectSupported()`;
- blur/blend/noise/runtime shader paths gated by `isRuntimeShaderSupported()`;
- no self-recording loops where a blur overlay records itself.

## Testing and verification

Implementation should use TDD where practical for pure behavior or regressions. For visual component migration, verification must include compilation and existing behavior tests.

Required checks:

- `openspec validate miuix-component-migration --strict`
- focused shared compile after source changes: `./gradlew :shared:compileKotlinJvm --configuration-cache`
- Android duplicate-class/dependency check when Miuix modules are added: `./gradlew :androidApp:assembleDebug --configuration-cache`
- relevant existing shared tests, especially navigation/library tests if route surfaces are touched
- final broad check or exact blockers recorded:
  - `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
  - `/usr/bin/xcrun xcodebuild -version`
  - `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`

## Acceptance criteria

- Settings appearance selection uses a Miuix component rather than the custom expanding dropdown, unless implementation proves the Miuix component unavailable or incompatible and records the blocker.
- Added Miuix modules are pinned to the existing Miuix version and are actually used.
- No incompatible Miuix dependency or duplicate-class issue is introduced.
- Suitable standard custom UI pieces are migrated to Miuix components.
- Product-specific controls remain custom where Miuix is not a suitable semantic fit.
- Existing theme selection, search, library row selection, clear-library flow, playback, route animation, bottom bar, and glass/status-bar behavior are preserved.
- Verification commands pass, or exact blockers are recorded.
