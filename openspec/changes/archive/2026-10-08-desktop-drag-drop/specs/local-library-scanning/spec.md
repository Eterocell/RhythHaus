# Spec Delta

## ADDED Requirements

### Requirement: Desktop dropped sources preserve scan identity and deletion authority
The local-library scanner MUST preserve source-local identity, source-scoped deduplication, explicit scan admission, and existing missing-file confirmation for desktop dropped sources.

#### Scenario: Repeated drop and rescan
- **WHEN** the same folder or file is dropped again and then rescanned
- **THEN** existing track identities and user state remain stable and no media is copied.

#### Scenario: Existing unreadable path
- **WHEN** a persisted dropped path exists but is unreadable, non-regular, or a dangling symbolic link
- **THEN** its existing track is marked observed without replacing metadata or user state and cannot be removed as missing.

#### Scenario: Confirmed absence
- **WHEN** the persisted path itself no longer exists and a scan completes
- **THEN** the track is reported missing but retained until explicit missing-track removal.

#### Scenario: Unavailable fixed-file source recovery
- **WHEN** no member of a registered dropped-file set is currently readable
- **THEN** its Settings recovery action rescans that original set without requiring folder selection.
