# Crossfade Playback

## Purpose

Allow opt-in overlapping local audio transitions while preserving queue identity, playback policy, lifecycle safety and one authoritative system media session across the supported platforms.

## ADDED Requirements

### Requirement: Crossfade is optional and durably selected
The application SHALL expose Off, 3 seconds, 5 seconds and 8 seconds in English and Simplified Chinese. Off SHALL be the default for absent, corrupt or unreadable preferences. Accepted selection SHALL be published only after durable persistence; failed writes SHALL retain the previous accepted selection and permit retry.

#### Scenario: Exclusive confirmation
- **WHEN** an exclusive confirmation is visible
- **THEN** background crossfade preference click and focus actions are removed

### Requirement: Crossfade preference ownership survives recreation
One process-owned publication authority SHALL survive UI recreation. Startup SHALL restore the accepted policy before local or system playback is enabled.

#### Scenario: Delayed restoration
- **WHEN** saved crossfade policy is slow to read during restart
- **THEN** neither local nor remote Play is admitted until the saved policy or safe Off fallback has been applied

#### Scenario: Recreation during a write
- **WHEN** a UI consumer is recreated during an admitted preference write
- **THEN** the new consumer observes the same accepted state and persistence/controller policy cannot diverge

### Requirement: Eligible transitions overlap real native audio
The application SHALL use two simultaneously advancing native players on Android, iOS and macOS for eligible natural successor transitions. It SHALL NOT substitute serial fade-out/fade-in. Outgoing playback SHALL continue unchanged during successor preparation.

#### Scenario: Actual overlap
- **WHEN** an eligible successor is prepared before the outgoing track ends
- **THEN** both native players advance concurrently during the bounded overlap
- **AND** their linear envelopes progress from outgoing unity/incoming zero to outgoing zero/incoming unity

### Requirement: Transition eligibility and duration are bounded
A distinct successor in effective shuffled/repeated queue order, known positive durations and no active sleep timer SHALL be required. Repeat One, Stop After Current, manual transitions, unknown durations and single-occurrence queues SHALL NOT initiate crossfade. The overlap SHALL be bounded by the configured window and half of each duration, shortened to outgoing time remaining, and skipped below 250 milliseconds.

#### Scenario: Ineligible completion
- **WHEN** playback reaches an ineligible natural transition
- **THEN** the existing ordinary completion/selection behavior remains unchanged

### Requirement: Crossfade preserves playback authority and gain policy
Each voice SHALL retain captured generation, playback-run and queue-occurrence identity. Incoming actual Playing SHALL atomically promote that occurrence to logical current and emit exactly one actual-play event and checkpoint. A retiring outgoing completion SHALL NOT advance the queue again. System metadata and seek actions SHALL target the promoted incoming occurrence.

#### Scenario: Independent voice gains
- **WHEN** two tagged tracks overlap
- **THEN** each envelope multiplies its own track's ReplayGain factor, remains finite in 0..1 and never modifies system volume

#### Scenario: Session restoration
- **WHEN** a session is restored
- **THEN** one current occurrence is restored without reviving an overlap

#### Scenario: Duplicate track occurrences
- **WHEN** adjacent occurrences contain the same media track
- **THEN** handoff promotes the distinct successor occurrence without collapsing queue entries or recording its actual-play event twice

#### Scenario: Retiring completion
- **WHEN** the outgoing voice completes after incoming promotion
- **THEN** it retires without altering incoming selection, position or next-successor admission

### Requirement: Lifecycle changes settle both voices safely
Pause SHALL pause both voices and freeze envelopes; resume SHALL apply retained gains before playback. Seek, manual transport/selection, queue replacement, recovery, reconciliation removing a member, Off selection and sleep-timer arming SHALL cancel staging before promotion or retire outgoing after promotion. Stop, clear, release, interruptions and route loss SHALL affect both voices under the existing platform synchronization boundary.

#### Scenario: Pause during overlap
- **WHEN** the listener pauses and later resumes overlapping playback
- **THEN** both positions and envelopes freeze during pause and retained gain is applied before either voice resumes

### Requirement: Stale and failed incoming work preserves the winner
Stale callbacks, preparation jobs and gain updates SHALL NOT mutate a newer selection. Incoming preparation failure SHALL preserve outgoing playback and fall back to ordinary completion without retrying every progress tick. Incoming failure after promotion SHALL report the normal structured incoming error and retire outgoing, not silently select the old occurrence.

#### Scenario: Replacement while preparing
- **WHEN** a different queue selection wins while successor preparation is pending
- **THEN** the stale preparation cannot promote, play, fade or stop the winner

#### Scenario: Preparation failure
- **WHEN** successor preparation fails
- **THEN** outgoing playback continues and the same failed candidate is not retried until current run or candidate changes
