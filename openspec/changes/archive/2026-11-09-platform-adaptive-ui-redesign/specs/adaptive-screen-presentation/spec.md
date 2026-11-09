# Spec Delta

## Purpose

Defines complete, readable, and accessible presentation of RhythHaus's existing feature routes across compact, wide, and platform-specific surfaces without moving their business-state ownership.

## ADDED Requirements

### Requirement: Adaptive redesign covers every existing route family
The adaptive presentation SHALL cover Library's six views and details, Search, selection and metadata editing, static and smart playlists and rules, playlist documents, Now Playing and its queue/lyrics/timer/failure surfaces, Settings, source and scan management, onboarding, About and licenses, and desktop-drop feedback. Existing feature requirements remain authoritative for each surface's actions and state.

#### Scenario: Route inventory is rendered
- **WHEN** a user opens any existing in-scope route or foreground surface
- **THEN** RhythHaus SHALL render its named adaptive presentation without substituting a different route or omitting that surface from the redesign

#### Scenario: Feature state is authoritative
- **WHEN** a route shows empty, loading, success, failed, cancelled, selected, or active content
- **THEN** its visible content and available actions SHALL reflect the owning feature's current state and SHALL NOT claim progress, data, or capability that the feature has not reported

### Requirement: Common visual hierarchy uses the shared semantic system
Redesigned routes SHALL use shared Haus semantic color, type, spacing, and shape roles. Artwork is focal identity; track lists remain continuous rows; cards group related sections without replacing navigation or actions. Tokens MAY grow with system text but SHALL NOT shrink to hide content. Features SHALL distinguish selection, error, and focus by localized state or shape, not color alone, and SHALL NOT add a parallel palette.

#### Scenario: Approved token values
- **WHEN** a redesigned route renders at its default system text size
- **THEN** base spacing SHALL use 4/8/12/16/24/32/48dp steps; title, compact title, section, body, secondary, and auxiliary roles SHALL use 28/34sp, 24/30sp, 18/24sp, 16/22sp, 14/20sp, and 12/16sp respectively; artwork, controls, groups, and overlays SHALL start at 8/10/12/20dp corner radii

#### Scenario: Resolved theme changes while a feature route is open
- **WHEN** the resolved Haus scheme changes while a route, query, selection, or editor is active
- **THEN** the route SHALL render the same state using the new semantic roles without navigating, losing its draft, or replacing its owner

#### Scenario: Feature uses list and artwork hierarchy
- **WHEN** a Library, Search, Queue, or playlist surface renders a sequence of tracks
- **THEN** it SHALL retain continuous readable rows and use artwork as identity without adding a heavy card around each row

### Requirement: Screen presentation adapts without changing feature behavior
The responsive presentation SHALL reorganize existing feature content for the available window while preserving current route semantics, visible ordering, feature-owned state, and supported actions. It SHALL NOT add a second navigation, storage, scanning, playlist, or playback authority.

#### Scenario: Resize while a route is active
- **WHEN** the window crosses a supported compact, rail, or sidebar layout boundary while a feature route is active
- **THEN** the route SHALL adapt its presentation without converting resize into navigation or discarding its route-owned query, sort/filter, scroll, or draft state

#### Scenario: Feature-owned operation is invoked
- **WHEN** a user invokes an existing route action such as play, edit, reorder, import, recover, or confirm
- **THEN** the action SHALL continue through its existing feature or Shared owner and retain its existing outcome and failure semantics

### Requirement: Responsive text and content remain reachable
In-scope surfaces SHALL use the shared adaptive layout and semantic theme contract and remain usable at the approved compact, landscape, tablet, and desktop sizes. English and Simplified Chinese text SHALL wrap or scroll rather than disappear; required actions and list tails SHALL remain reachable at the platform's default and 200% text scale.

#### Scenario: Compact and wide layout
- **WHEN** an in-scope route is displayed at an approved compact size or at a rail/sidebar width
- **THEN** it SHALL use the matching approved layout, avoid unintended horizontal overflow, and keep its primary content and actions reachable

#### Scenario: Long localized or enlarged text
- **WHEN** the locale is English or Simplified Chinese and system text size is increased to 200%
- **THEN** titles, descriptions, rows, dialogs, and action labels SHALL remain readable and required actions SHALL remain reachable by scrolling or responsive reflow

### Requirement: State and input semantics remain accessible
Interactive presentations SHALL expose localized names, roles, and states for visible controls and support task-ordered keyboard/focus behavior. Gestures with assigned actions SHALL have a named alternative; hidden or occluded content SHALL expose no interaction semantics. Android targets SHALL be at least 48dp; iOS targets, at least 44pt; desktop controls SHALL remain operable by pointer and keyboard.

#### Scenario: Assistive technology traverses a visible surface
- **WHEN** keyboard or assistive technology focuses an in-scope surface
- **THEN** focus SHALL follow task order and visible controls SHALL expose their localized purpose and selected, disabled, loading, or error state without relying on color alone

#### Scenario: Surface becomes inactive
- **WHEN** a route or control is hidden, covered, or exiting
- **THEN** it SHALL expose no pointer, gesture, keyboard, click, or accessibility action until it is the active surface again

#### Scenario: Platform hit targets and desktop input
- **WHEN** an interactive control is rendered on Android, iOS, or macOS
- **THEN** its platform target SHALL meet the stated touch minimums or provide the desktop keyboard/focus and pointer affordances without reducing access to its action
