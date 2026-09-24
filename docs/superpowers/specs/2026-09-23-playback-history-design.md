# Playback History Design

## Intent

Add local, durable listening history so RhythHaus can show Recently Played and Recently Added without introducing network synchronization, recommendations, or a separate history-management workflow.

## Contract

A history record belongs to an existing library track and contains only `playCount` and `lastPlayedAtEpochMillis`. A controller generation is counted once only after it becomes `Playing`; loading, paused selection, errors, and repeated status callbacks do not count. A new admitted generation that actually plays counts again.

Recently Played shows tracks with a history record ordered by last-played descending. Recently Added uses the existing track creation timestamp descending. Both are flat Library lists: their visible ordering is their playback queue ordering, and existing selection and Back policies remain authoritative.

## Architecture

`track_play_history` is a separate SQLDelight relation foreign-keyed to `library_track`, so scan upserts cannot reset listener data and deleted tracks cascade cleanup. `LibraryRepository` owns atomic read/update operations. `PlaybackController` emits a deduplicated actual-play event keyed by current generation and occurrence. Shared consumes that event, records history off the UI thread, then re-publishes history through its existing serialized authoritative library publication owner.

The library implementation receives immutable history and created-time projections from Shared. It derives recent ordering in pure helpers and renders with the existing flat song-row path. No feature implementation depends on Shared or another feature implementation.

## Boundaries

Excluded: partial-play thresholds, skip classification, manual history deletion, charts, smart playlists, recommendation, sync, permission/access changes, or media-file/tag mutation.

## Verification

Database migration/restart/cascade/rescan coverage; controller generation deduplication and failure coverage; Shared concurrent scan/history publication coverage; library recent ordering/empty state/queue/selection/semantic coverage; EN/ZH resources; changed target compilation plus standalone formatting, static analysis, architecture, OpenSpec, and diff checks.