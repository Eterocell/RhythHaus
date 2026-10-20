# Playlist scroll chrome delta

## MODIFIED Requirements

### Requirement: Shared playlist scroll and Bottom Bar contract
Playlist detail SHALL use the existing album/artist track-list scroll-down hide and scroll-up reveal policy for the app-shell playback/contextual dock, independently of stable primary navigation. Its scroll state MUST be reported through the shared scroll visibility mechanism, and list content MUST use the shell's measured active dock and navigation clearance rather than a playlist-specific threshold or hard-coded clearance. Primary navigation MUST NOT be hidden by this scroll policy.

#### Scenario: Scroll down hides the Bottom Bar
- **WHEN** the user scrolls downward through playlist detail
- **THEN** the active app-shell playback dock SHALL hide according to the same policy used by album and artist detail, while primary navigation remains available

#### Scenario: Scroll up reveals the Bottom Bar
- **WHEN** the user scrolls upward through playlist detail
- **THEN** the active app-shell playback dock SHALL reveal according to the same policy used by album and artist detail without changing primary navigation

#### Scenario: Measured clearance keeps the final row reachable
- **WHEN** navigation and the active dock have been measured by the app shell
- **THEN** playlist detail SHALL append that same active combined clearance so the final row can scroll fully above the visible chrome

#### Scenario: Resize or change the active dock
- **WHEN** the window changes layout or the shell replaces playback actions with contextual actions
- **THEN** content SHALL use the new active measurements, not an outgoing or unmeasured dock's height, and unavailable layers SHALL expose no interactions
