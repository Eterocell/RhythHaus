# Android Split APK Releases Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Use `superpowers:test-driven-development` for each production change and `superpowers:verification-before-completion` before completion. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add opt-in ABI split release APKs for `arm64-v8a`, `armeabi-v7a`, and `x86_64`, plus a universal APK, while preserving the independent Google Play AAB, canonical metadata, signing behavior, ordinary builds, and configuration-cache compatibility.

**Architecture:** A narrow binary build-logic plugin owns the strict ABI contract and makes it available to both `taglib` and `androidApp`. `androidApp` uses AGP 9.3's built-in ABI split DSL and variant-aware verification tasks that classify outputs with `BuiltArtifactsLoader`, inspect native ZIP entries, and inspect metadata with configured Android SDK tools. The AAB verifier stays independent from split APK verification and must first prove its SDK-only conversion path against a real AGP AAB.

**Tech Stack:** Gradle 9.6.1, Kotlin DSL, Kotlin 2.4.10, AGP 9.3.0, Gradle TestKit, JUnit 4/Kotlin Test, AGP Variant API, `BuiltArtifactsLoader`, `SingleArtifact.APK`, `SingleArtifact.BUNDLE`, Java `ZipFile`, Android SDK `aapt2`, `apkanalyzer`, and `apksigner`.

## Global Constraints

- The sole editable ABI contract is exactly `rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64`.
- Split mode is enabled only by exact lowercase `rhythhaus.android.splitApk=true`. Absent and every other value mean disabled.
- Split mode emits exactly three ABI-filtered APKs and one unfiltered universal APK. Disabled mode emits one ordinary unfiltered APK.
- Use AGP 9.3 built-in `splits.abi`, never custom packaging, product flavors, density/language splits, output renaming, ABI version-code offsets, bundletool, checksums, uploads, publishing, or release automation.
- Preserve `applicationId=com.eterocell.rhythhaus`, canonical `rhythhaus.versionName`, numeric `rhythhaus.versionCode`, and `configureAppSigningConfigsForRelease()` with its existing secret handling.
- Classify APK outputs through AGP `BuiltArtifactsLoader` metadata, never filenames. Inspect native slices as ZIP entries.
- Signed APKs require `apksigner verify`; an unsigned APK is accepted only when release signing is not configured. Never print credentials, `local.properties`, command environments, or certificate data.
- `bundleRelease` and `verifyReleaseAab` remain independent from split mode and APK verification. Never run `apksigner` on an AAB.
- The AAB SDK conversion path uses root `AndroidManifest.xml`, root `resources.pb`, and all packaged `base/res/**` payloads required by that resource table. The manifest and table alone are known not to be self-contained under AGP 9.3. Never substitute filenames, source configuration, APK metadata, or AGP task inputs as AAB proof.
- Preserve runtime Kotlin, TagLib source/JNI behavior, UI, playback, scanning, persistence, dependencies, and non-Android packaging. Preserve unrelated worktree changes. Do not archive this OpenSpec change without an explicit request.
- Every production change begins with a witnessed RED test. Every commit is atomic, uses its listed Conventional Commit subject, and includes both `Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)` and `Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>`.

---

## File Structure

| Path | Responsibility |
|---|---|
| `gradle.properties` | Checked-in strict ABI value only, not the split opt-in. |
| `build-logic/convention/build.gradle.kts` | Binary plugin declaration and existing-library TestKit dependencies. |
| `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContract.kt` | ABI constants, parser, exact split switch, typed extension. |
| `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContractPlugin.kt` | Contract extension registration. |
| `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidReleaseVerification.kt` | Pure artifact, native-entry, metadata, signing, and SDK-result validators. |
| `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/VerifyReleaseApksTask.kt` | Metadata-driven APK verification and deterministic report. |
| `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/VerifyReleaseAabTask.kt` | Independent AAB base conversion, SDK metadata verification, and report. |
| `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/` | Parser/TestKit, pure verifier, and real AAB feasibility coverage. |
| `taglib/build.gradle.kts` | Uses the shared typed ABI contract in the Android native loop. |
| `androidApp/build.gradle.kts` | Applies the contract, configures AGP splits, and wires release Variant API verification. |
| `openspec/changes/android-split-apk-releases/tasks.md` | Unchecked implementation and review ledger until evidence exists. |
| `roadmap.md`, `progress.md` | Final evidence only. Update only roadmap item 22, preserving item 21 byte-for-byte. |

