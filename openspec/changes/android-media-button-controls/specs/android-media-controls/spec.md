## ADDED Requirements

### Requirement: Android hardware media-button controls
The Android app SHALL respond to hardware and peripheral media buttons — including the play/pause control on a wired headset/cable inline remote and on a Bluetooth headset — by routing `android.intent.action.MEDIA_BUTTON` events to the active Media3 session and applying them to the shared playback controller.

#### Scenario: Wired cable inline remote toggles playback
- **WHEN** audio is playing through the Android app and the user presses play/pause on a wired headset/cable inline remote
- **THEN** the Android playback engine receives the media-button event through a registered media-button receiver and toggles playback
- **AND** the shared playback state reflects the new playing/paused status

#### Scenario: Bluetooth headset toggles playback
- **WHEN** audio is playing and the user presses play/pause on a connected Bluetooth headset
- **THEN** playback toggles and the shared playback state reflects the new status

### Requirement: Android system transport controls
The Android app SHALL expose play, pause, next, and previous transport controls through the system media controls (lock screen and notification) backed by a `MediaSessionService`, and these controls SHALL operate on the shared playback queue.

#### Scenario: Lock-screen next/previous moves through the queue
- **WHEN** a queue with multiple tracks is loaded and the user taps next (or previous) on the system media controls
- **THEN** the engine advances to the adjacent queue track through the shared controller's skip handling and begins playback of that track

#### Scenario: System play/pause stays in sync with in-app controls
- **WHEN** the user toggles playback from the system media controls
- **THEN** the shared playback state and in-app Compose controls reflect the same playing/paused status

### Requirement: Auto-pause on audio output disconnect
The Android playback engine SHALL pause playback when the active audio output route becomes noisy (for example, the wired headset/cable is unplugged or the Bluetooth device disconnects).

#### Scenario: Unplugging the cable pauses playback
- **WHEN** audio is playing through a wired headset and the cable is unplugged
- **THEN** playback pauses rather than continuing through the device speaker
- **AND** the shared playback state reflects the paused status
