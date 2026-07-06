## ADDED Requirements

### Requirement: Shared UI string resources

RhythHaus shared Compose UI SHALL use Compose Multiplatform resources for localizable user-facing copy.

#### Scenario: Default English UI strings
- **WHEN** the app runs without a more specific supported locale
- **THEN** shared Compose UI labels, button text, status labels, and content descriptions are available from default English string resources

#### Scenario: Simplified Chinese UI strings
- **WHEN** the app runs in a Simplified Chinese locale
- **THEN** shared Compose UI labels, button text, status labels, and content descriptions use Simplified Chinese string resources

#### Scenario: User media metadata is not translated
- **WHEN** the UI displays track title, artist, album, or search query values from the user's library
- **THEN** those user-provided values are displayed unchanged
