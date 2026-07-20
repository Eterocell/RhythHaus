# Playlist Scroll Chrome Specification

## Purpose

Define how playlist detail participates in the shared app-shell Bottom Bar scroll visibility and measured-clearance contract.

## Requirements

### Requirement: Shared playlist scroll and Bottom Bar contract
Playlist detail SHALL use the existing album/artist track-list scroll-down hide and scroll-up reveal policy for the app-shell Bottom Bar. Its scroll state MUST be reported through the shared scroll visibility mechanism, and its list content MUST use the active Bottom Bar's measured clearance from the shell rather than a playlist-specific threshold or hard-coded clearance.

#### Scenario: Scroll down hides the Bottom Bar
- **WHEN** the user scrolls downward through playlist detail
- **THEN** the active app-shell Bottom Bar SHALL hide according to the same policy used by album and artist detail

#### Scenario: Scroll up reveals the Bottom Bar
- **WHEN** the user scrolls upward through playlist detail
- **THEN** the active app-shell Bottom Bar SHALL reveal according to the same policy used by album and artist detail

#### Scenario: Measured clearance keeps the final row reachable
- **WHEN** the Bottom Bar is active and its height has been measured by the app shell
- **THEN** playlist detail SHALL append that same active measured clearance so the final row can scroll fully above the bar
