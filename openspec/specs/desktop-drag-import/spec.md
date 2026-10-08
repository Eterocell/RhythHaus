# Desktop Drag Import Specification

## Purpose

Desktop drag import makes local music discovery immediate while preserving RhythHaus's local-first source lifecycle and explicit scan semantics.

## Requirements

### Requirement: Desktop drops admit folders and audio files
The desktop application MUST accept dropped directories and supported audio files. A directory MUST become a reference-backed folder source. A file drop MUST become a stable source whose scan observes the dropped files without copying them.

#### Scenario: Folder drop
- **WHEN** a readable directory is dropped
- **THEN** RhythHaus registers it as a JVM folder source and starts one App-owned scan
- **AND** the source identity is stable across repeated drops of the same canonical path.

#### Scenario: Audio-file drop
- **WHEN** one or more readable supported audio files are dropped
- **THEN** RhythHaus registers or reuses one desktop dropped-files source and scans those files in drop order
- **AND** playback uses the original file paths without copying them.

#### Scenario: Mixed drop
- **WHEN** a drop contains readable folders, supported files, and invalid items
- **THEN** valid items are admitted and invalid items are reported without discarding valid siblings.

### Requirement: Desktop drop validation is bounded and recoverable
The desktop drop adapter MUST reject unreadable paths, unsupported file types, non-regular files, and paths outside the allowed local filesystem contract without throwing through the UI. It MUST not copy, delete, or mutate user files.

#### Scenario: Invalid-only drop
- **WHEN** every dropped item is unsupported or unreadable
- **THEN** no source or scan is created and the UI reports a localized recoverable message.

#### Scenario: Duplicate drop
- **WHEN** a path already belongs to a registered desktop source
- **THEN** the drop does not create a duplicate source and reports or reuses the existing source.

### Requirement: Drop lifecycle uses existing source authority
Desktop drops MUST enter the existing source registration and App-owned scan coordinator path. Drops MUST NOT start a parallel scan, bypass mutation serialization, or automatically remove missing files.

#### Scenario: Scan failure or cancellation
- **WHEN** a drop scan fails or is cancelled
- **THEN** the source remains available for retry according to existing source lifecycle rules and no partial source mutation is reported as complete.

#### Scenario: Cancellation of the first sibling scan
- **WHEN** a drop admits multiple sources and the first scan is cancelled
- **THEN** every admitted source remains registered for explicit retry and later scans do not start.

#### Scenario: Partial batch failure
- **WHEN** an earlier source scan fails and a later sibling succeeds
- **THEN** terminal feedback retains the failed source and its explanation rather than presenting the entire batch as successful.

#### Scenario: Exclusive modal or rejected admission
- **WHEN** onboarding or an exclusive modal is foreground, or scan admission is rejected
- **THEN** no drop source is registered and no successful import is announced.

### Requirement: Desktop drop target is accessible and discoverable
The desktop drop surface MUST expose localized visible and accessibility text for accepting folders and audio files, expose active/inactive state to accessibility services, and retain the existing folder picker as a keyboard-equivalent fallback.

#### Scenario: Drag target receives focus
- **WHEN** a keyboard or accessibility user focuses the import surface
- **THEN** it exposes the same import action and localized description without requiring a drag gesture.

### Requirement: Local library scanning preserves source boundaries
The local-library scanner MUST preserve source-local identity, source-scoped deduplication, explicit scan admission, and existing missing-file confirmation for desktop dropped sources.

#### Scenario: Repeated drop and rescan
- **WHEN** the same folder or file is dropped again and then rescanned
- **THEN** existing track identities and user state remain stable and no media is copied.
