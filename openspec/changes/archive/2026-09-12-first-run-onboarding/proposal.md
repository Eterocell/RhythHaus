## Why

A new RhythHaus installation currently opens directly into the library without explaining its local-first storage model, how each platform imports music, when scanning occurs, or which recovery limits apply. A short first-run introduction is needed before Phase 1 release closure so users can reach the existing import flow with correct expectations and can revisit the guidance later.

## What Changes

- Add a one-time, localized onboarding flow presented after startup state has loaded and before normal library interaction.
- Explain that RhythHaus is local-first, does not stream or upload music, and needs the user to add or import supported local audio.
- Present platform-appropriate import guidance without exposing platform APIs to shared UI.
- Explain supported audio formats, scanning behavior, permission/access implications, and recovery limits for moved, deleted, or inaccessible files.
- Allow users to skip or finish onboarding; either action records completion and enters the normal library experience without requesting permissions or starting an import automatically.
- Add a Settings action that reopens onboarding without clearing its persisted completion state or resetting library/playback data.
- Keep playback, restored queue state, source registration, and background library initialization independent from onboarding visibility.

## Capabilities

### New Capabilities

- `first-run-onboarding`: Persistent first-run eligibility, localized onboarding presentation, platform-neutral guidance projection, completion/skip behavior, and Settings re-entry.

### Modified Capabilities

- None.

## Impact

- `:shared` remains the application composition owner and stores the small onboarding-completion preference beside existing app preferences.
- A focused `:feature:onboarding` Compose Multiplatform module owns the stateless onboarding screen and resources.
- `:feature:settings` gains a host-neutral action for reopening onboarding.
- Shared navigation and Back arbitration gain an onboarding destination whose first-run instance cannot be dismissed without recording skip or completion.
- Android, iOS, and JVM provide only a small platform guidance value; onboarding does not request permissions or invoke native pickers directly.
- New common/JVM UI, preference-store, route, lifecycle, localization, and architecture coverage is required; no database schema or dependency version changes are expected.
