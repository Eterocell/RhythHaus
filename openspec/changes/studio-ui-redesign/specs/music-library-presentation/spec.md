# Music library presentation delta

## Purpose

Define content-first browsing and search presentation that makes every existing local-library view and track action discoverable while preserving authoritative ordering, selection and metadata behavior.

## ADDED Requirements

### Requirement: Content-first Library home
A nonempty Library SHALL begin with a compact title/count, view and query controls, then music content. It MUST NOT insert a large brand header, persistent import card, duplicate queue heading or full-width playlist navigation before music. At 360×640dp and default text scale the first album row MUST be visible. Empty-library import MUST retain the real platform policy.

#### Scenario: Browse existing music
- **WHEN** a nonempty Library opens in a compact viewport
- **THEN** music MUST be immediately discoverable rather than displaced by onboarding or secondary navigation

### Requirement: Six legible browse views
Albums, Songs, Artists, Favorites, Recently Played and Recently Added SHALL remain at most two activations away. Compact layouts MUST show Albums/Songs/Artists and a named Favorites-and-recent chooser; desktop expanded navigation MUST expose the named collection views. Selected titles MUST identify the current view. Album grids MUST adapt from 128dp compact or 160dp wide minimum card widths rather than shrink labels.

#### Scenario: Open a recent collection
- **WHEN** the listener selects Recently Played or Recently Added through compact or wide navigation
- **THEN** the correct authoritative projection and existing deterministic order MUST appear under its complete view name

### Requirement: Discoverable applicable filtering
Each view SHALL retain its existing applicable sort keys/directions and favorite, artwork and source filters. Active criteria MUST be named, individually removable and clearable together. Existing filtering-before-sorting and recent/default ordering MUST remain unchanged; clearing a filter MUST NOT mutate library membership.

#### Scenario: Filter produces no matches
- **WHEN** active criteria exclude every track while the library contains music
- **THEN** the page MUST explain the active filters and offer clearing them, not recommend importing or claim files are missing

### Requirement: Consistent rows and page-scoped actions
Home, detail and Search SHALL share song-row visual hierarchy without sharing domain ownership. Primary activation MUST play the exact visible ordered sequence; selection activation MUST toggle once without playback. Existing eligible pages MUST retain non-gesture selection, favorites and now-playing indications. Metadata editing MUST remain reachable through a named secondary menu; editing MUST retain scan-original versus app-local override, draft restore, cancellation and validation semantics.

#### Scenario: Open and cancel metadata editing
- **WHEN** the listener opens a row menu, edits or restores metadata, then cancels
- **THEN** no override MUST be saved and the row MUST retain its authoritative identity and values

#### Scenario: Select from a filtered detail
- **WHEN** a row is activated in selection mode or selected rows are submitted
- **THEN** activation MUST toggle only once without playback and submission MUST retain the owning page's visible ordering

### Requirement: Search remains local and query-driven
Search SHALL begin with its query field and local-field guidance. Blank/whitespace queries MUST show no results; matching MUST remain case-insensitive over title, artist and album with existing order and duplicate occurrences. Nonblank searches MUST show count or query-specific no-match guidance and a clear action; query updates MUST reconcile selection through the existing owner.

#### Scenario: Clear an unsuccessful query
- **WHEN** the listener clears a query with no matches
- **THEN** the query/no-match state MUST clear without showing all tracks or initiating scan/import

### Requirement: Distinct content and source states
Library empty, empty collection, filtered empty, active scan, unavailable source and terminal scan failure/cancellation SHALL be visually and semantically distinct. Nonempty scanning MUST keep music usable with compact progress. Missing artwork/tags MUST use existing truthful local fallbacks; unavailable source MUST NOT be portrayed as deleted media.

#### Scenario: Scan fails after a successful scan
- **WHEN** a current scan fails or is cancelled after an earlier completed scan
- **THEN** the current terminal state and the separately identified previous completed report MUST remain distinguishable, with recovery and detailed reports delegated to Settings
