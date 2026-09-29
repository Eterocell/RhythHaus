# Tasks

## 1. Queue snapshot and feature presentation

- [ ] 1.1 Add a Queue-tab save action and frozen current-plus-upcoming snapshot draft; regression tests cover duplicates, historical-prefix exclusion, empty/unselected queue, and queue changes during naming.
- [ ] 1.2 Reuse the name modal for queue creation with EN/ZH copy, accessible compact layout, cancellation, failure retention, and success dismissal tests.

## 2. Shared authoritative mutation

- [ ] 2.1 Wire the Queue-tab callback to `PlaylistStateOwner.mutate { createWithEntries(name, trackIds) }` through Shared; integration tests prove durable ordered entries and playback state unchanged.

## 3. Acceptance

- [ ] 3.1 Run focused Library/Shared tests, desktop/iOS compilation, formatting, static analysis, architecture and OpenSpec validation; review diff and document exact environment blockers.
- [ ] 3.2 Sync the saved-playlists spec, archive this change, update roadmap/progress, and commit after reviewed verification; record user-owned manual acceptance separately.
