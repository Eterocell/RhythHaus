## Context

See `proposal.md` for motivation. RhythHaus currently composes all application lifecycle and cross-feature routing in `:shared`; `LibraryNavigationStack` owns route identity and Back arbitration. `:feature:settings` is a host-neutral leaf screen. App preferences already use platform-created Preferences DataStore instances in `:shared`, while feature UI modules own their localized resources. Library/source initialization and playback restoration must remain independent of a first-run presentation.

## Goals / Non-Goals

**Goals:**

- Resolve first-run eligibility without flashing an interactive library underneath an unresolved decision.
- Keep the onboarding screen stateless with respect to persistence, routing, library operations, and playback.
- Present exact Android, iOS, and macOS import guidance through a small platform-neutral projection.
- Preserve the existing single Shared navigation and Back authority.
- Make the flow usable in compact/short windows, localized, and accessible.

**Non-Goals:**

- Requesting runtime permissions or launching a picker from onboarding.
- Changing source registration, scanning, playback restoration, or notification-permission behavior.
- Resetting onboarding automatically after upgrades or adding analytics.
- Adding editable onboarding preferences beyond the Settings re-entry action.
- Teaching every existing RhythHaus feature; the flow covers only local storage, import, formats/scanning, and recovery expectations.

## Decisions

### Add a focused `:feature:onboarding` UI module

Create one Compose Multiplatform feature module using the existing `build-logic.kmp.feature.impl`, controlled-resource, Material/MIUIX, and UI-test conventions. It owns:

- immutable `OnboardingPage` and `OnboardingPresentation` rendering inputs;
- `OnboardingScreen`, page/progress/action semantics, and responsive scrolling;
- English and Simplified Chinese strings whose wording is not shared with other features.

The module receives scalar state and callbacks only. It does not depend on `:shared`, database, library, playback, DataStore, or platform frameworks. `:shared` depends on it as an implementation leaf and remains the composition root. This avoids placing another large leaf UI in `App.kt` while preserving the established feature dependency direction.

Rejected alternatives:

- Put onboarding in `:feature:settings`: first-run presentation is an app-entry concern, and coupling its lifecycle to Settings would give Settings ownership it does not have.
- Put all UI in `:shared`: smaller initially, but grows the facade root and violates the current feature-first direction.
- Add separate API/impl modules: no downstream module needs a stable onboarding API, so the split adds weight without an architectural seam.

### Store only a schema-version completion marker in Shared

Add `OnboardingPreferenceStore` in `:shared` with a flow of `OnboardingEligibility` (`Loading`, `Required`, `Completed`) and `markCompleted(schemaVersion)`. Platform actual factories create `onboarding.preferences_pb` under the same platform-specific application-support/files roots used by existing Shared preferences. The stored integer is the highest completed onboarding schema version; version `1` is required by this change.

A missing value, value below `1`, corruption, or read failure resolves to `Required`. A value at or above `1` resolves to `Completed`. Skip and Finish both call the same idempotent completion write. Library, theme, and playback preference files stay isolated.

A version marker rather than a Boolean allows a future materially changed onboarding contract to opt into a new required version without migrating or overwriting the original value. This change does not trigger such upgrades automatically.

### Gate presentation, not application initialization

`App` collects eligibility alongside theme and notification state. Library publication, default-source preparation, playlist refresh, playback-session restoration, and their existing coordinators continue running. Until eligibility resolves, Shared renders the themed background with a non-interactive loading surface. If `Required`, Shared makes a first-run onboarding destination authoritative. If `Completed`, it composes the normal library.

Skip/Finish persist completion first. Only after a successful write does Shared reveal the normal library. A persistence failure keeps onboarding visible and exposes a localized retryable error; it never pretends completion succeeded. This ordering prevents repeat onboarding caused by process death between dismissal and persistence.

### Use Shared navigation for Settings re-entry, but a launch mode for exit policy

Add `LibraryRoute.Onboarding(launchMode, pageIndex)` and render it as an active full-window overlay that never permits the Now Playing bar. Launch mode and current page are immutable canonical route payload, not a second remembered navigation state:

- `FirstRun`: pushed above Home when eligibility resolves Required, not dismissible by route Back on its first page; Skip/Finish persist and exit to Home.
- `Review`: pushed from Settings at page zero, and Close/platform Back pops to the existing Settings entry without writing preferences.

Next and in-flow Back replace the top route with the adjacent page payload. The canonical Back resolver produces that replace transition when `pageIndex > 0`; Review at page zero uses the ordinary route pop, while FirstRun at page zero is explicitly suppressed. Reopening always pushes page zero. This extends the existing canonical Back arbitration without a feature-local Back handler or state that can disagree with the route stack.

### Project platform wording through a closed common value

Define an expect/actual `OnboardingPlatform` provider in Shared returning `Android`, `IOS`, or `MacOS`. Shared maps it to the feature's closed `OnboardingPlatformGuidance` value. The UI selects localized platform-specific text; it receives no `Context`, `Intent`, Foundation URL, picker launcher, or permission controller.

The four pages are fixed for schema version 1:

1. **Your music stays local** — no streaming or upload.
2. **Add music** — exact active-platform import/reference behavior.
3. **Scan and formats** — recognized formats plus metadata/artwork scanning and skipped-item reporting.
4. **Keep access healthy** — moved/deleted/inaccessible files, rescan, source recovery, and Remove missing boundaries.

No page contains an Add Music button. Finishing returns to the existing Home empty-state/import affordance, preserving one import entry path and preventing onboarding from bypassing the App-owned operation coordinator.

### Settings owns only the row; Shared owns navigation

Add a `Review onboarding` row and callback to `SettingsScreen`, grouped near About. The feature emits `onReviewOnboarding`; Shared pushes `LibraryRoute.Onboarding` in Review mode. Settings does not read completion state and reopening never changes it.

### Test at observable boundaries

Permanent regressions cover:

- store defaults, schema comparison, corruption fallback, idempotent completion, and isolation from other preference files;
- first-run Loading/Required/Completed rendering and persistence-before-dismiss ordering;
- first-run versus Review Back/Close policy and route-stack return to the exact Settings entry;
- no library, scan, picker, permission, or playback callback from onboarding navigation;
- platform guidance mapping for all three values;
- English/Chinese resource ownership and semantics;
- 600×400 dp reachability and hidden-page semantic absence;
- Gradle architecture graph and all three target compilations.

Manual acceptance checks first install, Skip, Finish, restart suppression, Settings re-entry, compact/wide layout, and playback continuity while reviewing onboarding.

## Risks / Trade-offs

- **Startup waiting:** Eligibility adds one small DataStore read before the normal library becomes interactive. Library/playback initialization remains concurrent, and a dedicated loading surface prevents incorrect first-frame interaction.
- **Navigation complexity:** A first-run destination is lifecycle-derived while Review is stack-derived. Keeping launch mode explicit and testing Back ownership avoids a second navigation authority.
- **Copy drift:** Platform import behavior can evolve. The platform-specific strings and capability spec must be updated with future import-policy changes; they deliberately do not derive from low-level picker code.
- **Preference failure:** Persist-before-dismiss can temporarily keep users in onboarding when storage fails. This is preferable to silently losing the completion decision and showing the flow again after restart.
- **Additional module:** `:feature:onboarding` adds Gradle configuration cost, but isolates a durable UI capability and follows existing single-module feature precedents.
