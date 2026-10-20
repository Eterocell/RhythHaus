# Adaptive app shell delta

## Purpose

Define stable navigation and space-aware presentation for the existing RhythHaus route history, keeping music playback, contextual actions and content clearance independent of decorative layout.

## ADDED Requirements

### Requirement: Independent primary navigation
The shell SHALL expose Library, Search and Playlists in that order, independently of whether a track is selected. Compact navigation MUST have icons and labels; Settings MUST have a consistent global tool entry separate from playback. Actual route ownership MUST determine highlighting; Settings, About and onboarding MUST NOT falsely highlight a primary destination.

#### Scenario: Navigate before playing music
- **WHEN** no track is selected
- **THEN** all three primary destinations and Settings MUST remain reachable without rendering a pretend mini-player

### Requirement: Existing Home-rooted history
Primary entries SHALL retain the existing single Home-rooted history. Library MUST return to root; Search and Playlists MUST push normally, and repeating the current exact destination MUST NOT duplicate it. Departed pages MUST NOT acquire new retained background stacks. Real navigation MUST preserve current selection-clearing and editing lifetimes.

#### Scenario: Switch through details and primary entries
- **WHEN** the listener visits Library, album detail, Search, then Playlists and issues ordinary Back with no higher-priority target
- **THEN** Back MUST return to Search, album detail, then Library; activating Library instead MUST return directly to root

### Requirement: Available-space adaptive layout
Navigation SHALL use bottom entries below 720dp, an 80dp rail at 720–999dp, and a 208dp sidebar at 1000dp or wider. Page insets MUST be 16/24/32dp respectively, with maximum 1440dp main content. Library detail MUST appear beside browsing only with an actual selected detail and room for at least 320dp browsing, 24dp gap and 400dp detail; otherwise it MUST use a single task surface.

#### Scenario: Medium window and empty detail
- **WHEN** the window is 800×600dp or no album/artist detail is selected
- **THEN** content MUST occupy the usable area without a reserved empty detail column

#### Scenario: Narrow a live detail
- **WHEN** an existing detail can no longer fit beside browsing
- **THEN** it MUST remain the active single-task destination without losing selection, query, scroll or playback solely because of the resize

### Requirement: Measured navigation and contextual clearance
Stable primary navigation SHALL remain separate from scroll-sensitive playback chrome. The shell MUST use actual active navigation, playback or selection measurements and system/keyboard insets to keep the final content item reachable. Selection MUST replace playback actions, not stop playback; an unmeasured or hidden dock MUST expose no interactions. Full player, onboarding and exclusive layers MUST suppress underlying interactive chrome.

#### Scenario: Select while playback is active
- **WHEN** selection starts on an eligible page
- **THEN** only its contextual playback-slot actions MUST be active, list clearance MUST track the actual slot, and playback MUST continue unchanged

#### Scenario: Scroll to the final item
- **WHEN** the listener reaches the final row with navigation and the active dock visible
- **THEN** that row and its actions MUST be scrollable fully above them without a per-feature guessed bottom height

### Requirement: One Back and presentation authority
All route, player, selection, edit and modal dismissals SHALL continue through the existing shared arbitration and predictive-Back protocol. Leaf features MUST NOT install independent platform Back handlers. A cancelled prediction MUST retain the active state, and a completed request MUST wait for authoritative state settlement before admitting a conflicting dismissal.

#### Scenario: Cancel a predictive dismissal
- **WHEN** a predictive Back gesture is cancelled with a feature modal, selection or player active
- **THEN** the same foremost target MUST remain active and no underlying route or input target MUST be accidentally exposed
