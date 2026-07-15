# Android split APK releases — Task 4 report

Status: **BLOCKED — hard feasibility gate did not pass**

## Scope and baseline

- Base/HEAD: `8630c6a1e3025281c88c430dec51aacf0889dc4f` on `main`.
- AGP: `9.3.0`.
- Pre-existing coordinator changes in `.superpowers/sdd/progress.md` and
  `openspec/changes/android-split-apk-releases/tasks.md` were not edited or staged.
- No production AAB verifier, `SingleArtifact.BUNDLE` registration, custom packaging,
  dependency, signing action, filename/config/APK-metadata fallback, or design change was made.

## Real AGP AAB

Command:

```text
./gradlew :androidApp:clean :androidApp:bundleRelease \
  --configuration-cache --configuration-cache-problems=fail
```

Sanitized result:

```text
BUILD SUCCESSFUL in 4s
128 actionable tasks: 31 executed, 20 from cache, 77 up-to-date
Configuration cache entry stored.
```

The probe input was the resulting non-empty AGP release AAB under the standard
`androidApp/build/outputs/bundle/release/` directory. No artifact filename was used as
identity evidence.

## Contract RED

Before any conversion helper existed, the focused probe test required an explicit
existing, non-empty AAB and asserted canonical SDK-derived identity only.

Command:

```text
./gradlew -p build-logic :convention:test \
  --tests 'com.eterocell.gradle.android.AabMetadataProbeTest' \
  -Prhythhaus.aabProbeFile='<repo>/androidApp/build/outputs/bundle/release/<release-aab>'
```

Sanitized RED:

```text
AabMetadataProbeTest.kt: unresolved reference 'probeAabMetadata'
BUILD FAILED
```

This was the expected test-first failure: no production verifier or conversion helper
existed.

## Required exact-two-entry conversion attempt

The minimal test-only helper then performed exactly this flow in a temporary directory:

1. Open the real AAB with `ZipFile`.
2. Copy only `base/manifest/AndroidManifest.xml` to root `AndroidManifest.xml`.
3. Copy only `base/resources.pb` to root `resources.pb`.
4. Run configured SDK `aapt2 convert --output-format binary -o <temp-output> <temp-proto>`.
5. Only after successful conversion, run configured SDK `apkanalyzer manifest application-id`,
   `version-name`, and `version-code` on the converted archive.

The focused Gradle command reused its configuration cache but failed at `aapt2`:

```text
1 test completed, 1 failed
IllegalArgumentException: aapt2 failed; rerun locally for diagnostic output.
Configuration cache entry reused.
BUILD FAILED
```

A direct temporary reproduction of the same two-entry archive captured the sanitized SDK
cause:

```text
exit: 1
<temp>/base-proto.apk: error: no file associated with (file)
res/drawable-v24/$ic_launcher_foreground__0.xml type=protoXML.
```

No repository path, SDK path, credential, certificate, or secret is included above.

## Root-cause control

One temporary diagnostic control added the real AAB's 92 `base/res/**` payload entries in
addition to the manifest and resource table. This was not accepted as proof and was not
implemented because it violates the approved exact-two-entry contract. It established the
cause rather than offering a substitute:

```text
payload entries: 92
convert exit: 0
application-id exit: 0 value: com.eterocell.rhythhaus
version-name exit: 0 value: 0.1.0
version-code exit: 0 value: 100
```

Therefore the configured SDK tools and the AAB manifest identity are usable, but AGP 9.3's
`base/resources.pb` references packaged `base/res/**` files. An archive containing precisely
the two mandated entries is not self-contained and cannot pass `aapt2 convert` for this real
AAB.

## Stop decision and omitted work

The required feasibility path did not become GREEN. Per the Task 4 hard stop:

- `VerifyReleaseAabTask` was not created.
- No task-contract tests beyond the discarded feasibility probe were retained.
- `androidApp/build.gradle.kts` was not changed.
- `SingleArtifact.BUNDLE` was not registered.
- No with/without-split AAB task matrix or task cache claim is made.
- No `apksigner` command was run on the AAB.
- No diagnostics claim is made because no Kotlin/Gradle implementation file remains modified.
- No implementation commit was created or amended, and nothing was pushed. The blocked
  evidence report was committed separately as required.

The temporary test/helper and property forwarding were removed after the failed gate. The
repository retains no weaker verifier implementation. Continuing requires explicit user
approval of a design revision, such as allowing the resource payloads required by the proto
resource table; this report does not approve or implement that alternative.

## Concerns

- The approved exact-two-entry archive contract is incompatible with the observed real AGP
  9.3 bundle resource table.
- Any implementation that silently copies referenced resources, reads source configuration,
  uses APK/AGP metadata as proof, or derives identity from filenames would change the approved
  design and is intentionally absent.
