# Change: Miuix TopAppBar Migration

## Why

RhythHaus still uses hand-rolled `BackChip` + `Row` title bars on ordinary shared Compose screens even though Miuix provides `SmallTopAppBar` / `TopAppBar` for standard page chrome. Replacing these ordinary top bars continues the Miuix component migration and reduces bespoke layout code.

## What Changes

- Add a small shared RhythHaus wrapper around Miuix `SmallTopAppBar` for ordinary back/title screen chrome.
- Replace Settings and Search custom back/title rows with the wrapper.
- Replace the Library drill-down header's custom back/subtitle row with the wrapper while preserving the large page title below it.
- Preserve route-level layout, insets, search behavior, settings behavior, library drill-down behavior, nested-scroll glass chrome, and Now Playing behavior.

## Non-goals

- No full app redesign.
- No playback, scanner, database, route-stack, native platform, or adaptive-shell rewrite.
- No migration of nested-scroll glass chrome, Now Playing, artwork/equalizer visuals, scrubber gestures, or blur/backdrop wrappers.
- No new Miuix dependencies and no `miuix-navigation3-adaptive`.
