## ADDED Requirements

### Requirement: Selected track row uses user-facing copy

Selected track rows SHALL NOT display implementation/debug language or animation percentages. A selected track row SHALL display the user-facing status `Now playing`.

#### Scenario: Selected track row status
- **WHEN** a track row is selected
- **THEN** the row displays `Now playing`
- **AND** the row does not display `queued on shared UI`
- **AND** the row does not display a selected-state percentage

#### Scenario: Non-selected track row remains unchanged
- **WHEN** a track row is not selected
- **THEN** the row continues to show title, artist/album metadata, and duration without a selected-state status label
