# Sorting and Filtering Design

RhythHaus's next Phase 2 daily-use change adds ephemeral Library Home sorting and filtering. The active query is shared-owned state, not persisted user data. It filters by favorite membership, artwork availability, and authoritative source ID, then sorts by title, artist, album, added time, modified time, play count, or favorite status with a reversible direction and deterministic tie-breakers.

The feature stays within the existing Shared-to-Library projection boundary. Shared supplies immutable source and metadata maps already available from `LibraryTrack`; the Library feature owns the pure query helper and controls. Favorites, Recently Played, and Recently Added retain their membership semantics. Albums and Artists remain grouped with their existing detail order. The projected flat order is the only order used for visible rows, selection reconciliation, and playback queues. Query state resets with a new Library shell and never changes persistence, media, sources, playback, repeat, shuffle, or progress.

Verification covers projection correctness, missing metadata, mode precedence, grouping, selection invalidation, accessible localized controls, passive filtered-empty behavior, focused KMP compilation/tests, and existing quality/specification gates.
