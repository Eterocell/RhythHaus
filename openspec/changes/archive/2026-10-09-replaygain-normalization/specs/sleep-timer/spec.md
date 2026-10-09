# Spec Delta

## MODIFIED Requirements

### Requirement: Optional playback-only fade-out

The listener MAY enable a ten-second fade before either stop boundary. Fade SHALL attenuate only the app's playback gain, not hardware or system volume. Its attenuation SHALL multiply any active normalization factor. Cancellation, rearming, track changes and stopping SHALL restore the sleep attenuation to unity rather than overriding normalization; subsequent playback SHALL use the current track's normal effective gain.

#### Scenario: Timed fade
- **WHEN** fade is enabled and ten seconds or less remain until a timed stop while audio is playing
- **THEN** sleep attenuation decreases toward silence until stop and multiplies the current normalization factor.

#### Scenario: Final track has known duration
- **WHEN** fade is enabled, the current track is the final counted completion, and its remaining playable duration is at most ten seconds
- **THEN** sleep attenuation decreases toward silence until that natural completion.

#### Scenario: Unknown duration or sudden completion
- **WHEN** remaining track duration is unavailable or completion occurs before a fade can finish
- **THEN** the natural completion still stops playback without waiting for an invented duration.

#### Scenario: Fade interrupted
- **WHEN** fade is cancelled, a different timer is armed, the track changes, or playback stops
- **THEN** sleep attenuation returns to unity for subsequent playback
- **AND** application gain still includes the current track's normalization policy before new playback begins.

#### Scenario: Normalization changes during fade
- **WHEN** the listener changes normalization while a fade is active
- **THEN** the new normalization factor combines with the current sleep attenuation
- **AND** timer mode, remaining count or deadline and stop ownership remain unchanged.
