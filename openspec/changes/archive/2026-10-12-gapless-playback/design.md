# Design

## Context

The controller already owns queue traversal, repeat/shuffle order, playback generations, playback runs, completion admission, ReplayGain, sleep attenuation, crossfade, and failure recovery. Platform engines already support staged incoming voices for crossfade on Android, iOS, and macOS. Ordinary playback still loads a successor only after the current completion path settles. Gapless should reuse these authority boundaries rather than add a second queue or platform-specific controller.

## Goals

- Remove the ordinary decoder teardown/load gap for eligible consecutive local tracks.
- Reuse the existing controller generation/run/occurrence ownership and recovery semantics.
- Keep crossfade as the sole owner when its window is enabled and eligible.
- Preserve one logical media session and current system-control behavior.
- Keep the feature local-only, with no schema migration for library media.

## Non-goals

- Crossfade tuning or a new fade curve.
- Audio format transcoding, resampling, or a new decoder dependency.
- Network/streaming gapless guarantees.
- Editing media files or changing system output volume.
- Guaranteeing zero gap on a platform/decoder that cannot stage the next file; such cases remain ordinary supported playback behavior and are observable in acceptance evidence.

## Decisions

1. **One controller policy.** Add a process-owned persisted `GaplessPlaybackEnabled` preference and controller field. Restore it before session restoration. The policy is independent of crossfade but crossfade eligibility wins whenever both are enabled.
2. **Reuse the staged-voice seam.** Introduce a controller-owned gapless transition state using the existing `prepareIncoming`/`startIncoming`/`finishCrossfade` style boundary, but with no overlap gain: outgoing remains at its effective gain and incoming starts at its effective gain only at the completion boundary. Platform engines may use a decoder/player-specific queue or a prepared second voice behind the same contract.
3. **Preload threshold.** Prepare when remaining duration is within a conservative platform-neutral lead window, chosen as a controller constant and bounded by known current/successor durations. A successor is not prepared when durations are unavailable, crossfade owns the transition, or the queue has no eligible next occurrence.
4. **Completion ownership.** Natural completion of the outgoing generation atomically promotes the prepared successor if its token/run is still current. If preparation is not ready, fall back to ordinary completion rather than blocking the callback thread. Duplicate completion events are fenced by generation, run, and occurrence identity.
5. **Explicit command cleanup.** Selection, seek, stop, clear, pause, and skip cancel or retire gapless preparation through the same locked cleanup path as crossfade. Cleanup preserves the original cancellation exception and cannot restore an older source after a newer selection commits.
6. **Policy composition.** The promoted voice receives the controller’s current effective ReplayGain and sleep attenuation. Gapless does not alter timer completion accounting: only the authoritative natural completion is charged, and the outgoing completion is not double-counted during promotion.
7. **Platform fallback.** Android Media3, iOS AVAudioPlayer, and macOS native bridge each implement the narrow staging/promotion contract. If native preparation fails, the controller reports the successor failure under the existing structured playback error path and leaves the outgoing state authoritative until recovery settles it.
8. **Testing boundary.** Add policy/preload/completion race tests in Core; platform host/provider/native tests for two-file promotion; preference and Settings semantic tests; and a temporary WAV integration smoke proving both files retain their hashes and the successor begins without an idle state. Physical audible verification remains user-owned.

## Risks and mitigations

- **Decoder-specific gap:** report platform acceptance separately; do not claim universal zero-gap from controller tests.
- **Crossfade interaction:** make crossfade eligibility checked before gapless admission and add a no-duplicate-load regression.
- **Completion races:** carry captured generation, run, token, and occurrence through preparation and promotion; test stale callbacks after explicit selection and failed preparation.
- **Sleep timer double charge:** reuse the existing completion admission key and test one completion per authoritative occurrence.
