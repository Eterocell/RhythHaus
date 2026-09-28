# Sorting and Filtering

## Why

The Library currently exposes fixed ordering and browse modes, but daily use requires finding tracks by title, artist, album, date, play count, favorite state, artwork availability, and source without changing persisted library data.

## What Changes

- Add a shared, immutable Library browse query containing sort key, direction, and filters.
- Support title, artist, album, added time, modified time, play count, and favorite-first sorting.
- Support favorite-only, artwork-present, and source filters.
- Keep query state in the current Shared Library shell lifetime; do not persist it or add database columns.
- Apply the same visible order to flat Home rows, selection reconciliation, and playback queue construction.
- Keep Albums and Artists grouped presentation semantics and their internal track order unchanged.
- Add English and Simplified Chinese labels and accessibility state descriptions for the controls.

## Capabilities

### New Capabilities

- `library-sorting-filtering`: deterministic Library sorting/filtering projections and accessible controls.

## Impact

- `feature:library:impl`: pure query model, projection helper, controls, labels, and tests.
- `shared`: query state and narrow source/creation/history projection wiring.
- No database schema, playback engine, scanner, platform permission, or media-file changes.
