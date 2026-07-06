# Tasks

- [x] 1. Add Backdrop dependencies and a local RhythHaus glass wrapper.
  - Evidence: commit `a29a6cd` (`feat: add liquid glass backdrop wrapper`); `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.
- [x] 2. Record Library/Home content and apply Backdrop glass to nested-scroll top chrome.
  - Evidence: commit `bd3c6d5` (`feat: apply backdrop glass to library chrome`); `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.
- [x] 3. Record album/artist track-list content and apply Backdrop glass to nested-scroll top chrome.
  - Evidence: commit `12802cc` (`feat: apply backdrop glass to drilldown chrome`); `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` and `./gradlew :shared:compileKotlinJvm --configuration-cache` passed.
- [x] 4. Apply Backdrop glass to the bottom NowPlayingBar card while preserving existing controls and gestures.
  - Evidence: commit `acc0df6` (`feat: apply backdrop glass to bottom bar`); `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache`, `./gradlew :shared:compileKotlinJvm --configuration-cache`, and review passed.
- [x] 5. Run supported-platform verification and update progress/OpenSpec evidence.
  - Evidence: `openspec validate liquid-glass-backdrop-chrome --strict` passed; `git diff --check` passed; `/usr/bin/xcrun xcodebuild -version` reported Xcode 26.6 Build version 17F113; `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed; `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` passed.
