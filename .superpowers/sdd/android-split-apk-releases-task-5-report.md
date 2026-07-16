# Android split APK releases — Task 5 report

Status: **DONE — acceptance passed with one focused regression fix; known iOS blocker persists**

All Gradle commands below ran sequentially. No concurrent Gradle process touched
`androidApp/build`. Repository and SDK paths are sanitized as `<repo>` and `<sdk>`; no
credentials, environment values, certificate data, or signing secrets are included.

## 5.1 Build logic and invalid-property behavior

The exact required build-logic command initially exposed a new acceptance defect:

```text
./gradlew -p build-logic :convention:test :convention:validatePlugins \
  --configuration-cache --configuration-cache-problems=fail
AabMetadataProbeTest > realAgpBundleConvertsToCanonicalSdkMetadata FAILED
Required AAB probe input is missing; pass -Prhythhaus.aabProbeFile=<release.aab>.
46 tests completed, 1 failed
BUILD FAILED in 7s
Configuration cache entry stored.
```

The complete default build-logic suite intentionally has no explicit AAB probe input, while
the real feasibility probe must still run when a path is supplied. A focused regression test
was added before the fix. Witnessed RED:

```text
./gradlew -p build-logic :convention:test \
  --tests 'com.eterocell.gradle.android.AabMetadataProbeTest' \
  --configuration-cache --configuration-cache-problems=fail
AabMetadataProbeTest.kt: unresolved reference 'optionalAabProbePath' (three references)
BUILD FAILED in 710ms
Configuration cache entry stored.
```

The narrow test-only fix treats absent/blank input as “probe not requested,” while preserving
the existing non-empty-file gate and canonical SDK metadata assertions for explicit inputs.
Focused default-suite GREEN:

```text
./gradlew -p build-logic :convention:test \
  --tests 'com.eterocell.gradle.android.AabMetadataProbeTest' \
  --configuration-cache --configuration-cache-problems=fail
Reusing configuration cache.
BUILD SUCCESSFUL in 1s
12 actionable tasks: 2 executed, 10 up-to-date
Configuration cache entry reused.
```

Fresh exact Task 5 build-logic acceptance after the fix:

```text
./gradlew -p build-logic :convention:test :convention:validatePlugins \
  --configuration-cache --configuration-cache-problems=fail
Reusing configuration cache.
BUILD SUCCESSFUL in 6s
13 actionable tasks: 1 executed, 12 up-to-date
Configuration cache entry reused.
```

The explicit real-AAB probe was also retained. One intermediate invocation passed a relative
path while using `-p build-logic`; it failed the existing-file precondition because that path
was resolved from the nested build directory. The corrected plan-equivalent absolute path
passed:

```text
AAB_PATH=$(ls "$PWD/androidApp/build/outputs/bundle/release/"*.aab) && \
./gradlew -p build-logic :convention:test \
  --tests 'com.eterocell.gradle.android.AabMetadataProbeTest' \
  -Prhythhaus.aabProbeFile="$AAB_PATH" \
  --configuration-cache --configuration-cache-problems=fail
BUILD SUCCESSFUL in 2s
12 actionable tasks: 1 executed, 11 up-to-date
Configuration cache entry stored.
```

The focused fix is commit `1398191` (`fix(build): preserve release verification
cacheability`) with the required Sisyphus footer and co-author trailer. Only
`build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AabMetadataProbeTest.kt`
was staged and committed.

Both required invalid-property commands failed actionably:

```text
./gradlew :androidApp:tasks \
  -Prhythhaus.android.abis=arm64-v8a,armeabi-v7a --stacktrace
Gradle property 'rhythhaus.android.abis' must resolve to exactly
[arm64-v8a, armeabi-v7a, x86_64] in this order; actual [arm64-v8a, armeabi-v7a].
BUILD FAILED in 508ms

./gradlew :androidApp:tasks \
  -Prhythhaus.versionCode=not-an-integer --stacktrace
Gradle property 'rhythhaus.versionCode' must be an integer, was 'not-an-integer'
BUILD FAILED in 667ms
```

## 5.2 Release-channel configuration-cache storage and reuse

A preliminary split invocation found and reused Task 4's matching cache entry. To obtain the
required fresh first-storage/second-reuse evidence, only the generated local
`.gradle/configuration-cache` directory was removed; no source or artifact output changed.

Split APK verification, first run:

```text
./gradlew :androidApp:verifyReleaseApks \
  -Prhythhaus.android.splitApk=true \
  --configuration-cache --configuration-cache-problems=fail
BUILD SUCCESSFUL in 1s
127 actionable tasks: 4 executed, 123 up-to-date
Configuration cache entry stored.
```

Same split command, unchanged second run:

```text
Reusing configuration cache.
BUILD SUCCESSFUL in 382ms
118 actionable tasks: 4 executed, 114 up-to-date
Configuration cache entry reused.
```

The split report proved the exact matrix, native slices, canonical identity, and signing:

```text
mode: split
outputs: 4
filters: [arm64-v8a, armeabi-v7a, x86_64, unfiltered]
taglib[arm64-v8a]: [lib/arm64-v8a/librhythhaus_taglib.so]
taglib[armeabi-v7a]: [lib/armeabi-v7a/librhythhaus_taglib.so]
taglib[x86_64]: [lib/x86_64/librhythhaus_taglib.so]
taglib[unfiltered]: [lib/arm64-v8a/librhythhaus_taglib.so,
  lib/armeabi-v7a/librhythhaus_taglib.so, lib/x86_64/librhythhaus_taglib.so]
applicationId: com.eterocell.rhythhaus
versionName: 0.1.0
versionCode: 100
signed: verified
```

