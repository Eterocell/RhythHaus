# Playback Session Settings Design

## Intent

Implement the roadmap's Output/session settings entry with truthful platform output status, user-controlled automatic-resume behavior, and a safe artwork-cache clear action. The OpenSpec proposal and delta define observable requirements.

## Decisions

- Current output is a read-only projection. Report a route name only when an OS API identifies the active route; otherwise display unavailable. Never imply that a connected-device list is the active route.
- Persist `AutomaticResumePolicy.FollowSystemRecommendation` (default) or `AutomaticResumePolicy.Never` in its own process-owned preferences file.
- iOS keeps the OS `shouldResume` bit as the resume authority. The policy can veto resumption but cannot manufacture it.
- Android tracks transient audio-focus loss only. Resume on subsequent gain only if playback was active before the loss, policy permits it, and generation/session plus an intent token remain current. Permanent loss and explicit user pause/stop/play/selection prevent a stale resume.
- macOS route-loss pause remains unchanged and never auto-resumes without an OS recommendation.
- Settings clears only Coil's active image loader memory/disk caches. No database, library or media mutation.
- Android reports output unavailable unless an active route can be proven through the current playback route API. iOS reads `AVAudioSession.currentRoute.outputs`; macOS queries the selected output name through a serialized native seam.

## Existing integration points

- Playback engine and iOS provider already handle interruptions and route changes.
- Android `RhythHausPlaybackService` owns `AudioFocusRequest`; its callbacks currently pause for all focus loss and record gain without auto-resuming.
- macOS route-loss detection owns Core Audio listeners and has callback quiescence constraints.
- Shared Settings composes neutral preferences through callback-first state projections.
- Artwork uses Coil `ImageLoader` caches through Compose image requests.

## Non-goals

Output selection, route reopening, resuming after permanent focus loss/route loss/process restart, cache quotas/size reporting, media or database cleanup, and generalized audio-session/ducking changes.

## Risks

- Android's existing ExoPlayer boundary may not expose active routed device. Truthful unavailable output is preferable to a false active-route claim.
- macOS output-name query must obey native callback/lifecycle serialization without weakening listener quiescence.
- Cache clearing may race active requests and be repopulated; document best-effort semantics.
- Resume intents race user controls and playback generations; tests must prove explicit intent always wins.