## Shared Interfaces

All later tasks use these names and types.

```kotlin
package com.eterocell.gradle.android

const val RHYTHHAUS_ANDROID_ABIS_PROPERTY = "rhythhaus.android.abis"
const val RHYTHHAUS_ANDROID_SPLIT_APK_PROPERTY = "rhythhaus.android.splitApk"
val APPROVED_RHYTHHAUS_ANDROID_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64")

fun parseRhythHausAndroidAbis(rawValue: String?): List<String>
fun isRhythHausSplitApkEnabled(rawValue: String?): Boolean = rawValue == "true"

abstract class RhythHausAndroidAbiContractExtension {
    abstract val abis: ListProperty<String>
    abstract val splitApkEnabled: Property<Boolean>
}

data class ReleaseArtifactIdentity(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
)

data class ReleaseApkDescriptor(
    val file: File,
    val abi: String?,
    val metadataIdentity: ReleaseArtifactIdentity,
)
```

```kotlin
@CacheableTask
abstract class VerifyReleaseApksTask : DefaultTask() {
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty
    @get:Internal abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>
    @get:Input abstract val supportedAbis: ListProperty<String>
    @get:Input abstract val splitApkEnabled: Property<Boolean>
    @get:Input abstract val expectedApplicationId: Property<String>
    @get:Input abstract val expectedVersionName: Property<String>
    @get:Input abstract val expectedVersionCode: Property<Int>
    @get:Input abstract val releaseSigningConfigured: Property<Boolean>
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val apkAnalyzerExecutable: RegularFileProperty
    @get:Optional @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val apkSignerExecutable: RegularFileProperty
    @get:OutputFile abstract val reportFile: RegularFileProperty
}

@CacheableTask
abstract class VerifyReleaseAabTask : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aabFile: RegularFileProperty
    @get:Input abstract val expectedApplicationId: Property<String>
    @get:Input abstract val expectedVersionName: Property<String>
    @get:Input abstract val expectedVersionCode: Property<Int>
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val aapt2Executable: RegularFileProperty
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val apkAnalyzerExecutable: RegularFileProperty
    @get:OutputFile abstract val reportFile: RegularFileProperty
}
```

Inject `ExecOperations` into both task constructors. Task actions must not call `project.exec` or access `project`.

---

### Task 1: Establish the Shared Android ABI Contract

**Files:**
- Modify: `gradle.properties`, `build-logic/convention/build.gradle.kts`, `taglib/build.gradle.kts`
- Create: `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContract.kt`
- Create: `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContractPlugin.kt`
- Create: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractTest.kt`
- Create: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractPluginFunctionalTest.kt`

**Consumes:** Approved ABI value and existing `taglib` Android native task wiring.

**Produces:** Plugin ID `build-logic.android.abi-contract`, `RhythHausAndroidAbiContractExtension`, strict parser, and exact split switch.

- [ ] **Step 1: Write failing parser and opt-in tests**

```kotlin
@Test fun approvedOrderedValueIsAccepted() =
    assertEquals(APPROVED_RHYTHHAUS_ANDROID_ABIS,
        parseRhythHausAndroidAbis("arm64-v8a,armeabi-v7a,x86_64"))

@Test fun splitModeRequiresExactLowercaseTrue() {
    assertTrue(isRhythHausSplitApkEnabled("true"))
    listOf(null, "", "TRUE", "True", " true", "true ", "1", "yes").forEach {
        assertFalse(isRhythHausSplitApkEnabled(it))
    }
}
```

Add separate tests for trimmed accepted entries, missing/blank values, blank entries, duplicates, reordering, unsupported ABI, and reduced ABI set. Assert these exact message fragments: `Missing required Gradle property 'rhythhaus.android.abis'; expected 'arm64-v8a,armeabi-v7a,x86_64'.`, `contains a blank ABI entry`, `contains duplicate ABI(s): arm64-v8a`, and `must resolve to exactly [arm64-v8a, armeabi-v7a, x86_64] in this order`.

- [ ] **Step 2: Witness RED**

Run: `./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractTest' --stacktrace`

