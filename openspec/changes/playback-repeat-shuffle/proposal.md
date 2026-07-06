# Proposal: Playback Repeat and Shuffle Modes

## Summary

Add shared repeat and shuffle playback modes, then expose them as controls on the shared Compose `NowPlayingScreen`.

## Problem

RhythHaus currently advances automatically to the next queued track and stops at the end of the queue, while previous/next controls use ad-hoc wraparound logic in UI and shared controller paths. Users need explicit playback order controls: single-song repeat, playlist repeat, play only the current song then stop, play the current list then stop, and optional shuffled playback within the current list.

Without a shared playback mode model, these semantics would be duplicated or drift between UI buttons, platform media controls, and automatic completion.

## Goals

- Add repeat modes for single-track repeat, playlist repeat, play-current-song-then-stop, and play-current-list-then-stop.
- Add shuffle off/on modes for the current playback list.
- Make automatic completion and manual previous/next use one shared effective-order decision path.
- Preserve shared `PlaybackController` ownership of the queue and platform engines as single-current-item players.
- Add two controls to `NowPlayingScreen`: repeat mode cycle and shuffle toggle.
- Preserve visible library/list ordering when shuffle is enabled.
- Avoid new dependencies.

## Non-goals

- No persistence of repeat/shuffle settings.
- No system media notification, lock-screen, or hardware-command UI additions for repeat/shuffle controls.
- No mini-player `NowPlayingBar` repeat/shuffle controls.
- No saved playlist management.
- No platform-specific shuffle/repeat engines.
- No scanner, library database, artwork, metadata, navigation, or theme changes.
