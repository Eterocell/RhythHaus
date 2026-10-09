# Crossfade playback

## Decision and scope

Implement true overlapping playback on Android, iOS and macOS. Default Off; persisted choices Off, 3 seconds, 5 seconds and 8 seconds. Only natural traversal to a different upcoming queue occurrence is eligible. No overlap for Repeat One, Stop After Current, queue tail without a successor, unknown/nonpositive duration, or an active sleep timer. Repeat Playlist may overlap its effective last/first occurrences; a single-occurrence queue never overlaps itself. Manual Next/Previous, Retry, seek and restored sessions do not initiate a fade.

This is not gapless playback, a loudness analyser, serial fade-out/fade-in, or a second system media session. Existing ReplayGain remains attenuation-only. Media files, tags, source permissions and system volume are unchanged.

## Audible and logical handoff

Prepare the upcoming occurrence while the outgoing player continues. Preparation must not publish current-track changes, playback history or session checkpoints. Start a linear overlap only after preparation succeeds and both positive durations are known. The effective window is min(configured duration, outgoing duration / 2, incoming duration / 2); late preparation shortens it to the outgoing time remaining. Do not overlap if less than 250 ms remains.

During overlap, outgoing envelope = 1 - progress and incoming envelope = progress. Each is multiplied by that track's ReplayGain factor; neither exceeds unity. Incoming starts at zero. Progress uses advancing playback time, not wall time while paused. At incoming confirmed Playing, atomically promote its existing queue occurrence/generation/run to logical current; publish exactly one actual-play event and checkpoint. Keep the outgoing player only as a retiring voice, never as an independently selectable queue item. Its completion cannot advance the queue a second time. System metadata and seek target are incoming from promotion onward.

## Lifecycle

Pause pauses both players and freezes envelopes; resume applies both retained gains before either starts. Seeking, manual transport/selection, queue replacement, error recovery, reconciliation removing either member, disabling crossfade, and arming a sleep timer terminate the outgoing voice and retain the authoritative current incoming voice when already promoted. Before promotion, cancel preparation and retain outgoing current. Stop/clear/release invalidate both players and all staged callbacks. No old transition can stop, change volume or replace a newer selection.

A sleep timer already active excludes new overlap. Arming during overlap retires outgoing before applying the timer. This preserves after-current/N natural-completion semantics without counting a retiring voice toward a newly armed timer.

Preparation failure leaves outgoing playback unchanged and falls back to the existing ordinary natural transition. Incoming failure after promotion reports the normal structured error for incoming and retires outgoing; it does not silently return to a different occurrence. Failed preparation must not repeatedly retry every progress tick; retry only when the candidate or current playback run changes.

## Ownership and settings

One controller owns transition token, outgoing generation/run/occurrence, staged incoming generation/run/occurrence, loaded durations and envelope progress. Platform adapters own two native players under one audio-focus/route/session authority. Events carry their captured generation/run; prepared media must be callback-isolated until admitted. Preference owner has process lifetime and applies the durable accepted duration before startup transport is enabled, following ReplayGain. Settings uses a platform-neutral projection, EN/ZH radios, busy/error state and modal-inert semantics.

## Acceptance

Prove simultaneous real native playback with disposable audio, not just gain calls. Prove correct effective successor under shuffle/repeat/duplicates, per-track ReplayGain, pause/resume envelopes, history/checkpoints once, natural completion once, staged failure, late preparation and cancellation races. Recreate UI during an admitted write and delay preference restore: UI, persistence and controller remain consistent and playback stays gated. Physical listening/background/headset/system control acceptance belongs to the user and is not inferred from host tests.
