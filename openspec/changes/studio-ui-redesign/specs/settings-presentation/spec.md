# Settings presentation delta

## Purpose

Define goal-oriented settings and recovery presentation that keeps routine preferences separate from source maintenance and destructive actions, preserving truthful platform capabilities and existing ownership.

## ADDED Requirements

### Requirement: Goal-oriented settings groups
Settings SHALL group Appearance; Playback; Music sources; Backup and maintenance; and Application information. Theme, ReplayGain, crossfade duration, gapless, automatic resume and output status MUST remain distinct. Repeat and shuffle MUST remain player-session controls, not duplicated preferences. Saving, failure and action gating MUST reflect their existing owners.

#### Scenario: Adjust a playback preference
- **WHEN** the listener changes crossfade, gapless, ReplayGain or automatic resume
- **THEN** only that existing preference operation MUST be invoked and its accepted/saving/failed state MUST be accurately rendered

### Requirement: Truthful platform availability
Output status SHALL show a verified active route or unavailable, never a fabricated device or route selector. Notification recovery MUST appear only for actionable Android states. Source picker visibility, availability and mutation permission MUST remain distinct; iOS Files.app instructions MUST remain iOS-specific. Artwork-cache clearing MUST NOT delete media, library or playback state.

#### Scenario: Permission unavailable or picker blocked
- **WHEN** the host reports a missing capability, non-actionable notification state or unavailable source picker
- **THEN** Settings MUST accurately explain the applicable state without exposing a fake success action or platform-inaccurate guidance

### Requirement: Source recovery and scan reports
Sources SHALL retain add, scan, cancel, access recovery, summary and remove-missing workflows. Current failed/cancelled scans MUST remain distinguishable from the last completed report. Report interaction state MUST survive list recycling within the Settings destination. UI regrouping MUST NOT change operation serialization, accepted-scan validation or permission admission.

#### Scenario: Revisit a scan report
- **WHEN** a report is expanded, scrolled offscreen and revisited during the same Settings lifetime
- **THEN** report state MUST remain intact and only the latest eligible completed scan MUST permit the existing remove-missing operation

### Requirement: Explicit destructive scope
Source removal, library clearing and remove-missing confirmations SHALL name the affected object, truthful available counts, original-file implications and actual reversibility. They MUST be separated from routine preferences, require explicit confirmation, and preserve busy/error/cancellation behavior without claiming unsupported undo.

#### Scenario: Cancel or retry maintenance
- **WHEN** the listener cancels a destructive confirmation or its operation fails
- **THEN** cancellation MUST cause no mutation and failure MUST retain truthful feedback and the existing retry/admission rules rather than dismissing as success

#### Scenario: Clear a configured source with no indexed tracks
- **GIVEN** at least one configured source and no indexed tracks
- **WHEN** the listener opens Settings while mutations are enabled
- **THEN** the clear-library action remains available and opens its explicit confirmation
- **AND** the confirmation reports the current source count and zero tracks
- **AND** the existing mutation gate still disables the action while mutations are unavailable

### Requirement: Complete information and first-run surfaces
About, open-source licenses and onboarding re-entry SHALL use the same design system while retaining existing loading/retry, page bounds, platform wording, skip/finish persistence and review behavior. Onboarding MUST remain independent of permission requests and MUST NOT block local playback. Back and reduced-motion behavior MUST use the shared authority.

#### Scenario: Review onboarding after setup
- **WHEN** the listener reopens onboarding from Settings and exits or retries a failed completion
- **THEN** the existing review/persistence semantics MUST remain intact, and returning MUST restore the correct Settings route without requesting permissions
