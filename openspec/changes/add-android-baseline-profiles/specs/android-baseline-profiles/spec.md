# Spec Delta

## Purpose

Provide repeatable generation, distribution, and measurement of app-owned Android Baseline Profile rules for RhythHaus startup and Library scrolling.

## ADDED Requirements

### Requirement: Generate app-owned rules from real startup and scrolling

RhythHaus SHALL provide an opt-in release Baseline Profile generation workflow that exercises cold startup to a meaningful screen and the first scroll of a populated Library Songs list. The workflow SHALL use a fixed prepared catalog and refuse to claim successful journey coverage if the expected screen, fixture, or scroll movement is absent.

#### Scenario: Generate with the prepared catalog
- **WHEN** a developer runs profile generation against a supported test device with the prepared catalog
- **THEN** the generator shall cover a real cold launch and change the visible Songs list contents by scrolling
- **AND** it shall emit nonempty release profile rules containing RhythHaus code from both journeys

#### Scenario: A journey cannot run as intended
- **WHEN** setup leaves a permission dialog or onboarding over the Library, the fixture is missing, or the list does not move
- **THEN** the workflow shall fail with a diagnosis instead of accepting an idle or incomplete journey

### Requirement: Report meaningful Android launch readiness

The Android app SHALL report fully drawn when the first usable destination is visible, not when a loading or default empty placeholder first draws.

#### Scenario: Returning user reaches the Library
- **WHEN** the app has applied its initial Library publication and its Library is visible
- **THEN** Android shall receive the fully drawn signal, including for an intentionally empty or recoverable terminal Library

#### Scenario: First-run onboarding loads
- **WHEN** app eligibility is loading or the first usable onboarding page has not appeared
- **THEN** the app shall not report fully drawn yet
- **AND** it shall report fully drawn when that onboarding page is visible without waiting for catalog import

### Requirement: Ship generated rules without a device build dependency

The ordinary optimized Android release SHALL package app-owned generated Baseline Profile rules while preserving release identity, signing, and supported ABI behavior. Ordinary builds SHALL NOT run profile generation or require an attached device.

#### Scenario: Assemble a normal release with no device attached
- **WHEN** the checked-in generated rules are present and the developer assembles the Android release without a device
- **THEN** the APK shall contain the compiled profile and its metadata, including verified app-owned generated rules after optimization
- **AND** the ordinary APK, opt-in split APKs and Play AAB shall preserve their existing release contracts

### Requirement: Measure the shipped compilation behavior

RhythHaus SHALL provide paired startup and first-scroll benchmark runs on the same supported device, production-equivalent optimized APK, and fixture, comparing required Baseline Profile compilation against no ahead-of-time compilation. Missing profile, missing fully drawn timing, or invalid scroll samples SHALL NOT be accepted as results.

#### Scenario: Run a controlled comparison
- **WHEN** both modes complete on the same qualifying device against the same optimized APK and fixture
- **THEN** the report shall include at least ten TTID and TTFD samples per mode, startup medians, and scroll frame timing percentiles over at least five iterations per mode
- **AND** it shall record the device, APK/profile provenance and raw result artifacts without presuming improvement

#### Scenario: Compiled profile cannot be applied
- **WHEN** the required-profile run cannot install or use the profile
- **THEN** it shall fail rather than record an unprofiled result under the profiled mode
