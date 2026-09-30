# Spec Delta

## ADDED Requirements

### Requirement: Scanned metadata remains authoritative beneath app-local corrections

The local-library scan SHALL continue to update raw metadata read from source audio without replacing a listener's app-local per-field corrections. Deleting a track through the existing source-management lifecycle SHALL remove its associated corrections atomically with that track.

#### Scenario: Rescan preserves corrections and refreshes raw tags
- **WHEN** a track with a corrected artist is rescanned and its source artist changes
- **THEN** the corrected artist remains visible
- **AND** restoring the artist displays the newly scanned source artist

#### Scenario: Removal prunes corrections
- **WHEN** an accepted remove-missing, source removal, or clear-library operation deletes a track
- **THEN** its associated app-local corrections no longer exist
- **AND** no correction is applied to a later track based solely on its filename
