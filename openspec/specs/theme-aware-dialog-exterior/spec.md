# Theme-Aware Dialog Exterior Specification

## Purpose

Define a shared active-palette panel and exterior/scrim policy for Haus dialog variants.

## Requirements

### Requirement: Theme-aware Haus dialog exterior
`HausDialog` and `HausLazyDialog` SHALL resolve their panel and exterior/scrim presentation from the active `HausColors` palette. The panel and exterior SHALL remain visibly appropriate to the active light or dark palette, and the dark exterior/scrim SHALL not appear light.

#### Scenario: Dark theme dialog exterior
- **WHEN** either dialog variant is visible with the active dark Haus palette
- **THEN** its panel SHALL use the active dark panel color and its exterior/scrim SHALL be a dark palette-derived dim rather than a light-looking exterior

#### Scenario: Light theme dialog exterior
- **WHEN** either dialog variant is visible with the active light Haus palette
- **THEN** its panel SHALL use the active light panel color and its exterior/scrim SHALL retain the restrained palette-derived ink dim

#### Scenario: Dialog variants share the exterior policy
- **WHEN** `HausDialog` and `HausLazyDialog` are evaluated with the same active palette
- **THEN** both variants SHALL resolve equivalent panel and scrim policy values
