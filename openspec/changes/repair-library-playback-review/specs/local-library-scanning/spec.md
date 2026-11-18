## ADDED Requirements

### Requirement: Bound Android SAF sidecar enumeration by directory count
The Android SAF scanner MUST enumerate children once per directory, associating case-insensitive same-stem LRC sidecars only with supported audio files from that directory. MediaStore SHALL NOT perform sibling lookup.

#### Scenario: Mixed tree with optional lyrics
- **WHEN** a scan visits a populated tree containing audio, unrelated documents, missing or unreadable sidecars, and nested directories with repeated filenames
- **THEN** the scanner enumerates each directory once, preserves scan ordering and track identities, and falls back to embedded lyrics or no lyrics when the local candidate cannot be read within its bound
