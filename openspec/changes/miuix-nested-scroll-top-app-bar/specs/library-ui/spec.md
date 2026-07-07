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

### Requirement: Existing nested-scroll state semantics are preserved

The migration SHALL preserve the current `nestedScrollChromeStateFor(...)` activation semantics and existing Library list scroll reporting unless explicitly changed by a future spec.

#### Scenario: Scroll progress tests remain valid

- **GIVEN** existing `LibraryNavigationTest` coverage for Library scroll behavior
- **WHEN** the nested-scroll top chrome is migrated
- **THEN** those tests still pass
- **AND** no Miuix nested scroll connection is introduced that changes list scroll consumption in this change

### Requirement: TopAppBar wrapper remains backwards-compatible

If `RhythHausTopAppBar` is extended to support nested-scroll usage, existing Search, Settings, and `DrillDownHeader` call sites SHALL remain source-compatible and visually equivalent by default.

#### Scenario: Existing compact top bars require no call-site behavior change

- **GIVEN** Search, Settings, and `DrillDownHeader` already call `RhythHausTopAppBar(title, onBack, modifier, subtitle)`
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
