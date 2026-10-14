# Proposal

## Why

RhythHaus already plays local audio on iOS, but its library and playback controls are unavailable from a vehicle's CarPlay display. Native CarPlay integration makes the existing local-first library safely browsable and controllable while reusing the current Shared playback session.

## What Changes

- Add an iOS CarPlay audio scene with system-approved list templates for local library browsing and a Now Playing destination.
- Bridge CarPlay selection and transport actions to the existing Shared repository and playback controller; do not create a second queue or audio engine.
- Publish accurate current-track metadata, artwork, playback state, and supported transport capabilities to system Now Playing.
- Configure the CarPlay audio entitlement declaration and scene manifest; document that Apple-managed capability approval and provisioning are required for signed distribution.
- Add deterministic policy/bridge tests and simulator validation; retain physical CarPlay hardware and entitlement approval as user-owned acceptance evidence.

## Capabilities

### New Capabilities
- `ios-carplay-audio`: Native CarPlay audio browsing and transport integrated with the existing iOS local library and Shared playback state.

### Modified Capabilities
- `playback-session-settings`: Extend iOS system Now Playing/remote-command behavior to support CarPlay without changing playback ownership or automatic-resume policy.