Expected: test compilation fails only because the new contract symbols are unresolved.

- [ ] **Step 3: Implement the minimum typed plugin and parser**

```kotlin
class AndroidAbiContractPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<RhythHausAndroidAbiContractExtension>(
            "rhythHausAndroidAbiContract",
        )
        extension.abis.convention(providers.gradleProperty(RHYTHHAUS_ANDROID_ABIS_PROPERTY)
            .map(::parseRhythHausAndroidAbis)
            .orElse(providers.provider { parseRhythHausAndroidAbis(null) }))
        extension.splitApkEnabled.convention(providers
            .gradleProperty(RHYTHHAUS_ANDROID_SPLIT_APK_PROPERTY)
            .map(::isRhythHausSplitApkEnabled).orElse(false))
        extension.abis.finalizeValueOnRead()
        extension.splitApkEnabled.finalizeValueOnRead()
    }
}
```

The parser must split, trim, reject blank entries, reject duplicate names, compare the complete list to `APPROVED_RHYTHHAUS_ANDROID_ABIS`, then return that list. Declare the `gradlePlugin` entry with ID `build-logic.android.abi-contract` and implementation class `com.eterocell.gradle.android.AndroidAbiContractPlugin`; add only existing `gradleTestKit()`, `libs.junit`, `libs.kotlin.test`, and `libs.kotlin.testJunit` dependencies. Add `rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64` to `gradle.properties`, never the split property.

- [ ] **Step 4: Witness parser GREEN**

Run: `./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractTest'`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Write and run TestKit consumer RED/GREEN coverage**

Use `withPluginClasspath()` and a fixture that applies `id("build-logic.android.abi-contract")`, retrieves `RhythHausAndroidAbiContractExtension`, and prints its ABI list and split flag. Cover approved, missing, duplicate, reordered, `TRUE`, and exact `true` properties. Invoke the same fixture twice with one TestKit directory and assert the second output contains `Reusing configuration cache.`

Run: `./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractPluginFunctionalTest'`

Expected RED before plugin metadata is complete, then GREEN with `BUILD SUCCESSFUL` and `Reusing configuration cache.`

- [ ] **Step 6: Make TagLib consume the contract and validate repository behavior**

Apply `id("build-logic.android.abi-contract")` directly in `taglib/build.gradle.kts` and replace only the editable ABI list with:

```kotlin
val androidTagLibAbis = extensions
    .getByType<RhythHausAndroidAbiContractExtension>()
    .abis
    .get()
```

Keep the exhaustive ABI-to-NDK-triple `when` as a mapping, not a second contract. Run:

```bash
./gradlew :taglib:tasks -Prhythhaus.android.abis=arm64-v8a,x86_64 --stacktrace
./gradlew :taglib:tasks --all --configuration-cache
```

Expected: the first command fails with the expected-versus-actual ABI message, the second succeeds with the three existing `buildAndroidTagLibHelper-<abi>` tasks.

- [ ] **Step 7: Commit the isolated contract**

```bash
git add gradle.properties build-logic/convention/build.gradle.kts \
  build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContract.kt \
  build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContractPlugin.kt \
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractTest.kt \
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractPluginFunctionalTest.kt taglib/build.gradle.kts
```

```bash
git commit -m "build: centralize Android ABI contract" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

---

### Task 2: Configure Exact Opt-In AGP ABI Splits

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Modify: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractTest.kt`

**Consumes:** `RhythHausAndroidAbiContractExtension.abis` and `.splitApkEnabled`.

**Produces:** AGP 9.3 ABI splits with no output-name or version-code customization.

- [ ] **Step 1: Witness RED from the missing split artifact matrix**

Before changing `androidApp`, build release with the exact split opt-in and inspect AGP `output-metadata.json`:

```bash
./gradlew :androidApp:clean :androidApp:assembleRelease \
  -Prhythhaus.android.splitApk=true \
  --configuration-cache --configuration-cache-problems=fail
```

Expected RED: the build succeeds but AGP metadata contains the existing one unfiltered APK instead of the required three ABI outputs plus universal APK. Record the metadata element/filter summary before production edits. Task 1 already proves exact property parsing; do not duplicate that unit RED.

- [ ] **Step 2: Apply the AGP built-in split DSL**

