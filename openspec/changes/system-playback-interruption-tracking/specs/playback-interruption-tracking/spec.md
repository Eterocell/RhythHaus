## Purpose

Playback state reflects system-initiated interruption and output-route changes, so the interface pauses when the operating system pauses the media and resumes only when the system explicitly allows it.

## ADDED Requirements

### Requirement: System interruption pauses active playback

When the operating system interrupts an actively-playing session (an incoming call, Siri, or a competing device claiming the audio route), the app SHALL transition the playback state to Paused and the interface SHALL reflect that the media is paused.

#### Scenario: Interruption begins while playing
- **WHEN** playback is active and the system delivers an interruption-began signal
- **THEN** the app SHALL transition playback state to Paused
- **AND** the interface SHALL show the media as paused

#### Scenario: Interruption begins while already paused
- **WHEN** playback is already paused and the system delivers an interruption-began signal
- **THEN** the app SHALL remain Paused
- **AND** no spurious pause transition SHALL be emitted

### Requirement: Interruption end auto-resumes only when allowed and playback was active

When a system interruption ends, the app SHALL resume playback only when the system signals that resumption is permitted AND playback was active immediately before the interruption.

#### Scenario: Resumption permitted and playback was active
- **WHEN** an interruption ends and the system signals resumption is permitted
- **AND** playback was active immediately before the interruption began
- **THEN** the app SHALL resume playback and transition state to Playing

#### Scenario: Resumption not permitted
- **WHEN** an interruption ends and the system does not signal that resumption is permitted
- **AND** playback was active immediately before the interruption began
- **THEN** the app SHALL remain Paused and SHALL NOT resume

#### Scenario: Playback was not active before the interruption
- **WHEN** an interruption ends and the system signals resumption is permitted
- **AND** playback was not active immediately before the interruption began
- **THEN** the app SHALL remain Paused and SHALL NOT resume

### Requirement: Output-route disconnect pauses playback without auto-resume

When the active output route disconnects while playing (headphones or earbuds removed, or claimed by another device), the app SHALL pause playback and reflect Paused, and SHALL NOT auto-resume when a route later becomes available.

#### Scenario: Route disconnects while playing
- **WHEN** playback is active and the active output route disconnects
- **THEN** the app SHALL pause playback and transition state to Paused

#### Scenario: Route disconnects while paused
- **WHEN** playback is already paused and the active output route disconnects
- **THEN** the app SHALL remain Paused
- **AND** no spurious pause transition SHALL be emitted

#### Scenario: Route becomes available after a disconnect
- **WHEN** the output route disconnected while playing and later becomes available again
- **THEN** the app SHALL remain Paused and SHALL NOT auto-resume
