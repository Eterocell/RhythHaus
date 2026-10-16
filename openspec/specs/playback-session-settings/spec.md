# playback-session-settings Specification

## Purpose
Expose truthful output-session state and safe user control over interruption resumption and cached artwork without changing media, library data, or audio routing.

## Requirements

### Requirement: Report platform-verified current audio output
The application MUST show the currently active output only when the platform can identify its active route. It MUST show an explicit unavailable state rather than substituting a list of connected or eligible devices.

#### Scenario: Platform reports active output
- **WHEN** Settings requests output status and the platform reports an active route name
- **THEN** Settings displays that route name and updates it after a route-change event

#### Scenario: Active output cannot be identified
- **WHEN** the platform has no active route name or cannot query the active route
- **THEN** Settings displays localized unavailable status and does not list available devices as though they were active

### Requirement: Persist automatic-resume policy
Settings MUST expose `Follow system recommendation` and `Never` automatic-resume policies, persist the selection, and restore it across process restart. A user-issued playback or selection command MUST supersede any pending system-triggered resume.

#### Scenario: Follow iOS recommendation
- **WHEN** iOS playback was active before an interruption, policy is Follow system recommendation, and interruption end includes the system's should-resume recommendation
- **THEN** playback resumes through the existing controller path

#### Scenario: Do not follow iOS recommendation
- **WHEN** policy is Never and an interruption ends with should-resume
- **THEN** playback remains paused

#### Scenario: Android transient focus loss
- **WHEN** playback was active before transient audio-focus loss, policy is Follow system recommendation, focus is regained, and no user command or session replacement intervened
- **THEN** playback resumes once through the existing controller path

#### Scenario: Android permanent focus loss or explicit user intent
- **WHEN** Android reports permanent focus loss, or the user pauses/selects/stops before focus returns
- **THEN** focus gain does not resume playback

#### Scenario: macOS route loss
- **WHEN** the active output device disappears on macOS
- **THEN** playback remains paused because no OS resume recommendation is available; changing this preference MUST NOT select or reopen an output

### Requirement: Clear cached artwork without changing the library
Settings MUST provide an explicit action to clear the application's artwork image caches and report whether the clear operation succeeded. It MUST NOT delete source media, library records, persisted metadata, playlists, playback session state, or settings.

#### Scenario: Clear caches successfully
- **WHEN** the listener confirms the clear-cache action and cache clearing succeeds
- **THEN** memory and disk image caches are cleared and Settings announces completion
- **AND** rescanning, database writes, source mutation, and playback changes do not occur

#### Scenario: Cache clear failure
- **WHEN** a cache backend fails to clear
- **THEN** Settings preserves all library and playback state and presents a localized recoverable error

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
