## ADDED Requirements

### Requirement: Seek with accessibility or keyboard input
The Now Playing seek control MUST expose a localized accessible name, current value and adjustable range and accept keyboard adjustment while focused. No seek action SHALL be available without a positive duration.

#### Scenario: Accessible seek, clamping and cancellation
- **WHEN** a focused keyboard or accessibility adjustment is requested
- **THEN** the same transport callback used for pointer seeking receives one bounded target, while a cancelled drag still sends no seek
