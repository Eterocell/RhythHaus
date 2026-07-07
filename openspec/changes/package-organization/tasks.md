# Tasks

- [ ] 1. Move library UI/navigation into `library.ui`.
  - [ ] Move library UI/orchestration common files to `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/`.
  - [ ] Update package declarations and imports while preserving `App()` at the root package.
  - [ ] Move package-sensitive library UI/navigation tests to `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/`.
  - [ ] Run focused library navigation tests and shared JVM compile.

- [ ] 2. Move feature screens into feature packages.
  - [ ] Move Now Playing screen/bar UI to `com.eterocell.rhythhaus.nowplaying`.
  - [ ] Move Search UI to `com.eterocell.rhythhaus.search`.
  - [ ] Move Settings UI to `com.eterocell.rhythhaus.settings`.
  - [ ] Update library route/shell imports and any tests that reference moved symbols.
  - [ ] Run focused tests and shared JVM compile.

- [ ] 3. Move reusable UI and theme helpers with expect/actual alignment.
  - [ ] Move shared UI helper files to `com.eterocell.rhythhaus.ui`.
  - [ ] Move theme files and every platform actual to `com.eterocell.rhythhaus.theme`.
  - [ ] Move or update affected tests.
  - [ ] Run focused tests, shared JVM compile, and Android/iOS compile coverage if expect/actual declarations move.

- [ ] 4. Move playback/model files only where safe.
  - [ ] Assess playback and model move blast radius from current imports and platform source sets.
  - [ ] Move playback files to `com.eterocell.rhythhaus.playback` if compile-safe without Swift/API churn.
  - [ ] Move model/metadata/import-label files to `com.eterocell.rhythhaus.model` if expect/actual alignment is manageable.
  - [ ] Record any intentional deferrals in the task report.
  - [ ] Run focused tests and shared JVM compile.

- [ ] 5. Final verification and evidence.
  - [ ] Run `openspec validate package-organization --strict`.
  - [ ] Run package-correct focused tests for library navigation and any moved helper tests.
  - [ ] Run `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
  - [ ] Run `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`.
  - [ ] Run `git diff --check`.
  - [ ] Update `progress.md` with route, verification, changed files, blockers/deferrals, and next owner.
