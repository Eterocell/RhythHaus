# Android Split APK Releases - Task 3 Report

## Scope

- Base: `00ca186e6473bf0456da0c3bce7cbb480786d4be`
- Route: OpenSpec + Superpowers SDD Task 3
- Added pure release APK validators and cacheable `:androidApp:verifyReleaseApks`.
- Consumed release APKs only through AGP `SingleArtifact.APK`, `BuiltArtifactsLoader`, metadata filters, and each `BuiltArtifact.outputFile`.
- Did not implement AAB verification, alter signing configuration, output names/version codes, runtime/native sources, roadmap, root progress, or OpenSpec artifacts.

## Strict RED/GREEN

### Initial pure-verifier RED

Command:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest'
```

Result: expected `:convention:compileTestKotlin` failure. The compiler reported unresolved `ReleaseArtifactIdentity`, `ReleaseApkDescriptor`, `validateReleaseApkSet`, `expectedTagLibEntries`, `readTagLibEntries`, `validateApkTagLibEntries`, `parseApkAnalyzerIdentity`, and signing/sanitization APIs before production code existed. `BUILD FAILED in 3s`.

### Signing-policy RED

Command:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest.signingMatrixRequiresSuccessfulApksignerOnlyWhenSigningIsConfigured'
```

Result: expected assertion failure because a successfully verified APK without configured credentials was initially reported as unsigned. The minimal correction made successful verification report `verified`, while failed/unavailable verification remains acceptable only when signing is not configured.

### Pure-verifier GREEN

Command:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest'
```

Final result: pass, `BUILD SUCCESSFUL in 2s`; configuration cache reused. Coverage includes ordinary/split matrices, missing/duplicate/unsupported/density/multiple filters, exact TagLib slices, missing/extra slices, unrelated native libraries, trimmed analyzer output, numeric `000100 -> 100`, noninteger/mismatched identity, signing matrix, and sanitized tool failures.

### Review-finding duplicate ZIP RED/GREEN

Independent review identified that converting matching ZIP names directly to a set could hide duplicate exact TagLib entries. A malformed synthetic APK regression was added by rewriting equal-length ZIP local/central-directory entry names.

RED command:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest.zipInspectionRejectsDuplicateExactTagLibEntries'
```

Result: expected assertion failure at `AndroidReleaseVerificationTest.kt:128` because the set-based reader accepted the duplicate; `BUILD FAILED in 2s`.

GREEN implementation rejects duplicate matching names before set conversion. Final full verification after the fix:

- Pure suite: pass, `BUILD SUCCESSFUL in 1s`; configuration cache reused.
- Ordinary verification: pass, `BUILD SUCCESSFUL in 1m 6s`; configuration cache stored; 128 actionable tasks (118 executed, 10 up-to-date).
- Split verification: pass, `BUILD SUCCESSFUL in 44s`; configuration cache stored; 128 actionable tasks (34 executed, 17 from cache, 77 up-to-date).

## Actual Ordinary Release

Command:

```text
./gradlew :androidApp:clean :androidApp:verifyReleaseApks --configuration-cache --configuration-cache-problems=fail
```

Final result: pass, `BUILD SUCCESSFUL in 59s`; configuration cache stored; 128 actionable tasks (118 executed, 10 up-to-date).

Report:

```text
mode: ordinary
outputs: 1
filters: [unfiltered]
taglib[unfiltered]: [lib/arm64-v8a/librhythhaus_taglib.so, lib/armeabi-v7a/librhythhaus_taglib.so, lib/x86_64/librhythhaus_taglib.so]
applicationId: com.eterocell.rhythhaus
versionName: 0.1.0
versionCode: 100
signed: verified
```

## Actual Split Release

Command:

```text
./gradlew :androidApp:clean :androidApp:verifyReleaseApks -Prhythhaus.android.splitApk=true --configuration-cache --configuration-cache-problems=fail
```

Final result: pass, `BUILD SUCCESSFUL in 43s`; configuration cache stored; 128 actionable tasks (34 executed, 17 from cache, 77 up-to-date).

Report:

```text
mode: split
outputs: 4
filters: [arm64-v8a, armeabi-v7a, x86_64, unfiltered]
taglib[arm64-v8a]: [lib/arm64-v8a/librhythhaus_taglib.so]
taglib[armeabi-v7a]: [lib/armeabi-v7a/librhythhaus_taglib.so]
taglib[x86_64]: [lib/x86_64/librhythhaus_taglib.so]
taglib[unfiltered]: [lib/arm64-v8a/librhythhaus_taglib.so, lib/armeabi-v7a/librhythhaus_taglib.so, lib/x86_64/librhythhaus_taglib.so]
applicationId: com.eterocell.rhythhaus
versionName: 0.1.0
versionCode: 100
signed: verified
```

## Tooling, Cache, and Diagnostics

