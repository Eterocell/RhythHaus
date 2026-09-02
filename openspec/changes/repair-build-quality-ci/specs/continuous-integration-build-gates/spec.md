## Purpose

Continuous integration validates the quality and buildability of every supported RhythHaus platform without silently substituting platform test suites for build verification.

## ADDED Requirements

### Requirement: CI runs independent static-quality gates

GitHub CI SHALL run the architecture gate, the repository's ktfmt-backed formatting check, and Detekt as explicit quality validations on pull requests and pushes to `main`.

#### Scenario: Source or build logic violates a quality gate

- **WHEN** architecture policy, formatting, or Detekt finds a violation
- **THEN** the corresponding CI quality validation fails
- **AND** the failure identifies the invoked Gradle quality task

### Requirement: CI builds every supported platform deliverable

GitHub CI SHALL independently build Android, desktop JVM, and the iOS application on macOS runners.

#### Scenario: Android build validation

- **WHEN** CI validates Android
- **THEN** it assembles the debug Android application with Java 21 and the repository Gradle wrapper

#### Scenario: Desktop build validation

- **WHEN** CI validates desktop JVM
- **THEN** it compiles the desktop application Kotlin sources with Java 21 and the repository Gradle wrapper

#### Scenario: iOS application build validation

- **WHEN** CI validates iOS
- **THEN** it invokes Xcode to build the `iosApp` Simulator application without code signing
- **AND** Kotlin framework linkage and the Swift iOS wrapper are part of the build

### Requirement: Platform tests remain outside CI scope

The platform build jobs SHALL NOT execute Android Host Test or iOS Simulator Test tasks.

#### Scenario: Platform jobs are evaluated

- **WHEN** the Android, desktop, and iOS CI jobs execute
- **THEN** they run only their designated build commands
- **AND** no Android Host Test or iOS Simulator Test task is selected
