# Spec Delta

## ADDED Requirements

### Requirement: Row selection is available from a named secondary action
Eligible Library and Search rows SHALL offer a localized Select action in a named secondary-actions menu while normal mode is active. Invoking it SHALL enter the same page-scoped selection state as long press, select that track, and MUST NOT play it. The action SHALL be reachable without a long-press gesture on desktop and assistive-technology paths.

#### Scenario: Enter selection from the row menu
- **WHEN** a listener opens the named secondary-actions menu on an eligible row and chooses Select
- **THEN** that page SHALL enter selection mode with the row selected and SHALL NOT dispatch playback

#### Scenario: Enter selection without a pointer gesture
- **WHEN** a desktop or assistive-technology user invokes the named Select action
- **THEN** the same page-scoped selection state and selected track SHALL result as a long press

### Requirement: Row activation and selection actions have distinct semantics
When normal row activation plays a track, its accessible name SHALL identify the track and SHALL NOT describe selection. The localized selection action SHALL be separately named, and active selection SHALL expose the existing selected/toggle state without conflating it with now-playing state.

#### Scenario: Read a playable row
- **WHEN** assistive technology focuses a normal-mode track row
- **THEN** the accessible name SHALL identify the track and its primary action SHALL NOT be announced as Select

#### Scenario: Read a selected row
- **WHEN** assistive technology focuses a track row during selection mode
- **THEN** the row SHALL expose the track identity and selected/toggle state independently from any now-playing state