- `apkanalyzer` resolves from AGP's configured SDK provider, preferring `cmdline-tools/latest/bin/apkanalyzer` and then deterministic numeric version-directory probing. Every APK runs `manifest application-id`, `manifest version-name`, and `manifest version-code`.
- `apksigner` resolves only from AGP's configured build-tools revision and runs `verify --verbose -Werr`; `--print-certs` is never used.
- Tool stderr is captured but never included in errors or reports. Reports contain no paths, environment values, credentials, or certificate output.
- The cacheable task declares artifact directory, ABI/mode, identity, signing presence, SDK/build-tools inputs, and report output. It uses injected `ExecOperations`; the task action does not access `project`.
- Both ordinary and split final commands passed with `--configuration-cache-problems=fail` and stored cache entries. The pure test rerun reused its configuration cache.
- `GIT_MASTER=1 git diff --check`: pass with no output.
- `lsp_diagnostics` was attempted for all four modified Kotlin/Kotlin DSL files. Kotlin diagnostics are unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle Kotlin compilation and tests passed.
- Existing warnings remain: Android host tests are not enabled for `commonTest`; the Android Media3 artwork setter is deprecated; Gradle reports existing deprecations. The AboutLibraries task also reported exhausted GitHub rate limit during one clean build, without affecting release verification.

## Commit and Concerns

- Intended commit: `build(android): verify release APK artifacts` with the required Sisyphus footer and co-author trailer.
- Coordinator-owned `.superpowers/sdd/progress.md` and `openspec/changes/android-split-apk-releases/tasks.md` were present before Task 3 and remain unmodified by this implementation and unstaged.
- AAB feasibility and verification remain explicitly deferred to Task 4.
- Current local outputs were verifiably signed, so the actual reports say `signed: verified`; pure tests cover the credentials-absent unsigned-local branch.
- Independent review's only implementation Important finding (duplicate exact ZIP entries) was resolved through the RED/GREEN regression above. Its scope warning referred to coordinator-owned files that predated Task 3; those files were neither modified nor staged by this implementation.

## Task 3 Review Fixes - AGP Metadata Seam and Precise Tool Inputs

### P1: Pure AGP metadata identity validation

Root cause: `VerifyReleaseApksTask` compared `BuiltArtifacts.applicationId` and each `BuiltArtifact.versionName`/`versionCode` inline. Analyzer identity had pure tests, but AGP metadata mapping and mismatches did not have a separately testable seam.

Focused RED command:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest.agpMetadata*'
```

Result: expected `:convention:compileTestKotlin` failure on unresolved `releaseArtifactIdentityFromAgpMetadata` and `validateAgpReleaseArtifactIdentity`; `BUILD FAILED in 4s`.

GREEN:

- Added pure AGP metadata mapping to `ReleaseArtifactIdentity`, including missing nullable version metadata rejection.
- Added independent application ID, version name, and version code mismatch tests plus a successful mapping test.
- `VerifyReleaseApksTask` now calls the pure seam for every AGP output. The former three inline checks were removed.
- APK-internal identity remains independently validated through the three SDK `apkanalyzer manifest` commands.
- Focused AGP metadata tests: pass, `BUILD SUCCESSFUL in 4s`; configuration cache reused.
- Full `AndroidReleaseVerificationTest`: pass, `BUILD SUCCESSFUL in 924ms`; configuration cache reused.

### P2: Precise cacheable SDK tool inputs

Root cause: the cacheable task declared the complete SDK root as `@InputDirectory` with `PathSensitivity.ABSOLUTE`, fingerprinting unrelated SDK contents and its absolute path.

Fix:

- Removed the SDK directory and build-tools revision from the task input model.
- Added required `apkAnalyzerExecutable` as `@InputFile` and optional `apkSignerExecutable` as `@Optional @InputFile`; both use `PathSensitivity.NONE`, so Gradle fingerprints the actual executable contents without the absolute SDK path.
- Configuration resolves both providers from AGP `sdkComponents.sdkDirectory`. `apkanalyzer` retains latest-then-deterministic-version probing; `apksigner` remains limited to AGP's configured build-tools revision.
- The task action executes only those declared files and retains the existing signing matrix. No secret, credential, local-properties, path, environment, or certificate output was added.
- Task realization with `--configuration-cache-problems=fail`: pass, `BUILD SUCCESSFUL in 8s`; configuration cache stored.

### Final verification after both fixes

```text
./gradlew -p build-logic :convention:compileKotlin :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest'
./gradlew :androidApp:clean :androidApp:verifyReleaseApks --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:clean :androidApp:verifyReleaseApks -Prhythhaus.android.splitApk=true --configuration-cache --configuration-cache-problems=fail
```

Results:

- Pure compile/suite: pass, `BUILD SUCCESSFUL in 1s`; configuration cache reused.
- Ordinary verification: pass, `BUILD SUCCESSFUL in 53s`; configuration cache stored; 128 actionable tasks (118 executed, 10 up-to-date).
- Split verification: pass, `BUILD SUCCESSFUL in 32s`; configuration cache stored; 128 actionable tasks (34 executed, 17 from cache, 77 up-to-date).
- Split report remains four metadata-derived outputs with exact TagLib slices, identity `com.eterocell.rhythhaus` / `0.1.0` / `100`, and `signed: verified`.
- `GIT_MASTER=1 git diff --check`: pass with no output.
- `lsp_diagnostics` was attempted on all four modified Kotlin/Kotlin DSL files; Kotlin diagnostics remain unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation and tests are green.
- Existing unrelated warnings are unchanged: Android host-test configuration, Gradle deprecations, Android Media3 artwork API deprecation, and the AboutLibraries GitHub rate-limit notice.
- Coordinator-owned `.superpowers/sdd/progress.md` and `openspec/changes/android-split-apk-releases/tasks.md` remain untouched by this fix and unstaged. AAB verification remains Task 4 scope.
