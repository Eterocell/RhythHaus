# Session Progress

## Current state

Last updated: 2026-06-10
Current change: project harness creation and initial shared UI slice; harness route updated to openspec+superpowers
Workflow route: openspec+superpowers
State source of truth: OpenSpec for durable product changes; Superpowers for clarification/brainstorming/task execution discipline; this file for session continuity and verification evidence.

## Completed

- Initialized a first shared Compose Multiplatform product surface for RhythHaus.
- Added shared demo music models and formatting tests.
- Scoped desktop native packaging to macOS DMG only for current target scope.
- Confirmed OpenSpec is initialized via `openspec/` and `openspec/config.yaml`.
- Created project agent harness files:
  - `AGENTS.md`
  - `docs/harness-engineering.md`
  - `init.sh`
  - `progress.md`

## In progress

- OpenSpec change `play-music-all-platforms` has first implementation slice completed and validated.
  - Proposal: `openspec/changes/play-music-all-platforms/proposal.md`
  - Design: `openspec/changes/play-music-all-platforms/design.md`
  - Spec: `openspec/changes/play-music-all-platforms/specs/audio-playback/spec.md`
  - Tasks: `openspec/changes/play-music-all-platforms/tasks.md`
  - Implementation: shared playback model/controller/UI plus Android Media3, iOS AVFAudio, and macOS AVFoundation Objective-C++/JNI engine.
  - Validation: `./init.sh` -> BUILD SUCCESSFUL on 2026-06-10.
  - Follow-up backend implementation: Android playback migrated from platform `MediaPlayer` to Media3/ExoPlayer; macOS/JVM playback migrated from Java Sound `Clip` to native AVFoundation through a temporary JNA bridge, then replaced with a small Objective-C++ helper called through JNI; iOS remains on native AVFAudio `AVAudioPlayer`. MacOS/JVM playback now starts a daemon scheduled progress publisher while playing so the shared Compose progress slider advances continuously instead of only updating on play/pause/seek events.
  - Follow-up validation: added JVM regression test `nativeMacPlaybackEnginePublishesProgressWhilePlaying`; first run failed at `JvmPlaybackEngineTest.kt:105` because no periodic progress events were emitted; after the fix, targeted test passed. `openspec validate play-music-all-platforms` -> valid; `openspec validate import-local-audio` -> valid; `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; `/usr/bin/xcrun xcodebuild -version` -> Xcode 26.5 Build 17F42; `./gradlew :shared:iosSimulatorArm64Test :desktopApp:packageDmg --configuration-cache` -> BUILD SUCCESSFUL and produced `desktopApp/build/compose/binaries/main/dmg/RhythHaus-1.0.0.dmg`; `jar tf shared/build/libs/shared-jvm.jar | grep -E 'native/.*/librhythhaus_audio.dylib'` -> `native/macos-aarch64/librhythhaus_audio.dylib`.
- OpenSpec change `import-local-audio` has first manual import slice completed and validated.
  - Proposal: `openspec/changes/import-local-audio/proposal.md`
  - Design: `openspec/changes/import-local-audio/design.md`
  - Spec: `openspec/changes/import-local-audio/specs/local-audio-import/spec.md`
  - Tasks: `openspec/changes/import-local-audio/tasks.md`
  - Implementation: shared import model/UI, Android document picker, macOS/JVM native Finder-style file dialog, iOS unsupported-state placeholder.
  - Validation: `./init.sh` -> BUILD SUCCESSFUL on 2026-06-10; `openspec validate import-local-audio` -> valid.
  - Follow-up update: removed sample/demo library playback path and `AudioSource.DemoTone`; empty library now prompts for local import only. Replaced macOS/JVM Swing `JFileChooser` with native AWT `FileDialog` so macOS opens the system Finder-style panel.
  - Follow-up validation: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; `/usr/bin/xcrun xcodebuild -version` -> Xcode 26.5 Build 17F42; `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> BUILD SUCCESSFUL; `openspec validate import-local-audio` -> valid.

## Next steps

