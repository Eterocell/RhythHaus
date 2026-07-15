# Android Split APK Releases - Task 2 Report

## Scope

- Base: `3a19a64f516016d5c46f048a4b60cbbd70fe2e8d`
- Route: approved SDD Task 2 execution
- Production change: `androidApp/build.gradle.kts`
- Preserved Task 1 interfaces in `build-logic.android.abi-contract` without modification.
- Preserved coordinator-owned `.superpowers/sdd/progress.md` and `openspec/changes/android-split-apk-releases/tasks.md` changes unstaged.

## RED - Exact Opt-In Before Production Edits

Command:

```bash
./gradlew :androidApp:clean :androidApp:assembleRelease \
  -Prhythhaus.android.splitApk=true \
  --configuration-cache --configuration-cache-problems=fail
```

Result: `BUILD SUCCESSFUL in 46s`; configuration cache entry stored.

Release `output-metadata.json` summary before editing `androidApp`:

- Element count: 1
- Element type: `SINGLE`
- Filters: empty
- Version code: 100

This is the required artifact-level RED: exact lowercase `true` still produced one unfiltered APK because the application had not consumed the Task 1 contract.

## Implementation

- Applied the binary plugin directly with `id("build-logic.android.abi-contract")`.
- Obtained `RhythHausAndroidAbiContractExtension` by type.
- Read the shared `splitApkEnabled` and ordered `abis` contract values.
- Used only AGP's built-in `splits { abi { ... } }` DSL.
- Disabled mode sets only `isEnable = false`; it does not call `reset`, `include`, or set `isUniversalApk`.
- Exact enabled mode resets the ABI set, includes the shared three-ABI contract, and enables the universal APK.
- Added no output/version callbacks, filenames, version offsets, flavors, signing, runtime behavior, density splits, or language splits.
- `AndroidAbiContractTest` was inspected and left unchanged because Task 1 already tests exact lowercase `true`, absent input, and uppercase `TRUE`; Task 2 proves AGP output shape through actual release metadata instead of duplicating parser tests.

## GREEN - Tasks and Contract Test

```bash
./gradlew :androidApp:tasks --all --configuration-cache
```

Result: `BUILD SUCCESSFUL in 12s`; configuration cache entry stored. AGP exposed its built-in `packageReleaseUniversalApk` task; no verifier task was added.

```bash
./gradlew :build-logic:convention:test \
  --tests 'com.eterocell.gradle.android.AndroidAbiContractTest' \
  --configuration-cache --configuration-cache-problems=fail
```

Result: `BUILD SUCCESSFUL in 7s`; configuration cache entry stored.

## GREEN - Exact Lowercase `true`

```bash
./gradlew :androidApp:clean :androidApp:assembleRelease \
  -Prhythhaus.android.splitApk=true \
  --configuration-cache --configuration-cache-problems=fail
```

Result: `BUILD SUCCESSFUL in 31s`; configuration cache entry stored.

Release metadata summary:

- Element count: 4
- One element: type `UNIVERSAL`, filters empty
- Three elements: type `ONE_OF_MANY`, each with one `ABI` filter
- ABI filter values: `arm64-v8a`, `armeabi-v7a`, `x86_64`
- Every element version code: 100

The summary is based on metadata elements and filters, not output filenames.

## GREEN - Property Absent

```bash
./gradlew :androidApp:clean :androidApp:assembleRelease \
  --configuration-cache --configuration-cache-problems=fail
```

Result: `BUILD SUCCESSFUL in 2s`; configuration cache entry stored.

Release metadata summary:

- Element count: 1
- Element type: `SINGLE`
- Filters: empty
- Version code: 100

## GREEN - Non-Exact Uppercase `TRUE`

```bash
./gradlew :androidApp:clean :androidApp:assembleRelease \
  -Prhythhaus.android.splitApk=TRUE \
  --configuration-cache --configuration-cache-problems=fail
```

Result: `BUILD SUCCESSFUL in 1s`; configuration cache entry stored.

Release metadata summary:

- Element count: 1
- Element type: `SINGLE`
- Filters: empty
- Version code: 100

The unchanged version code across exact and disabled modes proves no split version offsets were introduced.

## Configuration Cache Reuse

Repeated the uppercase `TRUE` command unchanged.

Result: `BUILD SUCCESSFUL in 1s`; Gradle reported `Reusing configuration cache` and `Configuration cache entry reused`. No configuration-cache problem was reported under `--configuration-cache-problems=fail`.

## Diagnostics

- `lsp_diagnostics` for `androidApp/build.gradle.kts`: unavailable because `kotlin-ls` is not installed and installation was previously declined.
- `lsp_diagnostics` for `AndroidAbiContractTest.kt`: same unavailable-language-server result; the file was not modified.
- Executable Kotlin/Gradle diagnostics passed through the focused contract test, Gradle Kotlin DSL compilation, task discovery, and all release assemblies above.
- The first scoped `:androidApp:spotlessKotlinGradleCheck` found only import ordering in `androidApp/build.gradle.kts`; the import order was corrected and the final scoped check passed.
- Final-source exact-mode verification passed with `BUILD SUCCESSFUL in 16s`; release metadata still contained one unfiltered `UNIVERSAL` element plus the three approved ABI-filtered elements, all at version code 100.
- Existing non-blocking warning: TagLib's `commonTest` directory exists without Android host tests enabled.
- Existing Gradle deprecation notice remains unrelated to this change.

## Commit

- Atomic commit subject: `build(android): add opt-in ABI splits`
- This report is included in that commit; the immutable hash is reported by the executor after commit creation rather than added through a forbidden amend.
- Push: not performed.

## Concerns

- No implementation blocker.
- Kotlin LSP diagnostics remain unavailable by prior user decision; Gradle compilation/tests provide the executable diagnostics.
- Artifact verification intentionally reads AGP metadata directly and does not add the later verifier tasks excluded from Task 2.
