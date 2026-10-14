# Spec Delta

## ADDED Requirements

### Requirement: iOS system media controls share authoritative playback ownership

The iOS system Now Playing center and remote command center MUST represent the current Shared playback session and MUST route supported transport commands through the existing playback controller. CarPlay and lock-screen controls MUST NOT install independent playback ownership or overwrite one another's metadata.

#### Scenario: Current playback is published to system media controls
- **WHEN** the Shared playback state changes current track, status, position, duration, or supported commands
- **THEN** iOS publishes matching Now Playing metadata, artwork, elapsed time, rate, and command availability
- **AND** absent track metadata or artwork is removed rather than retained from the previous item

#### Scenario: Remote command is unavailable
- **WHEN** iOS invokes a command that is not valid for the current authoritative playback state
- **THEN** the command is rejected without mutating playback or queue state
- **AND** the command handler reports failure and no duplicate command target is installed

#### Scenario: Artwork changes while metadata remains current
- **WHEN** artwork is added, replaced, or removed for the current track
- **THEN** the system Now Playing artwork field changes while the current metadata, elapsed position, rate, and other fields remain intact
