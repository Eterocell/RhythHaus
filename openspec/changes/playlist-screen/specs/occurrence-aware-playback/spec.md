## ADDED Requirements

### Requirement: Occurrence identity for visible-list playback
The system SHALL model every queue slot as a stable occurrence ID paired with a `PlayableTrack`. Queue-position behavior, including selection, current-row highlighting, checkpoints, shuffle, skip, and reconciliation, MUST use occurrence identity. Library home, Search, album, and artist lists MUST create queue occurrences when converted to a playback queue. Saved-playlist playback MUST use playlist-entry IDs as occurrence IDs and preserve exact visible entry order.

#### Scenario: Select the second duplicate in a saved playlist
- **WHEN** the user selects the second visible occurrence of the same track in a saved playlist
- **THEN** the system SHALL start that selected occurrence instead of the first matching track

#### Scenario: Start playback from a visible library list
- **WHEN** the user selects a row in a visible Library, Search, album, or artist list
- **THEN** the system MUST build queue occurrences from that visible list and start the selected occurrence

### Requirement: Duplicate-preserving playback navigation
The system SHALL preserve repeated track IDs as distinct queue occurrences during playback. Shuffle order and skip behavior MUST operate on occurrence IDs, and selecting the active occurrence MUST follow the existing current-row selection contract without replacing the queue. Selecting a different occurrence from a saved playlist MUST replace the queue with the visible playlist occurrences and begin the selected occurrence.

#### Scenario: Skip between duplicate occurrences
- **WHEN** the current queue contains adjacent occurrences for the same library track and the user skips forward
- **THEN** the system SHALL advance to the next occurrence rather than treating it as the current occurrence

#### Scenario: Shuffle a queue with duplicates
- **WHEN** shuffle is enabled for a queue containing duplicate track IDs
- **THEN** the system MUST generate and navigate a shuffle order over distinct occurrence IDs

### Requirement: Occurrence-aware session persistence and legacy compatibility
The DataStore-owned playback session SHALL persist ordered occurrence ID and library-track ID pairs plus the explicit current occurrence. Snapshot validation MUST require unique occurrence IDs while allowing repeated library-track IDs. The system MUST normalize a valid legacy track-ID-only snapshot into deterministic unique occurrences, preserve valid queue membership, and restore it paused without autoplay.

#### Scenario: Restore duplicate occurrences after process restart
- **WHEN** a persisted occurrence-aware session contains two occurrences with the same library-track ID
- **THEN** the system SHALL restore both occurrences in their persisted order

#### Scenario: Restore a legacy session
- **WHEN** the system loads a valid legacy track-ID-only session snapshot
- **THEN** the system MUST create deterministic unique occurrences and restore the session paused without autoplay

### Requirement: Authoritative reconciliation of occurrence queues
The system SHALL reconcile queue occurrences against the authoritative library-track map. Reconciliation MUST drop only occurrences whose referenced track no longer exists, preserve all surviving duplicate occurrences, and keep the current occurrence loaded without reload when it survives. Runtime shuffle order MUST be regenerated from surviving occurrence IDs.

#### Scenario: Reconcile after a track is removed
- **WHEN** authoritative-library reconciliation finds one duplicate occurrence whose track no longer exists
- **THEN** the system MUST remove only occurrences for that missing track and preserve other surviving duplicates

#### Scenario: Reconcile a surviving current occurrence
- **WHEN** reconciliation runs and the current occurrence's track still exists
- **THEN** the system SHALL retain that occurrence without reloading playback

### Requirement: Paused restoration and session failure ownership
The system SHALL preserve paused restoration semantics for restored sessions. Playlist persistence MUST NOT become a second playback source of truth, and playback-load or session-store failures MUST retain existing controller and coordinator fail-safe behavior.

#### Scenario: Restore a paused occurrence-aware session
- **WHEN** the application restores a persisted paused session with duplicate occurrences
- **THEN** the system MUST retain the paused status and queue order without autoplay