Apply `id("build-logic.android.abi-contract")` directly and use its typed values in `androidApp/build.gradle.kts`:

```kotlin
val androidAbiContract = extensions.getByType<RhythHausAndroidAbiContractExtension>()
val splitApkEnabled = androidAbiContract.splitApkEnabled.get()
val supportedAndroidAbis = androidAbiContract.abis.get()

android {
    splits {
        abi {
            isEnable = splitApkEnabled
            if (splitApkEnabled) {
                reset()
                include(*supportedAndroidAbis.toTypedArray())
                isUniversalApk = true
            }
        }
    }
}
```

Do not call `reset`, `include`, or set `isUniversalApk` while disabled. Do not add output callbacks, output names, flavors, or version-code overrides.

- [ ] **Step 3: Prove split and disabled artifact shapes**

```bash
./gradlew :androidApp:tasks --all --configuration-cache
./gradlew :androidApp:clean :androidApp:assembleRelease -Prhythhaus.android.splitApk=true --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:clean :androidApp:assembleRelease --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:clean :androidApp:assembleRelease -Prhythhaus.android.splitApk=TRUE --configuration-cache --configuration-cache-problems=fail
```

Expected: all commands succeed. Inspect release `output-metadata.json` only to confirm four elements in exact mode and one unfiltered element otherwise. Do not encode observed filenames in production code or tests.

- [ ] **Step 4: Commit split configuration**

```bash
git add androidApp/build.gradle.kts build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractTest.kt
```

```bash
git commit -m "build(android): add opt-in ABI splits" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

---

### Task 3: Verify Release APK Artifacts from AGP Metadata

**Files:**
- Create: `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidReleaseVerification.kt`
- Create: `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/VerifyReleaseApksTask.kt`
- Create: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidReleaseVerificationTest.kt`
- Modify: `androidApp/build.gradle.kts`

**Consumes:** AGP release `SingleArtifact.APK`, `BuiltArtifactsLoader`, ABI contract, canonical versions, configured SDK/build-tools providers, and release signing configuration.

**Produces:** `:androidApp:verifyReleaseApks` and a deterministic credential-free report.

- [ ] **Step 1: Write failing pure verifier tests**

Use synthetic descriptors and temporary ZIPs. Cover split acceptance of exactly three ABI outputs and one unfiltered output; missing, duplicate, unsupported, density, and multiple filters; disabled acceptance of one unfiltered output only; expected-versus-actual filter failures; exact TagLib slices per ABI and all three slices in universal; missing or extra slices; unrelated native libraries ignored; metadata application ID/version mismatch; trimmed `apkanalyzer` output; noninteger version code; and the signing matrix.

Only `lib/<abi>/librhythhaus_taglib.so` participates in native-slice validation. A configured signing config plus failed or unavailable `apksigner` fails. No signing config plus failed or unavailable `apksigner` reports `signed: unsigned local release` without failure. No result may contain environments or credentials.

- [ ] **Step 2: Witness RED**

Run: `./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest'`

Expected: test compilation fails because verifier types and functions are unresolved.

- [ ] **Step 3: Implement pure validators and cacheable APK task**

Use `ReleaseArtifactIdentity`, `ReleaseApkDescriptor`, `expectedTagLibEntries`, `readTagLibEntries`, `validateReleaseApkSet`, `validateApkTagLibEntries`, and `parseApkAnalyzerIdentity`. Convert the canonical version code with:

```kotlin
val expectedVersionCode = canonicalVersionCode.toIntOrNull()
    ?: throw GradleException(
        "Gradle property 'rhythhaus.versionCode' must be an integer, was '$canonicalVersionCode'",
    )
```

`VerifyReleaseApksTask` must load `BuiltArtifacts` from `apkDirectory` with `builtArtifactsLoader`, require release metadata, convert each filter list to exactly one nullable ABI, reject unknown/multiple filters, resolve only `BuiltArtifact.outputFile`, require non-empty files, validate metadata and matrix, inspect ZIPs, run all three `apkanalyzer manifest` commands, apply the signing matrix, and write stable report lines.

Resolve `apkanalyzer` first at `<sdk>/cmdline-tools/latest/bin/apkanalyzer`, then deterministically probe versioned `cmdline-tools/*/bin/apkanalyzer`; do not silently use legacy `sdk/tools/bin/apkanalyzer` unless a probe proves it works. Resolve `apksigner` from AGP's configured build-tools revision, not a lexicographic newest directory. Use `apksigner verify --verbose -Werr <apk>`, never `--print-certs`.

