# Proposal

## Why

RhythHaus currently advances ordinary queues only after the active player reports completion. That callback-driven handoff can expose a silence gap between consecutive tracks, especially on short files and platform decoders with teardown latency. Gapless playback should keep the next supported occurrence prepared before the outgoing voice ends and transfer playback without audible silence, while preserving the existing queue and crossfade safety model.

## What Changes

- Add a persisted, opt-in gapless playback policy with Off and On states.
- Prepare the next queue occurrence before natural completion when gapless is enabled and crossfade is disabled.
- Promote a prepared successor at the outgoing end boundary without clearing the logical queue or waiting for a fresh ordinary load.
- Preserve repeat, shuffle, sleep-timer, ReplayGain, playback-generation, playback-run, failure-recovery, and explicit-transport semantics.
- Make crossfade authoritative when both policies are enabled; gapless must not create a competing second handoff.
- Add platform engine seams and native/host regressions for Android, iOS, and macOS.
- Add localized Settings controls and persistence/lifecycle coverage.

## Capabilities

### New Capabilities

- `gapless-playback`: User-visible gapless successor preparation and zero-gap natural queue advancement.

### Modified Capabilities

- `crossfade-playback`: Crossfade takes precedence over gapless when both are enabled, and policy changes cancel stale gapless preparation safely.
- `playback-failure-recovery`: Prepared-successor failures remain queue-level failures and must not corrupt the outgoing occurrence or remove media.

## Impact

- `:core:playback` controller and platform engine contracts.
- Android Media3 service/player ownership and transport protocol.
- iOS AVAudioPlayer provider handoff.
- macOS native audio bridge and JVM engine.
- Shared Settings preference ownership and Now Playing/playback state only where necessary.
- No database schema or media-file changes.
