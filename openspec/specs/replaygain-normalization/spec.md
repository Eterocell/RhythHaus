# ReplayGain Normalization Specification

## Purpose

Provide optional cross-platform, tag-driven ReplayGain attenuation that reduces loudness differences without modifying media, system volume, playback identity or transport state.

## Requirements

### Requirement: Persistent listener-selected normalization mode

The application SHALL offer Off, Track and Album modes with localized English and Simplified Chinese accessible controls. Off SHALL be the default for missing, invalid or unreadable preference state. A successful selection SHALL survive process restart. A failed preference write SHALL retain the last accepted mode and expose a recoverable error.

#### Scenario: First use or invalid preference
- **WHEN** no valid normalization preference can be restored
- **THEN** normalization is Off and media plays without ReplayGain attenuation.

#### Scenario: Change mode during playback
- **WHEN** a mode change is accepted while a track is playing or paused
- **THEN** its effective gain updates without reloading media, seeking, replacing the queue, changing repeat/shuffle or changing system volume.

#### Scenario: Persisting selection fails
- **WHEN** saving a requested mode fails
- **THEN** the previously accepted mode remains authoritative and an accessible localized error is shown.

### Requirement: Select valid track or album ReplayGain facts

Track mode SHALL select track gain/peak. Album mode SHALL select album gain/peak, falling back as a pair when album gain is absent or invalid. Gains SHALL be finite decibels; peaks SHALL be finite positive amplitudes. Invalid fields SHALL independently be absent. Missing selected gain and Off SHALL yield unity regardless of peak tags.

#### Scenario: Album gain with missing album peak
- **WHEN** Album mode selects a valid album gain but no valid album peak
- **THEN** that album gain is used without borrowing track peak or falling back solely because the peak is absent.

#### Scenario: Album fallback
- **WHEN** album gain is unavailable and valid track gain exists
- **THEN** Album mode uses track gain and only the corresponding track peak.

#### Scenario: Missing and malformed tags
- **WHEN** tags contain non-finite or malformed values, non-positive peaks, or only a peak without a selected gain
- **THEN** invalid fields do not cause playback failure and missing selected gain leaves normalization at unity.

### Requirement: Attenuation-only gain with optional peak protection

For valid selected gain g decibels, normalization SHALL equal min(1, 10^(g/20)); a valid corresponding peak p SHALL further bound it by min(1, 1/p). The result SHALL be finite and within 0..1. Positive gain SHALL NOT amplify playback. No preamp, loudness analysis or media-tag write SHALL be performed. Settings SHALL explain that this is attenuation-only and untagged tracks are unchanged.

#### Scenario: Negative gain
- **WHEN** selected gain is -6 dB without peak limitation
- **THEN** normalization is approximately 0.501187 rather than unity.

#### Scenario: Positive gain and excessive peak
- **WHEN** selected gain is +6 dB and corresponding peak is 2
- **THEN** normalization is 0.5, never amplified above unity.

#### Scenario: Very large finite values
- **WHEN** a finite gain or peak is outside ordinary tag ranges
- **THEN** calculation remains finite and bounded without overflowing a platform gain API.

### Requirement: Compose normalization with sleep attenuation

The audible application gain SHALL be the product of current-track normalization and current sleep-timer attenuation. Gain writes SHALL obey the existing playback ownership boundary so stale mode, fade or selection work cannot change the replacement track's gain. Restoration, retry, automatic advance, system-media commands and metadata reconciliation SHALL use the same policy before audible playback begins.

#### Scenario: Cancel fade on normalized media
- **WHEN** normalization is 0.5 and sleep attenuation is 0.4
- **THEN** application gain is 0.2
- **AND** cancelling the timer restores application gain to 0.5, not 1.

#### Scenario: Change mode during fade
- **WHEN** normalization is disabled while sleep attenuation is 0.4
- **THEN** application gain becomes 0.4 and the timer remains armed with its original stop boundary.

#### Scenario: Replace media during pending gain update
- **WHEN** an old gain update waits while a new track claims playback ownership
- **THEN** the old update cannot attenuate the new track
- **AND** the new track receives its own composed gain before playback.

#### Scenario: Pause and resume
- **WHEN** playback resumes during a progress-based fade
- **THEN** the composed gain is applied before audio resumes without a unity-volume burst.
