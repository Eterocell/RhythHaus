## ADDED Requirements

### Requirement: Settings information routes suppress the Now Playing bottom bar
The shared application shell SHALL omit the Now Playing bottom bar while the current route is Settings, About, or Open Source Libraries, regardless of whether a track is loaded.

#### Scenario: Settings is current
- **WHEN** the current route is `LibraryRoute.Settings`
- **THEN** the application shell does not show the Now Playing bottom bar

#### Scenario: About is current
- **WHEN** the current route is `LibraryRoute.SettingsAbout`
- **THEN** the application shell does not show the Now Playing bottom bar

#### Scenario: Open Source Libraries is current
- **WHEN** the current route is `LibraryRoute.OpenSourceLibraries`
- **THEN** the application shell does not show the Now Playing bottom bar

### Requirement: Other routes retain existing bottom bar behavior
The shared application shell SHALL permit the Now Playing bottom bar on every existing route outside the settings-information group, subject to the existing visibility state.

#### Scenario: Existing visibility state hides the bar
- **WHEN** the current route permits the bar and the existing visibility state is hidden
- **THEN** the application shell does not show the Now Playing bottom bar

#### Scenario: Returning from a suppressed route
- **WHEN** navigation leaves Settings, About, or Open Source Libraries for a route that permits the bar
- **THEN** the application shell follows the unchanged existing visibility state

### Requirement: Route suppression does not alter playback
Suppressing the Now Playing bottom bar SHALL be a presentation-only behavior and SHALL NOT stop, pause, replace, or otherwise mutate playback or its queue.

#### Scenario: Playback continues on a suppressed route
- **WHEN** playback is active and the user navigates to Settings, About, or Open Source Libraries
- **THEN** playback state and queue remain unchanged while the bar is omitted
