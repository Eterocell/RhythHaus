## ADDED Requirements

### Requirement: Split APK generation is opt-in
The Android build SHALL generate ABI-specific APKs only when `rhythhaus.android.splitApk=true`; absent or other values SHALL preserve existing ordinary APK behavior.

#### Scenario: Split mode enabled
- **WHEN** release assembly runs with `-Prhythhaus.android.splitApk=true`
- **THEN** the build SHALL generate one APK for each supported ABI and one universal APK

#### Scenario: Split mode disabled
- **WHEN** debug or release assembly runs without the exact opt-in value
- **THEN** the build SHALL retain its existing non-split APK behavior

### Requirement: Supported ABI contract is shared and strict
Native TagLib generation, Android APK split configuration, and artifact verification MUST consume `rhythhaus.android.abis`, whose approved ordered value is `arm64-v8a,armeabi-v7a,x86_64`.

#### Scenario: Approved ABI value
- **WHEN** the ABI property contains the approved ordered set
- **THEN** native generation, split generation, and verification SHALL use the same three values

#### Scenario: Invalid ABI value
- **WHEN** the ABI property is missing, blank, duplicated, reordered, added to, or reduced
- **THEN** configuration SHALL fail with an actionable expected-versus-actual message

### Requirement: Direct-download artifact matrix is complete
Split release mode SHALL produce exactly `arm64-v8a`, `armeabi-v7a`, and `x86_64` APKs plus one universal APK.

#### Scenario: ABI-specific APK contents
- **WHEN** an ABI-specific APK is verified
- **THEN** it SHALL contain only the matching `librhythhaus_taglib.so` slice and no TagLib slice for another ABI

#### Scenario: Universal APK contents
- **WHEN** the universal APK is verified
- **THEN** it SHALL contain all three supported TagLib slices

#### Scenario: Missing or extra output
- **WHEN** an expected output is absent, duplicated, or an unsupported ABI output exists
- **THEN** artifact verification SHALL fail and identify expected and actual filters

### Requirement: APK and AAB metadata remains canonical
Every APK and the release AAB SHALL retain application ID `com.eterocell.rhythhaus` and the configured canonical `rhythhaus.versionName` and `rhythhaus.versionCode` without ABI-derived version-code overrides.

#### Scenario: Verify release metadata
- **WHEN** APK and AAB verification runs
- **THEN** SDK metadata inspection SHALL match package, version name, and version code to the configured canonical values

#### Scenario: Convert bundle metadata for SDK inspection
- **WHEN** release AAB metadata is verified
- **THEN** verification SHALL create an isolated temporary proto archive containing the base manifest, base resource table, and packaged `base/res/**` payloads required by that table, convert it with SDK `aapt2`, and inspect the converted artifact with SDK `apkanalyzer`
- **AND** verification SHALL NOT use artifact filenames, source configuration, APK metadata, or AGP task inputs as identity proof

### Requirement: Google Play AAB remains independent
`bundleRelease` SHALL continue to produce one non-empty release AAB without requiring split APK mode.

#### Scenario: Build Play artifact
- **WHEN** `bundleRelease` runs without the split opt-in property
- **THEN** the release AAB SHALL be produced and pass independent metadata verification

### Requirement: Existing signing policy is preserved
Release APKs SHALL continue using the existing convention-plugin signing configuration without new credentials or policy. Signed APKs MUST pass SDK `apksigner verify`; unsigned local release outputs MAY be accepted only when signing credentials are absent.

#### Scenario: Signed release output
- **WHEN** signing credentials are configured and an APK is signed
- **THEN** artifact verification SHALL require `apksigner verify` to succeed

#### Scenario: Unsigned local output
- **WHEN** release signing credentials are absent
- **THEN** verification SHALL report unsigned output without exposing secrets and SHALL NOT fail solely because the APK is unsigned

### Requirement: Existing development workflows remain stable
The change SHALL preserve ordinary `assembleDebug`, IDE runs, `./init.sh`, application behavior, and non-Android packaging.

#### Scenario: Run repository verification
- **WHEN** existing verification runs without split mode
- **THEN** tasks and artifact expectations SHALL remain compatible with the pre-change workflow
