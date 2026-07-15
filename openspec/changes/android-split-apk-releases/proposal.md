## Why

RhythHaus direct-download Android releases currently provide only a universal APK containing all three native TagLib slices. ABI-specific APKs will reduce direct-download size while preserving the Android App Bundle as the Google Play artifact and retaining a universal compatibility fallback.

## What Changes

- Add an opt-in Android release mode that generates `arm64-v8a`, `armeabi-v7a`, and `x86_64` APKs plus one universal APK using AGP built-in ABI splits.
- Keep ordinary debug, IDE, `./init.sh`, and default APK behavior unchanged when split mode is disabled.
- Keep `bundleRelease` as an independent Google Play AAB path.
- Add one strict shared ABI property consumed by native TagLib builds, APK split configuration, and artifact verification.
- Add Gradle verification for output filters, native slices, application/version metadata, AAB presence, and signatures when release artifacts are signed.

## Capabilities

### New Capabilities
- `android-split-apk-releases`: Defines opt-in direct-download ABI APKs, universal fallback, Play AAB compatibility, canonical versioning, and artifact verification.

### Modified Capabilities

None.

## Impact

- Root Gradle properties and shared build-logic helpers.
- Android application and TagLib Gradle configuration.
- Generated Android APK/AAB release artifacts and verification tasks.
- No application runtime, public API, native TagLib source, signing policy, dependency, or non-Android packaging changes.
