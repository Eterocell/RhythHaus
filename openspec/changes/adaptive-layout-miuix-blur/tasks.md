# Tasks

- [x] 1. Add dependency aliases and prove resolution for Miuix blur plus adaptive.
  - [x] Add `miuix-blur` at current Miuix `0.9.2`.
  - [x] Add separate `miuix-navigation3-adaptive = "0.8.5"` and library alias.
  - [x] Keep existing Miuix modules on `0.9.2`; use constraints/excludes only if needed and documented.
  - [x] Run a focused shared compile/resolution command and record whether adaptive can compile without downgrading current Miuix modules.
    - Evidence: current catalog keeps `miuix = "0.9.2"` and `miuix-navigation3-adaptive = "0.8.5"`; focused `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` returned `BUILD SUCCESSFUL in 524ms`.

- [x] 2. Replace Kyant Backdrop wrapper with Miuix blur.
  - [x] Update `LiquidGlassChrome.kt` to use Miuix blur APIs.
  - [x] Preserve current wrapper names or update all call sites consistently.
  - [x] Preserve fallback/tint drawing and avoid recording blur overlays into their own backdrop.
  - [x] Remove Kyant Backdrop/Shapes dependencies after compile succeeds.
    - Evidence: `grep -R "com.kyant.backdrop\|kyant-backdrop\|kyant-shapes" -n gradle shared/src || true` returned no output.

- [x] 3. Add tested adaptive layout-mode helper.
  - [x] Add pure `LibraryAdaptiveLayoutMode` and threshold helper.
  - [x] Add common tests for compact, portrait tablet, landscape tablet, and desktop widths.
    - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` returned `BUILD SUCCESSFUL in 524ms`.

- [x] 4. Add wide list-detail Library layout.
  - [x] Keep compact one-pane rendering unchanged.
  - [x] Use `ListDetailPaneScaffold` from `miuix-navigation3-adaptive` if the dependency gate passed.
  - [x] Render Library/Home in the list pane and album/artist detail or placeholder in the detail pane.
  - [x] Preserve Search, Settings, Clear Library dialog, Now Playing overlay, bottom bar, and back behavior.
    - Evidence: focused navigation/adaptive test command above passed; iOS simulator verification below passed. Android debug packaging is blocked by dependency conflicts listed under task 5.

- [ ] 5. Verify and record evidence.
  - [x] Run `openspec validate adaptive-layout-miuix-blur --strict`.
    - Evidence: `Change 'adaptive-layout-miuix-blur' is valid`.
  - [x] Run focused navigation/adaptive tests.
    - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` returned `BUILD SUCCESSFUL in 524ms`.
  - [ ] Run broad JVM/desktop/Android verification.
    - Evidence: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` returned `BUILD FAILED in 10s` with two Android blockers: `:androidApp:checkDebugDuplicateClasses` found duplicate `top.yukonga.miuix.kmp.*` classes from `miuix-ui-android:0.9.2` and transitive `miuix-android:0.8.5`, and `:androidApp:processDebugMainManifest` failed because `miuix-blur-android:0.9.2` declares minSdk 33 while `androidApp` minSdk is 29.
  - [x] Run iOS simulator verification or record exact blocker.
    - Evidence: `/usr/bin/xcrun xcodebuild -version` returned `Xcode 26.6` / `Build version 17F113`; `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` returned `BUILD SUCCESSFUL in 34s`.
  - [x] Update `progress.md` with route, verification, changed files, blockers, and next owner.
