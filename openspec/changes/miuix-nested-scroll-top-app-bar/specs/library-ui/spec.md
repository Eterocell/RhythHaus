## ADDED Requirements

### Requirement: Nested-scroll collapsed chrome uses Miuix TopAppBar

The Library nested-scroll collapsed top chrome SHALL render its toolbar/title content using Miuix `SmallTopAppBar` or `TopAppBar`, either directly or through `RhythHausTopAppBar`.

#### Scenario: Library home collapsed chrome renders via Miuix app bar

- **GIVEN** Library home is scrolled far enough for `NestedScrollBlurChrome` to appear
- **WHEN** the top overlay title is shown
- **THEN** the title area is rendered by a Miuix top app bar path
- **AND** the existing RhythHaus glass/backdrop overlay remains in use
- **AND** the top overlay still covers the status bar without leaving a solid seam

#### Scenario: Library drill-down collapsed chrome renders via Miuix app bar

- **GIVEN** an album or artist drill-down is scrolled far enough for `NestedScrollBlurChrome` to appear
- **WHEN** the top overlay title is shown
- **THEN** the title area is rendered by a Miuix top app bar path
- **AND** drill-down list behavior, back gesture behavior, and Now Playing bar behavior remain unchanged

#### Scenario: Library drill-down collapsed chrome includes back navigation

- **GIVEN** an album or artist drill-down is scrolled far enough for `NestedScrollBlurChrome` to appear
- **WHEN** the top overlay title is shown
- **THEN** the collapsed Miuix top app bar shows the drill-down title
- **AND** the collapsed Miuix top app bar provides the same back navigation action as the expanded header

#### Scenario: Library drill-down expanded header removes the former subtitle top-bar title

- **GIVEN** an album or artist drill-down is at the top of the list
- **WHEN** the expanded header is shown
- **THEN** the expanded header shows the back action and the large drill-down title
- **AND** it does not show the former static top-bar title such as track count or artist subtitle above the large title

### Requirement: Miuix scroll behavior drives drill-down title collapse

The Library drill-down track list SHALL use Miuix `MiuixScrollBehavior` / `ScrollBehavior.nestedScrollConnection` for the expanded large title to collapsed top-bar title transition.

#### Scenario: Drill-down list connects to Miuix scroll behavior

- **GIVEN** an album or artist drill-down is displayed
- **WHEN** the user scrolls the track list
- **THEN** the drill-down `LazyColumn` is connected to the Miuix scroll behavior via nested scroll
- **AND** the Miuix `TopAppBar` renders both the expanded large title and collapsed title states
- **AND** the collapsed Miuix top app bar includes the back navigation action

### Requirement: Existing home nested-scroll state semantics are preserved

The migration SHALL preserve the current `nestedScrollChromeStateFor(...)` activation semantics for Library home and SHALL preserve existing Library list scroll reporting unless explicitly changed by a future spec.

#### Scenario: Scroll progress tests remain valid

- **GIVEN** existing `LibraryNavigationTest` coverage for Library scroll behavior
- **WHEN** the home nested-scroll top chrome remains on the existing state-derived path
- **THEN** those tests still pass
- **AND** drill-down adoption of Miuix nested scroll does not break Library scroll reporting tests

### Requirement: TopAppBar wrapper remains backwards-compatible

If `RhythHausTopAppBar` is extended to support nested-scroll usage, existing Search and Settings call sites SHALL remain source-compatible and visually equivalent by default. Library drill-down header call sites MAY change when required by a scoped drill-down nested-scroll behavior correction.

#### Scenario: Existing compact top bars require no call-site behavior change

- **GIVEN** Search and Settings already call `RhythHausTopAppBar(title, onBack, modifier, subtitle)`
- **WHEN** the wrapper is extended
- **THEN** those calls continue to compile
- **AND** their default color, back icon, localized accessibility description, and inset behavior remain unchanged

### Requirement: Scope remains limited to shared Compose Library chrome

The migration SHALL NOT add dependencies, add `miuix-navigation3-adaptive`, alter route transitions, alter playback controls, alter Library rows, or touch platform-specific project files.

#### Scenario: Dependency and platform scope remains unchanged

- **GIVEN** the change is complete
- **WHEN** the scoped diff is reviewed
- **THEN** Gradle dependency files are unchanged
- **AND** `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` is not included
- **AND** changed source is limited to shared Compose UI and associated OpenSpec/Superpowers evidence
