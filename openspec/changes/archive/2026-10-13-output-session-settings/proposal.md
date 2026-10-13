# Proposal

## Why

Playback behavior after interruptions and output changes differs by platform, while RhythHaus offers no Settings surface explaining the active output or controlling automatic resumption. Artwork/image caches also have no user-visible clear action, leaving users without a supported way to reclaim cached data.

## What Changes

- Add localized Settings information for the platform-reported current audio output, including an explicit unavailable/unknown state where a host cannot identify the routed output reliably.
- Add a persisted automatic-resume preference: follow an OS resume recommendation, or never resume automatically. A later user pause/selection always supersedes a pending system resume.
- Add a clear-artwork-cache action that clears the app's Coil memory and disk caches, reports completion/failure, and does not modify library files, database rows, or playback.
- Keep platform route ownership native; do not add output selection or change audio routing.

## Capabilities

### New Capabilities
- `playback-session-settings`: user-visible output status, persisted interruption/resume policy, and cache clearing behavior.

### Modified Capabilities
- `crossfade-playback`: no change.
- `sleep-timer`: no change.

## Impact

- Shared Settings route and preference ownership.
- Android audio-focus lifecycle, iOS interruption bridge, and macOS output-device query.
- Coil cache access from Settings.
- English and Simplified Chinese resources, tests, roadmap, and progress records.
- No database migration, new dependency, media-file mutation, or output-routing control.
