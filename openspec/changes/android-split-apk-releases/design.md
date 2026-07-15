## Context

The Android app currently produces one universal APK and a release AAB. The universal APK packages native TagLib libraries for `arm64-v8a`, `armeabi-v7a`, and `x86_64`. Direct downloads therefore carry native code for architectures the target device does not use.

AGP 9.3 provides built-in ABI APK splits and a universal fallback. RhythHaus needs those direct-download artifacts without changing ordinary debug/IDE builds, canonical versioning, existing signing configuration, or the Play AAB channel.

## Goals / Non-Goals

**Goals:**

- Generate three ABI APKs plus one universal APK only when explicitly requested.
- Preserve the independent release AAB for Google Play.
- Keep native generation, split configuration, and verification on one strict ABI contract.
- Verify artifact filters, native contents, identity, versions, and signed outputs.
- Preserve configuration-cache-compatible ordinary and release builds.

**Non-Goals:**

- Density/language splits, product flavors, bundletool management, or legacy Play multi-APK upload.
- ABI-specific version codes, custom names, checksums, publishing, or release-page automation.
- Signing-policy, TagLib source/JNI, runtime, UI, or non-Android packaging changes.

## Decisions

### One strict root ABI property

`rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64` is the sole editable ABI contract. A shared build-logic parser trims values, rejects blanks/duplicates, and requires exactly this ordered set. The TagLib native loop, Android application split DSL, and verification tasks all consume it.

Alternative rejected: independent hardcoded lists. They can drift and produce APKs without matching native libraries.

### Opt-in built-in AGP splits

`rhythhaus.android.splitApk=true` enables `splits.abi`, resets the implicit ABI list, includes the parsed supported set, and enables the universal APK. Any other value leaves split mode disabled and preserves existing APK shape.

Alternative rejected: always-on splits. It would unexpectedly change IDE/debug and existing verification outputs. Bundletool-derived or custom packaging adds tooling and maintenance not required by the roadmap.

### Canonical version metadata across channels

All APK and AAB outputs retain `rhythhaus.versionName` and `rhythhaus.versionCode`. ABI version-code offsets are not added because direct-download APKs are not a Play multi-APK set.

### Metadata-driven artifact verification

Verification classifies APK outputs through AGP `output-metadata.json`, checks native ZIP entries, and uses configured SDK `apkanalyzer` for package and version metadata. It requires one output per supported ABI plus one universal output, and independently checks one non-empty release AAB.

AAB identity verification builds a temporary proto archive from `base/manifest/AndroidManifest.xml`, `base/resources.pb`, and all packaged `base/res/**` payloads, preserving resource paths relative to `base/`. SDK `aapt2` converts that self-contained archive before `apkanalyzer` reads package and version identity. A real AGP 9.3 feasibility run proved that the manifest and resource table alone fail conversion because the table references packaged resources; including the base resource payloads succeeds. The verifier does not use filenames, source configuration, APK metadata, or AGP task inputs as identity evidence.

Signed APKs additionally pass SDK `apksigner verify`. Unsigned local release outputs are accepted only when signing credentials are absent; verification never prints secrets.

## Risks / Trade-offs

- **AGP output metadata shape changes** → Isolate parsing in one tested build-logic component and fail with the metadata path plus unsupported schema details.
- **Configuration-time property parsing can break all builds** → Parse the ABI property strictly but keep split activation opt-in; test missing/blank/duplicate/unsupported inputs with TestKit.
- **Release verification depends on Android SDK tools** → Resolve tools from the configured SDK and fail with an actionable installation/path error when metadata cannot be verified otherwise.
- **Four release APKs increase build time and storage** → Generate them only in explicit split mode; default debug and release workflows remain unchanged.
- **Unsigned local outputs cannot prove release signatures** → Record unsigned status honestly and require `apksigner` only when signing is configured and present.
- **The AAB resource table references packaged payloads** → Copy all `base/res/**` entries into an isolated temporary proto archive, reject unsafe paths, and leave the release bundle unchanged.

## Migration Plan

1. Add the strict ABI property and parser while preserving existing native outputs.
2. Make TagLib consume the parsed ABI contract and verify no native support changes.
3. Add opt-in application ABI splits and verify default mode remains unchanged.
4. Add APK/AAB verification tasks and run both release channels.
5. Roll back by disabling/removing the opt-in split configuration; the AAB and default universal APK paths remain intact throughout.

## Open Questions

None. Artifact set, versioning, signing behavior, verification tooling, and non-goals are approved.
