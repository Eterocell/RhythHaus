# Android Notification Permission Recovery

## ADDED Requirements

### Requirement: Explicit Android notification permission state

The system SHALL classify media notification permission into unavailable, granted, requestable denial, or settings-required denial without exposing Android framework types to Shared or Settings.

#### Scenario: Initial request
- **WHEN** RhythHaus starts on Android 13+ with `POST_NOTIFICATIONS` denied and no request active
- **THEN** Android requests it once and suppresses competing requests

#### Scenario: Requestable denial
- **WHEN** Android denies the permission but allows another request
- **THEN** Settings offers a request action

#### Scenario: Permanent denial
- **WHEN** Android denies the permission without allowing another request
- **THEN** Settings offers application notification settings

### Requirement: Non-blocking recovery explanation

Settings SHALL render recovery only for denied Android notification permission.

#### Scenario: Denied explanation
- **WHEN** permission is denied
- **THEN** Settings explains that in-app playback remains available and Android notification/lock-screen controls require notification permission
- **AND** renders one accessible recovery action

#### Scenario: Recovery during source mutation
- **WHEN** a library source mutation is active
- **THEN** the recovery action remains enabled and does not mutate library, queue, or playback state

### Requirement: Cross-platform isolation

The recovery surface SHALL not alter Settings or local playback on non-Android platforms or where Android notification permission is unavailable or granted.

#### Scenario: Non-Android or granted state
- **WHEN** RhythHaus runs on iOS, desktop, Android < 33, or granted Android 13+
- **THEN** no recovery card, action, or accessibility node is rendered
