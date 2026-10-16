# Spec Delta

## Purpose

This capability lets iOS users browse and play RhythHaus's local music and control the existing playback session from a CarPlay display using Apple's approved audio-app experience.

## ADDED Requirements

### Requirement: CarPlay exposes the local music library

The iOS app MUST present CarPlay-approved browsing templates for available local library content and MUST derive their contents from the same authoritative library data used by the iOS app.

#### Scenario: CarPlay connects with a populated library
- **WHEN** an authorized CarPlay audio scene connects and the local library is available
- **THEN** CarPlay presents supported library browsing destinations with localized labels
- **AND** list contents reflect the current Shared library state without duplicating or persisting a second catalog

#### Scenario: Library changes while CarPlay is connected
- **WHEN** a scan or library mutation publishes new authoritative library state
- **THEN** affected CarPlay lists refresh from that state
- **AND** a stale list item cannot play a track no longer present in the authoritative library

#### Scenario: CarPlay connects before library initialization
- **WHEN** the CarPlay scene connects while Shared library initialization is incomplete
- **THEN** it presents a bounded loading or empty state without blocking the iOS app
- **AND** it refreshes when authoritative library state becomes available

#### Scenario: CarPlay launches without the phone interface
- **WHEN** CarPlay starts the application before the phone interface is composed
- **THEN** library initialization and playback restoration proceed once for the application process
- **AND** actual playback records history without requiring the phone interface
- **AND** opening the phone interface later does not repeat restoration or history collection

#### Scenario: The initial playlist read fails
- **WHEN** library initialization succeeds but the initial playlist read fails
- **THEN** Songs, Albums, and Artists remain available for browsing and playback
- **AND** Playlists reports unavailable content rather than leaving the entire library loading
- **AND** a later successful playlist read restores the playlist destination

#### Scenario: A browsing list exceeds the platform item limit
- **WHEN** a category or detail contains more entries than one CarPlay list can display
- **THEN** every entry remains reachable through bounded page navigation
- **AND** selecting an entry on a later page retains its complete ordered browsing context

### Requirement: CarPlay track selection uses the shared playback session

CarPlay selection MUST use the application's existing playback queue and controller. It MUST NOT create a second audio engine or an independently persisted queue.

#### Scenario: User selects a playable library track
- **WHEN** the user selects a current library item in CarPlay
- **THEN** the existing Shared playback controller receives the selected track and an ordered queue derived from that browsing context
- **AND** playback, persistence, history, sleep timer, ReplayGain, crossfade, and gapless behavior remain owned by the existing playback session

#### Scenario: Selected item is stale or unavailable
- **WHEN** a CarPlay selection refers to a track that is no longer present or playable
- **THEN** the action is rejected without changing the queue or current playback
- **AND** CarPlay reports a recoverable failure rather than silently selecting a different track

#### Scenario: A selected occurrence is reselected after queue editing
- **WHEN** the user reselects the current browsing occurrence after the active queue was edited
- **THEN** the queue is replaced with the current browsing context's exact ordered occurrences
- **AND** duplicate tracks remain distinct occurrences

### Requirement: CarPlay transport reflects authoritative playback state

CarPlay MUST expose the supported system Now Playing and transport actions for the current shared playback state, with metadata and artwork derived from the authoritative current track.

#### Scenario: Transport action is invoked from CarPlay
- **WHEN** the user requests play, pause, next, previous, or seek through a supported CarPlay/system control
- **THEN** the request is routed through the existing playback controller
- **AND** unavailable or invalid actions have no effect and do not create a parallel command path

#### Scenario: Playback changes outside CarPlay
- **WHEN** playback state or the current track changes in the iOS app or through a system control
- **THEN** CarPlay Now Playing metadata, artwork, elapsed time, and supported actions update to match the authoritative state

#### Scenario: A transport request targets an obsolete playback occurrence
- **WHEN** a request carries playback identity from before an occurrence change or a later playback run
- **THEN** the request is rejected without changing playback even if the current track ID is the same

#### Scenario: Playback advances automatically
- **WHEN** gapless playback or crossfade promotes the next track
- **THEN** system metadata and artwork identify the promoted track
- **AND** previous and next actions reflect actual traversal boundaries and the active repeat policy

#### Scenario: CarPlay scene disconnects
- **WHEN** the CarPlay scene disconnects
- **THEN** CarPlay presentation resources and scene-specific observers are released
- **AND** ongoing playback and the iPhone app's playback state remain unchanged

#### Scenario: A previous connection completes an action after reconnecting
- **WHEN** an asynchronous presentation or selection from a disconnected scene completes after a new connection
- **THEN** its original action is completed exactly once
- **AND** it cannot present a failure or change the replacement connection's interface

### Requirement: CarPlay entitlement and scene configuration are explicit

The iOS target MUST declare the CarPlay audio scene and the matching audio entitlement configuration. Distribution MUST require Apple's managed CarPlay audio capability and provisioning profile; an unsigned or unentitled simulator build MUST remain buildable without claiming CarPlay distribution authorization.

#### Scenario: CarPlay entitlement is not granted
- **WHEN** the developer account does not have the managed CarPlay audio capability
- **THEN** local builds can compile the CarPlay integration without a fabricated provisioning entitlement
- **AND** release documentation identifies the Apple approval and provisioning prerequisite

#### Scenario: Authorized CarPlay simulator session
- **WHEN** an authorized simulator configuration starts a CarPlay display session
- **THEN** the CarPlay scene connects and shows the app's root browsing template
- **AND** no physical vehicle connection is required for this simulator check