The generated configuration-cache directory was cleared again before the independent AAB
pair. AAB verification, first run:

```text
./gradlew :androidApp:verifyReleaseAab \
  --configuration-cache --configuration-cache-problems=fail
BUILD SUCCESSFUL in 3s
128 actionable tasks: 12 executed, 5 from cache, 111 up-to-date
Configuration cache entry stored.
```

Same AAB command, unchanged second run:

```text
Reusing configuration cache.
BUILD SUCCESSFUL in 374ms
119 actionable tasks: 4 executed, 115 up-to-date
Configuration cache entry reused.
```

The independent AAB report was:

```text
outputs: 1
resources: 92
applicationId: com.eterocell.rhythhaus
versionName: 0.1.0
versionCode: 100
```

No `apksigner` command was run on the AAB. APK signing verification reported `verified`;
the local release therefore did not use the unsigned-local fallback in this run.

## 5.3 Non-exact ordinary behavior and supported regression matrix

Non-exact `True` remained disabled and generated one ordinary release APK:

```text
./gradlew :androidApp:clean :androidApp:verifyReleaseApks \
  -Prhythhaus.android.splitApk=True \
  --configuration-cache --configuration-cache-problems=fail
BUILD SUCCESSFUL in 4s
128 actionable tasks: 30 executed, 21 from cache, 77 up-to-date
Configuration cache entry stored.
```

Report:

```text
mode: ordinary
outputs: 1
filters: [unfiltered]
taglib[unfiltered]: [lib/arm64-v8a/librhythhaus_taglib.so,
  lib/armeabi-v7a/librhythhaus_taglib.so, lib/x86_64/librhythhaus_taglib.so]
applicationId: com.eterocell.rhythhaus
versionName: 0.1.0
versionCode: 100
signed: verified
```

Debug assembly remained ordinary:

```text
./gradlew :androidApp:assembleDebug \
  --configuration-cache --configuration-cache-problems=fail
BUILD SUCCESSFUL in 37s
93 actionable tasks: 33 executed, 7 from cache, 53 up-to-date
Configuration cache entry stored.
```

`androidApp/build/outputs/apk/debug/output-metadata.json` contained one `SINGLE` element,
`filters: []`, `applicationId: com.eterocell.rhythhaus`, `versionName: 0.1.0`, and
`versionCode: 100`. The directory contained one APK payload, its `.idsig` signing sidecar,
and metadata; the sidecar is not a second APK.

The supported repository matrix passed:

```text
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug \
  --configuration-cache --configuration-cache-problems=fail
BUILD SUCCESSFUL in 14s
110 actionable tasks: 12 executed, 98 up-to-date
Configuration cache entry stored.
```

## 5.4 `./init.sh`

Exact command:

```text
./init.sh
```

The script's JVM/desktop/Android stage passed and reused configuration cache:

```text
BUILD SUCCESSFUL in 425ms
101 actionable tasks: 4 executed, 97 up-to-date
Configuration cache entry reused.
```

Xcode toolchain output:

```text
Xcode 26.6
Build version 17F113
```

iOS main compilation and `iosSimulatorArm64MainKlibrary` completed, then the unchanged known
common-test blocker persisted exactly:

```text
e: <repo>/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt:64:28
   Unresolved reference 'Thread'.
e: <repo>/shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt:340:27
   Unresolved reference 'Thread'.
Execution failed for task ':shared:compileTestKotlinIosSimulatorArm64'.
BUILD FAILED in 13s
42 actionable tasks: 23 executed, 19 up-to-date
Configuration cache entry stored.
```

No iOS simulator test success is claimed, and this unrelated blocker was not modified.

## Scope, diagnostics, and blockers

- Production/runtime code, signing configuration, dependencies, shared UI, native TagLib
  behavior, non-Android packaging, `progress.md`, `roadmap.md`, and
  `.superpowers/sdd/progress.md` were not changed by Task 5.
- Existing unstaged artwork-collapse and progress-file changes were preserved and never staged.
- Kotlin LSP diagnostics remain unavailable because `kotlin-ls` is not installed and
  installation was previously declined. Gradle compilation, focused RED/GREEN, the complete
  build-logic suite, plugin validation, the explicit real-AAB probe, release verifiers, and
  the supported matrix are the executable diagnostics.
- Expected-failure commands and the incorrect relative-path probe are retained above rather
  than hidden.
- Blocker: `./init.sh` remains nonzero only because of the unchanged iOS common-test JVM-only
  `Thread` references. No Android release, build-logic, JVM, desktop, debug, signing, or
  configuration-cache blocker remains for Task 5.

## Independent focused review

The independent review of commit `1398191` passed the implementation with no Critical or
Important code defect: one test file changed, absent/blank input leaves the default suite
runnable, explicit nonblank input still requires an existing non-empty AAB, and SDK-derived
canonical identity verification remains intact. The reviewer initially could not discover
the regression RED/GREEN evidence from the OpenSpec ledger because this required report is
ignored under `.superpowers/`; the Task 5 section in `tasks.md` now links directly to this
report. The same reviewer then rechecked the linked evidence and returned `PASS`, with no
Critical or Important finding remaining for OpenSpec 5.5. No code change was required by the
review.
