# Output and session settings — design

## Intent

Implement the roadmap's Output and session settings entry as honest, platform-reported session information plus narrowly-scoped policy controls. See the change proposal for motivation.

## Current evidence

- Android `RhythHausPlaybackService` owns AudioFocusRequest. It pauses playback on focus loss and records focus gain, but does not resume. A transient-only pending resume flag must be invalidated by user transport/selection commands; permanent focus loss must never resume.
- iOS `RhythHausAudioPlayerProvider` receives AVAudioSession interruption and route-change notifications. `PlaybackEngine.ios.kt` resumes only when the OS interruption-ended event includes `shouldResume` and the session had been playing.
- macOS native playback already observes route snapshots and pauses on active output-device loss. It has no interruption-resume event.
- `ArtworkImage` uses Coil memory and disk cache policies. Settings currently has separate controls for ReplayGain, crossfade, and gapless.

## Goals / Non-Goals

**Goals:**
- Show a current output description only from a platform API that identifies the active route; otherwise show an explicit unknown/unavailable state.
- Persist an automatic-resume policy with default `FollowSystemRecommendation`; `Never` suppresses system-triggered resumption on all hosts.
- On Android, resume after transient focus loss only if playback was active before loss, focus is regained, policy allows it, and no user command superseded the pending resume. Do not resume after permanent focus loss.
- Preserve iOS `shouldResume` as the authority when policy is `FollowSystemRecommendation`; never auto-resume when policy is `Never`.
- Keep macOS route loss paused; the policy does not invent a resume recommendation where none exists.
- Clear only Coil image memory and disk caches through an explicit Settings action; do not delete source media, database metadata, or library rows.
- Report cache-clear success or recoverable failure to the user.

**Non-Goals:**
- Choosing or changing audio outputs.
- Resuming after route disconnect, permanent Android focus loss, app relaunch, or process restoration.
- Cache size reporting, cache quotas, or clearing databases/library artwork metadata.
- Generalized audio-session configuration, ducking changes, or focus-request redesign.

## Decisions

1. **Platform output projection.** Add a playback-owned immutable output status projection with `Available(name)` and `Unavailable(reason)` states. iOS queries `AVAudioSession.currentRoute.outputs`; macOS queries the native active/default output device through a serialized JNI seam. Android reports the active route only if the service can retrieve it from the active AudioTrack; otherwise report unavailable rather than presenting a list of connected devices. Refresh on platform route/device callbacks and when the Settings surface opens.
2. **Shared policy, native enforcement.** Shared owns and persists the enum `AutomaticResumePolicy { FollowSystemRecommendation, Never }`, injects it into the platform session owner, and Settings observes authoritative stored state. Native host controls deciding whether to call the existing controller-backed play path. Explicit transport or selection commands invalidate pending system-resume intent.
3. **Android focus transition.** Track transient focus loss separately from permanent loss. Set a pending-resume token only if controller playback was active at the loss. On focus gain, consume the token once and resume only if policy remains FollowSystemRecommendation and its playback generation is still current. User pause, stop, play, selection, or controller release invalidates it.
4. **Cache ownership.** Settings uses the active Coil `ImageLoader` to clear `memoryCache` and `diskCache`; the action runs off the UI thread where disk operations require it. Cache clearing is independent of repository and scanner lifecycle.
5. **Preference isolation.** Store policy in a dedicated process-owned preferences file/key, following ReplayGain/crossfade/gapless preference owner patterns. Cache clearing is an action, not persisted state.

## Risks / Trade-offs

- Android may not expose the currently routed output through the existing Media3 service abstraction. If no reliable active-route API exists for supported Android versions, the platform reports unavailable; do not substitute a list of connected devices.
- macOS route-name JNI work must preserve existing listener lifecycle/quiescence guarantees and must not invoke Core Audio from playback callbacks unsafely.
- Cache clear can race active artwork requests. Clearing both caches is best-effort; an in-flight request may repopulate an entry immediately, but source data and playback remain unaffected.
- Automatic resume races with explicit user intent and session replacement; generation/token fencing and command-supersession tests are required.
