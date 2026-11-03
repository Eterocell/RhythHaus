# Spec Delta

## Purpose

Defines a responsive application shell that keeps navigation, safe areas, foreground input, and chrome geometry coherent across supported window sizes and route transitions.

## ADDED Requirements

### Requirement: Responsive navigation is independent of playback
RhythHaus SHALL render one Home-rooted application stack with compact bottom navigation below 720dp, a navigation rail from 720dp through 999dp, and a sidebar at 1000dp and above. Settings SHALL remain a global tool action; primary navigation SHALL not depend on playback state.

#### Scenario: Compact window
- **WHEN** the safe-area-adjusted app window is narrower than 720dp
- **THEN** the shell SHALL show the three existing primary destinations in compact bottom navigation and SHALL keep Settings as a global action rather than a fourth primary destination

#### Scenario: Rail and sidebar windows
- **WHEN** the app window is 720–999dp wide or at least 1000dp wide
- **THEN** the shell SHALL use the navigation rail or sidebar respectively and SHALL preserve the same primary destinations and Home-rooted route semantics

#### Scenario: Navigation without playback
- **WHEN** no track is selected or playback stops
- **THEN** the shell SHALL keep primary navigation and the Settings action available according to the active route

### Requirement: Shared route header owns common page chrome
The shell SHALL provide one measured header for ordinary routes that need a title or route actions. The route supplies localized title, return action, and route actions through its explicit feature boundary. Header height stays common within the active layout class and text scale; long titles remain accessible when ellipsized. Feature content adds no duplicate app header or top safe inset. Full-window onboarding and expanded-player layers keep their own controls.

#### Scenario: Route header changes during navigation
- **WHEN** a route enters, exits, or returns within an unchanged window and text scale
- **THEN** the Shared header SHALL keep the same measured geometry while its title, return action, and available actions change to the active route

#### Scenario: Long title or many route actions
- **WHEN** the active route has a long localized title or more actions than fit beside the title
- **THEN** the title SHALL ellipsize visually while retaining its full accessible name and overflow actions SHALL use a named accessible menu without increasing header height

#### Scenario: Text scale or layout class changes
- **WHEN** the available width crosses a shell breakpoint or system text scale changes
- **THEN** the header SHALL recompute one common height for all routes in that layout/text class and keep its required actions reachable

### Requirement: Route transitions preserve one current safe-area rectangle
Visible application content and app-owned chrome SHALL use the current window-safe rectangle consistently before, during, and after route transitions. Intentional content translation MAY animate, but route animation SHALL NOT change the measured safe rectangle.

#### Scenario: Route transition at a fixed window size
- **WHEN** a route enters, exits, or an interactive transition is cancelled without a window or system-inset change
- **THEN** source and destination content SHALL retain the same shell safe rectangle at every transition frame and cancellation SHALL restore the source route without residual displacement

#### Scenario: Real window or system-inset change
- **WHEN** the window size, orientation, display cutout, system bars, or keyboard insets actually change
- **THEN** every visible application layer SHALL relayout from the latest window-safe geometry

### Requirement: Measured chrome clearance is applied once
Scrollable route content SHALL reserve the active measured navigation, playback or selection chrome, and safe-area clearance only where those surfaces occupy space. A stale or mismatched measurement SHALL NOT expose an interactive control or reserve an incorrect route-specific height.

#### Scenario: Bottom chrome and final row
- **WHEN** bottom navigation and a playback or selection bar occupy the compact layout
- **THEN** scrollable content SHALL keep its final row reachable above their measured combined occupied area without counting any inset twice

#### Scenario: Chrome measurement changes
- **WHEN** layout class, window width, bar content, or measured chrome height changes
- **THEN** route clearance SHALL update from the active measurements and any unmeasured or stale presentation SHALL remain non-interactive

### Requirement: Foreground layer owns input and keyboard avoidance
Only the foremost active route or overlay SHALL accept pointer, gesture, keyboard, focus, and accessibility actions. The layer that owns a visible text input SHALL consume its keyboard avoidance; an obscured route SHALL NOT independently resize or react to the same keyboard inset.

#### Scenario: Overlay covers a route
- **WHEN** a dialog, editor, onboarding surface, or expanded player is active above a route
- **THEN** the foreground surface SHALL own input and accessibility focus and the covered or exiting surface SHALL expose no interactive actions

#### Scenario: Keyboard opens for an overlay editor
- **WHEN** an overlay-owned text field receives focus and the keyboard appears
- **THEN** the overlay SHALL keep its editing and confirmation actions reachable while the obscured route retains its prior content rectangle

#### Scenario: Overlay closes while keyboard or window geometry changes
- **WHEN** an input overlay closes or window insets change during its exit
- **THEN** input ownership SHALL transfer using the latest geometry without exposing an uncovered intermediate frame with stale insets

### Requirement: System-bar appearance follows the visible surface
On Android, the shared shell SHALL be the sole writer of system-bar icon appearance and SHALL resolve it from the final visible surface beneath each system bar rather than from a global theme flag. A feature-owned surface SHALL NOT independently mutate system-bar appearance.

#### Scenario: Dark surface beneath Android system bars
- **WHEN** a visible dark route or overlay extends beneath an Android system bar
- **THEN** the system-bar icon appearance SHALL remain legible against that final rendered surface

#### Scenario: Overlay does not cover a system bar
- **WHEN** a foreground overlay does not cover a system-bar region
- **THEN** that region SHALL retain the appearance appropriate to the still-visible underlying surface

### Requirement: Reduced-motion behavior follows platform settings
Application transitions SHALL respect the active platform reduced-motion setting without persisting a separate app preference. The setting SHALL affect motion presentation without changing route, playback, input, or settlement behavior.

#### Scenario: Reduced motion is enabled
- **WHEN** Android, iOS, or macOS reports reduced motion enabled
- **THEN** route and overlay transitions SHALL use the approved low-motion presentation while settling to the same state and safe-area geometry

#### Scenario: Platform reduced-motion setting changes
- **WHEN** the active platform reduced-motion value changes while RhythHaus is running
- **THEN** subsequent transitions SHALL use the latest value without an app restart or a separately stored RhythHaus preference
