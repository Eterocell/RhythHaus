# Design

## Context
The Queue tab is rendered by `PlaylistHubScreen` in `:feature:playlists:impl`; its `queueTabPresentation` already defines current plus upcoming items. `PlaybackState.queue` may contain an already-played prefix. Shared owns the `PlaybackController` and forwards repository mutations through `PlaylistStateOwner.mutate`. `PlaylistRepository.createWithEntries` is atomic, preserves duplicates and existing naming rules. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:** save exactly the queue shown at action time with an accessible localized affordance and feedback, without changing playback; reuse existing transaction and publication sequencing.

**Non-Goals:** modifying queue or playback; new database schema, playlist entry editing, append-to-existing workflow, or smart playlist rules.

## Decisions

1. `PlaylistHubScreen` owns an independent save-queue name draft carrying the immutable ordered track IDs captured from `queueTabPresentation(playbackState)` when Save is clicked. Dialog confirmation sends the captured IDs, not the recomposed queue. The Saved-tab create dialog remains unchanged. Both modal drafts participate in feature dismissal registration; one modal is visible at a time.
2. Shared's `LibraryRoute.PlaylistHub` adapter supplies `onSaveQueueAsPlaylist(name, trackIds, onOutcome)`, forwarding to `onPlaylistMutation({ createWithEntries(name, trackIds) }, onOutcome)`. The existing `PlaylistStateOwner` serialization and reducer publish the new playlist; no duplicate persistence path.
3. Use `PlaylistNameDialog` and `playlistNameModalPresentation` for validation and failed-mutation feedback. On success close the modal; on failure retain both name draft and frozen track list. Only render Save while `queueTabPresentation` has a selected current item.
4. EN/ZH strings are owned by `:feature:playlists:impl`. Avoid claiming a save succeeded before the authoritative snapshot confirms.

## Risks / Trade-offs
A track removed after the snapshot can fail its foreign-key insert; existing atomic transaction leaves no partial playlist, and the modal stays open with error. Keeping the snapshot after a queue change is intentional and avoids a misleading mixed playlist. The Queue tab must not expose an inert action to accessibility on empty/unselected queues. Use UI tests for compact 600×400 dp reachability and Shared integration for controller immutability.
