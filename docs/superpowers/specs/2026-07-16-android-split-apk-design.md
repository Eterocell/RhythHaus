# Android Split APK Design

## Summary

RhythHaus will preserve the Android App Bundle as the Google Play release artifact and add an opt-in direct-download APK matrix using Android Gradle Plugin ABI splits. Split mode produces one APK for each native ABI supported by the existing TagLib build plus one universal fallback APK. Ordinary IDE, debug, and repository verification workflows retain their current single-APK behavior.

## Current State

`androidApp` currently produces standard debug and release APKs and release Android App Bundles. Release builds use R8 minification and resource shrinking, consume the canonical `rhythhaus.versionName` and `rhythhaus.versionCode` Gradle properties, and use the existing convention-plugin signing configuration.

The `taglib` module builds and packages `librhythhaus_taglib.so` for exactly three Android ABIs:

- `arm64-v8a`;
- `armeabi-v7a`;
- `x86_64`.

The current universal APK includes all three native slices. Native libraries are the material ABI-specific payload, so ABI splitting reduces direct-download artifact size without requiring density or language splits.

## Distribution Contract

RhythHaus has two Android release channels:

1. **Google Play:** `bundleRelease` produces the canonical AAB and leaves APK selection to Google Play.
2. **Direct download and testing:** opt-in ABI split mode produces three ABI-specific APKs and one universal fallback APK.

The direct-download matrix is:

| Artifact filter | Required TagLib native slices | Intended use |
|---|---|---|
| `arm64-v8a` | `lib/arm64-v8a/librhythhaus_taglib.so` only | Modern physical devices |
| `armeabi-v7a` | `lib/armeabi-v7a/librhythhaus_taglib.so` only | Supported 32-bit devices |
| `x86_64` | `lib/x86_64/librhythhaus_taglib.so` only | x86_64 emulators and devices |
| Universal | All three supported slices | Compatibility fallback |

No APK for an unsupported ABI is produced. The universal APK is required rather than optional so direct-download users always have a compatibility fallback.

## Opt-In Build Mode

ABI splitting is controlled by the Gradle property:

```text
rhythhaus.android.splitApk=true
```

The direct-download release build is invoked with:

```bash
./gradlew :androidApp:assembleRelease \
  -Prhythhaus.android.splitApk=true \
  --configuration-cache
```

When the property is absent or not exactly `true`, split mode is disabled and existing APK behavior remains unchanged. This prevents ordinary `assembleDebug`, IDE runs, `./init.sh`, and existing automation from unexpectedly changing their artifact shape.

The Android application module uses the official AGP ABI split DSL only when split mode is enabled:

- enable ABI splits;
- reset AGP's implicit ABI set;
- include exactly `arm64-v8a`, `armeabi-v7a`, and `x86_64`;
- enable the universal APK.

The supported ABI set has one build-owned source of truth in the root Gradle property:

```text
rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64
```

Both the TagLib Android native-build loop and Android application split configuration consume this property through a shared strict parser. The parser trims entries, rejects blanks and duplicates, and requires exactly the approved ordered set for this change. Artifact verification consumes the same parsed value, so the native builder, split generator, and verifier cannot drift through separately maintained lists.

## Versioning

Every APK and the AAB retain the canonical values from:

```text
rhythhaus.versionName
rhythhaus.versionCode
```

ABI-derived version-code overrides are explicitly excluded. The split APKs are direct-download artifacts and are not uploaded simultaneously through Google Play's legacy multi-APK mechanism. Keeping one canonical version avoids artificial upgrade ordering between direct-download APKs, the universal APK, and Play-installed builds.

Existing validation remains authoritative: a missing version property or non-integer `rhythhaus.versionCode` fails configuration with the current actionable error.

## App Bundle Compatibility

`bundleRelease` remains the Google Play path and must continue producing a valid release AAB. Split mode must not require bundletool, alter bundle version metadata, add product flavors, or replace the AAB with an APK set.

