# Sleep timer — approved autonomous design

## Product outcome

Now Playing offers one process-local sleep timer, with 15/30/45/60-minute stop or stop after current/3/5 naturally completed tracks. A ten-second fade can be enabled for either mode. The listener sees remaining time or tracks, can replace/cancel the timer, and does not lose the queue, repeat, shuffle, media or library state. The timer is not restored after app process restart.

## Cross-platform behavior

Elapsed time counts during pause and while Now Playing is closed; a timed deadline stops playback once. Natural track completions count exactly once for their authoritative generation, including RepeatOne, but skips/seeks/retries/queue restoration do not count. A final-track fade uses duration and position only when known; unknown duration completes and stops normally. The fade controls only in-app playback gain and is reversed on cancellation, replacement, stop, track switch, or the next playback start. Hardware volume is never changed.

## Architecture

`PlaybackController` owns tokenized timer state and generation-safe stop/completion decisions. It exposes immutable timer state and narrow commands. Each platform implements playback gain at the player/provider level: Android Media3 service, iOS Swift AVAudioPlayer through the serialized Kotlin bridge, macOS AVAudioPlayer through JNI. Shared DI/composition forwards controller state/actions to a feature-owned Now Playing panel, with English and Simplified Chinese resources and compact/split accessibility.

## Risks, verification, and ownership

The Android controller-to-service gain path and native bridge cancellation must be verified against real adapter behavior. Process termination clears the timer; background scheduling while audio plays needs user-led physical acceptance rather than claims based on compilation. The implementation owner runs automated tests and quality gates. The user performs manual device/UI/listening/system-control acceptance and reports results; until then those cases remain pending. Existing Shared baseline timeout and TagLib Android toolchain limitation are independently reported, not hidden.

## Artifacts

- OpenSpec change: `openspec/changes/sleep-timer/`.
- Executable plan: `docs/superpowers/plans/2026-09-28-sleep-timer.md`.

Direction is based on the user's standing authorization for autonomous specs/design/implementation direction; no separate design sign-off requested.
