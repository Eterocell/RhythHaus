## Why

The player accepts queues the preference codec cannot save. Progress writes scale with queue length, and Android folder traversal, album composition and Search cover loading scale or behave differently than their consumers expect. Seeking and CI omit supported input and verification paths.

## What Changes

Implement the eight fixes approved on 2026-11-18. Use the existing SQLDelight database for the full playback queue, import old preferences once, and keep a position-only update bounded. Align album identity across Library and CarPlay, load artwork by viewport, expose adjustable seeking, index SAF lyric sidecars per directory and gate existing tests in CI.

## Impact

Core Database, Core Playback, Library, Search, Now Playing, Shared, Android SAF, CarPlay and convention/CI tests. No new dependencies, platform targets, autoplay, network services or media-file edits. This change does not close the Studio manual acceptance matrix.
