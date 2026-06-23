## 1. Shared import model

- [x] 1.1 Add shared imported-audio models and import result/error state.
- [x] 1.2 Add shared mapper from imported audio files to `Track` / `PlayableTrack` values.
- [x] 1.3 Add common tests for title derivation, stable ids, source mapping, and merging demo/imported tracks.

## 2. Platform importers

- [x] 2.1 Add Android Activity Result document picker integration in `androidApp` and pass importer into shared `App`.
- [x] 2.2 Add macOS/JVM native AWT `FileDialog` importer for audio files so macOS uses the system Finder-style open panel.
- [x] 2.3 Add iOS unsupported importer state with clear UI copy and leave Swift/UIKit bridge as a follow-up.

## 3. Shared UI integration

- [x] 3.1 Add shared import button/status to the library surface.
- [x] 3.2 Replace metadata-only demo playback queue with imported playable tracks when imports exist.
- [x] 3.3 Keep demo tracks visible when no local files have been imported.
- [x] 3.4 Show user-visible import errors/limitations.

## 4. Verification and handoff

- [x] 4.1 Run `./init.sh`.
- [x] 4.2 Update `progress.md` with evidence, limitations, and next safe action.
- [x] 4.3 Validate this OpenSpec change.

## 5. Native TagLib metadata wrapper follow-up

- [x] 5.1 Record that import metadata is provided through the native `:taglib` wrapper seam, not hand-written Kotlin parsers.
- [x] 5.2 Record current platform state: macOS/JVM builds and tests pinned upstream `github.com/taglib/taglib` v2.3, Android builds/packages per-ABI upstream TagLib NDK helpers, and iOS has honest unsupported scaffold with pinned upstream TagLib v2.3 XCFramework/static-library layout documented.
- [x] 5.3 Validate this OpenSpec change after the documentation update.
- [x] 5.4 Build/link/package pinned upstream `github.com/taglib/taglib` v2.3 for Android with NDK/CMake per ABI and verify the resulting `librhythhaus_taglib.so` slices are packaged in the TagLib AAR and Android debug APK.
- [x] 5.5 Build/link/package pinned upstream `github.com/taglib/taglib` v2.3 for iOS device/simulator static libs assembled into an XCFramework before claiming iOS rich metadata support.
