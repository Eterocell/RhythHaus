## ADDED Requirements

### Requirement: Home shows no folder-management section when the library has content

The Library home SHALL render no source rows, rescan/remove source
controls, add-folder button, or scan-outcome panel when the library
contains tracks.

#### Scenario: Home renders with a populated library

- **WHEN** the Library home is rendered with one or more tracks
- **THEN** no configured-source rows, no per-source rescan or remove
  controls, no add-folder button, and no scan-outcome panel appear
- **AND** the header, playlists action, browse-mode picker, and
  album/artist/song content remain unchanged

#### Scenario: Home renders in the wide list-detail layout

- **WHEN** the Library home is rendered in the list-detail adaptive
  layout with a populated library
- **THEN** the same absence of folder-management and scan-outcome UI
  applies to the home pane

### Requirement: Home shows the add-folder option only when the library is empty

The Library home SHALL show the add-folder import card only when the
library is empty and the source picker action is visible.

#### Scenario: Home renders with an empty library

- **WHEN** the library has no tracks and the source picker action is
  visible
- **THEN** the import card with the add-folder action is rendered

#### Scenario: Home renders with a populated library

- **WHEN** the library has one or more tracks
- **THEN** no add-folder import card is rendered

### Requirement: Home shows scanning progress only while the library is empty

The Library home SHALL show the scanning-progress card only while the
library is empty and a scan is active.

#### Scenario: Empty library with an active scan

- **WHEN** the library has no tracks and a scan is active
- **THEN** the scanning-progress card with its cancel action is
  rendered

#### Scenario: Populated library with an active scan

- **WHEN** the library has one or more tracks and a scan is active
- **THEN** no scanning-progress card is rendered on home

### Requirement: Settings is the folder-management surface

Settings SHALL render the configured sources with rescan and remove
actions, the add-folder action, the active-scan progress slot, and the
terminal scan-outcome panel with summary, rescan/retry, view/hide
report, and remove-missing actions.

#### Scenario: Settings renders the scan-outcome panel

- **WHEN** a terminal scan session is displayed
- **THEN** the scan summary, rescan/retry action, view/hide report
  action, and, for a completed scan, the remove-missing action are
  rendered in Settings

#### Scenario: Scan report expansion survives list-item recycling

- **WHEN** the report is expanded and the Settings list item is
  recreated
- **THEN** the report remains expanded

#### Scenario: Scan report collapses when the displayed session changes

- **WHEN** the displayed scan session changes
- **THEN** the report collapses and the view-report action is shown
  again

### Requirement: Functionality is preserved through Settings

All folder-management and scan capabilities reachable from the previous
home manager card SHALL remain reachable from Settings.

#### Scenario: Every prior manager action remains reachable

- **WHEN** a user manages sources from Settings
- **THEN** add folder, rescan, recover lost access, remove source,
  cancel active scan, view/hide scan report, and remove missing tracks
  remain available

### Requirement: Scope remains limited to UI placement

The change SHALL remain limited to Library home and Settings UI
placement and wiring.

#### Scenario: Scoped diff is reviewed

- **WHEN** implementation is complete
- **THEN** no dependency, database, scanner, playback-engine,
  navigation-model, or platform-integration change is present
- **AND** no Windows or Linux product support is added
