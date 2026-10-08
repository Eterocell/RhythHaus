# Spec Delta

## MODIFIED Requirements

### Requirement: Local library scanning preserves source boundaries
The local-library scanner MUST preserve source-local identity, source-scoped deduplication, explicit scan admission, and existing missing-file confirmation for desktop dropped sources.

#### Scenario: Repeated drop and rescan
- **WHEN** the same folder or file is dropped again and then rescanned
- **THEN** existing track identities and user state remain stable and no media is copied.
