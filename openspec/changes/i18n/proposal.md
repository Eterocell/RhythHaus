# Change: i18n

## Why

RhythHaus currently hardcodes shared Compose UI copy in English. The roadmap calls for i18n support so shared UI can be localized without changing code.

## What Changes

- Add Compose Multiplatform string resources for English and Simplified Chinese.
- Migrate shared Compose UI strings and content descriptions to `stringResource`.
- Keep user media metadata and backend diagnostics out of localization scope.
