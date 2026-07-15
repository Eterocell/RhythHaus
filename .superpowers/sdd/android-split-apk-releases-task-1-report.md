# Android Split APK Releases - Task 1 Report

Status: PASS WITH DOCUMENTED DIAGNOSTIC LIMITATION

## Scope and files

Base: `713c60a230f00a66f0d0637b63b2d9a4b797fe1b`

- `gradle.properties`: added the required ordered `rhythhaus.android.abis` value; did not add the split property.
- `build-logic/convention/build.gradle.kts`: registered binary plugin `build-logic.android.abi-contract` and added only `gradleTestKit()`, `libs.junit`, `libs.kotlin.test`, and `libs.kotlin.testJunit` test dependencies.
- `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContract.kt`: added the approved ABI constant, strict parser, exact split switch, and typed extension.
- `build-logic/convention/src/main/kotlin/com/eterocell/gradle/android/AndroidAbiContractPlugin.kt`: added provider-backed conventions finalized on read.
- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractTest.kt`: added parser and split-switch unit coverage.
- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/android/AndroidAbiContractPluginFunctionalTest.kt`: added TestKit consumer, invalid-property, split-switch, and configuration-cache coverage.
- `taglib/build.gradle.kts`: applied the binary plugin directly by ID and replaced only the editable ABI list with the typed extension value.
- `.superpowers/sdd/android-split-apk-releases-task-1-report.md`: this evidence report.

No Android app split configuration, APK/AAB verifier, OpenSpec, roadmap, progress, runtime/native source, signing configuration, or unrelated dependency was changed.

## Parser RED/GREEN

Initial harness attempt:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractTest' --stacktrace
BUILD FAILED in 3s
```

This first attempt exposed absent test dependencies as well as absent contract symbols. After adding only the plan-approved test dependencies, the strict parser RED was rerun before production implementation:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractTest' --stacktrace
> Task :convention:compileTestKotlin FAILED
Unresolved reference 'APPROVED_RHYTHHAUS_ANDROID_ABIS'.
Unresolved reference 'parseRhythHausAndroidAbis'.
Unresolved reference 'isRhythHausSplitApkEnabled'.
BUILD FAILED in 1s
```

The two generic `Cannot infer type for type parameter 'T'` messages were direct consequences of the unresolved assertion operands. The Kotlin test APIs themselves resolved on this strict RED run.

Parser GREEN after minimal production implementation:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractTest'
BUILD SUCCESSFUL in 8s
12 actionable tasks: 5 executed, 7 up-to-date
```

Covered behavior: approved order, trimmed accepted entries, missing/blank values, blank entries, duplicates, reordered values, unsupported ABI, reduced ABI set, and exact lowercase `true` split opt-in. Required error fragments are asserted verbatim.

## TestKit RED/GREEN

With the implementation class present but plugin metadata intentionally absent:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractPluginFunctionalTest'
6 tests completed, 6 failed
BUILD FAILED in 15s
```

The consumer fixtures applied `id("build-logic.android.abi-contract")`; successful-build cases failed with `UnexpectedBuildFailure`, while invalid-property cases could not observe parser diagnostics because the binary plugin was unavailable. This was the expected pre-metadata TestKit RED.

After registering the plugin ID and correcting two fixture-only issues (generated Kotlin quoting, then serializing resolved extension values rather than a `ListProperty` object), the same functional class passed:

```text
./gradlew -p build-logic :convention:test --tests 'com.eterocell.gradle.android.AndroidAbiContractPluginFunctionalTest'
BUILD SUCCESSFUL in 7s
12 actionable tasks: 2 executed, 10 up-to-date
Configuration cache entry reused.
```

The six cases cover approved, missing, duplicate, reordered, uppercase `TRUE`, and exact lowercase `true` properties. The exact-`true` case invokes one fixture directory twice with `withPluginClasspath()` and asserts the second output contains `Reusing configuration cache.`

## Repository and TagLib evidence

Invalid repository configuration:

```text
./gradlew :taglib:tasks -Prhythhaus.android.abis=arm64-v8a,x86_64 --stacktrace
Failed to calculate the value of extension 'rhythHausAndroidAbiContract' property 'abis'.
Gradle property 'rhythhaus.android.abis' must resolve to exactly [arm64-v8a, armeabi-v7a, x86_64] in this order; actual [arm64-v8a, x86_64].
BUILD FAILED in 7s
```

Valid repository configuration:

```text
./gradlew :taglib:tasks --all --configuration-cache
buildAndroidTagLibHelper-arm64-v8a
buildAndroidTagLibHelper-armeabi-v7a
buildAndroidTagLibHelper-x86_64
BUILD SUCCESSFUL in 960ms
Configuration cache entry stored.
```

Fresh repeat during final verification:

```text
./gradlew :taglib:tasks --all --configuration-cache
BUILD SUCCESSFUL in 344ms
Configuration cache entry reused.
```

The existing exhaustive ABI-to-NDK-triple `when`, three helper task registrations, aggregate task, and packaging task were retained.

## Final verification and diagnostics

```text
./gradlew -p build-logic :convention:test
BUILD SUCCESSFUL in 5s
12 actionable tasks: 1 executed, 11 up-to-date
```

```text
GIT_MASTER=1 git diff --check
```

Result: exit 0 with no output.

`lsp_diagnostics` was requested for all four changed Kotlin source/test files. Each returned: `LSP server 'kotlin-ls' (.kt, .kts) is NOT INSTALLED; user previously declined installation — proceed without LSP.` Gradle compilation and tests are therefore the executable Kotlin diagnostics.

Observed non-blocking existing warnings: Gradle 9.6 deprecation warnings in existing delegated task registration code and the existing TagLib Android host-test-not-enabled warning. No warning suppression was added.

## Self-review

- Exact approved order/property: `rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64`.
- Exact split switch: only the string `true` enables split mode; the root split property remains absent.
- Parser validation order: missing/blank property, trimmed split entries, blank entry rejection, duplicate rejection, then exact complete-list/order comparison.
- Binary plugin: direct ID application in TagLib; no version-catalog alias.
- Typed consumer: TagLib reads `RhythHausAndroidAbiContractExtension.abis.get()` once and preserves its native mapping/tasks.
- Configuration cache: TestKit asserts reuse, and the repository TagLib task run reused its cache on the final repeat.
- Scope: diff contains only Task 1 implementation/tests/configuration plus this required report.

## Commit

One atomic commit is required and will use `build: centralize Android ABI contract` with the mandated Sisyphus footer and co-author trailer. The commit hash cannot be embedded in the commit's own tracked report without changing that hash; the final task response records the resulting hash.

## Concerns

- Kotlin LSP diagnostics were unavailable by prior installation decision; Gradle compilation/tests passed instead.
- Existing Gradle deprecation and Android host-test warnings remain unchanged and out of scope.
