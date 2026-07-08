## ADDED Requirements

### Requirement: Compose artwork rendering uses Coil-backed caching

Shared Compose artwork surfaces SHALL use Coil-backed image loading for embedded album art and project images where practical, instead of synchronously decoding artwork bytes in each call site.

#### Scenario: Track row artwork thumbnail
- **WHEN** a track row has embedded artwork bytes
- **THEN** the row artwork is rendered through a Coil-backed image loader with a thumbnail-sized request/cache role
- **AND** the existing fallback text remains visible when the artwork cannot be loaded
- **AND** selected-row overlays and click behavior remain unchanged

#### Scenario: Album and artist artwork surfaces
- **WHEN** album cards, artist rows, or drill-down top bars have representative embedded artwork bytes
- **THEN** those surfaces render the artwork through Coil-backed image loading
- **AND** repeated renders of the same artwork can reuse stable Coil cache keys
- **AND** surfaces without decodable artwork keep their existing fallback behavior

#### Scenario: Expanded Now Playing artwork
- **WHEN** the expanded Now Playing screen renders artwork
- **THEN** it uses a full/hero artwork request role rather than the thumbnail role
- **AND** the original `artworkBytes` remain available for platform media metadata and full-fidelity display

### Requirement: Koin owns app service construction

RhythHaus SHALL use Koin to construct and provide the existing app service graph instead of constructing those dependencies inline in `App()`.

#### Scenario: Shared app composition resolves services
- **WHEN** `App()` composes after platform startup
- **THEN** it resolves the playback controller, library repository, platform source access, library scanner, TagLib reader, metadata reader, and theme preference store from Koin
- **AND** `App()` does not directly instantiate those services inline
- **AND** the resolved services preserve the current process/app-lifetime behavior

#### Scenario: Platform startup initializes Koin
- **WHEN** Android, desktop, or iOS starts the shared UI
- **THEN** Koin is initialized before `App()` is rendered
- **AND** repeated initialization attempts do not crash previews or re-entry paths

### Requirement: Existing data and behavior remain compatible

The Coil/Koin change SHALL preserve existing persisted library data and playback behavior.

#### Scenario: No library schema migration
- **WHEN** the app is upgraded with the Coil/Koin change
- **THEN** no SQLDelight schema migration is required
- **AND** scanned track records continue to store original artwork bytes as before

#### Scenario: Playback behavior remains unchanged
- **WHEN** the user plays, pauses, seeks, skips, shuffles, or repeats tracks
- **THEN** playback behavior remains functionally unchanged
- **AND** platform media metadata can still receive original artwork bytes