- [ ] **Step 4: Register from the release Variant API**

```kotlin
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        tasks.register<VerifyReleaseApksTask>("verifyReleaseApks") {
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            supportedAbis.set(androidAbiContract.abis)
            splitApkEnabled.set(androidAbiContract.splitApkEnabled)
            expectedApplicationId.set("com.eterocell.rhythhaus")
            expectedVersionName.set(rhythHausVersionName)
            expectedVersionCode.set(rhythHausVersionCode.map { it.toIntOrNull()
                ?: throw GradleException("Gradle property 'rhythhaus.versionCode' must be an integer, was '$it'") })
            releaseSigningConfigured.set(android.buildTypes.getByName("release").signingConfig != null)
            reportFile.set(layout.buildDirectory.file("reports/androidReleaseVerification/release-apks.txt"))
        }
    }
}
```

Wire SDK tools from AGP `sdkComponents` providers, not `local.properties`. The artifact provider supplies production dependencies, so do not add task-name `dependsOn` calls.

- [ ] **Step 5: Witness GREEN and exercise ordinary and split outputs**

```bash
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidReleaseVerificationTest'
./gradlew :androidApp:clean :androidApp:verifyReleaseApks --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:clean :androidApp:verifyReleaseApks -Prhythhaus.android.splitApk=true --configuration-cache --configuration-cache-problems=fail
```

Expected: pure tests pass. Ordinary report is `mode: ordinary`, `outputs: 1`, `filters: [unfiltered]`. Split report is `mode: split`, `outputs: 4`, `filters: [arm64-v8a, armeabi-v7a, x86_64, unfiltered]`, with one matching TagLib entry per ABI and all three in universal. Report canonical identity and exactly one truthful signing status: `signed: verified` or `signed: unsigned local release`.

- [ ] **Step 6: Commit APK verification**

```bash
git add build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidReleaseVerification.kt \
  build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/VerifyReleaseApksTask.kt \
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidReleaseVerificationTest.kt androidApp/build.gradle.kts
```

```bash
git commit -m "build(android): verify release APK artifacts" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

---

### Task 4: Prove and Implement Independent AAB Verification

**Files:**
- Create: `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/VerifyReleaseAabTask.kt`
- Create: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AabMetadataProbeTest.kt`
- Modify: `androidApp/build.gradle.kts`

**Consumes:** `SingleArtifact.BUNDLE`, canonical metadata, and configured SDK `aapt2` plus `apkanalyzer`.

**Produces:** `:androidApp:verifyReleaseAab`, independent from split APK mode.

- [ ] **Step 1: Write the real SDK-only feasibility probe before production AAB code**

`AabMetadataProbeTest` must require an explicit existing, non-empty AGP release AAB test input and must fail if it is absent. It must read `base/manifest/AndroidManifest.xml`, `base/resources.pb`, and every `base/res/**` entry with `ZipFile`; write root `AndroidManifest.xml`, root `resources.pb`, and each resource payload at its path relative to `base/` into a temporary proto archive; invoke `aapt2 convert --output-format binary`; then invoke `apkanalyzer manifest application-id`, `version-name`, and `version-code` on the converted temporary APK.

The test must assert `com.eterocell.rhythhaus`, canonical version name, and numeric version code. It must not use output filenames, source configuration, APK metadata, or AGP task inputs as assertions.

- [ ] **Step 2: Build a real AAB and witness the required RED/GREEN conversion probe**

```bash
./gradlew :androidApp:clean :androidApp:bundleRelease --configuration-cache --configuration-cache-problems=fail
./gradlew -p build-logic :convention:test \
  --tests 'com.eterocell.gradle.android.AabMetadataProbeTest' \
  -Prhythhaus.aabProbeFile="$PWD/androidApp/build/outputs/bundle/release/androidApp-release.aab"
```

Expected RED: unresolved probe/verifier code. The earlier real AGP 9.3 feasibility run already proved that a two-entry archive fails because `resources.pb` references omitted packaged resources, while the revised archive with all `base/res/**` payloads converts and exposes all three canonical SDK metadata values. Implement only the test helper needed for the revised conversion, then rerun the same command and require GREEN with all three SDK metadata values.

