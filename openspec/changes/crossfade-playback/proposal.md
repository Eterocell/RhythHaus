# Proposal: Crossfade playback

## Why

Natural queue transitions currently stop one native audio player before loading another. Add opt-in genuine overlap without changing library content or system volume and without defeating ReplayGain, sleep timers or controller selection ownership.

## What Changes

- Persist Off/3s/5s/8s settings through Shared's process-owned preference authority.
- Prepare and play a successor concurrently on each first-platform engine, with linear per-player envelopes and per-track ReplayGain.
- Controller owns prepared/overlapping/retiring generations and exact queue occurrences; natural completion and actual playback history remain single-counted.
- Cancel staging or retire outgoing voices on incompatible transport, queue, sleep-timer or lifecycle changes.
- EN/ZH Settings and isolated regression/runtime acceptance.

## Capabilities

### New Capabilities
- `crossfade-playback`: natural dual-player transitions, lifecycle and persisted preference behavior.

### Modified Capabilities
- `sleep-timer`: active timers suppress crossfade; arming settles an active overlap before counting future completions.

## Impact

`:core:playback` controller and Android/iOS/JVM adapters; native macOS helper; iOS Swift provider; Shared preference/DI/startup; Settings. No database schema change, media writes, dependency/toolchain change, Windows/Linux support or background watcher.
