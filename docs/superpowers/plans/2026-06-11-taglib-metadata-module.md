# Native TagLib Kotlin Multiplatform Wrapper Implementation Plan

> Required workflow: use `subagent-driven-development` or `executing-plans` task-by-task. This plan replaces the earlier pure-Kotlin parser plan. Do not implement ID3/FLAC/MP4 parsing in Kotlin; Kotlin code should wrap the native TagLib backend through platform-specific bindings.

Goal: Add a separate Kotlin Multiplatform `:taglib` module that exposes a Kotlin-friendly metadata API backed by the actual TagLib library, then use that module from RhythHaus import flow.

Architecture summary:

- `:taglib` is a separate Gradle module.
- `commonMain` owns only API models, `expect` declarations, and small result mapping helpers.
- Native parsing is performed by TagLib through a small RhythHaus C ABI shim.
- Kotlin does not parse ID3/FLAC/MP4 tags directly.
- The C ABI shim wraps TagLib C++ and returns a stable, simple representation to Kotlin/JVM, Kotlin/Android, and Kotlin/Native iOS.

Why a C ABI shim:

- TagLib is primarily C++.
- Kotlin/Native and JNI can both bind to C APIs much more safely than C++ APIs.
- A stable RhythHaus-owned C API hides TagLib version/layout details.
- The same shim contract can be reused by Android JNI, desktop JVM JNI/JNA, and iOS cinterop.

Reference repo note:

`https://github.com/moya-a/k-taglib` is useful as historical API inspiration only. It is not a native TagLib wrapper and must not be copied as parser implementation.

Current preserved commits:

- Keep `c3c8d5c build: add taglib metadata module`.
- Keep `d774fbc feat: define taglib metadata api` if the API remains suitable.
- Reverted pure-Kotlin parser commits:
  - `294fa36 feat: detect supported tag formats`
  - `32f2536 feat: parse id3v1 metadata`
  - `462dc1c feat: parse id3v2 text metadata`

Current `:taglib` public API may be refactored from byte-input to path/source-input because native TagLib usually reads files/streams through `FileRef` or file names.

---

## Target API

Common API should be source-oriented, not byte-parser-oriented.

Proposed common API:

```kotlin
package com.eterocell.rhythhaus.taglib

data class TagMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val trackTotal: Int? = null,
    val discNumber: Int? = null,
    val discTotal: Int? = null,
    val durationMillis: Long? = null,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val artwork: EmbeddedArtwork? = null,
)

sealed interface TagReadResult {
    data class Found(val metadata: TagMetadata) : TagReadResult
    data class Unsupported(val reason: String) : TagReadResult
    data class Failed(val reason: String) : TagReadResult
}

interface TagLibReader {
    fun readPath(path: String): TagReadResult
}

expect fun createTagLibReader(): TagLibReader
```

Optional Android content-URI integration should happen outside the wrapper by copying an imported URI to an app-cache file and passing that path to `TagLibReader`. That keeps native TagLib path-based and avoids platform FD/stream complexity in the first slice.

---

## Native C ABI shim

Create a small C/C++ boundary owned by RhythHaus, not by Kotlin code.

Files:

```text
taglib/native/include/rh_taglib.h
taglib/native/src/rh_taglib.cpp
```

Proposed ABI:

```c
#pragma once

#ifdef __cplusplus
extern "C" {
#endif

typedef struct RhTagLibMetadata {
    char* title;
    char* artist;
    char* album;
    char* album_artist;
    char* genre;
    int year;
    int track;
    int duration_seconds;
    int bitrate;
    int sample_rate;
    int channels;
} RhTagLibMetadata;

typedef struct RhTagLibResult {
    int status; // 0 found, 1 unsupported, 2 failed
    char* error_message;
    RhTagLibMetadata metadata;
} RhTagLibResult;

RhTagLibResult rh_taglib_read_path(const char* path);
void rh_taglib_free_result(RhTagLibResult result);

#ifdef __cplusplus
}
#endif
```

Implementation uses TagLib C++:

```cpp
#include <taglib/fileref.h>
#include <taglib/tag.h>
#include <taglib/audioproperties.h>
```

Mapping rules:

- `TagLib::FileRef file(path)`
- if `file.isNull()` => Unsupported or Failed with message
- if `file.tag()` => title, artist, album, genre, year, track
- if `file.audioProperties()` => duration, bitrate, sampleRate, channels
- Allocate returned strings with `malloc`/`strdup`; free in `rh_taglib_free_result`.