**Stop condition:** If the revised SDK-only archive to `aapt2 convert` to `apkanalyzer` path cannot be made to pass against the real AGP 9.3 AAB, stop Task 4 and the implementation. Record the failing command and sanitized output, then request another user-approved design revision. Never continue by substituting filenames, source config, APK metadata, or AGP task inputs.

- [ ] **Step 3: Add failing task-contract tests after feasibility is proven**

Cover a missing or empty AAB, missing `base/manifest/AndroidManifest.xml`, missing `base/resources.pb`, malformed or unsafe `base/res/**` paths, `aapt2` conversion failure with sanitized stderr, and `apkanalyzer` identity mismatch with expected and actual values.

- [ ] **Step 4: Implement the proven task path only**

`VerifyReleaseAabTask` uses `temporaryDir`, never release-output directories. It copies the base manifest and resource table to their required root names and copies every packaged `base/res/**` payload to its path relative to `base/`, rejecting entries that cannot remain inside the temporary archive. It runs the exact `aapt2 convert` syntax proved by Step 2, then the same three `apkanalyzer manifest` commands as Task 3. It requires one non-empty AAB and writes a deterministic report. It must not run `apksigner`.

- [ ] **Step 5: Register independent AAB verification**

Register `verifyReleaseAab` from `variant.artifacts.get(SingleArtifact.BUNDLE)`, pass canonical identity and SDK providers, and report to `build/reports/androidReleaseVerification/release-aab.txt`. Do not depend on `verifyReleaseApks`, `assembleRelease`, or split mode. The bundle artifact provider supplies the `bundleRelease` dependency.

- [ ] **Step 6: Witness task GREEN and AAB independence**

```bash
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AabMetadataProbeTest'
./gradlew :androidApp:clean :androidApp:verifyReleaseAab --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:clean :androidApp:verifyReleaseAab -Prhythhaus.android.splitApk=true --configuration-cache --configuration-cache-problems=fail
```

Expected: all pass. Each report identifies one non-empty AAB with canonical package/version metadata. The second release verification produces the same AAB contract with split mode present, proving it is independent.

- [ ] **Step 7: Commit AAB verification**

```bash
git add build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/VerifyReleaseAabTask.kt \
  build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AabMetadataProbeTest.kt androidApp/build.gradle.kts
```

```bash
git commit -m "build(android): verify release app bundle" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
```

---

### Task 5: Prove Default Workflow and Configuration-Cache Stability

**Files:**
- Modify: only the minimum file exposed by a new failing regression test.

**Consumes:** Tasks 1 through 4.

**Produces:** Exact acceptance evidence without runtime changes.

- [ ] **Step 1: Verify build logic and property failures**

```bash
./gradlew -p build-logic :convention:test :convention:validatePlugins --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:tasks -Prhythhaus.android.abis=arm64-v8a,armeabi-v7a --stacktrace
./gradlew :androidApp:tasks -Prhythhaus.versionCode=not-an-integer --stacktrace
```

Expected: build logic succeeds. ABI failure includes the strict expected-versus-actual set. Version failure is `Gradle property 'rhythhaus.versionCode' must be an integer, was 'not-an-integer'`.

- [ ] **Step 2: Prove configuration-cache reuse for both release channels**

Run each command twice without source or argument changes:

```bash
./gradlew :androidApp:verifyReleaseApks -Prhythhaus.android.splitApk=true --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:verifyReleaseAab --configuration-cache --configuration-cache-problems=fail
```

Expected: each first run stores a configuration-cache entry and each second run contains `Reusing configuration cache.` plus `BUILD SUCCESSFUL`.

- [ ] **Step 3: Prove default and non-exact workflows**

```bash
./gradlew :androidApp:clean :androidApp:verifyReleaseApks -Prhythhaus.android.splitApk=True --configuration-cache --configuration-cache-problems=fail
./gradlew :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail
./init.sh
```

Expected: non-exact `True` reports one ordinary unfiltered release APK; debug produces one ordinary debug APK; the supported JVM/desktop/Android matrix succeeds. For `./init.sh`, record exact output. If the known unrelated iOS `AppScanCancellationTest.kt` JVM-only `Thread` failure persists, record it and do not claim iOS success or fix it in this change.

