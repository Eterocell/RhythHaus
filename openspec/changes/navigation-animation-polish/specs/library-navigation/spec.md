## ADDED Requirements

### Requirement: Android predictive back shows visual progress

When the user drags the Android predictive back gesture on a nested route, the incoming route SHALL track the gesture drag position. Completing the gesture SHALL pop exactly one route. Cancelling the gesture SHALL snap back to the current route.

#### Scenario: Predictive back drag shows progress
- **WHEN** the current route is Album Detail, Artist Detail, Now Playing, Search, or Settings
- **AND** the user drags the Android predictive back gesture
- **THEN** the incoming route visually tracks the drag position
- **AND** the app does not close

#### Scenario: Predictive back cancelled
- **WHEN** the user drags the predictive back gesture but releases before completing
- **THEN** the current route snaps back to its original position
- **AND** no route is popped

### Requirement: Bottom bar stays fixed during route transitions

The NowPlayingBar SHALL remain fixed at the bottom of the screen during all route transition animations. It SHALL NOT slide or fade with the animated route content.

#### Scenario: Route transition does not move bottom bar
- **WHEN** the user navigates between Home, Album Detail, Artist Detail, Search, or Settings
- **THEN** the NowPlayingBar stays in its fixed position at the bottom
- **AND** the bar's content continues to reflect current playback state

### Requirement: Bottom bar expands to Now Playing screen

Tapping the NowPlayingBar SHALL expand into the Now Playing screen with a growth animation from the bar position, not a fade or slide route transition. Closing the Now Playing screen SHALL collapse back to the bar shape.

#### Scenario: Expand from bar to Now Playing
- **WHEN** the user taps the NowPlayingBar with a track loaded
- **THEN** the Now Playing screen grows from the bar position to fill the screen
- **AND** the bar's compact content fades out as the Now Playing content fades in

#### Scenario: Collapse from Now Playing to bar
- **WHEN** the user invokes back from the Now Playing screen
- **THEN** the screen collapses back to the bar shape
- **AND** the bar's compact content fades back in