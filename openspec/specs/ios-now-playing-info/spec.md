### Requirement: Now Playing dictionary SHALL always contain duration or live-stream indicator

When a track is loaded and the `nowPlayingInfo` dictionary is set on `MPNowPlayingInfoCenter`, the system SHALL ensure that either `MPMediaItemPropertyPlaybackDuration` is present with a value greater than 0, or `MPNowPlayingInfoPropertyIsLiveStream` is set to `true`. The dictionary SHALL never omit both.

#### Scenario: Duration available from track metadata or AVAudioPlayer at load time
- **WHEN** a track is loaded and `durationMillis` is non-null (from TagLib or `audioPlayer.duration`)
- **THEN** `MPMediaItemPropertyPlaybackDuration` SHALL be present in the `nowPlayingInfo` dictionary with the duration in seconds
- **AND** `MPNowPlayingInfoPropertyIsLiveStream` SHALL NOT be present (or set to false)

#### Scenario: Duration unavailable at load time but available after play starts
- **WHEN** a track is loaded with `durationMillis = null` (both TagLib and `audioPlayer.duration` at `prepareToPlay` time returned null/0)
- **AND** `play()` is called
- **THEN** the system SHALL re-probe `audioPlayer.duration` after playback begins
- **AND** if `audioPlayer.duration` is now greater than 0, `durationMillis` SHALL be updated and `MPMediaItemPropertyPlaybackDuration` SHALL be present in the next `nowPlayingInfo` update

#### Scenario: Duration genuinely unknown after all probes
- **WHEN** `durationMillis` is still null after re-probing `audioPlayer.duration` in `play()`
- **THEN** `MPNowPlayingInfoPropertyIsLiveStream` SHALL be set to `true` in the `nowPlayingInfo` dictionary
- **AND** `MPMediaItemPropertyPlaybackDuration` MAY be omitted
- **AND** iOS SHALL render a live-stream indicator instead of a greyed-out slider

### Requirement: Now Playing dictionary SHALL always contain playback rate and elapsed time

The `nowPlayingInfo` dictionary SHALL always contain `MPNowPlayingInfoPropertyPlaybackRate` and `MPNowPlayingInfoPropertyElapsedPlaybackTime` when a track is loaded, regardless of whether duration is known.

#### Scenario: Track loaded but not yet playing
- **WHEN** a track is loaded via `load()` and playback has not started
- **THEN** `MPNowPlayingInfoPropertyPlaybackRate` SHALL be `0.0`
- **AND** `MPNowPlayingInfoPropertyElapsedPlaybackTime` SHALL be `0.0`

#### Scenario: Track playing
- **WHEN** `play()` has been called and playback is active
- **THEN** `MPNowPlayingInfoPropertyPlaybackRate` SHALL be `1.0`
- **AND** `MPNowPlayingInfoPropertyElapsedPlaybackTime` SHALL reflect the current playback position in seconds
