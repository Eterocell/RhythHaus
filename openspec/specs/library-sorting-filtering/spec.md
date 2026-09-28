# Library Sorting and Filtering

## Purpose

Provide deterministic, non-destructive sorting and filtering for the Home library presentation while preserving existing browse, selection, and playback semantics.

## Requirements

### Requirement: Query state is ephemeral and shared-owned

The system SHALL keep the active Library sort and filter query in the Shared Library shell lifetime and SHALL NOT persist it in the database or preferences.

#### Scenario: Query state resets with a new shell
- **WHEN** a new Library shell is created
- **THEN** it starts with the default sort and no filters
- **AND** no database or media mutation occurs

### Requirement: Flat Home results support deterministic sorting

The system SHALL support Title, Artist, Album, Added, Modified, Play Count, and Favorite sort keys with ascending and descending directions for flat Home presentations.

#### Scenario: Equal primary values use stable tie-breakers
- **WHEN** two visible tracks have equal values for the selected sort key
- **THEN** the result orders by case-insensitive title, artist, album, and stable track ID in that order
- **AND** repeated recomposition produces the same order

#### Scenario: Missing optional values do not crash
- **WHEN** a track lacks modified time, artwork, or play history
- **THEN** sorting completes deterministically using the defined empty/zero fallback
- **AND** the track remains eligible unless a matching filter excludes it

### Requirement: Filters narrow only visible Home tracks

The system SHALL support Favorite-only, Artwork-present, and Source filters, and SHALL apply them before sorting.

#### Scenario: Favorite filter shows current membership
- **WHEN** Favorite-only is enabled
- **THEN** every visible track belongs to the authoritative favorite ID set
- **AND** no unfavorited track is shown

#### Scenario: Artwork filter excludes absent artwork
- **WHEN** Artwork-present is enabled
- **THEN** only tracks with persisted non-empty artwork are shown, even when routine track rows omit artwork bytes for lazy loading

#### Scenario: Source filter selects one source
- **WHEN** a source filter is selected
- **THEN** only tracks whose authoritative source projection equals that source ID are shown
- **AND** an unknown source ID yields an empty result

### Requirement: Browse modes retain their membership semantics

The system SHALL preserve Favorites, Recently Played, and Recently Added membership rules while applying explicit query filters and sort choices to their visible flat results.

#### Scenario: Default ordering remains distinct from an explicit title sort
- **WHEN** the user selects Title ascending in a recent flat browse mode
- **THEN** the visible tracks sort by title instead of reverting to the mode's default recent ordering

#### Scenario: Recent and favorite modes do not broaden membership
- **WHEN** a user applies sorting or filtering while viewing Favorites, Recently Played, or Recently Added
- **THEN** the result remains limited to that mode's eligible tracks
- **AND** playback uses the exact resulting visible order

#### Scenario: Grouped modes remain grouped
- **WHEN** a user views Albums or Artists with a query active
- **THEN** album/artist grouping and detail-page track ordering remain unchanged
- **AND** the query does not create a flat queue from grouped cards

### Requirement: Query changes preserve safe interaction state

The system SHALL clear or reconcile Home selection when a query changes the visible flat track IDs or order.

#### Scenario: Selected track becomes filtered out
- **WHEN** an active filter excludes a selected track
- **THEN** that track is removed from selection before the next selection action
- **AND** a stale selection cannot enter a playback queue

#### Scenario: Query changes do not alter playback
- **WHEN** a user changes sorting or filtering while playback is active
- **THEN** the current playback track, queue, repeat, shuffle, and progress remain unchanged

### Requirement: Controls are localized and accessible

The system SHALL expose localized English and Simplified Chinese labels for sort/filter controls and selected states.

#### Scenario: Controls communicate current state
- **WHEN** a sort or filter control is focused by accessibility services
- **THEN** its label and state describe the active key, direction, or checked filter
- **AND** inactive filters remain actionable to clear or enable

#### Scenario: Recent-mode default does not claim an explicit sort
- **WHEN** a recent browse mode uses its initial date ordering before a sort choice
- **THEN** Title and Ascending are not announced as selected
- **AND** selecting Title explicitly activates Title/Ascending and updates the announced state

### Requirement: Empty filtered results are passive

The system SHALL show a localized empty-result message when the active query removes all visible tracks and SHALL NOT show an import or destructive-library action solely because filtering returned no rows.

#### Scenario: Filtered empty state
- **WHEN** the library contains tracks but the active query matches none
- **THEN** a passive localized empty state is shown
- **AND** source import and clear-library actions are not introduced by that empty result
