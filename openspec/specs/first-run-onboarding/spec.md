# First-run onboarding Specification

## Purpose

Define the first-run guidance RhythHaus presents so users understand its local-first storage, platform import model, supported audio, scan lifecycle, and recovery limits before relying on the library.

## Requirements

### Requirement: Onboarding eligibility is durable

RhythHaus SHALL determine onboarding eligibility from a durable completion value that is independent from library, source, playback-session, and theme state. A fresh installation and an unreadable or corrupt onboarding value SHALL require onboarding. Either Skip or Finish SHALL durably mark onboarding complete before the normal library becomes the active destination.

#### Scenario: Fresh installation
- **WHEN** RhythHaus starts without a recorded onboarding completion
- **THEN** it presents onboarding after eligibility has resolved and does not briefly expose an interactive normal library first

#### Scenario: Returning installation
- **WHEN** RhythHaus starts with onboarding completion recorded
- **THEN** it enters the normal library without presenting onboarding

#### Scenario: Completion survives restart
- **WHEN** a user skips or finishes onboarding and restarts RhythHaus
- **THEN** onboarding remains completed and is not presented automatically

#### Scenario: Corrupt preference fails safe
- **WHEN** the onboarding completion value cannot be read or decoded
- **THEN** RhythHaus treats onboarding as required without modifying library or playback data

### Requirement: Guidance is accurate for the active platform

Onboarding SHALL identify RhythHaus as a local-first player that neither streams nor uploads the user's audio. It SHALL explain how the active platform adds music, list WAV, AIFF, AU, MP3, M4A/AAC, FLAC, and OGG as recognized formats, explain that supported files are scanned for metadata and artwork, and state that unsupported or unreadable items may be skipped and reported.

#### Scenario: Android guidance
- **WHEN** onboarding is shown on Android
- **THEN** it explains folder selection and retained folder access without claiming that notification permission is required for in-app playback

#### Scenario: iOS guidance
- **WHEN** onboarding is shown on iOS
- **THEN** it explains Files.app import into `Documents/RhythHaus`, external-file copying, and scan-in-place behavior for files already under that managed directory

#### Scenario: macOS guidance
- **WHEN** onboarding is shown on macOS desktop
- **THEN** it explains selected-folder access and scanning without claiming that RhythHaus copies the selected library

### Requirement: Onboarding has explicit navigation and exit semantics

Onboarding SHALL provide deterministic Back, Next, Skip, and Finish actions with localized labels and accessibility semantics. Skip SHALL be available from every first-run page. Back SHALL move to the preceding page when one exists. Platform Back or dismiss on the first first-run page SHALL not bypass onboarding without the user explicitly choosing Skip or Finish.

#### Scenario: Move through pages
- **WHEN** the user activates Next before the final page
- **THEN** the next onboarding page becomes visible and its heading is announced as the active content

#### Scenario: Return to a preceding page
- **WHEN** the user activates Back after the first page
- **THEN** the preceding page becomes visible without changing durable completion

#### Scenario: Skip onboarding
- **WHEN** the user activates Skip on any first-run page
- **THEN** completion is persisted and the normal library becomes active

#### Scenario: Finish onboarding
- **WHEN** the user activates Finish on the final page
- **THEN** completion is persisted and the normal library becomes active

#### Scenario: System Back on first first-run page
- **WHEN** the first onboarding page is active and the user invokes platform Back
- **THEN** onboarding remains active and completion remains unrecorded

### Requirement: Onboarding does not mutate music or playback state

Displaying, navigating, skipping, finishing, or reopening onboarding SHALL NOT request a platform permission, launch a file or folder picker, create or remove a library source, start or cancel a scan, alter the playback queue, stop active playback, or clear restored playback state.

#### Scenario: Existing startup work continues
- **WHEN** first-run onboarding is visible while RhythHaus restores or initializes library and playback state
- **THEN** that work proceeds independently and no music mutation is attributed to onboarding

#### Scenario: User finishes onboarding
- **WHEN** Finish enters the normal library
- **THEN** the user sees the authoritative library and playback state produced by the existing lifecycle

### Requirement: Guidance can be reopened from Settings

Settings SHALL expose a localized, accessible action that opens onboarding for review. A Settings-launched onboarding instance SHALL begin at its first page, SHALL allow normal Back or dismiss to return to the same Settings destination, and SHALL not change the durable completion value.

#### Scenario: Reopen from Settings
- **WHEN** a user activates the onboarding action in Settings
- **THEN** onboarding opens at its first page above Settings

#### Scenario: Dismiss reopened onboarding
- **WHEN** a user invokes Back or Close from a Settings-launched onboarding instance
- **THEN** RhythHaus returns to the same Settings destination without resetting Settings state or onboarding completion

#### Scenario: Review while audio is playing
- **WHEN** onboarding is reopened from Settings during playback
- **THEN** playback continues and the playback queue is unchanged

### Requirement: Layout and localization remain usable

Onboarding SHALL provide English and Simplified Chinese resources, use safe-area-aware scrolling for compact or short windows, and keep all visible actions reachable at 600×400 dp and larger supported surfaces. Hidden pages and inactive actions SHALL expose no pointer, click, or accessibility actions.

#### Scenario: Short desktop window
- **WHEN** onboarding is rendered at 600×400 dp
- **THEN** the current page content and its exit or progression actions are reachable by scrolling

#### Scenario: Localized presentation
- **WHEN** the application locale is English or Simplified Chinese
- **THEN** onboarding headings, body text, platform guidance, progress, and action labels use that locale