1. Manually validate foreground play/pause/seek on Android device/emulator and macOS using real local audio files imported through the UI.
2. Verify packaged macOS DMG runtime behavior for the native AVFoundation Objective-C++/JNI helper.
3. Keep iOS playback on native Apple audio APIs; decide whether the existing Kotlin/Native `AVAudioPlayer`, AVFoundation `AVPlayer`, or a Swift bridge best fits the iOS import/media-library path.
4. Plan the iOS document-picker bridge so iOS can import local files instead of showing the current unsupported-state message.

## Decisions

- First platform scope: Android, iOS, macOS/desktop JVM.
- Windows/Linux support is future scope only.
- Use shared-first Compose Multiplatform UI.
- OpenSpec owns durable requirements/specs/tasks because `openspec/` exists.
- Playback backend direction: Android uses Media3/ExoPlayer, iOS uses native Apple audio APIs, and macOS uses a native AVFoundation Objective-C++/JNI helper rather than Java Sound or JNA for product-grade playback.
- Superpowers owns clarification, brainstorming, task execution discipline, and TDD-style implementation loops for durable work.
- Do not create `feature_list.json` for OpenSpec-owned tasks.
- Completed OpenSpec + Superpowers workflow changes should be committed by default unless the user explicitly says not to commit.
- Commit messages should use semantic/conventional style such as `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, or `chore: ...`.
- Harness owns verification, acceptance, scope, lifecycle, and handoff evidence.

## Verification evidence

Latest successful harness verification:

```bash
./init.sh
```

Result: BUILD SUCCESSFUL for both Gradle phases. Details from 2026-06-10 playback implementation:

- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.5, Build version 17F42.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: BUILD SUCCESSFUL.

Harness verification command to use going forward:

```bash
./init.sh
```

## Changed files in current playback work

- `gradle/libs.versions.toml` - added shared coroutine dependency alias.
- `shared/build.gradle.kts` - added `kotlinx-coroutines-core` to common code, Android Activity Compose and Media3 to Android shared source set, and a macOS native audio helper build/resource task for JVM.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt` - shared playback domain, controller, engine contract, fake engine, and formatting helper.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioImport.kt` - shared imported-audio model, import launcher contract, and imported-library mapping.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt` - added `AudioSource` to `Track` so imported rows are playable.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` - shared now-playing playback controls, import card, seek display, status/error display, and accessibility content descriptions.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt` - Android Media3/ExoPlayer engine with context-backed URI playback.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioImport.android.kt` - Android `OpenMultipleDocuments` audio picker.
- `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt` - provides Android application context for content URI playback.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` - iOS `AVAudioPlayer` engine and foreground audio session setup.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt` - iOS unsupported import placeholder with user-facing copy.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt` - macOS native AVFoundation-backed playback engine through an Objective-C++/JNI helper.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioImport.jvm.kt` - macOS/JVM native AWT file dialog importer.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/SharedCommonTest.kt` - playback and import mapping tests.
- `openspec/changes/play-music-all-platforms/design.md` - recorded first-slice engine and format decisions.
- `openspec/changes/play-music-all-platforms/tasks.md` - marked implemented/verified tasks and remaining manual validation.
- `openspec/changes/import-local-audio/*` - planned, specified, and task-tracked manual local audio import.
- `progress.md` - updated handoff/evidence.

## Completion evidence checklist

- [x] Workflow route recorded: `openspec+superpowers`.
- [x] Current owner recorded: harness-creator for harness files; OpenSpec for future durable product tasks.
- [x] Fact source conflict avoided: no `feature_list.json` created because OpenSpec is initialized.
- [x] Verification commands documented in `AGENTS.md`, `docs/harness-engineering.md`, and `init.sh`.
- [x] Known platform scope recorded.
- [x] Next safe action recorded.

## Handoff

Route: openspec+superpowers
Owner: harness-creator
Input: user request to store commit-after-flow and semantic commit-message preferences in project instructions instead of Hermes memory
Output: `AGENTS.md`, `docs/harness-engineering.md`, and `progress.md` updated with default commit behavior for completed OpenSpec + Superpowers workflows and semantic/conventional commit message requirement
Next owner: harness-creator for validation and semantic commit of this project-instruction update
Blockers: none currently; future iOS local music access strategy needs product decision
