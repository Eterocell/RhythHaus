## ADDED Requirements

### Requirement: Gate convention and application unit tests in CI
The test job MUST run the convention regression suite, KMP JVM tests including Database and Onboarding, Android application unit tests and desktop application tests. Platform build jobs SHALL stay independent.

#### Scenario: Repository test coverage on pull requests
- **WHEN** a pull request breaks an owned unit test or the architecture checker regressions
- **THEN** the test job reports failure independently of the application build jobs

### Requirement: Enforce Search resource owners without a closed vocabulary
Shared and Search MUST retain their required owned keys, with no duplicates, foreign generated imports or untranslated Search additions. New Search-owned keys SHALL be permitted when EN and ZH have matching key sets.

#### Scenario: Changing the Search vocabulary
- **WHEN** a new Search string is translated in both locales without duplication
- **THEN** ownership validation passes; missing translations, duplicate owners or foreign resource imports still fail