The AAB is verified independently from the direct-download APK matrix. Enabling split mode for `assembleRelease` must not remove or corrupt the bundle task, and building the AAB does not require split mode.

## Signing

Release signing continues to use `configureAppSigningConfigsForRelease()` from the existing Android application convention plugin. This change introduces no keystore, credential, secret, certificate, or signing-policy changes.

Artifact verification must support local unsigned release APKs when release credentials are unavailable. When an APK is signed, verification additionally runs the Android SDK `apksigner verify` command. The verification task must never print or persist signing credentials.

## Artifact Verification

Add a repository-owned Gradle verification task for the split release output. It must inspect generated artifacts and fail with an actionable message unless all requirements hold.

With split mode enabled, verification requires:

- exactly one APK for each supported ABI;
- exactly one universal APK;
- no unsupported ABI APK;
- each ABI APK contains its one matching `librhythhaus_taglib.so` and no TagLib slice for another ABI;
- the universal APK contains all three supported TagLib slices;
- every APK has application ID `com.eterocell.rhythhaus`;
- every APK has the configured `versionName` and canonical `versionCode`;
- signed APKs pass `apksigner verify` when the tool is available and signing is present.

The verification consumes AGP's `output-metadata.json` to classify ABI and universal APK outputs instead of relying on filename guesses. APK archive contents are inspected as ZIP entries. The configured Android SDK's `apkanalyzer` validates application ID, version name, and version code for both APK and AAB artifacts; no new external dependency is added.

Add independent AAB verification that requires `bundleRelease` to produce one non-empty release AAB with the canonical package/version metadata. AAB verification must not assume split APK mode is enabled.

## Failure Handling

Verification fails rather than silently accepting:

- a split ABI list that differs from the supported TagLib ABI set;
- a missing ABI APK or universal APK;
- duplicate outputs for one filter;
- an unsupported ABI output;
- a missing, mismatched, or extra TagLib native slice;
- mismatched application ID, `versionName`, or `versionCode`;
- a missing or empty release AAB;
- a signed APK that fails signature verification;
- missing required Android SDK inspection tooling when the corresponding verification cannot otherwise be completed reliably.

Failure messages must identify the artifact and expected versus actual filters, native entries, or metadata. Unsigned local release APKs are not a failure by themselves when signing credentials are absent.

## Testing and Acceptance

Implementation follows strict RED/GREEN TDD at the Gradle/build-contract level:

- first add a failing build-logic or verification test for the expected supported ABI and artifact contract;
- enable opt-in ABI splits and make the matrix test pass;
- build the direct-download release matrix and inspect every APK;
- build and inspect the release AAB independently;
- prove split mode disabled preserves the existing ordinary APK behavior;
- prove configuration-cache-compatible repeat builds succeed;
- run existing shared JVM tests, desktop compilation, Android debug assembly, and release minification checks to protect unrelated behavior;
- run `git diff --check` and strict OpenSpec validation.

Where signing credentials are available, signed artifact verification is required. Without credentials, record unsigned release output honestly and do not claim installation or signature validation. Device installation remains manual unless a compatible connected Android target is available.

## Constraints and Non-Goals

- Android application packaging only; shared application behavior is unchanged.
- Use AGP 9.3's built-in ABI split support.
- Keep `arm64-v8a`, `armeabi-v7a`, and `x86_64`; do not add or remove device support in this change.
- Keep one universal fallback APK.
- Keep the AAB as the Google Play artifact.
- Preserve canonical version name and version code across all artifacts.
- Preserve current signing configuration and secret handling.
- Do not add density splits, language splits, product flavors, legacy Play multi-APK version codes, bundletool management, custom release renaming, checksums, uploads, publishing, or release-page automation.
- Do not change TagLib source, JNI behavior, playback, scanning, UI, persistence, or non-Android platform packaging.