Artwork can be a follow-up because TagLib artwork extraction differs by format-specific classes.

---

## Platform strategy

Current implementation note: JVM/macOS now fetches, pins, builds, and links upstream `https://github.com/taglib/taglib` v2.3 at commit `1b94b93762636ebe5733180c3e825be4621e4c7f`, then verifies the JNI path with a native fixture test. Android and iOS are still scaffolds; their next build work must reuse that same pinned upstream source rather than adding a TagLib replacement, custom metadata parser, or unrelated native library.

### Android

Use Android NDK + CMake in `:taglib` to build pinned upstream `github.com/taglib/taglib` v2.3 from source for each supported ABI.

Recommended first implementation:

- Reuse the Gradle-pinned upstream TagLib version/commit (`v2.3`, `1b94b93762636ebe5733180c3e825be4621e4c7f`) and add an Android NDK toolchain CMake configure/build/install step per ABI.
- Expected build/install layout: `taglib/build/third-party/taglib-android-<abi>-build-v2.3` and `taglib/build/third-party/taglib-android-<abi>-install-v2.3`, with upstream headers and `lib/libtag.a` produced from the upstream source.
- Link each ABI's upstream `libtag.a` with `native/src/rh_taglib.cpp` and `native/jni/rh_taglib_jni.cpp` into the packaged `librhythhaus_taglib.so` slice for that ABI.
- Keep Android metadata reads unsupported until those per-ABI builds are wired, packaged, loaded, and tested on device/emulator.
- Kotlin Android actual calls JNI functions in `androidMain`.

Kotlin surface:

```kotlin
actual fun createTagLibReader(): TagLibReader = AndroidNativeTagLibReader()
```

JNI files:

```text
taglib/src/androidMain/kotlin/.../NativeTagLib.android.kt
taglib/native/jni/rh_taglib_jni.cpp
```

### macOS / desktop JVM

Use JNI or JNA. Prefer JNI for consistency with Android and existing project native audio JNI precedent. Current JVM/macOS wiring uses JNI, builds pinned upstream TagLib v2.3 from `github.com/taglib/taglib`, links the RhythHaus helper against the resulting static `libtag.a`, packages the helper in JVM resources, and has a native fixture test. Remaining desktop follow-up is runtime/DMG packaging and codesigning review, not parser implementation.

Files:

```text
taglib/src/jvmMain/kotlin/.../NativeTagLib.jvm.kt
taglib/native/jni/rh_taglib_jni.cpp
```

Gradle compiles a macOS native helper similarly to the existing desktop audio native helper pattern and does not require a Homebrew TagLib dependency for the tested JVM path.

### iOS

Use Kotlin/Native cinterop against the C ABI shim and an XCFramework/static libraries built from pinned upstream `github.com/taglib/taglib` v2.3.

First iOS implementation may be staged:

- Add `iosMain` actual that returns `Unsupported("Native TagLib iOS binding is not packaged yet")` until the pinned upstream TagLib v2.3 iOS static library/XCFramework build is defined.
- Do not claim iOS metadata support until cinterop and packaging pass.

Final iOS direction:

```text
taglib/src/nativeInterop/cinterop/rh_taglib.def
taglib/native/include/rh_taglib.h
taglib/build/third-party/taglib-ios-device-build-v2.3 -> static libtag.a from upstream v2.3
taglib/build/third-party/taglib-ios-simulator-build-v2.3 -> static libtag.a from upstream v2.3
taglib/third_party/taglib-ios/TagLib.xcframework with device/simulator slices and upstream headers
```

---

## Task 1: Keep module/API, remove byte-parser API naming

Files:

- `taglib/src/commonMain/kotlin/com/eterocell/rhythhaus/taglib/RhythHausTagLib.kt`
- `taglib/src/commonMain/kotlin/com/eterocell/rhythhaus/taglib/TagMetadata.kt`
- `taglib/src/commonMain/kotlin/com/eterocell/rhythhaus/taglib/TagReadResult.kt`
- `taglib/src/commonTest/kotlin/com/eterocell/rhythhaus/taglib/RhythHausTagLibTest.kt`

Steps:

- [ ] Replace byte-oriented `RhythHausTagLib.read(bytes: ByteArray)` with source/path-oriented API.
- [ ] Add `TagLibReader` and `expect fun createTagLibReader()`.
- [ ] Keep `TagMetadata` but add audio-property fields if desired.
- [ ] Update common tests to assert unsupported path behavior through a fake reader or public model tests only. Do not require a native library in common tests.
- [ ] Run `./gradlew :taglib:allTests --configuration-cache`.
- [ ] Commit: `feat: define native taglib wrapper api`.

Acceptance:

- No Kotlin parser/detector classes exist.
- Common API does not imply Kotlin parsing from bytes.

---

## Task 2: Add native C ABI shim skeleton

Files:

```text
taglib/native/include/rh_taglib.h
taglib/native/src/rh_taglib.cpp
taglib/native/CMakeLists.txt
```

Steps:

- [ ] Add the C header and C++ shim implementation.
- [ ] For the first skeleton, allow implementation to return `unsupported` if TagLib is not linked yet, but keep the ABI exact.
- [ ] Add CMake target `rhythhaus_taglib`.
- [ ] If local TagLib is available, wire `find_package(TagLib)` or pkg-config. If unavailable, document the blocker in comments/docs instead of faking success.
- [ ] Verify at least CMake configuration or Gradle native helper build on macOS if wired.
- [ ] Commit: `feat: add native taglib c shim`.

Acceptance:

- The native boundary is C ABI, not C++ exposed to Kotlin.
- Memory ownership is explicit through `rh_taglib_free_result`.

---

## Task 3: Add JVM/macOS native binding

Files:

```text
taglib/src/jvmMain/kotlin/com/eterocell/rhythhaus/taglib/TagLibReader.jvm.kt
taglib/native/jni/rh_taglib_jni.cpp
taglib/build.gradle.kts
```

Steps:

- [ ] Add JNI wrapper around `rh_taglib_read_path`.
- [ ] Add JVM actual `createTagLibReader()`.
- [ ] Decide library loading path using existing project native helper conventions where possible.
- [ ] Add JVM tests that run only when native library is available, or unit-test mapping with a fake native adapter.
- [ ] Run `./gradlew :taglib:jvmTest --configuration-cache`.
- [ ] Commit: `feat: add jvm taglib native reader`.

Acceptance:

- JVM binding calls native TagLib shim; it does not parse tags in Kotlin.
- Tests/builds do not fail on machines without TagLib unless this task explicitly installs/builds it.

---

## Task 4: Add Android native binding plan/scaffold

Files:

```text
taglib/src/androidMain/kotlin/com/eterocell/rhythhaus/taglib/TagLibReader.android.kt
taglib/native/jni/rh_taglib_jni.cpp
taglib/build.gradle.kts
```

Steps:

- [ ] Add Android actual reader using JNI.
- [ ] Configure Android external native build to fetch/reuse pinned upstream `github.com/taglib/taglib` v2.3 at commit `1b94b93762636ebe5733180c3e825be4621e4c7f` and build/install it per ABI with the Android NDK CMake toolchain.
- [ ] Link each ABI's upstream `libtag.a` with the RhythHaus shim/JNI sources into packaged `librhythhaus_taglib.so` outputs.
- [ ] Until those upstream-source builds are packaged and verified, keep Android actual returning Unsupported with a clear message and document the remaining packaging task.
- [ ] Run `./gradlew :taglib:assembleDebug --configuration-cache` or the correct Android multiplatform assemble/check task available in this project.
- [ ] Commit: `feat: scaffold android taglib reader`.

Acceptance:

- Android API shape is ready for pinned upstream TagLib v2.3, but Android support is not claimed until NDK builds/package/tests pass.
- No Kotlin metadata parser is introduced.

---

## Task 5: Add iOS cinterop plan/scaffold

Files:

```text
taglib/src/iosMain/kotlin/com/eterocell/rhythhaus/taglib/TagLibReader.ios.kt
taglib/src/nativeInterop/cinterop/rh_taglib.def
taglib/build.gradle.kts
```

Steps:

- [ ] Add iOS actual reader.
- [ ] Build pinned upstream `github.com/taglib/taglib` v2.3 at commit `1b94b93762636ebe5733180c3e825be4621e4c7f` as iOS device and simulator static libraries, then assemble a `TagLib.xcframework` containing upstream headers.
- [ ] If the pinned upstream TagLib iOS XCFramework/static libraries are not packaged yet, return Unsupported with clear message.
- [ ] Add cinterop def only when headers/libs are available; otherwise document exact expected layout.
- [ ] Run `./gradlew :taglib:iosSimulatorArm64Test --configuration-cache`.
- [ ] Commit: `feat: scaffold ios taglib reader`.

Acceptance:

- iOS remains honest; no metadata support is claimed until pinned upstream TagLib v2.3 is linked through cinterop and verified on device/simulator.

---

## Task 6: Integrate `:taglib` into shared import flow

Files:

```text
shared/build.gradle.kts
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadataReader.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioImport.kt
shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt
shared/src/commonTest/kotlin/com/eterocell/rhythhaus/SharedCommonTest.kt
```

Steps:

- [ ] Add `implementation(projects.taglib)` to `shared` common dependencies.
- [ ] Add shared app `AudioMetadata` mapping from `taglib.TagMetadata`.
- [ ] Add `AudioMetadataReader` seam that uses `TagLibReader` on supported file paths.
- [ ] For Android content URIs, first implementation may copy the URI stream to an app cache temp file before calling TagLib by path, or defer with a clear Unsupported result.
- [ ] Enrich imported tracks with metadata when available; preserve filename fallback when unsupported/failed.
- [ ] Run focused shared tests and compile.
- [ ] Commit: `feat: use taglib metadata facade for imports`.

Acceptance:

- Shared UI code depends on normalized metadata only.
- Unsupported platforms/file sources still import and play with filename fallback.

---

## Task 7: OpenSpec and progress updates

Status: completed in docs/state. OpenSpec now records the native-wrapper architecture and current platform state; `progress.md` records verification evidence and the next safe action. JVM/macOS now builds and tests pinned upstream TagLib v2.3. The next engineering task is Android/iOS source builds and packaging from the same pinned upstream `github.com/taglib/taglib` source, not adding Kotlin parsers.

Files:

```text
openspec/changes/import-local-audio/design.md
openspec/changes/import-local-audio/tasks.md
progress.md
docs/superpowers/plans/2026-06-11-taglib-metadata-module.md
```

Steps:

- [x] Record that metadata extraction is planned through a separate native TagLib wrapper module.
- [x] Record that pure Kotlin tag parsing was explicitly rejected.
- [x] Add packaging/linking follow-ups per platform.
- [x] Validate OpenSpec:
  - `openspec validate import-local-audio --strict`
- [x] Commit docs/state: `docs: record native taglib import metadata plan`.

---

## Task 8: Full verification

Run:

```bash
./gradlew :taglib:allTests :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
/usr/bin/xcrun xcodebuild -version
./gradlew :taglib:iosSimulatorArm64Test :shared:iosSimulatorArm64Test --configuration-cache
openspec validate import-local-audio
openspec validate play-music-all-platforms
```

Record exact results in `progress.md`.

---

## Risks and open decisions

- TagLib mobile source builds: Android and iOS must build from pinned upstream `github.com/taglib/taglib` v2.3 at commit `1b94b93762636ebe5733180c3e825be4621e4c7f`, matching the JVM/macOS source, instead of depending on ad hoc prebuilts or custom parsers.
- Android ABIs and binary size.
- iOS static library/xcframework build process.
- macOS DMG bundling and dylib codesigning/notarization implications.
- Android content URI handling: TagLib wants a path; app may need to copy imported URI content to app-local cache/storage.
- Artwork extraction should be follow-up after basic metadata and audio properties work.

---

## Current next safe action

Link and package pinned upstream TagLib v2.3 builds per platform before claiming complete rich metadata support:

1. macOS/JVM: keep building pinned upstream TagLib v2.3 into static `libtag.a`, link `librhythhaus_taglib.dylib`, and complete desktop runtime/DMG packaging review.
2. Android: build the same pinned upstream TagLib v2.3 source with Android NDK/CMake per ABI, then package `librhythhaus_taglib.so` slices that link the upstream `libtag.a` outputs.
3. iOS: build the same pinned upstream TagLib v2.3 source into device/simulator static libraries, assemble `TagLib.xcframework`, then commit Kotlin/Native cinterop wiring and replace the honest unsupported scaffold only after device/simulator linking is verified.

Do not add Kotlin ID3/FLAC/MP4 parsers.