- [ ] **Step 4: Commit only a tested regression fix**

If and only if a new failing test exposed a defect, stage only its focused fix and test, then commit `fix(build): preserve release verification cacheability` with the required Sisyphus footer and co-author trailer. Do not create an empty commit.

---

### Task 6: Complete Review and Durable Evidence

**Files:**
- Modify: `openspec/changes/android-split-apk-releases/tasks.md`, `roadmap.md`, `progress.md`
- Optionally Create: `.superpowers/sdd/android-split-apk-releases-task-*.md`

**Consumes:** Passing task evidence, commits, reports, and reviews.

**Produces:** Completed task ledger and final handoff without archival.

- [ ] **Step 1: Inspect scope and run strict OpenSpec validation**

```bash
git diff --check
openspec validate android-split-apk-releases --strict
```

Expected: `git diff --check` has no output and OpenSpec reports `Change 'android-split-apk-releases' is valid`. Review for no custom output names, ABI version codes, checksum/upload/publish code, credential reads, runtime/native changes, filename classification, AAB `apksigner`, task-action project access, or configuration-cache-incompatible closures.

- [ ] **Step 2: Run task-level and final reviews**

For each implementation task, complete specification and build-logic/configuration-cache reviews using its actual test/report evidence. Run the required `review-work` workflow for the complete change. It must cover constraints, Gradle quality, secret safety, actual split APK/AAB QA, and repository context. A Critical or Important finding blocks completion and requires a new RED test before its focused fix.

- [ ] **Step 3: Record only earned durable evidence**

Mark a ledger checkbox complete only after its command and review evidence exist. Update only roadmap item 22 with opt-in matrix, independent AAB, canonical metadata, honest signing status, cache result, and manual device-install limitation. Preserve roadmap item 21 byte-for-byte. Prepend `progress.md` with exact commands/results, scope, signing status, cache evidence, blockers, changed files, commits, and next owner. Do not edit approved design/proposal/specs unless a genuine contradiction receives user approval.

- [ ] **Step 4: Commit final evidence and inspect history**

```bash
git add openspec/changes/android-split-apk-releases/tasks.md roadmap.md progress.md
git status --short
```

```bash
git commit -m "docs: record Android split APK release evidence" \
  -m "Ultraworked with [Sisyphus](https://github.com/code-yeongyu/oh-my-openagent)" \
  -m "Co-authored-by: Sisyphus <clio-agent@sisyphuslabs.ai>"
git status --short
git log --oneline -6
```

If intended change-specific `.superpowers/sdd` reports are included, use `git add -f` only for their exact paths. Do not push or archive without an explicit request.

---

## Dependency Graph

```text
Task 1 ABI contract and TestKit
  -> Task 2 AGP split configuration
  -> Task 3 APK verifier
  -> Task 4 AAB feasibility probe, then independent verifier
  -> Task 5 cache/default acceptance
  -> Task 6 review and durable evidence
```

Tasks 3 and 4 both modify `androidApp/build.gradle.kts` and must not be implemented concurrently without one integration owner. Task 4's feasibility probe is a strict gate, not an optional test.

## Self-Review

- **Spec coverage:** Tasks 1 and 2 cover strict ABI parsing and exact opt-in split generation. Task 3 covers metadata-driven APK classification, ZIP-native checks, canonical APK metadata, and signing. Task 4 covers independent AAB production and SDK metadata inspection. Task 5 covers ordinary workflows, cache reuse, and repository regression. Task 6 covers validation, review, roadmap item 22 only, and handoff evidence.
- **Placeholder scan:** This plan contains no deferred implementation markers. Conditional Task 5 edits are prohibited unless a witnessed failing regression test requires them.
- **Type consistency:** Every build script and test uses `RhythHausAndroidAbiContractExtension`, `ReleaseArtifactIdentity`, `ReleaseApkDescriptor`, `VerifyReleaseApksTask`, and `VerifyReleaseAabTask` exactly as declared above.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-16-android-split-apk-releases.md`. Execute with Subagent-Driven Development, one fresh worker per task and review between tasks, or use Inline Execution with checkpoints. Task 4 must halt at its feasibility gate if the SDK-only AAB conversion cannot be proven.
