# Sleep Timer Specification

## Purpose

Give the listener a session-local, cross-platform sleep timer that ends playback after elapsed time or a chosen number of naturally completed tracks, with an optional gentle fade that does not change system volume or the music library.

## Requirements

### Requirement: Arm, replace, and cancel one sleep timer

The application SHALL expose at most one active sleep timer for the current process and SHALL show its current mode, remaining time or completions, and fade selection in Now Playing. The listener SHALL be able to cancel or replace it without changing playback queue, repeat, shuffle, media, library, or source state. A timer SHALL NOT be restored across process restart.

#### Scenario: Arm timed stop
- **WHEN** the listener selects a positive time interval
- **THEN** the timer counts elapsed real time, including when playback is paused or the Now Playing overlay is closed
- **AND** its expiration requests playback stop exactly once
- **AND** the current queue and repeat/shuffle selections remain intact

#### Scenario: Arm track-count stop
- **WHEN** the listener selects stop after current or after N tracks, where N is positive
- **THEN** only actual natural completions of the playing occurrence reduce the remaining count
- **AND** reaching zero stops playback before advancing to another occurrence, regardless of repeat mode

#### Scenario: Non-completion playback events
- **WHEN** playback is manually skipped, changed, retried, restored, errored, paused, or restarted without a natural completion
- **THEN** the remaining natural-completion count does not decrease

#### Scenario: Replace and cancel
- **WHEN** the listener replaces or cancels an armed timer
- **THEN** the previous deadline or completion count can no longer stop playback
- **AND** cancellation restores normal playback gain without changing the current media or playback state

#### Scenario: No current media or process termination
- **WHEN** the queue is cleared or the playback controller is released
- **THEN** the active timer is cancelled and playback gain is restored as applicable
- **AND** a new process starts with no armed timer

### Requirement: Safe expiration under playback changes

The timer SHALL coordinate with the authoritative playback controller; elapsed callbacks from a replaced timer or completed occurrences from an obsolete engine generation SHALL NOT stop a newer playback session. The timer SHALL NOT turn off or override ordinary repeat/shuffle settings.

#### Scenario: Stale elapsed timer
- **WHEN** an earlier deadline fires after rearming or cancellation
- **THEN** it does not stop or attenuate the current playback

#### Scenario: Stale completion
- **WHEN** an engine completion arrives from a superseded track generation
- **THEN** it neither decrements the timer nor stops the replacement track

#### Scenario: Timer expires during a load
- **WHEN** a timed deadline expires while a new track is loading
- **THEN** pending autoplay is cancelled and that load cannot begin audible playback after the timer has stopped it

### Requirement: Sleep stops take precedence over overlapping transitions
An active sleep timer SHALL suppress new crossfade and gapless transitions. Arming a timer during an overlap SHALL retire the outgoing voice and retain the authoritative incoming occurrence before admitting future timer completion events. Retiring outgoing completions SHALL NOT count toward a newly armed timer or advance the queue.

#### Scenario: Timer already active
- **WHEN** the current occurrence nears an otherwise eligible crossfade window with a sleep timer armed
- **THEN** it uses existing ordinary completion and timer stop precedence rather than starting overlap

#### Scenario: Timer already active with gapless enabled
- **WHEN** gapless is enabled and the current occurrence reaches its preload window while a sleep timer is armed
- **THEN** no gapless successor is staged, and ordinary completion remains responsible for counting or stopping

#### Scenario: Timer armed during overlap
- **WHEN** a listener arms a track-count timer while two voices overlap
- **THEN** outgoing playback retires and only subsequent natural completion of the authoritative current occurrence counts toward that timer

#### Scenario: Track-count timer during gapless handoff
- **WHEN** a TrackCount timer is armed during gapless playback
- **THEN** each outgoing natural completion is counted exactly once before successor promotion or timer stop
- **AND** a preparation-failed successor does not suppress that completion count

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

### Requirement: Accessible cross-platform controls

Now Playing SHALL expose localized English and Simplified Chinese timer controls and an accurate active-state summary in both compact and split layouts, including the 600×400 dp minimum window. Timer setup SHALL not request platform permissions or open the file picker.

#### Scenario: Active timer
- **WHEN** the timer is armed
- **THEN** an accessible control announces its active mode, remaining amount, and fade setting, and a cancellation action is available

#### Scenario: No timer
- **WHEN** no timer is armed
- **THEN** no stale countdown, stop control, or active-timer accessibility state is exposed
