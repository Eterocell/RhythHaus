# Task 3 report: Native CarPlay scene and templates

## Result

Task 3 is implemented. The native CarPlay scene consumes the process-owned Shared facade and the app-lifetime system-media publisher. It registers the CarPlay template scene, presents localized Songs, Albums, Artists, and Playlists destinations with detail navigation, and uses the shared Now Playing template. The scene observes authoritative Shared snapshots, rejects stale list selections, and removes scene-local observers and state on disconnect without stopping playback.

## Changed files

- `iosApp/iosApp/CarPlay/RhythHausCarPlaySceneDelegate.swift` — CarPlay scene connection/disconnection, Shared facade observer, category/detail navigation, selection dispatch, recoverable stale-selection alert, and scene-generation guards.
- `iosApp/iosApp/CarPlay/RhythHausCarPlayTemplateFactory.swift` — localized root and library list templates, loading/empty states, opaque entry IDs, template refresh, and exactly-once completion gates.
- `iosApp/iosApp/Info.plist` — CarPlay template application scene manifest.
- `iosApp/iosApp.xcodeproj/project.pbxproj` — adds the Chinese localization region and disables the generated scene-manifest setting in favor of the explicit manifest.
- `iosApp/iosApp/iOSApp.swift` — retains one process-owned Shared CarPlay facade for the system-media publisher and scene.
- `iosApp/iosApp/Localizable.xcstrings` — English and Simplified Chinese CarPlay labels, empty/loading states, and recoverable selection message.
- `iosApp/iosAppTests/RhythHausCarPlayTemplateTests.swift` — scene-manifest, root hierarchy, loading/empty/populated list, opaque ID/detail, stale selection, refresh, exactly-once completion, and disconnect/reconnect coverage.
- `openspec/changes/carplay-audio-integration/tasks.md` — marks Task 3.1–3.4 complete after focused XCTest/build and ABI evidence.

## Verification

- Focused iOS Simulator XCTest:
  ```sh
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=CF18689D-6D0E-440F-B301-933C7A063CD9' CODE_SIGNING_ALLOWED=NO -only-testing:iosAppTests/RhythHausCarPlayTemplateTests test
  ```
  **PASS:** `TEST SUCCEEDED`; 8 tests, 0 failures. The suite includes the app-bundle scene-manifest assertion. The build phase compiled the Swift consumer against Shared and reported `BUILD SUCCESSFUL` for the Kotlin framework task.
- Unsigned generic iOS Simulator app build:
  ```sh
  xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
  ```
  **PASS:** exit code 0; no stdout/stderr.
- Inspected the generated `Shared.h` in `shared/build/xcode-frameworks/Debug/iphonesimulator27.0/Shared.framework/Headers/Shared.h`; it exposes `SharedCarPlayFacade`, the snapshot-observer API, and `sharedCarPlayFacade()`. The successful Xcode test build also compiles the scene delegate's Swift-facing facade calls.
- `openspec status --change carplay-audio-integration --json` and `openspec instructions apply --change carplay-audio-integration --json` reported a complete spec-driven planning set and 8/15 tasks complete before this implementation. After the Task 3 checkbox update, the apply instructions were refreshed to verify the updated task progress.

## Decisions and evidence limits

- Preserved the partial scene, template factory, test, manifest, localization, and app integration writes after auditing them. The completed policy tests exercise the real template/session policies with a narrow fake snapshot source; production selection still revalidates through Shared.
- The implementation and tests were already present when this interrupted lane resumed. No pre-implementation XCTest RED output was available in the worktree, so this report does not claim a historical RED run. The focused tests passed after the partial implementation audit.
- Used `CPNowPlayingTemplate.shared` rather than adding a second metadata or transport owner; the existing app-lifetime `RhythHausSystemMediaPublisher` remains authoritative.
- CarPlay runtime/vehicle smoke, entitlement approval, and signing validation were not performed; those are Task 4 acceptance work, not Task 3.
- Preserved unrelated dirty files `roadmap.md` and `iosApp/iosAppTests/RhythHausAudioPlayerProviderTests.swift`; they are not part of the Task 3 commit.

## Concerns

The simulator test and unsigned app build establish compilation, policy behavior, and scene-manifest registration, not a live CarPlay display session. No implementation or compile/API blocker remains for Task 3.
